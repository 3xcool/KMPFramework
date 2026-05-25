package com.tekmoon.permissions

import com.tekmoon.logger.Loggers
import com.tekmoon.logger.ShowMeLoggerK
import com.tekmoon.presentation.viewmodel.CommonViewModel
import com.tekmoon.utilities.DispatcherProvider
import com.tekmoon.utilities.StandardDispatchers
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface PermissionsAction {
    data class Check(val autoRequest: Boolean = false) : PermissionsAction
    data class RequestOne(val permission: PermissionType) : PermissionsAction
    data object RequestAll : PermissionsAction
    data object OpenSettings : PermissionsAction
    data object ResetAlreadyRequestedFlags : PermissionsAction
}

sealed interface PermissionsEvent {
    data object Granted : PermissionsEvent
    data object Denied : PermissionsEvent
    data object OpenedSettings : PermissionsEvent
}

@OptIn(ExperimentalTime::class)
class PermissionsViewModel(
    private val controller: PermissionsController,
    private val requiredPermissions: List<PermissionType>,
    dispatchers: DispatcherProvider = StandardDispatchers,
    logger: ShowMeLoggerK? = Loggers.current,
) : CommonViewModel<PermissionsAction, PermissionsEvent, PermissionsUiState>(
    dispatchers = dispatchers,
    logger = logger,
) {

    private var awaitingSettingsReturn: Boolean = false
    private var lastOutcomeEmitted: PermissionsEvent? = null

    override fun initialState(): PermissionsUiState = PermissionsUiState()

    override suspend fun setup() {
        // Kick off the first read of the system state.
        checkPermissions()
    }

    override fun onAction(action: PermissionsAction) {
        when (action) {
            is PermissionsAction.Check -> checkPermissions(action.autoRequest)
            is PermissionsAction.RequestOne -> requestPermission(action.permission)
            PermissionsAction.RequestAll -> requestAllPermissions()
            PermissionsAction.OpenSettings -> openSettings()
            PermissionsAction.ResetAlreadyRequestedFlags -> resetAlreadyRequestedFlags()
        }
    }

    // ---------- Public convenience entry points (also accessible via onAction). ----------

    fun checkPermissions(autoRequest: Boolean = false) {
        launch(name = "checkPermissions") {
            val permissionStatuses = buildPermissionStatusesPreservingAlreadyRequested()
            val allGranted = permissionStatuses.values
                .filter { it.isRequired }
                .all { it.state == PermissionState.GRANTED }

            updateState { prev ->
                prev.copy(
                    permissions = permissionStatuses,
                    allRequiredGranted = allGranted,
                    isLoading = false,
                    updateTrigger = Clock.System.now().toEpochMilliseconds(),
                )
            }

            if (awaitingSettingsReturn) {
                awaitingSettingsReturn = false
                emitOutcome(allGranted)
            }

            if (!allGranted && autoRequest) {
                requestAllPermissions()
            }
        }
    }

    fun resetAlreadyRequestedFlags() {
        awaitingSettingsReturn = false
        updateState { state ->
            state.copy(
                permissions = state.permissions.mapValues { (_, v) ->
                    v.copy(isAlreadyRequested = false)
                },
            )
        }
    }

    fun requestPermission(permission: PermissionType) {
        launch(name = "requestPermission") {
            try {
                markAlreadyRequested(permission)

                updateState { it.copy(isRequestingPermissions = true) }
                val resultState = controller.requestPermission(permission)
                updatePermissionState(permission, resultState)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logSafe { "requestPermission failed -> ${e.stackTraceToString()}" }
            } finally {
                updateState { it.copy(isRequestingPermissions = false) }
                withState { state ->
                    if (!awaitingSettingsReturn) emitOutcome(state.allRequiredGranted)
                }
            }
        }
    }

    /**
     * Sequentially await each result and update right after each grant/deny.
     * Emits:
     * - OpenedSettings when redirecting to settings
     * - Granted/Denied when the request flow completes (unless we redirected to settings;
     *   in that case we defer until the next checkPermissions after return).
     */
    fun requestAllPermissions() {
        launch(name = "requestAllPermissions") {
            try {
                updateState { it.copy(isRequestingPermissions = true) }

                for (permission in requiredPermissions) {
                    val permissionStatus = state.value.permissions[permission]
                    val current = permissionStatus?.state ?: PermissionState.NOT_DETERMINED
                    val alreadyRequested = permissionStatus?.isAlreadyRequested == true

                    when (current) {
                        PermissionState.GRANTED -> {
                            // no-op
                        }

                        PermissionState.DENIED_ALWAYS,
                        PermissionState.DENIED,
                        PermissionState.NOT_DETERMINED -> {
                            if (alreadyRequested) {
                                // SECOND PASS: already prompted -> send user to settings
                                markAlreadyRequested(permission)
                                openSettings()
                                continue
                            }

                            // FIRST PASS: actually request
                            markAlreadyRequested(permission)

                            val startTime: Instant = Clock.System.now()
                            val resultState = controller.requestPermission(permission)
                            val elapsedMs = (Clock.System.now() - startTime).inWholeMilliseconds

                            updatePermissionState(permission, resultState)

                            if (elapsedMs < 200) {
                                val stateNow = state.value.permissions[permission]?.state
                                if (stateNow == PermissionState.DENIED_ALWAYS) {
                                    openSettings()
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logSafe { "requestAllPermissions failed -> ${e.stackTraceToString()}" }
            } finally {
                updateState { it.copy(isRequestingPermissions = false) }
                withState { state ->
                    if (!awaitingSettingsReturn) {
                        emitOutcome(state.allRequiredGranted)
                    } else {
                        logSafe { "Request finished but awaiting settings return -> defer outcome" }
                    }
                }
            }
        }
    }

    fun openSettings() {
        awaitingSettingsReturn = true
        lastOutcomeEmitted = null
        emit(PermissionsEvent.OpenedSettings)
        controller.openAppSettings()
    }

    // ---------- Internal helpers ----------

    private fun emitOutcome(allGranted: Boolean) {
        val event = if (allGranted) PermissionsEvent.Granted else PermissionsEvent.Denied
        if (lastOutcomeEmitted == event) return
        lastOutcomeEmitted = event
        emit(event)
    }

    private suspend fun buildPermissionStatusesPreservingAlreadyRequested(): Map<PermissionType, PermissionStatus> {
        val permissionStatuses = mutableMapOf<PermissionType, PermissionStatus>()
        requiredPermissions.forEach { p ->
            val state = controller.getPermissionState(p)
            val prev = this.state.value.permissions[p]
            permissionStatuses[p] = PermissionStatus(
                permission = p,
                state = state,
                isRequired = true,
                isAlreadyRequested = prev?.isAlreadyRequested ?: false,
            )
        }
        return permissionStatuses
    }

    private fun markAlreadyRequested(permission: PermissionType) {
        updateState { state ->
            val updated = state.permissions.toMutableMap()
            val existing = updated[permission]
            if (existing != null && !existing.isAlreadyRequested) {
                updated[permission] = existing.copy(isAlreadyRequested = true)
            } else if (existing == null) {
                updated[permission] = PermissionStatus(
                    permission = permission,
                    state = PermissionState.NOT_DETERMINED,
                    isRequired = true,
                    isAlreadyRequested = true,
                )
            }
            state.copy(permissions = updated)
        }
    }

    private fun updatePermissionState(permission: PermissionType, newState: PermissionState) {
        val updated = state.value.permissions.toMutableMap()

        val existing = updated[permission]
        updated[permission] = (existing ?: PermissionStatus(permission, newState, isRequired = true))
            .copy(
                state = newState,
                isAlreadyRequested = existing?.isAlreadyRequested ?: false,
            )

        val allGranted = updated.values
            .filter { it.isRequired }
            .all { it.state == PermissionState.GRANTED }

        updateState { prev ->
            prev.copy(
                permissions = updated,
                allRequiredGranted = allGranted,
                updateTrigger = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    private inline fun logSafe(crossinline message: () -> String) {
        logger?.d(message())
    }
}

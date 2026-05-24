package com.tekmoon.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tekmoon.logger.ShowMeLoggerK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface PermissionsEvent {
    data object Granted : PermissionsEvent
    data object Denied : PermissionsEvent
    data object OpenedSettings : PermissionsEvent
}

@OptIn(ExperimentalTime::class)
class PermissionsViewModel(
    private val controller: PermissionsController,
    private val requiredPermissions: List<PermissionType>,
    private val logger: ShowMeLoggerK? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<PermissionsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var awaitingSettingsReturn: Boolean = false
    private var lastOutcomeEmitted: PermissionsEvent? = null

    init {
        checkPermissions()
    }

    fun checkPermissions(autoRequest: Boolean = false) {
        viewModelScope.launch {
            val permissionStatuses = buildPermissionStatusesPreservingAlreadyRequested()
            val allGranted = permissionStatuses.values
                .filter { it.isRequired }
                .all { it.state == PermissionState.GRANTED }

            _uiState.update { prev ->
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
        _uiState.update { state ->
            state.copy(
                permissions = state.permissions.mapValues { (_, v) ->
                    v.copy(isAlreadyRequested = false)
                },
            )
        }
    }

    fun requestPermission(permission: PermissionType) {
        viewModelScope.launch {
            try {
                markAlreadyRequested(permission)

                _uiState.update { it.copy(isRequestingPermissions = true) }
                val resultState = controller.requestPermission(permission)
                updatePermissionState(permission, resultState)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger?.e("requestPermission failed -> ${e.stackTraceToString()}")
            } finally {
                _uiState.update { it.copy(isRequestingPermissions = false) }
                if (!awaitingSettingsReturn) {
                    emitOutcome(_uiState.value.allRequiredGranted)
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
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRequestingPermissions = true) }

                for (permission in requiredPermissions) {
                    val permissionStatus = _uiState.value.permissions[permission]
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
                                val state = _uiState.value.permissions[permission]?.state
                                if (state == PermissionState.DENIED_ALWAYS) {
                                    openSettings()
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger?.e("requestAllPermissions failed -> ${e.stackTraceToString()}")
            } finally {
                _uiState.update { it.copy(isRequestingPermissions = false) }

                if (!awaitingSettingsReturn) {
                    emitOutcome(_uiState.value.allRequiredGranted)
                } else {
                    logger?.d("Request finished but awaiting settings return -> defer outcome")
                }
            }
        }
    }

    fun openSettings() {
        awaitingSettingsReturn = true
        lastOutcomeEmitted = null
        _events.trySend(PermissionsEvent.OpenedSettings)
        controller.openAppSettings()
    }

    // -----------------------------
    // Internal helpers
    // -----------------------------

    private fun emitOutcome(allGranted: Boolean) {
        val event = if (allGranted) PermissionsEvent.Granted else PermissionsEvent.Denied
        if (lastOutcomeEmitted == event) return
        lastOutcomeEmitted = event
        _events.trySend(event)
    }

    private suspend fun buildPermissionStatusesPreservingAlreadyRequested(): Map<PermissionType, PermissionStatus> {
        val permissionStatuses = mutableMapOf<PermissionType, PermissionStatus>()
        requiredPermissions.forEach { p ->
            val state = controller.getPermissionState(p)
            val prev = _uiState.value.permissions[p]
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
        _uiState.update { state ->
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

    private fun updatePermissionState(permission: PermissionType, state: PermissionState) {
        val updated = _uiState.value.permissions.toMutableMap()

        val existing = updated[permission]
        updated[permission] = (existing ?: PermissionStatus(permission, state, isRequired = true))
            .copy(
                state = state,
                isAlreadyRequested = existing?.isAlreadyRequested ?: false,
            )

        val allGranted = updated.values
            .filter { it.isRequired }
            .all { it.state == PermissionState.GRANTED }

        _uiState.update { prev ->
            prev.copy(
                permissions = updated,
                allRequiredGranted = allGranted,
                updateTrigger = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }
}

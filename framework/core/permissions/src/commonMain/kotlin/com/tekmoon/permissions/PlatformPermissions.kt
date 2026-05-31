package com.tekmoon.permissions

import androidx.compose.runtime.Composable

enum class PermissionState {
    NOT_DETERMINED,
    GRANTED,
    DENIED,
    DENIED_ALWAYS,
}

enum class PermissionType {
    CAMERA,
    RECORD_AUDIO,
    LOCATION,
    LOCATION_ALWAYS,
    GALLERY,
    WRITE_STORAGE,
    READ_STORAGE,
    NOTIFICATION,
    CONTACTS,
    CALENDAR,
    BLUETOOTH,
}

data class PermissionStatus(
    val permission: PermissionType,
    val state: PermissionState,
    val isRequired: Boolean = true,
    val isAlreadyRequested: Boolean = false,
)

data class PermissionsUiState(
    val permissions: Map<PermissionType, PermissionStatus> = emptyMap(),
    val allRequiredGranted: Boolean = false,
    val isLoading: Boolean = true,
    val isRequestingPermissions: Boolean = false,
    val updateTrigger: Long = 0L,
) {
    fun hasPendingRequiredPermissions(): Boolean {
        if (permissions.isEmpty()) return false
        return permissions.values.any { it.isRequired && it.state != PermissionState.GRANTED }
    }

    fun hasPermissionDeniedAlways(): Boolean {
        return permissions.values.any { it.isRequired && it.state == PermissionState.DENIED_ALWAYS }
    }
}

/**
 * Common interface for permission handling across platforms.
 */
interface PermissionsController {
    suspend fun getPermissionState(permission: PermissionType): PermissionState
    suspend fun requestPermission(permission: PermissionType): PermissionState
    fun openAppSettings()
}

/**
 * Factory to create platform-specific permission controller.
 */
@Composable
expect fun rememberPermissionsController(): PermissionsController

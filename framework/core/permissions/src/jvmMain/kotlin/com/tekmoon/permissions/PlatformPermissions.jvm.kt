package com.tekmoon.permissions

import androidx.compose.runtime.Composable

/**
 * Desktop JVM implementation of [PermissionsController].
 *
 * Desktop apps typically have no runtime permission system: any permission is treated as
 * [PermissionState.GRANTED]. Override this in a desktop-specific feature if you need to
 * gate on OS-level checks (e.g. macOS camera/microphone via TCC).
 */
@Composable
actual fun rememberPermissionsController(): PermissionsController = DesktopPermissionsController

private object DesktopPermissionsController : PermissionsController {
    override suspend fun getPermissionState(permission: PermissionType): PermissionState = PermissionState.GRANTED
    override suspend fun requestPermission(permission: PermissionType): PermissionState = PermissionState.GRANTED
    override fun openAppSettings() {
        // No-op on desktop.
    }
}

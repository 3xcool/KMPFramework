package com.tekmoon.permissions

import androidx.compose.runtime.Composable

/**
 * iOS implementation of [PermissionsController].
 *
 * TODO: Implement using `UNUserNotificationCenter`, `CLLocationManager`,
 * `AVCaptureDevice`, `PHPhotoLibrary`, etc. depending on [PermissionType].
 */
@Composable
actual fun rememberPermissionsController(): PermissionsController = NotImplementedPermissionsController

private object NotImplementedPermissionsController : PermissionsController {
    override suspend fun getPermissionState(permission: PermissionType): PermissionState {
        throw NotImplementedError("iOS PermissionsController is not implemented yet")
    }

    override suspend fun requestPermission(permission: PermissionType): PermissionState {
        throw NotImplementedError("iOS PermissionsController is not implemented yet")
    }

    override fun openAppSettings() {
        throw NotImplementedError("iOS PermissionsController is not implemented yet")
    }
}

package com.tekmoon.permissions

import androidx.compose.runtime.Composable

/**
 * iOS implementation of [PermissionsScreen].
 *
 * TODO: Wire up the same controller + ViewModel pattern used on Android.
 */
@Composable
actual fun PermissionsScreen(
    requiredPermissions: List<PermissionType>,
    onPermissionsDenied: () -> Unit,
    onAllPermissionsGranted: () -> Unit,
    content: @Composable (PermissionsViewModel) -> Unit,
) {
    throw NotImplementedError("iOS PermissionsScreen is not implemented yet")
}

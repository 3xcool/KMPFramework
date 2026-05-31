package com.tekmoon.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun PermissionsScreen(
    requiredPermissions: List<PermissionType>,
    onPermissionsDenied: () -> Unit = {},
    onAllPermissionsGranted: () -> Unit = {},
    content: @Composable (PermissionsViewModel) -> Unit,
)

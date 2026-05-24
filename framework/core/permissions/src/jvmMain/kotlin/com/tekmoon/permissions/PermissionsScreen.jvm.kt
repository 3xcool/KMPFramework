package com.tekmoon.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Desktop JVM implementation of [PermissionsScreen].
 *
 * Skips the request flow entirely and assumes all permissions are granted, then calls
 * [onAllPermissionsGranted] and renders [content].
 */
@Composable
actual fun PermissionsScreen(
    requiredPermissions: List<PermissionType>,
    onPermissionsDenied: () -> Unit,
    onAllPermissionsGranted: () -> Unit,
    content: @Composable (PermissionsViewModel) -> Unit,
) {
    val controller = rememberPermissionsController()
    val viewModel = remember(requiredPermissions) {
        PermissionsViewModel(controller, requiredPermissions)
    }

    val latestOnGranted = rememberUpdatedState(onAllPermissionsGranted)
    LaunchedEffect(viewModel) {
        latestOnGranted.value()
    }

    content(viewModel)
}

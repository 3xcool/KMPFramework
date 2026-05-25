package com.tekmoon.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
actual fun PermissionsScreen(
    requiredPermissions: List<PermissionType>,
    onPermissionsDenied: () -> Unit,
    onAllPermissionsGranted: () -> Unit,
    content: @Composable (PermissionsViewModel) -> Unit,
) {
    val controller = rememberPermissionsController()

    // Make a stable key per permission set
    val permissionsKey = remember(requiredPermissions) {
        val normalized = requiredPermissions
            .distinct()
            .sortedBy { it.name }
            .joinToString(separator = "_") { it.name }
        "PermissionsViewModel_$normalized"
    }

    val viewModel = viewModel(key = permissionsKey) {
        PermissionsViewModel(
            controller = controller,
            requiredPermissions = requiredPermissions,
        )
    }

    val latestOnDenied = rememberUpdatedState(onPermissionsDenied)
    val latestOnGranted = rememberUpdatedState(onAllPermissionsGranted)

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                PermissionsEvent.Granted -> latestOnGranted.value()
                PermissionsEvent.Denied -> latestOnDenied.value()
                PermissionsEvent.OpenedSettings -> {
                    // do nothing
                }
            }
        }
    }

    content(viewModel)
}

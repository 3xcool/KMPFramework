package com.tekmoon.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.pm.PackageManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [PermissionsController] using Compose-based launcher.
 */
internal class AndroidPermissionsController(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val launcher: PermissionLauncher,
) : PermissionsController {

    private suspend fun awaitResumed() {
        val lifecycle = lifecycleOwner.lifecycle
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        suspendCancellableCoroutine<Unit> { cont ->
            val obs = LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME && cont.isActive) cont.resume(Unit)
            }
            lifecycle.addObserver(obs)
            cont.invokeOnCancellation { lifecycle.removeObserver(obs) }
        }
    }

    override suspend fun getPermissionState(permission: PermissionType): PermissionState {
        val manifestPermission = permission.toManifestPermission() ?: return PermissionState.GRANTED
        return when {
            ContextCompat.checkSelfPermission(
                activity,
                manifestPermission,
            ) == PackageManager.PERMISSION_GRANTED -> PermissionState.GRANTED

            ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission) ->
                PermissionState.DENIED

            else -> PermissionState.NOT_DETERMINED
        }
    }

    override suspend fun requestPermission(permission: PermissionType): PermissionState {
        awaitResumed()
        val manifestPermission = permission.toManifestPermission() ?: return PermissionState.GRANTED

        if (ContextCompat.checkSelfPermission(
                activity,
                manifestPermission,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return PermissionState.GRANTED
        }

        val granted = launcher.requestPermission(manifestPermission)

        return when {
            granted -> PermissionState.GRANTED
            ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission) ->
                PermissionState.DENIED
            else -> PermissionState.DENIED_ALWAYS
        }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    private fun PermissionType.toManifestPermission(): String? = when (this) {
        PermissionType.CAMERA -> Manifest.permission.CAMERA
        PermissionType.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
        PermissionType.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        PermissionType.LOCATION_ALWAYS -> {
            if (Build.VERSION.SDK_INT >= 29) {
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            } else {
                // Before API 29, background location = fine location
                Manifest.permission.ACCESS_FINE_LOCATION
            }
        }
        PermissionType.GALLERY -> Manifest.permission.READ_EXTERNAL_STORAGE
        PermissionType.WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        PermissionType.READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
        PermissionType.NOTIFICATION -> {
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null // no runtime permission pre-33 -> always granted
            }
        }
        PermissionType.CONTACTS -> Manifest.permission.READ_CONTACTS
        PermissionType.CALENDAR -> Manifest.permission.READ_CALENDAR
        PermissionType.BLUETOOTH -> {
            if (Build.VERSION.SDK_INT >= 31) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH // normal permission, implicitly granted
            }
        }
    }
}

/**
 * Wrapper for permission launcher to handle async results using Channel.
 * This ensures sequential permission requests work correctly.
 */
internal class PermissionLauncher(
    private val launcher: (Array<String>) -> Unit,
    private val resultChannel: Channel<Map<String, Boolean>>,
    private val lifecycleOwner: LifecycleOwner,
) {
    private suspend fun awaitResumed() {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        suspendCancellableCoroutine<Unit> { cont ->
            val obs = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && cont.isActive) {
                    cont.resume(Unit)
                }
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            cont.invokeOnCancellation { lifecycleOwner.lifecycle.removeObserver(obs) }
        }
    }

    suspend fun requestPermission(permission: String): Boolean {
        awaitResumed()
        launcher(arrayOf(permission))
        val result = resultChannel.receive()
        return result[permission] ?: false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Create permissions controller in a Composable context.
 */
@Composable
actual fun rememberPermissionsController(): PermissionsController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
        ?: throw IllegalStateException(
            "PermissionsController must be used from a composition attached to an Activity. " +
                "Current context: ${context::class.java.name}",
        )

    val resultChannel = remember { Channel<Map<String, Boolean>>(Channel.UNLIMITED) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        resultChannel.trySend(permissions)
    }

    val permissionLauncherWrapper = remember(resultChannel) {
        PermissionLauncher(
            launcher = { permissions -> permissionLauncher.launch(permissions) },
            resultChannel = resultChannel,
            lifecycleOwner = lifecycleOwner,
        )
    }

    return remember(activity, permissionLauncherWrapper) {
        AndroidPermissionsController(activity, lifecycleOwner, permissionLauncherWrapper)
    }
}

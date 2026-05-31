package com.tekmoon.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusDenied
import platform.Contacts.CNAuthorizationStatusNotDetermined
import platform.Contacts.CNAuthorizationStatusRestricted
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKEntityType
import platform.EventKit.EKEventStore
import platform.Foundation.NSURL
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of [PermissionsController].
 *
 * Covers Camera, Microphone, Location, Notification, Photos (Gallery), Contacts, Calendar via the
 * standard Apple authorization APIs. Bluetooth and storage permissions are treated as already
 * granted on iOS — Bluetooth uses a different permission model (per-CBCentralManager instance),
 * and there is no equivalent to Android's WRITE_STORAGE in the iOS sandbox.
 */
@OptIn(ExperimentalForeignApi::class)
internal object IOSPermissionsController : PermissionsController {

    override suspend fun getPermissionState(permission: PermissionType): PermissionState =
        when (permission) {
            PermissionType.CAMERA -> getCameraState()
            PermissionType.RECORD_AUDIO -> getMicrophoneState()
            PermissionType.LOCATION -> getLocationState(always = false)
            PermissionType.LOCATION_ALWAYS -> getLocationState(always = true)
            PermissionType.GALLERY,
            PermissionType.READ_STORAGE -> getPhotosState()
            PermissionType.WRITE_STORAGE -> PermissionState.GRANTED // no iOS equivalent
            PermissionType.NOTIFICATION -> getNotificationState()
            PermissionType.CONTACTS -> getContactsState()
            PermissionType.CALENDAR -> getCalendarState()
            PermissionType.BLUETOOTH -> PermissionState.GRANTED // managed per CBCentralManager
        }

    override suspend fun requestPermission(permission: PermissionType): PermissionState =
        when (permission) {
            PermissionType.CAMERA -> requestCamera()
            PermissionType.RECORD_AUDIO -> requestMicrophone()
            PermissionType.LOCATION -> requestLocation(always = false)
            PermissionType.LOCATION_ALWAYS -> requestLocation(always = true)
            PermissionType.GALLERY,
            PermissionType.READ_STORAGE -> requestPhotos()
            PermissionType.WRITE_STORAGE -> PermissionState.GRANTED
            PermissionType.NOTIFICATION -> requestNotification()
            PermissionType.CONTACTS -> requestContacts()
            PermissionType.CALENDAR -> requestCalendar()
            PermissionType.BLUETOOTH -> PermissionState.GRANTED
        }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) {
            app.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }

    // ---------- Camera ----------

    private fun getCameraState(): PermissionState =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> PermissionState.GRANTED
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted -> PermissionState.DENIED_ALWAYS
            AVAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
            else -> PermissionState.NOT_DETERMINED
        }

    private suspend fun requestCamera(): PermissionState = suspendCancellableCoroutine { cont ->
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            cont.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED_ALWAYS)
        }
    }

    // ---------- Microphone ----------

    private fun getMicrophoneState(): PermissionState {
        val session = AVAudioSession.sharedInstance()
        return when (session.recordPermission()) {
            AVAudioSessionRecordPermissionGranted -> PermissionState.GRANTED
            AVAudioSessionRecordPermissionDenied -> PermissionState.DENIED_ALWAYS
            AVAudioSessionRecordPermissionUndetermined -> PermissionState.NOT_DETERMINED
            else -> PermissionState.NOT_DETERMINED
        }
    }

    private suspend fun requestMicrophone(): PermissionState = suspendCancellableCoroutine { cont ->
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            cont.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED_ALWAYS)
        }
    }

    // ---------- Location ----------

    private fun getLocationState(always: Boolean): PermissionState {
        val status = CLLocationManager().authorizationStatus
        return when (status) {
            kCLAuthorizationStatusAuthorizedAlways -> PermissionState.GRANTED
            kCLAuthorizationStatusAuthorizedWhenInUse ->
                if (always) PermissionState.DENIED else PermissionState.GRANTED
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> PermissionState.DENIED_ALWAYS
            kCLAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
            else -> PermissionState.NOT_DETERMINED
        }
    }

    /**
     * Requests location authorization. iOS delivers the result asynchronously via the
     * `CLLocationManagerDelegate`. To keep this implementation suspend-friendly without
     * adding a long-lived delegate object, we kick off the request, give the system a brief
     * moment to flip the status, and re-read it. For finer-grained control (e.g. ANT-state
     * machines), wire your own `CLLocationManagerDelegate`.
     */
    private suspend fun requestLocation(always: Boolean): PermissionState {
        val manager = CLLocationManager()
        if (always) manager.requestAlwaysAuthorization() else manager.requestWhenInUseAuthorization()
        // Allow the user some time to interact with the prompt before re-reading.
        // The host VM also re-checks state after returning from the system dialog.
        return getLocationState(always)
    }

    // ---------- Photos ----------

    private fun getPhotosState(): PermissionState =
        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized,
            PHAuthorizationStatusLimited -> PermissionState.GRANTED
            PHAuthorizationStatusDenied,
            PHAuthorizationStatusRestricted -> PermissionState.DENIED_ALWAYS
            PHAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
            else -> PermissionState.NOT_DETERMINED
        }

    private suspend fun requestPhotos(): PermissionState = suspendCancellableCoroutine { cont ->
        PHPhotoLibrary.requestAuthorization { status ->
            val mapped = when (status) {
                PHAuthorizationStatusAuthorized,
                PHAuthorizationStatusLimited -> PermissionState.GRANTED
                PHAuthorizationStatusDenied,
                PHAuthorizationStatusRestricted -> PermissionState.DENIED_ALWAYS
                else -> PermissionState.DENIED
            }
            cont.resume(mapped)
        }
    }

    // ---------- Notifications ----------

    private suspend fun getNotificationState(): PermissionState = suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            val mapped = when (status) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional -> PermissionState.GRANTED
                UNAuthorizationStatusDenied -> PermissionState.DENIED_ALWAYS
                UNAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
                else -> PermissionState.NOT_DETERMINED
            }
            cont.resume(mapped)
        }
    }

    private suspend fun requestNotification(): PermissionState = suspendCancellableCoroutine { cont ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                cont.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED_ALWAYS)
            }
    }

    // ---------- Contacts ----------

    private fun getContactsState(): PermissionState =
        when (CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)) {
            CNAuthorizationStatusAuthorized -> PermissionState.GRANTED
            CNAuthorizationStatusDenied,
            CNAuthorizationStatusRestricted -> PermissionState.DENIED_ALWAYS
            CNAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
            else -> PermissionState.NOT_DETERMINED
        }

    private suspend fun requestContacts(): PermissionState = suspendCancellableCoroutine { cont ->
        CNContactStore().requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
            cont.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED_ALWAYS)
        }
    }

    // ---------- Calendar ----------

    private fun getCalendarState(): PermissionState =
        when (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)) {
            EKAuthorizationStatusAuthorized -> PermissionState.GRANTED
            EKAuthorizationStatusDenied,
            EKAuthorizationStatusRestricted -> PermissionState.DENIED_ALWAYS
            EKAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
            else -> PermissionState.NOT_DETERMINED
        }

    private suspend fun requestCalendar(): PermissionState = suspendCancellableCoroutine { cont ->
        EKEventStore().requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
            cont.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED_ALWAYS)
        }
    }
}

@Composable
actual fun rememberPermissionsController(): PermissionsController =
    remember { IOSPermissionsController }

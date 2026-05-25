# Roadmap

Companion to [architecture-overview.md](architecture-overview.md) and [architecture-details.md](architecture-details.md). Lists every item still flagged **Planned** plus the test-coverage gap.

## Conventions

- **Status legend:** ✅ done · 🔄 in progress · ⏭️ next · ⏳ planned · 🧪 needs device testing (deferred).
- **Prioritization:** non-platform work first (Phases 1–3). Anything that requires running on a real device — and especially anything iOS-native that can't be verified from Compose Multiplatform alone — moves to **Phase 4** and is paused until we have device testing in place.
- Each entry maps to a section in `architecture-details.md`.

## Status snapshot (today)

### ✅ Ported and compiling
- `core/presentation` → `CommonViewModel<Action, Event, State>` (MVI base, `DispatcherProvider` + `ShowMeLoggerK?` injected).
- `core/session` → full `DataSession` family (`DataSessionImpl`, `DataSessionSource`, `DataSessionState`, `DataSessionLoadPolicy`, `DataSessionStores`, `DataSessionValueKey`, `FakeDataSession`).
- `core/permissions` → `PermissionsController` + `PermissionsViewModel` (extends `CommonViewModel`) + `PermissionsScreen` + `PermissionDisclaimerDialog` on top of `DsDialogWeb`. Android actual is feature-complete. iOS actual covers Camera/Microphone/Location/Photos/Notification/Contacts/Calendar via the standard Apple APIs.
- `core/media` → `picker` + `camera` + `bitmap` sub-packages. Android actuals are feature-complete (PhotoPicker, FileProvider-based camera, EXIF normalization, Palette dominant color). iOS bitmap covers load/encode/compress via Skia.

### ⏳ Logged in the architecture doc as **Planned** — covered by this roadmap.

## Phase 1 — Core foundation (no device testing)

Cross-cutting building blocks that the rest of the framework depends on. All commonMain — should be doable without running on a device.

### Configuration / Bootstrap
- ⏳ `Framework.start(config: FrameworkConfig.Init)` in `framework/sdk` — single entry point that initializes `FrameworkConfig`, `ShowMeLoggerK`, `HttpClientFactory`, and (later) analytics + crash reporting. Once this exists, the `println(...)` fallbacks in `CommonViewModel` can be removed.

### Domain (`core/domain`)
- ✅ `Resource<T>` wrapper — `sealed interface Resource<T>`: `Idle | Loading | Success(data) | Paginating(data) | Error(errorType, message?, data?)`. Repositories return `Flow<Resource<T>>`; ViewModels collect into `StateFlow`. `DataError` unified into open interface hierarchy (`ClientError`, `ServerError`, `NoInternet`, `Serialization`, `LocalError`, `UnknownError`).
- ✅ Pagination primitives — `Paginator<T,K>` + `PagingSource<T,K>` + `PagingMerger` (UTC conflict resolution) + `PagingState<T>` + `RetryablePagingSource` with exponential back-off via `RetryPolicy` / `withRetry`.
- ✅ Forms / Validation — `ValidationError` (open interface, built-ins: Required/TooShort/TooLong/InvalidEmail/InvalidPattern/Custom), `Validator<T>` + `AsyncValidator<T>` + `and` chaining, `ValidationTrigger` (OnChange/OnBlur/OnSubmit/OnChangeAfterBlur), `FieldState<T>`, `FieldController<T>` (sync+async, scoped coroutines), `FormController` (validateAll, whileSubmitting), `Validators` factory. Presentation: `ValidationError.toUiText()`, `rememberFieldController`, `FieldController.rememberState()`.
- ✅ `UiText` / domain error display — solved via typed `ValidationError` + `BuiltInValidationError` in domain; presentation layer maps to `UiText` via extension. No Compose deps in domain.

### Utilities (`core/utils`)
- ⏳ Date / Time utilities on top of `kotlinx-datetime` — `Instant.format(locale, pattern)`, `LocalDate.relative(now)`, time-zone-safe `now()` via overridable `Clock`.
- ⏳ Move Screen Size (`DeviceScreenConfiguration`) from `core/presentation` to `core/utils` so non-Compose modules can consume it.

### Data (`core/data`)
- ✅ SQLDelight wiring — `DatabaseDriverFactory` expect/actual (Android `AndroidSqliteDriver`, iOS `NativeSqliteDriver`, JVM `JdbcSqliteDriver` → `java.io.tmpdir`). `FlowQuery` extensions (`asFlowList`, `asFlowOne`, `asFlowOneOrNull`). `InstantColumnAdapter`, `LocalDateColumnAdapter`, `LocalDateTimeColumnAdapter`. `SqlDelightConventionPlugin` registered in build-logic. Schema files are intentionally client-owned — framework is driver-only.
- ✅ Typed WebSocket client wrapper — `RealtimeClient<Event>` interface + `RealtimeClientImpl` (Ktor-backed). `RealtimeConfig<Event>` with pluggable `ReconnectPolicy` (ExponentialBackoff / Fixed / Custom / Never), `TokenDelivery` (QueryParam / Header / Custom), `ConnectionState` flow, typed `events: SharedFlow<Event>`, `FakeRealtimeClient` test double. `ktor-client-websockets` added to version catalog.
- ✅ HTTP interceptor for auth header injection + retry, plumbed through `HttpClientFactory`. `TokenProvider` interface, `TokenGate` (single-flight mutex with snapshot-before-wait thundering-herd protection), `AuthEvent.SessionExpired` via `SharedFlow`, `NoAuth` opt-out attribute.

### Session (`core/session`)
- ⏳ Verify `DataSessionImpl` IO ops actually run on `dispatchers.io` once the consuming feature ports — the default scope now uses `mainImmediate`, but `draftStore.save` / `source.saveLocal` happen on whoever invoked the mutation. Consider wrapping store calls in `withContext(dispatchers.io)`.

### UI components (`core/designsystem/components`)
- ✅ `DsBanner` — full-width prominent prompt with Info/Success/Warning/Danger types, required primary action, optional secondary action, optional dismiss (required `dismissContentDescription` when dismissable). Leading accent stripe, type-tinted background.
- ✅ `DsToast` + `DsToastHost` — transient themed feedback, no action. `DsToastController` + `rememberDsToastController` mirror the snackbar pattern. Each `show()` fires `PlatformAccessibility.announce()` so TalkBack/VoiceOver read the message, and the host marks itself as a `LiveRegionMode.Polite` semantics region for belt-and-suspenders coverage.
- ✅ Accessibility audit pass — required `contentDescription` parameters on every interactive primitive (DsAlert dismiss, DsClickableText/DsLinkText, DsTextField password toggle), Role.Button semantics throughout, `dsMinimumTouchTarget(48.dp)` wrapper enforcing tap regions on Small/Medium variants without changing visuals, and `LocalDsFontScale` bridging iOS `UIContentSizeCategory` into DsTheme typography (Android/JVM let Compose `.sp` handle OS scaling natively).

### Logging
- ⏳ `framework/logger-file` — new sub-module. `FileLogWriter` implementing `LogWriter`, writing a rolling log file in the platform cache directory (Android `cacheDir`, iOS `NSCachesDirectory`, JVM `System.getProperty("java.io.tmpdir")`). Kept separate so the engine stays free of file-I/O deps.
- ⏳ `framework/logger-remote` — new sub-module. `RemoteLogWriter` that batches + ships logs via `core/data`'s `HttpClient`. Kept separate so the engine doesn't pull in networking.

### Localization / i18n
- ⏳ Document supported-languages list + fallback strategy.
- ⏳ Locale-aware formatting helpers (date, number, currency) — coordinates with the Date/Time work in Utilities.

### SDK / Public API (`framework/sdk`)
- ⏳ Expand `framework/sdk` from "umbrella module" to a real bootstrap surface (see Configuration above).

## Phase 2 — Core Utils (common code only, platform actuals deferred)

### Connectivity (new module `framework/core/connectivity`)
- ⏳ `expect class ConnectivityObserver` with `val status: Flow<ConnectionStatus>` (`Available` / `Unavailable` / `Losing` / `Lost`).
- ⏳ Android actual via `ConnectivityManager.NetworkCallback`.
- ⏳ Desktop JVM actual via `InetAddress.isReachable` polling fallback.
- 🧪 iOS actual via `NWPathMonitor` — needs device testing (Phase 4).

### Storage (new module `framework/core/storage`)
- ⏳ `expect class SecureStore` — Android EncryptedSharedPreferences/Keystore, iOS Keychain, JVM Secret Service / Keychain. Consumed by Auth.Cryptography.
- ⏳ `expect class FileStorage` — downloads / attachments directory abstraction.
- ⏳ `expect class Preferences` — `multiplatform-settings` or DataStore-equivalent.

## Phase 3 — Core Features (new modules)

Each feature lives in `framework/feature/<name>` and is re-exported through `framework/sdk`. Order roughly by which the consuming app needs first.

### Auth (`framework/feature/auth`)
- ⏳ `TokenManager` — store/refresh access + refresh tokens, single-flight refresh, expose `getValidAccessToken()`.
- ⏳ Ktor `Auth` plugin integration / custom `HttpClientFactory` interceptor for 401 retry.
- ⏳ `AuthRepository` — login / logout / register, returns `Result<User, DataError.Remote>`.
- ⏳ `SessionManager` — observable `SessionState` (`Flow<SessionState>`), forced sign-out on refresh failure.
- ⏳ Cryptography wrapper around `core/storage`'s `SecureStore`.

### Analytics (`framework/feature/analytics`)
- ⏳ `expect class AnalyticsClient { fun track(event: String, params: Map<String, Any?>) }`.
- ⏳ Adapters: Firebase Analytics, Mixpanel, Amplitude.

### Device (`framework/feature/device`)
- ⏳ `expect val deviceInfo: DeviceInfo` exposing `osName`, `osVersion`, `model`, `manufacturer`, `appVersion`.
- ⏳ Battery / locale / time zone / density helpers (some already in `DeviceScreenConfiguration`).
- ⏳ WiFi / IP address — pair with Permissions where the platform requires it.

### Push Notifications (`framework/feature/push`)
- ⏳ Common `PushMessage` model + `PushReceiver` interface.
- ⏳ Topic subscriptions API.
- ⏳ Android FCM integration (`FirebaseMessaging.getToken`, foreground/background handlers). Requires Google Services in the consuming app's manifest.
- 🧪 iOS APNs integration via `UNUserNotificationCenter` — Phase 4 (device testing).

### Crash Reporting (`framework/feature/crash`)
- ⏳ `expect class CrashReporter` with `recordException` + `setUserId` + breadcrumbs.
- ⏳ Crashlytics adapter, Sentry adapter.
- ⏳ Bridge `ShowMeLoggerK` Error/Warn severities → breadcrumbs (implement as a `LogWriter`, mirroring the file/remote writer pattern).

### Feature Flags / Remote Config (`framework/feature/feature-flags`)
- ⏳ `expect class FeatureFlagClient { fun <T> get(key, default): T; fun observe(key): Flow<T> }`.
- ⏳ Adapters: Firebase Remote Config, ConfigCat, GrowthBook.
- ⏳ Default-value registration at startup so the app always has a fallback before the first sync.

### Background Work (`framework/feature/background-work`)
- ⏳ `expect class BackgroundScheduler` with `schedule(task: BackgroundTask)`.
- ⏳ Android actual via WorkManager.
- 🧪 iOS actual via `BGTaskScheduler` — Phase 4 (device testing).

### App Update / In-app Review (`framework/feature/app-update`)
- ⏳ Force / soft update flow driven by `min_supported_version` / `recommended_version` flags from Feature Flags.
- ⏳ Android Play In-App Review integration.
- 🧪 iOS `SKStoreReviewController.requestReview()` — Phase 4 (device testing).

## Phase 4 — Platform-specific work (needs device testing — deferred)

> **Note:** Everything below is paused until we have a device-testing setup (real device + entitlements + provisioning). Implementing iOS-native Compose interop without being able to run it on hardware too easily ships code that "looks right" but doesn't actually work. Same applies to FCM/APNs token flows and OS-level entitlements.

### Permissions — iOS verification
- 🧪 Verify every flow on a real iOS device: Camera, Microphone, Photos, Notifications (incl. provisional), Contacts, Calendar, Location (when-in-use vs. always).
- 🧪 The current Location implementation re-reads `authorizationStatus` after `requestWhenInUseAuthorization`/`requestAlwaysAuthorization` returns. The actual prompt resolution is delivered via `CLLocationManagerDelegate` — may need to switch to a delegate-based wait pattern depending on what device testing shows.

### Permissions — Desktop JVM
- 🧪 Real OS-level checks where applicable (macOS TCC for camera / microphone / accessibility). Currently every permission returns `GRANTED` on JVM.

### MediaPicker — iOS
- 🧪 `PHPickerViewController` integration:
  - Build `PHPickerConfiguration` from `mediaPickerTypes` + `selectionLimit`.
  - Delegate translating `[PHPickerResult]` → `[PickedMediaData]` via `itemProvider.loadFileRepresentationForTypeIdentifier`.
  - Presentation from current root view controller (`UIApplication.sharedApplication.keyWindow?.rootViewController`).
  - Honor `returnBytes` / `maxBytesToReturn`.

### Camera — iOS
- 🧪 `UIImagePickerController` integration:
  - `sourceType = .camera`, `cameraDevice` per `CameraType`, `mediaTypes` per `CameraMode`.
  - Delegate writing capture to `NSTemporaryDirectory`, extracting `width`/`height` (UIImage) or `duration`/`width`/`height` (AVURLAsset).
  - Honor `returnBytes` / `maxBytesToReturn`.

### Bitmap — iOS
- 🧪 `getDominantColorFromUrl` / `getDominantColorFromBitmap` — implement with `CIAreaAverage` on a downsampled CIImage. Currently returns `Color.Black`.

### MediaPicker — Desktop JVM
- ⏳ `JFileChooser` / `FileDialog` for desktop picking. Lower priority than the iOS work above.

### Bitmap — Desktop JVM
- ⏳ `loadImageBitmap` / `encodeImageBitmapToPng` / `compressImageToJpeg` using `BufferedImage` + `ImageIO`. Currently returns stubs.

## Phase 5 — Tests (cross-cutting; currently at zero coverage)

The framework has **no unit tests across the ported / new modules.** This is the single biggest gap before the framework can be considered stable. Tests should land alongside or immediately after each Phase 1–3 item, not as a separate cleanup pass.

### Existing modules (gaps to close)
- ⏳ `core/utils` — `StandardDispatchers` returns the canonical `Dispatchers.*`; `Random` determinism for tests.
- ⏳ `core/domain` — `Result.map / onSuccess / onFailure` semantics; `DataError` enum exhaustiveness.
- ⏳ `framework/logger` — `ShowMeLoggerK` routes through every `LogWriter`; `Severity.isLoggable` filtering; `LoggerConfig` enabled flag short-circuits; `CommonLogWriter` formatting; `EmptyCommonLogWriter` no-op behavior.
- ⏳ `core/designsystem` — snapshot tests for `Ds*` components (Paparazzi-equivalent or Roborazzi).
- ⏳ `framework/kompass` — audit existing samples vs. tests; add tests for back-stack reducer, deep link resolution, scope lifecycle (`defaultScope` vs `newScope`), and process-death save/restore.

### New / ported modules (no tests today)
- ⏳ `core/presentation` — `CommonViewModel`:
  - state collection initial value from `initialState()`;
  - `setup()` runs once on first collect;
  - `emit()` delivers events in order;
  - `withCatching` swallows exceptions and routes to `logger`;
  - exception handler in scope routes to `logger`;
  - scope cancelled when VM is cleared.
- ⏳ `core/session` — `DataSessionImpl`:
  - combine of draft + local + remote + savedState resolves correctly per `DataSessionLoadPolicy`;
  - `resolveConflict` is consulted when both local and remote present;
  - `hasChanges` reflects source-defined change detection;
  - `refresh()` is CONFLATED (back-to-back refreshes coalesce);
  - `attach()` is idempotent for same `sessionId`/`entityId`;
  - `clearSession(clearSavedState = true/false)` resets state and conditionally clears `savedStateStore`;
  - `savedStateFlow` decodes via the key's `serializer`;
  - `updateSavedState` round-trips through the store. Use `InMemoryDraftStore` + an in-memory `SavedStateStore` test double.
- ⏳ `core/permissions` — `PermissionsViewModel`:
  - initial state has `isLoading = true` and flips to `false` after first `checkPermissions`;
  - first-pass request → `DENIED` emits `PermissionsEvent.Denied`;
  - second-pass already-requested routes to `openSettings()` and emits `OpenedSettings`;
  - `awaitingSettingsReturn` defers outcome emission until the next `checkPermissions`;
  - `resetAlreadyRequestedFlags()` clears the per-permission `isAlreadyRequested` flag.
  - Use a `FakePermissionsController` for these.
- ⏳ `core/media`:
  - `PickedMediaData` equality + hashCode contract (with and without `bytes`).
  - `ImageSource.Bytes` content equality.

### Integration / framework-level
- ⏳ `framework/sdk` — `Framework.start(config)` initializes sub-systems in expected order; double-start is idempotent; missing config produces a clear error.
- ⏳ CI workflow: `compileKotlinJvm` for every module on PR; `assembleDebug` for Android sample; `compileKotlinIosArm64` for iOS targets; later, snapshot tests + unit-test gates.

## Quick wins (small, do-anytime)

- ⏳ Once `Framework.start()` guarantees a `ShowMeLoggerK`, remove the `println(...)` fallbacks in `CommonViewModel` and `PermissionsViewModel.logSafe`.
- ⏳ Move `Screen Size` from `core/presentation/DeviceScreenConfiguration.kt` to `core/utils` (listed under Utilities above — small enough to do whenever).
- ⏳ Add KDoc headers to ported types that came from Cantina without doc comments (most are documented, a few sparse).
- ⏳ Verify the new `core/permissions`, `core/session`, and `core/media` modules are picked up by the `framework/sdk` umbrella in IDE indexing (the `api(...)` lines are in `sdk/build.gradle.kts`).
- ⏳ Tag the framework `0.0.2` once Phase 1 closes — `Framework.VERSION` already exists in `framework/sdk/.../Framework.kt`.

## Out of scope (for now)

- Compose for Web (wasmJs) targets — none of the ported code touches it, and Compose Multiplatform Web is still moving fast.
- Server-side adapters (kotlinx.rpc, ktor server). Framework is mobile/desktop client only.
- Code-generation tooling (KSP processors for repositories or DTOs).

# Architecture — Details

Spec-level companion to [architecture-overview.md](architecture-overview.md). Each section lists what the topic is, the Gradle module it lives in, and the key types/files that back it. Unimplemented items are flagged **Planned**. Items already implemented in another codebase (e.g. UnifiedCantina) that need to be ported are flagged **Ported (cleanup needed)** with the cleanup notes.

## Layering & rules

Dependency direction: **Core ← Core Utils ← Core Features**.

- **Core** is the foundation and must stay agnostic. UI components in Core never bundle a platform behavior (no "ask-for-permission" inside a `Ds*` component).
- **Core Utils** sits a level above Core and may depend on Core (including UI components). This is the layer where platform behaviors (permissions, connectivity, media access) are combined with UI primitives. The permission rationale dialog lives here, not in `core/designsystem/components`.
- **Core Features** sits above both and is where end-user feature modules live.

Two cross-cutting rules that apply everywhere:

- **Logging** is always done through `ShowMeLoggerK`. Do not import `co.touchlab.kermit.Logger` or `println` for logging in framework code.
- **Threading** is always done through an injected `DispatcherProvider`. Do not import `Dispatchers.IO` / `Dispatchers.Default` directly inside framework code — take a `DispatcherProvider` as a constructor parameter.

These two rules drive most of the "cleanup needed" notes below.

## Core

### Configuration
Module: `framework/core/data` (package `com.tekmoon.data`)

- **FrameworkConfig** — `FrameworkConfig.kt`. Singleton `object` exposing `apiBaseUrl`, `apiKey`, `environment`, `debug`, `isPro`, `adEnabled`. Consumer apps must call `FrameworkConfig.init(...)` before any networking happens.
- **Bootstrap / init flow** — **Planned**. Extract `Framework.start(config: FrameworkConfig.Init)` in `framework/sdk` that initializes `FrameworkConfig`, `ShowMeLoggerK`, `HttpClientFactory`, analytics, and crash reporting in a single entry point.

### System Design
Module: `framework/core/designsystem` (package `com.tekmoon.designsystem`)

Token-based theme exposed via `DsTheme`. All UI primitives consume these tokens instead of Material defaults.

- **Colors** — `foundation/DsColors.kt`: `DsColors` (surfaces `bgDark`/`bg`/`bgLight`, text `text`/`textMuted`, content `content`/`contentMuted`, accent `primary`/`primaryMuted`/`onPrimary`, alerts `danger`/`warning`/`success`/`info`).
- **Fonts / Typography** — `foundation/DsTypography.kt`.
- **Theming** — `DsTheme` composition local + `foundation/DsSurface.kt`, `foundation/DsElevation.kt`, `foundation/DsShapes.kt`, `foundation/DsShadows.kt`, `foundation/DsRipple.kt`.
- **Dimens** — covered by `DsElevation`, `DsShapes`, and typography sizes inside `DsTypography`.
- **Spacing** — `foundation/DsSpacing.kt`: raw tokens `xs`/`sm`/`md`/`lg`/`xl`/`xxl` + semantic `contentPadding`/`cardPadding`/`sectionGap`/`dialogPadding`.
- **Accessibility** — **Planned**. Define content-description guidelines, dynamic font scaling (read OS settings via `PlatformDensity`), semantics for `Ds*` components, minimum touch-target enforcement.
- **Preview tooling** — `preview/DsPreviewScaffold` and `preview/rememberPreviewImageLoader` for hosting `@Preview` composables that render with `DsTheme` and a fake image loader. Kompass also ships `kompass/preview/NavigationPreview.kt`.

### Platform
Module: `framework/core/designsystem/platform` and `framework/core/domain` (package `com.tekmoon.designsystem.platform`, `com.tekmoon.domain`)

Cross-cutting expect/actual abstraction so the rest of the framework stays in `commonMain`.

- **Platform info** — `domain/Platform.kt` (+ `Platform.android.kt`, `Platform.ios.kt`, `Platform.jvm.kt`).
- **PlatformContext** — `designsystem/platform/PlatformContext.kt`. Abstraction over Android `Context` / iOS view controller / JVM frame.
- **PlatformDensity** — `designsystem/platform/PlatformDensity.kt`. OS-level density and font-scale.
- **PlatformLocals** — `designsystem/platform/PlatformLocals.kt`. CompositionLocals bridging native APIs.
- **PlatformCursor / SystemUiEffect** — `designsystem/platform/PlatformCursor.kt`, `designsystem/platform/SystemUiEffect.kt`. Desktop cursor + system bars colors.
- **PlatformServices / PlatformServicesProvider** — `designsystem/platform/PlatformServices.kt`, `PlatformServicesProvider.kt`. Service-locator surface for things only the host platform can provide.

### Utilities
Module: `framework/core/utils` (package `com.tekmoon.utilities`)

- **Date / Time Utils** — **Planned**. Use `kotlinx-datetime`. Provide `Instant.format(locale, pattern)`, `LocalDate.relative(now)`, time-zone-safe `now()` via `Clock.System` (overridable in tests).
- **Screen Size** — currently in `framework/core/presentation/.../DeviceScreenConfiguration.kt`. Stays in Presentation (see below); kept here only as a topic pointer.
- **DispatcherProvider** — `DispatcherProvider.kt`. Coroutine dispatcher abstraction so tests can inject `UnconfinedTestDispatcher`. Inject into ViewModels and repositories.
- **Random** — `Random.kt` (+ platform actuals). Seedable / deterministic random for tests.

### Domain
Module: `framework/core/domain` (package `com.tekmoon.domain`)

- **UiText** — implemented in design system (`com.tekmoon.designsystem.ui.UiText`) as a `sealed class` with `DynamicString`, `Annotated`, `StringRes`, `StringResArgs`, `QuantityStringRes`. Consider re-exposing from `domain` for non-Compose layers.
- **Result** — `util/data/Result.kt`: `sealed interface Result<DATA, E : Error>` with `Success` / `Failure` plus `map`, `onSuccess`, `onFailure` operators.
- **Resource** — **Planned**. Decide whether to keep `Result` as the single primitive or add a `Resource` (Loading/Success/Error) wrapper for UI state.
- **Pagination** — **Planned**. Define `PagedSource<T>`, `PagingState<T>`, and a `paginate(flow)` operator on top of `Result`.
- **Error Handling** — `util/data/Error.kt` (marker) + `util/data/DataError.kt` with `DataError.Remote` (`BAD_REQUEST`, `REQUEST_TIMEOUT`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `TOO_MANY_REQUESTS`, `NO_INTERNET`, `PAYLOAD_TOO_LARGE`, `SERVER_ERROR`, `SERVICE_UNAVAILABLE`, `SERIALIZATION`, `UNKNOWN`) and `DataError.Local` (`DISK_FULL`, `NOT_FOUND`, `UNKNOWN`).
- **Forms / Validation** — **Planned**. `Validator<T>` interface + composable `rememberFieldState(...)`; common rules (required, email, regex, min/max length).
- Also shipped: `util/CollectEvents.kt`, `util/Random.kt`, `Platform.kt`.

> ViewModel base lives in **Presentation**, not Domain — see next section.

### Presentation
Module: `framework/core/presentation` (package `com.tekmoon.presentation`)

The presentation layer hosts the MVI base class, app-shell composables, and shared modifier helpers. Today it contains `CoreBottomNavBar`, `DeviceScreenConfiguration`, and `ModifierExt`. The `CommonViewModel` below is **Ported (cleanup needed)** from UnifiedCantina.

- **CommonViewModel** — base class for all framework ViewModels. **Ported (cleanup needed)** from `com.cantina.features.bots.builder.core.domain.CommonViewModel`.

  Public API (target shape after port):
  ```kotlin
  abstract class CommonViewModel<Action : Any, Event : Any, State : Any>(
      sharingStarted: SharingStarted = SharingStarted.Eagerly,
      replayEvents: Int = 0,
      extraBufferCapacity: Int = 64,
      onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
      private val dispatchers: DispatcherProvider,   // injected, replaces direct Dispatchers.IO
      private val logger: ShowMeLoggerK,             // injected, replaces Kermit
  ) : ViewModel() {
      val state: StateFlow<State>
      val uiEvents: SharedFlow<Event>
      protected abstract fun initialState(): State
      protected open suspend fun setup() {}
      abstract fun onAction(action: Action)
      protected fun emit(event: Event)
      protected fun updateState(reducer: (State) -> State)
      protected fun withState(block: (State) -> Unit)
      protected inline fun launch(name: String? = null, block: suspend CoroutineScope.() -> Unit)
      protected inline fun launchIO(name: String? = null, block: suspend () -> Unit)
      protected fun <T> withCatching(block: () -> T)
  }
  ```

  Behaviors carried over:
  - `state` is `MutableStateFlow(initialState())` exposed read-only; `onStart { setup() }` runs once when collection begins; `stateIn(scope, sharingStarted, _state.value)`.
  - `uiEvents` is a `MutableSharedFlow` with configurable replay / buffer / overflow.
  - Scope is `viewModelScope + CoroutineExceptionHandler { Logger.e(...) } + CoroutineName(this::class.simpleName)`.
  - `launchIO` switches to the IO dispatcher; `launch` stays on the default.

  Cleanup needed when porting:
  - Replace `co.touchlab.kermit.Logger` calls with the injected `ShowMeLoggerK`.
  - Replace `com.airtime.primus.platform.DispatcherIO` with `dispatchers.io` from the injected `DispatcherProvider`.
  - Visibility: `public`. The class is consumer API.

- **CoreBottomNavBar** — `components/CoreBottomNavBar.kt`. App-shell bottom navigation built on `DsTheme` primitives. Composes `DsImage` + `DsText` + theme tokens; not a Ds primitive itself.
- **Screen configuration** — `DeviceScreenConfiguration.kt`. Exposes width/height/density buckets used for adaptive layouts.
- **Shared Modifier extensions** — `ModifierExt.kt` (e.g. `clickableWithoutIndication`).

### Data
Module: `framework/core/data` (package `com.tekmoon.data`)

- **Network Handlers** — `networking/HttpClientExt.kt` exposes `expect suspend fun platformSafeCall(...)` and typed extension functions `HttpClient.get/post/put/delete` that return `Result<T, DataError.Remote>`.
- **Repository calls** — **Planned**. Repository base interfaces/abstract classes still to be defined here.
- **Ktor client setup** — `networking/HttpClientFactory.kt`: `ContentNegotiation(json)`, `HttpTimeout` (20s request + socket), `Logging`, `WebSockets`, `defaultRequest { header(...) ; contentType(Json) }`.
- **Http interceptors** — `defaultRequest` and `Logging` plugins inside `HttpClientFactory`. Add bespoke interceptors there (auth header injection, retry).
- **JSON serializers** — `kotlinx.serialization` `Json { ignoreUnknownKeys = true }` wired in `HttpClientFactory`.
- **Base DTOs** — **Planned**. No shared DTO base in `data/` yet. Suggested: `ApiResponse<T>`, `Envelope<T>`, error DTOs aligned with `DataError.Remote`.
- **SQLDelight schema** — **Planned**. SQLDelight not yet wired in `core/data`.
- **Caches** — **Planned**. In-memory LRU + persistent cache contracts.
- **Shared queries** — **Planned**. Once SQLDelight is in, shared `.sq` files for common entities (User, Session).
- **WebSockets / Realtime** — `WebSockets` plugin already installed in `HttpClientFactory`; lacks a typed client wrapper. **Planned**: `expect class RealtimeClient { fun connect(url): Flow<Message> }`.
- **Storage** — **Planned**. Files (downloads, attachments), secure storage (Keystore / Keychain, used by Auth.Cryptography), preferences (multiplatform-settings or DataStore-equivalent).
- Also shipped: `networking/UrlConstants.kt`, `logging/KermitLogger.kt` (internal Kermit adapter — kept only as a bridge for Ktor `Logging`; not a public logging API).

### Session
Module: `framework/core/session` (**new module — Planned to create**). Package `com.tekmoon.session`.

**Ported (cleanup needed)** from UnifiedCantina `com.cantina.features.bots.builder.core.sessionStateManager`. This is a generic primitive — Cantina put it inside a feature package, but in the framework it is its own module.

Manages the lifecycle of a stateful editing session for a single entity, combining four sources (local + remote + draft + savedState) into a single `StateFlow`.

Public API:

- **`interface DataSession<Model : Any>`** — `DataSession.kt`
  - `val state: StateFlow<DataSessionState<Model>>`
  - `fun attach(source: DataSessionSource<Model>)`
  - `fun refresh(policy: DataSessionLoadPolicy? = null)`
  - `fun updateDraft(reducer: (Model) -> Model)`
  - `fun updateInitialAndDraft(reducer: (initial: Model, draft: Model) -> Pair<Model, Model>)`
  - `fun commit(model: Model)` / `fun commit(reducer: (Model) -> Model)`
  - `fun syncRemoteChange(reducer: (initial: Model, draft: Model) -> Pair<Model, Model>)`
  - `fun discardDraft()`
  - `suspend fun clearSession(clearSavedState: Boolean = false)`
  - `fun <T : Any> savedStateFlow(key: DataSessionValueKey<T>): Flow<T>`
  - `fun <T : Any> updateSavedState(key: DataSessionValueKey<T>, reducer: (T) -> T)`

- **`class DataSessionImpl<Model>`** — default implementation. Uses `combine(draftFlow, localFlow, remoteFlow, savedStateFlow)` plus `mutationMutex` + a CONFLATED refresh channel. Conflict resolution is delegated to the source via `resolveConflict(local, remote)`.

- **`data class DataSessionState<Model>`** — `sessionId`, `entityId`, `isAttached`, `initial`, `resolved`, `resolvedSource`, `draft`, `local`, `remote`, `savedState`, `hasLoadedDraft`/`Local`/`Remote`, `hasChanges`, `isRefreshing`, `isInitialized`. Plus `enum class DataSessionResolvedSource { None, Draft, Local, Remote }`.

- **`interface DataSessionSource<Model>`** — feature-provided contract: `sessionId`, `entityId`, `loadPolicy`, `localFlow()`, `remoteFlow()`, `createInitialBaseline(...)`, `resolveConflict(local, remote)`, `hasChanges(initial, current)`, `saveLocal(model)`.

- **`enum class DataSessionLoadPolicy`** — `LocalThenRemote`, `RemoteThenLocal`, `LocalOnly`, `RemoteOnly`, `SessionOnly`.

- **Stores** — `DataSessionStores.kt`:
  - `interface DraftStore<Model>` — `observe(sessionId)`, `save(...)`, `clear(...)`.
  - `interface SavedStateStore` — same shape but values are `Map<String, String>`.
  - `class DataSavedStateHandleStore(savedStateHandle, keyPrefix)` — Android impl backed by `SavedStateHandle`.
  - `class InMemoryDraftStore<Model>` — for tests / prototypes.

- **`DataSessionValueKey<T>`** — typed key for `savedStateFlow` / `updateSavedState`, with `serializer` + `defaultValue`.

- **`FakeDataSession`** — test double, ported alongside the rest.

Cleanup needed when porting:
- Repackage from `com.cantina.features.bots.builder.core.sessionStateManager` to `com.tekmoon.session`.
- Inject `DispatcherProvider` instead of using `Dispatchers.Main.immediate` directly in the default scope.
- Visibility: `public` for everything in the table above (`DataSession`, `DataSessionImpl`, `DataSessionState`, `DataSessionResolvedSource`, `DataSessionSource`, `DataSessionLoadPolicy`, `DraftStore`, `SavedStateStore`, `DataSavedStateHandleStore`, `InMemoryDraftStore`, `DataSessionValueKey`). Only the private helpers (`RefreshRequest`, `RemoteSnapshot`, `resolveResolvedSource`) stay private.
- Keep the dependency on `androidx.lifecycle.SavedStateHandle` confined to `DataSavedStateHandleStore` — consumers without it can use `InMemoryDraftStore` or provide their own `SavedStateStore`.

### Navigation
Module: `framework/kompass` (package `com.tekmoon.kompass`) — custom Redux-pattern navigation library.

- **Common nav graph** — `NavigationGraph.kt` (`Destination` interface with stable `id`), `TypedDestination.kt` (typed args via `KSerializer`), `NavigationController.kt` (`NavController` facade), `NavigationHandler.kt` (reducer), `NavigationState.kt` (back stack), `NavigationHost.kt` (Composable host).
- **Deep links** — `DeepLinkHandler.kt`. Resolves URIs at runtime; controller exposes them via the `deepLinkHandlers` constructor parameter.
- **Transitions** — `NavigationTransition.kt`. Pluggable enter/exit animations per navigation event.
- **Scenes** — `SceneLayout.kt`. Multi-pane / scene-based layouts on top of the back stack.
- **Scopes** — `NavigationScope.kt`. `defaultScope()` for singletons (shared across entries), `newScope()` for fresh per-screen lifecycle; `rememberScoped<T>()` replaces raw ViewModel.
- **Back handling** — `PlatformBackHandler.kt` + `util/BackPressedChannel.kt`. Platform back gesture/button routed through the same reducer.
- Destination IDs follow `"kompass/<feature>/<name>"`. Args are `@Serializable` data classes encoded via `Json.encodeToString`.

### UI components
Module: `framework/core/designsystem/components` — Compose primitives that consume `DsTheme`. **Must stay agnostic** — no permissions, no media access, no platform behaviors here.

Shipped today: `DsText.kt`, `DsButton.kt`, `DsCard.kt`, `DsTextField.kt` (Edit Text), `DsDialog.kt`, `DsAlert.kt`, `DsCircularProgress.kt`, `DsClickableText.kt`, `DsLinkText.kt`, `DsSelectableText.kt`. Image/shimmer in `image/DsImage.kt`, `image/DsShimmer.kt`.

- **Image loading** — `image/DsImage.kt`, `image/DsShimmer.kt`, `LocalDsImageLoader`. Async image with shimmer placeholder, swappable loader (real on app, fake in previews). This is the UI side; raw bitmap manipulation (load/encode/compress/dominant color) lives in **Core Utils → Media → Image processing**.
- **Banners / Toasts** — **Planned**.
- **Widgets / Graphs** — **Planned**.
- App-shell composables (`CoreBottomNavBar`, screen configuration) live in **Presentation**, not here.

### Logging
Module: `framework/logger` (engine) + planned writer sub-modules. Public API: `ShowMeLoggerK`.

- **Engine** — `framework/logger`. `ShowMeLoggerK` exposes `v/d/i/w/e/a(message, onLog)` and routes through `domain/LogWriter.kt` (`isLoggable(severity) → processLog(message, severity)`). Configuration via `domain/LoggerConfig.kt` (`DefaultLoggerConfig`, `TestLoggerConfig`), `domain/Severity.kt` (`Verbose`/`Debug`/`Info`/`Warn`/`Error`/`Assert`), `domain/LogFlavor.kt`, `domain/LogAdditionalInfo.kt`. The engine has no file or network dependencies.
- **println / logcat writer** — built into the engine: `data/CommonLogWriter.kt`, `androidMain/AndroidLogcatWriter.kt`, `data/EmptyCommonLogWriter.kt` (for tests). Platform configs `AndroidLogConfig`, `IosLogConfig`, `DesktopLogConfig`.
- **Local file writer** — **Planned** sub-module `framework/logger-file`. Implements `LogWriter` writing to a rolling log file in the platform cache directory. Kept in its own module so the engine stays free of file-I/O dependencies.
- **Remote log writer** — **Planned** sub-module `framework/logger-remote`. Implements `LogWriter` that batches + ships logs to a configured endpoint (uses `framework/core/data`'s `HttpClient`). Also kept separate so the engine doesn't pull in networking.

> Cross-cutting rule: framework code logs through `ShowMeLoggerK` only. The Kermit adapter in `core/data/logging/KermitLogger.kt` exists strictly as a bridge for Ktor `Logging` plugin output.

### Localization / i18n
Module: `framework/core/designsystem` (uses Compose Multiplatform Resources)

- **Strings + plurals** — Compose Resources `StringResource` / `PluralStringResource`, consumed through `UiText.StringRes`, `UiText.StringResArgs`, `UiText.QuantityStringRes` (`designsystem/ui/UiText.kt`); see `designsystem/ui/StringResSample.kt` for the pattern.
- **Locale fallback** — **Planned**. Document the supported-languages list and what happens for unsupported locales.
- **Locale-aware formatting** — **Planned**. Coordinate with Utilities → Date / Time Utils for date, number, and currency formatting.

### SDK / Public API
Module: `framework/sdk` (package `com.tekmoon.framework`)

- **Umbrella module** — `Framework.kt` (`object Framework { const val VERSION = "0.0.1" }`). The Gradle module re-exports `core:designsystem`, `core:data`, `core:domain`, `core:presentation`, `kompass`, and `logger` via `api(...)` so consumers depend on a single artifact.
- **Public API surface** — Today only `Framework.VERSION` is exposed at runtime. **Planned**: add `Framework.start(config: FrameworkConfig.Init)` that initializes config + logger + HTTP factory in one call.

## Core Utils

Core Utils sits **above** Core and may depend on Core (including UI components). This is where permission flows + their disclaimer dialogs live, where media access is wrapped with picker dialogs, etc.

### Permissions
Module: `framework/core/permissions` (**new module — Planned to create**). Package `com.tekmoon.permissions`.

**Ported (cleanup needed)** from UnifiedCantina `com.cantina.features.common.permissions`. The Cantina implementation is complete and production-ready; the port is mostly visibility changes.

Public API (target shape after port):

- **`enum class PermissionType`** — `CAMERA`, `RECORD_AUDIO`, `LOCATION`, `LOCATION_ALWAYS`, `GALLERY`, `WRITE_STORAGE`, `READ_STORAGE`, `NOTIFICATION`, `CONTACTS`, `CALENDAR`, `BLUETOOTH`.
- **`enum class PermissionState`** — `NOT_DETERMINED`, `GRANTED`, `DENIED`, `DENIED_ALWAYS`.
- **`data class PermissionStatus(permission, state, isRequired, isAlreadyRequested)`**.
- **`data class PermissionsUiState(permissions, allRequiredGranted, isLoading, isRequestingPermissions, updateTrigger)`** with helpers `hasPendingRequiredPermissions()`, `hasPermissionDeniedAlways()`.
- **`interface PermissionsController`**
  - `suspend fun getPermissionState(permission: PermissionType): PermissionState`
  - `suspend fun requestPermission(permission: PermissionType): PermissionState`
  - `fun openAppSettings()`
- **`@Composable expect fun rememberPermissionsController(): PermissionsController`**
- **`class PermissionsViewModel(controller, requiredPermissions)`** — sequential request flow, second-pass-routes-to-settings logic, awaitingSettingsReturn handling, emits `PermissionsEvent.Granted` / `Denied` / `OpenedSettings`. Currently extends `ViewModel` directly; on port make it extend the framework `CommonViewModel` so it picks up the same scope / logging / dispatcher contract.
- **`@Composable expect fun PermissionsScreen(requiredPermissions, onPermissionsDenied, onAllPermissionsGranted, content)`** — composable wrapper that creates the controller + VM with a stable key per permission set and routes events.
- **`PermissionDisclaimerDialog`** — built on `DsDialog` / `DsButton` / `DsText`. Lives here (not in `designsystem/components`) because it combines a UI primitive with a platform behavior. Use case: rationale shown before calling `requestPermission`.

Android implementation notes (`PlatformPermissions.android.kt`):
- `AndroidPermissionsController(activity, lifecycleOwner, launcher)` uses `ContextCompat.checkSelfPermission` + `ActivityCompat.shouldShowRequestPermissionRationale` to map to `PermissionState`. Pre-API-33 `NOTIFICATION` returns `GRANTED` (no runtime permission).
- `PermissionLauncher` wraps `ActivityResultContracts.RequestMultiplePermissions` over a `Channel` so sequential requests work; both controller and launcher `awaitResumed()` before launching to avoid lifecycle races.
- `rememberPermissionsController()` walks the `Context → Activity` chain and throws a descriptive `IllegalStateException` if invoked outside an Activity-backed composition.

Cleanup needed when porting:
- Repackage from `com.cantina.features.common.permissions` to `com.tekmoon.permissions`.
- Flip `internal` → `public` on every type listed above (everything in the public API table).
- Replace direct `Logger` calls in `PermissionsViewModel` with `ShowMeLoggerK`.
- Make `PermissionsViewModel` extend `CommonViewModel<Action, Event, State>` (current implementation predates that base).

### Connectivity
Module: `framework/core/connectivity` (**new module — Planned to create**). Package `com.tekmoon.connectivity`. **Planned** — nothing in Cantina to port directly.

- **Internet Connectivity check** — observable state, not one-shot.
- Suggested API: `expect class ConnectivityObserver { val status: Flow<ConnectionStatus> }` with `Available` / `Unavailable` / `Losing` / `Lost`.
- Android: `ConnectivityManager.NetworkCallback`. iOS: `NWPathMonitor`. Desktop: `InetAddress.isReachable` polling fallback.

### Media
Module: `framework/core/media` (**new module — Planned to create**). Package `com.tekmoon.media`.

**Ported (cleanup needed)** from UnifiedCantina `com.cantina.features.common.{mediapicker, camera, bitmap}`. Aggregates three closely related sub-topics behind a single module.

#### Media → MediaPicker
Public API (target shape after port):

- **`enum class MediaPickerType`** — `Image`, `Video`.
- **`data class PickedMediaData(uri, bytes, mimeType, fileName, sizeBytes, width, height, durationMs)`** — equality / hash already implemented over content (not reference).
- **`@Composable expect fun rememberMediaPickerLauncher(mediaPickerTypes, selectionLimit = 1, returnBytes = true, maxBytesToReturn = 8_000_000L, onCancelled, onResult): MediaPickerLauncher`**
- **`class MediaPickerLauncher(onLaunch: (requestKey: String?) -> Unit)`** with `fun launch(requestKey: String? = null)`.

Android implementation notes (`PlatformMediaPickerLauncher.android.kt`):
- Single-pick uses `ActivityResultContracts.PickVisualMedia`; multi-pick uses `PickMultipleVisualMedia(maxItems = selectionLimit)`.
- `pickMedia(...)` runs on `Dispatchers.IO` (will become `dispatchers.io` after cleanup), reads bytes only when `size <= maxBytesToReturn`, extracts dimensions via `BitmapFactory.Options(inJustDecodeBounds = true)` for images and `MediaMetadataRetriever` for video duration / dimensions.
- A `pendingRequestKey` captured per launch lets callers correlate the async callback.

#### Media → Camera
Public API (target shape after port):

- **`enum class CameraType`** — `Front`, `Back`.
- **`enum class CameraMode`** — `Photo`, `Video`.
- **`@Composable expect fun rememberCameraLauncher(cameraType = Back, cameraMode = Photo, returnBytes = true, maxBytesToReturn = 10_000_000L, onResult): CameraLauncher`**
- **`class CameraLauncher(onLaunch: (requestKey: String?, fileProviderAuthority: String) -> Unit)`** with `fun launch(requestKey: String? = null, fileProviderAuthority: String)`.

Android implementation notes (`PlatformCamera.android.kt`):
- Uses `TakePictureWithFacing` (custom `ActivityResultContract` extending `ACTION_IMAGE_CAPTURE` with best-effort front-camera extras) for photos and `ActivityResultContracts.CaptureVideo()` for video.
- Temp files in `cacheDir/camera_images` / `cacheDir/camera_videos`; URIs through `FileProvider.getUriForFile(context, fileProviderAuthority, file)` — authority is supplied by the consumer at `launch()` time so the framework doesn't bake in a value.
- After capture, `processMediaFile(...)` runs `normalizeJpegOrientation(file, unmirrorFront = isFront)` to fix EXIF rotation and un-mirror selfies, then extracts dimensions / duration.
- `tempFilePath` / `tempUriStr` survive process death via `rememberSaveable`.

`normalizeJpegOrientation` is an **internal** helper (stays internal after port) — applies EXIF rotation/flip via `Matrix` and resets `TAG_ORIENTATION` to `ORIENTATION_NORMAL` so downstream viewers don't double-rotate.

#### Media → Image processing
Public API (target shape after port):

- **`expect suspend fun loadImageBitmap(source: ImageSource, context: Any?): ImageBitmap?`** — `ImageSource.Url` / `ImageSource.Bytes` / `ImageSource.PlatformUri`.
- **`expect fun encodeImageBitmapToPng(image: ImageBitmap): ByteArray`**
- **`expect suspend fun getDominantColorFromUrl(url: String, context: PlatformContext): Color`**
- **`expect suspend fun getDominantColorFromBitmap(bitmap: ImageBitmap, context: PlatformContext): Color`**
- **`expect fun compressImageToJpeg(bytes: ByteArray, maxDimension: Int = 1080, quality: Int = 85): ByteArray`**

Android implementation notes (`PlatformImageBitmap.android.kt`):
- `BitmapFactory.decodeByteArray` / `decodeStream`; calls `prepareToDraw()` on IO so the GPU upload happens off the main thread.
- `getDominantColor*` uses `androidx.palette.graphics.Palette` with a 24-color cap and falls back through dominant → darkVibrant → darkMuted → muted (darkened) → solid black.
- `compressImageToJpeg` scales so the longest edge is `maxDimension` then compresses at `quality`.

Cleanup needed when porting (all three sub-topics):
- Repackage from `com.cantina.features.common.{mediapicker, camera, bitmap}` to `com.tekmoon.media.{picker, camera, bitmap}`.
- Flip `internal` → `public` on consumer-facing types (`PickedMediaData`, `MediaPickerType`, `CameraType`, `CameraMode`, `MediaPickerLauncher`, `CameraLauncher`, `rememberMediaPickerLauncher`, `rememberCameraLauncher`, and all `loadImageBitmap` / `encode...` / `compress...` / `getDominantColor...` declarations). Helpers like `normalizeJpegOrientation`, `pickMedia`, `processMediaFile`, `TakePictureWithFacing` stay `internal`.
- Replace direct `Dispatchers.IO` / `Dispatchers.Default` with `dispatchers.io` / `dispatchers.default` from an injected `DispatcherProvider`.
- Replace `e.printStackTrace()` calls with `ShowMeLoggerK.e(...)`.
- `PlatformContext` is already a framework type — re-point Android `import com.cantina.PlatformContext` → `com.tekmoon.designsystem.platform.PlatformContext`.
- `ImageSource` currently lives in `com.cantina.features.common.imagecropper`; move to `com.tekmoon.media.bitmap` (or keep alongside an eventual `image-cropper` Core Feature).

## Core Features

> **Status:** This category is **planned**. Each feature should live in its own module under `framework/feature/<name>` and be wired through `framework/sdk` (`Framework.kt`).

### Auth
- **Token manager** — store/refresh access + refresh tokens; expose `getValidAccessToken(): String?`.
- **Refresh strategy** — Ktor `Auth` plugin or custom interceptor in `HttpClientFactory`; single-flight refresh + retry on 401.
- **AuthRepository** — login / logout / register; returns `Result<User, DataError.Remote>`.
- **SessionManager** — observable session state (`Flow<SessionState>`), forced sign-out on refresh failure.
- **Cryptography / secure storage** — Android: EncryptedSharedPreferences / Keystore. iOS: Keychain. Desktop: OS keychain (Secret Service / Keychain). Shared `expect class SecureStore { fun put/get/remove(key) }`.

### Analytics
- **User events** — `expect class AnalyticsClient { fun track(event: String, params: Map<String, Any?>) }`. Pluggable adapters (Firebase, Mixpanel, Amplitude).
- **A/B test** — variant assignment with a stable user-bucket key; pairs with Feature Flags below for the flag-evaluation engine.

### Device
- **Device OS version** — `expect val deviceInfo: DeviceInfo` exposing `osName`, `osVersion`, `model`, `manufacturer`, `appVersion`.
- **WiFi, IP address** — Android: `WifiManager` / `ConnectivityManager`. iOS: `CNCopyCurrentNetworkInfo` (entitlement required) + `getifaddrs`. Pair with the Permissions flow above where the platform demands it.
- **Battery / locale / time zone / density** — battery level + charging state, system locale, default time zone, screen density. Some already in `DeviceScreenConfiguration`.

### Push Notifications
- **Token management** — `expect class PushTokenProvider { suspend fun token(): String? }`. Android: FCM (`FirebaseMessaging.getToken`). iOS: APNs registration via `UNUserNotificationCenter`.
- **Foreground / background handlers** — common `PushMessage` model + `PushReceiver` interface; platform actuals translate `RemoteMessage` (Android) / `UNNotification` (iOS).
- **Topic subscriptions** — `suspend fun subscribe(topic: String)` / `unsubscribe`.
- Integrates with **Permissions → Notification** for runtime permission and with the Analytics adapter for delivery / open events.

### Crash Reporting
- **Adapter** — `expect class CrashReporter { fun recordException(throwable: Throwable, extras: Map<String, Any?> = emptyMap()); fun setUserId(id: String?) }`. Default impls: Crashlytics, Sentry.
- **Non-fatal reporting** — explicit `recordException` path, separate from process crashes.
- **Breadcrumbs** — funnel `ShowMeLoggerK` `Error`/`Warn` severities into the crash reporter as breadcrumbs (likely implemented as a `LogWriter` adapter, similar to the planned file / remote writers).
- Bootstraps in `Framework.start(...)` once `FrameworkConfig` is initialized.

### Feature Flags / Remote Config
- **Flag evaluation** — `expect class FeatureFlagClient { fun <T> get(key: String, default: T): T; fun observe(key: String): Flow<T> }`.
- **Remote sync** — periodic fetch with backoff; force-refresh on cold start. Providers: Firebase Remote Config, ConfigCat, Statsig, GrowthBook.
- **Default values** — typed defaults registered at startup so the app always has a fallback even before first sync.
- Distinct from Analytics' A/B test, which is purely about variant assignment + reporting.

### Background Work
- **Abstraction** — `expect class BackgroundScheduler { fun schedule(task: BackgroundTask) }`. Android: WorkManager. iOS: `BGTaskScheduler`. Desktop: coroutine scope + persisted queue.
- **Periodic + one-shot** — share a `BackgroundTask(id, periodic: Boolean, interval, constraints, payload)` model.
- **Constraints** — network type (any / unmetered), charging, idle, storage-not-low; map to the platform-native constraint model.

### App Update / In-app Review
- **Force / soft update** — read remote-config flag (`min_supported_version`, `recommended_version`); show blocking dialog for force, soft banner for recommended.
- **Review prompt** — Android: Google Play In-App Review. iOS: `SKStoreReviewController.requestReview()`. Show after a "happy-moment" event with throttling (max once per N days).

## Module map

### Existing
```
framework/
├── core/
│   ├── designsystem   → System Design + UI components + UiText + Platform locals + i18n via Compose Resources
│   ├── utils          → Utilities (DispatcherProvider, Random)
│   ├── data           → Data + Configuration (FrameworkConfig, Ktor, WebSockets)
│   ├── domain         → Domain (Result, DataError, Platform)
│   └── presentation   → Presentation (CoreBottomNavBar, DeviceScreenConfiguration, ModifierExt)
├── kompass            → Navigation (graph, deep links, transitions, scenes, scopes, back)
├── logger             → Logging engine (ShowMeLoggerK)
└── sdk                → Umbrella module re-exporting the above
```

### Planned (new modules)
```
framework/
├── core/
│   ├── session         → Session (DataSession + stores + policies)    -- port from Cantina
│   ├── permissions     → Core Utils → Permissions                     -- port from Cantina
│   ├── connectivity    → Core Utils → Connectivity                    -- new
│   ├── media           → Core Utils → Media (picker + camera + bitmap)-- port from Cantina
│   └── storage         → Data → Storage (files + secure + prefs)      -- new
├── logger-file         → Logging → local file writer                  -- new
├── logger-remote       → Logging → remote log writer                  -- new
└── feature/
    ├── auth            → Auth                                         -- new
    ├── analytics       → Analytics                                    -- new
    ├── device          → Device                                       -- new
    ├── push            → Push Notifications                           -- new
    ├── crash           → Crash Reporting                              -- new
    ├── feature-flags   → Feature Flags / Remote Config                -- new
    ├── background-work → Background Work                              -- new
    └── app-update      → App Update / In-app Review                   -- new
```

### Presentation also picks up CommonViewModel
Existing `framework/core/presentation` will gain `CommonViewModel<Action, Event, State>` (ported from Cantina). No new module needed for it.

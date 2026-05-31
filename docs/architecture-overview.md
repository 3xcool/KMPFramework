# Architecture — Overview

High-level topic map of the Tekmoon KMP Framework. For specs and the modules backing each topic, see [architecture-details.md](architecture-details.md).

## Layering

```
Core Features   (top)     ← features built on top of everything below
Core Utils      (middle)  ← may depend on Core (including UI components)
Core            (bottom)  ← foundation, must stay agnostic
```

UI components live in **Core** and must stay agnostic. Anything that combines a UI primitive with a platform behavior (e.g. a permission rationale dialog) lives in **Core Utils**, not in UI components.

## Core

- **Configuration**
  - FrameworkConfig (apiBaseUrl, apiKey, environment, debug, isPro, adEnabled)
  - Bootstrap / init flow
- **System Design**
  - Colors
  - Fonts
  - Theming
  - Dimens
  - Spacing
  - Accessibility
  - Preview tooling
- **Platform**
  - expect / actual abstraction
  - PlatformContext / PlatformDensity / PlatformLocals
  - PlatformServices / PlatformServicesProvider
- **Utilities**
  - Date / Time Utils (timezone, locale-aware formatting)
  - Screen Size
  - DispatcherProvider (coroutines / threading)
  - Random
- **Domain**
  - UiText
  - Result
  - Resource
  - Pagination
  - Error Handling
  - Forms / Validation
- **Presentation**
  - CommonViewModel (MVI base: Action / Event / State)
  - CoreBottomNavBar
  - Screen configuration (DeviceScreenConfiguration)
  - Shared Modifier extensions
- **Data**
  - Network Handlers
  - Repository calls
  - Ktor client setup
  - Http interceptors
  - JSON serializers
  - Base DTOs
  - SQLDelight schema
  - Caches
  - Shared queries
  - WebSockets / Realtime
  - Storage (files, secure storage, preferences)
- **Session**
  - DataSession (local + remote + draft + savedState → single StateFlow)
  - DataSessionSource (feature-provided contract)
  - Load policies (LocalThenRemote / RemoteThenLocal / LocalOnly / RemoteOnly / SessionOnly)
  - DraftStore / SavedStateStore (incl. SavedStateHandle-backed impl)
  - Conflict resolution + change tracking
- **Navigation**
  - Common nav graph
  - Deep links
  - Transitions
  - Scenes
  - Scopes (default / new)
  - Back handling
- **UI components** (agnostic — no platform behaviors here)
  - Buttons
  - Cards
  - Edit Text
  - Dialogs
  - Banners
  - Toasts
  - Alerts
  - Widgets
  - Graphs...
  - Image loading (DsImage, shimmer, cache)
- **Logging**
  - Engine: ShowMeLoggerK + LogWriter contract + Severity
  - println / logcat writer (built-in)
  - Local file writer (sub-module `logger-file`)
  - Remote log writer (sub-module `logger-remote`)
- **Localization / i18n**
  - Strings + plurals (Compose Resources)
  - Locale fallback
  - Locale-aware formatting
- **SDK / Public API**
  - Umbrella module re-exports
  - Framework.VERSION
  - Framework.start(...)

## Core Utils

- **Permissions**
  - Notification
  - Gallery
  - Camera / Location / Contacts / Calendar / Bluetooth / ...
  - Permission disclaimer dialog (uses UI components)
  - PermissionsScreen + PermissionsViewModel
- **Connectivity**
  - Internet connectivity check (observable)
- **Media**
  - MediaPicker (image / video, single / multi)
  - Camera (photo / video, front / back, EXIF normalization)
  - Image processing (load, encode, compress, dominant color)

## Core Features

- **Auth**
  - Token manager
  - Refresh strategy
  - AuthRepository
  - SessionManager
  - Cryptography / secure storage
- **Analytics**
  - User events
  - A/B test
- **Device**
  - Device OS version
  - WiFi, IP address
  - Battery / locale / time zone / density
- **Push Notifications**
  - FCM / APNs token management
  - Foreground / background handlers
  - Topic subscriptions
- **Crash Reporting**
  - Crashlytics / Sentry adapter
  - Non-fatal reporting
  - Breadcrumbs (fed by Logging)
- **Feature Flags / Remote Config**
  - Flag evaluation
  - Remote sync
  - Default values
- **Background Work**
  - WorkManager / BGTaskScheduler abstraction
  - Periodic + one-shot tasks
  - Constraints (network, charging)
- **App Update / In-app Review**
  - Force / soft update
  - Review prompt timing

# Localization & i18n

How the Tekmoon KMP framework handles locales, formatting, and translated strings — and where the responsibility splits between the framework and the consuming app.

---

## TL;DR

- The framework ships **no translated strings**. Translation lives in the consuming app's string resources (Android `res/values-*/`, iOS `Localizable.strings`, Compose Multiplatform `composeResources/values-*/`).
- The framework ships **locale-aware formatting helpers** — dates, numbers, currencies — that delegate to each platform's native formatter. Anything the platform's BCP-47 machinery accepts works.
- Locales are passed around as a small `LocaleTag` value class wrapping a BCP-47 tag. `LocaleTag.System` defers to the platform default.
- Fallbacks happen at the platform layer, not in the framework. The framework's only fallback rule: an empty `LocaleTag` becomes the platform default.

---

## What the framework owns vs. what the app owns

| Concern | Owned by |
|---|---|
| `Instant.format`, `Double.formatNumber`, `Double.formatCurrency`, `Long.formatNumber` | Framework (`core/utils`, `com.tekmoon.utilities.format` + `.time`) |
| `LocalDate.relative(now): RelativeTime` (returns a sealed bucket, **not** a localized string) | Framework |
| Mapping `RelativeTime` to localized copy (e.g. `"3 days ago"` vs `"há 3 dias"`) | Consuming app |
| Pluralization rules | Consuming app (Android plurals, iOS `.stringsdict`, etc.) |
| String catalogs and per-locale translation files | Consuming app |
| Default locale at boot | Consuming app / OS — framework does not override |

The framework deliberately stops at "give me formatted numbers and dates for locale X". Translated copy is a product-team concern that varies per app, so it stays out of the framework's surface area.

### Why not ship translated strings?

The framework is a reusable library across multiple apps. Bundling a "framework string catalog" would mean every consuming app inherits — and has to translate — strings it may never use, and the framework would need a release cycle every time a translation is corrected. Letting each app own its catalog avoids that coupling.

---

## `LocaleTag`

```kotlin
package com.tekmoon.utilities.format

data class LocaleTag(val tag: String) {
    companion object {
        val System: LocaleTag = LocaleTag("")
    }
}
```

`tag` is a **BCP-47 language tag** — the same format used by `Locale.forLanguageTag(...)` on Android/JVM and `NSLocale(localeIdentifier:)` on iOS. Examples:

| Tag | Meaning |
|---|---|
| `"en"` | English (region-less; platform picks a regional fallback) |
| `"en-US"` | English, United States |
| `"pt-BR"` | Portuguese, Brazil |
| `"pt-PT"` | Portuguese, Portugal |
| `"ja-JP"` | Japanese, Japan |
| `"zh-Hans-CN"` | Simplified Chinese, mainland China |
| `""` (= `LocaleTag.System`) | Whatever the OS reports as the user's preferred locale at format time |

iOS prefers underscore separators internally (`pt_BR`). The framework's iOS actual transparently rewrites `-` → `_` before handing the tag to `NSLocale`, so callers always use BCP-47 hyphen form.

---

## Supported locales

**Any BCP-47 tag the platform accepts is supported for formatting.**

The framework does not maintain a fixed allowlist. Each platform's formatting backend exposes its own ICU/CLDR data:

| Platform | Backend | Locale data source |
|---|---|---|
| Android | `java.text.NumberFormat`, `java.text.SimpleDateFormat` | Android system ICU |
| JVM (desktop) | same as Android | JDK ICU |
| iOS | `NSNumberFormatter`, `NSDateFormatter` | Apple Foundation / CLDR |

If a tag is malformed, all three platforms degrade to the platform default rather than throwing — this is the formatter behavior, not framework code.

**Practical guidance:** the locales the framework has been exercised against in CI are listed in the test files (`InstantFormatTest`, `NumberFormatTest`). Today: `en-US` and `pt-BR`. Adding more is a matter of writing assertions, not adding code.

---

## Fallback strategy

There are three layers, ordered from cheapest to costliest:

### 1. Framework — empty tag → platform default

```kotlin
1234.56.formatNumber(LocaleTag.System)  // uses OS-reported locale at format time
"2026-05-30T12:34:56Z".toInstant().format("yyyy-MM-dd")  // same, no locale arg
```

`LocaleTag("")` (== `LocaleTag.System`) is the sentinel — the actual implementation passes the platform default into `Locale.forLanguageTag` / `NSLocale.currentLocale` at the formatter level.

### 2. Platform — BCP-47 fallback chain

When a tag like `pt-BR-x-private` doesn't fully match installed data, each platform walks back through region, then script, then language:

```
pt-BR-x-private  →  pt-BR  →  pt  →  root (en-US-equivalent)
```

Android does this via `Locale.forLanguageTag` + ICU. iOS does it via `NSLocale`. The framework does not intervene.

### 3. App — string resource fallbacks

For translated copy (which is **not** the framework's concern), the consuming app uses each platform's native fallback:

- **Android / Compose Multiplatform**: resource qualifier chain — `values-pt-rBR/` → `values-pt/` → `values/` (the default catalog, conventionally English).
- **iOS**: `NSBundle` walks `Localizable.strings` per-language, falling back to the development region declared in `Info.plist`.

The framework's `UiText` type (`com.tekmoon.designsystem.ui.UiText`) is the bridge: it carries either a `StringRes` (resolved against the platform fallback chain) or a `DynamicString` (server-supplied, no fallback). Both surface as `String` via `UiText.asString()` at the composable.

---

## Picking a locale at runtime

The framework has no opinion on **how** the consuming app decides which locale to format with. Three common patterns, in order of how often we see them:

1. **OS default** — pass `LocaleTag.System` (or omit the parameter). Tracks the device-level setting the user already configured. Default for most apps.
2. **User preference** — the app stores a `selectedLocale: LocaleTag?` in `core/storage` `Preferences`; pass it through whenever formatting. Useful for apps where the in-app language shouldn't track the OS.
3. **Server-supplied** — for dynamic content (price tags, marketing strings driven by SDUI), the server includes the BCP-47 tag in its payload. The renderer passes it straight into the formatters.

Pattern (3) is what makes the SDUI module ([roadmap entry](roadmap.md#server-driven-ui-frameworkfeaturesdui)) work cleanly — the server controls the locale of dynamically-rendered content without the framework needing a translation catalog.

---

## What about pluralization?

Pluralization rules vary by locale (English has 2 forms — singular/plural; Polish has 3; Arabic has 6) and the framework does not implement them. Use the platform's native machinery:

- **Android / Compose Multiplatform**: `plurals` resource type with `quantity` qualifiers, surfaced as `UiText.QuantityStringRes(resource, quantity, args)`.
- **iOS**: `.stringsdict` files.

`UiText.QuantityStringRes` is already in `com.tekmoon.designsystem.ui.UiText` and resolves to the right plural form at composition time.

---

## Migrating from `Locale.getDefault()`

If you have older code that imports `java.util.Locale` or `NSLocale` directly, swap to `LocaleTag`:

```kotlin
// Before
val s = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

// After
val s = instant.format("yyyy-MM-dd")  // LocaleTag.System is the default
```

The `LocaleTag` wrapper keeps the call site KMP-compatible and lets you swap to a non-system locale later without touching platform imports.

---

## Reference

- API surface: `com.tekmoon.utilities.format.{LocaleTag, formatNumber, formatCurrency}` and `com.tekmoon.utilities.time.{Instant.format, TimeProvider, RelativeTime, LocalDate.relative}` in `:framework:core:utils`.
- UI bridge: `com.tekmoon.designsystem.ui.UiText` in `:framework:core:designsystem`.
- Tests: `NumberFormatTest`, `InstantFormatTest`, `RelativeTimeTest`, `TimeProviderTest` in `:framework:core:utils` commonTest.

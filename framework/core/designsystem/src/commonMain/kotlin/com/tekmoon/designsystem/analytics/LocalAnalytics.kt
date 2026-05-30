package com.tekmoon.designsystem.analytics

import androidx.compose.runtime.staticCompositionLocalOf
import com.tekmoon.analytics.AnalyticsClient
import com.tekmoon.analytics.NoOpAnalyticsClient

/**
 * Composition-scoped [AnalyticsClient] read by interactive `Ds*` primitives
 * (`DsButton`, `DsIconButton`, `DsClickableText`, `DsLinkText`, …) whenever an `analyticsId`
 * is supplied.
 *
 * Defaults to [NoOpAnalyticsClient] so the design system stays usable in previews, snapshot
 * tests, or apps that haven't wired analytics yet. Provide the real client at the root of
 * your composition (typically inside `DsTheme { ... }` content), feeding it from
 * `Framework.analytics`:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalAnalytics provides Framework.analytics) {
 *     App()
 * }
 * ```
 *
 * Marked `static` because the client identity does not change during a session — composables
 * reading it never need to recompose when the value swaps (production never swaps it).
 */
val LocalAnalytics = staticCompositionLocalOf<AnalyticsClient> { NoOpAnalyticsClient }

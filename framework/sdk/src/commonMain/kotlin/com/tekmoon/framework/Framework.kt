package com.tekmoon.framework

/**
 * Tekmoon KMP Framework umbrella module.
 *
 * This module re-exports all framework sub-modules via api() dependencies:
 * - core:designsystem (DsTheme, DsButton, DsColors, etc.)
 * - core:data (HTTP client, BuildKonfig)
 * - core:domain (Domain models)
 * - core:presentation (CoreBottomNavBar, shared UI)
 * - kompass (Navigation library)
 * - logger (Logging abstraction)
 *
 * Usage in consumer apps:
 * ```
 * implementation("com.tekmoon:framework:1.0.0")
 * ```
 */
object Framework {
    const val VERSION = "0.0.1"
}

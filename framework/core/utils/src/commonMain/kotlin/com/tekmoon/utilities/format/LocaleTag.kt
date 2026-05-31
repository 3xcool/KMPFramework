package com.tekmoon.utilities.format

/**
 * Platform-neutral locale identifier in BCP-47 form (e.g. `"en-US"`, `"pt-BR"`, `"ja-JP"`).
 *
 * An empty [tag] (or [LocaleTag.System]) defers to the platform's default locale at format time.
 *
 * The string is forwarded to each platform's locale machinery — `Locale.forLanguageTag(...)` on
 * Android/JVM, `NSLocale(localeIdentifier = ...)` on iOS — so any BCP-47-valid tag is accepted.
 */
data class LocaleTag(val tag: String) {
    companion object {
        /** Sentinel that defers to the platform's default locale. */
        val System: LocaleTag = LocaleTag("")
    }
}

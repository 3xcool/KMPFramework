package com.tekmoon.utilities.format

/**
 * Formats this [Double] as a locale-aware decimal number.
 *
 * @param locale BCP-47 locale tag; [LocaleTag.System] uses the platform default
 * @param fractionDigits when non-null, fixes both minimum and maximum fraction digits to this
 *   count (e.g. `2` → `"1,234.56"`); when null, the platform default precision applies
 * @param useGrouping whether to insert the locale's grouping separator (default `true`)
 */
expect fun Double.formatNumber(
    locale: LocaleTag = LocaleTag.System,
    fractionDigits: Int? = null,
    useGrouping: Boolean = true,
): String

/**
 * Formats this [Long] as a locale-aware integer.
 *
 * @param locale BCP-47 locale tag; [LocaleTag.System] uses the platform default
 * @param useGrouping whether to insert the locale's grouping separator (default `true`)
 */
expect fun Long.formatNumber(
    locale: LocaleTag = LocaleTag.System,
    useGrouping: Boolean = true,
): String

/**
 * Formats this [Double] as a currency amount using the platform's locale-aware currency formatter.
 *
 * @param currencyCode ISO 4217 currency code (e.g. `"USD"`, `"BRL"`, `"JPY"`); determines the
 *   symbol and the default fraction-digit count
 * @param locale BCP-47 locale tag; controls symbol placement, decimal/grouping separators, and
 *   localized symbol form (e.g. `"R$"` vs `"BRL"`). [LocaleTag.System] uses the platform default.
 */
expect fun Double.formatCurrency(
    currencyCode: String,
    locale: LocaleTag = LocaleTag.System,
): String

package com.tekmoon.utilities.format

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

actual fun Double.formatNumber(
    locale: LocaleTag,
    fractionDigits: Int?,
    useGrouping: Boolean,
): String {
    val formatter = NumberFormat.getInstance(resolveLocale(locale)).apply {
        if (fractionDigits != null) {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
        isGroupingUsed = useGrouping
    }
    return formatter.format(this)
}

actual fun Long.formatNumber(
    locale: LocaleTag,
    useGrouping: Boolean,
): String {
    val formatter = NumberFormat.getInstance(resolveLocale(locale)).apply {
        isGroupingUsed = useGrouping
    }
    return formatter.format(this)
}

actual fun Double.formatCurrency(
    currencyCode: String,
    locale: LocaleTag,
): String {
    val curr = Currency.getInstance(currencyCode)
    val formatter = NumberFormat.getCurrencyInstance(resolveLocale(locale)).apply {
        currency = curr
        // getCurrencyInstance() keeps the LOCALE'S default precision (2 for en-US), not the
        // currency's. Override both so JPY (0), BHD (3), etc. render correctly.
        minimumFractionDigits = curr.defaultFractionDigits
        maximumFractionDigits = curr.defaultFractionDigits
    }
    return formatter.format(this)
}

private fun resolveLocale(locale: LocaleTag): Locale =
    if (locale.tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(locale.tag)

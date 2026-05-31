package com.tekmoon.utilities.format

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.numberWithDouble
import platform.Foundation.numberWithLongLong

actual fun Double.formatNumber(
    locale: LocaleTag,
    fractionDigits: Int?,
    useGrouping: Boolean,
): String {
    val formatter = decimalFormatter(locale, useGrouping).apply {
        if (fractionDigits != null) {
            minimumFractionDigits = fractionDigits.toULong()
            maximumFractionDigits = fractionDigits.toULong()
        }
    }
    return formatter.stringFromNumber(NSNumber.numberWithDouble(this)) ?: this.toString()
}

actual fun Long.formatNumber(
    locale: LocaleTag,
    useGrouping: Boolean,
): String {
    val formatter = decimalFormatter(locale, useGrouping).apply {
        maximumFractionDigits = 0u
    }
    return formatter.stringFromNumber(NSNumber.numberWithLongLong(this)) ?: this.toString()
}

actual fun Double.formatCurrency(
    currencyCode: String,
    locale: LocaleTag,
): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        if (locale.tag.isNotEmpty()) {
            this.locale = NSLocale(localeIdentifier = locale.tag.replace('-', '_'))
        }
        this.currencyCode = currencyCode
    }
    return formatter.stringFromNumber(NSNumber.numberWithDouble(this)) ?: this.toString()
}

private fun decimalFormatter(locale: LocaleTag, useGrouping: Boolean): NSNumberFormatter =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        if (locale.tag.isNotEmpty()) {
            this.locale = NSLocale(localeIdentifier = locale.tag.replace('-', '_'))
        }
        usesGroupingSeparator = useGrouping
    }

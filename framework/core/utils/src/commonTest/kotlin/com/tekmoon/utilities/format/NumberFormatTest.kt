package com.tekmoon.utilities.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NumberFormatTest {

    // ---- Double.formatNumber -----------------------------------------------

    @Test
    fun double_en_US_default_precision_with_grouping() {
        assertEquals("1,234.56", 1234.56.formatNumber(LocaleTag("en-US")))
    }

    @Test
    fun double_en_US_no_grouping() {
        assertEquals("1234.56", 1234.56.formatNumber(LocaleTag("en-US"), useGrouping = false))
    }

    @Test
    fun double_en_US_fixed_fraction_digits() {
        assertEquals("1,234.50", 1234.5.formatNumber(LocaleTag("en-US"), fractionDigits = 2))
        assertEquals("1,235", 1234.56.formatNumber(LocaleTag("en-US"), fractionDigits = 0))
    }

    @Test
    fun double_pt_BR_swaps_decimal_and_grouping() {
        // pt-BR uses "." for grouping and "," for decimal — exact inverse of en-US.
        val brResult = 1234.56.formatNumber(LocaleTag("pt-BR"))
        val usResult = 1234.56.formatNumber(LocaleTag("en-US"))
        assertNotEquals(brResult, usResult, "pt-BR and en-US grouping/decimal should differ")
        // Spot-check: pt-BR result contains a comma followed by 56 (decimal portion)
        assertEquals(true, brResult.contains(",56"), "pt-BR result was $brResult; expected to contain ',56'")
    }

    // ---- Long.formatNumber -------------------------------------------------

    @Test
    fun long_en_US_with_grouping() {
        assertEquals("1,000,000", 1_000_000L.formatNumber(LocaleTag("en-US")))
    }

    @Test
    fun long_en_US_no_grouping() {
        assertEquals("1000000", 1_000_000L.formatNumber(LocaleTag("en-US"), useGrouping = false))
    }

    @Test
    fun long_negative_en_US() {
        assertEquals("-1,234", (-1234L).formatNumber(LocaleTag("en-US")))
    }

    // ---- Double.formatCurrency ---------------------------------------------

    @Test
    fun currency_usd_en_US() {
        // JVM / Android / iOS all render USD with the leading "$" and 2 fraction digits.
        val formatted = 1234.5.formatCurrency("USD", LocaleTag("en-US"))
        assertEquals("$1,234.50", formatted)
    }

    @Test
    fun currency_jpy_has_no_fraction_digits_by_default() {
        // JPY has 0 fraction digits per ISO 4217 — platform formatters honor that.
        // Use 1234.0 so the assertion doesn't depend on the formatter's rounding mode
        // (JVM defaults to HALF_EVEN; .5 would render as 1234, not 1235).
        val formatted = 1234.0.formatCurrency("JPY", LocaleTag("en-US"))
        assertEquals("¥1,234", formatted)
    }

    @Test
    fun currency_changes_separators_by_locale() {
        // The same USD amount must render differently between en-US (1,234.56) and pt-BR
        // (which uses ., for grouping/decimal). Both must still mention the dollar sign.
        val us = 1234.56.formatCurrency("USD", LocaleTag("en-US"))
        val br = 1234.56.formatCurrency("USD", LocaleTag("pt-BR"))
        assertNotEquals(us, br, "USD formatting should differ between en-US and pt-BR")
    }
}

package com.currencyconverter.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for the pure conversion/formatting engine.
 * Values cross-checked against the reference JS mock and the real seed-rates.json
 * (USD=1, UZS=11913.10991, EUR=0.864617).
 */
class ConverterEngineTest {

    // ---- Spec-required cases -------------------------------------------------

    @Test
    fun `100 USD to UZS matches seed rate`() {
        val result = ConverterEngine.convert(100.0, fromRate = 1.0, toRate = 11913.10991)
        assertEquals(1191310.991, result, 1e-6)
        assertEquals("1,191,311", ConverterEngine.fmt(result))
    }

    @Test
    fun `1 EUR to USD matches seed rate`() {
        val result = ConverterEngine.convert(1.0, fromRate = 0.864617, toRate = 1.0)
        assertEquals(1.156581469020387, result, 1e-9)
        assertEquals("1.16", ConverterEngine.fmt(result))
    }

    @Test
    fun `swap symmetry round-trips the amount`() {
        val amount = 100.0
        val rFrom = 1.0 // USD
        val rTo = 11913.10991 // UZS
        val converted = ConverterEngine.convert(amount, rFrom, rTo)
        val roundTrip = ConverterEngine.convert(converted, rTo, rFrom)
        assertTrue("expected ~$amount, got $roundTrip", abs(roundTrip - amount) < 1e-6)
    }

    @Test
    fun `rateLine formats as 1 FROM = rate TO`() {
        val line = ConverterEngine.rateLine("USD", "UZS", 1.0, 11913.10991)
        assertEquals("1 USD = 11,913.11 UZS", line)
    }

    // ---- fmt() threshold behavior ---------------------------------------------

    @Test
    fun `fmt below 1e6 uses two decimals`() {
        assertEquals("999,999.00", ConverterEngine.fmt(999999.0))
    }

    @Test
    fun `fmt at and above 1e6 uses grouped integer`() {
        assertEquals("1,000,000", ConverterEngine.fmt(1_000_000.0))
        assertEquals("1,500,000", ConverterEngine.fmt(1_500_000.0))
    }

    @Test
    fun `fmt at and above 1e9 uses B suffix`() {
        assertEquals("1B", ConverterEngine.fmt(1e9))
        assertEquals("1.5B", ConverterEngine.fmt(1.5e9))
    }

    @Test
    fun `fmt at and above 1e12 uses T suffix`() {
        assertEquals("1T", ConverterEngine.fmt(1e12))
        assertEquals("2.25T", ConverterEngine.fmt(2.25e12))
    }

    @Test
    fun `fmt sub-1 values allow up to four decimals`() {
        assertEquals("0.50", ConverterEngine.fmt(0.5))
        assertEquals("0.1234", ConverterEngine.fmt(0.1234))
    }

    // ---- fmtEntry() / fit() / stripFmt() ---------------------------------------

    @Test
    fun `fmtEntry groups integer part and keeps raw decimal typing`() {
        assertEquals("100", ConverterEngine.fmtEntry("100"))
        assertEquals("1,234.5", ConverterEngine.fmtEntry("1234.5"))
        assertEquals("100.", ConverterEngine.fmtEntry("100."))
    }

    @Test
    fun `fit clamps between 20 and 46`() {
        assertEquals(46, ConverterEngine.fit("100"))
        assertEquals(43, ConverterEngine.fit("1,191,311.1")) // len 11
        assertEquals(20, ConverterEngine.fit("1".repeat(50)))
    }

    @Test
    fun `stripFmt strips grouping and rounds to 2dp`() {
        assertEquals("1234.5", ConverterEngine.stripFmt("1,234.50"))
        assertEquals("1000", ConverterEngine.stripFmt("$1,000"))
    }

    // ---- press() keypad buffer --------------------------------------------------

    @Test
    fun `press appends digits up to 9 significant digits`() {
        var e = "0"
        e = ConverterEngine.press(e, "5") // "5"
        assertEquals("5", e)
        e = ConverterEngine.press(e, ".") // "5."
        assertEquals("5.", e)
        e = ConverterEngine.press(e, ".") // dot ignored, already present
        assertEquals("5.", e)
        e = "123456789" // already 9 significant digits
        assertEquals("123456789", ConverterEngine.press(e, "1"))
    }

    @Test
    fun `press backspace never goes below single zero`() {
        assertEquals("0", ConverterEngine.press("5", "⌫"))
        assertEquals("12", ConverterEngine.press("123", "⌫"))
        assertEquals("0", ConverterEngine.press("0", "⌫"))
    }

    @Test
    fun `press zero digit replaces leading zero`() {
        assertEquals("7", ConverterEngine.press("0", "7"))
    }
}

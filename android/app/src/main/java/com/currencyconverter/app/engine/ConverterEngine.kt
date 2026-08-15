package com.currencyconverter.app.engine

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Pure currency-conversion math and text formatting.
 * No Android framework dependency -> fully unit testable on the JVM.
 *
 * Mirrors the reference JS implementation in
 * design/currency-converter-app-design/project/Currency Converter v2.dc.html
 */
object ConverterEngine {

    private val usSymbols = DecimalFormatSymbols(Locale.US)

    private fun groupedFormat(minFractionDigits: Int, maxFractionDigits: Int): DecimalFormat {
        val df = DecimalFormat()
        df.decimalFormatSymbols = usSymbols
        df.isGroupingUsed = true
        df.minimumFractionDigits = minFractionDigits
        df.maximumFractionDigits = maxFractionDigits
        df.roundingMode = RoundingMode.HALF_UP
        return df
    }

    /** result = amount / rate[from] * rate[to] */
    fun convert(amount: Double, fromRate: Double, toRate: Double): Double {
        if (fromRate == 0.0) return 0.0
        return amount / fromRate * toRate
    }

    /** "1 <FROM> = <fmt(rate[to]/rate[from])> <TO>" */
    fun rateLine(fromCode: String, toCode: String, fromRate: Double, toRate: Double): String {
        val perOne = if (fromRate == 0.0) 0.0 else toRate / fromRate
        return "1 $fromCode = ${fmt(perOne)} $toCode"
    }

    /** Display formatting for a computed/result value (not the raw entry buffer). */
    fun fmt(n: Double): String {
        val a = abs(n)
        return when {
            a >= 1e12 -> groupedFormat(0, 2).format(n / 1e12) + "T"
            a >= 1e9 -> groupedFormat(0, 2).format(n / 1e9) + "B"
            a >= 1e6 -> groupedFormat(0, 0).format(n)
            else -> {
                val maxDp = if (a >= 1) 2 else 4
                groupedFormat(2, maxDp).format(n)
            }
        }
    }

    /** Formats the live entry buffer (grouped integer part, raw decimal part as typed). */
    fun fmtEntry(s: String): String {
        val parts = s.split(".", limit = 2)
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else null
        val intVal = intPart.ifEmpty { "0" }.toDoubleOrNull() ?: 0.0
        val gi = groupedFormat(0, 0).format(intVal)
        return if (decPart == null) gi else "$gi.$decPart"
    }

    /** Dynamic font size (sp) for the big value text, per spec's `fit()`. */
    fun fit(s: String): Int {
        val len = max(s.length, 1)
        val computed = floor(300.0 / (0.62 * len)).toInt()
        return max(20, min(46, computed))
    }

    /** Strips grouping/formatting from a displayed string back into a raw numeric entry string. */
    fun stripFmt(s: String): String {
        val cleaned = s.filter { it.isDigit() || it == '.' }
        val n = cleaned.toDoubleOrNull() ?: 0.0
        val rounded = Math.round(n * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            var str = String.format(Locale.US, "%.2f", rounded)
            str = str.trimEnd('0').trimEnd('.')
            str
        }
    }

    /** Applies one keypad press to the raw entry buffer. Max 9 significant digits, single '.'. */
    fun press(entry: String, key: String): String {
        return when {
            key == "⌫" -> if (entry.length > 1) entry.dropLast(1) else "0"
            key == "." -> if (entry.contains(".")) entry else "$entry."
            entry.replace(".", "").length < 9 -> if (entry == "0") key else entry + key
            else -> entry
        }
    }
}

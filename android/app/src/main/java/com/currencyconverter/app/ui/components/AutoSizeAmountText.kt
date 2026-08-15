package com.currencyconverter.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.currencyconverter.app.ui.theme.PlusJakartaSans

private const val MAX_SP = 46
private const val MIN_SP = 20

/**
 * Amount line: font size fitted to the available width by REAL text measurement
 * (binary search over TextMeasurer.measure), not the design's char-count approximation
 * (`clamp(20, floor(300 / (0.62*len)), 46)`). Never clips: if even MIN_SP overflows the
 * line, it is used anyway per spec ("clamp(20, ...)" is a hard floor).
 */
@Composable
fun AutoSizeAmountText(
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxSp: Int = MAX_SP,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        val maxWidthPx = constraints.maxWidth
        val fontSizeSp = remember(value, maxWidthPx, maxSp) {
            fitFontSize(measurer, value, maxWidthPx, maxSp)
        }
        Text(
            text = value,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.6).sp,
            fontSize = fontSizeSp.sp,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun fitFontSize(
    measurer: androidx.compose.ui.text.TextMeasurer,
    value: String,
    maxWidthPx: Int,
    maxSp: Int = MAX_SP,
): Int {
    if (maxWidthPx <= 0) return maxSp

    fun widthAt(sizeSp: Int): Int {
        val layout = measurer.measure(
            text = value,
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.6).sp,
                fontSize = sizeSp.sp,
            ),
        )
        return layout.size.width
    }

    // Binary search the largest size in [MIN_SP, maxSp] whose measured width fits.
    if (widthAt(maxSp) <= maxWidthPx) return maxSp
    var lo = MIN_SP
    var hi = maxSp
    var best = MIN_SP
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (widthAt(mid) <= maxWidthPx) {
            best = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return best
}

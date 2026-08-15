package com.currencyconverter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brand mark from brand/logo.svg: a rounded tile (accent) with two opposed arrows
 * (currentColor -> accentInk) meaning "exchange". Drawn on a Canvas (not a static vector
 * drawable) so the arrow color can follow the accentInk token, which differs light vs dark.
 *
 * Source path data (48x48 viewBox):
 *   M14 19h16 / M26 14.5 30.5 19 26 23.5 / M34 29H18 / M22 24.5 17.5 29 22 33.5
 *   stroke-width 3.6, round cap/join, tile radius 14/48.
 */
@Composable
fun ConverterLogo(
    size: Dp = 30.dp,
    tileColor: Color,
    glyphColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 48f
        val cornerPx = 14f * scale
        drawRoundRect(
            color = tileColor,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )

        val strokeWidthPx = 3.6f * scale
        val stroke = Stroke(
            width = strokeWidthPx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.cornerPathEffect(0f),
        )

        fun pt(x: Float, y: Float) = Offset(x * scale, y * scale)

        // Top line + right-pointing arrowhead: M14 19h16 / M26 14.5 30.5 19 26 23.5
        val topLine = Path().apply {
            moveTo(pt(14f, 19f).x, pt(14f, 19f).y)
            lineTo(pt(30f, 19f).x, pt(30f, 19f).y)
        }
        val topArrow = Path().apply {
            moveTo(pt(26f, 14.5f).x, pt(26f, 14.5f).y)
            lineTo(pt(30.5f, 19f).x, pt(30.5f, 19f).y)
            lineTo(pt(26f, 23.5f).x, pt(26f, 23.5f).y)
        }
        // Bottom line + left-pointing arrowhead: M34 29H18 / M22 24.5 17.5 29 22 33.5
        val bottomLine = Path().apply {
            moveTo(pt(34f, 29f).x, pt(34f, 29f).y)
            lineTo(pt(18f, 29f).x, pt(18f, 29f).y)
        }
        val bottomArrow = Path().apply {
            moveTo(pt(22f, 24.5f).x, pt(22f, 24.5f).y)
            lineTo(pt(17.5f, 29f).x, pt(17.5f, 29f).y)
            lineTo(pt(22f, 33.5f).x, pt(22f, 33.5f).y)
        }

        drawPath(topLine, color = glyphColor, style = stroke)
        drawPath(topArrow, color = glyphColor, style = stroke)
        drawPath(bottomLine, color = glyphColor, style = stroke)
        drawPath(bottomArrow, color = glyphColor, style = stroke)
    }
}

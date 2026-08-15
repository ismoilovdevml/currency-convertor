package com.currencyconverter.app.ui.components

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.currencyconverter.app.ui.theme.CurrencyConverterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** In-memory decode cache: 155 small flag PNGs total, cheap to keep resident once decoded. */
private val flagCache = ConcurrentHashMap<String, ImageBitmap?>()

private suspend fun decodeFlag(assets: AssetManager, path: String): ImageBitmap? {
    flagCache[path]?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull().also { flagCache[path] = it }
    }
}

/** Round flag avatar loaded from `assets/flags/<cc>.png`; falls back to a neutral circle. */
@Composable
fun FlagImage(assets: AssetManager, assetPath: String?, size: Dp, modifier: Modifier = Modifier) {
    val colors = CurrencyConverterTheme.colors
    val bitmap by produceState<ImageBitmap?>(initialValue = assetPath?.let { flagCache[it] }, key1 = assetPath) {
        value = assetPath?.let { decodeFlag(assets, it) }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.line)
            // Spec: "1px inset hairline rgba(12,30,22,0.08)" around the flag circle.
            .border(1.dp, Color(0x140C1E12), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            // Flags are 2:1 (wide); Crop (not the Fit default) fills the circle edge-to-edge
            // instead of letterboxing, matching the iOS reference rendering.
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text(
                text = "?",
                color = colors.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

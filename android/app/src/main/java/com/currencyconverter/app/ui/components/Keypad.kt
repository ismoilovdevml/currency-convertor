package com.currencyconverter.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.currencyconverter.app.ui.theme.CurrencyConverterTheme
import com.currencyconverter.app.ui.theme.PlusJakartaSans

private const val BACKSPACE_KEY = "⌫"
private val KEYS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", BACKSPACE_KEY)

/**
 * 3×4 numeric keypad. Uses weighted rows/cells (no fixed height) so it fills exactly the space
 * the parent gives it and NEVER clips the bottom row — the previous fixed 326.dp + aspectRatio
 * grid overflowed the last row on wider/taller devices (e.g. Galaxy S24 Ultra).
 */
@Composable
fun Keypad(onPress: (String) -> Unit, onClearAll: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KEYS.chunked(3).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowKeys.forEach { key ->
                    // Long-pressing backspace clears the whole entry (handy shortcut for the Clear button).
                    val onLong = if (key == BACKSPACE_KEY) onClearAll else null
                    KeyButton(key = key, onPress = onPress, onLongClick = onLong, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.KeyButton(key: String, onPress: (String) -> Unit, onLongClick: (() -> Unit)?, modifier: Modifier) {
    val colors = CurrencyConverterTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = { onPress(key) },
            onLongClick = onLongClick,
        )
    } else {
        Modifier.clickable(interactionSource = interaction, indication = null) { onPress(key) }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (pressed) colors.accent else colors.key)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (pressed) colors.accentInk else colors.fg
        if (key == BACKSPACE_KEY) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(
                text = key,
                fontFamily = PlusJakartaSans,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
        }
    }
}

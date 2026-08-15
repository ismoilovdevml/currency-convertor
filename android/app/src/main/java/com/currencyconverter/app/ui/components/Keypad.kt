package com.currencyconverter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun Keypad(onPress: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = CurrencyConverterTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(326.dp),
        userScrollEnabled = false,
    ) {
        items(KEYS) { key ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (pressed) colors.accent else colors.key)
                    .clickable(interactionSource = interaction, indication = null) { onPress(key) },
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
    }
}

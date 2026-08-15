package com.currencyconverter.app.ui

import android.content.res.AssetManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.currencyconverter.app.ui.components.AutoSizeAmountText
import com.currencyconverter.app.ui.components.ConverterLogo
import com.currencyconverter.app.ui.components.CurrencySheetOverlay
import com.currencyconverter.app.ui.components.FlagImage
import com.currencyconverter.app.ui.components.Keypad
import com.currencyconverter.app.ui.theme.CurrencyConverterTheme
import com.currencyconverter.app.ui.theme.PlusJakartaSans
import com.currencyconverter.app.viewmodel.ConverterDisplay
import com.currencyconverter.app.viewmodel.ConverterViewModel
import com.currencyconverter.app.viewmodel.EntrySide
import com.currencyconverter.app.viewmodel.SheetTarget

@Composable
fun ConverterScreen(state: ConverterDisplay, viewModel: ConverterViewModel, assets: AssetManager) {
    val colors = CurrencyConverterTheme.colors

    // System back closes the open picker sheet instead of exiting the app.
    BackHandler(enabled = state.sheetOpen) { viewModel.closeSheet() }

    // Responsive: on short screens compact the card so the keypad is never squeezed off-screen.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val compact = maxHeight < 680.dp
    val cardPad = if (compact) 14.dp else 20.dp
    val dividerPad = if (compact) 6.dp else 14.dp
    val headerH = if (compact) 50.dp else 58.dp
    val valueMaxSp = if (compact) 30 else 46

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 18.dp, end = 18.dp, bottom = 12.dp),
    ) {
        // ---- Header ----
        Row(
            modifier = Modifier.fillMaxWidth().height(headerH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                ConverterLogo(size = 30.dp, tileColor = colors.accent, glyphColor = colors.accentInk)
                Text(
                    text = "Converter",
                    fontFamily = PlusJakartaSans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.fg,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Connectivity STATUS (not a toggle): follows the real network state automatically.
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (state.online) colors.accentSoft else colors.key)
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(if (state.online) colors.accent else colors.muted),
                    )
                    Text(
                        text = state.modeLabel,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.online) colors.accentText else colors.muted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable { viewModel.toggleTheme() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (state.darkTheme) "☀" else "☾", fontSize = 14.sp, color = colors.fg)
                }
            }
        }

        // ---- Converter card ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .padding(cardPad),
        ) {
            CurrencyRowButton(
                code = state.fromCode,
                name = state.fromName,
                flagAsset = state.fromFlagAsset,
                assets = assets,
                onClick = { viewModel.openSheet(SheetTarget.FROM) },
            )
            ValueRow(
                value = state.fromValue,
                active = state.activeFrom,
                valueColor = colors.fg,
                maxSp = valueMaxSp,
                onClick = { viewModel.focusSide(EntrySide.FROM) },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = dividerPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.line))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable { viewModel.swap() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⇅", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentInk)
                }
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.line))
            }

            CurrencyRowButton(
                code = state.toCode,
                name = state.toName,
                flagAsset = state.toFlagAsset,
                assets = assets,
                onClick = { viewModel.openSheet(SheetTarget.TO) },
            )
            ValueRow(
                value = state.toValue,
                active = state.activeTo,
                valueColor = colors.accentText,
                maxSp = valueMaxSp,
                onClick = { viewModel.focusSide(EntrySide.TO) },
            )
        }

        // ---- Rate row ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = state.rateLine,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
            )
            Text(
                text = state.updatedLine,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ---- Clear row (product addition beyond the design spec) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.key)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .clickable { viewModel.clearEntry() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Clear",
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- Keypad: weight(1f) so it fills remaining space and never clips on any device ----
        Keypad(onPress = { viewModel.press(it) }, onClearAll = { viewModel.clearEntry() }, modifier = Modifier.weight(1f))
    }
    }

    CurrencySheetOverlay(
        visible = state.sheetOpen,
        title = state.sheetTitle,
        query = state.query,
        rows = state.sheetList,
        assets = assets,
        onQueryChange = { viewModel.search(it) },
        onPick = { viewModel.pickCurrency(it) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onClose = { viewModel.closeSheet() },
    )
}

@Composable
private fun CurrencyRowButton(
    code: String,
    name: String,
    flagAsset: String?,
    assets: AssetManager,
    onClick: () -> Unit,
) {
    val colors = CurrencyConverterTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlagImage(assets = assets, assetPath = flagAsset, size = 34.dp)
        Text(
            text = code,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.fg,
        )
        Text(
            text = name,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(150.dp),
        )
        Text("▼", fontSize = 10.sp, color = colors.muted)
    }
}

@Composable
private fun ValueRow(
    value: String,
    active: Boolean,
    valueColor: Color,
    maxSp: Int,
    onClick: () -> Unit,
) {
    val colors = CurrencyConverterTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        AutoSizeAmountText(
            value = value,
            color = valueColor,
            maxSp = maxSp,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (active) {
            Spacer(modifier = Modifier.width(4.dp))
            BlinkingCaret(color = colors.accent)
        }
    }
}

@Composable
private fun BlinkingCaret(color: Color) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f at 0
                1f at 605 using LinearEasing
                0f at 616
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "caretAlpha",
    )
    Box(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .width(3.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = alpha)),
    )
}

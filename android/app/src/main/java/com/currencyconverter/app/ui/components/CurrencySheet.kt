package com.currencyconverter.app.ui.components

import android.content.res.AssetManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.currencyconverter.app.R
import com.currencyconverter.app.ui.theme.CurrencyConverterTheme
import com.currencyconverter.app.ui.theme.PlusJakartaSans
import com.currencyconverter.app.viewmodel.SheetRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

@Composable
fun CurrencySheetOverlay(
    visible: Boolean,
    title: String,
    query: String,
    rows: List<SheetRow>,
    assets: AssetManager,
    onQueryChange: (String) -> Unit,
    onPick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CurrencyConverterTheme.colors
    val focusRequester = remember { FocusRequester() }
    // Sheet ochilganда qidiruv avtomatik fokus olsin (slide animatsiyasi tugagach).
    LaunchedEffect(visible) {
        if (visible) {
            delay(250)
            runCatching { focusRequester.requestFocus() }
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.scrim)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
            )
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(240)) { it },
                exit = slideOutVertically(tween(200)) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(colors.sheet)
                        .imePadding()
                        .padding(horizontal = 18.dp),
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.line),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            fontFamily = PlusJakartaSans,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.fg,
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.key)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 13.sp, color = colors.fg)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.key)
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.search_hint),
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.muted,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.fg,
                                ),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(rows, key = { it.code }) { row ->
                            CurrencyRow(
                                row = row,
                                assets = assets,
                                onClick = { onPick(row.code) },
                                onToggleFavorite = { onToggleFavorite(row.code) },
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    row: SheetRow,
    assets: AssetManager,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = CurrencyConverterTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Star toggle — hit target 36x36, independent tap target from row selection.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onToggleFavorite() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (row.isFavorite) "★" else "☆",
                    fontSize = 17.sp,
                    color = if (row.isFavorite) colors.accent else colors.starEmpty,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .padding(vertical = 11.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                FlagImage(assets = assets, assetPath = row.flagAsset, size = 34.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.code,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.fg,
                        maxLines = 1,
                    )
                    Text(
                        text = row.name,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = row.rateText,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                )
                if (row.selected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentInk)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
    }
}

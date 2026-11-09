package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.NotoSansBengaliFontFamily
import com.example.ui.theme.extraColors

/** Actions for the two tap areas on the header picture. Set once in AppNavHost. */
class HeaderActions(
    val onAppLanguage: () -> Unit = {},
    val onTranslationLanguage: () -> Unit = {},
    val onSearch: () -> Unit = {}
)

val LocalHeaderActions = staticCompositionLocalOf { HeaderActions() }

/**
 * Simple top bar used on every screen except Home: back arrow, screen name and the screen's buttons.
 * (The picture header is only on Home. See AppHeaderPicture.)
 */
@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    applyStatusBarInset: Boolean = true
) {
    val extra = MaterialTheme.extraColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(extra.headerBg)
            .then(if (applyStatusBarInset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) { title() }
            actions()
        }
        HorizontalDivider(color = extra.headerLine.copy(alpha = 0.55f), thickness = 0.8.dp)
    }
}

/** Hides the picture header when the person scrolls down and shows it again when they scroll up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberHeaderScrollBehavior(): TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

/**
 * The header picture with the language pill and the search circle.
 * With a scrollBehavior it slides away on scroll down (not stuck at the top).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeaderPicture(scrollBehavior: TopAppBarScrollBehavior? = null) {
    val actions = LocalHeaderActions.current
    val languageDesc = stringResource(R.string.language_label)
    val searchDesc = stringResource(R.string.search_title)
    var menuOpen by remember { mutableStateOf(false) }
    val collapse = if (scrollBehavior != null) {
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val barState = scrollBehavior.state
                barState.heightOffsetLimit = -placeable.height.toFloat()
                val offset = barState.heightOffset.roundToInt()
                layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) {
                    placeable.place(0, offset)
                }
            }
            .clipToBounds()
    } else {
        Modifier
    }
    Box(
        modifier = collapse
            .fillMaxWidth()
            .background(Color(0xFF0C481F))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Image(
            painter = painterResource(R.drawable.header_bar),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        // Invisible tap areas over the language pill and the search circle of the picture.
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val w = maxWidth
            val h = maxHeight
            Box(
                modifier = Modifier
                    .offset(x = w * 0.010f, y = h * 0.36f)
                    .size(width = w * 0.205f, height = h * 0.50f)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button, onClickLabel = languageDesc) { menuOpen = true }
                        .testTag("header_language_button")
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_app_language)) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onAppLanguage()
                        },
                        modifier = Modifier.testTag("menu_app_language")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_translation_language)) },
                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            actions.onTranslationLanguage()
                        },
                        modifier = Modifier.testTag("menu_translation_language")
                    )
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = w * 0.835f, y = h * 0.36f)
                    .size(width = w * 0.155f, height = h * 0.50f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClickLabel = searchDesc) { actions.onSearch() }
                    .testTag("header_search_button")
            )
        }
    }
}

/** Header title: serif for English, Noto Sans Bengali for Bengali text. */
@Composable
fun AppTitle(text: String) {
    val bengali = text.any { it in '\u0980'..'\u09FF' }
    Text(
        text = text,
        color = MaterialTheme.extraColors.headerText,
        fontFamily = if (bengali) NotoSansBengaliFontFamily else CormorantFontFamily,
        fontWeight = if (bengali) FontWeight.Medium else FontWeight.Bold,
        fontSize = if (bengali) 19.sp else 24.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Section title used in Settings: serif text with a thin line under it. */
@Composable
fun SectionTitle(text: String) {
    val bengali = text.any { it in '\u0980'..'\u09FF' }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = MaterialTheme.extraColors.accentGold,
            fontFamily = if (bengali) NotoSansBengaliFontFamily else CormorantFontFamily,
            fontWeight = if (bengali) FontWeight.Medium else FontWeight.Bold,
            fontSize = if (bengali) 19.sp else 24.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.extraColors.headerLine.copy(alpha = 0.55f),
            thickness = 0.8.dp
        )
    }
}

/** Thin line with a small gold star in the middle, between ayahs. */
@Composable
fun AyahDivider(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.extraColors.headerLine
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val cy = size.height / 2f
        val cx = size.width / 2f
        val r = 6.dp.toPx()
        val gap = r + 6.dp.toPx()
        val sw = 0.8.dp.toPx()
        drawLine(gold.copy(alpha = 0.6f), Offset(0f, cy), Offset(cx - gap, cy), sw)
        drawLine(gold.copy(alpha = 0.6f), Offset(cx + gap, cy), Offset(size.width, cy), sw)
        val star = Path().apply {
            for (i in 0 until 16) {
                val ang = Math.PI * 2 * i / 16 - Math.PI / 2
                val rad = if (i % 2 == 0) r else r * 0.62f
                val x = cx + (rad * Math.cos(ang)).toFloat()
                val y = cy + (rad * Math.sin(ang)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, gold, style = Stroke(width = sw))
        drawCircle(gold, radius = 1.6.dp.toPx(), center = Offset(cx, cy))
    }
}

class BottomBarItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: ImageVector,
    val label: String,
    val testTag: String
)

/** Bottom bar with a soft pill behind the selected item. */
@Composable
fun AppBottomBar(items: List<BottomBarItem>) {
    val extra = MaterialTheme.extraColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(extra.barBg)
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            thickness = 0.8.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val tint = if (item.selected) extra.accentGold else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .testTag(item.testTag)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (item.selected) extra.barPill else Color.Transparent)
                            .selectable(
                                selected = item.selected,
                                onClick = item.onClick,
                                role = Role.Tab
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.label,
                            color = tint,
                            fontSize = 12.sp,
                            fontWeight = if (item.selected) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Switch colours like the design: cream round thumb, green track. */
@Composable
fun appSwitchColors(): SwitchColors {
    val c = MaterialTheme.colorScheme
    val dark = c.background.red < 0.5f
    return SwitchDefaults.colors(
        checkedThumbColor = if (dark) Color(0xFFFFF6DC) else Color.White,
        checkedTrackColor = if (dark) Color(0xFF4FA377) else c.primary,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = if (dark) Color(0xFFD9D4C4) else c.outline,
        uncheckedTrackColor = if (dark) Color(0xFF244A3B) else c.surfaceVariant,
        uncheckedBorderColor = Color.Transparent
    )
}

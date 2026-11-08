package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TopAppBarDefaults
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

/** Top bar with the gold arch under it. Same slots as the normal TopAppBar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val header = MaterialTheme.extraColors.headerBg
    Column(modifier = modifier) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = header,
                scrolledContainerColor = header
            )
        )
        HeaderArch()
    }
}

@Composable
private fun HeaderArch() {
    val header = MaterialTheme.extraColors.headerBg
    val line = MaterialTheme.extraColors.headerLine
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val sw = 1.2.dp.toPx()
        val w = size.width
        val h = size.height - sw
        val mid = w / 2f

        val above = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            cubicTo(w * 0.70f, h, w * 0.56f, h * 0.80f, mid, 0f)
            cubicTo(w * 0.44f, h * 0.80f, w * 0.30f, h, 0f, h)
            close()
        }
        drawPath(path = above, color = header)

        val curve = Path().apply {
            moveTo(0f, h)
            cubicTo(w * 0.30f, h, w * 0.44f, h * 0.80f, mid, 0f)
            cubicTo(w * 0.56f, h * 0.80f, w * 0.70f, h, w, h)
        }
        drawPath(path = curve, color = line, style = Stroke(width = sw))
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
        fontSize = if (bengali) 22.sp else 30.sp,
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

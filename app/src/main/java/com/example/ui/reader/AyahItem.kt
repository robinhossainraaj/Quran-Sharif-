package com.example.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AyahDivider
import com.example.data.db.AyahWithText
import com.example.ui.theme.extraColors
import com.example.ui.theme.getArabicTextStyle
import com.example.ui.theme.getBanglaTextStyle
import com.example.util.BanglaDigits

@Composable
fun AyahItem(
    ayah: AyahWithText,
    isBookmarked: Boolean,
    isHighlighted: Boolean,
    isPlayingAudio: Boolean = false,
    useBanglaDigits: Boolean,
    arabicFontSizeSp: Int,
    banglaFontSizeSp: Int,
    showBangla: Boolean,
    onBookmarkClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onPlayFromHere: (() -> Unit)? = null
) {
    val ayahNumberStr = if (useBanglaDigits) BanglaDigits.convert(ayah.ayahNo) else ayah.ayahNo.toString()

    val targetBgColor = if (isHighlighted || isPlayingAudio) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color.Transparent
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 400),
        label = "highlight_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(animatedBgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("ayah_item_${ayah.ayahNo}")
    ) {
        // Row 1: Ayah number badge on left + Action buttons on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge (accentGold when audio is playing)
            val badgeBg = if (isPlayingAudio) MaterialTheme.extraColors.accentGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
            val badgeText = if (isPlayingAudio) MaterialTheme.extraColors.accentGold else MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ayahNumberStr,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }

            // Action buttons (48dp touch targets)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onPlayFromHere != null) {
                    IconButton(
                        onClick = onPlayFromHere,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_from_here_${ayah.ayahNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play_from_here),
                            tint = if (isPlayingAudio) MaterialTheme.extraColors.accentGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bookmark_ayah_${ayah.ayahNo}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.nav_bookmarks),
                        tint = if (isBookmarked) MaterialTheme.extraColors.accentGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("copy_ayah_${ayah.ayahNo}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_ayah),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("share_ayah_${ayah.ayahNo}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_ayah),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text (Amiri Quran font, Gold)
        Text(
            text = ayah.arabicDisplay,
            style = getArabicTextStyle(arabicFontSizeSp),
            color = MaterialTheme.extraColors.accentGold,
            modifier = Modifier.fillMaxWidth()
        )

        // Bangla text
        if (showBangla) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = ayah.bangla,
                style = getBanglaTextStyle(banglaFontSizeSp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AyahDivider()
    }
}

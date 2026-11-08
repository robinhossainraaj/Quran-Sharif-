package com.example.ui.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.player.AudioRepeatMode
import com.example.audio.player.PlaybackUiState
import com.example.util.BanglaDigits

// Green and gold look of the floating player
private val PlayerGreenTop = Color(0xFF0F503A)
private val PlayerGreenBottom = Color(0xFF0A3727)
private val PlayerBorder = Color(0xFF22805A)
private val PlayerGold = Color(0xFFF3D98B)
private val PlayerGoldLight = Color(0xFFFDEAA8)
private val PlayerGoldDeep = Color(0xFFE6BF62)
private val PlayerInk = Color(0xFF0F2E1F)
private val PlayerText = Color(0xFFF3F8F4)
private val PlayerSoftText = Color(0xFFCFE3D8)
private val PlayerMuted = Color(0xFF9FCAB5)
private val PlayerButtonFill = Color(0xFF0C4631)
private val PlayerButtonBorder = Color(0xFF2A8A63)
private val PlayerSmallButton = Color(0xFF17604A)
private val PlayerTrack = Color(0xFF2D6F55)

/**
 * Floating audio player.
 *
 * The time line belongs to the ayah that is playing now. Each ayah is its own audio file,
 * so the bar and the time start again from zero when the next ayah begins.
 */
@Composable
fun MiniPlayerBar(
    state: PlaybackUiState,
    useBanglaDigits: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onChangeReciter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(12.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(PlayerGreenTop, PlayerGreenBottom)))
            .border(1.dp, PlayerBorder, shape)
            .testTag("mini_player_bar")
    ) {
        if (expanded) {
            ExpandedPlayer(
                state = state,
                useBanglaDigits = useBanglaDigits,
                onCollapse = { onExpandedChange(false) },
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onRepeatToggle = onRepeatToggle,
                onSpeedSelected = onSpeedSelected,
                onSeek = onSeek,
                onChangeReciter = onChangeReciter,
                onClose = onClose
            )
        } else {
            CollapsedPlayer(
                state = state,
                useBanglaDigits = useBanglaDigits,
                onExpand = { onExpandedChange(true) },
                onPlayPause = onPlayPause,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun ExpandedPlayer(
    state: PlaybackUiState,
    useBanglaDigits: Boolean,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onChangeReciter: () -> Unit,
    onClose: () -> Unit
) {
    var showSpeedMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: icon, surah, ayah, collapse, close
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MosqueTile(size = 56.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerTitle(state),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlayerText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playerSubtitle(state, useBanglaDigits),
                    fontSize = 13.sp,
                    color = PlayerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlayerSmallButton)
                    .clickable(role = Role.Button, onClick = onCollapse)
                    .testTag("player_collapse_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimise player",
                    tint = PlayerText,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose)
                    .testTag("player_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Player",
                    tint = PlayerMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Change reciter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PlayerGold.copy(alpha = 0.10f))
                .border(1.dp, Color(0xFF5A9F7F), RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onChangeReciter)
                .padding(horizontal = 12.dp)
                .testTag("player_change_reciter"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = PlayerGold,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = state.reciterName.ifBlank { "Mishary Alafasy" },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PlayerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.player_change_reciter),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlayerGold
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PlayerGold,
                modifier = Modifier.size(18.dp)
            )
        }

        // Time line of THIS ayah
        AyahTimeLine(
            state = state,
            useBanglaDigits = useBanglaDigits,
            onSeek = onSeek
        )

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed
            Box {
                RoundControl(
                    size = 46.dp,
                    onClick = { showSpeedMenu = true },
                    modifier = Modifier.testTag("player_speed_button")
                ) {
                    val speedText = "${state.speed}x"
                    Text(
                        text = if (useBanglaDigits) BanglaDigits.convert(speedText) else speedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerText
                    )
                }
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false }
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                        DropdownMenuItem(
                            text = { Text("${spd}x") },
                            onClick = {
                                showSpeedMenu = false
                                onSpeedSelected(spd)
                            },
                            modifier = Modifier.testTag("speed_option_$spd")
                        )
                    }
                }
            }

            RoundControl(
                size = 52.dp,
                onClick = onPrevious,
                modifier = Modifier.testTag("player_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = PlayerText,
                    modifier = Modifier.size(26.dp)
                )
            }

            PlayButton(state = state, onClick = onPlayPause)

            RoundControl(
                size = 52.dp,
                onClick = onNext,
                modifier = Modifier.testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = PlayerText,
                    modifier = Modifier.size(26.dp)
                )
            }

            RoundControl(
                size = 46.dp,
                onClick = onRepeatToggle,
                modifier = Modifier.testTag("player_repeat_button")
            ) {
                when (state.repeatMode) {
                    AudioRepeatMode.OFF -> Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = stringResource(R.string.audio_repeat_off),
                        tint = PlayerText.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                    AudioRepeatMode.AYAH -> Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription = stringResource(R.string.audio_repeat_ayah),
                        tint = PlayerGold,
                        modifier = Modifier.size(22.dp)
                    )
                    AudioRepeatMode.SURAH -> Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = stringResource(R.string.audio_repeat_surah),
                        tint = PlayerGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedPlayer(
    state: PlaybackUiState,
    useBanglaDigits: Boolean,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MosqueTile(size = 48.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerTitle(state),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlayerText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playerSubtitle(state, useBanglaDigits),
                    fontSize = 12.sp,
                    color = PlayerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            PlayButton(state = state, onClick = onPlayPause, size = 48.dp, ring = false)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PlayerSmallButton)
                    .clickable(role = Role.Button, onClick = onExpand)
                    .testTag("player_expand_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Open player",
                    tint = PlayerText,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose)
                    .testTag("player_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Player",
                    tint = PlayerMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Thin progress line of the current ayah
        val duration = state.durationMs
        val fraction = if (duration > 0L) {
            (state.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PlayerTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(PlayerGold)
            )
        }
    }
}

@Composable
private fun AyahTimeLine(
    state: PlaybackUiState,
    useBanglaDigits: Boolean,
    onSeek: (Long) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val duration = state.durationMs.coerceAtLeast(0L)
    val maxValue = if (duration > 0L) duration.toFloat() else 1f
    val shownPosition = if (dragging) dragValue else state.positionMs.toFloat()

    Column {
        Slider(
            value = shownPosition.coerceIn(0f, maxValue),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(dragValue.toLong())
            },
            valueRange = 0f..maxValue,
            enabled = duration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = PlayerGoldLight,
                activeTrackColor = PlayerGold,
                inactiveTrackColor = PlayerTrack,
                disabledThumbColor = PlayerGoldLight.copy(alpha = 0.6f),
                disabledActiveTrackColor = PlayerGold.copy(alpha = 0.6f),
                disabledInactiveTrackColor = PlayerTrack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_time_line")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(shownPosition.toLong(), useBanglaDigits),
                fontSize = 13.sp,
                color = PlayerSoftText
            )
            Text(
                text = formatTime(duration, useBanglaDigits),
                fontSize = 13.sp,
                color = PlayerSoftText
            )
        }
    }
}

@Composable
private fun PlayButton(
    state: PlaybackUiState,
    onClick: () -> Unit,
    size: Dp = 68.dp,
    ring: Boolean = true
) {
    val ringPadding = if (ring) 5.dp else 0.dp
    Box(
        modifier = Modifier
            .size(size + ringPadding * 2)
            .clip(CircleShape)
            .background(if (ring) PlayerGold.copy(alpha = 0.22f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PlayerGoldLight, PlayerGoldDeep)))
                .clickable(role = Role.Button, onClick = onClick)
                .testTag("player_play_button"),
            contentAlignment = Alignment.Center
        ) {
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.42f),
                    strokeWidth = 3.dp,
                    color = PlayerInk
                )
            } else {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = PlayerInk,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
    }
}

@Composable
private fun RoundControl(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PlayerButtonFill)
            .border(1.dp, PlayerButtonBorder, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Small mosque picture drawn in code (no image file needed). */
@Composable
private fun MosqueTile(size: Dp) {
    val tileShape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(tileShape)
            .background(Brush.linearGradient(listOf(Color(0xFF146A4A), Color(0xFF0B3F2C))))
            .border(1.dp, Color(0xFF2A8A63), tileShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height

            // base
            drawRoundRect(
                color = PlayerGold,
                topLeft = Offset(w * 0.08f, h * 0.82f),
                size = Size(w * 0.84f, h * 0.09f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // building under the dome
            drawRect(
                color = PlayerGold,
                topLeft = Offset(w * 0.30f, h * 0.58f),
                size = Size(w * 0.40f, h * 0.24f)
            )
            // dome
            val dome = Path().apply {
                moveTo(w * 0.30f, h * 0.58f)
                cubicTo(w * 0.30f, h * 0.28f, w * 0.70f, h * 0.28f, w * 0.70f, h * 0.58f)
                close()
            }
            drawPath(dome, PlayerGold)
            // finial on top of the dome
            drawLine(
                color = PlayerGold,
                start = Offset(w * 0.50f, h * 0.14f),
                end = Offset(w * 0.50f, h * 0.34f),
                strokeWidth = w * 0.05f,
                cap = StrokeCap.Round
            )
            // two minarets
            for (cx in listOf(0.15f, 0.85f)) {
                drawRect(
                    color = PlayerGold,
                    topLeft = Offset(w * (cx - 0.05f), h * 0.40f),
                    size = Size(w * 0.10f, h * 0.42f)
                )
                val tip = Path().apply {
                    moveTo(w * (cx - 0.065f), h * 0.40f)
                    lineTo(w * cx, h * 0.24f)
                    lineTo(w * (cx + 0.065f), h * 0.40f)
                    close()
                }
                drawPath(tip, PlayerGold)
            }
        }
    }
}

@Composable
private fun playerTitle(state: PlaybackUiState): String {
    return if (state.surahName.isBlank()) {
        ""
    } else {
        stringResource(R.string.player_surah_title_format, state.surahName)
    }
}

@Composable
private fun playerSubtitle(state: PlaybackUiState, useBanglaDigits: Boolean): String {
    return if (state.isBismillahItem) {
        "বিসমিল্লাহির রহমানির রাহীম"
    } else {
        val ayahStr = if (useBanglaDigits) BanglaDigits.convert(state.currentAyahNo) else state.currentAyahNo.toString()
        val totalStr = if (useBanglaDigits) BanglaDigits.convert(state.totalAyahsInSurah) else state.totalAyahsInSurah.toString()
        stringResource(R.string.player_ayah_of_format, ayahStr, totalStr)
    }
}

private fun formatTime(ms: Long, useBanglaDigits: Boolean): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val text = "$minutes:${seconds.toString().padStart(2, '0')}"
    return if (useBanglaDigits) BanglaDigits.convert(text) else text
}

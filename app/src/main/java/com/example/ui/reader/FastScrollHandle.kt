package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.extraColors
import com.example.util.BanglaDigits
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FastScrollHandle(
    listState: LazyListState,
    totalAyahs: Int,
    useBanglaDigits: Boolean,
    onScrollToAyah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalAyahs <= 3) return

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var activeAyahNumber by remember { mutableIntStateOf(1) }

    // Natural scroll progress from LazyListState
    val scrollProgress by remember {
        derivedStateOf {
            val firstIdx = listState.firstVisibleItemIndex
            if (firstIdx <= 0) 0f
            else (firstIdx.toFloat() / (totalAyahs + 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    val currentFraction = if (isDragging) dragProgress else scrollProgress

    // Auto-hide bubble after dragging ends
    var showBubble by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging) {
        if (isDragging) {
            showBubble = true
        } else {
            delay(1200)
            showBubble = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp), // 48dp touch target width
        contentAlignment = Alignment.TopEnd
    ) {
        val totalHeightPx = constraints.maxHeight.toFloat()
        val thumbHeightDp = 44.dp

        fun updateFromY(y: Float) {
            val fraction = (y / totalHeightPx).coerceIn(0f, 1f)
            dragProgress = fraction
            val calculatedAyah = (fraction * (totalAyahs - 1)).roundToInt() + 1
            val clampedAyah = calculatedAyah.coerceIn(1, totalAyahs)
            activeAyahNumber = clampedAyah
            onScrollToAyah(clampedAyah)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .pointerInput(totalAyahs, totalHeightPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            updateFromY(offset.y)
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                }
                .pointerInput(totalAyahs, totalHeightPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            updateFromY(offset.y)
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateFromY(change.position.y)
                        }
                    )
                }
        ) {
            val thumbOffsetY = ((totalHeightPx - 80f) * currentFraction).coerceIn(0f, totalHeightPx - 80f)

            // Draggable thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = thumbOffsetY.roundToInt()) }
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    // Floating verse indicator bubble
                    AnimatedVisibility(
                        visible = showBubble || isDragging,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 6.dp
                        ) {
                            val ayahStr = if (useBanglaDigits) BanglaDigits.convert(activeAyahNumber) else activeAyahNumber.toString()
                            Text(
                                text = stringResource(R.string.fast_scroll_verse, ayahStr),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Handle pill
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isDragging) 8.dp else 5.dp,
                                height = if (isDragging) 50.dp else 40.dp
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isDragging) MaterialTheme.extraColors.accentGold else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            .testTag("fast_scroll_handle")
                    )
                }
            }
        }
    }
}

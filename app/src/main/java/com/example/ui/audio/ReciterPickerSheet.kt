package com.example.ui.audio

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.catalog.Reciter
import com.example.audio.preview.AudioPreviewController
import com.example.audio.preview.PreviewState
import com.example.ui.theme.extraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterPickerSheet(
    sheetState: SheetState,
    reciters: List<Reciter>,
    selectedReciterId: String,
    previewController: AudioPreviewController,
    onSelectReciter: (Reciter) -> Unit,
    onDismiss: () -> Unit
) {
    val previewState by previewController.previewState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            previewController.stopPreview()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            previewController.stopPreview()
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.reciter_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                items(reciters, key = { it.id }) { reciter ->
                    val isSelected = (reciter.id == selectedReciterId)
                    val isPreviewingThis = when (val s = previewState) {
                        is PreviewState.Playing -> s.reciterId == reciter.id
                        is PreviewState.Buffering -> s.reciterId == reciter.id
                        else -> false
                    }
                    val isBufferingThis = (previewState as? PreviewState.Buffering)?.reciterId == reciter.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                previewController.stopPreview()
                                onSelectReciter(reciter)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("reciter_row_${reciter.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                previewController.stopPreview()
                                onSelectReciter(reciter)
                            },
                            modifier = Modifier.testTag("reciter_radio_${reciter.id}")
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reciter.displayName(true),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${reciter.nameEn} · ${reciter.style} · ${reciter.bitrate} kbps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if ((previewState as? PreviewState.Error)?.reciterId == reciter.id) {
                                val errorMsg = (previewState as PreviewState.Error).message
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 48dp sample voice button
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            color = if (isPreviewingThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                previewController.playPreview(reciter)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isBufferingThis) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPreviewingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewingThis) stringResource(R.string.stop_sample) else stringResource(R.string.play_sample),
                                        tint = if (isPreviewingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

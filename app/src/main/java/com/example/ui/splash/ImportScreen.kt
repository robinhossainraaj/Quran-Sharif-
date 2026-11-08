package com.example.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.import.ImportState
import com.example.util.BanglaDigits

// Fixed colours on purpose (same on dark and light theme).
private val Cream = Color(0xFFFFF8E6)
private val Gold = Color(0xFFE3B84A)
private val TrackColor = Color(0xFF0C4A27)
private val TrackBorder = Color(0xFF2F7A4B)
private val FillBrush = Brush.verticalGradient(
    listOf(Color(0xFFF3D77C), Color(0xFFD9A93A), Color(0xFFB98A22))
)
private val RajdhaniFont = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold)
)

// Position of the tagline in the artwork: 1060 px of 1870 px from the top.
private const val TAGLINE_CENTER_FRACTION = 1060f / 1870f

@Composable
fun ImportScreen(
    importState: ImportState,
    onRetry: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF033815))
    ) {
        // The artwork (lantern, arch, emblem, name, mosque) is the exact picture.
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Tagline, loading bar and loading text. Everything is on one centre line.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = maxHeight * TAGLINE_CENTER_FRACTION - 16.dp)
        ) {
            Text(
                text = stringResource(R.string.import_tagline),
                fontFamily = RajdhaniFont,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
                color = Cream,
                textAlign = TextAlign.Center
            )

            // Space between the tagline and the loading bar: 25
            Spacer(modifier = Modifier.height(25.dp))

            when (importState) {
                is ImportState.Error -> {
                    Text(
                        text = stringResource(R.string.import_failed),
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Color(0xFFFFB4AB),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = importState.message,
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Cream.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Color(0xFF042A14)
                        ),
                        modifier = Modifier.testTag("import_retry_button")
                    ) {
                        Text(
                            text = stringResource(R.string.retry),
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                }
                else -> {
                    val progressFloat = if (importState is ImportState.Importing && importState.total > 0) {
                        (importState.progress.toFloat() / importState.total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.73f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TrackColor)
                            .border(1.5.dp, TrackBorder, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                            .testTag("import_progress_indicator")
                    ) {
                        if (progressFloat > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFloat)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(FillBrush)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val message = if (importState is ImportState.Importing) {
                        val progressText = "${BanglaDigits.convert(importState.progress)} / ${BanglaDigits.convert(importState.total)}"
                        stringResource(R.string.import_subtitle, importState.progress).replace(
                            "${importState.progress}/114",
                            progressText
                        )
                    } else {
                        stringResource(R.string.checking_data)
                    }
                    Text(
                        text = message,
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        color = Cream,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

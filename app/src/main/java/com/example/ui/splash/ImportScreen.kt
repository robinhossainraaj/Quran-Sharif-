package com.example.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.import.ImportState
import com.example.util.BanglaDigits

// Fixed green and gold colours on purpose (same on dark and light theme).
private val GreenTop = Color(0xFF0B5A2B)
private val GreenMid = Color(0xFF063D1D)
private val GreenBottom = Color(0xFF042A14)
private val SkylineColor = Color(0xFF052E17)
private val Gold = Color(0xFFE3B84A)
private val GoldLight = Color(0xFFF0CB6A)
private val Cream = Color(0xFFFFF8E6)
private val SoftText = Color(0xFFE9DFC2)
private val TrackColor = Color(0x2EFFF8E6)

@Composable
fun ImportScreen(
    importState: ImportState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GreenTop, GreenMid, GreenBottom)))
    ) {
        ArchDecoration(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(170.dp)
        )
        MosqueSkyline(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
        )

        // Centre block: emblem, name, divider, tagline (all on one centre line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 110.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .background(GreenMid, CircleShape)
                    .border(3.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notification),
                    contentDescription = stringResource(R.string.app_name),
                    tint = Cream,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            val fullName = stringResource(R.string.app_name_full)
            val firstWord = fullName.substringBefore(' ')
            val otherWords = fullName.substringAfter(' ', "")
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Cream)) { append(firstWord) }
                    if (otherWords.isNotEmpty()) {
                        append(" ")
                        withStyle(SpanStyle(color = Gold)) { append(otherWords) }
                    }
                },
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 44.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(220.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Gold.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                StarMark(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Gold.copy(alpha = 0.8f))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.import_tagline),
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                color = SoftText,
                textAlign = TextAlign.Center
            )
        }

        // Loading block: lower part of the screen, centred
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 64.dp, end = 64.dp, bottom = 190.dp)
        ) {
            when (importState) {
                is ImportState.Importing -> {
                    val progressFloat = (importState.progress.toFloat() / importState.total.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .testTag("import_progress_indicator"),
                        color = GoldLight,
                        trackColor = TrackColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val progressText = "${BanglaDigits.convert(importState.progress)} / ${BanglaDigits.convert(importState.total)}"
                    Text(
                        text = stringResource(R.string.import_subtitle, importState.progress).replace(
                            "${importState.progress}/114",
                            progressText
                        ),
                        fontSize = 13.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center
                    )
                }
                is ImportState.Error -> {
                    Text(
                        text = stringResource(R.string.import_failed),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFB4AB),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = importState.message,
                        fontSize = 13.sp,
                        color = SoftText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = GreenBottom
                        ),
                        modifier = Modifier.testTag("import_retry_button")
                    ) {
                        Text(
                            text = stringResource(R.string.retry),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.checking_data),
                        fontSize = 13.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 390f
        val sy = size.height / 170f
        fun arch(dy: Float): Path = Path().apply {
            moveTo(-10f * sx, (130f + dy) * sy)
            cubicTo(70f * sx, (130f + dy) * sy, 120f * sx, (80f + dy) * sy, 195f * sx, (12f + dy) * sy)
            cubicTo(270f * sx, (80f + dy) * sy, 320f * sx, (130f + dy) * sy, 400f * sx, (130f + dy) * sy)
        }
        drawPath(arch(0f), Gold, style = Stroke(width = 3.dp.toPx()))
        drawPath(arch(12f), Gold.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun StarMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.width / 18f
        val path = Path().apply {
            moveTo(9f * s, 1f * s)
            lineTo(11f * s, 7f * s)
            lineTo(17f * s, 9f * s)
            lineTo(11f * s, 11f * s)
            lineTo(9f * s, 17f * s)
            lineTo(7f * s, 11f * s)
            lineTo(1f * s, 9f * s)
            lineTo(7f * s, 7f * s)
            close()
        }
        drawPath(path, Gold)
    }
}

@Composable
private fun MosqueSkyline(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 390f
        val sy = size.height / 150f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x8C7BD34A), Color.Transparent),
                center = Offset(size.width / 2f, size.height),
                radius = size.width * 0.6f
            )
        )
        val path = Path()
        fun move(x: Float, y: Float) = path.moveTo(x * sx, y * sy)
        fun line(x: Float, y: Float) = path.lineTo(x * sx, y * sy)
        fun curve(cx: Float, cy: Float, x: Float, y: Float) =
            path.quadraticBezierTo(cx * sx, cy * sy, x * sx, y * sy)

        move(0f, 150f)
        line(0f, 118f); line(18f, 118f)
        line(18f, 92f); line(22f, 84f); line(26f, 92f); line(26f, 118f)
        line(60f, 118f); curve(70f, 98f, 80f, 118f)
        line(110f, 118f)
        line(110f, 100f); line(114f, 94f); line(118f, 100f); line(118f, 118f)
        line(140f, 118f); curve(150f, 100f, 160f, 118f)
        line(165f, 118f); curve(195f, 62f, 225f, 118f)
        line(230f, 118f); curve(240f, 100f, 250f, 118f)
        line(272f, 118f)
        line(272f, 100f); line(276f, 94f); line(280f, 100f); line(280f, 118f)
        line(310f, 118f); curve(320f, 98f, 330f, 118f)
        line(364f, 118f)
        line(364f, 92f); line(368f, 84f); line(372f, 92f); line(372f, 118f)
        line(390f, 118f); line(390f, 150f)
        path.close()
        drawPath(path, SkylineColor)

        drawLine(
            color = Gold.copy(alpha = 0.6f),
            start = Offset(0f, size.height - 1.dp.toPx()),
            end = Offset(size.width, size.height - 1.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
}

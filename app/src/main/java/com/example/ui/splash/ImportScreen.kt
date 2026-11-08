package com.example.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.import.ImportState
import com.example.util.BanglaDigits

@Composable
fun ImportScreen(
    importState: ImportState,
    onRetry: () -> Unit
) {
    val bgColor = Color(0xFF14352D)
    val goldColor = Color(0xFFF4E7BF)
    val textColor = Color(0xFFE3EEEA)
    val subColor = Color(0xFF9AD3C0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Book Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF1B4A3F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notification),
                    contentDescription = stringResource(R.string.app_name),
                    tint = goldColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name_full),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = goldColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.app_name_bn),
                fontSize = 20.sp,
                color = subColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            when (importState) {
                is ImportState.Importing -> {
                    val progressFloat = (importState.progress.toFloat() / importState.total.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp)
                            .testTag("import_progress_indicator"),
                        color = goldColor,
                        trackColor = Color(0xFF1B4A3F)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val progressText = "${BanglaDigits.convert(importState.progress)} / ${BanglaDigits.convert(importState.total)}"
                    Text(
                        text = stringResource(R.string.import_subtitle, importState.progress).replace(
                            "${importState.progress}/114",
                            progressText
                        ),
                        fontSize = 15.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
                is ImportState.Error -> {
                    Text(
                        text = stringResource(R.string.import_failed),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = importState.message,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = goldColor,
                            contentColor = Color(0xFF0E1A18)
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
                        fontSize = 15.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

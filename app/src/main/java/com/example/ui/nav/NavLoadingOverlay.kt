package com.example.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import kotlinx.coroutines.delay

/**
 * A very short green + gold loading animation shown whenever the user moves from one screen to
 * another. It never blocks touches (no click handler) and disappears by itself.
 */
@Composable
fun NavLoadingOverlay(currentRoute: String?) {
    var visible by remember { mutableStateOf(false) }
    var lastRoute by remember { mutableStateOf(currentRoute) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_gold))

    LaunchedEffect(currentRoute) {
        if (currentRoute != lastRoute) {
            lastRoute = currentRoute
            visible = true
            delay(380)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC033815)),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

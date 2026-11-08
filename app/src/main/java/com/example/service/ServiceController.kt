package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object ServiceController {

    fun sync(context: Context, enabled: Boolean) {
        val intent = Intent(context, ScreenWatcherService::class.java)
        try {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        } catch (e: Exception) {
            // Some OEM battery savers or background restrictions may prevent starting the service
        }
    }
}

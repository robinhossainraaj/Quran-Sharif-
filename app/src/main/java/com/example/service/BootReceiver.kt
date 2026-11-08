package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.QuranApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? QuranApp
                    val settings = withTimeoutOrNull(3000) {
                        app?.container?.settingsRepository?.settingsFlow?.first()
                    }
                    if (settings?.widgetRefreshOnScreenOn == true) {
                        ServiceController.sync(context, true)
                    }
                } catch (e: Exception) {
                    // Ignore background start errors
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

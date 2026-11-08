package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.service.ServiceController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuranApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Asynchronously check and import data on app startup
        container.applicationScope.launch {
            container.quranImporter.checkAndImport()
        }
    }
}

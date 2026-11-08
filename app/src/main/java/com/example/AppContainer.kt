package com.example

import android.content.Context
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.audio.download.AudioDownloadManager
import com.example.audio.player.PlaybackController
import com.example.audio.preview.AudioPreviewController
import com.example.audio.storage.AudioStorage
import com.example.data.QuranRepository
import com.example.data.db.QuranDatabase
import com.example.data.import.QuranImporter
import com.example.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: QuranDatabase by lazy {
        QuranDatabase.getInstance(context)
    }

    val quranRepository: QuranRepository by lazy {
        QuranRepository(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val quranImporter: QuranImporter by lazy {
        QuranImporter(context, database, settingsRepository)
    }

    val audioStorage: AudioStorage by lazy {
        AudioStorage(context)
    }

    val audioDownloadManager: AudioDownloadManager by lazy {
        AudioDownloadManager(context, audioStorage, settingsRepository, applicationScope)
    }

    val audioPreviewController: AudioPreviewController by lazy {
        val catalog = ReciterCatalogLoader.load(context)
        AudioPreviewController(context, catalog.cdnBase)
    }

    val playbackController: PlaybackController by lazy {
        PlaybackController(context, audioStorage, quranRepository, settingsRepository, applicationScope)
    }
}

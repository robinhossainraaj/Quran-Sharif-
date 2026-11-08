package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.import.ImportState
import com.example.ui.nav.AppNavHost
import com.example.ui.nav.DeepLinkTarget
import com.example.ui.splash.ImportScreen
import com.example.ui.theme.QuranSharifTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var deepLinkTarget by mutableStateOf<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition {
                val app = applicationContext as? QuranApp
                app?.container?.quranImporter?.importState?.value is ImportState.Checking
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "SplashScreen installation fallback", e)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QuranApp
        val container = app.container

        // Process potential deep link from widget
        processIntentExtras(intent)

        setContent {
            val importState by container.quranImporter.importState.collectAsStateWithLifecycle()
            val settings by container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.prefs.UserSettings()
            )

            QuranSharifTheme(themeMode = settings.themeMode) {
                when (importState) {
                    is ImportState.Importing, is ImportState.Error -> {
                        ImportScreen(
                            importState = importState,
                            onRetry = {
                                lifecycleScope.launch {
                                    container.quranImporter.checkAndImport(force = true)
                                }
                            }
                        )
                    }
                    is ImportState.Ready -> {
                        AppNavHost(
                            container = container,
                            deepLinkTarget = deepLinkTarget,
                            onDeepLinkConsumed = {
                                deepLinkTarget = null
                                intent.removeExtra(EXTRA_SURAH_ID)
                                intent.removeExtra(EXTRA_AYAH_NO)
                            }
                        )
                    }
                    is ImportState.Checking, is ImportState.Idle -> {
                        // Handled by system splash screen keep-on-screen condition
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntentExtras(intent)
    }

    private fun processIntentExtras(intent: Intent?) {
        if (intent == null) return
        val surahId = intent.getIntExtra(EXTRA_SURAH_ID, -1)
        val ayahNo = intent.getIntExtra(EXTRA_AYAH_NO, -1)
        if (surahId > 0) {
            deepLinkTarget = DeepLinkTarget(surahId = surahId, ayahNo = ayahNo)
        }
    }

    companion object {
        const val EXTRA_SURAH_ID = "EXTRA_SURAH_ID"
        const val EXTRA_AYAH_NO = "EXTRA_AYAH_NO"
    }
}

package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.import.DataSourceInfo
import com.example.widget.WidgetPrefs
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_settings")

data class UserSettings(
    val themeMode: String = "SYSTEM",             // "SYSTEM", "LIGHT", "DARK"
    val appLanguage: String = "",                 // "", "en", "bn"
    val arabicSizeSp: Int = 30,
    val banglaSizeSp: Int = 16,
    val showBangla: Boolean = true,
    val banglaDigits: Boolean = true,
    val keepScreenOn: Boolean = false,
    val widgetRefreshOnScreenOn: Boolean = true,
    val lastReadSurah: Int = 0,
    val lastReadAyah: Int = 0,
    val importDoneV1: Boolean = false,
    val importDoneV2: Boolean = false,
    val translationId: String = WidgetPrefs.DEFAULT_TRANSLATION_ID,
    val widgetArabicScalePct: Int = 100,
    val widgetTranslationScalePct: Int = 100,
    val translationsCatalogHash: String = "",
    val dataSourceInfo: DataSourceInfo = DataSourceInfo(),
    val audioReciterId: String = "alafasy",
    val audioWifiOnly: Boolean = true,
    val audioBismillahPrefix: Boolean = true,
    val audioFollow: Boolean = true,
    val audioSpeed: Float = 1.0f,
    val audioRepeatMode: String = "OFF",
    val mobileDataWarningShown: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ARABIC_SIZE_SP = intPreferencesKey("arabic_size_sp")
        val BANGLA_SIZE_SP = intPreferencesKey("bangla_size_sp")
        val SHOW_BANGLA = booleanPreferencesKey("show_bangla")
        val BANGLA_DIGITS = booleanPreferencesKey("bangla_digits")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val WIDGET_REFRESH_ON_SCREEN_ON = booleanPreferencesKey("widget_refresh_on_screen_on")
        val LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val IMPORT_DONE_V1 = booleanPreferencesKey("import_done_v1")
        val IMPORT_DONE_V2 = booleanPreferencesKey("import_done_v2")

        val TRANSLATION_ID = stringPreferencesKey("translation_id")
        val WIDGET_ARABIC_SCALE_PCT = intPreferencesKey("widget_arabic_scale_pct")
        val WIDGET_TRANSLATION_SCALE_PCT = intPreferencesKey("widget_translation_scale_pct")
        val TRANSLATIONS_CATALOG_HASH = stringPreferencesKey("translations_catalog_hash")

        val TANZIL_CREDIT = stringPreferencesKey("ds_tanzil_credit")
        val TANZIL_LICENSE = stringPreferencesKey("ds_tanzil_license")
        val TRANS_CREDIT = stringPreferencesKey("ds_trans_credit")
        val TRANS_LICENSE = stringPreferencesKey("ds_trans_license")

        val AUDIO_RECITER_ID = stringPreferencesKey("audio_reciter_id")
        val AUDIO_WIFI_ONLY = booleanPreferencesKey("audio_wifi_only")
        val AUDIO_BISMILLAH_PREFIX = booleanPreferencesKey("audio_bismillah_prefix")
        val AUDIO_FOLLOW = booleanPreferencesKey("audio_follow")
        val AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val AUDIO_REPEAT_MODE = stringPreferencesKey("audio_repeat_mode")
        val MOBILE_DATA_WARNING_SHOWN = booleanPreferencesKey("mobile_data_warning_shown")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val transId = prefs[PreferencesKeys.TRANSLATION_ID] ?: WidgetPrefs.DEFAULT_TRANSLATION_ID
            val arScale = prefs[PreferencesKeys.WIDGET_ARABIC_SCALE_PCT] ?: 100
            val trScale = prefs[PreferencesKeys.WIDGET_TRANSLATION_SCALE_PCT] ?: 100

            // Ensure WidgetPrefs has these mirrored for widget background access
            WidgetPrefs.setTranslationId(context, transId)
            WidgetPrefs.setArabicScalePct(context, arScale)
            WidgetPrefs.setTranslationScalePct(context, trScale)

            UserSettings(
                themeMode = prefs[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                appLanguage = prefs[PreferencesKeys.APP_LANGUAGE] ?: "",
                arabicSizeSp = prefs[PreferencesKeys.ARABIC_SIZE_SP] ?: 30,
                banglaSizeSp = prefs[PreferencesKeys.BANGLA_SIZE_SP] ?: 16,
                showBangla = prefs[PreferencesKeys.SHOW_BANGLA] ?: true,
                banglaDigits = prefs[PreferencesKeys.BANGLA_DIGITS] ?: true,
                keepScreenOn = prefs[PreferencesKeys.KEEP_SCREEN_ON] ?: false,
                widgetRefreshOnScreenOn = prefs[PreferencesKeys.WIDGET_REFRESH_ON_SCREEN_ON] ?: true,
                lastReadSurah = prefs[PreferencesKeys.LAST_READ_SURAH] ?: 0,
                lastReadAyah = prefs[PreferencesKeys.LAST_READ_AYAH] ?: 0,
                importDoneV1 = prefs[PreferencesKeys.IMPORT_DONE_V1] ?: false,
                importDoneV2 = prefs[PreferencesKeys.IMPORT_DONE_V2] ?: false,
                translationId = transId,
                widgetArabicScalePct = arScale,
                widgetTranslationScalePct = trScale,
                translationsCatalogHash = prefs[PreferencesKeys.TRANSLATIONS_CATALOG_HASH] ?: "",
                dataSourceInfo = DataSourceInfo(
                    tanzilTextCredit = prefs[PreferencesKeys.TANZIL_CREDIT] ?: "",
                    tanzilLicense = prefs[PreferencesKeys.TANZIL_LICENSE] ?: "",
                    translationCredit = prefs[PreferencesKeys.TRANS_CREDIT] ?: "",
                    translationLicense = prefs[PreferencesKeys.TRANS_LICENSE] ?: ""
                ),
                audioReciterId = prefs[PreferencesKeys.AUDIO_RECITER_ID] ?: "alafasy",
                audioWifiOnly = prefs[PreferencesKeys.AUDIO_WIFI_ONLY] ?: true,
                audioBismillahPrefix = prefs[PreferencesKeys.AUDIO_BISMILLAH_PREFIX] ?: true,
                audioFollow = prefs[PreferencesKeys.AUDIO_FOLLOW] ?: true,
                audioSpeed = prefs[PreferencesKeys.AUDIO_SPEED] ?: 1.0f,
                audioRepeatMode = prefs[PreferencesKeys.AUDIO_REPEAT_MODE] ?: "OFF",
                mobileDataWarningShown = prefs[PreferencesKeys.MOBILE_DATA_WARNING_SHOWN] ?: false
            )
        }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { it[PreferencesKeys.APP_LANGUAGE] = lang }
    }

    suspend fun setArabicSizeSp(size: Int) {
        context.dataStore.edit { it[PreferencesKeys.ARABIC_SIZE_SP] = size.coerceIn(20, 48) }
    }

    suspend fun setBanglaSizeSp(size: Int) {
        context.dataStore.edit { it[PreferencesKeys.BANGLA_SIZE_SP] = size.coerceIn(12, 26) }
    }

    suspend fun setShowBangla(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_BANGLA] = show }
    }

    suspend fun setBanglaDigits(useBangla: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BANGLA_DIGITS] = useBangla }
    }

    suspend fun setKeepScreenOn(keepOn: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = keepOn }
    }

    suspend fun setWidgetRefreshOnScreenOn(refresh: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WIDGET_REFRESH_ON_SCREEN_ON] = refresh }
    }

    suspend fun setTranslationId(id: String) {
        WidgetPrefs.setTranslationId(context, id)
        context.dataStore.edit { it[PreferencesKeys.TRANSLATION_ID] = id }
    }

    suspend fun setWidgetArabicScalePct(scalePct: Int) {
        val clamped = scalePct.coerceIn(70, 150)
        WidgetPrefs.setArabicScalePct(context, clamped)
        context.dataStore.edit { it[PreferencesKeys.WIDGET_ARABIC_SCALE_PCT] = clamped }
    }

    suspend fun setWidgetTranslationScalePct(scalePct: Int) {
        val clamped = scalePct.coerceIn(70, 150)
        WidgetPrefs.setTranslationScalePct(context, clamped)
        context.dataStore.edit { it[PreferencesKeys.WIDGET_TRANSLATION_SCALE_PCT] = clamped }
    }

    suspend fun resetWidgetTextSize() {
        WidgetPrefs.setArabicScalePct(context, 100)
        WidgetPrefs.setTranslationScalePct(context, 100)
        context.dataStore.edit {
            it[PreferencesKeys.WIDGET_ARABIC_SCALE_PCT] = 100
            it[PreferencesKeys.WIDGET_TRANSLATION_SCALE_PCT] = 100
        }
    }

    suspend fun setLastRead(surahId: Int, ayahNo: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.LAST_READ_SURAH] = surahId
            it[PreferencesKeys.LAST_READ_AYAH] = ayahNo
        }
    }

    suspend fun resetLastRead() {
        context.dataStore.edit {
            it[PreferencesKeys.LAST_READ_SURAH] = 0
            it[PreferencesKeys.LAST_READ_AYAH] = 0
        }
    }

    suspend fun setImportDone(done: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IMPORT_DONE_V1] = done }
    }

    suspend fun setImportDoneV2(done: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IMPORT_DONE_V2] = done }
    }

    suspend fun setTranslationsCatalogHash(hash: String) {
        context.dataStore.edit { it[PreferencesKeys.TRANSLATIONS_CATALOG_HASH] = hash }
    }

    suspend fun setDataSourceInfo(info: DataSourceInfo) {
        context.dataStore.edit {
            it[PreferencesKeys.TANZIL_CREDIT] = info.tanzilTextCredit
            it[PreferencesKeys.TANZIL_LICENSE] = info.tanzilLicense
            it[PreferencesKeys.TRANS_CREDIT] = info.translationCredit
            it[PreferencesKeys.TRANS_LICENSE] = info.translationLicense
        }
    }

    suspend fun setAudioReciterId(reciterId: String) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_RECITER_ID] = reciterId }
    }

    suspend fun setAudioWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_WIFI_ONLY] = wifiOnly }
    }

    suspend fun setAudioBismillahPrefix(prefix: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_BISMILLAH_PREFIX] = prefix }
    }

    suspend fun setAudioFollow(follow: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_FOLLOW] = follow }
    }

    suspend fun setAudioSpeed(speed: Float) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_SPEED] = speed }
    }

    suspend fun setAudioRepeatMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_REPEAT_MODE] = mode }
    }

    suspend fun setMobileDataWarningShown(shown: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.MOBILE_DATA_WARNING_SHOWN] = shown }
    }
}

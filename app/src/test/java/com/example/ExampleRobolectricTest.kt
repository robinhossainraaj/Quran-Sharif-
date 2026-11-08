package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.QuranDatabase
import com.example.data.db.TranslationInfoEntity
import com.example.data.db.TranslationTextEntity
import com.example.widget.WidgetPrefs
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: QuranDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("quran_widget_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        context.getSharedPreferences("quran_widget_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun readStringFromContext() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Quran Sharif", appName)
    }

    @Test
    fun testWidgetPrefsDefaultsAndUpdates() {
        // Verify default
        val initialId = WidgetPrefs.getTranslationId(context)
        assertEquals("bn_muhiuddin", initialId)
        assertEquals(100, WidgetPrefs.getArabicScalePct(context))
        assertEquals(100, WidgetPrefs.getTranslationScalePct(context))

        // Update and verify
        WidgetPrefs.setTranslationId(context, "bn_hoque")
        val updatedId = WidgetPrefs.getTranslationId(context)
        assertEquals("bn_hoque", updatedId)

        WidgetPrefs.setArabicScalePct(context, 120)
        assertEquals(120, WidgetPrefs.getArabicScalePct(context))

        WidgetPrefs.setTranslationScalePct(context, 85)
        assertEquals(85, WidgetPrefs.getTranslationScalePct(context))
    }

    @Test
    fun testTranslationDatabaseOperations() = runBlocking {
        val dao = db.quranDao()

        val info = TranslationInfoEntity(
            id = "bn_hoque",
            nameEn = "Zohurul Hoque",
            nameBn = "জহুরুল হক",
            translator = "Dr. Zohurul Hoque",
            source = "Tanzil Project",
            version = "1.0",
            licenseNote = "Non-commercial",
            licenseText = "Standard non-commercial",
            ayahCount = 6236,
            isDefault = false
        )
        dao.insertTranslationInfo(info)

        val retrievedInfo = dao.translationInfo("bn_hoque")
        assertNotNull(retrievedInfo)
        assertEquals("Zohurul Hoque", retrievedInfo?.nameEn)

        dao.insertTranslationTexts(
            listOf(
                TranslationTextEntity("bn_hoque", 1, "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে"),
                TranslationTextEntity("bn_hoque", 2, "যাবতীয় প্রশংসা আল্লাহরই জন্য যিনি সকল সৃষ্টির পালনকর্তা")
            )
        )

        assertEquals(2, dao.translationTextCount("bn_hoque"))
    }
}

package com.example.data.import

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.R
import com.example.data.db.AyahEntity
import com.example.data.db.JuzEntity
import com.example.data.db.QuranDatabase
import com.example.data.db.SurahEntity
import com.example.data.db.TranslationInfoEntity
import com.example.data.db.TranslationTextEntity
import com.example.data.prefs.SettingsRepository
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray

sealed class ImportState {
    object Idle : ImportState()
    object Checking : ImportState()
    data class Importing(val progress: Int, val total: Int = 114) : ImportState()
    object Ready : ImportState()
    data class Error(val message: String) : ImportState()
}

class QuranImporter(
    private val context: Context,
    private val database: QuranDatabase,
    private val settingsRepository: SettingsRepository
) {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Checking)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    suspend fun checkAndImport(force: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            _importState.value = ImportState.Checking
            val quranDao = database.quranDao()
            val settings = settingsRepository.settingsFlow.first()

            val baseDataReady = !force && settings.importDoneV1 &&
                    quranDao.ayahCount() == 6236 && quranDao.surahCount() == 114

            if (!baseDataReady) {
                executeImport()
            }

            // Next, verify / import multiple translations
            checkAndImportTranslations(force)

            _importState.value = ImportState.Ready
        } catch (e: Exception) {
            Log.e("QuranImporter", "Import failed", e)
            _importState.value = ImportState.Error(e.localizedMessage ?: "Unknown import error")
        }
    }

    private suspend fun executeImport() = withContext(Dispatchers.IO) {
        _importState.value = ImportState.Importing(0, 114)
        val assets = context.assets

        // 1. Parse text files
        val uthmaniResult = assets.open("quran-uthmani.txt").use {
            TanzilParser.parse(it, "quran-uthmani.txt")
        }
        val cleanResult = assets.open("quran-simple-clean.txt").use {
            TanzilParser.parse(it, "quran-simple-clean.txt")
        }
        val banglaResult = assets.open("bn.bengali.txt").use {
            TanzilParser.parse(it, "bn.bengali.txt")
        }

        // 2. Parse XML metadata
        val xmlResult = assets.open("quran-data.xml").use {
            QuranDataXmlParser.parse(it)
        }

        // 3. Verify invariants (Section 4.2)
        verifyInvariants(uthmaniResult.ayahs, cleanResult.ayahs, banglaResult.ayahs, xmlResult)

        // 4. Extract Bismillah normalization target from 1:1
        val surah1Ayah1 = uthmaniResult.ayahs.first { it.surahId == 1 && it.ayahNo == 1 }
        val normalizedBismillah = ArabicNormalizer.normalize(surah1Ayah1.text)

        // 5. Build entities
        val surahNamesBn = context.resources.getStringArray(R.array.surah_names_bn)
        require(surahNamesBn.size == 114) {
            "surah_names_bn string array must contain exactly 114 names, found ${surahNamesBn.size}"
        }

        val surahEntities = xmlResult.suras.map { meta ->
            SurahEntity(
                id = meta.index,
                arabicName = meta.name,
                transliteration = meta.tname,
                englishMeaning = meta.ename,
                banglaName = surahNamesBn[meta.index - 1],
                revelationType = meta.type,
                revelationOrder = meta.order,
                ayahCount = meta.ayas
            )
        }

        val ayahEntities = ArrayList<AyahEntity>(6236)
        val ayahIdMap = HashMap<Pair<Int, Int>, Int>(6236)

        for (i in 0 until 6236) {
            val globalId = i + 1
            val uthmani = uthmaniResult.ayahs[i]
            val clean = cleanResult.ayahs[i]
            val bangla = banglaResult.ayahs[i]

            val displayArabic = BismillahHelper.extractDisplayArabic(
                surahId = uthmani.surahId,
                ayahNo = uthmani.ayahNo,
                arabicText = uthmani.text,
                normalizedBismillah = normalizedBismillah
            )
            val searchArabic = ArabicNormalizer.normalize(clean.text)

            val entity = AyahEntity(
                id = globalId,
                surahId = uthmani.surahId,
                ayahNo = uthmani.ayahNo,
                arabicOriginal = uthmani.text,
                arabicDisplay = displayArabic,
                arabicSearch = searchArabic,
                bangla = bangla.text
            )
            ayahEntities.add(entity)
            ayahIdMap[Pair(uthmani.surahId, uthmani.ayahNo)] = globalId
        }

        // Build Juz entities with accurate startAyahId
        val juzEntities = xmlResult.juzs.map { juzMeta ->
            val startAyahId = ayahIdMap[Pair(juzMeta.sura, juzMeta.aya)]
                ?: throw IllegalStateException("Juz ${juzMeta.index} references nonexistent ayah ${juzMeta.sura}:${juzMeta.aya}")
            JuzEntity(
                index = juzMeta.index,
                startSurah = juzMeta.sura,
                startAyah = juzMeta.aya,
                startAyahId = startAyahId
            )
        }

        // 6. Insert inside single database transaction
        val quranDao = database.quranDao()
        database.withTransaction {
            quranDao.clearSurahs()
            quranDao.clearAyahs()
            quranDao.clearJuzs()

            quranDao.insertSurahs(surahEntities)
            quranDao.insertJuzs(juzEntities)

            // Insert ayahs in batches per surah for progress emission
            val ayahsBySurah = ayahEntities.groupBy { it.surahId }
            for (surahId in 1..114) {
                val batch = ayahsBySurah[surahId].orEmpty()
                quranDao.insertAyahs(batch)
                _importState.value = ImportState.Importing(surahId, 114)
            }
        }

        // 7. Store DataSourceInfo comments from files
        val tanzilComments = uthmaniResult.comments.joinToString("\n")
        val banglaComments = banglaResult.comments.joinToString("\n")
        val dsInfo = DataSourceInfo(
            tanzilTextCredit = "Tanzil Project (tanzil.net)",
            tanzilLicense = tanzilComments,
            translationCredit = "Bengali Translation: Muhiuddin Khan (via Tanzil.net)",
            translationLicense = banglaComments
        )
        settingsRepository.setDataSourceInfo(dsInfo)
        settingsRepository.setImportDone(true)
    }

    private suspend fun checkAndImportTranslations(force: Boolean = false) = withContext(Dispatchers.IO) {
        val quranDao = database.quranDao()
        val settings = settingsRepository.settingsFlow.first()
        val assets = context.assets

        val jsonString = assets.open("translations/translations.json").bufferedReader().use { it.readText() }
        val catalogHash = sha256(jsonString)

        if (!force && settings.importDoneV2 && settings.translationsCatalogHash == catalogHash && quranDao.translationInfoCount() >= 2) {
            // Check that default translation has 6236 entries
            if (quranDao.translationTextCount("bn_muhiuddin") == 6236 && quranDao.translationTextCount("bn_hoque") == 6236) {
                return@withContext
            }
        }

        val jsonArray = JSONArray(jsonString)
        val validInfos = mutableListOf<TranslationInfoEntity>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val id = item.getString("id")
            val filePath = item.getString("file")
            val nameEn = item.getString("nameEn")
            val nameBn = item.getString("nameBn")
            val translator = item.getString("translator")
            val source = item.getString("source")
            val version = item.optString("version", "1.0")
            val licenseNote = item.optString("licenseNote", "")
            val licenseText = item.optString("licenseText", "")
            val ayahCount = item.optInt("ayahCount", 6236)
            val isDefault = item.optBoolean("default", false)

            try {
                val parsedResult = assets.open(filePath).use { stream ->
                    TanzilParser.parse(stream, filePath)
                }

                if (parsedResult.ayahs.size != 6236) {
                    Log.w("QuranImporter", "Translation $id in $filePath has ${parsedResult.ayahs.size} lines, expected 6236. Skipping.")
                    continue
                }

                // Verify line mapping
                var validOrder = true
                var expectedGlobalId = 1
                for (a in parsedResult.ayahs) {
                    if (a.text.isBlank()) {
                        validOrder = false
                        break
                    }
                    expectedGlobalId++
                }

                if (!validOrder) {
                    Log.w("QuranImporter", "Translation $id has blank lines. Skipping.")
                    continue
                }

                val infoEntity = TranslationInfoEntity(
                    id = id,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    translator = translator,
                    source = source,
                    version = version,
                    licenseNote = licenseNote,
                    licenseText = licenseText,
                    ayahCount = ayahCount,
                    isDefault = isDefault
                )

                val textEntities = ArrayList<TranslationTextEntity>(6236)
                for ((idx, a) in parsedResult.ayahs.withIndex()) {
                    textEntities.add(
                        TranslationTextEntity(
                            translationId = id,
                            ayahId = idx + 1,
                            text = a.text
                        )
                    )
                }

                database.withTransaction {
                    quranDao.insertTranslationInfo(infoEntity)
                    quranDao.clearTranslationTexts(id)
                    quranDao.insertTranslationTexts(textEntities)
                }

                validInfos.add(infoEntity)
                Log.i("QuranImporter", "Successfully imported translation: $id (${infoEntity.nameEn})")
            } catch (e: Exception) {
                Log.e("QuranImporter", "Failed to parse/import translation $id from $filePath", e)
            }
        }

        settingsRepository.setTranslationsCatalogHash(catalogHash)
        settingsRepository.setImportDoneV2(true)

        // If current selection is invalid, reset to default
        val currentTransId = settings.translationId
        if (validInfos.isNotEmpty() && validInfos.none { it.id == currentTransId }) {
            val fallback = validInfos.firstOrNull { it.isDefault }?.id ?: validInfos.first().id
            settingsRepository.setTranslationId(fallback)
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyInvariants(
        uthmani: List<ParsedAyah>,
        clean: List<ParsedAyah>,
        bangla: List<ParsedAyah>,
        xml: QuranXmlResult
    ) {
        // Invariant 1: Exactly 6236 ayah lines in each file
        require(uthmani.size == 6236) { "Uthmani file must have 6236 ayahs, found ${uthmani.size}" }
        require(clean.size == 6236) { "Simple clean file must have 6236 ayahs, found ${clean.size}" }
        require(bangla.size == 6236) { "Bangla file must have 6236 ayahs, found ${bangla.size}" }

        // Invariant 2: Exactly 114 surahs
        require(xml.suras.size == 114) { "XML metadata must have 114 surahs, found ${xml.suras.size}" }

        val surahAyasMap = xml.suras.associate { it.index to it.ayas }

        // Invariant 3: Identical (sura, aya) keys and matching surah counts
        val uthmaniBySurah = uthmani.groupBy { it.surahId }
        val cleanBySurah = clean.groupBy { it.surahId }
        val banglaBySurah = bangla.groupBy { it.surahId }

        for (s in 1..114) {
            val expectedAyas = surahAyasMap[s]
                ?: throw IllegalStateException("Surah $s missing from metadata")

            val uList = uthmaniBySurah[s].orEmpty()
            val cList = cleanBySurah[s].orEmpty()
            val bList = banglaBySurah[s].orEmpty()

            require(uList.size == expectedAyas) {
                "Surah $s in Uthmani text has ${uList.size} ayahs, expected $expectedAyas"
            }
            require(cList.size == expectedAyas) {
                "Surah $s in Simple Clean text has ${cList.size} ayahs, expected $expectedAyas"
            }
            require(bList.size == expectedAyas) {
                "Surah $s in Bangla text has ${bList.size} ayahs, expected $expectedAyas"
            }

            for (a in 1..expectedAyas) {
                val uAyah = uList[a - 1]
                val cAyah = cList[a - 1]
                val bAyah = bList[a - 1]

                require(uAyah.ayahNo == a && cAyah.ayahNo == a && bAyah.ayahNo == a) {
                    "Ayah numbering mismatch in surah $s: expected $a"
                }

                // Invariant 4: No blank text
                require(uAyah.text.isNotBlank()) { "Blank Arabic text at $s:$a" }
                require(cAyah.text.isNotBlank()) { "Blank clean text at $s:$a" }
                require(bAyah.text.isNotBlank()) { "Blank Bangla text at $s:$a" }
            }
        }

        // Invariant 5: Exactly 30 juz entries, juz 1 starts at 1:1
        require(xml.juzs.size == 30) { "XML must contain 30 juzs, found ${xml.juzs.size}" }
        val juz1 = xml.juzs.first()
        require(juz1.index == 1 && juz1.sura == 1 && juz1.aya == 1) {
            "Juz 1 must start at 1:1, found ${juz1.sura}:${juz1.aya}"
        }

        // Spot checks
        require(surahAyasMap[1] == 7)
        require(surahAyasMap[2] == 286)
        require(surahAyasMap[3] == 200)
        require(surahAyasMap[9] == 129)
        require(surahAyasMap[18] == 110)
        require(surahAyasMap[36] == 83)
        require(surahAyasMap[55] == 78)
        require(surahAyasMap[67] == 30)
        require(surahAyasMap[112] == 4)
        require(surahAyasMap[113] == 5)
        require(surahAyasMap[114] == 6)

        val juz2 = xml.juzs[1]
        require(juz2.sura == 2 && juz2.aya == 142) { "Juz 2 start check failed: ${juz2.sura}:${juz2.aya}" }
        val juz3 = xml.juzs[2]
        require(juz3.sura == 2 && juz3.aya == 253) { "Juz 3 start check failed: ${juz3.sura}:${juz3.aya}" }
        val juz29 = xml.juzs[28]
        require(juz29.sura == 67 && juz29.aya == 1) { "Juz 29 start check failed: ${juz29.sura}:${juz29.aya}" }
        val juz30 = xml.juzs[29]
        require(juz30.sura == 78 && juz30.aya == 1) { "Juz 30 start check failed: ${juz30.sura}:${juz30.aya}" }
    }
}

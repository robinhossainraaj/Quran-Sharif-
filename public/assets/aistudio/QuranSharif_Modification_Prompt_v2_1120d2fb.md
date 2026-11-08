# Quran Sharif — Modification Prompt (v2)

> Paste this whole file into Google AI Studio **in the same chat/project where the app was built**, and attach (or paste) the current project files if the AI does not already have them.
> Two changes only: **(A) selectable Bangla translations in Settings** and **(B) adjustable text size in the widget**. Everything else must keep working exactly as it does now.

---

## 0. Before you ask the AI (owner steps)

1. Get the Bangla translations from these **official sources** (I checked each on its own website):

   | Catalog id | Translator | Official source | What you download |
   |---|---|---|---|
   | `bn_muhiuddin` | Muhiuddin Khan | Tanzil — tanzil.net/trans (id `bn.bengali`) | Tanzil text file (`sura\|aya\|text`) |
   | `bn_hoque` | Zohurul Hoque | Tanzil — tanzil.net/trans (id `bn.hoque`) | Tanzil text file (`sura\|aya\|text`) |
   | `bn_zakaria` | Abu Bakr Zakaria | QuranEnc.com (key `bengali_zakaria`) | CSV, XLS or XML from the translation page |

   **Terms you must read and follow (from the sites themselves):**
   - **Tanzil translations:** "for non-commercial purposes only". Commercial use (paid app, ads, etc.) needs permission from the translator or publisher. If an app uses more than three Tanzil translations it must link to tanzil.net/trans. Keep the credit/license `#` lines in the files.
   - **QuranEnc translation:** no modification, addition or deletion of the content; clearly name the publisher and source (QuranEnc.com); mention the version number; keep the transcript/version information with the text; no inappropriate advertisements; update to the latest version from the source.
   - If you will publish on Play Store with ads or a price, the Tanzil translations are not allowed without written permission. Decide this before release.
   - Keep the original text. Never edit or retype translation text, and never ask an AI to type or "fix" it.

2. Tanzil files (`bn.bengali.txt`, `bn.hoque.txt`): download them as they are, they are already in `sura|aya|text` format.

3. QuranEnc file: the app reads only `sura|aya|text`, so convert the downloaded XML/CSV with a small script that **copies the text exactly**. Give the AI this prompt, with the real downloaded QuranEnc file attached (do this before the main prompt):
```
I attached a real translation file downloaded from QuranEnc.com (translation key bengali_zakaria). First open it and show me its first 3 records and its real column/tag names. Then write a Python 3 script convert_quranenc.py that converts it to a UTF-8 text file bn.zakaria.txt with one line per ayah in the form  sura|aya|text.
Rules: copy the translation text byte-for-byte; do not trim inside the text, normalize, fix spelling or re-type anything; only the line break after each record is added. If a text contains a newline, stop and tell me instead of changing it. Do not include footnotes in the text column (tell me clearly how many records had footnotes). The first lines of the output must be "#" comment lines containing: translation name, translator, publisher "QuranEnc.com", the source page URL, the version number and date taken from the file/page metadata (never invented; write "unknown" if the file has none), and the sentence "Text is unmodified; see QuranEnc.com terms of use." The script must verify and print: exactly 6236 records, keys (sura 1..114, aya 1..count) match the standard ayah counts of 114 surahs, no blank text; and exit with an error if any check fails.
```
   Put the resulting `bn.zakaria.txt` in the translations folder. The script and the original download are only for you; they are not part of the app.

4. Put every translation file in `app/src/main/assets/translations/` (new folder). Move the existing `bn.bengali.txt` there too; the catalog below says which path is used.
5. Create `app/src/main/assets/translations/translations.json` (the catalog). Example, edit to match your real files:
```json
[
  {
    "id": "bn_muhiuddin",
    "file": "translations/bn.bengali.txt",
    "nameEn": "Muhiuddin Khan",
    "nameBn": "",
    "translator": "Muhiuddin Khan",
    "source": "Tanzil Project (tanzil.net)",
    "version": "",
    "licenseNote": "Non-commercial use only (Tanzil terms).",
    "default": true
  },
  {
    "id": "bn_hoque",
    "file": "translations/bn.hoque.txt",
    "nameEn": "Zohurul Hoque",
    "nameBn": "",
    "translator": "Zohurul Hoque",
    "source": "Tanzil Project (tanzil.net)",
    "version": "",
    "licenseNote": "Non-commercial use only (Tanzil terms).",
    "default": false
  },
  {
    "id": "bn_zakaria",
    "file": "translations/bn.zakaria.txt",
    "nameEn": "Abu Bakr Zakaria",
    "nameBn": "",
    "translator": "Abu Bakr Zakaria",
    "source": "QuranEnc.com",
    "version": "FILL IN from the QuranEnc page (e.g. the version shown for the download)",
    "licenseNote": "Unmodified text. Publisher: QuranEnc.com. See QuranEnc terms of use.",
    "default": false
  }
]
```
`nameBn` is a UI label only. Leave it empty if unsure; the app then shows `nameEn`. `version` is copied from the source page, never invented. Exactly one entry has `"default": true`. If you do not use a translation, delete its entry (and its file); the app shows only valid ones.

---

## 1. Prompt to give the AI

```
You are modifying my existing, working, fully offline Android app "Quran Sharif" (Kotlin, Jetpack Compose, Material 3, Room, DataStore, Glance widget). Make exactly the two changes below. Do not refactor unrelated code, do not rename packages, do not change the design, and keep every existing feature working. Output every file that changes, in full, each in its own code block headed by its full path (no "...rest unchanged"). At the end, output a list of every file added/changed/deleted and the QA checklist answered line by line.

ABSOLUTE RULES (unchanged from the original brief)
- Never write, generate, correct, complete or paraphrase any Quran Arabic text or any translation text. All text comes only from the asset files. This includes tests, @Preview functions, previews and docs: use placeholders like "ARABIC TEXT" / "বাংলা অনুবাদ".
- No INTERNET permission, no network libraries, no analytics. Fully offline.
- Every new UI string goes in values/strings.xml (English) and values-bn/strings.xml (Bangla). No hard-coded UI strings.
- No new dependencies unless truly unavoidable (if so, name the version and why).

======================================================
CHANGE A — Multiple selectable Bangla translations
======================================================

A1. Data source
- New asset folder: app/src/main/assets/translations/ containing a catalog file translations.json and one Tanzil-format file per translation (lines "sura|aya|text", "#" comment lines, optional UTF-8 BOM on the first line, same parsing rules as the existing TanzilParser).
- translations.json is an array of objects with fields: id (String, unique, lowercase letters/digits/underscore), file (asset path), nameEn, nameBn (may be empty), translator, source, version (String, may be empty), licenseNote (String, may be empty), default (Boolean, exactly one true). The translation_info table also stores version and licenseNote. Parse it with org.json.JSONArray (no new library). Validate: unique ids, exactly one default, every listed file that exists is parsed. A catalog entry whose file is missing from assets is skipped (not shown to the user) and logged, never a crash. If NO listed file exists, fail with a clear error on the import screen.
- The existing "bn.bengali.txt" becomes the translation with id "bn_muhiuddin" (or whatever id the catalog gives it). Keep reading the Arabic files exactly as before.

A2. Database (Room version 1 -> 2, with a REAL migration; never fallbackToDestructiveMigration)
- New entity:
  @Entity(tableName = "translation_text", primaryKeys = ["translationId", "ayahId"], indices = [Index("ayahId")])
  data class TranslationTextEntity(val translationId: String, val ayahId: Int, val text: String)
- New entity "translation_info": id (PK), nameEn, nameBn, translator, source, licenseText (the "#" header/comment lines of that file, stored exactly as in the file), ayahCount.
- Migration_1_2: create both tables; copy every row of ayah.bangla into translation_text under the default translation id (INSERT INTO translation_text SELECT '<defaultId>', id, bangla FROM ayah). Do NOT drop or modify the ayah, surah, juz or bookmark tables. Bookmarks, last-read position and settings must survive the update. Keep the old ayah.bangla column in the entity (it is simply no longer read by the UI) to avoid a risky column-drop migration. Export no schema (exportSchema=false as before) but add a Room migration test or an explicit comment describing the migration.
- Add DAO queries:
  - translations(): Flow<List<TranslationInfoEntity>>
  - ayahsOfSurahWithText(surahId, translationId): Flow<List<AyahWithText>> (JOIN ayah and translation_text; AyahWithText has all AyahEntity fields + the translation text as "translationText")
  - ayahWithText(ayahId, translationId), randomAyahWithText(translationId)
  - randomWidgetAyahId(translationId, maxArabic, maxTranslation): Int?  (same idea as before but length is checked on translation_text.text for the selected translation)
  - searchWithText(q, qNorm, translationId): List<AyahWithText>  — translation_text.text LIKE '%'||:q||'%' ESCAPE '\' OR arabicSearch LIKE '%'||:qNorm||'%' ESCAPE '\' ORDER BY id LIMIT 100 (user input still escaped for % _ \)
  - bookmarks list must also load text for the selected translation.
- Fallback: if a translation_text row is missing for an ayah (should never happen after validation), show an empty string, never crash.

A3. Import / verification (QuranImporter)
- Keep the existing import for surah, ayah, juz untouched.
- New step "import translations": for each catalog entry whose file exists, parse and verify exactly like the old Bangla check: exactly 6236 lines, the (sura, aya) keys identical to the Arabic key set, no blank text, per-surah ayah counts equal to quran-data.xml. If one translation fails verification, skip ONLY that translation (do not show it in Settings), record the reason, and show a warning in the About screen; the others still work. If the default translation fails, fall back to the first valid one.
- Insert translation rows in batches inside transactions, with progress on the import screen ("Preparing translations… 1/2").
- DataStore flag: add "translations_imported_v2" (Boolean, default false) and a "translations_catalog_hash" (hash of the sorted list of id+file name+file size). On app start: if the flag is false, or the hash differs (the owner added a new translation file in an update), run ONLY the translation import (no wipe of ayah/bookmark data), then set the flag/hash. Existing users upgrading from v1 therefore keep everything and just get the new translations after a quick import screen. Keep "import_done_v1" logic as it is.
- Add DataIntegrityTest cases (pure JVM, no Quran text asserted): every catalog file that exists has 6236 ayahs and matching keys; translation ids are unique; exactly one default.

A4. Settings
- New repository field: translation_id (String, default = catalog default id). If the stored id is no longer valid, use the default silently.
- Settings screen, section "Reading" gets a new item at the top: "Bangla translation" (বাংলা অনুবাদ).
  - Shows the selected translation name + translator on one line; tap opens a full-screen list or a modal bottom sheet (RadioButton list, one row per valid translation, each row 48dp+ tall).
  - Each row: translation name (nameBn if not empty else nameEn), translator, source line, and a live sample line that loads Surah 112 Ayah 1 for that translation from the database (real data, never hard-coded; shows a placeholder while loading).
  - Selecting a row saves immediately and updates every screen instantly (Flow based): Home ayah card, Reader, Search results, Bookmarks, share/copy text, widget (see A6).
  - If only ONE valid translation exists, still show the item but disabled with the text "Only one translation available".
- The existing "Show Bangla translation" switch stays and applies to whichever translation is selected.

A5. Everywhere translation text is used
- Replace every read of ayah.bangla (Home, Reader, Search, Bookmarks, share/copy, Settings live preview, widget) with the selected translation's text through the new queries. The selected translationId comes from the settings Flow; in ViewModels use flatMapLatest on translationId so lists reload when it changes, while keeping the reader scroll position (do not rebuild keys: LazyColumn keys stay ayah ids).
- Share/copy text format gets one extra line at the end: the translation name, e.g.
  <arabicDisplay>

  <translation text>

  — <Bangla surah name> <surah>:<ayah>
  <translation name> · Quran Sharif
- About screen: replace the single translation credit with a section listing EVERY valid translation: name, translator, source, version (if not empty), licenseNote, and its exact license/comment text from the file header (verbatim, not paraphrased), plus the Tanzil credit as before. For any translation whose source is "QuranEnc.com" also show the line "Publisher: QuranEnc.com (quranenc.com)" and the version. Show the sentence "Translations from Tanzil are for non-commercial use." when at least one Tanzil translation is present. All of this is plain text; no links are opened and no network is used.

A6. Widget and translation
- The widget uses the selected translation. When the user changes the translation in Settings, call WidgetUpdater.rerender(context) (defined in B4) so the SAME ayah is shown in the new translation (do not pick a new random ayah). When the widget picks a random ayah, it uses randomWidgetAyahId(selectedTranslationId, ...).
- The widget card shows a tiny translation label next to "QURAN SHARIF" only if there is more than one valid translation (e.g. "QURAN SHARIF · Muhiuddin Khan"), truncated with ellipsis.

======================================================
CHANGE B — Adjustable text size in the widget
======================================================

B1. Settings (new section "Widget", above the existing widget items)
- Two sliders (Material 3 Slider, discrete steps), both persisted in DataStore AND mirrored into the existing synchronous WidgetPrefs (SharedPreferences) because the widget reads WidgetPrefs:
  - "Widget Arabic text size": 70% to 150%, step 10, default 100  (keys: widget_arabic_scale_pct)
  - "Widget translation text size": 70% to 150%, step 10, default 100 (key: widget_translation_scale_pct)
- Show the value next to each slider ("100%").
- "Reset widget text size" text button sets both back to 100.
- A LIVE PREVIEW card directly under the sliders that looks like the real widget (same colors #14352D card, #9AD3C0 reference, #F4E7BF Arabic, #2C5A4D divider, #E3EEEA translation, #7FB8A5 label, 28dp radius). It uses the currently shown widget ayah (read widget_ayah_id from WidgetPrefs; if missing, use Surah 112 Ayah 1 loaded from the DB) and the selected translation. The Arabic in the preview is rendered with the SAME ArabicBitmapRenderer and the SAME size math as the real widget so what the user sees is what the widget shows. Update the preview as the slider moves; apply to the real widget when the user releases the slider (onValueChangeFinished), not on every tick.

B2. Size math (single source of truth: a small object WidgetSizing)
- baseArabicStartPx = 46f, baseArabicMinPx = 24f, baseTranslationSp = 14f
- arabicStartPx = baseArabicStartPx * arabicScalePct / 100f ; arabicMinPx = baseArabicMinPx * arabicScalePct / 100f (coerce at least 16f)
- translationSp = (baseTranslationSp * translationScalePct / 100f) rounded to the nearest 0.5f
- ArabicBitmapRenderer.render keeps its width of 640px and the shrink-to-fit loop, but maxHeightPx is raised from 360 to 420 so larger text still fits. Never create a bitmap larger than 640 x 420 px (RemoteViews memory limit). If the text still does not fit at arabicMinPx, it is allowed to shrink further down to 16px rather than crash.
- Long ayahs: the length limits scale with the user's size so large text does not overflow:
  maxArabic = (180 * 100f / arabicScalePct).toInt().coerceAtLeast(60)
  maxTranslation = (260 * 100f / translationScalePct).toInt().coerceAtLeast(80)
  Use these values in randomWidgetAyahId. If it returns null, relax the limits (x1.5) and finally pick any ayah; never crash.
- Glance Text for the translation uses fontSize = translationSp.sp, maxLines = 8, ellipsis safety net stays. The reference line and the small label are not affected by the sliders.
- If the widget is smaller than the content (user chose big text on a small widget), content is clipped from the bottom, not crashed. Add a one-line helper text under the sliders: "Make the widget bigger on your home screen if large text does not fit."

B3. Apply changes to the widget
- Reading the sizes: QuranWidget.provideGlance reads the two scale values from WidgetPrefs (defaults 100 if missing) on every update.

B4. WidgetUpdater changes
- Keep refresh(context) (pick a NEW random ayah, then update). Add rerender(context): a suspend function that does NOT change widget_ayah_id and only calls QuranWidget().updateAll(context) (used for text size changes, translation changes, and theme-independent redraws). Wrap in try/catch, never throw.
- The "Refresh widget now" button, the widget's own refresh icon, the screen-on service and the 30-minute fallback keep calling refresh() (new random ayah) exactly as before.

B5. Persistence
- New DataStore keys: translation_id (String), widget_arabic_scale_pct (Int, 100), widget_translation_scale_pct (Int, 100), translations_imported_v2 (Boolean), translations_catalog_hash (String). SettingsRepository setters also write the two widget scale values to WidgetPrefs synchronously. On app start, if WidgetPrefs has no scale values but DataStore does (for example after an update), copy them across once.

======================================================
RESOURCES / STRINGS
======================================================
Add English and Bangla strings for: Bangla translation, Only one translation available, Preparing translations, Translation unavailable warning on About, Widget Arabic text size, Widget translation text size, Reset widget text size, widget size helper text, Preview. Natural, correct Bangla wording.

======================================================
QA CHECKLIST — answer EVERY line in the final message
======================================================
Data
- [ ] Each valid translation has 6236 ayahs and matching keys; invalid ones are skipped with a visible warning, no crash.
- [ ] Upgrading from v1 keeps bookmarks, last read, settings; default translation text equals the old Bangla text.
- [ ] No Quran or translation text appears in code, tests, previews or resources.
- [ ] Still no INTERNET permission.
Translation selection
- [ ] Settings shows all valid translations with live sample (112:1) from DB; selection persists after restart.
- [ ] Changing the translation updates Home, Reader (scroll position kept), Search, Bookmarks, share/copy, About credits, and the widget (same ayah).
- [ ] Search finds words in the SELECTED translation; Arabic search and 2:255 style references still work.
- [ ] About lists every translation with verbatim license text, version, publisher and the non-commercial note for Tanzil ones.
- [ ] Translation text is shown exactly as in the files (no trimming, no normalization), including the QuranEnc one.
Widget size
- [ ] Both sliders persist; preview matches the real widget; widget updates on slider release.
- [ ] Extreme values (70% and 150%) never crash and never create a bitmap over 640x420.
- [ ] Refresh button, screen-on refresh, 30-minute update and tap-to-open-ayah still work.
- [ ] Rotation, process death, dark/light switch, language switch still fine; release build (R8) works.
Build
- [ ] assembleDebug, assembleRelease and ./gradlew test pass; list the changed files.
```

---

## 2. After the AI answers

1. Replace/add the files it lists. Put the translation files and `translations.json` in `app/src/main/assets/translations/`.
2. Run `DataIntegrityTest` first. It must pass before you run the app.
3. Run the app on a phone that already has the old version installed (do NOT uninstall) to test the upgrade path, then test a fresh install.
4. Test the widget at 70%, 100% and 150%, and with the widget resized small and large.
5. If something fails, send the AI the exact error message or logcat lines.

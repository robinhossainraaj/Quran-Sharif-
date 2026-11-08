# Quran Sharif — Complete Android App Build Specification

> Paste this whole file into Google AI Studio (or any coding AI) as the project brief.
> Target: a **fully offline**, native **Android** app (Kotlin + Jetpack Compose) with **Arabic + Bangla** Quran, a **home-screen widget** that shows a random ayah, a splash screen, a home screen, and a settings screen.

---

## 0. How to use this file

### 0.1 Steps the human (app owner) must do BEFORE asking the AI to code

1. Go to **tanzil.net/download** and download these plain-text files (do not edit them):
   | File to save as (exact name) | Tanzil option to choose |
   |---|---|
   | `quran-uthmani.txt` | Quran Text → **Uthmani**, output format **Text (with Pause Marks off)**; plain text, one ayah per line, format `sura\|aya\|text` |
   | `quran-simple-clean.txt` | Quran Text → **Simple Clean**, same format (used ONLY for search) |
   | `bn.bengali.txt` | Translations → Bengali → **Muhiuddin Khan** (or another Bengali translation you trust), format `sura\|aya\|text` |
   | `quran-data.xml` | The metadata file from Tanzil (surah names, ayah counts, revelation type, juz start markers) |
2. Download fonts (free, OFL license) from **fonts.google.com**:
   - **Amiri Quran** (Regular) → rename to `amiri_quran_regular.ttf`
   - **Noto Sans Bengali** (Regular and Medium) → rename to `noto_sans_bengali_regular.ttf` and `noto_sans_bengali_medium.ttf`
   - Android resource file names MUST be lowercase letters, digits, underscore only.
3. Put them in the project exactly here (the AI must reference these exact paths):
   - `app/src/main/assets/quran-uthmani.txt`
   - `app/src/main/assets/quran-simple-clean.txt`
   - `app/src/main/assets/bn.bengali.txt`
   - `app/src/main/assets/quran-data.xml`
   - `app/src/main/res/font/amiri_quran_regular.ttf`
   - `app/src/main/res/font/noto_sans_bengali_regular.ttf`
   - `app/src/main/res/font/noto_sans_bengali_medium.ttf`
4. Build in **Android Studio** (latest stable). AI Studio only writes the code; it cannot build the APK.

### 0.2 Instructions to the AI (read carefully)

- Output the **complete project**, file by file, each file in its own code block headed by its full path. No "…rest unchanged", no placeholders, no pseudo-code. Every file must compile.
- Before writing code, **open and inspect** the four data files (their first and last lines and the header comments starting with `#`) and adapt parsing to what is really there. Do not guess formats.
- Follow Section 1 (non-negotiable rules) over any other instruction.
- After the code, output the **QA checklist (Section 13)** with every item answered, and a short list of anything you were unsure about.
- If a Gradle/library API is uncertain for the version you chose, use the stable, documented API and say which version you used. Do not invent APIs.

---

## 1. Non-negotiable rules

1. **Never write, generate, correct, translate, "complete" or paraphrase any Quran Arabic text or Bangla translation.** All of it comes only from the data files in Section 0.1. This includes `@Preview` functions, unit tests, sample screens, widget previews and documentation: use obvious placeholders like `"ARABIC TEXT"` / `"বাংলা অনুবাদ"` there, never real ayah text.
2. **Do not modify the Arabic text.** It is displayed exactly as stored. The only allowed derived data: (a) a search-only normalized column (Section 5.2); (b) stripping a leading Bismillah from ayah 1 for display under the strict rules of Section 4.4.
3. **Fully offline.** Do NOT declare the `INTERNET` permission. No analytics, crash reporters, ads, remote fonts, remote config, `WebView`, or network libraries.
4. **No Hilt/Dagger, no Koin.** Use a simple manual `AppContainer` (created in `Application.onCreate`) to avoid build errors.
5. **Language:** Kotlin only. UI: Jetpack Compose + Material 3. Avoid experimental APIs where a stable alternative exists; if one is unavoidable, add the correct `@OptIn`.
6. **Attribution is mandatory:** the About screen shows Tanzil credit and the translation/font credits read from the data files' header comments (Section 8.8).
7. Every user-visible UI string lives in `strings.xml` (English) and `values-bn/strings.xml` (Bangla). No hard-coded UI strings in composables.
8. Code must be null-safe, crash-free on rotation/process death, and must not block the main thread (all DB/file work on `Dispatchers.IO`).

---

## 2. Product overview

**App name:** Quran Sharif (Bangla display name: কুরআন শরীফ)
**Package:** `com.quransharif.app` (owner may rename)
**Languages inside the app:** Arabic (Uthmani script) and Bangla translation, plus UI in Bangla/English.
**Offline:** 100%.

### 2.1 Feature list (all required)

| # | Feature | Summary |
|---|---|---|
| 1 | Splash | System splash (Android 12+ API, with backport) + first-run data import screen with progress |
| 2 | Home | "Ayah of the moment" card, Continue reading, quick links |
| 3 | Quran tab | 114 surahs list + 30 juz list |
| 4 | Reader | Arabic + Bangla per ayah, bookmark/copy/share, size controls from settings, last-read tracking |
| 5 | Search | Search Bangla text, Arabic text (diacritics-insensitive), or reference like `2:255` |
| 6 | Bookmarks | Saved ayahs list |
| 7 | Settings | Theme, language, text sizes, translation toggle, keep screen on, widget behaviour, data resets, About |
| 8 | Widget | Home-screen widget showing a random ayah (Arabic + Bangla + reference); changes every time the screen turns on (optional service), tap to open the ayah, refresh button |
| 9 | About | Credits, licenses, version, privacy statement |

### 2.2 Out of scope (do not build)
Audio recitation, tafsir, online sync, accounts, notifications other than the single foreground-service notification, prayer times, ads.

---

## 3. Technical setup

- `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35` (or the latest stable your Android Studio supports; keep all three consistent).
- Use a **mutually compatible** set of stable versions via `gradle/libs.versions.toml`: Android Gradle Plugin 8.x, Kotlin 2.x with the **Compose compiler Gradle plugin** (`org.jetbrains.kotlin.plugin.compose`), KSP matching the Kotlin version, Compose BOM (latest stable), Material 3, Room (latest stable 2.x, KSP), DataStore Preferences, Navigation Compose, Lifecycle (viewmodel-compose, runtime-compose), `androidx.activity:activity-compose`, `androidx.appcompat:appcompat` (needed for in-app language switching), `androidx.core:core-splashscreen`, `androidx.glance:glance-appwidget` (+ `glance-material3`), `androidx.core:core-ktx`, kotlinx-coroutines-android. Test: JUnit4 only.
- State the exact versions you chose in a comment at the top of `libs.versions.toml`. A clean **Gradle sync must succeed** with these.
- `buildFeatures { compose = true }`, Java/Kotlin JVM target 17.
- Release build: `isMinifyEnabled = true`, `isShrinkResources = true`, with a working `proguard-rules.pro` (Room, Glance and DataStore work with default consumer rules; add keep rules only if actually needed). The release build must be tested to run correctly.
- Edge-to-edge: call `enableEdgeToEdge()` in `MainActivity.onCreate` and handle insets with `Scaffold` padding (targetSdk 35 forces edge-to-edge anyway).
- `android:allowBackup="false"`.
- Single-activity architecture; `MainActivity` extends **`AppCompatActivity`** (required for `AppCompatDelegate.setApplicationLocales`) and calls `setContent {}`.

### 3.1 Package / file structure (create exactly this, you may add small helper files)

```
app/src/main/
  AndroidManifest.xml
  assets/ (the 4 data files)
  java/com/quransharif/app/
    QuranApp.kt                      // Application, creates AppContainer
    AppContainer.kt                  // db, repositories, datastore
    MainActivity.kt
    data/
      db/ QuranDatabase.kt, SurahEntity.kt, AyahEntity.kt, JuzEntity.kt,
          BookmarkEntity.kt, QuranDao.kt, BookmarkDao.kt
      import/ TanzilParser.kt, QuranDataXmlParser.kt, QuranImporter.kt,
              ArabicNormalizer.kt, BismillahHelper.kt, DataSourceInfo.kt
      prefs/ SettingsRepository.kt     // DataStore
      QuranRepository.kt
    ui/
      theme/ Color.kt, Theme.kt, Type.kt
      nav/ AppNavHost.kt, Routes.kt
      splash/ ImportScreen.kt
      home/ HomeScreen.kt, HomeViewModel.kt
      quran/ QuranScreen.kt, QuranViewModel.kt
      reader/ ReaderScreen.kt, ReaderViewModel.kt, AyahItem.kt
      search/ SearchScreen.kt, SearchViewModel.kt
      bookmarks/ BookmarksScreen.kt, BookmarksViewModel.kt
      settings/ SettingsScreen.kt, SettingsViewModel.kt, AboutScreen.kt
      components/ (shared composables)
    widget/
      QuranWidget.kt, QuranWidgetReceiver.kt, WidgetUpdater.kt,
      ArabicBitmapRenderer.kt, RefreshAction.kt, WidgetPrefs.kt
    service/
      ScreenWatcherService.kt, BootReceiver.kt, ServiceController.kt
    util/ BanglaDigits.kt, ShareUtils.kt
  res/ (see Section 10)
app/src/test/java/com/quransharif/app/ DataIntegrityTest.kt, NormalizerTest.kt
```

---

## 4. Data sources and parsing (the part that must be perfect)

### 4.1 File formats

**`quran-uthmani.txt`, `quran-simple-clean.txt`, `bn.bengali.txt`** (Tanzil): one ayah per line:
```
<sura>|<aya>|<text>
```
- Lines starting with `#` are comments/copyright (usually at the end). Skip them, and skip blank lines.
- Strip a UTF-8 BOM (`﻿`) from the very first line if present.
- Split with `line.split("|", limit = 3)` (the text could in theory contain `|`).
- Read as UTF-8 with `assets.open(name).bufferedReader(Charsets.UTF_8).useLines { ... }`.
- Parsing must `require` exactly 3 parts and integer `sura`/`aya`; on failure throw an exception naming file and line number.

**`quran-data.xml`** (Tanzil metadata). Expected shape (VERIFY by opening the file; adapt names if different):
```xml
<quran>
  <suras>
    <sura index="1" ayas="7" start="0" name="الفاتحة" tname="Al-Faatiha" ename="The Opening" type="Meccan" order="5" rukus="1" />
    ...
  </suras>
  <juzs>
    <juz index="1" sura="1" aya="1" />
    ...
  </juzs>
  ...
</quran>
```
Parse with `javax.xml.parsers.DocumentBuilderFactory` (works on Android and in plain JVM unit tests; do NOT use `XmlPullParser`, it is not available in unit tests).
Use: `index`, `ayas`, `name` (Arabic surah name), `tname` (transliteration), `ename` (English meaning), `type` (`Meccan`/`Medinan`), `order` (revelation order), and the `<juz>` start markers.

### 4.2 Verification invariants (enforced in code AND unit tests)

All must hold, otherwise the import fails with a clear error screen (Section 8.1):

1. Each of the three text files has exactly **6236** ayah lines, ordered by (sura, aya) with no gaps and no duplicates.
2. Exactly **114** surahs. For every surah, the number of ayahs in each file equals `ayas` from `quran-data.xml`.
3. The three files have identical (sura, aya) key sets.
4. No Arabic or Bangla text is blank.
5. 30 juz entries, each referencing an existing (sura, aya); juz 1 starts at 1:1.

Spot checks (use as unit-test assertions on counts/positions, NOT on Quran text):
- Ayah counts: surah 1 = 7, 2 = 286, 3 = 200, 9 = 129, 18 = 110, 36 = 83, 55 = 78, 67 = 30, 112 = 4, 113 = 5, 114 = 6.
- Juz starts: juz 2 = 2:142, juz 3 = 2:253, juz 29 = 67:1, juz 30 = 78:1.
- Total = 6236.

### 4.3 Global ayah id
After verification assign `id = 1..6236` in file order. `id` is the primary key of the ayah table (enables O(1) random pick: random id in 1..6236).

### 4.4 Bismillah handling (display only)
In some Tanzil text files the Bismillah is prefixed to ayah 1 of surahs 2–114. Handling, strictly:
1. Read the Bismillah text from the DB: Surah 1, Ayah 1 (Arabic and Bangla). Never hard-code it.
2. For surahs other than 1 and 9, for ayah 1 ONLY: split Arabic by space; if the first 4 tokens, after `ArabicNormalizer.normalize`, equal the normalized Bismillah, then store `arabicDisplay` = remaining tokens joined by a single space (otherwise leave the text untouched).
3. Store the original in `arabicOriginal` too. Never discard it. Copy/share uses the display version.
4. The reader shows the Bismillah as a decorative header (from Surah 1:1 data) above ayah 1 for surahs 2–114 except 9. Surah 1 shows its ayah 1 normally. Surah 9 shows no Bismillah.
5. A unit test confirms for every surah (except 1, 9) that `arabicDisplay` of ayah 1 is non-empty.

### 4.5 DataSourceInfo
Read the `#` header/footer comment lines from each text file at import and store them (e.g., in DataStore or a small table) to display on the About screen: Tanzil text credit, translation name/translator, and the license text exactly as it appears in the files. Do not paraphrase license text.

---

## 5. Database (Room)

### 5.1 Entities
```kotlin
@Entity(tableName = "surah")
data class SurahEntity(
    @PrimaryKey val id: Int,            // 1..114
    val arabicName: String,             // from xml "name"
    val transliteration: String,        // xml "tname"
    val englishMeaning: String,         // xml "ename"
    val banglaName: String,             // from res string-array (Appendix A), index id-1
    val revelationType: String,         // "Meccan" | "Medinan"
    val revelationOrder: Int,
    val ayahCount: Int
)

@Entity(tableName = "ayah", indices = [Index(value = ["surahId", "ayahNo"], unique = true)])
data class AyahEntity(
    @PrimaryKey val id: Int,            // 1..6236 global
    val surahId: Int,
    val ayahNo: Int,
    val arabicOriginal: String,         // exact Tanzil Uthmani line text
    val arabicDisplay: String,          // same, minus leading Bismillah where applicable
    val arabicSearch: String,           // normalize(quran-simple-clean text)
    val bangla: String                  // exact translation text
)

@Entity(tableName = "juz")
data class JuzEntity(@PrimaryKey val index: Int, val startSurah: Int, val startAyah: Int, val startAyahId: Int)

@Entity(tableName = "bookmark")
data class BookmarkEntity(@PrimaryKey val ayahId: Int, val createdAt: Long)
```
`exportSchema = false`, database version 1, `fallbackToDestructiveMigration` NOT used (this is v1; if schema changes later, add proper migration).

### 5.2 Search normalization (`ArabicNormalizer.normalize(s)`)
Remove: U+0640 (tatweel), U+0610–U+061A, U+064B–U+065F, U+0670, U+06D6–U+06DC, U+06DF–U+06E8, U+06EA–U+06ED, U+08D3–U+08FF. Map `أ إ آ ٱ` → `ا`, and `ى` → `ي`. Trim. Apply it to (a) `quran-simple-clean.txt` text when building `arabicSearch`, (b) the user's Arabic query, (c) the Bismillah comparison in 4.4.

### 5.3 DAO queries (required)
- `surahs(): Flow<List<SurahEntity>>`, `surah(id)`
- `ayahsOfSurah(surahId): Flow<List<AyahEntity>>` ordered by `ayahNo`
- `ayahById(id)`, `ayahBySurahAndNo(surahId, ayahNo)`
- `randomAyah(): AyahEntity` — `SELECT * FROM ayah ORDER BY RANDOM() LIMIT 1`
- `randomWidgetAyahId(maxArabic: Int, maxBangla: Int): Int?` — `SELECT id FROM ayah WHERE LENGTH(arabicDisplay) <= :maxArabic AND LENGTH(bangla) <= :maxBangla ORDER BY RANDOM() LIMIT 1`
- `search(q: String, qNorm: String): List<AyahEntity>` — `WHERE bangla LIKE '%' || :q || '%' ESCAPE '\' OR arabicSearch LIKE '%' || :qNorm || '%' ESCAPE '\' ORDER BY id LIMIT 100` (escape `%`, `_`, `\` in user input before binding)
- `juzList(): Flow<List<JuzEntity>>`
- Bookmarks: `observeAll(): Flow<List<BookmarkEntity>>`, `isBookmarked(ayahId): Flow<Boolean>`, `insert`, `delete`, `deleteAll`
- `ayahCount(): Int` (used to detect a complete import)

### 5.4 Import pipeline (`QuranImporter`)
1. On app start: if `ayahCount() == 6236` and surah count 114 and DataStore flag `import_done_v1 == true` → ready.
2. Else: wipe tables, parse all four files (pure Kotlin functions, unit-testable), run invariants (4.2), insert inside **one transaction** in batches (e.g., per surah), emit progress `0..114`, finally set `import_done_v1 = true`.
3. Any exception → state `Error(message)` with a Retry button that wipes and retries.
4. The import must take only a few seconds, run on `Dispatchers.IO`, and survive the Activity being recreated (do it in an app-scoped coroutine in `AppContainer`, not in a composable).

---

## 6. Design system

> **OWNER NOTE:** the palette below comes from the widget mockup. If you want the look of your previous apps, replace ONLY the tokens in 6.1 and the font notes in 6.2; everything else stays.

### 6.1 Color tokens
| Token | Dark | Light |
|---|---|---|
| background | `#0E1A18` | `#F6F7F4` |
| surface (cards) | `#14352D` | `#FFFFFF` |
| surfaceVariant | `#1B4A3F` | `#E4ECE8` |
| primary | `#9AD3C0` | `#14352D` |
| onPrimary | `#0E1A18` | `#F4E7BF` |
| accentGold (Arabic text, key highlights) | `#F4E7BF` | `#7A5C12` |
| onBackground / onSurface (Bangla body) | `#E3EEEA` | `#1B2A26` |
| secondaryText | `#9AD3C0` | `#3F6B5D` |
| outline / divider | `#2C5A4D` | `#C9D8D1` |
| error | `#FFB4AB` | `#B3261E` |

All text/background pairs must meet WCAG AA contrast (4.5:1 for body text). Build a Material 3 `ColorScheme` from these in `Theme.kt` for both modes. Theme mode comes from settings (System / Light / Dark). Dynamic color (Material You) is OFF.

### 6.2 Typography
- Arabic: `FontFamily(Font(R.font.amiri_quran_regular))`. Default 30sp, `lineHeight = fontSize * 2.0`, `textDirection = TextDirection.Rtl`, `textAlign = TextAlign.Right`. Large line height is mandatory so diacritics never clip.
- Bangla and all UI text: `Noto Sans Bengali` (regular + medium) via `R.font.*`. Body 16sp, line height 1.6×.
- Define `QuranTypography` in `Type.kt`; reader sizes are overridden at runtime from settings.

### 6.3 Shape/spacing
Corner radius 28dp for hero/widget-like cards, 16dp for list cards, 12dp for chips. Screen padding 16dp. Touch targets ≥ 48dp. Use thin 1dp dividers with the `outline` token.

### 6.4 Icons
Material icons (`androidx.compose.material:material-icons-extended`) or hand-made `ImageVector`s. No emoji anywhere in the UI.

### 6.5 App icon (adaptive)
`ic_launcher_background` = solid `#14352D`. Foreground vector (108×108 viewport, open-book glyph in gold):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:translateX="30" android:translateY="30" android:scaleX="2" android:scaleY="2">
        <path android:pathData="M12,6c-2,-1.5 -5,-2 -8,-1.5V18c3,-0.5 6,0 8,1.5 2,-1.5 5,-2 8,-1.5V4.5C17,4 14,4.5 12,6z"
            android:strokeColor="#F4E7BF" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" />
        <path android:pathData="M12,6v13.5"
            android:strokeColor="#F4E7BF" android:strokeWidth="1.8" android:strokeLineCap="round" />
    </group>
</vector>
```
Also provide a monochrome layer (`<monochrome>`) for themed icons using the same vector in a single color.

---

## 7. Architecture

- MVVM. ViewModels expose `StateFlow<UiState>`; composables collect with `collectAsStateWithLifecycle()`.
- `AppContainer` holds `QuranDatabase`, `QuranRepository`, `SettingsRepository`, `QuranImporter`, and the app-scoped `CoroutineScope`. ViewModels are created with a simple `ViewModelProvider.Factory` that reads the container from `Application`.
- Settings in **DataStore Preferences** (Section 11). Everything reactive.
- Navigation Compose; routes in `Routes.kt`:
  - `home`, `quran`, `bookmarks`, `settings`, `about`, `search`
  - `reader/{surahId}?ayah={ayahNo}` (`ayah` optional int, default `-1`)
- Bottom `NavigationBar` with 4 destinations: Home, Quran, Bookmarks, Settings. Hide it on Reader, Search and About.
- Deep link from the widget: `MainActivity` reads Intent extras `EXTRA_SURAH_ID` and `EXTRA_AYAH_NO` in both `onCreate` and `onNewIntent` (`launchMode="singleTop"`) and navigates to the reader. Clear the extras after use so rotation doesn't re-navigate.

---

## 8. Screen specifications

### 8.1 Splash and first-run import
1. **System splash:** `androidx.core:core-splashscreen`. `Theme.App.Starting` with `windowSplashScreenBackground = #14352D`, `windowSplashScreenAnimatedIcon` = the foreground vector from 6.5, `postSplashScreenTheme = @style/Theme.QuranSharif`. Call `installSplashScreen()` before `super.onCreate` and keep it on screen (`setKeepOnScreenCondition`) only while the app state is `Checking`.
2. **State machine** (`AppState`): `Checking → Ready` (data already imported) or `Checking → Importing(progress) → Ready` or `→ Error(message)`.
3. **ImportScreen** (shown for `Importing` and `Error`): full-screen `#14352D` background, logo, title "Quran Sharif" and "কুরআন শরীফ", a `LinearProgressIndicator` (progress/114) with text "Preparing Quran data… 37/114" (localized), and for `Error` the message plus a **Retry** button.
4. Do not show any Quran text on this screen.

### 8.2 Home screen
Top app bar: title "Quran Sharif", search icon (→ Search).
Content (scrollable column):
1. **Ayah of the moment card** (28dp radius, surface color): reference line (`সূরা <banglaName> · <surah>:<ayah>`, in Bangla digits if enabled), Arabic text (gold), divider, Bangla translation, bottom row with: refresh icon (new random ayah via `randomAyah()`), bookmark toggle, share, "Read surah" text button (→ reader at that ayah). The shown ayah is kept in the ViewModel (and `rememberSaveable` id) so rotation doesn't change it; it changes only on refresh or when the app is freshly opened.
2. **Continue reading** card (only if a last-read position exists): surah name and ayah number, tap → reader at that ayah.
3. Two quick-link cards: **Surah list** (→ Quran tab, Surah sub-tab) and **Juz list** (→ Quran tab, Juz sub-tab).
Empty/loading states handled; no crashes if DB is empty.

### 8.3 Quran tab
- Top bar: title, search icon.
- `TabRow` with two tabs: **Surah** and **Juz**.
- **Surah list** (`LazyColumn`, key = id): row shows an octagon/diamond badge with number (Bangla digits per setting), `banglaName` + transliteration, below it `"মক্কী"/"মাদানী" · <n> আয়াত` (localized; Meccan → মক্কী, Medinan → মাদানী), and the Arabic surah name on the right in Amiri Quran 24sp. Tap → reader at ayah 1.
- **Juz list**: row "Juz N" (পারা N) with subtitle "Starts at <surah banglaName> <surah>:<ayah>"; tap → reader at that surah/ayah.

### 8.4 Reader
- Top bar: back, surah title (Bangla name), overflow with "Go to ayah…" (numeric dialog 1..ayahCount, validated) and "Bookmark surah start".
- `LazyColumn` (key = ayah id). First item: **surah header card** (Arabic name large, Bangla name, English meaning, revelation type, ayah count) and, if applicable per 4.4, the Bismillah line below it.
- Each ayah item (`AyahItem`):
  - Row 1: ayah number badge (left) + icon buttons: bookmark (filled when saved), copy, share (all 48dp).
  - Arabic (`arabicDisplay`) with size from settings.
  - If "Show Bangla" is on: Bangla translation with its own size.
  - Thin divider.
- **Share/copy text format:**
  ```
  <arabicDisplay>

  <bangla>

  — <Bangla surah name> <surah>:<ayah>
  Quran Sharif
  ```
- **Scroll to ayah** when the `ayah` argument ≥ 1: after data loads, `LazyListState.scrollToItem(index = ayahNo)` (header occupies index 0) and highlight that item (background `surfaceVariant`) for ~2 seconds, once only.
- **Last read:** when scrolling stops (`snapshotFlow` on `isScrollInProgress`, debounced) save `(surahId, firstVisibleAyahNo)` to DataStore.
- If "Keep screen on while reading" is enabled, set `FLAG_KEEP_SCREEN_ON` on the window only while the Reader is composed (use `DisposableEffect`).
- Pinch-to-zoom is not required; sizes are controlled in Settings. (Optional: a text-size quick action in the overflow that opens a bottom sheet with the same two sliders.)

### 8.5 Search
- Top bar with a `TextField` (auto-focus), debounced 300 ms, min 2 characters.
- Behaviors: (1) if the query matches `^\d{1,3}\s*[:：]\s*\d{1,3}$` show a single result "Go to <s>:<a>" if valid; (2) otherwise run `search(q, normalize(q))`; Bangla digits in the query are converted to ASCII digits for reference parsing.
- Results list (max 100): reference, short Arabic (1–2 lines), Bangla snippet with the matching text highlighted (use `AnnotatedString` with a background span; for Arabic highlighting is optional). Tap → reader at that ayah.
- States: initial hint, no results, loading.

### 8.6 Bookmarks
List of saved ayahs, newest first: reference, Arabic (max 2 lines), Bangla (max 2 lines), delete icon (undo via snackbar). Tap → reader. Empty state with explanation. Top-bar action "Delete all" with confirmation dialog.

### 8.7 Settings
Sections and controls (all persisted in DataStore, applied immediately):

**Appearance**
- Theme: three `FilterChip`s — System / Light / Dark.
- Language (ভাষা): System default / English / বাংলা — implemented with `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))` (`"en"`, `"bn"`, or empty list for system). The change recreates the activity automatically.

**Reading**
- Arabic text size: slider 20–48sp, step 2, default 30; plus a live preview that uses a real ayah loaded from the DB (surah 112 ayah 1 via `ayahBySurahAndNo`), never hard-coded text.
- Bangla text size: slider 12–26sp, step 1, default 16, same preview.
- Show Bangla translation (switch, default on).
- Use Bangla digits for numbers (switch, default on) → `BanglaDigits.convert`.
- Keep screen on while reading (switch, default off).

**Widget**
- "Change widget ayah when the screen turns on" (switch, default **on**). On enable: on Android 13+ request `POST_NOTIFICATIONS` (explain why first), then start `ScreenWatcherService`. On disable: stop it. Show helper text: "Uses a small silent notification so Android keeps this running."
- Button "Battery optimization settings" → `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (the list screen; do NOT use the request permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Helper text: some phone brands (Xiaomi, Oppo, Vivo, Huawei, Samsung) stop background services; set the app to "Unrestricted/No restrictions" if the widget does not change.
- "Refresh widget now" button.

**Data**
- Reset reading progress (confirm dialog).
- Delete all bookmarks (confirm dialog).

**About** → About screen (8.8).

### 8.8 About
App name/version (`BuildConfig`/`PackageInfo`), "Works fully offline. No data collected. No internet permission." Credits block built from `DataSourceInfo`: Tanzil Project text credit and link text `tanzil.net` (plain text, no network call; optional "Copy link"), translation name/translator and license text from the file header, font credits (Amiri Quran — SIL Open Font License; Noto Sans Bengali — SIL Open Font License). Include the sentence: "Quran text is shown exactly as provided by the source, without modification."

---

## 9. Home-screen widget (Jetpack Glance)

### 9.1 Behavior
- Widget name in picker: "Quran Sharif — Random Ayah". Default size 4×2 cells, resizable.
- Shows: reference line, Arabic ayah, Bangla translation, tiny "QURAN SHARIF" label, a refresh icon button.
- **Changes to a new random ayah**: (a) every time the screen turns on (if the setting is on, via the service in 9.4), (b) when the user taps the refresh icon, (c) when the widget is first added, (d) as a fallback about every 30 minutes (`updatePeriodMillis=1800000`).
- Tapping the ayah body opens the app at that exact ayah (Reader), via an explicit `Intent` to `MainActivity` with `EXTRA_SURAH_ID`, `EXTRA_AYAH_NO` (use `actionStartActivity` with `actionParametersOf`).
- Colors fixed to the dark card style (works on any wallpaper): card `#14352D`, reference `#9AD3C0`, Arabic `#F4E7BF`, divider `#2C5A4D`, Bangla `#E3EEEA`, label `#7FB8A5`. 28dp rounded corners (`GlanceModifier.cornerRadius`; on API < 31 fall back to a rounded background drawable).

### 9.2 Why the Arabic is rendered as a bitmap (important)
Glance/RemoteViews `Text` cannot use custom fonts, and system fonts may render Quranic marks incorrectly. Therefore:
- Render the Arabic ayah to a `Bitmap` with `StaticLayout` + `TextPaint` using `ResourcesCompat.getFont(context, R.font.amiri_quran_regular)` and show it with Glance `Image(ImageProvider(bitmap))`.
- Bangla uses a normal Glance `Text` (Android system Bengali font), 13–14sp.
- Reference implementation of the renderer (keep logic, adapt names):
```kotlin
object ArabicBitmapRenderer {
    fun render(
        context: Context, text: String, color: Int,
        widthPx: Int = 640, startSizePx: Float = 46f, minSizePx: Float = 24f, maxHeightPx: Int = 360
    ): Bitmap {
        val typeface = ResourcesCompat.getFont(context, R.font.amiri_quran_regular)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.color = color
        }
        var size = startSizePx
        var layout: StaticLayout
        while (true) {
            paint.textSize = size
            layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.35f)
                .setIncludePad(true)
                .build()
            if (layout.height <= maxHeightPx || size <= minSizePx) break
            size -= 2f
        }
        val bmp = Bitmap.createBitmap(widthPx, layout.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        layout.draw(Canvas(bmp))
        return bmp
    }
}
```
- RemoteViews has a bitmap memory limit. Keep bitmaps ≤ 640×360 px (~0.9 MB). Never render at full screen size.
- **Long ayahs:** the widget picks only ayahs with `LENGTH(arabicDisplay) <= 180` and `LENGTH(bangla) <= 260` (constants `WIDGET_MAX_ARABIC`, `WIDGET_MAX_BANGLA` in `WidgetUpdater`). If `randomWidgetAyahId` returns null, fall back to ayah id 1's neighbours by relaxing the limits (never crash). The widget Bangla `Text` uses `maxLines = 6` with ellipsis as a safety net.

### 9.3 State and update flow
- Store the current widget ayah id in a plain `SharedPreferences` (`WidgetPrefs`, key `widget_ayah_id`) — same ayah for all widget instances. Synchronous and simple.
- `WidgetUpdater.refresh(context)`: `suspend`; picks a new id (not equal to the previous when possible, retry up to 3 times), writes the pref, then `QuranWidget().updateAll(context)`. Wrap in try/catch and log errors; never throw out of a receiver/service.
- `QuranWidget.provideGlance`: read the id from `WidgetPrefs` (if missing → pick random and store), load the ayah from the DB (`QuranRepository` via `AppContainer`, accessible from `applicationContext as QuranApp`), render the bitmap, then `provideContent { ... }`. If the DB isn't ready (first launch before import), show a "Open Quran Sharif to finish setup" message that opens the app on tap.
- `RefreshAction` is a `ActionCallback` that calls `WidgetUpdater.refresh`.
- `QuranWidgetReceiver : GlanceAppWidgetReceiver`, `glanceAppWidget = QuranWidget()`.
- Widget provider XML `res/xml/quran_widget_info.xml`:
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="110dp"
    android:targetCellWidth="4"
    android:targetCellHeight="2"
    android:minResizeWidth="180dp"
    android:minResizeHeight="110dp"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="1800000"
    android:widgetCategory="home_screen"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:description="@string/widget_description" />
```

### 9.4 Screen-on trigger (foreground service)
`ACTION_SCREEN_ON` cannot be declared in the manifest; it must be registered dynamically at runtime by a long-lived component. Implement `ScreenWatcherService`:
```kotlin
class ScreenWatcherService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                scope.launch { WidgetUpdater.refreshIfWidgetPresent(applicationContext) }
            }
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannelIfNeeded()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        return START_STICKY
    }
    override fun onDestroy() { unregisterReceiver(receiver); scope.cancel(); super.onDestroy() }
}
```
- Notification: channel `widget_refresh`, importance **LOW** (silent, no heads-up), ongoing, small icon = monochrome book glyph, text "Quran Sharif widget is active" (localized), tapping opens the app. On Android 13+ the user can hide it; the service still runs.
- `refreshIfWidgetPresent`: only update if `GlanceAppWidgetManager(context).getGlanceIds(QuranWidget::class.java)` is not empty (saves battery).
- `ServiceController.sync(context, enabled)`: starts (`ContextCompat.startForegroundService`) or stops the service. It is called: when the setting is toggled (from the foreground UI), on app start (`MainActivity.onCreate`) if the setting is on, and from `BootReceiver`.
- `BootReceiver` handles `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`: reads the setting from DataStore (`runBlocking` with short timeout inside `goAsync()` or a coroutine + `goAsync`) and starts the service if enabled; wrap in try/catch because Android may refuse background starts on some devices.
- Honest limitation to put in the About/Settings helper text: Android or phone-maker battery savers can stop background services; the refresh button and the 30-minute fallback still work.

---

## 10. Manifest and resources

### 10.1 `AndroidManifest.xml` (complete, adapt package names)
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <!-- No INTERNET permission. The app is fully offline. -->

    <application
        android:name=".QuranApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:localeConfig="@xml/locales_config"
        android:supportsRtl="true"
        android:theme="@style/Theme.App.Starting">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.QuranWidgetReceiver"
            android:exported="true"
            android:label="@string/widget_name">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/quran_widget_info" />
        </receiver>

        <service
            android:name=".service.ScreenWatcherService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Listens for screen-on events locally to show a new Quran ayah in the user's home-screen widget. No network or personal data is used." />
        </service>

        <receiver android:name=".service.BootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <!-- Backport of per-app language for API < 33 -->
        <service
            android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
            android:enabled="false"
            android:exported="false">
            <meta-data android:name="autoStoreLocales" android:value="true" />
        </service>
    </application>
</manifest>
```

### 10.2 Resources checklist
- `res/xml/locales_config.xml`: `<locale-config><locale android:name="en"/><locale android:name="bn"/></locale-config>`
- `res/values/strings.xml` and `res/values-bn/strings.xml`: **complete** sets for every UI string (Bangla in natural, correct wording). Include `surah_names_bn` string-array (Appendix A) in a non-translatable `res/values/surah_names_bn.xml`.
- `res/values/themes.xml`: `Theme.App.Starting` (parent `Theme.SplashScreen`) and `Theme.QuranSharif` (a Material3 NoActionBar based theme matching Section 6).
- `res/font/*` as in Section 0.1. `res/drawable` icon vectors, `res/mipmap-anydpi-v26/ic_launcher.xml` (+ round) referencing background color, foreground vector and monochrome.
- `res/drawable/ic_notification.xml` monochrome vector for the foreground-service notification.
- `res/drawable/widget_background.xml` rounded rectangle `#14352D` (API < 31 fallback).

---

## 11. Settings keys (DataStore Preferences)

| Key | Type | Default |
|---|---|---|
| `theme_mode` | String: `SYSTEM` / `LIGHT` / `DARK` | `SYSTEM` |
| `app_language` | String: `""` (system) / `en` / `bn` | `""` |
| `arabic_size_sp` | Int | 30 |
| `bangla_size_sp` | Int | 16 |
| `show_bangla` | Boolean | true |
| `bangla_digits` | Boolean | true |
| `keep_screen_on` | Boolean | false |
| `widget_refresh_on_screen_on` | Boolean | true |
| `last_read_surah` | Int | 0 (none) |
| `last_read_ayah` | Int | 0 |
| `import_done_v1` | Boolean | false |

`SettingsRepository` exposes a `Flow<Settings>` data class and suspend setters.

---

## 12. Edge cases and quality bar

- Rotation, dark/light switch, language switch, process death: no state loss that matters (home ayah id and reader scroll are restored; last read persisted).
- Large font scale (system font size 200%): no clipped or overlapping text; layouts scroll.
- Tablets/landscape: content width capped (~720dp) and centered.
- TalkBack: every icon button has a content description; Arabic text has `lang="ar"` semantics where supported; the widget has content descriptions on Image and refresh button.
- Never use `!!` on data from DB/files without a guard; handle empty results gracefully.
- No memory leaks: unregister receivers, cancel scopes, no Activity context in singletons.
- Cold start to interactive Home (after first import) ≤ 1.5 s on a mid-range device.
- First-run import ≤ 10 s on a mid-range device; DB size reasonable (< 25 MB).
- `Bangla digits` helper: map `0-9` ↔ `০-৯` (`BanglaDigits.convert(String)` and `.toAscii(String)`).

---

## 13. QA checklist — the AI must answer EVERY line in its final message

**Data**
- [ ] Parsed counts: Arabic = 6236, Simple Clean = 6236, Bangla = 6236, surahs = 114, juz = 30.
- [ ] Per-surah ayah counts match `quran-data.xml` for all 114 surahs.
- [ ] Spot checks in 4.2 pass in `DataIntegrityTest`.
- [ ] Arabic text is stored and shown unmodified (only the Bismillah-prefix rule in 4.4 applies, original kept).
- [ ] No Quran text appears anywhere in the source code, tests, previews, resources or widget previews.

**Build**
- [ ] Gradle sync clean, versions listed and mutually compatible.
- [ ] `./gradlew assembleDebug` and `assembleRelease` succeed (R8 enabled), `./gradlew test` passes.
- [ ] Manifest has NO `INTERNET` permission.
- [ ] All three fonts load (names lowercase) and Arabic renders with diacritics fully visible in Reader, Home and widget.

**Functionality**
- [ ] First launch: splash → import progress → Home. Second launch: no import, fast start.
- [ ] Home random ayah refresh, bookmark toggle, share, copy, open in reader.
- [ ] Reader: header + Bismillah rules (shown for 2–8 and 10–114, not for 9; surah 1 shows ayah 1 normally), scroll-to-ayah + highlight, last-read saved and restored.
- [ ] Surah list (114) and Juz list (30) open the right positions.
- [ ] Search: Bangla word, Arabic without diacritics, reference like `2:255` and `۲:۲۵۵`-style Bangla digits `২:২৫৫` all work; special characters `% _ \` don't break the query.
- [ ] Bookmarks add/remove/delete-all with undo.
- [ ] Settings: theme, language, sizes (live preview), toggles, resets all persist across restart.
- [ ] Widget: can be added, shows Arabic bitmap + Bangla + reference, refresh button works, tap opens the exact ayah, long ayahs never crash or overflow, survives reboot.
- [ ] With the setting ON the widget changes each time the screen turns on; with it OFF the service is stopped and the notification disappears.
- [ ] Android 13+ notification permission flow works and denial doesn't crash.
- [ ] Rotation and process death handled.

**Docs**
- [ ] README with: data download steps, font steps, build steps, how to rename the package, known limitations (OEM battery killers).

---

## 14. Build and run (owner steps, include in README)

1. Android Studio → Open the project folder → wait for Gradle sync.
2. Verify the 4 data files and 3 font files are in place (Section 0.1).
3. Run `DataIntegrityTest` (right-click → Run). It must pass **before** you run the app.
4. Run on a device/emulator (API 26+). First launch imports the data.
5. Long-press the home screen → Widgets → "Quran Sharif — Random Ayah" → place it.
6. Open the app → Settings → keep "Change widget ayah when the screen turns on" enabled, allow notifications, and on phones with aggressive battery savers set the app to "Unrestricted".
7. Release APK/AAB: Build → Generate Signed Bundle / APK; keep your keystore backed up.

---

## Appendix A — Bangla surah names (display labels, index 1–114)

> These are UI labels only (not Quran text). Bangla transliteration spelling varies between publishers; **the owner should verify them against a trusted Bengali mushaf** (e.g., Islamic Foundation Bangladesh) before release. Store as `<string-array name="surah_names_bn">` in this exact order.

| # | Name | # | Name | # | Name |
|---|---|---|---|---|---|
| 1 | আল-ফাতিহা | 39 | আয-যুমার | 77 | আল-মুরসালাত |
| 2 | আল-বাকারা | 40 | গাফির | 78 | আন-নাবা |
| 3 | আলে ইমরান | 41 | হা-মীম আস-সাজদাহ | 79 | আন-নাযি'আত |
| 4 | আন-নিসা | 42 | আশ-শূরা | 80 | আবাসা |
| 5 | আল-মায়িদা | 43 | আয-যুখরুফ | 81 | আত-তাকভীর |
| 6 | আল-আনআম | 44 | আদ-দুখান | 82 | আল-ইনফিতার |
| 7 | আল-আ'রাফ | 45 | আল-জাসিয়া | 83 | আল-মুতাফফিফীন |
| 8 | আল-আনফাল | 46 | আল-আহকাফ | 84 | আল-ইনশিক্বাক্ব |
| 9 | আত-তাওবা | 47 | মুহাম্মাদ | 85 | আল-বুরূজ |
| 10 | ইউনুস | 48 | আল-ফাতহ | 86 | আত-ত্বারিক্ব |
| 11 | হুদ | 49 | আল-হুজুরাত | 87 | আল-আ'লা |
| 12 | ইউসুফ | 50 | ক্বাফ | 88 | আল-গাশিয়াহ |
| 13 | আর-রা'দ | 51 | আয-যারিয়াত | 89 | আল-ফাজর |
| 14 | ইবরাহীম | 52 | আত-তূর | 90 | আল-বালাদ |
| 15 | আল-হিজর | 53 | আন-নাজম | 91 | আশ-শামস |
| 16 | আন-নাহল | 54 | আল-ক্বামার | 92 | আল-লাইল |
| 17 | আল-ইসরা | 55 | আর-রাহমান | 93 | আয-যুহা |
| 18 | আল-কাহফ | 56 | আল-ওয়াকিয়া | 94 | আশ-শারহ |
| 19 | মারইয়াম | 57 | আল-হাদীদ | 95 | আত-তীন |
| 20 | ত্বা-হা | 58 | আল-মুজাদালা | 96 | আল-আলাক্ব |
| 21 | আল-আম্বিয়া | 59 | আল-হাশর | 97 | আল-ক্বদর |
| 22 | আল-হাজ্জ | 60 | আল-মুমতাহিনা | 98 | আল-বাইয়্যিনাহ |
| 23 | আল-মু'মিনূন | 61 | আস-সাফ | 99 | আয-যিলযাল |
| 24 | আন-নূর | 62 | আল-জুমু'আ | 100 | আল-আদিয়াত |
| 25 | আল-ফুরকান | 63 | আল-মুনাফিকূন | 101 | আল-ক্বারিআহ |
| 26 | আশ-শু'আরা | 64 | আত-তাগাবুন | 102 | আত-তাকাসুর |
| 27 | আন-নামল | 65 | আত-তালাক | 103 | আল-আসর |
| 28 | আল-কাসাস | 66 | আত-তাহরীম | 104 | আল-হুমাযাহ |
| 29 | আল-আনকাবূত | 67 | আল-মুলক | 105 | আল-ফীল |
| 30 | আর-রূম | 68 | আল-কলম | 106 | কুরাইশ |
| 31 | লুকমান | 69 | আল-হাক্কাহ | 107 | আল-মাঊন |
| 32 | আস-সাজদাহ | 70 | আল-মা'আরিজ | 108 | আল-কাওসার |
| 33 | আল-আহযাব | 71 | নূহ | 109 | আল-কাফিরূন |
| 34 | সাবা | 72 | আল-জিন | 110 | আন-নাসর |
| 35 | ফাতির | 73 | আল-মুযযাম্মিল | 111 | আল-লাহাব |
| 36 | ইয়াসীন | 74 | আল-মুদ্দাসসির | 112 | আল-ইখলাস |
| 37 | আস-সাফফাত | 75 | আল-ক্বিয়ামাহ | 113 | আল-ফালাক্ব |
| 38 | সোয়াদ | 76 | আদ-দাহর | 114 | আন-নাস |

## Appendix B — Reference visual of the widget (from the approved mockup)

```
┌──────────────────────────────────────────┐  card #14352D, radius 28dp
│ সূরা আর-রাদ · ১৩:২৮                    ⟳ │  reference #9AD3C0 13sp  | refresh icon
│                                          │
│        (Arabic ayah, bitmap, gold)       │  #F4E7BF, Amiri Quran, centered
│ ──────────────────────────────────────── │  divider #2C5A4D
│ (Bangla translation, 14–16sp, #E3EEEA)   │
│                              QURAN SHARIF│  #7FB8A5 11sp, letter-spaced
└──────────────────────────────────────────┘
```

## Appendix C — Definition of Done
The project opens in Android Studio, syncs, passes `DataIntegrityTest`, runs on an API 26+ device, imports data once, works fully with airplane mode on, shows a correctly rendered Arabic widget that changes on every screen-on (when allowed), has no INTERNET permission, and every item in Section 13 is answered "done" with evidence.

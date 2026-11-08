# Quran Sharif (কুরআন শরীফ) — Android App

A fully offline, native Android application built with Kotlin and Jetpack Compose featuring the Holy Quran with Uthmani Arabic script, authentic Bengali translation (Maulana Muhiuddin Khan), an interactive home-screen widget with Arabic bitmap rendering, bookmarks, reading progress tracker, and search.

---

## 1. Features

- **100% Offline**: Zero internet permission declared. All text, metadata, and fonts are bundled directly in the app.
- **Holy Quran**: Full 114 Surahs and 30 Juz (Para) with Uthmani Arabic script and Bengali translation.
- **Home-Screen Widget (Glance)**: Displays a random ayah on your Android home screen with rendered Arabic typography, Bengali translation, reference, and an instant refresh button.
- **Screen-On Refresh Service**: Optional low-priority background watcher that refreshes the widget with an inspiring ayah whenever you unlock or turn on your device.
- **Reader Experience**:
  - Decorative Bismillah header for Surahs 2–114 (excluding Surah 9; Surah 1 shows Ayah 1 naturally).
  - Quick bookmark, copy, and share actions for every single ayah.
  - "Go to ayah" quick navigation.
  - Automatic last-read position tracking.
  - Optional "Keep screen on while reading" toggle.
- **Search**: Fast, diacritic-insensitive Arabic search, Bengali text search, and instant reference lookup (e.g. `2:255` or `২:২৫৫`).
- **Bookmarks**: Save any ayah and manage your bookmarks with one-tap undo.
- **Customizable Appearance & Reading**:
  - Dark / Light / System theme modes.
  - English and বাংলা language switching via Android's per-app language framework.
  - Independent Arabic (20–48sp) and Bengali (12–26sp) font size sliders with live preview.
  - Option to display numbers in Bengali digits (`০–৯`).

---

## 2. Pre-bundled Data and Fonts

The application bundles authentic data files and fonts:
- `app/src/main/assets/quran-uthmani.txt` — Tanzil Uthmani text (6,236 ayahs)
- `app/src/main/assets/quran-simple-clean.txt` — Tanzil Simple Clean text (used for search indexing)
- `app/src/main/assets/bn.bengali.txt` — Maulana Muhiuddin Khan Bengali translation
- `app/src/main/assets/quran-data.xml` — Tanzil Quran metadata (114 Surahs, 30 Juz)
- `app/src/main/res/font/amiri_quran_regular.ttf` — Amiri Quran Arabic font (OFL license)
- `app/src/main/res/font/noto_sans_bengali_regular.ttf` & `noto_sans_bengali_medium.ttf` — Noto Sans Bengali font (OFL license)

---

## 3. How to Build and Run in Android Studio

1. **Open the Project**:
   - Launch Android Studio (Giraffe, Hedgehog, Iguana, Ladybug or newer).
   - Select **Open** and choose this project root directory.
   - Wait for Gradle sync to complete cleanly.

2. **Verify Tests**:
   - Open `app/src/test/java/com/example/DataIntegrityTest.kt`.
   - Right-click and choose **Run 'DataIntegrityTest'**.
   - Verify that all invariants (6,236 ayahs, 114 surahs, 30 juz, spot checks) pass.

3. **Run the App**:
   - Connect an Android device (API 26+) or launch an emulator.
   - Click **Run 'app'** (`Shift + F10`).
   - On the first launch, the app prepares the local SQLite database in ~2 seconds.

4. **Add the Home-Screen Widget**:
   - Long-press on an empty space on your device's home screen.
   - Select **Widgets** → **Quran Sharif** → **Quran Sharif — Random Ayah**.
   - Place and resize the widget on your home screen.

5. **Enable Screen-On Ayah Updates**:
   - Open Quran Sharif → **Settings**.
   - Under **Home-Screen Widget**, ensure "Change widget ayah when the screen turns on" is enabled.
   - On Android 13+, allow notifications so the silent foreground service can run.

---

## 4. Testing Debug APK on a Real Device

An automated deployment process copies the compiled debug APK directly to the `download APK/` folder in the project root:
- Location: `download APK/QuranSharif-debug.apk` (and `download APK/app-debug.apk`)
- You can copy this APK directly to any Android device running Android 8.0 (API 26) or higher.

To build and deploy via command line:
```bash
gradle assembleDebug
```
The automated Gradle task `deployApkToDownloadFolder` will automatically place the latest APK inside `download APK/`.

---

## 5. How to Rename the Package

If you want to change the package name (e.g. to `com.quransharif.app`):
1. In `app/build.gradle.kts`:
   - Update `namespace = "com.quransharif.app"`
   - Update `applicationId = "com.quransharif.app"` (or your custom ID)
2. In Android Studio Project view:
   - Right click `app/src/main/java/com/example` → **Refactor** → **Rename** → Rename package to `com.quransharif.app`.
3. In `app/src/main/AndroidManifest.xml`:
   - Update package references if any absolute class names are used.
4. Clean and rebuild the project.

---

## 6. Known Limitations (OEM Battery Savers)

Some phone manufacturers (such as Xiaomi/MIUI, Oppo/ColorOS, Vivo/Funtouch, Huawei/EMUI, and Samsung/OneUI) apply aggressive battery-saving killers that terminate background services when the app is swiped away.

**Solution**:
- Go to device **Settings** → **Apps** → **Quran Sharif** → **Battery / Battery Saver**.
- Set to **No restrictions** or **Unrestricted**.
- Enable **Autostart** if present on your device.
- The in-app widget also includes a manual **Refresh (⟳)** button and a 30-minute fallback timer that operates independently of the background service.

---

## 7. Credits & Attribution

- **Quran Text**: Tanzil Project ([tanzil.net](https://tanzil.net)). Quran text is shown exactly as provided by the source, without modification.
- **Translation**: Maulana Muhiuddin Khan (Bengali Translation, Tanzil Project).
- **Arabic Font**: Amiri Quran (SIL Open Font License).
- **Bengali Font**: Noto Sans Bengali (Google Fonts, SIL Open Font License).

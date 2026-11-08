# Quran Sharif — Audio Recitation Feature Prompt (v3)

> Paste the prompt in Section 2 into Google AI Studio **in the same chat/project as the app**, with the current project files available to the AI.
> One feature only: **download-on-demand recitation audio with several reciters, a voice preview, and text that follows the audio verse by verse with highlight.** Everything else must keep working exactly as now.

---

## 1. What was checked, and what was NOT checked

**Checked on the source website (alquran.cloud/cdn and alquran.cloud/terms-and-conditions):**
- Per-ayah audio URL pattern: `https://cdn.islamic.network/quran/audio/{bitrate}/{edition}/{number}.mp3`
- `{number}` is the **global ayah number 1–6236**. This equals our database `ayah.id` (file order of the Tanzil text). So no mapping table is needed.
- Bitrates that exist on the CDN: 192, 128, 64, 48, 40, 32 kbps. Not every reciter has every bitrate.
- Edition ids shown on the page (with the default bitrate the page lists): `ar.alafasy` 128, `ar.husary` 128, `ar.minshawi` 128, `ar.sudais` 192, `ar.shuraim` 128, `ar.abdulbasit` 192, `ar.ajamy` 128, `ar.hudhaify` 128, `ar.muhammadayoub` 128, `ar.muhammadjibreel` 128. The page calls this list a sample; the live list is the API's audio edition list.
- Terms (summary of their page): recitations are licensed by the reciters or their estates for free **non-commercial** redistribution; copyright stays with the reciters and they may ask for content to be removed. This app is non-profit and non-commercial, so this fits. Keep it free, no ads, no payments.

**NOT checked (you must check, Section 0):**
- File sizes per surah (nobody has measured them; they depend on reciter and bitrate).
- Whether the file for ayah 1 of surahs 2–114 already contains the Bismillah (the app has a setting to handle both cases).
- That every edition id in `reciters.json` really exists at the bitrate you put there.
- The full text of their Terms page beyond the summary above. Read it once yourself.

---

## 0. Owner steps before asking the AI

1. **Check each reciter you want** in a browser (replace the edition and bitrate):
   - `https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3`
   - `https://cdn.islamic.network/quran/audio/128/ar.alafasy/6236.mp3`
   Both must play. Do this for every reciter in your list. If a bitrate gives an error, try 64 or 192. Choose a lower bitrate (64 or 128) when you can, because files are big.
2. **Listen test for the Bismillah:** play `.../ar.alafasy/8.mp3` (that is Surah 2, ayah 1). If you hear "Bismillah..." before "Alif Lam Meem", the ayah-1 files include the Bismillah, so set the app setting "Play Bismillah before surah" OFF by default (the prompt supports both).
3. Create `app/src/main/assets/audio/reciters.json`. Edit to your verified list:
```json
{
  "cdnBase": "https://cdn.islamic.network/quran/audio",
  "reciters": [
    { "id": "alafasy",   "edition": "ar.alafasy",   "nameEn": "Mishary Rashid Alafasy",         "nameBn": "", "style": "Murattal", "bitrate": 128, "default": true },
    { "id": "husary",    "edition": "ar.husary",    "nameEn": "Mahmoud Khalil Al-Husary",       "nameBn": "", "style": "Murattal", "bitrate": 128, "default": false },
    { "id": "minshawi",  "edition": "ar.minshawi",  "nameEn": "Mohamed Siddiq Al-Minshawi",     "nameBn": "", "style": "Murattal", "bitrate": 128, "default": false },
    { "id": "sudais",    "edition": "ar.sudais",    "nameEn": "Abdul Rahman Al-Sudais",         "nameBn": "", "style": "Murattal", "bitrate": 192, "default": false },
    { "id": "shuraim",   "edition": "ar.shuraim",   "nameEn": "Saud Al-Shuraim",                "nameBn": "", "style": "Murattal", "bitrate": 128, "default": false },
    { "id": "abdulbasit","edition": "ar.abdulbasit","nameEn": "Abdul Basit Abdul Samad",        "nameBn": "", "style": "Murattal", "bitrate": 192, "default": false }
  ]
}
```
   `nameBn` is a UI label; leave empty if unsure. Exactly one `"default": true`. Delete any reciter you did not verify.
4. Update your store/privacy texts: the app now uses the internet **only** to preview and download recitation audio.
5. When released: keep the app free with no ads. Remove any reciter if they or their estate ask.

---

## 2. Prompt to give the AI

```
You are modifying my existing, working Android app "Quran Sharif" (Kotlin, Jetpack Compose, Material 3, Room, DataStore, Glance widget, Bangla translations selectable). Add exactly ONE feature: recitation audio. Do not refactor unrelated code, rename packages or change the design language. Keep all existing features working. Output every changed or new file in full, each in its own code block headed by its full path (no "...rest unchanged"). At the end list files added/changed/deleted and answer the QA checklist line by line.

ABSOLUTE RULES
- Never write, generate, correct or paraphrase any Quran Arabic text or translation text. Use placeholders in tests and @Preview.
- All Quran text, translations, search, widget and bookmarks stay fully OFFLINE and must never touch the network.
- The network is allowed ONLY for audio preview and audio download. Put ALL network code in one package com.quransharif.app.audio.net (no network call anywhere else). Add <uses-permission android:name="android.permission.INTERNET" /> and nothing else network related. HTTPS only: add res/xml/network_security_config.xml with cleartextTrafficPermitted="false" and reference it from the manifest. No analytics, no ads, no tracking, no third-party SDKs.
- No new network library: use HttpURLConnection inside kotlinx coroutines (Dispatchers.IO).
- Allowed new dependencies: androidx.media3 (media3-exoplayer, media3-session; media3-ui is NOT needed), androidx.work:work-runtime-ktx. Use stable versions that match my current Kotlin/AGP/compileSdk; state the exact versions. Nothing else.
- Every new UI string goes in values/strings.xml (English) and values-bn/strings.xml (Bangla, natural wording). No hard-coded UI strings.
- Update every place that says the app is offline / has no internet permission (About screen text, README, comments): new wording = "Quran text and translations work fully offline. The internet is used only to preview and download recitation audio. No personal data is collected."

--------------------------------------------------------------
1. DATA SOURCE (read-only catalog + URL rules)
--------------------------------------------------------------
- Asset file app/src/main/assets/audio/reciters.json (I provide it). Fields: cdnBase (String), reciters[] with id, edition, nameEn, nameBn (may be empty), style, bitrate (Int), default (Boolean, exactly one true). Parse with org.json. Validate: unique ids, exactly one default, https cdnBase. Invalid entries are skipped and logged, never crash.
- Per-ayah URL: "$cdnBase/$bitrate/$edition/$ayahId.mp3", where ayahId is our existing global ayah.id (1..6236). No mapping table.
- Never hard-code reciter names or URLs in Kotlin; they come from reciters.json (constants only for the preview ayah ids and limits below).

--------------------------------------------------------------
2. STORAGE (so APK size does not grow)
--------------------------------------------------------------
- Files go in app-specific storage: context.getExternalFilesDir("audio") ?: File(filesDir, "audio"). No storage permission is needed. Structure:
  audio/{reciterId}/{ayahId}.mp3   and per completed surah a marker file audio/{reciterId}/surah_{surahId}.done
- A surah is "downloaded" for a reciter only if surah_{n}.done exists AND every required ayah file exists with size > 0. Source of truth = files (no new Room table, no DB migration). Provide AudioStorage with: isSurahDownloaded(reciterId, surahId), downloadedSurahs(reciterId): Set<Int>, sizeBytes(reciterId, surahId?), delete(reciterId, surahId?), totalSizeBytes().
- Required files for a surah = ayah ids of that surah, plus ayah id 1 (the Bismillah recording) for every surah except 1 and 9 when the setting "Play Bismillah before surah" is on (see 5). Download the Bismillah file once per reciter and share it.
- All downloads write to "{ayahId}.mp3.part" and rename to ".mp3" only after the checks in 3.

--------------------------------------------------------------
3. DOWNLOADS (WorkManager)
--------------------------------------------------------------
- DownloadWorker (CoroutineWorker), unique work name "dl_{reciterId}_{surahId}", input = reciterId + surahId. KEEP_EXISTING policy. One surah per work, files downloaded with at most 3 in parallel (Semaphore).
- Constraints: NetworkType.UNMETERED when setting "Download on Wi-Fi only" is on (default ON), otherwise NetworkType.CONNECTED. Also setRequiresStorageNotLow(true).
- Runs as a foreground worker (setForeground with ForegroundInfo, foreground service type dataSync; add the FOREGROUND_SERVICE_DATA_SYNC permission and the correct SystemForegroundService manifest merge entry with foregroundServiceType="dataSync" tools:node="merge"). Notification channel "audio_download", importance LOW, shows "Downloading <reciter> · <surah> n/total" with a progress bar and a Cancel action. On Android 13+ a denied notification permission must not block the download.
- Resume: on retry skip files that already exist complete; Result.retry() with backoff on IOException; maximum 5 attempts per file; HTTP 404/410 = permanent failure for that reciter (mark failure "This recitation is not available", Result.failure()).
- Per-file checks before rename: HTTP 200; if Content-Length is present the bytes written must equal it; size >= 2048 bytes; first bytes are an MP3 (starts with "ID3" or an MPEG frame sync 0xFF followed by a byte with top 3 bits set). A failed check deletes the .part and counts as a failed attempt.
- Check free space first (StatFs): if free space < 150 MB stop with the error "Not enough storage".
- Never download on the main thread; handle ENOSPC, SocketTimeout (connect 15 s, read 30 s), UnknownHost cleanly with user-visible error text, never crash.
- AudioDownloadManager (singleton from AppContainer) exposes StateFlow<Map<Pair<String,Int>, DownloadState>> where DownloadState = Idle | Queued | Running(done, total) | Failed(reason) | Done, built from WorkManager's getWorkInfosForUniqueWorkFlow and from AudioStorage on startup. API: start(reciterId, surahId), cancel(reciterId, surahId), delete(reciterId, surahId).
- Do NOT show an exact size before download (sizes are unknown). Show "<n> ayahs · <bitrate> kbps" and, while running, the live MB downloaded; after completion, the real size on disk.
- On mobile data with Wi-Fi-only OFF, show a one-time confirm dialog the first time: "Audio files can be large. Download using mobile data?"

--------------------------------------------------------------
4. REACTER PICKER WITH VOICE PREVIEW  (name it ReciterPicker)
--------------------------------------------------------------
- Full-screen list or modal bottom sheet. One row per reciter (nameBn if not empty else nameEn, style, bitrate), a radio button for "my default reciter", and a "Play sample" (play/stop) icon button, 48dp.
- Preview: streams a short sample over HTTPS using a SEPARATE lightweight ExoPlayer owned by an AudioPreviewController (not the main playback player): plays Al-Fatiha ayah ids 1, 2, 3 in sequence (constant PREVIEW_AYAH_IDS = listOf(1, 2, 3)), then stops. Only one preview at a time; tapping again stops it; leaving the screen, starting normal playback or the app going to background stops and releases it. Show a small loading indicator while buffering and an inline error text if the network fails.
- Preview does not need the audio to be downloaded and does not write files.
- Selecting a reciter saves it as the default (DataStore key audio_reciter_id; if the saved id no longer exists, fall back to the catalog default).

--------------------------------------------------------------
5. PLAYBACK ENGINE + SYNC
--------------------------------------------------------------
- Media3: PlaybackService : MediaSessionService with an ExoPlayer; foreground service type mediaPlayback (permission FOREGROUND_SERVICE_MEDIA_PLAYBACK, manifest service entry with foregroundServiceType="mediaPlayback" and the MediaSessionService intent filter). Notification channel is created by Media3's default notification provider; give a recognizable small icon. Audio focus handled (setAudioAttributes(CONTENT_TYPE_SPEECH ... handleAudioFocus = true)), setHandleAudioBecomingNoisy(true). Playback continues with the screen off and when the app is in the background. Lock-screen/notification controls: play/pause, previous, next.
- Request POST_NOTIFICATIONS (Android 13+) with an explanation the first time the user starts audio; a denial must not stop playback.
- A singleton PlaybackController (in AppContainer) connects to the service through a MediaController and exposes ONE StateFlow<PlaybackUiState>(reciterId, surahId, currentAyahId: Int?, isBismillahItem: Boolean, isPlaying, isBuffering, speed, repeatMode, error). The UI never talks to ExoPlayer directly.
- Playlist: for surah S and reciter R, build MediaItems from LOCAL files only, in order: [ayah id 1 recording as the Bismillah item, if S != 1 && S != 9 && setting "Play Bismillah before surah" is on] + [all ayahs of S in order]. Each MediaItem carries MediaMetadata extras: ayahId, isBismillah. "Play from ayah n" = same playlist, start index at that ayah. If the surah is not fully downloaded, never play partial audio: open the download sheet instead (see 6).
- Sync: in Player.Listener (onMediaItemTransition, onIsPlayingChanged, onPlaybackStateChanged, onPlayerError, onPositionDiscontinuity) update currentAyahId / isBismillahItem. The Reader observes the StateFlow:
  - highlights the current ayah item (background surfaceVariant + the ayah-number badge in accentGold), and highlights the Bismillah header while the Bismillah item plays;
  - when "Follow audio" is on, animates the list to the new ayah on every change using animateScrollToItem (ayah index + 1 because index 0 is the header), placing it near the top with a small offset; skip the animation if the ayah is already fully visible;
  - follow mode pauses automatically as soon as the user drags/scrolls the list manually (detect via the list's drag interaction), and a small "Follow" chip appears at the top; tapping it re-enables follow and scrolls to the current ayah. It resets to the setting default when a new playback starts;
  - at the end of the last ayah: if repeat = surah, restart; if repeat = off, stop and clear the highlight after a short delay.
- Features: play/pause, previous/next ayah, repeat mode (Off / Repeat ayah / Repeat surah), speed (0.75x, 1.0x, 1.25x, 1.5x) via setPlaybackParameters, tap-and-hold menu or ayah overflow "Play from here".
- Rotation, process death and activity recreation must not stop playback (it lives in the service). When the Reader is reopened it re-syncs from the StateFlow. Leaving the Reader does not stop audio; the mini player (see 6) continues to show.
- If the file for an item is missing/corrupt at play time: skip to the next item, show a snackbar "Some audio files are missing. Download again?" with a button, never crash.
- Release the player when the service is destroyed; stop the service when playback ends and nothing is queued.

--------------------------------------------------------------
6. UI
--------------------------------------------------------------
Reader
- Top bar gets a play icon. Behavior: if the default reciter has this surah downloaded -> start playing from ayah 1 (or from the currently visible first ayah if the user long-pressed "Play from here"). If not downloaded -> open the AudioSheet (below).
- When audio is active for this surah, a mini player bar docks at the bottom of the Reader (above system bars): reciter name, current ayah "n / total", previous, play/pause, next, repeat toggle, speed button, close (stops playback). 56dp tall, uses the app's card style.
- Each ayah's overflow/long-press gets "Play from here".
AudioSheet (modal bottom sheet) for a surah
- Title "Audio · <Bangla surah name>". Shows the currently selected reciter (tap to change -> ReciterPicker) and one state button: Download / Queued / Downloading (live progress, MB, Cancel) / Failed (reason + Retry) / Downloaded (Play + Delete).
- Link row "Choose another reciter" with the preview buttons.
Quran tab
- A small "downloaded" icon on surah rows when the default reciter has that surah downloaded.
Home
- If audio is playing, a compact "Now playing" card (surah, ayah, play/pause) that opens the Reader at the current ayah.
Settings -> new section "Audio" (above About)
- Default reciter (opens ReciterPicker with preview)
- Download on Wi-Fi only (switch, default on)
- Play Bismillah before surah (switch, default ON; if I tell you my audio files already contain it, I will turn it off)
- Follow audio automatically (switch, default on)
- Manage downloads -> DownloadsScreen
DownloadsScreen
- Grouped by reciter: each downloaded surah with size and a delete icon, "Delete all for this reciter" with confirmation, total storage used at the top, and a note "Files are stored in this app's own storage and are removed if you uninstall the app."
About screen -> add a block "Recitation audio": "Audio is provided by the Al Quran Cloud / Islamic Network CDN (cdn.islamic.network). Copyright of every recitation belongs to the reciter, licensed for free non-commercial redistribution. This app is non-profit. If a reciter or rights holder asks for removal, the recitation will be removed." (plain text, no network call) plus the list of reciters from reciters.json.

--------------------------------------------------------------
7. SETTINGS KEYS (DataStore)
--------------------------------------------------------------
audio_reciter_id (String, default = catalog default), audio_wifi_only (Boolean, true), audio_bismillah_prefix (Boolean, true), audio_follow (Boolean, true), audio_speed (Float, 1.0), audio_repeat_mode (String OFF/AYAH/SURAH, OFF).

--------------------------------------------------------------
8. TESTS (pure JVM, no Quran text)
--------------------------------------------------------------
- UrlBuilder: URL for (alafasy,128,ayahId 1) and 6236 matches the pattern exactly.
- Ayah id arithmetic with the standard counts: surah 1 ayah 1 = id 1, surah 2 ayah 1 = id 8, surah 114 ayah 6 = id 6236; playlist for surah 9 has no Bismillah item, for surah 1 none, for surah 2 starts with the Bismillah item when the setting is on.
- Mp3 header check: accepts ID3 and 0xFF 0xFB/0xF3/0xF2 starts, rejects HTML (e.g. "<!DOCTYPE") and empty files.
- AudioStorage: marker/partial logic (a surah with a missing ayah file is NOT downloaded; .part files are ignored and deleted).
- reciters.json parsing: duplicates, missing default, wrong scheme.

--------------------------------------------------------------
9. QA CHECKLIST — answer EVERY line
--------------------------------------------------------------
- [ ] Manifest: INTERNET + media/dataSync foreground permissions only; cleartext traffic disabled; no network code outside audio.net.
- [ ] Offline test: airplane mode -> text, translations, search, widget, bookmarks all work; audio UI shows clear messages; already downloaded surahs still play offline.
- [ ] Preview plays 3 ayahs from the CDN for each reciter and stops cleanly on leave/background; only one preview at a time.
- [ ] Download: progress, cancel, resume after killing the app, Wi-Fi-only respected, 404 handled, low storage handled, corrupt files rejected.
- [ ] Downloaded files live in app-specific storage; APK size unchanged; uninstall removes them; delete from Downloads screen frees space.
- [ ] Playback continues with screen off and in the background; lock-screen/notification controls work; headphones unplug pauses.
- [ ] Sync: highlight and scroll follow every ayah change, Bismillah header highlights during the Bismillah item, manual scroll pauses follow, "Follow" chip resumes, repeat ayah/surah and speed work.
- [ ] Rotation, process death, switching translation, theme and language do not stop playback or lose the highlight.
- [ ] Surah 1 and 9 never get a Bismillah item; other surahs follow the setting.
- [ ] Android 13+ notification permission denial does not break playback or downloads.
- [ ] Existing features (reader, search, bookmarks, widget, translations, widget text size) still work; release build with R8 works; assembleDebug, assembleRelease and ./gradlew test pass.
```

---

## 3. After the AI answers

1. Replace/add the files it lists and add `reciters.json` to `assets/audio/`. Sync Gradle.
2. Run the unit tests first.
3. Test on a real phone: preview each reciter, download a short surah (for example Surah 112), play it, turn the screen off, scroll manually, and watch the highlight.
4. Do the Bismillah listen test (Section 0) and turn the Settings switch off if the ayah-1 files already include it.
5. If something fails, send the AI the exact error message or logcat lines.

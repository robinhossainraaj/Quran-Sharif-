package com.example.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AppTitle
import com.example.ui.components.AppTopBar
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.audio.download.DownloadState
import com.example.data.import.BismillahHelper
import com.example.ui.audio.AudioSheet
import com.example.ui.audio.MiniPlayerBar
import com.example.ui.audio.ReciterPickerSheet
import com.example.ui.theme.extraColors
import com.example.ui.theme.getArabicTextStyle
import com.example.util.BanglaDigits
import com.example.util.ShareUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    targetAyahNo: Int = -1,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var highlightedAyahNo by rememberSaveable { mutableIntStateOf(-1) }
    var hasScrolledToTarget by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showGoToDialog by remember { mutableStateOf(false) }

    // Audio sheets state
    var showAudioSheet by remember { mutableStateOf(false) }
    var showReciterPicker by remember { mutableStateOf(false) }
    var showMobileDataConfirm by remember { mutableStateOf(false) }

    val audioSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reciterPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val catalog = remember { ReciterCatalogLoader.load(context) }
    val currentReciter = catalog.findById(state.userSettings.audioReciterId) ?: catalog.defaultReciter()

    // Follow audio state: false = following (auto scroll + highlight), true = user turned it off
    var userPausedFollow by rememberSaveable { mutableStateOf(false) }
    // Floating player: big player or small bar
    var playerExpanded by rememberSaveable { mutableStateOf(true) }

    BackHandler {
        onNavigateBack()
    }

    // Keep screen on while reading if enabled in settings
    val keepScreenOn = state.userSettings.keepScreenOn
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (keepScreenOn) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Scroll to target ayah and highlight for ~2 seconds
    LaunchedEffect(state.ayahs, targetAyahNo) {
        if (!hasScrolledToTarget && state.ayahs.isNotEmpty() && targetAyahNo >= 1) {
            val targetIndex = targetAyahNo.coerceIn(1, state.ayahs.size)
            listState.scrollToItem(index = targetIndex)
            highlightedAyahNo = targetAyahNo
            hasScrolledToTarget = true
            delay(2000)
            highlightedAyahNo = -1
        }
    }

    // Track last read position when scrolling stops (debounced)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(800)
            .distinctUntilChanged()
            .collect { firstIndex ->
                if (firstIndex >= 1 && state.ayahs.isNotEmpty()) {
                    val ayahNo = firstIndex.coerceIn(1, state.ayahs.size)
                    viewModel.saveLastRead(ayahNo)
                }
            }
    }

    // Detect a REAL finger drag to turn "Follow" off.
    // Old code used listState.isScrollInProgress, but that is also true while the app
    // scrolls by itself to the playing ayah. So the app switched Follow off on its own,
    // and auto-scroll stopped after the first ayah. A drag interaction only comes from the finger.
    val thisSurahAudioActive by rememberUpdatedState(
        playbackState.isActive && playbackState.surahId == viewModel.surahId
    )
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start && thisSurahAudioActive) {
                userPausedFollow = true
            }
        }
    }

    // Follow audio automatic scroll (runs again when the user turns Follow back on)
    val followAudioEnabled = state.userSettings.audioFollow
    LaunchedEffect(
        playbackState.currentAyahNo,
        playbackState.isBismillahItem,
        playbackState.isPlaying,
        userPausedFollow
    ) {
        if (followAudioEnabled && !userPausedFollow && playbackState.isActive && playbackState.surahId == viewModel.surahId) {
            if (playbackState.isBismillahItem) {
                listState.animateScrollToItem(index = 0)
            } else {
                val targetItemIndex = playbackState.currentAyahNo // Index 0 is header card, ayah 1 is index 1
                if (targetItemIndex in 1..state.ayahs.size) {
                    listState.animateScrollToItem(index = targetItemIndex, scrollOffset = -30)
                }
            }
        }
    }

    val currentSurahDownloadState = downloadStates[Pair(currentReciter.id, viewModel.surahId)]
        ?: if (viewModel.isSurahDownloaded(currentReciter.id)) DownloadState.Done else DownloadState.Idle

    val isCurrentSurahPlaying = playbackState.isActive && playbackState.surahId == viewModel.surahId

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { AppTitle(state.surah?.banglaName ?: stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Audio Playback / Sheet Icon
                    IconButton(
                        onClick = {
                            scope.launch {
                                val isDownloaded = viewModel.isSurahDownloaded(currentReciter.id)
                                if (isDownloaded) {
                                    if (isCurrentSurahPlaying) {
                                        viewModel.playOrPause()
                                    } else {
                                        userPausedFollow = false
                                        val firstVisible = listState.firstVisibleItemIndex.coerceAtLeast(1)
                                        viewModel.playSurah(firstVisible)
                                    }
                                } else {
                                    showAudioSheet = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("audio_play_button")
                    ) {
                        Icon(
                            imageVector = if (isCurrentSurahPlaying && playbackState.isPlaying) {
                                Icons.Default.Pause
                            } else if (isCurrentSurahPlaying) {
                                Icons.Default.PlayArrow
                            } else {
                                Icons.Default.Headphones
                            },
                            contentDescription = stringResource(R.string.audio_play_button),
                            tint = if (isCurrentSurahPlaying) MaterialTheme.extraColors.accentGold else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // More Menu
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("reader_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.go_to_ayah),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.audio_title)) },
                            onClick = {
                                showMenu = false
                                showAudioSheet = true
                            },
                            modifier = Modifier.testTag("menu_audio_sheet")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.go_to_ayah)) },
                            onClick = {
                                showMenu = false
                                showGoToDialog = true
                            },
                            modifier = Modifier.testTag("menu_go_to_ayah")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookmark_surah_start)) },
                            onClick = {
                                showMenu = false
                                viewModel.bookmarkSurahStart()
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.surah_bookmarked))
                                }
                            },
                            modifier = Modifier.testTag("menu_bookmark_surah_start")
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val surah = state.surah
            val showBismillah = surah != null && BismillahHelper.shouldShowBismillahHeader(surah.id)
            val isBismillahPlaying = isCurrentSurahPlaying && playbackState.isBismillahItem

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = if (isCurrentSurahPlaying) {
                            if (playerExpanded) 310.dp else 100.dp
                        } else {
                            24.dp
                        }
                    )
                ) {
                    // Header card (index 0)
                    item(key = "surah_header") {
                        if (surah != null) {
                            SurahHeaderCard(
                                surah = surah,
                                showBismillah = showBismillah,
                                bismillahArabic = state.bismillahArabic,
                                bismillahBangla = state.bismillahBangla,
                                useBanglaDigits = state.userSettings.banglaDigits,
                                showBangla = state.userSettings.showBangla,
                                isBismillahPlaying = isBismillahPlaying
                            )
                        }
                    }

                    // Ayah items
                    itemsIndexed(state.ayahs, key = { _, ayah -> ayah.id }) { _, ayah ->
                        val isBookmarked = state.bookmarkedAyahIds.contains(ayah.id)
                        val isHighlighted = (ayah.ayahNo == highlightedAyahNo)
                        val isPlayingThisAyah = isCurrentSurahPlaying &&
                                !playbackState.isBismillahItem &&
                                (playbackState.currentAyahNo == ayah.ayahNo)

                        AyahItem(
                            ayah = ayah,
                            isBookmarked = isBookmarked,
                            isHighlighted = isHighlighted,
                            isPlayingAudio = isPlayingThisAyah,
                            useBanglaDigits = state.userSettings.banglaDigits,
                            arabicFontSizeSp = state.userSettings.arabicSizeSp,
                            banglaFontSizeSp = state.userSettings.banglaSizeSp,
                            showBangla = state.userSettings.showBangla,
                            onBookmarkClick = {
                                viewModel.toggleBookmark(ayah.id)
                                scope.launch {
                                    val msg = if (isBookmarked) {
                                        context.getString(R.string.bookmark_removed)
                                    } else {
                                        context.getString(R.string.bookmark_added)
                                    }
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onCopyClick = {
                                val textToCopy = buildString {
                                    append(ayah.arabicDisplay)
                                    if (state.userSettings.showBangla) {
                                        append("\n\n")
                                        append(ayah.bangla)
                                    }
                                    append("\n— ${surah?.banglaName ?: ""} [${ayah.surahId}:${ayah.ayahNo}]")
                                }
                                ShareUtils.copyToClipboard(context, "Ayah", textToCopy)
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                                }
                            },
                            onShareClick = {
                                val shareText = buildString {
                                    append(ayah.arabicDisplay)
                                    if (state.userSettings.showBangla) {
                                        append("\n\n")
                                        append(ayah.bangla)
                                    }
                                    append("\n— ${surah?.banglaName ?: ""} [${ayah.surahId}:${ayah.ayahNo}]")
                                    append("\n\n${context.getString(R.string.shared_via_app)}")
                                }
                                ShareUtils.shareText(
                                    context,
                                    shareText,
                                    "${surah?.banglaName ?: ""} ${ayah.ayahNo}"
                                )
                            },
                            onPlayFromHere = {
                                scope.launch {
                                    userPausedFollow = false
                                    val ok = viewModel.playSurah(startAyahNo = ayah.ayahNo)
                                    if (!ok) {
                                        showAudioSheet = true
                                    }
                                }
                            }
                        )
                    }
                }

                // Follow switch, shown while this surah's audio is active.
                //   "Following" (filled): the list scrolls and highlights with the audio.
                //   "Follow" (outlined): auto scroll is off, audio keeps playing. Tap to follow again.
                // Dragging the list with a finger switches Follow off by itself.
                AnimatedVisibility(
                    visible = isCurrentSurahPlaying && followAudioEnabled,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    val following = !userPausedFollow
                    AssistChip(
                        onClick = { userPausedFollow = following },
                        label = {
                            Text(
                                stringResource(
                                    if (following) R.string.following_audio_chip else R.string.follow_audio_chip
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (following) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (following) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (following) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ),
                        border = AssistChipDefaults.assistChipBorder(true),
                        modifier = Modifier.testTag("follow_audio_chip")
                    )
                }

                // Right-Side Fast Scroll Handle
                if (surah != null && surah.ayahCount > 3) {
                    FastScrollHandle(
                        listState = listState,
                        totalAyahs = surah.ayahCount,
                        useBanglaDigits = state.userSettings.banglaDigits,
                        onScrollToAyah = { targetAyah ->
                            // Using the side handle is the user moving by hand: stop following
                            if (isCurrentSurahPlaying) userPausedFollow = true
                            scope.launch {
                                listState.scrollToItem(targetAyah)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                // Docked Mini Player Bar at bottom
                AnimatedVisibility(
                    visible = isCurrentSurahPlaying,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    MiniPlayerBar(
                        state = playbackState,
                        useBanglaDigits = state.userSettings.banglaDigits,
                        expanded = playerExpanded,
                        onExpandedChange = { playerExpanded = it },
                        onPlayPause = { viewModel.playOrPause() },
                        onPrevious = { viewModel.skipToPrevious() },
                        onNext = { viewModel.skipToNext() },
                        onRepeatToggle = { viewModel.setRepeatMode(playbackState.repeatMode.next()) },
                        onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
                        onSeek = { viewModel.seekInAyah(it) },
                        onChangeReciter = {
                            // Stop the recitation while the person listens to reciter previews
                            viewModel.pauseAudio()
                            showReciterPicker = true
                        },
                        onClose = { viewModel.stopAudio() }
                    )
                }
            }
        }
    }

    // Audio Sheet
    if (showAudioSheet && state.surah != null) {
        val surah = state.surah!!
        val diskSize = viewModel.getSurahDiskSizeBytes(currentReciter.id)

        AudioSheet(
            sheetState = audioSheetState,
            surahId = surah.id,
            surahBanglaName = surah.banglaName,
            ayahCount = surah.ayahCount,
            reciter = currentReciter,
            downloadState = currentSurahDownloadState,
            surahDiskSizeBytes = diskSize,
            useBanglaDigits = state.userSettings.banglaDigits,
            onStartDownload = {
                val wifiOnly = state.userSettings.audioWifiOnly
                val warned = state.userSettings.mobileDataWarningShown
                if (!wifiOnly && !warned) {
                    showMobileDataConfirm = true
                } else {
                    viewModel.startDownload(currentReciter.id)
                }
            },
            onCancelDownload = { viewModel.cancelDownload(currentReciter.id) },
            onDeleteAudio = { viewModel.deleteAudio(currentReciter.id) },
            onPlayAudio = {
                userPausedFollow = false
                scope.launch {
                    val firstVisible = listState.firstVisibleItemIndex.coerceAtLeast(1)
                    viewModel.playSurah(firstVisible, currentReciter.id)
                }
            },
            onChangeReciterClick = {
                showReciterPicker = true
            },
            onDismiss = { showAudioSheet = false }
        )
    }

    // Reciter Picker Sheet
    if (showReciterPicker) {
        ReciterPickerSheet(
            sheetState = reciterPickerSheetState,
            reciters = catalog.reciters,
            selectedReciterId = currentReciter.id,
            previewController = viewModel.audioPreviewController,
            onSelectReciter = { newReciter ->
                viewModel.setAudioReciterId(newReciter.id)
                showReciterPicker = false
                if (isCurrentSurahPlaying) {
                    // Keep listening from the same ayah with the new reciter.
                    // If this reciter's audio is not downloaded yet, open the download sheet.
                    scope.launch {
                        userPausedFollow = false
                        val ok = viewModel.playSurah(
                            startAyahNo = playbackState.currentAyahNo.coerceAtLeast(1),
                            reciterId = newReciter.id
                        )
                        if (!ok) showAudioSheet = true
                    }
                }
            },
            onDismiss = { showReciterPicker = false }
        )
    }

    // One-time Mobile Data Download Warning Dialog
    if (showMobileDataConfirm) {
        AlertDialog(
            onDismissRequest = { showMobileDataConfirm = false },
            title = { Text(stringResource(R.string.audio_mobile_data_warning_title)) },
            text = { Text(stringResource(R.string.audio_mobile_data_warning_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMobileDataConfirm = false
                        viewModel.setMobileDataWarningShown()
                        viewModel.startDownload(currentReciter.id)
                    }
                ) {
                    Text(stringResource(R.string.audio_download_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMobileDataConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Go to Ayah Dialog
    if (showGoToDialog) {
        var inputAyahText by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }
        val maxAyah = state.surah?.ayahCount ?: 1
        val maxAyahStr = if (state.userSettings.banglaDigits) BanglaDigits.convert(maxAyah) else maxAyah.toString()

        AlertDialog(
            onDismissRequest = { showGoToDialog = false },
            title = { Text(stringResource(R.string.go_to_ayah)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.ayah_number_hint, maxAyah),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputAyahText,
                        onValueChange = {
                            inputAyahText = it
                            inputError = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("go_to_ayah_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedNo = BanglaDigits.toAscii(inputAyahText.trim()).toIntOrNull()
                        if (parsedNo != null && parsedNo in 1..maxAyah) {
                            showGoToDialog = false
                            scope.launch {
                                listState.scrollToItem(parsedNo)
                                highlightedAyahNo = parsedNo
                                delay(2000)
                                highlightedAyahNo = -1
                            }
                        } else {
                            inputError = context.getString(R.string.ayah_out_of_range)
                        }
                    },
                    modifier = Modifier.testTag("go_to_ayah_confirm")
                ) {
                    Text(stringResource(R.string.go_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SurahHeaderCard(
    surah: com.example.data.db.SurahEntity,
    showBismillah: Boolean,
    bismillahArabic: String,
    bismillahBangla: String,
    useBanglaDigits: Boolean,
    showBangla: Boolean,
    isBismillahPlaying: Boolean = false
) {
    val typeStr = if (surah.revelationType.equals("Meccan", ignoreCase = true)) {
        stringResource(R.string.meccan)
    } else {
        stringResource(R.string.medinan)
    }
    val ayahCountStr = if (useBanglaDigits) BanglaDigits.convert(surah.ayahCount) else surah.ayahCount.toString()
    val surahNoStr = if (useBanglaDigits) BanglaDigits.convert(surah.id) else surah.id.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            .testTag("surah_header_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Surah Number and Meta
            Text(
                text = "সূরা নম্বর $surahNoStr · $typeStr · ${stringResource(R.string.ayah_count_format, ayahCountStr)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extraColors.secondaryText
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Large Arabic Name
            Text(
                text = surah.arabicName,
                style = getArabicTextStyle(34),
                color = MaterialTheme.extraColors.accentGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Bangla Name & English Meaning
            Text(
                text = surah.banglaName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${surah.transliteration} • ${surah.englishMeaning}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bismillah Header (if applicable)
            if (showBismillah && bismillahArabic.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    thickness = 0.8.dp,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isBismillahPlaying) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = bismillahArabic,
                            style = getArabicTextStyle(26),
                            color = MaterialTheme.extraColors.accentGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showBangla && bismillahBangla.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = bismillahBangla,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

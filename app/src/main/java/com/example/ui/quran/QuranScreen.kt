package com.example.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import com.example.QuranApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AppTitle
import com.example.ui.components.AppTopBar
import com.example.data.db.SurahEntity
import com.example.ui.theme.extraColors
import com.example.ui.theme.getArabicTextStyle
import com.example.util.BanglaDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    viewModel: QuranViewModel,
    initialTab: Int = 0,
    onNavigateToSearch: () -> Unit,
    onNavigateToReader: (surahId: Int, ayahNo: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialTab) {
        if (initialTab in 0..1) {
            viewModel.selectTab(initialTab)
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { AppTitle(stringResource(R.string.nav_quran)) },
                showStrip = false
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Row (Surah / Juz)
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.extraColors.accentGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                        color = MaterialTheme.extraColors.accentGold
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    selectedContentColor = MaterialTheme.extraColors.accentGold,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = stringResource(R.string.tab_surah),
                            fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    },
                    modifier = Modifier.testTag("surah_tab")
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    selectedContentColor = MaterialTheme.extraColors.accentGold,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = stringResource(R.string.tab_juz),
                            fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    },
                    modifier = Modifier.testTag("juz_tab")
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                ) {
                    if (state.selectedTab == 0) {
                        SurahList(
                            surahs = state.surahs,
                            useBanglaDigits = state.userSettings.banglaDigits,
                            defaultReciterId = state.userSettings.audioReciterId,
                            onSurahClick = { surahId -> onNavigateToReader(surahId, 1) }
                        )
                    } else {
                        JuzList(
                            juzs = state.juzs,
                            useBanglaDigits = state.userSettings.banglaDigits,
                            onJuzClick = { surahId, ayahNo -> onNavigateToReader(surahId, ayahNo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahList(
    surahs: List<SurahEntity>,
    useBanglaDigits: Boolean,
    defaultReciterId: String,
    onSurahClick: (surahId: Int) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as QuranApp
    val downloadedSurahs = remember(defaultReciterId) {
        app.container.audioStorage.downloadedSurahIds(defaultReciterId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(surahs, key = { it.id }) { surah ->
            SurahRowItem(
                surah = surah,
                useBanglaDigits = useBanglaDigits,
                isAudioDownloaded = downloadedSurahs.contains(surah.id),
                onClick = { onSurahClick(surah.id) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun SurahRowItem(
    surah: SurahEntity,
    useBanglaDigits: Boolean,
    isAudioDownloaded: Boolean = false,
    onClick: () -> Unit
) {
    val numberStr = if (useBanglaDigits) BanglaDigits.convert(surah.id) else surah.id.toString()
    val typeStr = if (surah.revelationType.equals("Meccan", ignoreCase = true)) {
        stringResource(R.string.meccan)
    } else {
        stringResource(R.string.medinan)
    }
    val ayahsCountStr = if (useBanglaDigits) BanglaDigits.convert(surah.ayahCount) else surah.ayahCount.toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("surah_item_${surah.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Octagon/Diamond badge for number
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = numberStr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center info: Bangla name + transliteration + type / ayah count
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = surah.banglaName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${surah.transliteration})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAudioDownloaded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = stringResource(R.string.audio_download_completed),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$typeStr · ${stringResource(R.string.ayah_count_format, ayahsCountStr)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Arabic surah name in Amiri Quran 24sp
        Text(
            text = surah.arabicName,
            style = getArabicTextStyle(24),
            color = MaterialTheme.extraColors.accentGold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun JuzList(
    juzs: List<JuzWithSurah>,
    useBanglaDigits: Boolean,
    onJuzClick: (surahId: Int, ayahNo: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(juzs, key = { it.juz.index }) { item ->
            val juzIndexStr = if (useBanglaDigits) BanglaDigits.convert(item.juz.index) else item.juz.index.toString()
            val startSurahStr = if (useBanglaDigits) BanglaDigits.convert(item.juz.startSurah) else item.juz.startSurah.toString()
            val startAyahStr = if (useBanglaDigits) BanglaDigits.convert(item.juz.startAyah) else item.juz.startAyah.toString()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJuzClick(item.juz.startSurah, item.juz.startAyah) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("juz_item_${item.juz.index}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = juzIndexStr,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.juz_format, juzIndexStr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.juz_starts_at,
                            item.surahBanglaName,
                            startSurahStr,
                            startAyahStr
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

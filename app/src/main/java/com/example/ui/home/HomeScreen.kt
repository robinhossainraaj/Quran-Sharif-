package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AppTitle
import com.example.ui.components.AppTopBar
import com.example.ui.theme.extraColors
import com.example.ui.theme.getArabicTextStyle
import com.example.ui.theme.getBanglaTextStyle
import com.example.util.BanglaDigits
import com.example.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToReader: (surahId: Int, ayahNo: Int) -> Unit,
    onNavigateToQuranTab: (tabIndex: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { AppTitle(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_hint),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. Now Playing Audio Card (when recitation is playing)
                if (state.playbackState.isActive && state.playbackState.surahId > 0) {
                    val pState = state.playbackState
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToReader(pState.surahId, pState.currentAyahNo.coerceAtLeast(1))
                            }
                            .testTag("home_now_playing_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayback() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("home_now_playing_toggle")
                            ) {
                                Icon(
                                    imageVector = if (pState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (pState.isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.audio_playing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                val surahTitle = if (pState.surahName.isNotBlank()) pState.surahName else "Surah ${pState.surahId}"
                                val ayahStr = if (pState.currentAyahNo > 0) {
                                    if (state.userSettings.banglaDigits) BanglaDigits.convert(pState.currentAyahNo) else pState.currentAyahNo.toString()
                                } else ""
                                Text(
                                    text = if (ayahStr.isNotBlank()) "$surahTitle · $ayahStr" else surahTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (pState.reciterName.isNotBlank()) {
                                    Text(
                                        text = pState.reciterName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.stopPlayback() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("home_now_playing_close")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // 1. Ayah of the Moment Card (28dp radius)
                if (state.currentAyah != null && state.currentSurah != null) {
                    val ayah = state.currentAyah!!
                    val surah = state.currentSurah!!
                    val useBangla = state.userSettings.banglaDigits

                    val refText = if (useBangla) {
                        "সূরা ${surah.banglaName} · ${BanglaDigits.convert(ayah.surahId)}:${BanglaDigits.convert(ayah.ayahNo)}"
                    } else {
                        "Surah ${surah.banglaName} · ${ayah.surahId}:${ayah.ayahNo}"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .testTag("ayah_of_the_moment_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Reference and Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = refText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.extraColors.secondaryText,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = stringResource(R.string.ayah_of_the_moment),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.extraColors.secondaryText.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Arabic Ayah Text (Amiri Quran font, Gold)
                            Text(
                                text = ayah.arabicDisplay,
                                style = getArabicTextStyle(state.userSettings.arabicSizeSp),
                                color = MaterialTheme.extraColors.accentGold,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bangla Translation
                            if (state.userSettings.showBangla) {
                                Text(
                                    text = ayah.bangla,
                                    style = getBanglaTextStyle(state.userSettings.banglaSizeSp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Card Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Refresh Ayah
                                    IconButton(
                                        onClick = { viewModel.refreshAyah() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("refresh_ayah_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.refresh_ayah),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Bookmark Toggle
                                    IconButton(
                                        onClick = { viewModel.toggleBookmark() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("bookmark_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (state.isAyahBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = stringResource(R.string.nav_bookmarks),
                                            tint = if (state.isAyahBookmarked) MaterialTheme.extraColors.accentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Share
                                    IconButton(
                                        onClick = {
                                            val shareText = ShareUtils.formatAyahText(
                                                arabicDisplay = ayah.arabicDisplay,
                                                bangla = ayah.bangla,
                                                surahBanglaName = surah.banglaName,
                                                surahId = ayah.surahId,
                                                ayahNo = ayah.ayahNo,
                                                useBanglaDigits = useBangla,
                                                translatorName = state.translationInfo?.nameEn
                                            )
                                            ShareUtils.shareText(
                                                context = context,
                                                text = shareText,
                                                title = context.getString(R.string.share_ayah)
                                            )
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("share_ayah_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = stringResource(R.string.share_ayah),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Read Surah button
                                TextButton(
                                    onClick = { onNavigateToReader(ayah.surahId, ayah.ayahNo) },
                                    modifier = Modifier.testTag("read_surah_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.read_surah),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else if (state.isLoading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // 2. Continue Reading Card (if exists)
                if (state.lastReadSurah != null && state.lastReadAyahNo > 0) {
                    val surah = state.lastReadSurah!!
                    val ayahNo = state.lastReadAyahNo
                    val ayahStr = if (state.userSettings.banglaDigits) BanglaDigits.convert(ayahNo) else ayahNo.toString()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToReader(surah.id, ayahNo) }
                            .testTag("continue_reading_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = stringResource(R.string.continue_reading),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.continue_reading),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.extraColors.secondaryText
                                )
                                Text(
                                    text = stringResource(R.string.last_read_reference, surah.banglaName, ayahStr),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 3. Quick Links Section
                Text(
                    text = stringResource(R.string.quick_links),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Surah List Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToQuranTab(0) }
                            .testTag("surah_list_quick_link"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = stringResource(R.string.surah_list_card),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.surah_list_card),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.surah_list_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Juz List Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToQuranTab(1) }
                            .testTag("juz_list_quick_link"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = stringResource(R.string.juz_list_card),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.juz_list_card),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.juz_list_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

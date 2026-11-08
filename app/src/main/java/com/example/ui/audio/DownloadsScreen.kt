package com.example.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QuranApp
import com.example.R
import com.example.audio.catalog.Reciter
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.data.db.SurahEntity
import com.example.util.BanglaDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadedSurahItem(
    val surah: SurahEntity,
    val sizeBytes: Long
)

data class ReciterDownloadsGroup(
    val reciter: Reciter,
    val surahs: List<DownloadedSurahItem>,
    val totalSizeBytes: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as QuranApp
    val container = app.container
    val storage = container.audioStorage

    var groups by remember { mutableStateOf<List<ReciterDownloadsGroup>>(emptyList()) }
    var totalStorageUsed by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    var deleteReciterConfirm by remember { mutableStateOf<Reciter?>(null) }
    var deleteSurahConfirm by remember { mutableStateOf<Pair<Reciter, SurahEntity>?>(null) }

    fun refreshData() {
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                val catalog = ReciterCatalogLoader.load(context)
                val allSurahs = container.quranRepository.surahs().first().associateBy { it.id }
                val newGroups = mutableListOf<ReciterDownloadsGroup>()

                for (reciter in catalog.reciters) {
                    val surahIds = storage.downloadedSurahIds(reciter.id)
                    if (surahIds.isNotEmpty()) {
                        val items = surahIds.mapNotNull { sId ->
                            val surah = allSurahs[sId]
                            if (surah != null) {
                                val size = storage.surahSizeBytes(reciter.id, (1..surah.ayahCount).toList())
                                DownloadedSurahItem(surah, size)
                            } else null
                        }.sortedBy { it.surah.id }

                        val reciterTotalSize = storage.reciterSizeBytes(reciter.id)
                        newGroups.add(ReciterDownloadsGroup(reciter, items, reciterTotalSize))
                    }
                }
                val totalBytes = storage.totalSizeBytes()

                withContext(Dispatchers.Main) {
                    groups = newGroups
                    totalStorageUsed = totalBytes
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.audio_manage_downloads),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("downloads_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (groups.isEmpty() && !isLoading) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.audio_downloads_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.audio_downloads_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total storage banner & note
                    item(key = "storage_header") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.audio_storage_used, formatFileSize(totalStorageUsed)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.audio_storage_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Reciter groups
                    items(groups, key = { it.reciter.id }) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.reciter.displayName(true),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${group.reciter.nameEn} · ${formatFileSize(group.totalSizeBytes)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = { deleteReciterConfirm = group.reciter },
                                        modifier = Modifier.testTag("delete_reciter_${group.reciter.id}")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.audio_delete_all_reciter),
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                group.surahs.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${BanglaDigits.convert(item.surah.id)}. ${item.surah.banglaName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${item.surah.englishMeaning} · ${formatFileSize(item.sizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { deleteSurahConfirm = Pair(group.reciter, item.surah) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.audio_delete_surah),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete single surah confirmation dialog
    deleteSurahConfirm?.let { (reciter, surah) ->
        AlertDialog(
            onDismissRequest = { deleteSurahConfirm = null },
            title = { Text(stringResource(R.string.audio_delete_surah)) },
            text = { Text(stringResource(R.string.audio_delete_surah_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rId = reciter.id
                        val sId = surah.id
                        deleteSurahConfirm = null
                        scope.launch(Dispatchers.IO) {
                            storage.deleteSurah(rId, sId, (1..surah.ayahCount).toList())
                            refreshData()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSurahConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete all reciter audio confirmation dialog
    deleteReciterConfirm?.let { reciter ->
        AlertDialog(
            onDismissRequest = { deleteReciterConfirm = null },
            title = { Text(stringResource(R.string.audio_delete_all_reciter)) },
            text = { Text(stringResource(R.string.audio_delete_all_reciter_confirm, reciter.displayName(true))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rId = reciter.id
                        deleteReciterConfirm = null
                        scope.launch(Dispatchers.IO) {
                            storage.deleteReciter(rId)
                            refreshData()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteReciterConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

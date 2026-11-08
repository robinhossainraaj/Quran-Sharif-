package com.example.ui.translations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.db.TranslationInfoEntity
import com.example.data.translations.CatalogState
import com.example.data.translations.DownloadStatus
import com.example.data.translations.RemoteTranslation
import com.example.ui.components.AppTitle
import com.example.ui.components.AppTopBar
import com.example.ui.components.SectionTitle
import com.example.ui.theme.extraColors
import com.example.util.restartApp
import java.util.Locale
import kotlinx.coroutines.launch

private data class LangGroup(
    val code: String,
    val english: String,
    val native: String,
    val items: List<RemoteTranslation>
)

private fun englishName(code: String, sample: RemoteTranslation): String {
    val fromLocale = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH)
    if (fromLocale.isNotBlank() && !fromLocale.equals(code, ignoreCase = true)) return fromLocale
    // Fallback: take the language from the catalogue title, e.g. "Filipino Translation (Tagalog) - ..."
    val head = sample.title.substringBefore(" - ")
    return head.replace(Regex("\\s*translation", RegexOption.IGNORE_CASE), "").trim().ifBlank { code }
}

private fun nativeName(code: String, english: String): String {
    val loc = Locale.forLanguageTag(code)
    val n = loc.getDisplayLanguage(loc)
    return if (n.isBlank() || n.equals(code, ignoreCase = true) || n.equals(english, ignoreCase = true)) "" else n
}

@Composable
fun TranslationsScreen(
    viewModel: TranslationsViewModel,
    onNavigateBack: () -> Unit
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()

    var langCode by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = langCode != null) { langCode = null }
    LaunchedEffect(Unit) { viewModel.completed.collect { pendingId = it } }

    val groups: List<LangGroup> = remember(catalog) {
        val loaded = (catalog as? CatalogState.Loaded)?.items.orEmpty()
        loaded.groupBy { it.languageCode }
            .map { (code, list) ->
                val en = englishName(code, list.first())
                LangGroup(code, en, nativeName(code, en), list)
            }
            .sortedBy { it.english.lowercase() }
    }
    val currentGroup = groups.firstOrNull { it.code == langCode }
    val titleText = currentGroup?.english ?: stringResource(R.string.tr_title)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { AppTitle(titleText) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (langCode != null) langCode = null else onNavigateBack() },
                        modifier = Modifier.testTag("translations_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentGroup == null) {
                // 1. Translations already on the phone (including the built-in Bengali ones)
                if (installed.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.tr_installed_section)) }
                    items(installed, key = { "inst_${it.id}" }) { info ->
                        InstalledCard(
                            info = info,
                            inUse = info.id == selectedId,
                            onUse = { pendingId = info.id }
                        )
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }

                // 2. Languages from the online catalogue
                item { SectionTitle(stringResource(R.string.tr_languages_section)) }
                when (val c = catalog) {
                    is CatalogState.Loading -> item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.tr_loading),
                                color = MaterialTheme.extraColors.secondaryText
                            )
                        }
                    }
                    is CatalogState.Error -> item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tr_load_failed),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = c.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extraColors.secondaryText
                            )
                            Button(
                                onClick = { viewModel.reload() },
                                modifier = Modifier.testTag("translations_retry_button")
                            ) { Text(stringResource(R.string.tr_retry)) }
                        }
                    }
                    is CatalogState.Loaded -> items(groups, key = { "lang_${it.code}" }) { g ->
                        LanguageCard(group = g, onClick = { langCode = g.code })
                    }
                }
            } else {
                items(currentGroup.items, key = { it.key }) { item ->
                    EditionCard(
                        item = item,
                        status = status[item.id],
                        isInstalled = installed.any { it.id == item.id },
                        inUse = item.id == selectedId,
                        onDownload = { viewModel.download(item) },
                        onCancel = { viewModel.cancel(item.id) },
                        onUse = { pendingId = item.id }
                    )
                }
            }
        }
    }

    val pending = pendingId
    if (pending != null) {
        val name = installed.firstOrNull { it.id == pending }?.nameEn
            ?: (catalog as? CatalogState.Loaded)?.items?.firstOrNull { it.id == pending }?.title
            ?: pending
        AlertDialog(
            onDismissRequest = { pendingId = null },
            title = { Text(stringResource(R.string.tr_restart_title)) },
            text = { Text(stringResource(R.string.tr_restart_message, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingId = null
                        scope.launch {
                            viewModel.apply(pending)
                            restartApp(context)
                        }
                    },
                    modifier = Modifier.testTag("translations_restart_button")
                ) { Text(stringResource(R.string.tr_restart)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingId = null }) { Text(stringResource(R.string.tr_later)) }
            }
        )
    }
}

@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

@Composable
private fun LanguageCard(group: LangGroup, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("translation_language_${group.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.english,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val sub = buildString {
                    if (group.native.isNotEmpty()) append(group.native).append(" · ")
                    append(stringResource(R.string.tr_count_format, group.items.size))
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extraColors.secondaryText
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.extraColors.secondaryText
            )
        }
    }
}

@Composable
private fun UseOrInUse(inUse: Boolean, onUse: () -> Unit) {
    if (inUse) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.tr_in_use),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        OutlinedButton(onClick = onUse) { Text(stringResource(R.string.tr_use)) }
    }
}

@Composable
private fun InstalledCard(info: TranslationInfoEntity, inUse: Boolean, onUse: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installed_translation_${info.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.nameEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extraColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            UseOrInUse(inUse = inUse, onUse = onUse)
        }
    }
}

@Composable
private fun EditionCard(
    item: RemoteTranslation,
    status: DownloadStatus?,
    isInstalled: Boolean,
    inUse: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onUse: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("translation_edition_${item.key}"),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extraColors.secondaryText,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val meta = buildString {
                append(stringResource(R.string.tr_source_line))
                if (item.version.isNotBlank()) append(" · ").append(stringResource(R.string.tr_version_format, item.version))
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.extraColors.secondaryText
            )
            Spacer(Modifier.height(2.dp))
            when {
                status is DownloadStatus.Running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.tr_downloading_format, (status.progress * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                    }
                }
                isInstalled -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) { UseOrInUse(inUse = inUse, onUse = onUse) }
                }
                else -> {
                    if (status is DownloadStatus.Failed) {
                        Text(
                            text = stringResource(R.string.tr_failed_format, status.message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.testTag("translation_download_${item.key}")
                        ) {
                            Text(
                                stringResource(
                                    if (status is DownloadStatus.Failed) R.string.tr_retry else R.string.tr_download
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

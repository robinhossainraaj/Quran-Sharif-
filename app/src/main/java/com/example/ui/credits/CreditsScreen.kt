package com.example.ui.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.TranslationInfoEntity
import com.example.ui.components.AppTitle
import com.example.ui.components.AppTopBar
import com.example.ui.components.SectionTitle
import com.example.ui.theme.extraColors

@Composable
fun CreditsScreen(
    translations: List<TranslationInfoEntity>,
    onNavigateBack: () -> Unit
) {
    val downloaded = translations.filter { it.id.startsWith("qe_") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { AppTitle(stringResource(R.string.credits_page_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("credits_back_button")
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
            item { SectionTitle(stringResource(R.string.credits_section_sources)) }
            item { CreditCard(stringResource(R.string.credits_quranenc_title), stringResource(R.string.credits_quranenc_body)) }
            item { CreditCard(stringResource(R.string.credits_tanzil_title), stringResource(R.string.credits_tanzil_body)) }
            item { CreditCard(stringResource(R.string.credits_audio_title), stringResource(R.string.credits_audio_body)) }

            item { SectionTitle(stringResource(R.string.credits_section_downloaded)) }
            if (downloaded.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.credits_none_downloaded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.extraColors.secondaryText
                    )
                }
            } else {
                items(downloaded, key = { it.id }) { info ->
                    val body = buildString {
                        if (info.translator.isNotBlank()) append(info.translator).append("\n")
                        append(stringResource(R.string.tr_source_line))
                        if (info.version.isNotBlank()) append(" · ").append(stringResource(R.string.tr_version_format, info.version))
                    }
                    CreditCard(info.nameEn, body)
                }
            }
        }
    }
}

@Composable
private fun CreditCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extraColors.secondaryText
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * App languages. The order follows the size of the Muslim population
 * that speaks each language (biggest first). Names are shown in their own language.
 */
private val appLanguageTags = listOf(
    "id" to "Bahasa Indonesia",
    "ur" to "اردو",
    "bn" to "বাংলা",
    "ar" to "العربية",
    "fa" to "فارسی",
    "tr" to "Türkçe",
    "en" to "English",
    "ha" to "Hausa",
    "hi" to "हिन्दी",
    "ms" to "Bahasa Melayu",
    "sw" to "Kiswahili",
    "ps" to "پښتو",
    "uz" to "Oʻzbekcha",
    "fr" to "Français",
    "ru" to "Русский",
    "so" to "Soomaali",
    "ta" to "தமிழ்",
    "zh-CN" to "中文（简体）",
    "bs" to "Bosanski",
    "de" to "Deutsch",
    "es" to "Español",
    "pt" to "Português",
    "ja" to "日本語",
    "ko" to "한국어"
)

/** The list of choices: System Default first, then every language. */
@Composable
fun AppLanguageList(
    current: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("" to stringResource(R.string.language_system)) + appLanguageTags
    Column {
        for ((tag, label) in options) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = current == tag,
                        role = Role.RadioButton,
                        onClick = { onSelect(tag) }
                    )
                    .padding(vertical = 4.dp)
                    .testTag("app_language_option_${tag.ifEmpty { "system" }}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = current == tag, onClick = null)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

/**
 * App language picker. The language changes at once (the screen redraws itself),
 * so no restart is needed for this one.
 */
@Composable
fun AppLanguageDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_app_language)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppLanguageList(current = current, onSelect = onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tr_close)) }
        }
    )
}

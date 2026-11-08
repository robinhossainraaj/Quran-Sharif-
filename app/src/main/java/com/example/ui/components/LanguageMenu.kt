package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
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
 * App language picker. The language changes at once (the screen redraws itself),
 * so no restart is needed for this one.
 */
@Composable
fun AppLanguageDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "bn" to stringResource(R.string.language_bn),
        "en" to stringResource(R.string.language_en)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_app_language)) },
        text = {
            androidx.compose.foundation.layout.Column {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tr_close)) }
        }
    )
}

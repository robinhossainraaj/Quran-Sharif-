package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.db.TranslationInfoEntity
import com.example.service.ServiceController
import com.example.ui.theme.extraColors
import com.example.ui.theme.getArabicTextStyle
import com.example.ui.theme.getBanglaTextStyle
import com.example.util.BanglaDigits
import com.example.widget.ArabicBitmapRenderer
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.QuranApp
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.ui.audio.ReciterPickerSheet
import com.example.widget.WidgetSizing
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var showTranslationDialog by remember { mutableStateOf(false) }
    var showResetProgressDialog by remember { mutableStateOf(false) }
    var showDeleteBookmarksDialog by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }
    val catalog = remember { ReciterCatalogLoader.load(context) }
    var showReciterPicker by remember { mutableStateOf(false) }
    val reciterPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setWidgetRefreshOnScreenOn(true)
            ServiceController.sync(context, true)
        }
    }

    val currentTranslation = state.availableTranslations.firstOrNull {
        it.id == state.userSettings.translationId
    } ?: state.availableTranslations.firstOrNull()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Appearance
                SettingsSectionHeader(title = stringResource(R.string.section_appearance))

                // Theme Mode Chips
                Column {
                    Text(
                        text = stringResource(R.string.theme_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.userSettings.themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            label = { Text(stringResource(R.string.theme_system)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("theme_system_chip")
                        )
                        FilterChip(
                            selected = state.userSettings.themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            label = { Text(stringResource(R.string.theme_light)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("theme_light_chip")
                        )
                        FilterChip(
                            selected = state.userSettings.themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            label = { Text(stringResource(R.string.theme_dark)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("theme_dark_chip")
                        )
                    }
                }

                // App Language Chips
                Column {
                    Text(
                        text = stringResource(R.string.language_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.userSettings.appLanguage.isEmpty(),
                            onClick = { viewModel.setAppLanguage("") },
                            label = { Text(stringResource(R.string.language_system)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("lang_system_chip")
                        )
                        FilterChip(
                            selected = state.userSettings.appLanguage == "bn",
                            onClick = { viewModel.setAppLanguage("bn") },
                            label = { Text(stringResource(R.string.language_bn)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("lang_bn_chip")
                        )
                        FilterChip(
                            selected = state.userSettings.appLanguage == "en",
                            onClick = { viewModel.setAppLanguage("en") },
                            label = { Text(stringResource(R.string.language_en)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("lang_en_chip")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Section 2: Reading Preferences
                SettingsSectionHeader(title = stringResource(R.string.section_reading))

                // Bangla Translation Selector Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTranslationDialog = true }
                        .testTag("select_translation_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.translation_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val transName = currentTranslation?.let {
                                "${it.nameBn} (${it.translator})"
                            } ?: "মুহিউদ্দীন খান"
                            Text(
                                text = transName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Arabic Text Size Slider (20 - 48 sp)
                Column {
                    val sizeVal = state.userSettings.arabicSizeSp
                    val sizeText = if (state.userSettings.banglaDigits) BanglaDigits.convert(sizeVal) else sizeVal.toString()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.arabic_font_size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${sizeText}sp",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sizeVal.toFloat(),
                        onValueChange = { viewModel.setArabicSize(it.toInt()) },
                        valueRange = 20f..48f,
                        steps = 13,
                        modifier = Modifier.testTag("arabic_size_slider")
                    )
                }

                // Bangla Text Size Slider (12 - 26 sp)
                Column {
                    val sizeVal = state.userSettings.banglaSizeSp
                    val sizeText = if (state.userSettings.banglaDigits) BanglaDigits.convert(sizeVal) else sizeVal.toString()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.bangla_font_size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${sizeText}sp",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sizeVal.toFloat(),
                        onValueChange = { viewModel.setBanglaSize(it.toInt()) },
                        valueRange = 12f..26f,
                        steps = 13,
                        modifier = Modifier.testTag("bangla_size_slider")
                    )
                }

                // Live Preview Card using real ayah (Surah 112:1)
                if (state.previewAyah != null) {
                    val previewAyah = state.previewAyah!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.preview_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.extraColors.secondaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                currentTranslation?.let {
                                    Text(
                                        text = it.nameBn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = previewAyah.arabicDisplay,
                                style = getArabicTextStyle(state.userSettings.arabicSizeSp),
                                color = MaterialTheme.extraColors.accentGold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.userSettings.showBangla) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = previewAyah.bangla,
                                    style = getBanglaTextStyle(state.userSettings.banglaSizeSp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Toggles
                SettingSwitchRow(
                    title = stringResource(R.string.show_bangla_translation),
                    checked = state.userSettings.showBangla,
                    onCheckedChange = { viewModel.setShowBangla(it) },
                    testTag = "show_bangla_switch"
                )

                SettingSwitchRow(
                    title = stringResource(R.string.use_bangla_digits),
                    checked = state.userSettings.banglaDigits,
                    onCheckedChange = { viewModel.setBanglaDigits(it) },
                    testTag = "bangla_digits_switch"
                )

                SettingSwitchRow(
                    title = stringResource(R.string.keep_screen_on),
                    checked = state.userSettings.keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) },
                    testTag = "keep_screen_on_switch"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Section 3: Home-Screen Widget
                SettingsSectionHeader(title = stringResource(R.string.section_widget))

                // Widget Arabic Text Size Slider (70% - 150%)
                Column {
                    val arScale = state.userSettings.widgetArabicScalePct
                    val arScaleStr = if (state.userSettings.banglaDigits) BanglaDigits.convert(arScale) else arScale.toString()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.widget_arabic_size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$arScaleStr%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = arScale.toFloat(),
                        onValueChange = { viewModel.setWidgetArabicScale(it.toInt()) },
                        valueRange = 70f..150f,
                        steps = 15,
                        modifier = Modifier.testTag("widget_arabic_size_slider")
                    )
                }

                // Widget Bangla Text Size Slider (70% - 150%)
                Column {
                    val trScale = state.userSettings.widgetTranslationScalePct
                    val trScaleStr = if (state.userSettings.banglaDigits) BanglaDigits.convert(trScale) else trScale.toString()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.widget_translation_size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$trScaleStr%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = trScale.toFloat(),
                        onValueChange = { viewModel.setWidgetTranslationScale(it.toInt()) },
                        valueRange = 70f..150f,
                        steps = 15,
                        modifier = Modifier.testTag("widget_translation_size_slider")
                    )
                }

                // Reset Widget Text Size Button
                OutlinedButton(
                    onClick = {
                        viewModel.resetWidgetTextSize()
                        scope.launch {
                            snackbarHostState.showSnackbar("Widget text size reset to 100%")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_widget_text_size_button")
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.widget_text_size_reset))
                }

                // Live Widget Preview Card
                if (state.previewAyah != null) {
                    val previewAyah = state.previewAyah!!
                    val arScale = state.userSettings.widgetArabicScalePct
                    val trScale = state.userSettings.widgetTranslationScalePct
                    val arStartPx = WidgetSizing.computeArabicStartPx(arScale)
                    val arMinPx = WidgetSizing.computeArabicMinPx(arScale)
                    val trSp = WidgetSizing.computeTranslationSp(trScale)

                    Text(
                        text = stringResource(R.string.widget_preview_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Widget simulation card
                    val widgetBg = Color(0xFF14352D)
                    val refColor = Color(0xFF9AD3C0)
                    val dividerColor = Color(0xFF2C5A4D)
                    val banglaColor = Color(0xFFE3EEEA)
                    val labelColor = Color(0xFF7FB8A5)

                    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(previewAyah.arabicDisplay, arStartPx, arMinPx) {
                        previewBitmap = ArabicBitmapRenderer.render(
                            context = context,
                            text = previewAyah.arabicDisplay,
                            color = android.graphics.Color.parseColor("#F4E7BF"),
                            startSizePx = arStartPx,
                            minSizePx = arMinPx,
                            maxHeightPx = 420
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .testTag("widget_live_preview_card"),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = widgetBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val refText = "সূরা আল-ইখলাস · ১১২:১"
                                Text(
                                    text = refText,
                                    color = refColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "⟳",
                                    color = refColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Arabic Image preview
                            previewBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Widget Arabic Preview",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(dividerColor)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Translation
                            Text(
                                text = previewAyah.bangla,
                                color = banglaColor,
                                fontSize = trSp.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Branding label
                            val brandingText = currentTranslation?.nameEn?.let {
                                "QURAN SHARIF · $it"
                            } ?: "QURAN SHARIF"
                            Text(
                                text = brandingText,
                                color = labelColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.widget_resize_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Screen on widget refresh toggle
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.widget_refresh_on_screen_on),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.userSettings.widgetRefreshOnScreenOn,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            showNotificationRationale = true
                                        } else {
                                            viewModel.setWidgetRefreshOnScreenOn(true)
                                            ServiceController.sync(context, true)
                                        }
                                    } else {
                                        viewModel.setWidgetRefreshOnScreenOn(true)
                                        ServiceController.sync(context, true)
                                    }
                                } else {
                                    viewModel.setWidgetRefreshOnScreenOn(false)
                                    ServiceController.sync(context, false)
                                }
                            },
                            modifier = Modifier.testTag("widget_screen_on_switch")
                        )
                    }
                    Text(
                        text = stringResource(R.string.widget_refresh_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Battery optimization button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // If activity not found
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("battery_optimization_button")
                ) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.battery_optimization_settings))
                }
                Text(
                    text = stringResource(R.string.battery_optimization_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Refresh widget now button
                Button(
                    onClick = {
                        scope.launch {
                            WidgetUpdater.refresh(context)
                            snackbarHostState.showSnackbar(context.getString(R.string.widget_refreshed))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refresh_widget_now_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.refresh_widget_now), fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Section 4: Data Resets
                SettingsSectionHeader(title = stringResource(R.string.section_data))

                OutlinedButton(
                    onClick = { showResetProgressDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_reading_progress_button")
                ) {
                    Text(stringResource(R.string.reset_reading_progress))
                }

                OutlinedButton(
                    onClick = { showDeleteBookmarksDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_bookmarks_settings_button")
                ) {
                    Text(stringResource(R.string.delete_all_bookmarks))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Section 5: Audio Recitation
                SettingsSectionHeader(title = stringResource(R.string.section_audio))

                // Default Reciter Row
                val currentReciter = catalog.findById(state.userSettings.audioReciterId) ?: catalog.defaultReciter()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReciterPicker = true }
                        .testTag("setting_reciter_row"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.audio_reciter_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentReciter.displayName(true)} · ${currentReciter.bitrate} kbps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Download on Wi-Fi only
                SettingSwitchRow(
                    title = stringResource(R.string.audio_wifi_only),
                    checked = state.userSettings.audioWifiOnly,
                    onCheckedChange = { viewModel.setAudioWifiOnly(it) },
                    testTag = "setting_audio_wifi_only"
                )

                // Play Bismillah before surah
                SettingSwitchRow(
                    title = stringResource(R.string.audio_bismillah_prefix),
                    checked = state.userSettings.audioBismillahPrefix,
                    onCheckedChange = { viewModel.setAudioBismillahPrefix(it) },
                    testTag = "setting_audio_bismillah_prefix"
                )

                // Follow audio automatically
                SettingSwitchRow(
                    title = stringResource(R.string.audio_follow),
                    checked = state.userSettings.audioFollow,
                    onCheckedChange = { viewModel.setAudioFollow(it) },
                    testTag = "setting_audio_follow"
                )

                // Manage Downloads Row
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDownloads() }
                        .testTag("setting_manage_downloads_row"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.audio_manage_downloads),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.audio_manage_downloads_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Section 6: About Navigation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAbout() }
                        .testTag("about_app_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.about_app),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.offline_badge),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Translation Selection Dialog
    if (showTranslationDialog) {
        AlertDialog(
            onDismissRequest = { showTranslationDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.select_translation_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (translation in state.availableTranslations) {
                        val isSelected = translation.id == state.userSettings.translationId
                        val sampleText = state.translationPreviews[translation.id]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setTranslation(translation.id)
                                        showTranslationDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.translation_updated)
                                            )
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .testTag("translation_option_${translation.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = translation.nameBn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.translation_by_format, translation.translator),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (!sampleText.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.translation_sample_ayah),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.extraColors.secondaryText
                                            )
                                            Text(
                                                text = sampleText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTranslationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Reset Progress Confirmation Dialog
    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            title = { Text(stringResource(R.string.reset_reading_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reset_reading_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetReadingProgress()
                        showResetProgressDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.progress_reset_done))
                        }
                    },
                    modifier = Modifier.testTag("confirm_reset_progress_button")
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete All Bookmarks Confirmation Dialog
    if (showDeleteBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBookmarksDialog = false },
            title = { Text(stringResource(R.string.delete_all_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_all_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllBookmarks()
                        showDeleteBookmarksDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.all_bookmarks_deleted))
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_all_bookmarks_button")
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBookmarksDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Notification Permission Rationale Dialog
    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text(stringResource(R.string.notification_permission_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.notification_permission_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationale = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showReciterPicker) {
        val app = context.applicationContext as QuranApp
        ReciterPickerSheet(
            sheetState = reciterPickerSheetState,
            reciters = catalog.reciters,
            selectedReciterId = state.userSettings.audioReciterId,
            previewController = app.container.audioPreviewController,
            onSelectReciter = { newReciter ->
                viewModel.setAudioReciterId(newReciter.id)
                showReciterPicker = false
            },
            onDismiss = { showReciterPicker = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

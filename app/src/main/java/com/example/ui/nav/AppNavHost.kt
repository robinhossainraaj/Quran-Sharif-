package com.example.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppBottomBar
import com.example.ui.components.HeaderActions
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.prefs.UserSettings
import com.example.ui.components.AppLanguageDialog
import com.example.ui.credits.CreditsScreen
import com.example.ui.translations.TranslationsScreen
import com.example.ui.translations.TranslationsViewModel
import kotlinx.coroutines.launch
import com.example.ui.components.LocalHeaderActions
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.components.BottomBarItem
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.R
import com.example.ui.bookmarks.BookmarksScreen
import com.example.ui.bookmarks.BookmarksViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.quran.QuranScreen
import com.example.ui.quran.QuranViewModel
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.AboutScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

data class DeepLinkTarget(
    val surahId: Int,
    val ayahNo: Int
)

@Composable
fun AppNavHost(
    container: AppContainer,
    deepLinkTarget: DeepLinkTarget?,
    onDeepLinkConsumed: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle deep link from widget
    LaunchedEffect(deepLinkTarget) {
        if (deepLinkTarget != null && deepLinkTarget.surahId > 0) {
            navController.navigate(Routes.reader(deepLinkTarget.surahId, deepLinkTarget.ayahNo)) {
                popUpTo(Routes.HOME)
            }
            onDeepLinkConsumed()
        }
    }

    // One shared way to switch bottom-bar tabs, so every tab behaves the same.
    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Hide bottom bar on reader, search, and about screens
    val showBottomBar = currentRoute in listOf(
        Routes.HOME,
        Routes.QURAN,
        "${Routes.QURAN}?tab={tab}",
        Routes.BOOKMARKS,
        Routes.SETTINGS
    )

    var showAppLanguageDialog by remember { mutableStateOf(false) }
    val appLanguageScope = rememberCoroutineScope()
    val userSettings by container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = UserSettings()
    )

    val headerActions = HeaderActions(
        onAppLanguage = { showAppLanguageDialog = true },
        onTranslationLanguage = {
            navController.navigate(Routes.TRANSLATIONS) { launchSingleTop = true }
        },
        onSearch = {
            navController.navigate(Routes.SEARCH) { launchSingleTop = true }
        }
    )

    CompositionLocalProvider(LocalHeaderActions provides headerActions) {
    if (showAppLanguageDialog) {
        AppLanguageDialog(
            current = userSettings.appLanguage,
            onSelect = { tag ->
                showAppLanguageDialog = false
                appLanguageScope.launch {
                    container.settingsRepository.setAppLanguage(tag)
                    val locales = if (tag.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(tag)
                    }
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            },
            onDismiss = { showAppLanguageDialog = false }
        )
    }
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    items = listOf(
                        BottomBarItem(
                            selected = currentRoute == Routes.HOME,
                            onClick = { navigateToTab(Routes.HOME) },
                            icon = Icons.Default.Home,
                            label = stringResource(R.string.nav_home),
                            testTag = "nav_home_item"
                        ),
                        BottomBarItem(
                            selected = currentRoute == Routes.QURAN || currentRoute?.startsWith(Routes.QURAN) == true,
                            onClick = { navigateToTab(Routes.QURAN) },
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = stringResource(R.string.nav_quran),
                            testTag = "nav_quran_item"
                        ),
                        BottomBarItem(
                            selected = currentRoute == Routes.BOOKMARKS,
                            onClick = { navigateToTab(Routes.BOOKMARKS) },
                            icon = Icons.Default.Bookmark,
                            label = stringResource(R.string.nav_bookmarks),
                            testTag = "nav_bookmarks_item"
                        ),
                        BottomBarItem(
                            selected = currentRoute == Routes.SETTINGS,
                            onClick = { navigateToTab(Routes.SETTINGS) },
                            icon = Icons.Default.Settings,
                            label = stringResource(R.string.nav_settings),
                            testTag = "nav_settings_item"
                        )
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Home
            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository,
                        playbackController = container.playbackController
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                    onNavigateToReader = { sId, aNo -> navController.navigate(Routes.reader(sId, aNo)) },
                    onNavigateToQuranTab = { tab ->
                        navController.navigate(Routes.quranWithTab(tab)) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Quran
            composable(
                route = "${Routes.QURAN}?tab={tab}",
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                val quranViewModel: QuranViewModel = viewModel(
                    factory = QuranViewModel.Factory(
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                QuranScreen(
                    viewModel = quranViewModel,
                    initialTab = initialTab,
                    onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                    onNavigateToReader = { sId, aNo -> navController.navigate(Routes.reader(sId, aNo)) }
                )
            }

            // Bookmarks
            composable(Routes.BOOKMARKS) {
                val bookmarksViewModel: BookmarksViewModel = viewModel(
                    factory = BookmarksViewModel.Factory(
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                BookmarksScreen(
                    viewModel = bookmarksViewModel,
                    onNavigateToReader = { sId, aNo -> navController.navigate(Routes.reader(sId, aNo)) }
                )
            }

            // Settings
            composable(Routes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        context = container.context,
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToAbout = { navController.navigate(Routes.ABOUT) },
                    onNavigateToDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onNavigateToCredits = { navController.navigate(Routes.CREDITS) }
                )
            }

            // Downloads Screen
            composable(Routes.DOWNLOADS) {
                com.example.ui.audio.DownloadsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // About
            composable(Routes.ABOUT) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        context = container.context,
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                AboutScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Translation language (download and choose a translation)
            composable(Routes.TRANSLATIONS) {
                val translationsViewModel: TranslationsViewModel = viewModel(
                    factory = TranslationsViewModel.Factory(
                        manager = container.translationManager,
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                TranslationsScreen(
                    viewModel = translationsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Credits
            composable(Routes.CREDITS) {
                val translations by container.quranRepository.translations()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                CreditsScreen(
                    translations = translations,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Search
            composable(Routes.SEARCH) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = SearchViewModel.Factory(
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReader = { sId, aNo -> navController.navigate(Routes.reader(sId, aNo)) }
                )
            }

            // Reader
            composable(
                route = Routes.READER,
                arguments = listOf(
                    navArgument("surahId") { type = NavType.IntType },
                    navArgument("ayahNo") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val surahId = backStackEntry.arguments?.getInt("surahId") ?: 1
                val ayahNo = backStackEntry.arguments?.getInt("ayahNo") ?: -1

                val readerViewModel: ReaderViewModel = viewModel(
                    key = "reader_$surahId",
                    factory = ReaderViewModel.Factory(
                        surahId = surahId,
                        quranRepository = container.quranRepository,
                        settingsRepository = container.settingsRepository,
                        audioStorage = container.audioStorage,
                        audioDownloadManager = container.audioDownloadManager,
                        playbackController = container.playbackController,
                        audioPreviewController = container.audioPreviewController
                    )
                )
                ReaderScreen(
                    viewModel = readerViewModel,
                    targetAyahNo = ayahNo,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

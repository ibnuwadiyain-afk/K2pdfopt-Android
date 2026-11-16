package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.K2PdfOptNativeBridge
import com.example.ui.screens.BuiltInReaderScreen
import com.example.ui.screens.HomeQueueScreen
import com.example.ui.screens.LanguageManagerScreen
import com.example.ui.screens.LogsAndAboutScreen
import com.example.ui.screens.PreviewOverlayScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.K2PdfOptTheme
import com.example.util.LocaleFormatUtils
import com.example.viewmodel.K2MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: K2MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingShareOrOpenIntent(intent)

        setContent {
            val darkTheme by mainViewModel.darkTheme.collectAsStateWithLifecycle()
            val einkTheme by mainViewModel.einkTheme.collectAsStateWithLifecycle()

            K2PdfOptTheme(
                darkTheme = darkTheme,
                einkHighContrast = einkTheme
            ) {
                K2PdfOptAppRoot(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareOrOpenIntent(intent)
    }

    private fun handleIncomingShareOrOpenIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    mainViewModel.importExternalUris(listOf(uri), autoStartOneTap = true)
                }
            }
            Intent.ACTION_SEND -> {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                streamUri?.let { uri ->
                    mainViewModel.importExternalUris(listOf(uri), autoStartOneTap = true)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    mainViewModel.importExternalUris(uris, autoStartOneTap = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun K2PdfOptAppRoot(
    viewModel: K2MainViewModel,
    initialTab: Int = 0
) {
    val baseContext = LocalContext.current
    val uiLangTag by viewModel.uiLanguageTag.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val einkTheme by viewModel.einkTheme.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeProfileName by viewModel.activeProfileName.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val selectedDocId by viewModel.selectedDocId.collectAsStateWithLifecycle()
    val previewPageNumber by viewModel.previewPageNumber.collectAsStateWithLifecycle()
    val beforeBitmap by viewModel.beforeBitmap.collectAsStateWithLifecycle()
    val afterBitmap by viewModel.afterBitmap.collectAsStateWithLifecycle()
    val overlayBlocks by viewModel.overlayBlocks.collectAsStateWithLifecycle()
    val pageOcrOverride by viewModel.pageOcrLangOverride.collectAsStateWithLifecycle()
    val tessCatalog by viewModel.tessCatalog.collectAsStateWithLifecycle()
    val tessStorageBytes by viewModel.tessStorageBytes.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val batchReports by viewModel.batchReports.collectAsStateWithLifecycle()
    val nativeLogs by viewModel.nativeLogs.collectAsStateWithLifecycle()
    val deviceHealth by viewModel.deviceHealth.collectAsStateWithLifecycle()
    val watchedFolder by viewModel.watchedFolderPath.collectAsStateWithLifecycle()
    val stressTestStatus by viewModel.stressTestStatus.collectAsStateWithLifecycle()

    val locContext = remember(baseContext, uiLangTag) {
        LocaleFormatUtils.createLocalizedContext(baseContext, uiLangTag)
    }
    val isRtl = LocaleFormatUtils.isRtlLocale(uiLangTag)
    val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    var currentTab by remember { mutableIntStateOf(initialTab) }
    var showLangMenu by remember { mutableStateOf(false) }

    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    // SAF Multi-file Document Picker (PDF, DjVu, PNG, JPEG)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importExternalUris(uris, autoStartOneTap = false)
        }
    }

    // SAF Custom .traineddata Importer
    val traineddataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomTraineddataUri(uri)
        }
    }

    // SAF Direct APK Saver
    val apkSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) {
            viewModel.exportInstalledApkToUri(uri)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = locContext.getString(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("top_bar_title")
                        )
                    },
                    actions = {
                        Box {
                            TextButton(
                                onClick = { showLangMenu = true },
                                modifier = Modifier.testTag("btn_language_picker")
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Language")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(uiLangTag.uppercase())
                            }
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                LocaleFormatUtils.SUPPORTED_LOCALES.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text("${opt.nativeName} (${opt.englishName})") },
                                        onClick = {
                                            viewModel.setUiLanguage(opt.tag)
                                            showLangMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    val items = listOf(
                        Triple(0, locContext.getString(R.string.nav_home), Icons.AutoMirrored.Filled.ListAlt),
                        Triple(1, locContext.getString(R.string.nav_preview), Icons.Default.Edit),
                        Triple(2, locContext.getString(R.string.nav_settings), Icons.Default.Settings),
                        Triple(3, locContext.getString(R.string.nav_ocr), Icons.Default.Translate),
                        Triple(4, locContext.getString(R.string.nav_profiles), Icons.Default.Bookmarks),
                        Triple(5, locContext.getString(R.string.nav_reader), Icons.AutoMirrored.Filled.MenuBook),
                        Triple(6, locContext.getString(R.string.nav_about), Icons.Default.Info)
                    )
                    items.forEach { (idx, label, icon) ->
                        NavigationBarItem(
                            selected = currentTab == idx,
                            onClick = { currentTab = idx },
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_$idx")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> HomeQueueScreen(
                        locContext = locContext,
                        localeTag = uiLangTag,
                        queue = queue,
                        batchReports = batchReports,
                        settings = settings,
                        activeProfileName = activeProfileName,
                        deviceHealth = deviceHealth,
                        watchedFolderPath = watchedFolder,
                        stressTestStatus = stressTestStatus,
                        onAddFilesClick = {
                            docPickerLauncher.launch(
                                arrayOf("application/pdf", "image/vnd.djvu", "image/x-djvu", "image/*")
                            )
                        },
                        onGenerateCorpusClick = { viewModel.generateAndLoadSampleCorpus() },
                        onToggleWatchFolder = { viewModel.toggleWatchFolder() },
                        onOptimizePhoneClick = { viewModel.optimizeForThisPhone() },
                        onStartBatchClick = { viewModel.startBatchConversion() },
                        onPauseJobClick = { viewModel.pauseBatchJob() },
                        onResumeJobClick = { viewModel.resumeBatchJob() },
                        onCancelJobClick = { viewModel.cancelBatchJob() },
                        onRun300PageTestClick = { viewModel.run300PageMemoryStabilityTest() },
                        onApplyRecommendedProfile = { viewModel.applyRecommendedProfileForDoc(it) },
                        onOpenInEditor = { docId ->
                            viewModel.selectDocumentForPreview(docId, 1)
                            currentTab = 1
                        },
                        onOpenInReader = { currentTab = 5 },
                        onShareOutput = { path, mime ->
                            viewModel.shareConvertedFileOrProfile(baseContext, path, mime)
                        },
                        onExportApkClick = {
                            apkSaveLauncher.launch("K2pdfopt-v2.55-android.apk")
                        }
                    )

                    1 -> PreviewOverlayScreen(
                        locContext = locContext,
                        localeTag = uiLangTag,
                        queue = queue,
                        selectedDocId = selectedDocId,
                        previewPageNumber = previewPageNumber,
                        beforeBitmap = beforeBitmap,
                        afterBitmap = afterBitmap,
                        overlayBlocks = overlayBlocks,
                        pageOcrLangOverride = pageOcrOverride,
                        pageRangeValue = settings.pageRange,
                        onSelectDocAndPage = { docId, page ->
                            viewModel.selectDocumentForPreview(docId, page)
                        },
                        onUpdateBlockBounds = { blkId, l, t, r, b ->
                            viewModel.updateOverlayBlockBounds(blkId, l, t, r, b)
                        },
                        onCycleProtectPolicy = { blkId ->
                            viewModel.cycleBlockProtectPolicy(blkId)
                        },
                        onAddCustomBlock = { viewModel.addCustomCropOrProtectedBlock() },
                        onChangePageOcrOverride = { viewModel.setPageOcrLanguageOverride(it) },
                        onSavePageOverrides = { viewModel.saveCurrentPageOverrides() },
                        onResetPageOverrides = { viewModel.resetCurrentPageOverrides() },
                        onUpdatePageRange = { newRange ->
                            viewModel.updateSettings(settings.copy(pageRange = newRange))
                        }
                    )

                    2 -> {
                        val totalPages = queue.find { it.id == selectedDocId }?.pageCount ?: 2
                        val estKb = viewModel.engine.estimateOutputSizeKb(totalPages, settings)
                        SettingsScreen(
                            locContext = locContext,
                            localeTag = uiLangTag,
                            settings = settings,
                            estimatedSizeKb = estKb,
                            darkTheme = darkTheme,
                            einkTheme = einkTheme,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onApplyRawCli = { viewModel.applyRawCliFlags(it) },
                            onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                            onToggleEinkTheme = { viewModel.setEinkTheme(it) }
                        )
                    }

                    3 -> LanguageManagerScreen(
                        locContext = locContext,
                        localeTag = uiLangTag,
                        catalog = tessCatalog,
                        totalStorageBytes = tessStorageBytes,
                        settings = settings,
                        onToggleQualityFastOrBest = { fast ->
                            viewModel.updateSettings(settings.copy(ocrFastMode = fast))
                        },
                        onToggleLanguageInPriority = { code ->
                            viewModel.toggleOcrLanguageInPriority(code)
                        },
                        onDownloadLanguage = { code, best ->
                            viewModel.downloadTessLanguage(code, best)
                        },
                        onDeleteLanguage = { code ->
                            viewModel.deleteTessLanguage(code)
                        },
                        onImportCustomTraineddataClick = {
                            traineddataLauncher.launch("*/*")
                        }
                    )

                    4 -> ProfilesScreen(
                        locContext = locContext,
                        profiles = profiles,
                        activeProfileName = activeProfileName,
                        onSaveNewProfile = { n, d -> viewModel.saveCurrentAsProfile(n, d) },
                        onApplyProfile = { viewModel.applyProfile(it) },
                        onDeleteProfile = { viewModel.deleteCustomProfile(it) },
                        onImportProfileJson = { viewModel.importProfileFromJsonString(it) },
                        onShareProfileFile = { path, mime ->
                            viewModel.shareConvertedFileOrProfile(baseContext, path, mime)
                        }
                    )

                    5 -> BuiltInReaderScreen(
                        locContext = locContext,
                        localeTag = uiLangTag,
                        queue = queue,
                        bookmarks = bookmarks,
                        onUpdateBookmark = { path, page, night ->
                            viewModel.updateBookmarkPosition(path, page, night)
                        }
                    )

                    else -> LogsAndAboutScreen(
                        locContext = locContext,
                        currentUiLangTag = uiLangTag,
                        nativeLogs = nativeLogs,
                        onSelectUiLanguage = { viewModel.setUiLanguage(it) },
                        onClearLogs = { K2PdfOptNativeBridge.clearLogs() }
                    )
                }
            }
        }
    }
}

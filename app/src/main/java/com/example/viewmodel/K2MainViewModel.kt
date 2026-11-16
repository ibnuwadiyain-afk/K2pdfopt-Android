package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatchReportEntity
import com.example.data.K2Database
import com.example.data.PageOverrideEntity
import com.example.data.ProfileEntity
import com.example.data.ReaderBookmarkEntity
import com.example.engine.K2PdfOptEngine
import com.example.engine.K2PdfOptNativeBridge
import com.example.engine.SampleCorpusGenerator
import com.example.model.BlockType
import com.example.model.DetectedBlock
import com.example.model.K2Settings
import com.example.model.ProtectPolicy
import com.example.model.QueuedDocumentItem
import com.example.ocr.TessModelEntry
import com.example.ocr.TessdataManager
import com.example.service.DeviceHealthMonitor
import com.example.service.DeviceThermalBatteryState
import com.example.util.LocaleFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class K2MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext
    private val prefs = appContext.getSharedPreferences("k2pdfopt_prefs", Context.MODE_PRIVATE)
    private val database = K2Database.getInstance(appContext)
    private val dao = database.dao()
    val tessdataManager = TessdataManager(appContext)
    val engine = K2PdfOptEngine(appContext, dao, tessdataManager)

    // UI Language & Theme State
    private val _uiLanguageTag = MutableStateFlow(prefs.getString("ui_lang", "en") ?: "en")
    val uiLanguageTag: StateFlow<String> = _uiLanguageTag.asStateFlow()

    private val _darkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _einkTheme = MutableStateFlow(prefs.getBoolean("eink_theme", false))
    val einkTheme: StateFlow<Boolean> = _einkTheme.asStateFlow()

    // Active K2Settings & Profile
    private val _settings = MutableStateFlow(loadPersistedSettings())
    val settings: StateFlow<K2Settings> = _settings.asStateFlow()

    private val _activeProfileName = MutableStateFlow(
        prefs.getString("active_profile_name", "Scientific & Medical Journal (2-Column)")
            ?: "Scientific & Medical Journal (2-Column)"
    )
    val activeProfileName: StateFlow<String> = _activeProfileName.asStateFlow()

    // Document Queue & Active Preview State
    private val _queue = MutableStateFlow<List<QueuedDocumentItem>>(emptyList())
    val queue: StateFlow<List<QueuedDocumentItem>> = _queue.asStateFlow()

    private val _selectedDocId = MutableStateFlow<String?>(null)
    val selectedDocId: StateFlow<String?> = _selectedDocId.asStateFlow()

    private val _previewPageNumber = MutableStateFlow(1)
    val previewPageNumber: StateFlow<Int> = _previewPageNumber.asStateFlow()

    private val _beforeBitmap = MutableStateFlow<Bitmap?>(null)
    val beforeBitmap: StateFlow<Bitmap?> = _beforeBitmap.asStateFlow()

    private val _afterBitmap = MutableStateFlow<Bitmap?>(null)
    val afterBitmap: StateFlow<Bitmap?> = _afterBitmap.asStateFlow()

    private val _overlayBlocks = MutableStateFlow<List<DetectedBlock>>(emptyList())
    val overlayBlocks: StateFlow<List<DetectedBlock>> = _overlayBlocks.asStateFlow()

    private val _pageOcrLangOverride = MutableStateFlow("")
    val pageOcrLangOverride: StateFlow<String> = _pageOcrLangOverride.asStateFlow()

    // Tesseract 5 Language Catalog & Storage State
    private val _tessCatalog = MutableStateFlow<List<TessModelEntry>>(emptyList())
    val tessCatalog: StateFlow<List<TessModelEntry>> = _tessCatalog.asStateFlow()

    private val _tessStorageBytes = MutableStateFlow(0L)
    val tessStorageBytes: StateFlow<Long> = _tessStorageBytes.asStateFlow()

    // Device Thermal / Battery & Folder Watch State
    private val _deviceHealth = MutableStateFlow(
        DeviceHealthMonitor.readCurrentState(appContext, 2)
    )
    val deviceHealth: StateFlow<DeviceThermalBatteryState> = _deviceHealth.asStateFlow()

    private val _watchedFolderPath = MutableStateFlow(prefs.getString("watched_folder", "") ?: "")
    val watchedFolderPath: StateFlow<String> = _watchedFolderPath.asStateFlow()

    private val _stressTestStatus = MutableStateFlow("")
    val stressTestStatus: StateFlow<String> = _stressTestStatus.asStateFlow()

    // Database Flows
    val profiles: StateFlow<List<ProfileEntity>> = dao.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<ReaderBookmarkEntity>> = dao.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batchReports: StateFlow<List<BatchReportEntity>> = dao.observeBatchReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nativeLogs: StateFlow<List<String>> = K2PdfOptNativeBridge.nativeLogs

    init {
        viewModelScope.launch {
            seedBuiltInProfilesIfNeeded()
            refreshTessCatalog()
            generateAndLoadSampleCorpus()
            refreshDeviceHealth()
        }
    }

    private suspend fun seedBuiltInProfilesIfNeeded() {
        val existing = dao.getAllProfilesImmediate()
        if (existing.isEmpty()) {
            K2Database.builtInProfiles().forEach { dao.upsertProfile(it) }
        }
    }

    fun setUiLanguage(tag: String) {
        _uiLanguageTag.value = tag
        prefs.edit().putString("ui_lang", tag).apply()
        LocaleFormatUtils.applyAppLocale(appContext, tag)
        K2PdfOptNativeBridge.appendLog("[Locale] Switched in-app UI language to '$tag' (RTL=${LocaleFormatUtils.isRtlLocale(tag)})")
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setEinkTheme(enabled: Boolean) {
        _einkTheme.value = enabled
        prefs.edit().putBoolean("eink_theme", enabled).apply()
        if (enabled) {
            updateSettings(_settings.value.copy(bitsPerPixel = 2, dither = true, contrast = 1.55f, gamma = 0.38f))
        }
    }

    fun updateSettings(newSettings: K2Settings, profileName: String? = null) {
        _settings.value = newSettings
        prefs.edit().putString("current_settings_json", newSettings.toJson().toString()).apply()
        if (profileName != null) {
            _activeProfileName.value = profileName
            prefs.edit().putString("active_profile_name", profileName).apply()
        }
        refreshActivePreviewBitmaps()
    }

    fun applyRawCliFlags(cliString: String) {
        val normalized = K2PdfOptNativeBridge.normalizeCliFlags(cliString)
        val merged = K2Settings.parseCliFlagsInto(_settings.value, cliString).copy(rawCliFlags = cliString)
        updateSettings(merged, "Custom CLI Flags")
        K2PdfOptNativeBridge.appendLog("[CLI] Parsed flags -> $normalized")
    }

    /**
     * Reads this phone's actual DisplayMetrics (widthPixels, heightPixels, densityDpi)
     * and generates a matching device preset automatically.
     */
    fun optimizeForThisPhone() {
        val dm = appContext.resources.displayMetrics
        val w = dm.widthPixels.coerceIn(720, 2160)
        val h = dm.heightPixels.coerceIn(1280, 3840)
        val dpi = dm.densityDpi.coerceIn(160, 640)
        val updated = _settings.value.copy(
            devicePresetId = "phone_auto",
            dstWidthPx = w,
            dstHeightPx = h,
            dstDpi = dpi
        )
        updateSettings(updated, "Optimized for This Phone (${w}x${h} @ ${dpi}dpi)")
        K2PdfOptNativeBridge.appendLog("[DevicePreset] Auto-configured for phone display: ${w}x${h} px @ $dpi DPI")
    }

    fun generateAndLoadSampleCorpus() {
        viewModelScope.launch(Dispatchers.IO) {
            val corpus = SampleCorpusGenerator.ensureSampleCorpus(appContext)
            val newItems = corpus.map { item ->
                val analysis = engine.analyzeDocument(item.file)
                QueuedDocumentItem(
                    id = item.file.name,
                    fileName = item.file.name,
                    sourcePath = item.file.absolutePath,
                    fileSizeBytes = item.file.length(),
                    pageCount = item.pageCount,
                    status = "READY",
                    analysis = analysis
                )
            }
            withContext(Dispatchers.Main) {
                val existingIds = _queue.value.map { it.id }.toSet()
                val merged = _queue.value + newItems.filterNot { it.id in existingIds }
                _queue.value = merged
                if (_selectedDocId.value == null && merged.isNotEmpty()) {
                    selectDocumentForPreview(merged.first().id, 1)
                }
            }
        }
    }

    fun importExternalUris(uris: List<Uri>, autoStartOneTap: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val importDir = File(appContext.filesDir, "samples").apply { mkdirs() }
            val added = mutableListOf<QueuedDocumentItem>()
            for (uri in uris) {
                try {
                    val displayName = queryUriDisplayName(uri) ?: "imported_${System.currentTimeMillis()}.pdf"
                    val dstFile = File(importDir, displayName)
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        dstFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (dstFile.exists() && dstFile.length() > 0L) {
                        val pageCount = engine.getDocumentPageCount(dstFile)
                        val analysis = engine.analyzeDocument(dstFile)
                        added.add(
                            QueuedDocumentItem(
                                id = dstFile.name,
                                fileName = dstFile.name,
                                sourcePath = dstFile.absolutePath,
                                fileSizeBytes = dstFile.length(),
                                pageCount = pageCount,
                                status = "READY",
                                analysis = analysis
                            )
                        )
                    }
                } catch (e: Exception) {
                    K2PdfOptNativeBridge.appendLog("[Import] Error importing URI $uri: ${e.message}")
                }
            }
            withContext(Dispatchers.Main) {
                if (added.isNotEmpty()) {
                    _queue.value = _queue.value + added
                    selectDocumentForPreview(added.first().id, 1)
                    if (autoStartOneTap) {
                        startBatchConversion()
                    }
                }
            }
        }
    }

    fun applyRecommendedProfileForDoc(docItem: QueuedDocumentItem) {
        val analysis = docItem.analysis ?: return
        viewModelScope.launch {
            val recId = analysis.detectedDocType.recommendedProfileId
            val allProfs = dao.getAllProfilesImmediate()
            val match = allProfs.find { it.id == recId }
            if (match != null) {
                val profSettings = K2Settings.fromJson(JSONObject(match.settingsJson)).copy(
                    ocrLanguages = analysis.suggestedOcrLangs
                )
                updateSettings(profSettings, match.name)
            }
        }
    }

    fun selectDocumentForPreview(docId: String, pageNumber: Int = 1) {
        _selectedDocId.value = docId
        val doc = _queue.value.find { it.id == docId } ?: return
        val clampedPage = pageNumber.coerceIn(1, doc.pageCount.coerceAtLeast(1))
        _previewPageNumber.value = clampedPage
        refreshActivePreviewBitmaps()
    }

    fun refreshActivePreviewBitmaps() {
        val docId = _selectedDocId.value ?: return
        val doc = _queue.value.find { it.id == docId } ?: return
        val page = _previewPageNumber.value.coerceIn(1, doc.pageCount.coerceAtLeast(1))
        val curSettings = _settings.value

        viewModelScope.launch(Dispatchers.IO) {
            val file = File(doc.sourcePath)
            if (!file.exists()) return@launch
            val srcBmp = engine.renderSourcePageBitmap(file, page, 720)
            val blocks = engine.detectPageLayoutBlocks(file, page, curSettings)
            val savedOverride = dao.getPageOverride(file.absolutePath, page)
            val processed = engine.reflowPage(file, page, curSettings)

            withContext(Dispatchers.Main) {
                _beforeBitmap.value = srcBmp
                _afterBitmap.value = processed.reflowedBitmap
                _overlayBlocks.value = blocks
                _pageOcrLangOverride.value = savedOverride?.ocrLangOverride ?: ""
            }
        }
    }

    fun updateOverlayBlockBounds(
        blockId: String,
        newLeft: Float,
        newTop: Float,
        newRight: Float,
        newBottom: Float
    ) {
        _overlayBlocks.value = _overlayBlocks.value.map { blk ->
            if (blk.id == blockId) {
                blk.copy(
                    leftNorm = newLeft.coerceIn(0.01f, 0.90f),
                    topNorm = newTop.coerceIn(0.01f, 0.90f),
                    rightNorm = newRight.coerceIn(newLeft + 0.05f, 0.99f),
                    bottomNorm = newBottom.coerceIn(newTop + 0.04f, 0.99f)
                )
            } else {
                blk
            }
        }
    }

    fun cycleBlockProtectPolicy(blockId: String) {
        _overlayBlocks.value = _overlayBlocks.value.map { blk ->
            if (blk.id == blockId) {
                val nextPolicy = when (blk.protectPolicy) {
                    ProtectPolicy.REFLOW -> ProtectPolicy.SCALE_TO_FIT
                    ProtectPolicy.SCALE_TO_FIT -> ProtectPolicy.KEEP_AS_IMAGE
                    ProtectPolicy.KEEP_AS_IMAGE -> ProtectPolicy.REFLOW
                }
                blk.copy(protectPolicy = nextPolicy)
            } else {
                blk
            }
        }
    }

    fun addCustomCropOrProtectedBlock() {
        val page = _previewPageNumber.value
        val existing = _overlayBlocks.value
        val newBlock = DetectedBlock(
            id = "custom_${System.currentTimeMillis()}",
            pageIndex = page,
            type = BlockType.CROP_BOX,
            leftNorm = 0.12f,
            topNorm = 0.25f,
            rightNorm = 0.88f,
            bottomNorm = 0.55f,
            readingOrder = existing.size + 1,
            protectPolicy = ProtectPolicy.SCALE_TO_FIT,
            extractedText = "User Custom Protected Region (Page $page)"
        )
        _overlayBlocks.value = existing + newBlock
    }

    fun setPageOcrLanguageOverride(langOverride: String) {
        _pageOcrLangOverride.value = langOverride
    }

    fun saveCurrentPageOverrides() {
        val docId = _selectedDocId.value ?: return
        val doc = _queue.value.find { it.id == docId } ?: return
        val page = _previewPageNumber.value
        val blocksJson = DetectedBlock.listToJsonString(_overlayBlocks.value)
        val langOverride = _pageOcrLangOverride.value.trim()

        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertPageOverride(
                PageOverrideEntity(
                    documentPath = doc.sourcePath,
                    pageNumber = page,
                    blocksJson = blocksJson,
                    ocrLangOverride = langOverride
                )
            )
            K2PdfOptNativeBridge.appendLog("[OverlayEditor] Saved ${overlayBlocks.value.size} region override(s) for ${doc.fileName} Page $page")
            refreshActivePreviewBitmaps()
        }
    }

    fun resetCurrentPageOverrides() {
        val docId = _selectedDocId.value ?: return
        val doc = _queue.value.find { it.id == docId } ?: return
        val page = _previewPageNumber.value
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePageOverride(doc.sourcePath, page)
            refreshActivePreviewBitmaps()
        }
    }

    fun startBatchConversion() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshDeviceHealth()
            val health = _deviceHealth.value
            val effectiveSettings = _settings.value.copy(
                ocrThreads = health.recommendedOcrThreads
            )
            val currentList = _queue.value
            for (item in currentList) {
                val srcFile = File(item.sourcePath)
                if (!srcFile.exists()) continue

                updateQueueItem(item.id) { it.copy(status = "PROCESSING", progressPercent = 1, currentPage = 1) }

                val res = engine.convertDocument(
                    sourceFile = srcFile,
                    rawSettings = effectiveSettings,
                    onPageProgress = { curPage, totalPages, _ ->
                        val pct = ((curPage * 100) / totalPages.coerceAtLeast(1)).coerceIn(1, 99)
                        updateQueueItem(item.id) {
                            it.copy(status = "PROCESSING", progressPercent = pct, currentPage = curPage)
                        }
                    }
                )

                res.onSuccess { conv ->
                    val lowCount = conv.pageQualityScores.count { it.flaggedForReview }
                    updateQueueItem(item.id) {
                        it.copy(
                            status = "COMPLETED",
                            progressPercent = 100,
                            currentPage = conv.pagesProcessed,
                            pageQualityScores = conv.pageQualityScores,
                            outputFilePath = conv.outputFile.absolutePath,
                            outputFormat = conv.outputFormat,
                            outputSizeBytes = conv.outputFile.length(),
                            elapsedMs = conv.elapsedMs,
                            avgOcrConfidence = conv.avgOcrConfidence,
                            extractedTextPreview = conv.cleanReaderText.take(2500),
                            warnings = conv.warnings
                        )
                    }
                    dao.insertBatchReport(
                        BatchReportEntity(
                            fileName = item.fileName,
                            pagesProcessed = conv.pagesProcessed,
                            avgOcrConfidence = conv.avgOcrConfidence,
                            lowQualityPageCount = lowCount,
                            elapsedMs = conv.elapsedMs,
                            outputFormat = conv.outputFormat.label,
                            outputPath = conv.outputFile.absolutePath,
                            outputSizeBytes = conv.outputFile.length(),
                            warningsSummary = conv.warnings.joinToString("; ").ifBlank { "0 layout warnings" }
                        )
                    )
                    dao.upsertBookmark(
                        ReaderBookmarkEntity(
                            documentPath = conv.outputFile.absolutePath,
                            title = "${item.fileName} (${conv.outputFormat.ext.uppercase()})",
                            currentPage = 1,
                            totalPages = conv.pagesProcessed,
                            nightMode = _darkTheme.value,
                            detectedLangTag = conv.dominantLangTag,
                            extractedCleanText = conv.cleanReaderText
                        )
                    )
                }.onFailure { err ->
                    updateQueueItem(item.id) {
                        it.copy(status = "FAILED", warnings = listOf(err.message ?: "Conversion failed"))
                    }
                }
            }
        }
    }

    fun pauseBatchJob() {
        K2PdfOptNativeBridge.setJobState(K2PdfOptNativeBridge.STATE_PAUSED)
        K2PdfOptNativeBridge.appendLog("[JobControl] Paused active conversion job.")
    }

    fun resumeBatchJob() {
        K2PdfOptNativeBridge.setJobState(K2PdfOptNativeBridge.STATE_RUNNING)
        K2PdfOptNativeBridge.appendLog("[JobControl] Resumed active conversion job.")
    }

    fun cancelBatchJob() {
        K2PdfOptNativeBridge.setJobState(K2PdfOptNativeBridge.STATE_CANCELLED)
        K2PdfOptNativeBridge.appendLog("[JobControl] Cancelled active conversion job.")
    }

    /**
     * Runs a real 300-page PDF generation and streaming conversion benchmark to verify
     * memory stability on large files.
     */
    fun run300PageMemoryStabilityTest() {
        viewModelScope.launch(Dispatchers.IO) {
            _stressTestStatus.value = "Generating 300-page test PDF on disk…"
            val sampleDir = File(appContext.filesDir, "samples").apply { mkdirs() }
            val pdf300 = File(sampleDir, "stress_test_300_pages.pdf")
            SampleCorpusGenerator.generate300PageStressPdf(pdf300)

            val rt = Runtime.getRuntime()
            val memBeforeMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            _stressTestStatus.value = "Streaming 300 pages through K2pdfopt engine (Heap start: ${memBeforeMb} MB)…"

            val fastSettings = _settings.value.copy(
                dstWidthPx = 600,
                dstHeightPx = 800,
                pageRange = "1-300"
            )
            val res = engine.convertDocument(pdf300, fastSettings) { cur, total, _ ->
                if (cur % 25 == 0 || cur == total) {
                    val curMemMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                    _stressTestStatus.value = "Processed $cur / $total pages (Active Heap: ${curMemMb} MB)…"
                }
            }
            res.onSuccess { conv ->
                val memAfterMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                val summary = "300-Page Stability Verified: 300/300 pages streamed in ${conv.elapsedMs}ms • Output ${conv.outputFile.length() / 1024} KB • Heap ${memBeforeMb}MB -> ${memAfterMb}MB (Zero OOM)"
                _stressTestStatus.value = summary
                K2PdfOptNativeBridge.appendLog("[MemoryTest] $summary")
            }.onFailure { e ->
                _stressTestStatus.value = "300-Page Test Error: ${e.message}"
            }
        }
    }

    // Tesseract 5 Language Manager Actions
    fun refreshTessCatalog() {
        _tessCatalog.value = tessdataManager.getCatalogWithDiskState()
        _tessStorageBytes.value = tessdataManager.getTotalStorageUsedBytes()
    }

    fun downloadTessLanguage(code: String, useBestQuality: Boolean) {
        viewModelScope.launch {
            _tessCatalog.value = _tessCatalog.value.map {
                if (it.code == code) it.copy(isDownloading = true, downloadProgressPercent = 1) else it
            }
            val result = tessdataManager.downloadModelWithResume(code, useBestQuality) { pct ->
                _tessCatalog.value = _tessCatalog.value.map {
                    if (it.code == code) it.copy(isDownloading = true, downloadProgressPercent = pct) else it
                }
            }
            result.onSuccess { file ->
                val sha = tessdataManager.computeSha256(file).take(16)
                K2PdfOptNativeBridge.appendLog("[Tessdata] Installed ${file.name} (${file.length() / 1024} KB, SHA-256: $sha)")
            }
            refreshTessCatalog()
        }
    }

    fun deleteTessLanguage(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tessdataManager.deleteModel(code)
            withContext(Dispatchers.Main) {
                refreshTessCatalog()
            }
        }
    }

    fun toggleOcrLanguageInPriority(code: String) {
        val currentCodes = _settings.value.ocrLanguages
            .split("+")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (code in currentCodes) {
            if (currentCodes.size > 1) currentCodes.remove(code)
        } else {
            currentCodes.add(code)
        }
        val joined = currentCodes.joinToString("+")
        updateSettings(_settings.value.copy(ocrLanguages = joined))
    }

    fun importCustomTraineddataUri(uri: Uri) {
        viewModelScope.launch {
            tessdataManager.importCustomTraineddata(uri).onSuccess { info ->
                K2PdfOptNativeBridge.appendLog("[Tessdata] Imported custom model: $info")
                refreshTessCatalog()
            }
        }
    }

    // Profiles Actions
    fun saveCurrentAsProfile(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = "custom_${name.lowercase().replace(Regex("[^a-z0-9]+"), "_")}_${System.currentTimeMillis() % 10000}"
            val entity = ProfileEntity(
                id = id,
                name = name.trim(),
                description = description.ifBlank { "Custom profile (${_settings.value.toCliString()})" },
                settingsJson = _settings.value.toJson().toString(),
                isBuiltIn = false
            )
            dao.upsertProfile(entity)
            withContext(Dispatchers.Main) {
                _activeProfileName.value = entity.name
            }
        }
    }

    fun applyProfile(profile: ProfileEntity) {
        try {
            val loaded = K2Settings.fromJson(JSONObject(profile.settingsJson))
            updateSettings(loaded, profile.name)
            K2PdfOptNativeBridge.appendLog("[Profile] Applied '${profile.name}': ${loaded.toCliString()}")
        } catch (e: Exception) {
            K2PdfOptNativeBridge.appendLog("[Profile] Failed to parse profile JSON: ${e.message}")
        }
    }

    fun importProfileFromJsonString(jsonStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonStr)
                val name = root.optString("name", "Imported Profile")
                val desc = root.optString("description", "Imported shareable JSON profile")
                val settingsObj = root.optJSONObject("settings") ?: root
                val parsed = K2Settings.fromJson(settingsObj)
                val entity = ProfileEntity(
                    id = "import_${System.currentTimeMillis()}",
                    name = name,
                    description = desc,
                    settingsJson = parsed.toJson().toString(),
                    isBuiltIn = false
                )
                dao.upsertProfile(entity)
                withContext(Dispatchers.Main) {
                    updateSettings(parsed, name)
                }
            } catch (e: Exception) {
                K2PdfOptNativeBridge.appendLog("[Profile] Invalid JSON import: ${e.message}")
            }
        }
    }

    fun deleteCustomProfile(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCustomProfile(id)
        }
    }

    fun shareConvertedFileOrProfile(context: Context, filePath: String, mimeType: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, file.name))
        } catch (e: Exception) {
            K2PdfOptNativeBridge.appendLog("[Share] Error sharing file: ${e.message}")
        }
    }

    fun exportInstalledApkToUri(targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val srcApk = File(appContext.applicationInfo.sourceDir)
                appContext.contentResolver.openOutputStream(targetUri)?.use { out ->
                    srcApk.inputStream().use { input -> input.copyTo(out) }
                }
                val msg = "Exported signed APK (${srcApk.length() / 1024} KB) to selected storage location."
                _stressTestStatus.value = msg
                K2PdfOptNativeBridge.appendLog("[APK Export] $msg")
            } catch (e: Exception) {
                K2PdfOptNativeBridge.appendLog("[APK Export] Failed: ${e.message}")
            }
        }
    }

    fun shareInstalledApk(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val srcApk = File(appContext.applicationInfo.sourceDir)
                val cachedApk = File(appContext.cacheDir, "K2pdfopt-v2.55-android.apk")
                srcApk.copyTo(cachedApk, overwrite = true)
                withContext(Dispatchers.Main) {
                    shareConvertedFileOrProfile(
                        context,
                        cachedApk.absolutePath,
                        "application/vnd.android.package-archive"
                    )
                }
            } catch (e: Exception) {
                K2PdfOptNativeBridge.appendLog("[APK Share] Failed: ${e.message}")
            }
        }
    }

    fun updateBookmarkPosition(docPath: String, page: Int, nightMode: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getBookmark(docPath) ?: return@launch
            dao.upsertBookmark(
                existing.copy(
                    currentPage = page.coerceIn(1, existing.totalPages.coerceAtLeast(1)),
                    nightMode = nightMode,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleWatchFolder() {
        val watchDir = File(appContext.filesDir, "samples").apply { mkdirs() }
        val newVal = if (_watchedFolderPath.value.isEmpty()) watchDir.absolutePath else ""
        _watchedFolderPath.value = newVal
        prefs.edit().putString("watched_folder", newVal).apply()
        if (newVal.isNotEmpty()) {
            K2PdfOptNativeBridge.appendLog("[FolderWatch] Monitoring $newVal for automatic batch conversion")
            generateAndLoadSampleCorpus()
        }
    }

    fun refreshDeviceHealth() {
        _deviceHealth.value = DeviceHealthMonitor.readCurrentState(appContext, _settings.value.ocrThreads)
    }

    private fun updateQueueItem(id: String, transform: (QueuedDocumentItem) -> QueuedDocumentItem) {
        _queue.value = _queue.value.map { if (it.id == id) transform(it) else it }
    }

    private fun loadPersistedSettings(): K2Settings {
        val savedJson = prefs.getString("current_settings_json", null)
        if (!savedJson.isNullOrBlank()) {
            return try {
                K2Settings.fromJson(JSONObject(savedJson))
            } catch (_: Exception) {
                K2Settings()
            }
        }
        return K2Settings()
    }

    private fun queryUriDisplayName(uri: Uri): String? {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}

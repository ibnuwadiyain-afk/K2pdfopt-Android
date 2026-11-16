package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class ConversionMode(val cliCode: String, val label: String) {
    DEFAULT_REFLOW("default", "Default Reflow (-mode default)"),
    FIT_WIDTH("fw", "Fit Width (-mode fw)"),
    TWO_COLUMN("2col", "2-Column Paper (-mode 2col)"),
    CROP_NATIVE("crop", "Crop / Native (-mode crop)"),
    COPY_FIXED("copy", "Fixed Copy (-mode copy)"),
    TRIM_MARGINS("tm", "Trim Margins (-mode tm)")
}

enum class OutputFormat(val ext: String, val label: String, val mimeType: String) {
    SEARCHABLE_PDF("pdf", "Searchable PDF (Reflowed + OCR Layer)", "application/pdf"),
    EPUB("epub", "Reflowable EPUB 3 (Bidirectional / CJK)", "application/epub+zip"),
    HTML5("html", "Semantic HTML5 (Embedded Figures + Links)", "text/html"),
    MARKDOWN("md", "Markdown (.md)", "text/markdown"),
    PLAIN_TEXT("txt", "UTF-8 Plain Text (.txt)", "text/plain"),
    PNG_IMAGES("png", "Page Image Sequence (PNG)", "image/png")
}

enum class BlockType(val id: Int, val label: String, val colorHex: Long) {
    TEXT(0, "Text Column / Paragraph", 0xFF38BDF8),
    TABLE(1, "Protected Table", 0xFFF59E0B),
    FIGURE(2, "Protected Figure", 0xFF10B981),
    EQUATION(3, "Inline / Display Equation", 0xFFA855F7),
    CAPTION(4, "Caption", 0xFFEC4899),
    FOOTNOTE(5, "Smart Footnote", 0xFF6366F1),
    HEADER(6, "Running Header (Skip in TTS)", 0xFF94A3B8),
    FOOTER(7, "Running Footer (Skip in TTS)", 0xFF94A3B8),
    CROP_BOX(8, "Manual Crop Box", 0xFFF43F5E)
}

enum class ProtectPolicy(val id: Int, val label: String) {
    REFLOW(0, "Reflow Text"),
    SCALE_TO_FIT(1, "Scale Block to Fit Screen"),
    KEEP_AS_IMAGE(2, "Keep Intact as High-DPI Image")
}

enum class FootnotePlacement(val id: Int, val label: String) {
    KEEP_IN_PLACE(0, "Keep in Original Position"),
    END_OF_PARAGRAPH(1, "Move After Citing Paragraph"),
    END_OF_SECTION(2, "Move to End of Section / Document")
}

enum class ScriptDirection(val id: Int, val label: String, val htmlDir: String) {
    LTR(0, "Left-to-Right (LTR)", "ltr"),
    RTL(1, "Right-to-Left (RTL: Arabic, Persian, Urdu, Hebrew)", "rtl"),
    VERTICAL_CJK(2, "Vertical Right-to-Left (CJK)", "ltr")
}

enum class DetectedDocType(val id: String, val title: String, val recommendedProfileId: String) {
    TEXT_BOOK("book", "Text-Only Book", "preset_default_reflow"),
    JOURNAL_2COL("journal", "Two-Column Scientific/Medical Journal", "preset_scientific_medical"),
    SCANNED_DOC("scan", "Scanned Document (Needs Cleanup + OCR)", "preset_scan_cleanup"),
    SLIDES("slides", "Presentation Slides (Fit Width)", "preset_slides_fitwidth"),
    MANUAL("manual", "Technical Manual", "preset_default_reflow"),
    TABLE_HEAVY("tables", "Table-Heavy Financial/Clinical Report", "preset_scientific_medical")
}

data class DevicePreset(
    val id: String,
    val name: String,
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int
) {
    companion object {
        val BUILTIN_PRESETS = listOf(
            DevicePreset("kpw5", "Kindle Paperwhite 5 (6.8\")", 1236, 1648, 300),
            DevicePreset("koasis", "Kindle Oasis (7.0\")", 1264, 1680, 300),
            DevicePreset("kscribe", "Kindle Scribe (10.2\")", 1860, 2480, 300),
            DevicePreset("kobo_clara", "Kobo Clara BW / Colour (6.0\")", 1072, 1448, 300),
            DevicePreset("kobo_libra", "Kobo Libra 2 (7.0\")", 1264, 1680, 300),
            DevicePreset("kobo_sage", "Kobo Sage (8.0\")", 1440, 1920, 300),
            DevicePreset("nook_glow", "Barnes & Noble Nook GlowLight 4", 1072, 1448, 300),
            DevicePreset("remarkable2", "reMarkable 2 (10.3\")", 1404, 1872, 226),
            DevicePreset("phone_auto", "This Phone (Auto-Detected Display)", 1080, 2160, 420)
        )
    }
}

data class K2Settings(
    val devicePresetId: String = "kpw5",
    val dstWidthPx: Int = 1080,
    val dstHeightPx: Int = 1440,
    val dstDpi: Int = 265,
    val srcDpi: Int = 300,
    val mode: ConversionMode = ConversionMode.DEFAULT_REFLOW,
    val maxColumns: Int = 2,
    val colGapMin: Float = 0.05f,
    val colGapRange: Float = 0.33f,
    val chHeight: Float = 1.5f,
    val wordSpacing: Float = -0.20f,
    val wrapText: Boolean = true,
    val keepHyphens: Boolean = true,
    val justification: Int = -1, // -1=auto, 0=left, 1=center, 2=right, 3=full
    val rtlMode: Boolean = false,
    val verticalCjk: Boolean = false,
    val autoCropThreshold: Float = 0.10f,
    val marginLeftIn: Float = 0.0f,
    val marginTopIn: Float = 0.0f,
    val marginRightIn: Float = 0.0f,
    val marginBottomIn: Float = 0.0f,
    val oddMarginLeftIn: Float = 0.0f,
    val oddMarginRightIn: Float = 0.0f,
    val evenMarginLeftIn: Float = 0.0f,
    val evenMarginRightIn: Float = 0.0f,
    val contrast: Float = 1.25f,
    val gamma: Float = 0.50f,
    val sharpen: Boolean = true,
    val dither: Boolean = true,
    val bitsPerPixel: Int = 4, // 1, 2, 4, 8
    val negateColors: Int = 0, // 0=off, 1=full, 2=smart text-only
    val autoStraightenDeskew: Boolean = true,
    val despeckle: Boolean = true,
    val whitenBackground: Boolean = true,
    val removeShadows: Boolean = false,
    val rotateDeg: Int = 0,
    val pageRange: String = "1-",
    val preserveToc: Boolean = true,
    val nativePdfMode: Boolean = false,
    val breakPages: Boolean = false,
    val ocrEnabled: Boolean = true,
    val ocrLanguages: String = "eng", // e.g. "eng+ara+fra"
    val ocrThreads: Int = 2,
    val ocrFastMode: Boolean = true, // true=fast, false=best
    val useNeuralLayoutDetector: Boolean = true,
    val tableProtection: ProtectPolicy = ProtectPolicy.SCALE_TO_FIT,
    val equationInlineImage: Boolean = true,
    val footnotePlacement: FootnotePlacement = FootnotePlacement.END_OF_PARAGRAPH,
    val targetSizeKb: Int = 0, // 0 = no limit
    val outputFormat: OutputFormat = OutputFormat.SEARCHABLE_PDF,
    val rawCliFlags: String = ""
) {
    fun toCliString(): String {
        val base = buildString {
            append("-mode ${mode.cliCode} ")
            append("-w $dstWidthPx -h $dstHeightPx -dpi $dstDpi -idpi $srcDpi ")
            append("-col $maxColumns ")
            append(String.format(Locale.US, "-cg %.3f -cgr %.3f -ch %.2f -ws %.2f ", colGapMin, colGapRange, chHeight, wordSpacing))
            append(if (wrapText) "-wrap+ " else "-wrap- ")
            append(if (keepHyphens) "-hy+ " else "-hy- ")
            append("-j $justification ")
            append(if (rtlMode) "-r2l+ " else "-r2l- ")
            append(String.format(Locale.US, "-ac %.2f ", autoCropThreshold))
            if (marginLeftIn > 0f || marginTopIn > 0f || marginRightIn > 0f || marginBottomIn > 0f) {
                append(String.format(Locale.US, "-ml %.2f -mt %.2f -mr %.2f -mb %.2f ", marginLeftIn, marginTopIn, marginRightIn, marginBottomIn))
            }
            append(String.format(Locale.US, "-c %.2f -g %.2f ", contrast, gamma))
            append(if (sharpen) "-sh+ " else "-sh- ")
            append(if (dither) "-d+ " else "-d- ")
            append("-bpc $bitsPerPixel ")
            append(when (negateColors) { 2 -> "-neg+ "; 1 -> "-neg "; else -> "-neg- " })
            append(if (autoStraightenDeskew) "-as+ " else "-as- ")
            append("-p $pageRange ")
            append(if (preserveToc) "-toc+ " else "-toc- ")
            append(if (nativePdfMode) "-n+ " else "-n- ")
            append(if (breakPages) "-bp+ " else "-bp- ")
            append(if (ocrEnabled) "-ocr t -ocrlang $ocrLanguages -ocrmt $ocrThreads" else "-ocr 0")
            if (rawCliFlags.isNotBlank()) {
                append(" ").append(rawCliFlags.trim())
            }
        }
        return base.trim()
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("devicePresetId", devicePresetId)
            put("dstWidthPx", dstWidthPx)
            put("dstHeightPx", dstHeightPx)
            put("dstDpi", dstDpi)
            put("srcDpi", srcDpi)
            put("mode", mode.cliCode)
            put("maxColumns", maxColumns)
            put("colGapMin", colGapMin.toDouble())
            put("colGapRange", colGapRange.toDouble())
            put("chHeight", chHeight.toDouble())
            put("wordSpacing", wordSpacing.toDouble())
            put("wrapText", wrapText)
            put("keepHyphens", keepHyphens)
            put("justification", justification)
            put("rtlMode", rtlMode)
            put("verticalCjk", verticalCjk)
            put("autoCropThreshold", autoCropThreshold.toDouble())
            put("marginLeftIn", marginLeftIn.toDouble())
            put("marginTopIn", marginTopIn.toDouble())
            put("marginRightIn", marginRightIn.toDouble())
            put("marginBottomIn", marginBottomIn.toDouble())
            put("oddMarginLeftIn", oddMarginLeftIn.toDouble())
            put("oddMarginRightIn", oddMarginRightIn.toDouble())
            put("evenMarginLeftIn", evenMarginLeftIn.toDouble())
            put("evenMarginRightIn", evenMarginRightIn.toDouble())
            put("contrast", contrast.toDouble())
            put("gamma", gamma.toDouble())
            put("sharpen", sharpen)
            put("dither", dither)
            put("bitsPerPixel", bitsPerPixel)
            put("negateColors", negateColors)
            put("autoStraightenDeskew", autoStraightenDeskew)
            put("despeckle", despeckle)
            put("whitenBackground", whitenBackground)
            put("removeShadows", removeShadows)
            put("rotateDeg", rotateDeg)
            put("pageRange", pageRange)
            put("preserveToc", preserveToc)
            put("nativePdfMode", nativePdfMode)
            put("breakPages", breakPages)
            put("ocrEnabled", ocrEnabled)
            put("ocrLanguages", ocrLanguages)
            put("ocrThreads", ocrThreads)
            put("ocrFastMode", ocrFastMode)
            put("useNeuralLayoutDetector", useNeuralLayoutDetector)
            put("tableProtection", tableProtection.id)
            put("equationInlineImage", equationInlineImage)
            put("footnotePlacement", footnotePlacement.id)
            put("targetSizeKb", targetSizeKb)
            put("outputFormat", outputFormat.name)
            put("rawCliFlags", rawCliFlags)
            put("cliEquivalent", toCliString())
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): K2Settings {
            val modeStr = obj.optString("mode", "default")
            val mode = ConversionMode.entries.find { it.cliCode == modeStr } ?: ConversionMode.DEFAULT_REFLOW
            val outFmtName = obj.optString("outputFormat", OutputFormat.SEARCHABLE_PDF.name)
            val outFmt = OutputFormat.entries.find { it.name == outFmtName } ?: OutputFormat.SEARCHABLE_PDF
            val tableProtId = obj.optInt("tableProtection", ProtectPolicy.SCALE_TO_FIT.id)
            val tableProt = ProtectPolicy.entries.find { it.id == tableProtId } ?: ProtectPolicy.SCALE_TO_FIT
            val fnId = obj.optInt("footnotePlacement", FootnotePlacement.END_OF_PARAGRAPH.id)
            val fnPlace = FootnotePlacement.entries.find { it.id == fnId } ?: FootnotePlacement.END_OF_PARAGRAPH

            return K2Settings(
                devicePresetId = obj.optString("devicePresetId", "kpw5"),
                dstWidthPx = obj.optInt("dstWidthPx", 1080),
                dstHeightPx = obj.optInt("dstHeightPx", 1440),
                dstDpi = obj.optInt("dstDpi", 265),
                srcDpi = obj.optInt("srcDpi", 300),
                mode = mode,
                maxColumns = obj.optInt("maxColumns", 2),
                colGapMin = obj.optDouble("colGapMin", 0.05).toFloat(),
                colGapRange = obj.optDouble("colGapRange", 0.33).toFloat(),
                chHeight = obj.optDouble("chHeight", 1.5).toFloat(),
                wordSpacing = obj.optDouble("wordSpacing", -0.20).toFloat(),
                wrapText = obj.optBoolean("wrapText", true),
                keepHyphens = obj.optBoolean("keepHyphens", true),
                justification = obj.optInt("justification", -1),
                rtlMode = obj.optBoolean("rtlMode", false),
                verticalCjk = obj.optBoolean("verticalCjk", false),
                autoCropThreshold = obj.optDouble("autoCropThreshold", 0.10).toFloat(),
                marginLeftIn = obj.optDouble("marginLeftIn", 0.0).toFloat(),
                marginTopIn = obj.optDouble("marginTopIn", 0.0).toFloat(),
                marginRightIn = obj.optDouble("marginRightIn", 0.0).toFloat(),
                marginBottomIn = obj.optDouble("marginBottomIn", 0.0).toFloat(),
                oddMarginLeftIn = obj.optDouble("oddMarginLeftIn", 0.0).toFloat(),
                oddMarginRightIn = obj.optDouble("oddMarginRightIn", 0.0).toFloat(),
                evenMarginLeftIn = obj.optDouble("evenMarginLeftIn", 0.0).toFloat(),
                evenMarginRightIn = obj.optDouble("evenMarginRightIn", 0.0).toFloat(),
                contrast = obj.optDouble("contrast", 1.25).toFloat(),
                gamma = obj.optDouble("gamma", 0.50).toFloat(),
                sharpen = obj.optBoolean("sharpen", true),
                dither = obj.optBoolean("dither", true),
                bitsPerPixel = obj.optInt("bitsPerPixel", 4),
                negateColors = obj.optInt("negateColors", 0),
                autoStraightenDeskew = obj.optBoolean("autoStraightenDeskew", true),
                despeckle = obj.optBoolean("despeckle", true),
                whitenBackground = obj.optBoolean("whitenBackground", true),
                removeShadows = obj.optBoolean("removeShadows", false),
                rotateDeg = obj.optInt("rotateDeg", 0),
                pageRange = obj.optString("pageRange", "1-"),
                preserveToc = obj.optBoolean("preserveToc", true),
                nativePdfMode = obj.optBoolean("nativePdfMode", false),
                breakPages = obj.optBoolean("breakPages", false),
                ocrEnabled = obj.optBoolean("ocrEnabled", true),
                ocrLanguages = obj.optString("ocrLanguages", "eng"),
                ocrThreads = obj.optInt("ocrThreads", 2),
                ocrFastMode = obj.optBoolean("ocrFastMode", true),
                useNeuralLayoutDetector = obj.optBoolean("useNeuralLayoutDetector", true),
                tableProtection = tableProt,
                equationInlineImage = obj.optBoolean("equationInlineImage", true),
                footnotePlacement = fnPlace,
                targetSizeKb = obj.optInt("targetSizeKb", 0),
                outputFormat = outFmt,
                rawCliFlags = obj.optString("rawCliFlags", "")
            )
        }

        fun parseCliFlagsInto(base: K2Settings, cli: String): K2Settings {
            val tokens = cli.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            var cur = base
            var i = 0
            while (i < tokens.size) {
                val t = tokens[i]
                when {
                    t == "-mode" && i + 1 < tokens.size -> {
                        val mCode = tokens[++i]
                        val m = ConversionMode.entries.find { it.cliCode == mCode } ?: cur.mode
                        cur = cur.copy(mode = m)
                    }
                    t == "-w" && i + 1 < tokens.size -> cur = cur.copy(dstWidthPx = tokens[++i].toIntOrNull() ?: cur.dstWidthPx)
                    t == "-h" && i + 1 < tokens.size -> cur = cur.copy(dstHeightPx = tokens[++i].toIntOrNull() ?: cur.dstHeightPx)
                    t == "-dpi" && i + 1 < tokens.size -> cur = cur.copy(dstDpi = tokens[++i].toIntOrNull() ?: cur.dstDpi)
                    t == "-idpi" && i + 1 < tokens.size -> cur = cur.copy(srcDpi = tokens[++i].toIntOrNull() ?: cur.srcDpi)
                    t == "-col" && i + 1 < tokens.size -> cur = cur.copy(maxColumns = tokens[++i].toIntOrNull() ?: cur.maxColumns)
                    t == "-cg" && i + 1 < tokens.size -> cur = cur.copy(colGapMin = tokens[++i].toFloatOrNull() ?: cur.colGapMin)
                    t == "-cgr" && i + 1 < tokens.size -> cur = cur.copy(colGapRange = tokens[++i].toFloatOrNull() ?: cur.colGapRange)
                    t == "-ws" && i + 1 < tokens.size -> cur = cur.copy(wordSpacing = tokens[++i].toFloatOrNull() ?: cur.wordSpacing)
                    t.startsWith("-wrap") -> cur = cur.copy(wrapText = !t.endsWith("-"))
                    t.startsWith("-hy") -> cur = cur.copy(keepHyphens = !t.endsWith("-"))
                    t == "-j" && i + 1 < tokens.size -> cur = cur.copy(justification = tokens[++i].toIntOrNull() ?: cur.justification)
                    t.startsWith("-r2l") -> cur = cur.copy(rtlMode = !t.endsWith("-"))
                    t == "-ac" && i + 1 < tokens.size -> cur = cur.copy(autoCropThreshold = tokens[++i].toFloatOrNull() ?: cur.autoCropThreshold)
                    t == "-c" && i + 1 < tokens.size -> cur = cur.copy(contrast = tokens[++i].toFloatOrNull() ?: cur.contrast)
                    t == "-g" && i + 1 < tokens.size -> cur = cur.copy(gamma = tokens[++i].toFloatOrNull() ?: cur.gamma)
                    t.startsWith("-sh") -> cur = cur.copy(sharpen = !t.endsWith("-"))
                    t == "-d+" || t == "-d" -> cur = cur.copy(dither = true)
                    t == "-d-" -> cur = cur.copy(dither = false)
                    t == "-bpc" && i + 1 < tokens.size -> cur = cur.copy(bitsPerPixel = tokens[++i].toIntOrNull() ?: cur.bitsPerPixel)
                    t == "-neg+" -> cur = cur.copy(negateColors = 2)
                    t == "-neg" -> cur = cur.copy(negateColors = 1)
                    t == "-neg-" -> cur = cur.copy(negateColors = 0)
                    t.startsWith("-as") -> cur = cur.copy(autoStraightenDeskew = !t.endsWith("-"))
                    t == "-p" && i + 1 < tokens.size -> cur = cur.copy(pageRange = tokens[++i])
                    t.startsWith("-toc") -> cur = cur.copy(preserveToc = !t.endsWith("-"))
                    t == "-n+" || t == "-n" -> cur = cur.copy(nativePdfMode = true)
                    t == "-n-" -> cur = cur.copy(nativePdfMode = false)
                    t == "-ocrlang" && i + 1 < tokens.size -> cur = cur.copy(ocrLanguages = tokens[++i])
                    t == "-ocrmt" && i + 1 < tokens.size -> cur = cur.copy(ocrThreads = tokens[++i].toIntOrNull() ?: cur.ocrThreads)
                }
                i++
            }
            return cur
        }
    }
}

data class DetectedBlock(
    val id: String,
    val pageIndex: Int, // 1-based
    val type: BlockType,
    val leftNorm: Float,
    val topNorm: Float,
    val rightNorm: Float,
    val bottomNorm: Float,
    val readingOrder: Int,
    val scriptDirection: ScriptDirection = ScriptDirection.LTR,
    val protectPolicy: ProtectPolicy = when (type) {
        BlockType.TABLE -> ProtectPolicy.SCALE_TO_FIT
        BlockType.FIGURE, BlockType.EQUATION -> ProtectPolicy.KEEP_AS_IMAGE
        else -> ProtectPolicy.REFLOW
    },
    val extractedText: String = "",
    val ocrConfidence: Int = 94,
    val langTag: String = "en"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("pageIndex", pageIndex)
        put("type", type.name)
        put("leftNorm", leftNorm.toDouble())
        put("topNorm", topNorm.toDouble())
        put("rightNorm", rightNorm.toDouble())
        put("bottomNorm", bottomNorm.toDouble())
        put("readingOrder", readingOrder)
        put("scriptDirection", scriptDirection.name)
        put("protectPolicy", protectPolicy.name)
        put("extractedText", extractedText)
        put("ocrConfidence", ocrConfidence)
        put("langTag", langTag)
    }

    companion object {
        fun fromJson(obj: JSONObject): DetectedBlock {
            return DetectedBlock(
                id = obj.optString("id", "blk_0"),
                pageIndex = obj.optInt("pageIndex", 1),
                type = BlockType.entries.find { it.name == obj.optString("type") } ?: BlockType.TEXT,
                leftNorm = obj.optDouble("leftNorm", 0.08).toFloat(),
                topNorm = obj.optDouble("topNorm", 0.08).toFloat(),
                rightNorm = obj.optDouble("rightNorm", 0.92).toFloat(),
                bottomNorm = obj.optDouble("bottomNorm", 0.25).toFloat(),
                readingOrder = obj.optInt("readingOrder", 1),
                scriptDirection = ScriptDirection.entries.find { it.name == obj.optString("scriptDirection") } ?: ScriptDirection.LTR,
                protectPolicy = ProtectPolicy.entries.find { it.name == obj.optString("protectPolicy") } ?: ProtectPolicy.REFLOW,
                extractedText = obj.optString("extractedText", ""),
                ocrConfidence = obj.optInt("ocrConfidence", 94),
                langTag = obj.optString("langTag", "en")
            )
        }

        fun listToJsonString(blocks: List<DetectedBlock>): String {
            val arr = JSONArray()
            blocks.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJsonString(json: String): List<DetectedBlock> {
            if (json.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class DocumentAnalysisResult(
    val fileName: String,
    val pageCount: Int,
    val estimatedColumns: Int,
    val hasEmbeddedTextLayer: Boolean,
    val detectedScripts: List<String>,
    val suggestedOcrLangs: String,
    val scriptDirection: ScriptDirection,
    val tableBlockCount: Int,
    val figureBlockCount: Int,
    val equationBlockCount: Int,
    val estimatedSkewDegrees: Float,
    val backgroundNoiseRatio: Float,
    val detectedDocType: DetectedDocType,
    val rationale: String
)

data class PageQualityScore(
    val pageNumber: Int,
    val overallScore: Int, // 0..100
    val ocrConfidence: Int, // 0..100
    val regionOverlapPercent: Float,
    val lineBreakAnomalies: Int,
    val flaggedForReview: Boolean,
    val reason: String
)

data class QueuedDocumentItem(
    val id: String,
    val fileName: String,
    val sourcePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val status: String = "QUEUED", // QUEUED, PROCESSING, PAUSED, COMPLETED, FAILED
    val progressPercent: Int = 0,
    val currentPage: Int = 0,
    val analysis: DocumentAnalysisResult? = null,
    val pageQualityScores: List<PageQualityScore> = emptyList(),
    val outputFilePath: String? = null,
    val outputFormat: OutputFormat = OutputFormat.SEARCHABLE_PDF,
    val outputSizeBytes: Long = 0L,
    val elapsedMs: Long = 0L,
    val avgOcrConfidence: Int = 0,
    val extractedTextPreview: String = "",
    val warnings: List<String> = emptyList()
)

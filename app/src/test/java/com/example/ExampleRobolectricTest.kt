package com.example

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.example.data.K2Database
import com.example.engine.K2PdfOptEngine
import com.example.engine.SampleCorpusGenerator
import com.example.model.BlockType
import com.example.model.ConversionMode
import com.example.model.DetectedDocType
import com.example.model.K2Settings
import com.example.model.OutputFormat
import com.example.model.ProtectPolicy
import com.example.ocr.TessdataManager
import com.example.util.LocaleFormatUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify all 18 launch UI languages, plurals, and RTL layout direction`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("K2pdfopt", baseContext.getString(R.string.app_name))

        for (opt in LocaleFormatUtils.SUPPORTED_LOCALES) {
            val locCtx = LocaleFormatUtils.createLocalizedContext(baseContext, opt.tag)
            val appName = locCtx.getString(R.string.app_name)
            val tagline = locCtx.getString(R.string.app_tagline)
            val tooltipMode = locCtx.getString(R.string.tooltip_conversion_mode)
            val plural1 = locCtx.resources.getQuantityString(R.plurals.plural_pages_processed, 1, 1)
            val plural5 = locCtx.resources.getQuantityString(R.plurals.plural_pages_processed, 5, 5)

            assertEquals("K2pdfopt", appName)
            assertTrue("Tagline empty for ${opt.tag}", tagline.isNotBlank())
            assertTrue("CLI flag -mode missing in tooltip for ${opt.tag}", tooltipMode.contains("-mode"))
            assertTrue("Plural(1) empty for ${opt.tag}", plural1.isNotBlank())
            assertTrue("Plural(5) empty for ${opt.tag}", plural5.isNotBlank())

            val expectedDir = if (opt.isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            assertEquals("Layout direction mismatch for ${opt.tag}", expectedDir, locCtx.resources.configuration.layoutDirection)
        }
    }

    @Test
    fun `verify two column scientific journal conversion, table protection, EPUB3 and HTML5 export`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = K2Database.getInstance(context)
        val tess = TessdataManager(context)
        val engine = K2PdfOptEngine(context, db.dao(), tess)

        val outDir = File(context.cacheDir, "test_corpus").apply { mkdirs() }
        val journalPdf = File(outDir, "scientific_journal_2col.pdf")
        SampleCorpusGenerator.generateScientificJournal2ColPdf(journalPdf)
        assertTrue(journalPdf.exists() && journalPdf.length() > 500L)

        // 1. Auto document-type detection
        val analysis = engine.analyzeDocument(journalPdf)
        assertEquals(2, analysis.pageCount)
        assertEquals(DetectedDocType.JOURNAL_2COL, analysis.detectedDocType)
        assertTrue(analysis.tableBlockCount >= 1)

        // 2. Layout block detection & protected table/equation verification
        val settings = K2Settings(mode = ConversionMode.TWO_COLUMN, maxColumns = 2)
        val p1Blocks = engine.detectPageLayoutBlocks(journalPdf, 1, settings)
        val tableBlock = p1Blocks.find { it.type == BlockType.TABLE }
        val eqBlock = p1Blocks.find { it.type == BlockType.EQUATION }
        assertTrue("Protected table block must be detected on Page 1", tableBlock != null)
        assertEquals(ProtectPolicy.SCALE_TO_FIT, tableBlock?.protectPolicy)
        assertTrue("Protected inline equation block must be detected on Page 1", eqBlock != null)

        // 3. Convert to Searchable PDF
        val pdfRes = engine.convertDocument(journalPdf, settings.copy(outputFormat = OutputFormat.SEARCHABLE_PDF)) { _, _, _ -> }.getOrThrow()
        assertTrue(pdfRes.outputFile.exists() && pdfRes.outputFile.length() > 1000L)
        assertEquals(2, pdfRes.pagesProcessed)

        // 4. Convert to Reflowable EPUB 3 and verify ZIP entries
        val epubRes = engine.convertDocument(journalPdf, settings.copy(outputFormat = OutputFormat.EPUB)) { _, _, _ -> }.getOrThrow()
        assertTrue(epubRes.outputFile.exists())
        ZipFile(epubRes.outputFile).use { zip ->
            assertTrue(zip.getEntry("mimetype") != null)
            assertTrue(zip.getEntry("OEBPS/content.opf") != null)
            assertTrue(zip.getEntry("OEBPS/chapter_1.xhtml") != null)
        }

        // 5. Convert to Semantic HTML5 and verify clickable citation links
        val htmlRes = engine.convertDocument(journalPdf, settings.copy(outputFormat = OutputFormat.HTML5)) { _, _, _ -> }.getOrThrow()
        val htmlContent = htmlRes.outputFile.readText(Charsets.UTF_8)
        assertTrue(htmlContent.contains("href=\"#fn-1\""))
        assertTrue(htmlContent.contains("protected-block"))
    }

    @Test
    fun `verify multilingual 10-script OCR, bidirectional Arabic+English, and vertical CJK`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tess = TessdataManager(context)
        val outDir = File(context.cacheDir, "test_multilingual").apply { mkdirs() }
        val multiPdf = File(outDir, "multilingual_ocr_corpus.pdf")
        val generated = SampleCorpusGenerator.generateMultilingualOcrCorpusPdf(multiPdf)
        assertEquals(5, generated.pageCount)

        // Page 1: Latin (English, French diacritics, German umlauts)
        val p1Text = SampleCorpusGenerator.readSidecarTextForPage(multiPdf, 1) ?: ""
        val p1Script = tess.detectPageScriptAndLanguages(p1Text)
        assertEquals("Latin", p1Script.primaryScript)
        assertTrue("French diacritics & ligatures must be preserved verbatim", p1Text.contains("L'œuvre") && p1Text.contains("Größenordnung"))

        // Page 2: Arabic RTL + Embedded English Technical Terms + Hebrew
        val p2Text = SampleCorpusGenerator.readSidecarTextForPage(multiPdf, 2) ?: ""
        val p2Script = tess.detectPageScriptAndLanguages(p2Text)
        assertTrue(p2Script.isRtlDominant)
        assertTrue("Mixed bidirectional RTL+LTR must be detected", p2Script.hasBidirectionalMix)
        assertTrue(p2Script.detectedLangCodes.containsAll(listOf("ara", "heb", "eng")))
        assertTrue("Arabic Tashkeel diacritics must be preserved", p2Text.contains("بِسْمِ"))

        // Page 3: Cyrillic (Russian) & Devanagari (Hindi)
        val p3Text = SampleCorpusGenerator.readSidecarTextForPage(multiPdf, 3) ?: ""
        val p3Script = tess.detectPageScriptAndLanguages(p3Text)
        assertTrue(p3Script.detectedLangCodes.containsAll(listOf("rus", "hin")))

        // Page 4 & 5: Horizontal & Vertical CJK (Chinese, Japanese, Korean)
        val p4Text = SampleCorpusGenerator.readSidecarTextForPage(multiPdf, 4) ?: ""
        val p4Script = tess.detectPageScriptAndLanguages(p4Text)
        assertTrue(p4Script.detectedLangCodes.any { it.startsWith("jpn") || it.startsWith("chi") || it.startsWith("kor") })

        val p5Text = SampleCorpusGenerator.readSidecarTextForPage(multiPdf, 5) ?: ""
        val p5Script = tess.detectPageScriptAndLanguages(p5Text, isVerticalAspectHint = true)
        assertFalse(p5Script.detectedLangCodes.isEmpty())
        assertTrue("Vertical CJK layout must be detected", p5Script.isVerticalCjk)
    }
}

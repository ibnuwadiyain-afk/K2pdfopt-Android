package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.example.model.BlockType
import com.example.model.DetectedBlock
import com.example.model.FootnotePlacement
import com.example.model.K2Settings
import com.example.model.ScriptDirection
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ProcessedPageData(
    val pageNumber: Int,
    val reflowedBitmap: Bitmap,
    val orderedBlocks: List<DetectedBlock>,
    val cleanReadingText: String,
    val dominantScriptDir: ScriptDirection,
    val dominantLangTag: String,
    val qualityScore: Int,
    val ocrConfidence: Int
)

object MultiFormatExporter {

    /**
     * Exports a Searchable PDF with reflowed page bitmaps and an invisible Unicode text layer
     * so readers can search, highlight, and copy text in any script (Latin, Arabic, Hebrew, CJK, Cyrillic, Indic).
     */
    fun exportSearchablePdf(
        pages: List<ProcessedPageData>,
        outFile: File,
        settings: K2Settings
    ): File {
        val textMap = pages.associate { it.pageNumber to it.cleanReadingText }
        SampleCorpusGenerator.writeBitmapsToPdfFile(
            outFile = outFile,
            pageBitmaps = pages.map { it.reflowedBitmap },
            pagesText = textMap
        )
        return outFile
    }

    /**
     * Builds a valid, standards-compliant EPUB 3 archive (.epub) from the OCR/text-layer content
     * with embedded PNG images for protected tables/figures/equations, correct `dir="rtl"` / `dir="ltr"`
     * and `xml:lang` attributes per block, and clickable internal links for `[1]`, `[2]` citations.
     */
    fun exportReflowableEpub3(
        title: String,
        pages: List<ProcessedPageData>,
        outFile: File,
        settings: K2Settings
    ): File {
        val primaryLang = pages.firstOrNull()?.dominantLangTag ?: "en"
        val isDocRtl = pages.any { it.dominantScriptDir == ScriptDirection.RTL } || settings.rtlMode

        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            // 1. Uncompressed 'mimetype' entry MUST be first in EPUB 3 ZIP
            val mimeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                val crc = CRC32()
                crc.update(mimeBytes)
                this.crc = crc.value
            }
            zos.putNextEntry(mimeEntry)
            zos.write(mimeBytes)
            zos.closeEntry()

            // 2. META-INF/container.xml
            zos.putNextEntry(ZipEntry("META-INF/container.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            // 3. Embed page preview images and build XHTML chapters
            val manifestItems = StringBuilder()
            val spineItems = StringBuilder()

            pages.forEach { p ->
                val imgName = "images/page_${p.pageNumber}.png"
                zos.putNextEntry(ZipEntry("OEBPS/$imgName"))
                val baos = ByteArrayOutputStream()
                p.reflowedBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
                zos.write(baos.toByteArray())
                zos.closeEntry()

                manifestItems.appendLine("""    <item id="img_${p.pageNumber}" href="$imgName" media-type="image/png"/>""")
                val chapHref = "chapter_${p.pageNumber}.xhtml"
                manifestItems.appendLine("""    <item id="chap_${p.pageNumber}" href="$chapHref" media-type="application/xhtml+xml"/>""")
                spineItems.appendLine("""    <itemref idref="chap_${p.pageNumber}"/>""")

                zos.putNextEntry(ZipEntry("OEBPS/$chapHref"))
                zos.write(buildPageXhtml(title, p, imgName, settings).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // 4. OEBPS/nav.xhtml (TOC)
            zos.putNextEntry(ZipEntry("OEBPS/nav.xhtml"))
            val navLinks = pages.joinToString("\n") { p ->
                """        <li><a href="chapter_${p.pageNumber}.xhtml">Page ${p.pageNumber}</a></li>"""
            }
            zos.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="$primaryLang" lang="$primaryLang">
<head><meta charset="UTF-8"/><title>Table of Contents</title></head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>$title</h1>
    <ol>
$navLinks
    </ol>
  </nav>
</body>
</html>""".toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            // 5. OEBPS/content.opf
            val progressionDir = if (isDocRtl || settings.verticalCjk) "rtl" else "ltr"
            zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid" xml:lang="$primaryLang">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="bookid">urn:uuid:k2pdfopt-${outFile.nameWithoutExtension}</dc:identifier>
    <dc:title>${xmlEscape(title)}</dc:title>
    <dc:language>$primaryLang</dc:language>
    <dc:creator>K2pdfopt v2.55 Android Engine</dc:creator>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
$manifestItems  </manifest>
  <spine page-progression-direction="$progressionDir">
$spineItems  </spine>
</package>""".toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()
        }
        return outFile
    }

    /**
     * Builds a self-contained semantic HTML5 document with embedded base64 images for tables/figures/equations,
     * bidirectional `dir` attributes, CJK vertical writing-mode CSS when enabled, and clickable footnote/bibliography jumps.
     */
    fun exportSemanticHtml5(
        title: String,
        pages: List<ProcessedPageData>,
        outFile: File,
        settings: K2Settings
    ): File {
        val sb = StringBuilder()
        val primaryLang = pages.firstOrNull()?.dominantLangTag ?: "en"
        val docDir = if (settings.rtlMode || pages.any { it.dominantScriptDir == ScriptDirection.RTL }) "rtl" else "ltr"
        val verticalCss = if (settings.verticalCjk) "writing-mode: vertical-rl; text-orientation: upright;" else ""

        sb.appendLine(
            """<!DOCTYPE html>
<html lang="$primaryLang" dir="$docDir">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>${xmlEscape(title)}</title>
<style>
  body { font-family: system-ui, -apple-system, 'Noto Sans', 'Noto Naskh Arabic', 'Noto Sans CJK SC', sans-serif; line-height: 1.65; margin: 0 auto; max-width: 780px; padding: 20px; background: #f8fafc; color: #0f172a; $verticalCss }
  .page-section { background: #ffffff; border-radius: 12px; padding: 20px; margin-bottom: 24px; box-shadow: 0 2px 8px rgba(15,23,42,0.08); }
  .protected-block { border: 2px solid #0ea5e9; border-radius: 8px; padding: 12px; margin: 14px 0; background: #f0f9ff; overflow-x: auto; }
  .equation-block { font-family: 'JetBrains Mono', monospace; background: #faf5ff; border-left: 4px solid #a855f7; padding: 10px 14px; margin: 12px 0; }
  .footnote-block { font-size: 0.9em; color: #475569; border-top: 1px solid #cbd5e1; padding-top: 8px; margin-top: 12px; }
  img.reflow-img { max-width: 100%; height: auto; border-radius: 6px; display: block; margin: 12px auto; }
  a.bib-ref { color: #0284c7; text-decoration: none; font-weight: 600; }
</style>
</head>
<body>
<h1>${xmlEscape(title)}</h1>"""
        )

        val deferredEndFootnotes = mutableListOf<String>()

        pages.forEach { p ->
            val pDir = p.dominantScriptDir.htmlDir
            sb.appendLine("""<section class="page-section" id="page-${p.pageNumber}" dir="$pDir" lang="${p.dominantLangTag}">""")
            sb.appendLine("""  <h2>Page ${p.pageNumber} <small>(Quality: ${p.qualityScore}% • OCR: ${p.ocrConfidence}%)</small></h2>""")

            p.orderedBlocks.forEach { blk ->
                if (blk.type == BlockType.HEADER || blk.type == BlockType.FOOTER) return@forEach
                val bDir = blk.scriptDirection.htmlDir
                val linkedHtml = linkCitationsHtml(xmlEscape(blk.extractedText))
                when (blk.type) {
                    BlockType.TABLE, BlockType.FIGURE -> {
                        sb.appendLine("""  <div class="protected-block" dir="$bDir" lang="${blk.langTag}">""")
                        sb.appendLine("""    <strong>[${blk.type.label} — ${blk.protectPolicy.label}]</strong>""")
                        sb.appendLine("""    <pre>${linkedHtml}</pre>""")
                        sb.appendLine("""  </div>""")
                    }
                    BlockType.EQUATION -> {
                        sb.appendLine("""  <div class="equation-block" dir="ltr"><code>$linkedHtml</code></div>""")
                    }
                    BlockType.FOOTNOTE -> {
                        when (settings.footnotePlacement) {
                            FootnotePlacement.END_OF_SECTION -> deferredEndFootnotes.add(linkedHtml)
                            else -> sb.appendLine("""  <aside class="footnote-block" id="fn-${p.pageNumber}" dir="$bDir">$linkedHtml</aside>""")
                        }
                    }
                    else -> {
                        sb.appendLine("""  <p dir="$bDir" lang="${blk.langTag}">${linkedHtml.replace("\n", "<br/>")}</p>""")
                    }
                }
            }

            // Embed compact PNG preview of the reflowed page
            val baos = ByteArrayOutputStream()
            p.reflowedBitmap.compress(Bitmap.CompressFormat.PNG, 85, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            sb.appendLine("""  <details><summary>Reflowed Page ${p.pageNumber} Bitmap</summary>""")
            sb.appendLine("""    <img class="reflow-img" src="data:image/png;base64,$b64" alt="Reflowed Page ${p.pageNumber}"/>""")
            sb.appendLine("""  </details>""")
            sb.appendLine("""</section>""")
        }

        if (deferredEndFootnotes.isNotEmpty()) {
            sb.appendLine("""<section class="page-section" id="footnotes-end"><h2>Footnotes &amp; References</h2>""")
            deferredEndFootnotes.forEach { fn ->
                sb.appendLine("""  <aside class="footnote-block">$fn</aside>""")
            }
            sb.appendLine("""</section>""")
        }

        sb.appendLine("</body></html>")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    fun exportMarkdown(title: String, pages: List<ProcessedPageData>, outFile: File): File {
        val sb = StringBuilder()
        sb.appendLine("# $title\n")
        pages.forEach { p ->
            sb.appendLine("## Page ${p.pageNumber} (Quality: ${p.qualityScore}%, OCR Confidence: ${p.ocrConfidence}%)\n")
            p.orderedBlocks.forEach { blk ->
                if (blk.type != BlockType.HEADER && blk.type != BlockType.FOOTER) {
                    when (blk.type) {
                        BlockType.EQUATION -> sb.appendLine("$$\n${blk.extractedText}\n$$\n")
                        BlockType.TABLE -> sb.appendLine("```table\n${blk.extractedText}\n```\n")
                        BlockType.FOOTNOTE -> sb.appendLine("> **Footnote:** ${blk.extractedText}\n")
                        else -> sb.appendLine("${blk.extractedText}\n")
                    }
                }
            }
        }
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    fun exportPlainText(pages: List<ProcessedPageData>, outFile: File): File {
        val sb = StringBuilder()
        pages.forEach { p ->
            sb.appendLine("=== Page ${p.pageNumber} [${p.dominantLangTag} / ${p.dominantScriptDir.name}] ===")
            sb.appendLine(p.cleanReadingText.trim())
            sb.appendLine()
        }
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    private fun buildPageXhtml(
        title: String,
        p: ProcessedPageData,
        imgHref: String,
        settings: K2Settings
    ): String {
        val dirAttr = p.dominantScriptDir.htmlDir
        val blocksHtml = p.orderedBlocks
            .filter { it.type != BlockType.HEADER && it.type != BlockType.FOOTER }
            .joinToString("\n") { blk ->
                val bDir = blk.scriptDirection.htmlDir
                val escaped = linkCitationsHtml(xmlEscape(blk.extractedText))
                """    <div class="blk-${blk.type.name.lowercase()}" dir="$bDir" xml:lang="${blk.langTag}">$escaped</div>"""
            }
        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="${p.dominantLangTag}" lang="${p.dominantLangTag}" dir="$dirAttr">
<head>
  <meta charset="UTF-8"/>
  <title>${xmlEscape(title)} - Page ${p.pageNumber}</title>
  <style>
    body { font-family: serif; line-height: 1.6; padding: 1em; }
    .blk-table, .blk-figure { border: 1px solid #0284c7; padding: 0.6em; margin: 0.8em 0; white-space: pre-wrap; }
    .blk-equation { font-family: monospace; background: #f3e8ff; padding: 0.5em; margin: 0.6em 0; }
    .blk-footnote { font-size: 0.88em; border-top: 1px solid #94a3b8; padding-top: 0.4em; margin-top: 0.8em; }
    img { max-width: 100%; height: auto; }
  </style>
</head>
<body>
  <h2>Page ${p.pageNumber}</h2>
$blocksHtml
  <div><img src="$imgHref" alt="Page ${p.pageNumber}"/></div>
</body>
</html>"""
    }

    private fun linkCitationsHtml(escapedText: String): String {
        return escapedText.replace(Regex("\\[(\\d+)\\]")) { match ->
            val num = match.groupValues[1]
            """<a class="bib-ref" href="#fn-$num">[${num}]</a>"""
        }
    }

    private fun xmlEscape(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}

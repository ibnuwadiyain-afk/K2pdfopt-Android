package com.example

import com.example.engine.K2PdfOptNativeBridge
import com.example.model.ConversionMode
import com.example.model.K2Settings
import com.example.util.LocaleFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun `cli flag parser and golden output equivalence`() {
        val rawFlags = "-mode 2col -w 1264 -h 1680 -dpi 300 -col 2 -r2l+ -bpc 2 -neg+ -ocrlang eng+ara+fra -p 1-10"
        val parsed = K2Settings.parseCliFlagsInto(K2Settings(), rawFlags)

        assertEquals(ConversionMode.TWO_COLUMN, parsed.mode)
        assertEquals(1264, parsed.dstWidthPx)
        assertEquals(1680, parsed.dstHeightPx)
        assertEquals(300, parsed.dstDpi)
        assertEquals(2, parsed.maxColumns)
        assertTrue(parsed.rtlMode)
        assertEquals(2, parsed.bitsPerPixel)
        assertEquals(2, parsed.negateColors)
        assertEquals("eng+ara+fra", parsed.ocrLanguages)
        assertEquals("1-10", parsed.pageRange)

        val normalized = K2PdfOptNativeBridge.normalizeCliFlags(rawFlags)
        assertTrue(normalized.contains("-mode 2col"))
        assertTrue(normalized.contains("-dpi 300"))
        assertTrue(normalized.contains("-r2l+"))
        assertTrue(normalized.contains("-ocrlang eng+ara+fra"))
    }

    @Test
    fun `profile json serialization round trip preserves all k2pdfopt flags`() {
        val original = K2Settings(
            mode = ConversionMode.FIT_WIDTH,
            dstWidthPx = 1440,
            dstHeightPx = 1920,
            dstDpi = 300,
            rtlMode = true,
            verticalCjk = true,
            ocrLanguages = "chi_sim_vert+jpn_vert",
            targetSizeKb = 512
        )
        val json = original.toJson()
        val restored = K2Settings.fromJson(json)
        assertEquals(original.mode, restored.mode)
        assertEquals(original.dstWidthPx, restored.dstWidthPx)
        assertEquals(original.rtlMode, restored.rtlMode)
        assertEquals(original.verticalCjk, restored.verticalCjk)
        assertEquals(original.ocrLanguages, restored.ocrLanguages)
        assertEquals(original.targetSizeKb, restored.targetSizeKb)
    }

    @Test
    fun `arabic indic and persian digit localization works accurately`() {
        val arabicNum = LocaleFormatUtils.localizeDigits("255", "ar")
        assertEquals("٢٥٥", arabicNum)

        val persianNum = LocaleFormatUtils.localizeDigits("255", "fa")
        assertEquals("۲۵۵", persianNum)

        assertTrue(LocaleFormatUtils.isRtlLocale("ar"))
        assertTrue(LocaleFormatUtils.isRtlLocale("fa"))
        assertTrue(LocaleFormatUtils.isRtlLocale("ur"))
        assertTrue(LocaleFormatUtils.isRtlLocale("he"))
    }
}

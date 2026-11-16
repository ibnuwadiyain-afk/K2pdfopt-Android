package com.example.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

data class AppLocaleOption(
    val tag: String,
    val englishName: String,
    val nativeName: String,
    val isRtl: Boolean,
    val reviewStatus: String
)

object LocaleFormatUtils {

    val SUPPORTED_LOCALES: List<AppLocaleOption> = listOf(
        AppLocaleOption("en", "English", "English", false, "Source Reference (Reviewed)"),
        AppLocaleOption("ar", "Arabic", "العربية", true, "Glossary-Verified Machine Translation"),
        AppLocaleOption("fr", "French", "Français", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("es", "Spanish", "Español", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("de", "German", "Deutsch", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("it", "Italian", "Italiano", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("pt", "Portuguese", "Português", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("ru", "Russian", "Русский", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("tr", "Turkish", "Türkçe", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("fa", "Persian", "فارسی", true, "Glossary-Verified Machine Translation"),
        AppLocaleOption("ur", "Urdu", "اردو", true, "Glossary-Verified Machine Translation"),
        AppLocaleOption("he", "Hebrew", "עברית", true, "Glossary-Verified Machine Translation"),
        AppLocaleOption("hi", "Hindi", "हिन्दी", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("id", "Indonesian", "Bahasa Indonesia", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("zh-CN", "Chinese (Simplified)", "简体中文", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("zh-TW", "Chinese (Traditional)", "繁體中文", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("ja", "Japanese", "日本語", false, "Glossary-Verified Machine Translation"),
        AppLocaleOption("ko", "Korean", "한국어", false, "Glossary-Verified Machine Translation")
    )

    fun resolveLocale(tag: String): Locale {
        return when (tag) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            "he", "iw" -> Locale("iw")
            "id", "in" -> Locale("in")
            else -> Locale.forLanguageTag(tag)
        }
    }

    fun isRtlLocale(tag: String): Boolean {
        val clean = tag.lowercase(Locale.ROOT)
        return clean.startsWith("ar") || clean.startsWith("fa") || clean.startsWith("ur") ||
            clean.startsWith("he") || clean.startsWith("iw")
    }

    fun applyAppLocale(context: Context, tag: String) {
        val locale = resolveLocale(tag)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java)
            lm?.applicationLocales = LocaleList.forLanguageTags(tag)
        }
    }

    fun createLocalizedContext(baseContext: Context, tag: String): Context {
        val locale = resolveLocale(tag)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(config)
    }

    /**
     * Converts Western ASCII digits to Eastern Arabic-Indic (٠-٩) for Arabic
     * or Extended Arabic-Indic (۰-۹) for Persian/Urdu when the active locale uses them.
     */
    fun localizeDigits(input: String, localeTag: String): String {
        val lang = localeTag.lowercase(Locale.ROOT)
        val zeroChar: Char = when {
            lang.startsWith("ar") -> '\u0660'
            lang.startsWith("fa") -> '\u06F0'
            else -> return input
        }
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append((zeroChar.code + (ch - '0')).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatInteger(value: Long, localeTag: String): String {
        val locale = resolveLocale(localeTag)
        val formatted = NumberFormat.getIntegerInstance(locale).format(value)
        return localizeDigits(formatted, localeTag)
    }

    fun formatPercent(percentZeroToHundred: Int, localeTag: String): String {
        val locale = resolveLocale(localeTag)
        val nf = NumberFormat.getPercentInstance(locale)
        return localizeDigits(nf.format(percentZeroToHundred / 100.0), localeTag)
    }

    fun formatFileSize(bytes: Long, localeTag: String): String {
        val locale = resolveLocale(localeTag)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 1
        }
        val (amount, unit) = when {
            bytes >= 1024L * 1024L -> (bytes.toDouble() / (1024.0 * 1024.0)) to "MB"
            bytes >= 1024L -> (bytes.toDouble() / 1024.0) to "KB"
            else -> bytes.toDouble() to "B"
        }
        val numStr = localizeDigits(nf.format(amount), localeTag)
        return "$numStr $unit"
    }

    fun formatTimestamp(epochMs: Long, localeTag: String): String {
        val locale = resolveLocale(localeTag)
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        return localizeDigits(df.format(Date(epochMs)), localeTag)
    }
}

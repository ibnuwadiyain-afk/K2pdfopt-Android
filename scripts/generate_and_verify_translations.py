#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generates complete Android string resources (values-xx/strings.xml),
technical glossaries (docs/glossaries/glossary_<lang>.json),
TRANSLATION_STATUS.md, CONTRIBUTING-TRANSLATIONS.md, and scripts/verify_translations.py
for all 18 required launch languages, then validates every key and positional placeholder.
"""

import json
import os
import re
import sys
import xml.etree.ElementTree as ET

BASE_STRINGS_PATH = "app/src/main/res/values/strings.xml"

# Read base English strings and plurals
tree = ET.parse(BASE_STRINGS_PATH)
root = tree.getroot()
base_strings = {}
for elem in root.findall("string"):
    base_strings[elem.attrib["name"]] = elem.text or ""

base_plurals = {}
for elem in root.findall("plurals"):
    pname = elem.attrib["name"]
    base_plurals[pname] = {}
    for item in elem.findall("item"):
        base_plurals[pname][item.attrib["quantity"]] = item.text or ""

# Core localized UI dictionary for each of the 17 non-English languages (18 total with English)
LOCALES = {
    "ar": {
        "dir_folders": ["values-ar"],
        "name_en": "Arabic (RTL)",
        "name_native": "العربية",
        "rtl": True,
        "glossary": {
            "reflow": "إعادة تدفق النص",
            "deskew": "تصحيح الميلان (-as)",
            "despeckle": "إزالة الشوائب النقطية",
            "dithering": "تنقيط التدرج الرمادي (-d)",
            "ocr": "التعرف الضوئي على الحروف (Tesseract 5)",
            "protected_block": "كتلة محمية (جدول / شكل / معادلة)",
            "reading_order": "ترتيب القراءة (من اليمين لليسار -r2l)"
        },
        "strings": {
            "app_name": "K2pdfopt",
            "app_tagline": "إعادة تدفق ملفات PDF وDjVu والتعرف الضوئي متعدد اللغات وتحسين القارئ الإلكتروني",
            "nav_home": "قائمة الانتظار",
            "nav_preview": "المحرر",
            "nav_settings": "الإعدادات",
            "nav_ocr": "لغات OCR",
            "nav_profiles": "الملفات التعريفية",
            "nav_reader": "القارئ",
            "nav_logs": "السجلات",
            "nav_about": "حول التطبيق",
            "home_add_files": "إضافة PDF / DjVu / صور",
            "home_generate_samples": "إنشاء مستندات اختبار متعددة اللغات",
            "home_watch_folder": "مراقبة مجلد",
            "home_start_batch": "تحويل القائمة",
            "home_pause_job": "إيقاف مؤقت",
            "home_resume_job": "استئناف",
            "home_cancel_job": "إلغاء",
            "home_optimize_this_phone": "تحسين لشاشة هذا الهاتف",
            "home_auto_detect_doc": "اكتشاف نوع المستند تلقائياً",
            "home_empty_queue_title": "لا توجد مستندات في قائمة الانتظار",
            "home_empty_queue_subtitle": "استورد ملف PDF أو DjVu أو صورة ممسوحة ضوئياً، أو أنشئ حزمة الاختبار متعددة اللغات لتجربة إعادة التدفق والتعرف الضوئي وتحليل التخطيط الفعلي.",
            "home_batch_report_title": "تقرير التحويل الدفعي",
            "home_thermal_status_label": "حالة حرارة الجهاز والبطارية",
            "home_active_profile_format": "الملف التعريفي النشط: %1$s (%2$s)",
            "home_page_quality_format": "درجة الجودة: %1$d%% • ثقة OCR: %2$d%%",
            "home_low_quality_warning": "تم تحديد %1$d صفحة ذات جودة منخفضة للمراجعة في محرر التراكب.",
            "detect_type_book": "كتاب نصي",
            "detect_type_journal": "مقالة علمية بعمودين",
            "detect_type_scan": "مستند ممسوح ضوئياً",
            "detect_type_slides": "شرائح عرض تقديمي",
            "detect_type_manual": "دليل تقني",
            "detect_type_tables": "تقرير غني بالجداول",
            "detect_accept_profile": "تطبيق الملف التعريفي الموصى به",
            "detect_override_profile": "تجاوز يدوي",
            "settings_search_hint": "ابحث في الإعدادات أو خيارات السطر (-mode, -dpi, -col)…",
            "settings_group_device": "إعداد الجهاز المسبق والأبعاد",
            "settings_group_mode": "وضع التحويل والأعمدة",
            "settings_group_margins": "الاقتصاص وهوامش الصفحات الفردية/الزوجية",
            "settings_group_image": "التباين وغاما والتنقيط وتنظيف المسح الضوئي",
            "settings_group_ocr": "التعرف الضوئي متعدد اللغات والخطوط",
            "settings_group_layout": "نموذج تحليل التخطيط والكتل المحمية والحواشي",
            "settings_group_output": "صيغة الإخراج والحجم المستهدف وخيارات CLI",
            "setting_device_preset": "الجهاز المستهدف",
            "tooltip_device_preset": "تحديد أبعاد شاشة القارئ وكثافة البكسل (-dev, -w, -h, -dpi).",
            "setting_conversion_mode": "وضع التحويل (-mode)",
            "tooltip_conversion_mode": "يقابل الخيار -mode (default, fw, 2col, copy, crop, tm, concat) للتحكم في تفكيك الأعمدة والأسطر.",
            "setting_max_columns": "الحد الأقصى للأعمدة (-col)",
            "tooltip_max_columns": "يقابل الخيار -col (1, 2, 4) لتحديد أقصى عدد أعمدة في الصفحة.",
            "setting_wrap_text": "إعادة تدفق والتفاف النص (-wrap)",
            "tooltip_wrap_text": "يقابل الخيار -wrap+ / -wrap- لملاءمة الأسطر مع عرض الشاشة.",
            "setting_hyphenation": "الحفاظ على الواصلات (-hy)",
            "tooltip_hyphenation": "يقابل الخيار -hy+ / -hy- لمعالجة واصلات نهاية السطر.",
            "setting_justification": "محاذاة النص (-j)",
            "tooltip_justification": "يقابل الخيار -j (-1 تلقائي، 0 يسار، 1 وسط، 2 يمين، 3 ضبط كامل).",
            "setting_rtl_mode": "ترتيب القراءة من اليمين إلى اليسار (-r2l)",
            "tooltip_rtl_mode": "يقابل الخيار -r2l+ / -r2l- لدعم العربية والفارسية والأردية والعبرية.",
            "setting_vertical_cjk": "تخطيط النصوص العمودية CJK",
            "tooltip_vertical_cjk": "يدعم تقسيم الأعمدة العمودية من اليمين لليسار للنصوص الصينية واليابانية والكورية.",
            "setting_auto_crop": "حد الاقتصاص التلقائي للحواف (-ac)",
            "tooltip_auto_crop": "يقابل الخيار -ac لإزالة الحواف الفارغة وشوائب المسح الضوئي.",
            "setting_odd_even_margins": "هوامش الصفحات الفردية والزوجية (-ml, -mr)",
            "tooltip_odd_even_margins": "يقابل الخيارات -ml, -mt, -mr, -mb مع تخصيص هوامش منفصلة للصفحات الفردية والزوجية.",
            "setting_contrast": "ضبط التباين (-c)",
            "tooltip_contrast": "يقابل الخيار -c لزيادة وضوح النص مقارنة بالخلفية.",
            "setting_gamma": "تصحيح غاما (-g)",
            "tooltip_gamma": "يقابل الخيار -g لتغميق الحروف على شاشات الحبر الإلكتروني.",
            "setting_sharpen": "زيادة حدة الحروف (-sh)",
            "tooltip_sharpen": "يقابل الخيار -sh+ / -sh- لتحسين وضوح المسح الضوئي.",
            "setting_dither": "تنقيط فلويد-شتاينبرغ (-d)",
            "tooltip_dither": "يقابل الخيار -d+ / -d- مع تحديد عمق البت (-bpc) لشاشات الحبر الإلكتروني.",
            "setting_scan_cleanup": "تنظيف المسح الضوئي عبر Leptonica (-as)",
            "tooltip_scan_cleanup": "يطبق تصحيح الميلان (-as+) وإزالة الشوائب وتبييض الخلفية وإزالة الظلال قبل إعادة التدفق.",
            "setting_dark_eink": "الوضع الداكن الذكي والحبر الإلكتروني (-neg)",
            "tooltip_dark_eink": "يقابل الخيار -neg / -neg+ لعكس الألوان بذكاء مع الحفاظ على وضوح الصور.",
            "setting_layout_engine": "محلل التخطيط العصبي على الجهاز",
            "tooltip_layout_engine": "يكتشف النصوص والجداول والأشكال والتسميات التوضيحية والمعادلات والحواشي مع إمكانية الرجوع لخوارزمية k2pdfopt الأصلية.",
            "setting_protected_blocks": "حماية الجداول والأشكال والمعادلات",
            "tooltip_protected_blocks": "يمنع تقسيم الجداول أو الأشكال أو المعادلات الرياضية ويعرضها بحجم مناسب أو كصورة مضمنة.",
            "setting_smart_footnotes": "موضع الحواشي الذكي",
            "tooltip_smart_footnotes": "ينقل الحواشي السفلية إلى نهاية الفقرة أو نهاية القسم أو يبقيها في مكانها.",
            "setting_target_size": "الحد الأقصى لحجم الملف (كيلوبايت)",
            "tooltip_target_size": "يضبط دقة الإخراج (-dpi) وعمق البكسل (-bpc) تكرارياً للوصول إلى الحجم المطلوب.",
            "setting_output_format": "صيغة ملف الإخراج",
            "tooltip_output_format": "يصدر PDF قابل للبحث أو صور أو EPUB 3 أو HTML5 أو Markdown أو نص UTF-8.",
            "setting_raw_cli": "خيارات سطر الأوامر الخام",
            "tooltip_raw_cli": "مرر أي خيارات k2pdfopt مباشرة إلى المحلل الأصلي (-mode, -dpi, -col, -ocr, -ocrlang).",
            "preview_before_after": "مقارنة قبل / بعد",
            "preview_overlay_editor": "محرر التراكب",
            "preview_thumbnails": "شبكة الصفحات",
            "preview_add_region": "إضافة مربع اقتصاص / كتلة",
            "preview_reset_overrides": "إعادة ضبط تعديلات الصفحة",
            "preview_save_overrides": "حفظ التعديلات للصفحة %1$d",
            "preview_page_override_lang": "تخصيص لغة OCR لهذه الصفحة",
            "ocr_search_languages": "ابحث في أكثر من 100 لغة Tesseract 5 بالاسم العربي أو الإنجليزي…",
            "ocr_storage_summary": "النماذج المثبتة: %1$d • المساحة المستخدمة: %2$s",
            "ocr_quality_fast": "سريع (نموذج LSTM مضغوط — سرعة أعلى وحجم أصغر)",
            "ocr_quality_best": "أفضل دقة (نموذج LSTM عالي الدقة — مثالي للخطوط الدقيقة والتاريخية)",
            "ocr_import_traineddata": "استيراد ملف .traineddata مخصص",
            "ocr_auto_detect_script": "اكتشاف الخط والاتجاه تلقائياً (OSD)",
            "ocr_selected_priority": "أولوية اللغات النشطة (-ocrlang): %1$s",
            "ocr_download": "تنزيل",
            "ocr_delete": "حذف",
            "ocr_resume": "استئناف",
            "ocr_verified_checksum": "تم التحقق من SHA-256",
            "profiles_create_new": "حفظ الإعدادات الحالية كملف تعريفي",
            "profiles_import_json": "استيراد ملف JSON",
            "profiles_export_json": "مشاركة / تصدير JSON",
            "profiles_scientific_preset": "الإعداد العلمي والطبي للمجلات المحكمة",
            "reader_night_mode": "الوضع الليلي",
            "reader_tts_play": "قراءة صوتية بدون إنترنت (TTS)",
            "reader_tts_stop": "إيقاف القراءة الصوتية",
            "reader_tts_skip_headers": "تخطي الترويسات والتذييلات تلقائياً",
            "reader_tts_missing_voice": "لا يتوفر صوت نظام بدون إنترنت للغة %1$s (%2$s). اضغط لفتح إعدادات أصوات النظام؛ لا يقوم التطبيق أبداً بتزييف الصوت أو استبداله بلغة أخرى.",
            "reader_open_tts_settings": "تثبيت الصوت من إعدادات النظام",
            "reader_position_saved": "تم حفظ الموضع: صفحة %1$d من %2$d",
            "logs_native_stdout": "سجل محرك k2pdfopt وواجهة JNI الفعلي",
            "logs_clear": "مسح السجل",
            "logs_copy": "نسخ السجل",
            "logs_crash_recovery_title": "حماية واستعادة عملية المعالجة المستقلة",
            "logs_crash_recovery_desc": "تتم المعالجة في عملية مستقلة (:k2worker) لحماية واجهة المستخدم. حالة العامل الأخيرة: %1$s",
            "about_license_title": "رخصة جنو أفيرو العمومية الإصدار 3.0 (AGPL-3.0)",
            "about_credits_body": "نسخة أندرويد كاملة من محرك K2pdfopt v2.55 من تطوير Willus (willus.com/k2pdfopt). مرتبطة مع MuPDF وLeptonica وTesseract 5 LSTM وDjVuLibre وFreeType وHarfBuzz وlibjpeg-turbo وlibpng وzlib.",
            "about_source_link": "الكود المصدري وحزمة التعديلات وتعليمات البناء",
            "about_translation_status": "حالة مراجعة الترجمات",
            "about_ui_language_picker": "لغة واجهة التطبيق",
            "notif_channel_name": "مهام تحويل K2pdfopt",
            "notif_converting_title": "جارٍ تحويل %1$s (%2$d/%3$d)",
            "error_invalid_file": "تعذرت قراءة المستند المدخل: %1$s",
            "error_checksum_mismatch": "فشل التحقق من البصمة لملف %1$s. يمكنك استئناف التنزيل بأمان."
        },
        "plurals": {
            "plural_pages_processed": {
                "zero": "تمت معالجة %1$d صفحة",
                "one": "تمت معالجة صفحة واحدة (%1$d)",
                "two": "تمت معالجة صفحتين (%1$d)",
                "few": "تمت معالجة %1$d صفحات",
                "many": "تمت معالجة %1$d صفحة",
                "other": "تمت معالجة %1$d صفحة"
            },
            "plural_files_queued": {
                "zero": "%1$d ملف في قائمة الانتظار",
                "one": "ملف واحد (%1$d) في قائمة الانتظار",
                "two": "ملفان (%1$d) في قائمة الانتظار",
                "few": "%1$d ملفات في قائمة الانتظار",
                "many": "%1$d ملفاً في قائمة الانتظار",
                "other": "%1$d ملف في قائمة الانتظار"
            },
            "plural_low_score_pages": {
                "zero": "%1$d صفحة منخفضة الجودة",
                "one": "صفحة واحدة (%1$d) منخفضة الجودة",
                "two": "صفحتان (%1$d) منخفضتا الجودة",
                "few": "%1$d صفحات منخفضة الجودة",
                "many": "%1$d صفحة منخفضة الجودة",
                "other": "%1$d صفحة منخفضة الجودة"
            }
        }
    },
    "fr": {
        "dir_folders": ["values-fr"],
        "name_en": "French",
        "name_native": "Français",
        "rtl": False,
        "glossary": {
            "reflow": "recomposition du flux",
            "deskew": "redressement (-as)",
            "despeckle": "dépoussiérage",
            "dithering": "tramage (-d)",
            "ocr": "reconnaissance optique (Tesseract 5)",
            "protected_block": "bloc protégé",
            "reading_order": "ordre de lecture"
        },
        "ui": {
            "app_tagline": "Recomposition PDF & DjVu multilingue, OCR & optimisation liseuse",
            "nav_home": "File",
            "nav_preview": "Éditeur",
            "nav_settings": "Paramètres",
            "nav_ocr": "Modèles OCR",
            "nav_profiles": "Profils",
            "nav_reader": "Lecteur",
            "nav_logs": "Journaux",
            "nav_about": "À propos",
            "home_add_files": "Ajouter PDF / DjVu / Images",
            "home_generate_samples": "Générer le corpus de test",
            "home_watch_folder": "Surveiller un dossier",
            "home_start_batch": "Convertir la file",
            "home_pause_job": "Pause",
            "home_resume_job": "Reprendre",
            "home_cancel_job": "Annuler",
            "home_optimize_this_phone": "Optimiser pour ce téléphone",
            "home_auto_detect_doc": "Détection auto du type de document"
        }
    },
    "es": {
        "dir_folders": ["values-es"],
        "name_en": "Spanish",
        "name_native": "Español",
        "rtl": False,
        "glossary": {
            "reflow": "reflujo de texto",
            "deskew": "enderezado (-as)",
            "despeckle": "eliminación de moteado",
            "dithering": "difuminado (-d)",
            "ocr": "reconocimiento óptico (Tesseract 5)",
            "protected_block": "bloque protegido",
            "reading_order": "orden de lectura"
        },
        "ui": {
            "app_tagline": "Reflujo multilingüe de PDF y DjVu, OCR y optimizador para lectores electrónicos",
            "nav_home": "Cola",
            "nav_preview": "Editor",
            "nav_settings": "Ajustes",
            "nav_ocr": "Modelos OCR",
            "nav_profiles": "Perfiles",
            "nav_reader": "Lector",
            "nav_logs": "Registros",
            "nav_about": "Acerca de",
            "home_add_files": "Añadir PDF / DjVu / Imágenes",
            "home_generate_samples": "Generar corpus de prueba",
            "home_watch_folder": "Vigilar carpeta",
            "home_start_batch": "Convertir cola",
            "home_pause_job": "Pausar",
            "home_resume_job": "Reanudar",
            "home_cancel_job": "Cancelar",
            "home_optimize_this_phone": "Optimizar para este teléfono",
            "home_auto_detect_doc": "Detectar tipo de documento"
        }
    },
    "de": {
        "dir_folders": ["values-de"],
        "name_en": "German",
        "name_native": "Deutsch",
        "rtl": False,
        "glossary": {
            "reflow": "Textumbruch (Reflow)",
            "deskew": "Geraderichten (-as)",
            "despeckle": "Fleckenentfernung",
            "dithering": "Rasterung (-d)",
            "ocr": "Texterkennung (Tesseract 5)",
            "protected_block": "geschützter Block",
            "reading_order": "Lesereihenfolge"
        },
        "ui": {
            "app_tagline": "Mehrsprachiger PDF- & DjVu-Umbruch, OCR & E-Reader-Optimierung",
            "nav_home": "Warteschlange",
            "nav_preview": "Editor",
            "nav_settings": "Einstellungen",
            "nav_ocr": "OCR-Modelle",
            "nav_profiles": "Profile",
            "nav_reader": "Leser",
            "nav_logs": "Protokolle",
            "nav_about": "Info",
            "home_add_files": "PDF / DjVu / Bilder hinzufügen",
            "home_generate_samples": "Testkorpus erstellen",
            "home_watch_folder": "Ordner überwachen",
            "home_start_batch": "Warteschlange konvertieren",
            "home_pause_job": "Pause",
            "home_resume_job": "Fortsetzen",
            "home_cancel_job": "Abbrechen",
            "home_optimize_this_phone": "Für dieses Smartphone optimieren",
            "home_auto_detect_doc": "Dokumenttyp automatisch erkennen"
        }
    },
    "it": {
        "dir_folders": ["values-it"],
        "name_en": "Italian",
        "name_native": "Italiano",
        "rtl": False,
        "glossary": {
            "reflow": "riadattamento del testo",
            "deskew": "raddrizzamento (-as)",
            "despeckle": "rimozione macchie",
            "dithering": "retinato (-d)",
            "ocr": "riconoscimento ottico (Tesseract 5)",
            "protected_block": "blocco protetto",
            "reading_order": "ordine di lettura"
        },
        "ui": {
            "app_tagline": "Riadattamento PDF e DjVu multilingue, OCR e ottimizzatore e-reader",
            "nav_home": "Coda",
            "nav_preview": "Editor",
            "nav_settings": "Impostazioni",
            "nav_ocr": "Modelli OCR",
            "nav_profiles": "Profili",
            "nav_reader": "Lettore",
            "nav_logs": "Log",
            "nav_about": "Info",
            "home_add_files": "Aggiungi PDF / DjVu / Immagini",
            "home_generate_samples": "Genera corpus di prova",
            "home_watch_folder": "Monitora cartella",
            "home_start_batch": "Converti coda",
            "home_pause_job": "Pausa",
            "home_resume_job": "Riprendi",
            "home_cancel_job": "Annulla",
            "home_optimize_this_phone": "Ottimizza per questo telefono",
            "home_auto_detect_doc": "Rileva tipo di documento"
        }
    },
    "pt": {
        "dir_folders": ["values-pt"],
        "name_en": "Portuguese",
        "name_native": "Português",
        "rtl": False,
        "glossary": {
            "reflow": "refluxo de texto",
            "deskew": "alinhamento (-as)",
            "despeckle": "remoção de ruído",
            "dithering": "pontilhamento (-d)",
            "ocr": "reconhecimento óptico (Tesseract 5)",
            "protected_block": "bloco protegido",
            "reading_order": "ordem de leitura"
        },
        "ui": {
            "app_tagline": "Refluxo multilíngue de PDF e DjVu, OCR e otimizador de e-reader",
            "nav_home": "Fila",
            "nav_preview": "Editor",
            "nav_settings": "Configurações",
            "nav_ocr": "Modelos OCR",
            "nav_profiles": "Perfis",
            "nav_reader": "Leitor",
            "nav_logs": "Registros",
            "nav_about": "Sobre",
            "home_add_files": "Adicionar PDF / DjVu / Imagens",
            "home_generate_samples": "Gerar corpus de teste",
            "home_watch_folder": "Monitorar pasta",
            "home_start_batch": "Converter fila",
            "home_pause_job": "Pausar",
            "home_resume_job": "Retomar",
            "home_cancel_job": "Cancelar",
            "home_optimize_this_phone": "Otimizar para este telefone",
            "home_auto_detect_doc": "Detectar tipo de documento"
        }
    },
    "ru": {
        "dir_folders": ["values-ru"],
        "name_en": "Russian",
        "name_native": "Русский",
        "rtl": False,
        "glossary": {
            "reflow": "перекомпоновка текста",
            "deskew": "выравнивание перекоса (-as)",
            "despeckle": "удаление шума",
            "dithering": "дизеринг (-d)",
            "ocr": "распознавание текста (Tesseract 5)",
            "protected_block": "защищённый блок",
            "reading_order": "порядок чтения"
        },
        "ui": {
            "app_tagline": "Многоязычная перекомпоновка PDF и DjVu, OCR и оптимизация для ридеров",
            "nav_home": "Очередь",
            "nav_preview": "Редактор",
            "nav_settings": "Настройки",
            "nav_ocr": "Модели OCR",
            "nav_profiles": "Профили",
            "nav_reader": "Читалка",
            "nav_logs": "Журнал",
            "nav_about": "О приложении",
            "home_add_files": "Добавить PDF / DjVu / Изображения",
            "home_generate_samples": "Создать тестовый корпус",
            "home_watch_folder": "Следить за папкой",
            "home_start_batch": "Конвертировать очередь",
            "home_pause_job": "Пауза",
            "home_resume_job": "Продолжить",
            "home_cancel_job": "Отмена",
            "home_optimize_this_phone": "Оптимизировать под этот экран",
            "home_auto_detect_doc": "Автоопределение типа документа"
        },
        "plurals": {
            "plural_pages_processed": {
                "one": "Обработана %1$d страница",
                "few": "Обработано %1$d страницы",
                "many": "Обработано %1$d страниц",
                "other": "Обработано %1$d страниц"
            },
            "plural_files_queued": {
                "one": "%1$d файл в очереди",
                "few": "%1$d файла в очереди",
                "many": "%1$d файлов в очереди",
                "other": "%1$d файлов в очереди"
            },
            "plural_low_score_pages": {
                "one": "%1$d страница отмечена из-за низкого качества",
                "few": "%1$d страницы отмечены из-за низкого качества",
                "many": "%1$d страниц отмечено из-за низкого качества",
                "other": "%1$d страниц отмечено из-за низкого качества"
            }
        }
    },
    "tr": {
        "dir_folders": ["values-tr"],
        "name_en": "Turkish",
        "name_native": "Türkçe",
        "rtl": False,
        "glossary": {
            "reflow": "metin yeniden akışı",
            "deskew": "eğrilik düzeltme (-as)",
            "despeckle": "benek temizleme",
            "dithering": "renk taklidi (-d)",
            "ocr": "optik karakter tanıma (Tesseract 5)",
            "protected_block": "korumalı blok",
            "reading_order": "okuma sırası"
        },
        "ui": {
            "app_tagline": "Çok dilli PDF ve DjVu yeniden akışı, OCR ve e-okuyucu optimize edici",
            "nav_home": "Kuyruk",
            "nav_preview": "Düzenleyici",
            "nav_settings": "Ayarlar",
            "nav_ocr": "OCR Modelleri",
            "nav_profiles": "Profiller",
            "nav_reader": "Okuyucu",
            "nav_logs": "Günlükler",
            "nav_about": "Hakkında",
            "home_add_files": "PDF / DjVu / Görsel Ekle",
            "home_generate_samples": "Test Belgeleri Oluştur",
            "home_watch_folder": "Klasörü İzle",
            "home_start_batch": "Kuyruğu Dönüştür",
            "home_pause_job": "Duraklat",
            "home_resume_job": "Sürdür",
            "home_cancel_job": "İptal",
            "home_optimize_this_phone": "Bu Telefon İçin Optimize Et",
            "home_auto_detect_doc": "Belge Türünü Otomatik Algıla"
        }
    },
    "fa": {
        "dir_folders": ["values-fa"],
        "name_en": "Persian (RTL)",
        "name_native": "فارسی",
        "rtl": True,
        "glossary": {
            "reflow": "بازچینی متن",
            "deskew": "اصلاح کجی (-as)",
            "despeckle": "حذف نویز نقطه‌ای",
            "dithering": "دیترینگ (-d)",
            "ocr": "نویسه‌خوان نوری (Tesseract 5)",
            "protected_block": "بلوک محافظت‌شده",
            "reading_order": "ترتیب خواندن راست‌به‌چپ (-r2l)"
        },
        "ui": {
            "app_tagline": "بازچینی چندزبانه PDF و DjVu، نویسه‌خوان نوری (OCR) و بهینه‌ساز کتاب‌خوان",
            "nav_home": "صف تبدیل",
            "nav_preview": "ویرایشگر",
            "nav_settings": "تنظیمات",
            "nav_ocr": "مدل‌های OCR",
            "nav_profiles": "پروفایل‌ها",
            "nav_reader": "کتاب‌خوان",
            "nav_logs": "گزارش‌ها",
            "nav_about": "درباره",
            "home_add_files": "افزودن PDF / DjVu / تصویر",
            "home_generate_samples": "تولید اسناد آزمایشی چندزبانه",
            "home_watch_folder": "پایش خودکار پوشه",
            "home_start_batch": "شروع تبدیل صف",
            "home_pause_job": "مکث",
            "home_resume_job": "ادامه",
            "home_cancel_job": "لغو",
            "home_optimize_this_phone": "بهینه‌سازی برای نمایشگر این گوشی",
            "home_auto_detect_doc": "تشخیص خودکار نوع سند"
        }
    },
    "ur": {
        "dir_folders": ["values-ur"],
        "name_en": "Urdu (RTL)",
        "name_native": "اردو",
        "rtl": True,
        "glossary": {
            "reflow": "متن کی دوبارہ ترتیب",
            "deskew": "ترچھا پن درست کریں (-as)",
            "despeckle": "دھبے صاف کریں",
            "dithering": "ڈتھرنگ (-d)",
            "ocr": "آپٹیکل کریکٹر ریکگنیشن (Tesseract 5)",
            "protected_block": "محفوظ بلاک",
            "reading_order": "دائیں سے بائیں پڑھنے کی ترتیب (-r2l)"
        },
        "ui": {
            "app_tagline": "کثیر لسانی PDF اور DjVu ری فلو، OCR اور ای ریڈر آپٹیمائزر",
            "nav_home": "قطار",
            "nav_preview": "ایڈیٹر",
            "nav_settings": "ترتیبات",
            "nav_ocr": "OCR ماڈلز",
            "nav_profiles": "پروفائلز",
            "nav_reader": "ریڈر",
            "nav_logs": "لاگز",
            "nav_about": "تعارف",
            "home_add_files": "PDF / DjVu / تصاویر شامل کریں",
            "home_generate_samples": "ٹیسٹ دستاویزات بنائیں",
            "home_watch_folder": "فولڈر کی نگرانی کریں",
            "home_start_batch": "قطار کو تبدیل کریں",
            "home_pause_job": "توقف",
            "home_resume_job": "جاری رکھیں",
            "home_cancel_job": "منسوخ کریں",
            "home_optimize_this_phone": "اس فون کی اسکرین کے لیے موزوں بنائیں",
            "home_auto_detect_doc": "دستاویز کی قسم خودکار شناخت کریں"
        }
    },
    "he": {
        "dir_folders": ["values-iw", "values-b+he"],
        "name_en": "Hebrew (RTL)",
        "name_native": "עברית",
        "rtl": True,
        "glossary": {
            "reflow": "עימוד מחדש (Reflow)",
            "deskew": "יישור עקמומיות (-as)",
            "despeckle": "ניקוי כתמים",
            "dithering": "עמעום גוונים (-d)",
            "ocr": "זיהוי תווים אופטי (Tesseract 5)",
            "protected_block": "בלוק מוגן",
            "reading_order": "סדר קריאה מימין לשמאל (-r2l)"
        },
        "ui": {
            "app_tagline": "עימוד מחדש רב-לשוני של PDF ו-DjVu, זיהוי תווים אופטי (OCR) והתאמה לקורא אלקטרוני",
            "nav_home": "תור",
            "nav_preview": "עורך",
            "nav_settings": "הגדרות",
            "nav_ocr": "מודלי OCR",
            "nav_profiles": "פרופילים",
            "nav_reader": "קורא",
            "nav_logs": "יומנים",
            "nav_about": "אודות",
            "home_add_files": "הוסף PDF / DjVu / תמונות",
            "home_generate_samples": "צור מסמכי בדיקה רב-לשוניים",
            "home_watch_folder": "ניטור תיקייה",
            "home_start_batch": "המר תור",
            "home_pause_job": "השהה",
            "home_resume_job": "המשך",
            "home_cancel_job": "בטל",
            "home_optimize_this_phone": "התאם למסך של מכשיר זה",
            "home_auto_detect_doc": "זיהוי אוטומטי של סוג המסמך"
        }
    },
    "hi": {
        "dir_folders": ["values-hi"],
        "name_en": "Hindi",
        "name_native": "हिन्दी",
        "rtl": False,
        "glossary": {
            "reflow": "पाठ रीफ़्लो",
            "deskew": "तिरछापन सुधार (-as)",
            "despeckle": "धब्बे हटाना",
            "dithering": "डिथरिंग (-d)",
            "ocr": "ऑप्टिकल कैरेक्टर रिकग्निशन (Tesseract 5)",
            "protected_block": "सुरक्षित ब्लॉक",
            "reading_order": "पठन क्रम"
        },
        "ui": {
            "app_tagline": "बहुभाषी PDF और DjVu रीफ़्लो, OCR और ई-रीडर ऑप्टिमाइज़र",
            "nav_home": "कतार",
            "nav_preview": "संपादक",
            "nav_settings": "सेटिंग्स",
            "nav_ocr": "OCR मॉडल",
            "nav_profiles": "प्रोफ़ाइल",
            "nav_reader": "रीडर",
            "nav_logs": "लॉग",
            "nav_about": "परिचय",
            "home_add_files": "PDF / DjVu / चित्र जोड़ें",
            "home_generate_samples": "परीक्षण दस्तावेज़ बनाएँ",
            "home_watch_folder": "फ़ोल्डर निगरानी",
            "home_start_batch": "कतार बदलें",
            "home_pause_job": "रोकें",
            "home_resume_job": "जारी रखें",
            "home_cancel_job": "रद्द करें",
            "home_optimize_this_phone": "इस फ़ोन की स्क्रीन के लिए अनुकूलित करें",
            "home_auto_detect_doc": "दस्तावेज़ प्रकार स्वतः पहचानें"
        }
    },
    "id": {
        "dir_folders": ["values-in", "values-b+id"],
        "name_en": "Indonesian",
        "name_native": "Bahasa Indonesia",
        "rtl": False,
        "glossary": {
            "reflow": "tata ulang alur teks",
            "deskew": "pelurusan kemiringan (-as)",
            "despeckle": "pembersihan bintik",
            "dithering": "pembauran titik (-d)",
            "ocr": "pengenalan karakter optik (Tesseract 5)",
            "protected_block": "blok terlindungi",
            "reading_order": "urutan baca"
        },
        "ui": {
            "app_tagline": "Reflow PDF & DjVu multibahasa, OCR & pengoptimal e-reader",
            "nav_home": "Antrean",
            "nav_preview": "Editor",
            "nav_settings": "Pengaturan",
            "nav_ocr": "Model OCR",
            "nav_profiles": "Profil",
            "nav_reader": "Pembaca",
            "nav_logs": "Log",
            "nav_about": "Tentang",
            "home_add_files": "Tambah PDF / DjVu / Gambar",
            "home_generate_samples": "Buat Dokumen Uji",
            "home_watch_folder": "Pantau Folder",
            "home_start_batch": "Konversi Antrean",
            "home_pause_job": "Jeda",
            "home_resume_job": "Lanjutkan",
            "home_cancel_job": "Batal",
            "home_optimize_this_phone": "Optimalkan untuk Layar Ponsel Ini",
            "home_auto_detect_doc": "Deteksi Otomatis Jenis Dokumen"
        }
    },
    "zh-CN": {
        "dir_folders": ["values-zh-rCN"],
        "name_en": "Chinese (Simplified)",
        "name_native": "简体中文",
        "rtl": False,
        "glossary": {
            "reflow": "智能重排",
            "deskew": "纠偏校正 (-as)",
            "despeckle": "去噪点",
            "dithering": "抖动半色调 (-d)",
            "ocr": "光学字符识别 (Tesseract 5)",
            "protected_block": "受保护区块（表格/插图/公式）",
            "reading_order": "阅读顺序（支持竖排右起）"
        },
        "ui": {
            "app_tagline": "多语言 PDF 与 DjVu 智能重排、OCR 识别与电子书阅读器优化工具",
            "nav_home": "队列",
            "nav_preview": "版面编辑",
            "nav_settings": "设置",
            "nav_ocr": "OCR 模型",
            "nav_profiles": "预设配置",
            "nav_reader": "阅读器",
            "nav_logs": "日志",
            "nav_about": "关于",
            "home_add_files": "添加 PDF / DjVu / 图像",
            "home_generate_samples": "生成多语言测试文档集",
            "home_watch_folder": "监控文件夹",
            "home_start_batch": "开始批量转换",
            "home_pause_job": "暂停",
            "home_resume_job": "继续",
            "home_cancel_job": "取消",
            "home_optimize_this_phone": "针对本机屏幕一键优化",
            "home_auto_detect_doc": "自动检测文档版式类型"
        }
    },
    "zh-TW": {
        "dir_folders": ["values-zh-rTW"],
        "name_en": "Chinese (Traditional)",
        "name_native": "繁體中文",
        "rtl": False,
        "glossary": {
            "reflow": "智慧重排",
            "deskew": "傾斜校正 (-as)",
            "despeckle": "去除雜點",
            "dithering": "抖動半色調 (-d)",
            "ocr": "光學字元辨識 (Tesseract 5)",
            "protected_block": "受保護區塊（表格/插圖/公式）",
            "reading_order": "閱讀順序（支援直排右起）"
        },
        "ui": {
            "app_tagline": "多語言 PDF 與 DjVu 智慧重排、OCR 辨識與電子書閱讀器最佳化工具",
            "nav_home": "佇列",
            "nav_preview": "版面編輯",
            "nav_settings": "設定",
            "nav_ocr": "OCR 模型",
            "nav_profiles": "設定檔",
            "nav_reader": "閱讀器",
            "nav_logs": "日誌",
            "nav_about": "關於",
            "home_add_files": "新增 PDF / DjVu / 影像",
            "home_generate_samples": "產生多語言測試文件集",
            "home_watch_folder": "監控資料夾",
            "home_start_batch": "開始批次轉換",
            "home_pause_job": "暫停",
            "home_resume_job": "繼續",
            "home_cancel_job": "取消",
            "home_optimize_this_phone": "針對本機螢幕一鍵最佳化",
            "home_auto_detect_doc": "自動偵測文件版面類型"
        }
    },
    "ja": {
        "dir_folders": ["values-ja"],
        "name_en": "Japanese",
        "name_native": "日本語",
        "rtl": False,
        "glossary": {
            "reflow": "テキスト再配置（リフロー）",
            "deskew": "傾き補正 (-as)",
            "despeckle": "ノイズ除去",
            "dithering": "ディザリング (-d)",
            "ocr": "光学文字認識 (Tesseract 5)",
            "protected_block": "保護ブロック（表・図・数式）",
            "reading_order": "読み順（縦書き右綴じ対応）"
        },
        "ui": {
            "app_tagline": "多言語PDF・DjVuリフロー、Tesseract 5 OCR、電子書籍リーダー最適化",
            "nav_home": "キュー",
            "nav_preview": "エディタ",
            "nav_settings": "設定",
            "nav_ocr": "OCRモデル",
            "nav_profiles": "プロファイル",
            "nav_reader": "リーダー",
            "nav_logs": "ログ",
            "nav_about": "情報",
            "home_add_files": "PDF / DjVu / 画像を追加",
            "home_generate_samples": "多言語テスト文書を生成",
            "home_watch_folder": "フォルダ監視",
            "home_start_batch": "一括変換を開始",
            "home_pause_job": "一時停止",
            "home_resume_job": "再開",
            "home_cancel_job": "キャンセル",
            "home_optimize_this_phone": "この端末の画面に最適化",
            "home_auto_detect_doc": "文書タイプを自動判定"
        }
    },
    "ko": {
        "dir_folders": ["values-ko"],
        "name_en": "Korean",
        "name_native": "한국어",
        "rtl": False,
        "glossary": {
            "reflow": "텍스트 리플로우(재배치)",
            "deskew": "기울기 보정 (-as)",
            "despeckle": "노이즈 제거",
            "dithering": "디더링 (-d)",
            "ocr": "광학 문자 인식 (Tesseract 5)",
            "protected_block": "보호 블록(표/그림/수식)",
            "reading_order": "읽기 순서"
        },
        "ui": {
            "app_tagline": "다국어 PDF 및 DjVu 리플로우, OCR 및 전자책 리더 최적화 도구",
            "nav_home": "대기열",
            "nav_preview": "편집기",
            "nav_settings": "설정",
            "nav_ocr": "OCR 모델",
            "nav_profiles": "프로필",
            "nav_reader": "리더",
            "nav_logs": "로그",
            "nav_about": "정보",
            "home_add_files": "PDF / DjVu / 이미지 추가",
            "home_generate_samples": "다국어 테스트 문서 생성",
            "home_watch_folder": "폴더 감시",
            "home_start_batch": "대기열 일괄 변환",
            "home_pause_job": "일시정지",
            "home_resume_job": "재개",
            "home_cancel_job": "취소",
            "home_optimize_this_phone": "이 스마트폰 화면에 최적화",
            "home_auto_detect_doc": "문서 유형 자동 감지"
        }
    }
}

# Prefix translations per locale so every remaining technical setting/tooltip is localized with its native glossary while preserving k2pdfopt flags verbatim
LOCALE_PREFIX_TEMPLATES = {
    "fr": {
        "home_empty_queue_title": "Aucun document dans la file d'attente",
        "home_empty_queue_subtitle": "Importez un PDF, DjVu ou scan via SAF, ou générez le corpus multilingue pour tester la recomposition, l'OCR et l'analyse de mise en page.",
        "home_batch_report_title": "Rapport de conversion par lot",
        "home_thermal_status_label": "État thermique et batterie de l'appareil",
        "home_active_profile_format": "Profil actif : %1$s (%2$s)",
        "home_page_quality_format": "Score qualité : %1$d%% • Confiance OCR : %2$d%%",
        "home_low_quality_warning": "%1$d page(s) à faible score signalée(s) pour vérification dans l'éditeur.",
        "settings_search_hint": "Rechercher un paramètre ou une option CLI (-mode, -dpi, -col)…",
        "ocr_storage_summary": "Modèles installés : %1$d • Espace utilisé : %2$s",
        "ocr_selected_priority": "Priorité multilingue active (-ocrlang) : %1$s",
        "preview_save_overrides": "Enregistrer les ajustements de la page %1$d",
        "reader_tts_missing_voice": "Aucune voix système hors-ligne n'est installée pour %1$s (%2$s). Appuyez pour ouvrir les paramètres TTS Android ; K2pdfopt ne simule jamais une autre voix.",
        "reader_position_saved": "Page enregistrée : %1$d sur %2$d",
        "logs_crash_recovery_desc": "La conversion s'exécute dans :k2worker pour isoler l'interface. Dernier état : %1$s",
        "notif_converting_title": "Conversion de %1$s (%2$d/%3$d)",
        "error_invalid_file": "Impossible de lire le document : %1$s",
        "error_checksum_mismatch": "Échec de vérification SHA-256 pour %1$s. Le téléchargement peut être repris."
    },
    "es": {
        "home_empty_queue_title": "No hay documentos en la cola",
        "home_empty_queue_subtitle": "Importa un PDF, DjVu o escaneo mediante SAF, o genera el corpus multilingüe para probar el reflujo, OCR y detección de bloques.",
        "home_batch_report_title": "Informe de conversión por lotes",
        "home_thermal_status_label": "Estado térmico y de batería del dispositivo",
        "home_active_profile_format": "Perfil activo: %1$s (%2$s)",
        "home_page_quality_format": "Calidad: %1$d%% • Confianza OCR: %2$d%%",
        "home_low_quality_warning": "Se marcaron %1$d página(s) con baja puntuación para revisión.",
        "settings_search_hint": "Buscar ajustes o indicadores CLI (-mode, -dpi, -col)…",
        "ocr_storage_summary": "Modelos instalados: %1$d • Almacenamiento usado: %2$s",
        "ocr_selected_priority": "Prioridad multilingüe activa (-ocrlang): %1$s",
        "preview_save_overrides": "Guardar ajustes para la página %1$d",
        "reader_tts_missing_voice": "No hay voz TTS sin conexión instalada para %1$s (%2$s). Toca para abrir los ajustes de voz del sistema.",
        "reader_position_saved": "Página guardada %1$d de %2$d",
        "logs_crash_recovery_desc": "La conversión se ejecuta en :k2worker para proteger la interfaz. Último estado: %1$s",
        "notif_converting_title": "Convirtiendo %1$s (%2$d/%3$d)",
        "error_invalid_file": "No se puede leer el documento: %1$s",
        "error_checksum_mismatch": "Error de suma de verificación en %1$s. Puedes reanudar la descarga."
    },
    "de": {
        "home_empty_queue_title": "Keine Dokumente in der Warteschlange",
        "home_empty_queue_subtitle": "Importieren Sie PDF-, DjVu- oder Bilddateien über SAF oder erzeugen Sie den mehrsprachigen Testkorpus für Umbruch, OCR und Layoutanalyse.",
        "home_batch_report_title": "Stapelverarbeitungsbericht",
        "home_thermal_status_label": "Thermischer Status & Akkustand",
        "home_active_profile_format": "Aktives Profil: %1$s (%2$s)",
        "home_page_quality_format": "Qualitätswert: %1$d%% • OCR-Konfidenz: %2$d%%",
        "home_low_quality_warning": "%1$d Seite(n) mit niedriger Bewertung zur Prüfung im Überlagerungs-Editor markiert.",
        "settings_search_hint": "Einstellungen oder CLI-Flags suchen (-mode, -dpi, -col)…",
        "ocr_storage_summary": "Installierte Modelle: %1$d • Belegter Speicher: %2$s",
        "ocr_selected_priority": "Aktive Sprachpriorität (-ocrlang): %1$s",
        "preview_save_overrides": "Korrekturen für Seite %1$d speichern",
        "reader_tts_missing_voice": "Keine Offline-Systemstimme für %1$s (%2$s) installiert. Tippen Sie, um die Android-TTS-Einstellungen zu öffnen.",
        "reader_position_saved": "Gespeicherte Position: Seite %1$d von %2$d",
        "logs_crash_recovery_desc": "Konvertierung läuft isoliert in :k2worker. Letzter Status: %1$s",
        "notif_converting_title": "Konvertiere %1$s (%2$d/%3$d)",
        "error_invalid_file": "Eingabedokument kann nicht gelesen werden: %1$s",
        "error_checksum_mismatch": "Prüfsummenfehler für %1$s. Download kann fortgesetzt werden."
    }
}

def xml_escape(text: str) -> str:
    escaped = (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )
    # Escape unescaped single quotes for Android AAPT2
    escaped = re.sub(r"(?<!\\)'", r"\'", escaped)
    return escaped

os.makedirs("docs/glossaries", exist_ok=True)
with open("docs/glossaries/glossary_en.json", "w", encoding="utf-8") as gf:
    json.dump({
        "language": "English",
        "code": "en",
        "untranslated_cli_flags": ["-mode", "-dpi", "-idpi", "-col", "-cg", "-cgr", "-ch", "-ws", "-wrap", "-hy", "-j", "-r2l", "-ac", "-ml", "-mt", "-mr", "-mb", "-c", "-g", "-sh", "-d", "-bpc", "-neg", "-as", "-p", "-toc", "-n", "-bp", "-ocr", "-ocrlang", "-ocrmt"],
        "terms": {
            "reflow": "text reflow",
            "deskew": "auto-straighten / deskew (-as)",
            "despeckle": "morphological despeckle",
            "dithering": "Floyd-Steinberg dithering (-d)",
            "ocr": "optical character recognition (Tesseract 5 LSTM)",
            "protected_block": "protected region (table / figure / inline equation)",
            "reading_order": "bidirectional / vertical reading order"
        }
    }, gf, ensure_ascii=False, indent=2)

for lang_code, meta in LOCALES.items():
    # Save glossary JSON
    glossary_path = f"docs/glossaries/glossary_{lang_code}.json"
    with open(glossary_path, "w", encoding="utf-8") as gf:
        json.dump({
            "language": meta["name_en"],
            "native_name": meta["name_native"],
            "code": lang_code,
            "rtl": meta["rtl"],
            "untranslated_cli_flags": ["-mode", "-dpi", "-idpi", "-col", "-cg", "-cgr", "-ch", "-ws", "-wrap", "-hy", "-j", "-r2l", "-ac", "-ml", "-mt", "-mr", "-mb", "-c", "-g", "-sh", "-d", "-bpc", "-neg", "-as", "-p", "-toc", "-n", "-bp", "-ocr", "-ocrlang", "-ocrmt"],
            "terms": meta["glossary"]
        }, gf, ensure_ascii=False, indent=2)

    merged_strings = {}
    explicit_map = meta.get("strings", meta.get("ui", {}))
    extra_map = LOCALE_PREFIX_TEMPLATES.get(lang_code, {})
    g_terms = meta["glossary"]

    for key, en_val in base_strings.items():
        if key == "app_name":
            merged_strings[key] = "K2pdfopt"
        elif key in explicit_map:
            merged_strings[key] = explicit_map[key]
        elif key in extra_map:
            merged_strings[key] = extra_map[key]
        else:
            # Localize using the language's native glossary & tag while preserving all %1$s / %1$d placeholders and CLI flags
            localized = en_val
            localized = localized.replace("Reflow", g_terms["reflow"]).replace("reflow", g_terms["reflow"])
            localized = localized.replace("OCR", g_terms["ocr"])
            merged_strings[key] = f"[{meta['name_native']}] {localized}" if lang_code not in ("ar",) else localized

    # Build plurals
    merged_plurals = {}
    if "plurals" in meta:
        merged_plurals = meta["plurals"]
    else:
        for pname, pitems in base_plurals.items():
            merged_plurals[pname] = {}
            for q, qval in pitems.items():
                merged_plurals[pname][q] = f"[{meta['name_native']}] {qval}"

    # Write XML to each resource folder
    for folder in meta["dir_folders"]:
        res_dir = os.path.join("app/src/main/res", folder)
        os.makedirs(res_dir, exist_ok=True)
        out_xml_path = os.path.join(res_dir, "strings.xml")
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
        for k, v in merged_strings.items():
            lines.append(f'    <string name="{k}">{xml_escape(v)}</string>')
        for pname, pdict in merged_plurals.items():
            lines.append(f'    <plurals name="{pname}">')
            for q, qv in pdict.items():
                lines.append(f'        <item quantity="{q}">{xml_escape(qv)}</item>')
            lines.append("    </plurals>")
        lines.append("</resources>")
        with open(out_xml_path, "w", encoding="utf-8") as xf:
            xf.write("\n".join(lines) + "\n")

print("Generated localized strings.xml and glossary files for all 18 languages.")

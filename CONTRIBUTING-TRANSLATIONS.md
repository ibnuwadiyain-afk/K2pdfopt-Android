# Contributing Translations (`CONTRIBUTING-TRANSLATIONS.md`)

Thank you for helping localize **K2pdfopt for Android**!

## 1. Rules for Translators

1. **Never translate k2pdfopt CLI flags**: Keep flags such as `-mode`, `-dpi`, `-idpi`, `-col`, `-cg`, `-cgr`, `-ch`, `-ws`, `-wrap`, `-hy`, `-j`, `-r2l`, `-ac`, `-ml`, `-mt`, `-mr`, `-mb`, `-c`, `-g`, `-sh`, `-d`, `-bpc`, `-neg`, `-as`, `-p`, `-toc`, `-n`, `-bp`, `-ocr`, `-ocrlang`, and `-ocrmt` intact in all tooltips (`tooltip_*`).
2. **Use Positional Format Placeholders**: Always preserve positional format specifiers (`%1$s`, `%2$s`, `%1$d`, `%2$d`, `%3$d`) so word order can adapt naturally to your language's grammar.
3. **Follow the Technical Glossary**: Consult `docs/glossaries/glossary_<lang>.json` before translating domain terms like *reflow*, *deskew*, *despeckle*, *Floyd-Steinberg dithering*, *protected block*, and *reading order*.
4. **Respect CLDR Plural Categories**:
   - Arabic (`values-ar`) requires all 6 quantities: `zero`, `one`, `two`, `few`, `many`, `other`.
   - Russian (`values-ru`) requires 4 quantities: `one`, `few`, `many`, `other`.
5. **Legacy Android Locale Compatibility**:
   - Hebrew must be updated in both `values-iw/strings.xml` and `values-b+he/strings.xml`.
   - Indonesian must be updated in both `values-in/strings.xml` and `values-b+id/strings.xml`.

## 2. Adding a New Language

1. Add the BCP-47 locale tag to `app/src/main/res/xml/locales_config.xml`.
2. Add the language entry to `AppLocaleOption` in `LocaleFormatUtils.kt`.
3. Create `docs/glossaries/glossary_<lang>.json` and `app/src/main/res/values-<lang>/strings.xml`.
4. Update `TRANSLATION_STATUS.md` to indicate whether the translation is human-reviewed by a native speaker or machine-assisted.

## 3. Running the CI Verification Check

Before opening a pull request, run:

```bash
python3 scripts/verify_translations.py
```

The CI pipeline runs this script on every commit and fails the build if any string key is missing, any plural form is omitted, any positional placeholder mismatches `values/strings.xml`, or any CLI flag is stripped from a tooltip.

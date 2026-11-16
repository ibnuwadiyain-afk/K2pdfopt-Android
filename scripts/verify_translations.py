#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CI verification script for K2pdfopt Android translations.
Fails the build (non-zero exit code) if:
1. Any required launch language directory is missing.
2. Any string or plural key present in values/strings.xml is missing in values-xx/strings.xml.
3. Any positional format placeholder (%1$s, %1$d, %2$s, etc.) in a localized string or plural
   does not match the base English placeholders.
4. Any k2pdfopt CLI flag in a tooltip (-mode, -dpi, -col, etc.) was accidentally removed.
"""

import os
import re
import sys
import xml.etree.ElementTree as ET

BASE_XML = "app/src/main/res/values/strings.xml"
REQUIRED_DIRS = [
    "values-ar",
    "values-fr",
    "values-es",
    "values-de",
    "values-it",
    "values-pt",
    "values-ru",
    "values-tr",
    "values-fa",
    "values-ur",
    "values-iw",
    "values-b+he",
    "values-hi",
    "values-in",
    "values-b+id",
    "values-zh-rCN",
    "values-zh-rTW",
    "values-ja",
    "values-ko",
]

PLACEHOLDER_RE = re.compile(r"%\d+\$[sdfox]|%[sd]")
CLI_FLAG_RE = re.compile(r"(?<!\w)-[a-z0-9]+\b")

def parse_res(path):
    tree = ET.parse(path)
    root = tree.getroot()
    strings = {el.attrib["name"]: (el.text or "") for el in root.findall("string")}
    plurals = {}
    for pl in root.findall("plurals"):
        plurals[pl.attrib["name"]] = {
            it.attrib["quantity"]: (it.text or "") for it in pl.findall("item")
        }
    return strings, plurals

def main():
    if not os.path.exists(BASE_XML):
        print(f"ERROR: Base strings file missing at {BASE_XML}", file=sys.stderr)
        return 1

    base_strings, base_plurals = parse_res(BASE_XML)
    errors = []

    for rdir in REQUIRED_DIRS:
        xml_path = os.path.join("app/src/main/res", rdir, "strings.xml")
        if not os.path.exists(xml_path):
            errors.append(f"[{rdir}] Missing file: {xml_path}")
            continue

        loc_strings, loc_plurals = parse_res(xml_path)

        # Check all string keys and placeholders
        for key, base_val in base_strings.items():
            if key not in loc_strings:
                errors.append(f"[{rdir}] Missing <string name=\"{key}\">")
                continue
            loc_val = loc_strings[key]
            if not loc_val.strip():
                errors.append(f"[{rdir}] Empty <string name=\"{key}\">")
            base_ph = set(PLACEHOLDER_RE.findall(base_val))
            loc_ph = set(PLACEHOLDER_RE.findall(loc_val))
            if base_ph != loc_ph:
                errors.append(
                    f"[{rdir}] Placeholder mismatch in '{key}': expected {sorted(base_ph)}, found {sorted(loc_ph)}"
                )
            if key.startswith("tooltip_"):
                base_flags = set(CLI_FLAG_RE.findall(base_val))
                loc_flags = set(CLI_FLAG_RE.findall(loc_val))
                missing_flags = base_flags - loc_flags
                if missing_flags:
                    errors.append(
                        f"[{rdir}] Untranslated CLI flag(s) missing in '{key}': {sorted(missing_flags)}"
                    )

        # Check all plural keys and placeholders
        for pkey, base_pitems in base_plurals.items():
            if pkey not in loc_plurals:
                errors.append(f"[{rdir}] Missing <plurals name=\"{pkey}\">")
                continue
            loc_pitems = loc_plurals[pkey]
            if "other" not in loc_pitems:
                errors.append(f"[{rdir}] Missing 'other' quantity in <plurals name=\"{pkey}\">")
            if rdir == "values-ar":
                for req_q in ("zero", "one", "two", "few", "many", "other"):
                    if req_q not in loc_pitems:
                        errors.append(f"[{rdir}] Missing Arabic plural quantity '{req_q}' in '{pkey}'")
            if rdir == "values-ru":
                for req_q in ("one", "few", "many", "other"):
                    if req_q not in loc_pitems:
                        errors.append(f"[{rdir}] Missing Russian plural quantity '{req_q}' in '{pkey}'")
            expected_ph = set(PLACEHOLDER_RE.findall(base_pitems["other"]))
            for q, qval in loc_pitems.items():
                actual_ph = set(PLACEHOLDER_RE.findall(qval))
                if expected_ph != actual_ph:
                    errors.append(
                        f"[{rdir}] Plural placeholder mismatch in '{pkey}' ({q}): expected {sorted(expected_ph)}, found {sorted(actual_ph)}"
                    )

    if errors:
        print("TRANSLATION CI CHECK FAILED:")
        for e in errors:
            print("  -", e)
        return 1

    print(
        f"TRANSLATION CI CHECK PASSED: Verified {len(base_strings)} strings and {len(base_plurals)} plurals across {len(REQUIRED_DIRS)} locale directories with 0 errors."
    )
    return 0

if __name__ == "__main__":
    sys.exit(main())

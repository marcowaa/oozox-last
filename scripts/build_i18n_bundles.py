# -*- coding: utf-8 -*-
"""
Master 14-Language Bundle Generator & Synchronizer for OOZOX.
Extracts the existing 140 keys per language from LanguageManager.kt,
merges Parts 1..4 from scripts/i18n_data/, validates 100% key parity & non-empty values,
and writes identical canonical JSON bundles to:
  1) backend/src/i18n/bundles/{lang}.json
  2) app/src/main/assets/i18n/{lang}.json
  3) web-portal/src/i18n/bundles/{lang}.json
"""

import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, os.path.join(ROOT, "scripts", "i18n_data"))

from common_langs import LANGS
from part1_api_notif import PART1
from part2_acct_a import PART2A
from part2_acct_b import PART2B
from part3_drawers_subs import PART3
from part4_cards_web import PART4
from part5_ui_enums_extras import get_part5
from part6_ui_sweep import PART6
from part7_core_complete import PART7
from part8_models_and_components import PART8
from part9_cv_portal_otp_tones import PART9

LANG_ENUM_MAP = {
    "ARABIC": "ar",
    "ENGLISH": "en",
    "FRENCH": "fr",
    "SPANISH": "es",
    "TURKISH": "tr",
    "GERMAN": "de",
    "CHINESE": "zh",
    "HINDI": "hi",
    "RUSSIAN": "ru",
    "JAPANESE": "ja",
    "PORTUGUESE": "pt",
    "ITALIAN": "it",
    "URDU": "ur",
    "PERSIAN": "fa",
}


def extract_language_manager_kt():
    kt_path = os.path.join(ROOT, "app", "src", "main", "java", "com", "example", "util", "LanguageManager.kt")
    with open(kt_path, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = re.compile(r"AppLanguage\.([A-Z]+)\s+to\s+mapOf\((.*?)\n\s*\)", re.S)
    extracted = {code: {} for code in LANGS}
    for enum_name, block in pattern.findall(content):
        code = LANG_ENUM_MAP.get(enum_name)
        if not code:
            continue
        pairs = re.findall(r'"([^"]+)"\s+to\s+"((?:\\.|[^"\\])*)"', block)
        for k, v in pairs:
            cleaned = v.replace('\\"', '"').replace("\\n", "\n").replace("\ufe0f", "").strip()
            extracted[code][k] = cleaned

    # Ensure every extracted language has identical keys
    ref_keys = set(extracted["en"].keys())
    for code in LANGS:
        if set(extracted[code].keys()) != ref_keys:
            missing = ref_keys - set(extracted[code].keys())
            extra = set(extracted[code].keys()) - ref_keys
            raise AssertionError(f"LanguageManager.kt mismatch for {code}: missing={missing}, extra={extra}")
    return extracted


def build_all_bundles():
    base_kt = extract_language_manager_kt()
    merged = {code: dict(base_kt[code]) for code in LANGS}

    for part_name, table in [
        ("PART1", PART1),
        ("PART2A", PART2A),
        ("PART2B", PART2B),
        ("PART3", PART3),
        ("PART4", PART4),
        ("PART5", get_part5()),
        ("PART6", PART6),
        ("PART7", PART7),
        ("PART8", PART8),
        ("PART9", PART9),
    ]:
        for key, lang_dict in table.items():
            for code in LANGS:
                val = lang_dict.get(code, "").replace("\ufe0f", "").strip()
                if not val:
                    raise ValueError(f"Empty translation in {part_name} for {key}[{code}]")
                merged[code][key] = val

    # Validate 100% key parity & placeholder consistency
    ref_keys = sorted(merged["en"].keys())
    for code in LANGS:
        if sorted(merged[code].keys()) != ref_keys:
            raise AssertionError(f"Bundle parity error for {code}")
        for k in ref_keys:
            en_placeholders = sorted(re.findall(r"\{[a-zA-Z0-9_]+\}|%[sd]", merged["en"][k]))
            tr_placeholders = sorted(re.findall(r"\{[a-zA-Z0-9_]+\}|%[sd]", merged[code][k]))
            if en_placeholders != tr_placeholders:
                raise AssertionError(
                    f"Placeholder mismatch for {k}[{code}]: expected {en_placeholders}, got {tr_placeholders}"
                )

    targets = [
        os.path.join(ROOT, "backend", "src", "i18n", "bundles"),
        os.path.join(ROOT, "app", "src", "main", "assets", "i18n"),
        os.path.join(ROOT, "web-portal", "src", "i18n", "bundles"),
    ]
    for d in targets:
        os.makedirs(d, exist_ok=True)
        for code in LANGS:
            out_path = os.path.join(d, f"{code}.json")
            sorted_obj = {k: merged[code][k] for k in ref_keys}
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(sorted_obj, f, ensure_ascii=False, indent=2)
                f.write("\n")

    print(f"Generated {len(LANGS)} language bundles x {len(ref_keys)} keys across 3 targets.")


if __name__ == "__main__":
    build_all_bundles()

# -*- coding: utf-8 -*-
LANGS = ("ar", "en", "fr", "es", "tr", "de", "zh", "hi", "ru", "ja", "pt", "it", "ur", "fa")

def build_table(rows):
    out = {}
    for key, vals in rows.items():
        if len(vals) != len(LANGS):
            raise ValueError(f"Key '{key}' has {len(vals)} languages, expected {len(LANGS)}")
        out[key] = dict(zip(LANGS, vals))
    return out

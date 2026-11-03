#!/usr/bin/env python3
"""Check all Compose and iOS translations using only the Python standard library.

This checks resource structure, not whether translations convey the same meaning.
"""

import plistlib
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COMPOSE_RESOURCES = Path("lib_strings/src/commonMain/composeResources")
IOS_RESOURCES = Path("iosApp/iosApp")
LANGUAGE_HELPER = Path(
    "feature/setting/src/commonMain/kotlin/com/mrl/pixiv/setting/LanguageHelper.kt"
)
LOCALE_ALIASES = {"zh-CN": "zh-Hans", "zh-TW": "zh-Hant", "pt-PT": "pt"}
FORMAT_TOKEN = re.compile(
    r"%(?:(\d+)\$)?([-#+ 0,(<]*)(?:\d+)?(?:\.\d+)?"
    r"([tT][a-zA-Z]|[bBhHsScCdoxXeEfgGaAu@%n])"
)
STRINGS_TOKEN = re.compile(
    r'\s+|//[^\n]*|/\*.*?\*/|"((?:[^"\\]|\\.)*)"\s*=\s*'
    r'"((?:[^"\\]|\\.)*)"\s*;',
    re.DOTALL,
)


def placeholders(text):
    """Keep argument numbers, types and counts; allow numbered reordering."""
    tokens = Counter()
    position = 0
    previous_index = None
    for token in FORMAT_TOKEN.finditer(text):
        index, flags, kind = token.groups()
        if kind in ("%", "n"):
            continue
        if "<" in flags:
            index = previous_index
        elif index is None:
            position += 1
            index = str(position)
        tokens[index, kind] += 1
        previous_index = index
    return tokens


def read_compose(directory, errors):
    entries = {}
    for path in sorted(directory.glob("*.xml")):
        try:
            resource_root = ET.parse(path).getroot()
        except ET.ParseError as error:
            errors.append(f"{path}: {error}")
            continue
        if resource_root.tag != "resources":
            errors.append(f"{path}: expected a resources root")
        for element in resource_root:
            name = element.get("name")
            if not name or element.tag not in ("string", "plurals", "string-array"):
                errors.append(f"{path}: unsupported resource {element.tag!r}: {name!r}")
                continue
            if name in entries:
                errors.append(f"{path}: duplicate key {name}")
                continue
            if element.tag == "string":
                items = {"text": "".join(element.itertext())}
            else:
                items = {}
                for index, item in enumerate(element):
                    key = item.get("quantity") if element.tag == "plurals" else str(index)
                    if item.tag != "item" or key is None or key in items:
                        errors.append(f"{path}: invalid or duplicate item in {name}")
                    items[key] = "".join(item.itertext())
                if element.tag == "plurals" and "other" not in items:
                    errors.append(f"{path}: {name} must contain the other plural form")
            if not items or any(not value.strip() for value in items.values()):
                errors.append(f"{path}: empty translation for {name}")
            entries[name] = (element.tag, element.get("translatable") != "false", items)
    if not entries:
        errors.append(f"{directory}: no string resources found")
    return entries


def compare_keys(label, reference, translated, required, errors):
    for key in sorted(required - translated.keys()):
        errors.append(f"{label}: missing key {key}")
    for key in sorted(translated.keys() - reference.keys()):
        errors.append(f"{label}: extra key {key}")


def check_compose(root, errors):
    resource_root = root / COMPOSE_RESOURCES
    reference = read_compose(resource_root / "values", errors)
    required = {key for key, (_, translatable, _) in reference.items() if translatable}
    directories = sorted(path for path in resource_root.glob("values-*") if path.is_dir())
    if not directories:
        errors.append(f"{resource_root}: no translated locales found")
    for directory in directories:
        translated = read_compose(directory, errors)
        compare_keys(directory, reference, translated, required, errors)
        for key in sorted(reference.keys() & translated.keys()):
            kind, translatable, items = reference[key]
            other_kind, other_translatable, other_items = translated[key]
            label = f"{directory}: {key}"
            if kind != other_kind:
                errors.append(f"{label}: resource type differs from default")
                continue
            if translatable != other_translatable:
                errors.append(f"{label}: translatable differs from default")
            if not translatable and items != other_items:
                errors.append(f"{label}: non-translatable resource differs from default")
            if kind == "string-array" and items.keys() != other_items.keys():
                errors.append(f"{label}: array item count differs from default")
            # Plural categories vary by language; compare additional categories to other.
            for item, value in other_items.items():
                original = items.get(item, items.get("other", ""))
                if placeholders(value) != placeholders(original):
                    errors.append(f"{label}[{item}]: format placeholders differ from default")
    locales = {"zh-Hans"}
    for directory in directories:
        locale = directory.name.removeprefix("values-").replace("-r", "-")
        locales.add(LOCALE_ALIASES.get(locale, locale))
    return locales, len(required)


def read_ios(path, errors):
    entries = {}
    if not path.is_file():
        errors.append(f"{path}: missing localization file")
        return entries
    text = path.read_text(encoding="utf-8-sig")
    offset = 0
    while offset < len(text):
        token = STRINGS_TOKEN.match(text, offset)
        if token is None:
            line = text.count("\n", 0, offset) + 1
            errors.append(f"{path}:{line}: invalid .strings syntax")
            break
        offset = token.end()
        key, value = token.groups()
        if key is None:
            continue
        if key in entries:
            errors.append(f"{path}: duplicate key {key}")
        if not value.strip():
            errors.append(f"{path}: empty translation for {key}")
        entries[key] = value
    if not entries:
        errors.append(f"{path}: no localized strings found")
    return entries


def check_ios(root, compose_locales, errors):
    resource_root = root / IOS_RESOURCES
    reference = read_ios(resource_root / "zh-Hans.lproj/InfoPlist.strings", errors)
    plist_path = resource_root / "Info.plist"
    with plist_path.open("rb") as file:
        info = plistlib.load(file)
    permission_keys = {key for key in info if key.endswith("UsageDescription")}
    localized_permissions = {key for key in reference if key.endswith("UsageDescription")}
    for key in sorted(permission_keys - localized_permissions):
        errors.append(f"{plist_path}: {key} has no localized permission description")
    for key in sorted(localized_permissions - permission_keys):
        errors.append(f"{plist_path}: localized permission {key} has no Info.plist entry")
    directories = sorted(path for path in resource_root.glob("*.lproj") if path.is_dir())
    locales = set()
    for directory in directories:
        if directory.stem == "Base":
            continue
        locales.add(LOCALE_ALIASES.get(directory.stem, directory.stem))
        path = directory / "InfoPlist.strings"
        translated = read_ios(path, errors)
        compare_keys(path, reference, translated, reference.keys(), errors)
        for key in sorted(reference.keys() & translated.keys()):
            if placeholders(reference[key]) != placeholders(translated[key]):
                errors.append(f"{path}: {key}: format placeholders differ from default")
    if locales != compose_locales:
        errors.append(
            "Compose/iOS locales differ: "
            f"missing from iOS={sorted(compose_locales - locales)}, "
            f"missing from Compose={sorted(locales - compose_locales)}"
        )
    return len(locales), len(reference)


def check_language_picker(root, compose_locales, errors):
    path = root / LANGUAGE_HELPER
    source = path.read_text(encoding="utf-8")
    match = re.search(r"private val localeList = listOf\((.*?)\)", source, re.DOTALL)
    if match is None:
        errors.append(f"{path}: cannot find localeList; update the localization checker")
        return
    tags = re.findall(r'"([^"]+)"', match[1])
    locales = {LOCALE_ALIASES.get(tag, tag) for tag in tags}
    if len(locales) != len(tags):
        errors.append(f"{path}: duplicate locale in language picker")
    if locales != compose_locales:
        errors.append(
            f"{path}: language picker differs from resources: "
            f"missing={sorted(compose_locales - locales)}, "
            f"extra={sorted(locales - compose_locales)}"
        )


def main():
    errors = []
    locales, key_count = check_compose(ROOT, errors)
    ios_count, ios_key_count = check_ios(ROOT, locales, errors)
    check_language_picker(ROOT, locales, errors)
    if errors:
        print("Localization checks failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(
        f"Localization checks passed: {len(locales)} Compose locales, "
        f"{key_count} translatable keys; {ios_count} iOS locales, "
        f"{ios_key_count} keys; language picker matches."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())

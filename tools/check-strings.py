#!/usr/bin/env python3
"""The rules aapt applies to string resources, which xmllint does not: an apostrophe or a double quote
inside a <string> must be escaped (\\' or \\"), and an ampersand must be an entity. Run by tools/lint.sh."""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
problems = []
for path in sorted(ROOT.glob("res/values*/*.xml")):
    raw = path.read_text(encoding="utf-8")
    for match in re.finditer(r"<string\b[^>]*>(.*?)</string>", raw, re.S):
        body = match.group(1)
        name = re.search(r'name="([^"]+)"', match.group(0)).group(1)
        if re.search(r"(?<!\\)'", body):
            problems.append(f"{path.relative_to(ROOT)}: {name}: unescaped apostrophe (use \\')")
        if re.search(r'(?<!\\)"', body):
            problems.append(f'{path.relative_to(ROOT)}: {name}: unescaped double quote (use \\")')
        if re.search(r"&(?!#?\w+;)", body):
            problems.append(f"{path.relative_to(ROOT)}: {name}: bare ampersand (use &amp;)")
    ET.parse(path)  # still well-formed XML
if problems:
    print("\n".join(problems))
    sys.exit(1)
print("string resources follow the aapt rules")

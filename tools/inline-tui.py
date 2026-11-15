#!/usr/bin/env python3
"""Keeps the copies of tools/tui.sh inside the scripts in step with it.

    python3 tools/inline-tui.py          write the block into each script
    python3 tools/inline-tui.py --check  fail if a script's copy differs (CI runs this)

A script carries the block between the two marker lines below; the block is replaced as a whole.
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools" / "tui.sh"
TARGETS = [ROOT / "shangos.sh"]
BEGIN = "# >>> shangOS TUI (generated from tools/tui.sh by tools/inline-tui.py; do not edit) >>>"
END = "# <<< shangOS TUI <<<"


def block():
    return f"{BEGIN}\n{SOURCE.read_text().rstrip()}\n{END}\n"


def main():
    check = "--check" in sys.argv
    wanted = block()
    bad = []
    for target in TARGETS:
        text = target.read_text()
        start, stop = text.find(BEGIN), text.find(END)
        if start < 0 or stop < 0:
            bad.append(f"{target.name}: no TUI block (add the two marker lines)")
            continue
        stop += len(END) + 1
        current = text[start:stop]
        if current == wanted:
            continue
        if check:
            bad.append(f"{target.name}: TUI block differs from tools/tui.sh")
        else:
            target.write_text(text[:start] + wanted + text[stop:])
            print(f"updated {target.name}")
    if check:
        if bad:
            print("\n".join(bad))
            return 1
        print("TUI blocks are in step with tools/tui.sh")
    return 0


if __name__ == "__main__":
    sys.exit(main())

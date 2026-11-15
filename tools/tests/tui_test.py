#!/usr/bin/env python3
"""Drives tools/tui.sh in a real pseudo-terminal (arrow keys as escape sequences) and checks the answers.

Run: python3 tools/tests/tui_test.py     (no dependencies; Linux or macOS)
"""
import os
import pty
import select
import subprocess
import sys
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TUI = os.path.join(ROOT, "tools", "tui.sh")
UP, DOWN, LEFT, RIGHT = b"\x1b[A", b"\x1b[B", b"\x1b[D", b"\x1b[C"
ENTER = b"\r"


def drive(script, keys, settle=0.25):
    """Runs bash with a pseudo-terminal, sends the keys one by one, and returns the output."""
    pid, fd = pty.fork()
    if pid == 0:
        os.environ["TERM"] = "xterm"
        os.execvp("bash", ["bash", "-c", f'source "{TUI}"; tui_init; {script}'])
    out = bytearray()

    def drain(seconds):
        end = time.time() + seconds
        while time.time() < end:
            ready, _, _ = select.select([fd], [], [], 0.05)
            if ready:
                try:
                    out.extend(os.read(fd, 4096))
                except OSError:
                    return

    drain(0.6)
    for key in keys:
        os.write(fd, key)
        drain(settle)
    drain(0.6)
    try:
        os.waitpid(pid, 0)
    except ChildProcessError:
        pass
    return out.decode("utf-8", "replace")


def plain(script, stdin_text):
    """Runs the same script with no terminal (a pipe): the numbered fallback."""
    result = subprocess.run(
        ["bash", "-c", f'source "{TUI}"; tui_init; {script}'],
        input=stdin_text, capture_output=True, text=True, timeout=20,
        env={**os.environ, "TERM": "dumb"},
    )
    return result.stdout + result.stderr


CASES = []


def case(name):
    def wrap(fn):
        CASES.append((name, fn))
        return fn
    return wrap


@case("arrow keys move the choice and Enter picks it")
def _():
    out = drive('choose "Pick" alpha beta gamma; echo "RESULT=$CHOICE"', [DOWN, DOWN, ENTER])
    return "RESULT=3" in out


@case("a number key jumps straight to that item")
def _():
    out = drive('choose "Pick" alpha beta gamma; echo "RESULT=$CHOICE"', [b"2", ENTER])
    return "RESULT=2" in out


@case("q goes to the last item (Quit)")
def _():
    out = drive('choose "Pick" alpha beta Quit; echo "RESULT=$CHOICE"', [b"q", ENTER])
    return "RESULT=3" in out


@case("Up at the top stays at the top")
def _():
    out = drive('choose "Pick" alpha beta; echo "RESULT=$CHOICE"', [UP, UP, ENTER])
    return "RESULT=1" in out


@case("yes/no: Enter on the default answers no")
def _():
    out = drive('yesno "Go?" n; echo "RC=$?"', [ENTER])
    return "RC=1" in out


@case("yes/no: Left then Enter answers yes")
def _():
    out = drive('yesno "Go?" n; echo "RC=$?"', [LEFT, ENTER])
    return "RC=0" in out


@case("yes/no: default y answers yes on Enter")
def _():
    out = drive('yesno "Go?" y; echo "RC=$?"', [ENTER])
    return "RC=0" in out


@case("yes/no: y and n keys answer at once")
def _():
    return "RC=0" in drive('yesno "Go?" n; echo "RC=$?"', [b"y"]) and "RC=1" in drive(
        'yesno "Go?" y; echo "RC=$?"', [b"n"])


@case("the cursor is shown again after a choice")
def _():
    out = drive('choose "Pick" alpha beta; echo "RESULT=$CHOICE"', [ENTER])
    return "\x1b[?25h" in out


@case("plain fallback (no terminal): a typed number chooses")
def _():
    out = plain('choose "Pick" alpha beta gamma; echo "RESULT=$CHOICE"', "3\n")
    return "RESULT=3" in out


@case("plain fallback: a bad answer is asked again")
def _():
    out = plain('choose "Pick" alpha beta; echo "RESULT=$CHOICE"', "9\n2\n")
    return "RESULT=2" in out and "1 to 2" in out


@case("plain fallback yes/no reads y or n")
def _():
    return "RC=0" in plain('yesno "Go?" n; echo "RC=$?"', "y\n") and "RC=1" in plain(
        'yesno "Go?" y; echo "RC=$?"', "n\n")


def main():
    failed = 0
    for name, fn in CASES:
        ok = fn()
        print(("ok    " if ok else "FAIL  ") + name)
        failed += 0 if ok else 1
    print(f"\n{len(CASES) - failed} of {len(CASES)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

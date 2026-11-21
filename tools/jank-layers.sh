#!/bin/bash
# Finds which layer of the screen makes frames late, by switching layers off one at a time and running the same
# D-pad loop as tools/baseline.sh after each. Needs a DEBUG build installed as the default Home (the switches
# are debug-only, see Layers.java), so the absolute numbers are a little worse than a release build: compare the
# lines with each other, not with baseline.sh.
#   tools/jank-layers.sh [-s DEVICE] [package/.Activity]      ROUNDS=2 (default)   LOWRES=1 also tries a screen at 3/4 size
# Variants: none | wallpaper | shadow | clip | shadow,clip | tiles | wallpaper,shadow,clip
# Reading it: the biggest drop in "late" and in GPU time names the layer to make cheaper. If "tiles" (nothing but the
# wallpaper and the bar drawn) is still late, the cost is not in the tiles.
set -uo pipefail
DEV=""; if [ "${1:-}" = "-s" ]; then DEV="$2"; shift 2; fi
COMP="${1:-dev.madusha.shangos/.MainActivity}"; PKG="${COMP%%/*}"; ROUNDS="${ROUNDS:-2}"
ADB=(adb); [ -n "$DEV" ] && ADB=(adb -s "$DEV")
sh() { "${ADB[@]}" shell "$@" | tr -d '\r'; }
nap() { [ "${FAST:-0}" = 1 ] || sleep "$@"; }
"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "No TV connected: adb connect <IP>:5555"; exit 1; }
sh pm list packages "$PKG" | grep -qx "package:$PKG" || { echo "$PKG is not installed"; exit 1; }
if ! sh cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | grep -q "^$PKG/"; then
  echo "ABORT: $PKG is not the default Home (see SET_HOME in tools/baseline.sh)"; exit 1
fi
# run-as only works for a debuggable app, so it is a reliable test (dumpsys flags vary by Android version).
if ! "${ADB[@]}" shell run-as "$PKG" id 2>/dev/null | grep -q "uid="; then
  echo "ABORT: this is not a debug build (./build.sh debug), so the layer switches do nothing"; exit 1
fi

restore() { sh wm size reset >/dev/null 2>&1; sh wm density reset >/dev/null 2>&1; }
trap restore EXIT

field() { sed -nE "s/^ *$1: *([0-9.]+).*/\1/p" <<<"$2" | head -1; }
report() {  # $1 = label, $2 = gfxinfo text
  local g="$2" frames janky up since secs fps
  frames=$(field 'Total frames rendered' "$g")
  if [ -z "$frames" ] || [ "$frames" = 0 ]; then echo "  $1: no frames drawn"; return; fi
  [ "$frames" -lt 100 ] && echo "  (this round drew only $frames frames: too few to trust the share late; raise WALKS)"
  janky=$(sed -nE 's/^ *Janky frames: *[0-9]+ \(([0-9.]+)%\).*/\1/p' <<<"$g" | head -1)
  up=$(field 'Uptime' "$g"); since=$(sed -nE 's/^ *Stats since: *([0-9]+)ns.*/\1/p' <<<"$g" | head -1)
  secs=$(awk -v u="${up:-0}" -v s="${since:-0}" 'BEGIN{printf "%.1f", (u - s/1e6)/1000}')
  fps=$(awk -v f="$frames" -v t="$secs" 'BEGIN{ if (t > 0) printf "%.1f", f/t; else print "n/a" }')
  printf '  %s: %s frames (%s/s), %s%% late | total p50 %s p90 %s ms | gpu p50 %s p90 %s ms | slow UI %s, slow draw %s\n' \
    "$1" "$frames" "$fps" "${janky:-?}" "$(field '50th percentile' "$g")" "$(field '90th percentile' "$g")" \
    "$(field '50th gpu percentile' "$g")" "$(field '90th gpu percentile' "$g")" \
    "$(field 'Number Slow UI thread' "$g")" "$(field 'Number Slow issue draw commands' "$g")"
}

press() {  # $1 = key, $2 = how many. BATCH=1 sends them in ONE `input` call: every separate `input keyevent` starts a
  # Java process, and on a weak TV 60 of them in a row use enough CPU to make the launcher's frames late by themselves.
  if [ "${BATCH:-0}" = 1 ]; then sh "input keyevent $(yes "$1" | head -n "$2" | tr '\n' ' ')"
  else sh "for i in \$(seq 1 $2); do input keyevent $1; done"; fi
}

walk() {  # One round of moving around Home like a person: 4 right, 4 left, WALKS times, one key every PACE seconds.
  # The old loop (LOOP=old) sent 30 rights then 30 lefts back to back: after 4 moves the focus is at the end of the row, so
  # most presses did nothing and a round drew only ~20 frames, too few for the share "late" to mean anything.
  if [ "${LOOP:-walk}" = old ]; then press KEYCODE_DPAD_RIGHT 30; press KEYCODE_DPAD_LEFT 30; return; fi
  sh "n=0; while [ \$n -lt ${WALKS:-5} ]; do for k in RIGHT RIGHT RIGHT RIGHT LEFT LEFT LEFT LEFT; do input keyevent KEYCODE_DPAD_\$k; sleep ${PACE:-0.6}; done; n=\$((n+1)); done"
}

run_variant() {  # $1 = layers to switch off ("none" = all on)
  echo "== layers off: $1"
  sh am start -n "$COMP" --es shangos.layers "$1" >/dev/null 2>&1; nap 3
  for r in $(seq 1 "$ROUNDS"); do
    sh dumpsys gfxinfo "$PKG" reset >/dev/null
    walk; nap 1
    report "round $r" "$(sh dumpsys gfxinfo "$PKG")"
  done
}

echo "device: $(sh getprop ro.product.manufacturer) $(sh getprop ro.product.model), $PKG $(sh dumpsys package "$PKG" | sed -nE 's/^ *versionName=//p' | head -1)"
echo "display: $(sh wm size | tail -1), $(sh wm density | tail -1)"
for v in none wallpaper shadow clip shadow,clip tiles wallpaper,shadow,clip; do run_variant "$v"; done
if [ "${LOWRES:-0}" = 1 ]; then
  # A fill-rate test: the same screen at three quarters of the width and height (56% of the pixels). If "late" and the GPU
  # times fall a lot, the screen is pixel-bound; if they do not, the cost is not in drawing pixels.
  SIZE=$(sh wm size | sed -nE 's/^Physical size: *([0-9]+)x([0-9]+).*/\1 \2/p' | head -1)
  DENS=$(sh wm density | sed -nE 's/^Physical density: *([0-9]+).*/\1/p' | head -1)
  if [ -n "$SIZE" ] && [ -n "$DENS" ]; then
    set -- $SIZE; LW=$(($1 * 3 / 4)); LH=$(($2 * 3 / 4))
    echo "== all layers on, display forced to ${LW}x${LH} (fill-rate test: a big drop means the screen is pixel-bound)"
    sh wm size "${LW}x${LH}" >/dev/null; sh wm density "$((DENS * 3 / 4))" >/dev/null; nap 4
    run_variant none
  fi
fi
echo "(display restored)"

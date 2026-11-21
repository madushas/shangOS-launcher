#!/bin/bash
# Finds which layer of the screen makes frames late, by switching layers off one at a time and running the same
# D-pad loop as tools/baseline.sh after each. Needs a DEBUG build installed as the default Home (the switches
# are debug-only, see Layers.java), so the absolute numbers are a little worse than a release build: compare the
# lines with each other, not with baseline.sh.
#   tools/jank-layers.sh [-s DEVICE] [package/.Activity]      ROUNDS=2 (default)   LOWRES=1 also tries 1280x720
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
if ! sh dumpsys package "$PKG" | grep -q "DEBUGGABLE"; then
  echo "ABORT: this is not a debug build (./build.sh debug), so the layer switches do nothing"; exit 1
fi

restore() { sh wm size reset >/dev/null 2>&1; sh wm density reset >/dev/null 2>&1; }
trap restore EXIT

field() { sed -nE "s/^ *$1: *([0-9.]+).*/\1/p" <<<"$2" | head -1; }
report() {  # $1 = label, $2 = gfxinfo text
  local g="$2" frames janky up since secs fps
  frames=$(field 'Total frames rendered' "$g")
  if [ -z "$frames" ] || [ "$frames" = 0 ]; then echo "  $1: no frames drawn"; return; fi
  janky=$(sed -nE 's/^ *Janky frames: *[0-9]+ \(([0-9.]+)%\).*/\1/p' <<<"$g" | head -1)
  up=$(field 'Uptime' "$g"); since=$(sed -nE 's/^ *Stats since: *([0-9]+)ns.*/\1/p' <<<"$g" | head -1)
  secs=$(awk -v u="${up:-0}" -v s="${since:-0}" 'BEGIN{printf "%.1f", (u - s/1e6)/1000}')
  fps=$(awk -v f="$frames" -v t="$secs" 'BEGIN{ if (t > 0) printf "%.1f", f/t; else print "n/a" }')
  printf '  %s: %s frames (%s/s), %s%% late | total p50 %s p90 %s ms | gpu p50 %s p90 %s ms | slow UI %s, slow draw %s\n' \
    "$1" "$frames" "$fps" "${janky:-?}" "$(field '50th percentile' "$g")" "$(field '90th percentile' "$g")" \
    "$(field '50th gpu percentile' "$g")" "$(field '90th gpu percentile' "$g")" \
    "$(field 'Number Slow UI thread' "$g")" "$(field 'Number Slow issue draw commands' "$g")"
}

run_variant() {  # $1 = layers to switch off ("none" = all on)
  echo "== layers off: $1"
  sh am start -n "$COMP" --es shangos.layers "$1" >/dev/null 2>&1; nap 3
  for r in $(seq 1 "$ROUNDS"); do
    sh dumpsys gfxinfo "$PKG" reset >/dev/null
    sh 'for i in $(seq 1 30); do input keyevent KEYCODE_DPAD_RIGHT; done; for i in $(seq 1 30); do input keyevent KEYCODE_DPAD_LEFT; done'; nap 1
    report "round $r" "$(sh dumpsys gfxinfo "$PKG")"
  done
}

echo "device: $(sh getprop ro.product.manufacturer) $(sh getprop ro.product.model), $PKG $(sh dumpsys package "$PKG" | sed -nE 's/^ *versionName=//p' | head -1)"
echo "display: $(sh wm size | tail -1), $(sh wm density | tail -1)"
for v in none wallpaper shadow clip shadow,clip tiles wallpaper,shadow,clip; do run_variant "$v"; done
if [ "${LOWRES:-0}" = 1 ]; then
  DENS=$(sh wm density | sed -nE 's/^Physical density: *([0-9]+).*/\1/p' | head -1)
  if [ -n "$DENS" ]; then
    echo "== all layers on, display forced to 1280x720 (fill-rate test: a big drop means the screen is pixel-bound)"
    sh wm size 1280x720 >/dev/null; sh wm density "$((DENS * 2 / 3))" >/dev/null; nap 4
    run_variant none
  fi
fi
echo "(display restored)"

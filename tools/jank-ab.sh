#!/bin/bash
# Finds out WHAT makes the frame test late, by running the same D-pad loop as tools/baseline.sh twice:
#   normal   everything as the user set it
#   still    system animations off (animator_duration_scale 0): shangOS then runs no tile, wallpaper or fade motion
# and printing, for each round, the frames drawn, the share that was late, the GPU and total frame times, and the
# causes gfxinfo counts. If "still" is smooth and "normal" is not, the motion is the cost; if both are late, it is
# something else (layout, bitmaps, the draw itself). The TV's own animation setting is put back at the end.
#   tools/jank-ab.sh [-s DEVICE] [package/.Activity]      ROUNDS=3 (default)
set -uo pipefail
DEV=""; if [ "${1:-}" = "-s" ]; then DEV="$2"; shift 2; fi
COMP="${1:-dev.madusha.shangos/.MainActivity}"; PKG="${COMP%%/*}"; ROUNDS="${ROUNDS:-3}"
ADB=(adb); [ -n "$DEV" ] && ADB=(adb -s "$DEV")
sh() { "${ADB[@]}" shell "$@" | tr -d '\r'; }
nap() { [ "${FAST:-0}" = 1 ] || sleep "$@"; }
"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "No TV connected: adb connect <IP>:5555"; exit 1; }
sh pm list packages "$PKG" | grep -qx "package:$PKG" || { echo "$PKG is not installed"; exit 1; }
if ! sh cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | grep -q "^$PKG/"; then
  echo "ABORT: $PKG is not the default Home (see SET_HOME in tools/baseline.sh)"; exit 1
fi

OLD_SCALE="$(sh settings get global animator_duration_scale)"; { [ -z "$OLD_SCALE" ] || [ "$OLD_SCALE" = null ]; } && OLD_SCALE=1.0
restore() { sh settings put global animator_duration_scale "$OLD_SCALE" >/dev/null 2>&1; }
trap restore EXIT

field() { sed -nE "s/^ *$1: *([0-9.]+).*/\1/p" <<<"$2" | head -1; }
report() {  # $1 = label, $2 = gfxinfo text
  local g="$2" frames janky since up secs fps
  frames=$(field 'Total frames rendered' "$g")
  if [ -z "$frames" ] || [ "$frames" = 0 ]; then echo "  $1: no frames drawn (the app was not on screen)"; return; fi
  [ "$frames" -lt 100 ] && echo "  (this round drew only $frames frames: too few to trust the share late; raise WALKS)"
  janky=$(sed -nE 's/^ *Janky frames: *[0-9]+ \(([0-9.]+)%\).*/\1/p' <<<"$g" | head -1)
  up=$(field 'Uptime' "$g"); since=$(sed -nE 's/^ *Stats since: *([0-9]+)ns.*/\1/p' <<<"$g" | head -1)
  secs=$(awk -v u="${up:-0}" -v s="${since:-0}" 'BEGIN{printf "%.1f", (u - s/1e6)/1000}')
  fps=$(awk -v f="$frames" -v t="$secs" 'BEGIN{ if (t > 0) printf "%.1f", f/t; else print "n/a" }')
  printf '  %s: %s frames in %s s (%s per second), %s%% late | total p50 %s p90 %s p99 %s ms | gpu p50 %s p90 %s ms\n' \
    "$1" "$frames" "$secs" "$fps" "${janky:-?}" "$(field '50th percentile' "$g")" "$(field '90th percentile' "$g")" \
    "$(field '99th percentile' "$g")" "$(field '50th gpu percentile' "$g")" "$(field '90th gpu percentile' "$g")"
  printf '        causes: missed vsync %s, slow UI thread %s, slow bitmap uploads %s, slow draw commands %s, deadline missed %s\n' \
    "$(field 'Number Missed Vsync' "$g")" "$(field 'Number Slow UI thread' "$g")" "$(field 'Number Slow bitmap uploads' "$g")" \
    "$(field 'Number Slow issue draw commands' "$g")" "$(field 'Number Frame deadline missed' "$g")"
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

run_variant() {  # $1 = name, $2 = animator scale
  echo "== $1 (animator_duration_scale $2)"
  sh settings put global animator_duration_scale "$2" >/dev/null
  for r in $(seq 1 "$ROUNDS"); do
    sh am start -W -n "$COMP" >/dev/null 2>&1; nap 3
    sh dumpsys gfxinfo "$PKG" reset >/dev/null
    walk; nap 1
    report "round $r" "$(sh dumpsys gfxinfo "$PKG")"
  done
}
echo "device: $(sh getprop ro.product.manufacturer) $(sh getprop ro.product.model), $PKG $(sh dumpsys package "$PKG" | sed -nE 's/^ *versionName=//p' | head -1)"
run_variant normal "$OLD_SCALE"
run_variant still 0
echo "(the animation setting was restored to $OLD_SCALE)"

#!/bin/bash
# Measures shangOS, Luma TV and the TV's own launcher on YOUR TV, one after the other, and writes one JSON file.
#   tools/bench.sh -s 192.168.1.50:5555                  everything that is installed
#   tools/bench.sh -s 192.168.1.50:5555 --only stock,shangos
#   tools/bench.sh -s 192.168.1.50:5555 --out docs/data/bench.json
#   tools/bench.sh -s 192.168.1.50:5555 --perf light       shangOS's Performance is Light (you set it in Settings; recorded)
# Needs: adb, python3, and each launcher installed once (open it once). Takes about 5 minutes per launcher; QUICK=1 shortens it.
# Do not touch the remote while it runs. At the end the TV gets its original Home screen back and anything this script
# switched on again is switched off again. The JSON says which TV, which versions and when; the release build can publish it
# (see docs/MEASURING.md). Method and fairness rules: docs/MEASURING.md.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
# Defaults a fair run needs, set here so nobody has to set environment variables.
SET_HOME="${SET_HOME:-1}"   # each launcher is the default Home while it is measured (reversible)
COMPILE="${COMPILE:-speed}" # each launcher is compiled first, so all are measured in the same state
PERF="auto"                 # shangOS Performance, as you set it by hand in its Settings: auto, light or quality
export SET_HOME COMPILE
DEV="" ONLY="shangos,luma,stock" OUT="docs/data/bench.json" WORK=""
usage() { sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }
while [ $# -gt 0 ]; do
    case "$1" in
        -s) DEV="${2:?-s needs the TV address}"; shift 2 ;;
        --only) ONLY="${2:?--only needs a list: shangos,luma,stock}"; shift 2 ;;
        --out) OUT="${2:?--out needs a path}"; shift 2 ;;
        --perf) PERF="${2:?--perf needs auto, light or quality}"; shift 2 ;;
        --help | -h) usage 0 ;;
        *) usage 1 >&2 ;;
    esac
done
case "$PERF" in auto | light | quality) ;; *) echo "--perf takes auto, light or quality"; exit 1 ;; esac
ADB=(adb)
[ -n "$DEV" ] && ADB=(adb -s "$DEV")
sh() { "${ADB[@]}" shell "$@" | tr -d '\r'; }
command -v adb >/dev/null 2>&1 || { echo "adb is needed"; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "python3 is needed"; exit 1; }
"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "No TV connected: adb connect <IP>:5555"; exit 1; }
STOCK_PKGS="com.google.android.tvlauncher com.google.android.apps.tv.launcherx com.google.android.leanbacklauncher"
ALL_MANAGED_PKGS="$STOCK_PKGS com.google.android.tungsten.setupwraith"
installed() { sh pm list packages "$1" | grep -qx "package:$1"; }
disabled() { sh pm list packages -d "$1" | grep -qx "package:$1"; }
home_activity_of() { sh cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME "$1" | tail -1; }
ORIGINAL_HOME="$(sh cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
WAS_DISABLED=""
WAS_ENABLED=""
for p in $ALL_MANAGED_PKGS; do
    if installed "$p"; then
        if disabled "$p"; then
            WAS_DISABLED="$WAS_DISABLED $p"
        else
            WAS_ENABLED="$WAS_ENABLED $p"
        fi
    fi
done
WORK="$(mktemp -d)"
# shellcheck disable=SC2329 # runs from the EXIT trap
restore() {
    for p in $WAS_DISABLED; do sh pm disable-user --user 0 "$p" >/dev/null 2>&1; done
    for p in $WAS_ENABLED; do sh pm enable --user 0 "$p" >/dev/null 2>&1; done
    [[ "$ORIGINAL_HOME" =~ ^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$ ]] && sh cmd package set-home-activity --user 0 "$ORIGINAL_HOME" >/dev/null 2>&1
    rm -rf "$WORK"
    echo "The TV's Home is back to $ORIGINAL_HOME."
}
trap restore EXIT
trap 'echo; echo "Stopped."; exit 130' INT TERM

disable_stock_all() {
    # Disable Google TV launcher and its home hooker / setup wrapper so third-party launchers receive the Home intent
    for p in $STOCK_PKGS com.google.android.tungsten.setupwraith; do
        if installed "$p" && ! disabled "$p"; then
            echo "   Disabling $p so third-party launchers receive the Home intent"
            sh pm disable-user --user 0 "$p" >/dev/null 2>&1
        fi
    done
}

prepare_launcher() {
    local comp="$1"
    local pkg="${comp%%/*}"
    if [[ " $STOCK_PKGS " == *" $pkg "* ]]; then
        disabled "$pkg" && sh pm enable --user 0 "$pkg" >/dev/null 2>&1
    else
        # Disable stock launchers so this third-party launcher can resolve as default Home
        disable_stock_all
    fi
}

# shangOS's Performance is set by hand in its Settings (Motion and sound, Performance): a release build cannot be
# changed over adb, so this script never changes it. --perf says what you set; the report records it (it cannot
# check it). For Auto it also predicts the tier from the TV's RAM, cores and bitness, with the rules that
# shangOS's Hardware.classify uses.
predict_tier() {
    local kb cores abi lowram bytes is64=0 gb=1073741824
    kb="$(sh grep MemTotal /proc/meminfo | tr -dc '0-9')"
    cores="$(sh grep -c '^processor' /proc/cpuinfo | tr -dc '0-9')"
    abi="$(sh getprop ro.product.cpu.abi)"
    lowram="$(sh getprop ro.config.low_ram)"
    bytes=$(( ${kb:-0} * 1024 ))
    case "$abi" in arm64* | x86_64*) is64=1 ;; esac
    if [ "$lowram" = true ]; then echo LIGHT
    elif [ "$bytes" -lt $(( 3 * gb / 2 )) ]; then echo LIGHT
    elif [ "$bytes" -lt $(( 2 * gb )) ] && [ "$is64" = 0 ]; then echo LIGHT
    elif [ "$bytes" -ge $(( 3 * gb )) ] && [ "$is64" = 1 ] && [ "${cores:-0}" -ge 4 ]; then echo QUALITY
    else echo BALANCED; fi
}
tier_for() { case "$1" in light) echo LIGHT ;; quality) echo QUALITY ;; *) predict_tier ;; esac; }

REPORTS=()
run_one() { # run_one LABEL PACKAGE/ACTIVITY
    local label="$1" comp="$2"
    echo "== Measuring $label ($comp): about 5 minutes, do not touch the remote"
    prepare_launcher "$comp"
    if OUTDIR="$WORK" SET_HOME=1 COMPILE=speed tools/baseline.sh ${DEV:+-s "$DEV"} "$comp" "$label"; then REPORTS+=("$WORK/measure-$label.txt"); else echo "   $label failed; it is left out"; fi
}

# 1. Measure stock first if requested, then disable it entirely so subsequent launchers can bind Home reliably
case ",$ONLY," in *,stock,*)
    found="" foundpkg=""
    for p in $STOCK_PKGS; do
        installed "$p" || continue
        disabled "$p" && sh pm enable --user 0 "$p" >/dev/null 2>&1
        act="$(home_activity_of "$p")"
        if [[ "$act" == "$p/"* ]]; then
            found="$act"; foundpkg="$p"
            break
        fi
        [[ " $WAS_DISABLED " == *" $p "* ]] && sh pm disable-user --user 0 "$p" >/dev/null 2>&1
    done
    # The label names the launcher (googletv, androidtv or tvhome), so the site can say which one was measured.
    case "$foundpkg" in
        com.google.android.tvlauncher|com.google.android.apps.tv.launcherx) lbl=googletv ;;
        com.google.android.leanbacklauncher) lbl=androidtv ;;
        *) lbl=tvhome ;;
    esac
    if [ -n "$found" ]; then
        run_one "$lbl" "$found"
    else
        echo "No stock launcher found: skipped"
    fi
    disable_stock_all
    ;;
esac

# 2. Measure shangOS
case ",$ONLY," in *,shangos,*)
    disable_stock_all
    if installed dev.madusha.shangos; then
        echo "shangOS Performance for this run: $PERF (recorded as you set it; the script does not change it)."
        [ -t 0 ] && { read -r -p "Set Performance to $PERF in shangOS's Settings on the TV, then press Enter to measure: " _ || true; }
        TIER="$(tier_for "$PERF")"
        echo "   the tier this gives on this TV: $TIER"
        run_one shangos dev.madusha.shangos/.MainActivity
        if [ -f "$WORK/measure-shangos.txt" ]; then
            printf 'INFO perf_setting=%s\nINFO perf_tier=%s\n' "$PERF" "$TIER" >>"$WORK/measure-shangos.txt"
        fi
    else echo "shangOS is not installed on this TV: skipped"; fi ;;
esac

# 3. Measure luma
case ",$ONLY," in *,luma,*)
    disable_stock_all
    if installed dev.luma.tv; then run_one luma dev.luma.tv/.MainActivity; else echo "Luma TV is not installed (install the latest from https://github.com/0xProbabillity/luma-tv/releases): skipped"; fi ;;
esac

if [ "${#REPORTS[@]}" -eq 0 ]; then echo "Nothing was measured."; exit 1; fi
python3 tools/bench-json.py "${REPORTS[@]}" --out "$OUT" && python3 tools/bench-json.py --check "$OUT"
echo "Next: python3 tools/build-site.py   (the website shows this file), then commit $OUT"

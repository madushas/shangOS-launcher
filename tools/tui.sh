# shangOS TUI: arrow-key choices and yes/no, with no dependencies (bash 3.2 and later).
# This is the source of truth for the block inlined into shangos.sh (tools/inline-tui.py keeps the copies in step).
# Without a terminal (a pipe, TERM=dumb, or SHANGOS_PLAIN=1) every prompt falls back to typed numbers and y/n.

tui_init() { # decides between the drawn menus and plain prompts; call once at start
    TUI_OK=0
    if [ -t 0 ] && [ -t 1 ] && [ "${TERM:-dumb}" != dumb ] && [ -z "${SHANGOS_PLAIN:-}" ]; then TUI_OK=1; fi
    if [ "$TUI_OK" = 1 ] && [ -z "${NO_COLOR:-}" ]; then
        C_DIM=$'\033[2m' C_BOLD=$'\033[1m' C_ACC=$'\033[36m' C_INV=$'\033[7m' C_RESET=$'\033[0m'
    else
        C_DIM="" C_BOLD="" C_ACC="" C_INV="" C_RESET=""
    fi
    case "${BASH_VERSINFO[0]:-3}" in 3) TUI_ESC_T=1 ;; *) TUI_ESC_T=0.1 ;; esac
    trap 'tui_restore' EXIT
}

tui_restore() { # shows the cursor again whatever happened
    [ "${TUI_OK:-0}" = 1 ] && printf '\033[?25h'
    return 0
}

tui_key() { # reads one key into KEY: up down left right enter back space esc eof, or the character typed
    local c1 c2 c3
    KEY=""
    if ! IFS= read -rsn1 c1; then KEY=eof; return 0; fi
    case "$c1" in
        "" | $'\n' | $'\r') KEY=enter ;;
        $'\177' | $'\b') KEY=back ;;
        ' ') KEY=space ;;
        $'\003') KEY=ctrl-c ;;
        $'\033')
            KEY=esc
            if IFS= read -rsn1 -t "$TUI_ESC_T" c2 && { [ "$c2" = "[" ] || [ "$c2" = "O" ]; }; then
                if IFS= read -rsn1 -t "$TUI_ESC_T" c3; then
                    case "$c3" in
                        A) KEY=up ;;
                        B) KEY=down ;;
                        C) KEY=right ;;
                        D) KEY=left ;;
                    esac
                fi
            fi
            ;;
        *) KEY="$c1" ;;
    esac
    return 0
}

tui_width() { # the width of the terminal, never less than 40
    local w
    w="$(tput cols 2>/dev/null || echo 80)"
    [ "$w" -ge 40 ] 2>/dev/null || w=80
    echo "$w"
}

choose() { # choose "Title" option... -> CHOICE (1-based). Up/Down move, Enter chooses, 1-9 jump, q goes to the last.
    local title="$1" n i opt sel=1 drawn=0 width
    shift
    local -a opts=("$@")
    n=${#opts[@]}
    if [ "$TUI_OK" != 1 ]; then
        echo
        echo "$title"
        i=1
        for opt in "${opts[@]}"; do
            printf '  %d) %s\n' "$i" "$opt"
            i=$((i + 1))
        done
        while true; do
            read -r -p "Choose 1-$n: " REPLY || exit 1
            if [[ "$REPLY" =~ ^[0-9]+$ ]] && [ "$REPLY" -ge 1 ] && [ "$REPLY" -le "$n" ]; then
                CHOICE="$REPLY"
                return 0
            fi
            echo "Please type a number from 1 to $n."
        done
    fi
    width=$(( $(tui_width) - 8 ))
    printf '\033[?25l'
    while true; do
        [ "$drawn" -gt 0 ] && printf '\033[%dA' "$drawn"
        printf '\r\033[K  %s%s%s\n' "$C_BOLD" "$title" "$C_RESET"
        i=1
        for opt in "${opts[@]}"; do
            if [ "$i" -eq "$sel" ]; then
                printf '\r\033[K  %s›%s %s%s %s %s\n' "$C_ACC" "$C_RESET" "$C_INV" "$C_BOLD" "${opt:0:$width}" "$C_RESET"
            else
                printf '\r\033[K    %s%s%s\n' "$C_DIM" "${opt:0:$width}" "$C_RESET"
            fi
            i=$((i + 1))
        done
        drawn=$((n + 1))
        tui_key
        case "$KEY" in
            up | k | K) [ "$sel" -gt 1 ] && sel=$((sel - 1)) ;;
            down | j | J) [ "$sel" -lt "$n" ] && sel=$((sel + 1)) ;;
            q | Q) sel=$n ;;
            enter) CHOICE=$sel; break ;;
            [1-9]) [ "$KEY" -le "$n" ] && sel=$KEY ;;
            eof | ctrl-c) printf '\033[?25h'; echo; exit 130 ;;
        esac
    done
    printf '\033[?25h'
    return 0
}

yesno() { # yesno "Question" [y|n default] -> status 0 for yes. Left/Right or Y/N, Enter confirms.
    local q="$1" default="${2:-n}" yes ans=0 drawn=0 key
    if [ "$TUI_OK" != 1 ]; then
        local hint="y/N"
        [ "$default" = y ] && hint="Y/n"
        read -r -p "$q ($hint) " REPLY || return 1
        [[ "${REPLY:-$default}" =~ ^[Yy] ]]
        return $?
    fi
    yes=0
    [ "$default" = y ] && yes=1
    printf '\033[?25l'
    while true; do
        [ "$drawn" -gt 0 ] && printf '\033[%dA' "$drawn"
        printf '\r\033[K  %s%s%s\n' "$C_BOLD" "$q" "$C_RESET"
        if [ "$yes" -eq 1 ]; then
            printf '\r\033[K  %s Yes %s    No\n' "$C_INV$C_BOLD" "$C_RESET"
        else
            printf '\r\033[K    Yes    %s No %s\n' "$C_INV$C_BOLD" "$C_RESET"
        fi
        drawn=2
        tui_key
        key="$KEY"
        case "$key" in
            left | right | up | down | h | l | tab) yes=$((1 - yes)) ;;
            y | Y) ans=1; break ;;
            n | N) ans=0; break ;;
            enter) ans=$yes; break ;;
            eof | ctrl-c) printf '\033[?25h'; echo; exit 130 ;;
        esac
    done
    printf '\033[?25h'
    return $((1 - ans))
}

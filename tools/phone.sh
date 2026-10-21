#!/usr/bin/env bash
# Drives a real Android phone over wireless debugging from a computer (adb in a container, key kept in ~/.snapkey-phone-adb).
# Usage: PHONE=<ip>:<port> tools/phone.sh <command>
#   start                     starts the adb-phone container      stop         removes it
#   pair <ip:port> <code>     pairing (port and code of the "Pair device with pairing code" dialog)
#   connect                   connects to $PHONE (port shown on the Wireless debugging screen)
#   dump [n]                  texts on screen                     shot <file.png>   screenshot
#   tap "<text>"              taps the element whose text or description contains this text
#   type "<text>"             keyboard input                      search "<word>"   search in Settings (finds hidden settings faster than scrolling)
#   sh <command>              shell command on the phone
set -euo pipefail
C=adb-phone
A=(docker exec "$C" adb -s "${PHONE:-}")
case "${1:-}" in
start) docker rm -f "$C" >/dev/null 2>&1 || true
       docker run -d --name "$C" --network host -v "$HOME/.snapkey-phone-adb:/root/.android" ghcr.io/cirruslabs/android-sdk:35 sleep 7200 >/dev/null
       docker exec "$C" adb start-server ;;
stop) docker rm -f "$C" >/dev/null ;;
pair) docker exec "$C" adb pair "$2" "$3" ;;
connect) docker exec "$C" adb connect "$PHONE" ;;
dump) "${A[@]}" shell uiautomator dump /sdcard/u.xml >/dev/null
      "${A[@]}" shell cat /sdcard/u.xml | grep -o '\(text\|content-desc\)="[^"]\+"' | head -"${2:-40}" ;;
shot) "${A[@]}" exec-out screencap -p > "$2" ;;
tap) "${A[@]}" shell uiautomator dump /sdcard/u.xml >/dev/null
     xy=$("${A[@]}" shell cat /sdcard/u.xml | python3 -c '
import html, re, sys
t = sys.argv[1].lower()
for n in re.findall(r"<node [^>]*>", sys.stdin.read()):
    b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n)
    if b and any(t in html.unescape(l).lower() for l in re.findall(r"(?:text|content-desc)=\"([^\"]*)\"", n)):
        x1, y1, x2, y2 = map(int, b.groups()); print((x1 + x2) // 2, (y1 + y2) // 2); break' "$2")
     [ -n "$xy" ] || { echo "not found: $2" >&2; exit 1; }
     "${A[@]}" shell input tap $xy ;;
type) "${A[@]}" shell input text "${2// /%s}" ;;
search) "${A[@]}" shell am force-stop com.android.settings
        "${A[@]}" shell am start -a android.settings.SETTINGS >/dev/null; sleep 3
        "$0" tap "Rechercher" 2>/dev/null || "$0" tap "Search"; sleep 2
        "$0" type "$2"; sleep 2; "$0" dump 30 ;;
sh) shift; "${A[@]}" shell "$@" ;;
*) sed -n '2,10p' "$0"; exit 1 ;;
esac

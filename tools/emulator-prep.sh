#!/bin/bash
# Prépare un émulateur Android neuf (conteneur Docker) pour tester SnapKey Mapper : attend le démarrage, root, options
# développeur, Chrome sans premier lancement, permissions des applis Google qui bloquent un essai, installation du debug.
# Usage : tools/emulator-prep.sh [chemin de l'APK de debug]   (conteneur : variable EMU, défaut snapkey-emu)
EMU=${EMU:-snapkey-emu}
APK=${1:-$(dirname "$0")/../app/build/outputs/apk/full/debug/app-full-debug.apk}
A="docker exec $EMU adb"
docker exec "$EMU" bash -c 'export PATH=$PATH:/opt/android-sdk-linux/platform-tools; timeout 150 adb wait-for-device; for i in $(seq 1 100); do [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = 1 ] && break; sleep 3; done'
$A root >/dev/null 2>&1; sleep 3
$A shell settings put global development_settings_enabled 1                       # sans cela l'écran des options développeur se referme
$A shell "echo '_ --no-first-run --disable-fre --no-default-browser-check' > /data/local/tmp/chrome-command-line; chmod 755 /data/local/tmp/chrome-command-line"
for p in READ_CONTACTS WRITE_CONTACTS POST_NOTIFICATIONS; do $A shell pm grant com.google.android.contacts android.permission.$p; done
$A shell pm grant com.google.android.youtube android.permission.POST_NOTIFICATIONS
docker cp "$APK" "$EMU:/tmp/app.apk" && $A install -r /tmp/app.apk
$A shell pm grant io.github.damienllado.snapkeymapper android.permission.POST_NOTIFICATIONS   # sinon la boîte des notifications cache l'écran

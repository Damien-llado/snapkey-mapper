#!/usr/bin/env python3
"""Rejoue l'installation automatique (édition full) sur l'émulateur : débogage sans fil, code de couplage lu à l'écran,
saisie dans la notification, contrôle de READ_LOGS et de la notification finale.

Préparation : émulateur Android 35 démarré (tools/emulator-prep.sh), build full debug installé, `adb unroot` (le shell
doit être l'utilisateur 2000 : root ne peut pas poster de notification), pas de `adb root` pendant ce test.
Usage : python3 tools/test_setup.py          (conteneur : variable EMU, défaut snapkey-emu ; écran 1080 x 2400)
"""
import os, re, shlex, subprocess, sys, time

EMU = os.environ.get("EMU", "snapkey-emu")
P = "io.github.damienllado.snapkeymapper"


def adb(*args):
    return subprocess.run(["docker", "exec", EMU, "adb", *args], capture_output=True, text=True).stdout


def sh(cmd):
    return adb("shell", cmd)


def dump():
    sh("uiautomator dump /sdcard/u.xml")
    return sh("cat /sdcard/u.xml")


def tap_text(text, exact=True, tries=6):
    """Touche le premier élément dont le texte (ou la description) correspond ; ne marche pas sur l'écran d'accueil de l'appli."""
    for _ in range(tries):
        for node in re.findall(r"<node [^>]*>", dump()):  # nœud par nœud : un nœud peut avoir text="" et content-desc="Send"
            m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
            for label in re.findall(r'(?:text|content-desc)="([^"]*)"', node):
                if m and ((label == text) if exact else (text.lower() in label.lower())):
                    x1, y1, x2, y2 = map(int, m.groups())
                    sh(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")
                    return True
        time.sleep(1)
    return False


def step(msg):
    print("-", msg, flush=True)


def main():
    sh("input keyevent KEYCODE_HOME; am force-stop com.android.settings")  # boîte de dialogue restée ouverte par un essai précédent
    sh(f"cmd locale set-app-locales {P} --locales en-US")  # les textes cherchés sont en anglais
    sh("svc wifi enable")
    sh("settings put global development_settings_enabled 1")
    sh("settings put global adb_wifi_enabled 0")  # coupe puis rallume : efface les annonces mDNS (ports) des essais précédents
    time.sleep(3)
    sh(f"pm revoke {P} android.permission.READ_LOGS")
    sh(f"am force-stop {P}")
    sh('cmd notification post -t x setup "reset"')  # même tag que la notification finale : remplace celle d'un essai précédent
    step("ouvre l'appli et touche « Set up log reading automatically » (l'appli ouvre les réglages du débogage sans fil)")
    sh(f"am start -n {P}/.DebugFire --es screen setup")  # même action que le bouton, sans coordonnées à toucher
    time.sleep(5)
    step("va dans Options pour les développeurs > Débogage sans fil")
    for _ in range(10):
        if tap_text("Wireless debugging", tries=1):
            break
        sh("input swipe 540 1800 540 700 300")
        time.sleep(1)
    time.sleep(2)
    if sh("settings get global adb_wifi_enabled").strip() != "1":
        step("active le débogage sans fil")
        tap_text("Use wireless debugging")
        time.sleep(2)
        tap_text("Allow")  # boîte « Allow wireless debugging on this network? » (texte exact : l'autre case contient « allow » aussi)
        time.sleep(3)
    step("ouvre « Pair device with pairing code »")
    if not tap_text("Pair device with pairing code"):
        sys.exit("échec : l'écran du débogage sans fil n'est pas ouvert")
    time.sleep(3)
    code = (re.search(r'text="(\d{6})"', dump()) or sys.exit("échec : pas de code à l'écran")).group(1)
    step(f"code lu : {code}")
    time.sleep(5)
    sh("cmd statusbar expand-notifications")
    time.sleep(2)
    step("saisit le code dans la notification de l'appli")
    if not tap_text("Enter code"):
        sys.exit("échec : la notification de l'appli n'est pas apparue")
    time.sleep(2)
    sh(f"input text {code}")
    time.sleep(1)
    tap_text("Send")  # flèche d'envoi du champ de réponse
    time.sleep(14)
    granted = "READ_LOGS: granted=true" in sh(f"dumpsys package {P}")
    notif = re.search(r"android.text=String \((Log reading is set up[^)]*)", sh("dumpsys notification --noredact"))
    print("READ_LOGS accordée :", granted)
    print("notification finale :", notif.group(1) if notif else "ABSENTE")
    sys.exit(0 if granted and notif else 1)


main()

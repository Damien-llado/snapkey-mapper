#!/usr/bin/env python3
"""Teste les recettes sur un émulateur : ouvre chaque lien (valeurs d'exemple) par l'activité DebugFire du build
debug, lit l'activité au premier plan et compare au paquet attendu (tools/recipe_expect.json).

Usage : python3 tools/test_recipes.py [--write] [id ...]
        python3 tools/test_recipes.py --resolve [id ...]   mode rapide : demande à Android quelle activité prendrait le lien, sans rien lancer
                                                           (0,1 s par recette au lieu de 30 s ; sert de test de non-régression et de test de "dispatch")
  ADB="docker exec snapkey-emu adb" (défaut) ; SHOTS=<dossier> pour garder une capture par recette ;
  --write inscrit la date dans le champ "tested" des recettes qui passent.
"""
import base64, datetime, json, os, re, shlex, subprocess, sys, time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "app/src/main/assets/recipes/recipes.json"
EXPECT = json.loads((ROOT / "tools/recipe_expect.json").read_text())
ADB = shlex.split(os.environ.get("ADB", "docker exec snapkey-emu adb"))
SHOTS = os.environ.get("SHOTS")
PKG = "io.github.damienllado.snapkeymapper"
# apps stopped before each recipe so that a screen left open by the previous one cannot hide the result
FRESH = ["com.android.settings", "com.android.camera2", "com.google.android.dialer", "com.google.android.apps.messaging",
         "com.android.chrome", "com.google.android.calendar", "com.google.android.contacts", "com.google.android.gm"]


def adb(*args, binary=False):
    r = subprocess.run(ADB + list(args), capture_output=True)
    return r.stdout if binary else r.stdout.decode(errors="replace")


def encode(value):
    """Même règle qu'Uri.encode d'Android : lettres, chiffres et _-!.~'()* restent."""
    return "".join(c if re.match(r"[A-Za-z0-9_\-!.~'()*]", c) else "".join(f"%{b:02X}" for b in c.encode()) for c in value)


def filled(recipe):
    link = recipe["link"]
    for v in recipe["vars"]:
        kind, value = v.get("kind", "text"), v["example"]
        value = (value if kind == "raw" else re.sub(r"[^0-9+]", "", value) if kind == "phone"
                 else re.sub(r"\D", "", value) if kind == "digits" else encode(value))
        link = link.replace(f"[{v['name']}]", value)
    return link


def top():
    out = adb("shell", "dumpsys", "activity", "activities")
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ \S+ (\S+)", out)
    return m.group(1) if m else "?"


def screen_text():
    adb("shell", "uiautomator", "dump", "/sdcard/u.xml")
    return re.sub(r"\s+", "", " ".join(re.findall(r'text="([^"]*)"', adb("shell", "cat", "/sdcard/u.xml"))))


def check(spec):
    """spec : texte (paquet attendu en premier plan) ou dict : top, ui (textes lus à l'écran), log (regex dans le journal des
    démarrages d'activités), shell + match (regex), level ("screen" par défaut : on voit le résultat ; "dispatch" : l'appli
    visée reçoit le lien mais l'émulateur ne peut pas aller plus loin, connexion ou carte SIM manquante)."""
    if isinstance(spec, str):
        spec = {"top": spec}
    where, ok = top(), True
    if "top" in spec:
        tops = [spec["top"]] if isinstance(spec["top"], str) else spec["top"]
        ok &= any(t in where for t in tops)
    if "ui" in spec:
        text = screen_text()
        ok &= all(re.sub(r"\s+", "", t) in text for t in spec["ui"])
    if "log" in spec:
        ok &= bool(re.search(spec["log"], adb("logcat", "-d", "-s", "ActivityTaskManager:I")))
    if "shell" in spec:
        ok &= bool(re.search(spec["match"], adb("shell", *shlex.split(spec["shell"]))))
    return ok, where


def resolve(link):
    """L'activité qu'Android choisirait pour ce lien, comme le fait l'appli (ACTION_VIEW, ou l'intent décrit par intent:)."""
    args = [link] if link.startswith("intent:") else ["-a", "android.intent.action.VIEW", "-d", link]
    out = adb("shell", "cmd package resolve-activity --brief " + " ".join(shlex.quote(a) for a in args)).strip().splitlines()
    return out[-1].strip() if out and "/" in out[-1] else None


def main_resolve(wanted, write):
    """Avec --write : les recettes pas encore vérifiées dont le lien est pris par la bonne appli passent au niveau "dispatch"."""
    data = json.loads(RECIPES.read_text())
    today = datetime.date.today().isoformat()
    bad = 0
    for app in data["apps"]:
        mine = [app["package"]] + app.get("also", []) if app.get("package") else []
        for r in app["recipes"]:
            if wanted and r["id"] not in wanted:
                continue
            handler = resolve(filled(r))
            ok = bool(handler) and (app.get("kind") == "system" or not mine or any(handler.startswith(m) for m in mine))
            bad += not ok
            print(("OK  " if ok else "FAIL"), r["id"], "->", handler or "aucune activité ne prend ce lien")
            if write and ok and not r.get("tested"):
                r["tested"], r["level"] = today, "dispatch"
    if write:
        RECIPES.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    sys.exit(1 if bad else 0)


def main():
    if "--resolve" in sys.argv:
        main_resolve([a for a in sys.argv[1:] if not a.startswith("--")], "--write" in sys.argv)
    write = "--write" in sys.argv
    wanted = [a for a in sys.argv[1:] if not a.startswith("--")]
    data = json.loads(RECIPES.read_text())
    today = datetime.date.today().isoformat()
    results = {}
    for app in data["apps"]:
        for r in app["recipes"]:
            if wanted and r["id"] not in wanted:
                continue
            adb("shell", "input", "keyevent", "KEYCODE_HOME")
            adb("shell", "pm", "clear", "com.google.android.deskclock")  # no timer or alarm left over from the previous recipe
            for pkg in FRESH:
                adb("shell", "am", "force-stop", pkg)
            adb("logcat", "-c")
            link = filled(r)
            adb("shell", "am", "start", "-n", f"{PKG}/.DebugFire", "--es", "b64", base64.b64encode(link.encode()).decode())
            time.sleep(3.5)
            where = top()
            want = EXPECT.get(r["id"])
            ok, where = check(want) if want else (False, where)
            if "failed" in adb("logcat", "-d", "-s", "DebugFire:W"):
                ok, where = False, "lien refusé par l'appli"
            adb("logcat", "-c")
            results[r["id"]] = (ok, where, link)
            print(("OK  " if ok else "FAIL" if want else "??  "), r["id"], "->", where)
            if SHOTS:
                Path(SHOTS, r["id"] + ".png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))
            if write and ok:
                r["tested"] = today
                r["level"] = (want if isinstance(want, dict) else {}).get("level", "screen")
                RECIPES.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")  # écrit au fil de l'eau : un plantage de l'émulateur ne perd rien
    failed = [i for i, (ok, _, _) in results.items() if not ok]
    print(f"{len(results) - len(failed)}/{len(results)} passent")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()

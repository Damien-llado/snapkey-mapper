#!/usr/bin/env python3
"""Textes à traduire de la bibliothèque, et contrôle d'une traduction.

  python3 tools/recipe_texts.py extract <fichier>   écrit les textes anglais (noms des groupes, titres, descriptions, notes, champs)
  python3 tools/recipe_texts.py check <code> <fichier>   compare la traduction à la source : mêmes clés, rien de vide
  python3 tools/recipe_texts.py install <code> <fichier>   copie la traduction dans app/src/main/assets/recipes/i18n/<code>.json

Forme du fichier : {"apps": {id: {"name"}}, "recipes": {id: {"title", "desc", "note", "vars": {NOM: {"label", "where"}}}}}
Les marques (Spotify, Gmail...) ne se traduisent pas : seuls les groupes d'Android (kind "system") ont un nom à traduire.
"""
import json, shutil, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets/recipes"


def source():
    data = json.loads((ASSETS / "recipes.json").read_text())
    out = {"apps": {}, "recipes": {}}
    for a in data["apps"]:
        if a.get("kind") == "system":
            out["apps"][a["id"]] = {"name": a["name"]}
        for r in a["recipes"]:
            t = {"title": r["title"], "desc": r["desc"]}
            if r.get("note"):
                t["note"] = r["note"]
            if r["vars"]:
                t["vars"] = {v["name"]: {"label": v["label"], "where": v["where"]} for v in r["vars"]}
            out["recipes"][r["id"]] = t
    return out


def problems(src, tr):
    found = []
    for group in ("apps", "recipes"):
        for key, fields in src[group].items():
            got = tr.get(group, {}).get(key)
            if got is None:
                found.append(f"{group}/{key} manquant")
                continue
            for f, v in fields.items():
                if f == "vars":
                    for name, sub in v.items():
                        for g in sub:
                            if not got.get("vars", {}).get(name, {}).get(g, "").strip():
                                found.append(f"{group}/{key}/vars/{name}/{g} vide ou manquant")
                elif not str(got.get(f, "")).strip():
                    found.append(f"{group}/{key}/{f} vide ou manquant")
        for key in tr.get(group, {}):
            if key not in src[group]:
                found.append(f"{group}/{key} en trop")
    return found


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "extract":
        Path(sys.argv[2]).write_text(json.dumps(source(), indent=1, ensure_ascii=False) + "\n")
    elif cmd in ("check", "install"):
        code, path = sys.argv[2], Path(sys.argv[3])
        errs = problems(source(), json.loads(path.read_text()))
        print("\n".join(errs) or "ok")
        if errs:
            sys.exit(1)
        if cmd == "install":
            (ASSETS / "i18n").mkdir(exist_ok=True)
            shutil.copy(path, ASSETS / "i18n" / f"{code}.json")

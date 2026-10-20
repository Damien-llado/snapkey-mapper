#!/usr/bin/env python3
"""Génère les logos de la bibliothèque (VectorDrawable) à partir de recipes.json.

Sources, toutes libres : Simple Icons (CC0, logos de marques en vecteur, couleur officielle),
Lucide (ISC, icônes génériques des fonctions Android), et pour ChatGPT le tracé déjà utilisé
par le site (Lobe Icons, MIT). Usage : python3 tools/logos.py
"""
import json, re, sys, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "app/src/main/assets/recipes/recipes.json"
OUT = ROOT / "app/src/main/res/drawable"
SI = "https://cdn.jsdelivr.net/npm/simple-icons@16.35.0"
LUCIDE = "https://cdn.jsdelivr.net/npm/lucide-static@1.60.0/icons"
ACCENT = "#0A8A63"


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return r.read().decode()


def lucide_paths(svg):
    """Convertit les formes d'une icône Lucide (cercle, rectangle, ligne...) en tracés."""
    out = []
    for tag, attrs in re.findall(r"<(path|circle|rect|line|polyline|polygon|ellipse)\s([^>]*?)/?>", svg):
        a = dict(re.findall(r'([\w-]+)="([^"]*)"', attrs))
        f = lambda k, d=0: float(a.get(k, d))
        if tag == "path":
            out.append(normalize(a["d"]))
        elif tag == "circle":
            cx, cy, r = f("cx"), f("cy"), f("r")
            out.append(f"M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0")
        elif tag == "ellipse":
            cx, cy, rx, ry = f("cx"), f("cy"), f("rx"), f("ry")
            out.append(f"M{cx - rx},{cy}a{rx},{ry} 0 1,0 {2 * rx},0a{rx},{ry} 0 1,0 {-2 * rx},0")
        elif tag == "line":
            out.append(f"M{f('x1')},{f('y1')}L{f('x2')},{f('y2')}")
        elif tag in ("polyline", "polygon"):
            pts = a["points"].split()
            out.append("M" + "L".join(pts) + ("z" if tag == "polygon" else ""))
        elif tag == "rect":
            x, y, w, h = f("x"), f("y"), f("width"), f("height")
            r = min(f("rx", a.get("ry", 0)), w / 2, h / 2)
            out.append(f"M{x + r},{y}h{w - 2 * r}a{r},{r} 0 0,1 {r},{r}v{h - 2 * r}a{r},{r} 0 0,1 {-r},{r}h{-(w - 2 * r)}a{r},{r} 0 0,1 {-r},{-r}v{-(h - 2 * r)}a{r},{r} 0 0,1 {r},{-r}z")
    return out


NUM = re.compile(r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?")
ARGS = {"m": 2, "l": 2, "h": 1, "v": 1, "c": 6, "s": 4, "q": 4, "t": 2, "a": 7, "z": 0}


def normalize(d):
    """Écrit un tracé SVG avec tous les nombres séparés. Android refuse les indicateurs d'arc collés ("a11 11 0 005.6 1.4")
    que les fichiers SVG compacts utilisent (message : « a needs to be followed by a multiple of 7 floats »)."""
    out, i, cmd = [], 0, "m"
    while i < len(d):
        c = d[i]
        if c in " ,\t\n":
            i += 1
        elif c.isalpha():
            cmd = c
            out.append(c)
            i += 1
        else:
            for j in range(ARGS[cmd.lower()]):
                while d[i] in " ,\t\n":
                    i += 1
                if cmd.lower() == "a" and j in (3, 4):  # indicateur : un seul caractère, 0 ou 1
                    out.append(d[i])
                    i += 1
                else:
                    m = NUM.match(d, i)
                    out.append(m.group(0))
                    i = m.end()
    return " ".join(out)


def vector(body):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'
            f'{body}</vector>\n')


def build(spec):
    kind, name = spec.split(":", 1)
    if kind == "lucide":
        paths = lucide_paths(get(f"{LUCIDE}/{name}.svg"))
        body = "".join(
            f'    <path android:pathData="{d}" android:fillColor="#00000000" android:strokeColor="{ACCENT}" '
            f'android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" />\n' for d in paths)
        return vector(body)
    if kind == "si":
        svg = get(f"{SI}/icons/{name}.svg")
        d = normalize(re.search(r'<path d="([^"]+)"', svg).group(1))
        color = next(e["hex"] for e in SI_DATA if e["slug"] == name)
        return vector(f'    <path android:pathData="{d}" android:fillColor="#{color}" />\n')
    if kind == "site":  # tracé déjà présent dans la page du site
        html = (ROOT / "docs/index.html").read_text()
        m = re.search(rf'<symbol id="b-{name}"[^>]*>(.*?)</symbol>', html, re.S)
        d = normalize(re.search(r' d="([^"]+)"', m.group(1)).group(1))
        return vector(f'    <path android:pathData="{d}" android:fillColor="#101010" android:fillType="evenOdd" />\n')
    sys.exit(f"logo inconnu : {spec}")


if __name__ == "__main__":
    SI_DATA = json.loads(get(f"{SI}/data/simple-icons.json"))
    data = json.loads(RECIPES.read_text())
    for app in data["apps"]:
        (OUT / f"logo_{app['id'].replace('-', '_')}.xml").write_text(build(app["logo"]))
        print("logo", app["id"], app["logo"])

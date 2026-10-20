#!/usr/bin/env python3
"""Met dans le manifeste la liste des applis de la bibliothèque (<queries><package>), pour que l'appli sache lesquelles
sont installées sans la permission QUERY_ALL_PACKAGES. À relancer après chaque ajout d'appli dans recipes.json."""
import json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
manifest = ROOT / "app/src/main/AndroidManifest.xml"
apps = json.loads((ROOT / "app/src/main/assets/recipes/recipes.json").read_text())["apps"]
packages = [p for a in apps if a.get("package") for p in [a["package"]] + a.get("also", [])]
lines = "".join(f'        <package android:name="{p}" />\n' for p in packages)
text = manifest.read_text()
block = f"        <!-- recipes:begin (tools/queries.py) -->\n{lines}        <!-- recipes:end -->\n"
if "<!-- recipes:begin" in text:
    text = re.sub(r"        <!-- recipes:begin.*?<!-- recipes:end -->\n", lambda m: block, text, flags=re.S)
else:
    text = text.replace("    </queries>", block + "    </queries>", 1)
manifest.write_text(text)

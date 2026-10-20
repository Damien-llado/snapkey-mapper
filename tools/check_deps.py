#!/usr/bin/env python3
"""Compare chaque dépendance épinglée (tools/deps.json) à sa dernière version et cherche des failles connues (OSV).
À lancer avant chaque release et chaque fois qu'on ajoute une dépendance. Usage : python3 tools/check_deps.py"""
import json, re, subprocess, urllib.request
from pathlib import Path


def get(url, data=None):
    req = urllib.request.Request(url, data=data, headers={"User-Agent": "snapkey-check-deps", "Content-Type": "application/json"})
    return urllib.request.urlopen(req, timeout=30).read().decode()


def stable(versions, major=None):
    ok = [v for v in versions if re.fullmatch(r"\d+(\.\d+)*", v) and (major is None or v.split(".")[0] == major)]
    return max(ok, key=lambda v: [int(x) for x in v.split(".")], default="?")


def latest(d):
    k, n, major = d["kind"], d["name"], d.get("major")
    if k == "npm":
        return json.loads(get(f"https://registry.npmjs.org/{n}/latest"))["version"]
    if k in ("maven", "google-maven"):
        g, a = n.split(":")
        base = "https://repo1.maven.org/maven2" if k == "maven" else "https://dl.google.com/dl/android/maven2"
        return stable(re.findall(r"<version>([^<]+)</version>", get(f"{base}/{g.replace('.', '/')}/{a}/maven-metadata.xml")), major)
    if k == "gradle":
        return stable([v["version"] for v in json.loads(get("https://services.gradle.org/versions/all"))], major)
    if k == "github-tags":  # git ls-remote : pas de limite de l'API GitHub
        out = subprocess.run(["git", "ls-remote", "--tags", f"https://github.com/{n}"], capture_output=True, text=True).stdout
        return stable([m for m in re.findall(r"refs/tags/(\S+?)(?:\^\{\})?$", out, re.M)])


def vulns(d):
    eco = {"npm": "npm", "maven": "Maven", "google-maven": "Maven"}.get(d["kind"])
    if not eco:
        return "non couvert par OSV (voir les avis de sécurité du dépôt)"
    r = json.loads(get("https://api.osv.dev/v1/query", json.dumps({"package": {"name": d["name"], "ecosystem": eco}, "version": d["pinned"]}).encode()))
    return ", ".join(v["id"] for v in r.get("vulns", [])) or "aucune faille connue"


for d in json.loads((Path(__file__).parent / "deps.json").read_text()):
    last = latest(d)
    state = "à jour" if last == d["pinned"] else f"NOUVELLE VERSION {last}"
    print(f"{d['name']:42} {d['pinned']:8} {state:24} {vulns(d)}")

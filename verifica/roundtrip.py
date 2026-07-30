#!/usr/bin/env python3
"""Ogni QR generato dall'encoder Java deve essere riletto da un decoder vero."""
# come si lancia, dalla radice del progetto:
#   javac --release 8 -encoding UTF-8 -d out $(find src -name "*.java")
#   javac --release 8 -encoding UTF-8 -cp out -d verifica/bin verifica/Dump.java verifica/Export.java
#   python3 verifica/roundtrip.py

import subprocess, sys, zxingcpp
from PIL import Image

TEXTS = [
    "TST-0000-00-001", "TST-2026-07-999", "AB0100", "00042", "1234567890123",
    "TST-A04-DEMO-000001", "X1", "HELLO WORLD",
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", "demo-minuscolo-2026",
    "Etichetta con accenti: perche'", "$%*+-./: SPAZI",
    "9" * 120, "Z" * 300, "m" * 700, "7" * 1500,
]
ECC = ["LOW", "MEDIUM", "QUARTILE", "HIGH"]
MASKS = list(range(8)) + [-1]

def render(rows, scale=6, quiet=4):
    n = len(rows) + 2 * quiet
    img = Image.new("L", (n * scale, n * scale), 255)
    px = img.load()
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == "1":
                for dy in range(scale):
                    for dx in range(scale):
                        px[(x + quiet) * scale + dx, (y + quiet) * scale + dy] = 0
    return img

jobs = [(t, e, m) for t in TEXTS for e in ECC for m in MASKS]
stdin = "".join("%s\t%s\t%d\n" % j for j in jobs)
proc = subprocess.run(["java", "-cp", "out:verifica/bin", "Dump"],
                      input=stdin.encode("utf-8"), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
if proc.returncode != 0:
    print(proc.stderr.decode("utf-8", "replace")); sys.exit(2)

blocks, cur = [], None
for line in proc.stdout.decode("utf-8").splitlines():
    if line.startswith("###"):
        _, ver, mode, mask, size = line.split("\t")
        cur = {"version": int(ver), "mode": mode, "rows": []}
        blocks.append(cur)
    else:
        cur["rows"].append(line)

ok = bad = 0
seen_versions, seen_modes = set(), set()
for (text, ecc, mask), got in zip(jobs, blocks):
    res = zxingcpp.read_barcode(render(got["rows"]))
    seen_versions.add(got["version"]); seen_modes.add(got["mode"])
    if res is not None and res.text == text:
        ok += 1
    else:
        bad += 1
        if bad <= 8:
            print("  FAIL %r %s mask%d V%d -> %s"
                  % (text[:20], ecc, mask, got["version"],
                     repr(res.text)[:40] if res else "illeggibile"))

print("riletti correttamente: %d / %d" % (ok, ok + bad))
print("versioni coperte: %s" % sorted(seen_versions))
print("modalita' coperte: %s" % sorted(seen_modes))
sys.exit(1 if bad else 0)

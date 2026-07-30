#!/usr/bin/env python3
"""Confronto modulo-per-modulo tra l'encoder Java e segno."""
# come si lancia, dalla radice del progetto:
#   javac --release 8 -encoding UTF-8 -d out $(find src -name "*.java")
#   javac --release 8 -encoding UTF-8 -cp out -d verifica/bin verifica/Dump.java verifica/Export.java
#   python3 verifica/crosscheck.py

import subprocess, sys, segno

TEXTS = [
    "TST-0000-00-001",
    "TST-2026-07-999",
    "AB0100",
    "00042",
    "1234567890123",
    "TST-A04-DEMO-000001",
    "X1",
    "HELLO WORLD",
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
    "demo-minuscolo-2026",           # forza byte
    "Etichetta con accenti: perché",  # forza byte + UTF-8
    "$%*+-./: SPAZI",
    "9" * 120,
    "Z" * 300,
    "m" * 700,
]
ECC = {"LOW": "l", "MEDIUM": "m", "QUARTILE": "q", "HIGH": "h"}
MODE = {"NUMERIC": "numeric", "ALPHANUMERIC": "alphanumeric", "BYTE": "byte"}

jobs = [(t, e, m) for t in TEXTS for e in ECC for m in range(8)]
stdin = "".join("%s\t%s\t%d\n" % j for j in jobs)

proc = subprocess.run(
    ["java", "-cp", "out:verifica/bin", "Dump"],
    input=stdin.encode("utf-8"), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
if proc.returncode != 0:
    print("Java è morto:\n" + proc.stderr.decode("utf-8", "replace"))
    sys.exit(2)

blocks, cur = [], None
for line in proc.stdout.decode("utf-8").splitlines():
    if line.startswith("###"):
        _, ver, mode, mask, size = line.split("\t")
        cur = {"version": int(ver), "mode": mode, "mask": int(mask), "rows": []}
        blocks.append(cur)
    elif cur is not None:
        cur["rows"].append(line)

assert len(blocks) == len(jobs), "%d blocchi per %d job" % (len(blocks), len(jobs))

ok = bad = 0
failures = []
for (text, ecc, mask), got in zip(jobs, blocks):
    ref = segno.make(text, error=ECC[ecc], mask=mask,
                     mode=MODE[got["mode"]], boost_error=False, micro=False)
    ref_rows = ["".join(str(b) for b in row) for row in ref.matrix]
    if ref.version != got["version"]:
        bad += 1
        failures.append("versione %r %s mask%d: mia V%d, segno V%s"
                        % (text[:22], ecc, mask, got["version"], ref.version))
        continue
    if ref_rows != got["rows"]:
        bad += 1
        diff = sum(1 for r1, r2 in zip(ref_rows, got["rows"])
                   for c1, c2 in zip(r1, r2) if c1 != c2)
        failures.append("matrice %r %s mask%d V%d: %d moduli diversi"
                        % (text[:22], ecc, mask, got["version"], diff))
        continue
    ok += 1

print("matrici identiche: %d / %d" % (ok, ok + bad))
for f in failures[:12]:
    print("  FAIL " + f)

# la scelta automatica della maschera deve coincidere con quella di segno
stdin2 = "".join("%s\t%s\t-1\n" % (t, e) for t in TEXTS for e in ECC)
proc2 = subprocess.run(["java", "-cp", "out:verifica/bin", "Dump"],
                       input=stdin2.encode("utf-8"), stdout=subprocess.PIPE)
auto = [l.split("\t") for l in proc2.stdout.decode("utf-8").splitlines() if l.startswith("###")]
same = diffm = 0
for (t, e), info in zip([(t, e) for t in TEXTS for e in ECC], auto):
    ref = segno.make(t, error=ECC[e], mode=MODE[info[2]], boost_error=False, micro=False)
    if int(info[3]) == ref.mask:
        same += 1
    else:
        diffm += 1
        if diffm <= 5:
            print("  maschera diversa su %r %s: mia %s, segno %s" % (t[:22], e, info[3], ref.mask))
print("maschera scelta uguale a segno: %d / %d" % (same, same + diffm))
sys.exit(1 if bad or diffm else 0)

"""Prueft die Attributtabelle auf Widersprueche, die man am Bild nicht sieht.

Aufruf (liest flaggen.csv aus diesem Ordner, also vorher build-signaturseite.py laufen lassen):
    python pruefe-attribute.py

Zwei Sorten von Pruefung:

  1. Kettenregeln -- eine Spalte muss genau dann gesetzt sein, wenn die Frage davor
     hingefuehrt hat. "Gesetzt" heisst: nicht 'x'. Eine Flagge mit Hintergrundtyp=2
     ohne Kreuzantworten ist ein Widerspruch, egal wie sie aussieht.

  2. Ausreisser -- Einzelgaenger, die sich von einem grossen Cluster in genau einem
     Attribut unterscheiden. Das findet Tippfehler, meldet aber auch viele echte
     Sonderfaelle; die Liste ist zum Durchsehen, nicht zum Abarbeiten.

Die Spalten werden ueber ihre Ueberschrift gefunden, nicht ueber ihre Position --
eingefuegte Spalten brechen also nichts. Kommt ein Zweig dazu, kommt hier eine Zeile
in REGELN dazu.
"""
import collections
import csv
import io
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")

# (Bedingungsspalte, Werte die hinfuehren, Spalte die dann gesetzt sein muss)
REGELN = [
    ("Hintergrundtyp", {"0"}, "W-Streifen"),
    ("W-Streifen", {"3"}, "3W"),
    ("W-Streifen", {"5"}, "5W"),
    ("Hintergrundtyp", {"1"}, "S-Streifen"),
    ("Hintergrundtyp", {"1"}, "S-Anordnung"),
    ("Hintergrundtyp", {"2"}, "Kreuzausrichtung"),
    ("Hintergrundtyp", {"2"}, "Kreuzarme"),
    ("Hintergrundtyp", {"3"}, "Diagonal Richtung"),
    ("Hintergrundtyp", {"3"}, "Diagonal Anzahl Streifen"),
    ("Hintergrundtyp", {"5"}, "SW Streifen"),
    ("Hintergrundtyp", {"7"}, "Spezial"),
    ("Dreieck von links?", {"1", "2", "3", "4", "5"}, "Dreiecksflächen"),
]

HIER = Path(__file__).resolve().parent
rows = list(csv.reader(io.open(HIER / "flaggen.csv", encoding="utf-8")))
HDR = next(i for i, r in enumerate(rows) if len(r) > 7 and r[7].strip() == "Signatur")
ATTR_START = 9
N_ATTR = len(rows[HDR + 2][7].split("|"))
NAME = [rows[HDR][i].strip() for i in range(ATTR_START, ATTR_START + N_ATTR)]
SPALTE = {n: i for i, n in enumerate(NAME)}

ZEILEN = [r for r in rows[HDR + 1:]
          if len(r) > ATTR_START + N_ATTR - 1 and r[2].strip()
          and (r[0].startswith("..") or r[0].startswith("http"))]

FLAGS = [(r[2].strip().replace("_", " "), [r[i].strip() for i in range(ATTR_START, ATTR_START + N_ATTR)])
         for r in ZEILEN]

print(f"{len(FLAGS)} Flaggen, {N_ATTR} Attribute")
print()

# ---- 1. Kettenregeln -------------------------------------------------------
fehler_gesamt = 0
for bedingung, werte, folge in REGELN:
    if bedingung not in SPALTE or folge not in SPALTE:
        print(f"  ! Regel uebersprungen, Spalte fehlt: {bedingung} -> {folge}")
        continue
    b, f = SPALTE[bedingung], SPALTE[folge]
    fehler = []
    for land, sig in FLAGS:
        hinfuehrend = sig[b] in werte
        gesetzt = sig[f] != "x"
        if hinfuehrend != gesetzt:
            fehler.append(f"{land} ({folge}={sig[f] or 'leer'}, {bedingung}={sig[b]})")
    fehler_gesamt += len(fehler)
    kopf = f"{bedingung}={'/'.join(sorted(werte))} -> {folge}"
    print(f"  {kopf:<48} {'ok' if not fehler else str(len(fehler)) + ' Abweichung(en)'}")
    for x in fehler:
        print(f"        {x}")

print()
print("Widersprueche gesamt:", fehler_gesamt)

# ---- 2. Ausreisser ---------------------------------------------------------
gruppen = collections.defaultdict(list)
for land, sig in FLAGS:
    gruppen[tuple(sig)].append(land)
gross = [(s, m) for s, m in gruppen.items() if len(m) >= 3]
einzeln = sorted((m[0], s) for s, m in gruppen.items() if len(m) == 1)

print()
print(f"Einzelgaenger mit genau einer Abweichung von einem Cluster (>=3) -- oft echte Sonderfaelle:")
for land, sig in einzeln:
    for gsig, gm in gross:
        abw = [i for i in range(N_ATTR) if sig[i] != gsig[i]]
        if len(abw) == 1:
            i = abw[0]
            print(f"     {land:<24} {NAME[i]:<20} {sig[i]:<4} statt {gsig[i]:<4} "
                  f"(Cluster von {len(gm)}, z.B. {gm[0]})")
            break

# ---- 3. Sondertests --------------------------------------------------------
# Was sich nicht als Kettenregel schreiben laesst: mehrere Bedingungen auf einmal,
# oder eine Spalte hinter der Signatur (die Elementspalten liegen dort).
ROH = {c.strip(): i for i, c in enumerate(rows[HDR]) if c.strip()}


def roh(zeile, name):
    i = ROH.get(name)
    return zeile[i].strip() if i is not None and len(zeile) > i else ""


# Eine einfarbige Flaeche ohne Goesch, ohne Dreieck und ohne Element waere leer.
leer = [z[2].strip().replace("_", " ") for z in ZEILEN
        if roh(z, "Hintergrundtyp") == "4"
        and roh(z, "Gösch?") == "0"
        and roh(z, "Dreieck von links?") == "0"
        and roh(z, "Zusatzelemente") in ("0", "", "x")]

print()
print("Einfarbig und voellig ohne Inhalt (gibt es nicht):", ", ".join(leer) if leer else "ok")

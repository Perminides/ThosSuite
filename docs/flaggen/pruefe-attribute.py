"""Prueft beide Attributblaetter auf Widersprueche, die man am Bild nicht sieht.

Aufruf (liest flaggen.csv und zusatzelemente.csv aus diesem Ordner, also vorher
build-signaturseite.py laufen lassen):
    python pruefe-attribute.py

Vier Sorten von Pruefung:

  1. Kettenregeln Hintergrund -- eine Spalte muss genau dann gesetzt sein, wenn die
     Frage davor hingefuehrt hat. "Gesetzt" heisst: nicht 'x' und nicht leer. Eine
     Flagge mit Hintergrundtyp=2 ohne Kreuzantworten ist ein Widerspruch, egal wie
     sie aussieht.

  2. Ausreisser -- Einzelgaenger, die sich von einem grossen Cluster in genau einem
     Attribut unterscheiden. Das findet Tippfehler, meldet aber auch viele echte
     Sonderfaelle; die Liste ist zum Durchsehen, nicht zum Abarbeiten.

  3. Sondertests -- was sich nicht als Kettenregel schreiben laesst.

  4. Kettenregeln Zusatzelemente, plus ein Abgleich beider Blaetter gegeneinander.
     Dort sind die Bedingungen Ausdruecke statt Wertelisten: Der Kreis erbt die
     Folgefragen des Aussenbereichs, und das ist kein Paar (Spalte, Werte) mehr.

Die Spalten werden ueberall ueber ihre Ueberschrift gefunden, nie ueber ihre Position
-- eingefuegte Spalten brechen also nichts. Kommt ein Zweig dazu, kommt hier eine
Zeile in REGELN oder ZE_TESTS dazu.

Achtung beim Regelschreiben: Eine Abweichung ist zuerst ein Verdacht gegen die Regel,
nicht gegen die Daten. Ein 'Stern' in "Welche Himmelskoerper?" steckt auch in
"1 Stern" und "Mond und Stern" -- die Anzahlfrage haengt aber am Plural.
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

def gesetzt(wert):
    """Eine Spalte traegt einen Wert -- 'x' und leer heissen beide: hier wird nicht gefragt."""
    return wert.strip() not in ("", "x")


def pruefe(titel, zeilen, tests):
    """zeilen: (Land, wert(spaltenname)). tests: (Beschreibung, Bedingung, Folgespalte)."""
    print(titel)
    fehler_gesamt = 0
    for beschreibung, bedingung, folge in tests:
        fehler = []
        for land, wert in zeilen:
            if bedingung(wert) != gesetzt(wert(folge)):
                fehler.append(f"{land} ({folge}={wert(folge) or 'leer'})")
        fehler_gesamt += len(fehler)
        kopf = f"{beschreibung} -> {folge}"
        print(f"  {kopf:<62} {'ok' if not fehler else str(len(fehler)) + ' Abweichung(en)'}")
        for x in fehler:
            print(f"        {x}")
    print(f"  Widersprueche: {fehler_gesamt}")
    print()
    return fehler_gesamt


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

# ---- 1. Kettenregeln Hintergrund -------------------------------------------
HG_ZEILEN = [(land, lambda n, s=sig: s[SPALTE[n]] if n in SPALTE else "") for land, sig in FLAGS]
HG_TESTS = [(f"{b}={'/'.join(sorted(w))}", (lambda n, w=w: lambda wert: wert(n) in w)(b), f)
            for b, w, f in REGELN]
fehler_gesamt = pruefe("Hintergrund-Blatt", HG_ZEILEN, HG_TESTS)

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

# ---- 4. Kettenregeln Zusatzelemente ----------------------------------------
# Zweites Blatt, eine Zeile je Flagge mit Element. Die Spalten werden ueber ihre
# Ueberschrift gefunden, die Bedingungen sind Ausdruecke statt Wertelisten -- der
# Kreis erbt die Folgefragen des Aussenbereichs, das laesst sich nicht als Paar
# (Spalte, Werte) schreiben.
ZE_ROWS = list(csv.reader(io.open(HIER / "zusatzelemente.csv", encoding="utf-8")))
ZE_HDR = {c.strip(): i for i, c in enumerate(ZE_ROWS[0]) if c.strip()}
ZE_ZEILEN = [(z[2].strip().replace("_", " "),
              lambda n, z=z: z[ZE_HDR[n]].strip() if n in ZE_HDR and len(z) > ZE_HDR[n] else "")
             for z in ZE_ROWS[1:] if len(z) > 2 and z[2].strip()]

WEICHE = "Zusatzelemente außerhalb des Gösch"
# Nur der Singular fuehrt zur Kategoriefrage. "Mehrere einfarbige Figuren" ist eine
# Endstation wie "Komplexes Emblem" -- Grenadas Sterne und Muskatnuss haetten keine
# gemeinsame Kategorie.
EINE_FIGUR = "Nur eine einfarbige Figur (Umrandung zählt nicht)"
# Kein Inhalt, der eine eigene Farbe braeuchte: leer, nicht zerlegt, oder der Kreis
# selbst zweigeteilt (dann traegt er zwei Farben statt einer Figur).
KREIS_OHNE_INHALT = ("Nichts", "Komplexes Emblem", "Der Kreis ist zweigeteilt")

ZE_TESTS = [
    ("Weiche=Kreis",
     lambda w: w(WEICHE).startswith("Klar farbig abgegrenzter Kreis"), "Kreisstruktur"),
    ("Himmelskoerper aussen oder im Kreis",
     lambda w: "Nur Himmelskörper" in (w(WEICHE), w("Kreisstruktur")), "Welche Himmelskörper?"),
    ("mehrere Sterne (Plural)",
     lambda w: "Sterne" in w("Welche Himmelskörper?"), "Wie viel Sterne?"),
    ("Mond und Stern zusammen",
     lambda w: "Mond" in w("Welche Himmelskörper?") and "Stern" in w("Welche Himmelskörper?"),
     "Formation Mond mit Stern(en)"),
    ("genau eine einfarbige Figur, aussen oder im Kreis",
     lambda w: EINE_FIGUR in (w(WEICHE), w("Kreisstruktur")), "Einfarbige Figur Kategorie"),
    ("Weiche != Keine",
     lambda w: w(WEICHE) != "Keine", "Wo?"),
    ("Weiche traegt eine Form (nicht Keine, nicht komplex)",
     lambda w: w(WEICHE) not in ("Keine", "Komplexes Emblem"), "Farbe (Kreis bzw. Zusatzelement)"),
    ("Kreis hat einen faerbbaren Inhalt",
     lambda w: gesetzt(w("Kreisstruktur")) and w("Kreisstruktur") not in KREIS_OHNE_INHALT,
     "Farbe Zusatzelement im Kreis"),
    ("Goesch hat einen Inhalt",
     lambda w: gesetzt(w("Gösch")), "Farbe Zusatzelement im Gösch"),
]

print()
fehler_gesamt += pruefe(f"Zusatzelemente-Blatt ({len(ZE_ZEILEN)} Zeilen)", ZE_ZEILEN, ZE_TESTS)

# ---- 5. Beide Blaetter gegeneinander ---------------------------------------
IM_BLATT = {land for land, _ in ZE_ZEILEN}
haupt = {z[2].strip().replace("_", " "): z for z in ZEILEN}

fehlt = [l for l, z in haupt.items() if roh(z, "Zusatzelemente") == "1" and l not in IM_BLATT]
zuviel = [l for l in IM_BLATT if l in haupt and roh(haupt[l], "Zusatzelemente") != "1"]
print("Hauptblatt sagt Element, Elementzeile fehlt:", ", ".join(sorted(fehlt)) if fehlt else "ok")
print("Elementzeile da, Hauptblatt sagt kein Element:", ", ".join(sorted(zuviel)) if zuviel else "ok")

goesch_haupt = {l for l, z in haupt.items() if roh(z, "Gösch?") == "1"}
goesch_ze = {land for land, w in ZE_ZEILEN if gesetzt(w("Gösch"))}
print("Gösch nur im Hauptblatt:", ", ".join(sorted(goesch_haupt - goesch_ze)) or "ok")
print("Gösch nur im Elementblatt:", ", ".join(sorted(goesch_ze - goesch_haupt)) or "ok")

print()
print("Widersprueche gesamt:", fehler_gesamt)

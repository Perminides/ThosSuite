"""Prueft das Systematik-Blatt auf Widersprueche, die man am Bild nicht sieht.

Aufruf (holt das Blatt selbst, schreibt systematik.csv daneben):
    python pruefe-attribute.py
    python pruefe-attribute.py --offline     # nimmt die vorhandene systematik.csv

Vier Sorten von Pruefung:

  1. Kettenregeln Hintergrund -- eine Spalte muss GENAU DANN einen Wert tragen, wenn
     die Frage davor hierher gefuehrt hat. "Gesetzt" heisst: nicht 'x' und nicht leer.
     Eine Flagge mit Hintergrundtyp=2 ohne Kreuzantworten ist ein Widerspruch, egal
     wie sie aussieht.

  2. Kettenregeln Zusatzelemente -- dieselbe Idee rechts im Blatt. Dort sind die
     Bedingungen Ausdruecke statt Wertelisten: Der Kreis erbt die Folgefragen des
     Aussenbereichs, und das ist kein Paar (Spalte, Werte) mehr.

  3. Invarianten -- was frueher ein Abgleich zwischen zwei Blaettern war und seit
     dem Zusammenlegen innerhalb einer Zeile steht.

  4. Ausreisser -- Einzelgaenger, die sich von einem grossen Cluster in genau einem
     Attribut unterscheiden. Findet Tippfehler, meldet aber auch viele echte
     Sonderfaelle; die Liste ist zum Durchsehen, nicht zum Abarbeiten.

Spalten werden nirgends ueber ihre Position gelesen -- siehe sheet.py. Kommt ein
Zweig dazu, kommt hier eine Zeile in HG_REGELN oder ZE_TESTS dazu.

Achtung beim Regelschreiben: Eine Abweichung ist zuerst ein Verdacht gegen die
Regel, nicht gegen die Daten. Ein 'Stern' in "Welche Himmelskoerper?" steckt auch
in "1 Stern" und "Mond und Stern" -- die Anzahlfrage haengt aber am Plural.
"""
import collections
import sys

from sheet import lade

sys.stdout.reconfigure(encoding="utf-8")

# --- Hintergrund: (Bedingungsspalte, Werte die hinfuehren, Spalte die dann gesetzt sein muss)
HG_REGELN = [
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
    ("Dreieck von links?", {"1", "2", "3", "4", "5"},
     "Die Dreiecksform(en) bestehen aus wie vielen Farben?"),
]

WEICHE = "Zusatzelemente außerhalb des Gösch"
# Die langen Blattwerte werden ueber ihren Anfang erkannt, nicht zeichengenau: Sie
# werden gelegentlich praezisiert (am 26.08.2026 bekam die Mehrzahl-Figur denselben
# Zusatz "(Umrandung zaehlt nicht)" wie die Einzahl), und daran soll keine Regel
# zerbrechen.
EINE_FIGUR = "Nur eine einfarbige Figur"
# Kein Inhalt, der eine eigene Farbe braeuchte: leer, nicht zerlegt, oder der Kreis
# selbst zweigeteilt (dann traegt er zwei Farben statt einer Figur).
KREIS_OHNE_INHALT = ("Nichts", "Anders", "Komplexes Emblem", "Der Kreis ist zweigeteilt")


def gesetzt(wert):
    """'x' und leer heissen beide: hier wird nicht gefragt."""
    return wert.strip() not in ("", "x")


# --- Zusatzelemente: (Beschreibung, Bedingung, Spalte die dann gesetzt sein muss)
# Die Weiche traegt bei jeder Flagge einen Wert, notfalls "Keine" -- deshalb duerfen
# die Bedingungen hier direkt auf ihm arbeiten und muessen kein 'x' abfangen.
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
     lambda w: any(v.startswith(EINE_FIGUR) for v in (w(WEICHE), w("Kreisstruktur"))),
     "Einfarbige Figur Kategorie"),
    ("etwas liegt ausserhalb",
     lambda w: w(WEICHE) != "Keine", "Wo?"),
    ("etwas ausserhalb, das eine Form hat",
     lambda w: w(WEICHE) not in ("Keine", "Anders"), "Farbe (Kreis bzw. Zusatzelement)"),
    ("Kreis hat einen faerbbaren Inhalt",
     lambda w: gesetzt(w("Kreisstruktur")) and w("Kreisstruktur") not in KREIS_OHNE_INHALT,
     "Farbe Zusatzelement im Kreis"),
    # "Anders" ist auch im Goesch eine Endstation: nicht zerlegt, also nichts zu faerben --
    # dieselbe Regel wie ausserhalb.
    ("Goesch hat einen faerbbaren Inhalt",
     lambda w: gesetzt(w("Gösch")) and w("Gösch") != "Anders", "Farbe Zusatzelement im Gösch"),
]


def pruefe(titel, zeilen, tests):
    """zeilen: (Land, wert(spaltenname)). tests: (Beschreibung, Bedingung, Folgespalte)."""
    print(titel)
    fehler_gesamt = 0
    for beschreibung, bedingung, folge in tests:
        fehler = [f"{land} ({folge}={wert(folge) or 'leer'})"
                  for land, wert in zeilen if bedingung(wert) != gesetzt(wert(folge))]
        fehler_gesamt += len(fehler)
        kopf = f"{beschreibung} -> {folge}"
        print(f"  {kopf:<62} {'ok' if not fehler else str(len(fehler)) + ' Abweichung(en)'}")
        for x in fehler:
            print(f"        {x}")
    print(f"  Widersprueche: {fehler_gesamt}")
    print()
    return fehler_gesamt


blatt = lade(neu_holen="--offline" not in sys.argv)
ZEILEN = [(blatt.land(z), (lambda n, z=z: blatt.spalte(z, n))) for z in blatt.zeilen]

print(f"{len(ZEILEN)} Flaggen, {blatt.n_attr} Attribute "
      f"(Kopfzeile {blatt.hdr}, Attributblock ab Spalte {blatt.attr_start})")
if blatt.ohne_signatur:
    print("  ! Zeile ohne Signatur, wird nicht geprueft:", ", ".join(blatt.ohne_signatur))
if blatt.ohne_bild:
    print("  ! noch ohne Bildpfad:", ", ".join(sorted(blatt.ohne_bild)))
print()

# ---- 1. Kettenregeln Hintergrund -------------------------------------------
HG_TESTS = [(f"{b}={'/'.join(sorted(w))}", (lambda n, w=w: lambda wert: wert(n) in w)(b), f)
            for b, w, f in HG_REGELN]
fehler_gesamt = pruefe("Hintergrund", ZEILEN, HG_TESTS)

# ---- 2. Kettenregeln Zusatzelemente ----------------------------------------
fehler_gesamt += pruefe("Zusatzelemente", ZEILEN, ZE_TESTS)

# ---- 3. Invarianten ---------------------------------------------------------
# Frueher der Abgleich zweier Blaetter, seit dem Zusammenlegen eine Zeilenprueflung.
print("Invarianten")


def melde(text, treffer):
    global fehler_gesamt
    fehler_gesamt += len(treffer)
    print(f"  {text:<62} {'ok' if not treffer else ', '.join(sorted(treffer))}")


melde("Attribut Gösch?=1  <->  Gösch-Inhalt gesetzt",
      [land for land, v in ZEILEN if (v("Gösch?") == "1") != gesetzt(v("Gösch"))])
# Die Weiche wird bei JEDER Flagge beantwortet, notfalls mit "Keine". Traege sie
# irgendwo 'x', taeuchte die Elementfrage nur bei Flaggen mit Element auf -- und wer
# dann bei Frage 3 "kein Goesch" geantwortet hat, koennte "Keine" ausschliessen,
# ohne die Flagge zu kennen. Genau dieser Leak ist am 26.08.2026 geschlossen worden.
melde("die Weiche traegt überall einen Wert (nie 'x')",
      [land for land, v in ZEILEN if not gesetzt(v(WEICHE))])
# Eine einfarbige Flaeche ohne Goesch, ohne Dreieck und ohne Element waere leer.
melde("einfarbig, ohne Gösch, ohne Dreieck, ohne Element (gibt es nicht)",
      [land for land, v in ZEILEN if v("Hintergrundtyp") == "4" and v("Gösch?") == "0"
       and v("Dreieck von links?") == "0" and v(WEICHE) == "Keine"])
print()

# ---- 4. Ausreisser ---------------------------------------------------------
gruppen = collections.defaultdict(list)
for z in blatt.zeilen:
    gruppen[tuple(blatt.attribute(z))].append(blatt.land(z))
gross = [(s, m) for s, m in gruppen.items() if len(m) >= 3]
einzeln = sorted((m[0], s) for s, m in gruppen.items() if len(m) == 1)

print("Einzelgaenger mit genau einer Abweichung von einem Cluster (>=3) -- oft echte Sonderfaelle:")
for land, sig in einzeln:
    for gsig, gm in gross:
        abw = [i for i in range(blatt.n_attr) if sig[i] != gsig[i]]
        if len(abw) == 1:
            i = abw[0]
            print(f"     {land:<24} {blatt.name[i]:<26} {sig[i]:<4} statt {gsig[i]:<4} "
                  f"(Cluster von {len(gm)}, z.B. {gm[0]})")
            break

print()
print("Widersprueche gesamt:", fehler_gesamt)

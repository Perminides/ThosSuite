"""Erzeugt die Hintergruende, die in keinen Zweig passen -- die Spezialflaggen.

Aufruf:
    python build-spezial-sketch.py <zielordner> spezial-1 [spezial-2 ...]

Die Nummer ist der Wert der Spalte `Spezial` im Blatt, nicht eine laufende Zahl. Tragen zwei
Laender denselben Wert, teilen sie sich eine Datei -- Bahrain und Katar tun das.

`spezial-0` (Antigua und Barbuda) ist von Hand entstanden und bleibt es; hier wachsen die neuen.

Konvention (siehe Flaggen-Deck.md):
  * Leinwand IMMER 180 x 120, x von 0 bis 180, y von -120 bis 0. Der Leser invertiert.
  * In properties steht nur die Flaechennummer, nullbasiert.
  * Die Flaechen stossen Punkt auf Punkt aneinander -- gemeinsame Kanten tragen dieselben
    Koordinaten, sonst blitzt beim Fuellen der Hintergrund durch.
"""
import json
import sys
from pathlib import Path

NL = chr(10)

BREITE, HOEHE = 180.0, 120.0
STELLEN = 3


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), STELLEN)


def punkte(folge):
    return [[runde(x), runde(-y)] for x, y in folge]


def flaeche(nummer, *teile):
    """Eine Flaeche aus einem oder mehreren getrennten Stuecken."""
    ringe = [punkte(folge) for folge in teile]
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[r + [r[0]]] for r in ringe]}}


def zackenkante(grund, spitze, anzahl):
    """Die Zackenlinie von oben nach unten: Grundkante, Spitze, Grundkante, ...

    Jede Spitze sitzt in der Mitte ihres Abschnitts, oben und unten also symmetrisch -- die Reihe
    hat keinen sichtbaren Anfang. Bei ungerader Anzahl liegt eine Spitze genau auf der Mitte.
    """
    hoehe = HOEHE / anzahl
    folge = [(grund, 0.0)]
    for i in range(anzahl):
        folge.append((spitze, (i + 0.5) * hoehe))
        folge.append((grund, (i + 1) * hoehe))
    return folge


# --- die Spezialhintergruende ------------------------------------------------
def gezackt():
    """Bahrain und Katar: ein Band an der Mastseite, zur Flugseite hin gezackt.

    Sieben Zacken, obwohl Bahrain fuenf und Katar neun hat. Die Zahl ist nie gefragt worden, also
    darf die Skizze sie nicht zeigen -- dieselbe Auflage wie beim kanonischen Stern. Die Mitte
    zwischen beiden echten Zahlen ist keiner der beiden Flaggen naeher als der anderen.

    Die Grundkante liegt auf dem Rasterdrittel: eine Zahl, die es schon gibt, statt einer neuen.
    """
    kante = zackenkante(BREITE / 3, 78.0, 7)
    mast = [(0.0, 0.0)] + kante + [(0.0, HOEHE)]
    flug = [(BREITE, 0.0)] + kante + [(BREITE, HOEHE)]
    return [flaeche(0, mast), flaeche(1, flug)]


def bosnisches_dreieck():
    """Bosnien und Herzegowina: ein gelbes Dreieck auf blauem Grund, ohne die Sterne.

    Die Ecken stehen so in der Flaggen-SVG (viewBox 16 x 8): 4,24 und 12,24 auf der Oberkante,
    die senkrechte Seite hinunter bis zur Unterkante. Auf 180 uebertragen sind das 47,7 und
    137,7 -- die Grundseite bleibt damit exakt 90, also die halbe Breite.

    Das Blau ist **zwei Stuecke**: Links und rechts des Dreiecks haengen sie nur an dessen unterer
    Ecke zusammen. Ein einziger Ring muesste sich dort selbst beruehren -- zwei Stuecke sagen
    dasselbe, ohne zu schummeln.

    Die neun Sterne laengs der Schraege sind ein eigenes Element im Blatt und gehoeren nicht hierher.
    """
    links, rechts = BREITE * 4.24 / 16, BREITE * 12.24 / 16
    return [flaeche(0, [(0, 0), (links, 0), (rechts, HOEHE), (0, HOEHE)],
                       [(rechts, 0), (BREITE, 0), (BREITE, HOEHE), (rechts, HOEHE)]),
            flaeche(1, [(links, 0), (rechts, 0), (rechts, HOEHE)])]


SPEZIAL = {"spezial-1": gezackt, "spezial-2": bosnisches_dreieck}


def schreibe(zielordner, name):
    if name not in SPEZIAL:
        raise SystemExit("Kein Bauplan fuer %s -- bekannt sind: %s" % (name, ", ".join(sorted(SPEZIAL))))
    features = [json.dumps(f) for f in SPEZIAL[name]()]
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"features": [']
    text = NL.join(kopf) + NL + ("," + NL).join(features) + NL + "]" + NL + "}" + NL

    ziel = Path(zielordner) / (name + ".geojson")
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    print("%s  (%d x %d, %d Flaechen)" % (ziel, BREITE, HOEHE, len(features)))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    for name in sys.argv[2:]:
        schreibe(sys.argv[1], name)

"""Erzeugt die Hintergruende des Kreuz-Zweigs.

Aufruf:
    python build-kreuz-sketch.py <zielordner> kreuz-diagonal-uni ...

Der Name ist `kreuz-<ausrichtung>-<arme>`, genau wie der Generator ihn ableitet: `senkrecht`,
`diagonal` oder `beides`, dahinter `uni`, `dreifarbig`, `fimbriert` oder `unsichtbar`.

Gebaut ist bisher `diagonal-uni`. `senkrecht-uni` liegt als handgemachte Datei daneben und bleibt
es; die uebrigen Armformen sind eigene Geometrie und kommen, wenn die erste Flagge sie braucht.

Konvention (siehe Flaggen-Deck.md und die vorhandene Datei kreuz-senkrecht-uni):
  * Leinwand IMMER 180 x 120, x von 0 bis 180, y von -120 bis 0.
  * Erst die vier Felder, dann das Kreuz als letzte Flaeche. Daenemark steht im Blatt mit
    `Rot|Rot|Rot|Rot|Weiss` -- vier Felder, dann der Balken.
  * Die Felder laufen im Uhrzeigersinn, beginnend mit dem obersten.
  * Die Arme sind 20 breit, senkrecht gemessen -- dieselbe Zahl wie beim senkrechten Kreuz.
"""
import json
import sys
from pathlib import Path

NL = chr(10)

BREITE, HOEHE = 180.0, 120.0
ARM = 20.0                  # Breite der Arme, senkrecht gemessen
STELLEN = 3


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), STELLEN)


def flaeche(nummer, punkte):
    """Ein Ring; gerechnet wird in Tiefe von oben, geschrieben mit negativem y."""
    ring = [[runde(x), runde(-h)] for x, h in punkte]
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[ring + [ring[0]]]]}}


def andreaskreuz():
    """Zwei Baender von Ecke zu Ecke, dazu die vier Dreiecke dazwischen.

    Die Baender liegen mittig auf den Eckdiagonalen. Weil die Diagonale genau durch die Ecken
    laeuft, gehoeren die vier Leinwandecken zum Kreuz -- das Band ist dort halb abgeschnitten.

    Alle Punkte folgen aus zwei Zahlen: der Steigung und der halben Armbreite. `rand` ist, wo ein
    Bandrand die Ober- oder Unterkante trifft, `spitze` die Spitze eines Dreiecks.
    """
    m, halb = HOEHE / BREITE, ARM / 2
    rand = halb / m                                  # 15
    fern = BREITE - rand                             # 165
    quer = (HOEHE - 2 * halb) / (2 * m)              # 75, Spitze des linken Dreiecks
    weit = BREITE - quer                             # 105, Spitze des rechten
    mitte = HOEHE / 2

    oben = [(rand, 0), (fern, 0), (BREITE / 2, mitte - halb)]
    unten = [(fern, HOEHE), (rand, HOEHE), (BREITE / 2, mitte + halb)]
    rechts = [(BREITE, halb), (weit, mitte), (BREITE, HOEHE - halb)]
    links = [(0, HOEHE - halb), (quer, mitte), (0, halb)]

    kreuz = [(0, 0), (rand, 0), (BREITE / 2, mitte - halb), (fern, 0), (BREITE, 0),
             (BREITE, halb), (weit, mitte), (BREITE, HOEHE - halb), (BREITE, HOEHE),
             (fern, HOEHE), (BREITE / 2, mitte + halb), (rand, HOEHE), (0, HOEHE),
             (0, HOEHE - halb), (quer, mitte), (0, halb)]
    return [flaeche(0, oben), flaeche(1, rechts), flaeche(2, unten), flaeche(3, links),
            flaeche(4, kreuz)]


KREUZE = {"kreuz-diagonal-uni": andreaskreuz}


def schreibe(zielordner, name):
    if name not in KREUZE:
        raise SystemExit("Kein Bauplan fuer %s -- bekannt sind: %s" % (name, ", ".join(sorted(KREUZE))))
    features = [json.dumps(f) for f in KREUZE[name]()]
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"xy_coordinate_resolution": 1,',
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

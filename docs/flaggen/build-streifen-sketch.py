"""Erzeugt Strukturdateien fuer reine Streifen-Skizzen.

Aufruf:
    python build-streifen-sketch.py <zielordner> waagerecht-3 senkrecht-4 ...

Konvention (siehe Flaggen-Deck.md):
  * Das Seitenverhaeltnis ist immer 3:2 -- die Skizze wird in ein 3:2-Feld eingepasst,
    ein anderes Verhaeltnis wuerde gestaucht.
  * Die Groesse ist frei. Gewaehlt wird die kleinste gerade Hoehe ab 120, die durch die
    Streifenzahl teilbar ist; damit bleiben alle Koordinaten ganzzahlig.
  * Y ist negativ, wie in den Kartendateien -- der Leser invertiert beim Einlesen.
  * In properties steht nur die Flaechennummer, nullbasiert; bei waagerecht von oben,
    bei senkrecht von links.
"""
import json
import sys
from pathlib import Path

NL = chr(10)


def masse(anzahl):
    hoehe = anzahl
    while hoehe < 120 or hoehe % 2 != 0:
        hoehe += anzahl
    return int(hoehe * 3 / 2), hoehe


def ring(x0, y0, x1, y1):
    return [[x0, y0], [x1, y0], [x1, y1], [x0, y1], [x0, y0]]


def flaechen(richtung, anzahl):
    breite, hoehe = masse(anzahl)
    for i in range(anzahl):
        if richtung == "waagerecht":
            r = ring(0, -(i * hoehe // anzahl), breite, -((i + 1) * hoehe // anzahl))
        else:
            r = ring(i * breite // anzahl, 0, (i + 1) * breite // anzahl, -hoehe)
        yield {"type": "Feature",
               "properties": {"id": i},
               "geometry": {"type": "MultiPolygon", "coordinates": [[r]]}}


def schreibe(zielordner, name):
    richtung, anzahl = name.rsplit("-", 1)
    if richtung not in ("waagerecht", "senkrecht"):
        raise SystemExit("Nur waagerecht-<n> oder senkrecht-<n>, nicht: " + name)

    # Ein Feature je Zeile, wie in den Kartendateien -- so bleibt eine Aenderung im Diff lesbar.
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"xy_coordinate_resolution": 1,',
            '"features": [']
    features = [json.dumps(f) for f in flaechen(richtung, int(anzahl))]
    text = NL.join(kopf) + NL + ("," + NL).join(features) + NL + "]" + NL + "}" + NL

    ziel = Path(zielordner) / (name + ".geojson")
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    breite, hoehe = masse(int(anzahl))
    print("%s  (%d x %d, %s Flaechen)" % (ziel, breite, hoehe, anzahl))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    for name in sys.argv[2:]:
        schreibe(sys.argv[1], name)

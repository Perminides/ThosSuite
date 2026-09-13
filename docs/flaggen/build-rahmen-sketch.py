"""Erzeugt den Rahmen als Leinwand-Overlay.

Aufruf:
    python build-rahmen-sketch.py <elementordner>

Der Rahmen wird behandelt wie Goesch und Dreieck: eine eigene Datei im Elementordner, aber in
Leinwandkoordinaten (180 x 120, y von -120 bis 0) statt um den Nullpunkt zentriert. Der Generator
legt sie mit Feld -1 auf und faerbt sie aus der Spalte `Rahmen Farbe`.

Eine einzige Flaeche: das Band zwischen Leinwandrand und innerem Rechteck, als Ring mit Loch.
Breite 10. SketchPane rechnet die Skizze um zwei Strichbreiten zu gross, der aeusserste Streifen
verschwindet also im Bildrahmen; bei 10 bleibt davon genug stehen. Die 10 gibt es im System schon.
"""
import json
import sys
from pathlib import Path

NL = chr(10)

BREITE, HOEHE = 180.0, 120.0
BAND = 10.0


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Datei lesbar bleibt."""
    return int(wert) if float(wert).is_integer() else round(float(wert), 3)


def ring(x0, h0, x1, h1, gegenlaeufig=False):
    """Ein geschlossenes Rechteck; gerechnet in Tiefe von oben, geschrieben mit negativem y."""
    punkte = [(x0, h0), (x1, h0), (x1, h1), (x0, h1)]
    if gegenlaeufig:
        punkte.reverse()
    r = [[runde(x), runde(-h)] for x, h in punkte]
    return r + [r[0]]


def rahmen():
    aussen = ring(0, 0, BREITE, HOEHE)
    innen = ring(BAND, BAND, BREITE - BAND, HOEHE - BAND, gegenlaeufig=True)
    return [{"type": "Feature", "properties": {"id": 0},
             "geometry": {"type": "MultiPolygon", "coordinates": [[aussen, innen]]}}]


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    features = [json.dumps(f) for f in rahmen()]
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "rahmen",',
            '"comment": "Leinwand-Overlay wie Goesch und Dreieck: Koordinaten der ganzen Leinwand, aufgelegt mit Feld -1.",',
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"features": [']
    text = NL.join(kopf) + NL + ("," + NL).join(features) + NL + "]" + NL + "}" + NL
    ziel = Path(sys.argv[1]) / "rahmen.geojson"
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    print("%s  (Band %g, 1 Flaeche)" % (ziel, BAND))

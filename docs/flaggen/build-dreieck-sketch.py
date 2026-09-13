"""Erzeugt die Dreiecke von links als Leinwand-Overlay.

Aufruf:
    python build-dreieck-sketch.py <elementordner> dreieck-3-4 [dreieck-1 ...]

Der Name ist `dreieck-<form>` oder `dreieck-<form>-<farbanzahl>`, genau wie der Generator ihn
ableitet. Die Form ist der Wert der Spalte "Dreieck von links?", die Farbanzahl der Wert von
"Die Dreiecksform(en) bestehen aus wie vielen Farben?". Bei einer Farbe faellt der Zusatz weg.

Gebaut sind Form 1 (Dreieck nur in der linken Haelfte, Spitze bei 72) und Form 3 (bis zum
rechten Rand, Spitze bei 180). Trapez und Uebergang in eine Spur sind eigene Geometrie und kommen,
wenn die erste Flagge sie braucht. `dreieck-1` und `dreieck-3` kommen byteweise so heraus, wie sie
von Hand angelegt wurden.

Konvention (siehe Regeln.md):
  * Leinwand 180 x 120, x von 0 bis 180, y von -120 bis 0 -- Overlay wie Goesch und Rahmen.
  * Mehrere Farben sind ineinanderliegende Dreiecke mit derselben Grundseite, dem ganzen linken
    Rand. Die Spitzen liegen gleichmaessig verteilt: bei vier Farben auf einem, zwei, drei und vier
    Vierteln der Dreieckslaenge. Das innerste ist ein Dreieck, die aeusseren sind Pfeilbaender.
  * Gezaehlt wird nach der Spitze von links nach rechts, also vom innersten nach aussen. Guyana
    steht so mit `Rot|Schwarz|Gelb|Weiss` im Blatt, wie man die Flagge beschreibt.
"""
import json
import sys
from pathlib import Path

NL = chr(10)

BREITE, HOEHE = 180.0, 120.0
SPITZE = {1: 72.0, 3: BREITE}          # wie weit die Form nach rechts reicht


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), 3)


def flaeche(nummer, punkte):
    ring = [[runde(x), runde(-h)] for x, h in punkte]
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[ring + [ring[0]]]]}}


def dreiecke(form, anzahl):
    laenge = SPITZE[form]
    mitte = HOEHE / 2
    spitzen = [laenge * k / anzahl for k in range(1, anzahl + 1)]
    teile = [flaeche(0, [(0, 0), (spitzen[0], mitte), (0, HOEHE)])]
    for k in range(1, anzahl):
        teile.append(flaeche(k, [(0, 0), (spitzen[k], mitte), (0, HOEHE), (spitzen[k - 1], mitte)]))
    return teile


def schreibe(zielordner, name):
    teile = name.split("-")
    if teile[0] != "dreieck" or len(teile) not in (2, 3):
        raise SystemExit("Erwartet wird dreieck-<form>[-<farbanzahl>], nicht: " + name)
    form = int(teile[1])
    anzahl = int(teile[2]) if len(teile) == 3 else 1
    if form not in SPITZE:
        raise SystemExit("Form %d ist noch nicht gebaut: %s" % (form, name))
    features = [json.dumps(f) for f in dreiecke(form, anzahl)]
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
    print("%s  (%d Flaeche(n))" % (ziel, len(features)))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    for name in sys.argv[2:]:
        schreibe(sys.argv[1], name)

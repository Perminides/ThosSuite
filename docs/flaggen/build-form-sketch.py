"""Erzeugt die Skizze fuer Flaggen, die kein Rechteck sind.

Aufruf:
    python build-form-sketch.py <zielordner> nicht-rechteckig uni-quadratisch ...

Der Name ist die Antwort auf die Formfrage, nicht das Land -- auch wenn es heute nur Nepal gibt.

`<hintergrund>-quadratisch` ist kein eigener Entwurf, sondern wird aus `<hintergrund>.geojson` im
selben Ordner abgeleitet: Jede x-Koordinate wird auf ein Quadrat von 120 x 120 geschoben, mittig bei
x 30 bis 150. So gibt es jede Geometrie nur einmal. Kreise gehen nicht -- sie wuerden zur Ellipse.

`nicht-rechteckig` sind zwei Wimpel uebereinander, im Seitenverhaeltnis Nepals und mittig auf der
Leinwand: Die obere Spitze liegt auf halber Hoehe am rechten Rand, die Stufe bei einem Drittel der
Breite, die untere Spitze rechts unten. Flaeche 0 ist das Feld, Flaeche 1 der Rand als Ring.

Konvention (siehe Regeln.md):
  * Die Leinwand bleibt 180 x 120, auch wenn die Flaechen sie nicht fuellen. Die Datei sagt das
    im GeoJSON-Feld `bbox`; ohne es naehme SketchPane die Box der Wimpel, und Raster und
    Elementgroessen waeren andere als ueberall sonst.
  * Wohin Elemente auf dieser Skizze kommen, steht im Generator (ANKER), nicht hier: Die Mitte
    eines Rasterfelds laege neben dem schmalen oberen Wimpel.
"""
import json
import sys
from pathlib import Path

NL = chr(10)
BREITE, HOEHE = 180.0, 120.0
WIMPEL = HOEHE / 1.219        # Nepal ist 1 : 1,219
RAND = 4.0


def runde(wert):
    wert = round(float(wert), 3)
    return int(wert) if wert.is_integer() else wert


def wimpel():
    """Der Umriss in Dateikoordinaten (y nach unten negativ), gegen den Uhrzeigersinn."""
    x0 = (BREITE - WIMPEL) / 2
    return [(x0, 0), (x0, -HOEHE), (x0 + WIMPEL, -HOEHE), (x0 + WIMPEL / 3, -HOEHE / 2),
            (x0 + WIMPEL, -HOEHE / 2)]


def nach_innen(punkte, abstand):
    """Jede Kante um `abstand` nach innen versetzt, Ecken auf Gehrung -- auch die einspringende."""
    n = len(punkte)
    linien = []
    for i in range(n):
        (ax, ay), (bx, by) = punkte[i], punkte[(i + 1) % n]
        dx, dy = bx - ax, by - ay
        laenge = (dx * dx + dy * dy) ** 0.5
        nx, ny = -dy / laenge, dx / laenge      # links der Kante = innen bei Gegen-Uhrzeigersinn
        linien.append(((ax + nx * abstand, ay + ny * abstand), (dx, dy)))
    ergebnis = []
    for i in range(n):
        (px, py), (rx, ry) = linien[i - 1]
        (qx, qy), (sx, sy) = linien[i]
        t = ((qx - px) * sy - (qy - py) * sx) / (rx * sy - ry * sx)
        ergebnis.append((px + t * rx, py + t * ry))
    return ergebnis


def ring(punkte):
    r = [[runde(x), runde(y)] for x, y in punkte]
    return r + [r[0]]


QUADRAT = "-quadratisch"
LINKS = (BREITE - HOEHE) / 2   # wo das Quadrat beginnt


def quadratisch(zielordner, name):
    """Die Flaechen des rechteckigen Hintergrunds, in x auf das Quadrat gestaucht."""
    quelle = Path(zielordner) / (name[:-len(QUADRAT)] + ".geojson")
    if not quelle.exists():
        raise SystemExit("Fuer %s fehlt die rechteckige Vorlage %s" % (name, quelle))
    daten = json.loads(quelle.read_text(encoding="utf-8"))

    def stauchen(koordinaten):
        if isinstance(koordinaten[0], (int, float)):
            return [runde(LINKS + koordinaten[0] * HOEHE / BREITE), runde(koordinaten[1])]
        return [stauchen(k) for k in koordinaten]

    features = []
    for f in daten["features"]:
        if f["geometry"]["type"] == "Point":
            raise SystemExit("%s hat einen Kreis, der im Quadrat zur Ellipse wuerde" % quelle.name)
        features.append({"type": "Feature", "properties": {"id": f["properties"]["id"]},
                         "geometry": {"type": f["geometry"]["type"],
                                      "coordinates": stauchen(f["geometry"]["coordinates"])}})
    return features


def schreibe(zielordner, name):
    if name.endswith(QUADRAT):
        schreibe_datei(zielordner, name, quadratisch(zielordner, name))
        return
    if name != "nicht-rechteckig":
        raise SystemExit("Erwartet wird nicht-rechteckig oder <hintergrund>-quadratisch, nicht: " + name)
    aussen = wimpel()
    innen = nach_innen(aussen, RAND)
    features = [
        {"type": "Feature", "properties": {"id": 0},
         "geometry": {"type": "MultiPolygon", "coordinates": [[ring(innen)]]}},
        {"type": "Feature", "properties": {"id": 1},
         "geometry": {"type": "MultiPolygon", "coordinates": [[ring(aussen), ring(innen)]]}},
    ]
    schreibe_datei(zielordner, name, features)


def schreibe_datei(zielordner, name, features):
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"xy_coordinate_resolution": 1,',
            '"bbox": [0, %d, %d, 0],' % (-HOEHE, BREITE),
            '"features": [']
    text = NL.join(kopf) + NL + ("," + NL).join(json.dumps(f) for f in features) + NL + "]" + NL + "}" + NL
    ziel = Path(zielordner) / (name + ".geojson")
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    print("%s  (%d x %d, %d Flaechen)" % (ziel, BREITE, HOEHE, len(features)))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    for name in sys.argv[2:]:
        schreibe(sys.argv[1], name)

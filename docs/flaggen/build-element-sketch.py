"""Erzeugt die Strukturdateien der Zusatzelemente.

Aufruf:
    python build-element-sketch.py <zielordner>

Konvention (siehe Flaggen-Deck.md, §5):
  * Elementdateien sind um den Nullpunkt **zentriert**: |x| <= 30, |y| <= 20. Damit bleibt
    beim Skalieren der Mittelpunkt stehen -- Groesse und Lage haengen nicht aneinander.
    Hintergrunddateien sind das Gegenteil: Sie fuellen die Leinwand 0..180 / -120..0.
  * Faktor 1,0 heisst "fuellt ein Rasterfeld" (60 x 40). Groesser ist erlaubt.
  * Y ist positiv nach oben, der Leser invertiert beim Einlesen.
  * In properties steht nur die Flaechennummer, bei Kreisen zusaetzlich der Radius.
  * Eine Datei kann mehrere Flaechen tragen; eine Flaeche kann aus mehreren getrennten
    Teilen bestehen (MultiPolygon). Getrennte Teile duerfen sich nicht beruehren --
    beim Fuellen faellt der Strich weg, Beruehrendes verschmilzt zu einem Klecks.
"""
import json
import math
import sys
from pathlib import Path

NL = chr(10)
FELD_X, FELD_Y = 30.0, 20.0          # halbe Feldgroesse


def rund(punkte):
    return [[round(x, 2), round(y, 2)] for x, y in punkte]


def ring(punkte):
    """Schliesst einen Ring, wie GeoJSON es verlangt."""
    p = rund(punkte)
    return p + [p[0]]


def stern(cx, cy, aussen, zacken=5):
    """Zackenfigur mit der Spitze nach oben. Innenradius aus dem regelmaessigen Fuenfstern."""
    innen = aussen * math.sin(math.radians(18)) / math.sin(math.radians(126))
    punkte = []
    for i in range(zacken * 2):
        r = aussen if i % 2 == 0 else innen
        w = math.radians(90 + i * 180.0 / zacken)
        punkte.append((cx + r * math.cos(w), cy + r * math.sin(w)))
    return ring(punkte)


def rechteck(x0, y0, x1, y1):
    return ring([(x0, y0), (x1, y0), (x1, y1), (x0, y1)])


def zentriere(teile):
    """Schiebt eine Figur so, dass ihre Bounding Box um den Nullpunkt liegt."""
    xs = [p[0] for t in teile for p in t]
    ys = [p[1] for t in teile for p in t]
    dx, dy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    return [rund([(x - dx, y - dy) for x, y in t]) for t in teile]


def flaeche(nummer, teile):
    """Eine Flaeche aus einem oder mehreren getrennten Teilen."""
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[t] for t in teile]}}


def kreisflaeche(nummer, cx, cy, radius):
    return {"type": "Feature", "properties": {"id": nummer, "radius": radius},
            "geometry": {"type": "Point", "coordinates": [cx, cy]}}


# --- die Elemente ------------------------------------------------------------
def kreis():
    """Rund bleibt rund: Point plus radius, kein Vieleck. Die Hoehe begrenzt ihn, nicht die Breite."""
    return [kreisflaeche(0, 0, 0, FELD_Y)]


def einzelstern():
    return [flaeche(0, [stern(0, 0, FELD_Y)])]


def raute():
    return [flaeche(0, [ring([(0, FELD_Y), (FELD_X, 0), (0, -FELD_Y), (-FELD_X, 0)])])]


def schrift_t():
    """Platzhalter fuer Schrift: ein grosses T. Es zeigt, dass da etwas steht, und nicht was."""
    b, s, h = 18.0, 5.0, 8.0            # halbe Balkenbreite, halbe Stegbreite, Balkenhoehe
    return [flaeche(0, [ring([(-b, FELD_Y), (b, FELD_Y), (b, FELD_Y - h), (s, FELD_Y - h),
                             (s, -FELD_Y), (-s, -FELD_Y), (-s, FELD_Y - h), (-b, FELD_Y - h)])])]


def plus(cx, cy, staerke, laenge):
    """Ein Pluszeichen, im Uhrzeigersinn um seine Mitte."""
    d, l = staerke, laenge
    return ring([(cx - d, cy + l), (cx + d, cy + l), (cx + d, cy + d), (cx + l, cy + d),
                 (cx + l, cy - d), (cx + d, cy - d), (cx + d, cy - l), (cx - d, cy - l),
                 (cx - d, cy - d), (cx - l, cy - d), (cx - l, cy + d), (cx - d, cy + d)])


def sternhaufen():
    """Mehr als zwei Sterne: drei nach unten rechts versetzte plus ein grosses Plus oben rechts.

    Die Figur bleibt annaehernd quadratisch. Breit auseinandergezogen wuerde sie beim
    Verkleinern unnoetig klein, weil der Faktor an der breitesten Stelle haengt.
    """
    r = 5.0
    teile = [stern(-13, 4, r), stern(-4, -4, r), stern(5, -12, r), plus(11, 11, 3.0, 8.5)]
    return [flaeche(0, zentriere(teile))]


def zweisterne():
    """Genau zwei Sterne: dicht versetzt, aber getrennt -- nicht weit nebeneinander."""
    r = 8.0
    return [flaeche(0, zentriere([stern(-7, 6, r), stern(7, -6, r)]))]


def sichel(aussen=20.0, versatz=7.0, ausschnitt=18.0):
    """Kreis minus versetzter Kreis, nach rechts offen -- echte Boegen statt eines Vielecks.

    Die Bounding Box der uebrig bleibenden Sichel ist nicht die des aeusseren Kreises, deshalb
    wird sie ausgerechnet und die ganze Figur so verschoben, dass sie um den Nullpunkt liegt.
    """
    # Hoehe = 2 * Radius: Der oberste Punkt ist der Scheitel des aeusseren Kreises, nicht die
    # Hornspitze -- der Bogen woelbt sich ueber sie hinaus, und der Ausschnitt liegt daneben.
    # Mit Radius 20 ist die Sichel damit genau so hoch wie ein Rasterfeld, wie alle anderen.
    cos = (aussen ** 2 + versatz ** 2 - ausschnitt ** 2) / (2 * aussen * versatz)
    horn = aussen * cos                       # x der beiden Hornspitzen
    dx = (-aussen + horn) / 2                 # Mitte der Bounding Box
    return [{"type": "Feature",
             "properties": {"id": 0, "radius": aussen,
                            "cutout": {"x": round(versatz - dx, 2), "y": 0, "radius": ausschnitt}},
             "geometry": {"type": "Point", "coordinates": [round(-dx, 2), 0]}}]


def kappe(spitze_x, spitze_y, richtung, halbbreite, bogen=7):
    """Die runde Kuppe eines Fingers: ein Halbkreis um die Spitze, quer zur Richtung."""
    rx, ry = richtung
    quer = (-ry, rx)
    punkte = []
    for i in range(bogen + 1):
        w = math.pi * i / bogen
        punkte.append((spitze_x + halbbreite * (quer[0] * math.cos(w) + rx * math.sin(w)),
                       spitze_y + halbbreite * (quer[1] * math.cos(w) + ry * math.sin(w))))
    return punkte


def finger(ansatz_x, ansatz_y, richtung, laenge, halbbreite):
    """Ein Finger als Kapsel: rechte Flanke hoch, ueber die Kuppe, linke Flanke zurueck."""
    rx, ry = richtung
    qx, qy = -ry, rx
    sx, sy = ansatz_x + rx * laenge, ansatz_y + ry * laenge
    return ([(ansatz_x - qx * halbbreite, ansatz_y - qy * halbbreite)]
            + kappe(sx, sy, richtung, halbbreite)[::-1]
            + [(ansatz_x + qx * halbbreite, ansatz_y + qy * halbbreite)])


def hand():   # nicht mehr in ELEMENTE: die Hand kommt aus svg-zu-sketch.py
    """Eine offene Hand als eine Flaeche: Handflaeche, vier Finger, Daumen nach links oben.

    Ein Ring, kein Zusammensetzen aus Teilen -- getrennte Teile wuerden beim Fuellen zwar nicht
    verschmelzen, aber eine Hand faellt nun einmal nicht auseinander.
    """
    hoch = (0.0, 1.0)
    daumen_richtung = (-math.cos(math.radians(35)), math.sin(math.radians(35)))
    rand, oben, unten = 11.0, 1.0, -17.0
    finger_daten = [(8.0, 9.5, 2.0), (3.2, 14.0, 2.2), (-1.6, 16.0, 2.3), (-6.4, 13.5, 2.2)]

    punkte = [(rand, unten), (rand, oben)]
    for x, laenge, halbbreite in finger_daten:            # von rechts nach links
        punkte += finger(x, oben, hoch, laenge, halbbreite)
    punkte.append((-rand, oben))
    punkte += finger(-rand + 1.5, -4.0, daumen_richtung, 10.0, 2.4)
    punkte.append((-rand, -10.0))
    return [flaeche(0, zentriere([ring(punkte)]))]


ELEMENTE = {"kreis": kreis, "sichel": sichel, "stern": einzelstern, "raute": raute, "schrift-t": schrift_t,
            "stern-haufen": sternhaufen, "stern-zwei": zweisterne}


def schreibe(zielordner, name):
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"comment": "Elementdatei: um den Nullpunkt zentriert, Faktor 1,0 fuellt ein Rasterfeld.",',
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"features": [']
    features = [json.dumps(f) for f in ELEMENTE[name]()]
    text = NL.join(kopf) + NL + ("," + NL).join(features) + NL + "]" + NL + "}" + NL
    ziel = Path(zielordner) / (name + ".geojson")
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    print("%s  (%d Flaeche(n))" % (ziel, len(ELEMENTE[name]())))


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    for name in sys.argv[2:] or sorted(ELEMENTE):
        schreibe(sys.argv[1], name)

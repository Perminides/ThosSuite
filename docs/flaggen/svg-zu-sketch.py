"""Macht aus einem heruntergeladenen SVG-Piktogramm eine Strukturdatei.

Aufruf:
    python svg-zu-sketch.py <zielordner> <datei.svg> [name]

Der Weg dahin ist bewusst zweistufig: Hier draussen wird die unangenehme Welt erledigt --
Kurven, Boegen, relative Befehle, Y nach unten, beliebige viewBox --, und in den Ordner faellt
eine ganz normale Datei, wie sie die Erzeugerskripte daneben auch schreiben. Die Suite lernt
dabei nichts Neues.

Konvention (siehe Flaggen-Deck.md, §5):
  * Elementdateien sind um den Nullpunkt zentriert, |x| <= 30 und |y| <= 20.
  * Y ist positiv nach oben, der Leser invertiert beim Einlesen. SVG waechst nach unten,
    also wird hier gespiegelt.
  * Eine Datei kann mehrere Flaechen tragen; hier entsteht immer genau eine, weil ein
    Piktogramm eine Silhouette ist. Ihre Teilpfade werden zu Ringen eines MultiPolygons.

Kurven werden **abgeflacht**. Die Toleranz ist der groesste erlaubte Abstand zwischen der
echten Kurve und dem Streckenzug, in Leinwandeinheiten. Bei 0,01 und einer Hand von 30
Einheiten Breite liegt der Fehler auf dem Schirm deutlich unter einem zehntel Pixel --
sichtbar ist das nicht, und die Datei bleibt trotzdem eine Punktliste, die man messen kann.

Was NICHT unterstuetzt wird, faellt laut auf: Striche ohne Fuellung, Text, Masken,
Clip-Pfade, Verlaeufe. Solche Dateien sollen scheitern und nicht still falsch werden.
"""
import json
import math
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NL = chr(10)
FELD_X, FELD_Y = 30.0, 20.0        # halbe Feldgroesse
TOLERANZ = 0.01                    # groesster Abstand Kurve <-> Streckenzug, in Leinwandeinheiten

ZAHL = re.compile(r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?")
BEFEHL_MIT_ZAHLEN = re.compile(r"([MmZzLlHhVvCcSsQqTtAa])([^MmZzLlHhVvCcSsQqTtAa]*)")


def zerlege(d):
    """Der Pfad als Folge von (Befehl, Zahlen).

    Zahlen duerfen ohne Trenner aneinanderkleben, und ein Befehl darf gar keine haben -- ein `Z`
    direkt vor dem naechsten `M` hat keine. Deshalb wird nach Befehlsbuchstaben gesucht statt
    stur abwechselnd zu lesen.
    """
    return [(befehl, [float(z) for z in ZAHL.findall(roh)])
            for befehl, roh in BEFEHL_MIT_ZAHLEN.findall(d)]


def kubisch(p0, p1, p2, p3, toleranz):
    """Eine kubische Kurve als Streckenzug. Die Schrittzahl folgt aus der Laenge des Kontrollpolygons."""
    laenge = (abstand(p0, p1) + abstand(p1, p2) + abstand(p2, p3))
    schritte = max(2, int(math.ceil(math.sqrt(laenge / max(toleranz, 1e-6)))))
    punkte = []
    for i in range(1, schritte + 1):
        t = i / schritte
        s = 1 - t
        punkte.append((s*s*s*p0[0] + 3*s*s*t*p1[0] + 3*s*t*t*p2[0] + t*t*t*p3[0],
                       s*s*s*p0[1] + 3*s*s*t*p1[1] + 3*s*t*t*p2[1] + t*t*t*p3[1]))
    return punkte


def quadratisch(p0, p1, p2, toleranz):
    """Quadratisch ist kubisch mit zwei abgeleiteten Kontrollpunkten -- exakt, keine Naeherung."""
    c1 = (p0[0] + 2/3*(p1[0]-p0[0]), p0[1] + 2/3*(p1[1]-p0[1]))
    c2 = (p2[0] + 2/3*(p1[0]-p2[0]), p2[1] + 2/3*(p1[1]-p2[1]))
    return kubisch(p0, c1, c2, p2, toleranz)


def bogen(p0, rx, ry, winkel, gross, sweep, p1, toleranz):
    """Ein Ellipsenbogen, aufgeloest ueber die Mittelpunktform (W3C-Anhang F.6.5)."""
    if rx == 0 or ry == 0 or p0 == p1:
        return [p1]
    rx, ry = abs(rx), abs(ry)
    phi = math.radians(winkel)
    cos, sin = math.cos(phi), math.sin(phi)
    dx, dy = (p0[0]-p1[0])/2, (p0[1]-p1[1])/2
    x1, y1 = cos*dx + sin*dy, -sin*dx + cos*dy

    # Zu kleine Radien werden aufgeblasen, sonst gibt es keine Loesung (F.6.6).
    lam = x1*x1/(rx*rx) + y1*y1/(ry*ry)
    if lam > 1:
        rx, ry = rx*math.sqrt(lam), ry*math.sqrt(lam)

    zaehler = max(0.0, rx*rx*ry*ry - rx*rx*y1*y1 - ry*ry*x1*x1)
    nenner = rx*rx*y1*y1 + ry*ry*x1*x1
    faktor = math.sqrt(zaehler/nenner) * (-1 if gross == sweep else 1)
    cx1, cy1 = faktor * rx*y1/ry, faktor * -ry*x1/rx
    cx = cos*cx1 - sin*cy1 + (p0[0]+p1[0])/2
    cy = sin*cx1 + cos*cy1 + (p0[1]+p1[1])/2

    def winkel_zu(ux, uy, vx, vy):
        n = math.hypot(ux, uy) * math.hypot(vx, vy)
        w = math.acos(max(-1.0, min(1.0, (ux*vx + uy*vy) / n)))
        return -w if ux*vy - uy*vx < 0 else w

    start = winkel_zu(1, 0, (x1-cx1)/rx, (y1-cy1)/ry)
    delta = winkel_zu((x1-cx1)/rx, (y1-cy1)/ry, (-x1-cx1)/rx, (-y1-cy1)/ry)
    if not sweep and delta > 0:
        delta -= 2*math.pi
    elif sweep and delta < 0:
        delta += 2*math.pi

    radius = max(rx, ry)
    schritte = max(2, int(math.ceil(abs(delta) / (2 * math.acos(max(-1.0, 1 - toleranz/radius))))))
    punkte = []
    for i in range(1, schritte + 1):
        w = start + delta * i / schritte
        ex, ey = rx*math.cos(w), ry*math.sin(w)
        punkte.append((cos*ex - sin*ey + cx, sin*ex + cos*ey + cy))
    return punkte


def abstand(a, b):
    return math.hypot(a[0]-b[0], a[1]-b[1])


def ringe(d, toleranz):
    """Der Pfad als Liste geschlossener Punktzuege -- ein Zug je Teilpfad."""
    alle, aktuell = [], []
    pos = (0.0, 0.0)
    start = (0.0, 0.0)
    letzte_kubisch = None
    letzte_quadratisch = None

    for befehl, zahlen in zerlege(d):
        gross = befehl.isupper()
        b = befehl.upper()

        def punkt(i):
            """Ein Koordinatenpaar, absolut gerechnet."""
            return (zahlen[i] + (0 if gross else pos[0]), zahlen[i+1] + (0 if gross else pos[1]))

        if b == "M":
            if aktuell:
                alle.append(aktuell)
            pos = punkt(0)
            start, aktuell = pos, [pos]
            for i in range(2, len(zahlen), 2):      # weitere Paare sind LineTo
                pos = (zahlen[i] + (0 if gross else pos[0]), zahlen[i+1] + (0 if gross else pos[1]))
                aktuell.append(pos)
            letzte_kubisch = letzte_quadratisch = None
        elif b == "Z":
            if aktuell:
                alle.append(aktuell)
            aktuell, pos = [], start
            letzte_kubisch = letzte_quadratisch = None
        elif b in ("L", "H", "V"):
            schritt = {"L": 2, "H": 1, "V": 1}[b]
            for i in range(0, len(zahlen), schritt):
                if b == "L":
                    pos = (zahlen[i] + (0 if gross else pos[0]), zahlen[i+1] + (0 if gross else pos[1]))
                elif b == "H":
                    pos = (zahlen[i] + (0 if gross else pos[0]), pos[1])
                else:
                    pos = (pos[0], zahlen[i] + (0 if gross else pos[1]))
                aktuell.append(pos)
            letzte_kubisch = letzte_quadratisch = None
        elif b in ("C", "S"):
            schritt = 6 if b == "C" else 4
            for i in range(0, len(zahlen), schritt):
                if b == "C":
                    c1, c2, ziel = punkt(i), punkt(i+2), punkt(i+4)
                else:
                    c1 = pos if letzte_kubisch is None else (2*pos[0]-letzte_kubisch[0], 2*pos[1]-letzte_kubisch[1])
                    c2, ziel = punkt(i), punkt(i+2)
                aktuell += kubisch(pos, c1, c2, ziel, toleranz)
                pos, letzte_kubisch = ziel, c2
                letzte_quadratisch = None
        elif b in ("Q", "T"):
            schritt = 4 if b == "Q" else 2
            for i in range(0, len(zahlen), schritt):
                if b == "Q":
                    c, ziel = punkt(i), punkt(i+2)
                else:
                    c = pos if letzte_quadratisch is None else (2*pos[0]-letzte_quadratisch[0], 2*pos[1]-letzte_quadratisch[1])
                    ziel = punkt(i)
                aktuell += quadratisch(pos, c, ziel, toleranz)
                pos, letzte_quadratisch = ziel, c
                letzte_kubisch = None
        elif b == "A":
            for i in range(0, len(zahlen), 7):
                ziel = (zahlen[i+5] + (0 if gross else pos[0]), zahlen[i+6] + (0 if gross else pos[1]))
                aktuell += bogen(pos, zahlen[i], zahlen[i+1], zahlen[i+2],
                                 bool(zahlen[i+3]), bool(zahlen[i+4]), ziel, toleranz)
                pos = ziel
            letzte_kubisch = letzte_quadratisch = None
        else:
            raise SystemExit("Unbekannter Pfadbefehl: " + befehl)

    if aktuell:
        alle.append(aktuell)
    return alle


MATRIX = re.compile(r"(matrix|translate|scale|rotate|skewX|skewY)\s*\(([^)]*)\)")
GEZEICHNET = ("path", "rect", "circle", "ellipse", "polygon", "polyline", "line")
VERBOTEN = ("text", "mask", "clipPath", "linearGradient", "radialGradient", "filter", "image", "use")


def einheit():
    return (1.0, 0.0, 0.0, 1.0, 0.0, 0.0)


def mal(m, n):
    """Zwei affine Matrizen (a b c d e f) hintereinander: erst n, dann m."""
    a1, b1, c1, d1, e1, f1 = m
    a2, b2, c2, d2, e2, f2 = n
    return (a1*a2 + c1*b2, b1*a2 + d1*b2, a1*c2 + c1*d2,
            b1*c2 + d1*d2, a1*e2 + c1*f2 + e1, b1*e2 + d1*f2 + f1)


def matrix(text):
    """Ein transform-Attribut als eine Matrix. Mehrere Angaben wirken von links nach rechts."""
    m = einheit()
    if not text:
        return m
    for name, roh in MATRIX.findall(text):
        z = [float(v) for v in ZAHL.findall(roh)]
        if name == "matrix":
            n = tuple(z[:6])
        elif name == "translate":
            n = (1, 0, 0, 1, z[0], z[1] if len(z) > 1 else 0)
        elif name == "scale":
            n = (z[0], 0, 0, z[1] if len(z) > 1 else z[0], 0, 0)
        elif name == "rotate":
            w = math.radians(z[0])
            n = (math.cos(w), math.sin(w), -math.sin(w), math.cos(w), 0, 0)
            if len(z) == 3:                     # Drehung um einen Punkt
                n = mal(mal((1, 0, 0, 1, z[1], z[2]), n), (1, 0, 0, 1, -z[1], -z[2]))
        elif name == "skewX":
            n = (1, 0, math.tan(math.radians(z[0])), 1, 0, 0)
        else:
            n = (1, math.tan(math.radians(z[0])), 0, 1, 0, 0)
        m = mal(m, n)
    return m


def verschoben(ring, m):
    a, b, c, d, e, f = m
    return [(a*x + c*y + e, b*x + d*y + f) for x, y in ring]


def ist_hintergrund(ring, kasten):
    """Das Rechteck ueber die ganze Zeichenflaeche, mit dem manche Saetze ihre Symbole hinterlegen."""
    if kasten is None or len(set(ring)) > 4:
        return False
    x0, y0, x1, y1 = kasten
    xs = [p[0] for p in ring]
    ys = [p[1] for p in ring]
    breit = abs(min(xs) - x0) < 1 and abs(max(xs) - x1) < 1
    hoch = abs(min(ys) - y0) < 1 and abs(max(ys) - y1) < 1
    return breit and hoch


def sammle(element, m, kasten, toleranz, alle):
    """Laeuft den Baum ab und sammelt die Ringe, mit eingerechneten Transformationen."""
    tag = element.tag.split("}")[-1]
    if tag in VERBOTEN:
        raise SystemExit("Die Datei enthaelt <%s> -- das loest dieses Skript nicht auf." % tag)
    m = mal(m, matrix(element.get("transform")))

    if tag == "path":
        if (element.get("fill") or "").strip() == "none":
            raise SystemExit("Ein Pfad ohne Fuellung (fill=none) -- nimm die gefuellte Variante.")
        for ring in ringe(element.get("d"), toleranz):
            ring = verschoben(ring, m)
            if ist_hintergrund(ring, kasten):
                continue
            alle.append(ring)
    elif tag in GEZEICHNET:
        raise SystemExit("<%s> wird noch nicht umgewandelt -- bisher nur <path>." % tag)

    for kind in element:
        sammle(kind, m, kasten, toleranz, alle)


def lade(datei, toleranz):
    """Zweimal lesen: Die Toleranz gilt in Leinwandeinheiten, der Maßstab steht aber erst fest,
    wenn man die Ausdehnung kennt. Der erste Lauf misst grob, der zweite zeichnet richtig fein --
    sonst haette eine Quelle mit 512er Koordinaten zwanzigmal so viele Punkte wie eine mit 24er.
    """
    wurzel = ET.parse(datei).getroot()
    zahlen = [float(v) for v in ZAHL.findall(wurzel.get("viewBox") or "")]
    kasten = (zahlen[0], zahlen[1], zahlen[0] + zahlen[2], zahlen[1] + zahlen[3]) if len(zahlen) == 4 else None

    grob = []
    sammle(wurzel, einheit(), kasten, 1.0, grob)
    if not grob:
        raise SystemExit("Nichts Zeichenbares in der Datei.")

    alle = []
    sammle(wurzel, einheit(), kasten, toleranz / faktor(grob), alle)
    return alle


def faktor(alle):
    """Um wie viel die Figur beim Normieren kleiner wird."""
    xs = [p[0] for ring in alle for p in ring]
    ys = [p[1] for ring in alle for p in ring]
    return min(2*FELD_X / (max(xs) - min(xs)), 2*FELD_Y / (max(ys) - min(ys)))


def normiere(alle):
    """Auf ein Rasterfeld einpassen, Y spiegeln, um den Nullpunkt zentrieren."""
    xs = [p[0] for ring in alle for p in ring]
    ys = [p[1] for ring in alle for p in ring]
    breite, hoehe = max(xs) - min(xs), max(ys) - min(ys)
    faktor = min(2*FELD_X / breite, 2*FELD_Y / hoehe)
    mx, my = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    return [[(round((x - mx) * faktor, 3), round(-(y - my) * faktor, 3)) for x, y in ring]
            for ring in alle]


def schreibe(zielordner, name, alle):
    ringe_json = [[[x, y] for x, y in ring] + [[ring[0][0], ring[0][1]]] for ring in alle]
    feature = {"type": "Feature", "properties": {"id": 0},
               "geometry": {"type": "MultiPolygon", "coordinates": [[r] for r in ringe_json]}}
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"comment": "Aus einem SVG-Piktogramm erzeugt: um den Nullpunkt zentriert, Faktor 1,0 fuellt ein Rasterfeld.",',
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"features": [']
    text = NL.join(kopf) + NL + json.dumps(feature) + NL + "]" + NL + "}" + NL
    ziel = Path(zielordner) / (name + ".geojson")
    ziel.write_text(text, encoding="utf-8")
    punkte = sum(len(r) for r in alle)
    print("%s  (%d Ring(e), %d Punkte)" % (ziel, len(alle), punkte))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    quelle = Path(sys.argv[2])
    name = sys.argv[3] if len(sys.argv) > 3 else quelle.stem
    schreibe(sys.argv[1], name, normiere(lade(quelle, TOLERANZ)))

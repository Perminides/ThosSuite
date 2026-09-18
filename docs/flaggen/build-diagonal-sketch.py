"""Erzeugt die Hintergruende des Diagonal-Zweigs.

Aufruf:
    python build-diagonal-sketch.py <zielordner> diagonal-steigend-0 diagonal-fallend-0 ...

Der Name ist `diagonal-<richtung>-<baender>`, genau wie der Generator ihn ableitet:
`steigend`, `fallend` oder `faecher`, dahinter die Zahl der durchlaufenden Baender. Die **0**
ist dort kein Fehlen einer Antwort, sondern ein eigener Wert: „Teilung vorhanden, aber kein
eigenes Band" -- die beiden Flaechen stossen unmittelbar aneinander.

Die Baender liegen mittig auf der Eckdiagonalen und nehmen zusammen ein Drittel der Hoehe ein --
dieselbe Zahl wie das Rasterdrittel beim Goesch. Die echte Neigung einer Flagge ist flacher (Brunei
laeuft ueber 300 von 720), aber danach wird nicht gefragt; die Skizze zeigt die kanonische Diagonale.

Der Faecher ist eigene Geometrie, siehe `faecher`: Die Baender laufen nicht parallel, sondern
strahlen aus der Ecke links unten.

Konvention (siehe Flaggen-Deck.md):
  * Leinwand IMMER 180 x 120, x von 0 bis 180, y von -120 bis 0.
  * In properties steht nur die Flaechennummer, nullbasiert.
  * Gezaehlt wird von oben: Flaeche 0 ist das Dreieck, das die obere Kante traegt. Bhutan steht
    im Blatt mit `Gelb|Orange`, und sein Gelb ist die obere Flaeche.
"""
import json
import math
import sys
from pathlib import Path

NL = chr(10)

BREITE, HOEHE = 180.0, 120.0
WINZIG = 1e-9
RICHTUNGEN = ("steigend", "fallend", "faecher")
BAND_ANTEIL = 1 / 3          # wie viel der Hoehe alle Baender zusammen einnehmen
STELLEN = 3


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), STELLEN)


def flaeche(nummer, *teile):
    """Eine Flaeche aus einem oder mehreren getrennten Stuecken."""
    ringe = [[[runde(x), runde(-h)] for x, h in punkte] for punkte in teile]
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[r + [r[0]]] for r in ringe]}}


def halbebene(zelle, a, b, c):
    """Sutherland-Hodgman: der Teil der Flaeche, auf dem a*x + b*h + c >= 0 gilt."""
    raus = []
    for i in range(len(zelle)):
        p, q = zelle[i], zelle[(i + 1) % len(zelle)]
        wp, wq = a * p[0] + b * p[1] + c, a * q[0] + b * q[1] + c
        if wp >= -WINZIG:
            raus.append(p)
        if (wp > WINZIG) != (wq > WINZIG):
            t = wp / (wp - wq)
            raus.append((p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])))
    return raus


def streifen(steigung, versatz_oben, versatz_unten):
    """Die Leinwand, beschnitten auf den Streifen zwischen zwei Parallelen zur Diagonalen.

    Gerechnet wird in Tiefe von oben; {@code None} als Versatz heisst "bis zum Rand".
    """
    flaeche = [(0.0, 0.0), (BREITE, 0.0), (BREITE, HOEHE), (0.0, HOEHE)]
    if versatz_oben is not None:                    # h >= steigung*x + versatz
        flaeche = halbebene(flaeche, -steigung, 1.0, -versatz_oben)
    if versatz_unten is not None:                   # h <= steigung*x + versatz
        flaeche = halbebene(flaeche, steigung, -1.0, versatz_unten)
    # Liegt eine Ecke genau auf der Schnittlinie, liefert der Clipper sie zweimal: einmal als
    # behaltenen Punkt, einmal als Schnittpunkt. Hier faellt die Doppelung wieder heraus.
    gerundet = [(runde(x), runde(h)) for x, h in flaeche]
    return [p for i, p in enumerate(gerundet) if p != gerundet[i - 1]]


def gebaendert(richtung, baender):
    """Feld, Baender, Feld -- von oben gezaehlt, egal in welche Richtung die Diagonale laeuft.

    Die Diagonale ist die Mittellinie; kleinerer Versatz heisst weiter oben, auch bei steigend,
    weil dort die Steigung negativ ist und der Achsenabschnitt die Hoehe.
    """
    if richtung == "steigend":
        steigung, mitte = -HOEHE / BREITE, HOEHE
    else:
        steigung, mitte = HOEHE / BREITE, 0.0
    block = HOEHE * BAND_ANTEIL
    kanten = [mitte - block / 2 + i * block / baender for i in range(baender + 1)] if baender else [mitte]

    teile = [streifen(steigung, None, kanten[0])]
    for i in range(len(kanten) - 1):
        teile.append(streifen(steigung, kanten[i], kanten[i + 1]))
    teile.append(streifen(steigung, kanten[-1], None))
    return [flaeche(i, t) for i, t in enumerate(teile)]


def faecher(baender):
    """Marshallinseln und Seychellen: Baender, die aus der Ecke links unten strahlen.

    Der Faecher liegt symmetrisch um die Eckdiagonale, wie die parallelen Baender auch. Bei zwei
    Baendern ist die Grenze zwischen ihnen also genau die Diagonale, bei drei laeuft sie mitten durch
    das mittlere. Jedes Band ist ein fester Winkel: so gross, dass bei zwei Baendern das untere am
    rechten Rand ein Rasterdrittel breit endet. Der Faecher wird also mit jedem Band weiter.

    Bis zwei Baender ist das Feld **eine** Flaeche in zwei Stuecken ueber und unter dem Faecher, die
    Marshallinseln stehen so mit `Blau|Orange|Weiss` im Blatt. Die Stuecke beruehren sich nur im
    Punkt der Ecke. Ab drei Baendern sind es zwei Flaechen, oben die erste und unten die letzte, weil
    die Seychellen dort zwei Farben haben. Dazwischen kommen immer die Baender von oben.
    """
    if baender < 1:
        raise SystemExit("Ein Faecher ohne Band waere nur die Diagonale -- dafuer gibt es diagonal-steigend-0")
    ecke = (0.0, HOEHE)
    diagonale = math.atan2(HOEHE, BREITE)
    band = diagonale - math.atan2(HOEHE - HOEHE / 3, BREITE)
    halb = band * baender / 2
    if halb >= diagonale:
        raise SystemExit("So viele Baender passen nicht in den Faecher: %d" % baender)

    def randpunkt(winkel):
        """Wo ein Strahl aus der Ecke den Rand trifft, als Weg entlang des Randes."""
        tiefe = HOEHE - BREITE * math.tan(winkel)
        if tiefe >= 0:
            return BREITE + (HOEHE - tiefe)                  # rechter Rand
        return BREITE + HOEHE + (BREITE - HOEHE / math.tan(winkel))   # Oberkante

    def punkt(weg):
        """Der Punkt auf dem Rand zu einem Weg, gezaehlt ab der Ecke links unten im Uhrzeigersinn."""
        if weg <= BREITE:
            return (weg, HOEHE)
        if weg <= BREITE + HOEHE:
            return (BREITE, HOEHE - (weg - BREITE))
        if weg <= 2 * BREITE + HOEHE:
            return (BREITE - (weg - BREITE - HOEHE), 0.0)
        return (0.0, weg - 2 * BREITE - HOEHE)

    ecken_wege = [BREITE, BREITE + HOEHE, 2 * BREITE + HOEHE]    # rechts unten, rechts oben, links oben

    def stueck(von, bis):
        punkte = [ecke]
        if von > 0:
            punkte.append(punkt(von))
        punkte += [punkt(w) for w in ecken_wege if von < w < bis]
        if bis < 2 * (BREITE + HOEHE):
            punkte.append(punkt(bis))
        return punkte

    wege = [randpunkt(diagonale - halb + i * 2 * halb / baender) for i in range(baender + 1)]
    umfang = 2 * (BREITE + HOEHE)
    oben, unten = stueck(wege[-1], umfang), stueck(0.0, wege[0])
    teile = [flaeche(0, oben, unten)] if baender < 3 else [flaeche(0, oben)]
    for nummer, i in enumerate(reversed(range(baender)), start=1):
        teile.append(flaeche(nummer, stueck(wege[i], wege[i + 1])))
    if baender >= 3:
        teile.append(flaeche(baender + 1, unten))
    return teile


def schreibe(zielordner, name):
    teile = name.split("-")
    if len(teile) != 3 or teile[0] != "diagonal" or teile[1] not in RICHTUNGEN:
        raise SystemExit("Erwartet wird diagonal-<%s>-<baender>, nicht: %s"
                         % ("|".join(RICHTUNGEN), name))
    richtung, baender = teile[1], int(teile[2])
    teile = faecher(baender) if richtung == "faecher" else gebaendert(richtung, baender)
    features = [json.dumps(f) for f in teile]
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

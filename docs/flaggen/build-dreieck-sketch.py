"""Erzeugt die Dreiecke von links als Leinwand-Overlay.

Aufruf:
    python build-dreieck-sketch.py <elementordner> dreieck-3-4 [dreieck-1 ...]

Der Name ist `dreieck-<form>` oder `dreieck-<form>-<farbanzahl>`, genau wie der Generator ihn
ableitet. Die Form ist der Wert der Spalte "Dreieck von links?", die Farbanzahl der Wert von
"Die Dreiecksform(en) bestehen aus wie vielen Farben?". Bei einer Farbe faellt der Zusatz weg.

Gebaut sind Form 1 (Dreieck nur in der linken Haelfte, Spitze bei 72), Form 2 (Trapez, siehe
`trapez`), Form 3 (bis zum rechten Rand, Spitze bei 180) und Form 4 (Uebergang in eine Spur, siehe
`spur`). `dreieck-1` und `dreieck-3` kommen byteweise so heraus, wie sie
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
import math
import sys
from pathlib import Path

NL = chr(10)

BREITE, HOEHE = 180.0, 120.0
SPITZE = {1: 72.0, 3: BREITE}          # wie weit die Form nach rechts reicht
SAUM = 6.0                             # Breite eines Saums bei Form 4, senkrecht zur Kante


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), 3)


def flaeche(nummer, *stuecke):
    ringe = [[[runde(x), runde(-h)] for x, h in punkte] for punkte in stuecke]
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[ring + [ring[0]]] for ring in ringe]}}


def dreiecke(form, anzahl):
    laenge = SPITZE[form]
    mitte = HOEHE / 2
    spitzen = [laenge * k / anzahl for k in range(1, anzahl + 1)]
    teile = [flaeche(0, [(0, 0), (spitzen[0], mitte), (0, HOEHE)])]
    for k in range(1, anzahl):
        teile.append(flaeche(k, [(0, 0), (spitzen[k], mitte), (0, HOEHE), (spitzen[k - 1], mitte)]))
    return teile


def trapez(anzahl):
    """Kuwait: eher ein Trapez als ein Dreieck.

    Die lange Seite ist der ganze linke Rand, die kurze Seite steht bei 60 und reicht von 40 bis 80.
    Beide Zahlen sind das Raster: eine Feldbreite nach rechts, genau die mittlere Zeile hoch.

    Nur einfarbig. Mehrere Farben waeren ineinanderliegende Trapeze, und wie die sich verjuengen,
    entscheidet die erste Flagge, die sie braucht.
    """
    if anzahl != 1:
        raise SystemExit("Das Trapez gibt es bisher nur einfarbig, nicht mit %d Farben" % anzahl)
    return [flaeche(0, [(0, 0), (BREITE / 3, HOEHE / 3), (BREITE / 3, 2 * HOEHE / 3), (0, HOEHE)])]


def spur(anzahl):
    """Vanuatu, Suedafrika: ein Dreieck, dessen Saeume als liegendes Y bis zum rechten Rand laufen.

    Innen das Dreieck wie bei Form 1, Spitze bei 72. Jede weitere Farbe ist ein Saum darum, und
    anders als bei den Pfeilbaendern der Dreiecke laufen die Saeume PARALLEL: Jeder liegt im
    Abstand SAUM zur Kante des vorigen, senkrecht gemessen, trifft oben und unten mit voller Breite
    auf den Rand und laeuft als Arm bis zum rechten Rand -- so sieht das Y auf den echten Flaggen
    aus. Gezaehlt wird von innen nach aussen wie bei den Dreiecken. Jeder Saum laeuft mit dem Arm
    mit; Suedafrikas gelber Saum sitzt in Wahrheit nur am Dreieck.

    Bei GENAU zwei Farben schliesst aussen noch ein Saum in der Farbe des Dreiecks an: Vanuatus
    gelbes Y ist schwarz eingefasst. Er gehoert deshalb zu Flaeche 0 und ist kein eigener Wert --
    sonst stuenden drei Farben im Blatt, von denen zwei dieselbe waeren. Die Stuecke von Flaeche 0
    beruehren sich nur in den beiden linken Ecken der Leinwand. Ab drei Farben faellt er weg:
    Suedafrikas aeusserster Saum ist weiss und damit eine eigene Farbe.
    """
    mitte = HOEHE / 2
    spitze = SPITZE[1]
    steigung = mitte / spitze                        # der Kante von oben links zur Spitze
    schraeg = math.hypot(1, steigung)                # senkrechter Abstand -> Versatz in y

    def kante(k, y):
        """Wo die obere Kante der k-ten Form die Hoehe y erreicht; k = 0 ist das Dreieck selbst."""
        return (y + k * SAUM * schraeg) / steigung

    def oben(k):
        """Der obere Umriss der k-ten Form (k >= 1): Rand, Schraege, Arm."""
        arm = mitte - k * SAUM
        return [(kante(k, 0), 0), (kante(k, arm), arm), (BREITE, arm)]

    def spiegel(punkte):
        return [(x, HOEHE - y) for x, y in punkte]

    dreieck = [(0, 0), (spitze, mitte), (0, HOEHE)]
    if anzahl == 2:
        aussen = oben(2) + oben(1)[::-1]
        teile = [flaeche(0, dreieck, aussen, spiegel(aussen)[::-1])]
    else:
        teile = [flaeche(0, dreieck)]
    if anzahl > 1:                                   # der erste Saum umlaeuft die Spitze, ein Stueck
        o = oben(1)
        teile.append(flaeche(1, [(0, 0)] + o + spiegel(o)[::-1] + [(0, HOEHE), (spitze, mitte)]))
    for k in range(2, anzahl):
        # Ab hier trennt der Arm des inneren Saums den aeusseren in ein Stueck oben und eins unten.
        stueck = oben(k) + oben(k - 1)[::-1]
        teile.append(flaeche(k, stueck, spiegel(stueck)[::-1]))
    return teile


def schreibe(zielordner, name):
    teile = name.split("-")
    if teile[0] != "dreieck" or len(teile) not in (2, 3):
        raise SystemExit("Erwartet wird dreieck-<form>[-<farbanzahl>], nicht: " + name)
    form = int(teile[1])
    anzahl = int(teile[2]) if len(teile) == 3 else 1
    if form == 2:
        teile = trapez(anzahl)
    elif form == 4:
        teile = spur(anzahl)
    elif form in SPITZE:
        teile = dreiecke(form, anzahl)
    else:
        raise SystemExit("Form %d ist noch nicht gebaut: %s" % (form, name))
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
    print("%s  (%d Flaeche(n))" % (ziel, len(features)))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    for name in sys.argv[2:]:
        schreibe(sys.argv[1], name)

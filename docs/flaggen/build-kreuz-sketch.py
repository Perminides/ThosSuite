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
STREIFEN = 10.0             # ein Farbstreifen im dreifarbigen Arm; drei davon ergeben den Arm
STELLEN = 3


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), STELLEN)


def flaeche(nummer, *teile):
    """Eine Flaeche aus einem oder mehreren getrennten Stuecken.

    Gerechnet wird in Tiefe von oben, geschrieben mit negativem y.
    """
    ringe = [[[runde(x), runde(-h)] for x, h in folge] for folge in teile]
    return {"type": "Feature", "properties": {"id": nummer},
            "geometry": {"type": "MultiPolygon", "coordinates": [[r + [r[0]]] for r in ringe]}}


def rechteck(x0, h0, x1, h1):
    return [(x0, h0), (x1, h0), (x1, h1), (x0, h1)]


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


def dreifarbiges_kreuz():
    """Dominica: ein senkrechtes Kreuz, dessen Arme aus drei parallelen Farbstreifen bestehen.

    Die Frage im Deck heisst "drei parallele Farben" -- parallel und nicht geschachtelt. Das
    unterscheidet die Antwort vom fimbrierten Kreuz, wo eine Farbe die andere umrandet. Die Skizze
    muss diesen Unterschied tragen, sonst beantwortet sie die Frage nicht.

    Der Arm ist mit 30 breiter als die 20 des einfarbigen Kreuzes. Drei Streifen zu je 10 bleiben
    einzeln erkennbar; 20 durch 3 waere weder rund noch sichtbar. Die 10 gibt es im System schon.

    Am Kreuzungspunkt liegt der **senkrechte** Arm oben. Damit bleibt jeder waagerechte Streifen
    in zwei Stuecken stehen, links und rechts -- genau das Bild, das eine durchlaufende Spur ergibt.
    Jede Farbe ist eine Flaeche, egal aus wie vielen Stuecken.

    Nummeriert wird in Leserichtung: erst das Feld, dann die Streifen von links beziehungsweise von
    oben. Dominica steht im Blatt mit `Gruen|Gelb|Schwarz|Weiss` -- ein Feld, dann drei Streifen.
    """
    links, rechts = BREITE / 2 - 1.5 * STREIFEN, BREITE / 2 + 1.5 * STREIFEN
    oben, unten = HOEHE / 2 - 1.5 * STREIFEN, HOEHE / 2 + 1.5 * STREIFEN

    feld = flaeche(0, rechteck(0, 0, links, oben), rechteck(rechts, 0, BREITE, oben),
                   rechteck(0, unten, links, HOEHE), rechteck(rechts, unten, BREITE, HOEHE))

    x = [links + i * STREIFEN for i in range(4)]        # Raender der senkrechten Streifen
    h = [oben + i * STREIFEN for i in range(4)]         # Raender der waagerechten

    # Der erste Streifen haengt links mit seiner waagerechten Spur zusammen, der letzte rechts;
    # der mittlere steht allein. Deshalb drei Stuecklisten statt einer Schleife ueber alles.
    erste = flaeche(1, [(x[0], 0), (x[1], 0), (x[1], HOEHE), (x[0], HOEHE), (x[0], h[1]),
                        (0, h[1]), (0, h[0]), (x[0], h[0])],
                    rechteck(rechts, h[0], BREITE, h[1]))
    zweite = flaeche(2, rechteck(x[1], 0, x[2], HOEHE),
                     rechteck(0, h[1], links, h[2]), rechteck(rechts, h[1], BREITE, h[2]))
    dritte = flaeche(3, [(x[2], 0), (x[3], 0), (x[3], h[2]), (BREITE, h[2]), (BREITE, h[3]),
                         (x[3], h[3]), (x[3], HOEHE), (x[2], HOEHE)],
                     rechteck(0, h[2], links, h[3]))
    return [feld, erste, zweite, dritte]


KREUZE = {"kreuz-diagonal-uni": andreaskreuz,
          "kreuz-senkrecht-dreifarbig": dreifarbiges_kreuz}


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

"""Erzeugt die Hintergruende des Kreuz-Zweigs.

Aufruf:
    python build-kreuz-sketch.py <zielordner> kreuz-diagonal-uni ...

Der Name ist `kreuz-<ausrichtung>-<arme>`, genau wie der Generator ihn ableitet: `senkrecht`,
`diagonal` oder `beides`, dahinter `uni`, `dreifarbig`, `fimbriert` oder `unsichtbar`.

Gebaut werden hier `diagonal-uni`, `diagonal-unsichtbar`, `senkrecht-dreifarbig` und `senkrecht-fimbriert`. `senkrecht-uni`
liegt als handgemachte Datei daneben und bleibt es; `beides-fimbriert` ist der Union Jack und kommt aus
build-union-jack.py. Die uebrigen Armformen sind eigene Geometrie und kommen, wenn die erste Flagge sie
braucht.

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


def diagonal_unsichtbar():
    """Grenada: die Flaeche durch beide Eckdiagonalen in vier Dreiecke geteilt, ohne eigenes Band.

    "Nicht sichtbar" heisst, das Kreuz teilt die Flagge, hat aber keine eigene Farbe -- die vier
    Dreiecke stossen unmittelbar aneinander. Deshalb gibt es hier keine fuenfte Flaeche fuers
    Kreuz, nur die vier Felder, im Uhrzeigersinn ab dem obersten wie bei jedem Kreuz. Grenada
    stuende damit als `Gelb|Gruen|Gelb|Gruen` im Blatt. Rahmen, Kreis und Sterne sind eigene Teile.
    """
    mitte = (BREITE / 2, HOEHE / 2)
    oben = [(0, 0), (BREITE, 0), mitte]
    rechts = [(BREITE, 0), (BREITE, HOEHE), mitte]
    unten = [(BREITE, HOEHE), (0, HOEHE), mitte]
    links = [(0, HOEHE), (0, 0), mitte]
    return [flaeche(0, oben), flaeche(1, rechts), flaeche(2, unten), flaeche(3, links)]


def plus(links, rechts, oben, unten):
    """Ein Plus ueber die ganze Leinwand: senkrechter Arm links..rechts, waagerechter oben..unten."""
    return [(links, 0), (rechts, 0), (rechts, oben), (BREITE, oben), (BREITE, unten), (rechts, unten),
            (rechts, HOEHE), (links, HOEHE), (links, unten), (0, unten), (0, oben), (links, oben)]


def fimbriertes_kreuz():
    """Norwegen und Island: ein senkrechtes Kreuz mit schmalem andersfarbigem Saum.

    Der Saum umschliesst das Kreuz auf beiden Seiten -- geschachtelt, nicht parallel. Das
    unterscheidet ihn vom dreifarbigen Kreuz, wo drei Streifen nebeneinander laufen.

    Das innere Kreuz ist 20 breit, genau wie das einfarbige, damit es als dasselbe Kreuz gelesen
    wird. Der Saum ist 5 breit, schmaler als das Kreuz, sonst saehe er aus wie ein dritter Streifen.
    Zusammen 30, dieselbe Aussenbreite wie beim dreifarbigen Kreuz.

    Weil die Arme bis zum Leinwandrand laufen, ist der Saum kein Ring, sondern vier L-Stuecke, eins
    in jeder Ecke der Kreuzung. Sie beruehren sich nicht, das innere Kreuz liegt dazwischen.

    Drei Flaechen: das Feld in vier Stuecken, der Saum, das Kreuz. Wie beim dreifarbigen Kreuz ist
    das Feld eine Flaeche -- Island steht mit `Blau|Weiss|Rot` im Blatt, Norwegen waere `Rot|Weiss|Blau`.
    """
    mitte_x, mitte_h, halb, saum = BREITE / 2, HOEHE / 2, ARM / 2, 5.0
    il, ir, io, iu = mitte_x - halb, mitte_x + halb, mitte_h - halb, mitte_h + halb
    al, ar, ao, au = il - saum, ir + saum, io - saum, iu + saum
    feld = flaeche(0, rechteck(0, 0, al, ao), rechteck(ar, 0, BREITE, ao),
                   rechteck(ar, au, BREITE, HOEHE), rechteck(0, au, al, HOEHE))
    saeume = flaeche(1,
                     [(al, 0), (il, 0), (il, io), (0, io), (0, ao), (al, ao)],
                     [(ir, 0), (ar, 0), (ar, ao), (BREITE, ao), (BREITE, io), (ir, io)],
                     [(ir, iu), (BREITE, iu), (BREITE, au), (ar, au), (ar, HOEHE), (ir, HOEHE)],
                     [(0, iu), (il, iu), (il, HOEHE), (al, HOEHE), (al, au), (0, au)])
    return [feld, saeume, flaeche(2, plus(il, ir, io, iu))]


def dreifarbiges_kreuz():
    """Ein senkrechtes Kreuz, dessen Arme aus drei parallelen Farbstreifen bestehen (Dominica).

    Die Frage im Deck heisst "drei parallele Farben" -- parallel und nicht geschachtelt. Das
    unterscheidet die Antwort vom fimbrierten Kreuz, wo eine Farbe die andere umrandet.

    Die Geometrie ist **neutral**: neun Stuecke, die keine Farbzuordnung vorwegnehmen. Acht
    Randstreifen (je Arm die beiden aeusseren) und in der Mitte ein durchgehendes Plus aus den vier
    mittleren Streifen. An den vier einspringenden Ecken treffen zwei Randstreifen auf Gehrung
    aufeinander, so wie ein Bilderrahmen. Wer welche Farbe bekommt, entscheidet erst die Gruppierung
    unten -- und damit das Blatt.

    Der Arm ist mit 30 breiter als die 20 des einfarbigen Kreuzes. Drei Streifen zu je 10 bleiben
    einzeln erkennbar; 20 durch 3 waere weder rund noch sichtbar. Die 10 gibt es im System schon.
    """
    x0, x1, x2, x3 = 75.0, 85.0, 95.0, 105.0        # Raender der senkrechten Streifen
    y0, y1, y2, y3 = 45.0, 55.0, 65.0, 75.0         # Raender der waagerechten

    stuecke = {
        # Senkrechte Randstreifen, oben und unten vom Kreuz getrennt durch das mittlere Plus.
        "ol": [(x0, 0), (x1, 0), (x1, y1), (x0, y0)],
        "or": [(x2, 0), (x3, 0), (x3, y0), (x2, y1)],
        "ul": [(x0, y3), (x1, y2), (x1, HOEHE), (x0, HOEHE)],
        "ur": [(x2, y2), (x3, y3), (x3, HOEHE), (x2, HOEHE)],
        # Waagerechte Randstreifen, links und rechts.
        "lo": [(0, y0), (x0, y0), (x1, y1), (0, y1)],
        "lu": [(0, y2), (x1, y2), (x0, y3), (0, y3)],
        "ro": [(x3, y0), (BREITE, y0), (BREITE, y1), (x2, y1)],
        "ru": [(x2, y2), (BREITE, y2), (BREITE, y3), (x3, y3)],
        # Das mittlere Plus -- die vier mittleren Streifen haengen durch die Mitte zusammen.
        "plus": [(x1, 0), (x2, 0), (x2, y1), (BREITE, y1), (BREITE, y2), (x2, y2), (x2, HOEHE),
                 (x1, HOEHE), (x1, y2), (0, y2), (0, y1), (x1, y1)],
    }

    feld = [rechteck(0, 0, x0, y0), rechteck(x3, 0, BREITE, y0),
            rechteck(0, y3, x0, HOEHE), rechteck(x3, y3, BREITE, HOEHE)]

    # Gruppierung: welche Stuecke eine abfragbare Flaeche bilden. Dominica faerbt die senkrechten
    # Streifen von links nach rechts und die waagerechten von oben nach unten gleich -- links und
    # oben tragen also dieselbe Farbe, rechts und unten die andere. Dass dabei oben links und unten
    # rechts sichtbar verschmelzen und die beiden anderen Ecken nicht, faellt aus der Faerbung und
    # nicht aus der Geometrie. Eine Flagge, die den Rahmen durchgehend einfarbig zieht, bekaeme
    # dieselben neun Stuecke in einer anderen Gruppierung.
    gruppen = [["ol", "ul", "lo", "ro"], ["plus"], ["or", "ur", "lu", "ru"]]

    features = [flaeche(0, *feld)]
    for nummer, gruppe in enumerate(gruppen, start=1):
        features.append(flaeche(nummer, *[stuecke[name] for name in gruppe]))
    return features


KREUZE = {"kreuz-diagonal-uni": andreaskreuz,
          "kreuz-diagonal-unsichtbar": diagonal_unsichtbar,
          "kreuz-senkrecht-dreifarbig": dreifarbiges_kreuz,
          "kreuz-senkrecht-fimbriert": fimbriertes_kreuz}


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

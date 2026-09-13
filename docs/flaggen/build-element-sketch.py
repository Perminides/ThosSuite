"""Erzeugt die Strukturdateien der Zusatzelemente.

Aufruf:
    python build-element-sketch.py <zielordner>

Konvention (siehe Flaggen-Deck.md, §5):
  * Elementdateien sind um den Nullpunkt **zentriert**. Damit bleibt beim Skalieren der
    Mittelpunkt stehen -- Groesse und Lage haengen nicht aneinander. Hintergrunddateien sind
    das Gegenteil: Sie fuellen die Leinwand 0..180 / -120..0.
  * Die Datei traegt ihre natuerliche Groesse. Die meisten fuellen ein Rasterfeld (60 x 40);
    Kreis und Raute sind groesser, weil ihre Figuren es sind. Kein Groessenfaktor im Generator.
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
    """Rund bleibt rund: Point plus radius, kein Vieleck.

    Radius 25 statt der 20 eines Rasterfeldes: Der Kreis ist von Natur aus gross, und wo er einen
    Behaelter abgibt, brauchen seine Kinder Platz. Die Groesse steht hier und nicht als Faktor im
    Generator -- eine Datei sagt selbst, wie gross ihre Figur ist.
    """
    return [kreisflaeche(0, 0, 0, 25.0)]


def einzelstern():
    return [flaeche(0, [stern(0, 0, FELD_Y)])]


def raute():
    """Zwei Rasterfelder breit und hoch -- Brasiliens Raute spannt fast die halbe Flagge."""
    return [flaeche(0, [ring([(0, 2 * FELD_Y), (2 * FELD_X, 0), (0, -2 * FELD_Y), (-2 * FELD_X, 0)])])]


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


def kreuz():
    """Ein gleicharmiges Kreuz: Georgien (vier verstreut), Griechenland und Tonga (Gösch), Schweiz.

    Die Höhe bindet, also 40 hoch und 40 breit. Die Armstärke ist drei Zehntel der Spannweite --
    das Schweizer Verhältnis, Arm 6 breit und 7 lang. Kräftig genug, dass es auch in Georgiens
    Vierergruppe verkleinert noch als Kreuz und nicht als Strich gelesen wird.
    """
    return [flaeche(0, [plus(0, 0, 0.3 * FELD_Y, FELD_Y)])]


def dreisterne():
    """Drei im Dreieck, Spitze oben -- die haeufigste echte Anordnung."""
    return [flaeche(0, [stern(0, 9, 7.5), stern(-10, -7, 7.5), stern(10, -7, 7.5)])]


def viersterne():
    """Vier im Quadrat. Eine Raute waere die Alternative, sie kollidiert aber mit dem Element."""
    return [flaeche(0, [stern(x, y, 7) for x in (-10, 10) for y in (-9, 9)])]


def fuenfsterne():
    """Fuenf im gleichmaessigen Ring, einer oben."""
    return [flaeche(0, [stern(12 * math.cos(math.radians(90 + i * 72)),
                              12 * math.sin(math.radians(90 + i * 72)), 6) for i in range(5)])]


# Acht Mittelpunkte, per Ablehnungsprobe mit festem Startwert gesetzt und danach eingefroren:
# zufaellig aussehend, aber bei jedem Lauf gleich. Der kleinste Spalt betraegt 1,5 -- so beruehrt
# sich nichts und beim Fuellen verschmilzt nichts zu einem Klecks.
HAUFEN = [(2.15, -9.33), (-14.61, 4.33), (11.15, -4.26), (4.16, 7.54),
          (-15.47, -5.86), (-4.62, 1.03), (16.66, 5.76), (-6.12, 10.42)]


def sternhaufen():
    """Mehr als fuenf: ein ungeordneter Haufen.

    Bis vier oder fuenf erfasst man eine Anzahl auf einen Blick, darueber liest man nur noch
    "viele" -- genau da hoert die Skizze auf, genau zu sein. Acht sind es, aber bei Feldgroesse
    zaehlt sie niemand nach; die Unordnung sagt, dass die Zahl nicht gemeint ist.

    Radius 7 statt der 4, mit denen die Mittelpunkte gesetzt wurden: Bei 4 war die Schraffur der
    Markierung in den schmalen Zacken kaum zu erkennen. Zwei, drei Zacken beruehren sich dadurch --
    das ist gewollt und faellt in der Unordnung nicht auf.
    """
    return [flaeche(0, [stern(x, y, 7.0) for x, y in HAUFEN])]


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


def strich(cx, cy, richtung, halbe_laenge, halbbreite):
    """Ein Strich mit runden Enden: zwei Halbkreise, verbunden durch ihre Flanken.

    {@code halbe_laenge} ist der halbe Abstand der beiden Kappenmittelpunkte; die sichtbare Laenge
    ist also zwei mal halbe_laenge plus zwei mal halbbreite.
    """
    rx, ry = richtung
    vorn = (cx + rx * halbe_laenge, cy + ry * halbe_laenge)
    hinten = (cx - rx * halbe_laenge, cy - ry * halbe_laenge)
    # Gerade Bogenzahl: Dann liegt ein Punkt genau auf der Spitze und der Strich wird exakt so lang,
    # wie er soll. Bei sieben Abschnitten fehlten am Ende 0,4 Prozent.
    return ring(kappe(vorn[0], vorn[1], richtung, halbbreite, 8)
                + kappe(hinten[0], hinten[1], (-rx, -ry), halbbreite, 8))


def muster():
    """Neun Striche im Schachbrett -- ein Ausschnitt aus einem Ornamentband, nicht das Band selbst.

    Waagerecht, wo Spalte plus Zeile gerade ist, sonst senkrecht; so steht es in der Vorlage.
    Drei mal drei und damit quadratisch: Das Element wird uebereinandergestapelt, und ein breiterer
    Ausschnitt fuellte die Mastspalte aus und saehe nicht mehr nach Band aus.

    Nichts beruehrt sich. Gleich ausgerichtete Nachbarn stehen zwei Rasterschritte auseinander,
    quer stehende ueberlappen sich nicht -- ein Strich ist halb so dick wie lang.
    """
    raster = 2 * FELD_Y / 3          # drei Reihen fuellen die Feldhoehe
    dicke = raster / 2
    halbbreite = dicke / 2
    halbe_laenge = raster / 2 - halbbreite     # sichtbare Laenge bleibt genau ein Raster
    teile = []
    for zeile in range(3):
        for spalte in range(3):
            cx, cy = (spalte - 1) * raster, (1 - zeile) * raster
            richtung = (1.0, 0.0) if (spalte + zeile) % 2 == 0 else (0.0, 1.0)
            teile.append(strich(cx, cy, richtung, halbe_laenge, halbbreite))
    return [flaeche(0, teile)]


def landumriss():
    """Ein erfundenes Land: Kosovo und Zypern, beide mit ihrem Umriss auf der Flagge.

    Kein echtes Land -- wie beim Vogel soll die Silhouette zu keiner bestimmten Flagge passen. Was
    einen Umriss nach Land aussehen laesst, ist die Mischung: Grenzen mit wenigen deutlichen Knicken,
    daneben eine zerklueftete Kueste. Ueberall gleich feines Rauschen saehe nach einem Blatt aus.

    Die Eckpunkte stehen fest, dazwischen wird gewuerfelt, mit festem Startwert 202 -- bei ihm kreuzt
    sich der Umriss nicht. Kueste: Mittelpunktverschiebung in fuenf Stufen. Grenze: ein bis vier
    Zwischenpunkte mit mittlerem Ausschlag. Danach eingepasst wie ein Rasterfeld und mit Douglas-
    Peucker ausgeduennt, damit die Kueste rau bleibt, ohne dass die Datei aufquillt.
    """
    import random
    rnd = random.Random(202)

    def zerklueftet(a, b, tiefe, rauheit):
        if tiefe == 0:
            return [a]
        dx, dy = b[0] - a[0], b[1] - a[1]
        laenge = math.hypot(dx, dy) or 1e-9
        d = rnd.uniform(-1, 1) * laenge * rauheit
        m = ((a[0] + b[0]) / 2 - dy / laenge * d, (a[1] + b[1]) / 2 + dx / laenge * d)
        return zerklueftet(a, m, tiefe - 1, rauheit * 0.62) + zerklueftet(m, b, tiefe - 1, rauheit * 0.62)

    def geknickt(a, b, knicke, ausschlag, bogen):
        dx, dy = b[0] - a[0], b[1] - a[1]
        laenge = math.hypot(dx, dy) or 1e-9
        nx, ny = -dy / laenge, dx / laenge
        punkte = [a]
        for i in range(1, knicke + 1):
            t = (i + rnd.uniform(-0.25, 0.25)) / (knicke + 1)
            versatz = bogen * laenge * math.sin(math.pi * t) + rnd.uniform(-ausschlag, ausschlag) * laenge
            punkte.append((a[0] + dx * t + nx * versatz, a[1] + dy * t + ny * versatz))
        return punkte

    def ausduennen(punkte, toleranz):
        """Douglas-Peucker fuer einen offenen Linienzug."""
        if len(punkte) < 3:
            return punkte
        a, b = punkte[0], punkte[-1]
        dx, dy = b[0] - a[0], b[1] - a[1]
        laenge = math.hypot(dx, dy)
        weit, index = -1.0, 0
        for i in range(1, len(punkte) - 1):
            px, py = punkte[i]
            d = (abs(dy * px - dx * py + b[0] * a[1] - b[1] * a[0]) / laenge if laenge
                 else math.hypot(px - a[0], py - a[1]))
            if d > weit:
                weit, index = d, i
        if weit <= toleranz:
            return [a, b]
        return ausduennen(punkte[:index + 1], toleranz)[:-1] + ausduennen(punkte[index:], toleranz)

    # Eckpunkte im Uhrzeigersinn, y nach oben. K = Kueste, G = geschwungene Grenze, S = gerade Grenze.
    ecken = [(-14, 8), (-2, 10), (9, 7), (12, 1), (8, -3), (11, -10), (7, -14), (3, -6), (-4, -5), (-11, -7), (-15, -1)]
    arten = ["S", "G", "K", "K", "K", "K", "K", "K", "G", "G", "G"]
    punkte = []
    for i, art in enumerate(arten):
        a, b = ecken[i], ecken[(i + 1) % len(ecken)]
        if art == "K":
            punkte += zerklueftet(a, b, 5, 0.32)
        elif art == "G":
            punkte += geknickt(a, b, rnd.randint(2, 4), 0.09, rnd.uniform(-0.05, 0.05))
        else:
            punkte += geknickt(a, b, rnd.randint(1, 2), 0.06, 0.0)

    xs, ys = [p[0] for p in punkte], [p[1] for p in punkte]
    faktor = min(2 * FELD_X / (max(xs) - min(xs)), 2 * FELD_Y / (max(ys) - min(ys)))
    mx, my = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    punkte = [((x - mx) * faktor, (y - my) * faktor) for x, y in punkte]
    punkte = ausduennen(punkte + [punkte[0]], 0.03)[:-1]
    return [flaeche(0, [ring(punkte)])]


ELEMENTE = {"kreis": kreis, "sichel": sichel, "stern": einzelstern, "raute": raute, "schrift-t": schrift_t,
            "stern-haufen": sternhaufen, "stern-zwei": zweisterne, "muster": muster, "kreuz": kreuz, "landumriss": landumriss,
            "stern-drei": dreisterne, "stern-vier": viersterne, "stern-fuenf": fuenfsterne}


def schreibe(zielordner, name):
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"comment": "Elementdatei: um den Nullpunkt zentriert, in ihrer natuerlichen Groesse.",',
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

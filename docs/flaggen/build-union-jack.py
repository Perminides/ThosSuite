"""Erzeugt den Union Jack als Strukturdatei -- gerechnet, nicht abgezeichnet.

Aufruf:
    python build-union-jack.py <zielordner> element
    python build-union-jack.py <zielordner> hintergrund

Warum ein eigenes Skript: Der Union Jack ist das einzige Element mit **mehreren Flaechen in
festen Farben**, und dieselbe Zeichnung wird zweimal gebraucht -- einmal auf ein Rasterfeld
normiert (60 x 40) und einmal als Hintergrund auf der ganzen Leinwand (180 x 120). Beides
faellt aus derselben Rechnung, weil das Seitenverhaeltnis mit 3:2 dasselbe ist.

Die Masse stehen als Bruchteile der HOEHE, so wie es die amtliche Beschreibung tut. Damit
stimmt die Zeichnung in jedem Seitenverhaeltnis mit sich selbst ueberein; das Original ist
2:1 und hier eben 3:2, die Diagonalen laufen also steiler.

Der Gegenwechsel -- dass die roten Schraegbaender an der Mitte springen -- ist nachgemessen und
nicht erinnert: in der linken Haelfte liegt Rot **unterhalb** der Diagonalen, in der rechten
**oberhalb**. Vertauscht waere die Flagge auf dem Kopf.

Gerechnet wird ueber die vollstaendige Anordnung aller Schnittgeraden: Die Leinwand wird an
jeder Geraden zerteilt, jede entstandene Zelle bekommt ihre Farbe, und gleichfarbige Zellen
werden wieder verschmolzen, indem sich gemeinsame Kanten paarweise aufheben. Der Umweg lohnt
sich, weil so keine Naht uebrig bleibt: Eine ungefuellte Flaeche ist umrandet, und eine Naht
mitten im weissen Kreuz saehe nach einer Grenze aus, die es nicht gibt.
"""
import json
import sys
from pathlib import Path

NL = chr(10)

# Bruchteile der Hoehe. Das Schraegband wird wie das Kreuz SENKRECHT gemessen, nicht quer.
KREUZ_ROT = 1 / 5           # das durchgehende rote Kreuz
KREUZ_SAUM = 1 / 15         # weisser Saum daneben, je Seite
SCHRAEG_WEISS = 1 / 5       # weisses Schraegband, gesamt
SCHRAEG_ROT = 1 / 15        # rotes Band darin, an der Diagonalen anliegend

RASTER = 4                  # Nachkommastellen, auf die zum Kantenvergleich gerundet wird
STELLEN = 3                 # Nachkommastellen in der Datei
WINZIG = 1e-9

MASSE = {"element": (60.0, 40.0), "hintergrund": (180.0, 120.0)}
FARBEN = ["Blau", "Weiß", "Rot"]


def geraden(w, h):
    """Alle Geraden, an denen sich eine Farbe aendern kann, als (a, b, c) mit a*x + b*y + c."""
    alle = [(1.0, 0.0, -w / 2)]                                  # die Mitte: dort springt der Gegenwechsel
    for d in (KREUZ_ROT / 2, KREUZ_ROT / 2 + KREUZ_SAUM):
        alle += [(1.0, 0.0, -(w / 2 + h * d)), (1.0, 0.0, -(w / 2 - h * d)),
                 (0.0, 1.0, -(h / 2 + h * d)), (0.0, 1.0, -(h / 2 - h * d))]
    for versatz in (-SCHRAEG_WEISS / 2, -SCHRAEG_ROT, 0.0, SCHRAEG_ROT, SCHRAEG_WEISS / 2):
        alle.append((h / w, -1.0, h * versatz))                  # y = x*h/w + versatz
        alle.append((h / w, 1.0, -h + h * versatz))              # y = h - x*h/w + versatz
    return alle


def farbe_bei(x, y, w, h):
    """Die Farbe an einer Stelle. Das Kreuz sticht das Schraegband, so liegt es uebereinander."""
    if abs(x - w / 2) <= h * KREUZ_ROT / 2 or abs(y - h / 2) <= h * KREUZ_ROT / 2:
        return "Rot"
    saum = h * (KREUZ_ROT / 2 + KREUZ_SAUM)
    if abs(x - w / 2) <= saum or abs(y - h / 2) <= saum:
        return "Weiß"
    for diagonale in (x * h / w, h - x * h / w):
        abstand = y - diagonale
        if abs(abstand) <= h * SCHRAEG_WEISS / 2:
            links = x < w / 2
            rot = 0 <= abstand <= h * SCHRAEG_ROT if links else -h * SCHRAEG_ROT <= abstand <= 0
            return "Rot" if rot else "Weiß"
    return "Blau"


def halbebene(zelle, gerade, seite):
    """Sutherland-Hodgman: der Teil der Zelle auf einer Seite der Geraden."""
    a, b, c = gerade
    raus = []
    for i in range(len(zelle)):
        p, q = zelle[i], zelle[(i + 1) % len(zelle)]
        wp = seite * (a * p[0] + b * p[1] + c)
        wq = seite * (a * q[0] + b * q[1] + c)
        if wp >= -WINZIG:
            raus.append(p)
        if (wp > WINZIG) != (wq > WINZIG) and abs(wp - wq) > WINZIG:
            t = wp / (wp - wq)
            raus.append((p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])))
    return raus


def inhalt(ring):
    """Der doppelte Flaecheninhalt, vorzeichenbehaftet."""
    return sum(ring[i][0] * ring[(i + 1) % len(ring)][1] - ring[(i + 1) % len(ring)][0] * ring[i][1]
               for i in range(len(ring)))


def zellen(w, h):
    """Die Leinwand, an jeder Geraden zerteilt. Danach stossen Zellen immer Kante an Kante."""
    teile = [[(0.0, 0.0), (w, 0.0), (w, h), (0.0, h)]]
    for gerade in geraden(w, h):
        zerteilt = []
        for zelle in teile:
            for seite in (1, -1):
                stueck = halbebene(zelle, gerade, seite)
                if len(stueck) >= 3 and abs(inhalt(stueck)) > WINZIG:
                    zerteilt.append(stueck)
        teile = zerteilt
    # Einmal fangen, und ab hier rechnet alles mit denselben Zahlen -- sonst passt die
    # Flaeche der gerundeten Ringe nicht zu der der ungerundeten Zellen.
    gerundet = [entdoppelt([gefangen(p) for p in zelle]) for zelle in teile]
    return [zelle for zelle in gerundet if len(zelle) >= 3 and abs(inhalt(zelle)) > WINZIG]


def entdoppelt(ring):
    return [p for i, p in enumerate(ring) if p != ring[i - 1]]


def gefangen(punkt):
    return (round(punkt[0], RASTER), round(punkt[1], RASTER))


def verschmilz(teile):
    """Gleichfarbige Zellen zu Ringen: gemeinsame Kanten heben sich paarweise auf.

    Die Zellen liegen alle gleich herum, eine gemeinsame Kante taucht also einmal vorwaerts
    und einmal rueckwaerts auf. Was uebrig bleibt, ist der aeussere Rand.
    """
    offen = {}
    for zelle in teile:
        for i in range(len(zelle)):
            a, b = zelle[i], zelle[(i + 1) % len(zelle)]
            if offen.pop((b, a), None) is None:
                offen[(a, b)] = True

    folgt = {}
    for a, b in offen:
        folgt.setdefault(a, []).append(b)
    ringe = []
    while folgt:
        start = next(iter(folgt))
        ring, hier = [start], start
        while True:
            if hier not in folgt:
                raise SystemExit("Der Rand bricht ab -- die Zellen passen nicht zusammen.")
            weiter = folgt[hier].pop()
            if not folgt[hier]:
                del folgt[hier]
            if weiter == start:
                break
            ring.append(weiter)
            hier = weiter
        ringe.append(ring)
    return ringe


def pruefe(teile, ringe, name):
    """Verschmelzen darf nichts verlieren und nichts hinzuerfinden."""
    vorher = sum(abs(inhalt(z)) for z in teile) / 2
    nachher = abs(sum(inhalt(r) for r in ringe)) / 2
    if abs(vorher - nachher) > 1e-6 * max(1.0, vorher):
        raise SystemExit("%s: %.4f vor dem Verschmelzen, %.4f danach" % (name, vorher, nachher))


def flaechen(w, h):
    """Je Farbe eine Flaeche, in der Reihenfolge von FARBEN."""
    nach_farbe = {name: [] for name in FARBEN}
    for zelle in zellen(w, h):
        mx = sum(p[0] for p in zelle) / len(zelle)
        my = sum(p[1] for p in zelle) / len(zelle)
        nach_farbe[farbe_bei(mx, my, w, h)].append(zelle)
    for name in FARBEN:
        if not nach_farbe[name]:
            raise SystemExit("Keine einzige Zelle in " + name)
        ringe = verschmilz(nach_farbe[name])
        pruefe(nach_farbe[name], ringe, name)
        yield name, ringe


def zeichne(w, h, art):
    """Aus Flaggenkoordinaten (y nach unten) die Dateikoordinaten der jeweiligen Art."""
    mittig = art == "element"
    for nummer, (name, ringe) in enumerate(flaechen(w, h)):
        teile = []
        for ring in ringe:
            punkte = [[round(x - w / 2 if mittig else x, STELLEN),
                       round(h / 2 - y if mittig else -y, STELLEN)] for x, y in ring]
            teile.append(punkte + [punkte[0]])
        # Die Farbe steht nur in der Elementdatei: Dort ist sie die Aussage "wird nicht gefragt".
        # Als Hintergrund ist der Union Jack eine ganz normale Skizze und wird gefragt wie jede andere.
        eigenschaften = {"id": nummer, "farbe": name} if mittig else {"id": nummer}
        yield {"type": "Feature", "properties": eigenschaften,
               "geometry": {"type": "MultiPolygon", "coordinates": [[t] for t in teile]}}


def schreibe(zielordner, art):
    if art not in MASSE:
        raise SystemExit("Nur element oder hintergrund, nicht: " + art)
    w, h = MASSE[art]
    hinweis = ("Elementdatei: um den Nullpunkt zentriert, Faktor 1,0 fuellt ein Rasterfeld."
               " Die Farben stehen in properties.farbe fest und werden nicht gefragt."
               if art == "element" else "Hintergrunddatei: fuellt die Leinwand 180 x 120.")
    features = [json.dumps(f, ensure_ascii=False) for f in zeichne(w, h, art)]
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "union-jack",',
            '"comment": "%s",' % hinweis,
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"features": [']
    text = NL.join(kopf) + NL + ("," + NL).join(features) + NL + "]" + NL + "}" + NL

    ziel = Path(zielordner) / "union-jack.geojson"
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    print("%s  (%d x %d, %d Flaechen)" % (ziel, w, h, len(features)))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    schreibe(sys.argv[1], sys.argv[2])

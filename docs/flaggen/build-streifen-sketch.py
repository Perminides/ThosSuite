"""Erzeugt Strukturdateien fuer reine Streifen-Skizzen.

Aufruf:
    python build-streifen-sketch.py <zielordner> waagerecht-3 senkrecht-4 ...

Konvention (siehe Flaggen-Deck.md):
  * Das Seitenverhaeltnis ist immer 3:2 -- die Skizze wird in ein 3:2-Feld eingepasst,
    ein anderes Verhaeltnis wuerde gestaucht.
  * Die Leinwand ist IMMER 180 x 120. Sie ist keine Kosmetik: SketchPane rechnet das
    3x3-Raster als Drittel der zuerst geladenen Skizze, und Elementdateien sind auf ein
    Feld von 60 x 40 normiert. Eine abweichende Leinwand macht jedes angehaengte Element
    ein wenig zu klein.
  * Dezimalstellen bei Streifenzahlen, die 120 nicht teilen, sind Absicht und harmlos:
    Der Leser liest double, und Unterkante i und Oberkante i+1 tragen denselben Wert,
    es kann also keine Luecke entstehen.
  * Y ist negativ, wie in den Kartendateien -- der Leser invertiert beim Einlesen.
  * In properties steht nur die Flaechennummer, nullbasiert; bei waagerecht von oben,
    bei senkrecht von links.
"""
import json
import sys
from pathlib import Path

NL = chr(10)


BREITE, HOEHE = 180, 120
GOESCH = "-goesch"

# Breitenverhaeltnisse je Verteilungswert. Ohne Eintrag sind alle Streifen gleich breit.
# Schematisch, nicht massstabsgetreu: Nordkoreas duenne Streifen sind in Wirklichkeit halb so
# dick wie hier -- so duenn wuerden sie in der Skizze zu Strichen.
VERTEILUNG = {
    ("waagerecht", 5, 3): [2, 1, 4, 1, 2],      # Mitte breiter und 2 und 4 duenn
}


def masse(anzahl):
    return BREITE, HOEHE


def ring(x0, y0, x1, y1):
    return [[runde(x0), runde(y0)], [runde(x1), runde(y0)], [runde(x1), runde(y1)],
            [runde(x0), runde(y1)], [runde(x0), runde(y0)]]


def runde(wert):
    """Ganze Zahlen bleiben ganz, damit die Dateien lesbar bleiben."""
    return int(wert) if float(wert).is_integer() else round(float(wert), 3)


def ohne_goesch(x0, y0, x1, y1, gx, gy):
    """Ein Streifen, aus dem das Goesch-Feld herausgeschnitten ist.

    Ueberdecken statt schneiden reicht NICHT: Eine ungefuellte Flaeche ist durchsichtig, und
    weil die Farbreihenfolge gewuerfelt wird, saehe man die Streifen im Goesch, bis der selbst
    an der Reihe ist. Weggeschnitten ist weg.

    Drei Faelle: ganz im Goesch-Band (der Streifen faengt rechts davon an), ganz darunter
    beziehungsweise daneben (unveraendert), oder dazwischen -- dann ein L.
    """
    if x0 >= gx or y0 <= gy:                       # beruehrt den Goesch gar nicht
        return ring(x0, y0, x1, y1)
    if y1 >= gy:                                   # liegt vollstaendig im Goesch-Band
        return ring(gx, y0, x1, y1)
    return [[runde(v) for v in p] for p in
            [[gx, y0], [x1, y0], [x1, y1], [x0, y1], [x0, gy], [gx, gy], [gx, y0]]]


def grenzen(richtung, anzahl, verteilung, laenge):
    """Die Kanten der Streifen, von 0 bis laenge. Ohne Verhaeltnis gleichmaessig geteilt."""
    gewichte = VERTEILUNG.get((richtung, anzahl, verteilung), [1] * anzahl)
    if len(gewichte) != anzahl:
        raise SystemExit("Das Verhaeltnis passt nicht zur Streifenzahl: %s" % gewichte)
    gesamt = sum(gewichte)
    kanten, summe = [0.0], 0
    for g in gewichte:
        summe += g
        kanten.append(summe * laenge / gesamt)
    return kanten


def flaechen(richtung, anzahl, goesch, verteilung):
    """Die Streifen, und danach -- falls verlangt -- der Goesch als letzte Flaeche.

    Der Goesch belegt immer genau Rasterfeld 0 und passt sich der Streifenzahl NICHT an: Ein
    Sketch, der seine Groesse nach den Streifen richtete, verriete etwas, das keine Frage
    beantwortet hat. Umgekehrt duerfen die Streifen sehr wohl an ihm enden -- dass sie das tun,
    folgt aus der Streifenzahl und der Goesch-Frage, die beide schon beantwortet sind.
    """
    breite, hoehe = masse(anzahl)
    gx, gy = breite / 3, -hoehe / 3
    kanten = grenzen(richtung, anzahl, verteilung, hoehe if richtung == "waagerecht" else breite)
    for i in range(anzahl):
        if richtung == "waagerecht":
            x0, y0, x1, y1 = 0, -kanten[i], breite, -kanten[i + 1]
        else:
            x0, y0, x1, y1 = kanten[i], 0, kanten[i + 1], -hoehe
        r = ohne_goesch(x0, y0, x1, y1, gx, gy) if goesch else ring(x0, y0, x1, y1)
        yield {"type": "Feature",
               "properties": {"id": i},
               "geometry": {"type": "MultiPolygon", "coordinates": [[r]]}}
    if goesch:
        yield {"type": "Feature",
               "properties": {"id": anzahl},
               "geometry": {"type": "MultiPolygon", "coordinates": [[ring(0, 0, gx, gy)]]}}


def schreibe(zielordner, name):
    goesch = name.endswith(GOESCH)
    teile = (name[:-len(GOESCH)] if goesch else name).split("-")
    richtung, anzahl = teile[0], teile[1]
    verteilung = int(teile[2]) if len(teile) > 2 else None
    if richtung not in ("waagerecht", "senkrecht"):
        raise SystemExit("Nur waagerecht-<n>, senkrecht-<n>, wahlweise mit -goesch, nicht: " + name)

    # Ein Feature je Zeile, wie in den Kartendateien -- so bleibt eine Aenderung im Diff lesbar.
    kopf = ['{',
            '"type": "FeatureCollection",',
            '"name": "%s",' % name,
            '"crs": { "type": "name", "properties": { "name": "urn:ogc:def:crs:EPSG::3857" } },',
            '"xy_coordinate_resolution": 1,',
            '"features": [']
    features = [json.dumps(f) for f in flaechen(richtung, int(anzahl), goesch, verteilung)]
    text = NL.join(kopf) + NL + ("," + NL).join(features) + NL + "]" + NL + "}" + NL

    ziel = Path(zielordner) / (name + ".geojson")
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(text, encoding="utf-8")
    breite, hoehe = masse(int(anzahl))
    print("%s  (%d x %d, %d Flaechen)" % (ziel, breite, hoehe, int(anzahl) + (1 if goesch else 0)))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    for name in sys.argv[2:]:
        schreibe(sys.argv[1], name)

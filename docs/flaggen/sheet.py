"""Gemeinsamer Zugriff auf das Flaggen-Sheet fuer die Skripte daneben.

Ein Blatt, eine Zeile pro Flagge: 'Systematik'. Es traegt links die Hintergrund-
Attribute und rechts die Zusatzelemente. Die frueheren getrennten Blaetter
'Hintergrund' und 'Zusatzelemente' sind darin aufgegangen.

    from sheet import lade
    b = lade()                 # holt das Blatt und schreibt systematik.csv daneben
    b = lade(neu_holen=False)  # nimmt die vorhandene systematik.csv

Das Blatt wird NICHT ueber Spaltenpositionen gelesen. Der Kopf ist die Zeile, die
irgendwo "Signatur" traegt; wo der Attributblock anfaengt, wird gesucht: Es ist die
Stelle, ab der sich die Signatur aus den folgenden Spalten wieder zusammensetzen
laesst. Damit brechen eingefuegte, verschobene oder geloeschte Spalten nichts mehr --
genau das ist beim Aufraeumen am 26.08.2026 einmal passiert und soll sich nicht
wiederholen.

Achtung beim Aufloesen ueber Ueberschriften: Ein Blatt kann eine Ueberschrift
doppelt tragen (frueher standen "3W", "5W" und "Dreiecksflaechen" zweimal drin,
einmal im Attributblock und einmal in alten Arbeitsspalten). Deshalb liefert
`spalte()` fuer Attribute die Position aus dem gefundenen Block und nicht die
letzte gleichnamige Spalte.
"""
import csv
import io
import urllib.request
from pathlib import Path

SHEET = "1FX8SgpOr9G_Ss030KkQDAtOUE3AbEHuMPfxpl3PBQdE"
GID = 184257391          # Systematik. gid ist eine Id, keine Position -- Verschieben aendert sie nicht.
DATEI = "systematik.csv"
HIER = Path(__file__).resolve().parent


def hole(gid=GID):
    """Holt einen Tab als CSV-Text. Ohne gid liefert der Export den ersten Tab, nicht den gewuenschten."""
    url = f"https://docs.google.com/spreadsheets/d/{SHEET}/export?format=csv&gid={gid}"
    return urllib.request.urlopen(url, timeout=60).read().decode("utf-8")


class Blatt:
    """Die gelesene Tabelle samt der Stellen, an denen die Attribute stehen."""

    def __init__(self, rows):
        self.rows = rows
        self.hdr = next(i for i, r in enumerate(rows) if any(c.strip() == "Signatur" for c in r))
        kopf = rows[self.hdr]
        self.sig = next(i for i, c in enumerate(kopf) if c.strip() == "Signatur")

        # Datenzeile = Land in Spalte 2 und eine Signatur. Der Bildpfad taugt NICHT als
        # Kriterium: Neu angelegte Laender haben noch keinen, und sie waeren dann still
        # verschwunden statt aufzufallen (am 26.08.2026 genau so passiert, 7 Zeilen).
        self.zeilen = [r for r in rows[self.hdr + 1:]
                       if len(r) > self.sig and r[2].strip() and r[self.sig].strip()]
        self.ohne_signatur = [r[2].strip() for r in rows[self.hdr + 1:]
                              if len(r) > 2 and r[2].strip()
                              and (len(r) <= self.sig or not r[self.sig].strip())]
        self.ohne_bild = [r[2].strip() for r in self.zeilen
                          if not (r[0].startswith("..") or r[0].startswith("http"))]
        if not self.zeilen:
            raise RuntimeError("Keine Datenzeilen gefunden -- ist der richtige Tab geladen?")

        self.n_attr = len(self.zeilen[0][self.sig].split("|"))
        self.attr_start = self._suche_attributblock()
        self.name = [kopf[i].strip() for i in range(self.attr_start, self.attr_start + self.n_attr)]

        # Alles rechts vom Attributblock wird ueber die Ueberschrift aufgeloest.
        self.rest = {c.strip(): i for i, c in enumerate(kopf)
                     if c.strip() and not (self.attr_start <= i < self.attr_start + self.n_attr)}

    def _suche_attributblock(self):
        """Die Spalte, ab der sich die Signatur aus den folgenden n Spalten ergibt."""
        probe = self.zeilen[:20]
        for start in range(len(self.rows[self.hdr]) - self.n_attr + 1):
            if all(z[self.sig].strip() == "|".join(z[i].strip()
                   for i in range(start, start + self.n_attr))
                   for z in probe if len(z) >= start + self.n_attr):
                return start
        raise RuntimeError("Attributblock nicht gefunden -- passt die Signatur nicht mehr zu ihren Spalten?")

    def spalte(self, zeile, name):
        """Wert einer Spalte, ueber ihre Ueberschrift. Attribute stechen gleichnamige Altspalten.

        Ein unbekannter Name fliegt. Er ist fast immer eine umbenannte Spalte, und ein still
        geliefertes "" laesst dann jede Regel darauf als "nicht gesetzt" durchgehen -- am
        26.08.2026 hat mich das eine Spalte fuer leer halten lassen, die voll war.
        """
        if name in self.name:
            i = self.name.index(name) + self.attr_start
        elif name in self.rest:
            i = self.rest[name]
        else:
            raise KeyError(f"Spalte {name!r} gibt es im Blatt nicht. Vorhanden: "
                           + ", ".join(sorted(self.name) + sorted(self.rest)))
        return zeile[i].strip() if len(zeile) > i else ""

    def land(self, zeile):
        return zeile[2].strip().replace("_", " ")

    def signatur(self, zeile):
        return zeile[self.sig].strip()

    def attribute(self, zeile):
        return [zeile[i].strip() for i in range(self.attr_start, self.attr_start + self.n_attr)]


def lade(neu_holen=True):
    if neu_holen:
        (HIER / DATEI).write_text(hole(), encoding="utf-8")
    text = (HIER / DATEI).read_text(encoding="utf-8")
    return Blatt(list(csv.reader(io.StringIO(text))))


if __name__ == "__main__":
    # Direkt aufrufbar, damit man die systematik.csv holen kann, ohne einen Pruefer zu starten:
    #     python sheet.py
    blatt = lade()
    print("%s: %d Flaggen, %d Attribute (Kopfzeile %d, Attributblock ab Spalte %d)"
          % (DATEI, len(blatt.zeilen), blatt.n_attr, blatt.hdr, blatt.attr_start))
    if blatt.ohne_signatur:
        print("  ! ohne Signatur:", ", ".join(blatt.ohne_signatur))

# docs/skin

Die Doku zur Skin-Schicht — und die Werkzeuge, die den datenlastigen Teil davon erzeugen.

| Datei | Was drin steht |
|---|---|
| `Skin-Felder.md` | Die **Mechanik**: Vorgaben, Formate, Staffelung, Zustände, Fallen, tote Felder. Von Hand gepflegt. |
| `Skin-Matrix.xlsx` | Die **Zuordnung**: eine Zeile je CSS-Regel — Komponente, Bereich, Zustand, Selektor, Property, Feld, Vorgabe. **Erzeugt, nicht von Hand gepflegt.** |
| `matrix-erzeugen.py` | Liest `Skin.java` und schreibt die Tabelle neu. |
| `matrix-pruefen.py` | Prüft die Tabelle gegen den Quellcode. |
| `css/` | Das **Ergebnis**: je Skin das fertige Stylesheet, so wie die Suite es der Scene anhängt. **Erzeugt, nicht von Hand gepflegt.** |

**Die Trennregel:** Was im Stylesheet landet, steht in der Tabelle. Was nicht ins CSS geht, steht im
Markdown. Jedes Feld erscheint mit seiner Vorgabe in genau einem der beiden.

## Benutzen

Die Tabelle beantwortet zwei Fragen, je nachdem wonach man filtert:

- **Filter auf *Komponente*** → „Ich will die Region-Shapes umfärben, welche Felder sind das?"
- **Filter auf *Feld*** → „Was ziehe ich mir damit sonst noch mit ein?"

Die Spalte **Vorgabe** sagt, was passiert, wenn man den Schlüssel weglässt: *Pflicht* (dann knallt
es), ein fester Wert, *← Formel* (wird aus anderen Feldern abgeleitet) oder *entfällt* (die
CSS-Regel entsteht gar nicht erst). Zeilen mit **fest im Code** haben kein Feld — die lassen sich
über die properties-Datei nicht ändern.

## Neu erzeugen

Nach jeder Änderung an `Skin.java` — neue Regel, neues Feld, umbenannter Selektor:

```
python docs/skin/matrix-erzeugen.py
python docs/skin/matrix-pruefen.py
```

Einmalig nötig: `python -m pip install openpyxl`

**Eigene Spalten bleiben erhalten.** Alles ab Spalte I gehört dem Menschen und wird beim Neuerzeugen
unangetastet übernommen — etwa eine Spalte „so soll es werden" beim Umbau eines Skins. Die Spalten A
bis H werden ersetzt.

**Eine Handvoll Zeilen ist erfunden.** Sie tragen in der Spalte *Zustand* den Wert `(kein Zustand)`
und stehen für Fälle, in denen das **Fehlen** einer Regel die Aussage ist — etwa Karten-Formen ohne
Pseudoklasse, die dadurch unsichtbar und unklickbar sind. So etwas findet kein Parser, gesucht wird
es trotzdem. Sie stehen als Liste `SONDERZEILEN` oben in `matrix-erzeugen.py`, und die Prüfung
zählt sie getrennt.

**Achtung:** Die Zeilen werden neu sortiert. Eigene Spalten bleiben deshalb an ihrer *Zeilennummer*
kleben, nicht an ihrer Regel. Wer Notizen über eine Umsortierung retten will, kopiert sie vorher
in ein zweites Blatt.

## Die Stylesheets in `css/`

Der Ordner hält je Skin das fertige CSS — dasselbe, was `Skin.buildCss()` liefert und die Suite der
Scene anhängt. Geschrieben werden sie von `scripts.ui.SkinCssDump` (Run As → Java Application, das
Arbeitsverzeichnis muss die Projektwurzel sein).

**Wozu:** Ob ein Umbau der Skin-Schicht die Darstellung angetastet hat, ist von Hand nicht zu
beantworten — sieben Skins mal ein Dutzend Screens mal Hover- und Disabled-Zustände sieht kein Auge
zuverlässig durch, und der Fehler, der durchrutscht, ist per Definition der unauffällige. Alles
mündet aber in eine Zeichenkette je Skin, und die lässt sich zeichengenau vergleichen.

**Der Ablauf** nach einer Änderung an Skins, properties-Dateien oder der CSS-Erzeugung:

```
git diff docs/skin/css
```

vorher das Werkzeug laufen lassen. Dort steht dann, was sich geändert hat: nichts bei einem reinen
Umbau, sonst genau die Regeln, die gemeint waren. Die erzeugten Dateien gehören in denselben
Commit wie die Änderung, die sie verursacht hat — **dadurch** ist das Vorher immer vorhanden, ohne
dass irgendwo ein Maßstab gepflegt werden müsste. Es ist der letzte Commit.

Verglichen wird bewusst von Git und nicht vom Werkzeug: Eine eigene Vergleichslogik wäre Code, der
selbst falsch sein kann. Und bewusst von Hand statt im Build — das CSS ist eine Momentaufnahme, keine
Invariante. Ein Lauf, der bei jeder gewollten Änderung anschlägt, erzieht dazu, ihn wegzuklicken.

## Was die Prüfung prüft

1. **Vollzähligkeit** — jeder Selektor, den `Skin.java` erzeugt, kommt in der Tabelle vor. Der
   `CssBuilder` verbietet doppelte Selektoren, die Menge ist also eindeutig.
2. **Feldnamen** — jeder Wert der Spalte *Feld* existiert wirklich in `SkinProperties`. Mit Build
   gegen den Bytecode, ohne Build gegen die Quelle.
3. **Der Rest** — welche Felder nicht in der Tabelle stehen, gruppiert nach Rechtecken,
   Bild-/Symbolnamen und Sonstigem. Die *Sonstigen* sind die Liste, die in `Skin-Felder.md`
   gepflegt sein muss; taucht dort etwas Neues auf, fehlt es im Markdown.

## Grenzen

- Die Spalte **Vorgabe** ist von Hand gepflegt (oben in `matrix-erzeugen.py`), weil die Ableitungen
  im Vorgaben-Durchlauf von `buildCss` stehen und sich nicht zuverlässig auslesen lassen. Ein neues
  Feld ohne Eintrag meldet das Skript mit „Feld ohne Vorgabe-Eintrag".
- Die Zuordnung **Selektor → Komponente** ist ebenfalls von Hand (Liste `MAP`). Ein neuer Selektor,
  der durch alle Muster fällt, wird gemeldet und landet als `?` in der Tabelle.
- Zwischenvariablen in `Skin.java` werden aufgelöst, aber nur einfache (`Typ name = ausdruck;`).
  Kommt eine kompliziertere dazu, steht in der Spalte *Feld* ein `—`, obwohl ein Feld beteiligt ist.

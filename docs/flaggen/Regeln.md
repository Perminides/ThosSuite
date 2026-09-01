# Flaggen — Regeln

Blanke Liste der getroffenen Entscheidungen. Kein Fließtext, keine Begründungen außer dort, wo der
Satz sonst nicht wiederzufinden ist. Einzelne Fragen, Spalten und Wertelisten stehen **nicht** hier —
die stehen im Generator und im Blatt und ändern sich zu oft.

Offene Punkte: `ToDo.md`. Ausführliche Herleitung: `Flaggen-Deck.md`.

## Leinwand und Raster

- Leinwand immer **180 × 120**, Verhältnis 3:2.
- Y in der Datei nach oben positiv, beim Einlesen invertiert.
- **3×3-Raster**, ein Feld 60 × 40, Felder zeilenweise nummeriert 0…8.
- Hintergrunddateien füllen die Leinwand exakt. `SketchPane` nimmt den Maßstab aus der Bounding Box
  des zuerst geladenen Sketches — eine abweichende Leinwand verzieht jedes angehängte Element.
- Die Skizze wird um `2 × Strichbreite` zu groß gerechnet, damit ihre Außenkante im Clip des
  Bilderrahmens verschwindet.
- Streifengrenzen dürfen Dezimalstellen haben.

## Elementdateien

- Um den **Nullpunkt zentriert**, höchstens **40 × 40**. Die Höhe bindet; die restliche Feldbreite ist
  Reserve für Geschwister und Behälter.
- Faktor 1,0 füllt die Feldhöhe. Faktoren über 1 sind erlaubt.
- Eine Datei kann mehrere Flächen tragen. Getrennte Teile **einer** Fläche dürfen sich nicht berühren —
  beim Füllen fällt der Strich weg, Berührendes verschmilzt.
- Kreis: `Point` + `properties.radius`. Sichel: zusätzlich `properties.cutout`.
- Höhe einer Sichel = 2 × Radius. Der oberste Punkt ist der Scheitel des äußeren Kreises, nicht die
  Hornspitze.
- Sterne haben drei Bilder: eins, zwei, mehr als zwei.
- Ein Kreis mit **zwei Farben** ist geteilt: eigene Datei `geteilter-kreis`, zwei Halbscheiben (oben,
  unten). Der volle Kreis bleibt `kreis`. `sketchOf` wählt nach der Farbanzahl — wie der Stern nach der
  Anzahl. Eine Flagge mit zwei echten Kreisen wäre damit abgedeckt, solange beide dieselbe Farbe haben.
- Silhouetten kommen aus neutralen Piktogrammen, **nie aus der echten Flagge** — ein `Vogel` steht auf
  neun Flaggen und darf kein bestimmter Vogel sein.
- Ein zu komplexes **Emblem** wird zu einer generischen, mehrflächigen Platzhalter-Silhouette (das
  Dülmener Wappen). Eine Datei für alle Embleme; ungefärbt, die Flächen geben Struktur über ihre
  Kontur. Löst `Flaggen-Deck.md` §5 ab („komplexes Emblem wird nicht gezeichnet").

## Platzieren

- Größe und Mittelpunkte der Geschwister im selben Feld, je Anzahl:

  ```
  1   Faktor 0,8    Mitte 0
  2   Faktor 0,8    ∓10
  3   Faktor 0,56   ∓20 · 0
  4   Faktor 0,44   ∓22,5 · ∓7,5
  ```

- Die Größe trägt zweierlei in einer Zahl: den geteilten Platz (1,0 · 1,0 · 0,7 · 0,55) und die Luft
  zum Feldrand (× 0,8). Sie ist damit **je Anzahl** einstellbar. Die Verkleinerung wirkt wie ein Rand
  ringsum, weil die Dateien um den Nullpunkt zentriert sind; der Versatz bleibt unberührt.
- Größen stehen beim Platzieren, nicht in den Dateien — sonst müsste man alle anfassen, wenn man es
  sich anders überlegt.

- Keine Messung der einzelnen Datei. Wir verlassen uns auf die 40 × 40.
- **Behälter** und der Faktor für ihren Inhalt, je nach Anzahl der Kinder:

  ```
            1 Kind   2      3      4
  Raute     0,7      0,7    0,7    0,7
  Kreis     0,7      0,5    0,45   0,45
  ```

- Beim Kreis ist die Grenze gerechnet: Der Kasten eines Elements ist 40 × 40, seine äußere Ecke muss
  im Radius bleiben. Bei einem Kind ist das die halbe Diagonale — `40k/√2 ≤ 20`, also `k ≤ 0,707`;
  mit Versatz sinkt es auf 0,52 · 0,48 · 0,49. Die Raute ist am Bild gefunden, ihre Ecken laufen
  spitz zu.
- Das Kind erbt die ganze Kette.
- Ein Element hängt am **letzten Behälter davor im selben Feld**; gibt es keinen, am Feld.
- Gezeichnet wird in **Spaltenreihenfolge** E1 → E4, von hinten nach vorn.

## Flächen und Farben

- Flächen werden in **Leserichtung** nummeriert. Was über anderen liegt, kommt zuletzt.
- Die Farbliste ist **positionsbezogen**: eine Farbe je Fläche, in Flächenreihenfolge, `|`-getrennt.
- Acht Farben: Rot, Blau, Hellblau, Grün, Gelb, Orange, Weiß, Schwarz. Kein `bunt`.
- Die Farbfrage wird gestellt, wenn im Blatt eine Farbe steht. Der Leak ist bekannt und gekauft.
- Eine Fläche **ohne** Farbe im Blatt bleibt grau und wird nicht gefragt — so das ungefärbte Emblem.
  Farbe und Fläche laufen über die **Flächennummer** zusammen, nicht über die Position in der
  Farbliste; eine ungefärbte Fläche verschiebt die folgenden nicht.
- **Flächen sind nie durchsichtig.** Zwei Grautöne je Skin — hell markiert, dunkel noch nicht dran —
  damit ein Element die Linien darunter abdeckt. Keiner der beiden darf einer Antwortfarbe
  nahekommen; die gefährlichen Nachbarn sind Weiß und Schwarz.

## Gösch und Dreieck

- Der Gösch belegt **immer Feld 0** und passt sich nie der Streifenzahl an.
- Gösch und Dreieck werden in die **Hintergrunddatei gezeichnet**, nicht zur Laufzeit angehängt.
- Der Dateiname trägt den Gösch: `waagerecht-7-goesch`.
- Streifen **enden am Gösch**, sie laufen nicht unter ihm durch — eine ungefüllte Fläche ist
  durchsichtig, überdecken reicht deshalb nicht.
- Dreieckstiefe einheitlich 40 % der Breite.

## Sketch-Namen

Aus den Attributen abgeleitet, keine eigene Spalte:

```
waagerecht-<n>                     senkrecht-<n>
kreuz-<ausrichtung>-<arme>         diagonal-<richtung>-<bänder>
uni                                sw-<n>            spezial-<n>
```

Wörter statt Ziffern in den Zweignamen. `-goesch` wird angehängt, wenn die Flagge einen hat.

Wo nach der **Verteilung** gefragt wird, steht sie im Namen. Bei fünf waagerechten Streifen ist es die
**ganze Abfolge** der Breiten: `waagerecht-5-3-1-2-1-3` — die Zahlen sind das Verhältnis selbst,
`build-streifen-sketch.py` liest sie direkt, keine Tabelle. Drei waagerechte (`waagerecht-3-<3W>`) und
senkrechte (`senkrecht-<n>-<S-Anordnung>`) tragen weiter den **Index**; er schlägt in einer Fallback-
Tabelle nach (aktuell leer, also alle gleich breit). Bei allen anderen Streifenzahlen kein Zusatz.

## Fragen

- Aufbau einer Karte: Form → Rahmen → Gösch → Dreieck → Hintergrund → Zweigfragen → Skizze →
  Zusatzelemente → je Element Anzahl und Ort → zeichnen → **alle Farben** → echte Flagge.
- Gefärbt wird gesammelt am Ende, Hintergrundflächen und Elemente zusammen.
- Zwei Shuffle-Blöcke: erst alle **Attribut-Fragen** (Anzahl, geteilt) gemischt, dann alle **Ortsfragen**
  gemischt. So steht die Anzahl vor dem Ort, und in keinem Block leakt die Reihenfolge das Blatt.
- **Anzahl vor Ort** — dann stimmt in der Ortsfrage der Numerus.
- Bei **jedem Kreis** wird gefragt, ob er geteilt ist — konstant, damit die stille Annahme „ungeteilt"
  nicht leakt. Richtig ist „Ja" genau bei zwei Farben im Blatt (Grönland).
- Die **Position** ist die Richtung vom Mittelpunkt, nicht das überdeckte Feld. Werte 0…8 wie das
  Raster, 9 ist verstreut.
- Mehrere Instanzen, die **symmetrisch um die Mitte** liegen, gelten als zentriert.
- Tolerierte Zweitantworten stehen als Klammer in der Zelle: `4(9)`.
- Distraktoren werden gezogen, damit auch eine falsche Vorstellung anklickbar bleibt.

## Daten

- Karten-Id = Spalte 0 des Blattes. Sie überlebt jede Regenerierung.
- Spalte `ID` ist der Kartenmarker: Shape-Name oder `small|x|y`. Immer gefüllt.
- Erzeugt werden die Zeilen mit `Generieren = 1`.
- **Keine Kommas in Zellen** — mehrere Werte mit `|` trennen. Sonst braucht der Leser einen
  quotefähigen CSV-Parser.
- Deck-CSV: UTF-8 mit BOM, CRLF, `;` als Trenner.
- Der Generator ersetzt eine Karte an ihrer Id **oder** an ihrem `Mark:`.

## Werkzeuge

- `sheet.py` / `FlagSheet` holen das Blatt als `systematik.csv`. Nichts wird über Spaltenpositionen
  gelesen außer Nummer, Land und englischem Namen.
- `build-streifen-sketch.py` erzeugt Streifen, `build-element-sketch.py` die gerechneten Elemente,
  `svg-zu-sketch.py` macht aus einem Piktogramm eine Strukturdatei.
- Der Konverter flacht Kurven ab (Toleranz 0,01) und **bricht ab** bei Strichbildern, Text, Masken,
  Clip-Pfaden, Verläufen und `transform`.
- Der Generator prüft jede Zeile doppelt: durch den echten `Card`-Parser und durch einen Flächenlauf.

## Bewusst nicht so

- Kein `Flag:`-Expander in der Suite. Ein Generator schreibt eine normale Deck-CSV.
- Keine Sonderlogik im Anwendungscode: Ableitung, Größen und Namen wohnen im Generator.
- Kein `bunt` als Farbwert.
- Keine Kategorien-Zwischenebene bei den Elementen.
- Keine Elemente aus der echten Flagge.
- Keine *willkürliche* feste Reihenfolge im MC; natürlich geordnete Antwortsätze dürfen per `=` fix
  stehen. Die Distraktor-*Auswahl* variiert weiter.
- Keine verzögerte Auswertung als Deck-Sonderfall — `MC+` ist ein deklarierter, deck-unabhängiger Step.

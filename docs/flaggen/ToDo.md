# Flaggen-Deck — ToDos

Beschlossen am 29.08.2026, noch nicht in `Flaggen-Deck.md` eingearbeitet.

## Zusatzelemente — Tabelle

- Die Elementzelle wird auf vier Spalten je Element aufgeteilt: Element, Ort, Farbe, Anzahl.
- Der Ort ist immer das Rastersegment. Kein eigener Wert „Kreis": Was im selben Feld wie ein Kreis
  liegt, wird in den Kreis gemalt.
- Ort als Zahl 0…8, `Verteilt` bekommt die 9.
- Zeichenreihenfolge im Renderer: Rechteck → Kreis → Anderes.
- Der Gösch ist Feld 0, kein eigener Ortswert.
- Nepal ist kein Sonderfall: Das Raster liegt auf der Leinwand, nicht auf der Flagge.
- `Formation Sterne` und `Formation Mond mit Stern(en)` bleiben vorerst draußen.

## Farben

- `bunt` fällt weg.
- Die Farbfrage wird gestellt, wenn in der Tabelle eine Farbe steht — Entscheidung pro Flagge, beim
  Ausfüllen. Der Leak ist bekannt und gekauft.
- Keine neuen Elementkategorien, um die Frage abzuleiten.
- Prüfregel dafür: Anzahl der Farben = Anzahl der Flächen im Elementsketch — **oder null Farben**; eine
  ungefärbte Fläche bleibt grau (das Emblem).

## Generator

- Ein Skript neben `sheet.py`, liest `systematik.csv`, schreibt Deck-CSV und Sketch-Dateien. Kein
  Sheet-Zugriff in der Suite, keine DB-Tabelle, kein Migrator.
- Der Hauptgrund für den Generator ist, dass die Komplexität vor dem CSV liegt und nicht in den
  Steps — §4 begründet ihn heute falsch mit `git diff`.

- Die bestehenden Decks bleiben bei `a|b*c|d`. Umgeschrieben wird frühestens, wenn die Flaggen live
  und gut sind — an den Flaggen ist zu viel umgeworfen worden, um jetzt die anderen Decks anzufassen.
  Der Parser versteht solange beide Schreibweisen.

## Reihenfolge bis zum Grundgerüst

Beschlossen am 31.08.2026. „Grundgerüst steht" heißt: Eine neue Flagge braucht keine neue
Entscheidung mehr — nicht, dass 214 Zeilen gefüllt sind. Farben und Silhouetten entstehen pro
Flagge an dem Tag, an dem sie drankommt.

1. **Dreieck** — Fragen 4a und 4b im Generator, dazu die Sketches. Als Einziges ändert es den
   Fragensatz, deshalb zuerst.
2. **Emblem** — **erledigt (01.09.2026).** Eine generische, mehrflächige, ungefärbte Platzhalter-
   Silhouette (Dülmener Wappen) für alle komplexen Embleme; Vorhandensein und Ort werden gefragt, die
   Farbe nicht. Die 4 farbigen Fälle (St. Lucia u. a.) später als handgeschriebene Zusatzfrage. §5
   abgelöst, Regel steht in `Regeln.md`.
3. **Grönland** — **erledigt (01.09.2026).** Der Kreis bleibt *ein* Element „Kreis"; bei jedem Kreis
   wird konstant „Ist der Kreis geteilt?" gefragt (stoppt die stille Annahme „ungeteilt", leakt nichts).
   Geteilt = zwei Farben im Blatt; dann zieht `sketchOf` die Datei `geteilter-kreis` (zwei Halbscheiben),
   die die bestehende Farbfrage Fläche für Fläche füllt. Keine neue Elementkategorie, kein neuer Fragetyp.
4. **Eine eigene Session nur für MC** — **erledigt (31.08.2026).** `MC+` als deklarierter Step,
   Rückbau von `isCollectMode`, die Präfix-Syntax und die tolerierte Antwort.
5. **Echte SVG-Flaggen** — statt der `…-flag-square-small.png` die echten Flaggen als SVG anzeigen.
   Reine Anzeige, hängt an keinem der anderen Punkte; die Reihenfolge ist hier frei.
6. **Deck auf zwei CSVs** — muss stehen, bevor die erste handgeschriebene Zusatzfrage entsteht,
   sonst überschreibt sie der nächste Generatorlauf.
7. Danach Deck zurücksetzen, ab Id 1 generieren, Merge nach master, und dann eine Flagge pro Tag.

Kein Showstopper, wird nebenbei erledigt: Nepal (nicht rechtwinkliger Sketch), Schweiz und Vatikan
(quadratisch), Grenada (Symbole werden schlicht ins Segment gezeichnet).

## Breitenverhältnisse der übrigen Verteilungswerte

`5W` ist **erledigt (01.09.2026)**: Der Wert ist die ganze Abfolge (`3-1-2-1-3`), steht 1:1 als Antwort
und im Sketch-Namen, `build-streifen-sketch.py` liest die Breiten direkt — keine Tabelle mehr.

Offen bleiben `3W` und `S-Anordnung`: die tragen weiter einen Index in die (aktuell leere) Fallback-
Tabelle, also alle gleich breit. Echte Verhältnisse kommen dazu, wenn die erste Flagge sie braucht —
schematisch deutlich, nicht maßstabsgetreu. Wer will, stellt sie später auf denselben Abfolge-Stil um.

## Offen geblieben, ausdrücklich vertagt

- Wie zwei Elemente im selben Segment zueinander positioniert werden.

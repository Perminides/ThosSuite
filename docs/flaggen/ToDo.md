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

## MC — Syntax, Typen, Mechanik

Beschlossen am 31.08.2026, ersetzt die früheren Abschnitte „MC-Syntax" und „Step-Typen". Gilt
suite-weit, nicht nur für die Flaggen — MC ist deck-unabhängige Engine-Logik.

### Modell

- `sealed interface ChoiceStep extends Step { Set<AnswerOption> options(); List<String> orderHint(); }`,
  implementiert von `MC` und `MCPlus`. `Step permits … ChoiceStep …`.
- `AnswerOption(String text, Role role)`, `enum Role { CORRECT, WRONG_ALWAYS_SHOWN, TOLERATED, DISTRACTOR_OPTIONAL }` — die vier
  Präfixe `+ - ~ ?`.

### Rollen

- Zwei unabhängige Bits: **Platzierung** (`+`/`-` immer gezeigt · `~`/`?` Pool, gezogen) und **Klick in
  diesem Step** (`+` richtig · `-`/`?` falsch/Abbruch · `~` falsch/kein Abbruch).
- Die Rolle eines Texts darf über Steps variieren — mal `+`, mal `-`, mal `?`.
- Feste Distraktoren (`-`) sparsam: `Keine` immer, `Stern` wahrscheinlich. Mehr als zwei wird bei
  Brasilien eng (vier Richtige + `Keine` = fünf Pflichtplätze von acht).

### Syntax

- Steps `MC:` und `MC+:`. Zwei Schreibweisen, pro Step genau eine:
  - **Alt** `a|b*c|d` bleibt (richtig vor `*`, falsch danach) — kann nur `+`/`?`. Der 99-%-Fall.
  - **Präfix**, sobald eine Option mit `+` beginnt; darin `+ - ~ ?`, nackt → `?`.
- Erkennung durch Schnüffeln am **`+`**: beginnt eine Option mit `+` → Präfixform, sonst Alt. Nur das
  `+` löst aus — ein führendes `-`/`~`/`?` ist in Altform bloß Text (negative Zahlen: `-40°`).
- Die Rolle frisst nur das **erste Zeichen**, der Rest ist Text: `+-40°` = richtig „-40°", `--40°` =
  fester Distraktor „-40°".
- **Escape** `\`: ein führendes `\` macht das nächste Zeichen literal und wird weggeworfen. Nötig
  praktisch nur für ein führendes `+` in Altform (`\+5*…`); in Präfixform reicht die Rolle (`?-40°`).
- **Order-Bit**: führendes `=` am Body → `orderHint` = geschriebene Reihenfolge; sonst mischt Progress.
  Orthogonal zu Typ und Schreibweise (`MC:=…`, `MC+:=…`).
- In der Altform trennt der **erste** Stern richtig von falsch; jeder weitere ist Text (Gender-Stern
  `Einwohner*innen`). In der Präfixform ist `*` immer Text.
- FailFast: keine richtige Antwort · doppelte Texte · leerer Body.

### Verhalten

- `MC`: Einzelklick, sofort gewertet, Fehlklick (`-`/`?`) bricht ab, auch bei mehreren Richtigen
  (nacheinander anklicken). `~`-Klick: `incorrect`-Optik, **kein** Abbruch, wertungsfrei, weiter warten.
- `MC+`: sammeln + absenden, alles oder nichts. Sammelt **immer**, auch bei einer Richtigen — sonst
  leakt das Klickverhalten (sofort werten vs. markieren) die Zahl der Richtigen. `~` ignoriert (nicht
  Pflicht, nicht Strafe).
- `isCollectMode` (adbe074) fällt weg — der Modus kommt vom **Typ**, nicht aus der Zahl der Richtigen.
- Submit-Knopf **immer** sichtbar, auch bei `MC` (dort inert) — seine Anwesenheit darf den Modus nicht
  verraten.
- Typ ist Autorenwahl: Mehrfach-richtig als `MC` (anklicken) oder `MC+` (sammeln). Elementfarbfrage
  als `MC+`.
- Die `~`-Markierung nutzt den bestehenden `incorrect`-Zustand — kein sechster PseudoState, kein
  Skin-Anfassen. Fallbacks (`marked`, Button deaktivieren) liegen bereit, falls zu heftig.

### Cluster-Mechanik (im Progress, pro Durchlauf)

Zweck: Antworten über aufeinanderfolgende Steps stabil halten, ohne die richtige Antwort durch ihr
Auftauchen zu leaken.

- `start()`-Scan (deterministisch): `ChoiceStep`s nach Textmenge (Universum) gruppieren. Je Cluster
  `RequiredAnswers { fromAllSteps, order }` — `fromAllSteps` = alle Texte, die in irgendeinem Step des
  Clusters `+` oder `-` sind (Pflicht-Menge); `order` = der `orderHint`, falls einer da ist.
- Voraussetzung: gleichartige Steps mit **demselben Optionsuniversum** schreiben — nur die Präfixe
  wandern. Vergisst man's, springen die Antworten sichtbar; das ist das Netz, kein Generatorzwang.
- Darstellung faul je Cluster, gecacht nach Universum: Pflicht-Menge pinnen, aus dem Pool (`~`/`?`) auf
  ≤ 8 auffüllen (Zufall pro Durchlauf), ordnen (`order` oder einmal mischen), einfrieren. Jeder Step
  zieht die Rolle frisch aus seinen eigenen Optionen; Präsenz und Verhalten sind entkoppelt.
- `lastMcOrder` und der „gleiche Texte"-Zweig fallen weg; der Cache schlägt nach Universum, überlebt
  Anzahl-Zwischenschritte und andere Unterbrechungen.
- Order-Merge im Scan: leer + gefüllt → gefüllt; zwei gleiche → ok; zwei verschiedene → FailFast.
- FailFast: Pflicht-Menge > 8. Zu wenige Optionen sind kein Fehler (Ur/Uruk zeigt zwei).
- Clustern ist Herleitung fürs Anzeigen → wohnt im **Progress**, nicht im Parser (der liest nur) und
  nicht im Generator. Als reine Funktion, damit ein Validator sie aufrufen könnte. Wandert auf `Card`,
  falls es je außerhalb eines Durchlaufs gebraucht wird.

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

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
- Prüfregel dafür: Anzahl der Farben = Anzahl der Flächen im Elementsketch.

## MC — Syntax, Typen, Mechanik

Beschlossen am 31.08.2026, ersetzt die früheren Abschnitte „MC-Syntax" und „Step-Typen". Gilt
suite-weit, nicht nur für die Flaggen — MC ist deck-unabhängige Engine-Logik.

### Modell

- `sealed interface ChoiceStep extends Step { Set<AnswerOption> options(); List<String> orderHint(); }`,
  implementiert von `MC` und `MCPlus`. `Step permits … ChoiceStep …`.
- `AnswerOption(String text, Role role)`, `enum Role { RICHTIG, FALSCH, TOLERANT, FUELLER }` — die vier
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
2. **Emblem** — ein gezeichneter Platzhalter (Fragezeichen oder Wappen) statt eines Sonderfalls.
   Kippt §5s „komplexes Emblem wird nicht gezeichnet"; gehört dann in `Regeln.md`. Nach der Farbe
   wird ohnehin nur gefragt, wenn im Blatt eine steht.
3. **Grönland** — geteilter Kreis: eine Fläche, zwei Farben, eigene Datei.
4. **Eine eigene Session nur für MC** — `MC+` als deklarierter Step, Rückbau von `isCollectMode`,
   die Präfix-Syntax und die tolerierte Antwort. Gehört zusammen: Der Rückbau allein zerlegt die
   Elementfrage.
5. **Deck auf zwei CSVs** — muss stehen, bevor die erste handgeschriebene Zusatzfrage entsteht,
   sonst überschreibt sie der nächste Generatorlauf.
6. Danach Deck zurücksetzen, ab Id 1 generieren, Merge nach master, und dann eine Flagge pro Tag.

Kein Showstopper, wird nebenbei erledigt: Nepal (nicht rechtwinkliger Sketch), Schweiz und Vatikan
(quadratisch), Grenada (Symbole werden schlicht ins Segment gezeichnet).

## Breitenverhältnisse der übrigen Verteilungswerte

Der Sketch-Name trägt die Verteilung inzwischen, und `build-streifen-sketch.py` kennt eine Tabelle
dafür. Gefüllt sind erst `3W = 0` (alle gleich) und `5W = 3` (2 : 1 : 4 : 1 : 2). Die übrigen Werte
von `3W`, `5W` und `S-Anordnung` kommen dazu, wenn die erste Flagge sie braucht — schematisch
deutlich, nicht maßstabsgetreu.

## Offen geblieben, ausdrücklich vertagt

- Wie zwei Elemente im selben Segment zueinander positioniert werden.

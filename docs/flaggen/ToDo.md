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

## MC-Syntax — gilt für MC und MC+

- Vier Präfixe, jede Option trägt eines, nackte Optionen lehnt der Parser ab.
  `+` richtig · `-` falsch, immer sichtbar · `~` falsch, aber kein Abbruch · `?` falsch, zum Auffüllen.
- `~` bekommt keine Immer-Anzeige.
- Feste Distraktoren sparsam: `Keine` immer, `Stern` wahrscheinlich. Mehr als zwei wird bei Brasilien
  eng (vier richtige + `Keine` = fünf von acht Plätzen).

## Step-Typen

- `MC` behält sein Verhalten: Einzelklicks, sofort gewertet, auch bei mehreren richtigen Antworten.
- `MC+` wird ein eigener Step: markieren, absenden, alles oder nichts. Abgeschickt per Knopf.
- Die Ableitung aus der Zahl der richtigen Antworten (`isCollectMode`, adbe074) wird zurückgebaut.
- Der Typ hängt an der Fragedefinition, nicht an der Flagge — die Elementfarbfrage ist damit immer
  `MC+`.

## Generator

- Ein Skript neben `sheet.py`, liest `systematik.csv`, schreibt Deck-CSV und Sketch-Dateien. Kein
  Sheet-Zugriff in der Suite, keine DB-Tabelle, kein Migrator.
- Der Hauptgrund für den Generator ist, dass die Komplexität vor dem CSV liegt und nicht in den
  Steps — §4 begründet ihn heute falsch mit `git diff`.

- Die bestehenden Decks bleiben bei `a|b*c|d`. Umgeschrieben wird frühestens, wenn die Flaggen live
  und gut sind — an den Flaggen ist zu viel umgeworfen worden, um jetzt die anderen Decks anzufassen.
  Der Parser versteht solange beide Schreibweisen.

## Breitenverhältnisse der übrigen Verteilungswerte

Der Sketch-Name trägt die Verteilung inzwischen, und `build-streifen-sketch.py` kennt eine Tabelle
dafür. Gefüllt sind erst `3W = 0` (alle gleich) und `5W = 3` (2 : 1 : 4 : 1 : 2). Die übrigen Werte
von `3W`, `5W` und `S-Anordnung` kommen dazu, wenn die erste Flagge sie braucht — schematisch
deutlich, nicht maßstabsgetreu.

## Offen geblieben, ausdrücklich vertagt

- Wie zwei Elemente im selben Segment zueinander positioniert werden.

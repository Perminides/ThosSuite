# Flaggen — Regeln

Blanke Liste der getroffenen Entscheidungen. Kein Fließtext, keine Begründungen außer dort, wo der
Satz sonst nicht wiederzufinden ist. Einzelne Fragen, Spalten und Wertelisten stehen **nicht** hier —
die stehen im Generator und im Blatt und ändern sich zu oft.

Offene Punkte: `ToDo.md`. Ausführliche Herleitung: `Flaggen-Deck.md`.

## Leinwand und Raster

- Leinwand immer **180 × 120**, Verhältnis 3:2.
- Y in der Datei nach oben positiv, beim Einlesen invertiert.
- **3×3-Raster**, ein Feld 60 × 40, Felder zeilenweise nummeriert 0…8.
- Hintergrunddateien füllen die Leinwand exakt. Wo die Flächen das nicht tun (Nepal, `nicht-rechteckig`),
  steht die Leinwand als GeoJSON-`bbox` in der Datei: `[0, -120, 180, 0]`. `SketchPane` nimmt den Maßstab
  dann aus ihr, sonst aus der Bounding Box
  des zuerst geladenen Sketches — bei einer abweichenden Leinwand stimmen Größe und Versatz jedes
  angehängten Elements nicht mehr, denn die rechnen in festen Einheiten der Leinwand.
- Ein Feld im Skin, das nicht 3:2 ist, schadet nicht: Eingepasst wird mit **einem** Maßstab, die
  Skizze bleibt unverzerrt und bekommt leere Ränder.
- Die Skizze wird um `2 × Strichbreite` zu groß gerechnet, damit ihre Außenkante im Clip des
  Bilderrahmens verschwindet.
- Streifengrenzen dürfen Dezimalstellen haben.

## Elementdateien

- Um den **Nullpunkt zentriert**. Die Datei trägt ihre **natürliche Größe**: Die meisten füllen ein
  Rasterfeld (60 × 40), `kreis` hat Radius 25, `raute` ist 120 × 80. `svg-zu-sketch.py` normiert auf
  das Feld (`|x| ≤ 30`, `|y| ≤ 20`), von Hand gebaute Dateien dürfen darüber hinaus.
- Keine Größe und kein Faktor in der Datei. Wie groß eine Figur gezeichnet wird, entscheidet allein
  die Faktorenkette beim Platzieren.
- Die Faktoren dort rechnen mit einem Kasten von **40 × 40**. Ein **Kind**, das breiter ist, kann
  über seinen Behälter hinausragen; bisher betrifft das nur den Vogel mit 50,8, und der bleibt aus
  Behältern und Geschwistergruppen heraus. `kreis` und `raute` sind ebenfalls breiter, treten dort
  aber als **Behälter** auf — ihre Faktoren sind am Bild gefunden, nicht gerechnet.
- Eine Datei kann mehrere Flächen tragen. Getrennte Teile **einer** Fläche dürfen sich nicht berühren —
  beim Füllen fällt der Strich weg, Berührendes verschmilzt.
- Kreis: `Point` + `properties.radius`. Sichel: zusätzlich `properties.cutout`.
- Höhe einer Sichel = 2 × Radius. Der oberste Punkt ist der Scheitel des äußeren Kreises, nicht die
  Hornspitze.
- Sterne haben sechs Bilder: eins, zwei, drei, vier, fünf, Haufen. Der Haufen steht für alles ab
  sechs und zeigt keine zählbare Zahl.
- Ein Kreis mit **zwei Farben** ist geteilt: eigene Datei `geteilter-kreis`, zwei Halbscheiben (oben,
  unten). Der volle Kreis bleibt `kreis`. `sketchOf` wählt nach der Farbanzahl — wie der Stern nach der
  Anzahl. Eine Flagge mit zwei echten Kreisen wäre damit abgedeckt, solange beide dieselbe Farbe haben.
- Silhouetten kommen aus neutralen Piktogrammen, **nie aus der echten Flagge** — ein `Vogel` steht auf
  neun Flaggen und darf kein bestimmter Vogel sein.
- Ein zu komplexes **Emblem** wird zu einer generischen, mehrflächigen Platzhalter-Silhouette (das
  Dülmener Wappen). Eine Datei für alle Embleme; ungefärbt, die Flächen geben Struktur über ihre
  Kontur. Löst `Flaggen-Deck.md` §5 ab („komplexes Emblem wird nicht gezeichnet").

## Platzieren

- **Alle** Größenfaktoren stehen an einer Stelle: `CONTAINERS` im Generator. Ein Behälter je Zeile,
  ein Wert je Kinderzahl. Das Rasterfeld steht als `Segment` mit drin — es ist der äußerste Behälter,
  den jedes Element durchläuft.

  ```
            1 Kind   2       3       4
  Segment   0,8      0,8     0,56    0,44
  Raute     1,12     1,12    0,784   0,616
  Kreis     0,875    0,625   0,394   0,309
  ```

- Die gezeichnete Größe ist das **Produkt dieser Faktoren von außen nach innen**, mal den Koordinaten
  der Datei. Brasiliens Schrift zu zweit im Kreis in der Raute: `0,8 · 1,12 · 0,625 = 0,56`.
- `OFFSETS` trägt nur die **Orte**, keine Größen, je nach Anzahl der Geschwister:

  ```
  1   Mitte 0
  2   ∓10
  3   ∓20 · 0
  4   ∓22,5 · ∓7,5
  ```

- Ein Versatz skaliert mit dem Kasten, in dem er steht — also mit dem Faktor des Behälters, nicht
  mit dem des Elements selbst.
- Die Zahlen des `Segment` tragen zweierlei: den geteilten Platz (1,0 · 1,0 · 0,7 · 0,55) und die Luft
  zum Feldrand (× 0,8). Beides in einer Zahl heißt: Die Luft ist **je Anzahl** einstellbar, ohne eine
  Datei anzufassen.
- Keine Messung der einzelnen Datei. Gerechnet wird mit einem Kasten von **40 × 40** — nicht mit den
  60 × 40, die eine Datei haben darf. Der schlechteste Fall würde sonst alles kleiner machen, auch die
  schmalen Figuren, die es nicht nötig haben.
- Beim Kreis ist die Grenze ausrechenbar: Die äußere Ecke des Kastens muss im Radius 25 bleiben. Bei
  einem Kind ist das die halbe Diagonale — `40k/√2 ≤ 25`, also `k ≤ 0,884`; eingetragen ist 0,875. Bei
  mehreren kommt der Versatz dazu, die Grenzen sinken. Die Raute ist am Bild gefunden, ihre Ecken
  laufen spitz zu.
- Ändert man einen Behälter, ändern sich **alle vier** seiner Zahlen mit — sonst verschiebt sich nur
  der Fall mit dieser Kinderzahl gegen die anderen.
- Das Kind erbt die ganze Kette.
- Ein Element hängt am **letzten Behälter davor im selben Feld**; gibt es keinen, am Feld.
- Gezeichnet wird in **Spaltenreihenfolge** E1 → E4, von hinten nach vorn.

## Flächen und Farben

- Flächen werden in **Leserichtung** nummeriert. Was über anderen liegt, kommt zuletzt.
- **Ausnahme Kreuze:** Sind die vier Felder um ein Kreuz einzelne Flächen, laufen sie im
  **Uhrzeigersinn** — beim senkrechten Kreuz ab links oben, beim diagonalen ab dem oberen Dreieck —,
  danach kommt das Kreuz selbst. Vier Viertel um eine Mitte liest man im Kreis und nicht
  zeilenweise. Burundis `Rot|Grün|Rot|Grün` ergibt nur so oben und unten dieselbe Farbe; in
  Leserichtung läge Rot rechts. Einzeln sind die Felder beim einfarbigen und beim unsichtbaren
  Kreuz, weil dort verschiedenfarbige Viertel vorkommen (Dominikanische Republik, Grenada).
- Beim **dreifarbigen** und beim **gesäumten** Kreuz ist das Feld dagegen **eine** Fläche in vier
  Stücken und steht zuerst, danach die Teile des Kreuzes von außen nach innen. Island steht so mit
  `Blau|Weiß|Rot` im Blatt.
- **Ausnahme ineinanderliegende Dreiecke:** Sie zählen nach ihrer Spitze von links nach rechts,
  also vom innersten nach außen. Guyana steht so mit `Rot|Schwarz|Gelb|Weiß` im Blatt. Die Spitzen
  liegen gleichmäßig verteilt, bei vier Farben auf einem, zwei, drei und vier Vierteln der
  Dreieckslänge. Die Datei heißt dann `dreieck-<form>-<farbanzahl>`.
- Die Farbliste ist **positionsbezogen**: eine Farbe je Fläche, in Flächenreihenfolge, `|`-getrennt.
- Ein führendes **`&`** kehrt das um: `&Weiß` heißt „diese eine Farbe gilt dem **ganzen Element**".
  Alle seine Flächen werden gemeinsam hervorgehoben, einmal gefragt und gemeinsam gefüllt. Für das
  mehrflächige Emblem, dessen Teile zusammen ein Wappen ergeben und keine eigenen Farbträger sind.
  Nur mit genau einer Farbe erlaubt; bei einflächigen Elementen wirkungslos und geduldet. Die
  Entscheidung trifft das Blatt, nicht der Generator.
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
- Gösch und Dreieck sind **aufgelegte Silhouetten** aus `elements/`, angehängt nach ihrer Frage
  (`goesch`, `dreieck-<n>`, jeweils auf die ganze Leinwand mit `cell = -1`). Sie stehen deshalb
  **nicht** im Namen der Hintergrunddatei — es gibt kein `waagerecht-7-goesch`.
- Beide bekommen ihre eigene Farbfläche und werden gefüllt. Überdecken reicht nur, *weil* gefüllt
  wird: Eine ungefüllte Fläche ist durchsichtig, die Streifen liefen darunter durch.
- Dreieckstiefe einheitlich 40 % der Breite.

## Sketch-Namen

Aus den Attributen abgeleitet, keine eigene Spalte:

```
waagerecht-<n>                     senkrecht-<n>
kreuz-<ausrichtung>-<arme>         diagonal-<richtung>-<bänder>
uni                                sw-<n>            spezial-<n>
```

Wörter statt Ziffern in den Zweignamen. Gösch und Dreieck stehen nicht im Namen, sie werden aufgelegt.

Wo nach der **Verteilung** gefragt wird, steht sie im Namen. Bei fünf waagerechten Streifen ist es die
**ganze Abfolge** der Breiten: `waagerecht-5-3-1-2-1-3`. Die Zahlen sind eine **Rangfolge**, kein
Maß: 3 ist breiter als 2, 2 breiter als 1 — mehr sagen sie nicht. Eine 3 wird nicht dreimal so breit
gezeichnet wie eine 1. Die tatsächlichen Breiten in der Skizze werden nach Aussehen gewählt, so
wird `waagerecht-5-2-1-3-1-2` mit 2 : 1 : 4 : 1 : 2 gezeichnet. Die Zuordnung steht in der Tabelle
`GEZEICHNET` in `build-streifen-sketch.py`, ohne Eintrag zeichnet das Skript die Rangfolge wörtlich.
Derselbe Name ergibt so immer dieselbe Datei. Drei waagerechte (`waagerecht-3-<3W>`) und
senkrechte (`senkrecht-<n>-<S-Anordnung>`) tragen weiter den **Index**; er schlägt in einer Fallback-
Tabelle nach (aktuell leer, also alle gleich breit). Bei allen anderen Streifenzahlen kein Zusatz.

## Fragen

- Aufbau einer Karte: Form → **Weiche** (Kreuz · Diagonale · Nur ein Dreieck von links · Nichts davon) →
  beim Dreieck Form und Farbenzahl → Hintergrund → Zweigfragen → Skizze, Dreieck darauf → Gösch →
  Rahmen → Zusatzelemente → je Element Anzahl und Ort → zeichnen → **alle Farben** → echte Flagge.
  Das Dreieck wird früh benannt und erst auf den fertigen Hintergrund gelegt, Gösch und Rahmen direkt
  nach ihrer Frage. Bei Sonderhintergründen entfallen Gösch und Rahmen.
- Nach „Nicht rechteckig" entfallen Weiche, Dreieck, Hintergrund, Gösch und Rahmen — sie alle setzen ein
  Rechteck voraus. Die Skizze ist `nicht-rechteckig` (Feld und Rand), weiter geht es mit den Elementen.
  Weil die Feldmitten dort teils neben der Flagge lägen, legt `ANKER` im Generator je Feld Größe und
  Versatz fest — abgeleitet aus der Skizze, nicht aus dem Land.
- Nach „Quadratisch" geht alles weiter wie beim Rechteck, gezeichnet wird aber `<hintergrund>-quadratisch`:
  die rechteckige Skizze, von `build-form-sketch.py` in x auf das Quadrat 30…150 gestaucht, mit `bbox`.
  Die äußeren Rasterspalten rücken dort auf die Drittel des Quadrats (x 50 und 130). Gösch, Dreieck und
  Rahmen gibt es auf dem Quadrat noch nicht, der Generator bricht dann ab.
- Die Weiche entscheidet nur den Zweig; Einzelheiten fragen die Folgefragen. Reicht das Dreieck bis
  zum rechten Rand (Form 3 oder 4), entfallen Hintergrund- und Streifenfrage: Es verdeckt jede Grenze,
  die Skizze ist `waagerecht-2`, oben und unten werden gefärbt. Das Blatt braucht dort zwei
  Hintergrundfarben.
- **Eine Frage entfällt nur, wenn eine früher beantwortete sie überflüssig macht.** Nie, weil eine
  spätere Antwort sie überflüssig machen wird — sonst verrät schon ihr Fehlen, was kommt. Gösch und
  Rahmen entfallen bei Sonderhintergründen, weil „Anderes" vorher beantwortet ist; die Formfrage
  des Dreiecks gibt es nur nach „Nur ein Dreieck von links".
- Gefärbt wird gesammelt am Ende, Hintergrundflächen und Elemente zusammen.
- Zwei Shuffle-Blöcke: erst alle **Attribut-Fragen** (Anzahl, geteilt) gemischt, dann alle **Ortsfragen**
  gemischt. So steht die Anzahl vor dem Ort, und in keinem Block leakt die Reihenfolge das Blatt.
- **Anzahl vor Ort** — dann stimmt in der Ortsfrage der Numerus.
- Bei **jedem Kreis** wird gefragt, ob er geteilt ist — konstant, damit die stille Annahme „ungeteilt"
  nicht leakt. Richtig ist „Ja" genau bei zwei Farben im Blatt (Grönland).
- Die **Position** ist die Richtung vom Mittelpunkt, nicht das überdeckte Feld. Werte 0…8 wie das
  Raster, 9 ist verstreut.
- Mehrere Instanzen, die **symmetrisch um die Mitte** liegen, gelten als zentriert.
- Tolerierte Zweitantworten stehen als Klammer in der Zelle: `4(9)`, mehrere mit `|` getrennt:
  `4(9|5)`. Kein Komma — das trennt im Blatt die Spalten. Der Generator macht daraus `~`-Optionen:
  falsch, aber ohne Abbruch.
- Die Klammer gilt in **jeder Spalte, zu der eine Frage gehört** — Streifenzahlen, `5W`, Gösch,
  Rahmen, Dreieck, Anzahlen, alle kodierten Spalten. Gezeichnet und weitergefragt wird immer mit dem
  Wert vor der Klammer: Wer die tolerierte Antwort klickt, geht den Weg der richtigen weiter.
  Verboten ist sie in Spalten ohne Frage (`Generieren`, `Version`, `ShapeId`, `Spezial`) und beim
  Hintergrundtyp für Kreuz und Diagonale; dort bricht der Generator ab.
- Dieselbe Klammer gilt für **Elementnamen**: `Vogel (Emblem)` heißt „richtig ist Vogel, wer Emblem
  klickt, wird nicht bestraft". Überall sonst — Artikel, Ortsfrage, Elementdatei — zählt nur der
  Name vor der Klammer.
- Und für **Farben**: `Hellblau (Blau)` in der Farbliste. Gemalt wird die Farbe vor der Klammer.
  Gemessen sind die Grenzfälle in `blautoene.csv`; sieben Flaggen liegen im Mittelband, für die
  taugt genau diese Schreibweise.
- Distraktoren werden gezogen, damit auch eine falsche Vorstellung anklickbar bleibt.

## Daten

- Karten-Id = `(Version − 1) × 1000 + Land-Id`, aus der Spalte `Land-ID`. Sie trägt den
  Lernfortschritt und überlebt jede Regenerierung; zwei Zeilen mit derselben Karten-Id brechen den
  Lauf ab. Die **Land-Id** steht bewusst im Blatt statt der fertigen Karten-Id: Die
  handgeschriebenen Zusatzfragen gruppieren nach Land, nicht nach Flagge.
- Bei 214 Ländern reicht der Flaggenblock für **zehn** Versionen (bis 9214). Die elfte ergäbe 10214
  und läge im Block der Kartenkarten.
- Der Kartenmarker hinter `Mark:` ist der **Name**. Nur wo die Weltkarte ihn nicht kennt, steht in
  Spalte `ShapeId` eine Ausnahme (`middle|3434|951`) — leer heißt: nimm den Namen.
- Erzeugt werden die Zeilen mit `Generieren = 1`.
- **Keine Kommas in Zellen** — mehrere Werte mit `|` trennen. Sonst braucht der Leser einen
  quotefähigen CSV-Parser.
- **Kein Semikolon in einem Schritt** — es trennt die Spalten der Deck-Datei. Der Generator bricht ab.
- **Kein Zeilenumbruch in einer Zelle.** Das Fragefeld versteht dafür `<br />`, `<b>` und `<i>`.
- Deck-CSV: UTF-8 mit BOM, CRLF, `;` als Trenner.
- Die Deck-Datei entsteht bei jedem Lauf **komplett neu**, nach Id sortiert. Was nicht aus dem Blatt
  kommt, überlebt den Lauf nicht — handgeschriebene Zusatzfragen gehören in die zweite Deck-Datei.
  Der Lauf meldet, wie viele Karten vorher drin standen.

## Mehrere Flaggen für ein Land

- Eine **Zeile je Flagge**, dazu die Spalte `Version`: leer oder 1 ist die normale, dann 2, 3 …
  Alle Zeilen eines Landes tragen dieselbe `Land-ID`; die Karten-Id rechnet der Generator daraus.
- Die SVG-Datei trägt die Version ab der zweiten: `Afghanistan.svg`, `Afghanistan2.svg`. Fehlt sie,
  meldet der Lauf es und macht weiter.
- Der `Mark:` bleibt für alle Versionen derselbe — es ist dieselbe Fläche auf der Karte.
- Spalte `Hinweistext` steht als `<i>…</i><br />` **vor der ersten Frage** („Flagge bis 2021"). Kein
  eigener Schritt, das spart einen Klick; sichtbar ist er dann nur während der ersten Frage.
- Die erste Flagge eines Landes braucht keinen Hinweistext, jede weitere schon.

## Nummernblöcke

Jeder erzeugte Fragetyp bekommt einen eigenen Block von zehntausend, die handgeschriebenen
Zusatzfragen liegen ab einer Million.

```
        1 …  9214   Flagge skizzieren         generiert
    10001 … 19214   Flagge auf Karte finden   generiert
    20001 … 29214   der nächste Typ           generiert
  k·10000 + Id      Block k, bis k = 99       generiert
1000000 + Land·1000 + n                       von Hand, 1000 je Land
```

Die Blöcke 0 bis 99 reichen bis 998214 und stoßen damit nicht an die Million — Platz für
99 erzeugte Fragetypen. Die handgeschriebenen gruppieren nach **Land**, nicht nach Flagge; welche
Flagge gemeint ist, sagt der Karteninhalt.

## Werkzeuge

- `sheet.py` / `FlagSheet` holen das Blatt als `systematik.csv`. **Nichts** wird über eine
  Spaltenposition gelesen — jede Spalte hängt an ihrer Überschrift, der Attributblock an der
  Signatur. Spalten dürfen also frei verschoben werden, umbenannte fliegen mit Klartext auf.
- `build-streifen-sketch.py` erzeugt Streifen, `build-element-sketch.py` die gerechneten Elemente,
  `svg-zu-sketch.py` macht aus einem Piktogramm eine Strukturdatei.
- Der Konverter flacht Kurven ab (Toleranz 0,01) und **bricht ab** bei Strichbildern (`fill=none`),
  Text, Masken, Clip-Pfaden, Verläufen, Filtern, eingebetteten Bildern und `use`. `transform`
  dagegen **löst er auf**, und ein bildfüllendes Hintergrundrechteck wirft er weg — viele
  Symbolsätze legen ihre Piktogramme darauf ab.
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

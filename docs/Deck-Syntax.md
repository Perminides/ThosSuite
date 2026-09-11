# ThosSuite — Deck-Syntax

Das Format der Anki-Deck-CSVs: eine Zeile je Karte, zerlegt in Steps. Nachschlagewerk — im besten
Fall reicht die Beispielsammlung. Maßgeblich ist `Card.parseStep`; ändert der sich, ändert sich das hier.

## Zeile

Aufbau: `<id>;<remark>;<label,label,…>;<step>;<step>;…`

| Zeile | Bedeutung |
|---|---|
| `42;;Geografie;Output:Hauptstadt?;MC:Paris*Lyon\|Marseille;Pause:` | Karte 42, Label „Geografie", drei Steps. |
| `7;knifflig;Sport,Historie;Input:1998\|MCMXCVIII` | remark „knifflig", zwei Labels, ein Step. |

- `id` — ganze Zahl, überlebt jede Regenerierung. `remark` und `labels` dürfen leer sein.
- Eine Karte **muss irgendwo Input verlangen** (`MC`, `MC+`, `Input`, `Click`, `Fast`) — sonst fliegt sie.

## Beispielsammlung

| Step | Bedeutung |
|---|---|
| `Output:Wo liegt Rom?` | Zeigt den Text an. |
| `Image:rom.png` | Zeigt das Bild. |
| `Pause:` | Wartet auf einen Tastendruck. |
| `Input:Rom` | Tippen; „Rom" (ohne Groß/klein, getrimmt) ist richtig. |
| `Input:1998\|MCMXCVIII` | Tippen; beide Schreibweisen gelten. |
| `Click:hannover` | Die Form „hannover" auf der Karte anklicken. |
| `Click:berlin,potsdam-brandenburg` | Pflicht: berlin + potsdam; optional: brandenburg. |
| `Mark:bayern,sachsen` | Markiert die zwei Formen (nur Anzeige). |
| `MC:+Paris\|Lyon\|Marseille` | Auswahl, Einzelklick: Paris richtig, Rest falsch. |
| `MC:+Rot\|+Blau\|Grün` | Rot **und** Blau richtig, Grün falsch. |
| `MC:+Paris\|-Keine\|Lyon` | Paris richtig, „Keine" immer sichtbar, Lyon Füller. |
| `MC+:+Rot\|+Blau\|-Keine\|Gelb` | Sammeln + absenden: Rot+Blau richtig, „Keine" immer dabei. |
| `MC:=1\|2\|+3\|4\|mehr` | Feste Reihenfolge, 3 ist richtig. |
| `Fast:20:any:Berlin\|Hamburg` | Tippen auf Zeit, beliebige Reihenfolge, beide nötig. |
| `Fast:30:ordered:Gold\|Silber\|Bronze` | Tippen, in dieser Reihenfolge. |
| `SketchImage:waagerecht-3` | Lädt eine Skizze (setzt sie zurück). |
| `SketchImageAdd:stern,4` | Hängt „stern" in Rasterfeld 4 an. |
| `SketchImageFill:2,Rot` | Färbt Fläche 2 rot. |

Reicht das, bist du durch. Der Rest sind die Feinheiten von MC, Fast, Sketch und den Shuffle-Markern.

## MC / MC+ — Optionen

`MC` wertet jeden Klick **sofort** (ein Fehlklick bricht ab). `MC+` lässt **markieren und absenden**
(alles oder nichts). Beide teilen sich dieselbe Optionssyntax, und es gibt nur eine:

Optionen trennt `|`. Ein führendes `+ - ~ ?` gibt die Rolle, nackt heißt `?`. Die Rolle frisst nur
das erste Zeichen, der Rest ist Text. Mehr Regeln gibt es nicht.

Die alte Sternform `a|b*c|d` ist abgeschafft. Sie hat die Bedeutung einer Antwort davon abhängig
gemacht, ob eine **andere** Antwort derselben Zeile mit `+` beginnt: Ein führendes Minus war mal
Text und mal Rolle. Ein Step, der noch so geschrieben ist, hat jetzt keine richtige Antwort und
bricht ab, statt still etwas anderes zu bedeuten.

### Rollen

| Präfix | Rolle | Wirkung |
|---|---|---|
| `+` | richtig | die gesuchte Antwort |
| `-` | falsch, **immer sichtbar** | fester Distraktor (z. B. `Keine`) |
| `~` | falsch, **kein Abbruch** | wird als falsch gezeigt, bricht nicht ab und zählt in **keinem** der beiden Typen zur Antwort |
| `?` | Füller | wird nur zum Auffüllen gezogen |
| *(nackt)* | Füller | wie `?` |

### Sonderfälle

| Beispiel | | Bedeutung |
|---|---|---|
| `MC:+-40°\|0°` | ✓ | Die Rolle frisst nur das erste Zeichen → richtig ist „-40°". |
| `MC:+50°\|--40°` | ✓ | `--40°` = fester Distraktor „-40°". |
| `MC:+\+5\|0\|10` | ✓ | Escape **hinter** der Rolle → richtig ist „+5". Nur so gehen Vorzeichen und Rolle zusammen. |
| `MC:+0°\|\-40°` | ✓ | Escape ohne Rolle → „-40°" ist ein Füller, kein fester Distraktor. |
| `MC:+Ja\|+Nein` | ✓ | Beide richtig. |
| `MC:+Bürger\|alle Einwohner*innen` | ✓ | `*` ist gewöhnlicher Text — der Gender-Stern braucht nichts. |
| `MC:Paris\|Lyon` | ✗ | Kein `+` → keine richtige Antwort. |
| `MC:+Berlin\|Berlin` | ✗ | Text doppelt. |
| `MC:` | ✗ | Leer. |

Feste Reihenfolge: führendes `=` am Body (`MC:=…`, `MC+:=…`). Mehr als 8 sichtbare Pflicht-Antworten
(`+`/`-` zusammen) fliegen erst zur Laufzeit.

### Verhalten

Nicht Syntax, aber die Antwort auf „warum tickt das so":

- **Der Typ bestimmt den Modus, nicht die Zahl der richtigen Antworten.** `MC+` sammelt **immer**,
  auch bei einer einzigen Richtigen — sonst verriete das Klickverhalten (sofort werten gegen
  markieren), wie viele gesucht sind.
- Aus demselben Grund ist der **Submit-Knopf immer sichtbar**, bei `MC` inert. Seine Anwesenheit darf
  den Modus nicht verraten.
- Ein `~`-Klick wird in beiden Typen als falsch **gezeigt**, aber nicht gewertet: In `MC` läuft die
  Karte weiter, in `MC+` landet er gar nicht erst in der Auswahl — sonst könnte man ihn *statt* der
  richtigen Antwort abschicken. `isFinallyCorrect` streicht ihn zusätzlich heraus.
- Ein `~`-Klick **schüttelt** den Knopf kurz und lässt ihn danach stehen wie vorher. Keine Farbe:
  Grün und Rot heißen „gewertet", und gewertet wird hier nichts. Eine bleibende Markierung würde
  außerdem verraten, dass die Antwort nicht dazugehört.
- **Fragt eine Karte dasselbe Vokabular mehrfach, schreibe überall dieselbe Optionsmenge** und lass nur
  die Präfixe wandern. Dann stehen die Antworten über alle diese Steps hinweg an ihrem Platz; sonst
  springen sie sichtbar. Erzwungen wird nur, dass die Pflicht-Antworten (`+`/`-`) in einer solchen
  Gruppe zusammen höchstens acht sind.

## Fast — Details

Aufbau: `Fast:<sekunden>:<modus>:<antwort>\|<antwort>\|…` — Zeit je Feld > 0, höchstens 10 Felder.

| Modus | Bedeutung |
|---|---|
| `ordered` | in dieser Reihenfolge eingeben; doppelte Antworten erlaubt |
| `any` | beliebige Reihenfolge, **alle** nötig |
| `anyN` | N von allen genügen (`any3`); **keine** Hinweise |

Je Antwort: Schreibvarianten mit `,` getrennt, optional ein `<Hinweis>` davor.

| Beispiel | | Bedeutung |
|---|---|---|
| `Fast:15:any:Kubrick,Stanley Kubrick\|1968` | ✓ | Zwei Antworten, die erste mit zwei Varianten. |
| `Fast:30:ordered:<1.>Gold\|<2.>Silber` | ✓ | Mit Hinweisen, in Reihenfolge. |
| `Fast:20:any3:rot\|blau\|grün\|gelb\|schwarz` | ✓ | Drei von fünf genügen. |
| `Fast:20:any2:<Hinweis>rot\|blau` | ✗ | `anyN` + Hinweis geht nicht. |
| `Fast:0:any:rot\|blau` | ✗ | Zeit muss > 0 sein. |

## Sketch-Steps

Vom Flaggen-Generator ausgerechnet, selten von Hand. Felder `0…8` sind das 3×3-Raster.

| Step | Bedeutung |
|---|---|
| `SketchImage:waagerecht-3` | Hintergrund laden, Skizze zurücksetzen. |
| `SketchImageAdd:stern,4` | „stern" in Feld 4, Größe 1,0. |
| `SketchImageAdd:stern,4,0.8` | mit Größe 0,8. |
| `SketchImageAdd:stern,4,0.8,-10,0` | mit Größe und Versatz dx=-10, dy=0. |
| `SketchImageMark:2` | Fläche 2 hervorheben, alle anderen verlieren die Markierung. |
| `SketchImageMark:3\|4` | Flächen 3 und 4 gemeinsam hervorheben — ein Element aus mehreren Flächen. |
| `SketchImageFill:2,Rot` | Fläche 2 rot füllen (unbekannte Farbe fliegt). |
| `SketchImageFill:3\|4,Rot` | Flächen 3 und 4 rot füllen, eine Farbentscheidung für beide. |

## Shuffle-Marker

Einem Step **vorangestellt**; mischen die Reihenfolge ganzer Blöcke.

| Marker | Bedeutung |
|---|---|
| `<ShuffleStart>` | Beginn; Steps bis zum nächsten Marker = ein Segment. |
| `<ShuffleBreak>` | schließt das Segment, beginnt das nächste. |
| `<ShuffleEnd>` | schließt das letzte; die Segmente werden gegeneinander gemischt. |

- **Jedes Segment muss selbst Input verlangen** — sonst rutscht es je nach Wurf unbemerkt durch → Abbruch.
- `<ShuffleStart>` ohne `<ShuffleEnd>` → Abbruch.
- **Gemischt wird bei jedem Durchgang neu**, nicht einmal beim Einlesen des Decks — dieselbe
  Karte läuft beim nächsten Mal anders. Gleiche Stelle wie die Reihenfolge der MC-Antworten.

## Abspann — `<OnFail>`

Einem Step **vorangestellt**. Alles ab `<OnFail>` bis zum Zeilenende läuft nur, wenn die Karte falsch
beantwortet wurde, und ersetzt dann das sofortige Ende. Nach einem fehlerfreien Durchlauf wird der
Block nie erreicht.

| Zeile | Bedeutung |
|---|---|
| `Output:Frage?;MC:A*B;Image:X.svg;Pause:;<OnFail>Image:X.svg;Pause:` | Richtig: Bild, Pause, Ende. Falsch: Aufdeckung, Pause, dann Bild, Pause, Ende. |

- Der Block **darf nichts fragen** (`MC`, `MC+`, `Input`, `Click`, `Fast`) → Abbruch. Die Karte ist an
  dieser Stelle längst entschieden. Er zählt deshalb auch nicht für die Pflicht, dass eine Karte
  irgendwo Input verlangt.
- **Kein Endezeichen** — der Block reicht bis Zeilenende. Anders als bei `<ShuffleStart>` kann dabei
  nichts verloren gehen.
- Höchstens einmal je Zeile; nicht innerhalb eines Shuffle-Blocks und kein Shuffle darin → Abbruch.
- Der Fehler wird wie bisher aufgedeckt und pausiert; erst der Druck darauf startet den Abspann. Im
  Fehlerfall also zwei Tastendrücke — erst die Aufdeckung ansehen, dann den Abspann.
- Wer ihn **immer** sehen will, schreibt seine Steps zweimal: einmal am normalen Ende, einmal im
  Block. Der Flaggen-Generator tut genau das mit dem `Image:`-Step.

## Datei-Konventionen

- UTF-8 **mit BOM**, **CRLF**, `;` als Spaltentrenner.
- **Keine Kommas in Zellen** außer als Werttrenner (`,` trennt Varianten und Labels).
- `x` in einer Spalte des Systematik-Blattes heißt „diese Frage nicht stellen" (Generator-Ebene, nicht im Deck).

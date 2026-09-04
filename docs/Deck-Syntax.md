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
| `MC:Paris*Lyon\|Marseille` | Auswahl, Einzelklick: Paris richtig, Rest falsch. |
| `MC:Rot\|Blau*Grün` | Rot **und** Blau richtig, Grün falsch. |
| `MC:+Paris\|-Keine\|Lyon` | Präfix: Paris richtig, „Keine" immer sichtbar, Lyon Füller. |
| `MC+:+Rot\|+Blau\|-Keine\|Gelb` | Sammeln + absenden: Rot+Blau richtig, „Keine" immer dabei. |
| `MC:=1\|2\|+3\|4\|mehr` | Feste Reihenfolge, 3 ist richtig. |
| `Fast:20:any:Berlin\|Hamburg` | Tippen auf Zeit, beliebige Reihenfolge, beide nötig. |
| `Fast:30:ordered:Gold\|Silber\|Bronze` | Tippen, in dieser Reihenfolge. |
| `SketchImage:waagerecht-3` | Lädt eine Skizze (setzt sie zurück). |
| `SketchImageAdd:stern,4` | Hängt „stern" in Rasterfeld 4 an. |
| `SketchImageFill:2,Rot` | Färbt Fläche 2 rot. |

Reicht das, bist du durch. Der Rest sind die Feinheiten von MC, Fast, Sketch und den Shuffle-Markern.

## MC / MC+ — Formen

`MC` wertet jeden Klick **sofort** (ein Fehlklick bricht ab). `MC+` lässt **markieren und absenden**
(alles oder nichts). Beide teilen sich dieselbe Optionssyntax und **beide** verstehen beide Schreibweisen:

| Form | Regel |
|---|---|
| **Alt** | Richtig vor dem ersten `*`, falsch danach. Kein `*` → alle richtig. |
| **Präfix** | Gilt für den ganzen Step, **sobald eine seiner Optionen mit `+` beginnt**. Jede Option trägt dann eine Rolle, nackt heißt `?`. |

Nur ein führendes `+` schaltet auf Präfix um — ein führendes `-`/`~`/`?` allein bleibt Text.

### Rollen (Präfixform)

| Präfix | Rolle | Wirkung |
|---|---|---|
| `+` | richtig | die gesuchte Antwort |
| `-` | falsch, **immer sichtbar** | fester Distraktor (z. B. `Keine`) |
| `~` | falsch, **kein Abbruch** | anklickbar ohne Wertung (nur `MC`) |
| `?` | Füller | wird nur zum Auffüllen gezogen |
| *(nackt)* | Füller | wie `?` |

### Sonderfälle

| Beispiel | | Bedeutung |
|---|---|---|
| `MC:-40°*0°\|32°` | ✓ | Altform, „-40°" richtig — führendes Minus ist Text, kein Escape nötig. |
| `MC:+-40°\|0°` | ✓ | Die Rolle frisst nur das erste Zeichen → richtig ist „-40°". |
| `MC:+50°\|--40°` | ✓ | `--40°` = fester Distraktor „-40°". |
| `MC:Ja\|Nein` | ✓ | Beide richtig — kein `*`. |
| `MC:Bürger*alle Einwohner*innen` | ✓ | Nur der **erste** `*` trennt; jeder weitere ist Text (Gender-Stern). |
| `MC:\+5*0\|10` | ✓ | Escape `\`: „+5" bleibt die richtige Antwort. |
| `MC:+5*0\|10` | ✗ | „+5" wird als Präfix gelesen → verstümmelt, **ohne Abbruch**. Nimm `\+5`. |
| `MC:*a\|b` | ✗ | Keine richtige Antwort. |
| `MC:Berlin\|Berlin*Hamburg` | ✗ | Text doppelt. |
| `MC:` | ✗ | Leer. |

Feste Reihenfolge: führendes `=` am Body (`MC:=…`, `MC+:=…`). Mehr als 8 sichtbare Pflicht-Antworten
(`+`/`-` zusammen) fliegen erst zur Laufzeit.

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
| `SketchImageMove:2,5` | Fläche 2 nach Feld 5. |

## Shuffle-Marker

Einem Step **vorangestellt**; mischen die Reihenfolge ganzer Blöcke.

| Marker | Bedeutung |
|---|---|
| `<ShuffleStart>` | Beginn; Steps bis zum nächsten Marker = ein Segment. |
| `<ShuffleBreak>` | schließt das Segment, beginnt das nächste. |
| `<ShuffleEnd>` | schließt das letzte; die Segmente werden gegeneinander gemischt. |

- **Jedes Segment muss selbst Input verlangen** — sonst rutscht es je nach Wurf unbemerkt durch → Abbruch.
- `<ShuffleStart>` ohne `<ShuffleEnd>` → Abbruch.

## Datei-Konventionen

- UTF-8 **mit BOM**, **CRLF**, `;` als Spaltentrenner.
- **Keine Kommas in Zellen** außer als Werttrenner (`,` trennt Varianten und Labels).
- `x` in einer Spalte des Systematik-Blattes heißt „diese Frage nicht stellen" (Generator-Ebene, nicht im Deck).

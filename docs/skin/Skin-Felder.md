# ThosSuite — Die Felder eines Skins

**Stand:** 09.08.2026

**Charakter dieses Dokuments:** die *Mechanik* hinter einem Skin — wie ein Wert aus der
properties-Datei in die Oberfläche kommt, welche Formate es gibt, wie die Staffelungen greifen, was
die Zustände bedeuten und wo die Fallen liegen. Wer wissen will, **welches Feld welche Komponente
färbt**, schlägt nicht hier nach, sondern in der Tabelle daneben.

> ### Die Arbeitsteilung
>
> **`Skin-Matrix.xlsx`** — jede CSS-Regel als eigene Zeile: Komponente, Bereich, Zustand, Selektor,
> Property, Feld, Vorgabe. Filtere nach *Komponente*, um zu sehen, woran Du drehen musst. Filtere
> nach *Feld*, um zu sehen, was Du damit sonst noch triffst.
>
> **Dieses Dokument** — alles, was keine Tabellenzeile ist.
>
> **Die Trennregel:** Was im Stylesheet landet, steht in der Tabelle. Was nicht ins CSS geht, steht
> hier. Jedes Feld erscheint mit seiner Vorgabe in genau einem der beiden — so kann nichts
> auseinanderlaufen.
>
> Eine Kante hat die Regel: `dashBoardTileTopHeight` und `-BottomHeight` sind CSS,
> `dashBoardTileWidth` ist es nicht. Die Maße einer Dashboard-Kachel stehen dadurch in beiden
> Dokumenten.

## 0. Inhalt

1. Wie ein Wert in die Oberfläche kommt
2. Die Formate in der properties-Datei
3. Die Felder außerhalb des Stylesheets
4. Staffelung und Namensschemata
5. Die Zustände (Pseudoklassen)
6. Geteilte Felder — wo es beißt
7. Tote Felder

---

## 1. Wie ein Wert in die Oberfläche kommt

**Vier Sorten Vorgabe.** In der Spalte *Vorgabe* der Matrix steht eine davon:

- **Pflicht** — kein Vorgabewert. Fehlt das Feld, knallt es beim Erzeugen des Stylesheets
  (`CssBuilder.add` wirft bei `null`).
- **ein Wert** (z. B. `2`) — der Java-Vorgabewert am Feld selbst.
- **← Formel** — wird im Vorgaben-Durchlauf am Anfang von `buildCss` aus anderen Feldern
  abgeleitet, aber nur wenn das Feld `null` ist. Steht ein Wert in der Datei, gewinnt der.
- **entfällt** — bleibt das Feld leer, wird die betreffende CSS-Regel gar nicht erst geschrieben.
  Genau das hält bestehende Skins unberührt, wenn ein neues Feld dazukommt (so kamen
  `componentShadow` und `activeBorderColor` herein).

Daneben gibt es Zeilen mit **„fest im Code"** — Werte, die kein Feld haben und sich über die
properties-Datei nicht ändern lassen (`transparent`, `center`, feste Nullen). Rund ein Viertel der
Tabelle. Wer dort etwas ändern will, ändert Java.

**Vererbung.** Ein abgeleiteter Skin lädt erst die Datei seines Elternteils, dann seine eigene; das
Spätere gewinnt. `BaseColorSkin` ist der einzige heutige Fall.

**FailFast in eine Richtung.** Jeder Schlüssel in einer properties-Datei muss ein gleichnamiges Feld
haben, sonst bricht das Laden ab. Umgekehrt gilt das nicht — ein Feld ohne Schlüssel ist der
Normalfall.

> **Folge für die exe:** Der Config-Ordner ist gemeinsamer Zustand zwischen Entwicklungsversion und
> installierter Anwendung. Bekommt eine properties-Datei einen neuen Schlüssel, startet eine ältere
> exe nicht mehr, bis sie neu gebaut ist. Dasselbe gilt für die Skin-Wahl, die in der Datenbank
> steht: ein Skin, den ein Binary nicht kennt, lässt es beim Start werfen.

---

## 2. Die Formate in der properties-Datei

**Farbe** — Hex mit Doppelkreuz: sechsstellig (`#c9cfd6`) oder achtstellig mit Alpha (`#0000000F`).
Andere Schreibweisen gibt es nicht. Geparst wird über `Color.web`, das beide kennt und bei allem
anderen eine Ausnahme wirft. Das frühere `r,g,b,alpha` ist entfallen — es konnte nur Schwarz, weil
der Parser dort die Wertebereiche verwechselte.

**Schrift** — `Familie,Stil,Größe`, Stil nach Swing-Logik: `0` normal, `1` fett, `2` kursiv,
`3` beides.

**Rahmen** (`BorderParams`) — sieben Werte:
`Breite, Farbe, Innenabstand oben, rechts, unten, links, Eckradius`.
Kurzform mit drei Werten: `Breite, Farbe, Eckradius`.

- **Der Eckradius wird beim Einlesen halbiert.** FlatLaf zählte den Durchmesser, JavaFX den Radius —
  `28` in der Datei ergibt 14 Pixel. Häufigste Stolperstelle.
- Der Farb-Slot darf **leer** bleiben (`0,,10,20,10,20,28`), dann greift `borderColor`.
- Innerhalb dieses Formats sind Farben faktisch nur als Hex erlaubt: die Komma-Schreibweise würde
  die Positionen zerschießen.
- Breite `0` schaltet das Zeichnen nicht ab — ein Strich der Breite 0 wird einfach ein Pixel breit.
  Für rahmenlos gehört die Farbe auf durchsichtig.

**Rechteck** — `x, y, Breite, Höhe`. Breite oder Höhe `0` heißt nicht „unsichtbar", sondern „miss
dich selbst": die Komponente errechnet ihre Größe dann aus ihrem Inhalt. So machen es die
Zurück-Knöpfe und die Eingabefelder.

**Kommazahlen gibt es nicht.** Der Loader kennt Farbe, Schrift, Rahmen, Rechteck, ganze Zahl und
Text — **keinen Zweig für `Double`**. Die beiden Felder dieses Typs, `shapeMapStandardBorderWidth`
und `shapeMapFederalStateBorderWidth`, lassen sich aus einer properties-Datei deshalb **gar nicht
setzen**: Der Schlüssel käme durch die FailFast-Prüfung, weil das Feld existiert, und der Wert würde
danach stillschweigend verschluckt. Wer die Strichbreiten der Shape-Karte ändern will, ändert die
Vorgabe im Feld selbst. Dieselbe Falle wie bei `contentSize`.

Ein neues Feld für einen Bruchteil wird deshalb als **Prozentzahl** angelegt, nicht als Kommazahl —
so wie `mcResultTintPercent`.

**Roher CSS-Text** — einige Felder werden unverändert ins Stylesheet durchgereicht:
`componentShadow`, `chartRootPadding`, `menuButtonPadding`, `menuItemPadding`. Dort gilt
CSS-Syntax, kein eigenes Format. Ein Tippfehler fällt hier **nicht** auf — JavaFX überliest eine
kaputte Regel stillschweigend.

---

## 3. Die Felder außerhalb des Stylesheets

Diese 115 Felder werden nie zu CSS. Sie gehen über zweckgeschnittene Records nach `shared.ui`.

### 3.1 Einzelwerte

| Feld | Vorgabe | Wofür |
|---|---|---|
| `contentSize` | 1910 × 1000 | Größe der Spielfläche und damit faktisch die Fenstergröße. **Aus einer properties-Datei nicht setzbar** — der Parser kennt keinen Zweig dafür, ein Schlüssel würde vom FailFast durchgelassen und dann still ignoriert. Wer das braucht, überschreibt das Feld in einer Skin-Klasse. |
| `dashBoardTileWidth` | `250` | Breite einer Dashboard-Kachel. Die beiden Höhen stehen in der Matrix. |
| `verticalGapMC` | Pflicht | Senkrechter Abstand zwischen den Antwortknöpfen. |
| `popupMonitorMargin` | `20` | Abstand, den ein vergrößertes Vorschaubild zum Bildschirmrand hält — im Tagebuch wie bei den Filmen. |
| `imageMapOverlayContentInset` | `11` | Breite des weich auslaufenden Rahmens, der in die Mini-Kartenbilder eingebacken ist. Ohne ihn säßen die Klickpunkte verschoben. Je Karte überschreibbar als `<mapName>ImageMapOverlayContentInset`. |
| `hannoverImageMapOverlayContentInset` | `0` | Die einzige solche Ausnahme: Hannovers Karte reicht bis an jede Kante. |

### 3.2 Symbole

`backButtonIcon`, `skipButtonIcon`, `playButtonIcon`, `cancelButtonIcon` — alle Pflicht.

Gebaut wird heute nur **`BACK`**. Die drei anderen stehen in jeder properties-Datei, ihre Dateien
liegen aber nicht mehr im Symbol-Ordner.

Das Zurück-Symbol **wird eingefärbt**: jeder Pixel bekommt `textActiveComponentColor`, nur die
Deckkraft der Datei bleibt. Die PNG liefert also die Form, der Skin die Farbe — deshalb braucht es
für helle und dunkle Skins nur eine Datei. Nur diese eine Rolle wird eingefärbt.

**Die Größe kommt allein aus der Datei.** Das Bild wird ohne `fitWidth`/`fitHeight` angezeigt, also
1:1 (heute 50 × 50). Ein größeres Symbol braucht eine größere Datei.

### 3.3 Kartenbilder

Vier je Karte: `<mapName>MapImageName`, `…MapOverlayImageName`, `…MapInactiveImageName`,
`…MapInactiveOverlayImageName` — groß und klein, jeweils aktiv und ausgegraut. Hier gibt es **keine**
Staffelung: fehlt eines, bleibt es leer und wird still übersprungen. Die vier werden beim Start
vorgewärmt, solange der Startbildschirm steht.

Hannovers vier Namen stehen in keiner properties-Datei, sondern als Java-Vorgabe am Feld.

### 3.4 Wallpaper

`emptyWallpaperName` (der ruhige Grund für Statistik, Tagebuch, Filme, Dashboard und jede Lernform
ohne eigenes Bild), `startScreenWallpaperName` (der einzige geschmückte Bildschirm) sowie
`<mapName>WallpaperName` und `<kategorie>WallpaperName`.

**Was hinter dem Spielfeld liegt, ist ein Bild, keine Farbe.** `playFieldBackground` färbt Dialoge
und Popups, nicht die Spielfläche.

**Kein eigenes Wallpaper ist meistens Absicht, kein Versehen.** Die großen Kartenspiele (`world`,
`germany` und die Länderkarten) füllen den Bildschirm so weit aus, dass ein Motiv dahinter nur
stören würde — dort ist das leere Wallpaper der gewollte Zustand.

### 3.5 Die Layout-Rechtecke

77 Felder nach einem Muster:

```
<präfix> + Session + <Rolle>
```

**Rollen:** `MapPanel`, `ImagePanel`, `TextInputPanel`, `McPanel`, `AnswerSlotsPanel`, `BackButton`,
`QuestionPanel`, `ProgressPanel`, `HistoryPanel`, `ClockPanel`.

**Präfixe:** `mc`, `fw`, `world`, `germany`, `hannover`, `region`, `es`, `it`, `en`, `us`, `cs`,
`be`, `ch`, `hs`, `oz`, `au`, `br`, `hr`.

---

## 4. Staffelung und Namensschemata

**Rechtecke — zwei Stufen:** erst `<mapName>`, sonst `<kategorie>`.

Die Kategorie ist `anki` oder `region`. Da es **kein einziges Feld mit Präfix `anki`** gibt, läuft
die zweite Stufe für alle Anki-Decks systematisch ins Leere — dort muss jedes Rechteck unter seinem
eigenen Namen stehen.

**Hannover ist der Sonderfall:** seine acht Rechtecke fallen auf die der Welt-Karte zurück, und zwar
nicht über die Staffelung, sondern über den Vorgaben-Durchlauf in `buildCss`.

**Wallpaper — drei Stufen:** `<mapName>`, dann `<kategorie>`, dann `emptyWallpaperName`.

**Mini-Karten-Rahmen — zwei Stufen:** `<mapName>ImageMapOverlayContentInset`, sonst der allgemeine
Wert.

**Kartenbilder — keine Staffelung.**

---

## 5. Die Zustände (Pseudoklassen)

Die Matrix zeigt, welche Farbe ein Zustand ergibt. Hier steht, **wann** der Code ihn setzt.

| Zustand | Auf welchem Ding | Wann |
|---|---|---|
| `:active` | Shape-Karten-Form | Die Form ist zu suchen **und** anklickbar (Klick-Modus). |
| `:inactive` | Shape-Karten-Form | Die Form gehört zur Aufgabe, wird aber **getippt statt geklickt** (Eliminieren, Schreiben) — oder sie ist wirklich unbeteiligt. |
| `:paused` | Shape-Karte (ganze Fläche) | Die Karte nimmt keine Klicks an. Wird zusammen mit `:inactive` gesetzt, wenn getippt wird. |
| `:marked` | Karten-Form, beide Arten | Genau die eine Form, deren Namen gerade gefragt ist. |
| `:correct` / `:incorrect` | Karten-Form, Antwortknopf, Antwortfeld | Richtig bzw. falsch beantwortet. Auf der Bild-Karte ist `:incorrect` ein roter Punkt an der Klickstelle, keine Form. |
| `:active` | Antwortknopf | Der Knopf trägt eine anklickbare Antwort. |
| `:inactive` | Antwortknopf | Leer oder nach der Auflösung bedeutungslos. |
| `:active` | Antwortfeld (Fast Write) | Gehört zum laufenden Schritt, ist aber **nicht anklickbar**. |
| `:expected` | Antwortfeld | Das bei erzwungener Reihenfolge nächste Feld. **Bewusst ohne Regel** — das oberste nicht-grüne ist ohnehin das gesuchte. |
| `:squeezed` / `:tiny` | Antwortknopf und -feld | Der Text passt einzeilig nicht mehr. Rein Layout, additiv zu allem anderen. |
| `:focused` | Eingabefeld | Es wird eine Eingabe erwartet — `setActive` holt den Fokus. |
| `:disabled` | Eingabefeld | Keine Eingabe erwartet; zeigt ggf. die aufgedeckte Lösung. |
| `:invalid-query` | Suchfeld im Tagebuch | Die Abfrage ist nicht auswertbar. |
| `:paused` | Uhr | Die Uhr tickt nicht — **auch wenn sie auf 0 abgelaufen ist**. |
| `:achieved` / `:failed` / `:in-progress` | Diagrammbalken | Ziel erreicht / verfehlt / laufende Woche. |
| `:highlighted` | Vorschlagsliste | Der per Tastatur angewählte Vorschlag. |

**Drei Stolperstellen in der Benennung:**

1. **`:active` heißt dreierlei.** Auf der Karte „anklickbar", am Antwortknopf „belegt und
   anklickbar", am Antwortfeld „im Spiel, aber unanklickbar".
2. **Beim Eliminieren durch Tippen tragen die gesuchten Landkreise `:inactive`** — also dieselbe
   Farbe wie wirklich bedeutungslose Formen. Der Zustandsname beschreibt die *Klickbarkeit*, nicht
   die fachliche Rolle. Wer hier optisch unterscheiden will, braucht ein neues Feld.
3. **Die Bild-Karte kennt `:active`, `:inactive` und `:paused` gar nicht.** Sie tauscht stattdessen
   ihre Bilder gegen graue. Derselbe Aufruf aus der Session bewirkt bei den zwei Kartenarten also
   grundsätzlich Verschiedenes.

**Zustände sind zugleich Speicher:** Die Shape-Karte liest ihren Sitzungsstand aus den
Pseudoklassen zurück und spielt ihn wieder ein — deshalb überlebt eine laufende Session einen
Skinwechsel.

---

## 6. Geteilte Felder — wo es beißt

Die Matrix *zeigt* diese Kollisionen, sobald man nach dem Feld filtert. Hier steht, warum sie
weh tun.

1. **`activeComponentBgColor` ist der Universaleimer** — vierzehn Selektoren. Wer die Karten-Flächen
   anders färben will, färbt zwangsläufig den Griff der Bildlaufleiste mit.
2. **`disabledComponentBgColor` trägt zwei gegensätzliche Aussagen:** „nicht bedienbar" und „um
   diese Formen geht es gerade" (Eliminieren, siehe oben). Dazu den Hintergrund des
   Film-Kommentarfensters — diese Verwendung ist im Code selbst als Notlösung markiert.
3. **`shapeMapColor0` und `disabledComponentBgColor` treffen sich auf der Karte.** Deko-Nachbarn und
   inaktive Formen liegen nebeneinander; stehen sie auf demselben Wert, verschwimmt beides.
4. **`displayTextProgressBgColor`** färbt das Fortschrittsfeld *und* die obere Dashboard-Kachelhälfte
   — zwei völlig verschiedene Bildschirme an einem Schlüssel.
5. **`displayTextQuestionBgColor`** färbt das Fragefeld *und* die Tagebuchkarte.
6. **`incorrectTextColor`** heißt zweimal „falsche Antwort" und einmal nur „Hinweistext im
   Tagebuch".
7. **`textColor` ist die Rahmenfarbe der Tabelle.** Ein Skin mit hellem Text bekommt hell leuchtende
   Tabellenraster. So gewollt für rahmenlose Skins, aber überraschend.
8. **`menuBarBackground` und `playFieldBackground`** hängen an je neun Selektoren, und aus
   `playFieldBackground` werden vier verschiedene Abstufungen gerechnet. Eine Änderung verschiebt
   Bildlauf-Schiene, Tabellenzeilen und Kalenderzellen gleichzeitig.
9. **Reihenfolge entscheidet:** `.my-answer-slot:active` und `.my-mc-button:active` haben dieselbe
   Spezifität. Welche Farbe gewinnt, hängt allein daran, dass die Antwortfeld-Regeln *nach* den
   MC-Regeln geschrieben werden.
10. **`.button` zieht die Pfeilknöpfe von Kalender und Zahlenfeld mit.** Wer den Knöpfen etwas gibt,
    gibt es auch diesen — deshalb setzt die Kalender-Methode dieselben Farben nochmal.
11. **Verflochten auch außerhalb des CSS:** `bigComponentStyle` (Eckradius und Rahmenbreite der
    großen Flächen) füttert Bild-Panel *und* Bild-Karte; `mcMetrics` füttert Antwortauswahl *und*
    Fast-Write-Felder. Diese Wege stehen nicht in der Matrix, weil sie kein CSS sind.
12. **`thinBorderWidth` wird an einer Stelle ignoriert:** der Strich unter der Hauptmenüleiste steht
    fest auf 1 Pixel.

---

## 7. Tote Felder

| Feld | Lage |
|---|---|
| `disabledButtonBgColor` | Nirgends gelesen. Einziges Vorkommen im ganzen Quellbaum ist die Deklaration. |
| `stageBorderColor` | Nur in einem auskommentierten Block. |
| `menuDisabledForeground` | Wird im Vorgaben-Durchlauf aus zwei Farben gemischt — und danach nie gelesen. Die ausgegrauten Menüeinträge bekommen im CSS `textColor`; grau **aussehen** tun sie, weil JavaFX auf deaktivierten Knoten zusätzlich selbst entsättigt. Genau deshalb bringt es dort auch nichts, eine Farbe zu setzen. |
| `skipButtonIcon`, `playButtonIcon`, `cancelButtonIcon` | In jeder properties-Datei gesetzt, die Dateien fehlen im Symbol-Ordner, und keine Rolle außer `BACK` wird gebaut. |
| `hannoverSession*` (acht Rechtecke) | Deklariert, in keiner Datei gesetzt; leben ausschließlich vom Rückfall auf die Welt-Karte. |

# Vertagte Punkte

**Stand:** 29.07.2026 · Schritte 1–3f erledigt, alle Wächter scharf im Build

Alles, was während des Refactorings (26.–29.07.) bewusst **nicht** sofort gemacht wurde.
Sortiert nach **Fälligkeit**, nicht nach Thema — beim Abarbeiten ist „was ist jetzt dran" die
nützlichere Frage.

Schrittnummern beziehen sich auf `Skin-Refactoring-Plan.md` §8.

---

## A · Fällig im laufenden Umbau

### Schritt 3f — ✓ erledigt am 29.07.

Alle Bau-Methoden haben den Skin verlassen, alle Wächter sind leer und werden vom Build bewacht.
Was aus dieser Runde offen blieb:

| Punkt | Fundstelle |
|---|---|
| Bild-Karte: „EM 2021 — alle Länder grün, welches war falsch?" Das Zurücksetzen vor `markLastClickAsIncorrect()` löscht die bisherigen Markierungen mit | `ImageMapPane.markIncorrect` |
| Fragefeld der Region-Session: „Hier wäre allerdings CENTER schon angesagt" | `RegionSessionView:69` |

### Schritt 5 — Architekturdokument

`docs/Architektur-Dokumentation.md`, Abschnitt **„UI-Architektur: Skin-System"** ist nach dem Umbau
vollständig überholt. Er beschreibt Factory-Methoden, das Fallback-System und die
`SkinService`-API als „Stand heute korrekt" und trägt bereits den Vermerk *„Refactoring
ausstehend"*. Neu zu fassen sind mindestens:

- **Konzept** — der Skin baut nichts mehr, er liefert Werte und erzeugt das CSS
- **Factory-Methoden** — der ganze Abschnitt entfällt
- **Fallback-System** — bleibt inhaltlich, wandert aber hinter `sessionBounds(...)`
- **SkinService** — ist nur noch Registry; `setOwnerWindow`/`getOwnerWindow` liegen in `UiUtils`
- **Paket-Struktur** (§ „Technische Basis") — `shared.ui` / `shared.ui.components`, die vier
  Sprossen, die drei Wächter

Dazu die Schichtbeschreibung in `Design-Regeln.md`. Beides erst **nach** Schritt 4 — vorher
veraltet es noch einmal.

### Schritt 3b — Feature-Oberflächen (Diary, Movie, Dashboard, BarChart)

| Punkt | Fundstelle |
|---|---|
| ~~„Wieso baut hier der Skin?"~~ | **erledigt 28.07.** — `createMovieViewer` ist umgezogen, der Marker gelöscht |
| Der MovieScreen wird komplett anders aufgebaut als die übrigen | `SuiteSuggestionTextField:30` |
| Alc- und Fitbit-StatisticScreens sind voll mit UI-Kram | `Skin` (Klassen-Javadoc) |
| Code-Duplizierung in den `getBackgroundImage`-Methoden | `Skin`, drei Hintergrund-Methoden |
| `empty` und `default` gehen durcheinander — „hier holt empty das default" | `Skin.getEmptyBackgroundImage` |

### Schritt 3c — Session-/Lern-Teile

| Punkt | Fundstelle |
|---|---|
| ~~Zuschnitt der Session-Panes~~ | **erledigt 29.07.**, siehe `Learn-Zielbild-3c.md` §0 |
| ~~Das Feature reicht ein javafx-`Image` durch~~ | **erledigt** — die Panes liegen jetzt in `shared.ui` |
| ~~`ShapeMapState` im Feature~~ | **erledigt** — als eigenes Record in `shared.model` |
| Wie mit Hintergrundbildern umgegangen wird, gehört ins Regelwerk | `ComponentHost:33` |
| **`buildShapeMapWrapper`-Konstrukt.** Der Block gehört in die `ShapeMapPane`; das Hindernis (kein Zugriff auf Skin-Felder) ist unter U3 weg | `ShapeMapPane:65` |
| ~~`MultipleChoicePane.Metrics` — „streng genommen sickern skin.properties in eine UI-Komponente"~~ | **erledigt 29.07.** — wurde `shared.model.McMetrics`; nach der Schlüssel-Regel ist das kein Sickern, sondern der Normalfall |
| `overlayContentBounds` beschreibt, wo der Inhalt im Mini-Map-Bild sitzt — gehört eigentlich woandershin | `Skin.getOverlayContentBounds` |
| **Fehler im freien Spiel:** falsch geklickte Formen verschwinden bei „schwer" nicht, nur die richtigen. Bestehender Fehler, nicht aus dem Umbau | `RegionSessionView:16` |

### Jetzt fällig — die `SuiteXXX`-Familie durchsehen

**Der ursprüngliche Grund ist weggefallen.** Die Fassaden entstanden, weil Features javafx-frei
bleiben müssen: `GermanySessionPane` durfte kein `TextField` halten, also hielt es ein
`SuiteTextField`. **Seit 3c hält kein Feature mehr irgendeine Komponente** — die Panes liegen in
`shared.ui`, die Features bekommen fertige `ScreenView`s. Am besten zusammen mit Schritt 3f, weil
dort ohnehin die Konstruktoren angefasst werden.

**Entschieden (29.07.): Vererbung statt Fassade.** Eine Fassade schützt eine Grenze — und die Grenze
(„das Feature darf kein javafx anfassen") gibt es an dieser Stelle nicht mehr; innerhalb von
`shared.ui` kennen beide Seiten javafx. Was an Thorstens Instinkt wertvoll war, überlebt die
Vererbung: sinnvoll benannte Methoden wie `setActive(...)` (= leeren + aktivieren + fokussieren)
bleiben, nur das *Verbot* fällt weg — und das war nie der Punkt. Gegenprobe war `SuiteDialog`: als
Fassade bräuchte es ~15 Weiterreich-Methoden und `getDialogPane()` (32 Aufrufe) leckt trotzdem
durch. **Erster Schritt gemacht:** `ShapeMapPane extends StackPane`.

**Kriterium:** *Hat die Klasse eigenen Inhalt, oder verbirgt sie nur javafx?*

| | Zeilen | Befund |
|---|---|---|
| `SuiteTextField` | 46 | ✓ 29.07. — `extends TextField`, zwei Konstruktoren, Aussehen über `.text-field`. |
| `SuiteIconButton` | 29 | ✓ 29.07. — `extends Button`, holt sein Bild über `iconFor(rolle)`, Maße kommen rein. |
| `SuiteInfoLabel` | 168 | ✓ 29.07. — bekam `.my-info-label`; vorher kam ihr ganzes Aussehen aus den drei Session-Kennungen. |
| `SuiteImage` | 131 | ✓ 29.07. — holt den Eckradius selbst, Maße kommen rein. |
| `SuiteDatePicker` | 9 | trägt keinen Skin-Wert, nur die Entscheidung „keine Kalenderwochen". Überlebt nur über das Argument „Ort einer suite-weiten Festlegung", nicht über das Fassaden-Argument. **Dünnster Fall, legitim zu streichen.** |

**Folgefrage — `UiComponent` abschaffen.** Alle Bausteine sind inzwischen selbst Nodes und
implementieren `UiComponent` nur noch mit `getView() { return this; }`. Die einzige Ausnahme ist
`NoSessionMap`, das Nullobjekt: es hält eine leere `Group`. Wird auch das ein Node (etwa indem es
selbst von `Group` erbt), kann `ComponentHost` auf `Node...` umstellen und `UiComponent` samt
`getView()` entfallen.

### Das Karten-Konstrukt — umbenennen und erklären

**Umbenennen.** `SessionMap` ist nicht spezifisch genug: „Session" könnte irgendwann auch ein
kleines Spiel haben. Gemeint ist Lernen. Also `LearnMap`, `ShapeLearnMap`, `ImageLearnMap` — und für
das Nullobjekt eher `EmptyLearnMap` als `NoLearnMap`, das liest sich sonst wie eine Verneinung des
Lernens.

**Achtung, Anschlussfrage:** „Session" steckt auch in `SessionComponent`, `sessionBounds(…)`,
`SessionCallbacks`, `AnkiSessionView`, `RegionSessionView`. Entweder das Wort bedeutet dort
weiterhin „Lern-Session" und bleibt, oder es zieht mit um. Halbe Umbenennung wäre schlechter als
gar keine — einmal entscheiden.

**Erklären.** Das Konstrukt braucht einen kurzen Absatz — im Regeldokument oder als Paket-Javadoc.
Stand nach der Vereinfachung vom 29.07.:

```
ShapeLayer        Nachschlagetabelle: json-type → zIndex, interaktiv?, CSS-Layer-Klasse
MapNodeBuilder    Fabrik:             Geometrie → JavaFX-Node
SessionMap        das gemeinsame Vokabular, spricht durchgehend Ids
ShapeMapPane      die eine Karte, arbeitet ohnehin mit Ids
ImageMapPane      die andere Karte, arbeitet mit Geometrien und übersetzt selbst
NoSessionMap      die Karte der MC-Session, die keine hat (Nullobjekt)
```

Der Grund für das Interface: die beiden Panes sind sehr verschieden, die View soll beide bedienen,
ohne zu wissen welche.

✓ **Vereinfacht am 29.07.:** die beiden Übersetzer-Klassen `ShapeSessionMap` und `ImageSessionMap`
sind weg — die Panes setzen `SessionMap` selbst um. Mitgenommen: `Skin.applyImageMapLayout` (die
Methode, die die Pane von außen vermaß und einen Clip zurückgab) ist ebenfalls weg, `ImageMapPane`
bekommt ihr Feld im Konstruktor und baut den Clip selbst. Damit ist auch die Zeile erledigt, die im
alten Code den Marker *„Ich verstehe nicht, was hier passiert"* trug.

**Neu offen (29.07.): `#QuestionLabel` durch eine Modifikator-Klasse ersetzen.** Jetzt, wo
`SuiteInfoLabel` wiederverwendbar ist, ist die Kennung die falsche Mechanik — IDs sollen in einer
Szene eindeutig sein, zwei Info-Labels in einem Dialog brächen das. Sauberer:
`.my-info-label.question` statt `#QuestionLabel`. Betrifft `addSessionInfoLabelStyles` und die zwei
`setId(…)`-Aufrufe in den Views.

**Noch offen in der Familie:** `SuiteDatePicker` (dünnster Fall) und die Frage, ob
`SuiteInfoLabel` / `SuiteImage` / `MultipleChoicePane` ihre Maße im Konstruktor bekommen oder von
der View nach dem `new` gesetzt werden. `ShapeMapPane`, `SuiteTextField` und `SuiteIconButton` haben
sie im Konstruktor — das ist der Präzedenzfall.

### Schritt 3d — Chrome/Menüs

Aus dem Skin wandern: `createMenuBar`, `createMenu`, `createMenuItem`,
`createMainWindowHeaderBar`, `createResponsiveHeaderIcon`. Hängt an `MainWindow`, deshalb zuletzt.

**Offene Gestaltungsfrage — Kalenderwochen im DatePicker.** Ursache geklärt: `DiaryEditor:174`
macht als einzige Stelle `new DatePicker(...)` und setzt `setShowWeekNumbers` nicht, greift also
den JavaFX-Default ab. Die drei anderen Stellen gehen über `SuiteDatePicker` (ohne Kalenderwochen).
Der Unterschied ist **geerbt, nicht gewollt**. Zu entscheiden: nirgends, überall, oder je nach
Kontext als Parameter — in jedem Fall sollte auch der Editor über `SuiteDatePicker` gehen.

### Schritt 4 — Aufräumen

- `docs/Ordnerstruktur.txt` und `docs/Paketabhängigkeiten.dot` einmal neu erzeugen. Bewusst **nicht**
  zwischendurch — sie veralten bei jedem Move.
- Die drei Wächter-greps aus Plan §1.3 auf leer bringen.

### Schritt 5 — Regelwerk nachziehen

- **`StartScreen`-Regel:** *ein inhaltsloser Screen (reines Chrome/Hintergrund) darf sein eigener
  `ScreenView` sein.* Löst den (c)-Fall der ScreenView-Bestandsaufnahme per Regel statt per Code.
- **Dialog-Stufe 2a** fehlt: der parametrisierte Standarddialog (Primitive rein, Primitive oder
  `null` raus, kein Feature-seitiges Objekt). `TextPromptDialog`, `WhatsAppChatDialog`,
  `WhatsAppContactDialog` sehen nur deshalb wie Verstöße aus.
- **Verantwortungsrahmen** Feature / View / Skin (Plan §3.14) eintragen.
- **Die Schlüssel-Regel** (beschlossen 29.07.) — muss ins Regeldokument:

  > **Ein Baustein holt sich beim Skin, was für jede Verwendung gleich ist. Was von der Verwendung
  > abhängt, bekommt er übergeben.**
  >
  > Der Test steht in der Signatur des Skin-Zugangs: **braucht er ein Argument vom Aufrufer, dann
  > löst der Aufrufer auf und reicht das Ergebnis weiter.** Ein Argument *ist* der Kontext — wer
  > eines liefern muss, weiß etwas, das der Baustein nicht wissen soll.

  ```java
  bigComponentCornerRadius()               // kein Argument      → Baustein holt selbst
  iconFor(rolle)                           // Rolle, kein Kontext → Baustein holt selbst
  sessionBounds(mapName, kategorie, teil)  // braucht Schlüssel   → Aufrufer löst auf
  ```

  **Was die Regel nicht sagt.** Thorstens Entwurf enthielt „Komponenten bleiben feature-frei" und
  stolperte sofort über den eigenen Einwand: *„natürlich ist eine MovieCard nicht featurefrei"*.
  Zu Recht — das sind zwei Fragen, und nur die erste regelt diese Regel:

  | Frage | geregelt durch |
  |---|---|
  | Wer löst den Kontext auf? | **die Schlüssel-Regel** — gilt ausnahmslos für *alle* Bausteine, auch für `MovieCard` und `ShapeMapPane` |
  | Ist der Baustein an ein Feature gebunden? | eine **Namensfrage**, kein Konstruktionsprinzip |

  `MovieCard` darf sich `moviePosterWidth` holen (kein Schlüssel) und bekommt den Film übergeben
  (Kontext). Sie bleibt trotzdem eine Film-Komponente. Kein Widerspruch.

  **Nebenregel, die schon Praxis ist:** was ein Baustein sich holt, kommt bevorzugt als
  zweckgeschnittenes Record (`DialogStyle`, `MovieStyle`, `DashboardTileStyle`), nicht als
  Feld-Getter — damit keine Property-Namen das skin-Paket verlassen.

- **Keine Streams, außer sie sind unbedingt nötig.** Steht bisher nirgends geschrieben, wurde aber
  schon zweimal im Review beanstandet (`DashboardScreenView`, `ShapeMapPane`). Gehört ins
  Regeldokument.
- **Skin-Vertrag** neu fassen — der bestehende Abschnitt ist als „vorläufig" markiert.
- Die drei Wächter als bewachte Zusagen festschreiben.

### Schritt 6 — die Wächter in den Maven-Build

**Angelegt am 29.07.** — `src/test/java/app/ArchitekturRegelnTest.java`, ArchUnit + JUnit 5 +
Surefire. Läuft bewusst **nicht** beim Speichern in Eclipse (m2e ruft kein Surefire), sondern bei
Run As → Maven build oder Run As → JUnit Test.

Gewählt wurde ArchUnit, weil es als einziges alle Regelsorten in einer Sprache abdeckt und
**Bytecode** liest statt Textzeilen — voll qualifizierte Nutzung ohne `import` rutscht also nicht
durch. Checkstyle `ImportControl` schied aus, weil es keine Zyklen prüfen kann; javaparser war keine
eigene Option, weil `scripts/PackageDependencyGraph.java` bereits einen funktionierenden
Import-Scanner samt SCC-Berechnung enthält.

**Scharf ist bisher nur Wächter 2** (kein Feature kennt den Skin). Die übrigen stehen auskommentiert
in derselben Datei und werden freigeschaltet, sobald sie halten:

| Regel | frei nach |
|---|---|
| Wächter 1 — `shared.skin` kennt `shared.ui` nicht | Schritt 3f |
| Zyklenfreiheit | Schritt 3f (siehe unten) |
| Wächter 3 — Bausteine nur aus `shared.ui` heraus | Schritt 4 |

**Ein Zyklus existiert heute:** `app.shared.skin ↔ app.shared.ui.components`. Kein eigener Befund,
sondern Wächter 1 und 3 von der anderen Seite: der Skin importiert `MultipleChoicePane`/`SuiteImage`/
`SuiteInfoLabel`, die Bausteine holen sich den `SkinService`. Verschwindet mit 3f.

Offen: die Vier-Sprossen-Ordnung aus Plan §1.1 als `layeredArchitecture()` mit aufnehmen.

---

## B · Klein, jederzeit machbar

- **Titel-Padding ins CSS.** `SuiteHeaderBar` bekommt die `font` ausschließlich für
  `font.getSize() * 0.3`. Gehört nach `addDialogStyles`, Selektor `.my-title`. Danach entfällt der
  Konstruktor-Parameter **und** die `font` im `DialogStyle`. → `SuiteHeaderBar:22`
- **FailFast-Startup-Check** für properties-Schlüssel, die kein Feld beanspruchen (~15 Zeilen).
  Findet `borderBackButton` in `skin_basecolor.properties`, das seit unbekannt wann ins Leere läuft
  — die einzige systematische FailFast-Verletzung im Skin.
- **Re-Warming des Bildcaches nach Skinwechsel dokumentieren.** Heute lädt die erste Bildkarte nach
  einem Wechsel synchron; das ist bewusst akzeptiert, steht aber nirgends im Code.
- **Zwei Schlüssel für dasselbe Deck.** Wallpaper werden über die Deck-**Id** aufgelöst
  (`lk_bbWallpaperName`), Layouts über den **mapName** (`lkSession…Panel`). Beim Aufräumen der
  Properties vereinheitlichen.
- **`getContentSize`:** der Loader kennt keinen `Dimension2D`-Zweig, ein `contentSize=…` in einer
  properties-Datei würde still ignoriert. Steht als Kommentar in `SkinProperties`; Parser erweitern,
  falls je gebraucht.
- **Doppelte Innenhöhen in `DashboardTile`.** Die Kachel setzt `TOP_HEIGHT = 300` und
  `BOTTOM_HEIGHT = 100` programmatisch auf ihre beiden Hälften — und dasselbe passiert nochmal im
  CSS (`addDashboardStyles`: `.dashboard-tile-top` mit `dashBoardTileTopHeight` = 250,
  `.dashboard-tile-bottom` mit 100). Zwei Quellen für dieselbe Größe, und die Zahlen widersprechen
  sich (300 vs. 250). Vermutlich gewinnt das CSS — dann sind die Konstanten tot, so wie es
  `TILE_WIDTH`/`TILE_HEIGHT` schon waren (die sind beim Umzug rausgeflogen, weil der Skin sie
  unmittelbar nach dem Konstruktor überschrieben hat). Nachmessen, dann eine der beiden Quellen
  streichen. → `DashboardTile` (TODO in der Klasse)

---

## C · Eigene Runden

### Punkt-Notation in den properties

`learnSessionPanel.world.map=…` → `Map<String, LearnSessionPanel>`. Regel: *ein Punkt geht eine Ebene
tiefer; ob eine Ebene offen oder deklariert ist, sagt der Typ.* Gedeckelt auf zwei Ebenen.

**Dafür:** 96 Felddeklarationen weg; ein neues Deck kostet keine Java-Änderung mehr (heute 3 Felder
pro Länder-Deck); hannover schrumpft von 16 Zeilen auf 1; FailFast fällt ab.
**Dagegen:** ~30 Zeilen Typ-Ablaufen im Loader; Migration aller sieben properties-Dateien, einmalig
und unumkehrbar.
**Warum vertagbar:** wird durch das Warten *billiger*, nicht teurer — nach dem Umbau ist es eine
Änderung innerhalb einer einzigen Klasse.

### Dialog-Vertrag vereinheitlichen

- `WhatsAppChatDialog` wertet einen rohen `javafx.ButtonType` aus, während `Alerts` ein `ButtonEnum`
  liefert. Entweder das Ergebnis beim Klick mitschreiben, oder `SuiteDialog` eine Variante geben, die
  wie `showAlert` ein `ButtonEnum` zurückgibt. → `WhatsAppChatDialog:95`
- Einige Dialoge geben noch `Optional` statt record/`null` zurück (widerspricht Regel 5 des
  Regelwerks).

### `ImageBatchProcessor` ist nicht nur UI

Die Klasse enthält neben dem Auswahl-Dialog auch Datei-Auflistung (`Files.list`), Bildskalierung
(AWT/Scalr) und Schreiben/Verschieben. → `ImageBatchProcessor:33`

### Das Tinting im Alert

Wird einem Alert ein Bild mitgegeben, wird es mit der **Textfarbe** eingefärbt. Funktioniert nur für
einfarbige Icons; ein Foto käme monochrom heraus. Unintuitiv und im Grunde ein Hack. Bisher gibt es
genau einen Aufrufer (`mattress.TurnDialog`) — überdenken, sobald ein zweiter dazukommt.
→ `Alerts:137`

### Kollaps der sieben Skin-Stummelklassen

Keine deklariert ein Feld, keine setzt einen Wert — sie tragen nur einen Anzeigenamen und eine
properties-Datei. Ließe sich zu einer Klasse plus Tabelle zusammenziehen. **Dagegen:** sie sind der
Erweiterungspunkt für einen Skin, der irgendwann in Java etwas berechnen oder eine
`addXxxStyles`-Methode überschreiben will.

---

## D · Gestaltung — bewusst erst nach dem Umbau

Diese Punkte sind Design-Entscheidungen, keine Struktur. Sie werden leichter, sobald die
CSS-Erzeugung eine eigene, überschaubare Klasse ist und man sie einzeln anfassen kann.

| Punkt | Fundstelle |
|---|---|
| `borderColor` global vs. in `borderParams` — eins von beidem muss raus | `SkinProperties:41` |
| Moderneres Design ausprobieren: runde Ecken, kein Border, box-shadow | `SkinProperties:42` |
| Einfacher als box-shadow: transparente Hintergründe ohne Border | `SkinProperties:43` |
| Padding von ScrollPane + ButtonBar ergibt einen unschönen Abstand; darunter ein „fieser Hack" | `Skin:338`, `Skin:372` |
| Default für `menuBarBackground` fehlt | `Skin:794` |
| Hartes `50px`-Padding gehört in die properties | `Skin:883` |
| Magic Numbers | `Skin:1028` |
| Highlight-Background ignoriert Border-Radius — JavaFX clippt Children nicht an runden Ecken | `Skin:1029` |
| Drei `// TODO: anpassen` an Farb-/Padding-/Font-Werten | `Skin:1192–1194` |
| Thumbnail (vergrößerte Version im Popup) sollte eine eigene Komponente werden | `Skin:1785` |
| `-fx-background-insets`-Fix analog `ImageMapPane` fehlt | `SuiteImage:52` |
| **DarkMode ist sauhässlich** | — |

---

## E · Beobachtungen ohne Entscheidung

Aufgefallen, aber nichts beschlossen — hier steht bewusst kein Auftrag.

- **`createIconButton` hat keinen Fallback** und keinen Null-Check. Kein Fehler: Icon-Buttons gibt es
  nur bei `germany`, `mc`, `world`, und dort sind die Felder definiert. Aber nichts erzwingt das.
- **Die rohen `new Alert(...)` in `ThosSuiteApp` und `shared.DB` sind ungestylt**, weil sie keinen
  Owner setzen (siehe Plan §6). Beim Start ist das teils unvermeidbar — die Hauptscene existiert noch
  nicht. Ob man die späteren Fälle nachziehen will, ist offen.
- **`DiaryTagInputComponent` generischer machen** und von Movie und WhatsApp mitnutzen lassen?
  → `DiaryTagInputComponent:23`
- **`SuiteSuggestionTextField` wird nur von Movie genutzt** — anderswo wiederverwendbar?
  → `SuiteSuggestionTextField:27`
- **Guard in `ImageMapPane`** 1:1 aus der alten GeoMap-Fassung übernommen, greift real fast nie.
  → `ImageMapPane:260`
- **JDK-Workaround** für JDK-8350149 / JDK-8362873 (HBox-Höhenberechnung) — kann weg, sobald gefixt.
  → `Skin:2029`
- **Menu-Styles:** nicht überprüft, ob alle nötig sind. → `Skin:816`
- **Preload ohne Fälligkeitsfilter:** `AnkiDeckService` wärmt *alle* Anki-Decks mit Karte vor, der
  Kommentar daneben spricht von „Decks die heute fällig sind". Ausdrücklich als unwichtig eingestuft.

---

## Nicht vertagt, sondern verworfen

Damit sie nicht als frische Idee wiederkommen — die vollständige Liste steht in
`Skin-Refactoring-Plan.md` §9 und `Skin-Zielbild-Entscheidung.md`. Die wichtigsten:

- **U1** (der Skin baut) — die ernsthafte Alternative zum gewählten Zielbild.
- **javafx komplett aus `controller`** — machbar, bewusst nicht gemacht.
- **`shared.ui.<feature>`-Zweige** — ersetzt durch Oberfläche vs. Baustein.
- **Eine fünfte Sprosse `shared.components`** — löste ein Scheinproblem.
- **Split der properties-Dateien** — eine Datei pro Design.

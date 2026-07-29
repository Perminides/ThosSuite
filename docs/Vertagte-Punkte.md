# Vertagte Punkte

**Stand:** 29.07.2026 · Schritte 1–4 erledigt, alle Wächter scharf im Build. Offen: Schritt 5.

Alles, was während des Refactorings (26.–29.07.) bewusst **nicht** sofort gemacht wurde — und
alles, was dabei aufgefallen ist.

**Abschnitt A ist die Arbeitsliste**, sortiert nach Art und in der Reihenfolge, in der es dran ist:
erst der Code, der sich noch ändert (A1), dann das Regelwerk (A2). Die Sachfehler in A3 warten auf
nichts davon. B bis E sind Vorrat, nicht Plan.

Was **erledigt** ist, steht nicht hier, sondern in `Skin-Refactoring-Plan.md` §5.

---

## A · Was noch offen ist

### A1 · Vor dem Regelwerk — Code, der sich noch ändert

Diese Punkte kommen **vor** Schritt 5. Das Regelwerk beschreibt einen Zustand; wenn danach noch
`Session` zu `Learn` wird und `UiComponent` verschwindet, schreiben wir es zweimal.

**1. `Session` → `Learn` durchbenennen.** Beschlossen, nicht gemacht. `SessionMap` ist nicht
spezifisch genug — „Session" könnte irgendwann auch ein kleines Spiel haben, gemeint ist Lernen.

```
SessionMap        → LearnMap
ShapeMapPane        bleibt (heißt nach der Bauart, nicht nach dem Zweck)
NoSessionMap      → EmptyLearnMap     („NoLearnMap" läse sich wie eine Verneinung des Lernens)
SessionComponent  → LearnComponent
SessionCallbacks  → LearnCallbacks
sessionBounds(…)  → learnBounds(…)
AnkiSessionView   → AnkiLearnView
RegionSessionView → RegionLearnView
```

Nicht umbenennen: die paketprivaten Klassen in `learn.anki` und `learn.region` — dort ist „Learn"
im Paketnamen und wäre doppelt gemoppelt. **Halb umbenannt wäre schlechter als gar nicht.**

**2. `MainWindow` rund machen.** Drei Einwände aus der 3d-Runde:

*a) Eine fertige `MenuBar` über die shared-Grenze zu reichen, ist zu viel.* Erwartbarer wäre:
`MainWindow` bekommt eine leere Leiste und hängt seine Menüs selbst ein. Zu bedenken: das Symbol
sitzt *neben* der Menüleiste im selben `leading`-Block. Entweder gibt die Leiste den Block heraus
(dann wandert Layout-Wissen nach draußen), oder sie bekommt ein `setMenuBar(MenuBar)` — dann
überquert der Typ die Grenze nur später statt gar nicht. Die Frage ist also nicht *ob* der Typ die
Grenze überquert, sondern ob `MainWindow` beim Zusammenbau mitwirkt. Vielleicht ist die ehrlichere
Fassung: die Leiste nimmt die *Menüs* (`List<Menu>`) und baut die `MenuBar` selbst.

*b) Kein `getStyleClass()` außerhalb von `shared`.* Heute drei Stellen, alle in `MainWindow`:

```
MainWindow:82   root.getStyleClass().add("my-root")
MainWindow:229  spacer.getStyleClass().add("my-spacer")
MainWindow:234  spacer2.getStyleClass().add("my-spacer")
```

Anzeige-Entscheidung außerhalb der Anzeige-Schicht. Wäre als fünfte ArchUnit-Regel prüfbar — erst
aufstellen, wenn die drei Stellen weg sind.

*c) `my-title` entscheiden.* Die Klasse wird zweimal gesetzt (`MainWindowHeaderBar:41`,
`SuiteHeaderBar:31`), es gibt aber **keine CSS-Regel dafür** — dasselbe Fossil wie
`custom-text-label` beim InfoLabel. Entweder beide Titel bekommen eine Regel, oder die Klasse fällt
an beiden Stellen weg. Marker steht im Code.

Dazu gehört ein zweiter Punkt, der dieselbe Klasse betrifft: `SuiteHeaderBar` bekommt die `font`
**ausschließlich** für `font.getSize() * 0.3` — ein Padding. Wandert das in eine CSS-Regel
`.my-title` (in `addDialogStyles`), entfällt der Konstruktor-Parameter **und** die `font` im
`DialogStyle`. Beide Fragen zusammen entscheiden: gibt es eine `.my-title`-Regel, dann trägt sie
auch dieses Padding; gibt es keine, muss der Wert anders untergebracht werden.
→ `SuiteHeaderBar:22`

**3. `UiComponent` abschaffen.** Alle Bausteine sind inzwischen selbst Nodes und implementieren den
Kontrakt nur noch mit `getView() { return this; }`. Die einzige Ausnahme ist `NoSessionMap`, das
Nullobjekt: es hält eine leere `Group`. Wird auch das ein Node (etwa indem es selbst von `Group`
erbt), kann `ComponentHost` auf `Node...` umstellen und `UiComponent` samt `getView()` entfallen.

**4. `SuiteDatePicker` entscheiden.** Neun Zeilen, trägt keinen Skin-Wert, nur die Festlegung „keine
Kalenderwochen". Überlebt allein über das Argument „Ort einer suite-weiten Entscheidung", nicht über
das Fassaden-Argument. Dünnster Fall der Familie, legitim zu streichen.

Hängt zusammen mit der **Gestaltungsfrage Kalenderwochen**: `DiaryEditor:174` macht als einzige
Stelle `new DatePicker(...)` und greift damit den JavaFX-Default ab (mit Kalenderwochen), die drei
anderen gehen über `SuiteDatePicker` (ohne). Der Unterschied ist **geerbt, nicht gewollt.** Zu
entscheiden: nirgends, überall, oder je nach Kontext als Parameter — in jedem Fall sollte auch der
Editor denselben Weg gehen wie die anderen.

**5. `#QuestionLabel` durch eine Modifikator-Klasse ersetzen.** Jetzt, wo `SuiteInfoLabel`
wiederverwendbar ist, ist die Kennung die falsche Mechanik — IDs sollen in einer Szene eindeutig
sein, zwei Info-Labels in einem Dialog brächen das. Sauberer: `.my-info-label.question` statt
`#QuestionLabel`. Betrifft `addSessionInfoLabelStyles` und die zwei `setId(…)`-Aufrufe in den Views.

**6. Doppelte Innenhöhen in `DashboardTile`.** Die Kachel setzt `TOP_HEIGHT = 300` und
`BOTTOM_HEIGHT = 100` programmatisch auf ihre beiden Hälften — und dasselbe passiert nochmal im CSS
(`addDashboardStyles`: `.dashboard-tile-top` mit `dashBoardTileTopHeight` = 250,
`.dashboard-tile-bottom` mit 100). **Zwei Quellen für dieselbe Größe, und die Zahlen widersprechen
sich** (300 gegen 250). Vermutlich gewinnt das CSS — dann sind die Konstanten tot, so wie es
`TILE_WIDTH`/`TILE_HEIGHT` schon waren. Nachmessen, dann eine der beiden Quellen streichen.
Kein Aufräumen, sondern ein latenter Fehler. → `DashboardTile` (TODO in der Klasse)

**7. `overlayContentBounds` — gehört das in den Skin?** Es beschreibt, wo der Inhalt im
Mini-Map-Bild sitzt. Das sind Karten-/Asset-Daten, kein Styling; es liegt nur deshalb beim Skin,
weil es hartcodiert ist. Sobald berechenbar (Overlay-Größe + prozentualer Rand), wandert es hoch zur
Karte. → `SkinProperties.getOverlayContentBounds`

---

### A2 · Schritt 5 — Regelwerk und Architekturdokument

**`Design-Regeln.md`** — neu aufzunehmen:

- **Die Ordnung** aus Plan §1: die vier Sprossen, die Regel „nur nach unten", der Discriminator
  „wird es gezeigt oder verbaut?"
- **Der Verantwortungsrahmen** Feature / View / Skin (Plan §3.14), in Thorstens Formulierung:
  *„Stell eine Frage" sagt das Feature. „Zeige auf dem Fragepanel diesen Text" passiert in
  `shared.ui`.*
- **Die Schlüssel-Regel** (Plan §3.15):

  > **Ein Baustein holt sich beim Skin, was für jede Verwendung gleich ist. Was von der Verwendung
  > abhängt, bekommt er übergeben.**
  >
  > Der Test steht in der Signatur des Skin-Zugangs: **braucht er ein Argument vom Aufrufer, dann
  > löst der Aufrufer auf und reicht das Ergebnis weiter.** Ein Argument *ist* der Kontext.

  ```java
  bigComponentStyle()                      // kein Argument      → Baustein holt selbst
  iconFor(rolle)                           // Rolle, kein Kontext → Baustein holt selbst
  sessionBounds(mapName, kategorie, teil)  // braucht Schlüssel   → Aufrufer löst auf
  ```

  **Was die Regel nicht sagt:** ob ein Baustein an ein Feature gebunden ist. Das ist eine
  Namensfrage, kein Konstruktionsprinzip. `MovieCard` darf sich `moviePosterWidth` holen (kein
  Schlüssel) und bekommt den Film übergeben (Kontext) — und bleibt trotzdem eine Film-Komponente.

  **Nebenregel, schon Praxis:** was ein Baustein sich holt, kommt bevorzugt als zweckgeschnittenes
  Record (`DialogStyle`, `McMetrics`, `BigComponentStyle`), nicht als Feld-Getter — damit keine
  Property-Namen das skin-Paket verlassen.
- **Bausteine erben, sie verhüllen nicht** (Plan §3.16) — mit der Ausnahme, die `SuiteBackground`
  erzwingt: ist der javafx-Typ `final`, bleibt nur eine Fabrik.
- **Bausteine heißen nach dem, was sie sind** (Plan §3.17). Prüfsatz: *was, wenn ein zweites Deck
  derselben Bauart dazukäme?*
- **Keine Streams, außer sie sind unbedingt nötig.** Bisher nirgends geschrieben, aber zweimal im
  Review beanstandet (`DashboardScreenView`, `ShapeMapPane`).
- **Dialog-Stufe 2a** fehlt: der parametrisierte Standarddialog (Primitive rein, Primitive oder
  `null` raus, kein Feature-seitiges Objekt). `TextPromptDialog`, `WhatsAppChatDialog`,
  `WhatsAppContactDialog` sehen nur deshalb wie Verstöße aus.
- **Der Karten-Absatz** — das Konstrukt versteht man beim Draufschauen nicht:

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
- **Wie mit Hintergrundbildern umgegangen wird** → `ComponentHost:33`
- **Der Skin-Vertrag** neu gefasst — der bestehende Abschnitt ist als „vorläufig" markiert.
- **Die vier Wächter als bewachte Zusagen** — sie sind keine Vorsätze mehr, sie brechen den Build.

**`Architektur-Dokumentation.md`**, Abschnitt **„UI-Architektur: Skin-System"** ist vollständig
überholt. Er beschreibt Factory-Methoden, das Fallback-System und die `SkinService`-API als „Stand
heute korrekt" und trägt bereits den Vermerk *„Refactoring ausstehend"*. Neu zu fassen:

- **Konzept** — der Skin baut nichts mehr, er liefert Werte und erzeugt das CSS
- **Factory-Methoden** — der ganze Abschnitt entfällt
- **Fallback-System** — bleibt inhaltlich, wandert aber hinter `sessionBounds(…)`
- **SkinService** — nur noch Registry; `setOwnerWindow`/`getOwnerWindow` liegen in `UiUtils`
- **Paket-Struktur** — `shared.ui` / `shared.ui.components` / `.map`, die vier Sprossen, die Wächter
- **learn** ist gesondert beschrieben und muss nachgezogen werden: die Sessions gehen über einen
  Presenter statt direkt über eine View. Das ist die eine begründete Abweichung im Screen-Aufbau.

**Zuletzt:** `docs/Ordnerstruktur.txt` und `docs/Paketabhängigkeiten.dot` neu erzeugen. Bewusst
nicht zwischendurch — sie veralten bei jedem Move.

---

### A3 · Sachfehler — warten nicht auf den Umbau

| Fehler | Fundstelle |
|---|---|
| **Freies Spiel:** falsch geklickte Formen verschwinden bei „schwer" nicht, nur die richtigen | `RegionSessionView:16` |
| **EM 2021 — „alle Länder grün, welches war falsch?"** Das Zurücksetzen vor `markLastClickAsIncorrect()` löscht die bisherigen Markierungen mit | `ImageMapPane.markIncorrect` |
| **`MovieViewerScreen.refresh()` baut nicht vollständig neu** — setzt nur den Hintergrund. Ein Skin, der Positionen ändert, greift so nicht. Offen: nur-Hintergrund beibehalten oder voller Rebuild (verwürfe die laufende Suche) | `MovieViewerScreen.refresh` |
| Fragefeld der Region-Session: „Hier wäre allerdings CENTER schon angesagt" | `RegionSessionView:69` |

---

## B · Klein, jederzeit machbar

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
- **Der MovieScreen wird komplett anders aufgebaut als die übrigen.** → `SuiteSuggestionTextField:30`
- **Alc- und Fitbit-StatisticScreens sind voll mit UI-Kram.** → `Skin` (Klassen-Javadoc)
- **Die Vier-Sprossen-Ordnung** aus Plan §1.1 als `layeredArchitecture()` in den ArchUnit-Test
  aufnehmen. Heute prüfen die vier Regeln die drei Wächter und die Zyklenfreiheit, nicht die
  Reihenfolge der Sprossen.

---

## Nicht vertagt, sondern verworfen

Damit sie nicht als frische Idee wiederkommen — die vollständige Liste steht in
`Skin-Refactoring-Plan.md` §9 und `Skin-Zielbild-Entscheidung.md`. Die wichtigsten:

- **U1** (der Skin baut) — die ernsthafte Alternative zum gewählten Zielbild.
- **javafx komplett aus `controller`** — machbar, bewusst nicht gemacht.
- **`shared.ui.<feature>`-Zweige** — ersetzt durch Oberfläche vs. Baustein.
- **Eine fünfte Sprosse `shared.components`** — löste ein Scheinproblem.
- **Split der properties-Dateien** — eine Datei pro Design.

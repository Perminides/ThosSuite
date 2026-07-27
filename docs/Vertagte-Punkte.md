# Vertagte Punkte

**Stand:** 28.07.2026

Alles, was während der Refactoring-Besprechung (26.–28.07.) bewusst **nicht** sofort gemacht wurde.
Sortiert nach **Fälligkeit**, nicht nach Thema — beim Abarbeiten ist „was ist jetzt dran" die
nützlichere Frage.

Schrittnummern beziehen sich auf `Skin-Refactoring-Plan.md` §8.

---

## A · Fällig im laufenden Umbau

### Schritt 3b — Feature-Oberflächen (Diary, Movie, Dashboard, BarChart)

| Punkt | Fundstelle |
|---|---|
| „Das muss doch raus, oder? Wieso baut hier der Skin?" — löst sich, wenn `createMovieViewer` nach `shared.ui` wandert | `MovieViewerScreenView:66` |
| Der MovieScreen wird komplett anders aufgebaut als die übrigen | `SuiteSuggestionTextField:30` |
| Alc- und Fitbit-StatisticScreens sind voll mit UI-Kram | `Skin:124` |
| Code-Duplizierung in den `getBackgroundImage`-Methoden | `Skin:1350` |
| `empty` und `default` gehen durcheinander — „hier holt empty das default" | `Skin:1377` |

### Schritt 3c — Session-/Lern-Teile

| Punkt | Fundstelle |
|---|---|
| **Zuschnitt der Session-Panes.** Dass sie das Feature verlassen, ist entschieden; offen ist wie (eine Klasse pro Session-Art wie heute, oder anders) und wie viel im Feature bleibt (`SessionPresenter` bleibt) | Plan §4b |
| Das Feature holt sich ein javafx-`Image` für den Hintergrund und reicht es durch | `ComponentHost:28` |
| Wie mit Hintergrundbildern umgegangen wird, gehört ins Regelwerk | `ComponentHost:33` |
| **`buildShapeMapWrapper`-Konstrukt.** Der Block gehört in die `ShapeMapPane`; das einzige Hindernis war fehlender Zugriff auf Skin-Felder — unter U3 fällt es weg | `ShapeMapPane:65`, `Skin:1605` |
| `MultipleChoicePane.Metrics` — „streng genommen sickern skin.properties in eine UI-Komponente". Unter U3 ist das die **Blaupause**, nicht der Sündenfall. TODO umschreiben statt lösen | `Skin:1467` |
| `overlayContentBounds` beschreibt, wo der Inhalt im Mini-Map-Bild sitzt — gehört eigentlich woandershin | `Skin:1686` |
| `learn.region.SessionPresenter` importiert `ShapeMapPane.ShapeMapState` — ein verschachtelter Enum einer Komponente, direkt im Feature | — |

### Schritt 3d — Chrome/Menüs

| Punkt | Fundstelle |
|---|---|
| DatePicker: im Tagebuch mit Kalenderwochen, bei den Statistik-Screens ohne. Warum? | `Skin:1702` |

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
- **Verantwortungsrahmen** Feature / View / Skin (Plan §4b) eintragen.
- **Skin-Vertrag** neu fassen — der bestehende Abschnitt ist als „vorläufig" markiert.
- Die drei Wächter als bewachte Zusagen festschreiben.

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

### Punkt-Notation in den properties (Plan §4a)

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

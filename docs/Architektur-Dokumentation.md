# ThosSuite — Architektur-Dokumentation

**Stand:** 03.06.2026
**Charakter dieses Dokuments:** Die *Landkarte* der Suite — was es gibt, wo es liegt,
wie die Teile zusammenspielen. Beschreibend. Die *Regeln*, nach denen geschnitten und
abhängig gemacht wird (und warum), stehen separat in `ThosSuite_Design_Regeln.md`; dort
schlägt man nach, *bevor* man etwas Neues baut.

> **Strukturstand:** Die heutige Paketstruktur ist das Ergebnis eines Architektur-Durchgangs
> (Frühjahr 2026): Auflösung des alten `data`-Topfs, dezentrale Feature-Repositories,
> Zusammenführung des Lern-Kerns in `learn`, Skin als `shared.skin`. Begründungen dazu im
> Regel-Dokument.

---

## 📋 Projekt-Überblick

### Was ist ThosSuite?
Eine persönliche All-in-One-Desktop-Anwendung (JavaFX), die mehrere Lebensbereiche abdeckt.
Thorsten ist einziger Nutzer und einziger Entwickler.

**Lernen:**
- Karten-basiertes Lernen mit Spaced Repetition (Deutschland, Welt, Multiple Choice, Hannover)
- Region-Decks (Bundesländer/Regionen-Spiele mit drei Modi)
- Wochentagsberechnung

**Gesundheit & Tracking:**
- Fitbit-Integration (Schritte, Wochenpunkte, Streak)
- Alkohol-Tracker mit Kontostand-System
- Erinnerung zum Wenden der Matratze

**Film:**
- Import von Film-, Serien- und Episodenbewertungen aus dem TMDB-Account; typ-agnostischer
  Viewer (`MovieViewerScreen`), der alle drei Typen als einheitliche Kacheln rendert.

**Tagebuch & Nachrichten:**
- Tagebuch mit Bild-Anhängen; beim Start wird auf einen neuen Eintrag gedrängt, wenn der
  letzte zu lange her ist (`DiaryDialog`). Browsen und Durchsuchen über `DiaryViewerScreen`
  (Tag-basierter Query-Parser mit AND/OR/Klammern, Bild-Hover-Vorschau).
- Signal- und WhatsApp-Nachrichten werden inkrementell beim Start in die Suite-DB importiert.

**Organisation:**
- Dashboard mit Key-Metrics (Fitbit-Streak, Restschritte, Alkohol-Kontostand)

### Was wird wie gelernt?
**AnkiDecks** (Deutschland, Multiple Choice, Welt, Hannover): jeweils mehrere Tausend Karten,
jede mit eigenem Fortschritt (zuletzt beantwortet / Tage bis zur nächsten Fälligkeit). Täglich
werden die fälligen Karten gespielt, bis alle richtig beantwortet sind.
- **Deutschland:** ShapeMap (klickbare Karte aus Einzel-Shapes für Kreise/kreisfreie Städte).
  Gemischte Kartentypen (Bild zeigen → Kreis klicken, mehrere Kreise markieren, Namen tippen).
- **Multiple Choice:** rund 6000 reine MC-Karten, vollständig mit der Maus bedienbar.
- **Welt:** ImageMap (großes, verschiebbares Weltkarten-Bild mit nur einem sichtbaren
  Ausschnitt). Unsichtbare Overlay-Shapes; Treffer = richtig, Verfehlen = Shape wird gezeigt,
  Karte gilt als falsch, Fortschritt zurückgesetzt.
- **Hannover:** eigenes Anki-Deck in derselben Deck- und Session-Architektur.

**Region-Decks** haben *als Ganzes* einen Lernfortschritt und mehrere Modi (s. u.). Basis ist
eine ShapeMapPane (für Bundesländer dieselbe wie bei Deutschland). Kein Multiple Choice, keine
Bilder — nur ShapeMapPane plus TextInputField („Wie heißt der markierte Kreis?") oder
Aufforderung („Klicke auf Yorkshire"). Es geht ausschließlich um Regionen und ihre Hauptorte.

### Leitprinzipien
- Einfachheit vor Dogma. Der Code muss auch nach Monaten Pause schnell wieder verständlich
  sein (Kalt-Lese-Test). Im Zweifel die einfachere, leichter auffindbare Lösung.
- KISS, YAGNI, SOLID (v. a. Single Responsibility) — pragmatisch, nicht dogmatisch.
- **Keine Testbarkeit als Ziel.** FailFast: Exceptions werden nicht abgefangen. Tritt etwas
  Unerwartetes auf, fliegt sofort eine Exception; der einzige Nutzer fixt das Problem direkt.

---

## 🗝️ Technische Basis

### Framework & Tools
- **JavaFX 25**, **Java 25 LTS**
- **Null-Layout:** keine LayoutManager, feste Positionen (Desktop-App mit fester Auflösung;
  präzise Kontrolle wichtiger als Flexibilität). Positionen/Größen setzt der Skin.
- **Jackson** für JSON-Parsing (GeoJSON, TMDB, Fitbit)
- **SQLite** für strukturierte Daten

### Exception-Handling-Philosophie
RuntimeExceptions werden bis `ThosSuiteApp` durchgereicht und dort zentral über einen
`UncaughtExceptionHandler` behandelt (Alert mit Stacktrace → Prozessende). Weniger Boilerplate,
sofortige Rückmeldung über Fehler. Ob ein konkreter Laufzeitfehler fatal (globaler Handler) oder
lokal behebbar ist, ist eine Einzelfallentscheidung an der jeweiligen Stelle, keine pauschale
Architekturregel.

### Nebenläufigkeit
Keine Threads; alles läuft auf dem JavaFX Application Thread. Einzige Ausnahme: die
Startup-Initialisierung (Hintergrund-Thread für Config, Logging, Font-Loading) mit
Splash-Screen-Pattern.

### Logging
`java.util.logging` (JUL), keine externen Dependencies (sauber mit dem JPMS-Module-System).
- FileHandler ab INFO in `{dataFolder}/log/thossuite%u.log` (10 MB, 5 Rotationen)
- ConsoleHandler ab FINE in der Eclipse-Console
- `--debug`: FileHandler loggt auch FINE
- Harte Referenzen auf konfigurierte Logger (JUL nutzt sonst WeakReferences)

API (Klasse `Log` in `app.shared`):
```java
Log.debug(this, "...");      // FINE  – nur Console
Log.info(this, "...");       // INFO  – Console + File
Log.warn(this, "...");       // WARNING
Log.error(this, "...", ex);  // SEVERE + Stacktrace
// Auch mit Class<?> statt Object (für statische Methoden)
```

### Startup-Flow
```
main() → launch()
  → init()              // AppClock, GlobalExceptionHandler
  → start()
      → showSplashScreen()
      → getDataFolderFromArgs() oder showDirectoryChooser()
      → Background-Thread:
          → Config.init(), Log.initLog(), loadFonts()
          → Platform.runLater():
              → initializeMainWindow()
              → new Controller()
              → controller.runPreTasks()     // externe APIs, Splash sichtbar (z. B. Fitbit-Fetch)
              → mainWindow.show() (opacity=0 gegen White-Flash)
              → PauseTransition (CSS-Settle) → splashStage.close()
              → controller.runPostTasks()    // lokale Tasks nach sichtbarem MainWindow
              → primaryStage.setOpacity(1)
```
**PreTasks** laufen während des Splashscreens und sprechen externe Quellen an (Fitbit).
**PostTasks** laufen nach sichtbarem Hauptfenster und sind lokal: Fitbit-Review-Dialoge,
Alkohol-Tagesabfrage, Tagebuch-Prompt, Wochentagsberechnung, Matratzen-Erinnerung, die
inkrementellen Signal-/WhatsApp-Importe sowie der Film-Import.

### Datenbankzugriff & Connection-Management
Die `DB`-Klasse (`app.shared`) verwaltet Connections zu **zwei** SQLite-Datenbanken:
der Suite-DB und der separaten TMDB-DB.

1. **`getConnection()` — Singleton, AutoCommit true.** Für alle normalen Lese-/Schreibzugriffe
   auf die Suite-DB. Bleibt die gesamte Laufzeit offen und wird **nie** geschlossen. Lazy beim
   ersten Aufruf initialisiert.
2. **`getNewConnection()` — neue Connection ohne AutoCommit pro Aufruf.** Für Importe (Signal,
   WhatsApp) und performance-relevante Bulk-Writes (viele Spielstände einer Session). Bündelt
   viele Writes in einer Transaktion. Der Aufrufer schließt sie per try-with-resources und ist
   für `commit()`/`rollback()` verantwortlich.
3. **`getTmdbConnection()`** — Lesezugriff auf die separate TMDB-DB (Viewer-Abfragen).

**Kritische Regel — Statements und ResultSets immer schließen.** Ein offenes `ResultSet`/
`Statement` auf der Singleton-Connection verhindert spätere Commits auf `getNewConnection()`
(`SQLITE_BUSY`). Alle `Statement`/`ResultSet` zwingend per try-with-resources schließen — auch
beim reinen Lesen.
```java
Connection conn = DB.getConnection();
try (PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // ...
}
```

**Schattenkopie (tmpDB).** Beim Start kopiert `DB.init()` die Produktiv-DB in einen
konfigurierten Temp-Ordner (`tmpDB.folder`); alle Connections zeigen auf die Kopie. Beim
sauberen Beenden kopiert `DB.shutdown()` zurück. Dadurch löst Filen (Cloud-Sync) nur einmal pro
Session einen Upload aus statt bei jedem Save. Liegt beim Start bereits eine Temp-DB vor
(Absturzfall), bricht `init()` mit Exception ab — die Datei muss manuell geprüft werden.

### Code-Generierung
Code nur auf expliziten Zuruf generieren — hält Chats kurz und Diskussionen im Fokus.

---

## 📦 Paket-Struktur

Die Suite ist in **Sorten** von Paketen gegliedert (Details und Begründung im Regel-Dokument):
ein **Lern-Kern** (`learn`), viele **Peripherie-Features** (`alc`, `diary`, `fitbit`, `mattress`,
`messaging`, `movie`, `weekday`), ein **Fundament-Dach** (`shared` inkl. `shared.model` und
`shared.skin`), die **Orchestrierung** (`controller`) und abtrennbare Einmal-Klassen (`scripts`).

```
📁 app
--📄 ThosSuiteApp.java
--📁 alc
----📄 AlcStatisticsScreen.java
----📄 StartupService.java
----📁 model
------📄 DayEntry.java
------📄 RatioEntry.java
------📄 Status.java
----📁 repository
------📄 Repository.java
--📁 controller
----📄 Controller.java
----📄 DashboardScreen.java
----📄 MainWindow.java
----📄 SuiteExporter.java
----📁 model
------📄 PlayMenuItem.java
------📄 PlayMenuNode.java
--📁 diary
----📄 DiaryDialog.java
----📄 DiaryViewerScreen.java
----📁 model
------📄 Entry.java
----📁 repository
------📄 Repository.java
--📁 fitbit
----📄 ActivityTableDialog.java
----📄 ApiClient.java
----📄 DashboardService.java
----📄 DataFetcher.java
----📄 DataReviewService.java
----📄 FitbitStatisticsScreen.java
----📄 PointsCalculator.java
----📁 model
------📄 GoalHistoryEntry.java
------📄 WeekData.java
------📁 json
--------📄 Activity.java
--------📄 ActivityDaySummary.java
--------📄 ActivityLogList.java
--------📄 FitbitCredentials.java
--------📄 Food.java
--------📄 FoodDaySummary.java
--------📄 LoggedFood.java
--------📄 Parent.java
--------📄 Summary.java
----📁 repository
------📄 Repository.java
--📁 learn
----📄 ImageScaler.java
----📄 MapService.java
----📄 Progress.java
----📄 ShapeMapPane.java
----📁 anki
------📄 AnkiDeckService.java
------📄 AnkiDeckSession.java
------📄 AnkiPlayConfigDialog.java
------📄 CardProgress.java
------📄 CardSortService.java
------📄 GermanySessionPane.java
------📄 ImageMapPane.java
------📄 ImageMapSessionPane.java
------📄 MCSessionPane.java
------📄 SessionPresenter.java
------📄 SessionProgress.java
------📁 model
--------📄 AnkiLearnSessionInfo.java
--------📄 Card.java
--------📄 DeckSessionPane.java
--------📄 MultipleChoiceAnswers.java
--------📄 SessionPane.java
------📁 repository
--------📄 CsvDeckCardSource.java
--------📄 DbDeckProgressSource.java
--------📄 DeckRepository.java
--------📄 PlayedCardData.java
----📁 model
------📄 Deck.java
------📄 DeckCategory.java
------📄 GeoMap.java
------📄 LearnSessionInfo.java
------📄 LearnStat.java
------📄 MapElementListener.java
------📄 MapMetadata.java
------📄 MapType.java
------📄 SessionProgressCounter.java
------📄 ShapeMap.java
----📁 region
------📄 ClickSessionProgress.java
------📄 EliminationSessionProgress.java
------📄 RegionDeckService.java
------📄 RegionPlayConfigDialog.java
------📄 RegionSession.java
------📄 SessionPane.java
------📄 SessionPresenter.java
------📄 SessionProgress.java
------📄 WriteSessionProgress.java
------📁 model
--------📄 Mode.java
--------📄 RegionLearnSessionInfo.java
--------📄 SessionSpec.java
------📁 repository
--------📄 DbRegionDeckProgressSource.java
--------📄 RegionDeckRepository.java
----📁 repository
------📄 GeoJsonLoader.java
------📄 MapRepository.java
--📁 mattress
----📄 TurnDialog.java
----📁 model
------📄 MattressTurn.java
----📁 repository
------📄 MattressRepository.java
--📁 messaging
----📁 repository
------📄 MessageRepository.java
----📁 signal
------📄 SignalIncrementalImport.java
------📁 model
--------📄 AttachmentInfo.java
--------📄 ContactInfo.java
--------📄 ConversationInfo.java
------📁 repository
--------📄 SignalSourceRepository.java
----📁 whatsapp
------📄 WhatsAppChatDialog.java
------📄 WhatsAppContactDialog.java
------📄 WhatsAppCrypt15Decryptor.java
------📄 WhatsAppIncrementalImport.java
------📁 repository
--------📄 WhatsAppSourceRepository.java
--📁 movie
----📄 ApiClient.java
----📄 Cleanup.java
----📄 Importer.java
----📄 MovieViewerScreen.java
----📄 SeriesImporter.java
----📁 model
------📄 CrewPendingEntry.java
------📄 EpisodeForApi.java
------📄 NullCommentEntry.java
------📄 TvShowComparisonData.java
------📁 json
--------📄 AccountRatingJSON.java
--------📄 BelongsToCollectionJSON.java
--------📄 CastJSON.java
--------📄 CreatedByJSON.java
--------📄 CreditListJSON.java
--------📄 CrewJSON.java
--------📄 EpisodeJSON.java
--------📄 EpisodeRatingJSON.java
--------📄 EpisodeRatingsPageJSON.java
--------📄 EpisodeToAirJSON.java
--------📄 GenreJSON.java
--------📄 JobJSON.java
--------📄 MovieJSON.java
--------📄 MovieRatingJSON.java
--------📄 MovieRatingsPageJSON.java
--------📄 NetworkJSON.java
--------📄 PersonJSON.java
--------📄 ProductionCompanyJSON.java
--------📄 ProductionCountryJSON.java
--------📄 RoleJSON.java
--------📄 SeasonJSON.java
--------📄 SomethingWithAPosterJSON.java
--------📄 SpokenLanguageJSON.java
--------📄 TvShowJSON.java
--------📄 TvShowRatingJSON.java
--------📄 TvShowRatingsPageJSON.java
----📁 repository
------📄 CardDataFactory.java
------📄 CrewFilterRepository.java
------📄 EpisodeRepository.java
------📄 MovieRepository.java
------📄 PendingRepository.java
------📄 SeasonRepository.java
------📄 TmdbViewerRepository.java
------📄 TvShowRepository.java
--📁 scripts
----📄 CopyJavaAsTxt.java
----📄 FolderStructure.java
----📄 MemoryTestHelloWorld.java
----📄 MergeJavaToTxt.java
----📄 PackageDependencyGraph.java
----📄 SkeletonStripper.java
----📁 alc
------📄 AlcoholChartPrototype.java
------📄 AlcoholCsvMigration.java
----📁 diary
------📄 DiaryBatchTaggerApp.java
------📄 DiaryEntryPrototype.java
------📄 DiaryMigration.java
----📁 fitbit
------📄 FitbitChartPrototype.java
------📄 FitbitLogMigrator.java
----📁 learn
------📄 CopyLandkreiseStats.java
----📁 messages
------📄 SignalInitialImport.java
------📄 WhatsAppInitialImport.java
------📄 WhatsAppInitialImportDryRun.java
----📁 tmdb
------📄 AggregatedCreditsProbe.java
------📄 EpisodeRatingReview.java
------📄 FilmViewerPoC.java
------📄 SeasonRegularBackfill.java
----📁 ui
------📄 ButtonBorderTest.java
------📄 ComboBoxTest.java
------📄 CssInspector.java
------📄 DatePickerLocaleTest.java
------📄 RoundedImageTest.java
------📄 ShadowDemo.java
------📄 TableViewDialogTest.java
------📄 TableViewExample.java
------📄 TooltipImageTest.java
------📄 TooltipWrapTest.java
------📄 WhiteFlashTest.java
----📁 weekday
------📄 WeekdayImporter.java
--📁 shared
----📄 AppClock.java
----📄 Config.java
----📄 DashboardTile.java
----📄 DB.java
----📄 FilenIgnoreSource.java
----📄 ImagePane.java
----📄 KeyValueRepository.java
----📄 Log.java
----📄 MultipleChoicePane.java
----📄 SaveSelectionListener.java
----📄 Screen.java
----📄 SessionInfoLabel.java
----📄 SingleInstanceGuard.java
----📄 SuggestionTextField.java
----📄 TabCommitTextFieldTableCell.java
----📄 TagInputComponent.java
----📄 ThrowingConsumer.java
----📄 UIUtils.java
----📁 model
------📄 BorderParams.java
------📄 CardData.java
------📄 CardSortOrder.java
------📄 SessionSwitchStrategy.java
----📁 skin
------📄 BaseColorSkin.java
------📄 BlueGradientSkin.java
------📄 DarkMode.java
------📄 FlatWebSkin.java
------📄 FlowerSkin.java
------📄 RedGradientSkin.java
------📄 Skin.java
------📄 SkinService.java
------📄 SpicySkin.java
--📁 weekday
----📄 WeekdayDialog.java
----📁 model
------📄 WeekdayStats.java
----📁 repository
------📄 WeekdayRepository.java

```

---

## ⚙️ Config-System
Zentrale Konfigurationsklasse (`app.shared.Config`) für alle Suite-Einstellungen (Skin,
Sort-Order, Fenster-Positionen …). Singleton mit statischen Methoden. Dirty-Check via
Map-Vergleich: speichert nur bei tatsächlicher Änderung.

**Zugriffsstrategie:** Jede Klasse holt sich selbst, was sie braucht. Der Controller reicht
*keine* Config-Werte durch (das wäre Micromanagement).

---

## 🎨 UI-Architektur: Skin-System

### Konzept
Ein Skin = ein komplettes visuelles Design. Der Skin (`app.shared.skin`) trifft alle
Styling-Entscheidungen (Farben, Fonts, Borders) und setzt Positionen/Größen auf die
UI-Komponenten. Die Komponenten kennen **keine** konkreten Farb-/Layout-Werte; der Skin setzt
sie via CSS bzw. von außen.

**Schichtlage:** `shared.skin` hängt nur nach unten an `shared` und `shared.model`. Features und
Lern-Kern hängen ihrerseits am Skin (abwärts). Der Skin kennt **keine** Domänentypen — die Domäne
reicht ihm dumme DTOs/Keys hinab. (Der Skin-Vertrag im Detail steht im Regel-Dokument.)

### Factory-Methoden
Der Skin baut die atomaren UI-Bausteine, u. a. Eingabefelder, Antwort-Buttons,
`MultipleChoicePane`, `DashboardTile`, DatePicker, gestylte Alerts/Dialoge. Für die Karten gilt:
der Skin liefert Layout/Wrapper, die fachlichen Panes halten ihren Zustand selbst.

### CSS-Generierung
```java
scene.getStylesheets().add("data:text/css," + encodedCss);
```

### Reflection-basiertes Property-Laden
Skin-Properties werden ohne Code-Änderung aus `.properties`-Dateien geladen. **Subskin-Vererbung:**
ein abgeleiteter Skin lädt erst die Eltern-Config, dann die eigene.

### Fallback-System für Layouts
Region-Decks sollen ein gemeinsames Layout teilen:
```
1. spezifisches Layout suchen:  "spainSessionQuestionPanel"
2. nicht gefunden → Fallback:   "geo_deckSessionQuestionPanel"
3. alle Region-Decks teilen den Fallback
```

### SkinService — zentrale Registry
`getAllSkins()` (Liste aller Skins), `get()` (aktiver Skin), `set(Skin)` (aktivieren),
`refresh()` (neu laden, für Live-Bearbeitung), `setOwnerWindow(Stage)` (vor Dialogen nötig).

> Die `Skin`-Klasse ist derzeit noch groß (CSS-Erzeugung, Property-Laden, Layout-Bounds,
> Komponenten-Bau). Eine Aufteilung ist bewusst aufgeschoben (siehe Regel-Dokument).

---

## 🗺️ Map-System

### GeoMap — einheitlicher Map-Container (`learn.model`)
```java
private final Map<String, ShapeMap> shapes;
private final MapType type;
private final Image backgroundImage, overlayImage, inactiveBackgroundImage, inactiveOverlayImage;
```
**MapType:** `SHAPE` (klassische Shape-Maps — Shapes *sind* die Map) und `IMAGE` (navigierbare
Bild-Map wie Welt — Bild ist die Map, Shapes sind Overlay). `GeoMap` kann zur Laufzeit auch
`size|x|y`-Kürzel in einen passenden Kreis-Shape übersetzen.

### ShapeMap — einheitliches Shape-Objekt (`learn.model`)
```java
public record ShapeMap(String id, String deckId, String regionName, String capitalName,
    Set<String> altRegionNames, Set<String> altCapitalNames, String type,
    Node shape, boolean isShapeMap)
```
Enthält den JavaFX-`Node` und die Attribute aus der GeoJSON. Der Node trägt die CSS-Klasse
`map-shape` bzw. `image-map-shape`. `type` gibt an, ob das Shape spielbar ist (`"0"`) oder welche
Deko-CSS-Klasse greift (`"1"`, `"2"`, `"3"`).

### MapMetadata — alle verfügbaren Karten
```
GERMANY → germany.geojson + germany states.geojson (SHAPE)
ITALY · SPAIN · USA · CARIBBEAN · ENGLAND · BERLIN · SCHWEIZ · OZEANIEN · AUSTRIA · BAVARIA (SHAPE)
HANNOVER_STADTTEILE · HANNOVER_REGION (SHAPE)
WORLD → worldAreas.geojson + worldCountries.geojson + worldLines.geojson (IMAGE)
```

### GeoJSON-Properties — alle Daten in einer Datei
```json
{ "type": "Feature",
  "properties": { "id": "14522", "deckId": "lk_hs", "regionName": "Lahn-Dill-Kreis",
                  "capitalName": "Wetzlar", "altRegionNames": "Lahn-Dill, Lahn Dill",
                  "altCapitalNames": "", "type": 0 },
  "geometry": { ... } }
```

---

## 📚 Deck-System (Enum `Deck`)

### Anki-Decks
| Deck | ID | Karte |
|------|----|-------|
| GERMANY_CARDS | germany | Deutschland-Karte |
| MC_CARDS | mc | Multiple Choice |
| WORLD_CARDS | world | Weltkarte |
| HANNOVER_CARDS | hannover | Hannover-Karte |

### Region-Decks (Bundesländer)
Alle nutzen `MapMetadata.GERMANY` und `DeckCategory.REGION_DECK`:
`lk_sh, lk_bw, lk_bn, lk_bs, lk_bb, lk_hs, lk_mvp, lk_ns, lk_nrw, lk_rp, lk_sl, lk_sc, lk_sa, lk_th`

### Region-Decks (International)
SPANIEN (es) · ITALIEN (it) · USA (us) · CARIBBEAN (cs) · ENGLAND (en) · SCHWEIZ (ch) ·
OZEANIEN (oz) · AUSTRIA (au) · BAVARIA (br)

### Region-Decks (Hannover & Berlin)
| Deck | ID | Besonderheit |
|------|----|--------------|
| HANNOVER_STADTTEILE | hs | kein Kapital (`hasCapital=false`) |
| HANNOVER_REGION | hr | kein Kapital |
| BERLIN_WEST/NORD/MITTE/OST | be_we / be_no / be_mi / be_os | Teile von berlin.geojson |

Das `mapName`-Feld (`be` für alle Berlin-Decks) steuert, welche GeoJSON und welches
Skin-Layout genutzt werden.

---

## 🎮 Region-Decks

```
MapService → Set<ShapeMap>  →  RegionDeckService (cached)  →  Session arbeitet direkt mit ShapeMap
```

**3 Modi:** CLICK (auf Karte klicken) · ELIMINATION (alle Shapes durchgehen, bei Fehler Ende) ·
WRITE (Namen eingeben).
**2 Schwierigkeiten:** COLOURED (aktive Shapes farbig) · NO_COLOUR (alle grau).
**3 Fragetypen:** REGION · CAPITAL · BOTH. → 18 Kombinationen pro Deck (nur sinnvolle in DB).

**Session-Lifecycle:** Controller holt fällige ShapeMaps vom Service → erzeugt Session mit
ShapeMaps + SortOrder → registriert sich. Session-Ende: `save()` → `Service.saveSession()` →
Repository speichert + Cache-Update → `mainWindow.updateLearnItems()`.

**Resume (nur Click):** bei falschem Klick `savedState = pane.getState()`; der Nutzer kann
Resume statt Speichern wählen (`pane.setState(savedState)`).

---

## 📊 Statistik-Screens

Die drei Statistik-Oberflächen liegen jeweils in ihrem Feature bzw. der Orchestrierung und
implementieren `Screen` mit `SessionSwitchStrategy.IMMEDIATE`.

### DashboardScreen (`controller`)
`FlowPane` mit Kacheln (`SkinService.get().createDashboardTile()`): Restschritte des Tages
(via `fitbit.DashboardService`), aktueller Fitbit-Streak in Wochen (+ Rekord), aktueller
Alkohol-Kontostand.

### FitbitStatisticsScreen (`fitbit`)
BarChart + LineChart in einem `StackPane`. BarChart: Wochenpunkte pro Woche
(`:achieved`/`:failed`/`:in-progress`). LineChart (maus-transparent): Wochenziel als Linie.
Steuerung über DatePicker (Von/Bis) und Spinner (Balkenabstand).

### AlcStatisticsScreen (`alc`)
BarChart mit kumulativem Kontostand. X: Tage, Y: Kontostand. Balken `:achieved` (positiv) /
`:failed` (negativ). Tooltip: Datum | Kontostand | Grün:Gelb:Rot-Ratio. Balance startet beim
Kontostand *vor* dem gewählten Zeitraum, nicht bei 0.

---

## 📝 Tagebuch (`diary`)

- **`DiaryDialog`** — Eingabe von Tagebuchtext und Tags, Bild-Anhänge. Der Aufruf ist im
  `Controller` als Post-Startup-Task verankert; gedrängt wird, wenn der letzte Eintrag zu
  lange her ist.
- **`DiaryViewerScreen`** — Browsen und Durchsuchen der Einträge. Tag-basierter Query-Parser
  mit booleschem AND/OR und Klammern (in SQL übersetzt); Bild-Hover-Vorschau über `Popup`.
- Persistenz über `diary.repository.Repository`; Anhänge in eigener Tabelle, Thumbnails lazy.

---

## 📅 Wochentagsberechnung (`weekday`)
Eigenständiger Trainingsdialog (`WeekdayDialog`) für Datums-/Wochentagswissen: erzeugt eine
Datumsaufgabe, nimmt die Auswahl entgegen, wertet aus. Aufruf im `Controller` (an die tägliche
Nutzung gekoppelt). Persistenz über `weekday.repository.WeekdayRepository`.

---

## 🛏️ Matratzen-Erinnerung (`mattress`)
Wiederkehrender Erinnerungsdialog (`TurnDialog`) zum Wenden der Matratze: prüft im Rahmen der
Startlogik, ob fällig, zeigt den Dialog, protokolliert die Bestätigung
(`mattress.repository.MattressRepository`).

---

## 🍺 Alkohol-Tracker (`alc`)

Punktesystem mit zeitlich konfigurierbaren Ratios: **GREEN** (positiver Beitrag),
**YELLOW** (kleiner positiver Beitrag), **RED** (negativer Beitrag).

**Startup-Dialog (`alc.StartupService`):** in `runPostTasks()` aufgerufen; prüft auf fehlende
Tage seit dem letzten Eintrag bis gestern und fragt je fehlendem Tag „Wie war [Wochentag] der
[Datum]?" (Grün/Gelb/Rot/Abbrechen; bei Abbrechen kein Auto-Fill).

**DB-Schema:**
```sql
CREATE TABLE alcohol_days (
    date TEXT PRIMARY KEY,
    status TEXT  -- 'GREEN', 'YELLOW', 'RED'
);
CREATE TABLE alcohol_ratios (
    valid_from TEXT PRIMARY KEY,
    green_points INTEGER, yellow_points INTEGER, red_points INTEGER
);
```

**Balance** wird on-the-fly berechnet (nicht gespeichert): alle Ratios nach `valid_from` laden,
je Tag die gültige Ratio (`valid_from <= date`) finden, kumulativ aufaddieren (RED negativ).

---

## 💪 Fitbit-Integration (`fitbit`)

- **`DataFetcher` / `ApiClient`** — holen die Daten in `runPreTasks()` (blockiert im UI-Thread,
  Splash sichtbar).
- **`DataReviewService`** — zeigt in `runPostTasks()` die Review-/Bestätigungsdialoge und speichert.
- **`DashboardService`** — `calculateRemainingDailySteps(today)`, `calculateCurrentStreak(today)`
  (grüne Wochen ohne laufende Woche), `calculateRecordStreak()`.
- **`PointsCalculator`**, **`FitbitStatisticsScreen`** (Wochenpunkt-Diagramm),
  **`ActivityTableDialog`**.
- Domänen-Records in `fitbit.model` (`WeekData`, `GoalHistoryEntry`); API-DTOs in
  `fitbit.model.json`; DB-Zugriff in `fitbit.repository.Repository`.

**DB-Schema:**
```sql
CREATE TABLE fitbit_weeks        ( week_start TEXT PRIMARY KEY, points INTEGER );
CREATE TABLE fitbit_goal_history ( valid_from TEXT PRIMARY KEY, weekly_goal INTEGER );
CREATE TABLE fitbit_import_log   ( date TEXT PRIMARY KEY, steps INTEGER, ... );
```

---

## 🎬 Film / TMDB-Integration (`movie`)

- Bewertete Filme, Serien und Episoden werden im `MovieViewerScreen` als Kacheln dargestellt.
  Der Viewer ist **typ-agnostisch** — dieselbe Kachel rendert alle drei Typen. Datengrundlage ist
  das dumme DTO **`CardData`** (`shared.model`); die typ-spezifische Formatierung baut
  **`CardDataFactory`** (`movie.repository`).
- Die Suche läuft über SWYT-Felder (Director, Actor, Title); die Vorschlagslisten kommen aus
  UNION-Views (`all_directors`, `all_actors`, `all_titles`) über alle drei Typen.
- Filme werden täglich beim Start importiert (`movie.Importer`); Serien/Episoden manuell über
  einen Menübutton (`movie.SeriesImporter`). Importiert werden Zusammenfassungen, Schauspieler,
  Regisseure, eigene Bewertung; zu jeder neuen Bewertung wird ein Kommentar erfragt und gespeichert.
- Crew-Filterung über Whitelist/Blacklist (`crew_whitelist`, `crew_blacklist`). Bei Filmen landen
  unbekannte Jobs in Pending-Tabellen (`person_pending`, `crew_pending`) für die spätere
  Entscheidung im **`movie.Cleanup`**-PostTask; bei Serien/Episoden wird direkt beim Import gefragt.
- Import-Repositories in `movie.repository`: `MovieRepository`, `TvShowRepository`,
  `SeasonRepository`, `EpisodeRepository`, plus `TmdbViewerRepository` (Lese-Operationen für den
  Viewer). Ein regelmäßiger Lücken-Check holt fehlende Poster und Overviews nach.

---

## 💬 Messaging (`messaging`)

Gemeinsames `messaging.repository.MessageRepository` (Suite-DB, quellunabhängig); je Quelle ein
Unterpaket (`signal`, `whatsapp`) mit eigenem Fremdquellen-Repository.

### Signal-Import (`messaging.signal`)
Täglicher Inkrementalimport beim Start (PostTask) über `SignalIncrementalImport`; DB-Zugriff
auf die Signal-Quelle in `SignalSourceRepository`.

Ablauf: Caches laden (bekannte Chats/Kontakte/Blacklist) → letzten importierten Tag prüfen
(= gestern → nichts zu tun) → Integritätscheck → Import aller Nachrichten mit
`DATE(sent_at) > letzter Tag AND < heute` → Commit (bei Exception Rollback).

Gemeinsame Filterkette (Import *und* Integritätscheck): nur `type in (incoming, outgoing)`;
`isErased = 1` übersprungen; Blacklist-Chat übersprungen; ohne Body und ohne Attachments
übersprungen. Neuer Chat → blockierender Alert (importieren/blacklisten). Attachments:
AES/CBC-Entschlüsselung nach `[config:signal.attachmentDir]/<dateiname>`, in der DB nur der
Dateiname; fehlt die Quelldatei → FailFast. Eine Transaktion über den gesamten Import auf
`getNewConnection()`.

Wichtige Config-Keys: `signal.path`, `signal.key` (SQLCipher-Key, nie ins Git),
`signal.attachmentDir`.

### WhatsApp-Import (`messaging.whatsapp`)
Inkrementeller Import (`WhatsAppIncrementalImport`) nach konfigurierter Uhrzeit; SHA-256-Vergleich
der `crypt15`-Datei erkennt neue Backups; Entschlüsselung in `WhatsAppCrypt15Decryptor`;
Fremdquellen-DB-Zugriff in `WhatsAppSourceRepository`. Kontakt-/Chat-Dialoge:
`WhatsAppContactDialog`, `WhatsAppChatDialog`.

### DB-Schema (Messaging)
```
msg_contacts          — Personen (contact_id, display_name)
msg_contact_mapping   — serviceId → contact_id (source, raw_identifier, contact_id)
msg_chats             — Chats (chat_id, source, raw_identifier, is_group, display_name, blacklisted)
msg_chat_members      — Teilnehmer, lazy (chat_id, contact_id)
msg_messages          — Nachrichten (source, source_id, sent_at, from_contact, chat_id, content,
                        quote_message_id);  PRIMARY KEY (source, source_id)
msg_message_attachment— Anhänge (source, source_id, path, thumb_path, available)
```

---

## 🎮 Play Mode (Freies Spiel)
Neben dem regulären Lernmodus (fällige Items) gibt es einen Spielen-Modus **ohne**
Fortschritts-Speicherung.
- **`AnkiPlayConfigDialog`** (`learn.anki`): Kartenbereich (Min/Max-Index), max. Kartenzahl,
  Label-Filter; Session startet mit `isPlaySession=true` → kein Speichern.
- **`RegionPlayConfigDialog`** (`learn.region`): mehrere Decks kombinierbar; Modus CLICK/
  ELIMINATION/WRITE.
- **`PlayMenuItem` / `PlayMenuNode`** (`controller`):
  `record PlayMenuItem(String label, Object payload) implements PlayMenuNode {}` — `payload` ist
  ein `Deck` (Anki) oder `DeckCategory.REGION_DECK` (Region).

---

## 🔄 Session-Management

### Bildschirm-Kontrakt `Screen` (`app.shared`)
```java
public interface Screen {
    void start();
    void escClicked();
    default void endGracefully() {}
    void closeSilent(boolean save);
    SessionSwitchStrategy getSwitchStrategy();
    void refresh();
    Pane getView();
    default void endPause() {}
    default void sort(CardSortOrder order) {}
}
```
Ein Interface mit leeren Defaults für nicht zuständige Methoden — ein `AlcStatisticsScreen`
reagiert nicht auf `sort()`, und das ist **kein** Fehler (kein FailFast für Nicht-Zuständigkeit).
Nur Nicht-Lern-Oberflächen heißen `…Screen`; im Lern-Kern bleibt „Session" (DeckSession,
RegionSession …).

### SessionSwitchStrategy (`shared.model`)
```java
public enum SessionSwitchStrategy {
    IMMEDIATE,       // sofort wechseln (Dashboard, Statistiken, Play)
    OFFER_SAVE,      // Dialog "Speichern?" (Anki mit Fortschritt)
    CONFIRM_DISCARD  // Dialog "Fortschritt geht verloren!" (Region)
}
```

### Controller als Orchestrator
```java
private void requestSessionSwitch(Runnable startNewSessionRoutine) {
    switch (currentScreen.getSwitchStrategy()) {
        case IMMEDIATE       -> { currentScreen.closeSilent(false); startNewSessionRoutine.run(); }
        case OFFER_SAVE      -> { /* Dialog Speichern/Verwerfen/Abbrechen */ }
        case CONFIRM_DISCARD -> { /* Dialog Wirklich abbrechen? */ }
    }
}
```
Feature-Klassen und Lern-Sessions kennen den Controller **nicht**: das Ende einer Session wird
über ein `Runnable onSessionEnded` zurückgemeldet, das der Controller beim Erzeugen hereingibt.

---

## 📈 Spaced Repetition

`Progress` (`app.learn`) mit Default-Methode:
```java
default int calculateNewLevel(LocalDate lastPlayed, boolean answeredCorrectly, boolean wasOverdue)
```
**Level:** `newLevel = max(1, round(2 * gap * (1 + rand)))`, `rand ∈ [-0.1, 0.1]`,
`gap` = Tage zwischen `lastPlayed` und heute.
**Fälligkeit:** `nextDue = lastPlayed + level` — **nicht** in der DB gespeichert, bei Bedarf
berechnet (SQL oder Java).

---

## 🗄️ DB-Schemas (Region-Lernen)
```sql
CREATE TABLE region_learn_stat (
    deck TEXT, mode TEXT, first_played TEXT, last_played TEXT,
    level INTEGER, wrong_count INTEGER,
    PRIMARY KEY (deck, mode)
);
CREATE VIEW region_learn_stat_with_due AS
    SELECT *, date(last_played, '+' || level || ' days') AS due FROM region_learn_stat;
CREATE TABLE region_log (
    played_timestamp TEXT PRIMARY KEY, deck TEXT, mode TEXT,
    correct_flag INTEGER, wrong_region_id TEXT
);
```

---

## 🚀 Implementierter Funktionsumfang
- 4 AnkiDecks (Germany, World, Multiple Choice, Hannover), Spaced Repetition
- Region-Decks: alle drei Modi (Click, Elimination, Write), Easy/Hard, Resume bei Click,
  ShapeMapState für Skin-Wechsel; viele Decks (Bundesländer, Italien, USA, Karibik, England,
  Schweiz, Ozeanien, Österreich, Bayern, Hannover Stadt/Region, Berlin)
- Freies Spiel für Anki und Regionen
- CSS-basiertes Skinning, Skin-Wechsel zur Laufzeit
- Splash-Screen mit asynchroner Initialisierung
- Fitbit-Integration (Fetch, Review, Wochenpunkt-Diagramm, Dashboard-Metriken)
- Alkohol-Tracker (Startup-Abfrage, Kontostand, Diagramm)
- Tagebuch (Eingabe + Viewer mit Tag-Suche und Bild-Vorschau)
- Wochentagsberechnung, Matratzen-Erinnerung
- Signal- und WhatsApp-Inkrementalimport
- Film/TMDB-Viewer (typ-agnostisch: Filme, Serien, Episoden)

## 📋 Geplant / Offene Punkte
**Lernen:** automatische Endbedingungen für Anki-Sessions · graphische Fortschrittsanzeige
innerhalb einer Session · Fast Hints (Tippen mit Zeitlimit) · weitere Region-Decks (z. B.
Frankreich) · Preloading-Strategie für Karten/Maps.

**Statistiken:** schwerste Regionen, Performance-Trends · Dashboard als Home-Screen statt leerem
Hintergrund beim Start.

**Film/TMDB:** Anzeige-Varianten für Serien/Staffeln/Episoden im Viewer · manueller
TMDB-Vollabgleich mit Dashboard-Kachel („Zeit seit letztem Lauf"; Datum im Key-Value-Table) ·
Refactoring der veralteten Episoden-Flag-Struktur (`rated_season`, `directors_from_show`,
`actors_from_show`).

**Messaging:** monatlicher Konsistenz-Check (am 5., für den Vormonat, nur bei vollständiger
Vormonats-Abdeckung; Status-Anzeige statt FailFast) · WhatsApp-Quote-Unterstützung
(`quote_message_id` via Join `message_quoted.key_id → message._id`).

**Lern-Kern (eigene Design-Session):** das Quadrumvirat AnkiDeckSession / SessionPane /
SessionPresenter / CardProgress und die Richtung der Progress→Session/Presenter-Kopplung
(inkl. der Frage nach dem `transient`-Feld in `Card`).

1 Alert in der Persistenz. Im catch von DbDeckProgressSource.saveLearned steht new Alert(...) — UI in der Persistenzschicht. In sechs Monaten täglichem Gebrauch nie geflogen, aber fachlich am falschen Ort. Bewusst wörtlich stehen gelassen, du wolltest ein // !TODO setzen.

2 getSwitchStrategy ohne active-Guard. In der Schale liest die Methode progress.hasProgressed() + isFreePlay, prüft aber selbst kein active. Sie wird nur vor dem Schließen gerufen, also aktuell harmlos — aber sie ist die einzige Screen-Methode, die auf einer toten Session nicht knallt (alle anderen delegieren in geguardete Progress-Methoden). Entscheiden, ob das so bleibt oder ob du dort auch hart sein willst.

3 FailFast-Guard in AnkiDeckService.savePlayedCards (optional). Der Lookup dueCards.get(type).get(row.cardId()) läuft bewusst in eine nackte NPE, falls die Invariante „gespielte Karte ∈ dueCards" je bricht. Offen, ob du die NPE durch ein explizites [FAILFAST] mit Klartext ersetzen willst — reine Lesbarkeit im Fehlerfall, kein Verhaltensunterschied im Normalbetrieb.

4 Deck-Display-Name als Primärschlüssel. In card_learn_stat/card_log hängt der Schlüssel an Deck.getDisplayName() (String). Benennst du den Anzeigetext eines Decks mal um, verwaisen alle historischen Zeilen. Das ist eine bestehende Eigenschaft deiner DB, nicht durch dieses Refactoring eingeführt — ich habe sie bewusst nicht angefasst, aber sie ist erkannt.

5 Gemeinsame Abstraktion AnkiSessionProgress ↔ WriteSessionProgress. Ob beide am Ende ein gemeinsames SessionProgress-Interface teilen (region hat es bereits), haben wir explizit als größere Designfrage aufgeschoben.

6 Naming-Bruch in learn.anki.repository (dein neuer Punkt). In den übrigen Domänen heißt die SQL-ausführende Klasse ...Repository. Hier heißt der SQL-Ausführer ...Source (DbDeckProgressSource, CsvDeckCardSource), und DeckRepository ist der vorgelagerte Wrapper über beide. „Repository" bedeutet hier also genau das Gegenteil wie sonst. Der Kern der Vereinheitlichung ist die Entscheidung, welche Klasse den Namen Repository verdient: der SQL-Ausführer (dann wird DbDeckProgressSource zum ...Repository und der Wrapper braucht einen neuen Namen, denn er führt kein SQL aus) — oder du behältst den Wrapper als Repository und benennst dafür anderswo um. Eigene kleine Runde wert, weil der Wrapper zwei Quellen (CSV + DB) bündelt und die saubere Benennung davon abhängt, wie du diese Doppelrolle siehst.

7 Menü „Speichern" wird bei Statistik-Screens nicht ausgeblendet (anders als Region) — prüfen ob Bug. Marker gehört in die Menü-Sichtbarkeits-Logik im Controller, nicht an closeSilent. (Das TODO, das du dir machen wolltest.)

8 ObjectMapper-Vereinheitlichung: fitbit-Importer-Mapper vs. eigener, anders konfigurierter TmdbApiClient-Mapper — nicht blind zusammenwerfen. Offene Logik-Frage.
---

## 💡 Code-Konventionen
- Klassen `PascalCase`, Methoden `camelCase`, Konstanten `UPPER_SNAKE_CASE`, Pakete `lowercase`.
- Plural für Set-Parameter, Singular für String.
- SOLID (v. a. Single Responsibility), DRY mit Maß, Konsistenz vor Perfektion, Pragmatismus vor
  Dogma, keine vorzeitige Abstraktion.
- Records für immutable DTOs.

> Die **Paket-, Benennungs- und Abhängigkeitsregeln** (Sorten, Schnitt, Richtungen,
> Domänenpräfix, Skin-Vertrag, Paketgröße) stehen im Dokument **`ThosSuite_Design_Regeln.md`**.

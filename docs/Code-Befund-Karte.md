# ThosSuite — Klassenkarte

Eine rein beschreibende Aufnahme des Anwendungscodes: je Klasse ihre Rolle, ihre öffentliche
Schnittstelle und die Pakete, von denen sie abhängt. **Keine Bewertung.** Sie ist die Grundlage
für den Befund in `Code-Befund.md`, damit eine spätere Sitzung nicht den gesamten Code erneut
lesen muss, um eine einzelne Gruppe beurteilen zu können.

- **Stand:** 20.09.2026, Commit 78fdc6b
- **Umfang:** `src/main/java/app`, 257 Dateien, rund 27.700 Zeilen. Ohne das Paket `scripts`.
- **Entstanden** in zehn parallelen Durchgängen, je einer pro Gruppe; die Reihenfolge der
  Abschnitte unten entspricht diesen Gruppen.

Die Abschnitte „Beobachtungen (unbewertet)" am Ende jeder Gruppe sind Feststellungen, keine
Befunde — sie sagen, was auffällt, nicht, ob es gut oder schlecht ist.

| Gruppe | Abschnitt |
|---|---|
| Lernen — Anki und gemeinsame Lernbasis | `app.learn`, `learn.model`, `learn.repository`, `learn.anki` + Unterpakete |
| Lernen — Region-Decks | `app.learn.region` + Unterpakete |
| Film und Serien | `app.movie` + Unterpakete |
| Messaging | `app.messaging` + Unterpakete |
| Gesundheit und Tagesdaten | `app.fitbit`, `activity`, `alc`, `diary`, `mattress`, `weekday` |
| Controller, Start und Screens | `app.controller`, `app.tmp` |
| shared — Basisdienste und Modell | `app.shared`, `app.shared.model` |
| shared/ui — Oberflächenrahmen | `app.shared.ui` |
| UI-Bausteine | `app.shared.ui.components` + `map` |
| Skin | `app.shared.skin` |

---


---

## Paket app.learn
Enthält die klassenübergreifenden Dienste des Lern-Kerns: das Verkleinern von Lernbildern und den zentralen, gecachten Zugriff auf Kartengeometrien für Anki- wie Region-Decks.

### ImageScaler
- **Rolle:** Verkleinert Bilder aus dem Lern-Bilderordner interaktiv (Nutzer wählt zwischen zwei Skalierverfahren) und sichert die Originale in einen Backup-Ordner.
- **Öffentlich:**
  - `static void processImages()`
- **Kennt:** app.shared (Config, ImageUtils, Log, Alerts, ImageComparisonDialog, ButtonEnum, SelectionEnum), org.imgscalr, javax.imageio
- **Art:** Hilfsklasse

### MapService
- **Rolle:** Singleton, das die Shape-Definitionen aller Karten (Anki und Region) skinunabhängig cached; liefert und wärmt GeoMaps und filtert die je Deck spielbaren Shapes.
- **Öffentlich:**
  - `static MapService getInstance()`
  - `GeoMap getMap(Deck type)`
  - `void preloadShapes(Deck type)`
  - `Set<MapShape> getPlayableShapesForDeck(Deck type)`
- **Kennt:** app.learn.model (Deck, GeoMap, MapMetadata, MapShape), app.learn.repository (MapRepository)
- **Art:** Dienst

## Paket app.learn.model
Framework-freie Datenklassen des Lern-Kerns: Deck-Definitionen, Kartengeometrie und Spaced-Repetition-Zustand, ohne JavaFX-Abhängigkeit.

### CircleSizes
- **Rolle:** Enum der Kreisgrößen für unsichtbare Trefferflächen/Marker; ordnet einen CSV-Größennamen einem Pixel-Radius zu (`fromCsvName`).
- **Art:** Enum/Konstanten

### Deck
- **Rolle:** Zentrales Enum aller lernbaren Einheiten (Anki-Decks und Regionen) mit deren Metadaten: id, Anzeigename, Kartenname, Kategorie, CSV-Dateiname(n), zugehörige MapMetadata, Config-Schlüssel für das Tagesbudget neuer Karten, Hauptstadt-Flag.
- **Öffentlich:**
  - `getId()`, `getDisplayName()`, `getCategory()`, `getConfigValueNewCards()`, `getDeckFileName()`, `getMapMetadata()`, `hasCapital()`, `getMapName()`
- **Kennt:** app.learn.model (DeckCategory, MapMetadata)
- **Art:** Enum/Konstanten

### DeckCategory
- **Rolle:** Enum, unterscheidet Anki-Deck von Region-Deck.
- **Art:** Enum/Konstanten

### GeoMap
- **Rolle:** Hält die Shapes einer Karte samt Kartentyp; einzige Stelle, die ids (echtes Shape, sichtbarer Kreis, Zentrieranker) in framework-freie Geometrien übersetzt.
- **Öffentlich:**
  - `getShapes()`, `getShapeGeometries()`, `getIds()`, `getType()`
  - `MapShape getShape(String id)`
  - `List<ShapeGeometry> geometryFor(Set<String> ids)`
  - `void setShapes(List<MapShape> transformed)`
- **Kennt:** app.shared.model (ShapeGeometry), app.learn.model (MapShape, MapType, CircleSizes)
- **Art:** Modell/Datenhalter

### LearnSessionInfo
- **Rolle:** Abstrakte Basis für die Menüanzeige der Fälligkeit einer Lerneinheit (wie viele Karten fällig).
- **Öffentlich:**
  - `abstract String formatForMenu()`
  - `abstract boolean isStillDueToday()`
- **Kennt:** — (keine Abhängigkeit außerhalb des eigenen Pakets)
- **Art:** Modell/Datenhalter (abstrakte Basis)

### LearnStat
- **Rolle:** Spaced-Repetition-Stand eines Lerngegenstands (Anki: pro Karte, Region: pro Deck/Modus); berechnet Fälligkeit und neues Level nach einer Antwort.
- **Öffentlich:**
  - `getDueDate()`, `isDueToday()`, `getLastPlayed()`, `getCurrentLevel()`, `getWrongCount()`, `incrementWrongCount()`
  - `setLevel(int)`, `setLastPlayed(LocalDate)`
  - `int calculateNewLevel(boolean correct, boolean playAgainToday)`
- **Kennt:** app.shared (AppClock)
- **Art:** Modell/Datenhalter

### MapElementListener
- **Rolle:** Callback-Interface für einen Klick auf ein Kartenelement (`mouseClicked(String id)`, id kann bei Fehlklick null sein).
- **Art:** Hilfsklasse (Callback-Interface)

### MapMetadata
- **Rolle:** Enum, das je Karte die GeoJSON-Dateien, ein optionales Hintergrundbild und den Kartentyp (SHAPE/IMAGE) festlegt.
- **Öffentlich:**
  - `getMapType()`, `getGeoJsonFiles()`
- **Kennt:** app.learn.model (MapType)
- **Art:** Enum/Konstanten

### MapShape
- **Rolle:** Attribute eines Shapes aus dem GeoJSON (Regions-/Hauptstadtname samt Schreibvarianten) plus dessen framework-freie Geometrie; prüft Texttreffer und ob das Shape Lernstoff ist.
- **Öffentlich:**
  - Record-Accessoren `deckId()`, `regionName()`, `capitalName()`, `altRegionNames()`, `altCapitalNames()`, `geometry()`
  - `id()`, `isPlayable()`
  - `isMatchingCapital(String)`, `isMatchingRegion(String)`, `isMatching(String)`
- **Kennt:** app.shared.model (ShapeGeometry)
- **Art:** Modell/Datenhalter

### MapType
- **Rolle:** Enum SHAPE/IMAGE.
- **Art:** Enum/Konstanten

### SessionProgressCounter
- **Rolle:** Record `(correct, incorrect, total)` — Zähler für den Sitzungsfortschritt.
- **Art:** Modell/Datenhalter

## Paket app.learn.repository
Lädt Kartengeometrien und Skizzenstrukturen aus GeoJSON-Dateien und übersetzt sie in framework-freie Geometrien.

### GeoJsonLoader (paketprivat)
- **Rolle:** Erzeugt aus einer GeoJSON-Datei (Multipolygone, Polygone, MultiLineStrings) eine Liste von MapShapes; invertiert die Y-Koordinate für Bildschirmkoordinaten. Nur vom MapRepository verwendet.
- **Öffentlich:** `List<MapShape> load(Path filePath, boolean isShapeMap)` (paketsichtbar)
- **Kennt:** app.learn.model (MapShape), app.shared.model (ShapeGeometry, Point), Jackson (ObjectMapper)
- **Art:** Repository

### MapRepository
- **Rolle:** Dünner Wrapper, der für ein Deck die passenden GeoJSON-Dateien (aus der Config) auflöst und daraus über den GeoJsonLoader eine GeoMap baut. Soll nur vom MapService verwendet werden.
- **Öffentlich:** `GeoMap load(Deck type)`
- **Kennt:** app.learn.model (Deck, GeoMap, MapMetadata, MapType, MapShape), app.shared (Config), app.learn.repository (GeoJsonLoader, intern)
- **Art:** Repository

### SketchFileSource
- **Rolle:** Eigener, schmalerer GeoJSON-Leser für Skizzenstrukturen (Hintergründe und aufgelegte Elemente): liefert Teilflächen als ShapeGeometry, prüft lückenlose und eindeutige Flächennummerierung, unterstützt Polygon-, Kreis- (inkl. Aussparung) und Canvas-Bbox-Parsing.
- **Öffentlich:**
  - `List<ShapeGeometry> load(String subfolder, String structure)`
  - `SketchStructure loadBackground(String structure)`
- **Kennt:** app.shared (Config), app.shared.model (ShapeGeometry, Point, SketchStructure, SketchStructure.Canvas), Jackson
- **Art:** Repository

## Paket app.learn.anki
Bildet den Ablauf einer Anki-Lernsession ab: Laden aller Decks samt Fortschritt, Kartenreihenfolge und -zustand während der Session sowie die Vermittlung zwischen Ablauf und Anzeige.

### AnkiDeckService
- **Rolle:** Lädt im Konstruktor alle Anki-Decks, deren Karten und Fortschritt; hält alle Karten sowie die heute fälligen je Deck, ermittelt verfügbare Labels und die Namen der Bild-Karten-Decks fürs Vorwärmen; vermittelt das Speichern gespielter Karten ans Repository.
- **Öffentlich:**
  - `getDueGameInfos()`, `getDueCards(Deck)`, `getAvailableLabels(Deck)`
  - `getCardsForPlay(Deck, int minIndex, int maxIndex, int maxCards, Set<String> labelFilter)`
  - `savePlayedCards(Deck, List<PlayedCardData>)`
  - `getImageMapNames()`
- **Kennt:** app.learn (MapService), app.learn.anki.model (Card, AnkiLearnSessionInfo), app.learn.anki.repository (DeckRepository, PlayedCardData), app.learn.model (Deck, MapType, DeckCategory, LearnSessionInfo), app.shared (Config)
- **Art:** Dienst

### AnkiDeckSession
- **Rolle:** Schale einer Anki-Lernsession (implementiert `Screen`); Brücke zwischen Controller und dem eigentlichen Karten-Ablauf (SessionProgress/SessionPresenter), orchestriert Lebenszyklus (Start, Sortierwechsel, Pause/Resume, Speichern, Zusammenfassungs-Alert, Sessionende) für reguläre Lern- und freie Spielrunden.
- **Öffentlich:**
  - `static AnkiDeckSession forLearning(List<Card>, Runnable onSessionEnded, AnkiDeckService, Deck)`
  - `static AnkiDeckSession forFreePlay(List<Card>, Runnable onSessionEnded, AnkiDeckService, Deck)`
  - `start()`, `sortOrderChanged()`, `refresh()`, `closeSilent(boolean save)`, `closeLoud()`, `reactOnPausePressed()`, `enterPressed()`, `suspend()`, `resume()`, `escClicked()`, `getView()`, `getSwitchStrategy()`
- **Kennt:** app.learn.anki.model (Card), app.learn.model (Deck, SessionProgressCounter), app.shared (Config, Log), app.shared.model (ButtonEnum, Screen, ScreenView, SessionSwitchStrategy), app.shared.ui (Alerts)
- **Art:** Sitzung/Ablauf

### AnkiPlaySetup
- **Rolle:** Richtet ein freies Anki-Spiel ein: zeigt einen Konfigurationsdialog (Index-Bereich, Kartenzahl, Labels) und liefert die dazu passenden Karten vom AnkiDeckService.
- **Öffentlich:**
  - `static List<Card> show(Deck deckType, AnkiDeckService service)`
- **Kennt:** app.learn.anki.model (Card), app.learn.model (Deck), app.shared.model (AnkiDialogState, ButtonEnum), app.shared.ui (Alerts, AnkiConfigDialog)
- **Art:** Sitzung/Ablauf (Setup-Dialog)

### CardProgress
- **Rolle:** Kapselt den Mehrschritt-Ablauf einer einzelnen Karte innerhalb einer Session: führt Steps aus (Text, Klick, Multiple Choice, Fast-Write, Skizzen), wertet Nutzereingaben aus, verwaltet Pause- und OnFail-Zweig, friert MC-Antwortoptionen clusterweise pro Durchlauf ein und meldet das Kartenende an SessionProgress.
- **Öffentlich:**
  - `start()`, `checkTextInput(String)`, `timeUp()`, `elementClicked(String)`, `mcClicked(int)`, `mcSubmitted()`, `pauseKeyPressed()`, `cancel()`, `isPaused()`, `getPlayedTimestamp()`, `isCorrectlyAnswered()`
- **Kennt:** app.learn.anki.model (Card mit seinen Step-Typen, FastAnswers, MultipleChoiceAnswers), app.learn.anki (SessionPresenter, SessionProgress), app.shared (Log)
- **Art:** Sitzung/Ablauf

### CardSortService
- **Rolle:** Liefert zu einer CardSortOrder eine Sortierfunktion für Kartenlisten (zufällig oder nach Level/Fehlerzahl auf-/absteigend).
- **Öffentlich:**
  - `static Consumer<List<Card>> getSorter(CardSortOrder order)`
- **Kennt:** app.learn.anki.model (Card, CardSortOrder)
- **Art:** Hilfsklasse

### SessionPresenter
- **Rolle:** Vermittelt zwischen der Anzeige (shared.ui) und dem SessionProgress: reicht Nutzereingaben (Klick, MC-Auswahl, Text, Submit, Zeitablauf) weiter und setzt dessen Anzeige-Anweisungen (Bild, Frage, Skizze, MC-Optionen, Fast-Write-Uhr, Kartenverlauf, Fortschrittstext) auf der passenden AnkiLearnView um; wählt je Deck-Typ die konkrete View-Implementierung.
- **Öffentlich:** u. a. `getView()`, `refresh()`, `showImage`, `showSketch`, `addSketch`, `markSketchAreas`, `fillSketchAreas`, `showQuestion`, `showMultipleChoice`, `waitForClick`, `waitForText`, `showFastStep`, `fastAnswerCorrect`, `fastTimeUp`, `restartClock`, `toggleClock`, `stopClock`, `suspendClock`, `resumeClock`, `setCorrectText`, `textIsCorrect`, `mapClickChecked`, `setCorrectMapElements`, `markMapElements`, `mcClickChecked`, `mcMarked`, `mcClickRejected`, `setCorrectMc`, `setSubmitActive`, `sessionProgressChanged`, `newCardIncoming`, `cardFinished`, `pause` … (n weitere, paketsichtbare Aufrufe von Progress und View)
- **Kennt:** app.learn (MapService), app.learn.anki.model (Card), app.learn.model (Deck, GeoMap, LearnStat, SessionProgressCounter), app.learn.repository (SketchFileSource), app.shared.model (ScreenView, SketchColor, AnkiCallbacks), app.shared.ui (AnkiLearnView, FastWriteLearnView, ShapeMapLearnView, ImageMapLearnView, McLearnView)
- **Art:** Presenter/Controller

### SessionProgress (paketprivat)
- **Rolle:** Kapselt den kompletten Karten-Ablauf einer Anki-Lernsession: Kartenreihenfolge (neue Karten zuerst, Sortierung), hält je Karte einen CardProgress, reicht Nutzerinteraktionen weiter, aggregiert den Sitzungsfortschritt und erzeugt beim Speichern die Persistenz-Zeilen samt LearnStat-Update je gespielter Karte.
- **Öffentlich (paketsichtbar):**
  - `setPresenter(SessionPresenter)`, `start()`, `sort(CardSortOrder)`, `cardFinished(boolean)`
  - `textInputChanged(String)`, `elementClicked(String)`, `mcClicked(int)`, `submitClicked()`, `reactOnPausePressed()`, `timeUp()`, `escClicked()`, `isPaused()`
  - `canGoBack()`, `goBack()`, `refresh()`, `hasProgressed()`, `save()`, `createSessionProgress()`
- **Kennt:** app.learn.anki.model (Card, CardSortOrder), app.learn.anki.repository (PlayedCardData), app.learn.model (Deck, LearnStat, SessionProgressCounter), app.shared (AppClock, Log)
- **Art:** Sitzung/Ablauf

## Paket app.learn.anki.model
Domänenmodell einer Anki-Karte: ihre Step-Typen, Sortierordnung und die Zustandsobjekte ihrer Antwortmechaniken (Multiple Choice, Fast-Write).

### AnkiLearnSessionInfo
- **Rolle:** LearnSessionInfo-Implementierung für ein Anki-Deck; hält Decktyp sowie aktuell/heute fällige Kartenzahl und formatiert die Menüanzeige.
- **Öffentlich:**
  - `formatForMenu()`, `isStillDueToday()`, `getDeckType()`
- **Kennt:** app.learn.model (Deck, LearnSessionInfo)
- **Art:** Modell/Datenhalter

### Card
- **Rolle:** Repräsentiert eine geparste Lernkarte: zerlegt eine CSV-Zeile in eine Folge von Steps (Bild, Text, MC/MC+, Klick-, Markier-, Skizzen-, Fast-Write-Steps) inklusive gewürfelter Shuffle-Blöcke, liefert je Durchgang frisch gewürfelte Steps sowie den OnFail-Abspann, und hält Lernfortschritt (LearnStat) und Labels der Karte.
- **Öffentlich:**
  - Konstruktoren `Card(List<String> csvTokens)`, `Card(List<String> csvTokens, LearnStat learnStat)`
  - `isDueToday()`, `isNew()`, `getId()`, `getLearnStat()`, `setLearnStat(LearnStat)`, `getSteps()`, `getOnFailSteps()`, `getLabels()`
  - öffentliche innere Step-Typen: `Image`, `ClickMapElements`, `Output`, `Input`, `MC`, `MCPlus`, `MarkMapElements`, `Pause`, `Fast`, `SketchImage`, `SketchImageAdd`, `SketchImageMark`, `SketchImageFill`, `AnswerOption`, `Role`, `Answer`; Konstante `MAX_FAST_SLOTS`
- **Kennt:** app.learn.model (LearnStat), app.shared.model (SketchColor), app.shared (AppClock)
- **Art:** Modell/Datenhalter

### CardSortOrder
- **Rolle:** Enum der Anki-Sortierreihenfolgen (Zufällig, nach Level auf-/absteigend, nach Fehlerzahl auf-/absteigend) mit Anzeigename.
- **Öffentlich:** `getDisplayName()`
- **Art:** Enum/Konstanten

### FastAnswers
- **Rolle:** Zustand eines laufenden Fast-Write-Steps: prüft getippten Text gegen offene Antworten, verwaltet gebundene (Hinweis/Reihenfolge) oder freie Feldbelegung, liefert Starttexte der Felder sowie die beim Zeitablauf aufzudeckenden Restantworten.
- **Öffentlich:**
  - `accept(String typed)`, `isComplete()`, `slotHints()`, `revealRest()`, `expectedSlot()`
  - Record `Hit(int slot, String text)`
- **Kennt:** app.learn.anki.model (Card.Answer, Card.Fast — selbes Paket)
- **Art:** Sitzung/Ablauf (Stepzustand)

### MultipleChoiceAnswers
- **Rolle:** Die für einen MC/MC+-Step gezeigten Optionen in eingefrorener Reihenfolge; liefert Rolle je Index, die korrekten Indizes und prüft, ob eine Auswahl unter Ausschluss tolerierter Optionen exakt korrekt ist.
- **Öffentlich:**
  - `getAnswerOptions()`, `roleAt(int)`, `getCorrectIndexes()`, `isFinallyCorrect(Set<Integer>)`
- **Kennt:** app.learn.anki.model (Card.AnswerOption, Card.Role — selbes Paket)
- **Art:** Sitzung/Ablauf (Stepzustand)

## Paket app.learn.anki.repository
Liest Kartendaten aus CSV und Lernfortschritt aus der Datenbank, gebündelt hinter einem gemeinsamen DeckRepository.

### CsvDeckCardSource (paketprivat)
- **Rolle:** Lädt alle Karten eines Decks aus dessen CSV-Datei(en); löst pro Deck eine oder mehrere Dateien auf, liest sie zeilenweise ein und prüft auf doppelte Karten-IDs über alle Dateien eines Decks hinweg.
- **Öffentlich:** `List<Card> loadAll(Deck type)` (paketsichtbar)
- **Kennt:** app.learn.anki.model (Card), app.learn.model (Deck, DeckCategory), app.shared (Config)
- **Art:** Repository

### DbDeckProgressRepository (paketprivat)
- **Rolle:** Zugriff auf den Anki-Lernfortschritt in `card_learn_stat` und `card_log`: lädt LearnStat je Karte, speichert gespielte Karten (Log-Eintrag plus Upsert des Lernstands) und zählt heute neu gelernte, aktuell fällige sowie insgesamt gelernte Karten.
- **Öffentlich (paketsichtbar):**
  - `Map<String,LearnStat> loadAll(Deck)`
  - `void saveLearned(Deck, List<PlayedCardData>)`
  - `int getNewLearnedToday(Deck)`, `int getInitialDue(Deck)`, `int getNoOfLearnedCards()`
- **Kennt:** app.learn.model (Deck, LearnStat), app.shared (AppClock, DB)
- **Art:** Repository

### DeckRepository
- **Rolle:** Wrapper um CSV- und DB-Repository; kombiniert CSV-Karten mit ihrem gespeicherten LearnStat aus der DB zu vollständigen Card-Objekten und reicht Speichern sowie Zähl-Abfragen durch. Soll nur vom AnkiDeckService (und vom DashboardScreen) verwendet werden.
- **Öffentlich:**
  - `getAllHints(Deck)`, `savePlayedCards(Deck, List<PlayedCardData>)`, `getInitialDue(Deck)`, `getNewLearnedToday(Deck)`, `getNoOfLearnedCards()`
- **Kennt:** app.learn.anki.model (Card), app.learn.model (Deck, LearnStat), app.learn.anki.repository (CsvDeckCardSource, DbDeckProgressRepository, intern)
- **Art:** Repository

### PlayedCardData
- **Rolle:** Record, reines Persistenz-Datenobjekt einer gespielten Karte (`cardId`, `level`, `wrongCount`, `correctFlag`, `playedTimestamp`).
- **Art:** Modell/Datenhalter

### Beobachtungen (unbewertet)
- Card.java hat 515 Zeilen und CardProgress.java 527 Zeilen.
- DbDeckProgressRepository baut mehrere SQL-Statements durch String-Konkatenation von Deck-Id und Datum statt durchgängig über PreparedStatement-Parameter.
- AnkiDeckService ruft im Konstruktor explizit `System.gc()` auf.

---

## Paket app.learn.region
Ablauf und Steuerung der Region-Lernsessions: Service für Kartendaten und Lernstände, Aufbau des freien Spiels, die Session-Hülle für den Controller sowie je ein Ablauf pro Modus (Click, Elimination, Write) samt Presenter zur View.

### ClickSessionProgress
- **Rolle:** Ablauf des Klick-Modus (Region oder Hauptort auf der Karte finden), in den Varianten leicht/schwer. Steuert Frage-Reihenfolge, offene Regionen und die Pause nach einem Fehlklick.
- **Öffentlich:**
  - `ClickSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionDeckService service, Runnable onFinished)`
  - `void start()`
  - `void resume()`
  - `void elementClicked(String id)`
  - `void endPause()`
  - `boolean hasProgressed()`
  - verschachtelter Record `QuizElement(String toFind, String shapeId)`
- **Kennt:** app.learn.model (MapShape), app.learn.region.model (Mode, SessionSpec), SessionPresenter (WrongClickResolution), Basisklasse SessionProgress.
- **Art:** Sitzung/Ablauf

### EliminationSessionProgress
- **Rolle:** Ablauf des Eliminations-Modus. Nutzer tippt fortlaufend Namen, treffende Regionen scheiden aus der verbleibenden Menge aus.
- **Öffentlich:**
  - `EliminationSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionDeckService service, Runnable onFinished)`
  - `void start()`
  - `void cancel()`
  - `void textInputChanged(String text)`
  - `boolean hasProgressed()`
- **Kennt:** app.learn.model (MapShape), app.learn.region.model (SessionSpec), Basisklasse SessionProgress.
- **Art:** Sitzung/Ablauf

### RegionDeckService
- **Rolle:** Hält für alle Region-Decks die geladenen Kartenformen und die Lernstände je Deck/Modus vor; zentraler Zugriffspunkt für Region-Sessions auf diese Daten.
- **Öffentlich:**
  - `RegionDeckService()`
  - `List<LearnSessionInfo> getDueGameInfos()`
  - `Set<MapShape> getRegions(SessionSpec spec)`
  - `LearnStat getLearnStat(SessionSpec spec)`
  - `void savePlayedSession(SessionSpec spec, LearnStat stats, boolean correct, String incorrectId)`
- **Kennt:** app.learn (MapService), app.learn.model (Deck, DeckCategory, LearnSessionInfo, LearnStat, MapShape), app.learn.region.model (Mode, RegionLearnSessionInfo, SessionSpec), app.learn.region.repository.RegionDeckRepository, app.shared.AppClock.
- **Art:** Dienst

### RegionPlaySetup
- **Rolle:** Richtet ein freies Regions-Spiel ein: zeigt den Auswahldialog für Decks und Modus und liefert daraus Spec und passende Regionen. Nicht instanziierbar, nur statischer Zugang.
- **Öffentlich:**
  - `static RegionPlaySelection show(RegionDeckService service)`
- **Kennt:** app.learn.model (Deck, DeckCategory, MapMetadata, MapShape), app.learn.region.model (Mode, RegionPlaySelection, SessionSpec), app.shared.model (ButtonEnum, RegionDialogState), app.shared.ui (Alerts, RegionConfigDialog).
- **Art:** Dienst

### RegionSession
- **Rolle:** Hülle einer Regions-Lernsession und Ansprechpartner für den Controller (`Screen`). Hält den passenden `SessionProgress` (je nach Modus) und den `SessionPresenter`, wertet beim Abschluss das Ergebnis aus (Alert, Speichern, Abmelden beim Controller).
- **Öffentlich:**
  - `RegionSession(SessionSpec spec, Set<MapShape> regions, Runnable onSessionEnded, RegionDeckService regionService)`
  - `void start()`
  - `void escClicked()`
  - `SessionSwitchStrategy getSwitchStrategy()`
  - `void reactOnPausePressed()`
  - `void closeLoud()`
  - `void refresh()`
  - `ScreenView getView()`
  - `void closeSilent(boolean save)`
- **Kennt:** app.learn.model (MapShape), app.learn.region.model (Mode, SessionResult, SessionSpec), app.shared (AppClock, Log), app.shared.model (AlertOptions, ButtonEnum, Screen, ScreenView, SessionSwitchStrategy), app.shared.ui.Alerts.
- **Art:** Sitzung/Ablauf (implementiert zugleich `Screen`)

### SessionPresenter
- **Rolle:** Vermittler zwischen der Kartenansicht (`RegionLearnView`) und dem jeweiligen `SessionProgress`. Übersetzt Fortschritts-Ereignisse in View-Zustandsänderungen und Nutzereingaben (Klick, Text) zurück in Aufrufe am Progress.
- **Öffentlich:**
  - `SessionPresenter(SessionProgress progress, SessionSpec spec)`
  - `ScreenView getView()`
  - `void refresh()`
  - `void weWaitForClick(Set<String> ids)`
  - `void weWaitForEliminationText(Set<String> ids)`
  - `void weWaitForWriteText(String id)`
  - `void prepareWriteSession(Set<String> ids)`
  - `void setCorrectText(String correctText)`
  - `void showQuestion(String text)`
  - `void handleClickResult(String id, boolean correct, String correctId)`
  - `void handleCorrectAnswers(Set<String> matches)`
  - `void handleMissedWrite(String id)`
  - `void undoWrongClick(WrongClickResolution resolution)`
  - `void clickedMapElement(String id)`
  - `void typedText(String text)`
  - verschachteltes Enum `WrongClickResolution { ROLLBACK_FOR_RETRY, COMMIT_MISS_AND_CONTINUE }`
- **Kennt:** app.learn.region.model (Mode, SessionSpec), app.learn.MapService, app.shared.model (RegionCallbacks, ScreenView, ShapeMapState), app.shared.ui.RegionLearnView, JavaFX (über die View).
- **Art:** Presenter/Controller

### SessionProgress
- **Rolle:** Gemeinsamer Unterbau der drei Modus-Abläufe (Click, Elimination, Write). Hält Bezug zu Spec, `RegionDeckService` und Presenter, definiert den Abschluss einer Session (korrekt/inkorrekt) und schreibt den Lernstand fort. Abstrakte Klasse.
- **Öffentlich:** kein public API; Konstruktor und Kernmethoden sind protected bzw. paketsichtbar für die drei Unterklassen: `protected SessionProgress(SessionSpec, RegionDeckService, Runnable onFinished)`, `protected void finishCorrect()`, `protected void finishIncorrect(String, boolean, String)`, `protected Set<String> getIds(Set<MapShape>)`, dazu paketsichtbare/abstrakte Erweiterungspunkte `start()`, `hasProgressed()`, `resume()`, `endPause()`, `cancel()`, `elementClicked(String)`, `textInputChanged(String)`, `save()`.
- **Kennt:** app.learn.model (LearnStat, MapShape), app.learn.region.model (SessionResult, SessionSpec), app.shared.AppClock.
- **Art:** Sitzung/Ablauf (Basisklasse)

### WriteSessionProgress
- **Rolle:** Ablauf des Schreib-Modus. Die Karte markiert ein Element, der Nutzer tippt dessen Namen; Abbruch (ESC) zeigt die Lösung und pausiert, mit unterschiedlichem Ausgang je nach Lern- oder Spielsession.
- **Öffentlich:**
  - `WriteSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionDeckService service, Runnable onFinished)`
  - `void start()`
  - `void cancel()`
  - `void textInputChanged(String text)`
  - `void endPause()`
  - `boolean hasProgressed()`
- **Kennt:** app.learn.model (MapShape), app.learn.region.model (Mode, SessionSpec), Basisklasse SessionProgress.
- **Art:** Sitzung/Ablauf

## Paket app.learn.region.model
Wert- und Identitätstypen rund um eine Region-Session: Modus-Enum, Session-Identität (Spec), Ergebnis- und Auswahl-Transportobjekte sowie der Menüeintrag für fällige Sessions.

### Mode
- **Rolle:** Enum der neun Lernmodi (Click/Elimination/Write je nach Ziel Region/Hauptort/Beides und Schwierigkeit), mit Anzeigename und den drei Achsen als eigene Unter-Enums (`SubCategory`, `CapitalOrRegion`, `EasyHard`).
- **Art:** Enum/Konstanten

### RegionLearnSessionInfo
- **Rolle:** Repräsentiert im Menü eine konkrete fällige Region-Lernsession (Deck, Modus, Level, Fälligkeit); Unterklasse von `app.learn.model.LearnSessionInfo`.
- **Öffentlich:**
  - `RegionLearnSessionInfo(SessionSpec spec, int level, boolean isDueToday)`
  - `String formatForMenu()`
  - `boolean isStillDueToday()`
  - `SessionSpec getSpec()`
- **Kennt:** app.learn.model.LearnSessionInfo (Basisklasse).
- **Art:** Modell/Datenhalter

### RegionPlaySelection
- **Rolle:** Transportobjekt für das Ergebnis der Spielauswahl (Spec plus die dazu passenden Regionen). Record.
- **Art:** Modell/Datenhalter

### SessionResult
- **Rolle:** Ausgang einer Region-Session (korrekt, Fehlertext, ob Fortsetzen erlaubt ist), gebaut vom Progress und abgeholt von der Session-Hülle. Record.
- **Art:** Modell/Datenhalter

### SessionSpec
- **Rolle:** Identifiziert eine lernbare Region-Session eindeutig: Deck, Modus, optionale Zusatzdecks fürs freie Spiel und ob es sich um eine Spiel- oder Lernsession handelt.
- **Öffentlich:**
  - `SessionSpec(Deck deck, Mode mode)`
  - `SessionSpec(Deck deck, Mode mode, Set<Deck> additionals, boolean isPlaySession)`
  - `Deck getDeckType()`
  - `Mode getMode()`
  - `Set<Deck> getAdditonalDeckTypesForPlay()`
  - `boolean isPlaySession()`
- **Kennt:** app.learn.model.Deck.
- **Art:** Modell/Datenhalter

## Paket app.learn.region.repository
Zugriff auf die persistierten Region-Lernstände: ein schlanker Wrapper und die dahinterliegende JDBC-Anbindung.

### DbRegionDeckProgressRepository
- **Rolle:** Direkter Datenbankzugriff (JDBC) für Region-Lernstände und -Verlauf; liest und schreibt die Tabellen `region_learn_stat` und `region_log`.
- **Öffentlich:** `DbRegionDeckProgressRepository()`. Laden (`load`) und Speichern (`save`) sind paketsichtbar und ausschließlich für `RegionDeckRepository` gedacht.
- **Kennt:** app.learn.model.LearnStat, app.learn.region.model.SessionSpec, app.shared.AppClock, app.shared.DB (JDBC-Verbindung).
- **Art:** Repository

### RegionDeckRepository
- **Rolle:** Schlanker Wrapper um `DbRegionDeckProgressRepository`; einziger Zugriffspunkt, den `RegionDeckService` auf die Lernstände nutzt.
- **Öffentlich:**
  - `RegionDeckRepository()`
  - `LearnStat getLearnStat(SessionSpec spec)`
  - `void saveRegionSession(SessionSpec spec, LearnStat stats, boolean correct, String wrongId)`
- **Kennt:** app.learn.model.LearnStat, app.learn.region.model.SessionSpec, DbRegionDeckProgressRepository (intern).
- **Art:** Repository

### Beobachtungen (unbewertet)
- `SessionProgress` und seine drei Unterklassen bilden zusammen mit `SessionPresenter` und `RegionSession` einen dreistufigen Aufbau: Controller-Hülle → Presenter → modusspezifischer Ablauf.
- Der Presenter erhält seine Progress-Referenz über den Konstruktor, der Progress seine Presenter-Referenz umgekehrt über einen nachträglichen Setter (`setPresenter`).
- `SessionSpec`, `RegionLearnSessionInfo` und die beiden Repository-Klassen sind als gewöhnliche Klassen statt als Records modelliert, obwohl sie überwiegend Daten halten.

---

## Paket app.movie
Orchestriert den TMDB-Import (Filme, Serien, Episoden) über die REST-API und stellt den Viewer-Screen bereit, der importierte Mediendaten anzeigt.

### ApiClient
- **Rolle:** Kapselt die gesamte HTTP-Kommunikation mit der TMDB-API (v3 und v4) — Bewertungen, Detaildaten zu Film/Serie/Staffel/Episode, Credits, Personen, Bilder.
- **Öffentlich:**
  - `MovieRatingsPageJSON getRatedMovies(int page)`
  - `TvShowRatingsPageJSON getRatedTvShows(int page)`
  - `EpisodeRatingsPageJSON getRatedEpisodes(int page)`
  - `MovieJSON getMovieDetails(int movieId)`
  - `TvShowJSON getTvShowDetails(int tvShowId)`
  - `SeasonJSON getSeasonDetails(int tvShowId, int seasonNumber)`
  - `EpisodeJSON getEpisodeDetails(int tvShowId, int seasonNumber, int episodeNumber)`
  - `CreditListJSON getMovieCredits(int movieId)`
  - `CreditListJSON getAggregatedTvShowCredits(int tvShowId)`
  - `CreditListJSON getRegularSeasonCredits(int tvShowId, int seasonNumber)`
  - `CreditListJSON getAggregatedSeasonCredits(int tvShowId, int seasonNumber)`
  - `PersonJSON getPerson(int personId)`
  - `byte[] getImage(String path, String width)`
- **Kennt:** app.movie.model.json (alle Antwort-DTOs), app.shared (Config, Log), TMDB-REST-API (HTTP, v3/v4).
- **Art:** Dienst

### MovieCleanup
- **Rolle:** Aufräum-Ablauf nach dem Import — entscheidet offene Crew-Pending-Jobs per Whitelist/Blacklist-Dialog, fragt fehlende Kommentare zu kürzlich bewerteten Filmen ab und zeigt am Ende eine Zusammenfassung offener Punkt-Kommentare. Kein Netzwerkzugriff.
- **Öffentlich:** `MovieCleanup()`, `void run()`
- **Kennt:** app.movie.model (CrewPendingEntry, NullCommentEntry), app.movie.repository (CrewFilterRepository, EpisodeRepository, MovieRepository, PendingRepository, TvShowRepository), app.shared (Log, ButtonEnum, Alerts, TextPromptDialog).
- **Art:** Sitzung/Ablauf

### MovieImporter
- **Rolle:** Orchestriert den täglichen automatischen Filmimport von TMDB (neue Bewertungen laden, importieren, Umbewertungen per Rolling-Check erkennen, Fortschritts-Timestamps pflegen).
- **Öffentlich:** `MovieImporter()`, `void run()`
- **Kennt:** ApiClient, app.movie.model.json (MovieJSON, MovieRatingJSON, MovieRatingsPageJSON, CreditListJSON, CastJSON, CrewJSON), app.movie.repository (CrewFilterRepository, MovieRepository, PendingRepository), app.shared (Config, DB, ImageUtils, Log, ButtonEnum, Alerts).
- **Art:** Sitzung/Ablauf

### MovieViewerScreen
- **Rolle:** Bildschirm des Film-/Serien-Viewers; verbindet Auswahl (Regisseur, Schauspieler, Titel) mit der Kartenanzeige.
- **Öffentlich:** `MovieViewerScreen()`, `ScreenView getView()`, `void refresh()`, `SessionSwitchStrategy getSwitchStrategy()`
- **Kennt:** app.movie.repository.MovieViewerRepository, app.shared.model (Screen, ScreenView, SessionSwitchStrategy), app.shared.ui.MovieViewerScreenView, JavaFX (indirekt über View).
- **Art:** Presenter/Controller (implementiert `Screen`)

### SeriesImporter
- **Rolle:** Orchestriert den manuellen Serien-/Episodenimport: neue Serien und Episoden mit Kaskade (Show → Season → Episode bei Bedarf nachimportieren), Umbewertungen, Änderungs-Check an Seriendaten (Seasons/Episodes/Status/Last-Air-Date), Season-Regulars-Abgleich, Lücken-Check für fehlende Poster/Overviews, interaktive Kommentar- und Crew-Whitelist-Dialoge. Läuft komplett auf dem FX-Thread, jede Entität in eigener Transaktion.
- **Öffentlich:** `SeriesImporter()`, `void run()`
- **Kennt:** ApiClient, app.movie.model (EpisodeForApi, TvShowComparisonData), app.movie.model.json (CastJSON, CreditListJSON, CrewJSON, EpisodeJSON, EpisodeRatingJSON, EpisodeRatingsPageJSON, JobJSON, SeasonJSON, TvShowJSON, TvShowRatingJSON, TvShowRatingsPageJSON), app.movie.repository (CrewFilterRepository, EpisodeRepository, MovieRepository, SeasonRepository, TvShowRepository), app.shared (Config, DB, ImageUtils, Log, ButtonEnum, Alerts, TextPromptDialog).
- **Art:** Sitzung/Ablauf

## Paket app.movie.model
Kleine Datenhalter für importinterne Zwischenzustände, die keine direkten API-Antworten sind (Pending-Warteschlange, Vergleichsdaten, Lookup-Schlüssel).

### CrewPendingEntry
- **Rolle:** Datenhalter für einen offenen Crew-Pending-Eintrag (Person, Job, Department, Film) zur Abarbeitung in MovieCleanup.
- **Art:** Modell/Datenhalter

### EpisodeForApi
- **Rolle:** Record mit den drei Schlüsselfeldern (tvShowId, seasonNumber, episodeNumber), um eine Episode gegenüber der TMDB-API zu adressieren.
- **Art:** Modell/Datenhalter

### NullCommentEntry
- **Rolle:** Datenhalter für einen Film mit fehlendem Kommentar (id, Titel, deutscher Titel, Bewertung, Bewertungsdatum) zur Abfrage in MovieCleanup.
- **Art:** Modell/Datenhalter

### TvShowComparisonData
- **Rolle:** Hält den DB-Stand einer Serie (Anzahl Seasons/Episodes, Status, Last-Air-Date) und vergleicht ihn per `differs(TvShowJSON webData)` gegen den aktuellen API-Stand, um Änderungen zu erkennen.
- **Öffentlich:** `TvShowComparisonData(int, int, String, String)`, `boolean differs(TvShowJSON webData)`
- **Kennt:** app.movie.model.json.TvShowJSON.
- **Art:** Modell/Datenhalter

## Paket app.movie.model.json
Sammlung von 26 JSON-DTOs, die TMDB-API-Antworten 1:1 auf Java-Felder abbilden (mit Jackson deserialisiert). Abgedeckt werden Film- und Seriendetails (MovieJSON, TvShowJSON, SeasonJSON, EpisodeJSON), Bewertungslisten samt Paginierung (MovieRatingsPageJSON, TvShowRatingsPageJSON, EpisodeRatingsPageJSON und die zugehörigen *RatingJSON), Credits (CastJSON, CrewJSON, CreditListJSON, RoleJSON, JobJSON) sowie einfache Nebenstrukturen (GenreJSON, NetworkJSON, ProductionCompanyJSON, ProductionCountryJSON, SpokenLanguageJSON, BelongsToCollectionJSON, CreatedByJSON, EpisodeToAirJSON, PersonJSON, AccountRatingJSON). Die meisten Klassen bestehen nur aus öffentlichen Feldern ohne Logik; einzelne Felder wie `german_title`/`german_name` werden nicht von der API geliefert, sondern nachträglich von ApiClient aus einem zweiten Request befüllt. CastJSON und CrewJSON bilden zusätzlich mit Gettern/Settern (`character`/`credit_id` bzw. `job`/`credit_id`) flache und aggregierte Credit-Varianten auf dieselben Felder ab. `SomethingWithAPosterJSON` ist eine abstrakte Basisklasse (nur von MovieJSON genutzt) für den gemeinsamen Zugriff auf `id`/`poster_path`.

## Paket app.movie.repository
Datenzugriffsschicht für die TMDB-Datenbank: Insert/Update/Select für Filme, Serien, Staffeln, Episoden, Pending-Einträge und Crew-Filterlisten, sowie Aufbereitung der Anzeigedaten für den Viewer.

### CardDataFactory
- **Rolle:** Baut aus Rohfeldern das anzeigefertige `CardData`-DTO für den Viewer (Header-/Detailzeilen, Auswahl von Overview und Bild) getrennt nach Film, Serie und Episode. Reine Formatierungslogik ohne DB-Zugriff.
- **Öffentlich:**
  - `static CardData forMovie(int id, String title, String germanTitle, String originalTitle, LocalDate releaseDate, int rating, LocalDate ratedAt, List<String> directors, List<String> actors, String overview, String comment, String imageFilename)`
  - `static CardData forTvShow(…)`
  - `static CardData forEpisode(…)`
- **Kennt:** app.shared.model.CardData.
- **Art:** Hilfsklasse

### CrewFilterRepository
- **Rolle:** Verwaltet Whitelist und Blacklist der TMDB-Crew-Jobs; lädt beide beim Importstart komplett in den Speicher und hält sie während des Laufs lokal aktuell, um DB-Zugriffe pro Job zu vermeiden.
- **Öffentlich:**
  - `void load()`
  - `boolean isWhitelisted(String job)`
  - `boolean isBlacklisted(String job)`
  - `void addToWhitelist(String job, Connection con)`
  - `void addToWhitelist(String job)`
  - `void addToBlacklist(String job, Connection con)`
  - `void addToBlacklist(String job)`
  - `List<String> getPendingJobs()`
- **Kennt:** app.shared (DB, Log).
- **Art:** Repository

### EpisodeRepository
- **Rolle:** Datenbankoperationen rund um Episoden in der TMDB-Datenbank (Insert, Rating, Cast/Crew, Kommentare, Flags, fehlende Overviews). Kein Netzwerk-, kein Dateisystemzugriff.
- **Öffentlich:**
  - `void insertEpisode(EpisodeJSON episode, Connection conn)`
  - `void insertEpisodeRating(int episodeId, int rating, String comment, …)`
  - `void insertEpisodeCast(CastJSON cast, int episodeId, Connection conn)`
  - `void insertEpisodeCrew(CrewJSON crew, int episodeId, Connection conn)`
  - `Integer getEpisodeRating(int episodeId)`
  - `String getEpisodeComment(int episodeId)`
  - `List<EpisodeForApi> getEpisodesWithoutOverview()`
  - `List<String> loadDotCommentTitles()`
  - `void updateEpisodeRating(int episodeId, int rating, String comment)`
  - `void updateEpisodeFlags(int episodeId, Boolean ratedSeason)`
  - `void updateEpisodeFlags(int episodeId, Boolean ratedSeason, …)`
  - `void updateOverview(int episodeId, String overview)`
- **Kennt:** app.movie.model.EpisodeForApi, app.movie.model.json (CastJSON, CrewJSON, EpisodeJSON), app.shared (DB, Log).
- **Art:** Repository

### MovieRepository
- **Rolle:** Datenbankoperationen rund um Filme in der TMDB-Datenbank (Insert von Film, Rating, Bild, Person, Cast/Crew, Genres/Länder/Sprachen, Kommentar- und Lückenabfragen). Kein Netzwerk-, kein Dateisystemzugriff.
- **Öffentlich:**
  - `void insertMovie(MovieJSON movie, Connection conn)`
  - `void insertMovieRating(MovieRatingJSON rating, String comment, Connection conn)`
  - `void insertMovieImage(MovieJSON movie, int width, int height, String filename, Connection conn)`
  - `void insertPersonIfNotExists(PersonJSON person, Connection conn)`
  - `void insertMovieCast(CastJSON cast, int movieId, Connection conn)`
  - `void insertMovieCrew(CrewJSON crew, int movieId, Connection conn)`
  - `void insertMovieCrew(int movieId, int personId, String creditId, …)`
  - `void insertMovieGenres(MovieJSON movie, Connection conn)`
  - `void insertMovieCountries(MovieJSON movie, Connection conn)`
  - `void insertMovieLanguages(MovieJSON movie, Connection conn)`
  - `void updateMovieRating(MovieRatingJSON rating, String comment)`
  - `String getMovieComment(int movieId)`
  - `Integer getMovieRating(int movieId)`
  - `List<NullCommentEntry> loadNullCommentEntries()`
  - `void saveComment(int movieId, String comment)`
  - … (7 weitere: loadDotCommentTitles, getMoviesWithoutOverview, getMoviesWithoutPoster, updateMovieOverview, updateMoviePoster, updateMoviePosterPath)
- **Kennt:** app.movie.model.NullCommentEntry, app.movie.model.json (CastJSON, CrewJSON, GenreJSON, MovieJSON, MovieRatingJSON, PersonJSON, ProductionCountryJSON, SpokenLanguageJSON), app.shared (DB, Log).
- **Art:** Repository

### MovieViewerRepository
- **Rolle:** Leseoperationen für den Viewer; liefert `CardData` für Filme, Serien und Episoden über bestehende Detail-Views (movie_details, tv_show_details, episode_details) sowie Namenslisten für die Auswahl (Regisseure, Schauspieler, Titel) über eigene Views.
- **Öffentlich:**
  - `List<CardData> loadByDirector(String directorName)`
  - `List<CardData> loadByActor(String actorName)`
  - `List<CardData> loadByTitle(String title)`
  - `List<CardData> loadAllEpisodes()`
  - `List<String> loadAllDirectorNames()`
  - `List<String> loadAllActorNames()`
  - `List<String> loadAllTitles()`
- **Kennt:** app.shared (Config, DB, model.CardData), CardDataFactory (indirekt über gleiches Paket).
- **Art:** Repository

### PendingRepository
- **Rolle:** Verwaltet die Pending-Tabellen `person_pending` und `crew_pending`, in denen Personen und unbekannte Crew-Jobs während des Imports zwischengelagert werden, bis MovieCleanup über Übernahme oder Verwurf entscheidet.
- **Öffentlich:**
  - `void insertPersonPending(PersonJSON person, Connection conn)`
  - `void insertCrewPending(int movieId, int personId, String personName, String job, …)`
  - `void transferPersonToMain(int personId)`
  - `void deletePersonPending(int personId)`
  - `void deleteCrewPending(int movieId, int personId, String job)`
  - `List<CrewPendingEntry> loadCrewPendingEntries()`
  - `boolean hasMoreCrewPendingForPerson(int personId)`
- **Kennt:** app.movie.model.CrewPendingEntry, app.movie.model.json.PersonJSON, app.shared (DB, Log).
- **Art:** Repository

### SeasonRepository
- **Rolle:** Datenbankoperationen rund um Staffeln (Insert, Bild inkl. Kopieren des Show-Posters als Fallback, Cast/Crew, Existenzprüfung, Markieren von Season-Regulars gemäß der aggregated-Invariante aus SeriesImporter).
- **Öffentlich:**
  - `void insertSeason(SeasonJSON season, Connection conn)`
  - `void insertSeasonImage(SeasonJSON season, int width, int height, …)`
  - `void copyShowImageToSeason(SeasonJSON season, Connection conn)`
  - `void insertSeasonCast(CastJSON cast, SeasonJSON season, Connection conn)`
  - `void insertSeasonCrew(CrewJSON crew, SeasonJSON season, String job, …)`
  - `boolean seasonExists(int tvShowId, int seasonNumber)`
  - `void markRegularCast(CastJSON cast, int seasonId, Connection conn)`
  - `void markRegularCrew(CrewJSON crew, int seasonId, Connection conn)`
- **Kennt:** app.movie.model.json (CastJSON, CrewJSON, RoleJSON, SeasonJSON), app.shared (DB, Log).
- **Art:** Repository

### TvShowRepository
- **Rolle:** Datenbankoperationen rund um Serien in der TMDB-Datenbank (Insert, Rating, Bild, Cast/Crew, Genres/Länder/Sprachen, Vergleichsdaten für Änderungserkennung, Kommentar- und Lückenabfragen). Kein Netzwerk-, kein Dateisystemzugriff.
- **Öffentlich:**
  - `void insertTvShow(TvShowJSON show, Connection conn)`
  - `void insertTvShowRating(TvShowRatingJSON rating, String comment, Connection conn)`
  - `void insertTvShowImage(TvShowJSON show, int width, int height, …)`
  - `void insertTvShowImage(int tvShowId, String posterPath, int width, int height, …)`
  - `void insertTvShowCast(CastJSON cast, int tvShowId, Connection conn)`
  - `void insertTvShowCrew(CrewJSON crew, int tvShowId, String job, …)`
  - `void insertTvShowGenres(TvShowJSON show, Connection conn)`
  - `void insertTvShowCountries(TvShowJSON show, Connection conn)`
  - `void insertTvShowLanguages(TvShowJSON show, Connection conn)`
  - `Integer getTvShowRating(int tvShowId)`
  - `String getTvShowComment(int tvShowId)`
  - `boolean tvShowExists(int tvShowId)`
  - `TvShowComparisonData loadComparisonData(int tvShowId)`
  - `List<String> loadDotCommentTitles()`
  - `List<Integer> getShowsWithoutOverview()`
  - … (5 weitere: getShowsWithoutPoster, updateTvShowRating, updateTvShowData, updateOverview, updatePosterPath)
- **Kennt:** app.movie.model.TvShowComparisonData, app.movie.model.json (CastJSON, CrewJSON, GenreJSON, ProductionCountryJSON, RoleJSON, SpokenLanguageJSON, TvShowJSON, TvShowRatingJSON), app.shared (DB, Log).
- **Art:** Repository

### Beobachtungen (unbewertet)
- `app.movie.model.json` enthält 26 Klassen, für die meisten gilt: nur öffentliche Felder ohne Methoden.
- `SeriesImporter` trägt einen mehrseitigen Klassen-Javadoc-Kommentar, der eine fachliche Invariante (Season-Regulars ⊆ aggregierte Credits) begründet.
- `MovieRepository` (465 Zeilen) und `TvShowRepository` (390 Zeilen) sind die umfangreichsten Klassen dieser Gruppe.

---

# Messaging (Signal und WhatsApp)

## Paket app.messaging.repository
Quellenunabhängige Datenbankschicht für alle Messaging-Importe: Lesen und Schreiben der ThosSuite-DB-Tabellen zu Nachrichten, Chats, Kontakten und Anhängen, parametrisiert über eine Quellkennung ("signal", "whatsapp").

### MessageRepository
- **Rolle:** Kapselt sämtliche Lese- und Schreibzugriffe auf die ThosSuite-DB für Nachrichten, Chats, Kontakte, Chat-Mitgliedschaften, Nachrichtentypen und Anhänge. Schreibmethoden erwarten eine von außen verwaltete Transaktion (Connection wird hereingereicht).
- **Öffentlich:**
  - `getLastImportedSourceId(String source): String`
  - `getSentAtForSourceId(String source, String sourceId): LocalDateTime`
  - `loadBlacklistedChatIds(String source): Set<String>`
  - `loadKnownChats(String source): Map<String,Integer>`
  - `loadKnownContacts(String source): Map<String,Integer>`
  - `loadChatMembers(): Map<Integer,Set<Integer>>`
  - `loadMessageTypes(String source): Map<Integer,Boolean>`
  - `isAlreadyImported(Connection thos, String source, String sourceId): boolean`
  - `insertContact(Connection thos, String displayName): int`
  - `insertContactMapping(Connection thos, String source, String rawIdentifier, int contactId): void`
  - `insertChat(Connection thos, String source, String rawIdentifier, boolean isGroup, String displayName, boolean blacklisted): int`
  - `insertChatMemberIfAbsent(Connection thos, int chatId, int contactId): void`
  - `insertMessage(Connection thos, String source, String sourceId, LocalDateTime sentAt, int fromContact, int chatId, String content, String resolvedQuoteMsgId): void`
  - `insertAttachment(Connection thos, String source, String sourceId, String relativePath, boolean available): void`
  - … (3 weitere: `getMessageCountToday()`, `loadAllContactsByDisplayName()`, `getLastWhatsAppMessageDate()`)
  - Zusätzlich öffentliche Konstante `PLATZHALTER_NAME` (Anzeigename für Kontakte ohne bekannte Person).
- **Kennt:** app.shared (DB für Connection-Beschaffung). Kennt keine der beiden Quell-DBs (Signal/WhatsApp) direkt.
- **Art:** Repository

## Paket app.messaging.signal
Ablauf und Regeln für den inkrementellen Import neuer Signal-Nachrichten aus der lokalen Signal-Desktop-DB in die ThosSuite-DB, inklusive Anhangsentschlüsselung und Zitat-Auflösung.

### SignalIncrementalImport
- **Rolle:** Steuert den kompletten inkrementellen Signal-Import: Bestimmt das Importfenster anhand der zuletzt importierten Nachricht, filtert importierbare Nachrichten, legt bei Bedarf Chats und Kontakte an (mit Nutzer-Rückfrage), entschlüsselt Anhänge und schreibt alles in einer Transaktion in die ThosSuite-DB.
- **Öffentlich:** `run(): void`
- **Kennt:** app.messaging.repository (MessageRepository), app.messaging.signal.model (AttachmentInfo, ContactInfo, ConversationInfo), app.messaging.signal.repository (SignalSourceRepository), app.shared (Config, DB, Log, ButtonEnum, ThrowingConsumer, Alerts, MessageContactDialog), fremde Signal-SQLite-DB (SQLCipher-verschlüsselt, per JDBC), javax.crypto (AES/CBC zur Anhangsentschlüsselung).
- **Art:** Sitzung/Ablauf

## Paket app.messaging.signal.model
Reine Datenhalter für Werte, die aus der Signal-DB gelesen werden (Conversation, Contact, Attachment).

### AttachmentInfo
- **Rolle:** Datenhalter für ein aus der Signal-DB gelesenes Anhang-Metadatum (Pfad, lokaler Schlüssel, Dateiname, Größe, Content-Type, Attachment-Typ). Record.

### ContactInfo
- **Rolle:** Datenhalter für Namensfelder eines Signal-Kontakts (Name, Profilname, Profil-Nachname, vollständiger Profilname). Record.

### ConversationInfo
- **Rolle:** Datenhalter für Metadaten einer Signal-Conversation (Gruppe ja/nein, Name, Profilnamen, Nachrichtenanzahl, gemeinsame Gruppen). Record.

## Paket app.messaging.signal.repository
Datenzugriffsschicht auf die externe, verschlüsselte Signal-Desktop-DB (read-only).

### SignalSourceRepository
- **Rolle:** Kapselt alle lesenden SQL-Zugriffe auf die externe Signal-DB: Nachrichten im Importfenster, Conversation- und Kontaktdaten, Anhänge sowie die dreistufige Zitat-Auflösung (per Message-Id, Timestamp, EditHistory). Verbindung wird von außen übergeben.
- **Öffentlich:**
  - `forEachMessageInWindow(Connection signal, String lastSourceId, long cutoffMs, ThrowingConsumer<ResultSet> consumer): void`
  - `loadConversation(Connection signal, String conversationId): ConversationInfo`
  - `loadContact(Connection signal, String serviceId): ContactInfo`
  - `loadAttachments(Connection signal, String signalMsgId): List<AttachmentInfo>`
  - `resolveQuoteByMessageId(Connection signal, String messageId): String`
  - `resolveQuoteByTimestamp(Connection signal, long quoteId): String`
  - `resolveQuoteByEditHistory(Connection signal, long quoteId): String`
- **Kennt:** app.messaging.signal.model (AttachmentInfo, ContactInfo, ConversationInfo), app.shared.model.ThrowingConsumer, fremde Signal-SQLite-DB (über hereingereichte Connection).
- **Art:** Repository

## Paket app.messaging.whatsapp
Entschlüsselung und inkrementeller Import von WhatsApp-E2E-Backups (crypt15) in die ThosSuite-DB.

### WhatsAppCrypt15Decryptor
- **Rolle:** Entschlüsselt eine WhatsApp-crypt15-Backup-Datei (AES-256-GCM, zlib-komprimiert) mit dem 64-stelligen Hex-Schlüssel des Nutzers in eine lesbare SQLite-Datei. Einziger Einstiegspunkt ist `decrypt`.
- **Öffentlich:** `decrypt(String hexKey, Path inputFile, Path outputFile): void`
- **Kennt:** javax.crypto (AES/GCM, HMAC-SHA256), java.util.zip (Inflater). Keine Abhängigkeit zu anderen app-Paketen.
- **Art:** Hilfsklasse

### WhatsAppIncrementalImport
- **Rolle:** Steuert den täglichen inkrementellen WhatsApp-Import: prüft Fälligkeit und Hash-Änderung der Backup-Datei, lässt sie entschlüsseln, importiert neue Nachrichten inklusive Album-/Anhangs- und Zitat-Behandlung in einer Transaktion, verschiebt danach Anhangsdateien und zeigt eine Abschluss-Meldung. Konfiguration (Pfade, Schlüssel, Tagesgrenze) wird im Konstruktor aus `Config` gelesen.
- **Öffentlich:** `WhatsAppIncrementalImport()` (liest Konfiguration), `run(): void`
- **Kennt:** app.messaging.repository (MessageRepository), app.messaging.whatsapp (WhatsAppCrypt15Decryptor), app.messaging.whatsapp.repository (WhatsAppSourceRepository), app.shared (Config, DB, Log, ButtonEnum, Alerts, WhatsAppChatDialog, MessageContactDialog), fremde WhatsApp-crypt15-Datei und die daraus entschlüsselte SQLite-DB.
- **Art:** Sitzung/Ablauf

## Paket app.messaging.whatsapp.repository
Datenzugriffsschicht auf die entschlüsselte WhatsApp-SQLite-DB (read-only).

### WhatsAppSourceRepository
- **Rolle:** Kapselt die lesenden SQL-Zugriffe auf die entschlüsselte WhatsApp-DB: Iteration über neue Nachrichten inklusive Auflösung von Kontakt-Rohkennung, Chat- und Anhangsfeldern über mehrere Joins, sowie zweistufige Zitat-Auflösung (key_id, Fallback über Chat+Timestamp).
- **Öffentlich:**
  - `forEachMessage(Connection wa, long lastId, long lastTs, ThrowingConsumer<ResultSet> consumer): void`
  - `resolveQuote(Connection wa, String quoteKeyId, long quotingSourceId, int chatRowId, long timestamp): String`
- **Kennt:** app.shared.model.ThrowingConsumer, fremde WhatsApp-SQLite-DB (über hereingereichte Connection).
- **Art:** Repository

### Beobachtungen (unbewertet)
- SignalIncrementalImport (520 Zeilen) und WhatsAppIncrementalImport (559 Zeilen) sind die umfangreichsten Klassen der Gruppe.
- Beide Incremental-Import-Klassen (Signal, WhatsApp) implementieren Chat-Blacklisting, Kontakt-Dialog-Aufruf und Anhangs-Auflösung jeweils eigenständig; es gibt kein gemeinsames Interface oder eine gemeinsame Oberklasse dafür.
- WhatsAppCrypt15Decryptor hat als einzige Klasse der Gruppe keine Abhängigkeit zu anderen app-Paketen.

---

# Gesundheit und Tagesdaten

Sechs kleine, unabhängige Features rund um tägliche Gesundheits- und Alltagswerte: Fitbit-Punkte, Google-Health-Aktivität, Alkohol-Tracking, Tagebuch, Matratzen-Wenden und ein Wochentags-Rätsel.

## Paket app.fitbit
Import, Punkteberechnung und Statistik-Anzeige für Fitbit-Schritte/Aktivitäten; orchestriert OAuth2-Zugriff auf die Fitbit-API und die Übernahme der Tagesdaten in die eigene Datenbank.

### ActivityTablePresenter
- **Rolle:** Öffnet den Tabellendialog zur manuellen Korrektur der Activities eines Tages und übersetzt zwischen den Fitbit-JSON-Objekten und den generischen Tabellenzeilen der geteilten UI.
- **Öffentlich:**
  - `ActivityTablePresenter(LocalDate date, List<Activity> activities, ActivityDaySummary daySummary)`
  - `showAndWait(): DialogResult`
  - `record DialogResult(List<Activity> activities, int totalSteps)`
- **Kennt:** app.fitbit.model.json (Activity, ActivityDaySummary), app.shared.model.ActivityTableRow, app.shared.ui.ActivityTableDialog (JavaFX-Dialog)
- **Art:** Presenter/Controller

### ApiClient
- **Rolle:** Kommunikation mit der Fitbit-API inklusive OAuth2-Token-Verwaltung (Laden, Prüfen, automatisches Erneuern und Zurückschreiben der Credentials-Datei).
- **Öffentlich:**
  - `ApiClient()`
  - `getActivityDaySummary(LocalDate date): ApiResponse<ActivityDaySummary>`
  - `getActivitiesLogList(LocalDate date): ApiResponse<ActivityLogList>`
  - `record ApiResponse<T>(T data, String originalJson)`
- **Kennt:** Fitbit-API (HTTP), app.fitbit.model.json, app.shared (Config, Log). Enthält Zugangsdaten-Zugriff (Credentials-Datei, Client-Secret aus Config).
- **Art:** Dienst

### DashboardService
- **Rolle:** Berechnet abgeleitete Dashboard-Kennzahlen: verbleibende Tagesschritte zum Wochenziel, aktuellen und größten Wochen-Streak.
- **Öffentlich:**
  - `DashboardService()`
  - `calculateRemainingDailySteps(LocalDate today): int`
  - `calculateCurrentStreak(LocalDate today): int`
  - `calculateRecordStreak(): int`
- **Kennt:** app.fitbit.repository.Repository, app.fitbit.model.WeekData, PointsCalculator (gleiches Paket)
- **Art:** Dienst

### DataFetcher
- **Rolle:** Holt synchron alle seit dem letzten Import fehlenden Fitbit-Tagesdaten von der API und hält sie für die spätere Anzeige/Speicherung vor.
- **Öffentlich:**
  - `DataFetcher()`
  - `fetch(): void`
  - `hasData(): boolean`
  - `getFetchedDays(): List<DayData>`
  - `getProjection(): List<FitbitDayProjection>` — schlanke, bewusst öffentlich gehaltene Projektion der Rohdaten für einen externen Abnehmer außerhalb dieses Pakets; laut Kommentar ein befristetes Übergangsgerüst.
- **Kennt:** app.fitbit.repository.Repository, ApiClient (gleiches Paket), app.shared.Log
- **Art:** Dienst/Ablauf

### DataReviewService
- **Rolle:** Zeigt je abgeholtem Tag den Korrekturdialog, berechnet danach die Punkte und speichert Ergebnis sowie Rohdaten-Log; fasst am Ende eine Zusammenfassung für den User zusammen.
- **Öffentlich:**
  - `DataReviewService(DataFetcher dataFetcher)`
  - `showDialogsAndSave(): void`
- **Kennt:** DataFetcher, ActivityTablePresenter, PointsCalculator (gleiches Paket), app.fitbit.repository.Repository, app.fitbit.model.json.ActivityLogList, app.shared (Log, model.ButtonEnum, ui.Alerts)
- **Art:** Dienst/Ablauf

### FitbitDayProjection
- **Rolle:** Unveränderliche, öffentliche Projektion (Datum, Schritte, Aktivitäten) der von DataFetcher geholten Rohdaten eines Tages für einen externen Abnehmer; laut Kommentar Übergangsgerüst.

### FitbitStatisticsPresenter
- **Rolle:** Framework-freie Datenaufbereitung für den Statistik-Screen: rundet den Zeitraum auf ganze Wochen, ordnet je Woche Ziel und Zustand zu und liefert eine reine Balkendiagramm-Beschreibung.
- **Öffentlich:**
  - `get(LocalDate from, LocalDate to): BarChartData` (implementiert `BarChartDataProvider`)
- **Kennt:** app.fitbit.repository.Repository, app.fitbit.model (GoalHistoryEntry, WeekData), app.shared.model.BarChartData (und Bar, State, TargetLine, YAxis), app.shared.model.BarChartDataProvider, app.shared.Log
- **Art:** Presenter/Controller

### FitbitStatisticsScreen
- **Rolle:** Bindet FitbitStatisticsPresenter an die geteilte Balkendiagramm-View und stellt den Screen für die Suite bereit.
- **Öffentlich:**
  - `FitbitStatisticsScreen()`
  - `getView(): ScreenView`
  - `refresh(): void`
  - `getSwitchStrategy(): SessionSwitchStrategy` (implementiert `Screen`)
- **Kennt:** FitbitStatisticsPresenter (gleiches Paket), app.shared.model (Screen, ScreenView, SessionSwitchStrategy), app.shared.ui.BarChartScreenView (JavaFX)
- **Art:** View

### PointsCalculator
- **Rolle:** Berechnet aus Activities und Tageszusammenfassung die Tagespunkte nach festem Regelwerk je Aktivitätstyp (Bike, Spinning, Workout, Sport, Walk, Outdoor Bike, unbekannt).
- **Öffentlich:**
  - `static getDayPoints(ActivityLogList activities, ActivityDaySummary daySummary): int`
  - `static final double POINTS_FOR_STEP`
- **Kennt:** app.fitbit.model.json (Activity, ActivityDaySummary, ActivityLogList), app.shared (Log, model.ButtonEnum, ui.Alerts)
- **Art:** Dienst

## Paket app.fitbit.model
Reine Records für aggregierte, bereits berechnete Fitbit-Werte (Wochenpunkte, Ziel-Historie).

### GoalHistoryEntry
- **Rolle:** Ein Wochenziel-Eintrag mit Gültigkeitsbeginn (record, 2 Felder).

### WeekData
- **Rolle:** Fitbit-Punkte einer Woche inklusive Anmerkung (record, 3 Felder).

## Paket app.fitbit.model.json
Gruppe von Jackson-gebundenen JSON-DTOs für die Fitbit-API-Antworten und die Credentials-Datei: `Activity`, `ActivityDaySummary`, `ActivityLogList`, `FitbitCredentials`, `Food`, `FoodDaySummary`, `LoggedFood`, `Parent` (leere gemeinsame Basisklasse), `Summary`. Überwiegend Getter/Setter-Halter ohne Fachlogik; `Activity` bildet eine Ausnahme mit einer `getDate()`-Ableitung und einer Validierung in `setDistanceUnit()`. `FitbitCredentials` enthält Zugangsdaten-Zugriff (Access-/Refresh-Token-Felder).

## Paket app.fitbit.repository

### Repository
- **Rolle:** Persistiert Fitbit-Tagespunkte und Wochenziel-Historie in der Datenbank, liest sie für Dashboard/Statistik zurück und schreibt zusätzlich ein Roh-JSON-Log der API-Antworten in eine Datei.
- **Öffentlich:**
  - `getLastImportedDate(): LocalDate`
  - `saveDayPoints(LocalDate date, int points): void`
  - `getWeeklyGoalForDate(LocalDate date): int`
  - `getPointsForWeek(LocalDate date): int`
  - `getWeeksInRange(LocalDate from, LocalDate to): List<WeekData>`
  - `getAllGoalHistory(): List<GoalHistoryEntry>`
  - `logApiResponse(LocalDate date, String jsonResponse): void`
- **Kennt:** app.fitbit.model (GoalHistoryEntry, WeekData), app.shared (DB, Config, Log), Dateisystem (Log-Datei)
- **Art:** Repository

## Paket app.activity
Import von Schritten und Aktivitäten aus Googles Health-API. Enthält (anders als die übrigen fünf Features) weder eigenes Modell-/Persistenz-Repository noch einen Presenter/Screen in diesem Paket — nur den API-Client.

### ApiClient
- **Rolle:** Kommunikation mit der Google-Health-API (Schritte je Kalendertag, Aktivitätsliste) inklusive einmaligem OAuth2-Refresh pro Instanz; persistiert nichts selbst.
- **Öffentlich:**
  - `ApiClient()`
  - `fetchDailySteps(LocalDate from, LocalDate to): Map<LocalDate, Integer>`
  - `fetchActivities(LocalDate from, LocalDate to): List<Exercise>`
- **Kennt:** Google-Health-API (HTTP), app.activity.model.Exercise, app.shared.Config. Enthält Zugangsdaten-Zugriff (Client-ID/-Secret und Refresh-Token aus Config).
- **Art:** Dienst

## Paket app.activity.model

### Exercise
- **Rolle:** Unveränderliches Abbild einer Google-Health-Aktivität in Googles Originaleinheiten; bietet daneben zwei abgeleitete Werte (lokaler Kalendertag, Distanz in km) (record, 5 Felder plus zwei Ableitungsmethoden).

## Paket app.alc
Alkohol-Tracking: tägliche Ampel-Abfrage, Punkte-/Bilanzberechnung nach historischen Ratios und Statistik-Anzeige.

### AlcStatisticsPresenter
- **Rolle:** Framework-freie Datenaufbereitung für den Statistik-Screen: lädt Tageswerte im Zeitraum, ordnet je Tag die gültige Ratio zu und leitet aus dem Vorzeichen der Bilanz den Zustand ab.
- **Öffentlich:**
  - `get(LocalDate from, LocalDate to): BarChartData` (implementiert `BarChartDataProvider`)
- **Kennt:** app.alc.repository.AlcRepository, app.alc.model (DayEntry, RatioEntry), app.shared.model.BarChartData (und Bar, State, YAxis), app.shared.model.BarChartDataProvider, app.shared.Log
- **Art:** Presenter/Controller

### AlcStatisticsScreen
- **Rolle:** Bindet AlcStatisticsPresenter an die geteilte Balkendiagramm-View und stellt den Screen bereit.
- **Öffentlich:**
  - `AlcStatisticsScreen()`
  - `getView(): ScreenView`
  - `refresh(): void`
  - `getSwitchStrategy(): SessionSwitchStrategy` (implementiert `Screen`)
- **Kennt:** AlcStatisticsPresenter (gleiches Paket), app.shared.model (Screen, ScreenView, SessionSwitchStrategy), app.shared.ui.BarChartScreenView
- **Art:** View

### StartupService
- **Rolle:** Fragt beim Start fehlende Tage seit dem letzten Eintrag nacheinander per Ampel-Dialog ab und speichert die Antworten; bricht bei Abbruch durch den User ab.
- **Öffentlich:**
  - `StartupService()`
  - `checkAndPrompt(): void`
- **Kennt:** app.alc.repository.AlcRepository, app.alc.model.Status, app.shared (Log, model.ButtonEnum, ui.Alerts)
- **Art:** Sitzung/Ablauf

## Paket app.alc.model

### DayEntry
- **Rolle:** Ein Tag mit Status und berechneter Bilanz (record, 3 Felder).

### RatioEntry
- **Rolle:** Ein Satz Punktwerte für Grün/Gelb/Rot mit Gültigkeitsbeginn (record, 4 Felder).

### Status
- **Rolle:** Ampel-Zustand eines Tages (Enum: GREEN, YELLOW, RED).

## Paket app.alc.repository

### AlcRepository
- **Rolle:** Persistiert Tagesstatus und Ratio-Historie und berechnet die laufende Bilanz (aktuell, bis zu einem Stichtag oder für einen Zeitraum) aus Status und jeweils gültiger Ratio.
- **Öffentlich:**
  - `getDaysInRange(LocalDate from, LocalDate to): List<DayEntry>`
  - `saveDayStatus(LocalDate date, Status status): void`
  - `getCurrentRatio(): RatioEntry`
  - `getAllRatios(): List<RatioEntry>`
  - `getLastEntryDate(): LocalDate`
  - `getCurrentBalance(): int`
- **Kennt:** app.alc.model (DayEntry, RatioEntry, Status), app.shared (DB, AppClock, Log)
- **Art:** Repository

## Paket app.diary
Tagebuch mit Text, Tags und Bild-Anhängen: Editor-Dialog mit Anhang-/Thumbnail-Verwaltung und ein Such-Screen mit eigener kleiner Abfragesprache.

### DiaryEditorPresenter
- **Rolle:** Öffnet den Editor-Dialog für neuen oder bestehenden Eintrag, verdrahtet Speichern/Löschen, kopiert bzw. verknüpft Bild-Anhänge samt Thumbnail-Erzeugung und bestimmt anhand einer Zeitregel, ob der Dialog invasiv (mandatorisch) erscheint.
- **Öffentlich:**
  - `showNew(): void`
  - `showEdit(DiaryCardData clicked): void`
- **Kennt:** app.diary.repository.Repository, app.shared (Config, ImageUtils, model.DiaryAttachment, model.DiaryCardData, model.InvasiveConfig, ui.DiaryEditor), Dateisystem (Attachments-/Thumbnail-Ordner), Bildbibliotheken (javax.imageio, org.imgscalr)
- **Art:** Presenter/Ablauf

### DiaryScreen
- **Rolle:** Sucht Einträge über Freitext/Tag-Ausdruck und Zeitraum und zeigt sie als Karten; öffnet bei Klick den Editor. Enthält als private innere Klasse `QueryParser`, einen rekursiven-Abstiegs-Parser, der die Sucheingabe (mit `and`/`or`/Klammern/`tag:`) in ein SQL-WHERE-Fragment übersetzt.
- **Öffentlich:**
  - `DiaryScreen()`
  - `getView(): ScreenView`
  - `refresh(): void`
  - `getSwitchStrategy(): SessionSwitchStrategy` (implementiert `Screen`)
- **Kennt:** app.diary.model.Entry, app.diary.repository.Repository, DiaryEditorPresenter (gleiches Paket), app.shared (Config, model.DiaryAttachment, model.DiaryCardData, model.Screen, model.ScreenView, model.SessionSwitchStrategy, ui.DiaryScreenView)
- **Art:** View

## Paket app.diary.model

### Entry
- **Rolle:** Ein geladener Tagebucheintrag mit Text, Tags und Anhangspfaden (record, 5 Felder).

## Paket app.diary.repository

### Repository
- **Rolle:** Persistiert Einträge, Tags und Anhänge über mehrere zusammengehörige Tabellen (Eintrag, Tag, Zuordnung, Anhang) und liefert die Volltext-/Tag-Suche anhand eines vorgefertigten WHERE-Fragments.
- **Öffentlich:**
  - `saveEntry(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags): void`
  - `loadAllTags(): List<String>`
  - `findLastEntryTimestamp(): LocalDateTime`
  - `updateEntry(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags): void`
  - `search(String whereFragment, LocalDate from, LocalDate to, int limit): List<Entry>`
  - `deleteEntry(LocalDateTime createdAt): void`
  - `loadAttachments(LocalDateTime createdAt): List<String>`
  - `saveAttachment(LocalDateTime createdAt, String relativePath): void`
  - `deleteAttachment(LocalDateTime createdAt, String relativePath): void`
  - `isPathReferencedElsewhere(String relativePath): boolean`
- **Kennt:** app.diary.model.Entry, app.shared.DB
- **Art:** Repository

## Paket app.mattress
Erinnerung ans Matratzenwenden: prüft Fälligkeit, schlägt die Wenderichtung vor und speichert die Ausführung.

### MattressTurnDialog
- **Rolle:** Zeigt bei Fälligkeit (oder auf Anfrage) einen Dialog mit vorgeschlagener Wenderichtung inklusive Bild und speichert je nach Antwort die gewählte oder die andere Richtung.
- **Öffentlich:**
  - `showIfDue(): void`
  - `show(): void`
- **Kennt:** app.mattress.repository.MattressRepository, app.mattress.model.MattressTurn, app.shared (Config, Log, model.AlertOptions, model.ButtonEnum, ui.Alerts)
- **Art:** Sitzung/Ablauf

## Paket app.mattress.model

### MattressTurn
- **Rolle:** Ein Wende-Ereignis mit Zeitpunkt und Richtung (record, 2 Felder).

## Paket app.mattress.repository

### MattressRepository
- **Rolle:** Persistiert Wende-Ereignisse und berechnet aus dem letzten Eintrag, wie viele Tage bis zur nächsten Fälligkeit verbleiben.
- **Öffentlich:**
  - `getLastTurn(): MattressTurn`
  - `save(LocalDateTime turnedAt, String direction): void`
  - `getDaysUntilNextTurn(): long`
- **Kennt:** app.mattress.model.MattressTurn, app.shared (DB, Config)
- **Art:** Repository

## Paket app.weekday
Übungs-/Tages-Rätsel „Wochentag berechnen“: stellt ein zufälliges Datum in wechselndem Format dar, wertet die Antwort aus und führt eine Trefferstatistik.

### WeekdayDialog
- **Rolle:** Stellt ein zufälliges Datum (Format zufällig gewählt) als Wochentags-Rätsel dar, misst die Antwortzeit, wertet richtig/falsch aus und speichert das Ergebnis (nur im Tagesmodus, nicht im Übungsmodus).
- **Öffentlich:**
  - `showForDaily(): void`
  - `showForPractice(): void`
- **Kennt:** app.weekday.repository.WeekdayRepository, app.shared (Log, model.AlertOptions, model.ButtonEnum, ui.Alerts)
- **Art:** Sitzung/Ablauf

## Paket app.weekday.model

### WeekdayStats
- **Rolle:** Aktueller und größter Trefferstreak (record, 2 Felder).

## Paket app.weekday.repository

### WeekdayRepository
- **Rolle:** Speichert je Rätselversuch das Ergebnis, prüft ob heute schon gespielt wurde und berechnet aktuellen sowie maximalen Streak aus dem Verlauf.
- **Öffentlich:**
  - `playedToday(): boolean`
  - `save(LocalDate puzzleDate, int secondsNeeded): void`
  - `getWeekdayStats(): WeekdayStats`
- **Kennt:** app.weekday.model.WeekdayStats, app.shared.DB
- **Art:** Repository

### Beobachtungen (unbewertet)
- Fünf der sechs Features (fitbit, alc, diary, mattress, weekday) folgen dem Muster Dienst/Presenter + Modell + Repository mit eigenem Zugriff auf app.shared.DB; app.activity hat weder Repository noch Persistenz, nur einen API-Client und einen Modell-Record.
- Fitbit und Activity greifen beide auf eine externe API zu (Fitbit-API bzw. Google-Health-API) und lesen ihre Zugangsdaten aus app.shared.Config; die übrigen vier Features kommen ohne externe API aus.
- Nur fitbit und alc besitzen einen Statistik-Presenter, der BarChartDataProvider implementiert und einen eigenen Screen speist; diary hat einen Screen ohne Chart, mattress und weekday haben je nur einen Dialog ohne Screen.
- app.fitbit.model.json ist das einzige Modellpaket dieser Gruppe mit Jackson-gebundenen JSON-DTOs (Getter/Setter-Klassen); die Modellpakete der anderen fünf Features bestehen ausschließlich aus Records und einem Enum.

---

## Paket app (Wurzel)
Enthält den JavaFX-Einstiegspunkt der Suite: Startsequenz, Splashscreen, Fehlerbehandlung auf oberster Ebene.

### ThosSuiteApp
- **Rolle:** JavaFX-`Application`-Klasse und Programmeinstieg. Prüft Zeitzone, ermittelt den Datenordner, sichert die Single-Instance, initialisiert Log/Config/DB/Fonts im Hintergrundthread, baut Splashscreen und Hauptfenster auf und erzeugt den `Controller`. Registriert außerdem den globalen Uncaught-Exception-Handler und räumt beim Beenden auf (DB schließen).
- **Öffentlich:**
  - `static void main(String[] args)`
  - `void init() throws Exception`
  - `void start(Stage primaryStage)`
  - `void stop() throws Exception`
- **Kennt:** `app.controller` (`Controller`, `MainWindow`), `app.shared` (`AppClock`, `Config`, `DB`, `FilenIgnoreSource`, `Log`, `SingleInstanceGuard`), JavaFX-`Application`/`Stage`-API.
- **Art:** Sitzung/Ablauf

## Paket app.controller
Orchestrierungsschicht: verdrahtet das Hauptfenster mit den Feature-Diensten, hält die aktuell laufende Sitzung (`currentScreen`) und steuert den Start- sowie Session-Wechsel-Ablauf.

### Controller
- **Rolle:** Zentraler Knotenpunkt der Anwendung. Verbindet Menü-/Tastatur-Events des `MainWindow` mit den Feature-Modulen, verwaltet die aktuell aktive `Screen`-Sitzung samt Wechsel-Logik (Speichern/Verwerfen/Abbrechen) und führt die zweigeteilte Startsequenz aus (`runPreTasks` vor, `runPostTasks` nach Anzeige des Hauptfensters).
- **Öffentlich:**
  - `Controller(MainWindow mainWindow) throws InterruptedException`
  - `void runPreTasks()`
  - `void runPostTasks()`
  - `void requestSessionSwitch(Runnable startNewSessionRoutine)`
  - `void onLearnMenuItemSelected(LearnSessionInfo info)`
  - `void onPlayMenuItemSelected(PlayMenuNode item)`
  - `void onStatisticsMenuItemSelected(String item)`
  - `void sessionEnded()`
  - `void closeSelected()`
  - `void newSkinSelected(Skin newSkin)`
  - `void diaryCreateSelected()`
  - `void diaryViewSelected()`
  - `void movieSelected()`
  - `void exportSelected()`
  - `void additionalTmdbImportSelected()`
  - … (7 weitere: `saveMenuItemSelected`, `escPressed`, `pausePressed`, `enterPressed`, `sortOrderChanged`, `weekdaySelected`, `mattressSelected`)
- **Kennt:** `app.controller.model`, `app.controller` (`MainWindow`, `DashboardScreen`, `StartScreen`, `SuiteExporter`), `app.tmp.Comparison`, sowie die Feature-Pakete `app.alc`, `app.diary`, `app.fitbit`, `app.learn` (inkl. `app.learn.anki`, `app.learn.region`), `app.mattress`, `app.messaging.signal`, `app.messaging.whatsapp`, `app.movie`, `app.weekday`. Dazu `app.shared` (`Config`, `Log`, `UiUtils`, `model.ButtonEnum`, `model.Screen`, `skin.Skin`, `skin.SkinImageCache`, `skin.SkinService`, `ui.Alerts`).
- **Art:** Presenter/Controller

### DashboardScreen
- **Rolle:** Baut die Dashboard-Kachelansicht: holt Kennzahlen aus mehreren Feature-Repositories (Fitbit, Alkohol, Wochentag, Matratze, Nachrichten, Anki) und übergibt sie gesammelt an die zugehörige `ScreenView`. Implementiert `Screen`, ist also eine der wählbaren "Sitzungen" des Controllers.
- **Öffentlich:**
  - `DashboardScreen()`
  - `ScreenView getView()`
  - `void refresh()`
  - `SessionSwitchStrategy getSwitchStrategy()`
- **Kennt:** `app.alc.repository.AlcRepository`, `app.fitbit.DashboardService`, `app.learn.anki.repository.DeckRepository`, `app.mattress.repository.MattressRepository`, `app.messaging.repository.MessageRepository`, `app.weekday.model.WeekdayStats`, `app.weekday.repository.WeekdayRepository`, `app.shared` (`AppClock`, `Config`, `model.DashboardTileData`, `model.Screen`, `model.ScreenView`, `model.SessionSwitchStrategy`, `ui.DashboardScreenView`).
- **Art:** Sitzung/Ablauf

### MainWindow
- **Rolle:** Kapselt die Stage der Anwendung: Fensteraufbau (BorderPane, ContentPane, HeaderBar), Menüleiste (Datei/Optionen/Lernen/Spielen/Statistik/Module/Ansicht), globale Tastaturkürzel (Esc/Enter/Pause) sowie das Umschalten des angezeigten `ScreenView`-Inhalts. Nimmt vom Controller Callback-Runnables/Consumer entgegen, statt selbst Fachlogik zu enthalten.
- **Öffentlich:**
  - `MainWindow(Stage stage)`
  - `void buildStyledUi()`
  - `void showScreenView(ScreenView view)`
  - `void setPlayItems(List<PlayMenuNode> nodes)`
  - `void setLearnItems(List<LearnSessionInfo> infoList)`
  - `void addLearnItems(List<LearnSessionInfo> infoList)`
  - `void updateLearnItems(List<LearnSessionInfo> infoList)`
  - `void setCurrentSortOrder(CardSortOrder sortOrder)`
  - `void show()`
  - `void centerOnScreen()`
  - `Window getStage()`
  - `ObservableList<Image> getIcons()`
  - `void setWidth(int width)`
  - `void setHeight(int height)`
  - … (17 weitere: überwiegend `setXxxRunnable`/`setXxxConsumer`-Methoden zur Registrierung der Controller-Callbacks, z. B. `setCloseRunnable`, `setQuitRunnable`, `setSkinChangeConsumer`, `setDiaryCreateRunnable`, `setStatisticsConsumer`)
- **Kennt:** `app.controller.model.PlayMenuNode`, `app.learn.anki.model.CardSortOrder`, `app.learn.model.LearnSessionInfo`, `app.shared` (`Config`, `Log`, `model.ScreenView`, `ui.MainWindowHeaderBar`, `skin.Skin`, `skin.SkinService`), JavaFX-Scene-Graph-API.
- **Art:** View

### StartScreen
- **Rolle:** Leerer Platzhalter-Bildschirm, aktiv wenn keine Lern-/Spiel-Sitzung läuft. Darf jederzeit ohne Rückfrage einem anderen `Screen` weichen. Paketprivat, nur über den Controller erreichbar.
- **Öffentlich:** `SessionSwitchStrategy getSwitchStrategy()`, `void refresh()`, `ScreenView getView()` (Klasse selbst ist paketprivat, keine öffentliche Instanziierung von außen).
- **Kennt:** `app.shared` (`model.Screen`, `model.ScreenView`, `model.SessionSwitchStrategy`, `ui.StartScreenView`).
- **Art:** Sitzung/Ablauf

### SuiteExporter
- **Rolle:** Sichert die gesamte Suite querschnittlich: sammelt seit einem im Dialog gewählten Stichtag geänderte Dateien unterhalb des Wurzelordners (unter Beachtung einer Ignore-Regeldatei) und legt sie als AES-verschlüsseltes Zip im OneDrive-Ordner ab.
- **Öffentlich:**
  - `SuiteExporter()`
  - `void export()`
- **Kennt:** `app.shared` (`Config`, `Log`, `model.ButtonEnum`, `ui.Alerts`, `ui.DatePickerDialog`), Fremdbibliothek `net.lingala.zip4j` (AES-Zip).
- **Art:** Dienst

## Paket app.controller.model
Datentypen für das "Spielen"-Menü: welche Einträge es gibt und was sie beim Auswählen an den Controller zurückgeben.

### AnkiPlayItem
- **Rolle:** Menüeintrag für ein einzelnes Anki-Deck; trägt Anzeigetext und `Deck`-Typ. Reine Datenklasse (record), implementiert `PlayMenuNode`.
- **Kennt:** `app.learn.model.Deck`.
- **Art:** Modell/Datenhalter

### PlayMenuNode
- **Rolle:** Sealed Interface, das die zwei möglichen "Spielen"-Menüeinträge (`AnkiPlayItem`, `RegionPlayItem`) als geschlossene Menge definiert; gibt nur den Anzeigetext (`label()`) vor.
- **Kennt:** keine externen Pakete (nur die eigenen permits-Implementierungen).
- **Art:** Modell/Datenhalter

### RegionPlayItem
- **Rolle:** Sammeleintrag für die Region-Decks; trägt kein Deck, die eigentliche Deck-Auswahl passiert erst im nachgelagerten Dialog. Reine Datenklasse (record), implementiert `PlayMenuNode`.
- **Kennt:** keine externen Pakete.
- **Art:** Modell/Datenhalter

## Paket app.tmp
Übergangs-Gerüst ohne festen fachlichen Zugehörigkeitsort, laut Kommentar in den Klassen befristet auf Wegfall.

### Comparison
- **Rolle:** Vergleicht pro importiertem Tag Fitbit-Rohwerte (Schritte, Rad-km) mit Health-Rohwerten und zeigt die Differenzen in einem Popup. Zweigeteilt für den Startup-Split: `fetch` läuft blockierend im PreTask, `showPopup` im PostTask.
- **Öffentlich:**
  - `void fetch(List<FitbitDayProjection> fitbitDays)`
  - `void showPopup()`
- **Kennt:** `app.activity` (`ApiClient`, `model.Exercise`), `app.fitbit` (`FitbitDayProjection`, `model.json.Activity`), `app.tmp.HealthImportLog`, `app.shared` (`model.ButtonEnum`, `ui.Alerts`).
- **Art:** Dienst

### HealthImportLog
- **Rolle:** Schreibt die geholten Health-Tagesdaten eingedampft als JSON-Zeile in eine eigene Logdatei (`health_import.log`), symmetrisch zum Fitbit-Log, zur Rückverfolgung von Rad-km-Diskrepanzen.
- **Öffentlich:**
  - `void write(LocalDate date, Integer steps, List<Exercise> activities)`
- **Kennt:** `app.activity.model.Exercise`, `app.shared.Config`, Fremdbibliothek `com.fasterxml.jackson` (JSON-Serialisierung).
- **Art:** Hilfsklasse

### Startablauf
1. `ThosSuiteApp.main` setzt die Locale auf Deutschland und ruft `Application.launch()`; JavaFX ruft anschließend `init()` und `start()` auf.
2. `ThosSuiteApp.init()` initialisiert `AppClock` und registriert den globalen Uncaught-Exception-Handler.
3. `ThosSuiteApp.start()` prüft die Systemzeitzone gegen Europe/Berlin (Warn-Alert bei Abweichung, Abbruchmöglichkeit).
4. `ThosSuiteApp` zeigt einen transparenten Splashscreen (eigene Stage) an.
5. `ThosSuiteApp` ermittelt den Datenordner (Programmargument oder `DirectoryChooser`), prüft die Single-Instance-Sperre und entfernt verwaiste Log-Locks.
6. `ThosSuiteApp` initialisiert in einem Hintergrundthread Log, Config, DB-Vorbereitung, `FilenIgnoreSource` und lädt die Fonts.
7. Zurück im UI-Thread baut `ThosSuiteApp.initializeMainWindow` das `MainWindow` auf (Icons, `buildStyledUi()` inkl. Skin-Laden); die Stage bleibt zunächst über Opacity 0 unsichtbar.
8. `Controller`-Konstruktor läuft: registriert alle Callback-Runnables/Consumer am `MainWindow`, zeigt den `StartScreen`, erstellt `AnkiDeckService` und `RegionDeckService`, wärmt Kartenbilder vor und setzt die Lern-/Spielen-Menü-Beschriftungen.
9. `Controller.runPreTasks()` läuft noch vor dem sichtbaren Hauptfenster (Splash steht): Fitbit-Abruf, TMDB-Movie-Import und (Paket `app.tmp`) `Comparison.fetch`; Fehler der drei werden nur gemerkt, nicht gemeldet.
10. `ThosSuiteApp` zeigt das `MainWindow` (`show()`, `centerOnScreen()`) und wartet 500 ms, damit das CSS des Skins vollständig angewendet ist, während der Splash weiter sichtbar bleibt.
11. Splash wird geschlossen; `Controller.runPostTasks()` läuft: Owner-Fenster für Dialoge setzen, gesammelte Import-Fehler als eine Meldung zeigen, Fitbit-Review-Dialoge, `Comparison.showPopup()`, Alkohol-Startup-Prüfung, neuer Tagebucheintrag-Dialog, Wochentags-Dialog, Matratzen-Dialog, Movie-Cleanup, Signal- und WhatsApp-Import, Bildskalierung.
12. `ThosSuiteApp` setzt die Opacity der Hauptstage auf 1 und holt sie in den Vordergrund — das Fenster ist fertig gestartet.

### Beobachtungen (unbewertet)
- Controller kennt und orchestriert 9 Feature-Domänen-Pakete direkt (alc, diary, fitbit, learn/anki, learn/region, mattress, messaging/signal, messaging/whatsapp, movie, weekday, tmp).
- app.tmp enthält 2 Klassen, deren Kommentare beide einen Wegfalltermin (September) nennen.
- app.controller.model besteht aus 3 Klassen: einem sealed Interface und zwei Records als dessen einzige Implementierungen.

---

## Paket app.shared
Basisdienste der Suite: Konfiguration, Datenbankverbindungen, Logging, Single-Instance-Sperre, Sound sowie Bildbearbeitung für Datei- und Anzeigezwecke. Alle Features setzen direkt oder indirekt darauf auf.

### AppClock
- **Rolle:** Hält das „heute"-Datum als beim Klassenladen eingefrorene Konstante für die gesamte Laufzeit.
- **Öffentlich:**
  - `static final LocalDate TODAY`
  - `static void init()`
- **Kennt:** nur `java.time`.
- **Art:** Hilfsklasse.

### Config
- **Rolle:** Zentrale Fassade für alle Suite-Werte. Routet pro Key zwischen der unveränderlichen Menge aus Datei/computed-Pfaden (`ConfigFileSource`) und den veränderlichen Laufzeitwerten in der Datenbank (`KeyValueRepository`); jede Typumwandlung (Int, Path, LocalDateTime, Tage-seit) passiert hier zentral. Initialisiert beim Start in fester Reihenfolge auch `DB`. Schreiben auf einen unveränderlichen Key wirft (FailFast).
- **Öffentlich:**
  - `static void init(String folderPath)`
  - `static String get(String key)`
  - `static String getString(String key)`
  - `static String get(String key, String defaultValue)`
  - `static int getInt(String key)`
  - `static int getInt(String key, int defaultValue)`
  - `static Path getPath(String key)`
  - `static LocalDateTime getTime(String key)`
  - `static int getDaysSince(String key)`
  - `static void set(String key, String value)`
  - `static void setInt(String key, int value)`
  - `static void setTime(String key, LocalDateTime time)`
  - `static void delete(String key)`
- **Kennt:** `ConfigFileSource`, `KeyValueRepository`, `DB` (initialisiert sie mit den daraus abgeleiteten Pfaden).
- **Art:** Dienst (Fassade).

### ConfigFileSource
- **Rolle:** Liest `config/config.txt` einmalig beim Start ein und hält deren Rohwerte plus die daraus abgeleiteten (computed) Ordnerpfade der Suite. Read-only nach der Konstruktion.
- **Öffentlich:** package-private, nur für `Config` sichtbar — `boolean contains(String key)`, `String get(String key)` (wirft bei fehlendem Key).
- **Kennt:** Dateisystem (`config/config.txt`).
- **Art:** Modell/Datenhalter.

### DB
- **Rolle:** Hält die Singleton-Connections zu den beiden SQLite-Datenbanken der Suite (ThosSuite-DB und Film-DB) sowie separate, nicht-transaktionale Frisch-Connections für Bulk-Schreiboperationen.
- **Öffentlich:**
  - `static void init(Path dbPath, Path tmdbDbPath)`
  - `static Connection getConnection()`
  - `static Connection getNewConnection()`
  - `static Connection getTmdbConnection()`
  - `static Connection getNewTmdbConnection()`
  - `static void closeConnection()`
- **Kennt:** SQLite/JDBC über `DriverManager`.
- **Art:** Dienst.

### FilenIgnoreSource
- **Rolle:** Pflegt einen Eintrag in einer `.filenignore`-Datei für den Cloud-Sync, gesteuert über zwei Config-Werte (Pfad, einzutragende Zeile). Ohne gesetzte Werte passiert nichts.
- **Öffentlich:**
  - `static void addToIgnore() throws Exception`
  - `static void removeFromIgnore() throws Exception`
- **Kennt:** `Config`, Dateisystem.
- **Art:** Dienst.

### ImageUtils
- **Rolle:** Rechnet auf `BufferedImage` (Java2D): Skalieren in zwei Varianten, Umwandlung nach RGB ohne Alpha-Kanal, Abmessungen aus Rohdaten. Für Dateiverarbeitung gedacht, nicht für Bildschirmanzeige (dafür `UiUtils`).
- **Öffentlich:**
  - `static BufferedImage scaleSmooth(BufferedImage img, int width, int height, Scalr.Method methode, BufferedImageOp... ops)`
  - `static BufferedImage scaleStepwise(BufferedImage img, int width, int height)`
  - `static BufferedImage toRgb(BufferedImage img)`
  - `static int[] dimensions(byte[] imageData)`
- **Kennt:** `java.awt`/`ImageIO`, Fremdbibliothek `org.imgscalr`.
- **Art:** Hilfsklasse.

### KeyValueRepository
- **Rolle:** Liest und schreibt die `key_values`-Tabelle für persistente Laufzeitwerte (z. B. Import-Zeitpunkte, Datei-Hashes, UI-Präferenzen). Liefert und speichert ausschließlich rohe Strings; Typumwandlung macht die `Config`-Fassade. Throw-on-miss bei unbekanntem Key.
- **Öffentlich:** package-private, nur für `Config` sichtbar — `String get(String key)`, `void set(String key, String value)`, `void delete(String key)`, `List<String> allKeys()`.
- **Kennt:** `DB` (Connection), SQL.
- **Art:** Repository.

### Log
- **Rolle:** Zentrale Logging-Fassade auf Basis von `java.util.logging`. Konfiguriert beim Start File- und Console-Handler mit gemeinsamem Formatter (inkl. Log-Rotation) und stellt statische Methoden je Level bereit; unterdrückt bekannte Java-/JavaFX-interne Logger.
- **Öffentlich:**
  - `static void initLog(String dataFolder, Parameters params)`
  - `static boolean isInitialized()`
  - `static void debug(Object caller, String message)` / `static void debug(Class<?> callerClass, String message)`
  - `static void info(Object caller, String message)` / `static void info(Class<?> callerClass, String message)`
  - `static void warn(Object caller, String message)` / `static void warn(Class<?> callerClass, String message)`
  - `static void error(Object caller, String message, Throwable ex)` / `static void error(Object caller, String message)`
  - `static void error(Class<?> callerClass, String message, Throwable ex)` / `static void error(Class<?> callerClass, String message)`
- **Kennt:** `java.util.logging`, JavaFX (`Application.Parameters`).
- **Art:** Dienst.

### SingleInstanceGuard
- **Rolle:** Verhindert per Datei-Lock einen zweiten gleichzeitig laufenden Start der Suite; gibt den Lock über einen Shutdown-Hook wieder frei.
- **Öffentlich:**
  - `static boolean lockInstance(Path lockFile)`
- **Kennt:** `java.nio` (`FileChannel`/`FileLock`).
- **Art:** Dienst.

### Sound
- **Rolle:** Spielt kurze Klänge über `javax.sound` ab. Aufrufer nennen nur den Dateinamen; den `soundFolder` löst die Klasse selbst über `Config` auf. Clips werden beim ersten Zugriff geladen und danach gehalten.
- **Öffentlich:**
  - `static void play(String fileName)`
- **Kennt:** `Config` (soundFolder), `javax.sound.sampled`.
- **Art:** Dienst.

### UiUtils
- **Rolle:** JavaFX-Bildmanipulationen und UI-Hilfsfunktionen für die Anzeige — Owner-Fenster für Dialoge/Alerts, Farbkonvertierung (Hex) und -abwandlung (Kontrast-Ton), Bild-Tinting, Plus-Badge auf Bildern, Esc-Sperre für Alerts. Gegenstück zu `ImageUtils`, das auf Dateien statt Anzeige arbeitet.
- **Öffentlich:**
  - `static void setOwnerWindow(Window window)`
  - `static Window getOwnerWindow()`
  - `static String toHex(Color c)`
  - `static Image tintImage(Image source, Color tintColor)`
  - `static Color contrastingShade(Color c, int percent)`
  - `static void inactivateEscPress(Alert alert)`
  - `static Image addPlusSign(Image source, int targetWidth)`
- **Kennt:** JavaFX (`scene.control`, `scene.image`, `scene.paint`, `stage`), `Config` (iconFolder).
- **Art:** Hilfsklasse.

## Paket app.shared.model
Framework-weitgehend freie Modelle, Records, Enums und funktionale Interfaces, die als Austauschobjekte zwischen Features, Presentern/Controllern und den Skin-/View-Schichten dienen. Überwiegend kleine Datenhalter ohne eigenes Verhalten.

### UI-Bausteine und Skin-Werte
- **AlertOptions** — Fluent-Optionen für Alerts/Dialoge (zentriert, Esc-Sperre, Bild); Datenhalter für den Dialogaufbau.
- **BigComponentStyle** — Eckradius und Rahmenbreite großer Flächen (Bildkarte, Bildrahmen), muss zu den CSS-Maßen passen.
- **BorderParams** — vollständige Rahmenbeschreibung (Breite, Farbe, Insets, Bogen, Fokus-/Deaktiviert-Varianten) für Skin-gesteuerte Komponenten, mit Fabrikmethoden und Fallback-Farblogik.
- **ButtonEnum** — feste Menge deutschsprachiger Button-Beschriftungen (OK, Abbrechen, Wochentage, Import/Blacklist u. a.) für generische Dialoge.
- **DashboardTileStyle** — Breite und Gesamthöhe einer Dashboard-Kachel, wie sie der Skin braucht.
- **DialogStyle** — zweckgeschnittener Skin-Wert (Textfarbe) für den Dialog-/Alert-Pfad.
- **DismissEnum** — vier Kombinationen, ob ESC bzw. X-Knopf einen Dialog schließen dürfen.
- **McMetrics** — Maße für Multiple-Choice-Knöpfe (Schrift, Ränder, Zeilenabstand, Höhe, Abstand) für die Layoutberechnung der Auswahl-Ansicht.
- **MovieStyle** — Skin-Werte der Film-Oberfläche (Posterbreite, Schrift, Tooltip-Rand).
- **SelectionEnum** — sechsstufige Auswahl (ZERO..FIVE) mit Index-Konvertierung.

### Screen-Vertrag (Controller-Schnittstelle)
- **Screen** — Vertrag für jede bildschirmfüllende Fläche, die der Controller einheitlich behandelt (Lern-Sessions, Dashboard, Movie-Viewer u. a.); Default-Methoden für Lifecycle-Ereignisse wie Start, Refresh, Pause, Suspend/Resume, Sortierung, Schließen.
- **ScreenView** — minimaler Vertrag, der eine JavaFX-Pane liefert; Brücke zwischen Screen und Scene-Graph.
- **SessionSwitchStrategy** — Enum, wie der Controller beim Session-Wechsel vorgeht (sofort, Speichern anbieten, Verwerfen bestätigen).
- **SaveSelectionListener** — Ein-Methoden-Interface für den Menüpunkt „Speichern".

### Lern-Session-Callbacks und -Zustände
- **AnkiCallbacks** — gebündelte Rückmeldungen einer Lern-Session (Kartenklick, MC-Antwort, Texteingabe, Zurück, Absenden, Zeit abgelaufen) an ihren Presenter.
- **AnkiDialogState** — Rohzustand des Anki-Start-Dialogs (Min/Max/Kartenanzahl als Text, gewählte Labels); Parsen bleibt beim Feature.
- **RegionCallbacks** — reduzierte Variante von AnkiCallbacks für Region-Sessions (nur Kartenklick und Texteingabe).
- **RegionDialogState** — framework-freier Zustand des Region-Dialogs (Modus-Auswahl, Deck-Checkboxen inkl. Disabled-Status).
- **InvasiveConfig** — Schwellenwerte (Mindestzeichen, Mindest-Tags, Sekunden), ab wann der Tagebuch-Editor als „invasiv genutzt" gilt.

### Karten, Geometrie und Skizzen
- **ShapeGeometry** — unveränderliche Geometriebeschreibung einer Kartenform (Polygon, Linie, Kreis, Zentrieranker) inkl. id, Punkten in Bildschirmkoordinaten und Skalierungsmethode; dient Bild- und Shape-Karten sowie Skizzen gemeinsam.
- **ShapeMapState** — veränderlicher Spielstand einer Shape-Karte (korrekte/falsche/markierte/aktive/inaktive Ids, Interaktivität).
- **MapImages** — Bündel der vier Bild-Karten-Grafiken (Hintergrund/Overlay, je aktiv/inaktiv).
- **SketchColor** — die acht möglichen Füllfarben einer Skizze, mit Anzeigename und CSS-Style-Klasse.
- **SketchStructure** — Teilflächen und Leinwand einer Hintergrund-Skizze, wie aus der Strukturdatei gelesen.

### Feature-Datenhalter
- **ActivityTableRow** — framework-freie Zeile der editierbaren Aktivitätstabelle, mit einem opaken Durchreich-Feld für das aufrufende Feature.
- **CardData** — fertig aufbereitetes Anzeige-DTO für eine Viewer-Kachel (Filme/Serien/Episoden einheitlich), das der Skin rendert.
- **DashboardTileData** — Zahl-plus-Beschriftung einer Dashboard-Kachel; Grenzobjekt zwischen Controller und View.
- **DiaryAttachment** — Pfadpaar (Original, Thumbnail) eines Tagebuch-Anhangs.
- **DiaryCardData** — einziges Austauschobjekt der Diary-Feature-Grenze (Anzeige und Speichern/Löschen) mit Datum, Text, Tags, Anhängen.
- **BarChartData** — framework-freie Balkendiagramm-Beschreibung (Balken mit Zustand/Tooltip, optionale Ziellinie, Achsenverhalten) mit verschachtelten Records und Enum.
- **BarChartDataProvider** — Interface, das für einen Zeitraum eine BarChartData liefert; entkoppelt die View vom Feature.

### Funktionale Interfaces
- **ThrowingConsumer\<T\>** — Consumer-Variante, deren `accept()` eine geprüfte Exception werfen darf.

### Beobachtungen (unbewertet)
- `Config` wird von 27, `DB` von 18 und `Log` von 17 anderen Paketen importiert.
- `app.shared.model` wird von rund 20 Feature- und UI-Paketen quer durch die Suite importiert, u. a. alc, diary, fitbit, learn (inkl. anki/region), mattress, movie, messaging/signal, messaging/whatsapp, weekday sowie den shared-UI-Paketen.
- `app.shared.model` enthält sowohl generische UI-/Skin-Typen (BorderParams, ButtonEnum, DialogStyle, McMetrics) als auch feature-spezifische Datenhalter (DiaryCardData, CardData, AnkiCallbacks, RegionDialogState, ShapeGeometry).
- `ConfigFileSource` und `KeyValueRepository` sind package-private Kollaborateure von `Config`; `Config` ist die einzige nach außen sichtbare Zugriffsstelle für Konfigurationswerte.

---

## Paket app.shared.ui
Enthält die Oberflächen der Suite: ScreenViews, Lern-Ansichten und Dialoge. Framework-gebunden (JavaFX), holt Bausteine aus `shared.ui.components` und Werte aus `shared.skin`.

### ActivityTableDialog
- **Rolle:** Zeigt eine editierbare Tabelle in einem Dialog, der sich auf die Inhaltsgröße einschnappt. Fünf feste typisierte Spalten, DELETE entfernt löschbare Zeilen. Übersetzt zwischen `ActivityTableRow` und dem internen JavaFX-Zeilenmodell.
- **Öffentlich:**
  - `public List<ActivityTableRow> show(String title, List<ActivityTableRow> input)`
- **Kennt:** app.shared.model (ActivityTableRow), app.shared.ui.components (SuiteDialog, SuiteTabCommitTextFieldTableCell), JavaFX (TableView, Scene, Dialog, ScrollBar, …)
- **Art:** View

### Alerts
- **Rolle:** Zentraler Einstiegspunkt für Alert-Dialoge der gesamten Suite. Einzige Klasse, die `javafx.scene.control.ButtonType` kennt — nach außen ausschließlich `ButtonEnum`. Dismiss/X bedeutet immer `ButtonEnum.CANCEL`.
- **Öffentlich:**
  - `public static ButtonEnum show(String title, String message, ButtonEnum... buttons)`
  - `public static ButtonEnum show(String title, String message, AlertOptions options, ButtonEnum... buttons)`
- **Kennt:** app.shared (UiUtils), app.shared.model (AlertOptions, ButtonEnum, DialogStyle, DismissEnum), app.shared.skin (SkinService), app.shared.ui.components (SuiteHeaderBar), JavaFX (Alert, Stage, HeaderBar, …)
- **Art:** Dienst

### AnkiConfigDialog
- **Rolle:** Parametrisierter Dialog zur Konfiguration eines Anki-Durchlaufs — Index-Grenzen, maximale Kartenzahl, optionaler Label-Filter in Spalten. Liefert den eingestellten Zustand oder null bei Abbruch.
- **Öffentlich:**
  - `public AnkiConfigDialog(String title, String minDefault, String maxDefault, String maxCardsDefault, List<List<String>> labelColumns)`
  - `public AnkiDialogState showAndWait()`
- **Kennt:** app.shared.model (AnkiDialogState), app.shared.ui.components (SuiteDialog, SuiteTextField), JavaFX (CheckBox, Tooltip, …)
- **Art:** View

### AnkiLearnView
- **Rolle:** Gemeinsame Oberfläche aller Anki-Lernformen: Frage, Bild, Multiple-Choice, Fortschritt, Historie, Zurück-Knopf, optionales Eingabefeld. Übersetzt Presenter-Absicht in Anzeige; Unterklassen legen Karte, Eingabefeld und MC-Bedarf fest.
- **Öffentlich (Auszug, 26 Methoden):**
  - `protected AnkiLearnView(String deckId, String mapName, String category, AnkiCallbacks callbacks)`
  - `public void rebuild()`
  - `public ScreenView getView()`
  - `public void setQuestion(String text)`
  - `public void setImage(String imageName)`
  - `public void setProgress(String text)`
  - `public void setCardHistory(String text)`
  - `public void setSketch(SketchStructure sketch)`
  - `public void addSketch(List<ShapeGeometry> areas, int cell, double size, double offsetX, double offsetY)`
  - `public void markSketchAreas(List<Integer> areas)`
  - `public void fillSketchAreas(List<Integer> areas, SketchColor color)`
  - `public void setMultipleChoice(List<String> answers)`
  - `public void setMcCorrect(int id, boolean correct)`
  - `public void setMcMarked(int id, boolean marked)`
  - `public void setMcSolution(Set<Integer> correctIds)`
  - … (11 weitere: rejectMcClick, setSubmitActive, setBackActive, disableMcPanel, setTextFieldActive, setTextInTextField, resetMarkers, setMapActive, addIdsToCorrect, setIdToIncorrect, setMarkedIds, setClickTargets)
- **Kennt:** app.shared.model (ScreenView, ShapeGeometry, SketchColor, SketchStructure, AnkiCallbacks), app.shared.skin (LearnComponent, Skin, SkinService), app.shared.ui.components (MultipleChoicePane, SuiteIconButton, SuiteImage, SuiteInfoLabel, SuiteTextField), app.shared.ui.components.map (LearnMap), JavaFX (Node)
- **Art:** View

### BarChartScreenView
- **Rolle:** Framework-gebundene, feature-neutrale Anzeige einer `BarChartData` als Balkendiagramm mit optionaler Ziellinie, Zeitraum- und Abstandssteuerung. Holt Daten über einen übergebenen `BarChartDataProvider`.
- **Öffentlich:**
  - `public BarChartScreenView(BarChartDataProvider provider, LocalDate initialFrom, LocalDate initialTo)`
  - `public void rebuild()`
  - `public Pane getPane()`
- **Kennt:** app.shared.model (BarChartData, BarChartDataProvider, ScreenView), app.shared.skin (SkinService), app.shared.ui.components (SuiteBackground, SuiteDatePicker), JavaFX (BarChart, LineChart, NumberAxis, …)
- **Art:** View

### ComponentHost
- **Rolle:** Generischer Assembly-Host: hält übergebene Komponenten als Kinder einer Null-Layout-Pane. Kennt kein Feature, kein Layout — die Komponenten bringen ihre Position selbst mit.
- **Öffentlich:**
  - `public void clear()`
  - `public void addComponents(Node... components)`
  - `public void setWallpaper(Path wallpaper)`
  - `public Pane getPane()`
- **Kennt:** app.shared.model (ScreenView), app.shared.ui.components (SuiteBackground), JavaFX (Node, Pane)
- **Art:** View

### DashboardScreenView
- **Rolle:** Baut das Dashboard als `FlowPane` voller Kacheln. Bekommt reine Daten (`DashboardTileData`) vom Controller und erzeugt daraus die Kachel-Bausteine selbst.
- **Öffentlich:**
  - `public DashboardScreenView()`
  - `public void build(List<DashboardTileData> tiles)`
  - `public Pane getPane()`
- **Kennt:** app.shared.model (DashboardTileData, DashboardTileStyle, ScreenView), app.shared.skin (SkinService), app.shared.ui.components (SuiteBackground, DashboardTile), JavaFX (FlowPane)
- **Art:** View

### DatePickerDialog
- **Rolle:** Parametrisierter Standarddialog zur Abfrage eines einzelnen Datums; liefert das Datum oder null bei Abbruch.
- **Öffentlich:**
  - `public static LocalDate show(String title, String question, LocalDate defaultDate)`
- **Kennt:** app.shared.ui.components (SuiteDialog, SuiteDatePicker), JavaFX (ButtonType, Label, VBox)
- **Art:** Dienst

### DiaryEditor
- **Rolle:** Modale Editor-Ansicht für genau einen Tagebucheintrag. Baut die Widgets, hält den UI-Zustand (Text, Tags, Attachments) während der Bearbeitung und meldet Speichern/Löschen als `DiaryCardData` über Callbacks. Zwei Modi: neuer Eintrag (wiederholtes Speichern, optional invasiv mit Timer und Close-Blocker) und bestehender Eintrag (einmaliges Speichern/Löschen).
- **Öffentlich:**
  - `public DiaryEditor(DiaryCardData initialEntry, List<String> allTags, InvasiveConfig invasive, Consumer<DiaryCardData> onSave, Consumer<DiaryCardData> onDelete)`
  - `public void showNew()`
  - `public void showEdit()`
- **Kennt:** app.shared (Config, UiUtils), app.shared.model (DiaryAttachment, DiaryCardData, InvasiveConfig), app.shared.ui.components (DiaryTagInputComponent, SuiteDatePicker, SuiteDialog), JavaFX (Dialog, TextArea, FileChooser, Timeline, …)
- **Art:** View

### DiaryScreenView
- **Rolle:** Oberfläche der Tagebuch-Suche: Filterleiste (Volltext, Zeitraum), Trefferliste als Karten. Meldet Suchanfragen und Klick auf einen Eintrag über Listener an das Feature.
- **Öffentlich:**
  - `public interface SearchListener { void onSearch(String query, LocalDate from, LocalDate to); }`
  - `public void setSearchListener(SearchListener l)`
  - `public void setEditListener(Consumer<DiaryCardData> l)`
  - `public Pane getPane()`
  - `public void rebuild()`
  - `public void showResults(List<DiaryCardData> cards, boolean truncated, int maxResults)`
  - `public void setQueryValid(boolean valid)`
- **Kennt:** app.shared.model (DiaryCardData, ScreenView), app.shared.skin (SkinService), app.shared.ui.components (DiaryCard, SuiteBackground, SuiteCardList, SuiteDatePicker, SuiteTextField), JavaFX (VBox, TextField, PseudoClass, …)
- **Art:** View

### FastWriteLearnView
- **Rolle:** Lernform ohne Karte und ohne Multiple-Choice-Auswahl. Zeigt eine Spalte aus Antwortfeldern und eine Countdown-Uhr; getippt wird im gemeinsamen Eingabefeld der Basisklasse.
- **Öffentlich:**
  - `public FastWriteLearnView(String deckId, String mapName, String category, int slotCount, AnkiCallbacks callbacks)`
  - `public void rebuild()` (überschrieben)
  - `public void disableMcPanel()` (überschrieben, leer)
  - `public void showFastStep(List<String> hints, Integer expectedSlot, int firstSeconds, int nextSeconds)`
  - `public void revealSlot(int slot, String text, Integer expectedSlot)`
  - `public void revealMissing(Map<Integer, String> missing)`
  - `public void clearSlots()`
  - `public void restartClock()`
  - `public void toggleClock()`
  - `public void stopClock()`
  - `public void suspendClock()`
  - `public void resumeClock()`
  - `public void playCorrectSound()`
- **Kennt:** app.shared (Sound), app.shared.model (AnkiCallbacks), app.shared.skin (LearnComponent, Skin, SkinService), app.shared.ui.components (AnswerSlotPane, SuiteCountdownLabel), app.shared.ui.components.map (EmptyLearnMap, LearnMap)
- **Art:** View

### ImageComparisonDialog
- **Rolle:** Stellt zwei Bilder nebeneinander, je mit eigenem OK-Knopf, und liefert die getroffene Auswahl. Kennt nicht, woher die Bilder stammen oder was mit der Wahl geschieht.
- **Öffentlich:**
  - `public static SelectionEnum show(BufferedImage left, BufferedImage right)`
- **Kennt:** app.shared.model (SelectionEnum), app.shared.ui.components (SuiteDialog), JavaFX (SwingFXUtils, ImageView, ButtonBar, …)
- **Art:** Dienst

### ImageMapLearnView
- **Rolle:** Lernform mit Bild-Karte (Welt, Hannover) und Eingabefeld. Bekommt die Zuordnung von Auswahl zu Geometrie als Funktion übergeben.
- **Öffentlich:**
  - `public ImageMapLearnView(String deckId, String mapName, String category, Function<Set<String>, List<ShapeGeometry>> geometryFor, AnkiCallbacks callbacks)`
- **Kennt:** app.shared.model (AnkiCallbacks, ShapeGeometry), app.shared.skin (LearnComponent, Skin, SkinService), app.shared.ui.components.map (ImageMapPane, LearnMap)
- **Art:** View

### MainWindowHeaderBar
- **Rolle:** Titelleiste des Hauptfensters — Anwendungssymbol (wächst mit der Leistenhöhe, wählt die passende Auflösung), fertig übergebene Menüleiste, Titel. Wird von nichts in `shared.ui` eingebaut, ist selbst die fertige Fläche.
- **Öffentlich:**
  - `public MainWindowHeaderBar(Stage stage, MenuBar menuBar)`
- **Kennt:** JavaFX (HeaderBar, Stage, MenuBar, Bindings, Image) — keine weiteren app-Pakete
- **Art:** View

### McLearnView
- **Rolle:** Lernform Multiple Choice: keine Karte, kein Eingabefeld; die Antwortauswahl bleibt immer aktiv, `disableMcPanel` wirkt hier nicht.
- **Öffentlich:**
  - `public McLearnView(String deckId, String mapName, String category, AnkiCallbacks callbacks)`
- **Kennt:** app.shared.model (AnkiCallbacks), app.shared.ui.components.map (EmptyLearnMap, LearnMap)
- **Art:** View

### MessageContactDialog
- **Rolle:** Modaler Dialog zur Auflösung eines unbekannten Kontakts beim Nachrichtenimport, gemeinsam genutzt von WhatsApp und Signal. Bietet Autocomplete gegen bekannte Kontakte und liefert entweder eine bestehende contact_id oder einen neuen Anzeigenamen. Schließen per X ist blockiert.
- **Öffentlich:**
  - `public record Result(Integer existingContactId, String newDisplayName)`
  - `public static Result show(String quelle, String rawIdentifier, String suggestion, Map<String, Integer> knownContacts)`
- **Kennt:** app.shared.ui.components (SuiteDialog, SuiteSuggestionTextField), JavaFX (Stage, Button, Label, VBox)
- **Art:** Dienst

### MovieViewerScreenView
- **Rolle:** Oberfläche des Filmbrowsers: Suchfelder mit Vorschlägen für Regisseur, Schauspieler und Titel links, Ergebniskacheln rechts. Merkt sich die zuletzt gezeigten Karten, damit ein Skinwechsel die Anzeige wiederherstellen kann.
- **Öffentlich:**
  - `public interface SelectionListener { onDirectorSelected(String), onActorSelected(String), onTitleSelected(String) }`
  - `public void setSelectionListener(SelectionListener l)`
  - `public void setNames(List<String> directors, List<String> actors, List<String> titles)`
  - `public Pane getPane()`
  - `public void rebuild()`
  - `public void showCards(List<CardData> cards)`
- **Kennt:** app.shared.model (CardData, MovieStyle, ScreenView), app.shared.skin (SkinService), app.shared.ui.components (MovieCard, SuiteBackground, SuiteCardList, SuiteSuggestionTextField), JavaFX (VBox, HBox, Label)
- **Art:** View

### RegionConfigDialog
- **Rolle:** Konfigurationsdialog für eine Region-Session: Modus-Auswahl per ComboBox, Deck-Checkboxen in Spalten. Zustandsänderungen laufen über einen vom Feature übergebenen Reducer (`Zustand → Zustand`), der Verflechtung zwischen Elementen an einer Stelle beschreibt.
- **Öffentlich:**
  - `public RegionConfigDialog(RegionDialogState initial, UnaryOperator<RegionDialogState> reduce)`
  - `public RegionDialogState showAndWait()`
- **Kennt:** app.shared.model (RegionDialogState, Choice, Toggle), app.shared.ui.components (SuiteDialog), JavaFX (ComboBox, CheckBox, DialogPane)
- **Art:** View

### RegionLearnView
- **Rolle:** Oberfläche einer Region-Session: eine Karte plus wahlweise Fragefeld (Klick-Modi) oder Eingabefeld (Schreib-/Eliminierungs-Modi). Übersetzt Presenter-Absicht in Anzeige, kennt weder Deck noch MapService.
- **Öffentlich:**
  - `public RegionLearnView(String deckId, String mapName, String category, List<ShapeGeometry> geometries, boolean mitFragefeld, RegionCallbacks callbacks)`
  - `public void rebuild(boolean mitFragefeld)`
  - `public ScreenView getView()`
  - `public void addIdsToActive(Set<String> ids)`
  - `public void addIdsToMarked(Set<String> ids)`
  - `public void moveAllToActive()`
  - `public void moveResolvedToActive()`
  - `public void addIdsToCorrect(Set<String> elements)`
  - `public void addIdsToInactive(Set<String> elements)`
  - `public void setIdToIncorrect(String element)`
  - `public void setMapActive(boolean active)`
  - `public ShapeMapState getState()`
  - `public void setState(ShapeMapState state)`
  - `public void setTextInTextField(String string)`
  - `public void setTextFieldActive(boolean active)`
  - `public void setQuestion(String text)`
  - … (1 weitere: getQuestion)
- **Kennt:** app.shared.model (RegionCallbacks, ScreenView, ShapeGeometry, ShapeMapState), app.shared.skin (LearnComponent, Skin, SkinService), app.shared.ui.components (SuiteInfoLabel, SuiteTextField), app.shared.ui.components.map (ShapeMapPane)
- **Art:** View

### ShapeMapLearnView
- **Rolle:** Lernform mit Shape-Karte (aktuell nur das Deutschland-Deck) und Eingabefeld.
- **Öffentlich:**
  - `public ShapeMapLearnView(String deckId, String mapName, String category, List<ShapeGeometry> geometries, AnkiCallbacks callbacks)`
- **Kennt:** app.shared.model (AnkiCallbacks, ShapeGeometry), app.shared.skin (LearnComponent, SkinService), app.shared.ui.components.map (LearnMap, ShapeMapPane)
- **Art:** View

### StartScreenView
- **Rolle:** Startbildschirm: zeigt nur das geschmückte Wallpaper des Skins — der einzige Screen ohne Inhalt darüber.
- **Öffentlich:**
  - `public StartScreenView()`
  - `public void rebuild()`
  - `public Pane getPane()`
- **Kennt:** app.shared.model (ScreenView), app.shared.skin (SkinService), app.shared.ui.components (SuiteBackground)
- **Art:** View

### TextPromptDialog
- **Rolle:** Parametrisierter Standarddialog: Kopftext plus mehrzeiliges Textfeld, liefert den eingegebenen Text oder null bei Abbruch.
- **Öffentlich:**
  - `public static String show(String title, String headerText, String prefill)`
- **Kennt:** app.shared.ui.components (SuiteDialog), JavaFX (TextArea, ButtonType, VBox)
- **Art:** Dienst

### WhatsAppChatDialog
- **Rolle:** Modaler Dialog zur Auflösung eines unbekannten WhatsApp-Chats beim Import: Entscheidung Import/Ignorieren plus Anzeigename. Schließen per X ist blockiert; wirft eine FailFast-Exception, wenn kein Ergebnis vorliegt.
- **Öffentlich:**
  - `public record Result(boolean doImport, String displayName)`
  - `public static Result show(String rawIdentifier, String subject, boolean isGroup, String formattedTs)`
- **Kennt:** app.shared.ui.components (SuiteDialog), JavaFX (Stage, TextField, Bindings, ButtonBar)
- **Art:** Dienst

### Vererbung
- `AnkiLearnView` (abstrakt) → `FastWriteLearnView`, `ImageMapLearnView`, `McLearnView`, `ShapeMapLearnView`

### Beobachtungen (unbewertet)
- 4 von 23 Klassen des Pakets erben von `AnkiLearnView`.
- 6 Klassen bestehen ausschließlich aus einer statischen `show(...)`-Methode ohne Instanzfelder: Alerts, DatePickerDialog, ImageComparisonDialog, MessageContactDialog, TextPromptDialog, WhatsAppChatDialog.
- `ActivityTableDialog` ist mit 374 Zeilen die längste Datei des Pakets, größtenteils Javadoc zur Größenberechnung der Tabelle.

---

## Paket app.shared.ui.components
Bausteine, die von Oberflächen in `shared.ui` (und teils vom Feature aus, per Screen-View) in den Szenengraphen gehängt werden — Karten, Eingabefelder, Dialoge, Anzeigefelder. Es gibt bewusst kein Unterpaket je Feature; welchem Feature ein Baustein zuzuordnen ist, steht im Klassennamen.

### AnswerSlotPane
- **Rolle:** zeigt eine Säule aus Antwortfeldern, die sich schrittweise aufdecken (Diktat-artige Lernform). Reine Anzeige, nichts ist anklick- oder beschreibbar.
- **Öffentlich:**
  - `AnswerSlotPane(Rectangle2D bounds, int slotCount, McMetrics metrics)`
  - `AnswerSlotPane(double width, int slotCount, McMetrics metrics)`
  - `void showSlots(List<String> hints)`
  - `void revealCorrect(int slot, String text)`
  - `void revealMissing(Map<Integer, String> missing)`
  - `void setExpected(Integer slot)`
  - `void clear()`
- **Kennt:** JavaFX; app.shared.model (`McMetrics`); paketintern `ButtonTextFit`.
- **Art:** Baustein

### ButtonTextFit
- **Rolle:** paketprivate Hilfsklasse; misst, ob ein Antworttext in seinen Button passt, und schaltet dafür eine von zwei Layout-Stufen per Pseudoklasse (`:squeezed`, `:tiny`).
- **Öffentlich:** keine öffentliche API — die Klasse selbst ist paketprivat. Einzige Methode: `static void apply(Button btn, String text, McMetrics metrics)`, genutzt von `AnswerSlotPane` und `MultipleChoicePane`.
- **Kennt:** JavaFX; app.shared.model (`McMetrics`).
- **Art:** Hilfsklasse

### Card
- **Rolle:** Marker-Interface ohne Methoden — sagt nur, dass ein Typ als Kachel in einer `SuiteCardList` stehen darf.
- **Öffentlich:** keine Methoden.
- **Kennt:** nichts.
- **Art:** Hilfsklasse (Marker-Interface)

### DashboardTile
- **Rolle:** Dashboard-Kachel mit großer Zahl oben und Beschriftung unten. Passiver Baustein, feste Maße kommen als Style-Objekt herein.
- **Öffentlich:** `DashboardTile(String topValue, String bottomText, DashboardTileStyle style)`
- **Kennt:** JavaFX; app.shared.model (`DashboardTileStyle`).
- **Art:** Baustein

### DiaryCard
- **Rolle:** ein Tagebuch-Eintrag als Karte: Datum, Tags, Anhang-Thumbnails, Text. Passiver Baustein, bekommt seine Daten fertig herein.
- **Öffentlich:** `DiaryCard(DiaryCardData data)`
- **Kennt:** JavaFX; app.shared (`Config`); app.shared.model (`DiaryAttachment`, `DiaryCardData`); paketintern `SuiteThumbnail`.
- **Art:** Baustein

### DiaryTagInputComponent
- **Rolle:** Mehrfachauswahl von Tags — ein Vorschlagsfeld mit darunterliegenden Chips für das Gewählte. Liefert Feld und Chip-Fläche getrennt, weil sie im Layout des Aufrufers an verschiedenen Stellen sitzen.
- **Öffentlich:**
  - `DiaryTagInputComponent()`
  - `SuiteSuggestionTextField getTagInput()`
  - `FlowPane getChipPane()`
  - `ObservableList<String> getSelectedTags()`
  - `void setAllTags(List<String> tags)`
  - `void addTag(String tag)`
  - `void reset()`
  - `void requestFocus()`
- **Kennt:** JavaFX; paketintern `SuiteSuggestionTextField`.
- **Art:** Baustein

### MovieCard
- **Rolle:** eine Film-Kachel: Poster, Rating-Zahl, Infoblock mit klickbaren Regisseur-/Schauspieler-Namen und optionalem Kommentar-Popup. Passiver Baustein, bekommt Daten, Maße und beide Klick-Callbacks herein.
- **Öffentlich:** `MovieCard(CardData data, MovieStyle style, Consumer<String> onDirectorClicked, Consumer<String> onActorClicked)`
- **Kennt:** JavaFX; app.shared (`Config`, `UiUtils`); app.shared.model (`CardData`, `MovieStyle`).
- **Art:** Baustein

### MultipleChoicePane
- **Rolle:** zeigt bis zu acht Multiple-Choice-Antwortknöpfe mit drei Layoutstufen je nach Textlänge, Zustandsfarben und einem Ablehn-Schütteln bei ungültigem Klick.
- **Öffentlich:**
  - `MultipleChoicePane(Rectangle2D bounds, McMetrics metrics)`
  - `MultipleChoicePane(double width, McMetrics metrics)`
  - `void initiateMultipleChoice(List<String> answers)`
  - `void clearAndSetInactive()`
  - `void setCorrectAndInactive(Collection<Integer> correctIndices)`
  - `void setCorrect(int index, boolean correct)`
  - `void setMarked(int index, boolean marked)`
  - `void rejectClick(int index)`
  - `void addListener(Consumer<Integer> listener)`
- **Kennt:** JavaFX; app.shared.model (`McMetrics`); paketintern `ButtonTextFit`.
- **Art:** Baustein

### SketchPane
- **Rolle:** paketprivate schematische Skizze aus nummerierten Teilflächen (Polygon oder Kreis), die sich Fläche für Fläche einfärben lässt. Kennt keinen Lern-Typ, nur ihre Geometrien mit Nummer als id. Innenleben von `SuiteImage`.
- **Öffentlich:** Klasse selbst paketprivat; öffentliche Methoden darauf:
  - `void append(List<ShapeGeometry> geometries, int cell, double size, double offsetX, double offsetY)`
  - `void mark(List<Integer> marked)`
  - `void fill(List<Integer> filled, SketchColor color)`
- **Kennt:** JavaFX; app.shared.model (`ShapeGeometry`, `SketchColor`, `SketchStructure`); app.shared.skin (`SkinService`).
- **Art:** Baustein

### SuiteBackground
- **Rolle:** Fabrik für den Hintergrund eines Spielfelds — ein zentriertes, nicht wiederholtes, einpassendes Hintergrundbild. Unterklasse ist unmöglich, da die betroffenen JavaFX-Typen `final` sind.
- **Öffentlich:** `static Background of(Path wallpaper)`
- **Kennt:** JavaFX.
- **Art:** Hilfsklasse

### SuiteCardList
- **Rolle:** senkrechte Liste von Karten in einem Rollbereich mit dauerhaft sichtbarer Scrollbar — die Trefferanzeige von Tagebuch und Film.
- **Öffentlich:**
  - `SuiteCardList()`
  - `<T extends Region & Card> void setCards(List<T> content)`
- **Kennt:** JavaFX; paketintern `Card`.
- **Art:** Baustein

### SuiteCountdownLabel
- **Rolle:** ein Feld mit einer im Sekundentakt herunterzählenden Zahl; hält bei null automatisch an und meldet das Ablaufen einmal.
- **Öffentlich:**
  - `SuiteCountdownLabel(Rectangle2D bounds)`
  - `void onExpired(Runnable listener)`
  - `void start(int firstSeconds, int followingSeconds)`
  - `void restart()`
  - `void togglePause()`
  - `void stop()`
  - `void suspend()`
  - `void resume()`
- **Kennt:** JavaFX; paketintern `SuiteInfoLabel` (Elternklasse).
- **Art:** Baustein

### SuiteDatePicker
- **Rolle:** der Datumsauswähler der Suite; einzige Festlegung ist das Abschalten der Kalenderwochen.
- **Öffentlich:** `SuiteDatePicker(LocalDate defaultDate)`
- **Kennt:** JavaFX.
- **Art:** Baustein

### SuiteDialog
- **Rolle:** Dialog im Suite-Look mit eigener Titelleiste (`StageStyle.EXTENDED`, gesetzter Owner). Liest skin-abhängige Werte selbst und reicht sie an die eingebaute `SuiteHeaderBar` weiter.
- **Öffentlich:**
  - `SuiteDialog(String title)`
  - `VBox contentBox()`
  - `void setSuiteTitle(String title)`
- **Kennt:** JavaFX; app.shared (`UiUtils`); paketintern `SuiteHeaderBar`.
- **Art:** Baustein

### SuiteHeaderBar
- **Rolle:** die Titelleiste der Suite-Fenster, für Dialoge wie Alerts; nötig, weil `StageStyle.EXTENDED` eine eigene Fensterdekoration verlangt. Passiver Baustein.
- **Öffentlich:**
  - `SuiteHeaderBar(String title)`
  - `void setTitleText(String title)`
- **Kennt:** JavaFX.
- **Art:** Baustein

### SuiteIconButton
- **Rolle:** ein Knopf mit Symbol statt Beschriftung; holt sich Bilddatei und Einfärbung selbst beim Skin, nach außen bietet er nur einen Klick-Listener.
- **Öffentlich:**
  - `SuiteIconButton(Skin.IconButtonType rolle)`
  - `SuiteIconButton(Skin.IconButtonType rolle, Rectangle2D bounds)`
  - `void onClick(Runnable action)`
- **Kennt:** JavaFX; app.shared.skin (`Skin`, `SkinService`).
- **Art:** Baustein

### SuiteImage
- **Rolle:** Bilderrahmen aus Hintergrund-, Bild-/Skizzen- und Rahmen-Ebene; zeigt wahlweise ein Rasterbild, ein gerendertes SVG oder eine `SketchPane`. Bild und Skizze schließen sich gegenseitig aus.
- **Öffentlich:**
  - `SuiteImage(Rectangle2D bounds)`
  - `SuiteImage(double width, double height)`
  - `void setImage(String imageName)`
  - `void setSketch(SketchStructure structure)`
  - `void addSketch(List<ShapeGeometry> areas, int cell, double size, double offsetX, double offsetY)`
  - `void markSketchAreas(List<Integer> areas)`
  - `void fillSketchAreas(List<Integer> areas, SketchColor color)`
  - `Rectangle getBackgroundRect()`
  - `Rectangle getBorderRect()`
- **Kennt:** JavaFX; java.awt/Swing (`SwingFXUtils` als Brücke); die externe SVG-Bibliothek jsvg; app.shared (`Config`); app.shared.model (`BigComponentStyle`, `ShapeGeometry`, `SketchColor`, `SketchStructure`); app.shared.skin (`SkinService`); paketintern `SketchPane`.
- **Art:** Baustein

### SuiteInfoLabel
- **Rolle:** Textlabel mit eigenem, schlankem HTML-Parser (`<br/>`, `<b>`, `<i>`) statt teurer Browser-Engine; unterstützt weiche Umbrüche an Bindestrichen und eigene Smiley-Knoten.
- **Öffentlich:**
  - `SuiteInfoLabel(String text, Rectangle2D bounds)`
  - `SuiteInfoLabel(String text)`
  - `void centerText()`
  - `void setText(String text)`
  - `String getText()`
  - `void setFixedWidth(double width)`
  - `void setFixedHeight(double height)`
- **Kennt:** JavaFX.
- **Art:** Baustein

### SuiteSuggestionTextField
- **Rolle:** Textfeld mit Vorschlagsliste während des Tippens („Search While You Type"); entscheidet nicht, was eine Auswahl bedeutet, meldet sie nur an den Aufrufer.
- **Öffentlich:**
  - `SuiteSuggestionTextField(String promptText)`
  - `void setAllItems(List<String> items)`
  - `void setOnSelected(Consumer<String> onSelected)`
  - `void setTextAndTrigger(String text)`
  - `void setTextSilent(String text)`
  - `void clearSilent()`
- **Kennt:** JavaFX.
- **Art:** Baustein

### SuiteTabCommitTextFieldTableCell
- **Rolle:** Tabellenzelle mit Texteditor, bei der Tab/Shift-Tab wie Enter die Eingabe committet, statt den Editiermodus in einem inkonsistenten Zustand zu belassen.
- **Öffentlich:**
  - `SuiteTabCommitTextFieldTableCell(StringConverter<T> converter)`
  - `static <S> Callback<TableColumn<S,String>, TableCell<S,String>> forTableColumn()`
  - `static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(StringConverter<T> converter)`
  - `void startEdit()` (überschrieben)
- **Kennt:** JavaFX.
- **Art:** Baustein

### SuiteTextField
- **Rolle:** Textfeld mit `setActive(boolean)` als zentraler Operation — aktiv heißt geleert, freigegeben und fokussiert.
- **Öffentlich:**
  - `SuiteTextField()`
  - `SuiteTextField(Rectangle2D bounds)`
  - `void onType(Consumer<String> listener)`
  - `void setActive(boolean active)`
- **Kennt:** JavaFX.
- **Art:** Baustein

### SuiteThumbnail
- **Rolle:** Miniaturbild, das beim Überfahren das Original vergrößert in einem Popup zeigt, platziert in der Bildschirmhälfte mit dem meisten Platz. Kennt kein Feature, nur zwei Pfade und eine Höhe.
- **Öffentlich:** `SuiteThumbnail(Path thumbnail, Path original, double height)`
- **Kennt:** JavaFX; app.shared.skin (`SkinService`).
- **Art:** Baustein

### Vererbung
- `SuiteCountdownLabel extends SuiteInfoLabel` — einzige Erbkette innerhalb dieses Pakets.
- `DiaryCard extends VBox implements Card`, `MovieCard extends HBox implements Card` — beide erfüllen den `Card`-Vertrag.
- Alle übrigen Bausteine erben direkt von ihrem JavaFX-Typ (Pane, StackPane, Button, TextField, DatePicker, Dialog, ScrollPane, ImageView, TextFieldTableCell, HeaderBar, VBox, HBox).

## Paket app.shared.ui.components.map
Hält die beiden Lernkarten (Shape- und Bild-Karte) samt ihrem gemeinsamen Vertrag und ihrem Innenleben — ein zusammenhängender Cluster in eigenem Unterpaket.

### EmptyLearnMap
- **Rolle:** Nullobjekt für `LearnMap` — die Karte der MC-Session, die keine Karte hat. Leere `Group`, die nichts zeichnet und keinen Platz beansprucht.
- **Öffentlich:** `void reset()`, `void setActive(boolean)`, `void markCorrect(Set<String>)`, `void markIncorrect(String)`, `void setClickTargets(Set<String>)`, `void mark(Set<String>)`, `Node getView()` — alle leer bzw. geben sich selbst zurück.
- **Kennt:** JavaFX; paketintern `LearnMap`.
- **Art:** Baustein

### ImageMapPane
- **Rolle:** Karte als großes, per Drag & Drop verschiebbares Bild mit Minikarte; Formen sind zunächst unsichtbar und werden erst nach Klick oder Bewertung sichtbar. Kennt keinen Lern-Typ — Ids kommen herein, Geometrien liefert eine übergebene Funktion.
- **Öffentlich:**
  - `ImageMapPane(MapImages mapImages, int overlayContentInset, Rectangle2D bounds, Function<Set<String>, List<ShapeGeometry>> geometryFor)`
  - `Node getView()`
  - `void setListener(Consumer<String> listener)`
  - `void setActive(boolean active)`
  - `void reset()`
  - `void markCorrect(Set<String> ids)`
  - `void mark(Set<String> ids)`
  - `void setClickTargets(Set<String> ids)`
  - `void markIncorrect(String id)`
  - `void addToCorrect(List<ShapeGeometry> shapes)`
  - `void setMarked(List<ShapeGeometry> shapes)`
  - `void markLastClickAsIncorrect()`
- **Kennt:** JavaFX; app.shared.model (`BigComponentStyle`, `MapImages`, `ShapeGeometry`); app.shared.skin (`SkinImageCache`, `SkinService`); paketintern `LearnMap`, `MapNodeBuilder`.
- **Art:** Baustein

### LearnMap
- **Rolle:** gemeinsamer Vertrag der Lernkarten — spricht durchgehend Ids, unabhängig davon, ob die Umsetzung intern mit Ids oder Geometrien arbeitet. Erfüllt von `EmptyLearnMap`, `ImageMapPane`, `ShapeMapPane`.
- **Öffentlich:** `void reset()`, `void setActive(boolean active)`, `void markCorrect(Set<String> ids)`, `void markIncorrect(String id)`, `void setClickTargets(Set<String> ids)`, `void mark(Set<String> ids)`, `Node getView()`
- **Kennt:** JavaFX (`Node`).
- **Art:** Hilfsklasse (Vertrag/Schnittstelle)

### MapNodeBuilder
- **Rolle:** paketprivate Fabrik — die einzige Stelle, an der aus framework-freier `ShapeGeometry` der sichtbare JavaFX-Node für beide Kartenarten entsteht.
- **Öffentlich:** Klasse selbst paketprivat; `buildImageMapNode(ShapeGeometry geometry)` ist als einzige Methode `public`, `buildShapeMapNode(ShapeGeometry geometry)` paketprivat.
- **Kennt:** JavaFX; app.shared.model (`ShapeGeometry`); paketintern `ShapeLayer`.
- **Art:** Hilfsklasse (Fabrik)

### ShapeLayer
- **Rolle:** paketprivates Enum; leitet aus dem rohen GeoJSON-`type` einer Shape-Karte zIndex, CSS-Layer-Klasse und Interaktivität ab. Vier Werte: INTERACTIVE, NEIGHBOR, WATER, OVERLAY.
- **Öffentlich:** keine öffentlichen Methoden (Klasse und Methoden paketprivat).
- **Kennt:** nichts (keine Importe).
- **Art:** Enum/Konstanten

### ShapeMapPane
- **Rolle:** interaktive Karte, die vollständig aus Shapes besteht (z. B. Landkreise); manche Formen sind interaktiv, andere nicht. Kennt keinen Lern-Typ, meldet Klicks als reine Id.
- **Öffentlich:**
  - `ShapeMapPane(List<ShapeGeometry> geometries, Rectangle2D bounds)`
  - `Node getView()`
  - `void setClickListener(Consumer<String> listener)`
  - `void reset()`
  - `void setActive(boolean active)`
  - `void markCorrect(Set<String> ids)`
  - `void markIncorrect(String id)`
  - `void mark(Set<String> ids)`
  - `void setClickTargets(Set<String> ids)`
  - `void markActive(Set<String> ids)`
  - `void markInactive(Set<String> ids)`
  - `void moveResolvedToActive()`
  - `ShapeMapState getState()`
  - `void setState(ShapeMapState state)`
- **Kennt:** JavaFX; app.shared.model (`ShapeGeometry`, `ShapeMapState`); app.shared.skin (`SkinService`); paketintern `LearnMap`, `MapNodeBuilder`, `ShapeLayer`.
- **Art:** Baustein

### Vererbung
- `EmptyLearnMap extends Group implements LearnMap`
- `ImageMapPane extends StackPane implements LearnMap`
- `ShapeMapPane extends StackPane implements LearnMap`
- Keine Klasse dieses Pakets erbt von einer anderen Klasse desselben Pakets — die gemeinsame Basis ist ausschließlich das Interface `LearnMap`.

### Beobachtungen (unbewertet)
- `Card` wird von zwei Bausteinen implementiert (`DiaryCard`, `MovieCard`); `LearnMap` von drei (`EmptyLearnMap`, `ImageMapPane`, `ShapeMapPane`).
- `SuiteCountdownLabel` ist die einzige Klasse in `components`, die von einem anderen Baustein desselben Pakets erbt (`SuiteInfoLabel`).
- `SketchPane`, `MapNodeBuilder` und `ShapeLayer` sind paketprivat — ohne öffentliche Klasse nach außen, nur einzelne Methoden teils `public`.
- `SuiteImage` bindet neben JavaFX zusätzlich eine externe SVG-Bibliothek (jsvg) sowie java.awt/Swing (`SwingFXUtils`) als Brücke ein.

---

## Paket app.shared.skin
Zulieferer der Anzeige-Schicht: hält alle Aussehens-Werte (Farben, Fonts, Border, Rechtecke, Bildnamen) je Skin, lädt sie aus properties-Dateien und erzeugt daraus das CSS-Stylesheet der Anwendung. Baut selbst keine Oberfläche.

### BaseColorSkin
- **Rolle:** Abstrakte Zwischenschicht für Skins, die vor ihrer eigenen properties-Datei zusätzlich eine gemeinsame Basisdatei (`skin_basecolor.properties`) laden.
- **Öffentlich:** `BaseColorSkin()`
- **Kennt:** app.shared (Config)
- **Art:** Hilfsklasse

### BlueGradientSkin
- **Rolle:** Konkreter Skin „Moonlight", lädt `skin_moonlight.properties` nach der Basisdatei von `BaseColorSkin`.
- **Öffentlich:** `BlueGradientSkin()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### DarkMode
- **Rolle:** Konkreter Skin „Dark Mode", lädt `skin_darkmode.properties` direkt (ohne Basisdatei).
- **Öffentlich:** `DarkMode()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### FlatWebSkin
- **Rolle:** Konkreter Skin „FlatWeb", lädt `skin_flatweb.properties` direkt.
- **Öffentlich:** `FlatWebSkin()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### FlowerSkin
- **Rolle:** Konkreter Skin „Flower", lädt `skin_flower.properties` nach der Basisdatei von `BaseColorSkin`.
- **Öffentlich:** `FlowerSkin()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### LearnComponent
- **Rolle:** Enum der Session-Bestandteile (Karte, Textfeld, Bild, MC, Antwortfelder, Zurück-/Absenden-Knopf), jedes mit dem Property-Suffix, den `learnComponentBounds(...)` daraus bildet.
- **Art:** Enum/Konstanten

### RedGradientSkin
- **Rolle:** Konkreter Skin „Drive", lädt `skin_drive.properties` nach der Basisdatei von `BaseColorSkin`.
- **Öffentlich:** `RedGradientSkin()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### ShadowSpace
- **Rolle:** Errechnet aus dem CSS-Schatten-Effekt (`componentShadow`) den Platzbedarf je Seite in Pixeln, den ein Container (z. B. eine ScrollPane) für den Schatten freihalten muss.
- **Öffentlich:** Klasse selbst ist paketprivat (kein `public`), also außerhalb von `shared.skin` nicht erreichbar. Innerhalb des Pakets: `of(String effect) : ShadowSpace`, `isPresent() : boolean`, sowie die Record-Komponenten `top()`, `right()`, `bottom()`, `left()`.
- **Kennt:** keine weiteren app-Pakete; reine Text-/Zahlen-Verarbeitung.
- **Art:** Modell/Datenhalter (Record)

### Skin
- **Rolle:** Abstrakte Basisklasse aller Skins; erzeugt aus den in `SkinProperties` gehaltenen Werten das vollständige CSS-Stylesheet und hängt es an eine `Scene`. Gliedert das CSS in eine `add…Styles()`-Methode je Komponentengruppe (Button, TextField, Menü, Dialog, Bildkarte, Skizze, Shape-Karte, Multiple-Choice, Tabelle, Dashboard, Diagramm, Datepicker, Spinner, Vorschlagsbox, Tagebuch-/Filmkarten, Trefferliste).
- **Öffentlich:** `styleScene(Scene scene) : void`, `buildCss() : String`
- **Kennt:** JavaFX (Scene, Color, Font, Insets), app.shared (UiUtils), app.shared.model (BorderParams, SketchColor)
- **Art:** Dienst

### SkinImageCache
- **Rolle:** Cacht große, skin-abhängige Bilder (Kartenbilder) nur für den jeweils aktiven Skin und leert sich bei Skinwechsel selbst; einziger Ort, an dem solche Bilder von der Platte gelesen werden.
- **Öffentlich:** `getInstance() : SkinImageCache`, `get(Path path) : Image`, `warm(Path path) : void`, `warmMapImages(String mapName) : void`
- **Kennt:** JavaFX (Image), app.shared.model (MapImages), SkinService (selbes Paket)
- **Art:** Dienst

### SkinProperties
- **Rolle:** Hält alle Aussehens-Felder eines Skins (Farben, Fonts, Border, Maße, Layout-Rechtecke, Bild- und Wallpaper-Namen) und lädt sie per Reflection aus einer properties-Datei; reicht nach außen ausschließlich zweckgeschnittene Records/Werte heraus, nie Property-Namen. Skins erben davon über `Skin`.
- **Öffentlich (Auswahl, 18 Methoden insgesamt):** `getDisplayName() : String` (abstract), `getContentSize() : Dimension2D`, `dialogStyle() : DialogStyle`, `movieStyle() : MovieStyle`, `dashboardTileStyle() : DashboardTileStyle`, `learnComponentBounds(String deckId, String mapName, String category, LearnComponent teil) : Rectangle2D`, `learnTextLabelBounds(String deckId, String mapName, String category, Skin.TextLabelType typ) : Rectangle2D`, `bigComponentStyle() : BigComponentStyle`, `wallpaperPath(String deckId, String mapName, String category) : Path`, `mcMetrics(String deckId, String mapName, String category) : McMetrics`, `iconFor(IconButtonType rolle) : Image`, `mapImages(String mapName) : MapImages`, `sketchStrokeWidth() : double`, `shapeMapStrokeReserve() : double`, `popupMonitorMargin() : double` … (3 weitere: `emptyWallpaperPath()`, `startScreenWallpaperPath()`, `imageMapOverlayContentInset(String mapName)`). Zusätzlich zwei öffentliche Enums: `IconButtonType`, `TextLabelType`.
- **Kennt:** JavaFX (Color, Font, Insets, Rectangle2D, Image, Dimension2D, Text), app.shared (Config, UiUtils), app.shared.model (BorderParams, BigComponentStyle, DashboardTileStyle, DialogStyle, MapImages, McMetrics, MovieStyle)
- **Art:** Modell/Datenhalter (mit Lade- und Ableitungslogik)

### SkinService
- **Rolle:** Verwaltet die Liste aller verfügbaren Skins, hält den aktuell gewählten fest, schreibt die Auswahl nach `Config` und kann den aktuellen Skin neu instanziieren (Reload nach Bearbeiten der properties-Datei).
- **Öffentlich:** `set(Skin skin) : void`, `get() : Skin`, `getAllSkins() : List<Skin>`, `refresh() : void`
- **Kennt:** app.shared (Config), die konkreten Skin-Klassen desselben Pakets
- **Art:** Dienst

### SpicySkin
- **Rolle:** Konkreter Skin „Spicy", lädt `skin_spicy.properties` nach der Basisdatei von `BaseColorSkin`.
- **Öffentlich:** `SpicySkin()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### TileSkin
- **Rolle:** Konkreter Skin „Tiles", lädt `skin_tile.properties` direkt; laut Klassenkommentar ein Prototyp, dessen Schatten bisher nur bei einem Teil der Bausteine ankommt.
- **Öffentlich:** `TileSkin()`, `getDisplayName() : String`
- **Kennt:** app.shared (Config)
- **Art:** Dienst

### Weg einer Eigenschaft
1. Ein Schlüssel steht in der properties-Datei eines Skins (z. B. `skin_moonlight.properties` im `configFolder`) und trägt denselben Namen wie ein `protected`-Feld in `SkinProperties` (oder einer Zwischenklasse wie `BaseColorSkin`).
2. Der Konstruktor der konkreten Skin-Klasse ruft `loadAllConfigs(Path)`; das liest die Datei per Reflection und setzt jedes Feld der Klassenhierarchie, dessen Name als Schlüssel vorkommt. Ein Schlüssel ohne passendes Feld lässt `checkKeysHaveFields` sofort mit Exception scheitern.
3. `SkinService` hält die fertig geladene Instanz als `current` (beim Start aus `Config.get("pref.skinClass")` bestimmt, später über `set(...)` oder `refresh()` gewechselt).
4. `Skin.buildCss()` füllt zunächst noch offene Felder mit abgeleiteten Vorgaben und baut danach über `CssBuilder`, gruppiert je Komponente (`addButtonStyles`, `addTextFieldStyles`, `addMultipleChoiceStyles`, …), die CSS-Regeln aus den Feldwerten zusammen.
5. `Skin.styleScene(Scene)` hängt dieses Stylesheet als Daten-URL an `scene.getStylesheets()` — der einzige Aufrufpunkt aus der Anwendung heraus (aus `shared.ui`).
6. JavaFX wendet die Regeln auf jeden Knoten an, dessen Style-Klasse zum Selektor passt (`.button`, `.my-mc-button`, `.my-info-label.question`, …); das Bedienelement erscheint entsprechend, ohne den Skin selbst zu kennen.
7. Werte, die CSS nicht ausdrücken kann (Rechtecke, Bilder, Fonts als Objekt), nimmt ein Baustein in `shared.ui` stattdessen über eine zweckgeschnittene Methode der Skin-Instanz entgegen (`iconFor(...)`, `learnComponentBounds(...)`, `mapImages(...)`, `mcMetrics(...)`) — ein zweiter, CSS-unabhängiger Weg vom selben Feld zum sichtbaren Element.

### Beobachtungen (unbewertet)
- Drei Skins (`DarkMode`, `FlatWebSkin`, `TileSkin`) erben direkt von `Skin`, vier (`BlueGradientSkin`, `RedGradientSkin`, `FlowerSkin`, `SpicySkin`) über die Zwischenklasse `BaseColorSkin`.
- `ShadowSpace` ist die einzige Klasse des Pakets ohne `public`-Modifikator.
- `SkinProperties` enthält neben den Feldern auch das Parsen (Farbe, Font, BorderParams, Rechteck) und die Ableitungslogik für Vorgabewerte; diese Schritte liegen nicht in eigenen Klassen.

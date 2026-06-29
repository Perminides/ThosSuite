# ThosSuite — Architektur (allgemein)

**Stand:** 28.06.2026 (Struktur) — Inhalte noch ungeprüft (Phase 2 ausstehend).
**Charakter:** Das *immer mitzugebende* Fundament — was bei jeder Aufgabe gilt, egal welches
Feature: Überblick, technische Basis, Paketstruktur, Orchestrierungs-Mechanik, Fundament.
Feature-Details (Lern-Kern, einzelne Features, konkrete Screens) stehen separat in
`ThosSuite_Feature-Details.md`. Die *Regeln* (warum so geschnitten) in `ThosSuite_Design_Regeln.md`.

> **Strukturstand:** Die heutige Paketstruktur ist das Ergebnis eines Architektur-Durchgangs
> (Frühjahr 2026): Auflösung des alten `data`-Topfs, dezentrale Feature-Repositories,
> Zusammenführung des Lern-Kerns in `learn`, Skin als `shared.skin`. Begründungen dazu im
> Regel-Dokument.

## 📋 Überblick

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

<!-- ANNAHME: Implementierter Funktionsumfang aus dem Status-Block hierher (globaler Überblick, was die Suite kann) -->
### 🚀 Implementierter Funktionsumfang

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

## 🗝️ Technische Basis

### 📦 Paket-Struktur

Die Suite ist in **Sorten** von Paketen gegliedert (Details und Begründung im Regel-Dokument):
ein **Lern-Kern** (`learn`), viele **Peripherie-Features** (`alc`, `diary`, `fitbit`, `mattress`,
`messaging`, `movie`, `weekday`), ein **Fundament-Dach** (`shared` inkl. `shared.model` und
`shared.skin`), die **Orchestrierung** (`controller`) und abtrennbare Einmal-Klassen (`scripts`).

Der Abhängigkeitsgraph der Pakete liegt separat als `thossuite_packages.dot` (generiert von `PackageDependencyGraph`); bei Bedarf neu erzeugen statt von Hand pflegen.

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

<!-- ANNAHME: Config hierher (DB+Config = Laufzeitverhalten) -->
### ⚙️ Config-System

Zentrale Konfigurationsklasse (`app.shared.Config`) für alle Suite-Einstellungen (Skin,
Sort-Order, Fenster-Positionen …). Singleton mit statischen Methoden. Dirty-Check via
Map-Vergleich: speichert nur bei tatsächlicher Änderung.

**Zugriffsstrategie:** Jede Klasse holt sich selbst, was sie braucht. Der Controller reicht
*keine* Config-Werte durch (das wäre Micromanagement).

## 🧭 Orchestrierung (`controller`) — Mechanik

<!-- ANNAHME: nur die Bauregel-Mechanik; konkrete Screens (Dashboard, Play Mode) stehen im Feature-Details-Dok -->
<!-- ANNAHME: Bildschirm-Kontrakt Screen + SessionSwitchStrategy liegen in shared und könnten nach Fundament wandern -->
### 🔄 Session-Management

#### Bildschirm-Kontrakt `Screen` (`app.shared`)
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

#### SessionSwitchStrategy (`shared.model`)
```java
public enum SessionSwitchStrategy {
    IMMEDIATE,       // sofort wechseln (Dashboard, Statistiken, Play)
    OFFER_SAVE,      // Dialog "Speichern?" (Anki mit Fortschritt)
    CONFIRM_DISCARD  // Dialog "Fortschritt geht verloren!" (Region)
}
```

#### Controller als Orchestrator
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

## 🧱 Fundament (`shared`)

<!-- ANNAHME: aktuell nur Skin-System; Screen-Kontrakt steckt noch unter Orchestrierung/Session-Management -->
### 🎨 UI-Architektur: Skin-System

#### Konzept
Ein Skin = ein komplettes visuelles Design. Der Skin (`app.shared.skin`) trifft alle
Styling-Entscheidungen (Farben, Fonts, Borders) und setzt Positionen/Größen auf die
UI-Komponenten. Die Komponenten kennen **keine** konkreten Farb-/Layout-Werte; der Skin setzt
sie via CSS bzw. von außen.

**Schichtlage:** `shared.skin` hängt nur nach unten an `shared` und `shared.model`. Features und
Lern-Kern hängen ihrerseits am Skin (abwärts). Der Skin kennt **keine** Domänentypen — die Domäne
reicht ihm dumme DTOs/Keys hinab. (Der Skin-Vertrag im Detail steht im Regel-Dokument.)

#### Factory-Methoden
Der Skin baut die atomaren UI-Bausteine, u. a. Eingabefelder, Antwort-Buttons,
`MultipleChoicePane`, `DashboardTile`, DatePicker, gestylte Alerts/Dialoge. Für die Karten gilt:
der Skin liefert Layout/Wrapper, die fachlichen Panes halten ihren Zustand selbst.

#### CSS-Generierung
```java
scene.getStylesheets().add("data:text/css," + encodedCss);
```

#### Reflection-basiertes Property-Laden
Skin-Properties werden ohne Code-Änderung aus `.properties`-Dateien geladen. **Subskin-Vererbung:**
ein abgeleiteter Skin lädt erst die Eltern-Config, dann die eigene.

#### Fallback-System für Layouts
Region-Decks sollen ein gemeinsames Layout teilen:
```
1. spezifisches Layout suchen:  "spainSessionQuestionPanel"
2. nicht gefunden → Fallback:   "geo_deckSessionQuestionPanel"
3. alle Region-Decks teilen den Fallback
```

#### SkinService — zentrale Registry
`getAllSkins()` (Liste aller Skins), `get()` (aktiver Skin), `set(Skin)` (aktivieren),
`refresh()` (neu laden, für Live-Bearbeitung), `setOwnerWindow(Stage)` (vor Dialogen nötig).

> Die `Skin`-Klasse ist derzeit noch groß (CSS-Erzeugung, Property-Laden, Layout-Bounds,
> Komponenten-Bau). Eine Aufteilung ist bewusst aufgeschoben (siehe Regel-Dokument).

## 🧰 scripts

<!-- ANNAHME: Platzhalter — kein Prosa-Abschnitt; Inventar siehe thossuite_packages.dot / scripts-Paket -->

> Die **Paket-, Benennungs- und Abhängigkeitsregeln** (Sorten, Schnitt, Richtungen,
> Domänenpräfix, Skin-Vertrag, Paketgröße) stehen im Dokument **`ThosSuite_Design_Regeln.md`**.

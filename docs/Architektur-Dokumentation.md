# ThosSuite — Architektur (allgemein)

**Stand:** 07.08.2026 — Fast Write hinzugefügt

**Charakter:** Das *immer mitzugebende* Fundament — was bei jeder Aufgabe gilt, egal welches
Feature: Überblick, technische Basis, Paketstruktur, Orchestrierungs-Mechanik, Fundament.
Feature-Details (Lern-Kern, einzelne Features, konkrete Screens) stehen separat in
`Feature-Details.md`. Die *Regeln* (warum so geschnitten) in `Design-Regeln.md`.

## 📋 Überblick

### Was ist ThosSuite?
Eine persönliche All-in-One-Desktop-Anwendung (JavaFX), die mehrere Lebensbereiche abdeckt.
Perminides ist einziger Nutzer und einziger Entwickler.

**Lernen:**
- **Anki-Decks** — karten-basiertes Lernen, bei dem *jede Karte einzeln* ihren
  Spaced-Repetition-Stand trägt (zuletzt beantwortet, Tage bis zur nächsten Fälligkeit).
  Decks: Deutschland, Welt, Multiple Choice, Hannover, Fast Write.
- **Region-Decks** — hier trägt das *Deck als Ganzes* den Lernfortschritt, nicht die
  einzelne Frage. Decks: Autonome Regionen Spaniens, Stadtteile Hannovers, Städte der
  Region Hannover, Bezirke Bayerns, Bundesländer Österreichs, Länder Ozeaniens, Kantone
  der Schweiz, traditionelle Grafschaften Englands, Länder der Karibik, Ortsteile Berlins,
  Bundesstaaten der USA, Regionen Italiens und last but not least die deutschen Bundesländer.
- **Freies Spiel** — jedes Anki- und Region-Deck lässt sich auch außer der Reihe spielen,
  unabhängig von der Fälligkeit und ohne den Lernfortschritt zu verändern (etwa, um Besuch
  etwas zu zeigen).
- Wochentagsberechnung.

Welche Aktionen die Suite dabei erwartet und woraus eine Frage besteht, steht im
Feature-Details-Dokument (Lern-Kern).

**Gesundheit & Tracking:**
- Fitbit-Integration (Schritte, Wochenpunkte, Streak)
- Alkohol-Tracker mit Kontostand-System
- Erinnerung zum Wenden der Matratze

**Film:**
- Import von Film-, Serien- und Episodenbewertungen aus dem TMDB-Account; ein gemeinsamer
  Viewer für alle drei Typen.

**Tagebuch & Nachrichten:**
- Tagebuch mit Bild-Anhängen; Browsen und Durchsuchen über einen tag-basierten Viewer.
- Signal- und WhatsApp-Nachrichten werden inkrementell beim Start in die Suite-DB importiert.

**Dashboard:**
- Key-Metrics auf einen Blick (Fitbit-Streak, Restschritte, Alkohol-Kontostand, etc.)

## 🗝️ Technische Basis

### Paket-Struktur

Alles liegt unter dem Wurzelpaket `app`. Darunter ist die Suite in **Sorten** von Paketen
gegliedert (Details und Begründung im Regel-Dokument): die **Features** (`app.alc`, `app.diary`,
`app.fitbit`, `app.learn`, `app.mattress`, `app.messaging`, `app.movie`, `app.weekday`), das
**Fundament** (`app.shared`) und die **Orchestrierung** (`app.controller`). Daneben liegt `scripts`
als eigenes Wurzelpaket — abtrennbare Einmal-Klassen, die nicht zur Suite gehören.
`controller` darf auf jede Sprosse (außer shared.ui.component); **ein Feature greift nie ins Skin-Paket**. Diese Zusagen sind
keine Absicht, sondern geprüft — siehe „Architekturregeln im Build".

Der Abhängigkeitsgraph der Pakete liegt separat als `docs/Paketabhängigkeiten.dot` (generiert von
`scripts.PackageDependencyGraph`); bei Bedarf neu erzeugen statt von Hand pflegen.

### Framework & Tools
- **JavaFX 25**, **Java 25 LTS**
- **Jackson** für JSON-Parsing (GeoJSON, TMDB, Fitbit)
- **SQLite** für strukturierte Daten. Die Suite besitzt zwei eigene DBs (Suite-DB, Film-DB);
  beim Import wird zusätzlich lesend auf die fremden DBs von Signal und WhatsApp zugegriffen.
- **ArchUnit + JUnit 5 + Surefire** — ausschließlich für die Architekturregeln, siehe unten. Es gibt
  keine Unit-Tests und keine Testbarkeit als Designziel.

### Exception-Handling
RuntimeExceptions werden in der Regel bis `ThosSuiteApp` durchgereicht und dort zentral über
`Thread.setDefaultUncaughtExceptionHandler(...)` behandelt: Alert mit Stacktrace, dann
Prozessende. Ob ein konkreter Laufzeitfehler fatal (globaler Handler) oder lokal behebbar ist,
ist eine Einzelfallentscheidung an der jeweiligen Stelle.

### Nebenläufigkeit
Keine Threads; alles läuft auf dem JavaFX Application Thread. Einzige Ausnahmen: die
Startup-Initialisierung (ein Hintergrund-Thread für Config, Logging, Font-Loading sowie das
Setzen der DB auf `.filen.ignore`) mit Splash-Screen-Pattern und ein unschöner Hack bei der
Anzeige der Tabelle mit Fitbit-Daten. Auch die PreTasks mit externen
API-Calls (Fitbit) laufen nicht in eigenen Threads, sondern über `Platform.runLater` auf dem
FX-Thread.

### Logging
`java.util.logging` (JUL), keine externen Dependencies (sauber mit dem JPMS-Module-System;
Logback scheiterte an der Modul-/Classpath-Trennung).
- FileHandler ab INFO in `{dataFolder}/log/thossuite%u.log` (10 MB, 5 Rotationen)
- Root-Logger auf FINE; der ConsoleHandler reicht alles durch, was der Root durchlässt
- `--debug`: FileHandler loggt auch FINE
- Harte Referenzen auf konfigurierte Logger (JUL nutzt sonst WeakReferences)

API (Klasse `Log` in `app.shared`):
```java
Log.debug(this, "...");      // FINE  – nur Console (mit --debug auch Datei)
Log.info(this, "...");       // INFO  – Console + Datei
Log.warn(this, "...");       // WARNING
Log.error(this, "...", ex);  // SEVERE + Stacktrace
// Auch mit Class statt Object (für statische Methoden)
```

### Startup-Flow
```
main() → launch()
  → init()              // AppClock, setupGlobalExceptionHandler()
  → start()
      → Zeitzonen-Check (Europe/Berlin; sonst Warn-Dialog, ggf. Abbruch)
      → showSplashScreen()
      → getDataFolderFromArgs() oder showDirectoryChooser()
      → SingleInstanceGuard (suite.lock); alte .lck-Locks entfernen
      → Background-Thread:
          → Config.init(), Log.initLog(), FilenIgnoreSource.addToIgnore(), loadFonts()
          → Platform.runLater():
              → initializeMainWindow() (opacity=0 gegen White-Flash)
              → new Controller(mainWindow)
              → controller.runPreTasks()     // externe APIs: Fitbit-Fetch, Film-Import (TMDB)
              → mainWindow.show()
              → PauseTransition (CSS-Settle) → splashStage.close()
              → controller.runPostTasks()    // lokale Tasks, MainWindow noch unsichtbar
              → primaryStage.setOpacity(1)   // MainWindow erst jetzt sichtbar
```

**PreTasks** laufen während des Splashscreens und sprechen externe Quellen an: Fitbit-Fetch
und der Film-Import (TMDB). Beide nur, wenn die Suite nicht im Offline-Modus läuft
(Config-Flag `offline`).

**PostTasks** laufen nach geschlossenem Splash, aber noch bei unsichtbarem Hauptfenster
(opacity=0) — die Startup-Dialoge erscheinen also *vor* dem Fenster. Erst danach wird das
MainWindow sichtbar. PostTasks: Fitbit-Review-Dialoge, Alkohol-Tagesabfrage, Tagebuch-Prompt,
Wochentagsberechnung, Matratzen-Erinnerung, Film-Cleanup, die inkrementellen
Signal-/WhatsApp-Importe sowie das Verkleinern der Lern-Bilder.

### Datenbankzugriff & Connection-Management
Die `DB`-Klasse (`app.shared`) verwaltet Connections zu **zwei** SQLite-Datenbanken — der
Suite-DB und der separaten Film-DB. Für jede DB gibt es dasselbe Paar: eine Singleton-Connection
zum normalen Lesen/Schreiben und eine transaktionale Connection pro Aufruf für Importe und
Bulk-Writes.

**Singleton (`getConnection()`, `getTmdbConnection()`) — AutoCommit true.** Für alle normalen
Lese-/Schreibzugriffe. Bleibt die gesamte Laufzeit offen und wird **nie** von Consumern
geschlossen — nur einmal beim Shutdown über `DB.closeConnection()` aus `ThosSuiteApp.stop()`.
Lazy beim ersten Aufruf initialisiert.

**Transaktional (`getNewConnection()`, `getNewTmdbConnection()`) — neue Connection ohne
AutoCommit pro Aufruf.** Für Importe (Signal, WhatsApp) und performance-relevante Bulk-Writes
(viele Spielstände einer Session). Bündelt viele Writes in einer Transaktion. Der Aufrufer
schließt sie per try-with-resources und ist für `commit()`/`rollback()` verantwortlich.

**Kritische Regel — Statements und ResultSets immer schließen.** Ein offenes `ResultSet`/
`Statement` auf einer Singleton-Connection verhindert spätere Commits auf der zugehörigen
transaktionalen Connection (`SQLITE_BUSY`). Alle `Statement`/`ResultSet` zwingend per
try-with-resources schließen — auch beim reinen Lesen. Dies gilt natürlich nicht für die Connection, die kann und soll gern offen bleiben.

```java
Connection conn = DB.getConnection();
try (PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // ...
}
```
### Externe Attachments

Anhänge — Tagebuch-Bilder wie Nachrichten-Anhänge — liegen **außerhalb** des Suite-Ordners
unter dem Pfad aus `attachments.folder` in der config-Datei; in der DB steht nur der Dateiname, nie der Inhalt selbst.

Grund ist Portabilität: Die Suite lässt sich kopieren (etwa auf einen Stick), ohne die
gesammelten Anhänge mitzuschleppen. Eine kopierte Instanz hat die Dateien dann schlicht nicht —
ein fehlendes Attachment ist damit **erwartetes** Verhalten und *kein* FailFast-Fall. Wie ein
Feature auf das Fehlen reagiert (tolerieren beim Anzeigen; hart abbrechen bei einem nur lokal
laufenden Import), steht beim jeweiligen Feature.

### Code-Generierung
Code nur auf expliziten Zuruf generieren — hält Chats kurz und Diskussionen im Fokus.

### Config-System

**Eine Fassade, zwei Stores.** `Config` (`app.shared`) ist die einzige öffentliche Tür zu
allen Suite-Werten — statische Klasse, privater Konstruktor, global erreichbar (der Controller
reicht deshalb keine Werte durch Konstruktoren). Dahinter liegen zwei package-private Stores,
die kein Aufrufer direkt sieht:

- **`ConfigFileSource`** liest beim Start die config-Datei und hält deren rohe Werte zusammen
  mit den daraus abgeleiteten (computed) Ordnerpfaden in einer Map. **Read-only**
- **`KeyValueRepository`** bedient die `key_values`-Tabelle: veränderliche Laufzeitwerte, die
  Neustarts überdauern (Import-Zeitpunkte, Datei-Hashes, UI-Präferenzen wie Sortierreihenfolge
  oder zuletzt gewähltes Skin).

**Routing.** Die Fassade kennt als Einzige die Partition und leitet danach: Key in der
unveränderlichen Menge (Datei oder computed) → `ConfigFileSource`, sonst → `key_values`. Der
Aufrufer weiß nie, woher ein Wert kommt; er liest und schreibt nur an der Fassade.

**Kontrakt — throw on miss.** Ein unbekannter Key ist per Design ein Bug, kein abzufangender
Fall. Jeder Key wird vor seinem ersten Lauf mit Startwert angelegt — dieselbe Disziplin für
Datei- wie Tabellen-Keys. Es gibt kein `Optional`. Wenn Du doch mal einen optionalen Key
hast, der nicht existieren muss, dann kannst Du diesen mittels get(key, null), also mit null
als Default-Wert, ermitteln und anschließend auf null checken.

**Schreiben.** `set`/`delete` auf einen Tabellen-Key gehen an `key_values`; dieselben Aufrufe
auf einen Datei-/computed-Key **werfen sofort** (FailFast) — nur die Fassade kennt die volle
Partition und erkennt diesen Fall als Bug.

**Typisierung zentral.** Beide Stores liefern rohe Strings; die Umwandlung (String →
`Path`/`int`/`LocalDateTime`) passiert einmal in der Fassade (`getPath`, `getInt`, `getTime`,
`getDaysSince`). Ein falsches Format crasht damit zentral und sofort statt an zwanzig Stellen.
Die Default-Getter (`get(key, default)`, `getInt(key, default)`) fragen bewusst nur die
unveränderliche Menge — Tabellenwerte sind vorab angelegt und kennen keinen Default.

**Bootstrap — streng linear.** `Config.init(folderPath)` baut zuerst den `ConfigFileSource`
(Datei gelesen, computed Pfade da), leitet daraus die zwei DB-Pfade ab und reicht sie an
`DB.init(...)`, erzeugt dann das `KeyValueRepository` und prüft zuletzt die Startup-Invariante.
`DB` zieht seine Pfade **als Parameter** und hängt an niemandem — kein Rückaufruf in die Config,
die Konstruktionsreihenfolge bleibt zyklenfrei.

**Startup-Invariante — Kollisions-Check.** Kein Tabellen-Key darf den Namen eines Keys aus der
unveränderlichen Menge tragen; die Fassade prüft das einmal beim Start und crasht bei Kollision.
Die DB-Bereitschaft braucht keinen eigenen Guard: Tabellen-Keys laufen ohnehin über die DB, die
zu diesem Zeitpunkt bereits initialisiert ist.


## 🧭 Orchestrierung (`controller`) — Mechanik

### Rolle des Controllers
Der `Controller` ist die zentrale Event-Drehscheibe: Beim Aufbau registriert er sich für
sämtliche Menü- und Tastatur-Events des MainWindow (Lernen starten, Play, Statistiken, Skin
wechseln, Sortierung, Tagebuch, Export …). Das MainWindow kennt den Controller nicht direkt — es
ruft die hinterlegten Callbacks.

Er hält **genau einen aktiven Screen** (`currentScreen`) und routet die laufenden Events dorthin:
ESC, Pause, Sortier-Wechsel gehen an den aktuellen Screen, der entscheidet selbst, ob er reagiert.
Ein Screen-Wechsel ersetzt diesen einen aktiven Screen (siehe Session-Management). Daneben
orchestriert der Controller den Skin-Wechsel zur Laufzeit (MainWindow neu aufbauen, aktuellen
Screen refreshen).

Die externen Anstöße beim Start (Pre-/PostTasks) laufen ebenfalls über den Controller, sind aber
oben im Startup-Flow beschrieben.

### Session-Management

#### Bildschirm-Kontrakt `Screen` (`app.shared.model`)
Der neutrale Vertrag, über den der Controller jeden aktiven Inhalt gleich behandelt — Lern-Sessions
wie Nicht-Lern-Screens (`AlcStatisticsScreen`, `DashboardScreen`, `MovieViewerScreen` …). Drei
Methoden sind Pflicht, weil der Controller sie immer braucht: `getSwitchStrategy()`, `refresh()`,
`getView()`. Der Rest sind leere Defaults — ein Screen implementiert nur, was ihn betrifft.
Reagiert einer auf `sort()` nicht, ist das **kein** Fehler, sondern beabsichtigte
Nicht-Zuständigkeit (kein FailFast). Vollständige Signaturen im Code.

Jeder Screen hält seine Anzeige als `ScreenView` und reicht sie über `getView()` weiter.

#### Wechsel zwischen Screens
Jeder Screen meldet über `getSwitchStrategy()`, wie mit ihm beim Verlassen zu verfahren ist:

- **`IMMEDIATE`** — sofort wechseln (Dashboard, Statistiken, Play-Sessions)
- **`OFFER_SAVE`** — Dialog Speichern/Verwerfen/Abbrechen (Anki mit Fortschritt)
- **`CONFIRM_DISCARD`** — Warnung „Fortschritt geht verloren" (Region)

Will der User einen neuen Screen starten, ruft der Controller `requestSessionSwitch(...)` und
übergibt die Routine, die den neuen Screen aufbaut. Je nach Strategie des *aktuellen* Screens
läuft die Routine sofort, erst nach einem Speichern-Dialog oder erst nach einer Verwerfen-Warnung
— oder gar nicht, wenn der User abbricht.

Übergeben wird bewusst die *Aufbau-Routine*, nicht die fertig aufgebaute neue Session. Würde man
die neue Session schon vorab erzeugen, um sie dann zu übergeben, wäre dieser Aufbau umsonst, sobald
der User im Dialog abbricht. So wird erst gebaut, wenn die Entscheidung für den Wechsel steht.

**Solange ein solcher Dialog offen ist, ist der aktuelle Screen suspendiert** (`suspend()`, bei
Abbruch `resume()`). Der Grund: `showAndWait` startet eine verschachtelte Event-Schleife, in der die
Session weiterläuft, obwohl der Nutzer gar nicht mehr bei ihr ist — eine laufende Uhr etwa würde
ablaufen und die offene Karte als falsch werten. Was das Suspendieren bedeutet, entscheidet jeder
Screen selbst; die meisten haben nichts zu tun.

#### Rückmeldung ans Ende
Feature-Klassen und Lern-Sessions kennen den Controller **nicht**. Beim Erzeugen gibt der
Controller der Session einen End-Callback mit (`sessionEnded`); läuft die Session aus, ruft sie
ihn — der Controller räumt dann auf (Menü-Labels aktualisieren, leerer Hintergrund). Die Richtung
bleibt einseitig: Die Session ruft einen Callback, statt den Controller zu kennen.

## 🧱 Fundament (`shared`)

### UI-Architektur: Anzeige-Schicht und Skin

#### Konzept

Ein Skin = ein komplettes visuelles Design. **Der Skin baut nichts.** Er hält die Werte, die CSS
nicht ausdrücken kann (Rechtecke, Fonts, Border, Pfade), und erzeugt daraus das Stylesheet. Gebaut
wird eine Sprosse darüber, in `shared.ui`.

Das ist die Umkehrung des früheren Zustands, in dem der Skin die Komponenten selbst herstellte und
positionierte. Der Grund für die Umkehrung: so lässt sich als Regel durchsetzen, dass der Skin die
UI **nicht kennt** — und diese Regel ist per Build prüfbar, die alte war es nicht.

#### Die zwei Klassen im Skin-Paket

```
SkinProperties     Über 100 Felder · lädt sie aus der .properties-Datei · gibt sie über eine
                   bewusst geschnittene Fläche heraus
Skin               extends SkinProperties · erzeugt das CSS
                   genau eine öffentliche Methode: styleScene(Scene)
```

Dazu die sechs konkreten Skins (`DarkMode`, `FlatWebSkin` und die vier
Farbvarianten) — sie tragen nichts als einen Anzeigenamen und den Namen ihrer properties-Datei.
Plus `SkinService` als Registry und `SkinImageCache` für die großen Kartenbilder.

#### Was der Skin herausgibt

Keine Feld-Getter, keine Property-Namen — **zweckgeschnittene Records und Werte**:

Wer welchen Wert **holt** und wer ihn **übergeben bekommt**, regelt die Schlüssel-Regel im
Regel-Dokument: braucht der Zugang ein Argument vom Aufrufer, löst der Aufrufer auf.

#### CSS-Generierung

```java
scene.getStylesheets().add("data:text/css," + encodedCss);
```

`styleScene` baut den Stylesheet-String aus diversen `addXxxStyles`-Methoden über einen `CssBuilder` und
hängt ihn als Data-URL an die Scene. Bei einem Skinwechsel wird die Scene neu gestylt und alle
Oberflächen bauen sich neu auf (`Screen.refresh()` → `view.rebuild()`).

#### Reflection-basiertes Property-Laden

Skin-Properties werden ohne Code-Änderung aus `.properties`-Dateien geladen: `loadAllConfigs` läuft
die deklarierten Felder ab und sucht zu jedem Feldnamen einen gleichlautenden Schlüssel.
**Subskin-Vererbung:** ein abgeleiteter Skin lädt erst die Eltern-Config, dann die eigene; das
Spätere gewinnt.

Achtung: der Loader kennt nur `Color`, `Font`, `BorderParams`, `Integer`, `Rectangle2D` und
`String`. Ein Schlüssel für einen anderen Feldtyp würde **still ignoriert**.

#### Fallback-System für Layouts

Decks sollen sich ein Layout teilen können. Die Staffelung steht genau einmal, in
`SkinProperties.staffelung(…)`:

```
1. spezifisch:   esSessionQuestionPanel        (mapName des Decks)
2. Kategorie:    regionSessionQuestionPanel    (anki | region)
3. sonst:        null
```

Dieselbe Staffelung gilt für die Hintergrundbilder, dort mit einer dritten Stufe:
`<mapName>WallpaperName` → `<kategorie>WallpaperName` → `emptyWallpaperName`.

Die Property-Namen (`…SessionQuestionPanel`) bleiben unverändert und verlassen das Skin-Paket nie —
nach außen heißt der Bestandteil `LearnComponent.QUESTION`.

#### SkinService — zentrale Registry

`getAllSkins()` (Liste aller Skins), `get()` (aktiver Skin), `set(Skin)` (aktivieren), `refresh()`
(neu laden, für Live-Bearbeitung). Das Owner-Fenster für Dialoge liegt **nicht** hier, sondern in
`UiUtils`.

#### Die Anzeige-Schicht `shared.ui`

```
shared.ui                    Oberflächen — Screens, Dialoge, Lern-Ansichten, ComponentHost
shared.ui.components         Bausteine — SuiteTextField, SuiteDialog, DiaryCard, MultipleChoicePane …
shared.ui.components.map     die zwei Karten samt Innenleben
```

Ob etwas in die Wurzel oder zu den Bausteinen gehört, entscheidet die Frage *wird es eingebaut oder
ist es das Fertige* — Details im Regel-Dokument.

**Bausteine erben von ihrem JavaFX-Typ**, sie verhüllen ihn nicht: ein `SuiteTextField` *ist* ein
`TextField`. Wo JavaFX das verbietet (`Background` und `BackgroundImage` sind `final`), steht
stattdessen eine Fabrik: `SuiteBackground.of(pfad)`.

#### Architekturregeln im Build

Werden von ArchUnit beim mvn-build überwacht. Details siehe Regeldokument.

**Achtung bei ArchUnit-Versionen:** vor 1.4.1 kennt der Bytecode-Leser Klassendateiversion 69
(Java 25) nicht und überspringt sie **still**. Die Regeln melden dann „failed to check any classes"
statt eines Verstoßes.

## 🧰 Externes
Einmalige, abtrennbare Standalone-Klassen im Paket `scripts` (Migrationen, Fixes, Prototypen,
manuelle Tests) — sie laufen einmal und gehören nicht zum Produktivcode: nicht mitgebaut, nicht
im Build-Ergebnis. Das konkrete Inventar steht nicht hier, sondern ergibt sich aus dem
`scripts`-Paket bzw. `docs/Paketabhängigkeiten.dot`.

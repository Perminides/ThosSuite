# Codebase-Analyse ThosSuite (Außensicht)

Stand: 2026-06-09 · Basis: 209 Java-Dateien unter `src/main/java`, Maven-Build (`pom.xml`), JavaFX 25.
Reine Code-Analyse, keine Änderungen vorgenommen. Vorhandene Architektur-Dokumente wurden bewusst ignoriert.

---

## 1. Null automatisierte Tests bei 209 Klassen

`src/test` existiert, ist aber leer. Im `pom.xml` gibt es nicht einmal eine Test-Dependency (kein JUnit, kein TestFX). Dabei enthält die Codebase viel rein algorithmische, gut testbare Logik:

- `app/learn/anki/CardProgress.java` (303 Zeilen Lernlogik / Spaced-Repetition-Zustandsmaschine)
- `app/fitbit/PointsCalculator.java`
- `app/messaging/whatsapp/WhatsAppCrypt15Decryptor.java` (292 Zeilen Kryptografie — ein Fehler hier zerstört Importe still)
- `app/movie/Importer.java` / `SeriesImporter.java` (Rolling-Check-Logik mit Seitenzählern und Timestamps)

Jede Regression fällt erst zur Laufzeit beim manuellen Durchklicken auf. Das ist das größte strukturelle Risiko des Projekts, weil es alle anderen Punkte verschärft: Refactorings (z. B. der Skin-Klasse, Punkt 4) sind ohne Sicherheitsnetz kaum verantwortbar.

## 2. Blockierende I/O auf dem JavaFX-UI-Thread

Es gibt in der gesamten Anwendung genau einen selbst gestarteten Hintergrund-Thread (`app/ThosSuiteApp.java:143`, Splash-Init). Alles andere läuft auf dem FX Application Thread:

- `app/controller/Controller.java:119-125` (`runPreTasks`): Fitbit-HTTP-Fetch (`DataFetcher.fetch()`) und kompletter TMDB-Import (`new Importer().run()`) — synchrone Netzwerk- und DB-Arbeit. Der Kommentar gibt es selbst zu: *„Holt Daten im UI-Thread, blockiert aber die App"*.
- `app/controller/Controller.java:131-176` (`runPostTasks`): `SignalIncrementalImport` (415 Zeilen) und `WhatsAppIncrementalImport` (499 Zeilen, inkl. Crypt15-Entschlüsselung), danach `ImageScaler.processImages()` — alles sequentiell im UI-Thread.
- `app/movie/SeriesImporter.java` (782 Zeilen) wird über den Menüpunkt „Erweiterter TMDB-Import" ebenfalls direkt im Event-Handler ausgeführt (`Controller.java:374-376`).

Folge: Bei langsamem Netz oder großen Importen friert die UI ein; Windows meldet „Keine Rückmeldung". `javafx.concurrent.Task`/`Service` werden nirgends genutzt. Die `hs_err_pid*.log`-Dateien im Repo-Root deuten darauf hin, dass Abstürze/Hänger kein theoretisches Problem sind.

## 3. Globaler statischer Zustand mit fragiler Initialisierungsreihenfolge

`Config`, `DB`, `SkinService` und `Log` sind statische Singletons, die sich gegenseitig in **statischen Initialisierern** konsumieren:

- `app/shared/DB.java:19-22`: statischer Block liest `Config.get("dbFolder")`. Wird die Klasse `DB` vor `Config.init(...)` geladen, ist der Pfad `"null" + "thossuite.db"`.
- Der Beweis, dass das real passiert, liegt im Repo selbst: Die Dateien **`nullthossuite.db` und `nullmovies.db` im Projekt-Root sind eingecheckt** — von SQLite angelegte Datenbanken mit dem String „null" als Verzeichnis-Präfix.
- `app/shared/skin/SkinService.java:24-37`: noch ein statischer Block, der `Config.get("pref_skinClass")` liest — gleiche implizite Reihenfolge-Abhängigkeit.
- `app/shared/Config.java`: `public static String ROOT` ist ein öffentlich beschreibbares statisches Feld.

Konsequenzen: Klassenladereihenfolge wird zur unsichtbaren Korrektheitsbedingung, nichts davon ist isoliert testbar (siehe Punkt 1), und jeder `app.scripts`-Main, der eine Repository-Klasse anfasst, reproduziert den `null*.db`-Bug.

## 4. `Skin.java`: 2402-Zeilen-Gottklasse mit Reflection-Konfigurationssystem

`app/shared/skin/Skin.java` ist mit 2402 Zeilen mehr als dreimal so groß wie die zweitgrößte Klasse und vereint mindestens fünf Verantwortlichkeiten: CSS-Generierung, Widget-Factory (Menüs, Buttons, Dialoge, HeaderBar), Alert-Erzeugung, Karten-/Layout-Berechnung und Konfigurations-Laden.

Das Konfigurationssystem basiert auf Reflection über Feldnamen:

- `Skin.java:2378-2404` (`loadAllConfigs`): iteriert per `getDeclaredFields()`/`setAccessible(true)` über die Klassenhierarchie und befüllt `protected`-Felder aus `.properties`-Dateien.
- `Skin.java:2354-2372`: Zugriff per String-Konkatenation, z. B. `getFieldValue(mapName + "MapImageName")`.

Ein Tippfehler in einem Properties-Schlüssel oder ein umbenanntes Feld fällt erst zur Laufzeit (oder gar nicht, weil `value == null` still übersprungen wird) auf. IDE-Refactorings (Rename Field) brechen das System unbemerkt. Die Klassen-Javadoc dokumentiert sogar eine Konvention für **absichtliche CSS-Code-Duplikation** mit Warnkommentaren (`Skin.java:124-129`) — ein Hinweis, dass die Struktur an ihre Grenze gekommen ist.

## 5. Echte Bugs als Symptom fehlender Typsicherheit im Controller-Dispatch

- **Copy-Paste-Bug:** `app/controller/Controller.java:286-298` (`onStatisticsMenuItemSelected`): Der Menüpunkt „Fitbit" öffnet `AlcStatisticsScreen` statt `FitbitStatisticsScreen` — die Klasse `app/fitbit/FitbitStatisticsScreen.java` existiert, wird aber von nirgends instanziiert.
- **NPE-Falle ebenda:** Kommt ein unbekannter String an, bleibt `currentScreen` auf dem bereits geschlossenen alten Screen bzw. `null`, und `currentScreen.getView()` (Zeile 295) wirft eine NPE. Ursache ist der stringly-typed Dispatch: `MainWindow` reicht `"Dashboard"`/`"Fitbit"`/`"Alkohol"` als nackte Strings durch (`MainWindow.java:177-184`) statt eines Enums.
- **Garantierte NPE in `Config`:** `app/shared/Config.java:82-85`: `public static int getInt(String key) { return value != null ? Integer.parseInt(value) : null; }` — bei fehlendem Schlüssel wird `null` in ein `int` unboxed → `NullPointerException` statt verständlicher Fehlermeldung.
- **Doppelter Aufruf:** `app/learn/region/SessionPresenter.java:94-95`: `sessionPane.addIdsToCorrect(Set.of(id));` steht zweimal direkt hintereinander.
- Gleiches Muster bei der Sortierreihenfolge: `MainWindow.setCurrentSortOrder` (`MainWindow.java:417-421`) identifiziert Menüeinträge über den **Anzeigetext** (`item.getText().equals(sortOrder.getDisplayName())`) — eine Umbenennung im UI bricht die Logik.

## 6. DB-Schicht: geteilte Singleton-Connection, Ressourcen-Lecks und UI-Code im Datenzugriff

`app/shared/DB.java` hat gleich mehrere strukturelle Probleme:

- **UI im DB-Layer:** `DB.java:48` und `DB.java:92` zeigen bei geschlossener Connection einen JavaFX-`Alert` („Shit. Die connection ist closed? Wer ist der Übeltäter?") — eine Schichtenverletzung, die zudem crasht, wenn `getConnection()` jemals außerhalb des FX-Threads aufgerufen wird.
- **Statement-Leak:** `connection.createStatement().execute("PRAGMA foreign_keys = ON")` (`DB.java:52, 78, 96, 110`) — das `Statement` wird nie geschlossen, und das PRAGMA läuft bei jedem `getConnection()`-Aufruf erneut.
- **Irreführende Einrückung:** `DB.java:50-52`: das `if` ohne Klammern, die PRAGMA-Zeile ist eingerückt, als gehöre sie zum `if` — tut sie nicht.
- **Verantwortung beim Aufrufer:** Die Javadoc (`DB.java:39-43`) verlangt, dass jeder Aufrufer ResultSets per try-with-resources schließt, sonst `SQLITE_BUSY` — bei ~40 Repository-Klassen ein unkontrollierbarer Vertrag. Keine Synchronisation, obwohl mit `ThosSuiteApp.java:143` mindestens ein zweiter Thread existiert.

## 7. Parallelstrukturen statt Abstraktion: `learn.anki` vs. `learn.region` und vier Klassen namens `Repository`

Die Pakete `app/learn/anki` und `app/learn/region` enthalten je eine fast identische Klassenfamilie: `SessionPresenter`, `SessionProgress`, `SessionPane`, `*DeckService`, `*PlayConfigDialog`, `Db*ProgressSource` — gleiches Muster (Presenter hält `sessionPaneContainer`, baut Pane bei Skin-Wechsel neu, identische Javadoc inkl. „The MainWindow won't realize this :-)" in `learn/anki/SessionPresenter.java:18-19` und `learn/region/SessionPresenter.java:13-14`), aber ohne gemeinsame Basisklasse oder Interface. Jede Änderung am Session-Lifecycle muss zweimal gemacht werden.

Dazu kommt ein Namenskonflikt-Antipattern: vier Klassen heißen schlicht `Repository` (`app/alc/repository/`, `app/diary/repository/`, `app/fitbit/repository/`, jeweils `Repository.java`), zweimal `SessionPresenter`, zweimal `SessionProgress`, zweimal `SessionPane`, zweimal `ApiClient` (movie, fitbit). Das erzwingt voll qualifizierte Importe, macht Stacktraces und IDE-Navigation mehrdeutig und Verwechslungen wahrscheinlich.

## 8. Fehlerbehandlung: 195× `catch (Exception)`, alles wird zur `RuntimeException`, App-Exit als Default

- 195 `catch (Exception ...)`-Stellen in 41 Dateien. Das durchgängige Muster (z. B. `app/movie/repository/MovieRepository.java:63-65` und 18 weitere Stellen allein in dieser Datei): fangen, in `RuntimeException` wrappen, weiterwerfen.
- Der globale Handler (`app/ThosSuiteApp.java:362-401`) zeigt dann einen Stacktrace-Alert und beendet die Anwendung per `Platform.exit()`. Jeder nicht bedachte Randfall (etwa die NPE aus Punkt 5) ist damit ein **Totalabsturz mit Stacktrace-Popup für den Endnutzer**.
- Der Spezialfall ebenda ist zudem falsch herum gebaut: Der ScenicView-Check (`ThosSuiteApp.java:393-396`) kommt **nach** `alert.showAndWait()` — das Popup erscheint also auch für Exceptions, die man ignorieren wollte; nur der Exit wird unterdrückt.
- Checked Exceptions werden systematisch wegmaskiert; es gibt keine Unterscheidung zwischen erwartbaren Fehlern (Netz weg, Datei fehlt → Nutzerhinweis + weiterlaufen) und Programmierfehlern.

## 9. Verdrahtung MainWindow ↔ Controller über 17 einzeln gesetzte Callbacks

`app/controller/MainWindow.java:49-65` hält 17 `Runnable`/`Consumer`/`Supplier`-Felder, die der Controller einzeln per Setter injiziert (`Controller.java:81-97`). Probleme:

- **Kein Vertrag:** Vergisst man einen Setter, gibt es eine NPE erst beim Klick (`itemSave.setOnAction(_ -> onSaveSelected.run())`, `MainWindow.java:135` — ohne Null-Check; andere Stellen wie `MainWindow.java:241` prüfen auf `null`, inkonsistent).
- **Henne-Ei-Hacks:** `MainWindow.java:143-146` dokumentiert selbst einen „eklig"en Workaround, weil beim ersten `buildMenuBar` der Controller noch nicht existiert (`sortOrderSupplier == null`). Der Kommentar in `Controller.java:80` stellt die Architekturfrage gleich mit („Außer Claude erklärt mir, was an dieser zirkulären Beziehung nun so gefährlich sein soll...").
- Der Anfangszustand der Sortierung muss an zwei Stellen gesetzt werden — als bekanntes Problem markiert (`Controller.java:68`: „Gruselig!", plus auskommentierter Code `Controller.java:387-388`).

Ein schmales Interface (`MainWindowListener`/Events) oder die im Kommentar erwogene direkte Referenz wären beide konsistenter als der aktuelle Mischzustand.

## 10. Repo- und Build-Hygiene: toter Code wird mit ausgeliefert, Artefakte sind eingecheckt

- **Stale JAR-Exclusion:** `pom.xml:110-120` schließt `app/misc/**` aus der JAR aus — das Paket heißt aber inzwischen `app/scripts`. Damit werden 33 Klassen mit eigener `main`-Methode (UI-Experimente wie `ShadowDemo`, `MemoryTestHelloWorld`, einmalige Migrationen wie `DiaryMigration`, `AlcoholCsvMigration`, sowie `WhatsAppInitialImport` inkl. Krypto-Pfaden) **mit ausgeliefert**.
- **Doppelte Dependency:** `jackson-databind` ist zweimal deklariert (`pom.xml:22-26` und `pom.xml:32-36`).
- **Hartkodierter Benutzerpfad** als jpackage-Startargument: `pom.xml:227` (`C:/Users/permi/Documents/...`) — der Build ist damit an einen konkreten Rechner gebunden.
- **Eingecheckte Artefakte:** `nullmovies.db`, `nullthossuite.db`, `suite.lock` sind in Git getrackt (Symptom von Punkt 3); im Arbeitsverzeichnis liegen neun `hs_err_pid*.log`-Crashdumps.
- **Preview-Abhängigkeit:** Die App setzt auf JavaFX-Preview-Features (`-Djavafx.enablePreview=true` in `pom.xml:220`, `StageStyle.EXTENDED`/`HeaderBar` in `MainWindow.java:76,116` mit `@SuppressWarnings("deprecation")`) — jedes JavaFX-Update kann das Hauptfenster brechen.
- 66 markierte Schuldenkommentare (`!Sofort`/`!Später`/`!Architektur`/`TODO`) in 30 Dateien, viele davon offene Fragen im Produktivcode (z. B. `Controller.java:238`: „Warum ist das auskommentiert???").

---

## Priorisierung aus Außensicht

1. **Sofort beheben (Bugs):** Punkt 5 (Fitbit-Menü öffnet Alkohol-Screen, `Config.getInt`-NPE) — kleine, klar umrissene Fixes.
2. **Kurzfristig:** Punkt 10 (pom aufräumen, Artefakte aus Git entfernen) — billig und risikolos.
3. **Mittelfristig:** Punkt 1 + 2 (Testfundament für die algorithmischen Kerne, dann Importe auf `javafx.concurrent.Task` umstellen) — Tests zuerst, damit die Thread-Umstellung abgesichert ist.
4. **Langfristig:** Punkte 3, 4, 6, 7 (statics entkoppeln, Skin- und Session-Architektur) — nur mit Testabdeckung sinnvoll angehbar.

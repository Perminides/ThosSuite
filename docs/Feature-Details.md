# ThosSuite — Feature-Details

**Stand:** 28.06.2026 (Struktur) — Inhalte noch ungeprüft (Phase 2 ausstehend).
**Charakter:** Das *Nachschlagewerk* — pro Feature und Lern-Kern, wie es konkret gebaut ist. Nur
bei Bedarf mitgeben (der relevante Abschnitt). Das immer-Fundament steht in
`ThosSuite_Architektur_allgemein.md`, die *Regeln* in `ThosSuite_Design_Regeln.md`.

## 🧭 Orchestrierung — konkrete Screens & Play Mode

<!-- ANNAHME: dieser Einleitungssatz beschrieb ursprünglich alle drei Statistik-Screens (jetzt verteilt) -->
Die drei Statistik-Oberflächen liegen jeweils in ihrem Feature bzw. der Orchestrierung und
implementieren `Screen` mit `SessionSwitchStrategy.IMMEDIATE`.

### DashboardScreen (`controller`)
`FlowPane` mit Kacheln (`SkinService.get().createDashboardTile()`): Restschritte des Tages
(via `fitbit.DashboardService`), aktueller Fitbit-Streak in Wochen (+ Rekord), aktueller
Alkohol-Kontostand.

<!-- ANNAHME: Play Mode hier (Detail); Config-Dialoge liegen in learn.anki/learn.region, Menü-Knoten in controller -->
### 🎮 Play Mode (Freies Spiel)

Neben dem regulären Lernmodus (fällige Items) gibt es einen Spielen-Modus **ohne**
Fortschritts-Speicherung.
- **`AnkiPlayConfigDialog`** (`learn.anki`): Kartenbereich (Min/Max-Index), max. Kartenzahl,
  Label-Filter; Session startet mit `isPlaySession=true` → kein Speichern.
- **`RegionPlayConfigDialog`** (`learn.region`): mehrere Decks kombinierbar; Modus CLICK/
  ELIMINATION/WRITE.
- **`PlayMenuItem` / `PlayMenuNode`** (`controller`):
  `record PlayMenuItem(String label, Object payload) implements PlayMenuNode {}` — `payload` ist
  ein `Deck` (Anki) oder `DeckCategory.REGION_DECK` (Region).

## 🎓 Lern-Kern (`learn`)

<!-- ANNAHME: interne Reihenfolge (Map, Deck, Region, Spaced Repetition, DB) vorläufig -->
### 🗺️ Map-System

#### GeoMap — einheitlicher Map-Container (`learn.model`)
```java
private final Map<String, ShapeMap> shapes;
private final MapType type;
private final Image backgroundImage, overlayImage, inactiveBackgroundImage, inactiveOverlayImage;
```
**MapType:** `SHAPE` (klassische Shape-Maps — Shapes *sind* die Map) und `IMAGE` (navigierbare
Bild-Map wie Welt — Bild ist die Map, Shapes sind Overlay). `GeoMap` kann zur Laufzeit auch
`size|x|y`-Kürzel in einen passenden Kreis-Shape übersetzen.

#### ShapeMap — einheitliches Shape-Objekt (`learn.model`)
```java
public record ShapeMap(String id, String deckId, String regionName, String capitalName,
    Set<String> altRegionNames, Set<String> altCapitalNames, String type,
    Node shape, boolean isShapeMap)
```
Enthält den JavaFX-`Node` und die Attribute aus der GeoJSON. Der Node trägt die CSS-Klasse
`map-shape` bzw. `image-map-shape`. `type` gibt an, ob das Shape spielbar ist (`"0"`) oder welche
Deko-CSS-Klasse greift (`"1"`, `"2"`, `"3"`).

#### MapMetadata — alle verfügbaren Karten
```
GERMANY → germany.geojson + germany states.geojson (SHAPE)
ITALY · SPAIN · USA · CARIBBEAN · ENGLAND · BERLIN · SCHWEIZ · OZEANIEN · AUSTRIA · BAVARIA (SHAPE)
HANNOVER_STADTTEILE · HANNOVER_REGION (SHAPE)
WORLD → worldAreas.geojson + worldCountries.geojson + worldLines.geojson (IMAGE)
```

#### GeoJSON-Properties — alle Daten in einer Datei
```json
{ "type": "Feature",
  "properties": { "id": "14522", "deckId": "lk_hs", "regionName": "Lahn-Dill-Kreis",
                  "capitalName": "Wetzlar", "altRegionNames": "Lahn-Dill, Lahn Dill",
                  "altCapitalNames": "", "type": 0 },
  "geometry": { ... } }
```

### 📚 Deck-System (Enum `Deck`)

#### Anki-Decks
| Deck | ID | Karte |
|------|----|-------|
| GERMANY_CARDS | germany | Deutschland-Karte |
| MC_CARDS | mc | Multiple Choice |
| WORLD_CARDS | world | Weltkarte |
| HANNOVER_CARDS | hannover | Hannover-Karte |

#### Region-Decks (Bundesländer)
Alle nutzen `MapMetadata.GERMANY` und `DeckCategory.REGION_DECK`:
`lk_sh, lk_bw, lk_bn, lk_bs, lk_bb, lk_hs, lk_mvp, lk_ns, lk_nrw, lk_rp, lk_sl, lk_sc, lk_sa, lk_th`

#### Region-Decks (International)
SPANIEN (es) · ITALIEN (it) · USA (us) · CARIBBEAN (cs) · ENGLAND (en) · SCHWEIZ (ch) ·
OZEANIEN (oz) · AUSTRIA (au) · BAVARIA (br)

#### Region-Decks (Hannover & Berlin)
| Deck | ID | Besonderheit |
|------|----|--------------|
| HANNOVER_STADTTEILE | hs | kein Kapital (`hasCapital=false`) |
| HANNOVER_REGION | hr | kein Kapital |
| BERLIN_WEST/NORD/MITTE/OST | be_we / be_no / be_mi / be_os | Teile von berlin.geojson |

Das `mapName`-Feld (`be` für alle Berlin-Decks) steuert, welche GeoJSON und welches
Skin-Layout genutzt werden.

### 🎮 Region-Decks

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

### 📈 Spaced Repetition

`Progress` (`app.learn`) mit Default-Methode:
```java
default int calculateNewLevel(LocalDate lastPlayed, boolean answeredCorrectly, boolean wasOverdue)
```
**Level:** `newLevel = max(1, round(2 * gap * (1 + rand)))`, `rand ∈ [-0.1, 0.1]`,
`gap` = Tage zwischen `lastPlayed` und heute.
**Fälligkeit:** `nextDue = lastPlayed + level` — **nicht** in der DB gespeichert, bei Bedarf
berechnet (SQL oder Java).

### 🗄️ DB-Schemas (Region-Lernen)

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

## 🧩 Features

### 🍺 Alkohol-Tracker (`alc`)

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

#### AlcStatisticsScreen (`alc`)
BarChart mit kumulativem Kontostand. X: Tage, Y: Kontostand. Balken `:achieved` (positiv) /
`:failed` (negativ). Tooltip: Datum | Kontostand | Grün:Gelb:Rot-Ratio. Balance startet beim
Kontostand *vor* dem gewählten Zeitraum, nicht bei 0.

### 📝 Tagebuch (`diary`)

- **`DiaryDialog`** — Eingabe von Tagebuchtext und Tags, Bild-Anhänge. Der Aufruf ist im
  `Controller` als Post-Startup-Task verankert; gedrängt wird, wenn der letzte Eintrag zu
  lange her ist.
- **`DiaryViewerScreen`** — Browsen und Durchsuchen der Einträge. Tag-basierter Query-Parser
  mit booleschem AND/OR und Klammern (in SQL übersetzt); Bild-Hover-Vorschau über `Popup`.
- Persistenz über `diary.repository.Repository`; Anhänge in eigener Tabelle, Thumbnails lazy.

### 💪 Fitbit-Integration (`fitbit`)

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

#### FitbitStatisticsScreen (`fitbit`)
BarChart + LineChart in einem `StackPane`. BarChart: Wochenpunkte pro Woche
(`:achieved`/`:failed`/`:in-progress`). LineChart (maus-transparent): Wochenziel als Linie.
Steuerung über DatePicker (Von/Bis) und Spinner (Balkenabstand).

### 🛏️ Matratzen-Erinnerung (`mattress`)

Wiederkehrender Erinnerungsdialog (`TurnDialog`) zum Wenden der Matratze: prüft im Rahmen der
Startlogik, ob fällig, zeigt den Dialog, protokolliert die Bestätigung
(`mattress.repository.MattressRepository`).

### 💬 Messaging (`messaging`)

Gemeinsames `messaging.repository.MessageRepository` (Suite-DB, quellunabhängig); je Quelle ein
Unterpaket (`signal`, `whatsapp`) mit eigenem Fremdquellen-Repository.

#### Signal-Import (`messaging.signal`)
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

#### WhatsApp-Import (`messaging.whatsapp`)
Inkrementeller Import (`WhatsAppIncrementalImport`) nach konfigurierter Uhrzeit; SHA-256-Vergleich
der `crypt15`-Datei erkennt neue Backups; Entschlüsselung in `WhatsAppCrypt15Decryptor`;
Fremdquellen-DB-Zugriff in `WhatsAppSourceRepository`. Kontakt-/Chat-Dialoge:
`WhatsAppContactDialog`, `WhatsAppChatDialog`.

#### DB-Schema (Messaging)
```
msg_contacts          — Personen (contact_id, display_name)
msg_contact_mapping   — serviceId → contact_id (source, raw_identifier, contact_id)
msg_chats             — Chats (chat_id, source, raw_identifier, is_group, display_name, blacklisted)
msg_chat_members      — Teilnehmer, lazy (chat_id, contact_id)
msg_messages          — Nachrichten (source, source_id, sent_at, from_contact, chat_id, content,
                        quote_message_id);  PRIMARY KEY (source, source_id)
msg_message_attachment— Anhänge (source, source_id, path, thumb_path, available)
```

### 🎬 Film / TMDB-Integration (`movie`)

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

### 📅 Wochentagsberechnung (`weekday`)

Eigenständiger Trainingsdialog (`WeekdayDialog`) für Datums-/Wochentagswissen: erzeugt eine
Datumsaufgabe, nimmt die Auswahl entgegen, wertet aus. Aufruf im `Controller` (an die tägliche
Nutzung gekoppelt). Persistenz über `weekday.repository.WeekdayRepository`.

## ✅ Status

### 📋 Geplant / Offene Punkte

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

<!-- ANNAHME: Überschrift über die bestehende 1–8-Liste -->
### ⚠️ Bekannte Schwächen

1 Alert in der Persistenz. Im catch von DbDeckProgressSource.saveLearned steht new Alert(...) — UI in der Persistenzschicht. In sechs Monaten täglichem Gebrauch nie geflogen, aber fachlich am falschen Ort. Bewusst wörtlich stehen gelassen, du wolltest ein // !TODO setzen.

2 getSwitchStrategy ohne active-Guard. In der Schale liest die Methode progress.hasProgressed() + isFreePlay, prüft aber selbst kein active. Sie wird nur vor dem Schließen gerufen, also aktuell harmlos — aber sie ist die einzige Screen-Methode, die auf einer toten Session nicht knallt (alle anderen delegieren in geguardete Progress-Methoden). Entscheiden, ob das so bleibt oder ob du dort auch hart sein willst.

3 FailFast-Guard in AnkiDeckService.savePlayedCards (optional). Der Lookup dueCards.get(type).get(row.cardId()) läuft bewusst in eine nackte NPE, falls die Invariante „gespielte Karte ∈ dueCards" je bricht. Offen, ob du die NPE durch ein explizites [FAILFAST] mit Klartext ersetzen willst — reine Lesbarkeit im Fehlerfall, kein Verhaltensunterschied im Normalbetrieb.

4 Deck-Display-Name als Primärschlüssel. In card_learn_stat/card_log hängt der Schlüssel an Deck.getDisplayName() (String). Benennst du den Anzeigetext eines Decks mal um, verwaisen alle historischen Zeilen. Das ist eine bestehende Eigenschaft deiner DB, nicht durch dieses Refactoring eingeführt — ich habe sie bewusst nicht angefasst, aber sie ist erkannt.

5 Gemeinsame Abstraktion AnkiSessionProgress ↔ WriteSessionProgress. Ob beide am Ende ein gemeinsames SessionProgress-Interface teilen (region hat es bereits), haben wir explizit als größere Designfrage aufgeschoben.

6 Naming-Bruch in learn.anki.repository (dein neuer Punkt). In den übrigen Domänen heißt die SQL-ausführende Klasse ...Repository. Hier heißt der SQL-Ausführer ...Source (DbDeckProgressSource, CsvDeckCardSource), und DeckRepository ist der vorgelagerte Wrapper über beide. „Repository" bedeutet hier also genau das Gegenteil wie sonst. Der Kern der Vereinheitlichung ist die Entscheidung, welche Klasse den Namen Repository verdient: der SQL-Ausführer (dann wird DbDeckProgressSource zum ...Repository und der Wrapper braucht einen neuen Namen, denn er führt kein SQL aus) — oder du behältst den Wrapper als Repository und benennst dafür anderswo um. Eigene kleine Runde wert, weil der Wrapper zwei Quellen (CSV + DB) bündelt und die saubere Benennung davon abhängt, wie du diese Doppelrolle siehst.

7 Menü „Speichern" wird bei Statistik-Screens nicht ausgeblendet (anders als Region) — prüfen ob Bug. Marker gehört in die Menü-Sichtbarkeits-Logik im Controller, nicht an closeSilent. (Das TODO, das du dir machen wolltest.)

8 ObjectMapper-Vereinheitlichung: fitbit-Importer-Mapper vs. eigener, anders konfigurierter TmdbApiClient-Mapper — nicht blind zusammenwerfen. Offene Logik-Frage.

> Fundament & Regeln: siehe `ThosSuite_Architektur_allgemein.md` und `ThosSuite_Design_Regeln.md`.

Hier die knappe Liste — was wir aus dem Architekturdokument rausgenommen haben und was im Feature-Details-Dokument auftauchen sollte:
Lern-Kern (aus „Was wird wie gelernt?" und dem Überblick):

Die vier erwarteten Aktionen: MC-Antwort klicken, Antwort tippen, richtigen Shape klicken, unsichtbaren Shape auf großer Karte treffen

Die drei Fragetypen: Bild, Text, markierter Shape (gilt für ImageMap und ShapeMap)

Anki-Deck-Details: Deutschland (ShapeMap, gemischte Kartentypen), Multiple Choice (~6000 reine MC), Welt (ImageMap, verschiebbar, Overlay-Shapes, Verfehlen = falsch + Reset), Hannover (eigenes Anki-Deck)

Region-Deck-Mechanik: ShapeMapPane-Basis, TextInputField vs. Aufforderung, nur Regionen + Hauptorte

Region-Modi entschlüsseln: Click, Elimination, Write, Easy/Hard, Resume bei Click, ShapeMapState für Skin-Wechsel (die Stichwortzeile, die „kein Mensch versteht" — muss erklärt werden)

Freies Spiel: Detail-Mechanik (im Architekturdok nur als Konzept)

Film:

MovieViewerScreen typ-agnostisch (Filme/Serien/Episoden als einheitliche Kacheln)

Tagebuch:

DiaryDialog (Start-Drang auf neuen Eintrag, wenn letzter zu lange her)

DiaryViewerScreen (tag-basierter Query-Parser mit AND/OR/Klammern, Bild-Hover-Vorschau)

Aus „Implementierter Funktionsumfang" (Section ganz gestrichen):

Fitbit: Fetch, Review, Wochenpunkt-Diagramm, Dashboard-Metriken

Alkohol: Startup-Abfrage, Kontostand, Diagramm

Diese gehören als Feature-Details geprüft, falls noch nicht dort

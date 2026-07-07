# ThosSuite — Feature-Details

**Stand:** 01.07.2026 · Struktur steht, Inhalte in Prüfung.

**Charakter:** Das *Nachschlagewerk* — pro Feature (der Lern-Kern als größtes eingeschlossen),
wie es konkret gebaut ist. Deskriptiv wie das Architektur-Dokument, aber feature-tief statt
feature-agnostisch: Das immer geltende Fundament (technische Basis, Orchestrierungs-Mechanik,
Startup) steht in `Architektur-Dokumentation.md`; hier steht, was *ein bestimmtes* Feature
ausmacht. Jeder Block ist so geschrieben, dass er sich einzeln mitgeben lässt und für sich steht.
Die *Regeln* (warum so geschnitten und benannt) stehen in `Design-Regeln.md`.

**Aufbau eines Blocks:** feste Reihenfolge, leere Teile entfallen — Zweck, Mechanik, DB-Schema,
dann der Screen als Unterabschnitt. Genau eine Unterebene unter dem Feature (`###`);
tiefer wird es Fett-Label, Fließtext oder Liste.

## 🎓 Lern-Kern (`learn`)

Das zentrale Feature der Suite. Es bündelt mehrere Lerndecks, die sich Fortschrittslogik und Session-Aufbau teilen.

### Was gelernt wird

Gelernt wird nach dem Prinzip der verteilten Wiederholung (*spaced repetition*): Wer eine Aufgabe
richtig löst, hebt ihr Level, und mit dem Level wächst der Abstand bis zur nächsten Wiederholung.
Ein Fehler wirft das Level zurück — die Aufgabe kommt schon am nächsten Tag erneut. Sicheres 
verschwindet so für Wochen aus dem Blick, Wackeliges bleibt eng getaktet.

**Deutschland.** Eine Deutschlandkarte aus rund 400 anklickbaren Formen — den Kreisen und
kreisfreien Städten. Dazu ein Vorrat von etwa 2500 Fragen, von denen jede ihren eigenen Lernstand
trägt. Eine Frage mischt bis zu drei Bausteine: einen Text, ein Bild und eine Markierung auf der
Karte, einzeln oder zusammen. Geantwortet wird je nach Aufgabe verschieden — eine aus mehreren
vorgegebenen Antworten anklicken, die richtige Form auf der Karte treffen oder den gesuchten Namen
eintippen.

**Welt und Hannover.** Aufgebaut wie Deutschland, nur liegt die Karte als ein großes Bild vor, von
dem stets nur ein Ausschnitt zu sehen ist und das sich frei verschieben lässt; die beiden
unterscheiden sich allein in diesem Bild. Weil die Karte kein Raster einzelner Formen ist, zielt
ein Klick auf einen bestimmten, unsichtbar hinterlegten Bereich, und markiert werden frei
gezeichnete Umrisse auf dem Bild statt vorgestanzter Formen.

**Multiple Choice.** Rund 6000 reine Auswahlfragen, ohne Geografie und ohne Karte — hier zählt
allein, aus den vorgegebenen Antworten die richtige zu erkennen. Wie bei Deutschland trägt jede
Frage ihren eigenen Lernstand.

**Regionen.** Mehrere Karten aus anklickbaren Formen (analog der Deutschlandkarte oben) — Schweizer
Kantone, US-Bundesstaaten, deutsche Bundesländer und etliche mehr. Gelernt werden die Regionen
selbst und ihre Hauptorte. Anders als bei den Fragen oben trägt nicht die einzelne Region einen
Lernstand, sondern das Deck als Ganzes. Drei Arten von Aufgabe gibt es:

- **Finden** — die genannte Region oder den genannten Ort auf der Karte anklicken.
- **Eliminieren** — der Reihe nach alle Regionen oder alle Orte abklappern; ein Fehler beendet den
  Durchgang.
- **Benennen** — die gerade markierte Region oder den markierten Ort eintippen.

Im Menü **Lernen** liegt bereit, was heute fällig ist: bei den Regionen die anstehenden Sessions,
bei den Frage-Decks die Decks mitsamt ihren fälligen Karten. Unabhängig davon lässt sich jedes
Deck im **Freien Spiel** öffnen — dieselben Aufgaben außer der Reihe, ohne Fälligkeit und ohne dass
sich am Lernstand etwas ändert.

### Spaced Repetition

Für jede Aufgabe steuert ein Level, wann sie wieder fällig wird (`Progress.calculateNewLevel`). Bei
richtiger Antwort steigt das Level ungefähr auf das Doppelte des Abstands, der seit dem letzten
Spielen vergangen ist — mit einem Zufalls-Wackler von ±10 %, damit nicht alle Aufgaben im
Gleichschritt fällig werden:

    newLevel = max(1, round(2·gap · (1 ± 0,1)))     // gap = Tage seit lastPlayed

Bei falscher Antwort fällt das Level zurück, und die Aufgabe wird heute (bei Karten) oder morgen
(bei Regionen) wieder gestellt.

Das Level *ist* zugleich der Abstand in Tagen: Die nächste Fälligkeit ergibt sich als `lastPlayed +
level` und wird nicht gespeichert, sondern bei Bedarf berechnet.

### Map-System

Eine Karte ist ein `GeoMap`-Container: eine Sammlung von `ShapeMap`-Objekten (je eine anklickbare
Region mit ihren Namen und ihrem JavaFX-Node) plus die zugehörigen Bilder. Alle Daten einer Region
— IDs, Regions- und Ortsnamen samt Alternativschreibweisen — stecken in einer GeoJSON-Datei und
werden beim Laden in die `ShapeMap`-Objekte übernommen.

Zwei Sorten Karte, unterschieden über `MapType`:

- **SHAPE** — die Formen *sind* die Karte (Deutschland, alle Regionen-Decks). Jede Form ist ein
  eigener anklickbarer Node.
- **IMAGE** — ein großes Hintergrundbild *ist* die Karte (Welt, Hannover); die Formen liegen als
  unsichtbare Klickflächen darüber.

### Session-Aufbau

Eine Lern-Session besteht aus vier Klassen. Am Beispiel Anki:

```
   AnkiDeckSession
        │
        ▼
   SessionProgress  ◄────►  SessionPresenter  ◄────►  SessionPane
   (Karten-Ablauf)          (Scharnier)                (GUI)
```

- Die **Schale** (`AnkiDeckSession`) sitzt oben und kennt nur den Ablauf (`SessionProgress`) — und
  den nur in eine Richtung. Der Rückweg „letzte Karte fertig" läuft als Callback
  (`onLastCardDone`), nicht als gehaltene Session-Referenz.
- Darunter die Dreierkette **GUI ↔ Presenter ↔ Ablauf.** Beide Kanten sind echt gegenseitig; der
  Presenter ist das Scharnier zwischen Oberfläche und Ablauf.

**region** folgt derselben Vierer-Struktur und denselben Begriffen, weicht in der Umsetzung aber
ab — unter anderem sitzt dort die Auswertung an anderer Stelle. Ob region dem Anki-Muster folgen
soll, ist offen (`!Diagnose`-Block im Javadoc von `RegionSession`).

## 💪 Fitbit-Integration (`fitbit`)

Holt Aktivitätsdaten aus dem Fitbit-Account — Schritte wie auch Aktivitäten (z. B. Radfahren) —
und rechnet sie nach einem Schlüssel in Tagespunkte um. Daraus entstehen Wochenpunkte und ein
Streak erfüllter Wochen. Eine Woche gilt als **erfüllt**, wenn ihre Punkte das zum Zeitpunkt
gültige Wochenziel (`fitbit_goal_history`) erreichen — darauf beruhen Streak und Diagramm-Färbung.

> **Health-Migration ausstehend.** Die Fitbit-Web-API wird abgeschaltet; der Datenabruf muss auf
> Google Health umgestellt werden. Noch komplett offen — Abruf und Persistenz unten beschreiben
> den *aktuellen*, Fitbit-basierten Stand.

**Datenabruf (`DataFetcher` / `ApiClient`):** holen die Daten in `runPreTasks()` — blockierend
auf dem FX-Thread, während der Splash sichtbar ist (externer API-Call, siehe Architektur →
Startup-Flow).

**Review (`DataReviewService`):** zeigt in `runPostTasks()` die Review-/Bestätigungsdialoge und
speichert.

**Punkte (`PointsCalculator`):** rechnet Schritte und Aktivitäten nach einem Schlüssel in
Tagespunkte um.

**Dashboard-Metriken (`DashboardService`):** `calculateRemainingDailySteps(today)`,
`calculateCurrentStreak(today)` (erfüllte Wochen ohne die laufende), `calculateRecordStreak()` —
konsumiert vom DashboardScreen (`controller`).

**DB-Schema:** Tagespunkte liegen in `fitbit`; Wochenpunkte werden **nicht** gespeichert, sondern
im View `fitbit_weekly_points` täglich zu Wochen (ab Montag) aggregiert.
```sql
CREATE TABLE fitbit (
    date   TEXT PRIMARY KEY,
    points INTEGER,
    remark TEXT
);
CREATE TABLE fitbit_goal_history (
    valid_from  DATE PRIMARY KEY,
    weekly_goal INTEGER NOT NULL
);
CREATE VIEW fitbit_weekly_points AS
    SELECT DATE(date, '-' || ((CAST(strftime('%w', date) AS INTEGER) + 6) % 7) || ' days')
               AS week_start,
           SUM(points)          AS points,
           group_concat(remark) AS remark
      FROM fitbit
     GROUP BY week_start
     ORDER BY week_start DESC;
```

### FitbitStatisticsScreen

BarChart + LineChart in einem `StackPane`. BarChart: Wochenpunkte pro Woche
(`:achieved` / `:failed` / `:in-progress` für die laufende Woche). LineChart (maus-transparent):
das Wochenziel als Linie. Steuerung über DatePicker (Von/Bis) und Spinner (Balkenabstand).

## 📝 Tagebuch (`diary`)

Führt ein Tagebuch mit Bild-Anhängen; Einträge lassen sich über eine tag-basierte Suche browsen
und durchsuchen.

**Eintragsdialog (`DiaryDialog`):** im `Controller` als Post-Startup-Task verankert. Liegt der
letzte Eintrag länger als `diary.invasiveAfterHours` (Default 18 h) zurück, drängt der Dialog auf
einen neuen Eintrag und blockiert für bis zu `diary.invasiveSeconds` (Default 120 s), solange
nichts geschrieben wird. Nimmt Text, Tags und Bild-Anhänge auf. Kann auch aus dem Menü heraus aufgerufen werden ohne Neustart der Suite.

**Bilder** werden als externe Attachments gespeichert (siehe Architektur → Externe Attachments);
fehlt eine Bilddatei beim Anzeigen, wird das toleriert — das Fehlen ist erwartet, kein Fehlerfall.

**DB-Schema:**
```sql
CREATE TABLE diary_entry (
    created_at TEXT PRIMARY KEY,   -- ISO-8601-Timestamp, Identität des Eintrags
    entry_date TEXT NOT NULL,      -- vom DatePicker gewähltes Datum (≠ created_at)
    text       TEXT NOT NULL
);
CREATE TABLE diary_tag (
    name TEXT PRIMARY KEY           -- Tag-Name ist die Identität
);
CREATE TABLE diary_entry_tag (      -- Zuordnung Eintrag ↔ Tag
    entry_created_at TEXT NOT NULL REFERENCES diary_entry(created_at),
    tag_name         TEXT NOT NULL REFERENCES diary_tag(name),
    PRIMARY KEY (entry_created_at, tag_name)
);
CREATE TABLE diary_entry_attachment (
    entry_created_at TEXT NOT NULL REFERENCES diary_entry(created_at),
    path             TEXT NOT NULL,
    PRIMARY KEY (entry_created_at, path),
    CHECK (path NOT LIKE '%,%')     -- kein Komma im Pfad (wird woanders komma-gejoint)
);
```

## 🎬 Film-Bewertungen (`movie`)

Importiert die eigenen Film-, Serien- und Episodenbewertungen aus dem TMDB-Account und zeigt sie
in einem gemeinsamen Viewer. Der Viewer ist **typ-agnostisch**: dieselbe Kachel rendert Film,
Serie und Episode. Grundlage ist das dumme DTO `CardData` (`shared.model`), das die
typ-spezifische `CardDataFactory` (`movie.repository`) für jeden der drei Typen baut.

**Film-Import (`Importer`):** täglicher PreTask beim Start, unbeaufsichtigt während des Splash
(externer API-Call, siehe Architektur → Startup-Flow). Holt zu bewerteten Filmen Zusammenfassung,
Cast, Crew und die eigene Wertung; zu jeder neuen Bewertung wird ein Kommentar erfragt und
gespeichert. Weil er nicht nachfragen kann, parkt er unbekannte Crew-Jobs in den Pending-Tabellen
und überlässt die Entscheidung dem `MovieCleanup`-PostTask.

**Erweiterter Import (`SeriesImporter`):** manuell per Menübutton, komplett interaktiv auf dem
FX-Thread mit Inline-Dialogen. Drei Durchgänge — Serien-Check (neue Serien, erkannte
Umbewertungen, geänderte Seriendaten auf Rückfrage), Episoden-Check (neue Episoden samt Kommentar-
und `rated_season`-Frage, Umbewertungen) und ein Lücken-Check, der fehlende Poster und Overviews
für alle drei Typen nachholt:

- **Kaskade:** Eine bewertete Episode kann auf eine noch nicht importierte Serie/Staffel verweisen;
  `ensureShowExists` / `ensureSeasonExists` ziehen sie bei Bedarf nach — die Serie ggf. ohne
  eigenes Rating, nur über den Episodenpfad hereingekommen.
- **`rated_season`-Frage:** Pro neuer Episode genau eine Frage — bezieht sich die Bewertung auf
  diese eine Episode oder auf die ganze Staffel (Staffel-Bezug, wenn abgebrochen oder die letzte
  Episode bewertet wurde). Das Flag lässt sich später direkt in der DB korrigieren.
- **Crew-Filter:** Unbekannte Crew-Jobs lösen sofort eine Whitelist/Blacklist-Abfrage aus; die
  Entscheidung wird persistiert und gilt für den restlichen Lauf.
- **Transaktionen:** Jeder Entitäts-Import (Serie, Staffel, Episode) läuft in einer eigenen
  Transaktion über `getNewTmdbConnection()`; jeder Fehler — auch der Regular-Flip — rollt die ganze
  Entität zurück, kein halber Import.

**Repositories.** Der Import verteilt sich auf vier Entitäts-Repositories (`MovieRepository`,
`TvShowRepository`, `SeasonRepository`, `EpisodeRepository`) plus `CrewFilterRepository`
(Whitelist/Blacklist) und `PendingRepository`; der Viewer liest getrennt über
`MovieViewerRepository`. Import- und Lesepfad teilen sich bewusst nicht dieselbe Klasse.

### DB-Schema (Film-DB)

Das Filmschema liegt in der separaten Film-DB. Es dreht sich um drei bewertbare Typen — Film,
Serie, Episode — die sich Personen, Genres und Bilder teilen. Statt aller Spalten hier die Karte
nach Gruppen; die drei Typen sind durchweg parallel gebaut (`movie_*` ↔ `tv_show_*`).

**Kern-Entitäten:** `movie`, `tv_show`, `season`, `episode`, `person`, `genre` — je eine Zeile pro
TMDB-Objekt. `season` und `episode` hängen an `tv_show`; `person` und `genre` sind typübergreifend.

**Verknüpfungen (`*_to_*`):** Länder, Sprachen und Genres je Typ als reine n:m-Brücken. Die vier
Credit-Tabellen — `movie_to_person`, `tv_show_to_person`, `season_to_person`, `episode_to_person`
— tragen Rolle/Job jeder Person am jeweiligen Objekt (PK je `(objekt, person, credit_id)`, Cast =
`job IS NULL`, Crew = benannter Job). `season_to_person` hat zusätzlich die `regular`-Spalte
(siehe Regular-Flip).

**Bewertungen (`*_rating`):** `movie_rating`, `tv_show_rating`, `episode_rating` — eine Zeile je
bewertetem Objekt mit eigener Wertung, Kommentar und Zeitstempeln. `episode_rating` trägt zudem
`rated_season` — das Flag, das die Credit-Anzeige der Episode im Viewer steuert (unten).

**Bilder (`*_image`):** Poster/Backdrops je Typ, in mehreren Breiten; der Viewer greift auf die
154er-Variante. In der DB steht nur der Dateiname (siehe Architektur → Externe Attachments).

**Crew-Filter:** `crew_whitelist` / `crew_blacklist` entscheiden, welche Crew-Jobs übernommen
werden (`movie_to_person.job` hat einen FK auf die Whitelist). Unbekannte Jobs landen bei Filmen
in `crew_pending` / `person_pending` — beide **nur film-skopiert** (`crew_pending` kennt allein
`movie_id`), weil der unbeaufsichtigte Film-PreTask nicht fragen kann und die Entscheidung dem
`MovieCleanup`-PostTask überlässt. Der interaktive Serien-/Episoden-Import fragt sofort und
braucht darum keine Pending-Tabellen — eine gewollte Asymmetrie entlang der PreTask/PostTask-Linie.

**Such-Views (`all_directors`, `all_actors`, `all_titles`):** vereinen über alle drei Typen und
speisen die Tipp-Vorschläge der Suche im Viewer. `all_titles` zieht dabei alle Titelvarianten
(Original, Deutsch, Serien-/Episodenname) zusammen.

**Lese-Views (`*_details`):** `movie_details`, `tv_show_details`, `episode_details` bündeln je
Typ das, was die Kachel braucht (Titel, Rating, Kommentar, 154er-Bild), damit der Viewer nicht
selbst zusammen-joinen muss.

**Fallstrick — der Regular-Flip.** TMDB führt an einer Staffel zweierlei Credits: die
*aggregierten* (Vereinigung aller Episoden-Credits inkl. Gaststars, aus `/aggregate_credits`) und
die schmaleren *Season Regulars* (Stammbesetzung, auf Staffelebene gepflegt, aus `/credits`).
`season_to_person` wird mit den aggregierten befüllt; direkt danach werden in derselben
Transaktion die Zeilen, die auch im `/credits`-Ergebnis stehen, auf `regular = 1` markiert. Kein
Insert, nur ein Flip — möglich, weil *regulär ⊆ aggregiert* gilt (empirisch geprüft) und der Match
über `(season_id, person_id, credit_id)` eindeutig ist. Findet der Flip keine passende Zeile, ist
die Invariante verletzt → FailFast → Rollback der ganzen Staffel. Dieser `regular`-Wert speist
dann die Episoden-Anzeige: Bei `rated_season = 0` zeigt eine Episode ihre eigenen Credits *plus*
die Stammbesetzung der Staffel; bei `rated_season = 1` die volle Staffel-Besetzung.

### MovieViewerScreen

Zeigt alle bewerteten Filme, Serien und Episoden als einheitliche Kacheln (typ-agnostisch, siehe
oben). Die Suche läuft über drei Vorschlagsfelder — Regisseur, Schauspieler, Titel —, die schon
beim Tippen filtern; ihre Vorschläge kommen aus den typübergreifenden Such-Views `all_directors` /
`all_actors` / `all_titles`. Implementiert `Screen` mit `SessionSwitchStrategy.IMMEDIATE`.

### DiaryViewerScreen

Browsen und Durchsuchen der Einträge. Tag-basierter Query-Parser mit booleschem AND/OR und
Klammern, in SQL übersetzt. Bild-Hover-Vorschau über `Popup`.

## 🛏️ Matratzen-Erinnerung (`mattress`)

Erinnert daran, die Matratze zu wenden. Als Post-Startup-Task prüft `TurnDialog`, ob das Wenden
fällig ist, zeigt in dem Fall den Dialog und protokolliert die Bestätigung.

## 📅 Wochentagsberechnung (`weekday`)

Trainiert Datums-/Wochentagswissen: `WeekdayDialog` erzeugt eine Datumsaufgabe, nimmt die Antwort
entgegen und wertet aus. Läuft als Post-Startup-Task, an die tägliche Nutzung gekoppelt.

## 💬 Nachrichten-Archiv (`messaging`)

Importiert Signal- und WhatsApp-Nachrichten inkrementell in die Suite-DB, wo sie vielleicht
einmal die Tagebuch-Timeline mitspeisen (Anzeige im `DiaryViewerScreen`). Beide Quellen laufen
als PostTask beim Start; ein eigener Screen existiert nicht.

**Geteilter Kern, getrennte Zweige.** Was beide teilen, ist fast nur das Ziel-Schema und der
Schreibweg: `MessageRepository` (`messaging.repository`) schreibt quellunabhängig in die
`msg_`-Tabellen. Jeder Zweig bringt sein eigenes Fremdquellen-Lesen mit —
`SignalSourceRepository` liest die Signal-DB, `WhatsAppSourceRepository` die WhatsApp-Quelle. Der
gemeinsame Primärschlüssel `(source, source_id)` in `msg_messages` hält Signal- und
WhatsApp-IDs kollisionsfrei in einer Tabelle.

**DB-Schema** (quellunabhängig, von beiden Zweigen beschrieben):
- `msg_contacts` — die Personen, quellübergreifende Identität.
- `msg_contact_mapping` — bildet die rohe Kennung einer Quelle (serviceId/Nummer) auf einen
  Kontakt ab; die Brücke, über die ein Mensch Signal *und* WhatsApp umspannen könnte.
- `msg_chats` — Chats je Quelle, trägt das `blacklisted`-Flag (gesetzt, wenn ein neuer Chat
  abgelehnt wird).
- `msg_chat_members` — Teilnehmer eines Chats, lazy befüllt.
- `msg_messages` — die Nachrichten; PK `(source, source_id)`; hält `quote_message_id`.
- `msg_message_attachment` — Anhänge als relativer Pfad + Thumbnail + `available`-Flag. Beim
  *Import* ist eine fehlende Quelldatei FailFast. Die Anzeige wird vermutlich toleranter.

### Signal (`messaging.signal`)

Liest die Signal-Quelle live (SQLCipher-DB). Anker ist die zuletzt importierte `source_id`: über
sie wird der zugehörige `sent_at` direkt in der Signal-DB nachgeschlagen (keine
Zeitzonenkonvertierung) und als untere Grenze genutzt; ohne bisherigen Import ist die Grenze
Epoch 0. Obere Grenze ist *jetzt − 5 Minuten* — Puffer gegen Nachrichten, die die Signal-DB noch
nicht fertig geschrieben hat; die jüngsten fünf Minuten bleiben also bewusst außen vor.
Nachrichten mit `sent_at` genau auf der unteren Grenze werden per DB-Abfrage als bereits
importiert erkannt und übersprungen.

Die vollständige Filterkette sitzt an *einer* Stelle (`forEachImportableMessage`): Typ-Whitelist
(nur incoming/outgoing), `isErased`, bereits-importiert, Blacklist, sowie „weder Body noch
Attachment". Trifft der Import auf einen unbekannten Chat, hält er an und fragt per blockierendem
Alert (importieren / blacklisten); bei Ablehnung wird `blacklisted=1` gesetzt. Attachments werden
per AES/CBC in den suite-aufgelösten Signal-Unterordner entschlüsselt, in der DB steht nur der
Dateiname. Der gesamte Lauf ist eine Transaktion — alles oder nichts.

### WhatsApp (`messaging.whatsapp`)

Läuft einmal täglich nach `whatsapp.daystartHour` und nur, wenn sich das Vollbackup
(`msgstore.db.crypt15`, aus dem Filen-Sync) seit dem letzten Import geändert hat — erkannt per
SHA-256-Hash. Anders als Signal liest WhatsApp nicht live, sondern entschlüsselt die `crypt15`
mit dem E2E-Backup-Schlüssel in ein Temp-Verzeichnis, importiert daraus und löscht es wieder.
Anker ist ein Paar aus höchster importierter `_id` und letztem Timestamp: gelesen wird alles mit
`_id > lastId OR timestamp > lastTs` (Sortierung timestamp, dann `_id`), das Aussortieren bereits
importierter Zeilen macht der Aufrufer. Nach dem Commit werden Hash und Check-Zeitpunkt
gespeichert; die Attachments zieht ein eigener PostTask nach. Bleibt der Import länger aus
(`whatsapp.warningAfterDays`), warnt die Suite.

WhatsApp hält kein `model`-Paket — die Fremdzeilen werden direkt aus dem `ResultSet` gelesen,
ohne freistehende DTOs dazwischen.

## 🍺 Alkohol-Tracker (`alc`)

Hält pro Tag fest, wie der Alkoholkonsum war, und führt daraus einen laufenden Kontostand zur
Selbstbeobachtung — grüne und gelbe Tage zahlen ein, rote ziehen ab.

**Punktesystem — zeitlich konfigurierbare Ratios:** **GREEN** (positiver Beitrag), **YELLOW**
(kleiner positiver Beitrag), **RED** (negativer Beitrag). Welche Ratio zählt, hängt am Datum
(`valid_from`), sodass sich die Gewichtung über die Zeit verschieben lässt.

**Startup-Dialog (`StartupService`):** in `runPostTasks()` aufgerufen; prüft auf fehlende Tage
seit dem letzten Eintrag bis gestern und fragt je fehlendem Tag „Wie war [Wochentag] der
[Datum]?" (Grün/Gelb/Rot/Abbrechen; bei Abbrechen kein Auto-Fill).

**Kontostand** wird on-the-fly berechnet, nicht gespeichert: alle Ratios nach `valid_from` laden,
je Tag die gültige Ratio (`valid_from <= date`) finden, kumulativ aufaddieren (RED negativ).

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

### AlcStatisticsScreen

BarChart mit kumulativem Kontostand. X: Tage, Y: Kontostand. Balken `:achieved` (positiv) /
`:failed` (negativ). Tooltip: Datum | Kontostand | Grün:Gelb:Rot-Ratio. Der Kontostand startet
beim Wert *vor* dem gewählten Zeitraum, nicht bei 0.

## 🧭 Dashboard (`controller`)

Der einzige Screen, der keinem Feature gehört: Er zieht Kennzahlen quer durch die Suite auf einen
Blick zusammen. `FlowPane` aus Kacheln (`createDashboardTile()` des Skins), in fester Reihenfolge:

- Restschritte des Tages (`fitbit.DashboardService`, mit Tausenderpunkt)
- aktueller Fitbit-Streak in Wochen samt Rekord
- aktueller Alkohol-Kontostand
- aktueller Wochentags-Streak in Tagen samt Rekord
- Tage bis zum Wenden der Matratze
- heute importierte Nachrichten
- Tage seit dem letzten Extra-TMDB-Import (`Config.getDaysSince(...)`)

Jede Kachel ruft das Repository bzw. den Service ihres Features direkt — der Controller greift von
oben in alle herab (siehe Architektur → Orchestrierung). Implementiert `Screen` mit
`SessionSwitchStrategy.IMMEDIATE`.
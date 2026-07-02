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

### DiaryViewerScreen

Browsen und Durchsuchen der Einträge. Tag-basierter Query-Parser mit booleschem AND/OR und
Klammern, in SQL übersetzt. Bild-Hover-Vorschau über `Popup`.

## 🛏️ Matratzen-Erinnerung (`mattress`)

Erinnert daran, die Matratze zu wenden. Als Post-Startup-Task prüft `TurnDialog`, ob das Wenden
fällig ist, zeigt in dem Fall den Dialog und protokolliert die Bestätigung.

## 📅 Wochentagsberechnung (`weekday`)

Trainiert Datums-/Wochentagswissen: `WeekdayDialog` erzeugt eine Datumsaufgabe, nimmt die Antwort
entgegen und wertet aus. Läuft als Post-Startup-Task, an die tägliche Nutzung gekoppelt.

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
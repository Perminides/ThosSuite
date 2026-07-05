# Health-Migrations-Log

**Charakter dieses Dokuments:** Übergabe an das September-Ich. Kein Dokument, das Thorsten
lesen wird — es ist die Notiz an den Entwickler-Instanz, die beim Cutover von Fitbit auf
Google Health die Arbeit fortsetzt. Hier stehen die getroffenen Entscheidungen *und warum*,
die offenen Punkte und der grobe Plan. Bewusst ausführlich.

**Stand:** 05.07.2026. Fitbit-Web-API wird im **September 2026** abgeschaltet (genaues Datum
laut Google TBD). Ab dann liefert nur noch die Google-Health-API Daten.

**Aktueller Zustand:** Fitbit ist **Master** (holt, zeigt Review-Dialog, speichert Punkte).
Health läuft als **stiller Schatten** mit: holt dieselben Tage, vergleicht Roh-Schritte und
Roh-Rad-km, zeigt ein Popup nach dem Fitbit-Popup, loggt eingedampft in eine eigene Datei.
Health schreibt **nichts** in die DB und hat **keinen** Dialog.

---

## 1. Die zentrale, unbequeme Erkenntnis (Schritte) — NICHT neu aufrollen

**Keine API-Methode reproduziert die App-/Uhr-Zahl.** An sauberen Ein-Quellen-Tagen liefern
`list`, `reconcile` und `dailyRollUp` **exakt dieselbe** Zahl — und die liegt **konstant ~1 %
unter** dem, was die Uhr und die Health-App anzeigen (Beispiel 17.06.: App/Uhr 17013,
API 16836, Δ 177). Empirisch bewiesen mit `StepsListSum` (eine Quelle, Charge 6, `list` = 16836
= die anderen).

Wichtige Konsequenzen, damit das September-Ich nicht dieselben Sackgassen erneut abläuft:

- **Die Lücke ist NICHT die Nicht-getragen-Kürzung.** `reconcile` hat keine solche Klausel und
  liefert trotzdem denselben Wert wie `dailyRollUp`. Die Ursache sitzt tiefer, in dem, *was die
  öffentliche API als Schrittwert überhaupt führt*. Von außen nicht weiter bestimmbar.
- **Die App liest nicht über die öffentliche API**, sondern über einen internen Erstanbieter-Kanal
  (mutmaßlich). Cache wurde ausgeschlossen (App-Daten gelöscht, neu geladen → weiterhin 17013).
- **Health Connect** ist eine *separate*, gerätelokale Quelle und liegt **über** der Uhr (zählt
  noch höher). Also auch nicht die Uhr-Zahl. Für die Windows-Suite ohnehin nicht erreichbar
  (Android-lokal). Als Weg **verworfen**.
- **Die alte Fitbit-Web-API trifft die App exakt** (17013 = 17013). Sie stirbt aber im September.

**Hoffnung, aber nicht darauf bauen:** Googles eigene Doku sagt, der „Reconciled Stream" solle
die App treffen — tut er messbar nicht. Dazu läuft ein **Known Issue** in der Google-Health-
Community („Google Health app daily steps sometimes differ from legacy Fitbit Web API tracker
steps"), aktiv in Bearbeitung. Es ist also gut möglich, dass sich die ~1 % noch von allein
schließen. **Genau das ist der Zweck des Vergleichs-Popups:** Es sensibilisiert täglich, und
wenn es von „Δ -177" auf „identisch" springt, hat Google den Bug gefixt.

**Das ungelöste Kernproblem:** Am letzten Wochentag steuert Thorsten live nach der Uhr. Läuft die
Suite auf Health, verbucht sie ~1 % weniger → Uhr sagt „geschafft", Suite sagt „knapp verfehlt".
Über die API nicht lösbar. **Entscheidung vertagt:** die ~1 % akzeptieren (Wochenziel großzügig
setzen) *oder* auf Googles Fix warten. Ein fester Sicherheitsabschlag hilft nur bedingt, weil die
Lücke schwankt (0,1–1,8 %) und an Mehrquellen-Tagen sogar die Richtung dreht (Health höher).

---

## 2. Methodenwahl

**Schritte → `dailyRollUp`.** Ein POST über den Bereich, `windowSizeDays: 1`, ein Bucket pro
Tag. Gründe: Google zieht die Tagesgrenze selbst (zeitzonen- und DST-fest, „näht" den lokalen
Tag über Zeitzonensprünge — urlaubsfest), keine eigene Grenzberechnung, keine Paginierung. An
sauberen Tagen ohnehin identisch zu `reconcile`/`list`, also kein Genauigkeitsverlust.
`reconcile` wurde geprüft und verworfen: keine bessere App-Treue, dafür Paginierung + selbst
gezogene Tagesgrenze + spürbar langsamer.

**Wichtig — Fehlt ≠ Null:** Ein Tag, der im Rollup **fehlt**, heißt „nicht getragen / nicht
synchronisiert", **nicht** 0 Schritte. Niemals zu 0 machen — das würde eine erfüllte Woche
zerreißen. `countSum: "0"` dagegen ist eine echte Null.

**Aktivitäten → `list`.** Die einzige Methode, die `exerciseType` und Distanz **pro Aktivität
einzeln** liefert (`reconcile` fasst zusammen).

**Dokumentierter Bruch (bewusst):** Schritte über `dailyRollUp` (rekonziliiert), Aktivitäten über
`list` (roh, pro Quelle, **ungemerged**). An Mehrquellen-Tagen (Health Connect schreibt mit)
zählt `list` Aktivitäten potenziell **doppelt**. Aktuell tolerierbar, weil im Schatten und weil
Thorsten das Handy-Tracking abschalten will. **Beim Master-Umbau auflösen** — entweder `reconcile`
für Aktivitäten (mit Verlust der Einzel-Provenienz) oder `list` mit eigener Deduplizierung.

---

## 3. Aktivitäts-Übersetzung — die drei stillen Killer

Alle in der Lese-DTO `Exercise` gekapselt. Das war der aufwändigste Teil der Analyse.

1. **`exerciseType`-Enum → die Fitbit-Fälle.** Google liefert einen stabilen Enum, **nicht** die
   alten Freitext-Strings. Gesicherte Zuordnungen: `WALKING`, `SPINNING`, `BIKING`,
   `OUTDOOR_BIKE`, `WORKOUT`. **„Sport" noch NICHT gesehen** — Enum-Wert offen, beim ersten
   Sport-Tag ablesen. Unbekannte Typen fallen in den `default`-Zweig → Alert „ignoriere
   Aktivität X" (dort den **`exerciseType`** zeigen, nicht den lokalisierten `displayName`, weil
   man den Wert direkt in den Code übernehmen können will). **Keine erschöpfende Enum-Tabelle
   bauen** — Google kann neue Typen nachziehen; der `default`-Zweig ist die einzige robuste
   Behandlung. Bewusst verworfen.
2. **Millimeter → Kilometer.** Google liefert `distanceMillimeters`. Naiv weiterrechnen =
   Faktor-Million-Fehler (1,63 km stünden als 1633641 da). Gekapselt in `Exercise.distanceKm()`.
3. **UTC + Offset → lokaler Tag/Zeit.** Die `exercise`-Antwort liefert `interval.startTime` in
   **UTC** plus `startUtcOffset` (z. B. `"7200s"`) — **ohne** fertige lokale Zeit (anders als
   Schritte, die `civilStartTime` mitbringen). Der Offset ist der zum *Aufzeichnungszeitpunkt am
   Ort* geltende (urlaubsfest: in Japan liefert Google +9h). Gekapselt in `Exercise.localDate()`;
   die lokale Uhrzeit rechnet das Log selbst aus den beiden Rohfeldern.

**Schritte pro Aktivität (`metricsSummary.steps`):** vorhanden bei manchen Typen (Gehen),
**fehlend** bei Spinning/Biking (Feld wird weggelassen, nicht 0). Nullbar — **fehlend ≠ 0**.
- Der alte **Spinning-Abzug ist tot**: Google liefert keine Spinning-Schritte, also gibt es
  nichts abzuziehen. Die Logik muss **entfernt** werden (nicht „so lassen" — sie läse sonst ein
  fehlendes Feld und wäre toter Code).
- Aber: gewünscht ist eine **Warnung**, falls eine punkte-erzeugende / null-erwartende Aktivität
  jemals `steps != 0` trägt. Deshalb bleibt `steps` als **Wächter-Feld** in der DTO. Der Check
  gehört in den künftigen PointsCalculator (nur dort liegt das Wissen, welche Aktivität Punkte
  erzeugt): warnen bei BIKING, OUTDOOR_BIKE, SPINNING, WORKOUT; **nicht** bei WALKING/Sport
  (die laufen durch).

---

## 4. OAuth / Auth-Modell (anders als Fitbit!)

- **Statisches Refresh-Token.** Google rotiert das Refresh-Token **nicht** (Fitbit tat es bei
  jedem Refresh — daher dort die beschreibbare JSON-Datei). Health: nur lesen, in place refreshen,
  **nichts** zurückschreiben.
- **Access-Token 1h** (Fitbit 8h) — egal, wir holen einmal täglich und refreshen ohnehin.
- **Haltbarkeit gesichert:** App-Status **„In production", unverifiziert**, restricted Scope,
  Personal-Use-Ausnahme (<100 Nutzer). Das ist Googles dokumentierter Eigengebrauchs-Pfad —
  **keine** Verifizierung/CASA nötig. Refresh-Token dauerhaft gültig bei regelmäßiger Nutzung
  (verfällt nur nach 6 Monaten Nichtnutzung; die 7-Tage-Regel gilt **nur im Testing-Modus**, wir
  sind in Production).
- **Credentials in der Config:** `healthClientId`, `healthClientSecret`, `healthRefreshToken`.
  Einmal von Hand gesetzt, aus Suite-Sicht read-only (wie `tmdb.v4.accessToken`). Keine Datei,
  keine Rotation.
- **Ersteinwilligung:** über das eigenständige Tool `scripts.GoogleHealthConsent` — pures JDK
  (Loopback-Listener + PKCE, `access_type=offline`, `prompt=consent`, `S256`), **keine** Google-
  Bibliothek, Desktop-OAuth-Client. Gibt das Refresh-Token auf der Konsole aus, wird von Hand in
  die Config gelegt. **Kein Wegwerf-Skript** — stehende Reaktivierungs-Utility, aufheben.
- **Bruno / externe Abfragen:** Desktop-Client-Token + Desktop-Credentials nutzen (nicht den
  Playground-Web-Client — der kann verfallen). Token und Credentials nie über Kreuz mischen.
- **Google-Cloud-Projekt:** `thos-suite`, Health API aktiviert, Scope
  `activity_and_fitness.readonly` (restricted).

---

## 5. Health-Connect-Rauschen (Kontext für September)

- Seit **19.06.** (Akku der Uhr war leer) trackt das **Smartphone** Schritte über Health Connect
  und schreibt in den rekonziliierten Strom. Das erzeugt Mehrquellen-Tage: `reconcile`/`dailyRollUp`
  laufen dort auseinander, `list` zählt doppelt.
- Thorsten will das Handy-Schreibrecht für Schritte entziehen. Das bereinigt **nur künftige** Tage,
  nicht die Historie (die schreibt man nicht um).
- **Fitbit-Web-API hat auf Mehrquellen-Tagen ein GRÖSSERES Problem:** sie revidiert rückwirkend
  (Tracker-Priorität überschreibt Handy-Schritte; 19.06.: Import-Zeitpunkt 7198 → heute 2728).
  „Fitbit war immer konsistent" stimmt für solche Tage also nicht. → Anmerkung Thorsten: Das stimmt
  nicht! Die 2728 war schon immer die Antwort der Fitbit-Api. Ich habe das vermutlich manuell angepasst.

---

## 6. Was gebaut wurde (Dateien & Orte)

**Bleibender Kern (`app.activity`, wird der Master):**
- `app.activity.model.Exercise` — unveränderliche Lese-DTO (Record). Kapselt `distanceKm()` und
  `localDate()`. *(Exercise wird von Thorsten ins `model`-Unterpaket verschoben; `ApiClient`
  braucht dann den passenden Import.)*
- `app.activity.ApiClient` — Health-Client: refresh (aus Config), `fetchDailySteps` (dailyRollUp),
  `fetchActivities` (exercise-list, paginiert). *(Methode `MAPPER_readTree` hat hässlichen Namen →
  umbenennen auf `parse`.)*

**Übergangsgerüst (stirbt im September):**
- `app.fitbit.FitbitDayProjection` — Record, minimale öffentliche Projektion der geholten Fitbit-
  Rohwerte (Datum, Schritte, Aktivitätsliste). **Der einzige Fitbit-Eingriff.**
- `DataFetcher.getProjection()` — baut die Projektion. Liest **rohe** Schritte (vor dem Dialog).
- `app.tmp.Comparison` — der isolierte Schatten-Vergleicher. Holt Health pro Tag, vergleicht
  Schritte + Rad-km, Popup nach dem Fitbit-Popup, loggt über `HealthImportLog`.
- `app.tmp.HealthImportLog` — eingedampftes `health_import.log`, eine JSON-Zeile pro Tag, eigene
  Datei, Format symmetrisch zum Fitbit-Log (Datum als `[J,M,T]`, lokale ISO-Startzeit). Append,
  legt Datei bei Bedarf selbst an. **Dubletten möglich** bei Mehrfachlauf desselben Tages → beim
  Auswerten gilt die letzte Zeile pro Tag.
- **Controller:** zwei `!tmp`-Felder (`comparison`, `comparisonError`); `runPreTasks` (im
  offline-`if`, try-catch → Fehler merken, nicht über dem Splash melden); `runPostTasks`
  (Option A: direkt nach dem Fitbit-Block; Fehler → Alert, sonst `showPopup()`).

**Wegwerf-Messskripte (jederzeit löschbar):**
- `scripts.StepSourceComparison`, `scripts.StepsListSum` — dienten der Datenanalyse. Erledigt.

---

## 7. Fehler-Handling-Design (bewusst so)

`Comparison` **wirft** (FailFast). Der **Controller** fängt im PreTask, merkt den Fehler, zeigt ihn
im PostTask (erst da steht das MainWindow — kein Alert über dem Splash). Der Schatten darf den
Master-Start **niemals** reißen. Das ist eine **bewusste Ausnahme** von FailFast, lokalisiert an
der Orchestrierungs-Grenze — **nicht** in den Datenklassen (anders als Fitbits `DataFetcher`, der
den Fehler selbst sammelt; genau das wollen wir hier nicht nachahmen, es ist die Aufweichung, die
Thorsten missfällt).

---

## 8. Weitere getroffene Entscheidungen

- **Rad-km-Definition** (in `Comparison`, an einer Stelle, für beide Seiten): Fitbit-Namen „Bike",
  „Fahrrad", „Outdoor Bike"; Health-Enum BIKING, OUTDOOR_BIKE. „Outdoor Bike" ist **mitgezählt**,
  weil hier *rohe gefahrene Kilometer* verglichen werden (nicht Punkte — in der Punktelogik trägt
  Outdoor Bike keine km). *(Offen: finale Bestätigung, ob Outdoor Bike rein oder raus.)*
- **Popup zeigt Rohdaten, keine Punkte** — Punkte hätten Schwellen/Rundungen, die kleine
  Schrittdifferenzen verwischen. Das Roh-Signal ist das, woran man Googles Fix ablesen will.
- **Vergleich vor der Dialog-Korrektur:** Fitbit-Schritte werden roh projiziert (vor dem Review),
  damit Roh gegen Roh steht. Nebenwirkung: An Tagen mit Fitbit-Korrektur weicht das Health-Log
  (roh) vom Fitbit-Log (nach Review) um genau die Korrektur ab. Selten, aber wissen.

---

## 9. DB-Entscheidung für September (vereinbart, aufgeschoben)

Wenn Health Master wird, sollen roh und angepasst **direkt an den Punkte-Datensatz des Tages**:
zwei neue Spalten `raw_data` und `adjusted_data` in die Punkte-Tabelle (heute `fitbit`, **beim
Cutover umbenennen**). `adjusted_data` nur gefüllt, wenn es abweicht — eindeutig, weil erst
**nach** dem Review geschrieben wird (leer = keine Korrektur, nicht „noch nicht reviewt"). **Keine**
separate Audit-Tabelle (kein Mehrzeilen-Verlauf nötig, same key, same row). Das Datei-Logging
(`HealthImportLog`) ist damit Wegwerf — **löschen, nicht umbauen**.

---

## 10. Offene Punkte (klein)

- `list`-vs-`reconcile`-Bruch dokumentieren → *hiermit erledigt* (Abschnitt 2).
- `MAPPER_readTree` → `parse` umbenennen.
- `localDate()` behalten oder streichen → **behalten**. Im jetzigen Per-Tag-Abruf-Vergleicher
  ungenutzt, aber die lokale Log-Zeit nutzt den Offset, und der Master braucht `localDate()`,
  falls er im Bereich holt. Löschen-und-neu-bauen wäre Doppelarbeit.
- Bike-km-Definition final bestätigen.
- „Sport"-Enum-Wert ablesen, wenn ein Sport-Tag auftaucht.
- `Exercise` nach `app.activity.model` verschieben (läuft) + `ApiClient`-Import nachziehen.

---

## 11. Cutover-Plan September (grob)

1. **Bug-Status prüfen:** Hat das Vergleichs-Popup gezeigt, dass Google die ~1 % geschlossen hat?
   Wenn ja → Health ist app-treu, sauber. Wenn nein → Entscheidung ~1 % akzeptieren (großzügiges
   Wochenziel); ein anderer API-Weg existiert nicht.
2. **Master-Umbau in `app.activity`:**
   - PointsCalculator auf Health-Daten: volle `exerciseType`-Behandlung (Bike km×19, Spinning 300,
     Workout 200, Walk/Sport durchlaufend; Steps-Wächter-Warnung).
   - Review-Dialog auf die neutrale Ergebnis-Struktur (unveränderliche Lese-DTO rein, editierbare
     `ActivityRow`, eigene Ergebnis-Struktur raus — **nicht** Fitbits `Activity` recyceln).
   - Persistenz: DB mit `raw_data`/`adjusted_data`; `fitbit`-Tabelle umbenennen.
   - Aktivitäts-Methode entscheiden (`list`-Deduplizierung vs. `reconcile`), sobald das Health-
     Connect-Rauschen geklärt ist.
   - `GoogleHealthConsent`-Tool bleibt.
3. **Löschen:** `app.tmp` (Comparison, HealthImportLog), die vier `!tmp`-Controller-Stellen +
   Felder, `FitbitDayProjection`, den Fitbit-Code (ApiClient, DataFetcher, DataReviewService,
   PointsCalculator, ActivityTableDialog, `model.json`), das Datei-Logging, die Messskripte.
4. **Altes Fitbit-Token revoken.**

---

## 12. NICHT neu herleiten (abgeschlossen, nicht relitigieren)

- Keine API-Methode erreicht die App-/Uhr-Zahl. Nicht erneut testen. → Anmerkung Thorsten: Das
ist falsch. Wir müssen das sogar testen. Vielleicht wurde an den anderen API-Punkten mittlerweile
etwas geändert und neuerdings gibt es doch einen Weg an die echten Zahlen zu kommen!
- Die ~1 % sind **nicht** die Nicht-getragen-Kürzung.
- Health Connect liegt **über** der Uhr — nicht die Antwort.
- Refresh-Token in Production-unverifiziert **dauerhaft** (Eigengebrauch <100 Nutzer).
- Die App = die Uhr = die Live-Zahl, nach der Thorsten am letzten Wochentag steuert (empirisch
  bestätigt: Live-Uhr = spätere App-Tagessumme).

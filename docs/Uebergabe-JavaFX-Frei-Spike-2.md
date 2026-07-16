# Übergabe: JavaFX-frei-Spike — Stand nach Screen- und Dialog-Umbau

## Kontext / wer liest das
Du bist Claude, Coding-Partner an Thorstens privater JavaFX-Desktop-App **ThosSuite**
(Java/SQLite/Maven, Eclipse, deutschsprachig, Windows, Git Bash vorhanden).
Dieses Dokument + Thorstens `docs/` (Architektur-Dokumentation, **Design-Regeln**,
Feature-Details) + die genannten Schlüsseldateien bringen dich auf den Stand. **Lies
alles, bevor du etwas vorschlägst. Rate nicht — fehlt eine Datei oder ein Fakt, fordere
ihn an.**

> Beschreibt den **implementierten, laufenden** Stand. Bei Widerspruch zu älteren Notizen
> gilt: Code + dieses Dokument schlagen alte Notizen.

**Arbeitsregeln (Thorsten legt Wert darauf):**
- Kein Code ohne ausdrückliche Aufforderung. „Könntest Du…" zählt als Aufforderung.
- Kritischer Gegenpart sein, nicht abnicken. Vorschläge nicht loben, wenn du nicht
  überzeugt bist. Lange Diskussionen sind erwünscht — sie führen zu besserem Design.
- Knapp bleiben — aber Caveats, Gegenargumente und unangenehme Details NIE weglassen,
  nur um kürzer zu sein.
- Nicht raten während technischer Diagnose; nur sagen, was bekannt ist.
- Keine Streams im Feature-Code, wenn eine Schleife es lesbarer macht (Thorstens
  ausdrücklicher Wunsch bei diesem Refactoring).
- Unix-Befehle ok, aber dazuschreiben, dass sie in Git Bash laufen (alt.: Eclipse File
  Search / PowerShell `Select-String`).
- Nicht „ihr" übers Projekt — es arbeiten zwei: du (Thorsten) und Claude. „du"/„wir".

---

## 1. Das Ziel (unverändert)
JavaFX vollständig aus den Feature-Paketen ziehen. **Hard invariant, binär:** alle
`import javafx`-Zeilen liegen ausschließlich in `shared` oder in `controller`. Jedes
Feature-Paket ist 100 % frei. Test: `grep -rn "import javafx" src/main/java/app/<feature>/`
(Git Bash) MUSS leer sein. Kein „fast frei", keine Scheinlösungen, die den grep bestehen,
aber JavaFX-*Wissen* ins Feature tragen (Scene-Graph-Navigation, opakes Durchreichen eines
JavaFX-Objekts).

---

## 2. Was seit dem letzten Spike dazukam — GEBAUT, LÄUFT

### 2a. Screen-Modell — steht
Drei Rollen, drei Interfaces (Namen final): `Screen` (Feature-Impl, framework-frei, hält
`getView():ScreenView`), `ScreenView` (shared, `getPane():Pane`, nur MainWindow ruft
getPane), `UiComponent` (shared, `getNode():Node`). Lesekette `Screen → ScreenView → Pane`;
der Node überquert die Grenze nur in MainWindow. refresh = stabiler gemounteter ScreenView
+ In-place-Rebuild. Zwei Bauformen: dedizierte self-building Komponente (fitbit) und
generischer `ComponentHost` (learn).

Migriert u. a.: `DashboardScreen`, `StartScreen`, `DiaryViewerScreen`, `MovieViewerScreen`.
`ScreenView` heißt so (nicht `View`), weil `View` in JavaFX zu häufig ist.

**Chrome-Regel (Hintergrund u. ä.):** ist Skin-/shared-Verantwortung (in `skin.properties`
festgelegt), nie vom Feature entschieden oder als JavaFX-Objekt durchgereicht (auch opak =
smell). Ein dedizierter ScreenView holt seine Chrome selbst vom Skin, im Rebuild-Pfad (nicht
im Konstruktor — überlebt sonst keinen Skinwechsel).

### 2b. Alerts / `DialogButton` — steht
`SkinService.get().showAlert(title, message, DialogButton…)` zeigt den Alert und gibt einen
**suite-eigenen** `DialogButton` zurück. Nur der Skin kennt `javafx.ButtonType` und
übersetzt hin und zurück (Identität für die Rückübersetzung). Außerhalb des Skins existiert
`javafx.ButtonType` nicht mehr. Dismiss/X = `CANCEL`, der Aufrufer interpretiert. ESC-Ziel =
`CANCEL_CLOSE`; eigene Buttons ohne Standardrolle = `OTHER` (Reihenfolge innerhalb `OTHER` =
Übergabereihenfolge). ~50 Aufrufer sind umgestellt.

### 2c. Komplexe Dialoge — Blaupause + Region/Anki fertig
**Vertrag (ausnahmslos):** framework-freier Input rein → shared-JavaFX-Dialog →
framework-freies Ergebnis raus (record/enum/`Optional`, nie `ButtonType`/`Node`). Owner
intern via `SkinService.getOwnerWindow()`, kein `Window`-Parameter.

**Bespoke pro Dialog, kein generisches Framework.** Eine generische Formular-Engine (State-
Vokabular + Renderer, ~8 Klassen) wurde gebaut, getestet und **wieder verworfen**: bei der
tatsächlichen Stückzahl (kein Dialog-Typ kommt auf fünf) war sie nur Indirektion ohne
Wiederverwendung. Lesbarkeit schlägt Wiederverwendbarkeit. Jeder komplexe Dialog bekommt
eine eigene shared-Komponente.

**Aufteilung:** JavaFX (Widgets, `Dialog`, `showAndWait`) komplett nach `shared`; im Feature
nur Domäne (welche Felder/Optionen als framework-freie Daten, Ergebnis-Record, Mapping).
Verbindung über einen **konkreten, lesbaren Zustandstyp**, nicht über abstrakte
Deskriptoren/ids.
- **Der Label ist die id** (Anzeigenamen eindeutig). Feature liest per Label zurück, mappt
  Label → Domänentyp. Bei gekürzter Anzeige: Volltext in `CheckBox.userData`.
- **Reducer nur bei Verflechtung.** Region: reine Funktion `Zustand → Zustand`, nach jedem
  Klick den kompletten Sollzustand neu (Modi filtern, Regionen ausgrauen). Anki: keine
  Verflechtung → kein Reducer; das einzige reaktive Stück („OK aktiv?") lebt in der
  Komponente.

**Fertig migriert:**
- **Region:** `RegionDialogState` + `RegionConfigDialog` (shared) + `RegionPlayConfigForm`
  (Feature). Reducer-Ordnung: angehakte Regionen → wirksamer Modus → Ausgrauen (das
  Ausgrauen hängt vom *wirksamen* Modus ab, darum zuletzt). Ein Combo-Bug ist behoben:
  `modeCombo.setItems(FXCollections.observableArrayList(items))` statt
  `getItems().setAll(...)` — sonst zeigt die Button-Zelle einen veralteten Modus, während
  der Wert korrekt ist (In-place-Mutation zeichnet die Zelle nicht neu, `setValue` ist dann
  No-op).
- **Anki:** `AnkiDialogState` + `AnkiConfigDialog` (shared) + `AnkiPlayConfigForm` (Feature).
  Kein Reducer/kein disabled. OK aktiv, sobald alle drei Zahlenfelder leer-oder-Int (in der
  Komponente). Empty→Default-Parsing im Feature (min→0, max/maxCards→MAX_VALUE). Der alte
  On-OK-Alert „Buchstaben in Zahlenfeldern?" entfällt (OK ist bei Ungültigkeit disabled).

---

## 3. Was noch aussteht
Exakte Liste bekommt Thorsten per grep; hier die Kategorien + empfohlene Reihenfolge.

**Erst die schnellen Siege (Schwung):**
1. **`TurnDialog` (mattress)** → Tier 1 (Alert). Bild + Buttons, X → später. Alert um
   optionalen Graphic **als Path/Key** erweitern (Skin lädt + tint, `tintImageWithTextColor`),
   nicht als `Image` vom Feature.
2. **Text-Prompt-Form** (`askForComment` — TextArea → String; teilt sich MovieCleanup +
   SeriesImporter). Trivial: „Alert mit einem Textfeld".
3. **`WhatsAppChatDialog` (messaging)** → Text-Prompt + Validierung + zwei Ergebnis-Buttons.

**Dann die schweren Formen:**
4. **`ActivityTableDialog` (fitbit)** → editierbare Tabelle. Die Auto-Width-Binärsuche
   (`addPostLayoutPulseListener`, Opacity-Trick, Timeout) ist Framework und wandert 1:1 nach
   `shared`; das Feature macht `Activity` ↔ Zeile.
5. **`WhatsAppContactDialog` (messaging)** → Autocomplete (Popup/Tastatur-Engine nach
   `shared`; Feature macht Name ↔ contactId). Achtung: nutzt `SkinService.get().setDialogTitle()`
   statt `stage.setTitle()` (custom HeaderBar) — Grund steht im Klassenkommentar.

**Dann:**
6. **`AlcStatisticsScreen`** → strukturell identisch zu fitbit; eigene Baustelle, weil man
   die beiden idealerweise zu generischen Komponenten vereint. Zurückgestellt, bis man das
   angeht.

**Zuletzt, der Outlier:**
7. **`DiaryDialog` (diary)** → **keine Form.** Reicher, zustandsbehafteter Editor:
   FileChooser, Thumbnails (Scalr), invasiver Modus mit Timer + Close-Blocker,
   Save-bleibt-offen, Löschen. Behandeln wie ein Screen-Split (Feature framework-frei:
   DB/Attachment/Invasiv-Regeln; shared: Editor-View), nicht über die generischen Formen.
   Der härteste Brocken.

---

## 4. Bewusst zurückgestellt — NICHT jetzt anfassen
- **Paket-Platzierung.** Die neuen Screen-Views und Dialog-Komponenten liegen teils in
  `shared.ui.dialog` / flachem `shared.ui.components` statt `shared.ui.components.<feature>`
  (was die Design-Regeln vorsehen). Thorstens Entscheidung: erst alle Dialoge migrieren,
  **danach** aufräumen. Nicht mitten in der Migration umpacken.
- **Paketzirkel / Skin ist „rot".** Der Skin (Gottklasse, ~2600 Zeilen) hat im Zuge des
  Refactorings bereits Zirkel. Bewusst geparkt bis alles javafx-frei ist. Kein Fokus jetzt.
- **Skin-Abschnitt im Regeldokument** ist als „vorläufig" markiert und wird beim
  Skin-Refactoring neu gefasst — dorthin gehören auch die Alert/Dialog-Änderungen.
- **Architektur-Dokumentation** (`docs/Architektur-Dokumentation.md`) erst am Ende
  aktualisieren — mitten in der Migration dokumentiert es einen Halbstand. Abschlussaufgabe.

Das Regeldokument (`docs/Design-Regeln.md`) hat Thorsten bereits ergänzt: Drei-Rollen-Modell,
binärer framework-frei-Test, Dialog-Abschnitt.

---

## 5. Sackgassen — nicht wiederbeleben
- **Generische Formular-Engine** (`FormDescriptor`/`RawInput`/`FormState`/`FormReducer`/…):
  zu abstrakt für die Stückzahl. Ersetzt durch bespoke Komponenten mit konkretem Zustandstyp.
- **Ein Dismiss-Ziel-Parameter am Alert** („worauf mappt X"): unnötig. X = `CANCEL`, der
  Aufrufer interpretiert.
- **id-Felder / abstraktes Deskriptor-Vokabular:** der Label ist die id (eindeutige Namen).
- **Positionale Kopplung** als Sorge beim Rücklesen: hinfällig, weil der Label die id ist.

---

## 6. Schlüsseldateien
- Interfaces: `Screen`, `ScreenView`, `UiComponent` (shared).
- Referenz-Screens: `FitbitScreenView` (dediziert), `ComponentHost` + learn (generisch).
- Alert: `SkinService.showAlert`, `DialogButton`.
- Dialog-Referenz **mit** Verflechtung: `RegionDialogState`, `RegionConfigDialog`,
  `RegionPlayConfigForm`.
- Dialog-Referenz **ohne** Verflechtung: `AnkiDialogState`, `AnkiConfigDialog`,
  `AnkiPlayConfigForm`.
- `SkinService.createDialog(...)` — der gestylte Dialog-Shell, den die Komponenten intern
  nutzen (bleibt vorerst, Skin-intern).

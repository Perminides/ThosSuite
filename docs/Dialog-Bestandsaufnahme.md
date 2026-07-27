# Dialog-Bestandsaufnahme

**Stand:** 25.07.2026 · reine Ist-Erfassung

Bestandsaufnahme der Dialog-/Alert-Stellen. Grundlage: `docs/Design-Regeln.md`, Abschnitt
*Dialoge* — drei Stufen: (1) Einfach = Alert über die beiden `showAlert()`-Methoden der
`Skin`-Klasse, (2) Komplex = bespoke Komponente pro Dialog, (3) Sonderfall = zustandsbehafteter
Editor (Screen-Split-Analogie, framework-freies Grenzobjekt + `onSave`/`onDelete`-Callbacks).

---

## Teil 1 — `showAlert`-Nutzung

Die beiden public Methoden liegen in `shared.skin.Skin`:

- **Methode 1:** `showAlert(String title, String message, ButtonEnum... buttons)` (Zeile 1803) —
  delegiert intern an Methode 2.
- **Methode 2:** `showAlert(String title, String message, AlertOptions options, ButtonEnum... buttons)`
  (Zeile 1808) — der Kern.

Aufrufzahlen (Aufrufe im Suite-Code, Discriminator = wird ein `AlertOptions`-Argument übergeben):

| Methode | Aufrufe |
|---|---|
| Methode 1 (ohne `AlertOptions`) | **37** |
| Methode 2 (mit `AlertOptions`) | **4** |

- Die 4 Aufrufe von Methode 2: `WeekdayDialog` (`.centered().mandatory()`),
  `RegionSession` (2×, `.noEsc()`), `TurnDialog` (`.image(...)`).
- Zusätzlich zu diesen 4 ruft Methode 1 Methode 2 einmal intern auf (Delegation, `Skin.java:1804`);
  diese interne Delegation ist in der Zahl 4 **nicht** enthalten.

---

## Teil 2 — Dialog-/Alert-Stellen, die NICHT über `showAlert` laufen

Alle Stellen, die einen Dialog über `SkinService.get().createDialog(...)` + `showAndWait()`
zeigen (oder JavaFX-Dialog direkt aufbauen). Kategorien: (a) Komplex/bespoke,
(b) Sonderfall zustandsbehafteter Editor, (c) passt in keine der beiden.

| Dialog-Klasse (Ort) | Auslöser im Feature | Kategorie |
|---|---|---|
| `ActivityTableDialog` (shared.ui.surfaces.dialogs) | `fitbit.ActivityTablePresenter` (via `DataReviewService`) | (a) |
| `AnkiConfigDialog` (shared.ui.surfaces.dialogs) | `learn.anki.AnkiPlayConfigForm` | (a) |
| `RegionConfigDialog` (shared.ui.surfaces.dialogs) | `learn.region.RegionPlayConfigForm` | (a) |
| `TextPromptDialog` (shared.ui.surfaces.dialogs) | `movie.SeriesImporter`, `movie.MovieCleanup` | (a) |
| `WhatsAppChatDialog` (shared.ui.surfaces.dialogs) | `messaging.whatsapp.WhatsAppIncrementalImport` | (a) |
| `WhatsAppContactDialog` (shared.ui.surfaces.dialogs) | `messaging.whatsapp.WhatsAppIncrementalImport` | (a) |
| `DiaryEditor` (shared.ui.surfaces.dialogs) | `diary.DiaryEditorPresenter` | (b) |
| `ImageBatchProcessor` (shared.ui.surfaces.dialogs) | `learn.ImageScaler` | (c) |
| `SuiteExporter.showDatePickerDialog` (controller, inline) | `controller.SuiteExporter` selbst | (c) |

Außerhalb Feature-/controller-Code (Infrastruktur, roher `new Alert` ohne `showAlert`): siehe
Abschnitt „Infrastruktur" am Ende.

---

## (a)-Fälle: Feststellung zum Aufbau

Geprüfte Bestandteile laut Regeldokument: JavaFX/Widgets vollständig in `shared`; Feature behält
Domäne + Ergebnis-Record + Mapping (Label-als-Id); ggf. Reducer bei Verflechtung.

### `ActivityTableDialog` ← `ActivityTablePresenter`
- **JavaFX in shared:** vorhanden — `ActivityTableDialog` (shared) hält alles JavaFX (`TableView`,
  `Dialog`, inneres `Row` mit `SimpleStringProperty`/`SimpleIntegerProperty`/…). `ActivityTablePresenter`
  (fitbit) nennt keinen JavaFX-Typ.
- **Feature: Domäne + Record + Mapping:** vorhanden — `ActivityTablePresenter` mappt `Activity`
  (Domäne) ↔ `ActivityTableRow` (framework-freies Grenz-Record in `shared.model`) via
  `toRows()`/`fromRows()`; Ergebnis-Record `DialogResult`.
- **Label-als-Id:** nicht anwendbar (editierbare Tabelle, keine Label-Auswahl).
- **Reducer:** abwesend.

### `AnkiConfigDialog` ← `AnkiPlayConfigForm`
- **JavaFX in shared:** vorhanden — `AnkiConfigDialog` (shared) hält `TextField`/`CheckBox`/`Dialog`.
  `AnkiPlayConfigForm` (learn.anki) nennt keinen JavaFX-Typ.
- **Feature: Domäne + Record + Mapping:** vorhanden — Grenz-Record `AnkiDialogState` (`shared.model`);
  Feature-Record `AnkiPlayConfig`; Mapping `toConfig()`.
- **Label-als-Id:** vorhanden — `CheckBox.setUserData(label)`, volles Label bleibt Id auch bei gekürzter
  Anzeige; `readState()` liest `userData` als Id zurück.
- **Reducer:** abwesend; das reaktive Stück („OK aktiv?") liegt als `updateOk()` in der shared-Komponente.

### `RegionConfigDialog` ← `RegionPlayConfigForm`
- **JavaFX in shared:** vorhanden — `RegionConfigDialog` (shared) hält `ComboBox`/`CheckBox`/`Dialog`.
  `RegionPlayConfigForm` (learn.region) nennt keinen JavaFX-Typ.
- **Feature: Domäne + Record + Mapping:** vorhanden — Grenz-Record `RegionDialogState` mit
  `Choice`/`Toggle` (`shared.model`); Feature-Record `RegionPlayConfig`; Mapping `toConfig()`.
- **Label-als-Id:** vorhanden — `deckByLabel(...)`/`modeByLabel(...)` mappen Label → Domänentyp;
  `Toggle.label()`/`Choice.label()` sind die Ids.
- **Reducer:** vorhanden — `reduce(RegionDialogState) → RegionDialogState` wird als `UnaryOperator`
  hineingereicht; die Komponente ruft `reduce.apply(readState())` bei jeder Änderung (Verflechtung:
  Modi verschwinden, Decks grauen aus).

### `TextPromptDialog` ← `SeriesImporter`, `MovieCleanup`
- **JavaFX in shared:** vorhanden — `TextPromptDialog` (shared) hält `TextArea`/`Dialog`; statisches
  `show(title, header, prefill)`.
- **Feature: Domäne + Record + Mapping:** kein eigenes Feature-Form-/Presenter-Objekt; die Feature-Klassen
  (`SeriesImporter.askForComment`, `MovieCleanup`) rufen `TextPromptDialog.show(...)` direkt auf.
- **Ergebnis:** `Optional<String>` (kein Record/Enum).
- **Label-als-Id / Reducer:** nicht anwendbar.

### `WhatsAppChatDialog` ← `WhatsAppIncrementalImport`
- **JavaFX in shared:** vorhanden — `WhatsAppChatDialog` (shared) hält alles JavaFX.
- **Feature: Domäne + Record + Mapping:** kein eigenes Feature-Form-Objekt; `resolveChat(...)` ruft
  `WhatsAppChatDialog.show(...)` direkt mit primitiven Parametern auf.
- **Ergebnis-Record:** `Result(boolean doImport, String displayName)` — im shared-Dialog definiert (nicht im Feature).
- **Label-als-Id:** nicht anwendbar; OK-Aktivierung via `Bindings` in der Komponente.
- **Reducer:** abwesend.
- **Weitere Beobachtung:** Ergebnis wird über `((Dialog<ButtonType>) dialog).getResult()` (unchecked Cast)
  ausgelesen; im Code als TODO markiert.

### `WhatsAppContactDialog` ← `WhatsAppIncrementalImport`
- **JavaFX in shared:** vorhanden — `WhatsAppContactDialog` (shared) hält alles JavaFX inkl. Autocomplete-`Popup`.
- **Feature: Domäne + Record + Mapping:** kein eigenes Feature-Form-Objekt; `resolveContact(...)` ruft
  `WhatsAppContactDialog.show(rawIdentifier, Map<String,Integer> knownContacts)` direkt auf.
- **Ergebnis-Record:** `Result(Integer existingContactId, String newDisplayName)` — im shared-Dialog definiert;
  `null` bei Abbruch.
- **Label-als-Id:** die Auflösung Anzeigename → `contact_id` geschieht im shared-Dialog über die vom Feature
  hineingereichte Map `knownContacts` (`knownContacts.get(chosen)`).
- **Reducer:** abwesend.

---

## (b)-Fall: Sonderfall zustandsbehafteter Editor

### `DiaryEditor` ← `DiaryEditorPresenter`
- **framework-freie Hälfte im Feature:** `DiaryEditorPresenter` (diary) — Domäne (Anlegen/Update,
  Attachment-Kopie/Thumbnail/Diff, Invasiv-Regel), kein JavaFX.
- **gebundene Hälfte in shared:** `DiaryEditor` (shared.ui.surfaces.dialogs) — Widgets, `Dialog`, `showAndWait`.
- **einziges framework-freies Grenzobjekt in beide Richtungen:** `DiaryCardData`.
- **Callbacks:** `onSave` (`this::save`) und `onDelete` (`this::delete`) als `Consumer<DiaryCardData>`.
- **Fachregeln als Werte hinein:** `InvasiveConfig` (Schwellen/Timer-Dauer).

### Folgen weitere Klassen diesem Muster?
Nein. Keine der übrigen Nicht-`showAlert`-Dialog-Stellen nutzt das (b)-Muster (framework-freies
Grenzobjekt + `onSave`/`onDelete`-Callbacks + Screen-Split-Analogie). Die (a)-Dialoge liefern ihr
Ergebnis synchron über `showAndWait()` zurück statt über Callbacks. `DiaryEditor` ist die einzige Klasse.

---

## (c)-Fälle: Beobachtungen

### `ImageBatchProcessor` ← `ImageScaler`
- Zeigt einen JavaFX-Auswahldialog über `SkinService.get().createDialog(...)` + `showAndWait()`
  (`chooseImage`, zwei Bildvarianten, Ergebnis `Optional<SelectionEnum>`) — nicht über `showAlert` —
  und zusätzlich einen `showAlert`-Aufruf (Zeile 48).
- Die gesamte Klasse liegt in `shared.ui.surfaces.dialogs` und enthält neben dem Dialog auch
  Nicht-UI-Verarbeitung: Datei-Auflistung (`Files.list`), Bildskalierung (AWT/`Scalr`), Schreiben/Verschieben.
- Feature-seitig reicht `learn.ImageScaler` nur Ordnerpfade und Zielgrößen an `process(...)`; kein
  Ergebnis-Record, kein Mapping.

### `SuiteExporter.showDatePickerDialog` (inline in controller)
- Baut den JavaFX-Dialog inline in `controller.SuiteExporter` über `SkinService.get().createDialog(...)`
  und `createDialogContent()` auf (`DatePicker` + `Label`), `showAndWait()`, Ergebnis `Optional<LocalDate>`.
- Keine separate bespoke Dialog-Klasse in `shared`; kein Ergebnis-Record; Auf- und Auslösen liegen in
  derselben controller-Klasse.

---

## Infrastruktur (außerhalb Feature-/controller-Dialog-Rahmen)

Roher `new Alert(...)` / `new Stage(...)` direkt, nicht über `showAlert` — in Nicht-Feature-Paketen:

- `ThosSuiteApp` (App-Wurzel): Splash-`Stage` sowie `new Alert(AlertType.WARNING/ERROR)` +
  `showAndWait()` (globaler Fehler-Handler, Startmeldungen).
- `shared.DB`: `new Alert(AlertType.WARNING)` + `showAndWait()` (2×).

Diese liegen nicht im Feature-Code und laufen nicht über `showAlert`.

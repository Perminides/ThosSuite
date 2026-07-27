# shared-UI-Nutzung — Vollständigkeitscheck

**Stand:** 25.07.2026 · reine Ist-Erfassung

Prüft, ob die beiden bisher erhobenen Nutzungsfamilien — **Screen** (`ScreenView`/`UiComponent`-Ketten,
Bauform a/b/c) und **Dialog** (Alert/Dialog-Interaktion, Einfach/Komplex/Sonderfall/c) — die gesamte
Nutzung der shared-UI-Klassen abdecken.

## Scope

„shared-UI" wird hier als die framework-gebundene UI unter `shared` gefasst:

- **`shared.ui.*`** — `contracts`, `surfaces`, `surfaces.dialogs`, `components`, `components.learn`.
- **`shared.skin`** — `Skin`/`SkinService` (die Klasse, die die `shared.ui`-Komponenten erzeugt und
  serviert). Die Screen-/Dialog-Auswertungen haben diese Schicht als undurchsichtigen Produzenten
  behandelt; hier wird sie mitgezählt, um „alle Stellen" abzudecken.

Aufrufstellen wurden projektweit erhoben (Feature-Pakete, `controller`, `shared` selbst), nicht nur
aus Feature-Paketen.

---

## Teil 1 — `shared.ui.*`-Klassen: Aufrufstellen und Familie

| Klasse (`shared.ui…`) | Aufruf-/Nutzungsstellen (projektweit) | Familie |
|---|---|---|
| `contracts.Screen` | implementiert von 8 Screens; `controller.Controller` (hält `currentScreen`, ruft refresh/esc/save/…), `controller.MainWindow` | Screen |
| `contracts.ScreenView` | Rückgabe aus `Screen.getView()`; `controller.MainWindow.showScreenView(...)` (`view.getPane()`); die Panes/Presenter in learn | Screen |
| `contracts.UiComponent` | implementiert von `SuiteImage`, `SuiteInfoLabel`, `SuiteTextField`, `SuiteIconButton`, `MultipleChoicePane`, `ShapeMapPane`, `ImageMapPane`; `getView()` gerufen von `surfaces.ComponentHost.setComponents(...)` | Screen (b) |
| `surfaces.BarChartScreenView` | `alc.AlcStatisticsScreen`, `fitbit.FitbitStatisticsScreen` | Screen |
| `surfaces.ComponentHost` | learn-Panes (`region.SessionPane`, anki `MCSessionPane`/`GermanySessionPane`/`ImageMapSessionPane`) | Screen (b) |
| `surfaces.DashboardScreenView` | `controller.DashboardScreen` | Screen |
| `surfaces.DiaryScreenView` | `diary.DiaryScreen` | Screen |
| `surfaces.MovieViewerScreenView` | `movie.MovieViewerScreen` | Screen |
| `surfaces.dialogs.ActivityTableDialog` | `fitbit.ActivityTablePresenter` | Dialog |
| `surfaces.dialogs.AnkiConfigDialog` | `learn.anki.AnkiPlayConfigForm` | Dialog |
| `surfaces.dialogs.RegionConfigDialog` | `learn.region.RegionPlayConfigForm` | Dialog |
| `surfaces.dialogs.TextPromptDialog` | `movie.SeriesImporter`, `movie.MovieCleanup` | Dialog |
| `surfaces.dialogs.WhatsAppChatDialog` | `messaging.whatsapp.WhatsAppIncrementalImport` | Dialog |
| `surfaces.dialogs.WhatsAppContactDialog` | `messaging.whatsapp.WhatsAppIncrementalImport` | Dialog |
| `surfaces.dialogs.DiaryEditor` | `diary.DiaryEditorPresenter` | Dialog (Sonderfall b) |
| `surfaces.dialogs.ImageBatchProcessor` | `learn.ImageScaler` | Dialog (dort als c geführt) |
| `components.DashboardTile` | erzeugt in `shared.skin.Skin.createDashboardTile` (aus `DashboardScreen`), angeordnet in `DashboardScreenView` | Screen |
| `components.SuiteSuggestionTextField` | erzeugt in `Skin.createMovieViewer`, genutzt in `MovieViewerScreenView` | Screen |
| `components.MultipleChoicePane` | erzeugt in `Skin.createMultipleChoicePane`, genutzt in anki-Panes | Screen (b) |
| `components.SuiteImage` | erzeugt in `Skin.createImageComponent`, genutzt in anki-Panes | Screen (b) |
| `components.SuiteInfoLabel` | erzeugt in `Skin.createSessionInfoLabel`, genutzt in anki-Panes + `region.SessionPane` | Screen (b) |
| `components.SuiteTextField` | `new` in learn-Panes (umhüllt `skin.createInputField`) | Screen (b) |
| `components.SuiteIconButton` | `new` in anki-Panes (umhüllt `skin.createIconButton`) | Screen (b) |
| `components.DiaryTagInputComponent` | `new` in `surfaces.dialogs.DiaryEditor` | Dialog |
| `components.SuiteTabCommitTextFieldTableCell` | `SuiteTabCommitTextFieldTableCell.forTableColumn()` in `ActivityTableDialog` | Dialog |
| `components.learn.ShapeMapPane` | `new` in `region.SessionPane`, anki `GermanySessionPane` | Screen (b) |
| `components.learn.ImageMapPane` | `new` in anki `ImageMapSessionPane` | Screen (b) |
| `components.learn.MapNodeBuilder` | statische `buildShapeMapNode`/`buildImageMapNode`, gerufen von `ShapeMapPane`, `ImageMapPane` (beide shared) | Screen (b), shared-intern |
| `components.learn.ShapeLayer` | package-private Enum; `fromJsonId(...)` gerufen von `MapNodeBuilder`, `ShapeMapPane` | Screen (b), shared-intern (nicht public) |

**Ergebnis Teil 1:** Jede `shared.ui.*`-Klasse resolved in die Screen- **oder** Dialog-Familie. Keine
`shared.ui.*`-Klasse ist ohne Aufrufer, und keine wird außerhalb einer ScreenView-/UiComponent-Kette
oder einer Alert-/Dialog-Interaktion genutzt.

### Beobachtung zur Aufrufseite (shared selbst)
Ein Teil der `components` wird **innerhalb von `shared.skin.Skin`** instanziiert (`new SuiteImage`,
`new SuiteInfoLabel`, `new MultipleChoicePane`, `new DashboardTile`, `new SuiteSuggestionTextField`),
nicht im Feature. Diese Konstruktionsstellen liegen in `shared`; die erzeugten Komponenten fließen in
die oben genannten Screen-/Dialog-Ketten. `MapNodeBuilder`/`ShapeLayer` werden ausschließlich shared-intern
von den beiden Karten-Panes gerufen.

---

## Teil 2 — `shared.skin` (Skin/SkinService): Nutzung außerhalb Screen/Dialog

`SkinService` wird in 39 Dateien referenziert. Die meisten Aufrufe sind bereits erfasst:
Komponenten-Fabriken für ScreenViews/Panes (**Screen**), sowie `showAlert(...)`, `createDialog(...)`,
`createDialogContent(...)`, `getOwnerWindow()`, `setDialogTitle(...)` (**Dialog**). Zusätzlich enthält
`Controller` `SkinService.setOwnerWindow(...)` als Infrastruktur, die die Dialog-Familie stützt.

Die folgenden Nutzungen fallen in **keine** der beiden Familien und werden als eigene Kategorien geführt:

### Kategorie C — Fenster-Chrome, Menüs, Skin-Wechsel

- **`controller.MainWindow`** baut über `Skin`/`SkinService` den bleibenden Fensterrahmen:
  `getContentSize()` (Größe der contentPane), `styleScene(...)`, `createMainWindowHeaderBar(...)`,
  `createMenuBar()`, `createMenu(...)`, `createMenuItem(...)` (Datei-/Optionen-/Lernen-/Spielen-/
  Statistik-/Module-/Ansicht-Menü), `getAllSkins()` + `get()` (Skin-Liste im Ansicht-Menü).
  Dies ist weder ein `ScreenView` (die einzige `shared.ui`-Berührung hier ist `showScreenView(ScreenView)`,
  die Mount-Stelle der Screen-Familie) noch eine Dialog-Interaktion.
- **`controller.Controller`**: `SkinService.set(newSkin)` + `SkinService.refresh()` (Skin-Wechsel /
  „Aktualisieren") mit anschließendem `mainWindow.buildStyledUi()`.
- **`app.ThosSuiteApp`**: `new MainWindow(...)` + `mainWindow.buildStyledUi()` beim Start (Bootstrap
  des Chrome). Zusätzlich eine Splash-`Stage`.

Beobachteter Kontext: persistenter Fensterrahmen (HeaderBar, MenuBar, Content-Sizing, Scene-Styling)
und Skin-Auswahl/-Wechsel. Kein `getPane()`-Mount, kein `showAndWait`/`showAlert`.

### Kategorie D — Skin als framework-freie Ressourcen-/Pfad-Quelle

- **`learn.MapService`** ruft `SkinService.get()` und daraus `getMapImagePath(...)`,
  `getMapOverlayImagePath(...)`, `getMapInactiveImagePath(...)`, `getMapInactiveOverlayImagePath(...)` —
  liefert **String-Pfade** in ein framework-freies `MapImagePaths`-Record. Der Skin wird hier als
  Datenquelle (Bildpfade) genutzt, nicht als Node-/Komponenten-Fabrik. Keine `shared.ui`-Klasse, kein
  `ScreenView`, kein Dialog.

Beobachteter Kontext: framework-freier Feature-Code holt skin-abhängige Datei-Pfade als Strings.

---

## Teil 3 — Randnotizen (bereits andernorts erfasst / außerhalb `shared.ui`)

- **`shared.skin.SkinImageCache`** ruft `SkinService` intern (Skin-Infrastruktur, Bild-Cache).
- **`shared.UiUtils`** (Paket `shared`, nicht `shared.ui`) — `inactivateEscPress(...)` wird aus dem
  Alert-Pfad von `Skin` gerufen; JavaFX-Helfer im Dialog-Umfeld.
- **Rohe `new Alert(...)` / Splash-`Stage`** in `app.ThosSuiteApp` und `shared.DB` — nicht über
  `shared.ui`/`showAlert`; bereits in der Dialog-Bestandsaufnahme als Infrastruktur vermerkt.

---

## Zusammenfassung

- **Alle `shared.ui.*`-Klassen** (contracts, surfaces, dialogs, components, components.learn) sind durch
  die **Screen-** oder **Dialog**-Auswertung abgedeckt.
- Bezieht man die produzierende Schicht **`shared.skin`** ein, treten zwei Nutzungen zutage, die in
  keine der beiden Familien fallen:
  - **Kategorie C:** Fenster-Chrome / Menüs / Skin-Wechsel (`MainWindow`, `Controller`, `ThosSuiteApp`).
  - **Kategorie D:** Skin als framework-freie Ressourcen-/Pfad-Quelle (`learn.MapService`).

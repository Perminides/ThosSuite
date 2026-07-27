# Skin-Klasse — interne Bestandsaufnahme

**Stand:** 25.07.2026 · reine Ist-Erfassung

Macht sichtbar, was die abstrakte Klasse `app.shared.skin.Skin` (~2660 Zeilen) heute an
unterschiedlichen Dingen tut. Kein Zielbild, keine Unterpakete, keine Bewertung des Schnitts.

**Erfassungsumfang:** alle Methoden von `Skin` (public / protected / private). Die konkreten
Skin-Subklassen (`BaseColorSkin`, `DarkMode`, `FlatWebSkin`, …) und die innere Hilfsklasse
`CssBuilder` werden separat genannt. Gezählt wurden **73 Methoden** der Basisklasse.

---

## Teil 1 — Methodengruppen nach Verantwortlichkeit

### G1 — CSS/Styling erzeugen (24 Methoden)
`styleScene(Scene)` [public] + 23 private `addXxxStyles(CssBuilder)`:
`addButtonStyles`, `addCheckBoxStyles`, `addScrollbarStyles`, `addComboBoxStyles`, `addDialogStyles`,
`addTextFieldStyles`, `addTextAreaStyles`, `addMenuStyles`, `addSessionInfoLabelStyles`,
`addIconButtonStyles`, `addImageMapStyles`, `addImagePaneStyles`, `addMainWindowStyles`,
`addShapeMapStyles`, `addMultipleChoiceStyles`, `addMyTableStyles`, `addDashboardStyles`,
`addChartStyles`, `addDatePickerStyles`, `addSpinnerStyles`, `addSuggestionBoxStyles`,
`addDiaryViewerStyles`, `addMovieViewerStyles`.
- **State (überwiegend/ausschließlich hier):** die Farb-Felder (`textColor`, `activeComponentBgColor`,
  `menuBarBackground`, `shapeMapColor0/1`, `mcCorrectTextColor`, … ~40 Color-Felder), `borderColor`,
  `thinBorderColor`, `menuButtonPadding`/`menuItemPadding`, `imageMap*Width`, `shapeMap*BorderWidth`.
- `styleScene` füllt zusätzlich zahlreiche Felder lazy mit Defaults (`menuBarHoverBackground`,
  `dashBoardTileTopFontSize`, `hannoverSession*Panel` ← `worldSession*Panel`, …) und ruft die 23
  `addXxx`-Methoden auf. Baut über die innere Klasse `CssBuilder` einen CSS-String und hängt ihn als
  `data:`-Stylesheet in die Scene.
- **Innere Hilfsklasse `CssBuilder`** (9 Methoden: `start`, `add`×2, `end`, `rule`×2, `checkSelector`,
  `build`, `toString`) — nur von G1 genutzt.

### G2 — Fenster-Chrome / Menüs bauen (5 Methoden)
`createMenuBar()`, `createMenu(String)`, `createMenuItem(String)` [public],
`createMainWindowHeaderBar(Stage, MenuBar)` [public], `createResponsiveHeaderIcon(Stage, HeaderBar)` [private].
- **State:** `font` (Header-Spacing/Padding). Sonst wenig eigener Feld-State; liefert JavaFX-Rohbausteine
  (MenuBar/Menu/MenuItem) bzw. baut die HeaderBar samt responsivem Icon.

### G3 — Hintergrund-/Wallpaper-Bilder liefern (4 Methoden)
`getBackgroundImage(mapName, category)`, `getStartBackgroundImage()`, `getEmptyBackgroundImage()` [public],
`getBackgroundImageName(mapName, category)` [private].
- **State (ausschließlich):** die `…WallpaperName`-Felder (`defaultWallpaperName`, `emptyWallpaperName`,
  `mcWallpaperName`, `worldWallpaperName`, … 16 Stück), aufgelöst über `getFieldValue(...)`.

### G4 — Session-/Lern-Komponenten bauen (10 Methoden)
`createInputField`, `createImageComponent`, `createSessionInfoLabel`, `createMultipleChoicePane`,
`createIconButton`, `buildShapeMapWrapper`, `applyImageMapLayout`, `getOverlayContentBounds` [public];
`computeMcButtonHeight`, `mcLineSpacingSqueezed` [private].
- **State (ausschließlich):** die große Block-Serie `…Session…Panel` (Rectangle2D, ~90 Felder:
  `mcSessionQuestionPanel`, `worldSessionMapPanel`, `germanySessionBackButton`, …), plus
  `…SessionOverlayContentBounds`/`defaultOverlayContentBounds`, `backButtonIcon`/`skipButtonIcon`/
  `playButtonIcon`/`cancelButtonIcon`, `verticalGapMC`. Alle Bounds über `getFieldValue(name+"Session…Panel")`.
- **State (geteilt mit G1):** `borderSmallComponent` (MC-Höhe/Metrics), `borderBigComponent`
  (Image/ImageMap-Clip), `font`, `textActiveComponentColor` (Icon-Tint).

### G5 — Dialoge/Alerts anzeigen (9 Methoden)
`showAlert(title, msg, ButtonEnum…)`, `showAlert(title, msg, AlertOptions, ButtonEnum…)`,
`createDialog(Window, title)`, `setDialogTitle(Dialog, title)`, `createDialogContent()` [public];
`installCloseBlocker`, `buildAlertContent`, `toButtonType`, `createDialogHeaderBar` [private].
- **State:** kein eigener Feld-State außer `font` (im `createDialogHeaderBar`-Padding). `toButtonType`
  kapselt das `ButtonEnum → javafx.ButtonType`-Mapping. `createDialogHeaderBar` wird von **beiden**
  Einstiegen (`showAlert` und `createDialog`) genutzt. `buildAlertContent` ruft `tintImageWithTextColor` (G8).

### G6 — Feature-Oberflächen-Komponenten bauen (8 Methoden)
`createDashboardTile`, `createDatePicker`, `createDiaryViewer`, `createDiaryCard`, `createMovieViewer`,
`createCard` [public]; `setupCommentTooltip`, `createLinkedPersonLine` [private].
- **State (überwiegend hier):** `dashBoardTileWidth/TopHeight/BottomHeight` (Tile), `diaryViewerContentWidth`,
  `diaryTooltipMargin` (Diary-Karte/Tooltip + Movie-Kommentar-Popup), `moviePosterWidth` (Movie-Karte).
- **State (geteilt):** `font`, `getContentSize()` (Movie-Layout-Breiten). `createDatePicker` ruft
  `styleScene` (G1) für die Popup-Scene.

### G7 — Karten-Bildpfade liefern (4 Methoden)
`getMapImagePath`, `getMapInactiveImagePath`, `getMapInactiveOverlayImagePath`, `getMapOverlayImagePath` [public].
- **State (ausschließlich):** `…MapImageName`/`…MapInactiveImageName`/`…MapOverlayImageName`/
  `…MapInactiveOverlayImageName` (world/hannover-Felder), aufgelöst über `getFieldValue`. Liefert reine
  `Path`-Werte (keine Nodes).

### G8 — Bild-Tinting (1 Methode)
`tintImageWithTextColor(Image)` [public] — nutzt `textColor`; von `buildAlertContent` (G5) gerufen, sonst public.

### G9 — Konfiguration laden & parsen (6 Methoden)
`loadAllConfigs(Path)` [protected], `parseColor`, `parseFont`, `parseBorderParams`, `parseRectangle` [protected],
`getFieldValue(String)` [protected].
- `loadAllConfigs` schreibt per Reflection **alle** deklarierten Felder der ganzen Klassenhierarchie aus
  einer `.properties`-Datei (Typ-dispatch auf `parseColor/parseFont/parseBorderParams/parseRectangle`).
  Gerufen aus den Konstruktoren der Subklassen (`BaseColorSkin`, `DarkMode`, `FlatWebSkin`, …).
- `getFieldValue(String)` liest ein Feld per Namen (Reflection) — der Lese-Gegenpart, genutzt von G3, G4, G7.

### G10 — Fenster-Content-Größe (1 Methode)
`getContentSize()` [public] — liefert `Dimension2D(1910, 1000)`. Genutzt von `MainWindow` (Chrome) und
intern von G6 (`createMovieViewer`, `createCard`).

### G11 — Identität (1 Methode)
`getDisplayName()` [public abstract] — pro Subklasse implementiert; genutzt im Ansicht-Menü.

---

## Teil 2 — Geteilter/zentraler State (Kandidaten, die eine Trennung verschränken)

| Element | Deklariert / gehört zu | Von diesen Gruppen genutzt |
|---|---|---|
| `getFieldValue(String)` (Reflection-Leser) | G9 | **G3, G4, G7** — Namens-basierter Zugriff auf die `…WallpaperName`-, `…Session…Panel`- und `…MapImageName`-Felder |
| `loadAllConfigs(...)` (Reflection-Schreiber) | G9 | schreibt **alle** Felder; jede lesende Gruppe (G1, G3, G4, G6, G7) hängt an ihm als einzigem Beschreiber |
| `styleScene(Scene)` | G1 | **G2** (MainWindow-Chrome via `buildStyledUi`), **G5** (`showAlert`, `createDialog`), **G6** (`createDatePicker`-Popup) |
| `createDialogHeaderBar(String)` | G5 | von `showAlert` **und** `createDialog` (beide Einstiege in G5) |
| `font` | Feld | **G1, G2, G4, G5, G6** — breitester gemeinsamer Nenner |
| `getContentSize()` | G10 | **G2** (MainWindow) + **G6** (Movie-Layout) |
| `borderSmallComponent` / `borderBigComponent` | Feld | **G1** (CSS) + **G4** (MC-Metrics, Image-Clip) |
| `textColor` / `textActiveComponentColor` | Feld | **G1** (CSS) + **G8** (Tint) / **G4** (Icon-Tint) |
| `dashBoardTile*`, `diaryViewerContentWidth`, `diaryTooltipMargin`, `moviePosterWidth` | Feld | **G6** (Bau) + **G1** (`addDashboardStyles`/`addDiaryViewerStyles`/`addMovieViewerStyles`) |
| `…SessionXxxPanel` (Rectangle2D, ~90) | Feld | ausschließlich **G4** (hohe Kohäsion innerhalb G4) |
| `…WallpaperName` (16) | Feld | ausschließlich **G3** |
| `…MapImageName`-Serie | Feld | ausschließlich **G7** |
| innere Klasse `CssBuilder` | G1 | ausschließlich **G1** |

Beobachtung: Die reflektierenden Zugriffe (`loadAllConfigs` schreibt alles, `getFieldValue` liest über
Namens-Konvention `<prefix>SessionXxxPanel` / `<x>WallpaperName` / `<x>MapImageName`) verbinden G9 mit G3,
G4 und G7 über String-Namen statt über direkte Feldreferenzen. `styleScene` (G1) wird von G2, G5 und G6
als gemeinsamer Dienst gerufen. `font` und `getContentSize()` werden von je fünf bzw. zwei Gruppen gelesen.

---

## Teil 3 — Kreuzreferenz mit den drei bisherigen Auswertungen

| Skin-Gruppe | Erfasst in bisheriger Auswertung |
|---|---|
| G5 Dialoge/Alerts | **Dialoge**: `showAlert`×2 (Teil-1-Zählung: 37 + 4 Aufrufe); `createDialog`/`createDialogContent`/`setDialogTitle` als Bau der Komplex-Dialoge; `toButtonType`/`buildAlertContent`/`createDialogHeaderBar`/`installCloseBlocker` als deren Interna |
| G6 Feature-Oberflächen (Dashboard, DatePicker, Diary, Movie) | **Screens**: `createDashboardTile`→DashboardScreenView; `createDatePicker`→BarChartScreenView; `createDiaryViewer`/`createDiaryCard`→DiaryScreenView; `createMovieViewer`/`createCard`→MovieViewerScreenView |
| G4 Session-/Lern-Komponenten | **Screens**, Bauform b: `createInputField`/`createImageComponent`/`createSessionInfoLabel`/`createMultipleChoicePane`/`createIconButton`/`buildShapeMapWrapper`/`applyImageMapLayout`/`getOverlayContentBounds` → learn-`ComponentHost`-Panes |
| G3 Hintergrundbilder | **Screens**: `getEmptyBackgroundImage`→ScreenViews; `getBackgroundImage`→learn-Panes; `getStartBackgroundImage`→StartScreen (Screen-c) |
| G2 Chrome/Menüs + G10 `getContentSize` | **Vollständigkeitscheck**, Kategorie C (Fenster-Chrome/Menüs, `MainWindow`) |
| G1 CSS/Styling (`styleScene`) | **Vollständigkeitscheck**, Kategorie C (dort als `styleScene` genannt); intern von G2/G5/G6 gerufen. Die 23 `addXxx` sind privat und tauchen extern nicht auf |
| G7 Karten-Bildpfade | **Vollständigkeitscheck**, Kategorie D (`learn.MapService`, framework-freie Pfadquelle) |
| G11 `getDisplayName` | **Vollständigkeitscheck**, Kategorie C (Skin-Liste im Ansicht-Menü) |
| G8 `tintImageWithTextColor` | teils **Dialoge** (Alert-Bild via `buildAlertContent`); ansonsten public |
| **G9 Konfiguration laden & parsen** | **In keiner** der drei Auswertungen — dort wurde `Skin` als undurchsichtiger Produzent behandelt. `loadAllConfigs`/`parse*`/`getFieldValue` sind der Skin-interne Lade-/Zugriffsmechanismus (Aufruf aus den Subklassen-Konstruktoren beim Skin-Aufbau/-Wechsel) |

**Ergebnis der Kreuzreferenz:** Die Gruppen G1–G8, G10, G11 sind in den drei bisherigen Auswertungen als
von Feature/Dialog/UiComponent/Chrome genutzt bereits sichtbar. Nicht erfasst war bisher allein **G9**
(Konfiguration laden & parsen) — der reflektierende Lade- und Feldzugriffs-Mechanismus, an dem über
`getFieldValue`/`loadAllConfigs` die Gruppen G3, G4 und G7 hängen.

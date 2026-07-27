# ScreenView-Bestandsaufnahme

**Stand:** 25.07.2026 · reine Ist-Erfassung

Bestandsaufnahme aller `Screen`-Implementierungen in den Feature- und `controller`-Paketen,
ihrer zugehörigen `ScreenView`-Implementierung und ihrer Zuordnung zu den zwei Bauformen aus
`docs/Design-Regeln.md`:

- **(a)** dedizierte, sich selbst bauende shared-Komponente, die `ScreenView` implementiert;
  eine Layout-Pane (VBox/HBox/FlowPane/StackPane) positioniert die Kinder.
- **(b)** generischer `ComponentHost` (`ScreenView`), der vom Feature gestylte, absolut
  positionierte Komponenten in eine Null-Layout-Pane entgegennimmt (Skin positioniert).
- **(c)** passt in keine der beiden.

Discriminator: *Wer positioniert die Kinder?* Layout-Pane selbst → (a). Skin absolut über
`ComponentHost` → (b).

## Tabelle

| Screen-Klasse | ScreenView-Klasse | Bauform | Beobachtung bei (c) |
|---|---|---|---|
| `controller.DashboardScreen` | `shared.ui.surfaces.DashboardScreenView` | (a) | — |
| `diary.DiaryScreen` | `shared.ui.surfaces.DiaryScreenView` | (a) | — |
| `movie.MovieViewerScreen` | `shared.ui.surfaces.MovieViewerScreenView` | (a) | — |
| `alc.AlcStatisticsScreen` | `shared.ui.surfaces.BarChartScreenView` | (a) | — |
| `fitbit.FitbitStatisticsScreen` | `shared.ui.surfaces.BarChartScreenView` | (a) | — |
| `learn.anki.AnkiDeckSession` | `shared.ui.surfaces.ComponentHost` (über `SessionPresenter` → `SessionPane.getView()`) | (b) | — |
| `learn.region.RegionSession` | `shared.ui.surfaces.ComponentHost` (über `SessionPresenter` → `SessionPane.getView()`) | (b) | — |
| `controller.StartScreen` | `controller.StartScreen` (dieselbe Klasse) | (c) | Siehe unten |

## Beleg je Zeile

### (a)-Fälle

- **`DashboardScreen` → `DashboardScreenView`.** `DashboardScreenView` hält eine `FlowPane`
  (`setHgap/setVgap/setAlignment`) und setzt die `DashboardTile`-Kinder über
  `pane.getChildren().setAll(views)`. Die FlowPane arrangiert die Kacheln per Layout.
- **`DiaryScreen` → `DiaryScreenView`.** `DiaryScreenView` hält eine `VBox` (`Pos.TOP_CENTER`,
  `VBox.setVgrow`) und hängt `components.root()` als Kind ein; die VBox positioniert per Layout.
- **`MovieViewerScreen` → `MovieViewerScreenView`.** `MovieViewerScreenView` hält eine `VBox`
  und hängt `components.root()` als Kind ein; die VBox positioniert per Layout.
- **`AlcStatisticsScreen` → `BarChartScreenView`.** `BarChartScreenView` baut sich selbst aus
  `StackPane` → `VBox` (`chart-container`) → `HBox` (Controls) + `BarChart`/`StackPane`; die
  Layout-Panes positionieren. (Ein `BarChartScreenView` je Screen, konstruiert mit eigenem
  `BarChartDataProvider`.)
- **`FitbitStatisticsScreen` → `BarChartScreenView`.** Dieselbe `BarChartScreenView`-Klasse wie
  bei `AlcStatisticsScreen`, mit `FitbitStatisticsPresenter` als Provider.

### (b)-Fälle

- **`AnkiDeckSession` → `ComponentHost`.** `AnkiDeckSession.getView()` reicht
  `presenter.getView()` weiter; `SessionPresenter.getView()` reicht `sessionPane.getView()`
  weiter. Die konkreten Panes (`GermanySessionPane`, `MCSessionPane`, `ImageMapSessionPane`)
  halten je einen `ComponentHost` und übergeben skin-erzeugte, absolut positionierte Komponenten
  per `canvas.setComponents(...)`.
- **`RegionSession` → `ComponentHost`.** `RegionSession.getView()` reicht `presenter.getView()`
  weiter; `region.SessionPane` hält einen `ComponentHost` (`host`) und übergibt skin-erzeugte
  Komponenten per `host.setComponents(karte, questionArea | inputField)`.

### (c)-Fall

- **`StartScreen` → `StartScreen` (dieselbe Klasse).**
  - `StartScreen` (in `controller`) implementiert `Screen` **und** `ScreenView` in einer Klasse;
    `getView()` gibt `this` zurück.
  - `getPane()` liefert ein rohes `new Pane()`. Diese Pane bekommt keine Kinder gesetzt; es wird
    ausschließlich per `pane.setBackground(...)` ein Hintergrund gesetzt.
  - Der Hintergrund (Chrome) wird über `SkinService.get().getStartBackgroundImage()` geholt.
  - Es positioniert weder eine Layout-Pane Kinder (es gibt keine) noch existiert ein
    `ComponentHost` oder eine `UiComponent`.

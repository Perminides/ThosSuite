# Skin-Refactoring — Vorgehensplan

**Stand:** 29.07.2026 · v5 · Schritte 1–3c erledigt, `Skin` von 2660 auf 1706 Zeilen

**Charakter dieses Dokuments:** das Ergebnis der Vorbesprechung vom 26.–28.07. Es hält fest,
*was entschieden ist*, *was bewusst vertagt wurde*, *in welcher Reihenfolge* vorgegangen wird —
plus die geprüften Fakten, auf denen das beruht. Kein Regelwerk (das ist `Design-Regeln.md`, es
wird am Ende nachgezogen), keine Ist-Erfassung (das sind die vier Bestandsaufnahmen).

Die Zielbild-Entscheidung samt verworfener Alternative steht in `Skin-Zielbild-Entscheidung.md`.

---

## 1. Die Ordnung — zwei Regeln, drei greps

### 1.1 Zwischen den Paketen von `shared`

```
oben    shared.ui       Oberfläche — was gezeigt und was verbaut wird
        shared.skin     SkinProperties · Skin (CSS) · die 7 Skins · SkinService · SkinImageCache
        shared.model    Records, Enums, die drei Kontrakte
unten   shared          Config, DB, Log, AppClock, UiUtils
```

> **Nur nach unten.** Ein Paket benutzt, was im Stapel darunter steht. Nie seitwärts, nie nach oben.
>
> **Von außen:** `controller` darf auf jede Sprosse. **Features nie ins Skin-Paket.**
> `shared` greift nie hinaus.

**Warum die UI oben steht:** `shared.ui` baut. Der Skin ist ihr Zulieferer — er hält die Werte, die
CSS nicht ausdrücken kann (Bounds, Fonts, Border, Pfade), und erzeugt das Stylesheet. Wer geliefert
bekommt, steht oben.

### 1.2 Innerhalb von `shared.ui`

Dort gibt es genau eine Beziehung, und sie ist immer gleichgerichtet: **etwas wird aus etwas anderem
zusammengesetzt.** Kein Baustein weiß je, in welcher Oberfläche er landet.

> **In `shared.ui` liegen die Oberflächen, in `shared.ui.components` die Bausteine, aus denen sie
> bestehen. Die Wurzel benutzt `components`, nie umgekehrt.**

Der Discriminator steht schon im Code — die Kontrakte sagen es:

| | wohin |
|---|---|
| implementiert `ScreenView`, oder ist ein Dialog / Alert | **`shared.ui`** — es *wird gezeigt* |
| implementiert `UiComponent`, ist Baustein oder Fabrik | **`shared.ui.components`** — es *wird verbaut* |

Prüffrage in Worten: **wird das Ding gezeigt, oder wird es verbaut?**

Die Feature-Zugehörigkeit steht damit im Klassennamen, nicht im Paketnamen (`DiaryScreenView`,
`WhatsAppChatDialog`). Bei ~12 Oberflächen ist das kein Suchproblem.

### 1.3 Die drei Wächter

```bash
# 1  Der Skin kennt die UI nicht
grep -rn "app\.shared\.ui" src/main/java/app/shared/skin/

# 2  Kein Feature kennt den Skin
grep -rn "app\.shared\.skin" src/main/java/app/{alc,diary,fitbit,learn,mattress,messaging,movie,weekday}/

# 3  Nur shared.ui kennt die Bausteine
grep -rn "app\.shared\.ui\.components" --include=*.java src/main/java/app | grep -v "^src/main/java/app/shared/ui/"
```

Alle drei müssen am Ende leer sein. Stand 29.07.:

| | Treffer | wer noch |
|---|---|---|
| 1 · Skin kennt UI nicht | 3 | `Skin` baut noch `SuiteInfoLabel`, `SuiteImage`, `MultipleChoicePane` |
| 2 · Feature kennt Skin nicht | 3 | nur `learn.MapService` |
| 3 · nur `shared.ui` kennt Bausteine | 1 Datei | nur `Skin` selbst |

**Kein Feature hält mehr eine UI-Komponente.**

Wächter 3 sagt inhaltlich: *Features und `controller` sehen ausschließlich fertige Oberflächen; die
Bausteine sind Interna von `shared.ui`.* Das ist die Regel „fertige Komponente statt loser Teile" —
endlich an der Stelle, an der sie hingehört, nämlich an der Außengrenze. Innerhalb von `shared.ui`
wird mit Bausteinen und Werten gearbeitet; das ist dort der Job.

---

## 2. Zielbild

### 2.1 `shared.skin` — zwei Klassen plus die Skins

```
SkinProperties     die ~140 Felder, loadAllConfigs, parse*, getFieldValue,
                   die Resolver mit Fallback, contentSize
                   + eine bewusst geschnittene öffentliche API

Skin               erzeugt daraus das CSS: styleScene + 23 addXxxStyles + CssBuilder
                   extends SkinProperties

DarkMode, FlatWebSkin, BaseColorSkin (+ Blue/Flower/Red/Spicy)
                   Anzeigename + properties-Datei, extends Skin

SkinService · SkinImageCache
```

**`Skin` behält seinen Namen.** Wenn die Bau-Methoden abgewandert sind, enthält die Klasse genau die
CSS-Erzeugung — und das ist, was ein Skin tut. Kein Rename, keine neue Vokabel, null Aufwand. Die
Feldzugriffe in den 23 `addXxx`-Methoden bleiben unqualifiziert, weil die Vererbungskette steht.

*Ehrlich dazu:* Der Split A/B bringt eine kleinere Datei und eine klare Lesereihenfolge — mehr nicht.
Vererbung ist die engste Kopplung, die Java hat. Der Split, der wirklich etwas einbringt, ist der
zwischen A und den **Bau-Methoden**, weil die das Paket wechseln und die Features vom Skin lösen.

### 2.2 A's öffentliche Fläche

Keine Feld-Getter, sondern zweckgeschnittene Records:

```java
public SessionLayout sessionLayout(String deck, String kategorie);   // fertig aufgelöst
public McMetrics     mcMetrics();
public CardStyle     cardStyle();
public DialogStyle   dialogStyle();
```

**Was drinnen bleibt:** Feldnamen, Reflection, und die Fallback-Kette (Deck → Kategorie → Default),
damit sie nicht in fünfzehn UI-Klassen neu entsteht.

Sie wächst nach Bedarf: erst anlegen, wenn eine Klasse nach `shared.ui` wandert und den Wert
tatsächlich braucht. Nicht auf Verdacht entwerfen.

### 2.3 `shared.ui` — die Zielbelegung

```
shared.ui                        (Oberflächen — was gezeigt wird)
    Alerts                       ← neu, aus Skin.showAlert ×2 + Helfern
    BarChartScreenView
    DashboardScreenView
    DiaryScreenView
    MovieViewerScreenView
    DiaryEditor
    ActivityTableDialog
    AnkiConfigDialog
    RegionConfigDialog
    TextPromptDialog
    WhatsAppChatDialog
    WhatsAppContactDialog
    ImageBatchProcessor
    (später) die learn-Session-Panes
    (später) DatePickerDialog, aus SuiteExporter herausgelöst

shared.ui.components             (Bausteine — was verbaut wird)
    SuiteImage · SuiteInfoLabel · SuiteTextField · SuiteIconButton
    SuiteSuggestionTextField · SuiteTabCommitTextFieldTableCell
    MultipleChoicePane · DashboardTile · DiaryTagInputComponent
    ShapeMapPane · ImageMapPane · MapNodeBuilder · ShapeLayer
    ComponentHost
    Dialogs                      ← neu: createDialog, createDialogContent,
                                   createDialogHeaderBar, setDialogTitle
```

`Dialogs` ist eine **Fabrik**, kein Baustein im engeren Sinn — dieselbe Rolle wie `MapNodeBuilder`,
der dort heute schon liegt, ohne selbst Komponente zu sein.

### 2.4 Blätter bleiben passiv

`SuiteInfoLabel`, `MultipleChoicePane` & Co. *bekommen* Maße, sie *holen* sie nie. Gelesen wird beim
Komponierer, der das Blatt einsetzt:

```java
// in der Session-Pane, shared.ui
SessionLayout layout = props.sessionLayout(deck, kategorie);
questionArea = new SuiteInfoLabel("", layout.question());
mcPane       = new MultipleChoicePane(layout.mc(), props.mcMetrics());
```

---

## 3. Entschieden

1. **U3 — die UI baut.** Siehe `Skin-Zielbild-Entscheidung.md`. Der Preis (öffentliche Wertfläche
   auf A) ist gesehen und akzeptiert; die Gegenleistung ist eine mit greps durchsetzbare Regel.
2. **Kein Fassaden-Zwischenschritt.** Direkt schneiden, Ordnung vorher festgelegt.
3. **Genau eine Klasse kümmert sich um die properties.** A hält die Felder *und* lädt sie.
4. **`Skin` behält seinen Namen** und wird zur CSS-Klasse (§2.1).
5. **Namenswissen verlässt `shared.skin` nie.** Die API liefert Records, keine Feldnamen.
6. **Kein Feature greift je ins Skin-Paket** (Wächter 2).
7. **Nur `shared.ui` kennt `shared.ui.components`** (Wächter 3).
8. **Feature nennt nur Identität.** Ein Feature nennt Deck-Id und Kategorie. Alles Skin-Abhängige —
   Pfade, Bilder, Maße, Layouts — wird jenseits der Grenze aufgelöst.
9. **Das einmalige Cache-Wärmen gehört in den `controller`.** Das Feature liefert die Deck-Ids mit
   Karte, der Controller reicht sie weiter. `MapService.imagePathsFor` und `MapImagePaths` entfallen.
10. **`getContentSize()` ist eine Property** — erledigt, liegt in `SkinProperties`.
11. **`Alerts` liegt in `shared.ui`** (wird gezeigt), **`Dialogs` in `shared.ui.components`**
    (wird verbaut).
12. **Die Kontrakte** (`Screen`, `ScreenView`, `UiComponent`) wandern nach `shared.model` — sie sind
    Definitionen ohne Verhalten, dieselbe Sorte wie die Records.
13. **javafx bleibt in `controller` erlaubt.** `MainWindow` (431 Z.) wird nicht angefasst.

---

## 4. Vertagt

### 4a. Punkt-Notation in den properties (Map + Record)

`learnSessionPanel.world.map=20,20,960,960` wird `Map<String, LearnSessionPanel>` — erste Ebene
offen (Deck-Id aus der Datei), zweite deklariert (Record-Komponente). Regel: *ein Punkt geht eine
Ebene tiefer; ob eine Ebene offen oder deklariert ist, sagt der Typ.* Gedeckelt auf zwei Ebenen.

- **Dafür:** 96 Felddeklarationen verschwinden; ein neues Deck kostet keine Java-Änderung mehr
  (heute 3 Felder pro Länder-Deck); hannover schrumpft von 16 Zeilen auf 1; FailFast fällt ab.
- **Dagegen:** ~30 Zeilen Typ-Ablaufen im Loader; Migration aller sieben properties-Dateien,
  einmalig und unumkehrbar; der Loader ist danach nicht mehr in zehn Sekunden erklärt.
- **Warum vertagbar:** A liegt mit den Feldern **genau wie heute** vor. Die Frage wird zu einer
  Änderung *innerhalb einer einzigen Klasse* — durch das Vertagen billiger, nicht teurer.
- **FailFast ist unabhängig billiger zu haben:** ein Startup-Check, der geladene Schlüssel gegen
  deklarierte Feldnamen hält (~15 Zeilen), findet `borderBackButton` ohne Migration.

### 4b. Zuschnitt der Session-Panes

Dass sie das Feature verlassen und nach `shared.ui` gehen, ist entschieden (3.8). Offen ist nur der
Zuschnitt — eine Klasse pro Session-Art wie heute, oder anders; und wie viel im Feature bleibt
(`SessionPresenter` bleibt dort). Wird mit Schritt 3c entschieden.

Der zugehörige Verantwortungsrahmen, der ins Regelwerk gehört:

| Ebene | entscheidet | Beispiel |
|---|---|---|
| **Feature** | *was* und *wann* | „Deck Deutschland, Frage 7, Antwort war falsch" |
| **View** (`shared.ui`) | *welche Bausteine*, *wie verdrahtet* | „diese Session hat Karte, Frage, Eingabefeld, MC, Zurück-Button" |
| **Skin** | *wie es aussieht, wo es sitzt* | Farben, Fonts, Bounds, welches Wallpaper zu welcher Karte |

Prüffrage: **wovon hängt die Zeile ab?** Skin-Wechsel → Skin. Fachlogik → Feature. Keins von
beidem, nur „soll anders aussehen" → View.

### 4c. Kleineres

- **Kollaps der sieben Skin-Stummelklassen** zu einer Klasse plus Tabelle. Sie sind ein
  Erweiterungspunkt für einen Skin, der in Java etwas berechnen will. Eigene Frage.
- **Re-Warming des Bildcaches nach Skinwechsel.** Heute lädt die erste Bildkarte nach einem Wechsel
  synchron. Bewusst akzeptiert. **Gehört im Code dokumentiert.**
- **`StartScreen`** (Screen + ScreenView in einer Klasse): löst sich per Regel — *ein inhaltsloser
  Screen (reines Chrome/Hintergrund) darf sein eigener `ScreenView` sein.*
- **Dialog-Stufe 2a fehlt im Regelwerk:** der parametrisierte Standarddialog (Primitive rein,
  Primitive oder `null` raus, kein Feature-seitiges Objekt).
- **`SuiteExporter`s Inline-Datumsdialog** wird eine eigene Klasse in `shared.ui` — sonst ruft der
  `controller` die Fabrik `Dialogs` und verletzt Wächter 3.

---

## 5. Bereits erledigt

**27.07. — Vorarbeiten am Dialog-Pfad** (alles gemessen, nicht vermutet):

- **`initOwner`-Bug behoben.** `dialog.initOwner(parent)` überschrieb bedingungslos den ermittelten
  `effectiveParent`; drei Dialoge verloren dadurch ihren Owner.
- **`createDialog(Window, String)` → `createDialog(String)`.** Der Parameter war zu 100 % redundant.
  Zehn Aufrufstellen angepasst.
- **Vier `styleScene`-Aufrufe entfernt.** Nachweislich entbehrlich — siehe §6.
- **DatePicker-Block gelöscht.** Nicht nur überflüssig, sondern schädlich: eine einfarbige Corona
  um das Kalender-Popup, die ohne ihn verschwindet.

**28.07. — Schritt 1: `SkinProperties` herausgezogen.**

- Neue Klasse `app.shared.skin.SkinProperties`, `Skin extends SkinProperties`.
- Umgezogen: alle 140 Felder, `loadAllConfigs`, die vier `parse*`, `getFieldValue`,
  `getDisplayName()`.
- `getContentSize()` ist von hartcodiert zu einem `Dimension2D`-Feld geworden (Vorbereitung für
  einen HighRes-Skin). Achtung: der Loader kennt keinen `Dimension2D`-Zweig — ein
  `contentSize=…` in einer properties-Datei würde still ignoriert.
- Geprüft: `Skin` hat kein Instanzfeld mehr, keine verwaisten Importe, keine Aufrufstelle geändert.
  Suite startet, Skinwechsel und Lernsession laufen.

**28.07. — Paketumstellung (Schritt 2) und Alerts/Dialoge (3a).**

- `shared.ui.contracts` → `shared.model`; `components.learn`, `surfaces` und `surfaces.dialogs`
  aufgelöst. Übrig: `shared.ui` (Oberflächen) und `shared.ui.components` (Bausteine).
- `Alerts` (zeigt) nach `shared.ui`, `SuiteDialog<R>`/`SuiteHeaderBar` (werden verbaut) nach
  `shared.ui.components`. `SuiteDialog` als Unterklasse von `javafx.Dialog` statt als Fabrik — damit
  fielen zwei unchecked Casts weg (`ImageBatchProcessor`, `WhatsAppChatDialog`).
- Das Owner-Fenster aus `SkinService` nach `shared.UiUtils`.
- `SuiteExporter`s Inline-Datumsdialog wurde `shared.ui.DatePickerDialog`.

**28.07. — Feature-Oberflächen (3b).**

- **Dashboard:** `DashboardScreen` reicht `DashboardTileData`-Records durch; `DashboardScreenView`
  baut die Kacheln. `DashboardTile` bekommt die Maße im Konstruktor — `TILE_WIDTH`/`TILE_HEIGHT`
  waren tot, der Skin überschrieb sie unmittelbar nach dem `new`.
- **Diary:** `createDiaryViewer`, `createDiaryCard` und das Record `DiaryViewerComponents` sind weg.
  Neu: `DiaryCard extends VBox` und `SuiteDatePicker`. Der tote Parameter `createdAt` entfiel.
- **Movie:** `createMovieViewer`, `createCard`, `createLinkedPersonLine`, `setupCommentTooltip` und
  `MovieViewerComponents` sind weg. Neu: `MovieCard extends HBox`.

**29.07. — learn (3c).**

- **Region:** `learn.region.SessionPane` gelöscht, dafür `shared.ui.RegionSessionView`. Der
  Presenter treibt sie direkt. `ShapeMapState` als eigenständiges Record nach `shared.model`.
- **Anki:** die drei Panes (432 Zeilen) gelöscht, dafür `AnkiSessionView` (abstrakt) mit
  `GermanySessionView`, `McSessionView`, `ImageMapSessionView` — je unter 32 Zeilen.
- Die zwei Kartensprachen liegen hinter `SessionMap` (`ShapeSessionMap`, `ImageSessionMap`,
  `NoSessionMap`). `SessionCallbacks` bündelt die vier Rückmeldungen.
- **Gefunden und behoben:** `beginTx`/`endTx` waren tote No-ops (Interface-Defaults, nie
  implementiert, viermal gerufen). Und der deck-eigene Hintergrund war seit unbekannt wann bei
  Germany statt bei MC — sichtbar geworden, weil die drei Konfigurationen erstmals nebeneinander
  stehen.

Offene Kleinigkeiten: die Kommentare `// Owner intern` bei `AnkiConfigDialog` und
`RegionConfigDialog` sind überflüssig; der Kommentar bei `createDialog` könnte die **Bindung**
benennen statt „erbt".

---

## 6. Geklärt: wie das CSS in Dialoge kommt

`javafx.scene.control.Dialog` läuft intern über `HeavyweightDialog`. Dessen `updateStageBindings`:

```java
if (newOwner instanceof Stage newStage) {
    Scene newScene = newStage.getScene();
    if (newScene != null && dialogScene != null) {
        Bindings.bindContent(dialogScene.getStylesheets(), newScene.getStylesheets());
    }
}
```

**`initOwner(stage)` bindet die Stylesheet-Liste des Dialogs an die des Owner-Fensters** — kein
Kopieren, eine lebende Bindung. Kein Fallback, wenn der Owner `null` ist.

| Beobachtung | Erklärung |
|---|---|
| Dialoge korrekt gestylt ohne jedes `styleScene` | die Bindung erledigt es — die vier Aufrufe waren immer redundant |
| `AnkiConfigDialog` gelb, obwohl `getOwner()` null war | der alte Code band erst und entband dann; `unbindContent` **leert die Liste nicht** |
| Rohe `new Alert(...)` in `ThosSuiteApp`/`DB` grau | nie ein `initOwner` → nie gebunden → leere Liste |
| Skinwechsel wirkt auf offene Dialoge | `styleScene` macht `clear()`+`add()` auf der Hauptscene, die Bindung zieht nach |

**Folge:** `MainWindow:111` ist die einzige Stelle, die das CSS je setzt, und sie muss bleiben. Und
`initOwner` ist für jeden Dialog **Pflicht** — nicht wegen des Fensterverhaltens, sondern wegen der
Optik.

Quelle: [`HeavyweightDialog.java`, openjdk/jfx](https://raw.githubusercontent.com/openjdk/jfx/master/modules/javafx.controls/src/main/java/javafx/scene/control/HeavyweightDialog.java)

### 6a. Wie skin-abhängig die Dialoge wirklich sind

Der gesamte Dialog- und Alert-Apparat fasst **zwei** Skin-Werte an:

| Methode | Skin-Werte |
|---|---|
| `createDialogContent()` | **keine** — `new VBox(15)` + Style-Klasse |
| `installCloseBlocker`, `toButtonType` | keine |
| `createDialogHeaderBar` | **eine**: `font.getSize() * 0.3` fürs Titel-Padding |
| `buildAlertContent` | **eine**: `tintImageWithTextColor`, nur bei gesetztem Bild (ein Aufrufer suiteweit: `TurnDialog`) |
| `createDialog`, `showAlert` | keine eigenen |

Dazu `SkinService.getOwnerWindow()` — kein Skin-Wert, sondern das Hauptfenster, aus Bequemlichkeit
dort geparkt. Beim Umzug: das Padding wandert ins generierte CSS, der Owner wird vom `controller`
gesetzt, die Tint-Farbe kommt aus A. **Und `shared.ui` darf den Skin ohnehin rufen** — das ist
abwärts. Ein Push-Mechanismus wird nicht gebraucht.

---

## 7. Ausgangslage (geprüft, 26.–28.07.2026)

| | |
|---|---|
| Features javafx-frei | **8 von 8** |
| javafx sonst | `controller` 4 Dateien; `shared` 34, davon 4 außerhalb `ui`/`skin` |
| `Skin` nach Schritt 1 | ~2350 Zeilen, kein Instanzfeld mehr, 20 `create…`-Methoden, 23 `addXxxStyles` |
| `SkinProperties` | 140 Felder + `contentSize`, 7 Methoden |
| Skin-Aufrufe von außerhalb `shared` | ~90 Aufrufstellen, 38 davon `showAlert` |

**Befunde, die den Plan begründen:**

- **Alle sieben Skin-Subklassen sind feldfrei** — sie tragen einen Anzeigenamen und eine
  properties-Datei.
- **96 Felder hängen an `getFieldValue`:** 69 `Rectangle2D`, 16 `…WallpaperName`, 8 `…MapImageName`.
  Layout-Felder je Deck: `world` 9, `hannover` 8, `germany` 8, `mc` 6, je 3 für
  `us region oz it hs hr es en cs ch br be au`.
- **Layouts variieren wirklich pro Skin.** `worldSessionMapPanel`: `20,20` (basecolor/darkmode),
  `930,20` (erbende Skins), `540,20` (flatweb).
- **`hannover` hat 8 Felder, die in keiner properties-Datei vorkommen.** Sie werden in `styleScene`
  aus den `world`-Feldern kopiert. Ein Kategorie-Fallback kann das nicht ersetzen:
  `HANNOVER_CARDS` hat Kategorie `ANKI_DECK`, genau wie `WORLD_CARDS`. Deck-auf-Deck.
- **`borderBackButton`** steht in `skin_basecolor.properties`, hat kein Feld, wird still ignoriert.
- **Namenswissen verlässt `shared.skin` heute schon nicht.**
- **`createIconButton` hat keinen Fallback.** Kein Fehler — Icon-Buttons gibt es nur bei `germany`,
  `mc`, `world`. Aber nichts erzwingt das.

---

## 8. Reihenfolge

```
1  ✓ SkinProperties herausgezogen                                        28.07.
2  ✓ Paketumstellung                                                     28.07.
3a ✓ Alerts + Dialoge                                                    28.07.
3b ✓ Feature-Oberflächen (Dashboard, Diary, Movie, BarChart)             28.07.
3c ✓ learn (Region und Anki)                                             29.07.

── offen ──────────────────────────────────────────────────────────────────────

3e  MapService
    Die letzten drei Skin-Importe in einem Feature. Das Cache-Vorwärmen wandert
    in den controller (Entscheidung 3.9); imagePathsFor und MapImagePaths entfallen.
    Klein. Danach ist Wächter 2 leer.

3f  Die drei restlichen Bau-Methoden
    createSessionInfoLabel, createImageComponent, createMultipleChoicePane.
    Hier kommen SessionComponent und sessionBounds(...) zurück — die Komponenten
    nehmen ihre Maße dann selbst entgegen. Zugleich der Anlass für die
    SuiteXXX-Durchsicht (siehe Vertagte-Punkte.md): SuiteTextField und
    SuiteIconButton sind danach reine Fassaden ohne Zweck.
    Danach ist Wächter 1 leer.

3d  Chrome und Menüs
    createMenuBar, createMenu, createMenuItem, createMainWindowHeaderBar,
    createResponsiveHeaderIcon. Zuletzt, weil MainWindow daran hängt.

4   Aufräumen
    Skin enthält danach nur noch die CSS-Erzeugung — kein Rename nötig.
    Alle drei Wächter auf leer.

5   Regelwerk und Architekturdokument nachziehen
    Design-Regeln.md: die Ordnung aus §1, der Verantwortungsrahmen aus §4b in
    Thorstens Formulierung, StartScreen-Regel, Dialog-Stufe 2a, die drei Wächter
    als bewachte Zusagen, der neu gefasste Skin-Vertrag.
    Architektur-Dokumentation.md: der Abschnitt „UI-Architektur: Skin-System" ist
    dann vollständig überholt.
```

**Warum die Paketumstellung vor die Auflösung rückt:** sie ist risikoarm (reine Moves) und liefert
etwas zum Durchklicken. Fühlt sich die Ordnung beim Navigieren falsch an, merkt man es *bevor* Code
hineingegossen wird.

**Warum das CSS zuletzt kommt und nichts kostet:** der CSS-Block und die Bau-Methoden sind
vollständig entkoppelt (geprüft — keine Bau-Methode ruft `styleScene` oder `CssBuilder`; die
`addXxx` erwähnen `create…` nur in Kommentaren). Wenn die Bau-Methoden abgewandert sind, ist `Skin`
von selbst die CSS-Klasse. Der ursprünglich geplante Weg — `SkinCss` per Komposition — hätte ~200
Feldzugriffe zu `props.…` umschreiben müssen, für reine Ordnung.

---

## 9. Verworfene Ideen

- **U1 — der Skin baut** (`shared.skin` über `shared.ui`). Die ernsthafte Alternative; vollständige
  Gegenüberstellung in `Skin-Zielbild-Entscheidung.md`.
- **U2 — Werte per Push in die UI schieben.** U3 mit zusätzlicher Schiebe-Mechanik.
- **`shared.ui.<feature>`-Zweige** (die frühere Entscheidung 3.11). Ersetzt durch Oberfläche vs.
  Baustein: innerhalb von `shared.ui` ist „wird gebaut aus" die tragende Beziehung, nicht die
  Feature-Zugehörigkeit. Die steht im Klassennamen.
- **Eine fünfte Sprosse `shared.components`.** Sie sollte ein Problem lösen, das eine zu strenge
  Fassung der Ordnungsregel erzeugt hatte. Mit der korrigierten Regel unnötig.
- **Der Art-Schnitt `surfaces` / `dialogs` / `components`.** `surfaces` vs. `dialogs` trug nichts —
  beides sind Oberflächen.
- **`SkinCss` als eigener Name.** Was übrigbleibt, *ist* der Skin.
- **Geteilter properties-Leser.** Es soll genau *eine* Klasse für die properties geben.
- **Umzug der Session-Layouts nach `shared.ui`.** Die Layouts variieren pro Skin.
- **Namensmuster-Magie im Loader.** Entweder die Regel ist generell und in der Datei sichtbar, oder
  die Ladeseite bleibt unangetastet.
- **Split der properties-Dateien.** Eine Datei pro Design.
- **javafx komplett aus `controller`.**
- **Dünne `Skin`-Fassade als Zwischenschritt.**
- **Ein viertes Paket D für Bilder/Pfade.**
- **Sechsstufige Sprossen-Leiter** und die **Fertigungsketten-Metapher** als Ordnungsregel.

---

## 10. Korrekturen an früheren Annahmen

- **`loadAllConfigs` setzt nichts zusammen.** Es macht `props.getProperty(field.getName())`.
  Zusammengesetzt wird nur auf der *Leseseite*. Datei-Reihenfolge: basecolor → subskin.
- **Die Gottklasse ist ohne Loader-Änderung zerlegbar.**
- **`SkinImageCache` ist skin-spezifisch** (hält `cachedSkin`, invalidiert bei Skinwechsel). Bleibt
  in `shared.skin`; unter U3 dürfen die UI-Klassen ihn rufen.
- **Dialoge brauchen kein `styleScene`.** Popups auch nicht. §6.
- **`createIconButton` ohne Fallback ist kein Bug**, nur ungesichert.
- **Der hannover-Fallback lässt sich nicht über die Kategorie ausdrücken.**
- **Ein Unterpaket darf sein Elternpaket benutzen.** Meine erste Fassung der Ordnungsregel verbot
  das und war damit strenger als das bestehende Regelwerk (`learn.anki → learn`). Aus dem Fehler
  folgten die fünfte Sprosse und mehrere Scheinprobleme.
- **`Alerts` gehört nach `shared.ui`, nicht in `components`** — es wird gezeigt, nicht verbaut. Die
  Fehlplatzierung wäre ein direkter Verstoß gegen Wächter 3 gewesen.

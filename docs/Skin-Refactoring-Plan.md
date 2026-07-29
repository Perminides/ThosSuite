# Skin-Refactoring — Vorgehensplan

**Stand:** 29.07.2026 · v9 · Schritte 1–4 erledigt. **Alle drei Wächter sind leer und werden vom
Build bewacht**, der Paketgraph ist zyklenfrei. `Skin` von 2660 auf **1241** Zeilen und hat genau
eine öffentliche Methode: `styleScene`. Offen ist nur noch 5 (Regelwerk und Architekturdokument).

**Charakter dieses Dokuments:** es blickt **zurück** — *was entschieden ist*, *was erledigt ist*,
*in welcher Reihenfolge* vorgegangen wird, plus die geprüften Fakten, auf denen das beruht, und die
Annahmen, die sich als falsch erwiesen haben.

Was **offen** ist, steht ausschließlich in `Vertagte-Punkte.md`. Die Regeln selbst stehen in
`Design-Regeln.md` (wird am Ende nachgezogen), die Ist-Erfassung in den vier Bestandsaufnahmen, die
Zielbild-Entscheidung samt verworfener Alternative in `Skin-Zielbild-Entscheidung.md`.


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

**Alle drei sind seit 29.07. leer**, und sie bleiben es nicht aus Disziplin: die drei greps sind
als ArchUnit-Regeln in `src/test/java/app/ArchitekturRegelnTest.java` hinterlegt und brechen den
Build. Dazu als vierte Regel die Zyklenfreiheit des Paketgraphen — die greps können das nicht
prüfen, und sie war eine der sieben Ausgangsforderungen.

```
Tests run: 4, Failures: 0, Errors: 0
```

Die greps oben bleiben trotzdem stehen: sie sind die Erklärung der Regel in einer Zeile, und man
kann sie ohne Build laufen lassen.

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

**`Skin` behält seinen Namen.** Seit Schritt 4 enthält die Klasse genau die CSS-Erzeugung — und
das ist, was ein Skin tut. Kein Rename, keine neue Vokabel, null Aufwand. Die
Feldzugriffe in den 23 `addXxx`-Methoden bleiben unqualifiziert, weil die Vererbungskette steht.

*Ehrlich dazu:* Der Split A/B bringt eine kleinere Datei und eine klare Lesereihenfolge — mehr nicht.
Vererbung ist die engste Kopplung, die Java hat. Der Split, der wirklich etwas einbringt, ist der
zwischen A und den **Bau-Methoden**, weil die das Paket wechseln und die Features vom Skin lösen.

### 2.2 Die öffentliche Fläche von `SkinProperties`

Keine Feld-Getter, sondern zweckgeschnittene Records und Werte. Stand 29.07.:

```java
DialogStyle       dialogStyle();          // Werte-Bündel, ohne Schlüssel
DiaryStyle        diaryStyle();
MovieStyle        movieStyle();
DashboardTileStyle dashboardTileStyle();
Dimension2D       getContentSize();
int               bigComponentCornerRadius();
int               bigComponentBorderWidth();

Rectangle2D sessionBounds(String mapName, String kategorie, SessionComponent teil);
Rectangle2D sessionBounds(String mapName, String kategorie, Skin.TextLabelType typ);
McMetrics   mcMetrics();
MapImages   mapImages(String mapName);    // mit Schlüssel — der Aufrufer löst auf
Image       iconFor(Skin.IconButtonType rolle);
```

Die Trennlinie ist die Schlüssel-Regel (§3.15): was ohne Argument auskommt, holt sich der Baustein
selbst; was einen Schlüssel braucht, löst der Aufrufer auf und reicht das Ergebnis weiter.

**Was drinnen bleibt:** Feldnamen, Reflection, und die Staffelung (spezifisch → Kategorie), die
genau einmal existiert — in `SkinProperties.staffelung`.

*Ursprünglich geplant waren `SessionLayout`, `McMetrics` und `CardStyle`.* `SessionLayout` wurde
verworfen: ein Record mit neun Feldern, von denen je nach Lernform die meisten `null` wären. Die
Fläche wächst nach Bedarf, nicht auf Verdacht.

### 2.3 `shared.ui` — die Belegung

```
shared.ui                        (Oberflächen — was gezeigt wird)
    Alerts
    BarChartScreenView · DashboardScreenView · DiaryScreenView · MovieViewerScreenView
    DiaryEditor · ImageBatchProcessor
    ActivityTableDialog · AnkiConfigDialog · RegionConfigDialog · TextPromptDialog
    WhatsAppChatDialog · WhatsAppContactDialog · DatePickerDialog
    AnkiSessionView (abstrakt) + ShapeMapSessionView · McSessionView · ImageMapSessionView
    RegionSessionView
    MainWindowHeaderBar
    ComponentHost

shared.ui.components             (Bausteine — was verbaut wird)
    SuiteImage · SuiteInfoLabel · SuiteTextField · SuiteIconButton
    SuiteSuggestionTextField · SuiteTabCommitTextFieldTableCell · SuiteDatePicker
    SuiteDialog · SuiteHeaderBar
    MultipleChoicePane · DashboardTile · DiaryCard · MovieCard · DiaryTagInputComponent

shared.ui.components.map         (die Karten und ihr Innenleben)
    SessionMap (Kontrakt) · ShapeMapPane · ImageMapPane · NoSessionMap
    MapNodeBuilder · ShapeLayer          (beide paketprivat)
```

`SuiteDialog` ist eine Unterklasse von `javafx.Dialog` geworden, keine Fabrik — die ursprünglich
geplante Fabrik `Dialogs` gibt es nicht.

### 2.4 Bausteine bekommen ihre Lage, sie holen sie nicht

```java
// in der Session-View, shared.ui
questionArea = new SuiteInfoLabel("", skin.sessionBounds(mapName, kategorie, TextLabelType.QUESTION));
inputField   = new SuiteTextField(skin.sessionBounds(mapName, kategorie, SessionComponent.TEXT_INPUT));
```

Jeder dieser Bausteine hat daneben einen Konstruktor **ohne** Lage, für Aufrufer, die ihn in ein
Layout hängen statt ihn absolut zu positionieren. Der Lage-Konstruktor delegiert an den anderen,
damit keiner der beiden unbenutzt ist. Was ohne Schlüssel auskommt — Eckradius, Rahmenbreite,
Icon-Bild — holt sich der Baustein dagegen selbst (§3.15).

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
14. **Der Verantwortungsrahmen** — wer entscheidet was:

    | Ebene | entscheidet | Beispiel |
    |---|---|---|
    | **Feature** | *was* und *wann* | „Deck Deutschland, Frage 7, Antwort war falsch" |
    | **View** (`shared.ui`) | *welche Bausteine*, *wie verdrahtet* | „diese Session hat Karte, Frage, Eingabefeld, MC, Zurück-Knopf" |
    | **Skin** | *wie es aussieht, wo es sitzt* | Farben, Fonts, Bounds, welches Wallpaper zu welcher Karte |

    Prüffrage: **wovon hängt die Zeile ab?** Skin-Wechsel → Skin. Fachlogik → Feature. Keins von
    beidem, nur „soll anders aussehen" → View.

    In Thorstens Formulierung: *„Stell eine Frage" sagt das Feature. „Zeige auf dem Fragepanel
    diesen Text" passiert in `shared.ui`.*
15. **Die Schlüssel-Regel** (29.07.): *Ein Baustein holt sich beim Skin, was für jede Verwendung
    gleich ist. Was von der Verwendung abhängt, bekommt er übergeben.* Ablesbar an der Signatur des
    Skin-Zugangs — braucht er ein Argument vom Aufrufer, löst der Aufrufer auf. Gilt ausnahmslos,
    auch für Feature-Bausteine wie `MovieCard`.
16. **Bausteine erben, sie verhüllen nicht.** `SuiteTextField extends TextField` statt einer Fassade
    um ein `TextField`. Die Grenze, die Fassaden schützten (Features ohne javafx), verläuft nicht
    mehr hier — innerhalb von `shared.ui` kennen beide Seiten javafx.
17. **Bausteine heißen nach dem, was sie sind, nicht nach dem, wofür sie gerade benutzt werden.**
    Deshalb `ShapeMapSessionView`, nicht `GermanySessionView`. Der Prüfsatz dazu: *was, wenn ein
    zweites Deck derselben Bauart dazukäme?*

---

## 4. Was offen ist

Was offen ist, steht **nicht hier**, sondern in `Vertagte-Punkte.md` — sortiert danach, wann es
fällig wird. Dieses Dokument blickt zurück (entschieden, erledigt, geprüft, verworfen), jenes nach
vorn. Ein früherer §4 „Vertagt" führte beides parallel und hatte prompt einen Punkt als *vertagt*
gelistet, den §5 desselben Dokuments als *erledigt* auswies.


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
  `ShapeMapSessionView`, `McSessionView`, `ImageMapSessionView` — je unter 32 Zeilen.
- Die zwei Kartensprachen liegen hinter `SessionMap` (`ShapeSessionMap`, `ImageSessionMap`,
  `NoSessionMap`). `SessionCallbacks` bündelt die vier Rückmeldungen.
- **Gefunden und behoben:** `beginTx`/`endTx` waren tote No-ops (Interface-Defaults, nie
  implementiert, viermal gerufen). Und der deck-eigene Hintergrund war seit unbekannt wann bei
  Germany statt bei MC — sichtbar geworden, weil die drei Konfigurationen erstmals nebeneinander
  stehen.

**29.07. — `ShapeMapPane` ist jetzt selbst ein Node (3f, erster Teilschritt).**

- `Skin.buildShapeMapWrapper` (49 Zeilen) gelöscht. Messen, Skalieren, Positionieren und der
  Strichdicken-Fix stecken jetzt in `ShapeMapPane`, die von `StackPane` erbt. `getView()` liefert
  `this` und bleibt nur, solange der `ComponentHost` über `UiComponent` einhängt.
- `node.setStyle("")` ist entfallen. Es sollte Inline-Strichdicken einer früheren Session
  wegräumen — die Nodes entstehen aber im selben Konstruktor, es konnte nichts zu räumen geben.
- Neu und öffentlich: `SkinProperties.sessionMapBounds(mapName, kategorie)`. Das erste Session-Maß,
  das das skin-Paket verlässt; der Property-Name bleibt drin. **Offen:** ob die restlichen zehn
  ebenfalls benannte Methoden werden oder eine `sessionBounds(mapName, kategorie, teil)` mit
  Aufzählung — entscheidet sich, wenn die Bau-Methoden nachziehen.
- Thorstens TODO an der Stelle („Das Konstrukt ist so kompliziert und ich weiß auch nicht mehr genau
  warum wir das so gebaut hatten") ist damit beantwortet: der Umweg existierte nur, weil die Karte
  nicht an die Skin-Werte kam. Unter U3 kommt sie ran.

**29.07. — `SuiteTextField` und `SuiteIconButton` erben statt zu verhüllen (3f, zweiter Teilschritt).**

- `SuiteTextField extends TextField`, `SuiteIconButton extends Button`. Beide holen ihre Maße selbst
  (`new SuiteTextField(mapName, kategorie)` statt `new SuiteTextField(skin.createInputField(…))`).
- `Skin.createInputField` gelöscht. Sein Javadoc versprach eine CSS-Klasse `input-field`, die die
  Methode nie gesetzt hat — das Feld wird über `.text-field` gestylt.
- `Skin.createIconButton` → `iconButtonStyle(deckId, typ)`, liefert das Record `IconButtonStyle`
  (fertig eingefärbtes Bild + Feld). Das Zusammenbauen — `ImageView`, Style-Klasse, Maße — macht
  jetzt der Knopf. Dateinamen und Tönungsfarbe bleiben Skin-Werte und bleiben drin.
- Neu: `SkinProperties.sessionInputBounds(mapName, kategorie)`.
- Alle drei Klassen behalten vorerst `implements UiComponent` mit `getView() { return this; }` — der
  `ComponentHost` bekommt noch `UiComponent...`, weil die `SessionMap`-Implementierungen keine Nodes
  sind. Das fällt zusammen mit ihnen.

**29.07. — Die Bausteine nehmen Maße entgegen (3f, dritter Teilschritt).**

Zugrunde liegt eine Regel, die Thorsten aus einer schlechteren von mir herausgeschält hat. Mein
erster Versuch lautete „ein Baustein darf sich holen, was nicht sessionabhängig ist" — sein Einwand:
eine allgemeine Regel darf kein Feature-Wort enthalten. Die tragfähige Fassung ist mechanisch an der
Signatur ablesbar:

> **Ein Baustein darf sich jeden Skin-Wert selbst holen, für den der Skin keinen Schlüssel vom
> Aufrufer braucht. Alles, wofür er einen braucht, kommt vom Aufrufer.**

```java
bigComponentCornerRadius()                  // kein Argument     → Baustein holt selbst
iconFor(rolle)                              // Rolle, kein Kontext → Baustein holt selbst
sessionBounds(mapName, kategorie, teil)     // braucht Schlüssel  → Aufrufer löst auf
```

Ein Schlüssel *ist* der Kontext. Wer einen liefern muss, weiß etwas, das der Baustein nicht wissen
soll. Die Regel gilt ausnahmslos — auch für `ShapeMapPane`, die deshalb ihr Rechteck übergeben
bekommt statt es zu holen.

- **Vier Bausteine, je zwei Konstruktoren** — einer ohne Lage (für Layout-Container), einer mit
  (für absolut positionierende Hosts), der zweite delegiert an den ersten. Keiner ist tot.
  `SuiteTextField()` · `SuiteIconButton(rolle)` · `SuiteImage(w, h)` · `SuiteInfoLabel(text)`.
- **`SuiteInfoLabel` bekommt `.my-info-label`.** Vorher hatte sie <b>kein</b> eigenes Aussehen: der
  Skin stylte ausschließlich `#QuestionLabel`/`#ProgressLabel`/`#HistoryLabel`, außerhalb einer
  Session war sie nackt. Die drei ID-Regeln waren identisch bis auf die Hintergrundfarbe — jetzt
  trägt die Klasse alles Gemeinsame, die Kennungen nur noch die Abweichung. Das entspricht der
  Fallback-Kette, die beim Laden ohnehin schon auf `displayTextBgColor` zurückfällt.
- **Gelöscht:** `createImageComponent`, `createSessionInfoLabel`, das Record `IconButtonStyle`.
  `iconButtonStyle(deckId, typ)` wurde zu `iconFor(rolle)` — die Maße kommen jetzt getrennt.
- **Neu:** `SessionComponent` (Aufzählung, hält die Property-Endung) und
  `sessionBounds(mapName, kategorie, teil)` mit einer Überladung für `TextLabelType`. Die Staffelung
  spezifisch→Kategorie steht damit einmal statt sechsmal.
- Die `* 2`-Umrechnung für den Eckradius ist aus dem Skin in `SuiteImage` gewandert, dorthin also,
  wo `Rectangle.setArcWidth` gerufen wird. Der Skin führt durchgehend Radien, wie das CSS auch.
  Der unsichere Kommentar dazu ist beantwortet und weg.

Wächter 1: 3 → **1** (nur noch `MultipleChoicePane`). `mvn test` grün.

**29.07. — Die letzte Bau-Methode verlässt den Skin (3f fertig).**

- `createMultipleChoicePane` gelöscht. `MultipleChoicePane.Metrics` wurde zu `shared.model.McMetrics`
  und hat Knopfhöhe und Zwischenraum gleich mitbekommen — dadurch kann der Skin das Record füllen,
  ohne die Komponente zu importieren. Genau das war der letzte Faden zwischen den beiden Paketen.
- Die Auswahl holt sich ihre Maße jetzt über `mcMetrics()` selbst und bekommt nur ihr Feld.
- Thorstens TODO an der Stelle — *„streng genommen sickern damit skin.properties in eine
  UI-Komponente, nochmal prüfen wie ok das ist"* — ist damit beantwortet, und zwar nach einer Regel
  statt nach Gefühl: die Werte hängen an Schrift und Rändern, nicht am Aufrufer. Kein Schlüssel,
  also holt die Komponente sie selbst (§3.15).

**Damit sind alle drei Wächter leer und der Paketgraph zyklenfrei.** Die drei bis dahin
auskommentierten ArchUnit-Regeln sind scharf gestellt; der Test läuft mit vier Regeln grün.

**29.07. — Schritt 4 abgeschlossen: `Skin` ist nur noch CSS.**

Die sieben Werte-Zugänge sind nach `SkinProperties` gewandert (`wallpaperPath`,
`emptyWallpaperPath`, `startScreenWallpaperPath`, `mcMetrics`, `iconFor`, `getOverlayContentBounds`,
`mapImages`) samt ihrer privaten Helfer. Übrig bleibt in `Skin`: **eine** öffentliche Methode
`styleScene`, dahinter 23 `addXxxStyles` und der `CssBuilder`.

- **Die zwei Aufzählungen zogen mit** (`IconButtonType`, `TextLabelType`) — nicht aus Ordnungsliebe:
  `SkinProperties` hatte bereits `sessionBounds(…, Skin.TextLabelType)`, die Elternklasse verwies
  also auf ihr Kind. Alle Aufrufstellen bleiben unverändert, weil geschachtelte Typen vererbt werden
  und `Skin.TextLabelType` weiterhin auflöst.
- `mcLineSpacingSqueezed()` musste `protected` statt `private` werden: den Wert braucht auch die
  CSS-Seite, dort wird die zweizeilige MC-Stufe gestylt. Steht als Satz an der Methode.
- `SkinProperties` kennt jetzt `Config` und `UiUtils` — beides liegt unterhalb, die Ordnung erlaubt
  es. Und `computeMcButtonHeight()` baut kurz ein `javafx.scene.text.Text`, um die Schrifthöhe zu
  messen; das Objekt wird sofort verworfen und gilt bewusst nicht als „bauen".

**29.07. — Hintergründe als Wert, StartScreen aufgeteilt (Teil von Schritt 4).**

- **Der Skin liefert Pfade, nicht Bilder.** `getBackgroundImage(…)` und die zwei Verwandten sind
  `wallpaperPath(mapName, kategorie)`, `emptyWallpaperPath()` und `startScreenWallpaperPath()`
  geworden. `contain`/`cover`/`CENTER`/`NO_REPEAT` sind **keine** Skin-Entscheidung — sie sind für
  jedes Spielfeld gleich und liegen jetzt in `shared.ui.components.SuiteBackground`.
- **`SuiteBackground.of(pfad)` liefert einen fertigen `Background`.** Warum eine Fabrik und keine
  Unterklasse: `Background` **und** `BackgroundImage` sind `final` — die Wertfamilie des
  layout-Pakets ist versiegelt. Verhüllen brächte auch nichts, weil `Region.setBackground` den Typ
  ohnehin wieder verlangt; er müsste an jeder Aufrufstelle ausgepackt werden. Die acht Stellen, die
  vorher alle `pane.setBackground(new Background(x))` schrieben, sagen jetzt nur noch
  `pane.setBackground(SuiteBackground.of(pfad))`.
- **`StartScreen` aufgeteilt** in `controller.StartScreen implements Screen` (Navigation) und
  `shared.ui.StartScreenView implements ScreenView` (Anzeige). Damit implementiert keine Klasse mehr
  beide Verträge; die acht Screens liegen ausnahmslos bei ihrem Feature, die ScreenViews ausnahmslos
  in `shared.ui`. **Das löscht eine Sonderregel** — „ein inhaltsloser Screen darf sein eigener
  ScreenView sein" wird nicht mehr gebraucht.
- **`reload()` → `rebuild()`** an fünf Stellen. Vier Screens lesen sich jetzt wortgleich als
  `refresh() { view.rebuild(); }`.

**Verworfen dabei:** eine Vorschrift `rebuild()` auf dem `ScreenView`-Vertrag. Drei der sechs
Umsetzungen können das nicht — `DashboardScreenView.build(daten)` und
`MovieViewerScreenView.setNames(…)` brauchen Daten von außen, `ComponentHost` ist eine passive
Leinwand. Die Vorschrift gibt es ohnehin schon eine Ebene höher: `Screen.refresh()`.

**29.07. — Die Hintergrundbilder (Teil von Schritt 4).**

Ausgelöst durch eine Frage, die wie eine Kleinigkeit aussah: warum heißt eine Methode
`getEmptyBackgroundImage`, liefert aber `defaultWallpaperName`?

- **Befund:** die zwei Rückfall-Ketten waren gegenläufig. Der Startbildschirm bevorzugte
  `emptyWallpaperName`, alle anderen kartenlosen Bildschirme `defaultWallpaperName`. Vier der sieben
  Skins setzen beide Werte, es war also sichtbares Verhalten, kein schlafender Fehler.
- **Ursache:** ein Namensproblem. `emptyWallpaperName` meinte das *geschmückte* Bild des Skins,
  `defaultWallpaperName` das *leere*, das nicht ablenkt. Die Namen sagten das Gegenteil.
- **Umbenannt** (in 6 properties-Dateien und 2 Klassen, zweistufig weil die Namen kollidieren):

  ```
  emptyWallpaperName    →  startScreenWallpaperName    das geschmückte, nur für den Startbildschirm
  defaultWallpaperName  →  emptyWallpaperName          das leere, für alles ohne eigenes
  ```

  **Achtung:** die properties-Dateien liegen außerhalb von git. Dieser Absatz ist die einzige
  Aufzeichnung der Umbenennung.
- **`usesDeckBackground()` entfallen** — abstrakte Methode plus drei Implementierungen. Bei zwei von
  drei Lernformen wurde die Staffelung übersprungen; deshalb las `worldWallpaperName` niemand. Jetzt
  fragt jede Session dieselbe Kette: eigenes Bild der Karte → das der Kategorie → das leere.
- **Vier tote properties-Zeilen entfernt** (`lk` in drive/moonlight/spicy, `world` in moonlight) —
  ihr Wert war identisch mit dem, was die Kette ohnehin liefert. Ausgerechnet, nicht geschätzt.
- Die drei Hintergrund-Methoden waren zu 20 Zeilen wortgleich; daraus wurde ein privates
  `backgroundImage(name)`, die drei öffentlichen sind Einzeiler. Der `null/null`-Sonderzweig im
  Namensauflöser ist damit weg.

**29.07. — Chrome und Menüs (3d).**

- `createMenuBar`, `createMenu`, `createMenuItem` waren in voller Länge `return new MenuBar()`,
  `return new Menu(text)`, `return new MenuItem(text)` — kein Skin-Wert, keine Style-Klasse. Warum
  es sie gab, ist unbekannt; `MainWindow` durfte javafx immer benutzen. Ersatzlos gestrichen,
  24 Aufrufstellen wurden `new …`. `buildMenuBar(Skin)` braucht seinen Parameter nicht mehr.
- `createMainWindowHeaderBar` + `createResponsiveHeaderIcon` wurden `shared.ui.MainWindowHeaderBar`.
  Sie setzt ihre Style-Klasse selbst; vorher stand das im `MainWindow`.
- Der einzige Skin-Wert der Leiste war ein Innenabstand (`font.getSize() * 0.5`). Der ist eine
  CSS-Regel `.my-header-leading` geworden — damit braucht die Leiste **keinen** Skin-Zugang, und es
  musste kein öffentlicher Schrift-Getter aufgemacht werden.
- Der Stream in der Symbol-Auswahl wurde eine Schleife. Verhalten unverändert, inklusive der
  Annahme, dass die Symbole aufsteigend sortiert vorliegen — die steht jetzt im Javadoc.

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
3e ✓ MapService — danach war Wächter 2 leer                              29.07.
3f ✓ Die Bau-Methoden verlassen den Skin — in vier kleinen Schritten     29.07.
      ShapeMapPane (buildShapeMapWrapper zog ein)
      SuiteTextField + SuiteIconButton (Vererbung statt Fassade)
      alle vier Bausteine nehmen Maße entgegen → Schlüssel-Regel
      createMultipleChoicePane, Metrics wird shared.model.McMetrics
3d ✓ Chrome und Menüs                                                    29.07.
      drei Fabriken waren pure new X() → ersatzlos, 24 Aufrufstellen
      Header-Leiste wurde shared.ui.MainWindowHeaderBar
      MainWindow ist damit noch nicht rund — siehe Vertagte-Punkte.md
6  ✓ Die Wächter in den Maven-Build                                      29.07.
      ArchUnit, src/test/java/app/ArchitekturRegelnTest.java.
      Vier Regeln scharf: die drei Wächter plus Zyklenfreiheit.
4  ✓ Aufräumen — Skin ist nur noch CSS, eine öffentliche Methode        29.07.

   Damit: alle Wächter leer, Paketgraph zyklenfrei, Build bewacht beides.

── offen ──────────────────────────────────────────────────────────────────────

5   Regelwerk und Architekturdokument nachziehen
    Design-Regeln.md: die Ordnung aus §1, der Verantwortungsrahmen aus §3.14 in
    Thorstens Formulierung, die Schlüssel-Regel aus §3.15, Dialog-Stufe 2a,
    keine Streams, der Karten-Absatz, der neu gefasste Skin-Vertrag.
    Die drei Wächter sind dort keine Vorsätze mehr, sondern bewachte Zusagen.
    Architektur-Dokumentation.md: der Abschnitt „UI-Architektur: Skin-System" ist
    vollständig überholt.
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

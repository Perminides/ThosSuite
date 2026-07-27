# Skin-Refactoring — Vorgehensplan

**Stand:** 28.07.2026 · v3 · Zielbild entschieden (U3), Vorarbeiten erledigt

**Charakter dieses Dokuments:** das Ergebnis der Vorbesprechung vom 26.–28.07. Es hält fest,
*was entschieden ist*, *was bewusst vertagt wurde*, *in welcher Reihenfolge* vorgegangen wird —
plus die geprüften Fakten, auf denen das beruht. Kein Regelwerk (das ist `Design-Regeln.md`, es
wird am Ende nachgezogen), keine Ist-Erfassung (das sind die vier Bestandsaufnahmen).

Die Zielbild-Entscheidung samt verworfener Alternative steht in `Skin-Zielbild-Entscheidung.md`.

---

## 1. Die Ordnung in `shared` — die eine Regel

`shared` hat vier Top-Level-Pakete. Ihre Reihenfolge ist das Einzige, was man sich merken muss.

```
oben    shared.ui       hier entsteht Oberfläche: Bausteine, Views, Dialoge, Alerts
        shared.skin     liefert Werte und CSS: A · B · die 7 Skins · SkinService · SkinImageCache
        shared.model    Records, Enums, + die drei Kontrakte
unten   shared          Config, DB, Log, AppClock, UiUtils
```

> **Regel 1** — Für die vier Top-Level-Pakete von `shared` gilt diese feste Ordnung.
> **Regel 2** — Jedes Paket greift nur nach unten in eigene Unterpakete oder in ein Top-Level-Paket,
> das in der Ordnung unter ihm steht. Sonst nirgendwohin. Nie seitwärts, nie nach oben.
> **Regel 3** — Von außen: `controller` darf auf jede Sprosse. **Features nie ins Skin-Paket.**
> `shared` greift nie hinaus.

„Nach unten" heißt: benutzen, was im Stapel darunter steht. Dieselbe Konvention wie eine Ebene
höher im Regelwerk (`controller` oben, `shared` unten).

**Warum die UI oben steht:** `shared.ui` ist der Ort, an dem Oberfläche gebaut wird — Bausteine,
Views, Dialoge gleichermaßen. Der Skin ist ihr Zulieferer: er hält die Werte, die CSS nicht
ausdrücken kann (Bounds, Fonts, Border, Pfade), und erzeugt das Stylesheet. Wer geliefert bekommt,
steht oben.

**Geschwister sind gratis geregelt:** `shared.ui.diary` darf nicht in `shared.ui.movie`. Folgt aus
„nie seitwärts".

### Zwei Wächter, beide Einzeiler

```bash
# 1. Der Skin kennt die UI nicht (die Ordnung)
grep -rn "app\.shared\.ui" src/main/java/app/shared/skin/

# 2. Kein Feature kennt den Skin (Regel 3)
grep -rn "app\.shared\.skin" src/main/java/app/{alc,diary,fitbit,learn,mattress,messaging,movie,weekday}/
```

Beide müssen leer sein. **Heute ist keiner leer** — Wächter 1 wegen der ~28 Bau-Methoden in `Skin`,
die `shared.ui`-Typen zurückgeben; Wächter 2 wegen sieben Feature-Paketen. Genau das ist die Arbeit.

`controller` ist von Wächter 2 ausgenommen: `MainWindow` braucht `styleScene` und die
Fenstergeometrie. Das ist Orchestrierung, kein Feature.

---

## 2. Zielbild

`Skin` als Klasse verschwindet. Was bleibt:

| | Inhalt |
|---|---|
| **A** `SkinProperties` (in `shared.skin`) | die ~170 Felder, `loadAllConfigs`, `parse*`, die Resolver mit Fallback — dazu eine **bewusst geschnittene öffentliche API** |
| **B** CSS (in `shared.skin`) | `styleScene` + 23 `addXxxStyles` + `CssBuilder` |
| die 7 Skin-Klassen | werden Unterklassen von **A** |
| `SkinService`, `SkinImageCache` | bleiben in `shared.skin` |
| **alles Bauen** | wandert nach `shared.ui` — die ~28 heutigen `create…`-Methoden, jeweils zu der Klasse, die sie braucht |

### A's öffentliche Fläche

Keine Feld-Getter, sondern zweckgeschnittene Records:

```java
public SessionLayout sessionLayout(String deck, String kategorie);   // fertig aufgelöst
public McMetrics     mcMetrics();
public CardStyle     cardStyle();
public DialogStyle   dialogStyle();
```

**Was drinnen bleibt:** die Feldnamen, die Reflection, und die Fallback-Kette (Deck → Kategorie →
Default) — damit sie nicht in fünfzehn UI-Klassen neu entsteht. Nach außen geht nur, was jemand
tatsächlich braucht, in der Form, in der er es braucht. Das ist Regel 3.5 („Werte dürfen raus, aber
nur als bewusst geschnittenes DTO") in groß; `MultipleChoicePane.Metrics` ist das erste Exemplar.

### Blätter bleiben passiv

`SuiteInfoLabel`, `MultipleChoicePane` & Co. *bekommen* Maße, sie *holen* sie nie — alles andere
hieße, dass jedes Blatt vom Skin weiß. Gelesen wird bei dem Komponierer, der das Blatt einsetzt:

```java
// in der Session-Pane, shared.ui.learn
SessionLayout layout = props.sessionLayout(deck, kategorie);
questionArea = new SuiteInfoLabel("", layout.question());
mcPane       = new MultipleChoicePane(layout.mc(), props.mcMetrics());
```

---

## 3. Entschieden

1. **U3 — die UI baut.** Siehe `Skin-Zielbild-Entscheidung.md`. Der Preis (öffentliche Wertfläche
   auf A) ist gesehen und akzeptiert; die Gegenleistung ist eine mit einem grep durchsetzbare Regel.
2. **Kein Fassaden-Zwischenschritt.** Direkt schneiden, Ordnung vorher festgelegt.
3. **Genau eine Klasse kümmert sich um die properties** — A hält die Felder *und* lädt sie. Kein
   separater Loader, der per Reflection in fremde Felder schreibt.
4. **Namenswissen verlässt `shared.skin` nie.** Niemand fragt nach `hannoverMapOverlayImageName`;
   die API liefert Records, keine Feldnamen. Gilt heute schon (§7), bleibt bewacht.
5. **Kein Feature greift je ins Skin-Paket** (Regel 3). Der zweite Wächter aus §1.
6. **Feature nennt nur Identität.** Ein Feature nennt Deck-Id und Kategorie. Alles Skin-Abhängige —
   Pfade, Bilder, Maße, Layouts — wird jenseits der Grenze aufgelöst. Erledigt in einem Satz: das
   Hintergrundbild in `GermanySessionPane`, die drei Skin-Importe in `MapService`, und die Frage,
   warum eine Feature-Klasse positionierte Widgets anfordert.
7. **Das einmalige Cache-Wärmen gehört in den `controller`.** Das Feature liefert die Deck-Ids mit
   Karte, der Controller reicht sie an die Skin-Seite. `MapService.imagePathsFor` und
   `MapImagePaths` in `learn.model` entfallen.
8. **`getContentSize()` wird eine echte Property** (Vorbereitung für einen HighRes-Skin) und wird von
   `MainWindow` direkt aus A gelesen. Unter U3 ist das unauffällig — der Controller darf.
9. **`Alerts` liegt in `shared.ui`**, nicht im Skin. Die gesamte Dialog-Maschinerie fasst nur zwei
   Skin-Werte an (§6a), und Features importieren damit nichts Skin-Artiges.
10. **javafx bleibt in `controller` erlaubt.** `MainWindow` (431 Z.) wird nicht angefasst.
11. **`shared.ui` wird gemischt geschnitten:** Feature-spezifisches nach `shared.ui.<feature>`,
    Generisches im Art-Schnitt. Discriminator: steht ein Feature-Name im Klassennamen oder im Zweck?
    Dashboard zählt als Suite-Chrome, nicht als Feature.

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
- **Warum vertagbar:** A wird mit den Feldern **genau wie heute** herausgezogen. Die Frage wird
  danach zu einer Änderung *innerhalb einer einzigen Klasse* — durch das Vertagen billiger, nicht
  teurer.
- **FailFast ist unabhängig billiger zu haben:** ein Startup-Check, der geladene Schlüssel gegen
  deklarierte Feldnamen hält (~15 Zeilen), findet `borderBackButton` ohne Migration.

### 4b. Wohin die Session-Panes wandern — weitgehend entschieden

`GermanySessionPane` & Co. rufen heute sechsmal Bau-Methoden mit Deck-Namen auf und bekommen
positionierte Widgets zurück. Das endet durch Entscheidung 3.6. Unter U3 ist auch das Ziel klar:
**`shared.ui.learn`.**

Offen bleibt nur der Zuschnitt — ob eine Klasse pro Session-Art (wie heute) oder anders, und wie
viel Kompositionswissen dabei im Feature verbleibt (`SessionPresenter` bleibt dort). Wird mit
Schritt 3c entschieden.

Der zugehörige Verantwortungsrahmen, der ins Regelwerk gehört:

| Ebene | entscheidet | Beispiel |
|---|---|---|
| **Feature** | *was* und *wann* | „Deck Deutschland, Frage 7, Antwort war falsch" |
| **View** (`shared.ui`) | *welche Bausteine*, *wie verdrahtet* | „diese Session hat Karte, Frage, Eingabefeld, MC, Zurück-Button" |
| **Skin** | *wie es aussieht, wo es sitzt* | Farben, Fonts, Bounds, welches Wallpaper zu welcher Karte |

Prüffrage für jede Zeile: **wovon hängt sie ab?** Skin-Wechsel → Skin. Fachlogik → Feature. Keins
von beidem, nur „soll anders aussehen" → View.

### 4c. Kleineres

- **Kollaps der sieben Skin-Stummelklassen** zu einer Klasse plus Tabelle. Möglich — sie sind aber
  ein Erweiterungspunkt für einen Skin, der in Java etwas berechnen will. Eigene Frage.
- **Re-Warming des Bildcaches nach Skinwechsel.** Heute lädt die erste Bildkarte nach einem Wechsel
  synchron. Bewusst akzeptiert. **Gehört im Code dokumentiert.**
- **`StartScreen`** (Screen + ScreenView in einer Klasse): löst sich per Regel — *ein inhaltsloser
  Screen (reines Chrome/Hintergrund) darf sein eigener `ScreenView` sein.*
- **Dialog-Stufe 2a fehlt im Regelwerk:** der parametrisierte Standarddialog (Primitive rein,
  Primitive oder `null` raus, kein Feature-seitiges Objekt). `TextPromptDialog`,
  `WhatsAppChatDialog`, `WhatsAppContactDialog` sehen nur deshalb wie Verstöße aus.
- **Unterpakete innerhalb von `shared.skin`** sind unter U3 kein Thema mehr — dort bleiben nur
  ~11 Klassen.

---

## 5. Bereits erledigt (27.07.2026)

Vorarbeit aus der Besprechung — alles gemessen, nicht vermutet:

- **`initOwner`-Bug behoben.** `dialog.initOwner(parent)` überschrieb bedingungslos den ermittelten
  `effectiveParent`; drei Dialoge verloren dadurch ihren Owner. Zeile gelöscht.
- **`createDialog(Window, String)` → `createDialog(String)`.** Der Parameter war zu 100 % redundant.
  Zehn Aufrufstellen angepasst.
- **Vier `styleScene`-Aufrufe entfernt** (2× `showAlert` inkl. `sceneProperty`-Listener,
  1× `createDialog`, 1× DatePicker-Popup). Alle nachweislich entbehrlich — siehe §6.
- **DatePicker-Block gelöscht.** Nicht nur überflüssig, sondern *schädlich*: eine einfarbige Corona
  um das Kalender-Popup, die ohne ihn verschwindet. Verwaiste Importe mit entfernt.

Offene Kleinigkeiten daraus: die Kommentare `// Owner intern` bei `AnkiConfigDialog` und
`RegionConfigDialog` sind jetzt überflüssig; der neue Kommentar bei `createDialog` könnte die
**Bindung** benennen statt „erbt".

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

**Folge:** `MainWindow:111` ist die einzige Stelle, die das CSS je setzt, und sie muss bleiben. Jede
weitere `styleScene`-Zeile in einem Dialog- oder Popup-Pfad ist überflüssig. Und `initOwner` ist für
jeden Dialog **Pflicht** — nicht wegen des Fensterverhaltens, sondern wegen der Optik.

Quelle: [`HeavyweightDialog.java`, openjdk/jfx](https://raw.githubusercontent.com/openjdk/jfx/master/modules/javafx.controls/src/main/java/javafx/scene/control/HeavyweightDialog.java)

### 6a. Wie skin-abhängig die Dialoge wirklich sind

Durchgezählt — der gesamte Dialog- und Alert-Apparat fasst **zwei** Skin-Werte an:

| Methode | Skin-Werte |
|---|---|
| `createDialogContent()` | **keine** — `new VBox(15)` + Style-Klasse |
| `installCloseBlocker`, `toButtonType` | keine |
| `createDialogHeaderBar` | **eine**: `font.getSize() * 0.3` fürs Titel-Padding |
| `buildAlertContent` | **eine**: `tintImageWithTextColor`, nur bei gesetztem Bild (ein Aufrufer suiteweit: `TurnDialog`) |
| `createDialog`, `showAlert` | keine eigenen |

Dazu `SkinService.getOwnerWindow()` — kein Skin-Wert, sondern das Hauptfenster, aus Bequemlichkeit
dort geparkt. Beim Umzug nach `shared.ui`: das Padding wandert ins generierte CSS, der Owner wird
vom `controller` gesetzt, die Tint-Farbe kommt aus A.

---

## 7. Ausgangslage (geprüft, 26.–28.07.2026)

| | |
|---|---|
| Features javafx-frei | **8 von 8** |
| javafx sonst | `controller` 4 Dateien; `shared` 34, davon 4 außerhalb `ui`/`skin` |
| `Skin` | gut 2600 Zeilen, 73 Methoden, 11 Verantwortlichkeiten, ~170 Felder |
| Skin-Aufrufe von außerhalb `shared` | ~90 Aufrufstellen, 38 davon `showAlert` |
| `shared.ui` → `shared.skin` | 14 Dateien (8 Dialoge, 4 ScreenViews, 2 Karten-Panes) — unter U3 **erlaubt** |
| `shared.skin` → `shared.ui` | die ~28 Bau-Methoden — unter U3 **zu beseitigen** |

**Befunde, die den Plan begründen:**

- **Alle sieben Skin-Subklassen sind feldfrei.** Keine deklariert ein Feld, keine setzt einen Wert;
  sie tragen einen Anzeigenamen und eine properties-Datei. Deshalb kann A alle Felder aufnehmen.
- **96 Felder hängen an `getFieldValue`:** 72 `Rectangle2D`, 16 `…WallpaperName`, 8 `…MapImageName`.
  Layout-Felder je Deck: `world` 9, `hannover` 8, `germany` 8, `mc` 6, je 3 für
  `us region oz it hs hr es en cs ch br be au`.
- **Layouts variieren wirklich pro Skin.** `worldSessionMapPanel`: `20,20` (basecolor/darkmode),
  `930,20` (erbende Skins), `540,20` (flatweb). Ein Skin bestimmt auch die Anordnung.
- **`hannover` hat 8 Felder, die in keiner properties-Datei vorkommen.** Sie werden in `styleScene`
  (`Skin.java:398–405`) aus den `world`-Feldern kopiert. Ein Kategorie-Fallback kann das nicht
  ersetzen: `HANNOVER_CARDS` hat Kategorie `ANKI_DECK`, genau wie `WORLD_CARDS`. Deck-auf-Deck.
- **`borderBackButton`** steht in `skin_basecolor.properties`, hat kein Feld, wird still ignoriert.
  Einzige systematische FailFast-Verletzung im Skin.
- **Namenswissen verlässt `shared.skin` heute schon nicht.**
- **`createIconButton` hat keinen Fallback** und keinen Null-Check. Kein Fehler — Icon-Buttons gibt
  es nur bei `germany`, `mc`, `world`. Aber nichts erzwingt das.

Nachprüfen:

```bash
grep -rn "import javafx" src/main/java/app/learn/
grep -rn "WallpaperName\|SessionPanel\|MapImageName" --include=*.java src/main/java/app | grep -v "/shared/skin/"
grep -rn "app\.shared\.ui" src/main/java/app/shared/skin/
```

---

## 8. Reihenfolge

```
1.  A herausziehen (SkinProperties)
    Basisklasse der sieben Skins. Felder unverändert, getFieldValue bleibt drinnen.
    Öffentliche API als Records anlegen — erst so viel, wie Schritt 2 braucht.
    contentSize wird Property. Re-Warming-Entscheidung dokumentieren.

2.  B herausziehen (CSS)
    24 Methoden, in sich geschlossen. Danach ist `Skin` nur noch C.

3.  C nach shared.ui auflösen — in vier Portionen:
    3a  Dialoge + Alerts       braucht fast nichts (§6a): Padding → CSS,
                               Owner vom controller, Tint aus A
    3b  Feature-Views          Diary, Movie, Dashboard, BarChart
    3c  Session-/Lern-Teile    + 4b: die Panes verlassen learn nach shared.ui.learn
    3d  Chrome/Menüs           zuletzt, weil MainWindow daran hängt

4.  Aufräumen
    `Skin` löschen, die 7 Skins auf A umhängen, beide Wächter-greps auf leer bringen.

5.  Regelwerk nachziehen
    Skin-Vertrag neu, Ordnung aus §1, Verantwortungsrahmen aus §4b, StartScreen-Regel,
    Dialog-Stufe 2a, die zwei Wächter als bewachte Zusagen.
```

A zuerst, weil `shared.ui` seine API braucht, sobald irgendetwas umzieht. B als Zweites, weil es
groß und in sich geschlossen ist und `Skin` danach nur noch aus Bau-Methoden besteht. 3a zuerst
innerhalb von C, weil dort am wenigsten dranhängt.

---

## 9. Verworfene Ideen

- **U1 — der Skin baut** (`shared.skin` über `shared.ui`). Die ernsthafte Alternative; verworfen,
  weil `shared.skin` dann ~25 Klassen samt Diary-Ansicht und WhatsApp-Dialog enthielte und die
  Regel „kein Feature ins Skin-Paket" nicht greifbar wäre. Vollständige Gegenüberstellung in
  `Skin-Zielbild-Entscheidung.md`.
- **U2 — Werte per Push in die UI schieben.** U3 mit zusätzlicher Schiebe-Mechanik.
- **Geteilter properties-Leser.** Es soll genau *eine* Klasse für die properties geben.
- **Umzug der Session-Layouts nach `shared.ui.learn`.** Die Layouts variieren pro Skin, sie gehören
  skin-seitig. Das Featurewissen steckt in den *Felddeklarationen*, nicht am Ort.
- **Namensmuster-Magie im Loader.** Entweder die Regel ist generell und in der Datei sichtbar, oder
  die Ladeseite bleibt unangetastet. Kein Dazwischen.
- **Split der properties-Dateien.** Eine Datei pro Design.
- **javafx komplett aus `controller`.** Machbar, bewusst nicht gemacht — §3.10.
- **Dünne `Skin`-Fassade als Zwischenschritt.**
- **Ein viertes Paket D für Bilder/Pfade.** Kein eigenständiger Schnitt.
- **Sechsstufige Sprossen-Leiter** und die **Fertigungsketten-Metapher** als Ordnungsregel. Beide
  korrekt, beide nicht merkbar bzw. nicht in der Paketstruktur gespiegelt. Ersetzt durch §1.

---

## 10. Korrekturen an früheren Annahmen

- **`loadAllConfigs` setzt nichts zusammen.** Es macht `props.getProperty(field.getName())` — eine
  Regel für alle Felder. Zusammengesetzt wird nur auf der *Leseseite*. Die Datei-Reihenfolge ist
  basecolor → subskin (der Subskin gewinnt).
- **Die Gottklasse ist ohne Loader-Änderung zerlegbar.** Die Punkt-Notation ist keine Voraussetzung.
- **`SkinImageCache` ist skin-spezifisch** (hält `cachedSkin`, invalidiert bei Skinwechsel). Bleibt
  in `shared.skin` — unter U3 dürfen die UI-Klassen ihn rufen.
- **Dialoge brauchen kein `styleScene`.** Popups auch nicht. Mechanismus ist die
  Owner-Stylesheet-Bindung, §6.
- **`createIconButton` ohne Fallback ist kein Bug**, nur ungesichert.
- **Der hannover-Fallback lässt sich nicht über die Kategorie ausdrücken.**
- **Die zwei `shared.ui → shared.skin`-„Verstöße"** (`ShapeMapPane`, `ImageMapPane`) waren nur unter
  U1 welche. Unter U3 sind sie legal — und `buildShapeMapWrapper` darf dorthin wandern, wo Dein TODO
  es haben will: in die `ShapeMapPane`.

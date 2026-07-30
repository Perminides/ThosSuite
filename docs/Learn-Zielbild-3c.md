# 3c — Zielbild für learn

**Stand:** 29.07.2026 · **umgesetzt** — Region und Anki sind umgezogen

Gehört zu `Skin-Refactoring-Plan.md` Schritt 3c. Zweck war: erst das Bild, dann die Fragen. Das
Dokument bleibt als Begründung stehen; was tatsächlich entstanden ist, steht in §0.

---

## 0. Was daraus geworden ist

*Nachtrag:* die Karten liegen inzwischen in `shared.ui.components.map`, und die beiden Übersetzer
`ShapeSessionMap`/`ImageSessionMap` sind entfallen — die Panes setzen `SessionMap` selbst um.

```
shared.ui
    RegionSessionView          Karte + Frage ODER Eingabefeld; getState/setState
    AnkiSessionView (abstrakt) die sechs gemeinsamen Bestandteile
      ShapeMapSessionView       Shape-Karte · Eingabe · leerer Hintergrund      (29 Z.)
      McSessionView            keine Karte · keine Eingabe · deck-eigener      (18 Z.)
      ImageMapSessionView      Bild-Karte  · Eingabe · leerer Hintergrund      (31 Z.)

shared.ui.components.map
    SessionMap                 gemeinsames Vokabular, spricht durchgehend Ids
      ShapeMapPane             arbeitet ohnehin mit Ids
      ImageMapPane             arbeitet mit Geometrien und übersetzt selbst
      NoSessionMap             tut nichts — damit entfallen alle Null-Wächter

shared.model
    ShapeMapState              aus ShapeMapPane herausgezogen, unverändert
    SessionCallbacks           die vier Rückmeldungen gebündelt

gelöscht
    learn.region.SessionPane · learn.anki.{Germany,MC,ImageMap}SessionPane
    learn.anki.model.SessionPane (Interface)
```

**Antworten auf die Fragen aus §5:**

- **A — eine Ansicht oder drei?** Eine abstrakte Basis plus drei Unterklassen. Der erste Versuch war
  *eine* Klasse mit `MapKind`-Enum und zwölf Konstruktor-Parametern; das war die Union aller
  Varianten und wurde zu Recht als hässlich verworfen. Mit Vererbung nimmt jede Unterklasse nur,
  was sie braucht.
- **B — wie sieht das Grenzpaket aus?** Kein Record. Die Ansicht bekommt Deck-Id, Kartenname,
  Kategorie, Geometrien (bzw. die Funktion `ids → Geometrien`) und die `SessionCallbacks` direkt im
  Konstruktor. Ein eigener Spec-Typ hätte nur einen Namen gebraucht, den `learn.region.model`
  bereits belegt.
- **C — bleibt der 18-Methoden-Kontrakt?** Ja, aber ohne Interface: er ist jetzt die öffentliche
  Fläche der Ansicht. Das Interface wurde überflüssig, weil es nur noch eine Anki-Ansicht gibt.
- **D — `ShapeMapState`: Fachlogik oder Anzeige?** Fachlogik. Es war bereits framework-frei (fünf
  Mengen von Ids, ein Schalter) und nur zufällig in einer UI-Klasse verschachtelt. Jetzt in
  `shared.model`, das Feature darf es halten.
- **E — Reihenfolge?** Region zuerst, wie vorgeschlagen. Hat getragen.

**Zwei Funde, die nur durch den Umzug sichtbar wurden:**

- `beginTx`/`endTx` waren tote No-ops — im Interface als Default deklariert, von keiner Pane
  implementiert, vom Presenter viermal gerufen.
- Der deck-eigene Hintergrund lag bei Germany statt bei MC. Der Fehler war seit unbekannt wann
  drin; er fiel auf, weil die drei Konfigurationen erstmals nebeneinander stehen.

**Erster Belastungstest bestanden:** zwei Änderungen (Hintergrund vertauschen, MC-Panel nicht
deaktivieren) — beide sofort gefunden, beide ein bis zwei Zeilen, beide an der Stelle, an der man
sie vermutet.

---

## 1. Was die vier Panes heute sind

| | Germany | MC | ImageMap | Region |
|---|---|---|---|---|
| Zeilen | 165 | 103 | 164 | 85 |
| `ComponentHost` | ✓ | ✓ | ✓ | ✓ |
| questionArea | ✓ | ✓ | ✓ | ✓ (optional) |
| progressArea | ✓ | ✓ | ✓ | — |
| cardHistoryArea | ✓ | ✓ | ✓ | — |
| imageComponent | ✓ | ✓ | ✓ | — |
| mcPane | ✓ | ✓ | ✓ | — |
| backButton | ✓ | ✓ | ✓ | — |
| inputField | ✓ | — | ✓ | ✓ (alternativ) |
| Karte | `ShapeMapPane` | — | `ImageMapPane` | `ShapeMapPane` |
| Hintergrund | deck-spezifisch | leer | leer | deck-spezifisch |

**Die drei Anki-Panes sind fast identisch.** Sie unterscheiden sich in genau drei Dingen: welche
Karte (Shape / keine / Image), ob es ein Eingabefeld gibt, und welcher Hintergrund. Alles andere —
sieben Komponenten, ihre Verdrahtung mit dem Presenter, die achtzehn Weiterreich-Methoden — ist
dreimal dasselbe.

**Region ist ein anderes Tier:** Karte plus *entweder* Frage *oder* Eingabefeld, kein MC, kein
Zurück-Button, kein Fortschritt. Und als Einziges mit Zustand (`getState`/`setState`).

---

## 2. Der eigentliche Knackpunkt

Nicht der Skin. **Die Panes greifen nach oben in `learn`:**

| Was sie holen | Woher | Wer |
|---|---|---|
| `Deck` (Id, MapName, Kategorie) | `learn.model` | alle vier |
| `MapService.getMap(deck).getShapeGeometries()` | `learn` | Germany, Region |
| `MapService.imagePathsFor(deck)` | `learn` | ImageMap |
| `GeoMap` | `learn.model` | ImageMap |

Sobald die Panes in `shared.ui` liegen, ist **jede** dieser Zeilen verboten — `shared` greift nie
nach oben. Das ist die eigentliche Arbeit von 3c, nicht das Verschieben.

Alles, was sie heute selbst holen, muss ihnen künftig **hereingereicht** werden. Der Presenter
bleibt in `learn`, kennt `Deck` und `MapService`, und übergibt beim Bauen ein framework-freies
Paket: Deck-Id, Kartenname, Kategorie, die Shape-Geometrien bzw. die vier Bildpfade.

Das ist dieselbe Grenzregel, die bei Diary und Movie schon gilt — dort war sie nur billiger, weil
`DiaryCardData` und `CardData` bereits existierten.

---

## 3. Zielbild — welche Dateien es danach gibt

```
learn.anki
    SessionPresenter          bleibt. Kennt Deck und MapService, baut das Grenzpaket,
                              treibt die Pane über den Kontrakt
    AnkiDeckSession           bleibt (der Screen)
    …Progress, …Service       bleiben, unberührt

learn.region
    SessionPresenter          bleibt
    RegionSession             bleibt (der Screen)

learn
    MapService                ohne die drei Skin-Importe; nur noch Shapes.
                              imagePathsFor und MapImagePaths entfallen

shared.model
    SessionSpec               NEU — das Grenzpaket: was eine Session ausmacht,
                              framework-frei (Ids, Geometrien bzw. Bildpfade)
    SessionLayout             NEU — die Maße einer Session, aus SkinProperties
    AnkiSessionView           der Kontrakt, aus learn.anki.model hierher

shared.ui
    AnkiSessionPane           die drei Anki-Panes — als eine Klasse oder als drei
    RegionSessionPane         eigenständig

shared.ui.components
    unverändert               ShapeMapPane, ImageMapPane, MultipleChoicePane,
                              SuiteImage, SuiteInfoLabel, SuiteTextField,
                              SuiteIconButton, ComponentHost

shared.skin
    Skin                      verliert die acht Bau-Methoden; danach nur noch CSS
                              plus die Hintergrundbilder
    SkinProperties            bekommt sessionLayout(...)
```

Danach ist **Wächter 1 leer** (`Skin` kennt `shared.ui` nicht mehr) und **Wächter 3 leer** (kein
Feature hält mehr eine Komponente). Wächter 2 bis auf `styleScene` aus dem Controller ebenfalls.

---

## 4. Entschieden (28.07.2026)

> **Die Panes lösen sich auf. Der Presenter treibt die Ansicht in `shared.ui` direkt.**

Kein Vermittler im Feature. Geprüft und verworfen wurde die Variante, eine Pane als Vermittler dort
zu belassen: sie hätte die achtzehn Methoden ein zweites Mal gehabt, ohne die Übersetzung
einzusparen — auch ein Vermittler im Feature muss die shared-Ansicht konstruieren und darf ihr
dabei kein `Deck` reichen.

### Die Regel dahinter — Thorstens Formulierung

> **„Stell eine Frage" sagt das Feature. „Zeige auf dem Fragepanel diesen Text" passiert in
> `shared.ui`.**

Das ist die Antwort auf die geparkte Frage 4b, und sie gehört ins Regelwerk. Der Kontrakt zwischen
Presenter und Ansicht **ist** diese Übersetzung — er ist keine Schicht davor, sondern die Grenze
selbst. Deshalb bleiben die achtzehn Methoden; sie liegen nur künftig genau einmal, auf der
richtigen Seite.

### Offen geblieben: das Grenz-Vokabular

Legt man die Regel an, fallen Namen durch — nicht nur in learn:

| heute | Problem |
|---|---|
| `disableMcPanel`, `setTextFieldActive`, `setTextInTextField`, `setProgressText`, `setCardHistoryText` | benennen ein Bedienelement statt einer Absicht (5 von 18 im learn-Kontrakt) |
| `MovieViewerScreenView.showCards(…)` | „zeig Karten" ist Anzeige-Vokabular; „zeig diese Filme" wäre Absicht |
| `CardData`, `DiaryCardData` | Grenztypen, benannt nach dem, was daraus wird, statt nach dem, was sie sind (die Domäne kennt `Entry`) |

Strukturell sind diese Typen richtig platziert („die Grenze trägt Daten, keine Domänentypen, keine
Nodes"). Es ist durchgehend eine Benennungsfrage — beim Verschieben mitziehen.

---

## 5. Die Entscheidungen, die das Bild aufwirft

### A · Werden aus den drei Anki-Sessions eine Ansicht?

*(Teilweise beantwortet: da keine Pane-Klassen mehr im Feature liegen, sind die drei Anki-Sessions
ohnehin keine Klassen mehr, sondern drei Beschreibungen. Offen bleibt, ob `shared.ui` eine Ansicht
hat, die alle drei baut, oder drei Ansichten.)*

Die Tabelle in §1 sagt: sie unterscheiden sich in drei Schaltern. Dagegen steht Dein Regelwerk
(„Lesbarkeit schlägt Wiederverwendbarkeit") und die Erfahrung mit dem verworfenen
Formular-Framework.

Der Unterschied zu damals: dort waren es fünf Dialoge mit *verschiedenen* Feldern, hier sind es
drei Sessions mit *denselben* sieben Komponenten. Aber die Entscheidung ist Deine.

### B · Wie sieht das Grenzpaket aus?

Ein Record `SessionSpec`, den der Presenter baut. Offen ist, wie viel hineingehört — nur Ids und
Geometrien, oder auch die Schalter aus A (hat MC? hat Eingabefeld? welche Kartenart?).

Je nach Antwort auf A ist das ein schlanker Datensatz oder eine kleine Konfiguration.

### C · Bleibt der 18-Methoden-Kontrakt?

**Ja** — er ist die Übersetzung von Absicht in Widget und damit die Grenze selbst (siehe §4).
Offen ist nur, ob er ein *Interface* braucht: baut `shared.ui` eine Ansicht für alle drei
Anki-Sessions, kann der Presenter die Klasse direkt halten. Bleiben es drei Ansichten, braucht es
das Interface. Hängt also an A.

Unabhängig davon: die fünf Widget-Namen ziehen beim Verschieben auf Absichts-Vokabular um.

### D · `ShapeMapState` in der Region-Fachlogik

`region.SessionPresenter` speichert den Kartenzustand in zwei eigenen Records (`SavedState`,
`WrongClickSnapshot`), um falsche Klicks rückgängig zu machen. `ShapeMapState` ist ein
verschachtelter Typ einer UI-Komponente.

Zwei Wege: entweder wird `ShapeMapState` ein framework-freier Typ in `shared.model` (billig, das
Feature darf ihn dann halten), oder der Undo-Mechanismus wandert mit in die Pane (sauberer, aber
das ist Fachlogik — „was passiert bei einem Fehlklick" gehört ins Feature).

**Das ist die einzige Stelle, an der 3c nicht bloß Verschieben ist.** Meine Frage an Dich: ist das
Speichern des Kartenzustands für Dich Fachlogik oder Anzeigezustand?

### E · Reihenfolge innerhalb von 3c

Vorschlag: **Region zuerst.** 85 Zeilen, eine Karte, kein MC — die kleinste vollständige Übung für
das Grenzpaket. Wenn das trägt, sind die Anki-Panes dieselbe Mechanik in größer.

---

## 5. Was in 3c *nicht* passiert

- **`buildShapeMapWrapper`** in die `ShapeMapPane` ziehen (Dein TODO). Eigener Schritt nach 3c —
  während eines Umzugs umzubauen mischt zwei Fehlerquellen.
- **Die `SuiteXXX`-Durchsicht.** Erst danach, wenn feststeht, was die
  Panes wirklich halten.
- **Der `!Sofort` in `ComponentHost`** („zwingt eine Klasse im Feature, sich ein javafx Image zu
  holen") erledigt sich von selbst, sobald die Panes in `shared.ui` liegen.

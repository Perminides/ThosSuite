# Skin-Zielbild — die Weichenstellung

**Stand:** 28.07.2026 · **entschieden: U3**

> **Beschluss (28.07.2026): U3 — die UI baut.**
>
> Begründung im Wortlaut: *„Kein Feature greift jemals ins skin-Paket ist eine sehr klare, leicht zu
> forcierende Regel. Da sind die public-APIs ein Schönheitsfehler, aber halt einer, der mir durch
> Javas wenig mächtiges Paketmanagement auferlegt wird. Bin ich bereit zu bezahlen."*
>
> Ausschlaggebend war nicht die Eleganz, sondern die Durchsetzbarkeit: die Regel ist ein Einzeiler.
> Der Preis — eine öffentliche Wertfläche auf A, wo heute keine ist — wurde gesehen und bewusst
> akzeptiert. Java kann `exports app.shared.skin to app.shared.ui;` nur mit JPMS, und ein
> Mehrmodul-Build wäre für diese Suite ein schlechter Tausch.

Gehört zu `Skin-Refactoring-Plan.md`. Das Ergebnis ist dort eingearbeitet; dieses Dokument bleibt als
Begründung des Beschlusses stehen — inklusive der unterlegenen Variante, damit sie nicht in einem
halben Jahr als frische Idee wiederkommt.

---

## Die Frage

> **Wer baut die Oberfläche — der Skin oder die UI-Schicht?**

Sie kam bei den Dialogen hoch, betrifft aber **alle drei Ebenen der Oberfläche gleichermaßen**:

- **Blätter** — `createSessionInfoLabel`, `createMultipleChoicePane`, `createIconButton`,
  `createInputField`, `createImageComponent`, `createDashboardTile` …
- **Komponierer** — die vier ScreenViews (`DiaryScreenView`, `MovieViewerScreenView`,
  `DashboardScreenView`, `BarChartScreenView`) und die Session-Panes
- **Dialoge** — `Alerts` und die acht bespoke Dialogklassen

Alle drei sind derselbe Fall: etwas in `shared.ui`, das skin-abhängige Werte braucht. Was das eine
löst, löst das andere — und was für Dialoge gilt, muss für Komponenten gelten. **Kein Sonderweg für
eine Ebene.**

Die Antwort legt die Reihenfolge der Top-Level-Pakete in `shared` fest, und die ist nach dem ersten
Schnitt teuer zu ändern.

---

## Was in beiden Szenarien gleich bleibt

Damit der Vergleich fair ist — das steht in beiden Fällen fest:

- **Features sind skin-agnostisch.** Sie nennen nur Identität (Deck-Id, Kategorie) und bekommen
  fertige Oberflächen und Dialoge. Sie rechnen nie mit Skin-Werten.
- **Namenswissen verlässt `shared.skin` nie.** Niemand fragt nach `hannoverMapOverlayImageName`.
- **`A` (`SkinProperties`) hält die Werte und lädt sie** — eine Klasse, Reflection beim Laden bleibt.
- **`B`** erzeugt das CSS (`styleScene` + die 23 `addXxxStyles`), gerufen von `MainWindow`.
- **Die sieben Skin-Klassen** werden Unterklassen von A.
- **Die Ordnung ist ein Stapel mit „nur nach unten"** — nur die Reihenfolge der Sprossen steht zur
  Debatte.
- **Dialog-Styling braucht keinen Skin-Aufruf** (JavaFX-Owner-Bindung, siehe Plan §6).
- **Blattkomponenten bleiben passiv.** `SuiteInfoLabel`, `MultipleChoicePane` & Co. *bekommen*
  Maße und Werte, sie *holen* sie nie. Alles andere hieße, dass jedes Blatt vom Skin weiß.
- **Die Session-Panes verlassen das Feature.** `GermanySessionPane` & Co. rufen heute sechsmal
  Bau-Methoden mit Deck-Namen auf und bekommen positionierte Widgets zurück. Das endet durch
  Entscheidung 3.9 des Plans („Feature nennt nur Identität") — **in beiden Szenarien**, unabhängig
  von dieser Frage. Unterschiedlich ist nur das Ziel: unter U1 nach `shared.skin`, unter U3 nach
  `shared.ui.learn`. Die geparkte Frage 4b wird also so oder so fällig.

---

## U1 — Der Skin baut

```
oben    shared.skin     A · B · C · Alerts · 8 Dialoge · 4 ScreenViews · 7 Skins
        shared.ui       passive Bausteine + Kontrakte
        shared.model
unten   shared
```

**Prinzip:** Alles, was Bauarbeit ist, liegt im Skin. `shared.ui` enthält nur, was gebaut *wird*.

**Paketinhalte**

| | |
|---|---|
| `shared.skin` (~25 Klassen) | A, B, C, die 7 Skins, `SkinService`, `SkinImageCache`, `Alerts`, die 8 Dialogklassen, die 4 ScreenViews |
| `shared.ui` (~13 Klassen) | `SuiteImage`, `SuiteInfoLabel`, `SuiteTextField`, `SuiteIconButton`, `SuiteSuggestionTextField`, `MultipleChoicePane`, `ShapeMapPane`, `ImageMapPane`, `DashboardTile`, `DiaryTagInputComponent`, `ComponentHost`, `SuiteTabCommitTextFieldTableCell` + Kontrakte |

**Was folgt**

- **A bleibt dicht.** Keine öffentliche Fläche außerhalb von `shared.skin`, keine Getter. Nur C und B
  lesen die Felder — im selben Paket, also ohne jede Zugriffsanpassung.
- Die Komponenten in `shared.ui` bleiben passiv: sie bekommen Maße und Werte im Konstruktor
  hereingereicht, so wie `MultipleChoicePane.Metrics` es heute schon tut.
- **Deine Regel „Fertige Komponente statt loser Teile" gilt wörtlich weiter**, auch shared-intern.
- Die 8 Dialogklassen und die 4 ScreenViews ziehen nach `shared.skin` um.

**Blätter unter U1**

C liest den Wert und positioniert das Blatt — genau wie heute:

```java
// in C, shared.skin
public SuiteInfoLabel createSessionInfoLabel(String deck, String cat, TextLabelType type) {
    Rectangle2D bounds = props.sessionLayout(deck, cat).of(type);
    SuiteInfoLabel label = new SuiteInfoLabel("");
    label.setLayoutX(bounds.getMinX());
    label.setFixedWidth(bounds.getWidth());
    return label;                                    // fertig positioniert zurück
}
```

`MultipleChoicePane.Metrics` — das Werte-Record, das heute in die MC-Komponente hineingereicht wird —
bleibt die **Ausnahme**: nur nötig, weil MC sich selbst vermessen muss. Der Regelfall ist, dass C
fertig positioniert zurückgibt. Dein TODO an der Stelle (*„streng genommen sickern damit
skin.properties in eine UI-Komponente"*) bleibt damit eine offene Unbequemlichkeit.

**Wächter (muss leer sein)**

```bash
grep -rn "SkinService\|app\.shared\.skin" src/main/java/app/shared/ui/
```

**Was mit den offenen TODOs passiert**

- `buildShapeMapWrapper` bleibt im Skin — der TODO wird beantwortet mit „so ist es richtig".
- `MovieViewerScreenView:66` („Wieso baut hier der Skin?") wird beantwortet mit „weil er das soll" —
  die Klasse zieht mit ins Skin-Paket.

**Der Preis**

`shared.skin` wird das große Paket (~25 Klassen) und heißt weiterhin „skin", obwohl darin
Diary-Ansichten und WhatsApp-Dialoge liegen. Wer den Diary-Editor sucht, sucht ihn im Skin-Paket.
Und C bleibt groß — es baut Menüs, Session-Panels, Dashboard-Kacheln, Movie-Karten und Dialoge.

---

## U3 — Die UI baut

```
oben    shared.ui       alles Bauen: Komponenten, Dialoge, Views, Alerts
        shared.skin     A · B · 7 Skins · SkinService · SkinImageCache
        shared.model
unten   shared
```

**Prinzip:** `shared.ui` ist der Ort, an dem Oberfläche entsteht. Der Skin liefert die Werte, die
CSS nicht ausdrücken kann — Bounds, Fonts, Border, Pfade — und das Stylesheet.

**Paketinhalte**

| | |
|---|---|
| `shared.skin` (~11 Klassen) | A (mit öffentlicher typisierter API), B, die 7 Skins, `SkinService`, `SkinImageCache` |
| `shared.ui` (~27 Klassen) | alle Bausteine, die 4 ScreenViews, die 8 Dialogklassen, `Alerts`, dazu die ~28 Bau-Methoden, die heute in C liegen — jeweils bei der Klasse, die sie braucht |

**Was folgt**

- **C löst sich auf.** `createSessionInfoLabel` wandert dorthin, wo das Label gebaut wird;
  `createDialog`/`createDialogContent` zu den Dialogen; `createDashboardTile` zur Kachel.
  Aus A/B/C werden im Wesentlichen A und B.
- **A bekommt eine öffentliche, typisierte API.** `props.sessionLayout(deck, kategorie)` liefert ein
  Record, `props.font()`, `props.borderSmall()`. Die Fallback-Kette bleibt in A, damit sie nicht in
  15 Klassen dupliziert wird.
- **Die Namensregel bleibt unberührt:** typisierte Methoden, kein `get("worldSessionMcPanel")`.
- Dialoge und Komponenten werden identisch behandelt — kein Sondermechanismus für Alerts.

**Blätter unter U3**

Der Komponierer liest den Wert und reicht ihn ins Blatt — das Blatt bleibt so passiv wie unter U1,
nur der lesende Code steht woanders:

```java
// in der Session-Pane, shared.ui.learn
SessionLayout layout = props.sessionLayout(deck, kategorie);   // einmal lesen
questionArea = new SuiteInfoLabel("", layout.question());       // Werte hineinreichen
mcPane       = new MultipleChoicePane(layout.mc(), props.mcMetrics());
```

`MultipleChoicePane.Metrics` wird damit vom Sonderfall zur **Blaupause**: jede Komponente bekommt
ihr Werte-Record. Dein TODO an der Stelle ist beantwortet — so ist es gedacht.

Die Fallback-Kette (Deck → Kategorie → Default) bleibt in A, damit sie nicht in fünfzehn Klassen
neu entsteht. Der Komponierer liest ein fertig aufgelöstes Record, keine Einzelfelder.

**Wächter (muss leer sein)**

```bash
grep -rn "app\.shared\.ui" src/main/java/app/shared/skin/
```

Spiegelbild von U1: dort darf die UI den Skin nicht kennen, hier der Skin die UI nicht.

**Was mit den offenen TODOs passiert**

Beide lösen sich auf — und beide sind von Dir selbst in diese Richtung geschrieben:

- `buildShapeMapWrapper`: *„An sich kann dieser ganze Block direkt in die ShapeMapPane. Einziges
  Problem halt, dass Du da keinen Zugriff auf die skin-Felder hast … Aber nen Zirkel würde es nicht
  erstellen."* — Unter U3 fällt genau dieses Hindernis weg.
- `MovieViewerScreenView:66`: *„Das muss doch raus, oder? Wieso baut hier der Skin?"* — Unter U3
  baut er nicht mehr.

**Der Preis**

- **Entscheidung 3.3 des Plans fällt.** A ist nicht mehr dicht, sondern hat eine öffentliche API.
  Du warst dabei skeptisch — hast aber selbst gesagt: *„über kurz oder lang brauchen wir Getter,
  das wird sich eh nicht verhindern lassen."*
- **„Fertige Komponente statt loser Teile" muss neu gefasst werden** — als Regel für die
  *Feature*-Grenze, nicht für shared-intern. Features bekommen weiterhin Fertiges; innerhalb von
  `shared.ui` wird mit Werten gearbeitet.
- Skin-Werte werden künftig in ~15 UI-Klassen gelesen statt in ~30 Methoden einer Klasse. Mehr
  Lesestellen, dafür jede typisiert und dort, wo sie gebraucht wird.

---

## Gegenüberstellung

| | **U1 — Skin baut** | **U3 — UI baut** |
|---|---|---|
| Ordnung | skin über ui | ui über skin |
| `shared.skin` | ~25 Klassen | ~11 Klassen |
| `shared.ui` | ~13 Klassen | ~27 Klassen |
| A nach außen | dicht, keine Getter | öffentliche typisierte API |
| C | bleibt groß | löst sich auf |
| „Fertige Komponente" | gilt wörtlich weiter | gilt an der Feature-Grenze |
| Dialoge = Komponenten? | ja (beide im Skin) | ja (beide in ui) |
| Wer positioniert ein Blatt? | C, im Skin | der Komponierer, in ui |
| `MultipleChoicePane.Metrics` | Ausnahme, TODO bleibt offen | Blaupause, TODO erledigt |
| Wächter-grep | ui darf skin nicht kennen | skin darf ui nicht kennen |
| Deine zwei TODOs | werden verneint | werden erfüllt |
| „Wo ist der Diary-Editor?" | im **Skin**-Paket | in `shared.ui.diary` |
| Aufwand ab hier | kleiner (Umzüge) | größer, aber = Schritt 4, den es ohnehin gibt |

---

## Woran es sich entscheidet

Nicht an Eleganz, sondern an drei konkreten Fragen:

**1. Stört es Dich, den Diary-Editor und den WhatsApp-Dialog im Paket `skin` zu suchen?**
Wenn ja → U3. Das ist der Auffindbarkeits-Maßstab aus Deinem Regelwerk, direkt angewandt.

**2. Wie fest steht „der Skin ist dumm und liefert Fertiges"?**
Ist das ein Grundsatz → U1. Ist es eine Regel für die Feature-Grenze, die shared-intern nie gemeint
war → U3.

**3. Wie unwohl ist Dir bei einer öffentlichen API auf A?**
Unter U3 lesen ~15 UI-Klassen typisierte Werte aus A. Das ist der eigentliche Preis. Wenn Dir das
zu weit geht, ist U1 die ehrliche Wahl — dann bleibt der Skin die Werkstatt und darf dafür groß sein.

**4. Der `Metrics`-Test — die Frage an einem realen Stück Code.**
In `createMultipleChoicePane` reichst Du heute ein Werte-Record (font, overhead, borderWidth,
lineSpacing) in die Komponente hinein, mit dem TODO daneben: *„streng genommen sickern damit
skin.properties in eine UI-Komponente. Nochmal prüfen, wie ok das ist."*

Lies die Stelle und frag Dich:

- „**So sollte es überall sein**" → U3. `Metrics` ist die Blaupause, jede Komponente bekommt ihr Record.
- „**Das ist die Ausnahme, die die Regel bestätigt**" → U1. Der Regelfall bleibt, dass der Skin fertig
  positioniert zurückgibt.

Diese Frage halte ich für die aussagekräftigste der vier, weil sie nicht an Prinzipien hängt,
sondern an Code, den Du selbst geschrieben und selbst kommentiert hast.

---

## Einschätzung

Ich neige zu **U3**, aus zwei Gründen:

Deine beiden TODOs an den fraglichen Stellen argumentieren beide dafür, und zwar unabhängig
voneinander und ohne dass diese Diskussion damals existierte. Das ist ein stärkeres Signal als jede
Ableitung aus Prinzipien.

Und U3 macht `shared.ui` zu dem, was der Name sagt. Unter U1 heißt das Paket, in dem die
Diary-Ansicht liegt, `skin` — das ist genau die Sorte Überraschung, gegen die Dein
Auffindbarkeits-Maßstab geschrieben ist.

Der Aufwandsunterschied ist kleiner, als er wirkt: U3s Mehrarbeit *ist* Schritt 4 („C zerlegen"),
den es in beiden Szenarien gibt — nur mit einem anderen Ziel.

**Gegen meine Neigung spricht** die öffentliche API auf A. Das ist eine echte Aufweichung, sie
betrifft die zentrale Klasse, und sie lässt sich später nicht mehr gut zurücknehmen. Wenn Dir das
zu teuer ist, ist U1 kein Kompromiss, sondern eine saubere Alternative — sie kostet nur, dass
`shared.skin` das große Paket wird und der Name dann nicht mehr ganz passt.

# Vertagte Punkte

**Stand:** 30.07.2026 · **Der Umbau ist fertig, Abschnitt A ist leer.** Offen ist nur noch Vorrat.

Alles, was während des Refactorings (26.–29.07.) bewusst **nicht** sofort gemacht wurde — und
alles, was dabei aufgefallen ist.

**Abschnitt A war die Arbeitsliste**: sieben kleine Code-Punkte (A1) und vier Sachfehler (A3) — drei
davon behoben, einer als nicht reproduzierbar verworfen. Alles durch. **B bis E sind Vorrat, nicht
Plan**; wer hier nach der nächsten Aufgabe sucht, sucht in einer Ideensammlung.

Was **erledigt** ist, steht nicht hier, sondern in `Skin-Refactoring-Plan.md` §5.

---

## A · Was noch offen ist

### A1 · ✓ Kleine Code-Punkte — alle erledigt

~~**1. `MainWindow` rund machen.**~~ **Erledigt 29.07.**

- *a) `MenuBar` über die shared-Grenze* — **bewusst so gelassen.** `MainWindow` ganz nach `shared.ui`
  zu ziehen scheiterte daran, dass es `PlayMenuItem`, `CardSortOrder` und `LearnSessionInfo` hält —
  das wäre Feature-Wissen im Fundament. Aufteilen wäre die saubere Variante, kostet bei 426 Zeilen
  und 17 Callbacks aber mehr, als es einbringt. `MainWindow` ist die einzige Klasse im `controller`
  mit echtem JavaFX (14 Importe); das ist als benannte Ausnahme in Regel 5 festgehalten.
- *b) `getStyleClass()` außerhalb `shared`* — `my-root` ist ersatzlos raus (Klasse, auskommentiertes
  CSS, die Methode `addMainWindowStyles`). Die zwei `my-spacer` bleiben und sind die Ausnahme in
  **ArchUnit-Regel 5**, die seit 29.07. prüft, dass Style-Klassen nur in der Anzeige-Schicht vergeben
  werden.
- *c) `my-title`* — Hauptfenster-Titel verliert die Klasse (es gab nie eine Regel), Dialog-Titel
  bekommt `.my-dialog-title` samt Padding-Regel. Damit entfällt der `font`-Parameter von
  `SuiteHeaderBar` **und** die `font` im `DialogStyle`. Der Grund für den Unterschied: im Dialog ist
  der Titel das einzige Element der Leiste und bestimmt deren Höhe, im Hauptfenster tut das die
  Menüleiste.

~~**2. `UiComponent` abschaffen.**~~ **Erledigt 29.07.** `ComponentHost` nimmt `Node...`, die fünf
Bausteine haben ihr `getView()` verloren, `EmptyLearnMap` erbt von `Group`, der Kontrakt ist
gelöscht. **Nicht ganz weg:** `LearnMap` deklariert weiterhin ein `Node getView()` — ein Interface
kann kein Node sein, die drei Umsetzungen sind welche, der Typ ist es nicht. Genau eine Aufrufstelle
in `AnkiLearnView` benutzt es.

~~**3. `SuiteDatePicker` entscheiden.**~~ **Erledigt 29.07.** Die Klasse bleibt und trägt weiterhin
die Festlegung „keine Kalenderwochen" — für alle vier Stellen. `DiaryEditor:174` war die einzige,
die noch direkt `new DatePicker(...)` baute und damit den JavaFX-Default (mit Kalenderwochen)
abgriff; der Unterschied war geerbt, nicht gewollt.

~~**4. `#QuestionLabel` durch eine Modifikator-Klasse ersetzen.**~~ **Erledigt 29.07.** Aus
`#QuestionLabel` wurde `.my-info-label.question`; der Modifikator kommt aus
`TextLabelType.styleClass()`, also aus derselben Aufzählung, die schon den Property-Baustein trägt.
Die Spezifität bleibt gleich (zwei Klassen schlagen eine), am Aussehen ändert sich nichts. Geprüft
vorab: die drei Ids wurden nirgends nachgeschlagen.

~~**5. Doppelte Innenhöhen in `DashboardTile`.**~~ **Erledigt 29.07.** Nachgemessen mit absurden
Werten (3000 / 25): das Bild ändert sich nicht, also gewinnt das CSS. Die beiden Konstanten waren
tot und sind raus — dieselbe Geschichte wie zuvor bei `TILE_WIDTH`/`TILE_HEIGHT`.

~~**6. Die vorhandenen Streams loswerden.**~~ **Erledigt 29.07.** `java.util.stream` kommt in
`src/main/java/app` nicht mehr vor; behalten wurde keine Pipeline. Die im Punkt genannten „36" waren zu
niedrig gezählt — `Arrays.stream(…)`, `Files.walk(…)`, `Files.list(…)` und `br.lines()` fielen durch
das Suchmuster. Zwei Funde nebenbei: `MultipleChoiceAnswers.toString()`
gab die Pipeline statt der Texte aus, und der Ordner-Lauf in `SuiteExporter` schloss seinen
`Files.walk`-Stream nie — beides ist mit repariert.

Offen bleibt bewusst: fünf `.forEach(…)` auf Sammlungen in `ShapeMapPane`. Das sind keine Streams,
sondern Lambdas auf einer `Map`; ob die zu Schleifen werden, ist eine eigene Entscheidung.

~~**7. `overlayContentBounds` — gehört das in den Skin?**~~ **Erledigt 30.07.** Aufgeteilt statt
verschoben: der Skin liefert nur noch den **Rand** (`imageMapOverlayContentInset(mapName)`), *wo* der
Inhalt sitzt rechnet die Karte selbst — sie hat das Overlay-Bild und damit dessen Maße.

Vorher nachgemessen, und das hat zwei Sachen geklärt. Erstens ist der Wert **nicht** aus dem Bild
ableitbar, wie zuerst vermutet: der Welt-Rahmen ist weichgezeichnet (~19.600 halbtransparente
Pixel), die streng opake Box wäre 10px zu schmal. Der Rand bleibt also ein Wert. Zweitens war das
alte hartcodierte `(11, 11, 410, 254)` auf die **432×276**-Overlays eingestellt (432 − 2·11 = 410) —
fünf der sieben Welt-Overlays sind aber 430×274, dort lag es zwei Pixel daneben. Relativ gerechnet
passt es zu jedem Bild; der Fehler ist nebenbei mit weg.

**Der Wert bleibt pro Karte gestaffelt** (`<mapName>ImageMapOverlayContentInset` schlägt den
allgemeinen), und zwar weil er am *Bild* hängt und nicht am Skin-Geschmack: die Welt-Overlays haben
einen weich auslaufenden Rahmen eingebacken, Hannovers Karte reicht bis an jede Kante. Ein global
gültiger Wert wurde zwischendurch probiert und wieder verworfen — mit Rand 11 auf einem rahmenlosen
Bild lag ein Minikarten-Klick am Rand gut 400 Kartenpixel daneben, bei 960px Fenster also fast eine
halbe Fensterbreite. Die Fehleinschätzung kam davon, die Abweichung an der Kartenbreite zu messen
(3 %) statt am sichtbaren Fenster.

Keine properties-Datei hat die alten Schlüssel je gesetzt, es gab also nichts zu migrieren.

**Am Asset nachgezogen:** `Hannover small.png` war 390×300 mit einem 15px breiten transparenten
Streifen rechts (daher hing die Minikarte optisch schief) und im Seitenverhältnis nicht maßstabsgleich
zur 14000×10500-Hauptkarte. Neu ist sie 400×300 ohne Rand — damit sind `scaleX` und `scaleY` beide
exakt 35.

---

### A2 · ✓ Regelwerk und Architekturdokument — erledigt 29.07.

`Design-Regeln.md` steht auf v2.0, `Architektur-Dokumentation.md` ist nachgezogen,
`Ordnerstruktur.txt` und `Paketabhängigkeiten.dot` sind neu erzeugt. Was dabei geändert wurde und
welche sechs Stellen schlicht falsch waren, steht in `Skin-Refactoring-Plan.md` §5.

Die Nachzieh-Notiz zu `UiComponent`/`my-title` ist erledigt: beides steht im Regelwerk nicht mehr.

---

### A3 · ✓ Sachfehler — erledigt 30.07.

~~**EM 2021 — „alle Länder grün, welches war falsch?"**~~ **Verworfen 30.07., nicht reproduzierbar.**
Fünfmal gespielt, nichts aufgefallen; auch nach Monaten täglichem Welt-Lernen nicht. Der Code wurde
trotzdem durchgerechnet, damit die Idee nicht als frische wiederkommt:

- Das Leeren des Layers in `markIncorrect` ist **gewollt**. Es kann nur zwei Dinge treffen, und beide
  sollen so sein: die *optionalen* Antworten verschwinden (die werden zwar akzeptiert, sollen aber
  nach einem Fehler nicht als Lösung gezeigt und mitgelernt werden), und eine `MarkMapElements`-
  Markierung der Frage verschwindet (die Frage weiß man nach einem Klick noch). Bei einer Karte mit
  nur `mandatory` ändert das Leeren optisch ohnehin nichts — die volle Lösung baut alles neu auf.
- Die vermutete **Z-Reihenfolge** ist kein Problem: ein Falsch-Klick liegt per Definition außerhalb
  von `mandatory ∪ optional`, aufgedeckt wird nur `mandatory`. Der Marker kann nie darunterliegen.
- Das vermutete **veraltete `lastClick`** ebenfalls nicht: klickbar sind nur `mandatory ∪ optional`,
  ein Treffer darauf ist immer richtig. Ein falscher Klick geht zwangsläufig zum Viewport — und der
  setzt `lastClick`. Deshalb ist auch der Parameter `id` dort immer `null`.

Beide letzten Punkte hängen an genau diesen Invarianten; wer die Klickziele einmal weiter fasst,
holt sich die Fehler ein.

~~**Fragefeld der Region-Session: CENTER.**~~ **Erledigt 30.07.** Die Ausrichtung hängt am Schnitt
des Feldes: das Anki-Fragefeld ist ein Textblock (500×330, mehrzeilige CSV-Frage), das der
Region-Session ein einzeiliger Streifen (500×50) mit einem einzelnen Namen darin — 70px Text in
500px Feld. Deshalb `SuiteInfoLabel.centerText()`, gerufen nur von `RegionLearnView`; Anki bleibt
linksbündig, dort fransen zentrierte Zeilenenden aus.

Wirksam ist die Textausrichtung im `TextFlow`, **nicht** `setAlignment` am StackPane: `setFixedWidth`
gibt dem `TextFlow` die volle Feldbreite, waagerecht gibt es also nichts zu verschieben. Bewusst
nicht über den Skin gelöst — die Klasse `.question` tragen beide Felder, und die Ausrichtung ändert
sich mit dem Skin nicht.

~~**Freies Spiel: falsch geklickte Formen verschwinden bei „schwer" nicht.**~~ **Erledigt 30.07.**
Zwei Ursachen, beide in `region.SessionPresenter`.

`moveCorrectToActive` schaltete nur `CORRECT → ACTIVE`; die verpasste Region wird aber in
`undoWrongClick(COMMIT_MISS_AND_CONTINUE)` dauerhaft auf `INCORRECT` gesetzt und aus `sessionRegions`
entfernt — sie blieb den Rest der Session rot stehen und verriet, wo sie liegt. Die Methode heißt
jetzt `moveResolvedToActive` und nimmt `INCORRECT` mit.

Das allein reichte nicht: `undoWrongClick` spielte beim Weiterklicken den Schnappschuss von *vor*
dem Falsch-Klick ein — mitsamt dem Grün früherer Treffer — und trug das neue Rot obendrauf. Nach
zwei Fehlern in Folge stand die halbe Karte bunt da, und `moveResolvedToActive` kam nie zum Zug,
weil es nur am *richtigen* Klick hängt. Bei „schwer" gibt es nichts wiederherzustellen, die Karte
wird jetzt schlicht geleert; das rot/grün-Paar ist damit nur bis zum Weiterklicken zu sehen.
„Leicht" bleibt in beiden Punkten unberührt.

**Dabei aufgefallen und mit erledigt (30.07.): erledigte Kreise nahmen bei „leicht" weiter Klicks
an.** `ClickSessionProgress.elementClicked` ignoriert jetzt Ids, die nicht mehr in `sessionRegions`
stehen — dieselbe Menge, die an `weWaitForClick` geht. Damit gilt: *was nicht mehr aktiv gesetzt
wird, wird auch nicht mehr gewertet.* Bewusst **nur bei „leicht"**: bei „schwer" sind alle Kreise
unsichtbar und damit alle potenziell falsch, dort ist der Fehlgriff auf etwas längst Erledigtes ein
echter Fehler und macht den Modus aus. Die Pause-Abfrage steht vor dem Filter, ein Klick auf einen
markierten Kreis heißt also weiterhin „weiter". Kein `mouseTransparent` — die Karte kennt die Regeln
nicht, das entscheidet das Feature.

Zwei Randnotizen dazu: `lastClickedId` stand vor der Pause-Abfrage und wurde deshalb vom
*Weiter*-Klick überschrieben, bevor `endPause()` daraus die Meldung „Statt X wurde Y geklickt"
baute — sie nannte also die falsche Region. Durch das Umstellen stimmt sie. Und der Fall „leicht +
Lernsession" ist ungetestet, weil dafür gerade kein Deck fällig war.

~~**`MovieViewerScreen.refresh()` baut nicht vollständig neu.**~~ **Erledigt 30.07.** Entschieden:
voller Rebuild, laufende Suche überlebt. `MovieViewerScreenView.rebuild()` merkt sich die drei
Feldinhalte und die zuletzt gezeigten Kacheln, baut neu und schreibt beides zurück — ohne erneute
Abfrage. Dafür hat `SuiteSuggestionTextField` ein `setTextSilent`/`getText` bekommen, den stillen
Gegenspieler zu `setTextAndTrigger`. `reapplyBackground()` ist entfallen.

---

## B · Klein, jederzeit machbar

- **FailFast-Startup-Check** für properties-Schlüssel, die kein Feld beanspruchen (~15 Zeilen).
  Findet `borderBackButton` in `skin_basecolor.properties`, das seit unbekannt wann ins Leere läuft
  — die einzige systematische FailFast-Verletzung im Skin.
- **Re-Warming des Bildcaches nach Skinwechsel dokumentieren.** Heute lädt die erste Bildkarte nach
  einem Wechsel synchron; das ist bewusst akzeptiert, steht aber nirgends im Code.
- **Zwei Schlüssel für dasselbe Deck.** Wallpaper werden über die Deck-**Id** aufgelöst
  (`lk_bbWallpaperName`), Layouts über den **mapName** (`lkSession…Panel`). Beim Aufräumen der
  Properties vereinheitlichen.
- **`getContentSize`:** der Loader kennt keinen `Dimension2D`-Zweig, ein `contentSize=…` in einer
  properties-Datei würde still ignoriert. Steht als Kommentar in `SkinProperties`; Parser erweitern,
  falls je gebraucht.

---

## C · Eigene Runden

### Punkt-Notation in den properties

`learnSessionPanel.world.map=…` → `Map<String, LearnSessionPanel>`. Regel: *ein Punkt geht eine Ebene
tiefer; ob eine Ebene offen oder deklariert ist, sagt der Typ.* Gedeckelt auf zwei Ebenen.

**Dafür:** 96 Felddeklarationen weg; ein neues Deck kostet keine Java-Änderung mehr (heute 3 Felder
pro Länder-Deck); hannover schrumpft von 16 Zeilen auf 1; FailFast fällt ab.
**Dagegen:** ~30 Zeilen Typ-Ablaufen im Loader; Migration aller sieben properties-Dateien, einmalig
und unumkehrbar.
**Warum vertagbar:** wird durch das Warten *billiger*, nicht teurer — nach dem Umbau ist es eine
Änderung innerhalb einer einzigen Klasse.

### Dialog-Vertrag vereinheitlichen

- `WhatsAppChatDialog` wertet einen rohen `javafx.ButtonType` aus, während `Alerts` ein `ButtonEnum`
  liefert. Entweder das Ergebnis beim Klick mitschreiben, oder `SuiteDialog` eine Variante geben, die
  wie `showAlert` ein `ButtonEnum` zurückgibt. → `WhatsAppChatDialog:95`
- Einige Dialoge geben noch `Optional` statt record/`null` zurück (widerspricht Regel 5 des
  Regelwerks).

### `ImageBatchProcessor` ist nicht nur UI

Die Klasse enthält neben dem Auswahl-Dialog auch Datei-Auflistung (`Files.list`), Bildskalierung
(AWT/Scalr) und Schreiben/Verschieben. → `ImageBatchProcessor:33`

### Das Tinting im Alert

Wird einem Alert ein Bild mitgegeben, wird es mit der **Textfarbe** eingefärbt. Funktioniert nur für
einfarbige Icons; ein Foto käme monochrom heraus. Unintuitiv und im Grunde ein Hack. Bisher gibt es
genau einen Aufrufer (`mattress.TurnDialog`) — überdenken, sobald ein zweiter dazukommt.
→ `Alerts:137`

### Kollaps der sieben Skin-Stummelklassen

Keine deklariert ein Feld, keine setzt einen Wert — sie tragen nur einen Anzeigenamen und eine
properties-Datei. Ließe sich zu einer Klasse plus Tabelle zusammenziehen. **Dagegen:** sie sind der
Erweiterungspunkt für einen Skin, der irgendwann in Java etwas berechnen oder eine
`addXxxStyles`-Methode überschreiben will.

---

## D · Gestaltung — bewusst erst nach dem Umbau

Diese Punkte sind Design-Entscheidungen, keine Struktur. Sie werden leichter, sobald die
CSS-Erzeugung eine eigene, überschaubare Klasse ist und man sie einzeln anfassen kann.

| Punkt | Fundstelle |
|---|---|
| `borderColor` global vs. in `borderParams` — eins von beidem muss raus | `SkinProperties:41` |
| Moderneres Design ausprobieren: runde Ecken, kein Border, box-shadow | `SkinProperties:42` |
| Einfacher als box-shadow: transparente Hintergründe ohne Border | `SkinProperties:43` |
| Padding von ScrollPane + ButtonBar ergibt einen unschönen Abstand; darunter ein „fieser Hack" | `Skin:338`, `Skin:372` |
| Default für `menuBarBackground` fehlt | `Skin:794` |
| Hartes `50px`-Padding gehört in die properties | `Skin:883` |
| Magic Numbers | `Skin:1028` |
| Highlight-Background ignoriert Border-Radius — JavaFX clippt Children nicht an runden Ecken | `Skin:1029` |
| Drei `// TODO: anpassen` an Farb-/Padding-/Font-Werten | `Skin:1192–1194` |
| Thumbnail (vergrößerte Version im Popup) sollte eine eigene Komponente werden | `Skin:1785` |
| `-fx-background-insets`-Fix analog `ImageMapPane` fehlt | `SuiteImage:52` |
| **DarkMode ist sauhässlich** | — |

---

## E · Beobachtungen ohne Entscheidung

Aufgefallen, aber nichts beschlossen — hier steht bewusst kein Auftrag.

- **`createIconButton` hat keinen Fallback** und keinen Null-Check. Kein Fehler: Icon-Buttons gibt es
  nur bei `germany`, `mc`, `world`, und dort sind die Felder definiert. Aber nichts erzwingt das.
- **Die rohen `new Alert(...)` in `ThosSuiteApp` und `shared.DB` sind ungestylt**, weil sie keinen
  Owner setzen (siehe Plan §6). Beim Start ist das teils unvermeidbar — die Hauptscene existiert noch
  nicht. Ob man die späteren Fälle nachziehen will, ist offen.
- **`DiaryTagInputComponent` generischer machen** und von Movie und WhatsApp mitnutzen lassen?
  → `DiaryTagInputComponent:23`
- **`SuiteSuggestionTextField` wird nur von Movie genutzt** — anderswo wiederverwendbar?
  → `SuiteSuggestionTextField:27`
- **Guard in `ImageMapPane`** 1:1 aus der alten GeoMap-Fassung übernommen, greift real fast nie.
  → `ImageMapPane:260`
- **JDK-Workaround** für JDK-8350149 / JDK-8362873 (HBox-Höhenberechnung) — kann weg, sobald gefixt.
  → `Skin:2029`
- **Menu-Styles:** nicht überprüft, ob alle nötig sind. → `Skin:816`
- **Preload ohne Fälligkeitsfilter:** `AnkiDeckService` wärmt *alle* Anki-Decks mit Karte vor, der
  Kommentar daneben spricht von „Decks die heute fällig sind". Ausdrücklich als unwichtig eingestuft.
- **Der MovieScreen wird komplett anders aufgebaut als die übrigen.** → `SuiteSuggestionTextField:30`
- **Alc- und Fitbit-StatisticScreens sind voll mit UI-Kram.** → `Skin` (Klassen-Javadoc)
- **Die Vier-Sprossen-Ordnung** aus Plan §1.1 als `layeredArchitecture()` in den ArchUnit-Test
  aufnehmen. Heute prüfen die fünf Regeln die drei Wächter, die Zyklenfreiheit und die Style-Klassen, nicht die
  Reihenfolge der Sprossen.

---

## Nicht vertagt, sondern verworfen

Damit sie nicht als frische Idee wiederkommen — die vollständige Liste steht in
`Skin-Refactoring-Plan.md` §9 und `Skin-Zielbild-Entscheidung.md`. Die wichtigsten:

- **U1** (der Skin baut) — die ernsthafte Alternative zum gewählten Zielbild.
- **javafx komplett aus `controller`** — machbar, bewusst nicht gemacht.
- **`shared.ui.<feature>`-Zweige** — ersetzt durch Oberfläche vs. Baustein.
- **Eine fünfte Sprosse `shared.components`** — löste ein Scheinproblem.
- **Split der properties-Dateien** — eine Datei pro Design.

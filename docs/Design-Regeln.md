# ThosSuite — Design & Regeln (Paketstruktur, Abhängigkeiten, Benennung)

**Stand:** 17.07.2026 · v1.4

**Charakter dieses Dokuments:** das *Regelwerk* — die Prinzipien, nach denen die Suite
in Pakete und Klassen geschnitten, benannt und verbunden ist, und *warum*. Vorschreibend:
Hier schlägt man nach, **bevor** man etwas Neues baut, damit es in die Struktur passt. Was es
konkret gibt (der Klassenbaum, der Ist-Zustand), steht im Architektur-Dokument.

## 1. Grundlage: Maßstab und feste Regeln

**Maßstab — Auffindbarkeit.**

Das wichtigste Ziel ist, nach Monaten Pause den Ort einer Sache
*vorhersagen* zu können, ohne zu suchen. *Nicht* das Ziel: Pattern-Konformität, Testbarkeit,
Eleganz um ihrer selbst willen.

**Grundhaltung — FailFast, keine Tests.**

FailFast: Tritt etwas Unerwartetes auf, fliegt sofort eine Exception und der Prozess
endet (zentraler Handler in `ThosSuiteApp`, Alert mit Stacktrace). Fehler werden
**nicht** weggefangen, nicht still geloggt, nicht durch Ersatzwerte überbrückt.
**Kein defensives try-catch „für alle Fälle".**

Das verbietet *nicht* jeden try-catch — nur den, der einen Fehler schluckt und normal
weiterlaufen lässt. Legitim bleibt zweierlei: try-with-resources (schließt nur, fängt
nichts) und ein catch, das ausschließlich aufräumt oder zurückrollt und die Exception
danach **weiterwirft** (etwa `rollback()` beim Import). Sobald ein catch den Ablauf
fortsetzt, als wäre nichts gewesen, ist es falsch — der Fehler soll fliegen.

Keine Tests! Keine Unit-Tests, keine Test-Infrastruktur,
Testbarkeit ist kein Designziel. Einziger Nutzer und Entwickler ist Thorsten; ein Fehler
fällt im täglichen Gebrauch sofort auf und wird direkt behoben.

**Feste Regeln:**

1. **Keine Zirkel zwischen Paketen, auch keine transitiven.** Gilt ausnahmslos für alle Pakete.
2. **Auf oberster Ebene laufen die Abhängigkeiten nur nach unten** (Orchestrierung → Fundament).
   Im Inneren eines Pakets gilt die Abwärtsrichtung ebenfalls — mit einer Ausnahme: Hat ein
   Feature Zweige, greifen diese nach oben auf ihren gemeinsamen Kern (siehe „Wann ein Feature
   sich aufteilt"). Regel 1 bleibt davon in jedem Fall unberührt.
3. **Kein Paket auf oberster Ebene greift seitwärts in ein anderes.** Sie hängen nebeneinander,
   nicht ineinander. Was zwei davon brauchen, wandert nach unten in `shared`.
4. **Nicht jede Abweichung von einer Regel ist ein Fehler.** Erst fragen, ob ein guter Grund
   dahintersteckt.
5. **`null` statt `Optional` bei Rückgaben.** Fehlt ein Rückgabewert, wird `null` zurückgegeben, nicht `Optional`. Ausnahme: Der Wert stammt direkt aus einer `Optional`-liefernden JDK-API (Streams) — dann wird das `Optional` sofort am Entstehungsort ausgepackt (`orElse(null)`), nicht durch eigene Signaturen weitergereicht. `null`-Rückgaben gehören im Javadoc vermerkt.

<!-- TODO: Regel 4 braucht noch ein gutes Beispiel aus der Domäne (Schnitt/Benennung/Abhängigkeit),
     keins aus dem Prozess. Bleibt offen, bis eine echte Stutzer-Stelle auftaucht. -->

## 2. Die Pakete

### Die Sorten von Paketen

Vier Sorten. Übersicht — die genauen Regeln je Sorte folgen in den nächsten Abschnitten.

- **Orchestrierung (`controller`)** — startet die Suite und hält den Querschnitt übers Ganze
  (Dashboard). Darf als Einzige nach unten in alle Features greifen.
- **Features** (`alc`, `diary`, `fitbit`, `learn`, `mattress`, `messaging`, `movie`, `weekday`)
  — je ein abgeschlossenes, eingestöpseltes Feature. Manche teilen sich innen in Zweige
  (`messaging`, `learn`; siehe „Wann ein Feature sich aufteilt").
- **Fundament** — trägt nur das Gerüst, kennt kein einzelnes Feature und wird von mehreren
  Paketen darüber gebraucht (DB, Config, Log, Screen, Skin, generische Bausteine). Kennt
  niemanden über sich.
- **`scripts`** — einmalige Standalone-Klassen (Migrationen, Fixes, Prototypen, manuelle Tests),
  komplett vom Produktivcode abtrennbar. Wird nicht mitgebaut und landet nicht im Build-Ergebnis.

### Richtungen

```
┌─────────────────────────────────────────────┐
│  ORCHESTRIERUNG          controller          │
└─────────────────────────────────────────────┘
                    │  greift nach unten
                    ▼
┌─────────────────────────────────────────────┐
│  FEATURES                                    │
│  alc · diary · fitbit · learn · mattress ·   │
│  messaging · movie · weekday                 │
│            (nebeneinander, kein Zugriff      │
│             aufeinander)                     │
└─────────────────────────────────────────────┘
                    │  greift nach unten
                    ▼
┌─────────────────────────────────────────────┐
│  FUNDAMENT               shared              │
└─────────────────────────────────────────────┘
```

Drei Ebenen. Gleiche Ebene = nebeneinander, kein Zugriff aufeinander (Regel 3). Tiefere Ebene =
Zugriff nach unten erlaubt (Regel 2). `shared` kennt niemanden über sich.

### Innenschnitt eines Feature-Pakets

Die meisten Features sind innen nach diesem Muster aufgebaut — ein Standard, keine Pflicht:

```
feature
├── (Wurzel)    die aktiven Klassen
├── repository  Zugriff auf die Suite-Datenbank
└── model       freistehende Datenklassen
```

- `repository` immer dann, wenn es Suite-Datenbank-Zugriff gibt — eigenes Unterpaket, auch bei
  nur einer Klasse.
- `model` nur, wenn es freistehende Datenklassen gibt. Datenklassen, die fremde JSON-/API-Daten
  abbilden, kommen in ein eigenes `model.json`.
- Wo es passt, darf es weitere Unterpakete geben (etwa die Aufteilung in Zweige, eigener
  Abschnitt). Das Muster ist der Ausgangspunkt, nicht die Grenze.

**Auch innen läuft die Richtung nach unten:** Die Wurzel nutzt `repository` und `model`, das
`repository` nutzt `model`. Ein `model` zeigt nie zurück — nie in eine Wurzel, nie in ein
`repository`. Eine Datenklasse weiß nichts davon, wer sie lädt oder benutzt.

### Wann ein Feature sich aufteilt

Ein Feature darf sich in Unterpakete teilen, wenn es in mehreren **Zweigen** vorkommt, die je
ihre eigene Mechanik haben. Was die Zweige gemeinsam haben, bleibt im Kern (Wurzel, `repository`,
`model`); was ein Zweig exklusiv hat, wandert in sein Unterpaket. Wie viel gemeinsam im Kern
liegt, ist von Feature zu Feature verschieden:

- `messaging` teilt wenig — `signal` und `whatsapp` teilen sich fast nur das Datenbank-Schema,
  alles andere ist zweig-exklusiv.
- `learn` teilt viel — Maps, Shapes, Sessions und Fortschritt liegen gemeinsam im Kern; `anki`
  und `region` halten nur ihr Exklusives.

**Richtung:** Zweige greifen nach oben auf den geteilten Kern zu. Der Kern greift nie zurück nach
unten in einen Zweig, und die Zweige kennen einander nicht — so bleibt es zirkelfrei (Regel 1).
Das ist die einzige Stelle, an der die Abwärtsrichtung im Inneren bewusst nach oben aufgemacht
wird.

## 3. Die Klassen

### So lokal wie möglich

Eine Klasse wohnt so weit oben wie möglich — bei dem Feature, das sie nutzt. Nach unten ins
Fundament (`shared`) fällt sie nur, wenn sie sich oben nicht halten lässt. Zwei Gründe führen
dahin:

- **Sonst gäbe es einen Zirkel.** Beispiel `Screen`: Der Controller erzeugt die Oberflächen der
  Features, und jede Oberfläche erfüllt den `Screen`-Kontrakt. Läge `Screen` im `controller`,
  müsste das Feature nach oben greifen, um ihn zu erfüllen — `controller → feature` und zurück,
  ein Zirkel. Also liegt `Screen` unter beiden, in `shared`.
- **Mehrere Features brauchen es.** Kein Feature darf in ein anderes greifen (Regel 3). Was zwei
  Features gemeinsam brauchen, kann in keinem von beiden wohnen und sitzt unter beiden, in
  `shared` — wo jedes es erreicht.

Die Bremse beim Hochrutschen ist die Feature-Freiheit: Was Wissen über ein einzelnes Feature
trägt, bleibt im Feature und fällt nicht nach `shared` — auch dann nicht, wenn der Controller es
von oben mitnutzt (der Controller greift ohnehin von oben herab und zwingt nichts nach unten).

**Bewusste Ausnahme:** ein echt generisches Werkzeug, das heute nur *ein* Feature nutzt, aber
plausibel auch andere wollen werden. Das legen wir gleich in `shared`, damit das nächste Feature
es dort findet, statt es neu zu bauen. Maßstab ist nicht „könnte man allgemein machen" (das ginge
bei fast allem), sondern „ist es von Natur aus ein Allgemeinwerkzeug". Im Zweifel: Würde ein
anderes Feature das plausibel auch wollen?

### Framework-frei oder framework-gebunden

„So lokal wie möglich" beantwortet, *wie weit oben* eine Klasse wohnt. Diese zweite
Achse beantwortet eine unabhängige Frage: *auf welcher Seite der JavaFX-Grenze* sie
liegt. Beide zusammen sind das Platzierungswerkzeug — erst die Seite, dann die Höhe.

**Das Kriterium.** Was JavaFX (oder CSS) anfasst, ist framework-**gebunden**. Was ohne
JavaFX kompiliert — reine Logik, Daten, Geometrie, Schlüssel —, ist framework-**frei**.
Der Schnitt läuft mitten durch ein Feature: seine Daten- und Ablauflogik ist frei, seine
sichtbaren Bausteine sind gebunden.

**Die Adresse.** Framework-gebundenes eines Features wohnt nicht im Feature, sondern
unter `shared.ui.components.<feature>` — also `shared.ui.components.learn` für die
Karten-Panes, den `MapNodeBuilder`, die `ShapeGeometry`. Framework-freies bleibt oben im
Feature (`learn.model`, `learn.repository`, `learn`). So sammelt sich das JavaFX-Gebundene
an einem vorhersagbaren Ort statt im ganzen Feature verstreut, und das Feature selbst wird
Stück für Stück frei.

**Warum Features javafx-frei.** Wartbarkeit bedeutet hier: Der Code bleibt ohne UI-Framework-Kenntnisse lesbar und änderbar. Deshalb sind sämtliche Feature-Pakete vollständig framework-frei; JavaFX ist bewusst in `shared` und `controller` eingezäunt. Der JavaFX-Anteil ist so groß, wie er sein muss — Ziel ist nicht, ihn zu verkleinern, sondern die Grenze scharf zu halten. Auch innerhalb von `shared` gilt Lesbarkeit als Kriterium: vermeidbare Undurchsichtigkeit (unchecked Casts, funktionale Verrenkungen ohne Not) wird minimiert.

**Die Übergangsregel: Die Grenze trägt Daten — keine Domänentypen, keine Nodes.** Was von
der Feature-Seite nach `shared` hinabreicht, ist framework-freie *Datenvokabel*, nicht der
Domänentyp des Features und kein fertiger JavaFX-Node. Beleg: `ShapeGeometry` (in
`shared.ui.components.learn.model`) trägt id und Geometrie über die Grenze; der learn-eigene
`MapShape` mit seiner Fachlichkeit (Namen, Hauptstadt, Matching) bleibt in `learn.model`
und wird *nie* hinabgereicht. Der sichtbare Node entsteht erst jenseits der Grenze, im
`MapNodeBuilder`. So bleibt `shared` frei von Feature-Wissen (Regel 3 bleibt gewahrt) und
das Feature frei von JavaFX. Wo eine Grenze mehrfach überquert wird (rein zum Anzeigen, raus zum Speichern), trägt idealerweise *ein* Grenzobjekt beide Richtungen, statt je Richtung ein eigenes — ein Typ pro Grenze bleibt vorhersagbar.

Diese Achse ist der Grund, warum ein großer zusammenhängender Klumpen im
Abhängigkeitsgraphen entsteht, sobald JavaFX *nach unten* in `model`/`repository` eines
Features greift — und warum er sich auflöst, sobald der Node-Bau jenseits der Grenze sitzt
und nur noch framework-freie Daten hinabreichen.

**Die harte Fassung, binär.** „Feature ist framework-frei" ist kein Richtwert, sondern
ein Test: `grep -rn "import javafx" src/main/java/app/<feature>/` (Git Bash) muss
**leer** sein. JavaFX-Importe leben ausschließlich in `shared` oder in der obersten
Schicht (`controller`) — jedes Feature-Paket ist 100 % frei, oder das Refactoring gilt
als nicht fertig. Kein „97 %". Und keine geschummelten Scheinlösungen, die den grep
bestehen, aber JavaFX-*Wissen* ins Feature tragen (Scene-Graph-Navigation, ein
JavaFX-Objekt opak durchreichen).

### Pfad-Wissen: Struktur gehört der Suite, Dateien dem Feature

Die Ordner-Struktur der Suite ist Suite-Wissen und liegt in `Config` — als computed Pfade,
feature-benannte Ordner eingeschlossen (`fitbitFolder`, `learnImageFolder`,
`diaryAttachmentsFolder`). Dass ein Ordner nur einem Feature dient, macht ihn nicht zu
Feature-Wissen; die *Hierarchie* kennt die Suite. Zwei scharfe Kanten halten die Regel davon ab,
mit der Zeit zum Dateinamen-Sammelbecken zu verrotten:

1. **Ordner ja, Dateiname nie.** `Config` liefert Ordner-Knoten. Sobald ein Segment eine
   konkrete Datei benennt (`.log`, `.json`, `skin_moonlight.properties`, ein Bild- oder
   Icon-Name), hört `Config` auf — den letzten Schritt resolved die Aufrufstelle im Feature.
   `Config` bleibt so die Landkarte, nicht das Verzeichnis der Dateien. (Die beiden DB-Dateien
   sind keine Ausnahme: sie gehen per Parameter an `DB.init` und stehen nie auf der
   `getPath`-Fläche.)
2. **Nur eigener Boden.** „Struktur der Suite" ist der Baum, den die Suite besitzt — Hauptordner
   plus `attachments.folder`. Ein Ordner unter fremdem Wurzelpfad ist die Struktur *dieser
   fremden App*: `Config` hält nur den fremden Wurzelpfad (`signal.externalFolder`), die
   Unterstruktur (`attachments.noindex`) bleibt im Feature.

Folge für jede Aufrufstelle: den Ordner von `Config` holen, den laufzeit-variablen oder
feature-lokalen Namen darunter selbst resolven.

### Versteckt oder freistehend

Diese Frage betrifft fast nur Datenklassen. Sie entscheidet nicht *wo* eine Klasse wohnt
(das tut die Platzierung oben), sondern *ob* sie überhaupt als eigene, sichtbare Klasse
existiert oder im Erzeuger versteckt bleibt. Maßstab ist die Sichtbarkeit:

- Wird der Typ nur in *einer* Klasse gebraucht, bleibt er als innere (private) Klasse dort
  versteckt. Nicht herausziehen, nur um herauszuziehen.
- Muss er von außerhalb seiner Klasse erreichbar sein, wird er freistehend und kommt ins
  `model`-Paket. Erst dann stellt sich die Platzierungsfrage.

### Klassenbenennung

Klassen werden **ohne** Domänenpräfix benannt. Der Präfix (der Name des Feature-Pakets, bei
Zweigen der des Zweigs) kommt erst dazu, wenn die Klasse zum ersten Mal von **außerhalb** ihres
Feature-Pakets importiert wird — fast immer aus der Orchestrierung.

- Nur intern genutzt → kein Präfix: `diary.Repository`, `alc.Repository`, `mattress.TurnDialog`.
- Erster Import von außen → umbenennen mit Präfix: `fitbit.ActivityTableDialog`,
  `fitbit.FitbitStatisticsScreen`.
- **Ausnahme:** Namen, die genauso heißen würden wie ihr Paket oder zu allgemein wären (`Dialog`,
  `Screen`), behalten auch intern einen eigenen Namen — sonst ist `new Dialog()` nicht von
  JavaFX' eigenem `Dialog` zu unterscheiden.

**Am Namen ablesbar:** kurzer Name = nur intern benutzt; langer Name mit Präfix = von außen
importiert. Man sieht es der Klasse an, ohne in den Code zu schauen.

### Namensrolle: Source vs. Repository

Am Suffix ist die Datenquelle ablesbar:

- **`…Repository`** — Zugriff im Repository-Muster. Ohne Zusatz: die Suite-DB
  (`TmdbMovieRepository`, `diary.Repository`).
- **`…Source`** — die Daten stammen aus einer Datei (`ConfigFileSource`, `CsvDeckCardSource`).

### Bildschirm-Kontrakt: `Screen`, `ScreenView`, `UiComponent`

Eine Oberfläche wird über drei Rollen gebaut, jede ein eigenes Interface. Der Grund
für die Dreiteilung ist die JavaFX-Grenze: die Lebenszyklus-Logik bleibt framework-frei
im Feature, das Sichtbare liegt framework-gebunden in `shared`.

- **`Screen` (Rolle 1, in `shared`, im Feature implementiert, framework-frei).** Das
  Controller-zugewandte Interface: refresh, esc, save, sortOrderChanged,
  `getSwitchStrategy` … Es hat Zugriff auf ein `ScreenView` und reicht es über
  `getView() : ScreenView` weiter. Die feature-seitige `…Screen`/`…Session`-Klasse
  nennt nie einen JavaFX-Typ.
- **`ScreenView` (Rolle 2, in `shared`).** Der mountbare Anzeige-Lieferant:
  `getPane() : Pane`. `getPane` ruft ausschließlich MainWindow. Heißt bewusst
  `ScreenView`, nicht `View` — `View` ist in JavaFX zu häufig.
- **`UiComponent` (Rolle 3, in `shared`).** Die Bausteine im Host: `getNode() : Node`.

**Lesekette:** `Screen → ScreenView → Pane`. MainWindow mountet über
`screen.getView().getPane()` — hier, und nur hier, überquert der Node die Grenze.

**Warum das Feature frei bleibt:** Die framework-freie Grenze ist der *Typ, den das
Feature hält* (`ScreenView`, ein shared-Interface), nicht ein Rückgabetyp-Trick. Die
Feature-Klasse hält ein `ScreenView`-Objekt und reicht es weiter; sie ruft nie
`getPane()` und nennt keinen JavaFX-Typ.

**refresh = stabiler View + In-place-Rebuild.** Der gemountete `ScreenView` bleibt
dasselbe Objekt; refresh tauscht nur seine Kinder. Kein Controller-Reshow, kein
Swap-Container.

**Zwei Bauformen:** (1) eine dedizierte shared-Komponente, die sich selbst baut und
`ScreenView` implementiert (fitbit: `FitbitScreenView`); (2) ein generischer
`ComponentHost` (`ScreenView`), der vom Feature gestylte, absolut positionierte
Komponenten in eine Null-Layout-Pane entgegennimmt (learn). Discriminator: *wer
positioniert* — eine Layout-Pane (VBox/HBox) selbst → sie ist der View, kein Host;
der Skin absolut → `ComponentHost`.

**Benennung:** Nicht-Lern-Oberflächen `…Screen`, Lern-Oberflächen `…Session`
(`AnkiDeckSession`, `RegionSession`).

**Inhalt der Interfaces:** leere Defaults erlaubt — eine Oberfläche muss nicht auf
alles reagieren (ein `AlcStatisticsScreen` tut bei `sort()` nichts, kein Fehler).

### Dialoge

Dialoge folgen derselben Grenze wie Screens, nur ohne Lebenszyklus: framework-freier
Input rein → shared-JavaFX-Dialog → framework-freies Ergebnis raus. Sie sind **keine**
`ScreenView`s (werden nicht gemountet, haben kein `getPane`).

**Vertrag (ausnahmslos).** Ergebnis ist ein record/enum oder `null` bei komplexeren Dialogen, nie 
`ButtonType`/`Node`. (Frühere Dialoge geben teils noch `Optional` zurück; das ist nicht
 mehr erwünscht (Regel 5) und wird nach und nach entfernt.) Das Fenster holt sich der
 Dialog intern über `SkinService.getOwnerWindow()` — kein `Window`-Parameter vom Feature.
 Dismiss/X = immer Abbruch (`CANCEL`); der Aufrufer interpretiert (etwa `CANCEL → später`).

**Zwei Stufen.**
- **Einfach (Auswahl + statischer Inhalt):** ein Alert. `SkinService.get().showAlert(…)` gibt einen suite-eigenen `DialogButton` zurück; nur der Skin kennt `javafx.ButtonType` und übersetzt. Zusatzoptionen (Bild als Path, zentrierter Text, ESC-/X-Blockade via `Dismiss`) reicht ein `AlertOptions`-Objekt hinein — Bild lädt und tint der Skin, nicht das Feature.
- **Komplex (Felder, Mehrfachauswahl, Verflechtung):** eine **bespoke Komponente pro
  Dialog**. Kein generisches Formular-Framework — einmal gebaut und wieder verworfen,
  weil es bei der tatsächlichen Stückzahl (kein Dialog-Typ kommt auf fünf) nur
  Indirektion ohne Wiederverwendung war. Lesbarkeit schlägt Wiederverwendbarkeit.
- **Sonderfall: zustandsbehafteter Editor.** Ein reicher, über die Zeit veränderlicher Editor (Diary: Text, Tags, Anhänge, invasiver Modus) ist kein Ergebnis-Dialog. Er wird wie ein Screen-Split behandelt: framework-freie Hälfte im Feature (die feature-seitige Klasse mit Domäne — Speichern, Löschen, Regeln), gebundene Hälfte in `shared` (die Widgets). Statt eines Ergebnis-Records reicht **ein einziges** framework-freies Grenzobjekt in beide Richtungen, und die shared-Hälfte meldet über Callbacks zurück (`onSave`, `onDelete`). Fachregeln, die das Verhalten steuern (Schwellen, Timer-Dauer), kommen als framework-freie Werte hinein; ihre JavaFX-Umsetzung liegt in `shared`.

**Aufteilung einer komplexen Dialog-Klasse.** Das JavaFX (Widgets, `Dialog`,
`showAndWait`) wandert vollständig nach `shared`; im Feature bleibt nur Domäne: welche
Felder/Optionen (framework-freie Daten), das Ergebnis-Record und das Mapping.
Verbindung über einen konkreten, lesbaren Zustandstyp — nicht über abstrakte
Deskriptoren/ids.
- **Der Label ist die id.** Anzeigenamen sind eindeutig; das Feature liest die Auswahl
  per Label zurück und mappt Label → Domänentyp. Kein separates id-Feld. (Wird die
  Anzeige gekürzt, hält die Checkbox den Volltext in `userData`.)
- **Reducer nur bei Verflechtung.** Ändert ein Klick den Zustand *anderer* Elemente
  (Region: Modi verschwinden, Regionen grauen aus), beschreibt eine reine Funktion
  `Zustand → Zustand` nach jedem Klick den kompletten Sollzustand neu. Hat ein Dialog
  keine Verflechtung (Anki: unabhängige Felder), braucht er keinen Reducer — das einzige
  reaktive Stück („OK aktiv?") lebt dann direkt in der Komponente.

*(Platzierung der Screen-Views und Dialog-Komponenten unter `shared` — momentan teils
`shared.ui.dialog`/flaches `shared.ui.components` statt `shared.ui.components.<feature>`
— wird nach Abschluss der Dialog-Migration aufgeräumt.)*

### `Skin`-Vertrag (vorläufig — wird beim Skin-Refactoring neu gefasst)

> Dieser Abschnitt ist **nicht in Stein gemeißelt.** Das Skin-System wird ohnehin überarbeitet
> und danach gehört dieser Vertrag neu geschrieben.

Der Skin (`shared.skin`) ist **dumm**: Er kennt keine Domänenklassen.

- **Die Domäne reicht hinab, der Skin reicht Fertiges zurück.** Das Feature übergibt dem Skin
  schlichte Daten (ein DTO oder String-Schlüssel); der Skin wendet Geometrie, Größen und CSS
  intern an und gibt fertige `Region`/`Node`-Bausteine zurück. Er reicht keine Skin-Werte (Maße,
  Fonts, Borders) nach oben.
- **Fertige Komponente statt loser Teile.** Eine `create…`-Methode liefert eine echte gekapselte
  Komponente (wie `MultipleChoicePane`), keine lose `VBox`/`HBox`, in der Feature-Logik offen
  herumliegt. Das Feature *setzt* die Bausteine nur zusammen.
- **Fachliche Panes halten ihren Zustand selbst.** Der Skin liefert Layout/Wrapper; die
  fachliche Pane (Shape-/Image-Map) hält Shapes und Zustand. Misst sich eine Komponente selbst,
  bekommt sie die skin-abhängigen Messwerte als gebündeltes DTO hereingereicht, statt den Skin
  direkt zu rufen.
- **Einbahn.** Die generischen Bausteine in `shared` rufen den Skin nicht zurück; `shared.skin`
  hängt nur an `shared`.
- **Fabrik/Daten ist keine Komponente.** Nicht alles in `shared.ui.components` ist ein
  platzierbarer Baustein. Eine **Komponente** ist eine echte, gekapselte UI-Einheit, die
  in den Szenengraphen gehängt wird und ihren Zustand hält (`MultipleChoicePane`,
  `ShapeMapPane`). Eine **Fabrik** (`MapNodeBuilder`) *baut* Nodes, ist aber selbst keiner;
  ein **Datentyp** (`ShapeGeometry`) *beschreibt*, woraus gebaut wird, und hängt nirgends.
  Am Namen ablesbar: eine Komponente heißt `…Pane`, eine Fabrik `…Builder`, ein Datentyp
  trägt seinen Sachnamen. Die Unterscheidung ist *was die Klasse ist*, nicht *wo sie wohnt* —
  darum steht sie neben „Versteckt oder freistehend", nicht bei der Platzierung. (Die
  Zielstruktur für Fabriken/Daten unterhalb von `ui.components` ist noch offen; bis dahin
  liegen sie dort, ohne selbst Komponenten zu sein.)
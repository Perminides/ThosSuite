# ThosSuite — Design & Regeln (Paketstruktur, Abhängigkeiten, Benennung)

**Stand:** 29.07.2026 · v2.0 — nach dem Skin-Refactoring neu gefasst

**Charakter dieses Dokuments:** das *Regelwerk* — die Prinzipien, nach denen die Suite
in Pakete und Klassen geschnitten, benannt und verbunden ist, und *warum*. Vorschreibend:
Hier schlägt man nach, **bevor** man etwas Neues baut, damit es in die Struktur passt. Was es
konkret gibt (der Klassenbaum, der Ist-Zustand), steht im Architektur-Dokument.

## 0. Inhalt
1. Grundlage: Maßstab und feste Regeln
2. Die Pakete
3. Die Klassen
4. Die bewachten Zusagen

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
fortsetzt, als wäre nichts gewesen, ist es falsch — der Fehler soll fliegen. Oder in
einem Alert bzw. PopUp angezeigt werden (Apis nicht erreichbar etc.)

Keine Tests — mit **einer** Ausnahme (ArchUnit s. u.). Keine Unit-Tests, keine Test-Infrastruktur für
Fachlogik, Testbarkeit ist kein Designziel. Einziger Nutzer und Entwickler ist Perminides; ein Fehler
fällt im täglichen Gebrauch sofort auf und wird direkt behoben.

**Die Ausnahme sind die Architekturregeln** (`src/test/java/app/ArchitekturRegelnTest.java`,
ArchUnit). Sie prüfen keine Fachlogik, sondern die Struktur: wer wen kennen darf, und dass der
Paketgraph zyklenfrei ist. Der Grund, warum es sonst keine Tests gibt — „ein Fehler fällt im
Gebrauch auf" —, trägt hier nämlich **nicht**: ein Strukturverstoß fällt nicht auf.

**Feste Regeln:**

1. **Keine Zirkel zwischen Paketen, auch keine transitiven.** Gilt ausnahmslos für alle Pakete.
2. **Auf oberster Ebene laufen die Abhängigkeiten nur nach unten** (Orchestrierung → Fundament). Siehe Abschnitt "2. Die Pakete".
   Im Inneren eines Pakets gilt die Abwärtsrichtung ebenfalls — mit einer Ausnahme: Hat ein
   Feature Zweige, greifen diese nach oben auf ihren gemeinsamen Kern (siehe „Wann ein Feature
   sich aufteilt"). Regel 1 bleibt davon in jedem Fall unberührt.
3. **Kein Paket auf oberster Ebene greift seitwärts in ein anderes.** Siehe Abschnitt "2. Die Pakete".
4. **`null` statt `Optional` bei Rückgaben.** Fehlt ein Rückgabewert, wird `null` zurückgegeben, nicht `Optional`. Ausnahme: Der Wert stammt direkt aus einer `Optional`-liefernden JDK-API (Streams) — dann wird das `Optional` sofort am Entstehungsort ausgepackt (`orElse(null)`), nicht durch eigene Signaturen weitergereicht. `null`-Rückgaben gehören im Javadoc vermerkt.
5. **Keine Streams, außer sie sind unbedingt nötig.** Eine Schleife liest sich nach Monaten
   ohne Anlauf und ist leichte zu debuggen, eine Kette aus `filter`/`map`/`collect` nicht.
6. **Null-Layout:** keine LayoutManager, feste Positionen (Desktop-App mit fester Auflösung;
  präzise Kontrolle wichtiger als Flexibilität). Die Rechtecke stehen im Skin, gesetzt werden sie
  von der Oberfläche in `shared.ui` — der Skin fasst keine Komponente an.
7. **Kommentare und Javadoc beschreiben, was ist — nie, was war.** Kein „heißt nicht mehr",
  „tut nicht mehr", „lag früher woanders", „ist jetzt umgekehrt". Wer eine Klasse aufschlägt,
  will wissen, was sie tut; der Weg dahin beantwortet ihm keine Frage und verwirrt nach Monaten
  nur noch. Was war, steht in der Git-Historie. Das gilt auch für Marker: ein Marker beschreibt
  ein offenes Problem, nie eine erledigte Änderung.
8. **Inline Kommentare**  halten sich kurz. Ausnahmen wo absolut gerechtfertigt, aber an sich gehen
  diese nicht über eine Zeiel hinaus, das stört sonst den Lesefluss.

  *Abgrenzung:* Eine **verworfene Alternative mit Begründung** ist kein Rückblick, sondern eine
  Eigenschaft der heutigen Lösung — sie verhindert, dass jemand denselben Weg noch einmal geht
  („Button-Text togglen scheiterte an den festen ButtonBar-Breiten"). Erlaubt, solange sie sagt
  *warum es so ist*, und nicht *dass es mal anders war*.

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

- `repository` wenn es Suite-Datenbank-Zugriff gibt — eigenes Unterpaket, auch bei
  nur einer Klasse.
- `model` wenn es freistehende Datenklassen gibt. Datenklassen, die fremde JSON-/API-Daten
  abbilden, kommen in ein eigenes `model.json`.
- Wo es passt, darf es weitere Unterpakete geben (etwa die Aufteilung in Zweige, eigener
  Abschnitt).

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
- `learn` teilt viel im model-Paket und einiges im Kern; `anki` und `region` halten ihre Exklusives.

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

**Die Adresse.** Framework-gebundenes eines Features wohnt nicht im Feature, sondern in
`shared.ui` — als Oberfläche in der Wurzel, als Baustein in `shared.ui.components`. Nach welcher
Frage sich das entscheidet, steht im Abschnitt „Die Anzeige-Schicht". Framework-freies bleibt oben
im Feature (`learn.model`, `learn.repository`, `learn`). So sammelt sich das JavaFX-Gebundene an
einem vorhersagbaren Ort statt im ganzen Feature verstreut.

*Es gibt bewusst **kein** `shared.ui.components.<feature>`.* Zu welchem Feature ein Baustein gehört,
steht in seinem Klassennamen (`DiaryCard`, `MovieCard`, `DashboardTile`), nicht im Paketnamen. Bei
gut zwei Dutzend Bausteinen ist das kein Suchproblem, und es erspart die Frage, wohin etwas gehört,
das zwei Features nutzen.

**Warum Features javafx-frei.** Wartbarkeit bedeutet hier: Der Code bleibt ohne UI-Framework-Kenntnisse lesbar und änderbar. Deshalb sind sämtliche Feature-Pakete vollständig framework-frei; JavaFX ist bewusst in `shared` und `controller` eingezäunt. Der JavaFX-Anteil ist so groß, wie er sein muss — Ziel ist nicht, ihn zu verkleinern, sondern die Grenze scharf zu halten.

**Die Übergangsregel: Die Grenze trägt Daten — keine Domänentypen, keine Nodes.** Was von
der Feature-Seite nach `shared` hinabreicht, ist framework-freie *Datenvokabel*, nicht der
Domänentyp des Features und kein fertiger JavaFX-Node. Beleg: `ShapeGeometry` (in `shared.model`)
trägt id und Geometrie über die Grenze; der learn-eigene
`MapShape` mit seiner Fachlichkeit (Namen, Hauptstadt, Matching) bleibt in `learn.model`
und wird *nie* hinabgereicht. Der sichtbare Node entsteht erst jenseits der Grenze, im
`MapNodeBuilder`. So bleibt `shared` frei von Feature-Wissen (Regel 3 bleibt gewahrt) und
das Feature frei von JavaFX. Wo eine Grenze mehrfach überquert wird (rein zum Anzeigen, raus zum Speichern), trägt idealerweise *ein* Grenzobjekt beide Richtungen, statt je Richtung ein eigenes — ein Typ pro Grenze bleibt vorhersagbar.

**Die harte Fassung, binär.** „Feature ist framework-frei" ist kein Richtwert. Jedes Feature-Paket
ist 100 % frei. Keine geschummelten Scheinlösungen, die den grep bestehen, aber JavaFX-*Wissen* ins
Feature tragen (Scene-Graph-Navigation, ein JavaFX-Objekt opak durchreichen).

### Pfad-Wissen: Struktur gehört der Suite, Dateien dem Feature

Die Ordner-Struktur der Suite ist Suite-Wissen und liegt in `Config` — als computed Pfade,
feature-benannte Ordner eingeschlossen (`fitbitFolder`, `learnImageFolder`).
Dass ein Ordner nur einem Feature dient, macht ihn nicht zu
Feature-Wissen; die *Hierarchie* kennt die Suite. Zwei scharfe Kanten halten die Regel davon ab,
mit der Zeit zum Dateinamen-Sammelbecken zu verrotten:

1. **Ordner ja, Dateiname nie.** `Config` liefert Ordner-Knoten. Sobald ein Segment eine
   konkrete Datei benennt (`.log`, `.json`, `skin_moonlight.properties`, ein Bild- oder
   Icon-Name), hört `Config` auf — den letzten Schritt resolved die Aufrufstelle im Feature.
   `Config` bleibt so die Landkarte, nicht das Verzeichnis der Dateien.
2. **Nur eigener Boden.** „Struktur der Suite" ist der Baum, den die Suite besitzt — Hauptordner
   plus `attachments.folder`. Ein Ordner unter fremdem Wurzelpfad ist die Struktur *dieser
   fremden App*: `Config` hält nur den fremden Wurzelpfad (`signal.externalPath`), die
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

- Nur intern genutzt → kein Präfix: `diary.repository.Repository`, `fitbit.repository.Repository`.
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

Setzt eine Klasse mehrere Quellen zusammen, heißt sie als Tür nach außen `…Repository`; die genaue
Herkunft steht in den Namen dahinter (`DeckRepository` über `CsvDeckCardSource` + `DbDeckProgressRepository`).

### Bildschirm-Kontrakt: `Screen` und `ScreenView`

Eine Oberfläche wird über zwei Rollen gebaut, jede ein eigenes Interface. Der Grund
für die Zweiteilung ist die JavaFX-Grenze: die Lebenszyklus-Logik bleibt framework-frei
im Feature, das Sichtbare liegt framework-gebunden in `shared`.

- **`Screen` (Rolle 1, in `shared.model`, im Feature implementiert, framework-frei).** Das
  Controller-zugewandte Interface: refresh, esc, save, sortOrderChanged,
  `getSwitchStrategy` … Es hat Zugriff auf ein `ScreenView` und reicht es über
  `getView() : ScreenView` weiter. Die feature-seitige `…Screen`/`…Session`-Klasse
  nennt nie einen JavaFX-Typ.
- **`ScreenView` (Rolle 2, in `shared.model`).** Etwas dass das Hauptfenster ausfüllt. Der mountbare Anzeige-Lieferant:
  `getPane() : Pane`. `getPane` ruft ausschließlich MainWindow. Heißt bewusst
  `ScreenView`, nicht `View` — `View` ist in JavaFX zu häufig.

**Bausteine brauchen keinen dritten Kontrakt.** Sie erben von ihrem JavaFX-Typ und *sind* damit
Nodes; `ComponentHost` nimmt schlicht `Node...`.

Eine Ausnahme, und man sieht sie: `LearnMap` deklariert ein eigenes `getView()`. Ein Interface kann
kein `Node` sein — alle drei Karten *sind* welche, der Typ ist es nicht. Wer über den Kontrakt geht,
braucht deshalb diesen einen Schritt.

**Lesekette:** `Screen → ScreenView → Pane`. MainWindow mountet über
`screen.getView().getPane()` — hier, und nur hier, überquert der Node die Grenze.

**Warum das Feature frei bleibt:** Die framework-freie Grenze ist der *Typ, den das
Feature hält* (`ScreenView`, ein shared-Interface), nicht ein Rückgabetyp-Trick. Die
Feature-Klasse hält ein `ScreenView`-Objekt und reicht es weiter; sie ruft nie
`getPane()` und nennt keinen JavaFX-Typ.

**refresh = stabiler View + In-place-Rebuild.** Der gemountete `ScreenView` bleibt
dasselbe Objekt; `Screen.refresh()` tauscht nur seine Kinder. Kein Controller-Reshow, kein
Swap-Container.

**Ein Screen beendet sich nicht selbst — er meldet sein Ende und wird ersetzt.** Zwischen „fertig"
und „ersetzt" darf nichts stehen, das die Kontrolle an die Event-Schleife zurückgibt: kein
`showAndWait`, kein `Platform.runLater`, kein Thread, kein `Task`. Alles, was der Nutzer noch sehen
soll — Zusammenfassung, Ausblick, Rückfrage —, passiert **davor**, solange der Screen noch lebt.

Rechnen, Speichern und Loggen sind dabei unbedenklich: Solange der FX-Thread in unserem Code steht,
kommt von außen kein Klick dazwischen; die Ereignisse stauen sich in der Queue und werden erst
abgearbeitet, wenn der Controller den Screen längst ersetzt hat. Ein `showAndWait` dagegen startet
eine verschachtelte Event-Schleife und pumpt genau diese Queue weiter.

Daraus folgt die eigentliche Zusage: **Es gibt keinen toten Screen.** Ein Screen ist entweder
lebendig und erreichbar, oder er ist ersetzt und damit unerreichbar — dazwischen liegt kein
Zustand. Deshalb braucht es auch keine Wächter, die einen solchen Zustand abfangen; die frühere
Variante mit `active`-Flag und einem Wurf in jeder Methode ist genau daran gescheitert, dass sie
zwanzigmal dastand und nie ausgelöst hat.

Die Kopplung sieht von außen bei allen gleich aus:

```java
private final XView view = new XView();
public ScreenView getView() { return view; }
public void refresh()       { view.rebuild(); }
```

Das Verb heißt überall `ScreenView.rebuild()`. Was *innerhalb* von `Screen.refresh()` passiert, darf verschieden sein
— wer für seine View Daten sammeln muss, tut das dort (`DashboardScreen`). Und die zwei
Lern-Sessions halten statt einer View einen Presenter, der beides weiterreicht; das ist eine
begründete Abweichung.

**Warum `ScreenView` kein `rebuild()` vorschreibt:** die Hälfte der Views könnte es nicht erfüllen.
`DashboardScreenView.build(daten)` und `MovieViewerScreenView.setNames(…)` brauchen Daten von außen,
`ComponentHost` ist eine passive Leinwand. Die Vorschrift steht ohnehin eine Ebene höher, auf
`Screen.refresh()` — und die ist Pflicht.

**Zwei Bauformen:** (1) eine dedizierte Oberfläche in `shared.ui`, die sich selbst baut und
`ScreenView` implementiert (`DiaryScreenView`, `BarChartScreenView`, `StartScreenView`);
(2) ein generischer `ComponentHost` (`ScreenView`), eine Null-Layout-Pane, in die eine Lern-View
absolut positionierte Bausteine hängt. Discriminator: *wer positioniert* — eine Layout-Pane
(VBox/HBox) selbst → sie ist der View, kein Host; werden die Bausteine mit Rechtecken aus dem Skin
gesetzt → `ComponentHost`.

**Benennung:**

- Nicht-Lern-Oberflächen: Screen `…Screen`, View `…ScreenView`
  (`DiaryScreen` → `DiaryScreenView`).
- Lern-Oberflächen: Screen `…Session`, View `…LearnView`
  (`AnkiDeckSession` → `AnkiLearnView`, `RegionSession` → `RegionLearnView`).

Das Wort **Session** bleibt der Lernseite vorbehalten und meint dort einen Ablauf mit Anfang, Ende
und Fortschritt.

**Inhalt der Interfaces:** leere Defaults erlaubt — eine Oberfläche muss nicht auf
alles reagieren (ein `AlcStatisticsScreen` tut bei `sort()` nichts, kein Fehler).

### Dialoge

Dialoge folgen derselben Grenze wie Screens, nur ohne Lebenszyklus: framework-freier
Input rein → shared-JavaFX-Dialog → framework-freies Ergebnis raus. Sie sind **keine**
`ScreenView`s (werden nicht gemountet, haben kein `getPane`).

**Vertrag (ausnahmslos).** Ergebnis ist ein record/enum oder `null` bei komplexeren Dialogen, nie 
`ButtonType`/`Node` oder Optional. Das Hauptfenster holt sich der
 Dialog intern über `UiUtils.getOwnerWindow()` — kein `Window`-Parameter vom Feature.
 Dismiss/X = immer Abbruch (`CANCEL`); der Aufrufer interpretiert (etwa `CANCEL → später`).

**Zwei Stufen.**
- **Einfach (Auswahl + statischer Inhalt):** ein Alert. `Alerts.show(…)` (in `shared.ui`) gibt einen suite-eigenen `ButtonEnum` zurück; nur diese Klasse kennt `javafx.ButtonType` und übersetzt, niemals
die Aufrufer des Alerts. Zusatzoptionen (Bild als Path, zentrierter Text, ESC-/X-Blockade via `DismissEnum`) reicht ein `AlertOptions`-Objekt hinein — Bild lädt und tint der Skin, nicht das Feature.
- **Komplex (Felder, Mehrfachauswahl, Verflechtung):** eine **bespoke Komponente pro
  Dialog**. Kein generisches Formular-Framework
- **Parametrisiert:** der Standarddialog, der nur Primitive hinein- und Primitive
  oder `null` herausgibt — kein feature-seitiges Objekt, keine eigene Klasse pro Verwendung.
  `TextPromptDialog`, `WhatsAppChatDialog` und `WhatsAppContactDialog` sehen nur deshalb wie
  Verstöße gegen „bespoke pro Dialog" aus; sie sind diese Stufe.
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

### Die Anzeige-Schicht: `shared.ui`, `shared.ui.components`, `shared.skin`

Innerhalb von `shared` gibt es vier Sprossen. Ein Paket benutzt, was darunter steht — nie seitwärts,
nie nach oben. Liste (Beispiele unvollständig):

```
oben    shared.ui       Oberflächen und Bausteine — hier wird gebaut
        shared.skin     SkinProperties (Werte) · Skin (CSS) · die Skins · SkinService · Bildcache
        shared.model    Records, Enums, die drei Kontrakte
unten   shared          Config, DB, Log, AppClock, UiUtils, ...
```

Von außen: `controller` darf auf jede Sprosse (außer shared.ui.component). **Ein Feature greift nie ins Skin-Paket.**

**Warum die UI oben steht:** `shared.ui` baut. Der Skin ist ihr Zulieferer — er hält die Werte, die
CSS nicht ausdrücken kann (Rechtecke, Fonts, Border, Pfade), und erzeugt das Stylesheet. Wer
geliefert bekommt, steht oben.

#### Oberfläche oder Baustein?

> In `shared.ui` liegen die **Oberflächen**, in `shared.ui.components` die **Bausteine**, aus denen
> sie bestehen. Die Wurzel benutzt `components`, nie umgekehrt.

Prüffrage: **Wird es eingebaut, oder ist es das Fertige?**

- Baut etwas anderes in `shared.ui` dieses irgendwo ein → `components`.
- Ist es selbst die fertige Fläche, die herausgereicht wird → `shared.ui`.

Zwei Titelleisten machen den Unterschied greifbar: `SuiteHeaderBar` wird von `SuiteDialog`
*eingebaut* und liegt deshalb in `components`. `MainWindowHeaderBar` wird von nichts in `shared.ui`
eingebaut — sie ist selbst das Fertige — und liegt in der Wurzel. Gleicher Gegenstand,
verschiedene Rolle.

Ein zusammenhängender Cluster darf ein Unterpaket bekommen — `shared.ui.components.map` hält die
zwei Karten samt Innenleben.

#### Der Skin-Vertrag

**Der Skin baut nichts.** Er liefert Werte und erzeugt CSS. Die Aufteilung ist scharf:

- **`SkinProperties`** hält die Felder, lädt sie aus der properties-Datei und gibt sie über
  eine bewusst geschnittene Fläche heraus.
- **`Skin extends SkinProperties`** erzeugt das Stylesheet. Genau **eine** öffentliche Methode:
  `styleScene(Scene)`.

**Nach außen (in shared) gehen zweckgeschnittene Records, nie Property-Namen.** `dialogStyle()`, `mcMetrics()`,
`BigComponentStyle`, `MapImages` — niemand außerhalb von `shared.skin` fragt je nach
`hannoverMapOverlayImageName`. Die Fläche wächst nach Bedarf: erst anlegen, wenn eine Klasse den
Wert wirklich braucht, nicht auf Verdacht.

**Die Schlüssel-Regel — wer löst den Kontext auf?**

> **Ein Baustein holt sich beim Skin, was für jede Verwendung gleich ist. Was von der Verwendung
> abhängt, bekommt er übergeben.**

Der Test steht in der Signatur des Skin-Zugangs: **braucht er ein Argument vom Aufrufer, dann löst
der Aufrufer auf und reicht das Ergebnis weiter.** Ein Argument *ist* der Kontext — wer eines
liefern muss, weiß etwas, das der Baustein nicht wissen soll.

```java
bigComponentStyle()                            // kein Argument       → Baustein holt selbst
iconFor(rolle)                                 // Rolle, kein Kontext → Baustein holt selbst
learnComponentBounds(mapName, kategorie, teil) // braucht Schlüssel   → Aufrufer löst auf
```

Die Regel gilt **ausnahmslos**, auch für Bausteine, die nur ein Feature nutzt. `MovieCard` darf sich
`moviePosterWidth` holen (kein Schlüssel) und bekommt den Film übergeben (Kontext) — und bleibt
trotzdem eine Film-Komponente. *Ob* ein Baustein an ein Feature gebunden ist, ist eine Namensfrage
und kein Konstruktionsprinzip; die Regel sagt dazu nichts.

**Wer entscheidet was:**

| Ebene | entscheidet | Beispiel |
|---|---|---|
| **Feature** | *was* und *wann* | „Deck Deutschland, Frage 7, Antwort war falsch" |
| **View** (`shared.ui`) | *welche Bausteine*, *wie verdrahtet* | „diese Session hat Karte, Frage, Eingabefeld, MC, Zurück-Knopf" |
| **Skin** | *wie es aussieht, wo es sitzt* | Farben, Fonts, Rechtecke, welches Wallpaper zu welcher Karte |

Prüffrage: **wovon hängt die Zeile ab?** Skinwechsel → Skin. Fachlogik → Feature. Keins von beidem,
nur „soll anders aussehen" → View.

*„Stell eine Frage" sagt das Feature. „Zeige auf dem Fragepanel diesen Text" passiert in `shared.ui`.*

#### Bausteine erben, sie verhüllen nicht

Ein `SuiteTextField` **ist** ein `TextField`, es hält keins.

**Ausnahme, wenn JavaFX sie erzwingt.** Ist der Typ `final`, geht Erben nicht. Dann eine **Fabrik**: `SuiteBackground.of(pfad)` liefert einen fertigen `javafx.Background`. In diesem Sinne ist auch DiaryTagInputComponent eine Art Fabrik, sie liefert allerdings zwei getrennte Bausteine per get-Methode.

**Zwei Konstruktoren, wenn der Baustein positioniert werden kann.** Einer ohne Lage (für Aufrufer,
die ihn in ein Layout hängen), einer mit `Rectangle2D` (für absolut positionierende Hosts). Der
zweite delegiert an den ersten. Beispiel: `SuiteInfoLabel`.

#### Bausteine heißen nach dem, was sie sind

Nicht nach dem, wofür sie gerade benutzt werden. Deshalb `ShapeMapLearnView` und nicht
`GermanyLearnView`: die Klasse ist eine Shape-Karten-Ansicht, die zufällig gerade nur ein Deck
bedient. Prüfsatz: **was, wenn ein zweites Deck derselben Bauart dazukäme?** Muss dann nichts
umbenannt werden, stimmt der Name.

Der `Suite`-Präfix sagt „suite-weit brauchbar", nicht „wird überall benutzt". Ein `SuiteTextField`,
das man nur in einer Lern-Session bauen kann, wäre falsch benannt — auch wenn es heute nur dort
vorkommt.

#### Die Karten — was da eigentlich steht

Das Konstrukt in `shared.ui.components.map` erklärt:

```
ShapeLayer        Nachschlagetabelle: json-type → zIndex, interaktiv?, CSS-Layer-Klasse
MapNodeBuilder    Fabrik:             Geometrie → JavaFX-Node
LearnMap          das gemeinsame Vokabular, spricht durchgehend Ids
ShapeMapPane      die eine Karte, arbeitet ohnehin mit Ids
ImageMapPane      die andere Karte, arbeitet mit Geometrien und übersetzt selbst
EmptyLearnMap     die Karte der MC-Session, die keine hat (Nullobjekt)
```

`ShapeLayer` und `MapNodeBuilder` sind paketprivat — sie sind Innenleben, kein Angebot.

Der Grund für das Interface: die beiden Panes sind sehr verschieden (die eine adressiert Formen über
ihre id, die andere über Geometrien), und die View soll beide bedienen können, ohne zu wissen
welche. Zwei Eigenheiten sieht man nur dort: bei der Shape-Karte ist `setClickTargets` leer, weil
dort alle Formen von Anfang an im Szenengraphen liegen; und die Bild-Karte kennt die falsch geklickte
Form nicht per id, sie merkt sich den letzten Klick selbst.

#### Fabrik, Daten, Komponente

Nicht alles in `shared.ui.components` ist ein platzierbarer Baustein.

- Eine **Komponente** ist eine echte UI-Einheit, die in den Szenengraphen gehängt wird und ihren
  Zustand hält (`MultipleChoicePane`, `ShapeMapPane`).
- Eine **Fabrik** *baut* etwas, ist aber selbst keins (`MapNodeBuilder`, `SuiteBackground`).

Am Namen ablesbar: eine Komponente heißt `…Pane` oder trägt ihren Sachnamen, eine Fabrik `…Builder`
oder bietet ein `of(…)`. Die Unterscheidung ist *was die Klasse ist*, nicht *wo sie wohnt*.

## 4. Die bewachten Zusagen

Keine Vorsätze, sondern ArchUnit-Regeln, die den Build brechen. **Was** bewacht wird, steht hier;
**warum** und **mit welchen Ausnahmen**, steht im Javadoc der jeweiligen Regel in
`src/test/java/app/ArchitekturRegelnTest.java`.

```
 1  Der Skin kennt die UI nicht           shared.skin → shared.ui
 2  Kein Feature kennt den Skin           feature → shared.skin
 3  Nur shared.ui kennt die Bausteine     → shared.ui.components
 4  Der Paketgraph ist zyklenfrei
 5  Style-Klassen nur in der Anzeige-Schicht    getStyleClass() nur in shared.ui, shared.skin
 6  shared.model kennt nichts über sich   → shared.ui, shared.skin
 7  shared (Wurzel) kennt nichts drüber   → shared.ui, .skin, .model
 8  Oberste Ebene läuft nur abwärts       nur aus controller heraus oder nach shared hinein
 9  Keine Optional-Rückgaben
10  Keine Streams
11  Features sind framework-frei          javafx nur in shared, controller, app-Wurzel
```

Geprüft wird Bytecode, nicht Quelltext.

Wächter 10 ist **schärfer als die feste Regel 5**: die erlaubt Streams „wenn unbedingt nötig", der
Wächter verbietet sie ganz.

Ungeprüft bleiben die feste Regel 6 (Null-Layout), die feste Regel 7 (Kommentare beschreiben den
Ist-Zustand) und der Javadoc-Vermerk bei `null`-Rückgaben.

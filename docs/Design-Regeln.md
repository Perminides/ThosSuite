# ThosSuite — Design & Regeln (Paketstruktur, Abhängigkeiten, Benennung)

**Stand:** 06.07.2026 · v1.3

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

**Vier feste Regeln:**

1. **Keine Zirkel zwischen Paketen, auch keine transitiven.** Gilt ausnahmslos für alle Pakete.
2. **Auf oberster Ebene laufen die Abhängigkeiten nur nach unten** (Orchestrierung → Fundament).
   Im Inneren eines Pakets gilt die Abwärtsrichtung ebenfalls — mit einer Ausnahme: Hat ein
   Feature Zweige, greifen diese nach oben auf ihren gemeinsamen Kern (siehe „Wann ein Feature
   sich aufteilt"). Regel 1 bleibt davon in jedem Fall unberührt.
3. **Kein Paket auf oberster Ebene greift seitwärts in ein anderes.** Sie hängen nebeneinander,
   nicht ineinander. Was zwei davon brauchen, wandert nach unten in `shared`.
4. **Nicht jede Abweichung von einer Regel ist ein Fehler.** Erst fragen, ob ein guter Grund
   dahintersteckt.

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

### Bildschirm-Kontrakt `Screen`

- Der Bildschirm-Kontrakt heißt `Screen` und liegt in `shared` (warum dort: siehe „So lokal wie
  möglich").
- Nicht-Lern-Oberflächen heißen `…Screen`: `AlcStatisticsScreen`, `FitbitStatisticsScreen`,
  `DashboardScreen`, `MovieViewerScreen`, `DiaryViewerScreen`.
- Die Lern-Oberflächen heißen `…Session` (`AnkiDeckSession`, `RegionSession`), weil sie über die
  reine Bildschirmfläche hinaus die Lernsession tragen.
- **Inhalt:** ein Interface, alle Methoden drin, leere Defaults erlaubt. Eine Oberfläche muss
  nicht auf alles reagieren — ein `AlcStatisticsScreen` tut bei `sort()` nichts, und das ist kein
  Fehler.

### `Skin`-Vertrag (vorläufig — wird beim Skin-Refactoring neu gefasst)

> Dieser Abschnitt ist **nicht in Stein gemeißelt.** Das Skin-System wird ohnehin überarbeitet
> (die `Skin`-Gottklasse, siehe Anhang); danach gehört dieser Vertrag neu geschrieben.

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

## 4. Aufbau einer Lern-Session

[!WARNING] Gehört ins Architekturdokument!

Eine Lern-Session ist aus vier Klassen gebaut. Am Beispiel Anki, nach dem Callback-Fix:

```
												AnkiDeckSession
														│
														▼
   SessionPane  ◄────►  SessionPresenter  ◄────►  SessionProgress
      (GUI)               (Scharnier)            (Karten-Ablauf)
```

- Die **Schale** (`AnkiDeckSession`) sitzt oben und kennt nur den Ablauf (`SessionProgress`) —
  und den nur in eine Richtung. Der Rückweg „letzte Karte fertig" läuft als Callback
  (`onLastCardDone`), nicht als gehaltene Session-Referenz.
- Darunter die Dreierkette **GUI ↔ Presenter ↔ Ablauf.** Beide Kanten sind echt gegenseitig; der
  Presenter ist das Scharnier zwischen Oberfläche und Ablauf.

**region folgt derselben Vierer-Struktur und denselben Begriffen, weicht in der Umsetzung aber
ab** — unter anderem sitzt dort die Auswertung an anderer Stelle. Ob region diesem Muster folgen
soll, ist offen; die Befunde stehen als `!Diagnose`-Block im Javadoc von `RegionSession`. Eigene
Session, kein Nebenbei-Fix.

## 5. Anhang — Bewusst aufgeschoben

[!WARNING] Gehört ins Architekturdokument oder sonstewohin, aber nicht hier

Diese Punkte sind bekannt und absichtlich zurückgestellt — sie verlangen Logik-Umbau und sind
**kein** Anlass für beiläufige Eingriffe:

- **Chart-Logik in den Statistik-Screens** inline lassen (das Auslagern in Skin-Methoden wäre
  Logik-Umbau).
- **`CardSortOrder`** als dumme Enum in `shared.model`; eine Sortier-Factory im `anki`-Paket ist
  geplant, aber nicht implementiert.
- **`closeSilent(boolean save)`** — das `save`-Flag ist lern-gefärbt; eigener Schritt.
- **`Skin`-Klasse aufräumen** (Gottklasse: CSS-Erzeugung, Property-Laden, Layout-Bounds,
  Komponenten-Bau in einer Klasse).
- **Komponentenerstellung vereinheitlichen** — ein durchgängiges Muster (gleicher Schnitt,
  gleicher Bau-/Nutzungsweg).
- **Lernkern `learn` innen:** region weicht vom Anki-Muster ab und ist zu prüfen — siehe
  „Aufbau einer Lern-Session" und den `!Architektur`-Block im Javadoc von `RegionSession`.
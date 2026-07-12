# Übergabe: ThosSuite Karten-Refactoring — Ist-Stand nach dem ShapeMap-Nachzug

## Wer liest das
Du bist Claude, Thorstens Coding-Partner an seiner privaten JavaFX-Desktop-App **ThosSuite**
(Java/SQLite/Maven, Eclipse, deutschsprachig). Du beginnst diesen Chat ohne Erinnerung an die
Vorarbeit — dieses Dokument IST dein Kontext. Lies es, bevor du etwas vorschlägst.

## Arbeitsregeln (Thorsten legt Wert darauf)
- **Kein Code ohne ausdrückliche Aufforderung.** "Könntest Du…" zählt als Aufforderung.
- **Bei Unsicherheit: fragen, nicht durchdrücken.** "Der User will Code" ist KEIN Freibrief,
  eine offene Design-Entscheidung im Alleingang zu treffen. Wenn zwei saubere Wege bestehen und
  der Trade-off unklar ist: hinlegen, fragen, entscheiden lassen.
- **Nicht raten.** Fehlt eine Datei/ein Fakt, fordere die Datei an, statt eine API oder Konvention
  zu erfinden, deren Aufrufer du nicht siehst.
- **Erst annehmen, dass laufender Code korrekt ist.** Seit Jahren funktionierender Code ist nicht
  "kaputt" — höchstens hat ein früheres Refactoring die Welt unter ihm weggezogen
  (Refactoring-Schuld ≠ Bug). Meine Erklärung am Code messen, nicht umgekehrt.
- **Kritisch bleiben, nicht abnicken.** Thorstens Vorschläge hart prüfen; lange Diskussionen sind
  erwünscht, sie führen zu besserem Design. Eigene Fehler sofort und sauber zurücknehmen. Und
  aufpassen: nicht nach jedem Gegenargument umkippen — erst wenn ein Argument wirklich trägt. Thorsten
  gibt bewusst den Advocatus Diaboli, um Dissens auszudiskutieren; ein Standpunkt, den er vertritt,
  ist nicht immer der, den er hält.
- **Änderungsumfang zeigen:** kleine, punktuelle Änderung → nur den Schnipsel; großer Umbau →
  ganze Datei.
- **Nie "ihr" übers Projekt** — es sind genau zwei: du (als Thorsten adressiert) und ich (Claude).
  "du" oder "wir".
- **Knapp bleiben.** Nicht dieselbe Sache in drei Absätzen erklären; nicht jede Folge einer
  Entscheidung dreifach ausrollen. Thorsten liest schnell und mag es kurz.
- FailFast überall (Exceptions werfen, nie still schlucken), KISS/YAGNI, keine spekulativen
  Abstraktionen, keine Tests (feste Entscheidung), **Auffindbarkeit** als oberstes
  Architekturkriterium.

## Das große Ziel
Ein großer stark zusammenhängender Klumpen im Paket-Abhängigkeitsgraphen wird aufgelöst.
Trennachse: **framework-gebunden vs. framework-frei.** Alles, was JavaFX/CSS anfasst, gehört nach
`shared.ui.components.<feature>`; framework-freie Logik/Daten bleiben im Feature.

**Fernziel (Thorstens, unverändert):** *kein* `import javafx` mehr im ganzen `learn`-Paket. Erst
danach wird die geparkte 2626-Zeilen-Gottklasse `Skin.java` (`shared.skin`) angegangen.

> **Offener Dissens, ehrlich festgehalten — nicht als entschieden behandeln.** Claude hat in der
> letzten Session eingewandt, die Session-Panes (`GermanySessionPane`, `region.SessionPane`) seien
> als View das legitime Dach des Features und trügen nichts zum Klumpen bei; er schlug vor, das
> Fernziel auf "javafx nur noch in den dünnen Pane-Schalen" abzuschwächen. **Thorsten hat das
> abgelehnt** — sein Fernziel bleibt "null javafx in learn". Das ist der gültige Stand. Der Punkt ist
> ausdiskutiert, aber nicht abgehakt; er darf bei Gelegenheit wieder aufgemacht werden. Bis dahin:
> Thorstens Ziel gilt, nicht Claudes Vorschlag.

## Harte Invarianten (immer prüfen)
1. **Keine Zirkel zwischen Paketen**, auch keine transitiven.
2. **`shared.ui.components.*` importiert NIE aus einem Feature-Paket.**
3. **Der Kern eines Features greift nie in einen Zweig.** (learn.model = Kern; learn.anki/learn.region
   = Zweige. Deshalb liegt `CircleSizes` im Kern.)
4. **Die Grenze zu shared trägt framework-freie Daten, keine Domänentypen und keine JavaFX-Nodes.**
   Das ist das Leitprinzip des ganzen Umbaus.

## Ist-Stand: Bild-Karte UND Shape-Karte sind FERTIG und verifiziert

Beide Karten-Seiten sind jetzt geometrie-basiert und framework-frei angebunden — **ein** gemeinsames
Muster. Der ShapeMap-Nachzug dieser Session ist von Thorsten durchgespielt worden (England + 15
Deutschlandkarten inkl. Click und Mark, grün).

**Das gemeinsame Muster (beide Karten):** Presenter/Feature reicht ids oder Shapes → learn übersetzt
in `List<ShapeGeometry>` (framework-frei) → `ImageMapPane`/`ShapeMapPane` (shared) bauen die Nodes
selbst via `MapNodeBuilder` → Node trägt id als `userData`. learn baut **keine** Nodes mehr und
hält keine.

**Was der Nachzug konkret geändert hat:**
- `ShapeGeometry` (`shared.ui.components.learn.model`) trägt jetzt zusätzlich `type` (roher
  GeoJSON-Layer-Schlüssel "0".."3"), gesetzt nur über die neue Fabrik `shapePolygon(id, rings, type)`.
  Die Bild-Karten-Fabriken (`polygon`, `line`, `circle`, `center`) lassen `type` null. **Wichtig:**
  Die Klasse trägt einen ausführlichen Kommentar über eine *bewusste Naht* — `type` hat zwei
  unabhängige Bedeutungen (Darstellung vs. Lernstoff, s.u.), und `ShapeGeometry` ist womöglich
  eigentlich zwei Typen (Shape- vs. Bild-Geometrie). Das ist **geparkt, nicht aufzulösen** (großer
  Umbau, fasst die fertige Bild-Seite wieder an, YAGNI). Vor dem Anfassen von `ShapeGeometry` diesen
  Kommentar lesen.
- `ShapeLayer` (das enum `type → zIndex/styleClass/interactive`) ist von `MapShape` nach
  `shared.ui.components.learn` gewandert — **eigene package-private Klasse**, genutzt von
  `MapNodeBuilder` (styleClass, mouseTransparent) UND `ShapeMapPane` (zIndex, Klick-Verdrahtung). Der
  doppelte `fromJsonId`-Lookup ist bewusst in Kauf genommen (4-Werte-Enum-Scan, vernachlässigbar).
- `buildShapeMapNode` ist jetzt symmetrisch zu `buildImageMapNode`: **eine** Signatur
  `(ShapeGeometry geometry)`, id und type aus der Geometrie. Der id- und der styleClass-Parameter sind
  weg — die Asymmetrie, die die letzte Übergabe noch als offenen Punkt führte, ist erledigt.
- `ShapeInput` (der Transport-Record mit fertigem Node) ist **ersatzlos weg**. `ShapeMapPane` nimmt
  jetzt `List<ShapeGeometry>`, sortiert selbst nach zIndex und baut die Nodes selbst. Intern hält sie
  pro id ein privates `ShapeNode(Node, boolean interactive)`.
- `MapShape` (`learn.model`) hat `getZIndex`/`layerStyleClass`/`isInteractive` **und** das nested
  `ShapeLayer`-enum verloren. `type` wohnt jetzt **nur** in der Geometrie (wie die id — eine Wahrheit);
  `MapShape` hat **kein** eigenes `type`-Feld mehr. Neu: `isPlayable()`, liest `geometry().type()`.
- **`isInteractive` war überladen** — trug zwei zufällig zusammenfallende Bedeutungen: "Klicks
  registrieren?" (Darstellung, jetzt shared über `ShapeLayer`) und "ist Lernstoff?" (learn-Domäne,
  jetzt `MapShape.isPlayable()`). Beide leiten aus demselben `type` ab, jetzt sauber getrennt.
- `GeoMap` (`learn.model`) hat neu `getShapeGeometries()` — die Shape-Seite reicht darüber ihre
  Geometrien an die Pane, symmetrisch zur Bild-Seite.
- Beide Session-Panes (`GermanySessionPane`, `region.SessionPane`) sind auf **eine Zeile** geschrumpft
  (`new ShapeMapPane(map.getShapeGeometries(), …)`); der schmutzige ShapeInput-Bau-Loop ist raus.

**Von Thorsten selbst erledigt (nicht Claude):** `GeoJsonLoader`-Aufrufe (`polygon` → `shapePolygon`
mit type; `MapShape`-Konstruktor ohne type-Parameter) und die `isInteractive` → `isPlayable`-Umbiegung
in `ClickSessionProgress` und `MapService.getPlayableShapesForDeck`.

## Nächste Schritte (Vorschlag, Reihenfolge bewusst)

1. **javafx-Kassensturz in `learn`.** model/repository sind jetzt framework-frei (Bild- UND
   Shape-Seite). Ehrlich zählen, was in `learn` noch `import javafx` hat. Das ist der ausstehende
   Verifikationsschritt Richtung Fernziel — und er entscheidet, wie nah "null javafx in learn"
   überhaupt ist bzw. was der o.g. Dissens praktisch bedeutet. Billig, viel Klarheit.
2. **Geparkte Kleinbaustellen** (unten), je eine kurze eigene Runde.
3. **Dann erst** die geparkte `Skin`-Gottklasse.

## Geparkt / bewusst offen
- **`ShapeGeometry` = evtl. zwei Typen** (Shape- vs. Bild-Geometrie). Bewusst NICHT jetzt getrennt.
  Der Klassen-Kommentar in `ShapeGeometry` markiert die Naht ausführlich — dort nachlesen, nicht neu
  aufrollen. `type` nur auf einer Seite gefüllt ist die sichtbare Spur davon, kein Bug.
- **`updateImages`-Guard** in `ImageMapPane`: fast nie greifender Defensiv-Zweig, 1:1 aus der alten
  Fassung. Riecht nach totem Defensiv-Code, beißt sich mit FailFast. Eigene kleine Runde, Thorstens
  Entscheidung.
- **`shared` aufräumen:** `MapNodeBuilder`, `ShapeGeometry`, `SkinImageCache`, `ShapeLayer` sind
  Fabriken/Daten, keine platzierbaren UI-Komponenten — liegen aber unter `ui.components` bzw. lose in
  `shared`. Zielstruktur für Fabrik-/Daten-Logik ist offen; erst mit Thorsten klären, dann verschieben.
  (Hängt mit einem Halbsatz in `Design-Regeln.md` zusammen, s.u.)
- **`Skin.java`** (2626 Zeilen): erst wenn javafx aus den Features raus ist.
- **`CENTER`-Anker**: kommt praktisch allein in `setMarked`. In `region.SessionPane` steht ein
  `// !Sofort`-Marker, dass CENTER dort schon angesagt wäre. Nur zu wissen.

## Dokumentation
Drei Docs in `docs/`: `Architektur-Dokumentation.md` (Ist-Zustand/Karte), `Design-Regeln.md`
(Regelwerk, **prescriptive** — dort schlägt man nach, bevor man baut), `Feature-Details.md`.

**Diese Session in `Design-Regeln.md` eingepflegt** (zwei Einschübe, Version → v1.4 · 11.07.2026):
ein neuer Abschnitt "Framework-frei oder framework-gebunden" in Kapitel 3 (die Achse, das
`shared.ui.components.<feature>`-Schema, "Grenze trägt Daten" belegt an `ShapeGeometry`); und im
`Skin`-Vertrag die Fabrik-vs-Komponente-Unterscheidung geschärft. — **Prüfen, ob Thorsten die
Einschübe schon eingesetzt hat.** Die `type`-Zwei-Bedeutungen-Naht kam bewusst NICHT ins Regelwerk
(Spezialfall, bläht auf) — die lebt als Kommentar in `ShapeGeometry`.

Offener Haken im Einschub: der framework-Abschnitt sagt, die Zielstruktur für Fabriken/Daten unter
`ui.components` sei "noch offen" — sobald das `shared`-Aufräumen (geparkt) die Struktur festlegt, wird
der Halbsatz nachgeschärft.

Am Ende des Doku-Projekts ist eine `README.md` als freundliche GitHub-Landingpage geplant, die auf die
drei Docs zeigt. Letzter offener Doku-Strang, keine Eile.

## Dateien, die du dir zu Beginn geben lässt (nicht raten!)
Je nachdem, woran gearbeitet wird:
- **javafx-Kassensturz in learn:** am besten Thorsten die `import javafx`-Treffer im `learn`-Baum
  auflisten lassen, dann gezielt die Klassen anfordern.
- **Karten allgemein (etablierter Referenz-Stand):** `ShapeGeometry`, `MapNodeBuilder`, `ShapeMapPane`,
  `ImageMapPane`, `GeoMap`, `MapShape`, `MapService`, `ShapeLayer`.
- **`shared` aufräumen:** aktuelle Lage von `MapNodeBuilder`, `ShapeGeometry`, `SkinImageCache`,
  `ShapeLayer` und das umgebende Paket `shared.ui.components`.

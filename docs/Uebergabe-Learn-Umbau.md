# Übergabe: ThosSuite Karten-Refactoring — Ist-Stand nach dem Bild-Karten-Umbau

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
  erwünscht, sie führen zu besserem Design. Eigene Fehler sofort und sauber zurücknehmen.
- **Änderungsumfang zeigen:** kleine, punktuelle Änderung → nur den Schnipsel; großer Umbau →
  ganze Datei.
- **Nie "ihr" übers Projekt** — es sind genau zwei: du (als Thorsten adressiert) und ich (Claude).
  "du" oder "wir".
- FailFast überall (Exceptions werfen, nie still schlucken), KISS/YAGNI, keine spekulativen
  Abstraktionen, keine Tests (feste Entscheidung), **Auffindbarkeit** als oberstes
  Architekturkriterium.

## Das große Ziel
Ein großer stark zusammenhängender Klumpen im Paket-Abhängigkeitsgraphen wird aufgelöst.
Trennachse: **framework-gebunden vs. framework-frei.** Alles, was JavaFX/CSS anfasst, gehört nach
`shared.ui.components.<feature>`; framework-freie Logik/Daten bleiben im Feature.

**Fernziel:** *kein* `import javafx` mehr im ganzen `learn`-Paket. Erst danach wird die geparkte
2626-Zeilen-Gottklasse `Skin.java` (`shared.skin`) angegangen.

## Harte Invarianten (immer prüfen)
1. **Keine Zirkel zwischen Paketen**, auch keine transitiven.
2. **`shared.ui.components.*` importiert NIE aus einem Feature-Paket.**
3. **Der Kern eines Features greift nie in einen Zweig.** (learn.model = Kern; learn.anki/learn.region
   = Zweige. Deshalb musste `CircleSizes` in den Kern, s.u.)
4. **Die Grenze zu shared trägt framework-freie Daten, keine Domänentypen und keine JavaFX-Nodes.**
   Das ist das Leitprinzip des ganzen Umbaus.

## Ist-Stand: Der Bild-Karten-Umbau ist FERTIG (aber noch nicht erneut durchgespielt)

Die Bild-Karte (Welt/Hannover) ist vollständig geometrie-basiert und framework-frei angebunden.
Konkrete Klassen und wo sie liegen:

**shared-Seite (framework-gebunden):**
- `ShapeGeometry` (`shared.ui.components.learn.model`, früher `GeoGeometry`): framework-freie
  Geometrie-Vokabel. **Trägt jetzt ihre id.** Vier Formen über statische Fabriken:
  `polygon(id, rings)`, `line(id, lines)`, `circle(id, x, y, r)`, `center(id, x, y)`.
  `CENTER` ist ein reiner Zentrier-Anker (kein sichtbares Shape). Punkte liegen in
  Bildschirm-Koordinaten (Y-Invertierung im Loader).
- `MapNodeBuilder` (`shared.ui.components.learn`): DIE einzige Stelle, an der ein JavaFX-Node aus
  `ShapeGeometry` entsteht. `buildImageMapNode(geometry)` liest die id aus der Geometrie (setzt sie
  als `userData`). `buildShapeMapNode(id, geometry, layerStyleClass, interactive)` nimmt die id
  **noch als Parameter** — bewusste Asymmetrie, weil die ShapeMap-Seite noch nicht umgestellt ist.
- `ImageMapPane` (`shared.ui.components.learn`, früher in `learn.anki`): kennt kein `learn` mehr.
  Konstruktor bekommt vier Bild-**Pfade** (`java.nio.file.Path`) + `Rectangle2D` und holt die
  Bilder selbst aus dem `SkinImageCache`. `setToCheckShapes/addToCorrect/setMarked` nehmen
  `List<ShapeGeometry>`. Klicks über `Consumer<String>` (id, oder `null` = Klick ins Leere).
  Pro id existiert genau **ein** Node im Layer, wiedergefunden über `userData == id`
  (`place()`/`findInLayer()`); Zustandswechsel passieren am selben Node, kein Stapeln.
  `resetMarkers()` leert den Layer (Nodes leben pro Karte). `CENTER` wird in `place()` abgefangen →
  nur zentrieren, kein Node. Falsch-Klick-Marker ist die Ausnahme: keine wiederauffindbare id,
  wird schlicht angehängt.
- `SkinImageCache` (`shared.skin`): cacht große, skin-abhängige Bilder **für den aktiven Skin**
  (leert bei Skin-Wechsel). Generisch gehalten, kennt kein Feature — nimmt `Path`, gibt `Image`.
  `get(path)` / `warm(path)`.

**learn-Seite (framework-frei, außer den Panes selbst):**
- `GeoMap` (`learn.model`): javafx-frei. Hält nur Shapes + Typ. `geometryFor(Set<String>)
  → List<ShapeGeometry>` ist die EINE id→Geometrie-Übersetzung (echtes Shape via Lookup;
  "größe|x|y" → CIRCLE via `CircleSizes`; "empty|x|y" → CENTER).
- `MapShape` (`learn.model`): record. **Hält die id nicht mehr selbst** — `id()` delegiert an
  `geometry().id()` (eine Wahrheit). `equals`/`hashCode` id-basiert (über `id()`).
- `CircleSizes` (`learn.model`, von Thorsten aus `learn.anki.model` hierher gezogen): framework-frei,
  Name→Radius, `EMPTY` als Sonderwert. Musste in den Kern, weil `GeoMap.geometryFor` es braucht
  (Kern darf nicht in Zweig greifen).
- `MapImagePaths` (`learn.model`): record der vier Bildpfade (framework-frei).
- `MapRepository` (`learn.repository`): `load(Deck)` lädt nur noch Shapes, kein javafx.
- `GeoJsonLoader` (`learn.repository`): setzt die id jetzt in die Geometrie (`polygon(id, …)`),
  baut `MapShape` ohne id-Parameter.
- `MapService` (`learn`): javafx-frei. `getMap(deck)` (Shapes, gecacht, skin-unabhängig),
  `imagePathsFor(deck)` (vier Pfade aus dem Skin), `preload(deck)` (Splash: wärmt Shapes + bei
  Bild-Karten die Bilder via `SkinImageCache.warm`), `getPlayableShapesForDeck`.
- `AnkiDeckService` (`learn.anki`): ruft im Splash `preload(type)` statt `getMap`.
- `ImageMapSessionPane` (`learn.anki`): holt `GeoMap` + `MapImagePaths`, konstruiert `ImageMapPane`
  mit den vier Pfaden, `setListener(this::mapElementClicked)`. Pro Presenter-Aufruf genau ein
  `mapPane.setMarked(map.geometryFor(ids))` — keine Map, keine Hilfsmethode.

**Datenfluss:** Presenter reicht ids → `ImageMapSessionPane` → `GeoMap.geometryFor(ids)`
→ `List<ShapeGeometry>` (jede mit id, empty als CENTER) → `ImageMapPane` (shared) → `place()` →
`MapNodeBuilder` → Node (userData = id).

## Nächste Schritte (Vorschlag, Reihenfolge bewusst)

1. **ERST verifizieren.** Thorsten spielt ein paar Welt- und Deutschland-Karten durch, bevor
   Neues obendrauf kommt. Der Bild-Karten-Umbau lief seit dem letzten Durchspielen durch mehrere
   Änderungen (userData-Wiederverwendung, CENTER, id-in-Geometrie). Nicht auf ungeprüftem
   Fundament weiterbauen.
2. **ShapeMap-Seite nachziehen** (`ShapeMapPane`, `GermanySessionPane`, `region.SessionPane`,
   `ShapeInput`): auf denselben geometrie-basierten Stand. `ShapeInput` trägt aktuell noch einen
   **fertigen Node** (Spike-1-Stand) — also javafx in learn, exakt das, was wir bei der Bild-Karte
   gerade beseitigt haben. Danach fällt der id-Parameter aus `buildShapeMapNode`, die Asymmetrie im
   `MapNodeBuilder` verschwindet, beide Karten-Panes teilen ein Muster. Der Bild-Karten-Umbau ist
   der fertige Präzedenzfall.
3. **javafx-Kassensturz in `learn`.** Nach Schritt 2 ist javafx aus `learn.model`/`learn.repository`
   raus. Dann ehrlich prüfen, was in `learn` noch javafx zieht (vermutlich die Session-Panes selbst
   und ihr Umfeld) — Richtung Fernziel.
4. **Dann erst** die geparkte `Skin`-Gottklasse.

## Geparkt / bewusst offen
- **ShapeMap-Seite** noch nicht umgestellt (Schritt 2 oben) → Asymmetrie in `buildShapeMapNode`.
- **`updateImages`-Guard** in `ImageMapPane`: ein fast nie greifender Defensiv-Zweig (1:1 aus der
  alten Fassung übernommen). Riecht nach totem Defensiv-Code, beißt sich mit FailFast. Bewusst nicht
  angefasst — eigene kleine Runde, Thorstens Entscheidung.
- **`shared` aufräumen:** `MapNodeBuilder`, `ShapeGeometry`, `SkinImageCache` sind Fabriken/Daten,
  keine platzierbaren UI-Komponenten — liegen aber unter `ui.components` bzw. lose in `shared`. Die
  Zielstruktur für Fabrik-/Service-/Daten-Logik ist offen; erst mit Thorsten klären, dann verschieben.
- **`Skin.java`** (2626 Zeilen): erst wenn javafx aus allen Features raus ist.
- **`CENTER`-Anker**: kommt praktisch allein in `setMarked`. Käme er mit echten Shapes gemischt,
  gewänne am Ende die Zentrierung auf die Shapes (der Anker liegt nicht im Layer). Kein Handlungs-
  bedarf, nur zu wissen.

## Dokumentation (offener Nebenstrang)
Es gibt drei Docs in `docs/`: `Architektur-Dokumentation.md` (Ist-Zustand/Karte),
`Design-Regeln.md` (Regelwerk), `Feature-Details.md`. `Design-Regeln.md` hat offene Haken, die
dieser Umbau mit Inhalt gefüllt hat und die man festhalten sollte, solange die Begründung frisch
ist: die Achse framework-frei/-gebunden, das `shared.ui.components.<feature>`-Schema, "Grenze trägt
Daten, keine Domänentypen/Nodes" (jetzt belegt an `ShapeGeometry`-mit-id), und die
Fabrik-vs-Komponente-Unterscheidung. Am Ende des Doku-Projekts ist eine `README.md` als freundliche
GitHub-Landingpage geplant, die auf die drei Docs zeigt.

## Dateien, die du dir zu Beginn geben lässt (nicht raten!)
Je nachdem, woran gearbeitet wird:
- **ShapeMap-Nachzug:** `ShapeMapPane`, `GermanySessionPane`, `region.SessionPane`, `ShapeInput`,
  aktueller `MapNodeBuilder`.
- **Allgemein Karten:** aktuelle `ImageMapPane`, `ShapeGeometry`, `MapNodeBuilder`, `GeoMap`,
  `MapShape`, `MapService` als Referenz für den etablierten Stand.

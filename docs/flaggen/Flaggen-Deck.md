# ThosSuite — Flaggen-Deck (Planung)

**Stand:** 28.08.2026 — **Beide Fragenstaffeln stehen, alle 206 Flaggen sind danach attributiert**,
und **der Skizzen-Mechanismus ist entschieden.** Siebzehn Spalten für den Hintergrund, elf im
Zusatzelemente-Blatt, geprüft durch `pruefe-attribute.py`, null Widersprüche. Der **Durchstich
läuft**: Deutschland und Dänemark sind ein Proof of Concept in der Suite, mit aufbauender Skizze.

Am 26.08. ist geklärt worden, **was in eine Sketch-Datei gehört und was zur Laufzeit dazukommt**
(§5): Gösch und Dreieck werden hineingezeichnet, die Zusatzelemente über `SketchImageAdd` angehängt,
alles auf einer gemeinsamen Leinwand mit einem 3×3-Raster.

Am 27./28.08. ist die **Elementseite komplett umgebaut** worden — von der Weiche mit sechs Kategorien
zu einer flachen Mehrfachauswahl, mit Kreis und Gösch als Orten statt als Behältern (§3). Dafür
wurden die **MC-Syntax** neu gefasst (unten in §6) und die **echte Mehrfachauswahl** entworfen.

**Als Nächstes:** die Farbliste für die Hintergrundflächen — die einzige Lücke, die den Generator noch
blockiert. Danach der Generator bis zum Sketch.

**Charakter dieses Dokuments:** Übergabe an das Ich, das die Sache irgendwann anfasst. Festgehalten
ist, was entschieden ist, was Idee geblieben ist und was bewusst verworfen wurde. Deskriptiv für den
Stand der Überlegung, nicht vorschreibend — die Regeln der Suite stehen weiterhin in
`Design-Regeln.md`, der Ist-Zustand des Lern-Kerns in `Feature-Details.md`.

---

## 1. Zuschnitt

Ein **eigenes Flaggen-Deck**, kein Länder-Deck. Länderattribute (Hauptstadt, Währung, höchster
Berg, Einwohnerzahl) sind bewusst draußen: Hauptstädte und Währungen sitzen längst in einem echten
Anki-Deck, der höchste Berg Albaniens ist Trivia ohne Aufhänger.

Das Kernfeature: **eine Flagge aus dem Gedächtnis vollständig beschreiben**, geführt durch eine
Fragenkette, während sich daneben eine Skizze aufbaut. Dazu kommen Geschichten-Karten rund um die
Flaggen (Plus Ultra, Nepals Form, Liberia und die USA), die von Hand geschrieben werden.

Größenordnung: ~200 Beschreibungskarten, ~200 in Gegenrichtung, 150–200 Geschichten-Karten. Das Deck
hat damit eine Decke — anders als MC, Welt und Deutschland, die stetig wachsen.

## 2. Das Grundprinzip

**Messen statt klassifizieren.** Nicht „in welche Kategorie fällt diese Flagge" — eine Taxonomie
aller Weltflaggen schließt nie, und jeder neue Sonderfall wirft frühere Länder wieder um. Stattdessen
präzise Messfragen, die man durch Hinsehen beantwortet: „Wie viele Bänder laufen von Rand zu Rand
durch?"

**Wahrnehmungswörter sind das Warnsignal.** „Klar erkennbar", „dominant", „im Wesentlichen" in einer
Frage heißt: die Entscheidung ist noch nicht getroffen, sondern ans Bauchgefühl delegiert. Solche
Wörter werden durch geometrische Kriterien ersetzt und die Grenzfälle per Dekret geregelt. Beispiel:
*„Gibt es ein gerades Kreuz von oben nach unten und ganz links nach ganz rechts, das die Flagge in
vier Quadranten teilt? Auch unsichtbare Kreuze zählen."* — Panama ist damit eindeutig ja. Härtefälle
kommen in ein **CheatSheet**, das erklärt, welche Regel greift.

**Vollständigkeit, nicht Trennschärfe.** Die Fragenkette ist fertig, wenn jemand die Flagge aus der
Antwortfolge zeichnen könnte — nicht, wenn das Land eindeutig bestimmt ist. Dass Spanien die einzige
rot-gelb-rote Flagge ist, macht die Wappenfrage nicht überflüssig.

**Eine Frage entfällt nur, wenn ihre Antwort zwingend folgt.** Bei Panama ist die Restfarbe nach
„Kreuzfarbe unsichtbar" logisch festgelegt, der Step entfällt. Dass eine Antwort nur *häufig* ist
(Elementposition „Mitte"), rechtfertigt kein Weglassen — sonst verrät allein das Auftauchen der Frage
die Antwort.

**Die Attribute sind für die Fragen gebaut, aber nicht 1:1 deren Antworten.** Sie beschreiben die
fertige Flagge, die Fragenstaffel ist ein Weg dorthin — dieselbe Information, andere Richtung.
Dazwischen liegt eine **Ableitung**: Ein Teil der Fragen liest ein Attribut direkt, andere entstehen
aus einer Regel (der Rahmen aus ≥2 waagerecht *und* ≥2 senkrecht *und* kein Kreuz, das
Dreieck-ja/nein aus der Dreiecksform, der Hintergrund daraus, welches Attribut gesetzt ist). Diese
Ableitung wohnt im Generator — und weil ein Skript nicht so gut lesbar ist wie ein kurzer Ausdruck,
gehören ihre Regeln in dieses Dokument. Es wird ausdrücklich *nicht* versucht, alle Flaggen
wasserdicht zu attributisieren; das gelingt ohnehin nicht.
Preis: eine nachträglich eingefügte Frage kostet bis zu 200 nachzupflegende Zellen — im Regelfall
deutlich weniger, weil die meisten Fragen tief im Baum hängen. Wo eine neue Frage aus vorhandenen
Spalten berechenbar ist (Streifenzahl aus der Farbliste), kostet sie nichts.

**Und die Fragenfolge sticht die Attribute.** Die Attribute beschreiben die fertige Flagge, die
Fragenstaffel ist ein Weg dorthin — dieselbe Information, andere Richtung. Deshalb ist die erste
Frage auch nicht das erste Attribut. Wenn eine gute Fragenfolge ein Attribut braucht, das es noch
nicht gibt, kommt das Attribut dazu; nicht umgekehrt. Der Attributsatz ist Entwurf, kein Fundament.

### Regeln für Fragen und Antwortlisten

**Vergleichend formulieren, nicht graduell.** „Ist der mittlere Streifen breiter als die anderen?" ist
entscheidbar, „sind die Streifen ungefähr gleich breit?" nicht. Belize ist zweifelsfrei „Mitte
breiter", egal wie extrem — nach Grad gefragt wäre es strittig gewesen.

**Für jede einzelne Flagge darf genau eine Antwortoption zutreffen.** Das ist der Test auf
Wasserdichtigkeit — er gilt pro Flagge, nicht zwischen Flaggen: Dass Tschad und Rumänien dieselbe
Antwort geben, ist völlig in Ordnung. Die frühere Liste „dünne Ränder / breite Mitte" fiel dagegen
durch, weil auf Belize *beide* Optionen passen und beim Klicken unklar wäre, welche gemeint ist.

**Falsche Vorstellungen müssen ausdrückbar sein.** Die Antwortliste zeigt nicht nur, was vorkommt:
Bei der Streifenzahl werden 2 bis 9 angeboten, obwohl die 8 nie vorkommt, und „unterster breiter"
steht als reiner Distraktor in der Liste. Sonst wirst du in die richtige Richtung gedrängt und ein
falsches Bild im Kopf fällt nie auf.

**Eine Frage lohnt sich, wenn ihre Antwort im Zweig variiert.** Bei zwei waagerechten Streifen sind
alle zwölf Flaggen gleiche Hälften — dort entfällt die Breitenfrage. Nicht weil die Antwort trivial
wäre, sondern weil es bei zwei Streifen eine *andere* Frage wäre („oben oder unten" statt „Mitte oder
Rand"). Der Preis ist bekannt und akzeptiert: Ein falsches Bild von einer zweistreifigen Flagge mit
ungleichen Hälften würde nie auffallen.

**Dieselbe Frage in allen Zweigen.** Die Fünfer-Strukturfrage muss über alle 16 Fünfer sauber sein,
nicht nur über die ohne Gösch und Dreieck — sonst verrät die Optionsliste, in welchem Zweig man
steht.

## 3. Die Fragenkette

### Bänder

Ein **Band** läuft in einer Farbe von Rand zu Rand durch. Kreuzungen mit anderen Bändern
**unterbrechen** — Zentralafrika hat 0 waagerechte Bänder, weil das senkrechte rote durchläuft.
Elemente unterbrechen **nicht** — Belize hat 3 Bänder, das Wappen zählt nicht als Unterbrechung.
**0 Bänder = uni**, sonst zählte ein einfarbiges Feld gleichzeitig als waagerechtes und als
senkrechtes Band.

### Hintergrund

Betrachtet wird die Flagge **ohne Elemente und ohne Gösch**. Damit entfällt die Kollision
„waagerechte Streifen *und* Gösch" bei USA, Liberia, Malaysia und Co.

### Die festgelegten Attribute

Alle 206 Flaggen sind erfasst — die 193 UN-Mitglieder plus 13 Nicht-Staaten (Bermuda, Kaimaninseln,
Cookinseln, England, Grönland, Guam, Hongkong, Neukaledonien, Palästina, Puerto Rico, Schottland,
Taiwan, Wales). Nicht drin: Vatikan und Kosovo. Die Reihenfolge ist die der Signatur:

```
 1  Rechtwinklig?            7  W-Streifen           13  Kreuzarme
 2  Rahmen?                  8  3W                   14  Diagonal Richtung
 3  Gösch?                   9  5W                   15  Diagonal Anzahl Streifen
 4  Dreieck von links?      10  S-Streifen           16  SW Streifen
 5  Dreiecksflächen         11  S-Anordnung          17  Spezial
 6  Hintergrundtyp          12  Kreuzausrichtung
```

**Die ersten fünf gelten für alle**, die übrigen hängen am `Hintergrundtyp` — er ist die Weiche, und
jeder seiner Werte zieht seine eigenen Folgespalten nach sich:

| Hintergrundtyp | Flaggen | Folgespalten |
|---|---|---|
| 0 waagerechte Streifen | 103 | `W-Streifen`, daraus `3W` (bei 3) und `5W` (bei 5) |
| 1 senkrechte Streifen | 28 | `S-Streifen`, `S-Anordnung` |
| 2 Kreuz | 15 | `Kreuzausrichtung`, `Kreuzarme` |
| 3 diagonal | 12 | `Diagonal Richtung`, `Diagonal Anzahl Streifen` |
| 4 einfarbig | 35 | — |
| 5 senkrechtes Band mit waagerechten Streifen | 6 | `SW Streifen` |
| 7 speziell | 7 | `Spezial` |

Die **6 bleibt bewusst frei** — Platz für einen Fall, der noch kommen kann.

**`x` heißt „diese Frage wird hier nicht gestellt"**, und das ist etwas anderes als eine 0. Die 0 ist
ein regulärer Antwortwert — „nein", „waagerecht", „alle gleich breit", „uni", „steigend". Nur die
Zählspalten `W-Streifen`, `S-Streifen` und `SW Streifen` kennen sie nicht: Dort *ist* der Wert die
Anzahl, und die fängt bei 2 an. `Diagonal Anzahl Streifen` ist die Ausnahme unter den Zählspalten —
dort bedeutet die 0 „Teilung vorhanden, aber kein eigenes Band", so wie Bhutan und
Papua-Neuguinea.

Genau daran hängen die Prüfregeln in `pruefe-attribute.py`: Eine Spalte trägt **genau dann** einen
Wert statt `x`, wenn die Frage davor hierher geführt hat.

Die Werte entsprechen den Antwortoptionen ihrer Frage, nullbasiert:

```
3W  0 alle gleich breit          5W  0 2 und 4 dünn, Mitte nicht breiter
    1 mittlerer breiter              1 alle gleich
    2 mittlerer schmaler             2 Mitte breiter, 2 und 4 nicht dünn
    3 oberster breiter               3 Mitte breiter und 2 und 4 dünn
    (4 unterster breiter)            4 oberster am breitesten
                                     (5 unterster am breitesten)

S-Anordnung  0 gleichmäßig breit    Kreuzausrichtung  0 senkrecht     Kreuzarme  0 uni
             1 mittlerer breiter                      1 diagonal                 1 drei parallele Farben
             2 rechter breiter                        2 beides                   2 fimbriert
             (3 linker breiter)                                                  3 nicht sichtbar

Diagonal Richtung  0 steigend                        Diagonal Anzahl  0 kein Band, Flächen stoßen an
                   1 fallend                          Streifen         1 · 2 · 3 · 4 Bänder
                   2 strahlenförmig aus einer Ecke

Dreieck von links  1 einzelnes echtes Dreieck        Dreiecksflächen  1 äußere als Umrandung
                   2 abgeschnittenes Dreieck                          2 zwei verschiedene Tiefen
                   3 waagerecht zum Flugteil verlängert               3 gestaffelt und umrandet
                   4 echtes Dreieck, mehr als eine Farbe
                   5 einzeln, reicht bis zur Flugseite
```

Die eingeklammerten Werte sind reine Distraktoren und kommen bei keiner Flagge vor.

**`S-Anordnung` deckt alle senkrechten Streifenzahlen ab**, nicht nur die Dreier — sie sagt schlicht,
welcher Streifen der breiteste ist. Deshalb tragen Pakistan und Portugal (zwei Streifen) genauso einen
Wert wie Sri Lanka (vier). Die waagerechte Seite ist dort anders geschnitten: `3W` und `5W` sind zwei
Spalten für zwei Streifenzahlen, weil `3W` mit „mittlerer schmaler" etwas unterscheidet, das eine reine
„welcher ist der breiteste"-Frage nicht ausdrücken kann. Die Asymmetrie ist gewollt.

**„Rahmen" ist kein Attribut, sondern abgeleitet** — ≥2 waagerechte *und* ≥2 senkrechte Streifen
*und* kein Kreuz. Trifft Guam, Malediven, Montenegro, Sri Lanka und Grenada; die Kreuz-Bedingung hält
Dominica draußen, dessen Bänder vom Trikolore-Kreuz stammen. Die Regel gilt nur für rechteckige
Flaggen — Nepal hat einen Rand, aber keine Streifen.

Vorkommende Bänderzahlen: **2, 3, 4, 5, 6, 7, 9, 11, 13, 14** — Mauritius die 4, Uganda die 6,
Simbabwe die 7, Griechenland und Uruguay 9, Liberia 11, USA 13, Malaysia 14. Die 1 steht für uni
(ein Feld läuft in beide Richtungen durch), die **−1 für „Teilung vorhanden, aber unsichtbar"** —
Panamas Kreuz, Bhutans und Papua-Neuguineas Diagonale.

### Die Fragenstaffel

Fünf Fragen gelten für **alle 206** und tragen die Flagge Schicht für Schicht ab. Danach verzweigt es
sich nach dem Hintergrundtyp.

```
1  "Ist die Flagge rechteckig?"                    Ja | Nein → Spezialzweig (nur Nepal)
2  "Hat die Flagge einen Rahmen?"                  Ja | Nein        (abgeleitet, kein Attribut)
3  "Hat die Flagge einen Gösch?"                   Ja | Nein
4  "Entferne Rahmen und Gösch gedanklich.
    Ragt ein Dreieck ganz vom linken Rand herein?" Ja → 4a | Nein → 5
4a "Was beschreibt die Dreiecksfigur am besten?"   fünf Formen, siehe Attributwerte
4b "Wie sind die Flächen des Dreiecks angeordnet?" nur bei mehrfarbigem Dreieck
5  "Entferne auch das Dreieck und die Zusatzelemente.
    Was beschreibt den Hintergrund am besten?"     die sieben Hintergrundtypen
```

Zu Frage 1: Sie fängt Nepal ab, bevor es alles Weitere kaputtmacht — jede spätere Formulierung setzt
Rechteckigkeit voraus (Bänder von Rand zu Rand, Ecken für den Gösch, „vom ganzen linken Rand"). Dass
205 Flaggen mit „ja" antworten, spricht nicht dagegen.

Zu Frage 4: Kuwait, Südafrika und Vanuatu werden hier auffällig und brauchen eine CheatSheet-Regel.

### Die Zweige nach Frage 5

Jede Zeile ist eine Attributspalte, eine Frage und ihre Antwortoptionen. Eingeklammerte Werte sind
reine Distraktoren und kommen bei keiner Flagge vor.

**Waagerechte Streifen** (103 Flaggen)

| Spalte | Frage | Optionen |
|---|---|---|
| `W-Streifen` | Wie viele waagerechte Streifen? | 2 · 3 · 4 · 5 · 6 · 7 · 8 · 9, aus einem Bereich gezogen |
| `3W` | Wie sind die Streifen verteilt? *(nur bei 3)* | alle gleich breit · mittlerer breiter · mittlerer schmaler · oberster breiter · *(unterster breiter)* |
| `5W` | Wie sind die Streifen verteilt? *(nur bei 5)* | 2 und 4 dünn, Mitte nicht breiter · alle gleich · Mitte breiter, 2 und 4 nicht dünn · Mitte breiter **und** 2 und 4 dünn · oberster am breitesten · *(unterster am breitesten)* |

Bei den anderen Anzahlen gibt es nur ein oder zwei Flaggen, alle gleichmäßig — dort entfällt die
Verteilungsfrage. Bei zwei Streifen wäre es eine *andere* Frage („oben oder unten" statt „Mitte oder
Rand"); der Preis ist bekannt und akzeptiert.

Lesotho (etwa 3:4:3) ist der Grenzfall bei `3W`: „alle gleich breit" wird dort **nicht als falsch
gewertet**. Israel steht bei `5W` in der dritten Zeile, nicht in der vierten — seine Streifen sind
15 : 25 : 80 : 25 : 15, die **äußeren** sind die dünnsten. Kap Verde greift auf „oberster am
breitesten", weil seine drei mittleren Bänder gleich breit sind.

Die beiden Merkmale „2 und 4 am dünnsten" und „Mitte am breitesten" sind unabhängig und können
gemeinsam auftreten. Eine erste Fassung nannte pro Option nur *ein* Merkmal und ließ dadurch bei
Nordkorea, Suriname und Eswatini zwei Optionen zutreffen. Die Auflösung sind die ausdrücklichen
„aber nicht"-Zusätze — **Teilbeschreibungen überlappen, vollständige nicht.**

**Senkrechte Streifen** (28)

| Spalte | Frage | Optionen |
|---|---|---|
| `S-Streifen` | Wie viele senkrechte Streifen? | 2 (4 Flaggen) · 3 (23) · 4 (1) |
| `S-Anordnung` | Wie sind sie verteilt? | gleichmäßig · mittlerer breiter · rechter breiter · *(linker breiter)* |

Anders als waagerecht wird die Verteilungsfrage **bei jeder Streifenzahl** gestellt. Der Grund ist die
Verteilung: Bei den senkrechten Zweiern weicht die Hälfte ab (Pakistan und Portugal gegen Algerien und
Malta), bei den waagerechten Zweiern keine einzige.

**Kreuz** (15)

| Spalte | Frage | Optionen |
|---|---|---|
| `Kreuzausrichtung` | Welche Form hat das Kreuz? | senkrecht · diagonal · beides |
| `Kreuzarme` | Welche Form haben die Arme? | uni · drei parallele Farben · fimbriert · nicht sichtbar |

Fünfzehn Flaggen, sechs Sketches: Dänemark (das auch die Dominikanische Republik mitnimmt), Dominica,
Island, Jamaika, Panama und das Vereinigte Königreich.

Die zweite Frage bringt den Begriff **Fimbrierung** zurück, den §9 für die Streifen verworfen hat. Das
ist kein Rückfall: Der Einwand von damals hing an der Population, nicht am Begriff. Bei den Fünfern
gab es echte Grenzfälle, bei den Kreuzen nicht — die Menge ist klein und **geschlossen**.

`nicht sichtbar` ist der Panama-Fall und entspricht der `-1` bei den Bänderzahlen: Die Teilung ist da,
die Farbe des Kreuzes nicht zu sehen.

**Diagonal** (12)

| Spalte | Frage | Optionen |
|---|---|---|
| `Diagonal Richtung` | Wie läuft die Diagonale? | steigend · fallend · strahlenförmig aus einer Ecke |
| `Diagonal Anzahl Streifen` | Wie viele diagonale Bänder laufen durch? | kein Band, die Flächen stoßen aneinander · 1 · 2 · 3 · 4 |

Gezählt werden **Bänder**, nicht Flächen — derselbe Begriff wie überall sonst im Dokument. Die
Flächenzahl folgt daraus: `Bänder + 2`, bei „kein Band" sind es 2. Das rechnet der Generator.

Die beiden Fragen zerlegen den Zweig in acht Sketches. Bhutan und Papua-Neuguinea haben beide zwei
Flächen ohne Band, sind aber gespiegelt und teilen sich deshalb keinen. Der dritte Richtungswert trägt
die Seychellen, deren Fächer weder steigt noch fällt — ohne ihn wären sie als „steigend" falsch
beschrieben.

Dieser Zweig hat lange gefehlt: Alle zwölf trugen dieselbe Signatur, und damit konnte der Generator
für keine von ihnen einen Sketch-Namen ableiten. Aufgefallen ist es beim Durchspielen von
Papua-Neuguinea, dessen Karte nach Frage 5 endete.

**Einfarbig** (35) und **speziell** (7) haben keine Folgefrage. Bei `speziell` trägt die gleichnamige
Spalte eine durchnummerierte Kennung, aus der der Sketch-Name folgt; Antigua und Barbuda etwa bekommt
einen fertigen Sketch nur zum Einfärben.

**Senkrechtes Band mit waagerechten Streifen** (6) fragt nur `SW Streifen` — wie viele waagerechte
Streifen neben dem Band liegen.

### Farben

**Acht, und immer alle acht zur Auswahl:**

> Rot · Blau · Hellblau · Grün · Gelb · Orange · Weiß · Schwarz

Die Zahl fällt aus dem Inhalt, nicht aus der MC-Breite. Mehr Farben erzeugen **mehr** Grenzfälle,
nicht weniger: Bei nur „Blau" liegt nichts dazwischen; erst ein zweiter Blauton macht Kasachstan und
Usbekistan zu Entscheidungen. Die sechs unstrittigen (Rot, Blau, Grün, Gelb, Weiß, Schwarz) sind im
Kern die heraldischen Tinkturen. Türkis, Karmin, Maroon, Braun, Grau und Lila werden eingefaltet —
jede zöge eine neue Grenze für ein bis drei Länder Gewinn.

**Zur Grenze Blau / Hellblau — gemessen am 26.08.2026.** Eine frühere Fassung dieses Abschnitts
behauptete, Hellblau bilde einen „echten, abgesetzten Cluster". **Das stimmt nicht.** Über die 78
Flaggen mit nennenswertem Blauanteil, sortiert nach Helligkeit:

```
18.2 … 43.5   64 Flaggen, dichtes Kontinuum, nirgends mehr als 2,5 Punkte Lücke
      ↓ 6.3
49.8 … 56.7    7 Flaggen   Gabun, Kongo, Tuvalu, Guatemala, Somalia, Palau, Estland
      ↓ 6.5
63.1 … 70.0    7 Flaggen   San Marino, Fidschi, Botswana, Dschibuti, Mikronesien, Argentinien, St. Lucia
```

Es gibt eine dichte dunkle Masse und danach einen ausgefransten Schwanz. **Und die Helligkeit allein
bildet die Wahrnehmung nicht ab:** Luxemburg (`#00A1DE`) und Ruanda liegen rechnerisch bei 43.5 und
damit im dunklen Block, wirken aber hellblau — Farbton 196 statt 210, cyanwärts und voll gesättigt.
Ein Zahlenkriterium hätte sie falsch einsortiert; als CheatSheet-Regel taugt es ohnehin nicht, weil
niemand beim Draufschauen Prozente schätzt.

**Die zwei Blaus bleiben trotzdem** — aber mit anderem Argument: Die sieben im Mittelband sind genau
die Population, für die §6 Punkt 8 die **„nicht falsch"-Option** vorgesehen hat. Fünfzehn hellblaue Flaggen
als „Blau" zu malen sähe an fünfzehn Stellen falsch aus; sieben Grenzfälle zu tolerieren kostet
nichts. Was diese Entscheidung kippen würde: wenn das Mittelband beim Einsortieren von Hand deutlich
wächst — sieben Ausnahmen sind ein CheatSheet, zwanzig sind eine kaputte Grenze.

Die Messung bleibt als **Werkzeug zum Füllen der Tabelle** nützlich, nicht als Regel fürs Lernen.

Ins CheatSheet gehören: **Gold = Gelb** und die zwei, drei echten Randfälle (Kasachstans `#00ABC2`
ist deutlich ins Cyan gezogen, wird aber als Hellblau geführt).

**Die Farben liegen in einer Spalte als Liste in Flächenreihenfolge** (`hellblau|weiß|schwarz|weiß|
hellblau`). Es gibt keine Frage „Farbe des linken Streifens" — die Skizze hebt eine Fläche hervor, in
zufälliger Reihenfolge, und dazu wird die Farbe gewählt. Also **eine** Fragedefinition, n-mal
angewandt. Damit braucht es auch keine Leserichtungs-Konvention.

**Streifenbreiten werden nicht abgefragt.** Wer Anzahl und Farben weiß, hat das Bild im Kopf und
bekommt die Breiten mit. Spart eine ganze Fragenebene.

### Zusatzelemente

**Am 27./28.08.2026 komplett umgebaut.** Die alte Fassung fragte über eine *Weiche* mit sechs Werten,
welche **eine** Kategorie die Flagge trägt, und hängte daran Behälter für Kreis und Gösch mit eigenen
Wertelisten. Daran ist sie gescheitert: Jede Flagge mit zwei verschiedenen Dingen — Kosovos Karte und
seine Sterne, Grenadas Muskatnuss und seine Sterne, Kiribatis Vogel und seine Sonne — musste in eine
Schublade gezwungen werden, und fast jede dritte landete im Auffangkorb. Von siebzehn Kreis-Flaggen
standen sieben falsch da, weil die Regel „Elemente in Kreisen zählen nicht" beim Ausfüllen niemand im
Kopf behält.

**Jetzt: eine flache Liste, eine Mehrfachauswahl.**

> *Welche Zusatzelemente siehst Du?*

Man hakt an, was da ist. Keine Kategorie, keine Behälter, keine Ausnahmeregel. 28 Wörter, 152 Flaggen
mit Element, höchstens vier pro Flagge (Brasilien: Raute, Schrift, Kreis, Stern).

```
75×  Keine          14×  Mond            2×  Drache        1×  Zahnrad, Machete
60×  Stern          13×  Kreis           2×  Gebäude       1×  Dreizack, Raute
36×  Wappen/Emblem   9×  Sonne           2×  Blatt, Zweig  1×  Nuss, Blume, Chakra
                     8×  Vogel           2×  Krone         1×  Baum, Hut, Löwe
                     4×  Muster          2×  Landumriss    1×  Schwert, Hammer, Sichel
                     3×  Kreuz, Schrift                    1×  Union Jack …
```

**Kreis und Gösch sind keine Behälter mehr, sondern Orte.** „Im Kreis" und „im Gösch" sind
Positionswerte wie „links oben" — damit fallen zwei Sonderstrukturen, zwei Wertelisten und die
Ausnahmeregel ersatzlos weg. Grenadas Sterne stehen einmal in der Liste, egal ob innen oder außen.

**Je Element vier Angaben**, in vier Spaltengruppen `Element 1` bis `Element 4`:

```
Element, Ort, Farbe, Anzahl

Abchasien   Hand, Gösch, Weiß        |  Stern, Gösch, Weiß, 7
Algerien    Mond, Mitte, Rot         |  Stern, Mitte, Rot, 1
Panama      Stern, Verteilt, Blau, Rot, 2
```

Die Felder erkennen sich **selbst**: Was in der Farbliste steht, ist eine Farbe; was aus Ziffern
besteht, ist eine Anzahl. Deshalb darf eine Figur auch mehrere Farben tragen (Panamas blauer und
roter Stern), ohne dass die Position der Felder etwas bedeutet.

**Leer heißt „wird nicht gefragt".** Bei der Anzahl heißt das zugleich „es ist eines" — bei Sternen
wird trotzdem die 1 eingetragen, weil dort die Frage *gestellt* werden soll. Bei der Farbe heißt es,
dass das Element keine eine Farbe hat.

**`bunt` als neunter Farbwert**, aber nur in der Elementfarbfrage. Ohne ihn verrät das Auftauchen der
Farbfrage, ob das Element einfarbig ist — bei `Vogel` unterscheidet das Albaniens schwarzen Adler von
Ugandas buntem Kranich, *bevor* man geantwortet hat. Für die Hintergrundflächen gehört `bunt` nicht in
die Liste: Eine Fläche ist per Konstruktion einfarbig, der Wert wäre kein Irrtum, den jemand hat,
sondern ein Kategorienfehler — und er nähme einen der acht Plätze weg, auf denen heute alle Farben
gleichzeitig stehen.

**`Keine` steht immer zur Wahl.** Fehlte die Option, verriete ihr Fehlen, dass etwas da ist —
derselbe Leak wie beim alten `x` in der Weiche.

**„Im Kreis" nur, wenn es einen Kreis gibt.** Das kostet nichts: Der Generator schreibt die
Optionsliste, also nimmt er den Wert nur auf, wenn `Kreis` unter den Elementen steht. Und ein Leak ist
es nicht — dass es einen Kreis gibt, hat man selbst zwei Fragen vorher geantwortet.

**Was bleibt offen:** die *Beziehung* zwischen Elementen. Algeriens Stern sitzt in der Öffnung der
Mondsichel; beide stehen auf „Mitte", und das sagt es nicht. Dafür gibt es noch `Formation Sterne` und
`Formation Mond mit Stern(en)` — die einzigen zwei Altspalten, deren Inhalt nirgends sonst steht.
Ebenso offen: zwei Elemente im selben Rasterfeld.

**Was der Umbau nicht löst:** die 36 Flaggen mit `Wappen / Emblem / Symbol`. Ein Wappen ist in einer
MC-Frage nicht lernbar, egal wie die Systematik aussieht — dafür sind die Geschichten-Karten da. Der
Umbau macht die **104 anderen** besser, die vorher mit ihnen in einem Topf lagen.


### Leak oder nicht

Ein Kriterium, das an mehreren Stellen gebraucht wird:

> **Ein Leak ist Information, die das System verschenkt** — eine Frage, die nur bei bestimmter Antwort
> auftaucht, oder eine Optionsliste, in der ein möglicher Wert fehlt.
> **Verengung durch die eigene richtige Antwort ist kein Leak.** Dass nach „3 Streifen" nur noch drei
> Farben zu nennen sind, ist der Zweck der Kette, nicht ihr Fehler.

## 4. Karten und Daten

**Ein Generator schreibt eine ganz normale Deck-CSV.** Kein Sonderweg in der Suite: Der Parser liest
Flaggenkarten wie jede andere Zeile — Id in Spalte 0, Tag, dann die Steps. Damit hat jede Karte
Identität, CardProgress und Level über die bestehende Maschinerie, und der Tag `Flagge` erlaubt im
Anki-Konfigdialog „gib mir 20 Flaggenkarten".

**Zwei getrennte Dateien:** eine generierte mit einer Zeile pro Land, eine handgeschriebene mit den
Geschichten-Karten. Die generierte wird nie von Hand angefasst.

Drei Gründe für den Generator statt eines Interpreters in der Suite:

- Die Suite bleibt dumm — kein Step-Expander, keine Ableitungslogik im Anwendungscode.
- Der Fragensatz wird sichtbar: Was eine Karte fragt, steht in der Datei und nicht im Ablauf.
- **`git diff` wird zum Prüfwerkzeug.** Fragensatz ändern, Generator laufen lassen, und der Diff
  zeigt Zeile für Zeile, welche Karten sich wie geändert haben.

Der letzte Punkt war der Ausschlag. Der Interpreter hätte den Fragensatz still über alle Karten
geändert, ohne dass es irgendwo aufgefallen wäre.

Die **Karten-Id steht explizit in Spalte 0**, nicht im Zeileninhalt. Der Lernfortschritt überlebt
deshalb jede Regenerierung — das ist die Bedingung, unter der ein Generator überhaupt trägt.

Die **Attribute** liegen in einer eigenen Tabelle, nicht in der Deck-CSV — zwei verschiedene Dinge.

### Was in die Attributtabelle gehört

> **Bleibt das Attribut wahr und sinnvoll, wenn man die Fragenstaffel wegwirft?**

`Dreiecksform = 2` besteht den Test: Kuwaits Figur ist ein Trapez, egal ob und wie danach gefragt
wird. Ein Attribut „Antwort auf Frage 5" besteht ihn nicht — es stimmt nur, solange Frage 5 genau so
geschnitten ist, und wird bei einem Umbau der Staffel still falsch, ohne dass man es an 206 Zeilen
merkt.

Die Tabelle beschreibt also die Flagge, die Fragenstaffel ist ein Weg dorthin, und **dazwischen liegt
eine Ableitung**: der Rahmen aus ≥2 waagerecht *und* ≥2 senkrecht *und* kein Kreuz, das
Dreieck-ja/nein aus der Dreiecksform, der Hintergrund daraus, welches Attribut gesetzt ist. Diese
Ableitung wohnt im Generator.

> Aufpassen: Im Hintergrund steckt eine **Priorität** — Dominica hat waagerechte *und* senkrechte
> Streifen *und* ein Kreuz, die Reihenfolge der Regeln entscheidet. Das ist Logik, nicht bloß
> Abbildung, und braucht dieselbe Sorgfalt wie die Ausschließlichkeit einer Antwortliste. Eine Regel
> lässt sich über alle 206 laufen und an der Zweigverteilung prüfen; eine Spalte müsste man 206-mal
> von Hand treffen.

**Distraktoren** brauchen keine eigene Spalte: Es wird zufällig aus der Werteliste gezogen, die
richtige Antwort immer dabei. Passen alle Werte in die MC-Breite (Farben, Hintergrundkategorien),
werden schlicht alle gezeigt — dann fehlt nie etwas und es gibt nichts zu schließen. Wo es mehr sind
(neun Bänderzahlen), wechseln die gezogenen Ablenker von Durchgang zu Durchgang, was denselben Zweck
erfüllt. Das ist der Mechanismus, den die bestehenden MC-Decks schon nutzen.

Reines Ziehen hat aber eine Schwäche, die bei einem **großen** Vokabular zuschlägt: Je mehr Werte es
gibt, desto **leichter** wird die Frage, weil die Nachbarn, an denen man scheitern könnte, meistens
gar nicht gezogen werden. Wer bei einer Sternfrage Papagei, Wappen und Landkarte danebenstehen sieht,
muss nichts unterscheiden. Deshalb kann eine Zeile seit der neuen Syntax (§6) festlegen, **wer immer
danebensteht** — Mond und Sonne — und nur den Rest ziehen lassen. Nachbarschaft garantiert, Variation
erhalten.

**In den Flaggenkarten gibt es keine Input-Steps**, alles läuft über MC. Akzeptierte Schreibweisen
sind damit kein Thema.

## 5. Renderer und Screen

**Ein GeoJSON pro Struktur, nicht pro Land.** Flächen mit 0…n−1 nummeriert, die Farben stehen in der
Attributtabelle in Flächenreihenfolge. `waagerecht-3` wird von Deutschland, Österreich und vielen
anderen gemeinsam benutzt. Die Dateien liegen in `data/sketches` — nicht bei den Flaggen, denn weder
die Komponente noch die Steps wissen etwas von Flaggen.

**Zwei Flaggen können sich nur dann einen Sketch teilen, wenn sie gleich viele Flächen haben.** Die
Farbliste ist positionsbezogen — n Farben für n Flächen. Optische Ähnlichkeit reicht also nicht:
Simbabwe und Osttimor haben beide ein zweifarbiges Dreieck, aber neun gegen drei Flächen.

### Was in die Datei gehört und was dazukommt

> **Was vor dem Erscheinen der Skizze gefragt wurde, darf in die Datei. Was danach gefragt wird,
> kommt danach als eigener Schritt dazu.**

Die Skizze erscheint nach Frage 5. Gösch (Frage 3) und Dreieck (Frage 4) sind da beantwortet — sie zu
zeigen verrät nichts, es zeigt die eigene Antwort zurück. Die Zusatzelemente werden nach den Farben
gefragt und bleiben deshalb draußen.

**Gösch und Dreieck werden also in die Sketch-Dateien hineingezeichnet, nicht zur Laufzeit
darübergelegt.** Gemessen kostet das **13 Dateien**: 63 statt 50. Dafür bleibt die Suite dumm — kein
Dreieck, kein Gösch, keine Geometrie im Anwendungscode —, das Ergebnis ist einsehbar statt im Ablauf
versteckt, und `git diff` zeigt bei einer Änderung der Dreieckstiefe Datei für Datei, was sich bewegt
hat. Es ist dieselbe Entscheidung wie Generator statt Interpreter in §4, eine Ebene tiefer: Die
Geometrie ist in beiden Fällen dieselbe Rechnung, die Frage ist nur, ob sie einmal läuft oder jedes
Mal.

Verworfen wurde damit auch die Gegenrichtung: *alles* backen, die Elemente eingeschlossen. Weil jede
Stufe alles Vorherige mitbringen müsste, wären das bis zu **206 × 3** Dateien — und die Karte müsste
mitten im Durchlauf eine neue Datei laden und die bereits gegebenen Farbantworten wieder ausschreiben.
Diese nachgeschobenen Füllungen wären keine Antworten mehr, sondern Wiederherstellung, und die Zeile
läse sich nicht mehr als Protokoll des Dialogs. Genau das wollte §4 mit dem Generator gewinnen.

Für alles, was in die Datei kommt, gilt weiterhin: **immer dieselbe Gösch-Geometrie, immer dieselbe
Dreieckstiefe, keine Anpassung an die Streifenzahl.** Der Grund ist derselbe wie beim Leak: Ein
Sketch, der Details richtig zeigt, die nie beantwortet wurden — Chiles Gösch genau über dem oberen
Streifen, Togos genau über dreien von fünf —, verrät sie. Die Skizze soll die Flächen sauber zum
Einfärben zeigen, nicht die Flagge möglichst getreu abbilden.

Und für alles, was dazukommt, steht bisher **eine halbe Regel**:

> **Die Skizze darf nie mehr zeigen, als beantwortet wurde.** Ein Stern ist *ein* kanonischer Stern,
> überall derselbe — „Zacken zählen" ist verworfen (§9), also darf die Zeichnung die Zackenzahl nicht
> zeigen.

Die Gegenrichtung ist **ausdrücklich noch nicht entschieden.** Weniger zu zeigen als beantwortet ist
klar in Ordnung — bei sechzehn Sternen einen Haufen zu malen statt sechzehn abzuzählen verrät nichts
und behauptet nichts; das ist gerade die Schematik. Unklar ist der Fall darunter: Darf die Skizze
etwas *anderes* zeigen als die Antwort? Eine nach rechts offene Sichel, wo `Nach oben offen`
beantwortet wurde, ist streng genommen ein Widerspruch — und trotzdem womöglich egal, weil die Skizze
nie beansprucht hat, die Flagge abzubilden.

**Das wird am laufenden Bild entschieden, nicht auf dem Papier**, und bis dahin gilt hier
Ambiguitätstoleranz statt eines Gesetzes. Der konkrete Fall: `Formation Mond mit Stern(en)` trägt drei
Werte — `Nach rechts offen` (8), `Nach oben offen` (1), `Nach rechts oben offen` (1). Drei
Sichel-Dateien, ein Winkel im Step, oder eine Sichel ohne Richtung: alle drei bleiben offen.

> **Achtung, Quelle:** §9 ist an mehreren Stellen älter als die Zusatzelemente-Staffel vom 25.08. und
> in Einzelfällen von ihr überholt — „Sonne und Stern nicht unterscheiden" ist das Beispiel. Was das
> Blatt sagt, sticht.

**Ob der Union Jack gezeichnet wird, entscheidet Australien.** Der Gösch trägt sechs Werte —
`Union Jack` (7), `Stern` (3), `Kreuz` (2), `Sterne` (2), `Sonne` (2), `Mond + Stern` (1) —, und fünf
davon sind einfache Formen, die ohnehin gezeichnet werden. Der Union Jack ist der einzige Ausreißer.

Dafür, ihn zu zeichnen, spricht das Kriterium selbst: Aus der Antwort `Union Jack` folgt sein
Aussehen vollständig, Farben eingeschlossen — ihn zu zeigen verrät nichts, sondern ist §2s „eine
Frage entfällt, wenn ihre Antwort zwingend folgt". Dagegen spricht nur, dass er das einzige
realistische und das einzige vorgefüllte Stück der Skizze wäre. Ein **stilisierter** Union Jack —
die drei Kreuze ohne Fimbrierung und ohne die versetzten Diagonalen — ist der dritte Weg und
vermutlich der beste.

**Ausgeschlossen ist die dritte Variante:** ein generischer Platzhalter im Gösch. Der zeigte *weniger*
als die schon gegebene Antwort, und das ist in beide Richtungen falsch. Also der echte oder keiner.

**Die Dreieckstiefe ist einheitlich, etwa 40 % der Breite.** Tschechien reicht mit 50 % am weitesten,
die Philippinen und Dschibuti liegen bei gut 43 %, Bahamas, Jordanien und Sudan bei etwa einem
Drittel. Die Ausreißer haben eigene Formwerte: Eritreas Keil läuft bis zur Flugseite durch, Kuwaits
Figur ist ein Trapez, Südafrika und Vanuatu tragen ein liegendes Y.

### Leinwand und Raster

**Die Skizze ist schematisch, nicht formattreu.** Das Seitenverhältnis ist fest **3:2** — häufigstes
echtes Flaggenformat und das Format, für das die Skin-Maße gedacht sind. Der Kontrakt: *In diesem
Verhältnis sind Kreise Kreise; in einem anderen wird eingepasst und mittig gesetzt.* Das
`SuiteImage` darf jede Größe haben; wer Verzerrung vermeiden will, hält 3:2 ein.

**Alle Sketch-Dateien benutzen dieselbe Leinwand: 180 × 120**, also x ∈ [0, 180] und y ∈ [−120, 0]
(Y negativ wie in den Kartendateien, beim Laden invertiert). Das ist keine Kosmetik, sondern
Voraussetzung: `SketchPane` rechnet den Maßstab aus der Bounding Box der geladenen Flächen — bei
verschieden großen Leinwänden säße eine hinzugefügte Elementdatei falsch. Die Zahl selbst ist frei
wählbar (`3 × 2` täte es auch); 180 × 120 ist gewählt, weil dann auch die Rasterfelder rund sind.

**Das 3×3-Raster ist die Platzierungskonvention**, Feld 60 × 40, Nummerierung zeilenweise 0…8. Es ist
dieselbe Einteilung, die die Positionsfrage der Zusatzelemente ohnehin schon benutzt (§3) — Bild und
Frage teilen sich die neun Felder.

**Der Gösch belegt genau Feld 0.** Damit ist er in jeder Datei buchstäblich dieselben vier Punkte, und
die Auflage „immer dieselbe Gösch-Geometrie" ist nicht mehr zu befolgen, sondern nicht mehr
verletzbar. Er wird dadurch kleiner als in Wirklichkeit — echte Göschs sind eher halbe Breite —, und
das ist die Schematik, die dieser Abschnitt ohnehin will. Der Gewinn ist, dass ein Element *im* Gösch
ohne Sonderfall auf Feld 0 sitzt.

Die **Dreieckstiefe bleibt bei 40 %** und wird nicht aufs Raster gezogen. Ein Drittel läge am unteren
Rand des beobachteten Bereichs (33 bis 50 %), und das Dreieck ist gebacken — es braucht das Raster
für nichts.

**Ganzzahlige Streifengrenzen gehen damit nicht mehr für alle auf.** Das kgV der vorkommenden
Streifenzahlen ist 180 180; eine Leinwand, auf der alle aufgehen, gibt es praktisch nicht. Bei 120
bleiben 2 bis 6 rund — fast alle —, Dezimalstellen bekommen Simbabwe, Griechenland, Uruguay, Liberia,
die USA und Malaysia. Harmlos: `SketchFileSource` liest ohnehin `double`, in diese Dateien sieht
niemand hinein, und der Generator schreibt für Unterkante *i* und Oberkante *i+1* denselben Wert, es
kann also keine Lücke entstehen.

**Die reinen Streifenstrukturen werden erzeugt, nicht gezeichnet** —
`docs/flaggen/build-streifen-sketch.py` schreibt `waagerecht-<n>` und `senkrecht-<n>`. Das deckt 78
der 103 Flaggen im Waagerecht-Zweig ab; Dreieck, Gösch, Kreuz und die Sonderfälle bleiben Handarbeit.

Die echten Flaggen werden hochaufgelöst im wahren Format geladen und wie jedes andere Bild
verkleinert und mittig platziert.

**Bildbeschaffung — hier fehlt noch etwas.** Im Bilderordner liegen 200 Flaggen als
`*-flag-square-small.png`, und die sind **quadratisch gerendert**, also gestaucht. Fürs MC-Deck ist
das richtig, fürs Flaggen-Deck unbrauchbar: Die Karte zeigt am Ende eine falsche Flagge.

Es braucht also formattreue Bilder. SVG herunterladen, einmal groß als PNG exportieren (großzügig,
etwa 1500 px breit; später neu zu exportieren hieße, 200 Dateien anzufassen), und die App lädt PNG
wie bisher. Die **SVG-Quellen bleiben liegen**, dann kostet ein hochauflösender Skin später nur einen
Skriptlauf. `fxsvgimage` zur Laufzeit ist zugesagt zum Ausprobieren — das offene Risiko dabei ist der
Textanteil der Commons-SVGs (Brasiliens Spruchband, Saudi-Arabiens Schrift, Belizes Wappen).

`SuiteImage` passt ein Bild inzwischen **mittig ein statt es zu strecken**. Für die bestehenden Decks
ist das folgenlos — von 3131 Bildern im Lernordner ist genau eines nicht quadratisch, und das um
einen Pixel.

**Farbeinordnung nach dem Bild, das im Deck liegt** — nicht nach offiziellen Werten. Viele Länder
legen ihre Farben gar nicht fest (Usbekistan: nur „azurblau"), Pantone hat keine eindeutige
RGB-Entsprechung, und die Wikipedia-Farbtabellen widersprechen teils den dortigen SVGs (Mikronesien:
Tabelle `#ABCAE9`, Bild `#75B2DD`). Antwort und Augenschein müssen zusammenpassen, also entscheidet
das verwendete Bild. Alle 200 aus **einer** Quelle ziehen, damit Vergleiche belastbar bleiben —
Wikimedia Commons oder ein Sammelpaket wie `lipis/flag-icons`. Offizielle Werte und
Farbwechsel-Geschichten sind Stoff für Geschichten-Karten, kein Eingabewert.

**Ablauf einer Karte:** Struktur einschließlich Gösch und Dreieck (ohne Bild) → Skelett mit
hervorgehobener Fläche erscheint → Farben füllen sich Fläche für Fläche → die Zusatzelemente werden
gefragt und **einzeln dazugezeichnet** → zum Abschluss die echte Flagge.

**Zusatzelemente werden mitgezeichnet.** Japans Kreis markieren und einfärben zu lassen ist dieselbe
Mechanik wie bei einer Streifenfläche, und fünf Elementfragen ohne sichtbares Feedback wären der
einzige Teil der Karte, wo nichts passiert. Die Grenze zieht die Elementfrage selbst: Was einen Namen
trägt, aus dem die Form folgt (Kreis, Zackenfigur, Sichel), wird gezeichnet; was „komplexes Emblem"
heißt, nicht — dort sagt die Antwort ausdrücklich, dass nicht zerlegt wird. Es gilt dieselbe Auflage
wie für den Gösch: immer dieselbe Platzhalterform, nie die echte. Der `Union Jack` ist der eine Fall,
der zwischen beiden liegt (siehe oben).

Auch **„über die ganze Flagge verteilt" wird gezeichnet**, und zwar als drei Marker in *immer
denselben* drei Feldern. Die Antwort sagt ja etwas — mehrere Figuren, verstreut —, und das zu zeigen
berichtet sie, statt sie zu erfinden. Dass die Felder fest sind, hält es davon ab, als Australiens
echte Anordnung gelesen zu werden.

Eine falsche Antwort beendet die Karte, wie überall sonst in der Suite. Die Skizze bleibt dann
halbfertig stehen; sie baut sich also nur bei einem fehlerfreien Durchlauf ganz auf.

### Wie es gebaut ist

Vier Steps, bewusst getrennt, damit der Ablauf vollständig in der CSV-Zeile steht und kein Schritt
etwas über seine Nachbarn wissen muss:

```
SketchImage:<struktur>            laden, alle Flächen leer — und zugleich das Zurücksetzen
SketchImageAdd:<struktur>,<feld>  eine weitere Datei anhängen, ohne zurückzusetzen
SketchImageMove:<n>,<feld>        eine vorhandene Fläche in ein anderes Rasterfeld setzen
SketchImageMark:<n>               Fläche hervorheben
SketchImageFill:<n>,<Farbname>    Fläche einfärben, läuft nach dem MC
```

**`SketchImageAdd` ist das „Drübermalen"** — und es ist ausdrücklich *keine* Geometrie zur Laufzeit.
Es lädt eine zweite Datei und hängt ihre Flächen an, verschoben in Rasterfeld `<feld>` (0…8). Jede
Datei nummeriert für sich ab 0, beim Anhängen wird um die bisherige Flächenzahl verschoben; die
Elementdatei liegt kanonisch in Feld 0, mit etwas Luft ringsum. Platzieren ist damit eine
ganzzahlige Verschiebung, keine Skalierung und keine Figur.

Drei Dinge fallen dadurch weg oder werden billig: Aus dem Produkt „63 Hintergründe × Elemente" wird
eine **Summe** — `kreis`, `stern`, `sichel` werden von allen Hintergründen benutzt, so wie
`waagerecht-3` von Deutschland und Österreich. Die Reihenfolge der Auflagen **steht in der Zeile**
statt in einer Rangfolge-Konvention. Und ein neuer Elementtyp ist eine neue kleine Datei.

**Eine Elementdatei kann mehr als eine Fläche tragen.** Grönlands geteilter Kreis sind zwei
Halbscheiben in einer Datei, und `Add` hängt beide an.

**`Move` gibt es, damit ein Element sofort erscheint und erst danach an seinen Platz rückt.** Sonst
laufen zwei bis drei Elementfragen ohne jedes sichtbare Feedback ab, und die Figur taucht erst nach
der Positionsfrage auf. Also: `Add` in die Mitte, sobald die Form bekannt ist, dann die Frage nach
dem Ort, dann `Move`. Die Lage ist deshalb eine **Verschiebung des Knotens** statt eingebackener
Koordinaten — Setzen und Umsetzen sind derselbe Vorgang, und eine Translation zieht die Strichbreite
nicht mit.

> **Der `Move`-Schritt wird immer geschrieben, auch wenn er nichts bewegt.** Stünde er nur bei
> Elementen, die nicht in der Mitte sitzen, verriete allein sein Vorhandensein, dass die Antwort
> nicht „Mitte" lautet. Dieselbe Regel wie beim Gösch.

Er kennt Flächen, keine Elemente: Ein zweigeteilter Kreis braucht zwei `Move`-Schritte.

**Dass die Fragen dafür je Zweig anders geordnet sind, ist kein Problem** — das Blatt war nie die
Fragenreihenfolge. §2 sagt es ausdrücklich: Zwischen Attribut und Frage liegt eine Ableitung, und die
wohnt im Generator. Er darf `Wo?` im Kreis-Zweig früh und im Figuren-Zweig spät stellen; die Spalte
bleibt im Blatt da stehen, wo sie sich am besten liest. Weder `pruefe-attribute.py` noch die
HTML-Seite hängen an der Spaltenreihenfolge.

Gezeichnet wird in den Szenengraph, nicht in ein fertiges Bild: `SketchPane` (paketprivat, Innenleben
von `SuiteImage`) baut je Fläche ein `Shape`. Eine Fläche hat drei Zustände — frisch umrandet und
ungefüllt, markiert, gefüllt. **Gefüllt verliert sie ihren Strich**, damit zwei benachbarte Flächen
derselben Farbe am Ende nahtlos verschmelzen statt eine erfundene Naht zu zeigen.

**Kreise sind `Point` plus `radius` in den `properties`.** GeoJSON kennt keinen Kreis — der `Point`
ist echtes GeoJSON, die Kreisbedeutung ist unsere Konvention (dieselbe, die Leaflet benutzt). Der
Preis: In QGIS ist ein Kreis nicht zu zeichnen und nicht zu sehen, er wird getippt oder erzeugt.
Der Gewinn: exakt rund in jedem Maßstab statt eines 64-Ecks. `ShapeGeometry` trägt Mittelpunkt und
Radius bereits, `scaled()` nimmt beide mit.

Gezeichnet wird ein JavaFX-`Circle`, derselbe Knotentyp, den der `MapNodeBuilder` für Stadtpunkte
baut. Die Sichel ist **Kreis minus versetzter Kreis** über `Shape.subtract` — echte Bögen, keine
Näherung, und in der Datei ein `cutout` in den `properties`. Das bleibt im selben Vokabular, weil
eine Sichel wirklich ein Kreis minus einem Kreis *ist*; ein SVG-Pfad-String wäre eine Kodierung statt
einer Beschreibung.

**Der Startzustand steht als eine Regel im Skin**, nicht im JavaFX-Standard. Das ist die Bedingung
dafür, dass Formen unterschiedlicher Art nebeneinander liegen dürfen: Ein `Path` startet ungefüllt,
ein `Circle` schwarz. Seit `.my-sketch-area` die Füllung ausdrücklich auf transparent setzt, kann
jede neue Form dazukommen, ohne dass man ihre Vorgabe kennen muss. Die Farbregeln stehen als
Zwei-Klassen-Selektoren darüber und können mit der Basisregel nicht kollidieren.

Skaliert werden die **Koordinaten**, nicht der fertige Node — eine Transformation würde die Striche
mitwachsen lassen. Der Faktor gilt für beide Achsen gleich, die Skizze wird also eingepasst und nie
verzerrt. Sie fällt dabei absichtlich eine Spur zu groß aus, damit der Clip des Bilderrahmens ihre
äußere Kontur ganz wegschneidet: Sonst stünde ein eckiges Rechteck in einem Rahmen mit runden Ecken.
Kanten, die nicht anstoßen, bleiben stehen — sie sind die echte Kante der Skizze.

Die acht Farben liegen als Vorgaben an den Feldern in `SkinProperties`; keine Skin-Datei muss sie
kennen. Strich und Markierung leiten sich dagegen aus den Skin-eigenen Farben ab.

**Kein Klick ins Flaggenbild.** Alles läuft über MC und Input; das Bild bleibt reine Anzeige.

**Screen:** eigener Skin-Präfix `flagSession*`. Die Maße werden seit dem Staffelungs-Umbau über drei
Stufen aufgelöst — `deckId` → `mapName` → Kategorie. Weil das Deck `mapName = "world"` trägt, erbt es
Kartenbilder, Wallpaper und jedes noch nicht eigens gesetzte Maß vom Weltdeck; eingetragen werden muss
nur, was anders sein soll. Die Weltkarte bleibt im Deck, weil die Geschichten-Karten geografische
Fragen stellen werden.

Vorgesehener Layoutstand (noch nicht eingetragen):

```
MapPanel      = 790,20,1100,800
ImagePanel    = 20,20,750,500      # 3:2, Skizze randlos
QuestionPanel = 410,547,360,370
McPanel       = 20,547,370,0
```

## 6. Erweiterungen der Suite

**Gebaut:**

1. ~~Step-Expander `Flag:`~~ — **entfällt**. Der Generator schreibt die Steps aus, der Parser bleibt,
   wie er ist.
2. **Skizzen-Renderer** — `SketchPane` in `shared.ui.components`, paketprivat, Innenleben von
   `SuiteImage`. Kennt keine Flaggen: Teilflächen rein, markieren, einfärben.
3. **Drei Skizzen-Steps** — `SketchImage`, `SketchImageMark`, `SketchImageFill`. Reine Anzeige-Steps,
   gefragt wird daneben per MC.
4. **SuiteImage: mittig einpassen statt strecken** — erledigt, und wie erwartet ein No-Op für die
   bestehenden Decks.

5. **`SketchImageAdd` und `SketchImageMove`** — anhängen ohne Zurücksetzen und umsetzen ohne
   Neubauen. `SketchPane` merkt sich den Maßstab vom ersten Laden, statt ihn neu zu rechnen: Sonst
   schrumpfte eine Elementdatei, die versehentlich über den Rand ragt, nachträglich die ganze Skizze.
6. **Kreise in der Skizze** — `SketchFileSource` liest `Point` + `properties.radius`, `SketchPane`
   hält `Map<Integer, Shape>` und baut Polygon oder `Circle`. Der ungefüllte Startzustand steht jetzt
   ausdrücklich im Skin statt im JavaFX-Standard — die Bedingung dafür, dass zwei Formfamilien
   nebeneinander liegen dürfen.

Dazu, nicht ursprünglich geplant: eine **dritte Staffelungsebene** für die Skin-Maße
(`deckId` → `mapName` → Kategorie). Ohne sie hätte das Flaggen-Deck seine Maße aus zwei verschiedenen
Schlüsseln gelesen — der `!Sofort`-Marker in `AnkiLearnView` hatte genau das vorhergesagt.

**Noch offen:**

7. **Input mit Enter-Bestätigung** — der heutige Input prüft nach jedem Tastendruck und verrät damit
   die Antwort, sobald der Antwortraum klein und durchprobierbar ist. Wird gebraucht, weil der
   Kartenklick für die Gegenrichtung ausscheidet (siehe §8).
8. **Neue MC-Syntax: eine flache Liste mit Präfixen.** Siehe unten — sie ersetzt die Abschnitte und
   bringt die „nicht falsch"-Option und den Ablenker-Vorrat gleich mit.
9. **Echte Mehrfachauswahl** — eine an- und abhakbare Liste, die als Ganzes abgeschickt wird. Siehe
   unten; sie ist der Grund, warum es das `?`-Präfix überhaupt gibt.

### Die MC-Syntax

Heute trennt ein `*` die richtigen von den falschen Antworten:

```
MC:Blau|Weiß*Rot|Grün
```

Das trägt genau **eine** Eigenschaft je Option — richtig oder falsch. Gebraucht werden aber **zwei,
und sie sind unabhängig voneinander:**

```
Rolle   richtig · toleriert · falsch
Rang    immer zeigen · nur ziehen, wenn Platz ist
```

Abschnitte können das nicht: Jede weitere Eigenschaft wäre ein weiteres Sternchen, leere Abschnitte
müsste man abzählen, und ihre Reihenfolge müsste man auswendig wissen. Deshalb wandern die
Eigenschaften **an die Option**, und die Abschnitte entfallen:

```
(nichts)   falsch, immer zeigen
+          richtig
~          toleriert — zählt nicht als Fehler, ist aber auch nicht die gesuchte Antwort
?          falsch, Vorrat — wird gezogen, solange Plätze frei sind
```

```
MC:Blau|Weiß*Rot|Grün              →   MC:+Blau|+Weiß|Rot|Grün
MC:+Ja|Nein
MC:+Weiß|Rot|Blau|Hellblau|Grün|Gelb|Orange|Schwarz
MC:+2|~1|3|4|5                         Lesotho: 2 ist gesucht, 1 wird nicht als Fehler gewertet
MC:+Stern|+Mond|Sonne|Kreuz|?Papagei|?Wappen|?Landkarte|?Hand
```

**Was die Präfixe können, was Abschnitte nicht konnten:**

- **`~` ist die „nicht falsch"-Option** aus Punkt 8. Sie ist ein *Zustand* einer angezeigten Option,
  kein eigener Rang — deshalb passte sie nie in einen Abschnitt.
- **`?` trennt Vorrat von Nachbarschaft.** Ohne Präfix steht eine Option *immer* da. Damit lässt sich
  garantieren, dass bei einer Sternfrage Mond und Sonne danebenstehen, statt es dem Zufall zu
  überlassen — und der Rest wechselt trotzdem von Durchgang zu Durchgang, wie §4 es will.
- Reihenfolge egal, nichts zu zählen, kein leerer Abschnitt. Eine fünfte Eigenschaft wäre irgendwann
  ein fünftes Zeichen und kein Umbau.

**Die Umstellung ist mechanisch:** `a|b*c|d` wird zu `+a|+b|c|d`. Ein Skript über die Deck-Dateien,
zwanzig Minuten. Die Trennzeichen `;` und `:` bleiben tabu — das erste zerlegt die CSV-Zeile, das
zweite trennt Step-Name und Rumpf.

**Bewusst weggelassen:** eine dritte Ablenkerstufe („Reserve", die erst zieht, wenn der Vorrat leer
ist). Konstruierbar, aber kein Fall in Sicht — wäre später ein weiteres Präfix.

### Echte Mehrfachauswahl

Der heutige MC ist keine Mehrfachauswahl, sondern eine Reihe **unwiderruflicher Einzelklicks**: Jeder
Klick wird sofort gewertet, ein falscher beendet die Karte. Ein Button tut genau das — das ist kein
Mangel, sondern seine Natur. Was fehlt, ist etwas anderes: eine **Liste, die man an- und abhakt und
dann abschickt.**

**Wozu.** Bei „welche Elemente liegen auf der Flagge" muss man aufzählen, was man sieht. Als
Einzelklicks zerfällt das in sieben Ja/Nein-Fragen, bei denen jede Antwort sofort verraten wird und
die erste falsche abbricht. Als eine Liste ist es **ein Abruf**, bei dem nichts durchsickert, bevor
man sich festgelegt hat. Das ist der Unterschied zwischen Wiedererkennen und Erinnern — und damit
genau der Grund, aus dem der Generator überhaupt gebaut wird.

**Der Platz.** Das Antwortfeld ist beim Flaggen-Deck so voll wie beim Weltdeck, dem vollsten
überhaupt. Ein zweites Element passt nicht. Also ist es **kein zweites Element, sondern ein Modus des
vorhandenen**: Die Optionen bekommen ein Häkchenfeld, der Button feuert nicht mehr, sondern hakt an.
Keine neue Fläche, kein zusätzlicher Platzbedarf.

> **Auflage, die dabei gilt:** Komponenten erscheinen und verschwinden in dieser Suite nicht, sie
> wechseln nur zwischen aktiv und inaktiv. Das Kästchen gehört zum *Inhalt* eines Buttons — so wie
> sein Text, der ohnehin bei jeder Frage wechselt —, nicht zum Bestand der Komponente. Das Antwortfeld
> bleibt, was darin steht, ändert sich.

**Das ist das übliche Muster, nicht ein Sonderweg.** Google Photos öffnet ein Bild — bis eines
ausgewählt ist, dann selektiert derselbe Klick. Gmail macht dasselbe über den Avatar, Android über
langes Drücken, der Windows-Explorer über eine Option. Überall dort erscheint der Auswahlmarker beim
Betreten des Modus; permanent sichtbar wäre er eine Aufforderung zur falschen Interaktion.

**Auswertung: alles oder nichts.** Fehlende und zu viel angehakte Optionen zählen gleichermaßen als
falsch — `MultipleChoiceAnswers.isFinallyCorrect` macht das heute schon per Mengenvergleich. Beim
Aufdecken werden die **beiden Fehlerarten getrennt** gezeigt: was gefehlt hat und was zu viel war. Das
ist der eigentliche Lernmoment und der Punkt, an dem sich der Typ vom Buzzer unterscheidet.

**Die Anzahl wird nie genannt.** „Wähle alle zutreffenden", nicht „wähle zwei" — sonst beantwortet die
Zahl die halbe Frage.

**Zum Namen.** Der heutige Typ heißt `MC`, ist aber keine Mehrfachauswahl. Sein Kennzeichen ist die
Unwiderruflichkeit pro Klick — **`Buzzer`** träfe es. Dann wird `MultipleChoice` frei für den, bei dem
der Name zum ersten Mal stimmt. Preis: `Card.MC`, `MultipleChoiceAnswers`, `mcPane`, die
Presenter-Methoden — und der Step-Name in jeder bestehenden Deck-Zeile, also Daten und nicht nur Code.

**Offen:** Ob abgeschickt wird per Knopf (immer sichtbar, meist inaktiv) oder per Enter. Enter spart
Fläche, verlangt aber Wissen — wie das ESC beim heutigen Input, das auch niemand sieht.

**Vertagt, ausdrücklich nicht für dieses Deck:** Eine falsche Antwort beendet die Karte, und damit
bleibt das Auflösungsbild ungesehen. Bei den über 10 000 bestehenden Karten hat das nie gestört, bei
den Flaggen stört es — man will die Flagge sehen, gerade wenn man sie nicht wusste. Das ist eine
Änderung am Anki-Kartenablauf und betrifft alle Decks; sie wird **nach** den Flaggen angefasst, nicht
nebenbei für einen Sonderfall.

## 7. Reihenfolge

**Der Bau kam zuerst.** Kein halbes Jahr Datensammeln auf Verdacht: Eine heute angelegte Karte soll
morgen drankommen, und ein Modellfehler soll auffallen, solange er billig zu ändern ist. Dafür musste
vor dem ersten Land die ganze senkrechte Scheibe stehen — Fragensatz für waagerecht, Strukturdatei,
Steps, Renderer, SuiteImage-Einpassung, Skin-Layout. Eine billigere Teilvariante gab es nicht.

**Der Durchstich ist gemacht — mit einer Flagge statt fünf.** Deutschland läuft als handgeschriebene
Zeile in der Deck-CSV durch alle Schichten: Strukturdatei, Steps, Renderer, Skin-Layout, echte Karte
in der App. Eine Flagge reichte, weil die blockierenden Fragen Pipeline-Fragen waren und die sich
schon am ersten Fall zeigen.

Was der Durchstich beantwortet hat: der Renderer zeichnet in den Szenengraph; die Skizze sitzt im
Bilderrahmen und teilt sich dessen Feld mit dem Bild; die Steps stehen ausgeschrieben in der Zeile.

**Die Zweige sind entworfen und attributiert.** Waagerecht, senkrecht, Kreuz, diagonal, einfarbig,
das senkrechte Band mit waagerechten Streifen und die Spezialfälle — jede der 206 Flaggen kommt bis
zu ihrem Sketch durch. Das war die Bedingung für den Generator: Er liest die Attributtabelle, und
solange die noch Spalten dazubekommt, baut man ihn zweimal.

**Die Zusatzelemente sind vorgezogen worden.** Ursprünglich sollte direkt nach den Attributen der
Generator kommen. Stattdessen wurde erst die zweite Fragenstaffel gebaut, und der Grund war der
richtige: Am Ende kommt alles von den Fragen. Bis zum Sketch war die Kette klar, danach fing sie neu
an — und ein Generator, dem die Hälfte seiner Eingabe fehlt, wird zweimal gebaut.

Dabei ist nebenbei der diagonale Zweig entstanden, der vorher gar nicht auflöste, und vier Flaggen
sind aufgefallen, die in keiner Elementzeile standen (Japan, USA, St. Lucia, Schweiz).

**Der Skizzen-Mechanismus ist entschieden** (§5): Gösch und Dreieck gebacken, Elemente über
`SketchImageAdd`, festes 3×3-Raster auf einer gemeinsamen Leinwand. Damit sind auch die Farblisten
nicht mehr blockiert — die Flächennummerierung steht in der Datei, wie sie es immer tat, und die
Auflagen hängen sich hinten an.

**Als Nächstes: die vier Härtefälle auf Papier.** Bevor Code entsteht, werden vier Flaggen von Hand
durchgeschrieben — die GeoJSON-Datei und die CSV-Zeile —, weil sie zusammen alles anfassen, was neu
ist:

| Fall | Was er prüft |
|---|---|
| **Japan** | Der einfache Kreis: `Point` + `radius`, eine Fläche, eine Farbfrage. |
| **Grönland** | Der geteilte Kreis, zwei Halbscheiben — eine Elementdatei mit mehr als einer Fläche. |
| **Tunesien** | Sichel *und* Stern *im* Kreis: `Shape.subtract`, dazu der Behälter mit geerbten Folgefragen. |
| **Australien** | Gösch-Inhalt und „über die ganze Flagge verteilt", dazu die längste Karte des Decks. |

**Die unbequemen Fälle** — beim Generator die Kandidaten, die man zuletzt anfasst. Sie stehen
nirgends als Liste, sie ergeben sich: `Hintergrundtyp = 7`, dazu `Dreieck von links? ∈ {2, 3}`
(Kuwaits Trapez, das liegende Y von Südafrika und Vanuatu), `Rechtwinklig? = 0` (Nepal) und Grenada
mit seinen Symbolen auf dem Rahmen.

Australien entscheidet dabei die letzte offene Frage des Mechanismus: **ob der Gösch selbst eine
Farbfrage bekommt** — davon hängt ab, ob er eine füllbare Fläche ist oder nur eine Teilung.

**Danach der Generator.** Er schreibt beides: die Sketch-Dateien und die Deck-CSV. Und er bekommt
einen **Trockenlauf** — jede erzeugte Zeile wird durchgespielt, der Flächenstand mitgeführt und
geprüft, dass jeder `Mark`- und `Fill`-Index zu diesem Zeitpunkt existiert. Das ist der Preis dafür,
dass die gültige Flächennummer mit `Add` eine Summe über alles bisher Geladene ist: Ein Rechenfehler
fiele sonst nicht beim Parsen auf, sondern mitten in der Lern-Session. Die Prüfung gehört neben
`pruefe-attribute.py`, nicht in den Bau.

**Pro neuer Sketch-Familie eine handgeschriebene Karte.** So wie Deutschland: eine Zeile, ein Sketch,
einmal durchspielen. Nicht pro Zweig — allein waagerecht hat drei Familien (nur Streifen, mit
Dreieck, mit Gösch), und nur die erste ist bisher belegt.

**Der Renderer wird von Anfang an generisch über GeoJSON gebaut**, auch wenn am ersten Tag nur
`waagerecht-3` gebraucht wird. Dann ist eine neue Struktur später eine neue Datei und kein Code —
das Mitwachsen läuft über Daten.

**Danach ein Land pro Tag:** Wiki-Artikel zu Flagge und Wappen lesen, Fragen ausdenken, Bilder
suchen, Geschichten-Karten schreiben, Flaggentabelle füllen.

**Werkzeug beim Entwerfen:** Die Attribute liegen in einem Google Sheet, eine Zeile pro Land:

```
https://docs.google.com/spreadsheets/d/1FX8SgpOr9G_Ss030KkQDAtOUE3AbEHuMPfxpl3PBQdE/edit
```

Es hat **drei Tabs**: `Hintergrund` (gid 0), `Zusatzelemente` (gid 1267056858) und `Sheet2`.
`build-signaturseite.py` holt die ersten beiden als `flaggen.csv` und `zusatzelemente.csv`. **Ohne
`gid` liefert der Export nur den ersten Tab** — daran hing lange, dass das Elemente-Blatt außerhalb
des Sheets unsichtbar war und Aussagen darüber aus dem Gedächtnis kamen statt aus den Daten.

Die Flaggenbilder werden per Formel von `sciencekids.co.nz` nachgeladen; die Marshallinseln und
Mikronesien fehlen dort und tragen deshalb eine vollständige URL statt eines `../images/…`-Pfads.
Daneben gibt es eine HTML-Seite, die dieses Sheet beim
Öffnen live ausliest und die Flaggen nach identischer Attributsignatur gruppiert anzeigt — damit
lässt sich in Sekunden prüfen, ob ein Cluster wirklich mit *einem* Sketch auskommt. Aktueller Stand:
**206 Flaggen, 63 Signaturen, 34 Einzelgänger.**

Daneben liegt **`pruefe-attribute.py`**, das findet, was man am Bild nicht sieht: Spalten, die gesetzt
sein müssten, weil die Frage davor hingeführt hat, und umgekehrt. Es hat unter anderem Burundi und
Grenada ohne Kreuzantworten gefunden, Nepal mit Werten hinter seinem Ausstieg und eine Signatur-Formel,
die beim Einfügen einer Spalte nicht mitgewachsen war. Eine neue Frage ist dort eine Zeile in `REGELN`.

Seit dem 26.08. prüft es **beide Blätter** und gleicht sie gegeneinander ab (steht im Hauptblatt ein
Element, muss es eine Elementzeile geben, und umgekehrt; dasselbe für den Gösch). Im Elemente-Blatt
sind die Bedingungen Ausdrücke statt Wertelisten — der Kreis erbt die Folgefragen des Außenbereichs,
und das ist kein Paar (Spalte, Werte) mehr.

Daneben stehen **Sondertests** für alles, was sich nicht als Kettenregel schreiben lässt — mehrere
Bedingungen auf einmal, oder eine Spalte hinter der Signatur. Der erste davon fragt: Eine einfarbige
Flagge ohne Gösch, ohne Dreieck und ohne Element wäre eine leere Fläche; die gibt es nicht. Er hat
Japan, St. Lucia und die Schweiz gefunden.

Skript und Seite finden die Kopfzeile selbst (die mit „Signatur" in Spalte 7) und leiten die
Attributzahl aus der Signaturlänge ab. Eingefügte Zeilen und geänderte Attributspalten brechen also
nichts; fest bleiben müssen nur die Spaltenpositionen 0 (Bildpfad), 2 (Land), 7 (Signatur) und 9
(erste Attributspalte).

**Neuheit vor Vollständigkeit.** Solange unerprobte Wege offen sind, werden die bevorzugt: Wenn
Deutschland läuft, bringen die Niederlande nichts Neues. Das Kriterium ist dabei nicht nur eine neue
Struktur, sondern **jeder neue Wert in irgendeinem Antwortvokabular** — eine noch nicht vorgekommene
Farbe (Botswanas Hellblau, Kasachstans Türkis), ein neuer Elementtyp, eine neue Position. Erst wenn
alles einmal durchlaufen ist, wird mit den Flaggen aufgefüllt, die bekannte Wege gehen. Ab da ist es
reine Dateneingabe ohne Entwurfsrisiko.

## 8. Offene Fragen

- **Eingabefläche für die 200 Zeilen** — die Tabelle wird beim Länder-Tag nebenbei gefüllt, ein
  Datenbank-Werkzeug reicht vermutlich. Ein eigener Dialog ist nicht nötig, aber auch nicht
  ausgeschlossen.
- **Gegenrichtung** — bleibt der Enter-Input, also Erweiterung 5. Der Kartenklick scheidet aus:
  Bermuda, die Kaimaninseln, Guam und die Cookinseln sind auf der Weltkarte genauso wenig treffbar
  wie die Untereinheiten, die deshalb schon verworfen wurden. MC scheidet ohnehin aus (siehe
  Verworfen).
- **Woher der Sketch-Name kommt** — aus der Signatur abgeleitet oder als eigene Spalte. Für die
  Ableitung müssten die elf Spezialflaggen durchnummeriert werden statt alle `1` zu tragen; dann
  trägt sie. Entscheidet sich beim Generator.
- **Wie die Toleranz für „nicht falsch" in der Tabelle steht** — ein neuer Wert je Kombination
  (`5` = „2, toleriert 1"), ein Komma (`2,1`) oder eine Klammer (`2(1)`). Die Klammer lehnt sich an
  die Distraktor-Schreibweise dieses Dokuments an; der neue Wert skaliert nicht, weil jede weitere
  Kombination eine weitere Zahl bräuchte.
- **Die Namensebene der Elementfrage** — die Kategorien stehen (Tier, Pflanze, Gebäude, Gegenstand,
  abstraktes Symbol), die Namen darunter nicht. Ende bei „komplexes Emblem" bleibt.
- **Die Formation mehrerer Sterne.** Sechs von sechzehn Flaggen stehen auf `Anders`, und es sind
  ausgerechnet die, bei denen die Formation am meisten wert wäre: Australien, Neuseeland, China,
  Mikronesien, Usbekistan. Drei Wege stehen offen — die Frage streichen (dieselbe Begründung wie bei
  den Streifenbreiten: wer Anzahl und Position weiß, hat das Bild im Kopf), die sechs ausbenennen
  („Kreuz des Südens" allein trägt Australien, Neuseeland, Papua-Neuguinea und Samoa), oder eine
  gezielte Zusatzfrage von Hand. Die Antwortliste mischt heute drei Beschreibungssysteme:
  Reihenrichtung (diagonal, senkrecht, nebeneinander), Bogenform (Kreis, Halbkreis) und Musterbild
  (H, Dreieck).
- **Die Farblisten** fehlen auf beiden Seiten — für die Hintergrundflächen und für die Elemente. Ohne
  sie hört eine generierte Karte nach den Strukturfragen auf. Reine Dateneingabe. Sie war früher
  hinter den Generator vertagt, weil die Flächennummerierung noch offen war; seit §5 steht, ist sie
  es nicht mehr — die Nummern stehen in der Datei, die Auflagen hängen sich hinten an.
- ~~**Ob es eine Farbspalte für den Gösch-Inhalt gibt.**~~ Gab es nicht, ist am 26.08. als **Spalte Q
  „Farbe Zusatzelement im Gösch"** angelegt worden. Der Gösch *selbst* brauchte nie eine: Er steckt in
  der Sketch-Datei, ist damit eine Fläche wie jede andere und steht in der positionsbezogenen
  Hintergrund-Farbliste. — Alle Farbspalten tragen bis auf Weiteres den Platzhalter `Farbe` statt
  echter Werte; sie werden gefüllt, wenn sie gebraucht werden.
- **Wie zwei Farben in einer Zelle stehen.** Grönlands Kreis ist zweigeteilt und braucht zwei Farben,
  wo die Spalte eine vorsieht. Die Fragenseite ist geklärt (eine MC-Antwort mit zwei Klicks), die
  Schreibweise in der Tabelle nicht. Klärt der Grönland-Durchgang.
- **Ob die Skizze der Antwort widersprechen darf** — siehe §5. Weniger zeigen ist erlaubt; ob eine
  Sichel in der falschen Richtung stört, zeigt sich erst am laufenden Bild.
- **Grenada trägt Symbole auf dem Rahmen.** Fällt heute unter „mehrere einfarbige Figuren" und
  „verteilt"; ob das reicht, ist offen. Kein Zwang, es jetzt zu lösen.
- **Ob der Union Jack gezeichnet wird** — siehe §5. Entscheidet sich am Australien-Durchgang.
- **Ob die Sketch-Dateien bei GeoJSON bleiben.** Das Format trägt Ballast aus der Kartenwelt
  (`MultiPolygon`, CRS-Block, fünf Punkte für ein Rechteck), und ein Wechsel wäre heute noch billig.
  Der Kreis, der als Erstes dagegen sprach, ist erledigt (§5: `Point` + `radius`); offen ist, ob die
  komplizierteren Elementformen sich ähnlich sauber unterbringen lassen. Genau dafür laufen die vier
  Härtefälle. Der Rückfall wäre ein SVG-Pfad-String in den `properties` — kann alles, beschreibt
  nichts.
- **Flaggenvarianten** — Costa Rica, Haiti, Ecuador und San Marino haben eine Bürgerflagge ohne
  Wappen, Paraguay trägt auf Vorder- und Rückseite verschiedene Embleme. Es braucht eine Festlegung,
  welche Fassung gilt.
- **Zwilling Nicaragua / El Salvador** — beide blau-weiß-blau mit mittigem Dreieck im Kranz. Löst
  keine Struktur, nur eine gezielte Stichfrage.
- **Ob überhaupt gebaut wird.** Der Aufwand aus Expander und Renderer ist für ein Deck mit Decke
  beträchtlich. Die zwei Gründe, die dafür sprechen: die aufbauende Skizze geht handgeschrieben nicht,
  und nur ein Generator garantiert, dass ähnliche Flaggen denselben Pfad nehmen und exakt dort
  auseinanderlaufen, wo die Flaggen es tun.

## 9. Verworfen

> **Diese Tabelle ist älter als die Zusatzelemente-Staffel** und in Einzelfällen von ihr überholt —
> siehe die durchgestrichene Zeile zu Sonne und Stern. Wo eine Zeile dem Blatt widerspricht, gilt das
> Blatt. Beim nächsten Durchgang lohnt es, die Elementzeilen daraufhin durchzusehen.

| Idee | Warum |
|---|---|
| Länder-Deck mit Attributen (Hauptstadt, Währung, Berge, Einwohner) | Sitzt längst in einem echten Anki-Deck, kein Lerngewinn. Damit entfiel auch ein geplanter Zahlen-Step mit Toleranz. |
| Taxonomie statt Messung | Schließt nie ab; jeder neue Sonderfall kann frühere Länder rückwirkend umwerfen. |
| Bilder als Antwortoptionen der Einstiegsfrage | Zu viele Optionen zur Auswahl. Als Blattfrage weiter denkbar. |
| Klick ins Flaggenbild | Das Bild soll reine Anzeige bleiben, nicht interaktiv. |
| MC für die Gegenrichtung mit generierten Distraktoren | Verwechslungen sind teils lexikalisch (Sambia / Simbabwe), nicht aus der Flaggenähnlichkeit ableitbar — und von Hand will sie niemand pflegen. |
| Flaggen von Untereinheiten (US-Staaten, Kantone) als Wachstumspfad | Auf der Weltkarte kaum treffbar, und inhaltlich uninteressant. |
| Normalisierte Varianten der Flaggentabelle — Kindtabelle für Elemente, pfadkodierte Werte, schmale `(land, frage, antwort)`-Tabelle | Alle kaufen Nichtredundanz und zahlen mit Lesbarkeit. Eine Zeile soll eine Flagge vollständig beschreiben und beim Draufschauen verständlich sein; außerdem macht die breite Tabelle den *Fragensatz* inspizierbar (nach einer Spalte sortieren und die Verteilung ansehen), und ein falscher Fragensatz ist das eigentliche Risiko. |
| Kuratierte Distraktoren | Zufallsauswahl aus der Werteliste tut es, und Verwechslungen sind teils lexikalisch statt visuell — von Hand nicht sinnvoll pflegbar. |
| Der Begriff „Fimbrierung" — **bei den Streifen** | Fachlich richtig, aber er zwingt zu einer Grad-Entscheidung („Strich oder Streifen?") und produzierte laufend Grenzfälle — Suriname, Eswatini, Gambia. Ersetzt durch die vergleichende Formulierung „Streifen 2 und 4 klar am dünnsten", die ohne Fachbegriff und ohne Schwellenwert auskommt. **Bei den Kreuzen bleibt er**: kleine, geschlossene Menge, alle Fälle geprüft und eindeutig (siehe §3). |
| Bilderauswahl statt Zahlenfrage bei der Streifenzahl | Eine Bilderliste kann nur zeigen, was vorkommt — dann fehlt die Option für die Flagge, die man sich falsch vorstellt. Attrappen-Sketches dagegen sprengen die acht Optionen. |
| Interpreter in der Suite: `Flag:`-Expander, `schritt`- und `werteliste`-Tabelle, View auf `flagge` | Ein Generator schreibt stattdessen eine normale Deck-CSV. Ausschlaggebend war, dass eine Fragensatzänderung beim Interpreter alle Karten still mitgeändert hätte; beim Generator zeigt der Diff, was passiert ist. Nebenbei bleibt die Suite dumm und der Fragensatz lesbar. Preis: Die Ableitungslogik steckt jetzt in einem Skript statt in einem einsehbaren `CASE` — die Regeln gehören deshalb hier ins Dokument. |
| Zusatzelemente nach der **Zahl der Figuren** zerlegen — je Figur eine eigene Kategorienfrage | Setzt voraus, dass die Figurenzahl beantwortbar ist, und das ist sie nicht: Simbabwe ist ein Wappen oder ein Vogel plus ein Stern, Brasilien eine Raute plus Kugel oder drei Dinge, Irak und Kasachstan ebenso strittig. Eine Frage, deren Antwort schon beim Entwerfen nicht eindeutig fällt, trägt keinen ganzen Zweig. Stattdessen fängt „komplexes Emblem" diese Fälle als Endstation ab. |
| **Zacken zählen**, um Sonne von Stern zu unterscheiden | Overhead. Die Zackenzahl ist kein Wissen, das man behalten will — derselbe Fall wie die Streifenbreiten, die man mitbekommt, wenn man Anzahl und Farben weiß. |
| ~~Sonne und Stern **überhaupt unterscheiden**~~ — **zurückgenommen** | Stand hier mit der Begründung „nicht durch Hinsehen entscheidbar, die Antwort trägt nichts". Die Zusatzelemente-Staffel vom 25.08. unterscheidet die beiden trotzdem, und das Blatt sticht dieses Dokument. Die Zeile bleibt stehen, weil die Begründung erklärt, warum die Grenze unangenehm ist — nicht, weil sie noch gilt. Folge für die Skizze: zwei Werte brauchen zwei Formen (§5). |
| Eine eigene Werteliste für den **Kreisinhalt** | Sie war gröber als die allgemeine und verdeckte, dass vier von fünf „einfarbigen Figuren" im Kreis in Wahrheit Himmelskörper sind. Ersetzt durch dieselbe Liste wie außerhalb. Beim **Gösch** gilt das Gegenteil und er behält seine eigene Liste: Dort ist sie feiner, und `Union Jack` ist der informativste Wert im ganzen Blatt. |
| Ein Attribut, das Frage 5 beantwortet | Bestünde den Test „bleibt es wahr, wenn man die Fragenstaffel wegwirft" nicht. Es wäre nur so lange richtig, wie Frage 5 genau so geschnitten ist, und würde bei einem Umbau still falsch. |
| **Gösch und Dreieck zur Laufzeit über den Hintergrund legen** | Gemessen spart es 13 Dateien von 63 — und kostet dafür Geometrie im Anwendungscode, eine Rangfolge-Konvention und eine zur Laufzeit gerechnete Flächennummerierung. Dieselbe Rechnung läuft so oder so; die Frage ist nur, ob sie einmal läuft und eine einsehbare Datei hinterlässt oder jedes Mal. Das ist die Entscheidung „Generator statt Interpreter" aus §4, eine Ebene tiefer. |
| **Auch die Zusatzelemente backen** — pro Stufe eine vollständige Datei nachladen | Bis zu 206 × 3 Dateien, weil jede Stufe alles Vorherige mitbringen muss. Schwerer wiegt: `SketchImage` setzt zurück, die Karte müsste also die schon gegebenen Farbantworten wieder ausschreiben. Diese Füllungen gehörten zu keiner Frage, und die Zeile läse sich nicht mehr als Protokoll des Dialogs — genau das, was §4 mit dem Generator gewinnen wollte. Die Kombinatorik war dagegen **kein** gültiger Einwand: Sie ist beschränkt, nicht explosiv. |
| **Ein Kriterium „was den Hintergrund zerschneidet, gehört in die Datei"** | Klingt geometrisch sauber, hält aber nicht: Japans Kreis schneidet in das weiße Feld genauso wie ein Dreieck in die Streifen. Es entscheidet das zeitliche Kriterium (§5) — vor der Skizze gefragt oder danach. |
| **Ein Kreis als Vieleck** | Sieht auch als 64-Eck schlecht aus. Ersetzt durch `Point` + `radius`; die Sichel entsprechend durch `Shape.subtract` statt durch eine Näherung. |
| **Den Union Jack nachbauen** | Er ist *ein* Antwortwert. Zehn Flächen dafür wären das einzige Stück Realismus in einer schematischen Zeichnung und bräuchten Farben, nach denen keine Frage fragt. Benannt, nicht gezeichnet — wie „komplexes Emblem". |
| **Elementdateien je Position** (`kreis-mitte`, `stern-links-oben`) | Wieder ein Produkt aus Formen × neun Feldern. Stattdessen eine Datei je Form und das Rasterfeld im Step. |

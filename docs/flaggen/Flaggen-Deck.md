# ThosSuite — Flaggen-Deck (Planung)

**Stand:** 25.08.2026 — **Beide Fragenstaffeln stehen, alle 206 Flaggen sind danach attributiert.**
Siebzehn Spalten für den Hintergrund, elf im Zusatzelemente-Blatt, geprüft durch
`pruefe-attribute.py`, null Widersprüche. Der **Durchstich läuft**: Deutschland und Dänemark sind ein
Proof of Concept in der Suite, mit aufbauender Skizze.

Was für vollständige Karten noch fehlt: die **Farblisten**, für den Hintergrund wie für die Elemente.
Beide hängen an der Flächennummerierung und damit am **Generator**, der als Nächstes kommt. Offen ist
außerdem die Formation mehrerer Sterne (siehe §8).

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
Kern die heraldischen Tinkturen; Orange und Hellblau sind ihre Grenze wert, weil sie echte, abgesetzte
Cluster bilden. Türkis, Karmin, Maroon, Braun, Grau und Lila werden eingefaltet — jede zöge eine neue
Grenze für ein bis drei Länder Gewinn.

Ins CheatSheet gehören: **Gold = Gelb** und die zwei, drei echten Randfälle (Kasachstans `#00ABC2`
ist deutlich ins Cyan gezogen, wird aber als Hellblau geführt).

**Die Farben liegen in einer Spalte als Liste in Flächenreihenfolge** (`hellblau|weiß|schwarz|weiß|
hellblau`). Es gibt keine Frage „Farbe des linken Streifens" — die Skizze hebt eine Fläche hervor, in
zufälliger Reihenfolge, und dazu wird die Farbe gewählt. Also **eine** Fragedefinition, n-mal
angewandt. Damit braucht es auch keine Leserichtungs-Konvention.

**Streifenbreiten werden nicht abgefragt.** Wer Anzahl und Farben weiß, hat das Bild im Kopf und
bekommt die Breiten mit. Spart eine ganze Fragenebene.

### Zusatzelemente

Der zweite Fragenblock, nach den Farben. Er liegt in einem **eigenen Blatt** der Tabelle, eine Zeile
pro Flagge mit Element — 144 der 206. Wer in der Haupttabelle `Zusatzelemente = 0` tragt, steht dort
nicht.

Der Aufbau ist derselbe wie beim Hintergrund: eine Weiche, dann zweigeigene Folgefragen.

```
Was ist im Gösch?          nur wenn die Flagge einen hat (18 Flaggen)
Was liegt außerhalb?       die Weiche — sechs Werte
  Kreis                    → Was liegt im Kreis? → dieselben Werte noch einmal
  Himmelskörper            → welche → wie viele → Formation
  einfarbige Figur         → Kategorie (Tier, Pflanze, Gebäude, Gegenstand, abstraktes Symbol)
  komplexes Emblem         → Endstation
Wo?                        acht Positionen, für alle außer „Keine"
Farbe                      Kreis bzw. Element, und getrennt davon der Kreisinhalt
```

**Die Weiche hat sechs Werte** — Nur Himmelskörper (52), Komplexes Emblem (38), Nur eine einfarbige
Figur (24), Klar farbig abgegrenzter Kreis (14), Keine (10), Mehrere einfarbige Figuren gleicher
Farbe (6). Sie deckt alle 144 Zeilen ab, ohne Auffangkorb.

**Der Kreis ist ein Behälter, kein Inhalt.** Was in ihm liegt, wird mit *derselben Werteliste*
gefragt wie das, was außerhalb liegt — eine Fragedefinition, zweimal angewandt, so wie die Farbfrage
n-mal angewandt wird. Das spart eine zweite Werteliste und deckt auf, was eine eigene Liste verdeckt
hatte: Burundis drei Sterne, Äthiopiens und Nordkoreas Stern und Tunesiens Mond sind Himmelskörper
und keine „einfarbigen Figuren". Dazu kommt genau ein Wert, den es außerhalb nicht gibt: **der Kreis
ist selbst geteilt** (Grönland). Er sagt dem Generator, dass hier zwei Farbflächen kommen statt einer,
und wird über eine MC-Antwort mit zwei Klicks abgefragt.

Der Preis: Der Kreis-Zweig erbt die Folgefragen, Tunesien und Burundi laufen dadurch ein bis zwei
Schritte länger. Und Neukaledonien ist der einzige Treffer der Kategorienfrage innerhalb des Kreises —
ein Schönheitsfehler, aber keiner, der etwas kostet: Die Frage steht ohnehin, sie wird in diesem Zweig
nur selten erreicht.

**„Komplexes Emblem" ist eine Endstation, kein Auffangkorb.** Es bedeutet „hier liegt etwas zu
Komplexes, es wird nicht zerlegt" — Kenias Schild mit Speeren, Spaniens Wappen, Belarus mit seinem
Ornamentband. Solche Flaggen bekommen die Positionsfrage, aber keine Farbfrage. Früher standen daneben
ein zweiter Wert „Anders" und die Unterscheidung „ist das ein Wappen oder nicht"; beides ist
zusammengelegt, weil die Grenze zu ziehen war, ohne dass sie etwas eintrug.

**Die einfarbige Figur endet bei einer Kategorie**, nicht bei einem Namen: Tier, Pflanze, Gebäude,
Gegenstand, abstraktes Symbol. Die zweite Stufe — Adler, Zeder, Angkor Wat — ist vorgesehen, aber
nicht ausformuliert. Der Vorteil der Kategorie-Ebene bleibt derselbe: **Die Distraktoren der
Namensfrage kämen aus der beantworteten Kategorie**, also plausible Ablenker ohne Kuratierung und
ohne Leak.

Wenn ein einzelnes Element wichtig genug ist (Sri Lankas Löwe), wird daraus eine eigene Karte, nicht
ein weiterer Schritt in der Flaggenkarte. Zeder, Lorbeer und Ahorn sind Stoff für die
Geschichten-Karten.

**Die Position** wird per MC über acht feste Werte gefragt — Mitte, Links mittig, Links oben, Rechts
mittig, Links unten, Oben mittig, Rechts oben, Über die ganze Flagge verteilt —, **nicht** per Klick
ins Bild. Der letzte Wert trägt die Fälle, in denen mehrere Figuren an mehreren Stellen liegen:
Australien, Georgien, Grenada, Kasachstan, Papua-Neuguinea. Er ersetzt ein früheres „Anders", das
nichts aussagte.

**Die Anzahl bleibt dort in der Kategorie, wo sie die Folgefrage erübrigt.** `1 Stern` gegen `Sterne`
ist kein Schönheitsfehler, sondern der zulässige Fall aus §2: Eine Frage entfällt, wenn ihre Antwort
zwingend folgt. Bei `1 Stern` folgt die Anzahl aus der Kategorie — und dass die Anzahlfrage nach
`Sterne` erscheint und nach `1 Stern` nicht, ist Verengung durch die eigene richtige Antwort, kein
Leak.

**Wie lang eine Elementkarte wird:** Der Schwerpunkt liegt bei drei bis vier Fragen einschließlich
Farbe, das Maximum trägt Australien mit sieben (Gösch, Weiche, welche Himmelskörper, wie viele,
Formation, wo, Farbe). Sieben ist die Obergrenze, die das Deck sich setzt.

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

**Wie Gösch und Dreieck in den Sketch kommen, ist offen** (siehe §8); die aktuelle Idee ist, dass ein
Sketch nur den **Hintergrund** trägt und alles andere zur Laufzeit darübergelegt wird. Wie auch immer
es ausgeht — diese Auflage gilt: **immer dieselbe
Gösch-Geometrie, immer dieselbe Dreieckstiefe, keine Anpassung an die Streifenzahl.** Der Grund ist
derselbe wie beim Leak: Ein Sketch, der Details richtig zeigt, die nie beantwortet wurden — Chiles
Gösch genau über dem oberen Streifen, Togos genau über dreien von fünf —, verrät sie. Die Skizze soll
die Flächen sauber zum Einfärben zeigen, nicht die Flagge möglichst getreu abbilden.

**Die Dreieckstiefe ist einheitlich, etwa 40 % der Breite.** Tschechien reicht mit 50 % am weitesten,
die Philippinen und Dschibuti liegen bei gut 43 %, Bahamas, Jordanien und Sudan bei etwa einem
Drittel. Die Ausreißer haben eigene Formwerte: Eritreas Keil läuft bis zur Flugseite durch, Kuwaits
Figur ist ein Trapez, Südafrika und Vanuatu tragen ein liegendes Y.

**Die Skizze ist schematisch, nicht formattreu.** Das Seitenverhältnis ist fest **3:2** — häufigstes
echtes Flaggenformat und das Format, für das die Skin-Maße gedacht sind. Die *Größe* ist dagegen
frei: Gewählt wird die kleinste gerade Höhe ab 120, die durch die Streifenzahl teilbar ist, damit
alle Koordinaten ganzzahlig bleiben (3 Streifen → 180 × 120, 13 Streifen → 195 × 130). Der Kontrakt:
*In diesem Verhältnis sind Kreise Kreise; in einem anderen wird eingepasst und mittig gesetzt.*

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

**Ablauf einer Karte:** Struktur (ohne Bild) → Skelett mit hervorgehobener Fläche erscheint → Farben
füllen sich Fläche für Fläche → Gösch und Dreieck werden mitgezeichnet → die Zusatzelemente werden
gefragt → zum Abschluss die echte Flagge.

**Ob Zusatzelemente mitgezeichnet werden, ist offen.** Ursprünglich sollten sie nur benannt werden;
inzwischen spricht einiges dafür, wenigstens die einfachen Formen zu zeichnen — Japans Kreis
markieren und einfärben zu lassen, wäre dieselbe Mechanik wie bei einer Streifenfläche, und fünf
Elementfragen ohne jedes sichtbare Feedback wären der einzige Teil der Karte, wo nichts passiert. Die
Grenze läge dort, wo die Elementfrage sie ohnehin zieht: Was einen Namen trägt, aus dem die Form
folgt (Kreis, Zackenfigur, Sichel), ist zeichenbar; was „komplexes Emblem" heißt, nicht. Wenn
gezeichnet wird, gilt für die Skizze dieselbe Auflage wie für den Gösch — immer dieselbe
Platzhalterform, nie die echte.

Eine falsche Antwort beendet die Karte, wie überall sonst in der Suite. Die Skizze bleibt dann
halbfertig stehen; sie baut sich also nur bei einem fehlerfreien Durchlauf ganz auf.

### Wie es gebaut ist

Drei Steps, bewusst getrennt, damit der Ablauf vollständig in der CSV-Zeile steht und kein Schritt
etwas über seine Nachbarn wissen muss:

```
SketchImage:<struktur>            laden, alle Flächen leer — und zugleich das Zurücksetzen
SketchImageMark:<n>               Fläche hervorheben
SketchImageFill:<n>,<Farbname>    Fläche einfärben, läuft nach dem MC
```

Gezeichnet wird in den Szenengraph, nicht in ein fertiges Bild: `SketchPane` (paketprivat, Innenleben
von `SuiteImage`) baut je Fläche einen `Path`. Eine Fläche hat drei Zustände — frisch umrandet und
ungefüllt, markiert, gefüllt. **Gefüllt verliert sie ihren Strich**, damit zwei benachbarte Flächen
derselben Farbe am Ende nahtlos verschmelzen statt eine erfundene Naht zu zeigen.

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

Dazu, nicht ursprünglich geplant: eine **dritte Staffelungsebene** für die Skin-Maße
(`deckId` → `mapName` → Kategorie). Ohne sie hätte das Flaggen-Deck seine Maße aus zwei verschiedenen
Schlüsseln gelesen — der `!Sofort`-Marker in `AnkiLearnView` hatte genau das vorhergesagt.

**Noch offen:**

5. **Input mit Enter-Bestätigung** — der heutige Input prüft nach jedem Tastendruck und verrät damit
   die Antwort, sobald der Antwortraum klein und durchprobierbar ist. Wird gebraucht, weil der
   Kartenklick für die Gegenrichtung ausscheidet (siehe §8).
6. **MC braucht eine „nicht falsch"-Option** — für Grenzfälle wie Lesotho. Der Step bleibt stehen wie
   bei einer falschen Antwort, der Fehlerzähler zählt aber nicht hoch. Damit lernt man die Grenze,
   indem man einmal dagegenstößt, und muss die kanonische Antwort trotzdem selbst produzieren.
   Umsetzung: ein dritter Abschnitt hinter einem zweiten `*`, abwärtskompatibel, weil bestehende
   Zeilen mit einem `*` unverändert zerfallen. Der Aufwand steckt darin, dass `AnswerOption` drei
   Zustände statt eines `boolean` braucht und jede lesende Stelle sich zum dritten verhalten muss.

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

**Als Nächstes also der Generator.** Ihm fehlen noch die Farblisten (siehe §8), aber die Struktur, an
der er hängt, steht jetzt auf beiden Seiten.

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
  sie hört eine generierte Karte nach den Strukturfragen auf. Reine Dateneingabe, aber sie hängt an
  der Flächennummerierung: Erst wenn feststeht, wie ein Sketch nummeriert ist, kann die Liste getippt
  werden, denn sie ist positionsbezogen. Deshalb wandert sie hinter den Generator.
- **Wie Gösch, Dreieck und Elemente in den Sketch kommen** — in jede Kombination hineingezeichnet
  (mehr Dateien, Renderer bleibt dumm, Flächennummerierung steht in der Datei) oder zur Laufzeit als
  weiteres Polygon darübergelegt (weniger Dateien, dafür Nummerierung zur Laufzeit berechnet). Die
  aktuelle Idee: **Ein Sketch ist der Hintergrund**, alles andere wird zur Laufzeit darübergemalt —
  Gösch und Dreieck sind dann nur die ersten beiden Auflagen und keine Sonderfälle. Dafür spricht,
  dass die Fragenstaffel dieselbe Trennung schon macht (beide werden vor Frage 5 „gedanklich
  entfernt") und dass die Kombinatorik sonst explodiert. Ob es trägt, zeigt sich beim Bauen.
- **Wie die Auflagen nummeriert werden**, falls zur Laufzeit gezeichnet wird. Die Regel müsste
  lauten: **Hintergrundflächen `0..n-1` aus der Datei, danach die Auflagen in fester Rangfolge** —
  Dreieck, Gösch, Element; fehlt eine, rücken die späteren auf. Solange das gilt, kostet ein
  nachträglich gezeichnetes Element einen Eintrag am Ende und lässt alles davor in Ruhe. Die Regel
  kostet heute nichts, muss aber stehen, bevor die erste Farbliste getippt wird. Sie betrifft nur die
  Reihenfolge, in der der Generator die Flächen ausschreibt — in der Tabelle bleiben Hintergrund- und
  Elementfarben getrennt, sie haben nichts miteinander zu tun.
- **Ob die Sketch-Dateien bei GeoJSON bleiben.** Das Format trägt Ballast aus der Kartenwelt
  (`MultiPolygon`, CRS-Block, fünf Punkte für ein Rechteck), und ein Wechsel wäre heute billig, weil
  außer den generierten Streifendateien noch nichts existiert. Es gibt aber auch keinen Zwang: Kreise
  lassen sich als `Point` mit einem `radius` in den `properties` ablegen, und `ShapeGeometry` kann
  Kreise bereits. Was fehlt, ist ein Zweig in `SketchPane`, die heute nur `MoveTo`/`LineTo` kennt —
  und dort eine Kleinigkeit: Ein JavaFX-`Circle` startet **gefüllt**, ein `Path` nicht. „Noch nicht
  beantwortet" ist genau dieser ungefüllte Zustand, also müssten beide Formen ihren Startzustand aus
  derselben CSS-Regel bekommen statt aus dem JavaFX-Standard.
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
| Sonne und Stern **überhaupt unterscheiden** | Nicht durch Hinsehen entscheidbar, und die Antwort trägt nichts. Eingefaltet wie Türkis in Hellblau: eine Grenze weniger, die man ziehen muss. |
| Eine eigene Werteliste für den **Kreisinhalt** | Sie war gröber als die allgemeine und verdeckte, dass vier von fünf „einfarbigen Figuren" im Kreis in Wahrheit Himmelskörper sind. Ersetzt durch dieselbe Liste wie außerhalb. Beim **Gösch** gilt das Gegenteil und er behält seine eigene Liste: Dort ist sie feiner, und `Union Jack` ist der informativste Wert im ganzen Blatt. |
| Ein Attribut, das Frage 5 beantwortet | Bestünde den Test „bleibt es wahr, wenn man die Fragenstaffel wegwirft" nicht. Es wäre nur so lange richtig, wie Frage 5 genau so geschnitten ist, und würde bei einem Umbau still falsch. |

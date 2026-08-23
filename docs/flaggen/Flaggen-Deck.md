# ThosSuite — Flaggen-Deck (Planung)

**Stand:** 21.08.2026 — Eröffnungsstaffel, Waagerecht-Zweig, Gösch und Dreieck ausformuliert; alle
206 Flaggen sind attributiert. Es existiert noch **kein Code**.

**Charakter dieses Dokuments:** Übergabe an das Ich, das die Sache irgendwann anfasst. Festgehalten
ist, was entschieden ist, was noch offen ist und was bewusst verworfen wurde. Deskriptiv für den
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
Dazwischen liegt eine **View auf die Attributtabelle**, die Attribute auf Fragefelder abbildet: Ein
Teil der Fragen liest ein Attribut direkt, andere entstehen aus einem `CASE` (der Rahmen aus
≥2 waagerecht *und* ≥2 senkrecht *und* kein Kreuz, das Dreieck-ja/nein aus der Dreiecksform, der
Hintergrund daraus, welches Attribut gesetzt ist). Damit liegt die Ableitungslogik an einer
deklarativen, einsehbaren Stelle statt in Java, und die Schritt-Tabelle zeigt immer auf die View —
sie muss nicht wissen, ob dahinter eine Spalte oder ein Ausdruck steckt. Es wird
ausdrücklich *nicht* versucht, alle Flaggen wasserdicht zu attributisieren; das gelingt ohnehin nicht.
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
 1  Spezial ganz zum Schluss fragen      9  Kreuz parallel
 2  Durchgehende waagerechte Streifen   10  Diagonale Linie steigend
 3  3W  — Breitenmuster der Dreier      11  Diagonale Linie fallend
 4  5W  — Streifenstruktur der Fünfer   12  Dreiecksform
 5  Durchgehende senkrechte Streifen    13  Dreiecksflächen
 6  Senkrechter Streifen wo?            14  Gösch
 7  Kreuz senkrecht
 8  Kreuz diagonal
```

Die Werte entsprechen jeweils den Antwortoptionen ihrer Frage:

```
3W  1 alle gleich breit          5W  1 2 und 4 dünn, Mitte nicht breiter
    2 mittlerer breiter              2 alle gleich
    3 mittlerer schmaler             3 Mitte breiter, 2 und 4 nicht dünn
    4 oberster breiter               4 Mitte breiter und 2 und 4 dünn
    (5 unterster breiter)            5 oberster am breitesten
                                     (6 unterster am breitesten)

Dreiecksform  1 einzelnes echtes Dreieck        Dreiecksflächen  1 äußere als Umrandung
              2 abgeschnittenes Dreieck                          2 zwei verschiedene Tiefen
              3 waagerecht zum Flugteil verlängert               3 gestaffelt und umrandet
              4 echtes Dreieck, mehr als eine Farbe
              5 einzeln, reicht bis zur Flugseite
```

Die eingeklammerten Werte sind reine Distraktoren und kommen bei keiner Flagge vor. Entfallen sind
„Streifenfimbrierung" (steckt jetzt in 5W) sowie „Komplette Dreiecke von links", „Dreieck durchgehend"
und „Dreieck speziell?" (ersetzt durch die beiden Dreiecksspalten).

**„Rahmen" ist kein Attribut, sondern abgeleitet** — ≥2 waagerechte *und* ≥2 senkrechte Streifen
*und* kein Kreuz. Trifft Guam, Malediven, Montenegro, Sri Lanka und Grenada; die Kreuz-Bedingung hält
Dominica draußen, dessen Bänder vom Trikolore-Kreuz stammen. Die Regel gilt nur für rechteckige
Flaggen — Nepal hat einen Rand, aber keine Streifen.

Vorkommende Bänderzahlen: **2, 3, 4, 5, 6, 7, 9, 11, 13, 14** — Mauritius die 4, Uganda die 6,
Simbabwe die 7, Griechenland und Uruguay 9, Liberia 11, USA 13, Malaysia 14. Die 1 steht für uni
(ein Feld läuft in beide Richtungen durch), die **−1 für „Teilung vorhanden, aber unsichtbar"** —
Panamas Kreuz, Bhutans und Papua-Neuguineas Diagonale.

### Die Fragenstaffel bis Frage 8

Fragen 1 bis 5 gelten für **alle 204** und tragen die Flagge Schicht für Schicht ab; ab 6 geht es in
den Waagerecht-Zweig.

```
1  "Ist die Flagge rechteckig?"
     Ja                                    → weiter
     Nein                                  → Spezialzweig (nur Nepal)

2  "Hat die Flagge einen Rahmen?"
     Ja | Nein

3  "Hat die Flagge einen Gösch?"
     Ja | Nein

4  "Jetzt entferne den Rahmen und den Gösch gedanklich.
    Ragt ein oder ragen mehrere Dreieck(e) ganz vom linken Rand herein?"
     Ja                                    → 4a
     Nein                                  → 5

4a "Was beschreibt die Dreiecksfigur am besten?"
     Ein einzelnes echtes Dreieck                       → 5   (14 Flaggen)
     Abgeschnittenes Dreieck                            → 5   (Kuwait)
     Dreiecksform, waagerecht zum Flugteil verlängert   → 5   (Südafrika, Vanuatu)
     Echtes Dreieck, aber mehr als eine Farbe           → 4b
     Einzelnes Dreieck, das bis zur Flugseite reicht    → 5   (Eritrea)

4b "Wie sind die Flächen des Dreiecks angeordnet?"
     Zwei Flächen, die äußere als Umrandung             (Simbabwe)
     Zwei Dreiecke unterschiedlicher Tiefe              (Osttimor)
     Zwei Dreiecke, gestaffelt und umrandet             (Guyana)

5  "Nun entferne gedanklich auch das Dreieck und eventuelle Zusatzelemente.
    Was beschreibt den Hintergrund der restlichen Flagge am besten?"
     Waagerechte Streifen                  → 6
     Senkrechte Streifen | Kreuz mit vier Quadranten |
     Diagonale Streifen | Einfarbige Fläche | Anderes

6  "Wie viele waagerechte Streifen sieht man?"
     2 → 8     3 → 7a    4 → 8     5 → 7b
     6 → 8     7 → 8     8 → (kommt nicht vor)    9 → 8

7a "Wie sind die Streifen verteilt?"                        (nur bei 3)
7b "Wie sind die Streifen verteilt?"                        (nur bei 5)
     — Optionen siehe Tabellen unten

8  [Sketch erscheint, Fläche 1 markiert]
   je Fläche: "Welche Farbe hat die markierte Fläche?"
     Rot | Blau | Hellblau | Grün | Gelb | Orange | Weiß | Schwarz
```

**Geprüft ist das bislang für den Waagerecht-Zweig samt Gösch und Dreieck.** Nicht durchgespielt sind
die Zusatzelemente und das Einblenden der echten Flagge. Die Zusatzelemente kommen ohnehin erst
**nach** den Farben, weil sie über dem Hintergrund liegen und nicht mitgezeichnet werden.

**Der Gösch braucht keine eigene Frage.** Er ist eine Fläche wie jede andere und wird bei Frage 8
markiert und eingefärbt — nach der Konvention „Auflagen kommen ans Ende der Farbliste" als Fläche
n+1. Sein **Inhalt** ist ein Zusatzelement und wird nicht gezeichnet: 50 Sterne bei den USA, ein
Kreuz bei Griechenland, der Union Jack bei Australien, Neuseeland, Fidschi, den Cookinseln, Tuvalu,
den Kaimaninseln und Bermuda. Auf der **Sketch**-Seite ist er damit aber nicht erledigt — ob er in
jede Kombination hineingezeichnet oder programmatisch darübergelegt wird, ist offen.

Zu Frage 1: Sie fängt Nepal ab, bevor es alles Weitere kaputtmacht — jede spätere Formulierung setzt
Rechteckigkeit voraus (Bänder von Rand zu Rand, Ecken für den Gösch, „vom ganzen linken Rand"). Dass
203 Flaggen mit „ja" antworten, spricht nicht dagegen.

Die zehn Spezialflaggen verteilen sich so: Nepal fällt bei Frage 1 heraus, Kuwait, Südafrika und
Vanuatu werden bei Frage 4 auffällig (dort braucht es eine CheatSheet-Regel), die übrigen sechs landen
bei Frage 5 auf „anderes".

Bei Frage 6 werden **2 bis 9** angeboten, obwohl nur 2, 3, 4, 5, 6, 7 und 9 vorkommen — eine falsche
Vorstellung muss ausdrückbar sein. **Offen:** 11, 13 und 14 gibt es nur mit Gösch (Liberia, USA,
Malaysia). Nach der Regel „dieselbe Frage in allen Zweigen" müsste die Liste überall 2–14 umfassen und
acht Werte zufällig gezogen werden, sonst verrät die Optionsliste den Zweig.

Die Zusatzfrage 7 wird nur dort gestellt, wo die Antwort tatsächlich variiert:

**Bei 3 Streifen** (57 Flaggen) — *Wie sind die Streifen verteilt?*

| Antwort | Flaggen |
|---|---|
| Alle gleich breit | ~43 |
| Mitte breiter | Spanien, Libanon, Libyen, Laos, Kambodscha, Tadschikistan, São Tomé, Belize, Lesotho |
| Mitte schmaler | Lettland, Nauru |
| Oberster breiter | Kolumbien, Ecuador, Ruanda |
| Unterster breiter | kommt nicht vor — reiner Distraktor |

Lesotho (etwa 3:4:3) ist der Grenzfall: „alle gleich breit" wird dort **nicht als falsch gewertet**.

**Bei 5 Streifen** (16 Flaggen) — *Wie sind die Streifen verteilt?*

| Antwort | Flaggen |
|---|---|
| Streifen 2 und 4 klar am dünnsten, mittlerer aber nicht klar am breitesten | Botswana, Kenia, Gambia, Usbekistan, Mosambik, Südsudan |
| Alle gleich | Kuba, Puerto Rico, Togo |
| Mittlerer klar am breitesten, 2 und 4 aber nicht klar am dünnsten | Costa Rica, Israel, Thailand |
| Mittlerer klar am breitesten **und** 2 und 4 klar am dünnsten | Nordkorea, Suriname, Eswatini |
| Oberster am breitesten | Kap Verde |
| Unterster am breitesten | kommt nicht vor — Distraktor |

Die beiden Merkmale „2 und 4 am dünnsten" und „Mitte am breitesten" sind voneinander unabhängig und
können gemeinsam auftreten. Eine erste Fassung nannte pro Option nur *ein* Merkmal und ließ dadurch
bei Nordkorea, Suriname und Eswatini zwei Optionen zutreffen. Die Auflösung sind die
ausdrücklichen „aber nicht"-Zusätze oben — **Teilbeschreibungen überlappen, vollständige nicht.**

Kap Verde greift auf „oberster am breitesten", weil seine drei mittleren Bänder gleich breit sind und
damit weder 2/4 die dünnsten noch die Mitte die breiteste ist. Botswana greift dort *nicht*, weil
seine beiden Ränder gleich breit sind.

**Israel** steht in der dritten Zeile, nicht in der vierten: Die Streifen sind 15 : 25 : 80 : 25 : 15,
die **äußeren** sind also die dünnsten und nicht 2 und 4.

Bei allen anderen Anzahlen gibt es nur ein oder zwei Flaggen, alle gleichmäßig — dort entfällt die
Frage. Nordkoreas breiter Mittelstreifen und Botswanas breite Ränder laufen bewusst auf demselben
Sketch mit.

Der Zweig umfasst damit **26 Signaturen für 100 Flaggen**. Gösch und Dreieck gehören zum Sketch und
bilden eigene Signaturen — nur die Zusatzelemente sind Auflagen, die nicht mitgezeichnet werden.

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

### Elemente

Gösch ist ein **Element**, keine Hintergrundkategorie. In Kategorie 1 tritt er bei genau sieben
Flaggen auf — USA, Liberia, Malaysia, Uruguay, Griechenland, Togo, Chile — und sein Inhalt liegt
immer **im** Gösch, nie daneben.

**Die Elementfrage endet bei „Wappen".** Wappen werden nicht zerlegt — sonst folgten dreißig
Folgefragen zu Machete, Zahnrad, Kondor und Vulkan. „Wappen" ist selbst eine gültige Endantwort. Wenn
ein einzelnes Element wichtig genug ist (Sri Lankas Löwe), wird daraus eine eigene Karte, nicht ein
weiterer Schritt in der Flaggenkarte.

Darüber liegen **zwei Ebenen**: erst die Kategorie (Lebewesen, Pflanze, Bauwerk, Gegenstand, Schrift,
geometrische Figur, Himmelskörper, Wappen), dann der Name (Adler, Drache, Kranich, Zeder, Angkor Wat,
Krone …). Vierzehn der 26 „speziellen" Flaggen aus Kategorie 1 landen bei „Wappen", der Rest verteilt
sich einzeln.

Der Vorteil der Kategorie-Ebene: **Die Distraktoren der Namensfrage kommen aus der beantworteten
Kategorie** — beim Adler also Drache, Kranich, Löwe statt Angkor Wat und Zeder. Plausible Ablenker
ohne Kuratierung, und ohne Leak, weil die Kategorie selbst beantwortet wurde.

Die Position eines Elements wird per MC über ein festes Vokabular gefragt (Mitte, oben links, am
Mast, verteilt, ganzflächig …), **nicht** per Klick ins Bild.

### Leak oder nicht

Ein Kriterium, das an mehreren Stellen gebraucht wird:

> **Ein Leak ist Information, die das System verschenkt** — eine Frage, die nur bei bestimmter Antwort
> auftaucht, oder eine Optionsliste, in der ein möglicher Wert fehlt.
> **Verengung durch die eigene richtige Antwort ist kein Leak.** Dass nach „3 Streifen" nur noch drei
> Farben zu nennen sind, ist der Zweck der Kette, nicht ihr Fehler.

## 4. Karten und Daten

**Eine Flaggenkarte ist eine ganz normale CSV-Zeile** mit genau einem Step: `Flag:Deutschland`. Damit
hat sie eine Identität, einen CardProgress, ein Level — alles über bestehende Maschinerie. Der Tag
`Flagge` erlaubt im Anki-Konfigdialog „gib mir 20 Flaggenkarten".

Der Generator ist damit kein paralleler Kartenerzeuger, sondern ein **Step-Expander**: `Flag:` wird
beim Einlesen zur vollen Sequenz aufgelöst, deren Länge und Verzweigung von den Attributwerten
abhängt.

Bewusst in Kauf genommen: Die Deck-CSV ist Master dafür, *welche* Karten existieren, aber nicht mehr
dafür, *was* sie fragen. Und eine Änderung am Fragensatz ändert alle Flaggenkarten still mit, ohne
dass der Level das merkt.

Die **Attribute** liegen in einer eigenen Tabelle, nicht in der Deck-CSV — zwei verschiedene Dinge.
Geschichten-Karten sind normale handgeschriebene Zeilen in derselben Deck-CSV.

### Drei Tabellen und eine View

**`flagge`** — eine Zeile pro Land mit den Attributen (Stand: 15). Sie beschreibt die Flagge, nicht
die Fragen.

**View auf `flagge`** — bildet Attribute auf **Fragefelder** ab. Die meisten Felder lesen ein Attribut
direkt durch; drei entstehen aus einem `CASE`: der Rahmen (≥2 waagerecht *und* ≥2 senkrecht *und* kein
Kreuz), das Dreieck-ja/nein (Dreiecksform gesetzt) und der Hintergrund (welches Attribut gesetzt ist).
Damit steht die Ableitungslogik deklarativ an einer Stelle statt in Java.

> Aufpassen: Im Hintergrund-`CASE` steckt eine **Priorität** — Dominica hat waagerechte *und*
> senkrechte Streifen *und* ein Kreuz, die Reihenfolge der `WHEN`-Zweige entscheidet. Das ist Logik,
> nicht bloß Abbildung, und braucht dieselbe Sorgfalt wie die Ausschließlichkeit einer Antwortliste.

**`schritt`** — die Fragenstaffel, eine Zeile pro Knoten, Reihenfolge = Zeilenreihenfolge:

```
id          frage                                    art          werte        feld          routing
form        Ist die Flagge rechteckig?               MC           janein       form          nein>spezial
rahmen      Hat die Flagge einen Rahmen?             MC           janein       rahmen
goesch      Hat die Flagge einen Gösch?              MC           janein       goesch
dreieck     Ragt ein Dreieck … herein?               MC           janein       dreieck       nein>hintergrund
dform       Was beschreibt die Dreiecksfigur …?      MC           dformen      dreiecksform  4>dflaeche; *>hintergrund
dflaeche    Wie sind die Flächen angeordnet?         MC           dflaechen    dreiecksflae
hintergrund Was beschreibt den Hintergrund …?        MC           hgruende     hintergrund   waagerecht>anz_w; …
anz_w       Wie viele waagerechte Streifen?          MC           zahlen2_9    waagerecht    3>w3; 5>w5; *>skizze
w3          Wie sind die Streifen verteilt?          MC           w3werte      3W            *>skizze
w5          Wie sind die Streifen verteilt?          MC           w5werte      5W
skizze      Welche Farbe hat die markierte Fläche?   farbschleife farben       farbliste
echt        —                                        bild
```

Leeres Routing heißt „nächste Zeile", `*` steht für den Rest. Von zehn Knoten routen nur vier
überhaupt — die Staffel verzweigt viel weniger, als sie sich anfühlt. Der Code wird damit ein kleiner
Interpreter: Zeile lesen, Feldwert für dieses Land holen, Step erzeugen, Routing auswerten, weiter.
Kein `if` pro Flagge, eine neue Frage ist eine neue Zeile.

**`werteliste`** — `liste | wert | text | flag`, die Antwortoptionen samt Kennzeichen für „nicht
falsch". Dieselbe Legende, die im Sheet ohnehin geführt wird.

**Distraktoren** brauchen keine eigene Spalte: Es wird zufällig aus der Werteliste gezogen, die
richtige Antwort immer dabei. Passen alle Werte in die MC-Breite (Farben, Hintergrundkategorien),
werden schlicht alle gezeigt — dann fehlt nie etwas und es gibt nichts zu schließen. Wo es mehr sind
(neun Bänderzahlen), wechseln die gezogenen Ablenker von Durchgang zu Durchgang, was denselben Zweck
erfüllt. Das ist der Mechanismus, den die bestehenden MC-Decks schon nutzen.

**In den Flaggenkarten gibt es keine Input-Steps**, alles läuft über MC. Akzeptierte Schreibweisen
sind damit kein Thema.

## 5. Renderer und Screen

**Ein GeoJSON pro Struktur, nicht pro Land.** Flächen mit 1…n benannt, die Farben stehen in der
Attributtabelle in Flächenreihenfolge. `horizontal-3` wird von Deutschland, Österreich und vielen
anderen gemeinsam benutzt.

**Zwei Flaggen können sich nur dann einen Sketch teilen, wenn sie gleich viele Flächen haben.** Die
Farbliste ist positionsbezogen — n Farben für n Flächen. Optische Ähnlichkeit reicht also nicht:
Simbabwe und Osttimor haben beide ein zweifarbiges Dreieck, aber neun gegen drei Flächen.

**Gösch und Dreieck werden in die Sketches gebacken, nicht zur Laufzeit gezeichnet** — der Generator
stempelt sie mit hinein, der Renderer bleibt dumm. Auflage an den Generator: **immer dieselbe
Gösch-Geometrie, immer dieselbe Dreieckstiefe, keine Anpassung an die Streifenzahl.** Der Grund ist
derselbe wie beim Leak: Ein Sketch, der Details richtig zeigt, die nie beantwortet wurden — Chiles
Gösch genau über dem oberen Streifen, Togos genau über dreien von fünf —, verrät sie. Die Skizze soll
die Flächen sauber zum Einfärben zeigen, nicht die Flagge möglichst getreu abbilden.

**Die Dreieckstiefe ist einheitlich, etwa 40 % der Breite.** Tschechien reicht mit 50 % am weitesten,
die Philippinen und Dschibuti liegen bei gut 43 %, Bahamas, Jordanien und Sudan bei etwa einem
Drittel. Die Ausreißer haben eigene Formwerte: Eritreas Keil läuft bis zur Flugseite durch, Kuwaits
Figur ist ein Trapez, Südafrika und Vanuatu tragen ein liegendes Y.

**Die Skizze ist schematisch, nicht formattreu.** Es gibt ein vorgegebenes Standard-Seitenverhältnis;
vorgeschlagen und bislang unwidersprochen ist **2:3** — häufigstes echtes Flaggenformat und exakt das
Format des Bildfeldes. Koordinatenraum der Strukturdateien: **180 × 120**, was die häufigen
Streifenzahlen sauber teilt (2→60, 3→40, 5→24). Der Kontrakt: *In diesem Verhältnis sind Kreise
Kreise; in ein anderes gerendert wird gestaucht oder gestreckt.*

Die echten Flaggen werden hochaufgelöst im wahren Format geladen und wie jedes andere Bild
verkleinert und mittig platziert.

**Bildbeschaffung:** SVG ist **Quellformat, nicht Laufzeitformat** — herunterladen, einmal groß als
PNG exportieren (großzügig, etwa 1500 px breit; später neu zu exportieren hieße, 200 Dateien
anzufassen), und die App lädt PNG wie bisher. Die **SVG-Quellen bleiben liegen**, dann kostet ein
hochauflösender Skin später nur einen Skriptlauf. Kein SVG-Support in der Suite nötig; falls doch
einmal, dann eher `fxsvgimage` (baut JavaFX-Knoten) als Batik (rendert über AWT nach Raster).

**Farbeinordnung nach dem Bild, das im Deck liegt** — nicht nach offiziellen Werten. Viele Länder
legen ihre Farben gar nicht fest (Usbekistan: nur „azurblau"), Pantone hat keine eindeutige
RGB-Entsprechung, und die Wikipedia-Farbtabellen widersprechen teils den dortigen SVGs (Mikronesien:
Tabelle `#ABCAE9`, Bild `#75B2DD`). Antwort und Augenschein müssen zusammenpassen, also entscheidet
das verwendete Bild. Alle 200 aus **einer** Quelle ziehen, damit Vergleiche belastbar bleiben —
Wikimedia Commons oder ein Sammelpaket wie `lipis/flag-icons`. Offizielle Werte und
Farbwechsel-Geschichten sind Stoff für Geschichten-Karten, kein Eingabewert.

**Ablauf einer Karte:** Struktur (ohne Bild) → Skelett mit hervorgehobener Fläche erscheint → Farben
füllen sich Fläche für Fläche → Gösch wird mitgezeichnet → Zusatzelemente werden nur benannt, nicht
gezeichnet → zum Abschluss die echte Flagge.

**Kein Klick ins Flaggenbild.** Alles läuft über MC und Input; das Bild bleibt reine Anzeige.

**Screen:** eigener Skin-Präfix `flagSession*`, rein additiv — die sechs Bausteine (MAP, TEXT_INPUT,
IMAGE, MC, ANSWER_SLOTS, BACK_BUTTON) werden gestaffelt aufgelöst, erst über den Map-Namen, sonst
über die Kategorie. Die Weltkarte bleibt im Deck, weil die Geschichten-Karten geografische Fragen
stellen werden. Ein durchgespielter Layoutstand, zum Ausprobieren in `worldSession*` gesetzt:

```
MapPanel      = 790,20,1100,800
ImagePanel    = 20,20,750,500      # 3:2, Skizze randlos
QuestionPanel = 410,547,360,370
McPanel       = 20,547,370,0
```

## 6. Nötige Erweiterungen der Suite

1. **Step-Expander `Flag:`** — aus Attributen eine verzweigte Step-Sequenz bauen. Der heutige Parser
   übernimmt eine CSV-Zeile 1:1; das ist ein neues Stück Logik, kein Nebeneffekt.
2. **Skizzen-Renderer** — GeoJSON rein, Flächen zeichnen, eine hervorheben, nach und nach einfärben.
   Einbahnstraße ohne Klickziele. Unabhängig von Flaggendaten und Generator baubar.
3. **Bild-Step, der zur Laufzeit gezeichnet wird** — `Image:` lädt heute eine Datei.
4. **SuiteImage: mittig einpassen statt strecken** — No-Op für alle bestehenden Decks, weil dort
   quadratische Bilder in quadratischen Feldern liegen.
5. **Input mit Enter-Bestätigung** — der heutige Input prüft nach jedem Tastendruck und verrät damit
   die Antwort, sobald der Antwortraum klein und durchprobierbar ist. Nur nötig, falls die
   Gegenrichtung nicht über den Kartenklick läuft.
6. **MC braucht eine „nicht falsch"-Option** — für Grenzfälle wie Lesotho. Der Step bleibt stehen wie
   bei einer falschen Antwort, der Fehlerzähler zählt aber nicht hoch. Damit lernt man die Grenze,
   indem man einmal dagegenstößt, und muss die kanonische Antwort trotzdem selbst produzieren.
   Braucht ein zweites Markierungszeichen im CSV neben dem `*` für die richtige Antwort.

## 7. Reihenfolge

**Der Bau kommt zuerst.** Kein halbes Jahr Datensammeln auf Verdacht: Eine heute angelegte Karte soll
morgen drankommen, und ein Modellfehler soll auffallen, solange er billig zu ändern ist. Vor dem
ersten Land muss deshalb die ganze senkrechte Scheibe stehen — Fragensatz für waagerecht,
Flaggentabelle, `Flag:`-Expander, Renderer, SuiteImage-Zentrierung, Skin-Layout. Eine billigere
Teilvariante gibt es nicht.

**Der nächste Schritt ist ein Durchstich mit fünf Flaggen.** Nicht den ganzen Waagerecht-Zweig bauen
und nicht erst alle Fragenstaffeln fertig entwerfen, sondern einmal komplett durch alle Schichten —
DB-Tabelle, `Flag:`-Expander, Renderer, Skin-Layout, echte Karte in der App — aber nur fünf Länder
breit. Die Fragen, die alles blockieren, sind Pipeline-Fragen (vor allem die Renderer-Ausgabeform),
und die beantwortet man mit fünf Flaggen, nicht mit hundert.

```
Deutschland  3 Streifen, alle gleich    einfachster Fall
Belize       3 Streifen, 3W=2           Breitenfrage
Botswana     5 Streifen, 5W=1           zweite Verteilungsfrage
Bahamas      3 Streifen + Dreieck       Dreieck im Sketch
USA          13 Streifen + Gösch        Gösch, lange Farbliste
```

Danach ist beides billig: der Rest des Waagerecht-Zweigs ist Dateneingabe, und der Senkrecht-Zweig
ist Fragendesign ohne Bauunbekannte.

**Der Renderer wird von Anfang an generisch über GeoJSON gebaut**, auch wenn am ersten Tag nur
`horizontal-3` gebraucht wird. Dann ist eine neue Struktur später eine neue Datei und kein Code —
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
**206 Flaggen, 52 Signaturen, 25 Einzelgänger**; daraus rund **58 Sketches**, weil der Spezial-Cluster
(Antigua, Bahrain, Bosnien, Kiribati, Mazedonien, Nepal, Katar) in sieben einzelne zerfällt.

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
- **Gegenrichtung** — Kartenklick auf die Weltkarte oder Enter-Input. MC scheidet aus (siehe
  Verworfen). Der Kartenklick braucht nichts Neues, der Enter-Input braucht Erweiterung 5.
- **Renderer-Ausgabeform** — fertiges Bild, dann bleibt SuiteImage nutzbar; oder direkt in den
  Szenengraph, dann Vektoren und freie Skalierung, aber eine eigene Komponente. Muss **vor** dem
  Renderer entschieden werden.
- **Die Kategorie- und Namenslisten der Elementfrage** — welche Kategorien es genau gibt und welche
  Namen darunter hängen, ist noch nicht ausformuliert. Die Struktur steht (zwei Ebenen, Ende bei
  „Wappen"), die Werte nicht.
- **Die übrigen Zweige** — senkrecht, uni, Kreuz, diagonal, Dreieck-ohne-Streifen und „anderes".
  Der senkrechte dürfte schnell gehen, weil er dieselbe Struktur hat, nur gedreht: 22 Dreier,
  4 Zweier, und die Breitenfrage heißt dort „linker/rechter breiter" statt „oberster". Kanada und
  St. Vincent sind dort 1:2:1, Pakistan und Portugal ungleiche Zweier.
- **Wie Gösch und Dreieck in den Sketch kommen** — in jede Kombination hineingezeichnet (mehr Dateien,
  Renderer bleibt dumm, Flächennummerierung steht in der Datei) oder zur Laufzeit als weiteres Polygon
  darübergelegt (weniger Dateien, dafür Nummerierung zur Laufzeit berechnet). Tendenz zur Laufzeit,
  weil die genaue Ausrichtung zu den Streifen nicht wichtig ist.
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
| Der Begriff „Fimbrierung" | Fachlich richtig, aber er zwingt zu einer Grad-Entscheidung („Strich oder Streifen?") und produzierte laufend Grenzfälle — Suriname, Eswatini, Gambia. Ersetzt durch die vergleichende Formulierung „Streifen 2 und 4 klar am dünnsten", die ohne Fachbegriff und ohne Schwellenwert auskommt. |
| Bilderauswahl statt Zahlenfrage bei der Streifenzahl | Eine Bilderliste kann nur zeigen, was vorkommt — dann fehlt die Option für die Flagge, die man sich falsch vorstellt. Attrappen-Sketches dagegen sprengen die acht Optionen. |

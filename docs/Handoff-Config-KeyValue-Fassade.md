# Handoff: Config/KeyValue-Fassade

Dieses Dokument fasst eine abgeschlossene Design-Diskussion zusammen, damit wir in einem frischen Chat direkt bei der Implementierung starten können. Es hält die getroffenen Entscheidungen, ihre Begründungen und die bewusst verworfenen Alternativen fest, damit nichts neu diskutiert werden muss. Am Ende stehen die konkreten offenen Punkte für die Umsetzung.

## Ausgangslage

Zwei Klassen verwalten heute Werte, an getrennten Aufrufstellen und mit unterschiedlichen Kontrakten:

- **Config** liest zu Beginn eine Config-Datei ein, gibt Werte über get-Methoden nach außen und schreibt am Ende der Suite die Datei neu, falls per set etwas geändert wurde. Wirft bei unbekanntem Key.
- **KeyValueRepository** hat ebenfalls get/set, geht aber auf eine DB-Tabelle. Gedacht für Werte, die sich im Lauf ändern und daher öfter geschrieben werden. Liefert heute bei fehlendem Key `Optional.empty()` (nicht throw — dieser Punkt wird unten aufgelöst).

Beide liegen zusammen mit DB in `shared`.

## Teil 1 — bereits ausgereift und entschieden

Dieser Teil ist unstrittig und gilt als beschlossen:

- Die noch fälschlich zur Laufzeit änderbaren Keys wandern aus der Config in die KeyValue-Tabelle. Dort gehören veränderliche Werte hin.
- Die save-Methode der Config entfällt. Das File kann aus der Suite heraus **nicht mehr** verändert werden — es ist ab jetzt read-only.
- Nach Teil 1 hat Config folglich keine setter mehr, nur noch getter.

## Kernidee — die Fassade

Das eigentliche Ziel ist **keine** Speicherort-Kaschierung, sondern das Beseitigen einer Leckage: Mit zwei öffentlichen Klassen ist die Partition „veränderlich → Tabelle, unveränderlich → Datei" an *jeder* Aufrufstelle implizit mitcodiert. Jede Stelle „weiß" es dadurch, dass sie die eine oder andere Klasse greift. Dieses Wissen soll raus aus den Features.

Eine Fassade zentralisiert die Partition. Features holen sich nur noch ihre Werte und schreiben sie, ohne wissen zu müssen, was in Datei und was in Tabelle liegt. Das ist ein echter Kapselungsgewinn, kein kosmetischer.

### Was die Fassade wirklich versteckt

Nicht den *Speicherort*, sondern die **Herkunft** eines Werts. Es gibt drei Herkünfte, nicht zwei:

- **Roher Datei-Wert** — aus dem File gelesen, unveränderlich.
- **Berechneter (computed) Wert** — z. B. `dbFolder`, abgeleitet aus `folderPath` (`computedProps.put("dbFolder", folderPath + "data/")`). Hat *gar keinen* Speicherort, ist nach dem Laden sofort da, read-only. Die Berechnung soll an genau einer Stelle sitzen, nicht in der Suite verstreut.
- **Tabellen-Wert** — veränderlich, schreibbar.

Das Einzige, was ein Aufrufer von der Herkunft je braucht, ist: **darf ich schreiben?** Datei und computed → nein. Tabelle → ja. Für Routing und Prüfungen gilt daher: die „unveränderliche Menge" ist **Datei ∪ computed**.

## Zentrale Design-Entscheidung — neue Fassade über zwei internen Stores

Nicht die bestehende Config zur Fassade befördern, sondern eine **neue Fassade** über zwei package-private Stores legen:

- einen **read-only FileStore** (das heutige Config-Innere: liest die Datei, hält rohe + computed Keys),
- **KeyValueRepository** als read-write Store auf der DB.

Die Fassade ist die einzige öffentliche Klasse. Sie kennt als Einzige die vollständige Partition. Lesen und Schreiben laufen über sie; KeyValueRepository wird interner Kollaborateur, Features halten nur noch die Fassade.

### Warum nicht Config befördern — der Konstruktionszyklus

Würde Config selbst zur Fassade, entstünde ein **Konstruktionszyklus** (nicht Laufzeit, nicht Paket-Ebene): Config-Fassade braucht KeyValueRepo → KeyValueRepo braucht DB → DB braucht `dbFolder` aus Config → zurück bei Config. Das ist die geahnte „Katze beißt sich in den Schwanz".

Wichtig zur Einordnung: Dieser Zyklus ist **keine rote Kante** im Paket-Graphen — alle drei Klassen liegen in `shared`, es gibt also gar keine paketübergreifende Kante. Er ist auch kein wechselseitiges Klassen-*Kennen* (das ist harmlos), sondern wechselseitiges *Brauchen beim Bauen*. So etwas kompiliert klaglos durch und zeigt sich erst, wenn man eine gültige Konstruktionsreihenfolge sucht und keine findet. Ein FailFast-Throw crasht ihn nicht weg — er ist eine Struktureigenschaft.

Die Zwei-Store-Fassade zerschlägt dieses Brauchen und macht den Bootstrap **streng linear**:

**FileStore** (liest Datei, kennt `dbFolder`) → **DB** (nimmt `dbFolder` aus dem FileStore, *nicht* aus der Fassade) → **KeyValueRepository** (auf der DB) → **Fassade** (oben über beiden Stores).

Der Clou: Die heutige Reihenfolge ist bereits Config → DB. Der FileStore erbt genau diese Position; die Fassade setzt sich als neue oberste Schicht obendrauf. Es wird nichts umgekehrt. Würde Config befördert, kehrte man die gesunde Richtung um und handelte sich den Zyklus ein. Deshalb ist „Fassade oder Config befördern" **keine** vertagbare Implementierungsfrage, sondern die Entscheidung, ob der Bootstrap baubar bleibt.

## Schreibseite — der set-Kontrakt

- Fassade-`set` auf einen **Tabellen-Key**: schreibt (delegiert an KeyValueRepository).
- Fassade-`set` auf einen **Datei-/computed-Key**: **wirft** sofort.

Der werfende Setter ist **Gewinn, kein Preis**. Heute kann `KeyValueRepo.set("einFileKey", …)` stumm eine Tabellenzeile für einen Key anlegen, der ins File gehört — ein stiller FailFast-Verstoß, der im aktuellen Split lebt. Nur die Fassade kennt die vollständige Partition und kann diesen Fall als Bug erkennen und hart crashen. FailFast greift hier so früh wie möglich: unbekannt-schreibbar → Abbruch, du gehst an den Code.

Hinweis zur Compile-Zeit: Bei String-Keys (`set("foo")`, nicht `setFoo()`) gibt es *auf keiner Seite* Compiler-Schutz. Das war kein Argument für den Split. Wandert ein Key doch mal zwischen unveränderlich und veränderlich, ist der Merge sogar leichter: ein Eintrag in der Routing-Wahrheit (der FileStore-Menge) statt Handarbeit an allen Aufrufstellen. Reales Wandern von Keys gibt es an sich nicht.

## Startup-Invarianten

Zwei Zusicherungen beim Start, beide FailFast:

- **Kollisions-Check.** Kein Tabellen-Key trägt den Namen eines Keys aus der unveränderlichen Menge (Datei ∪ computed). Umsetzung: die unveränderliche Menge ist ohnehin im Speicher; einmalig jede Tabellenzeile dagegen prüfen, hart crashen bei Kollision. Diese Kollisionsmöglichkeit entsteht erst durch den gemeinsamen Namensraum der Fassade — der Check ist die neue Invariante, die genau das bewacht. Billig und einmalig.
- **DB-Bereitschaft.** Ein Tabellen-Key, der vor DB-Init gelesen wird, führt zum harten Abbruch. Das braucht **keinen** Extra-Guard: Die Fassade reicht Tabellen-Keys an die DB durch; ist die DB noch nicht da, wirft sie. Aus dem heute *stillen* Problem (wer KeyValueRepo vor der DB anfasst, kracht sowieso) wird an der Fassade ein sauberes FailFast.

Die Bereitschaftskopplung ist sicher, weil die Bootstrap-Phase per Konstruktion nur unveränderliche Keys berührt: `dbFolder` (computed) wird gebraucht, um die DB überhaupt zu öffnen, kann also unmöglich in der DB liegen. Tabellen-Keys werden im Code erst deutlich später abgefragt.

## Optional-Rausschmiss und Erstlauf-Disziplin

Das `Optional` von KeyValueRepository fällt **ersatzlos** weg. Kontrakt künftig: **throw on miss**, uniform für beide Stores und damit für die Fassade. Ein unbekannter Key ist per Design ein Bug im gerade entstehenden Code — beim Anlegen eines neuen Features mit neuem Key wird der Key vorher mit passendem Wert in Datei oder Tabelle hinterlegt.

Wichtige Klärung zum Erstlauf (z. B. Import-Anchor eines Importers beim allerersten Lauf): Das ist **kein** Sonderpfad und **keine** legitime Abwesenheit, die im Code über eine Exception als Kontrollfluss abgefangen werden müsste. Der Erstlauf ist **unter voller Kontrolle** — der Anchor wird vor dem ersten Lauf von Hand mit Startwert angelegt, dieselbe Disziplin wie bei jedem neuen Key. Damit ist der Key beim Erstlauf vorhanden, `empty` trägt **nirgends** Bedeutung, und es gibt keine zweite Kategorie „operativer Zustand, der fehlen darf".

Folge für die Aufrufstellen: Wo heute `Optional.empty()` behandelt wird, ist das **toter Code** — ein else-Zweig für einen Fall, den die Betriebsdisziplin nie eintreten lässt. Dieser Code wird beim Umstellen auf throw **gelöscht**, nicht ersetzt. Stellen, die das Optional ohnehin direkt auspacken, ändern nur ihren Rückgabetyp.

## Die finale Grenze

Die einzige tragende Unterscheidung ist **schreibbar (Tabelle) gegen nicht schreibbar (Datei/computed)**, und sie sitzt hinter der Fassade. Ausdrücklich verworfen als tragende Grenzen wurden:

- Datei-vs-Tabelle (nur Speicherort, für Aufrufer irrelevant),
- mutable-vs-immutable als sichtbare Aufrufstellen-Asymmetrie (soll gerade verschwinden),
- Konfiguration-vs-Laufzeitzustand (ein Phantom: es beschrieb einen Unterschied im Fehl-Kontrakt, den es hier nicht gibt, weil Abwesenheit betrieblich ausgeschlossen ist).

## Bewusst verworfene Alternativen

Damit wir sie nicht erneut aufmachen:

- **Config zur Fassade befördern** — erzeugt den Konstruktionszyklus (siehe oben).
- **Nur die getter vereinheitlichen (Halb-Merge)** — löst den Schmerz nicht: Schreiber hielten weiter eine Referenz auf KeyValueRepository, die „eine Referenz für alles"-Welt entstünde gar nicht.
- **Sentinel-Wert für den Erstlauf** (0/-1 als „kein vorheriger Import") — überkompliziert. Nicht nötig, weil der Erstlauf per Betriebsdisziplin gelöst ist: Key nicht vorhanden → Exception, fertig.
- **Optional bedeutungstragend erhalten** — hinfällig, da keine legitime Abwesenheit existiert.

## Offene Punkte für die Implementierung

1. **Konstruktionsreihenfolge verifizieren.** Sicherstellen, dass die DB ihren `dbFolder` aus dem FileStore zieht und **nicht** von der Fassade abhängt, damit der Bootstrap linear bleibt (FileStore → DB → KeyValueRepo → Fassade). Der heutige Ablauf ist Config → DB; das ist die zu erhaltende Richtung.
2. **Sichtbarkeit umstellen.** FileStore (heutiges Config-Innere) und KeyValueRepository package-private; Fassade als einzige öffentliche Klasse.
3. **Benennung der Fassade.** „Config" wäre streng genommen gelogen, sobald auch Tabellenwerte geliefert werden — aber kleinstes Problem, in der Umsetzung zu entscheiden. Bestehende Klassen werden dabei nicht zur Lüge: Config-Innere bleibt reiner Datei-Store, KeyValueRepo bleibt KeyValueRepo.
4. **Routing + unveränderliche Menge.** Regel: Key in der unveränderlichen Menge (Datei ∪ computed) → FileStore; sonst → durchreichen an KeyValueRepository. Die computed Keys müssen in diese Menge, sonst greift der Kollisions-Check nicht.
5. **throw on miss umsetzen.** Beide Stores und die Fassade werfen bei unbekanntem Key. Optional aus KeyValueRepository entfernen.
6. **Aufrufstellen durchgehen.** Rein mechanisch: `Optional.empty()`-Behandlung als toten Code löschen; direkt-auspackende Stellen auf neuen Rückgabetyp umstellen. Keine Sortierung nach Bedeutung mehr nötig.
7. **Startup-Checks einbauen.** Kollisions-Check und (implizit über Durchreichen) DB-Bereitschafts-FailFast.

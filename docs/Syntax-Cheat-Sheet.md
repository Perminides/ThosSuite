# Syntax-Cheat-Sheet

## Präambel
Du brauchst ein Komma oder ein Pipe im Text? Escapen mit Backslash sollte immer funktionieren.

## Input

### Einfach

Was sind die Schum-Städte?

```text
Input:Speyer,Speier|Worms|Mainz|-Trier
```

- Es kann nach einem Wort gesucht werden.
- Es kann nach einer `|`-getrennten Liste von Wörtern gesucht werden.
- Es können akzeptierte Alternativantworten durch Komma getrennt angegeben werden.
- Es können Stoppwörter durch `|-` angegeben werden, deren Tippen die Eingabe sofort als falsch wertet.

### Mit einer Stopp-Liste

Was sind die drei größten Städte Niedersachsens?

```text
Input:Städte:Hannover|Braunschweig|Oldenburg|~Osnabrück
```

- Es kann nach einer Menge von Wörtern gesucht werden.
- Es können mittels `|~` eigentliche Stoppwörter aus der Liste erlaubt werden.
- Alternativantworten und Stoppwörter sind hier nicht gewollt. Die gehören wenn dann in die Liste!

## MC

### Ohne Absenden (Falscher Klick beendet sofort)

Finde alle europäischen Länder:
```
MC:+Schweiz|Japan|+Island|~-Türkei|-Australien|Brasilien|Guam|China|USA|Mexiko|~Grönland
```

- Klick auf Japan, Brasilien, Guam, China, USA oder Mexiko beendet den Step sofort.
- \+ → Sobald Schweiz und Island angeklickt wurden, geht es weiter
- ~ → Türkei und Grönland schütteln sich beim Draufklicken, beenden den Step aber nicht
- \- → Auch ein Klick auf Australien beendet den Step sofort. Australien ist ein fester Distraktor und wird immer als Option angezeigt
- \-~ → Auch die sich schüttelnde Türkei wird immer angezeigt.

### Mit Absenden
MC+ hat die gleiche Syntax. Der Unterschied ist, dass die geklickten Antworten erst nach Absenden geprüft werden.

## Fast

### Unsortiert

Nenne einen Gott der Unterwelt.
```
Fast:10:any1:Hades,Haades|Pluto
```

- Es gibt 10 Sekunden pro Antwort
- Es muss 1 korrekte Antwort gegeben werden
- Wenn hinter any keine Zahl folgt, müssen alle Antworten gegeben werden
- Alternativschreibweisen werden durch Komma getrennt
- Unterschiedliche Antworten werden durch | getrennt

### Sortiert
Nenne die beiden Staatschefs von Deutschland
```
Fast:5:ordered:<Staatsoberhaupt>Steinmeier|<Regierungschef>Merkel
```

- Es gibt 5 Sekunden pro Antwort
- Die Antworten müssen in der Reihenfolge, wie sie im Step stehen gegeben werden
- Es können Hinweise in < > gegeben werden. Dies ist auch bei any ohne Anzahl erlaubt. Aber nicht bei any5, da dort dann nur 5 Felder als aktiv markiert werden. Welche Hinweise sollten gezeigt werden, wenn es mehr korrekte Antworten gibt?
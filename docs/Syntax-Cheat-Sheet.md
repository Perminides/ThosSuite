# Syntax-Cheat-Sheet

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

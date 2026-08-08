# ThosSuite — Das Startproblem: Stand der Diagnose

**Stand:** 07.08.2026 — nach der ersten Auswertung von `StartupDiagnostics`

**Charakter:** Ein *Ermittlungsprotokoll*, kein Architekturdokument. Hier steht, was am Startproblem
gesichert ist, was widerlegt wurde und was noch offen ist. Es wächst mit jedem Befund und fällt
zusammen mit `app.shared.ui.StartupDiagnostics` weg, sobald die Ursache feststeht.

## Das Symptom

Nach dem Start ist das Hauptfenster vollständig und korrekt gezeichnet. Ein Klick auf ein Menü
bewirkt sichtbar nichts. Sobald die Maus das Fenster einmal verlassen hat und wieder hineinkommt —
oder ein Dialog erscheint —, ist alles normal.

Tritt sporadisch auf. Auf dem Arbeitslaptop deutlich häufiger als privat.

## Was gesichert ist

**Die Eingabe kommt an.** Das Eingabeprotokoll hängt als Filter an der Scene und schreibt Klicks
mit, bevor ein Knoten sie behandeln kann. Im Fall vom 07.08. 14:22 steht dort:

```
14:22:16  EINGABE klick x=230 y=20 ziel=LabeledText
14:22:16  === REQUEST SESSION SWITCH ===
14:22:16  Starte AnkiSession Hannover
```

Der Klick wurde also verarbeitet und die Session gestartet — während auf dem Schirm nichts davon zu
sehen war.

**Die Darstellung hängt nicht komplett.** Ein Label im Fenster zählte im Sekundentakt sichtbar
hoch, während der Rest der Fläche einen veralteten Stand zeigte. Notiz aus dem Log:

> Lernen und darunter geklickt. Keine Reaktion. Zahl zählt hoch. […] mein Click wurde also sehr wohl
> registriert, nur das Fenster nicht aktualisiert.

**Der FX-Thread läuft.** Die Pulse-Zahl liegt in allen Fällen stabil bei 60–70 pro Sekunde, auch
während des Phänomens.

**Daraus folgt:** Es ist weder ein hängender Event-Loop noch ein Totalausfall der Darstellung
noch fehlende Eingabe. Es ist ein **partielles Darstellungsproblem** — ein Bereich der Fläche wird
aktualisiert, ein anderer nicht.

## Was widerlegt ist

| Hypothese | Widerlegt durch |
|---|---|
| Die Deckkraft steht noch auf 0 | `opacity=1.0` in allen Zeilen während des Phänomens |
| Der FX-Thread steht | Pulse laufen normal weiter |
| Die Klicks erreichen die Anwendung nicht | Eingabeprotokoll und ausgelöster Session-Switch |
| `Sound` (neue Klasse) stört beim Start | Nutzt `javax.sound.sampled`, lädt lazy, wird nur aus `FastWriteLearnView` gerufen — beim Start unbeteiligt |

## Die Häufigkeitsverteilung

Aus den `BEFUND`-Zeilen im Log:

```
03.08. – 06.08.    ~20 Starts    0 kaputt
07.08.             ~24 Starts    8 kaputt
```

Kein gleitender Anstieg, sondern ein Bruch an einem Tag. Am 07.08. kam das Deck **Fast Write**
dazu. Eine Notiz aus dem Log um 13:37 lautet: „Start war durch Fast Write vermokelt".

Das ist eine Korrelation, kein Nachweis — und die Diagnose läuft erst seit dem 03.08., davor gibt es
keine Zahlen. Das Phänomen selbst ist älter.

## Die offene Hypothese

Der `AnkiDeckService`-Konstruktor lädt beim Start **alle** Anki-Decks vollständig (`getAllHints`),
nicht nur die fälligen — Fast Write also mit. Er endet mit einem expliziten `System.gc()`
(seit 02.06. im Code, unverändert).

Dieser Konstruktor läuft im Controller-Konstruktor, also **auf dem FX-Thread**, unmittelbar nachdem
das MainWindow gebaut und gestylt wurde. Bei `-Xmx4g` ist das eine Stop-the-World-Pause in genau
der Aufbauphase.

JavaFX rendert in einem **eigenen Thread**, der eine Kopie des Szenengraphen führt und sie pro Puls
mit dem FX-Thread abgleicht. Die Vermutung: Eine lange Pause in dieser Phase lässt diese Kopie
teilweise veralten — was zum beobachteten Bild passt, dass ein Bereich aktualisiert wird und ein
anderer nicht.

**Das ist unbelegt.** Mit einem Deck mehr sammelt `System.gc()` mehr ein, was den Bruch am 07.08.
erklären würde — aber gemessen ist davon nichts.

## Was als Nächstes gemessen wird

1. **Startphasen mit Dauer** — Config, Fonts, MainWindow-Aufbau, `new Controller(…)`, PreTasks,
   jeder PostTask, das Zurückdrehen der Deckkraft.
2. **Darin die Dauer von `System.gc()`** samt Heap davor und danach. Unterscheiden sich kaputte
   Starts hier systematisch von heilen, ist die Ursache gefunden; unterscheiden sie sich nicht, ist
   die Hypothese widerlegt.
3. **Erst danach, falls nötig:** ein zweiter Zähler im `contentPane`. Er beantwortet die andere
   offene Frage — ob wirklich nur der Inhaltsbereich steht oder ob bisher zufällig auf den lebenden
   Teil geschaut wurde.

## Was die Diagnose misst — und was nicht

Behalten, weil unsichtbar und im Ernstfall aussagekräftig:

- **Eingabeprotokoll** (Filter auf der Scene): Klicks, Tasten, Maus-Ein-/Austritte
- **Sekundenzeile**: Pulse, Deckkraft, Fokus, Fensterzahl, Szenenmaße
- **Nachfrage am Ende**: das einzige Mittel, einen Log-Block als interessant zu markieren

Entfernt, weil ihre Frage beantwortet ist:

- **Der sichtbare Zähler** — hat den entscheidenden Befund geliefert (siehe oben) und misst nichts
  Neues mehr
- **Das Probe-Menü** — hat nie einen Befund geliefert und griff ins Geschehen ein: Am 07.08. stand
  es offen, als der Klick auf „Lernen" kam, und wurde dadurch geschlossen

**Eine bekannte Lücke:** Der Eingabefilter hängt an der Haupt-Scene und sieht deshalb **keine**
Klicks in Menü-Popups — die sind eigene Fenster mit eigener Scene. Im Fall 14:22 steht der Klick auf
die Menüleiste im Log, der Klick auf den Deck-Eintrag darunter nicht, obwohl er gewirkt hat. Ein
leeres Protokoll beweist also nicht, dass nicht geklickt wurde.

## Umgebung

Aus allen bisherigen Läufen identisch:

```
prism.order=null   prism.allowhidpi=false   prism.lcdtext=false
glass.win.uiScale=null   sun.java2d.uiScale=null
outputScale=1.0x1.0   bounds=2560x1440   scene=1910x1042
```

`prism.order=null` heißt: Standard-Pipeline. **Welche daraus tatsächlich wurde (D3D oder Software),
ist bislang nicht erfasst** — ein Fakt, der für die Ursachensuche noch fehlt.

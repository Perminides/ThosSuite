# ThosSuite — Das Startproblem: Stand der Diagnose

**Stand:** 08.08.2026 — Ursache gefunden, Gegenprobe steht aus

**Charakter:** Ein *Ermittlungsprotokoll*, kein Architekturdokument. Hier steht, was am Startproblem
gesichert ist, was widerlegt wurde und was noch offen ist. Es fällt zusammen mit
`app.shared.ui.StartupDiagnostics` weg, sobald die Gegenprobe durch ist.

## Das Symptom

Nach dem Start ist das Hauptfenster vollständig und korrekt gezeichnet. Ein Klick auf ein Menü
bewirkt sichtbar nichts. Sobald die Maus das Fenster einmal verlassen hat und wieder hineinkommt —
oder ein Dialog erscheint —, ist alles normal.

Tritt sporadisch auf. Auf dem Arbeitslaptop deutlich häufiger als privat.

## Der Befund: JDK-8367557

Ein bekannter Fehler in JavaFX, gemeldet als **JDK-8367557 „Extended stage seems to hang after drag
and drop"**. Die Beschreibung im Bug-Report ist wörtlich das Symptom oben:

> the application appearing unresponsive until the mouse cursor leaves and re-enters the
> application window

**Die Ursache laut dem eingereichten Patch:** Windows schickt dem Fenster eine `WM_MOUSELEAVE`, obwohl
der Zeiger noch über der Fensterfläche steht. Damit ist der Maus-Erfassungsstatus in `GlassWindow`
falsch: Das Fenster hält sich für „Maus draußen", registriert die Verfolgung deshalb nicht neu und
bekommt keine Mausnachrichten mehr. Ein *echtes* Verlassen setzt den Status zurück — und genau das
ist die Heilung, die wir beobachten.

Betroffen ist ausschließlich `StageStyle.EXTENDED`, das Preview-Feature aus JavaFX 25. Die Suite
nutzt es für Hauptfenster und Dialoge (`MainWindow`, `SuiteDialog`, `Alerts`).

Der Fix ist [PR openjdk/jfx#2003](https://github.com/openjdk/jfx/pull/2003), integriert am
09.12.2025, und steht in den **Release Notes von JavaFX 26**. Dort finden sich noch weitere
EXTENDED-Korrekturen — JDK-8368021, JDK-8371106, JDK-8370446, JDK-8369836. Das Preview-Feature wurde
in 26 spürbar nachgearbeitet.

**Konsequenz im Build:** `javafx.version` steht seit dem 08.08. auf 26. JavaFX 26 ist gegen Java 24
übersetzt und läuft auf JDK 25 — die jpackage-Bindung an JDK 25 (Manifest-Regression JDK-8284675)
bleibt davon unberührt.

Der Sprung kostete genau eine Anpassung: JDK-8369836 hat `HeaderBar.leading`/`trailing` in
`left`/`right` umbenannt (Preview-Feature, darf brechen). Grund ist JDK-8368021 — richtungsrelative
Namen saßen im RTL-Modus auf der falschen Seite. Betroffen war nur `MainWindowHeaderBar`; da links
hier links bleibt, ist die Umbenennung verhaltensgleich.

### Was daran noch nicht bewiesen ist

Im Bug-Report ist der Auslöser **Drag-and-Drop**, bei uns der **Deckkraftwechsel** beim
Sichtbarwerden. Übereinstimmung besteht bei Symptom, Heilung und betroffener Komponente, nicht beim
Auslöser. Es kann derselbe Fehler mit zwei Wegen hinein sein oder ein zweiter im selben Mechanismus.

Die Gegenprobe entscheidet das: Bleibt es unter JavaFX 26 sauber, war es dieser Fehler. Tritt es
weiter auf, war der Mechanismus richtig erkannt, der Auslöser aber ein eigener — und dann gehört ein
eigener Bug-Report geschrieben.

## Die Messung, die dorthin führte

Seit dem 08.08. stehen Millisekunden im Zeitstempel (`Log`-Formatter, `!tmp`). Erst damit sind
Deckkraftwechsel und Mauseintritt zu trennen — vorher lagen sie in derselben Sekunde.

Über 16 Läufe aufgetragen: der Abstand zwischen `opacity=1` und dem ersten `MOUSE_ENTERED`.

| Abstand `opacity=1` → erstes `MOUSE_ENTERED` | Läufe | Eingabe danach |
|---|---|---|
| 1–2 ms | 3 | **keine** |
| 4 ms – 563 ms | 13 | ja |

Eine saubere Trennung ohne Überlappung. Fällt der Mauseintritt in den Umschaltmoment, ist das
Fenster taub; liegt er auch nur vier Millisekunden daneben, läuft alles.

Das erklärt zwanglos, warum das Phänomen sporadisch auftritt: Es hängt daran, wo der Zeiger gerade
liegt und wie lange der Start dauert. Und es erklärt, warum ausgerechnet Maus-raus-und-wieder-rein
es auflöst.

### Die Ereignisse sind gestaut, nicht verloren

Der entscheidende Zusatzbefund. Als die Maus das Fenster endlich verließ und wieder betrat, kamen
**neun Ereignisse innerhalb von 32 Millisekunden** an — darunter der zwanzig Sekunden alte Klick,
ohne dass er wiederholt worden wäre.

Das schließt aus, dass die Anwendung die Eingabe verwirft oder ignoriert. Sie bekommt sie schlicht
nicht — und holt sie nach, sobald der Kanal wieder offen ist. Damit liegt das Problem unterhalb von
JavaFX' Ereignisverteilung, in der Anbindung ans Fenstersystem.

## Was sonst gesichert ist

**Das Fenster bekommt keine Eingabe.** Im kaputten Start vom 08.08. 13:06 steht zwischen dem
Sichtbarwerden und dem erlösenden `maus verlaesst fenster` **dreißig Sekunden lang kein einziges
Eingabe-Ereignis** — obwohl in der Zeit geklickt und die Maus bewegt wurde. Im heilen Start
unmittelbar danach stehen dort Bewegungen bei t=3s, t=4s und t=8s. Nicht einmal Mausbewegungen
kommen an.

**Die Darstellung ist in Ordnung.** Eine mitzählende Zahl lief während des Phänomens sichtbar
weiter. Sie zeigte nicht, dass „ein Teil noch lebt" — sie zeigte, dass das Zeichnen nie das Problem
war. Es gab schlicht nichts Neues zu zeigen, weil keine Eingabe ankam.

**Der FX-Thread läuft.** Die Pulse-Zahl liegt in allen Fällen stabil bei 60–70 pro Sekunde, auch
während des Phänomens.

**Es läuft die D3D-Pipeline.** Bestätigt am 08.08. über `prism.verbose` (`Growing pool D3D Vram
Pool`), nicht Software-Rendering. Der Schalter ist danach wieder aus dem Build genommen worden.

## Was widerlegt ist

| Hypothese | Widerlegt durch |
|---|---|
| Die Deckkraft steht noch auf 0 | `opacity=1.0` in allen Zeilen während des Phänomens |
| Der FX-Thread steht | Pulse laufen normal weiter |
| Die Darstellung hängt | Die mitzählende Zahl lief sichtbar weiter |
| Software-Rendering statt D3D | `prism.verbose` zeigt den D3D-VRAM-Pool |
| Die Eingabe geht verloren | Sie kommt beim Heilen vollständig nach |
| `Sound` (neue Klasse) stört beim Start | Nutzt `javax.sound.sampled`, lädt lazy, wird nur aus `FastWriteLearnView` gerufen — beim Start unbeteiligt |
| Die Ladelast beim Start | Siehe unten |

## Die Gegenprobe

**Was zu tun ist:** eine Serie Starts unter JavaFX 26, mit weiterlaufender `StartupDiagnostics`, und
die `BEFUND`-Zeilen auszählen.

**Wie viele:** Bei der bisherigen Quote von rund einem Drittel liegt die Wahrscheinlichkeit, zwanzig
saubere Starts hintereinander zufällig zu erwischen, bei etwa 0,03 %. **Zwanzig reichen.** Zehn
wären mit 2 % noch grenzwertig.

**Wo:** möglichst auf dem Arbeitslaptop — dort tritt das Phänomen häufiger auf, die Serie ist damit
aussagekräftiger. Deshalb steht der Rechnername im Log.

Erst danach fallen `StartupDiagnostics`, `KonsolenMitschrift`, der Millisekunden-Formatter in `Log`
und dieses Dokument weg.

## Die Häufigkeitsverteilung

Aus den `BEFUND`-Zeilen im Log:

```
03.08. – 06.08.    ~20 Starts    0 kaputt
07.08.             ~24 Starts    8 kaputt
08.08.              16 Starts    3 kaputt
```

Kein gleitender Anstieg, sondern ein Bruch an einem Tag. Am 07.08. kam das Deck **Fast Write** dazu.
Eine Notiz aus dem Log um 13:37 lautet: „Start war durch Fast Write vermokelt".

Das ist eine Korrelation, kein Nachweis. Die Diagnose läuft erst seit dem 03.08., davor gibt es
keine Zahlen; das Phänomen selbst ist älter. Am 07.08. gab es außerdem mehr Starts als an den
Vortagen zusammen — ein Entwicklungstag, möglicherweise auf einem anderen Rechner. Da das Phänomen
geräteabhängig unterschiedlich oft auftritt, können in der Tabelle zwei Populationen vermischt sein.

Mit JDK-8367557 als Ursache braucht der Bruch keine eigene Erklärung mehr: Ein zusätzliches Deck
verschiebt die Startdauer um Millisekunden, und genau darauf kommt es nach der Tabelle oben an.

## Verworfene Spur: die Ladelast

Der `AnkiDeckService`-Konstruktor lädt beim Start **alle** Anki-Decks vollständig (`getAllHints`)
und endet mit einem expliziten `System.gc()` — auf dem FX-Thread, unmittelbar nachdem das
MainWindow gebaut wurde. Bei `-Xmx4g` wäre das eine Stop-the-World-Pause in der Aufbauphase, und
die Vermutung war, dass sie die Kopie des Szenengraphen im Render-Thread teilweise veralten lässt.

**Hinfällig:** Fast Write hat 25 Karten und keine Landkarte. Die zusätzliche Last ist zu klein, um
irgendetwas zu erklären. `System.gc()` steht außerdem seit 02.06. unverändert im Code.

## Eine Fehldeutung, die korrigiert wurde

Bis zum 08.08. stand hier, die Eingabe komme an und es handle sich um ein *partielles
Darstellungsproblem*. Grundlage war der Klick im Log vom 07.08. 14:22. Übersehen wurden dabei die
zwei Zeilen unmittelbar davor:

```
14:22:16  EINGABE maus verlaesst fenster     ← die Heilung
14:22:16  EINGABE maus betritt fenster
14:22:16  EINGABE bewegung x=230 y=20
14:22:16  EINGABE klick x=230 y=20
```

Der Klick kam **nach** dem Maus-Aus-und-Wieder-Ein an, nicht währenddessen. Damit ist die
Beobachtung dieselbe wie am 08.08.: Während des Phänomens erreicht die Anwendung nichts.

## Was die Diagnose misst — und was nicht

Behalten, weil unsichtbar und für die Gegenprobe nötig:

- **Eingabeprotokoll** (Filter auf der Scene): Klicks, Tasten, Maus-Ein-/Austritte
- **Sekundenzeile**: Pulse, Deckkraft, Fokus, Fensterzahl, Szenenmaße
- **Millisekunden im Zeitstempel**: ohne sie ist der Abstand aus der Tabelle oben nicht messbar
- **Nachfrage am Ende**: das einzige Mittel, einen Log-Block als interessant zu markieren

Entfernt, weil ihre Frage beantwortet ist:

- **Der sichtbare Zähler** — hat den entscheidenden Befund geliefert und misst nichts Neues mehr
- **Das Probe-Menü** — hat nie einen Befund geliefert und griff ins Geschehen ein: Am 07.08. stand
  es offen, als der Klick auf „Lernen" kam, und wurde dadurch geschlossen
- **`-Dprism.verbose=true`** — hat D3D bestätigt, danach nur noch Rauschen im Log

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

`prism.order=null` heißt Standard-Pipeline; daraus wurde ausweislich `prism.verbose` **D3D**.

## Quellen

- [RFR: 8367557 — Extended stage seems to hang after drag and drop](http://www.mail-archive.com/openjfx-dev@openjdk.org/msg23562.html)
- [JDK-8367557 im Bug-Tracker](https://bugs.openjdk.org/browse/JDK-8367557)
- [openjdk/jfx PR #2003](https://github.com/openjdk/jfx/pull/2003)
- [JavaFX 26 Release Notes](https://github.com/openjdk/jfx/blob/jfx26/doc-files/release-notes-26.md)
- [JDK-8313424: JavaFX controls in the title bar (Preview)](https://bugs.openjdk.org/browse/JDK-8313424)

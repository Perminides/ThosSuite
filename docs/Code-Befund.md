# ThosSuite — Code-Befund

Die Bewertung zur beschreibenden Aufnahme in `Code-Befund-Karte.md`. Geprüft wird der Zuschnitt
von Verantwortung — was eine Klasse weiß, was sie wissen darf, wo dieselbe Zuständigkeit zweimal
liegt, ob die Schichten halten —, nicht die Implementierung einzelner Methoden.

Maßstab: `Design-Regeln.md`, `ArchitekturRegelnTest.java`, `Architektur-Dokumentation.md`,
`Feature-Details.md`. Umfang: `src/main/java/app` ohne das Paket `scripts`.

## Was tragfähig ist

Kein Lob, sondern eine Liste: benannte Entscheidungen, die im Code nachweisbar halten und die
beim Aufräumen nicht mit angefasst werden sollten. Jede ist beim Lesen der jeweiligen Gruppe
überprüft worden, nicht aus der Doku übernommen.

**Die JavaFX-Grenze, in der harten Fassung.** Kein Feature-Paket importiert `javafx` — geprüft
über alle acht Features. Und sie ist nicht nur gegreppt bestanden: Was hinübergeht, ist
durchgehend framework-freies Datenvokabular, nie ein Domänentyp und nie ein opak durchgereichter
Node. `MapShape` bleibt im Feature, `ShapeGeometry` überquert; `ImageMapLearnView` bekommt mit
`map::geometryFor` eine Funktion statt der Karte. Dieselbe Sorgfalt an fünf weiteren Grenzen:
`CardData`, `ActivityTableRow`, `BarChartData`, `DiaryCardData`, `ShapeMapState`. Jedes dieser
Objekte zahlt sich in Gruppe 11 aus — sie sind der Grund, warum drei von vier Schichten einen
Datenquellen-Wechsel unberührt überstehen.

**Die Schlüssel-Regel.** „Ein Baustein holt sich beim Skin, was für jede Verwendung gleich ist;
was von der Verwendung abhängt, bekommt er übergeben." Das Regelwerk behauptet „ausnahmslos" —
und in 28 Baustein-Dateien gibt es genau sechs Skin-Zugriffe, von denen keiner Deck-Id,
Kartenname oder Kategorie entgegennimmt. Die Auflösung bleibt vollständig in `shared.ui`. Eine
Regel, die man überprüfen kann und die stimmt, ist selten genug.

**Der Session-Wechsel.** `requestSessionSwitch` bekommt die *Aufbau-Routine*, nicht die fertige
Session — mit der Begründung daneben, dass ein Aufbau umsonst wäre, wenn der Nutzer abbricht.
Drei Strategien, drei getrennte Zweige, `suspend()`/`resume()` sauber um die Dialoge gelegt. Das
ist die tragende Mechanik der Suite und passt auf eine Bildschirmseite.

**Die Config-Fassade.** Eine Tür, zwei versteckte Stores, die Typumwandlung genau einmal,
Schreiben auf einen unveränderlichen Key wirft, und beim Start eine Kollisionsprüfung über den
gemeinsamen Namensraum. Dazu das Javadoc über `init`, das erklärt, warum `DB` seine Pfade als
Parameter bekommt und dass jede „Vereinfachung" einen Konstruktionszyklus erzeugt, der klaglos
kompiliert. Ohne diesen Absatz wäre das in einem Jahr nicht mehr zu rekonstruieren.

**FailFast wird wirklich gehalten.** In rund 27.700 Zeilen gibt es 23 `catch`-Blöcke, die einen
Fehler abfangen, ohne weiterzuwerfen — und fast alle sind namentlich begründet: die Start-Importe
im Controller mit eigenem Javadoc-Absatz, das Fenster vor der Log-Initialisierung, die
Poster-Aufräumung nach einem Rollback, die Query-Validierung im Tagebuch. Was nicht begründet ist,
steht als Befund in diesem Bericht. Genauso selten: Nur zwei Stellen werfen, ohne die Ursache
mitzunehmen. Bei einer Regel, die kein Test prüft, ist das ein hoher Wert.

**Ausnahmen stehen sichtbar, statt zu fehlen.** Der Architekturtest trägt die `app.tmp`-Zeile und
die `MainWindow`-Ausnahme im Regeltext, jeweils mit Javadoc — und mit dem Satz, dass der Build
brechen *soll*, wenn die Ausnahme wegfällt und noch etwas seitwärts greift. Dieselbe Haltung im
Code: `ShapeGeometry` benennt seine eigene Naht („Das ist die Naht. Nicht überrascht sein."),
`SkinProperties` schreibt neben `contentSize`, dass der Loader es still ignorieren würde,
`Skin-Felder.md` beschreibt die Kommazahlen-Falle vollständig. Dass Befund 10.1 überhaupt so
genau zu beschreiben war, liegt an dieser Gewohnheit.

**Bausteine erben, sie verhüllen nicht.** Geprüft: Jede Komponente erbt von ihrem JavaFX-Typ, mit
genau den drei Ausnahmen, die das Regelwerk selbst benennt (`SuiteBackground`,
`DiaryTagInputComponent`, `ButtonTextFit`). Keine Abweichung.

**`Alerts` als einzige Übersetzung zwischen `ButtonEnum` und `ButtonType`.** Der Aufrufer nennt
Knöpfe fachlich und bekommt fachlich zurück; „Dismiss und X heißen immer CANCEL" ist eine Zusage,
auf die sich acht Features stützen. Die Zuordnung ist ein vollständiger `switch` über das Enum —
ein neuer Knopf ist ein Übersetzungsfehler, kein Laufzeitproblem.

**Ein Screen beendet sich nicht selbst.** Die Regel, dass zwischen „fertig" und „ersetzt" nichts
stehen darf, das die Kontrolle an die Event-Schleife zurückgibt, ist in beiden Lernzweigen
durchgezogen und in `AnkiDeckSession.closeLoud()` mit dem Grund versehen. Die frühere Variante
mit `active`-Flag ist samt Begründung verworfen. Befund 6.1 ist die eine Stelle, an der die Zusage
unterlaufen wird — und dass man das überhaupt als Verstoß benennen kann, liegt daran, dass die
Zusage präzise formuliert ist.

**Fachliche Schlüssel statt Anzeigenamen in der Datenbank.** `Deck.getId()` und `Mode.name()`
gehen in die DB, nicht der Anzeigename — mit der Begründung im Javadoc von
`DbDeckProgressRepository`: „Welt" darf jederzeit „Weltkarte" werden, ohne dass die Historie
verwaist.

**Fremdsystem-Eigenheiten sind aufgeschrieben, wo sie gebraucht werden.** Der Regular-Flip von
TMDB samt der empirisch geprüften Invariante, das Signal-Importfenster über die Fremd-DB statt
über Zeitzonenrechnung, Googles fehlender Tag, der nicht 0 bedeutet, der
JavaFX-26-Rundungsfehler mit Reproducer-Verweis. Das ist Wissen, das man nicht zweimal erwirbt.

---

## Die wichtigsten Befunde

Zwölf Punkte quer über alle Gruppen, nach Wirkung sortiert. Die Details stehen jeweils in der
genannten Gruppe.

| # | Befund | Wirkung | Aufwand |
|---|---|---|---|
| 1 | **[1.2]** Auf der Karte liegengebliebene Klicks können eine Karte als richtig werten, obwohl die halbe Aufgabe offen ist. `clickedIds` wird gefüllt, bevor geprüft ist, ob der Step überhaupt Klicks erwartet — geleert wird es nur bei einem Fehlklick und bei einem vollständigen `Click`-Step. | Falscher Lernstand, unbemerkt; ob eine heutige Karte die Form hat, ist offen (siehe 1.2) | ½ h |
| 2 | **[7.1]** Die Suite hat zwei Antworten auf „welcher Tag ist heute": `AppClock.TODAY` (11 Dateien) und `LocalDate.now()` (46 Stellen in 25 Dateien). Über Mitternacht rechnen beide Hälften derselben Auswertung mit verschiedenen Tagen. | Still falsche Zahlen in Streaks, Tagesbudget, Fälligkeit; einziger Befund, der alle Gruppen berührt | ½ h Regel + 2 h Durchsicht |
| 3 | **[3.1]** API-Key und Session-ID stehen in der v3-URL und damit in jeder Fehlermeldung — die in der Logdatei und im Alert landet. | Zugangsdaten verlassen den Konfigordner, ein Fehlschlag genügt | 10 min |
| 4 | **[2.1]** Ein neu angelegtes Region-Deck wird nie fällig: Kein Codeweg legt die erste Zeile in `region_learn_stat` an. Ein Skript im Repo tut es von Hand. | Blockiert ein Wunschlisten-Feature; jedes weitere Deck braucht DB-Chirurgie | ½ h |
| 5 | **[6.2]** Die Suite kann sich nicht selbst einrichten. Vier Features und die Config-Fassade erwarten eine von Hand angelegte erste Zeile, und das Dashboard — der einzige Screen quer durch alles — stirbt an der ersten leeren Tabelle. | Acht funktionierende Kacheln gehen wegen einer neunten verloren | ½ h fürs Dashboard |
| 6 | **[3.2]** `saveImageToFileSystem` gibt es zweimal mit **entgegengesetztem** Verhalten bei „Datei existiert schon" — und beide schreiben Filmposter in denselben Ordner. | Für dieselbe Datei gelten zwei Regeln; welche, sieht man an der Aufrufstelle nicht | ¾ h |
| 7 | **[6.1]** Das Statistik-Menü wird über seinen Anzeigetext angesteuert. Trifft kein Vergleich, wird der bereits geschlossene Screen wieder angezeigt — genau der „tote Screen", den das Regelwerk ausschließt. | Bricht eine ausdrückliche Zusage; jeder neue Statistik-Eintrag ist betroffen | ½ h |
| 8 | **[Szenario B]** Ein neuer Menüpunkt kostet fünf Stellen in zwei Klassen; 18 Callback-Paare zwischen `MainWindow` und `Controller` stehen schon so da, und ein vergessenes meldet sich erst beim Klick. | Der einzige Aufwand, der mit jedem Feature wächst statt konstant zu bleiben | 2–3 h |
| 9 | **[5.1]** `DiaryScreen` baut SQL-Fragmente inklusive Tabellen-, Spalten- und Aliasnamen und reicht sie ans Repository. Der Wächter „SQL nur in Repositories" sieht das nicht, weil er auf `Statement` prüft. | Eine umbenannte Spalte bricht die Suche zur Laufzeit; Compiler und Test schweigen | ¾ h |
| 10 | **[10.1]** Vier `Double`-Skinfelder lassen sich aus keiner properties-Datei setzen — der Schlüssel kommt durch die FailFast-Prüfung und wird danach still verschluckt. Die Falle ist dokumentiert; zwei neue Felder sind trotzdem hineingelaufen. | Ein Skin kann Werte nicht setzen, die laut Kommentar ihm gehören — ohne jede Meldung | ½ h |
| 11 | **[3.5]** Der Lücken-Check des Serien-Imports fängt jeden Fehler ab, zählt nur Erfolge und meldet am Ende „Nichts Neues gefunden." | Ein vollständig fehlgeschlagener Lauf sieht aus wie ein vollständiger | ½ h |
| 12 | **[1.1]** `createViewFor` endet mit `default -> null`. Ein neues Anki-Deck ohne View-Zweig führt zur NPE in der ersten Karte statt zu einer Fehlermeldung. | Die einzige Stelle im Deck-Szenario, die ein Vergessen nicht meldet | 1 min |

**Das Muster hinter 7, 8 und 12:** Eine vergessene Erweiterung meldet sich nicht beim Übersetzen,
sondern beim Klicken. Überall dort, wo stattdessen ein Enum oder ein `sealed interface` steht —
`PlayMenuNode`, `ButtonEnum`, `Card.Step`, `LearnComponent`, `SessionSwitchStrategy` — gibt es das
Problem nicht. Die Suite kennt die Lösung; sie ist an drei Stellen nicht angewandt.

**Was auffällig selten vorkommt:** echte Doppelungen von Fachlogik. Die Kandidaten (1.3, 2.2, 5.2,
3.3) sind allesamt klein und lokal. Der Bericht enthält keinen einzigen Befund der Sorte „diese
Zuständigkeit ist über fünf Pakete verteilt" — die Schichtung hält.

---

## Stand

| Gruppe | Zustand |
|---|---|
| 1 · Lernen — Anki und gemeinsame Lernbasis | **erledigt** |
| 2 · Lernen — Region-Decks | **erledigt** |
| 3 · Film und Serien | **erledigt** |
| 4 · Messaging | **erledigt** |
| 5 · Gesundheit und Tagesdaten | **erledigt** |
| 6 · Controller, Start und Screens | **erledigt** |
| 7 · shared — Basisdienste und Modell | **erledigt** |
| 8 · shared/ui — Oberflächenrahmen | **erledigt** |
| 9 · UI-Bausteine | **erledigt** |
| 10 · Skin | **erledigt** |
| 11 · Änderungsszenarien | **erledigt** |

Alle elf Gruppen sind durch. Die beiden zusammenfassenden Abschnitte stehen oben, die offenen
Fragen am Ende der Datei.

### Entscheidungen je Befund

Die Untersuchung ist abgeschlossen; diese Tabelle führt die Arbeit danach. Jeder Befund trägt
unten in seinem Abschnitt dieselbe Zeile — wer dort etwas ändert, ändert es auch hier, sonst
laufen die beiden auseinander.

**Stände:** `offen` · `erledigt` · `verworfen` (mit Grund in einem Halbsatz dahinter)

| Nr | Befund | Aufwand | Stand |
|---|---|---|---|
| 1.1 | Ein unbekanntes Deck bekommt eine `null`-View statt eines Fehlers | eine Minute | erledigt |
| 1.2 | Vier Eingänge, vier Antworten auf „sind wir in Pause?" | eine halbe Stunde | erledigt |
| 1.3 | Die Spaced-Repetition-Formel steht an zwei Stellen | eine Viertelstunde | erledigt |
| 1.4 | Die Grammatik der Deck-Dateien wohnt in der Datenklasse | eine gute Stunde | erledigt |
| 1.5 | „Hint" heißt in diesem Zweig zweierlei | zehn Minuten | erledigt |
| 1.6 | Das `Deck`-Enum trägt zwei disjunkte Formen | eine Viertelstunde | erledigt |
| 1.7 | Fünf Kommentare beschreiben Code, den es nicht gibt | zwanzig Minuten | teilweise |
| 1.8 | Drei Namen für denselben Vorgang in `learn.repository` | zehn Minuten | erledigt |
| 1.9 | Zwei GeoJSON-Leser mit wortgleichen Geometrie-Methoden | zwanzig Minuten | erledigt |
| 1.10 | Zwei Wege, SQL zu schreiben, in einer Klasse | eine Viertelstunde | offen |
| 1.11 | Die Anzeigetexte der Anki-Session entstehen auf der Feature-Seite | eine halbe Stunde | offen |
| 1.12 | Toter Code | zwanzig Minuten | offen |
| 1.13 | Ablaufverfolgung landet im Dateilog | zehn Minuten | offen |
| 2.1 | Ein neu angelegtes Region-Deck wird nie fällig | eine halbe Stunde (gemeinsam mit 1.3) | offen |
| 2.2 | „Welcher Name gilt in diesem Modus" wird fünfmal beantwortet, auf zwei Arten | dreiviertel Stunde | offen |
| 2.3 | Die Fehlerliste am Sessionende steht dreimal | eine halbe Stunde (mit 2.2) | offen |
| 2.4 | `RegionDeckRepository` ist eine Attrappe | eine Viertelstunde | offen |
| 2.5 | Ein verschluckter Fehler | eine Minute | offen |
| 2.6 | Der Progress sagt dem Presenter etwas, das der Presenter schon weiß | eine Viertelstunde | offen |
| 2.7 | 270 Einzelabfragen beim Start | eine halbe Stunde | offen |
| 2.8 | Welches Deck bei einer kombinierten Spielsession das primäre ist, hängt am Hashwert | zehn Minuten | offen |
| 2.9 | Enum-`toString()` trägt Last | zwanzig Minuten | offen |
| 2.10 | Kleinkram | eine halbe Stunde | offen |
| 3.1 | API-Key und Session-ID landen in der Logdatei und im Fehler-Alert | zehn Minuten | offen |
| 3.2 | Zwei Methoden gleichen Namens mit entgegengesetztem Verhalten | dreiviertel Stunde | offen |
| 3.3 | Der Serien-Import steht zweimal | dreiviertel Stunde | offen |
| 3.4 | Der Import, der nicht fragen kann, fragt zweimal | zehn Minuten | offen |
| 3.5 | Der Lücken-Check verschluckt jeden Fehler und meldet trotzdem Erfolg | eine halbe Stunde | offen |
| 3.6 | Das Klassen-Javadoc nennt einen Config-Schlüssel, den es nicht gibt | zwei Minuten | offen |
| 3.7 | Zehnmal derselbe Parse-Block | eine halbe Stunde | offen |
| 3.8 | Jede bewertete Serie wird bei jedem Lauf zusätzlich zweimal vollständig geholt | zwanzig Minuten | offen |
| 3.9 | Toter Code | zehn Minuten | offen |
| 3.10 | Kleinkram | eine halbe Stunde | offen |
| 4.1 | Die eigene Signal-Kennung steht im Quelltext | zehn Minuten | offen |
| 4.2 | Die Kontakt-Auflösung steht in beiden Zweigen | eine Stunde | offen |
| 4.3 | Der WhatsApp-Import merkt sich „heute geprüft", bevor er geprüft hat | zehn Minuten | offen |
| 4.4 | Die zwei Wächter in `run()` stehen in der falschen Reihenfolge | zehn Minuten | offen |
| 4.5 | Die Attachments heißen „move", werden aber kopiert | fünf Minuten | offen |
| 4.6 | Eine quellenspezifische Methode in der quellenneutralen Klasse | eine Viertelstunde | offen |
| 4.7 | Zwei Schreibweisen für denselben Konfigurationswert | fünf Minuten | offen |
| 4.8 | Ein stumm verschlucktes Problem im Entschlüsseler | fünf Minuten | offen |
| 4.9 | Kleinkram | zwanzig Minuten | offen |
| 5.1 | Der Tagebuch-Screen baut SQL | dreiviertel Stunde | offen |
| 5.2 | „Ist die Matratze fällig" wird zweimal beantwortet, in zwei Einheiten | eine halbe Stunde | offen |
| 5.3 | `PointsCalculator` rechnet nicht nur, er fragt | dreiviertel Stunde | offen |
| 5.4 | „Welches Wochenziel galt in Woche X" — zwei Mechanismen im selben Paket | eine halbe Stunde | offen |
| 5.5 | `DashboardService` bekommt „heute" übergeben und benutzt es dann nicht | eine Viertelstunde | offen |
| 5.6 | `logApiResponse` loggt keine API-Antwort | zehn Minuten bis eine halbe Stunde | offen |
| 5.7 | Ein Übergangsgerüst, dessen Termin verstrichen ist | Entscheidung, keine Arbeit | offen |
| 5.8 | Kleinkram | eine halbe Stunde | offen |
| 6.1 | Das Statistik-Menü wird über seinen Anzeigetext angesteuert — und erzeugt dabei einen toten Screen | eine halbe Stunde | offen |
| 6.2 | Die Suite kann sich nicht selbst einrichten | eine halbe Stunde (Dashboard) | offen |
| 6.3 | Der Exporter fängt genau den Fehler ab, den `Config` bewusst wirft | eine Viertelstunde | offen |
| 6.4 | Vier öffentliche Methoden am `MainWindow` ohne Aufrufer — samt der Mechanik dahinter | eine Viertelstunde | offen |
| 6.5 | Vier Kommentare, die etwas anderes sagen als der Code | zwanzig Minuten | offen |
| 6.6 | Das Übergangsgerüst `app.tmp` ist fällig | Entscheidung, keine Arbeit | offen |
| 6.7 | Kleinkram | zwanzig Minuten | offen |
| 7.1 | Die Suite hat zwei Antworten auf „welcher Tag ist heute" | eine halbe Stunde + zwei Stunden Durchsicht | offen |
| 7.2 | `Config.getString` ist ein zweiter Name für `Config.get` | fünf Minuten | offen |
| 7.3 | `DB` baut viermal dieselbe Verbindung auf | zwanzig Minuten | offen |
| 7.4 | `FilenIgnoreSource`: zweimal dieselben vier Zeilen, und die zweite wirft beim Herunterfahren | eine Viertelstunde | offen |
| 7.5 | Zwei Stellen werfen ohne Ursache, eine reduziert sie auf den Text | fünf Minuten | offen |
| 7.6 | `UiUtils` trägt drei unverwandte Dinge, eines davon globalen Zustand | zwanzig Minuten | offen |
| 7.7 | Der Screen-Vertrag verweist auf Methoden, die es nicht gibt | zwei Minuten | offen |
| 7.8 | Kleinkram | eine Viertelstunde | offen |
| 8.1 | Regel 6 beschreibt nicht den Code, und der Architekturtest sagt das bereits | eine Viertelstunde Doku | offen |
| 8.2 | Der Erweiterungsvertrag von `AnkiLearnView` ist an drei Stellen überholt | eine halbe Stunde | offen |
| 8.3 | Die Thumbnail-Höhe steht in beiden Hälften des Tagebuch-Splits | eine Viertelstunde | offen |
| 8.4 | „Die einzige Stelle der Suite, die `ButtonType` kennt" — das sind 14 Stellen | fünf Minuten | offen |
| 8.5 | Ein bekannter Mangel steht als Fließtext statt als Marker | zwei Minuten | offen |
| 9.1 | `SuiteImage` reicht zwei Innen-Nodes nach außen — und niemand nimmt sie | zwei Minuten | offen |
| 9.2 | `ImageMapPane` bietet zwei Vokabulare an, von denen eines nur nach innen zeigt | zwei Minuten | offen |
| 9.3 | Die Thumbnail-Höhe steht ein drittes Mal — Erweiterung zu Befund 8.3 | mit 8.3 erledigt | offen |
| 9.4 | Ein Rückblick zu viel — und zwei, die bleiben dürfen | fünf Minuten | offen |
| 9.5 | Kleinkram | zehn Minuten | offen |
| 10.1 | Zwei neue Felder sind in eine Falle gelaufen, die schon aufgeschrieben war | eine halbe Stunde | offen |
| 10.2 | Die Beschreibung der Staffelung stimmt in drei Punkten nicht mehr | zehn Minuten | offen |
| 10.3 | Kleinkram | zehn Minuten | offen |
| Szenario B | Ein weiterer Screen | zwei bis drei Stunden | offen |

---

## 1 · Lernen — Anki und gemeinsame Lernbasis

Gelesen: `app.learn`, `app.learn.model`, `app.learn.repository`, `app.learn.anki` und dessen
`model`/`repository` — 32 Dateien, rund 3.600 Zeilen.

### 1.1 Ein unbekanntes Deck bekommt eine `null`-View statt eines Fehlers

**Beleg:** `SessionPresenter.java:76` — `default -> null; // oder throw new IllegalArgumentException?`

`createViewFor` schaltet über die konkreten `Deck`-Konstanten. Wer ein Anki-Deck anlegt und diese
Stelle übersieht, bekommt keinen Fehler, sondern eine Session mit `view == null`; der Absturz
kommt erst beim ersten `view.setQuestion(...)` — eine Ebene tiefer, in einer Methode, die mit der
Ursache nichts zu tun hat. Das ist der Gegenentwurf zu FailFast, und der Kommentar daneben stellt
genau die Frage, die die Grundhaltung schon beantwortet.

**Kleinster Schnitt:** `default -> throw new RuntimeException("Kein View-Typ für Deck " + type);`
**Aufwand:** eine Minute.

**Stand:** erledigt

### 1.2 Vier Eingänge, vier Antworten auf „sind wir in Pause?"

**Beleg:** `CardProgress.java:180` (`return`), `:284` (`throw`), `:358` (`return`), `:248` (gar
keine Frage)

`checkTextInput` steigt bei Pause aus, `mcSubmitted` auch, `mcClicked` wirft — mit einer
Meldung, die ein Selbstgespräch ist („Aha, das kann also passieren. Na dann hier lieber einfach
return machen"). `elementClicked` fragt nicht und legt die id in Zeile 248 in `clickedIds` ab,
*bevor* geprüft wird, ob der aktuelle Step überhaupt Klicks erwartet.

Das kostet konkret: `clickedIds` wird nur bei einem Fehlklick und bei einem vollständig
abgearbeiteten `Click`-Step geleert. Klickt man während eines `Output`-, `Image`- oder
`Mark`-Steps auf die Karte, bleibt die id liegen. Kommt später in derselben Karte ein
`Click:berlin,hamburg`, genügt ein einziger Klick auf `berlin` — `clickedIds.containsAll(mandatory)`
ist durch das vorher liegengebliebene `hamburg` schon erfüllt. Die Karte zählt als richtig,
obwohl die halbe Aufgabe offen blieb.

Ob es solche Karten heute gibt, konnte ich nicht nachsehen — die Deck-CSVs liegen im Datenordner.
Nötig ist eine Karte mit mehrteiligem `Click` hinter einem reinen Anzeige-Step, und die
Deck-Syntax erlaubt sie ausdrücklich (`Click:berlin,potsdam-brandenburg` steht so als Beispiel in
`Deck-Syntax.md`). Unabhängig davon ist das ungeprüfte `add` in Zeile 248 zu früh.

**Kleinster Schnitt:** `clickedIds.clear()` beim Start eines `ClickMapElements`-Steps in `process`,
und in `elementClicked` den Typtest vor das `add`. Die vier Pause-Antworten auf eine ziehen
(`if (isPaused) return;` oben in allen vieren) — der Weg „Klick beendet die Pause" läuft ohnehin
schon im Presenter (`SessionPresenter.java:243`), nicht hier.
**Aufwand:** eine halbe Stunde.

**Stand:** erledigt — die vier gleichartigen `clickedMcAnswers.clear()` in `mcClicked` und
`mcSubmitted` sind aus demselben Grund mit weggefallen: `process` leert die Menge beim Betreten
jedes `ChoiceStep`.

### 1.3 Die Spaced-Repetition-Formel steht an zwei Stellen

**Beleg:** `LearnStat.java:76` (`calculateNewLevel`) und `SessionProgress.java:210-213`

`LearnStat` trägt laut eigenem Javadoc „die Formel selbst". Für eine Karte, die noch nie gespielt
wurde, entscheidet aber `SessionProgress` über das Startlevel: `new LearnStat(TODAY, TODAY,
correct ? 1 : 0, correct ? 0 : 1)`. Damit liegt der Erstfall im Session-Ablauf und der
Wiederholungsfall im Modell. Wer das Verfahren ändert — anderes Startlevel, anderer Rückfall —,
muss beide finden; die Suchhilfe „die Formel steht in LearnStat" führt nur zu einer der beiden.

**Kleinster Schnitt:** eine statische Fabrik `LearnStat.forFirstPlay(boolean correct)` in
`LearnStat`, aufgerufen von `SessionProgress`.
**Aufwand:** eine Viertelstunde.

**Stand:** erledigt

### 1.4 Die Grammatik der Deck-Dateien wohnt in der Datenklasse

**Beleg:** `Card.java:94-458` — von 514 Zeilen sind rund 250 Parser und Syntaxprüfung
(`parseStep`, `parseMc`, `parseFast`, `parseSketchAdd`, `parseAreas`, `checkNoDuplicates`, dazu
die Shuffle-/OnFail-Zustandsmaschine im Konstruktor)

`app.learn.anki.model` ist laut Regelwerk das Paket der freistehenden Datenklassen, und `…Source`
ist die Rolle, an der die Datei-Herkunft steht. Tatsächlich zerlegt `CsvDeckCardSource` nur die
Zeile an `;` und reicht die Tokens weiter — das gesamte Dateiformat steckt in `Card`. Dass
`Deck-Syntax.md` in Zeile 4 ausdrücklich auf `Card.parseStep` zeigen muss, ist das Symptom: Die
Regel über die Suffixe trägt hier nicht mehr, die Doku muss einspringen.

Der Preis: Man kann keine Karte bauen, ohne CSV-Tokens zu erfinden. `Card` hat nur Konstruktoren,
die `List<String> csvTokens` nehmen — wer eine Karte will, muss erst Strings in CSV-Grammatik
erzeugen und sie von der Karte wieder auseinandernehmen lassen. Der Flaggen-Generator tut genau
das: Er baut eine ganze Karte, wirft sie weg und benutzt nur die Exception, weil der Parser der
Konstruktor ist und es keine Tür zu ihm gibt.

**Kleinster Schnitt:** `CardParser` in `learn.anki.repository`, neben `CsvDeckCardSource` — dort
steht das Dateiformat laut Namensrolle hin. Er bekommt die Token-Grammatik und die Marker
(`<ShuffleStart>`, `<OnFail>` …) samt der vier Grammatik-Würfe; `Card` behält `expectsInput` und
die drei Sinn-Prüfungen, die sich an der fertigen Struktur ablesen. Der Parser sagt, ob die Zeile
lesbar ist — `Card` sagt, ob die Karte Sinn ergibt. `Chunk`/`FixedStep`/`ShuffleBlock` werden dazu
öffentlich.
**Aufwand:** eine gute Stunde.

**Stand:** erledigt

### 1.5 „Hint" heißt in diesem Zweig zweierlei

**Beleg:** `DeckRepository.java:24` (`getAllHints` liefert `List<Card>`), `CardParser.java:120`
(„Problem beim parsen des Hints") gegen `Card.Answer(hint, variants)` und
`FastAnswers.slotHints()`

`Hint` ist einmal das alte Wort für Karte und einmal der Hinweistext, der bis zum Treffer im
Antwortfeld eines Fast-Steps steht. Beide Bedeutungen leben im selben Zweig, teils in derselben
Aufrufkette. Das ist der teuerste Namensfehler, weil er beim Lesen nicht auffällt: Beides klingt
plausibel.

**Kleinster Schnitt:** `getAllHints` → `getAllCards`, die lokalen `hints`/`h` mit, und die
Fehlermeldung in `CardParser`. Vier Dateien, keine Verhaltensänderung.
**Aufwand:** zehn Minuten.

**Stand:** erledigt

### 1.6 Das `Deck`-Enum trägt zwei disjunkte Formen

**Beleg:** `Deck.java:5-45` — Region-Decks haben durchweg `deckFileName == null` und
`configValueNewCards == null`, MC und Fast Write haben `mapDef == null`

Acht Spalten, von denen je nach Kategorie vier nie gefüllt sind. Die Getter geben das `null`
stumm weiter: `getDeckFileName()` auf einem Region-Deck liefert `null`, und nur der
Kategorie-Filter in `CsvDeckCardSource.java:78` verhindert die NPE. Dieselbe Vorsichtsmaßnahme
steht noch dreimal in `AnkiDeckService` (Zeilen 94, 99 und in `getDueGameInfos`). Wer eine Spalte
neu nutzt, muss wissen, welche Hälfte des Enums sie überhaupt hat — das sieht man dem Getter
nicht an.

Ein Aufteilen in zwei Enums scheidet aus: `Deck` ist zugleich der Schlüssel in der DB-Spalte
`deck` und der Name, über den der Skin seine Staffelung auflöst.

**Kleinster Schnitt:** die zwei kategoriegebundenen Getter laut werden lassen —
`getDeckFileName()` und `getConfigValueNewCards()` werfen, wenn die Kategorie nicht `ANKI_DECK`
ist. Aus „irgendwo eine NPE" wird „dieses Deck hat keine CSV".
**Aufwand:** eine Viertelstunde.

**Stand:** erledigt

### 1.7 Fünf Kommentare beschreiben Code, den es nicht gibt

Regel 7 des Regelwerks („Kommentare beschreiben, was ist — nie, was war") ist ungeprüft, und
genau hier fällt das auf:

- `CardSortOrder.java:7-8` — „Liegt in `shared` statt in `learn.anki`" samt ausführlicher
  Begründung; die Klasse liegt in `app.learn.anki.model`. Der Link daneben zeigt auf
  `Screen#sortOrderChanged(CardSortOrder)`; die Methode im Vertrag heißt `sortOrderChanged()`
  und nimmt nichts entgegen.
- `SessionProgress.java:30` — „Hält … eine Rückreferenz auf die Schale"; das Feld ist ein
  `Runnable`. `Feature-Details.md` sagt ausdrücklich das Gegenteil dessen, was hier steht.
- `SessionPresenter.java:300` — `cardFinished(Boolean correct)` dokumentiert den Parameter
  („can be null in case of back button"); der Rumpf liest ihn nie, drei Aufrufstellen übergeben
  ihn trotzdem.
- `CardProgress.java:449` — `@param correctlyAnswered` an einer parameterlosen Methode.
- `AnkiDeckService.java:35-83` — der lange Javadoc-Block mit der Speichermessung steht direkt
  vor einem *zweiten* Javadoc und dem Feld `imageMapNames`. Er beschreibt den Konstruktor, hängt
  aber am Feld; im Hover taucht er nirgends auf, wo er hingehört.

**Was es kostet:** Das ist die Schicht, die man nach Monaten zuerst liest. Vier der fünf Stellen
behaupten etwas, das man erst durch Nachlesen im Code widerlegt.
**Kleinster Schnitt:** löschen bzw. an das richtige Element hängen; bei `SessionPresenter` den
ungenutzten Parameter gleich mit entfernen.
**Aufwand:** zwanzig Minuten.

**Stand:** SessionProgress Runnable noch offen

### 1.8 Drei Namen für denselben Vorgang in `learn.repository`

**Beleg:** `MapRepository` (liest `.geojson`), `GeoJsonLoader` (liest `.geojson`),
`SketchFileSource` (liest `.geojson`) — alle drei im selben Paket

Die Benennungsregel verspricht, dass man die Herkunft am Suffix abliest: `…Source` für Datei,
`…Repository` für die Suite-DB oder für eine Tür, die mehrere Quellen zusammensetzt.
`MapRepository` setzt nichts zusammen — es löst den Dateinamen aus `Config` auf und ruft den
Loader. Im selben Paket steht mit `SketchFileSource` der korrekt benannte Zwilling. Das Paket
sagt dreimal dasselbe auf drei Arten, und die Regel trägt genau dort nicht, wo sie am leichtesten
zu befolgen wäre.

**Kleinster Schnitt:** `MapRepository` → `MapFileSource`, zwei Aufrufstellen in `MapService`.
`GeoJsonLoader` ist paketprivates Innenleben und darf heißen, wie er heißt.
**Aufwand:** zehn Minuten.

**Stand:** erledigt

### 1.9 Zwei GeoJSON-Leser mit wortgleichen Geometrie-Methoden

**Beleg:** `GeoJsonLoader.java:68-99` und `SketchFileSource.java:147-167` —
`parsePolygon`, `parseMultiPolygon` und `parsePoints` stehen zeichengleich in beiden

Dass es zwei Leser gibt, ist begründet, und `SketchFileSource` begründet es im Javadoc auch
sauber: Die `properties` sind völlig verschieden. Die *Geometrie* ist es nicht, und mit ihr die
eine Konvention, über die beide Dateiarten sich einig sein müssen — die Y-Invertierung. Sie steht
zweimal da. Wer sie einmal ändert, merkt am zweiten Ort nichts.

**Kleinster Schnitt:** die drei Methoden in eine paketprivate Klasse `GeoJsonGeometry` in
`learn.repository`; beide Leser rufen sie.
**Aufwand:** zwanzig Minuten.

**Stand:** erledigt

### 1.10 Zwei Wege, SQL zu schreiben, in einer Klasse

**Beleg:** `DbDeckProgressRepository.java:33`, `:95`, `:110` (Deck-Id und `AppClock.TODAY` in den
String konkateniert) gegen `:51-57` (`saveLearned`, durchgängig mit `?`)

`loadAll` ist der unangenehmste Fall: Es benutzt `prepareStatement` auf einem String, in dem der
Wert schon drinsteht — die Form eines Parameters ohne seinen Nutzen. Nebenbei ist das Datumsformat
damit an drei Stellen implizit auf `LocalDate.toString()` festgenagelt.

Ein Sicherheitsproblem ist das nicht, die Werte sind Code-Konstanten. Ein Lesbarkeitsproblem
schon: Die Klasse zeigt zwei Muster für eine Sache, und das schwächere steht dreimal.

**Kleinster Schnitt:** drei Statements auf `?` umstellen.
**Aufwand:** eine Viertelstunde.

Im selben Zug: `loadAll` liefert `Map<String, LearnStat>`, obwohl Karten-Ids `int` sind;
`DeckRepository.java:30` überbrückt das mit `String.valueOf(h.getId())`. Ein `Map<Integer, …>`
spart die Umwandlung und die Frage, warum sie dasteht.

**Stand:** offen

### 1.11 Die Anzeigetexte der Anki-Session entstehen auf der Feature-Seite

**Beleg:** `SessionPresenter.java:276-294` — `"Korrekt: " + … + "\nFalsch: " + …` und
`"Zuletzt gespielt: " + … + "\nLevel: " + …`, übergeben als fertiger String an
`view.setProgress(String)` und `view.setCardHistory(String)`

Die Tabelle im Regelwerk teilt zu: Feature entscheidet *was und wann*, die View *welche Bausteine
und wie verdrahtet*, der Skin *wie es aussieht*. Wortwahl und Zeilenumbruch eines Labels sind
keins der Feature-Dinge — sie sind „soll anders aussehen" und damit View-Sache. Der Region-Zweig
macht es auch nicht so: Sein Presenter reicht keine vorformatierten Fortschrittstexte hinüber.
Die beiden Zweige sind sich hier uneins, ohne fachlichen Grund.

**Was es kostet:** Ein anderer Wortlaut oder eine zweite Zeile im Fortschrittsfeld ist heute eine
Änderung im Feature-Code. `SessionProgressCounter` ist framework-frei und könnte die Grenze
unverändert überqueren.
**Kleinster Schnitt:** `setProgress(SessionProgressCounter)` und
`setCardHistory(LocalDate lastPlayed, int level, int wrongCount)`; das Zusammensetzen wandert in
`AnkiLearnView`.
**Aufwand:** eine halbe Stunde. Gegenprüfung in Gruppe 2 und 8, ob das ein Muster ist.

**Stand:** offen

### 1.12 Toter Code

Jedes Stück davon muss beim Durchlesen einmal bewertet werden und liefert dabei nichts:

| Stelle | Zustand |
|---|---|
| `model/MapElementListener.java` | ganze Datei, kein Nutzer in `app` |
| `GeoMap.java:93` `setShapes` | kein Aufrufer |
| `MapMetadata.java:23,29` `bgImageFile` | gesetzt, nie gelesen — Hintergrundbilder kommen aus dem Skin |
| `Card.java:81,93` `remark` | aus der CSV geparst, gespeichert, nie gelesen |
| `Card.java:35` `MarkMapElements.right` | gefüllt, nie gelesen; der Kommentar daneben sagt es selbst |
| `LearnSessionInfo.java:8` | auskommentierte Methodensignatur |
| `SessionPresenter.java:300` Parameter | siehe 1.7 |

**Aufwand:** zwanzig Minuten, inklusive Nachsehen, ob wirklich niemand ruft.

**Stand:** offen

### 1.13 Ablaufverfolgung landet im Dateilog

**Beleg:** `AnkiDeckSession.java:50,60,82`, `SessionProgress.java:46,67,175,199,228`,
`CardProgress.java:85`

`=== SESSION CONSTRUCTOR === Session@1234567` und eine Zeile je gespielter Karte laufen über
`Log.info` und landen damit laut Architekturdokument in der Logdatei. Das sind
Entwicklungsspuren mit `identityHashCode` — genau das, wofür `Log.debug` (FINE, Konsole, mit
`--debug` auch Datei) da ist. Eine Session mit 60 Karten schreibt so gut 70 Zeilen in die Datei,
in der man sonst nach Importfehlern sucht.

**Kleinster Schnitt:** die `=== … ===`-Zeilen und „Los geht es mit Karte" auf `Log.debug`;
„Starte AnkiSession" und „SAVE END" dürfen `info` bleiben, die sagen etwas über den Lauf aus.
**Aufwand:** zehn Minuten.

**Stand:** offen

### Was in dieser Gruppe trägt

Nicht als Lob, sondern damit es beim Umbauen stehen bleibt:

- **Die JavaFX-Grenze hält, und zwar in der harten Fassung.** Kein `javafx`-Import in den 32
  Dateien und auch kein opak durchgereichtes Objekt: `MapShape` bleibt im Feature, hinüber geht
  `ShapeGeometry` (`GeoMap.geometryFor`). `ImageMapLearnView` bekommt mit `map::geometryFor` eine
  Funktion statt einer Karte — der Domänentyp überquert die Grenze nie.
- **`FastAnswers` und `MultipleChoiceAnswers`** sind sauber geschnittene Step-Zustände: Sie
  kennen weder Presenter noch View, und ihre Sonderfälle (gebunden/ungebunden, toleriert) stehen
  als Begründung im Javadoc statt als Kommentar im Code.
- **Der DB-Schlüssel ist `Deck.getId()`, nicht der Anzeigename**, mit der Begründung im Javadoc
  von `DbDeckProgressRepository`. Genau der Punkt, an dem eine spätere Umbenennung sonst die
  Historie verwaisen ließe.

---

## 2 · Lernen — Region-Decks

Gelesen: `app.learn.region` mit `model` und `repository` — 15 Dateien, rund 1.460 Zeilen.

### 2.1 Ein neu angelegtes Region-Deck wird nie fällig

**Beleg:** `RegionDeckService.java:47-53` (nur vorhandene Stände landen im Cache), `:66-67`
(`getDueGameInfos` überspringt fehlende), `SessionProgress.java:137-142` (`save()` liest den Stand,
bevor es ihn schreibt)

Die erste Zeile in `region_learn_stat` entsteht im Anwendungscode nirgends. Der einzige
`INSERT` steht in `DbRegionDeckProgressRepository.save()`, und dorthin kommt man nur über
`closeLoud → saveAndEndSession → progress.save()` — also nur, wenn die Session überhaupt startet.
Starten kann sie nur aus dem Lernmenü, und ins Lernmenü kommt nur, wozu es schon einen Stand gibt.
`save()` selbst würde es auch nicht retten: Es beginnt mit `service.getLearnStat(spec)`, und das
läuft für ein unbekanntes Deck in eine NPE (`statCache.get(deck)` ist `null`).

Dass das kein theoretischer Fall ist, steht im Repo: `scripts/learn/CopyLandkreiseStats.java`
existiert genau dafür, Zeilen von Hand einzutragen. Der Umweg ist also schon gebaut — nur eben
neben dem Anwendungscode.

**Was es kostet:** Jedes weitere Region-Deck (die Wunschliste nennt Frankreich) braucht einen
manuellen DB-Eingriff, bevor es überhaupt sichtbar wird.
**Kleinster Schnitt:** In `getDueGameInfos` einen fehlenden Stand als „heute fällig, Level 0"
behandeln und in `save()` den Stand anlegen, wenn keiner da ist — dieselbe Fabrik, die auch
Befund 1.3 braucht (`LearnStat.forFirstPlay`).
**Aufwand:** eine halbe Stunde, gemeinsam mit 1.3.

**Stand:** offen

### 2.2 „Welcher Name gilt in diesem Modus" wird fünfmal beantwortet, auf zwei Arten

**Beleg:** `ClickSessionProgress.java:48` gegen `WriteSessionProgress.java:70-75` und `:124-129`
sowie `EliminationSessionProgress.java:29-34` und `:44-49`

`Mode` trägt die Achse `CapitalOrRegion` ausdrücklich als eigenes Unter-Enum, und zwei Stellen
nutzen sie auch so: `ClickSessionProgress` fragt `getCapitalOrRegion()`, `RegionPlaySetup.needsCapital`
ebenfalls. Die anderen vier schalten stattdessen über die Modus-Konstante selbst, jedes Mal mit
`default -> throw new RuntimeException("Das kommt jetzt einigermaßen unerwartet :)")` —
derselbe Satz dreimal.

**Was es kostet:** Ein zehnter Modus übersetzt sich anstandslos und fällt erst zur Laufzeit um,
an bis zu vier Stellen nacheinander. Und die eine Frage („nehme ich Regionsnamen, Hauptortnamen
oder beides?") hat zwei verschiedene Antwortmechaniken im selben Paket.

**Kleinster Schnitt:** ein paketprivater Helfer in `app.learn.region` mit zwei Methoden —
`nameOf(MapShape, Mode)` und `matches(MapShape, Mode, String)` —, beide über
`mode.getCapitalOrRegion()` geschaltet. Damit verschwinden die vier `default -> throw` und ein
neuer Modus ist automatisch abgedeckt, sobald er seine Achse angibt. In `learn.region`, nicht in
`learn.model`: `MapShape` gehört dem Kern und darf den Zweig nicht kennen.
**Aufwand:** dreiviertel Stunde.

**Stand:** offen

### 2.3 Die Fehlerliste am Sessionende steht dreimal

**Beleg:** `ClickSessionProgress.java:132-137`, `WriteSessionProgress.java:113-117`,
`EliminationSessionProgress.java:27-36`

Dreimal dasselbe Muster: Einleitungssatz, Schleife über die verpassten Elemente, jede Zeile
angehängt, dann `finishIncorrect(result, false, null)`. Dazu steht der Wächter
`else throw new RuntimeException("… Untersuchen!")` wortgleich in zwei der drei
`nextStep`-Methoden.

`SessionProgress` ist als abstrakte Klasse genau deshalb da — sein Javadoc sagt: „Hier steht, was
alle drei teilen". Das hier teilen alle drei und steht trotzdem draußen.

**Kleinster Schnitt:** `protected void finishWithMisses(String einleitung, List<String> namen)`
in `SessionProgress`, die den Wächter gleich mit übernimmt.
**Aufwand:** eine halbe Stunde. Zusammen mit 2.2 erledigt, denn die Namen kommen von dort.

**Stand:** offen

### 2.4 `RegionDeckRepository` ist eine Attrappe

**Beleg:** `RegionDeckRepository.java` — 23 Zeilen, hält genau eine Klasse und benennt deren zwei
Methoden um (`load` → `getLearnStat`, `save` → `saveRegionSession`)

Die Benennungsregel erlaubt `…Repository` als Tür nach außen, *wenn* eine Klasse mehrere Quellen
zusammensetzt. Der Anki-Zwilling `DeckRepository` tut das auch (CSV plus DB) und verdient den
Namen. Hier gibt es nur eine Quelle; der Name verspricht eine Zusammensetzung, die es nicht gibt,
und das eigene Javadoc nennt das Dahinterliegende „die db-Source".

**Was es kostet:** Jede Änderung an der Persistenz geht durch zwei Dateien, und wer den Aufbau
mit dem Anki-Zweig vergleicht, sucht die zweite Quelle, die es nicht gibt.
**Kleinster Schnitt:** `RegionDeckRepository` löschen, die zwei Methoden in
`DbRegionDeckProgressRepository` öffentlich machen und die Klasse zu `RegionDeckRepository`
umbenennen. Ein Aufrufer (`RegionDeckService`).
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 2.5 Ein verschluckter Fehler

**Beleg:** `DbRegionDeckProgressRepository.java:34-38` — `throw new RuntimeException("Ui, ich
bekomme die Stats für die RegionsSession nicht: …")`, **ohne** `e`

Das Nachbarmethode `save` reicht die Ursache korrekt weiter. Hier nicht: Eine umbenannte Spalte,
eine gesperrte Datei, ein Parse-Fehler im Datum — alles kommt als derselbe Satz ohne Stacktrace
an. Genau das, was FailFast verhindern soll: Der Fehler fliegt, aber er sagt nichts mehr.

**Kleinster Schnitt:** `, e` ergänzen.
**Aufwand:** eine Minute.

**Stand:** offen

### 2.6 Der Progress sagt dem Presenter etwas, das der Presenter schon weiß

**Beleg:** `SessionPresenter.java:21-24` (Enum `WrongClickResolution`),
`ClickSessionProgress.java:69-73` (leitet es aus `spec.isPlaySession()` ab und übergibt es),
`SessionPresenter.java:27` (hält `spec` selbst)

`undoWrongClick(resolution)` bekommt einen Wert übergeben, den der Empfänger aus einem Feld
ableiten könnte, das er schon hat. Dazu wohnt das Enum im Presenter, obwohl die Entscheidung
Progress-Wissen ist (Lernen oder freies Spiel) — die Zuständigkeit ist also nicht nur doppelt,
sondern auch auf der falschen Seite abgelegt.

**Kleinster Schnitt:** Enum und Parameter streichen, der Presenter fragt `spec.isPlaySession()`.
Der Import von `SessionPresenter.WrongClickResolution` in `ClickSessionProgress` fällt mit weg.
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 2.7 270 Einzelabfragen beim Start

**Beleg:** `RegionDeckService.java:38-54` — 27 Region-Decks × 10 Modi, je ein
`SELECT * FROM region_learn_stat WHERE deck = ? AND mode = ?`

Die Tabelle hat höchstens so viele Zeilen, wie es Kombinationen gibt, und wird komplett gebraucht.
Statt eines Durchlaufs über das Ergebnis eines einzigen SELECT laufen 270 Abfragen, von denen die
allermeisten nichts finden.

Die Nebenwirkung ist wichtiger als die Laufzeit: Weil nur Treffer im Cache landen
(`if (stat != null)`), kann der Cache hinterher „nie gespielt" nicht von „nicht geladen"
unterscheiden — das ist die Wurzel von Befund 2.1.

**Kleinster Schnitt:** ein `loadAll()` im Repository, das die ganze Tabelle in eine
`Map<Deck, Map<Mode, LearnStat>>` liest; der Service füllt daraus und weiß danach, dass ein
fehlender Eintrag „noch nie gespielt" heißt.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 2.8 Welches Deck bei einer kombinierten Spielsession das primäre ist, hängt am Hashwert

**Beleg:** `RegionPlaySetup.java:234` — `Deck primaryDeck = selectedDecks.iterator().next();` auf
einem `HashSet`

Das primäre Deck bestimmt `spec.getDeckType()` und damit die id, die an `RegionLearnView` geht und
dort die Skin-Staffelung anstößt. Die Auswahl von Berlin-Mitte plus Berlin-Ost kann also mal mit
`be_mi` und mal mit `be_os` laufen. Heute sieht man davon nichts, weil die Staffelung auf den
gemeinsamen `mapName` zurückfällt; sobald für eines der Geschwister ein eigener Skin-Wert
existiert, erscheint er bei gleicher Auswahl mal und mal nicht — und das ist ein Fehlerbild, das
sich schlecht reproduzieren lässt.

**Kleinster Schnitt:** das primäre Deck festlegen statt ziehen, etwa das mit der kleinsten
`getId()`.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 2.9 Enum-`toString()` trägt Last

**Beleg:** `Mode.java:39-42` (Anzeigename) und `DeckCategory.java:35-38` (`"anki"` / `"region"`);
abgeholt über `toString()` in `RegionLearnSessionInfo.java:19`, `SessionPresenter.java:39`,
`RegionPlaySetup.java:287,313,378` und `learn/anki/SessionPresenter.java:60`

Bei `DeckCategory` ist das Ergebnis der Schlüssel, über den der Skin seine Staffelung auflöst —
ein tragender Wert, der über einen `toString()`-Override herauskommt. Bei `Mode` ist es der
Anzeigename, über den `RegionPlaySetup` zusätzlich die Auswahl zurückliest (`modeByLabel`).
Damit bedeuten `name()` und `toString()` Verschiedenes; die Persistenz nutzt korrekt `.name()`,
aber die Unterscheidung steht nirgends geschrieben.

**Was es kostet:** Eine beiläufige Konkatenation (`"Modus " + mode` in einem Log, in einer
Meldung, in einem SQL) nimmt still den Anzeigenamen. Das fällt erst auf, wenn der Anzeigename
sich ändert.
**Kleinster Schnitt:** benannte Getter (`getDisplayName()`, `getSkinKey()`), die
`toString()`-Overrides weg.
**Aufwand:** zwanzig Minuten.

**Stand:** offen

### 2.10 Kleinkram

| Stelle | Was |
|---|---|
| `ClickSessionProgress.java:17-25` | Record `QuizElement` mit handgeschriebenen `getToFind()`/`getShapeId()` neben den Record-Accessoren `toFind()`/`shapeId()`; **beide** Formen werden in derselben Klasse benutzt. Dazu ein überflüssiges `;` nach der Deklaration. |
| `ClickSessionProgress.java:148-161` | `getNameForId(String)` packt sein Argument in ein Set, nur um die Set-Überladung zu rufen — deren `size() != 1`-Wächter kann deshalb nie auslösen. Findet sie nichts, liefert sie `""`: ein leerer Name mitten im Fehlertext des Nutzers. |
| `RegionSession.java:37` | `Runnable onSessionEnded;` paketsichtbar und nicht final, die drei Nachbarfelder `private final`. |
| `SessionSpec.java:17,39` | Feld `additionalDeckTypesForFreePlay`, Getter `getAdditonalDeckTypesForPlay` — Tippfehler und zwei Namen für eine Sache. |
| `RegionPlaySetup.java:259,308,319,350` | leere `@param`/`@return`-Tags; „Elimintaion" in Zeile 306. |
| `RegionLearnSessionInfo.java:16` | Kommentar „// getter für spec" steht über `formatForMenu()`. |
| `RegionSession.java:64,135,157` | dieselbe INFO-Ablaufverfolgung wie in Befund 1.13. |
| Sichtbarkeit | Der Region-Presenter ist durchgehend `public`, der Anki-Presenter paketprivat. Gleiche Rolle, zwei Konventionen — und im Region-Fall steht damit das ganze Presenter-Vokabular auch dem Controller offen. |

**Aufwand:** zusammen eine halbe Stunde.

**Stand:** offen

### Gegenprüfung zu Befund 1.11

Der Region-Presenter setzt **keine** vorformatierten Anzeigetexte in die View — er hat kein
Gegenstück zu `setProgress(String)` oder `setCardHistory(String)`. Die Texte, die er baut, sind
Alert-Inhalte (`SessionResult.incorrectText`, `RegionSession.getUntilString`), und die sind laut
Dialog-Regel genau das Richtige: framework-freier Input hinein, Ergebnis heraus.

Damit steht 1.11 enger, als es dort formuliert ist: Es geht um die beiden Label-Texte, die der
Anki-Presenter fertig zusammensetzt, **nicht** um `formatForMenu()`. Das ist der Vertrag von
`LearnSessionInfo` und wird von beiden Zweigen gleich erfüllt.

### Was in dieser Gruppe trägt

- **Die Aufteilung in drei Modus-Abläufe mit gemeinsamer Basisklasse**, samt der Begründung, warum
  eine abstrakte Klasse und kein Interface (Felder). Die drei Modi sind wirklich verschieden, und
  der geteilte Teil ist genau das, was oben steht — die Kritik in 2.2 und 2.3 ist, dass *noch mehr*
  dorthin gehört, nicht dass der Schnitt falsch läge.
- **`finishIncorrect` erzwingt eine Begründung** (`SessionProgress.java:79-80`). Eine Session kann
  nicht als gescheitert enden, ohne dem Nutzer zu sagen, was war.
- **Die Dialog-Matrix im Javadoc von `closeLoud()`.** Vier Fälle über zwei Achsen, aufgeschrieben
  an der Stelle, an der man beim Lesen darüber stolpert.
- **`RegionPlaySetup` hält die Dialogregeln genau ein**: Reducer, weil es Verflechtung gibt
  (Modi verschwinden, Decks grauen aus), das Label als id, framework-freier Zustand hinein und
  heraus. Das ist die Vorlage, an der sich ein nächster komplexer Dialog messen lässt.
- **Das SQL dieses Zweigs arbeitet durchgehend mit Parametern** — es ist das Muster, das dem
  Anki-Zweig in Befund 1.10 fehlt.

---

## 3 · Film und Serien

Gelesen: `app.movie` mit `model`, `model.json` und `repository` — 43 Dateien, rund 4.500 Zeilen.
Die 26 JSON-DTOs sind 1:1-Abbilder der TMDB-Antworten und als solche in Ordnung; sie kommen unten
nicht mehr vor.

### 3.1 API-Key und Session-ID landen in der Logdatei und im Fehler-Alert

**Beleg:** `ApiClient.java:361-362` (`url.append("?api_key=")…append("&session_id=")…`) und
`ApiClient.java:414` (`throw new RuntimeException("[FAILFAST] TMDB API request failed for URL: "
+ urlString, e)`)

Jeder v3-Request trägt Key und Session-ID in der URL. Schlägt er fehl — TMDB nicht erreichbar,
Zeitüberschreitung, 401 —, steht die vollständige URL in der Exception. Von dort geht sie zwei
Wege: in die Logdatei und über den zentralen Handler in `ThosSuiteApp` in einen Alert mit
Stacktrace auf den Bildschirm.

**Was es kostet:** Die Zugangsdaten liegen bewusst in einer eigenen Datei im Konfigordner. Über
diesen Weg wandern sie in `{dataFolder}/log/thossuite%u.log` — eine Datei, die rotiert,
mitgesichert und beim Suchen nach ganz anderen Fehlern geöffnet wird. Ein Fehlschlag genügt.

**Kleinster Schnitt:** In `sendGet` nur den Pfad in die Meldung nehmen, nicht den Query-String:
`urlString.substring(0, urlString.indexOf('?'))` (und für die v4-Seite gleich mit). Die
Diagnose-Information bleibt erhalten — welcher Endpunkt, ist alles, was man braucht.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 3.2 Zwei Methoden gleichen Namens mit entgegengesetztem Verhalten

**Beleg:** `MovieImporter.java:252-263` gegen `SeriesImporter.java:773-784`

Dieselbe Signatur, dasselbe Paket, dieselbe Aufgabe — und die eine entscheidende Zeile ist
umgekehrt:

```java
// MovieImporter
if (file.exists())
    throw new RuntimeException("Bild existiert bereits, das sollte nicht passieren: " + filename);

// SeriesImporter
if (file.exists())
    return; // Bei Seasons kann dasselbe Bild schon durch die Show existieren
```

Dazu steht `buildImageFilename` (`MovieImporter.java:269`, `SeriesImporter.java:786`) wortgleich
zweimal da.

**Was es kostet:** Beide schreiben in denselben Ordner, und beide schreiben **Film**-Poster:
`SeriesImporter.processMoviesWithoutPoster` (`:536`, `:543`) holt fehlende Filmposter nach und
benutzt dabei die tolerante Fassung. Für dieselbe Datei gelten also zwei Regeln, je nachdem,
welcher Import sie gerade schreibt. Liegt eine Datei unerwartet herum, bricht der tägliche
Filmimport beim Start die ganze Suite ab, während der manuelle Lauf sie stillschweigend behält.
Welche der beiden Regeln gilt, sieht man an der Aufrufstelle nicht — man muss wissen, in welcher
Klasse man steht.

**Kleinster Schnitt:** eine paketprivate Klasse `PosterFiles` in `app.movie` mit
`buildFilename(...)` und `save(filename, bytes, boolean darfSchonDaSein)`. Der Unterschied steht
dann als Argument an der Aufrufstelle, wo man ihn liest.
**Aufwand:** dreiviertel Stunde.

**Stand:** offen

### 3.3 Der Serien-Import steht zweimal

**Beleg:** `SeriesImporter.java:211-256` (`importNewTvShow`) und `:321-357` (`ensureShowExists`)

Beide holen Details und aggregierte Credits, laden zwei Poster, öffnen eine Transaktion und
schreiben in derselben Reihenfolge: `insertTvShow`, zweimal `savePoster`,
`processAggregatedCredits`, Genres, Länder, Sprachen, `commit`, im Fehlerfall `rollback` — mit
demselben doppelten try/catch-Rahmen um dieselbe `SQLException`. Der einzige Unterschied sind
drei Zeilen: `importNewTvShow` fragt vorher nach einem Kommentar und schreibt zusätzlich das
Rating.

**Was es kostet:** Jede Änderung am Serien-Import — ein weiteres Feld, eine dritte Bildbreite,
eine andere Reihenfolge — muss zweimal gemacht werden, und die zweite Stelle heißt nicht so, dass
man sie sucht. Für die Wunschliste („manueller TMDB-Vollabgleich") ist genau das die Stelle, die
man anfassen würde.

**Kleinster Schnitt:** eine Methode `importShow(TvShowJSON show, CreditListJSON credits,
TvShowRatingJSON ratingOrNull, String commentOrNull)`; `importNewTvShow` und `ensureShowExists`
werden zu je vier Zeilen davor.
**Aufwand:** dreiviertel Stunde.

**Stand:** offen

### 3.4 Der Import, der nicht fragen kann, fragt zweimal

**Beleg:** `MovieImporter.java:174` und `:183` — `Alerts.show("92er Poster fehlt", …)` bzw.
`("154er Poster fehlt", …)`, beide **innerhalb** der offenen Transaktion (`:158-190`)

`Feature-Details.md` begründet eine ganze Asymmetrie des Schemas damit, dass dieser Import nicht
fragen kann: „Weil er nicht nachfragen kann, parkt er unbekannte Crew-Jobs in den
Pending-Tabellen und überlässt die Entscheidung dem `MovieCleanup`-PostTask." Er läuft als
PreTask während des Splashscreens — das Hauptfenster gibt es noch nicht.

Zwei Alerts tun es trotzdem, und zwar an der ungünstigsten Stelle: `Alerts.show` ruft
`showAndWait`, startet also eine verschachtelte Event-Schleife, während eine
TMDB-Transaktion offen ist. Der Import steht so lange, bis jemand klickt — beim Start, hinter
oder neben dem Splash.

**Was es kostet:** Der Start der Suite kann auf einen Klick warten, der niemandem angekündigt
wurde, und hält dabei eine Transaktion offen. Fachlich ist die Meldung außerdem entbehrlich: Ein
fehlendes Poster ist kein Fehler, sondern eine Lücke — und für Lücken gibt es bereits den
Lücken-Check in Schritt 3 des Serien-Imports.

**Kleinster Schnitt:** die zwei Alerts durch `Log.info` ersetzen. Der Lücken-Check holt das
Poster beim nächsten manuellen Lauf ohnehin nach.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 3.5 Der Lücken-Check verschluckt jeden Fehler und meldet trotzdem Erfolg

**Beleg:** `SeriesImporter.java:519`, `:551`, `:567`, `:597`, `:614` — fünfmal
`catch (Exception e) { Log.warn(…, e.getMessage()); }` und weiter in der Schleife; dazu
`showSummary()` (`:700-718`)

Dass eine Schleife über hunderte Einträge nicht beim ersten schlechten Eintrag sterben soll, ist
nachvollziehbar. Das Problem ist, was danach passiert: Die Zähler `postersFound` und
`overviewsFound` zählen nur Erfolge, Fehlschläge zählt niemand. Läuft der Check also komplett
ins Leere — abgelaufener Token, geänderte API, kein Netz —, schreibt er N Warnungen in die
Logdatei und meldet dem Nutzer am Ende „Nichts Neues gefunden." Das ist derselbe Satz wie bei
tatsächlich vollständigen Daten.

Dazu: `e.getMessage()` statt `e` wirft den Stacktrace weg, also genau die Information, mit der
man den Grund fände.

**Kleinster Schnitt:** ein Zähler `gapChecksFailed`, in `showSummary()` mit einer eigenen Zeile
(„Fehlgeschlagene Nachholversuche: n"), und `Log.warn(…, e)` statt `e.getMessage()`. Das behält
die Robustheit der Schleife und nimmt ihr das Schweigen.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 3.6 Das Klassen-Javadoc nennt einen Config-Schlüssel, den es nicht gibt

**Beleg:** `MovieImporter.java:39` — „Bilder landen im Dateisystem unter
`Config.getPath("tmdb.imageFolder")`"; tatsächlich steht an allen vier Stellen
(`MovieImporter.java:244`, `:253`, `SeriesImporter.java:774`, `MovieCard.java:81`)
`Config.getPath("imageFolder").resolve("tmdb")`, und `ConfigFileSource.java:54` kennt nur
`imageFolder`.

Dass der Unterordner an der Aufrufstelle resolved wird, ist **kein** Befund: `ConfigFileSource`
hält diese Entscheidung in Zeile 69-71 ausdrücklich fest („Kein computed Unterordner je Feature
… sonst stünde dasselbe Wissen zweimal"). Sie gilt dort für `attachments.folder` und wird für
`imageFolder` genauso angewandt (`tmdb`, `svg`). Der Befund ist allein der Kommentar, der einen
Schlüssel beschreibt, den nie jemand angelegt hat — und der damit beim nächsten Lesen zu genau
der Änderung einlädt, gegen die sich die Suite bewusst entschieden hat.

**Kleinster Schnitt:** die Zeile im Javadoc auf den tatsächlichen Weg umschreiben.
**Aufwand:** zwei Minuten.

**Stand:** offen

### 3.7 Zehnmal derselbe Parse-Block

**Beleg:** `ApiClient.java:69-73`, `89-93`, `110-114`, `244-248`, `264-268`, `283-287`,
`304-308`, `321-325` und zwei weitere in den Detail-Methoden

Jede fachliche Methode endet mit demselben Muster: `try { return mapper.readValue(json, X.class); }
catch (Exception e) { throw new RuntimeException("[FAILFAST] TMDB <name>: JSON-Mapping
fehlgeschlagen. <kontext>", e); }`. Beim Hinzufügen eines Endpunkts werden acht Zeilen kopiert,
und der Methodenname in der Meldung muss von Hand nachgezogen werden — eine Kopie mit falschem
Namen darin fällt nirgends auf.

**Kleinster Schnitt:** `private <T> T parse(String json, Class<T> type, String kontext)`; die
Methoden werden zu zwei Zeilen. Vier Methoden brauchen danach weiterhin ihren EN/DE-Nachbau, aber
auch der schrumpft.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 3.8 Jede bewertete Serie wird bei jedem Lauf zusätzlich zweimal vollständig geholt

**Beleg:** `SeriesImporter.java:258-262` — `checkTvShowDataChanged` ruft
`api.getTvShowDetails(tvShowId)` für **jede** bewertete Serie, und `getTvShowDetails`
(`ApiClient.java:150-165`) macht zwei HTTP-Requests, einen für EN und einen für DE

Verglichen werden vier Felder: `number_of_seasons`, `number_of_episodes`, `status`,
`last_air_date`. Keins davon ist sprachabhängig — der zweite Request ist reine Verschwendung.
Dazu läuft der Check direkt hinter `importNewTvShow` auch für die Serie, die eben erst
importiert wurde, mitsamt der Details, die man eine Zeile vorher schon in der Hand hatte.

**Was es kostet:** bei N bewerteten Serien 2N Requests je Lauf, davon die Hälfte ohne Nutzen,
plus zwei überflüssige je Neuimport. Die API ist ratenbegrenzt, und der Lauf blockiert den
FX-Thread.

**Kleinster Schnitt:** `checkTvShowDataChanged(int, String, TvShowJSON bereitsGeholt)` mit
`null` als „bitte holen"; beim Neuimport das schon vorhandene Objekt durchreichen. Die
DE-Ersparnis wäre ein zweiter Schritt (`getTvShowDetailsEnOnly`) und lohnt erst, wenn 3.7
ohnehin angefasst wird.
**Aufwand:** zwanzig Minuten für den ersten Teil.

**Stand:** offen

### 3.9 Toter Code

| Stelle | Zustand |
|---|---|
| `MovieViewerRepository.loadAllEpisodes()` samt privater Überladung `loadEpisodes(String)` (`:184-192`, `:377-388`) | kein Aufrufer |
| `CrewFilterRepository.getPendingJobs()` (`:106-116`) | kein Aufrufer |
| `EpisodeRepository.updateEpisodeFlags(int, Boolean, Boolean, Boolean)` (`:221-240`) | kein Aufrufer; schreibt `actors_from_show`/`directors_from_show` — genau die Flag-Struktur, die die Wunschliste als veraltet führt |

**Aufwand:** zehn Minuten.

**Stand:** offen

### 3.10 Kleinkram

| Stelle | Was |
|---|---|
| `MovieImporter.java:231-238` | Zwei Javadoc-Blöcke hintereinander: Der erste („Speichert ein Bild im Dateisystem. Wirft Exception wenn bereits vorhanden") gehört zu `saveImageToFileSystem`, steht aber über dem Javadoc von `deletePoster` und damit über der falschen Methode. Derselbe Fehler wie in Befund 1.7. |
| `MovieImporter.java:72`, `:98`, `SeriesImporter.java:169`, `:438` | `LocalDate.now()` / `LocalDateTime.now()` statt `AppClock.TODAY`. Gehört zum übergreifenden Befund, der in Gruppe 7 steht — die Suite hat zwei Antworten auf „welcher Tag ist heute". |
| `MovieCleanup.java:113-126` und `SeriesImporter.java:728-741` | `askWhitelistOrBlacklist` zweimal: gleicher Dialogtitel, gleicher Aufbau der vier Zeilen, gleicher Wächter. Einmal mit `CrewPendingEntry`, einmal mit vier Strings. |
| `repository/CardDataFactory.java` | Reine Anzeige-Formatierung („Seasons: 12") ohne jeden DB-Zugriff, liegt aber im `repository`-Paket — das laut Regelwerk der Ort für Datenbankzugriff ist. Dass die Klasse dort steht, ist in `Feature-Details.md` festgehalten, also auffindbar; sauber ist die Wurzel `app.movie`. |
| `SeriesImporter.java:132-138`, `:152-154` | Sieben Zähler-Felder, die nur während eines Laufs leben und in `run()` von Hand zurückgesetzt werden. |
| `SeriesImporter.java:222` | `// !MagicNumber -> tmdb.posterWidths=92,154 in config?` — der Marker steht dort zu Recht; die Breiten 92 und 154 stehen an acht Stellen als Literal. |

**Aufwand:** zusammen eine halbe Stunde, ohne den Marker.

**Stand:** offen

### Was in dieser Gruppe trägt

- **Import- und Lesepfad sind getrennte Klassen.** Vier Entitäts-Repositories für den Import,
  `MovieViewerRepository` fürs Lesen. Der Viewer joint nicht selbst, sondern liest die
  `*_details`-Views; die Importer schreiben, ohne Anzeige-Fragen zu kennen. Das hält den
  umfangreichsten Teil dieser Gruppe überschaubar.
- **Das SQL dieses Pakets arbeitet durchgehend mit Parametern** — in acht Repository-Klassen
  keine einzige zusammengesetzte Bedingung.
- **Die Transaktionsgrenze ist je Entität gezogen und begründet**, und der Rollback erstreckt
  sich in `MovieImporter.importNewMovie` ausdrücklich auch auf die Dateien, die keine Transaktion
  hat (`:150-160`). Das ist die Sorte Detail, die man ein Jahr später nicht mehr selbst
  herleiten würde — hier steht sie als Kommentar daneben.
- **Die Regular-Flip-Invariante im Klassen-Javadoc von `SeriesImporter`** (`:71-107`): Was TMDB
  liefert, warum `regulär ⊆ aggregiert` gilt, woran es empirisch geprüft wurde, und was passiert,
  wenn es doch nicht gilt. Ein Fremdsystem-Detail, das nirgends sonst nachzulesen ist.
- **`CardData` als typ-agnostisches Grenzobjekt.** Eine Kachel rendert Film, Serie und Episode;
  die Unterschiede sind in `CardDataFactory` aufgelöst, bevor die Grenze überquert wird. Das ist
  genau die Übergangsregel des Regelwerks — Datenvokabel hinüber, kein Domänentyp.

---

## 4 · Messaging

Gelesen: `app.messaging` mit `repository`, `signal` und `whatsapp` samt deren Unterpaketen —
9 Dateien, rund 2.130 Zeilen.

### 4.1 Die eigene Signal-Kennung steht im Quelltext

**Beleg:** `SignalIncrementalImport.java:87` — `private static final String MY_SERVICE_ID =
"439b7480-…"`, benutzt in `:229`

Der Wert identifiziert genau eine Person: den Nutzer der Suite. Er steht eingecheckt im Code,
während die beiden anderen signal-spezifischen Werte derselben Klasse — `signal.externalPath`
und `signal.key` — ordentlich in der Konfiguration liegen und beim Lesen in Zeile 142 aus `Config`
kommen. Es gibt also schon den richtigen Ort, und dieser eine Wert liegt daneben.

**Was es kostet:** Der Code ist damit nicht mehr übertragbar, ohne dass eine persönliche Kennung
mitwandert — und in einem Repo, in dem ansonsten sorgfältig kein Personenbezug steht, ist das die
Ausnahme, die man nicht sieht, weil sie wie eine UUID aussieht.

**Kleinster Schnitt:** `signal.myServiceId` in die config-Datei, `Config.getString(...)` an der
einen Stelle.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 4.2 Die Kontakt-Auflösung steht in beiden Zweigen

**Beleg:** `SignalIncrementalImport.java:312-336` (`ensureContact`) und
`WhatsAppIncrementalImport.java:425-452` (`resolveContact`)

Beide tun in derselben Reihenfolge dasselbe: Cache fragen, `MessageContactDialog.show(quelle,
rohkennung, vorschlag, repo.loadAllContactsByDisplayName())`, bei `null` mit derselben
FailFast-Meldung abbrechen, je nach Ergebnis einen bestehenden Kontakt nehmen oder einen neuen
anlegen, `insertContactMapping` schreiben, Cache nachziehen. Der einzige Unterschied ist der
Namensvorschlag — Signal leitet einen aus der Fremd-DB ab, WhatsApp übergibt `null`.

Das Regelwerk begründet die Zweiteilung von `messaging` damit, dass die Zweige sich „fast nur das
Datenbank-Schema und den Schreibweg" teilen. Dieser Ablauf ist aber weder Schema noch Schreibweg,
sondern die **Identitätsregel der Suite**: die Entscheidung, ob eine Rohkennung zu einem schon
bekannten Menschen gehört. Genau dafür existiert `msg_contact_mapping` — die Brücke, über die
ein Mensch laut Feature-Doku „Signal *und* WhatsApp umspannen könnte". Sie ist das
Paradebeispiel für geteilten Kern, und ihre Bedienung liegt trotzdem zweimal im Zweig.

**Was es kostet:** Die Regel, nach der Dubletten vermieden werden, ändert sich an zwei Stellen
oder gar nicht. Ein dritter Messenger brächte eine dritte Kopie.

**Kleinster Schnitt:** eine Klasse `ContactResolver` im Kern (`app.messaging`), Konstruktor mit
Quellkennung und `MessageRepository`, eine Methode
`int resolve(Connection thos, String rawIdentifier, String suggestionOrNull)` samt eigenem Cache.
Beide Zweige verlieren je 25 Zeilen.
**Aufwand:** eine Stunde.

Dasselbe, kleiner, bei den Chat-Mitgliedern: `SignalIncrementalImport.java:342-347` hält den
Cache als `Set<String>` mit zusammengesetzten Schlüsseln `"chatId:contactId"`,
`WhatsAppIncrementalImport.java:458-463` als `Map<Integer, Set<Integer>>`. Eine Sache, zwei
Datenstrukturen, und die eine baut Schlüssel aus Zahlen zusammen.

**Stand:** offen

### 4.3 Der WhatsApp-Import merkt sich „heute geprüft", bevor er geprüft hat

**Beleg:** `WhatsAppIncrementalImport.java:135` — `Config.setTime(KV_LAST_CHECK,
LocalDateTime.now());` steht vor Hash-Vergleich, Entschlüsselung und Import

Alles danach ist FailFast: Entschlüsselung, Transaktion, Attachment-Kopie werfen im Fehlerfall.
Der Zeitstempel ist dann aber schon geschrieben. Nach dem Absturz und einem Neustart am selben
Tag steigt `isCheckDue()` sofort aus — der Import versucht es erst am nächsten Tag wieder.

Der Absturz selbst ist sichtbar, das gilt. Der *ausbleibende zweite Versuch* ist es nicht: Die
Suite startet danach wortlos durch und meldet nichts, weil `checkWarning()` nur im Zweig
„Hash unverändert" überhaupt läuft.

**Kleinster Schnitt:** `Config.setTime(KV_LAST_CHECK, …)` ans Ende von `run()` beziehungsweise in
den Zweig, der ohne Arbeit zurückkehrt.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 4.4 Die zwei Wächter in `run()` stehen in der falschen Reihenfolge

**Beleg:** `WhatsAppIncrementalImport.java:125` (`if (!isCheckDue())`) vor `:130`
(`if (whatsAppExternalDir == null)`), gegen `SignalIncrementalImport.java:129-133`, wo die
Konfigurationsprüfung als „Schritt 0" ganz vorne steht

Ist WhatsApp auf diesem Rechner nicht eingerichtet, bricht der Konstruktor (`:106-107`) ab und
lässt sechs Felder uninitialisiert — `dayStartHour` bleibt 0. `isCheckDue()` rechnet dann mit
dieser 0 einen Tagesbeginn aus und liest einen Config-Wert, bevor die Klasse überhaupt weiß, ob
sie zuständig ist. Es geht gut, aber nur zufällig.

Die zwei Zweige beantworten dieselbe Frage („bin ich hier überhaupt eingerichtet?") an
verschiedenen Stellen; der Signal-Zweig hat die richtige.

**Kleinster Schnitt:** die Null-Prüfung an den Anfang von `run()` ziehen. Sauberer noch: ein
Feld `configured` im Konstruktor setzen, dann steht die halbe Konstruktion nicht mehr offen.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 4.5 Die Attachments heißen „move", werden aber kopiert

**Beleg:** `WhatsAppIncrementalImport.java:99` (`pendingMoves`), `:488`, `:512`
(`copyAttachments`), `:526` (`record AttachmentMove`)

Dass kopiert und nicht verschoben wird, ist eine bewusste Entscheidung, und das Javadoc über
`copyAttachments` (`:506-511`) begründet sie ausführlich mit dem Verhalten des Sync-Dienstes.
Genau deshalb ist der Name teuer: Wer `AttachmentMove` und `pendingMoves` liest, nimmt an, dass
die Quelldatei danach weg ist — und das ist der Punkt, an dem die ganze Begründung hängt.

**Kleinster Schnitt:** `AttachmentCopy` und `pendingCopies`. Drei Umbenennungen in einer Datei.
**Aufwand:** fünf Minuten.

**Stand:** offen

### 4.6 Eine quellenspezifische Methode in der quellenneutralen Klasse

**Beleg:** `MessageRepository.java:367-376` — `getLastWhatsAppMessageDate()` mit
`where source = 'whatsapp'` im SQL-Text

`MessageRepository` ist überall sonst über einen `source`-Parameter geführt; das ist seine ganze
Idee („quellunabhängig", so auch das Paket-Javadoc). Diese eine Methode trägt die Quelle im Namen
und als Literal im SQL — und ist zugleich die einzige, die `createStatement` statt eines
PreparedStatement benutzt. Kommt ein dritter Messenger dazu, braucht er eine dritte Methode
gleichen Zuschnitts.

Nebenbei: `rs.next()` wird ohne Prüfung ausgewertet und das Ergebnis von `max(sent_at)` direkt
geparst. Ist noch keine WhatsApp-Nachricht importiert, liefert `max()` NULL und das Dashboard
stirbt an einer NPE ohne Kontext.

**Kleinster Schnitt:** `getLastMessageDate(String source)` mit Parameter und einem
Null-Check, der sagt, was fehlt. Ein Aufrufer (`DashboardScreen.java:78`).
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 4.7 Zwei Schreibweisen für denselben Konfigurationswert

**Beleg:** `SignalIncrementalImport.java:142` —
`Config.getString("signal.externalPath") + "/sql/db.sqlite"` gegen `:398` —
`Config.getPath("signal.externalPath").resolve("attachments.noindex").resolve(att.path())`

Zeile 398 macht es richtig und ist im Regelwerk sogar namentlich als Beispiel genannt (fremder
Wurzelpfad aus `Config`, Unterstruktur im Feature). Zeile 142 klebt stattdessen einen
Pfad mit Schrägstrich an einen String — auf einem Windows-System, wo `Config.getPath` sonst
überall `Path` liefert. Ein Pfad mit Trennzeichen am Ende oder mit Rückstrichen darin ergibt
hier still eine kaputte JDBC-URL.

**Kleinster Schnitt:** `Config.getPath("signal.externalPath").resolve("sql").resolve("db.sqlite")`
und das Ergebnis in die URL.
**Aufwand:** fünf Minuten.

**Stand:** offen

### 4.8 Ein stumm verschlucktes Problem im Entschlüsseler

**Beleg:** `WhatsAppCrypt15Decryptor.java:186-188` —
`catch (Exception e) { // Fallback auf feste Offsets }`

Der Fallback selbst ist in Ordnung: Scheitert der Protobuf-Parser am Header, gelten feste
Offsets, und ob das Ergebnis stimmt, prüft `validateSqliteHeader` anschließend hart. Der Fehler
ist bloß, dass nichts davon sichtbar wird. Ändert WhatsApp sein Headerformat, bekommt man
„kein SQLite-Header" ohne jeden Hinweis darauf, dass der eigentliche Weg vorher schon abgebrochen
ist — und die zwei Zahlen `FALLBACK_IV_OFFSET`/`FALLBACK_DATA_OFFSET` sind dann genau das, was
man untersuchen müsste.

**Kleinster Schnitt:** eine Zeile `Log.debug(this, "Protobuf-Header nicht lesbar, feste Offsets: "
+ e)`. Die Klasse hat als einzige der Gruppe bisher keine Abhängigkeit auf `app.shared`; das ist
kein Wert an sich, und der Architekturtest verlangt ohnehin, dass geloggt nur über `Log` wird.
**Aufwand:** fünf Minuten.

**Stand:** offen

### 4.9 Kleinkram

| Stelle | Was |
|---|---|
| `SignalIncrementalImport.java:88` | `private static final String signalId = "signal";` — Konstante in Feldschreibweise, während der WhatsApp-Zweig sie `SOURCE` nennt (`WhatsAppIncrementalImport.java:74`). Zwei Zweige, zwei Konventionen für dasselbe. |
| `SignalIncrementalImport.java:471` | `if (longest == fallback || …)` — Referenzvergleich auf Strings als Ersatz für ein „noch nichts gewählt". Funktioniert nur, weil `longest` anfangs dieselbe Referenz ist. Ein `boolean gewaehlt` sagt, was gemeint ist. |
| `SignalIncrementalImport.java:359` | Das Javadoc zur Attachment-Benennung erklärt korrekt, wie der Name entsteht, trägt dann aber ein eingeräumtes früheres Versäumnis nach („war ein fehler, aber es funktioniert anscheinend") und schließt mit einer offenen Meinungsverschiedenheit über die Extension-Tabelle. Regel 7 will den Ist-Zustand; die offene Frage gehört als Marker in den Code, nicht in die Beschreibung. |
| `WhatsAppIncrementalImport.java:105-114` | Der Konstruktor kehrt bei fehlender Konfiguration mitten in der Feldbelegung zurück und lässt sechs Felder leer. Siehe 4.4. |

**Aufwand:** zusammen zwanzig Minuten.

**Stand:** offen

### Ein Punkt, der hält, aber im Auge bleiben sollte

Beide Importe halten während der Rückfragen (`WhatsAppChatDialog`, `MessageContactDialog`,
`Alerts`) eine **offene Schreibtransaktion** auf der Suite-DB, und mitten darin liest
`loadAllContactsByDisplayName()` über die Singleton-Connection
(`MessageRepository.java:351-364`). Das Architekturdokument nennt genau diese Kombination als
kritische Regel: Ein offenes Statement auf der Singleton-Connection blockiert den Commit der
transaktionalen. Hier geht es gut, weil `MessageRepository` **jedes** Statement und jedes
ResultSet per try-with-resources schließt — ausnahmslos, in allen 19 Methoden. Das ist kein
Zufall, sondern die Disziplin, von der hier alles abhängt; sie ist der Grund, warum dieser Befund
keiner ist.

### Was in dieser Gruppe trägt

- **`MessageRepository` ist die sauberste Datenzugriffsklasse der Suite.** Durchgehend
  Parameter, durchgehend try-with-resources, quellenneutral bis auf die eine Ausnahme aus 4.6,
  und jede Schreibmethode nimmt die Transaktion von außen entgegen, statt sich eine zu besorgen.
- **`PLATZHALTER_NAME` mitsamt Begründung** (`:26-32`, `:338-350`): warum der Sammelname aus der
  Vorschlagsliste herausgehört, und was passieren würde, wenn er drinstünde. Eine Regel, die
  ohne diese Notiz beim nächsten Anfassen wieder verloren ginge.
- **Die Filterkette des Signal-Imports an einer Stelle** (`forEachImportableMessage`,
  `:194-238`), mit dem Versprechen im Klassen-Javadoc, dass Änderungen nur dort nötig sind. Der
  WhatsApp-Zweig hat kein Gegenstück; seine Entscheidungen liegen verteilt in `processRow`, und
  zwei davon sind auseinandergezogen, weil die Reihenfolge Bedeutung trägt (`:303-321`). Das ist
  dort begründet und kommentiert — aber es zeigt, welche der beiden Fassungen man beim nächsten
  Messenger kopieren will.
- **Das Importfenster über die Fremd-DB statt über Zeitzonen** (Signal): Die zuletzt importierte
  `source_id` wird in der Signal-DB selbst nachgeschlagen, statt einen Zeitstempel hin- und
  herzurechnen. Fünf Minuten Puffer am oberen Ende, Überlappung per DB-Abfrage abgefangen. Das
  ist die Sorte Entscheidung, die einen ganzen Fehlerklasse verschwinden lässt.

---

## 5 · Gesundheit und Tagesdaten

Gelesen: `app.fitbit`, `app.activity`, `app.alc`, `app.diary`, `app.mattress`, `app.weekday`
samt Unterpaketen — 40 Dateien, rund 3.160 Zeilen. Sechs kleine, voneinander unabhängige
Features; entsprechend stehen die Befunde meist je Feature für sich.

### 5.1 Der Tagebuch-Screen baut SQL

**Beleg:** `DiaryScreen.java:174` —
`"EXISTS (SELECT 1 FROM diary_entry_tag det WHERE det.entry_created_at = de.created_at AND
lower(det.tag_name) = '" + escapeSql(tagName) + "')"` — und `:181`,
`lower(de.text) LIKE '%…%'`; verwendet in `Repository.java:120-132` als `WHERE (%s)`

Der `QueryParser` ist eine private innere Klasse von `DiaryScreen` und liefert ein rohes
SQL-Fragment. Damit kennt der Screen Tabellennamen (`diary_entry_tag`), Spaltennamen
(`entry_created_at`, `tag_name`, `text`) **und** den Alias `de`, den es nur im SQL-Text von
`Repository.search` gibt. Zwischen den beiden Klassen läuft ein Vertrag, der nirgends steht:
„der Haupttabellen-Alias heißt `de`".

Der Wächter „SQL steht in Repositories, sonst nirgends" greift hier nicht — er prüft
Abhängigkeiten auf `Statement`/`PreparedStatement`, und die hat der Screen nicht. Das ist
genau die Lücke, die die Regel offen lässt: SQL als String entsteht außerhalb, ausgeführt wird
es drinnen.

**Was es kostet:** Eine umbenannte Spalte oder ein geänderter Alias bricht die Suche zur
Laufzeit; Compiler und Architekturtest sehen nichts. Dazu ist `escapeSql` (`:221`) die einzige
Stelle der ganzen Suite, an der ein Wert außerhalb eines Repositories in SQL hineingeschrieben
wird — bei einer lokalen Einzelnutzer-DB kein Sicherheitsproblem, aber eben auch keine Zeile, die
irgendwo sonst so stehen dürfte.

**Kleinster Schnitt:** `QueryParser` als paketprivate Klasse nach `diary.repository` verschieben
und `search(String rawQuery, …)` die Übersetzung selbst machen lassen; `InvalidQueryException`
fliegt dann von dort und der Screen fängt sie wie bisher für `setQueryValid(false)`. Ein
Dateiumzug, zwei geänderte Signaturen — die Abfragesprache selbst bleibt unangetastet.
**Aufwand:** dreiviertel Stunde.

**Stand:** offen

### 5.2 „Ist die Matratze fällig" wird zweimal beantwortet, in zwei Einheiten

**Beleg:** `MattressTurnDialog.java:28-29` (`ChronoUnit.WEEKS.between(...) <
Config.getInt("mattress.dueAfterWeeks", 4)`) gegen
`MattressRepository.java:46` (`Config.getInt("mattress.dueAfterWeeks", 4) * 7 -
ChronoUnit.DAYS.between(...)`)

Dieselbe Regel, zwei Rechnungen, zwei Einheiten, und der Vorgabewert `4` steht an beiden Stellen
noch einmal daneben. `ChronoUnit.WEEKS.between` schneidet ab: Der Dialog erscheint erst, wenn
volle vier Wochen um sind, die Dashboard-Kachel zählt dagegen tagweise herunter und steht schon
vorher auf 0 — beziehungsweise geht ins Negative, bis der Dialog kommt. Die Kachel sagt also
„fällig", und es passiert nichts, und umgekehrt.

Dazu wohnt die Fachregel in `MattressRepository`, also in der Klasse, die laut Regelwerk für
Datenbankzugriff da ist. Das Feature hat nur zwei aktive Klassen, und die Regel steht in der
falschen.

**Kleinster Schnitt:** `getDaysUntilNextTurn()` bleibt die eine Wahrheit, wandert aber in
`MattressTurnDialog` (oder eine kleine `MattressService`-Klasse); `showIfDue()` fragt
`getDaysUntilNextTurn() <= 0`. Der Vorgabewert steht dann einmal.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 5.3 `PointsCalculator` rechnet nicht nur, er fragt

**Beleg:** `PointsCalculator.java:114` und `:125` — `Alerts.show(...)` mitten in der
Berechnungsschleife einer statischen Methode

Die Klasse heißt `…Calculator`, ihr Javadoc sagt „Berechnet die Tagespunkte", ihre einzige
öffentliche Methode ist `static int getDayPoints(...)` — und sie öffnet modale Dialoge. Heute
geht das gut, weil der einzige Aufrufer (`DataReviewService`) ohnehin im PostTask mit Dialogen
arbeitet. Sobald jemand die Punkte ein zweites Mal ausrechnen will — rückwirkend über die
Historie, im Vergleich gegen eine zweite Datenquelle, beim anstehenden Umzug auf Google Health —
poppen Fenster auf.

Dazu die Eskalation: Fünf Plausibilitätsprüfungen, vier verschiedene Reaktionen.

| Anomalie | Reaktion |
|---|---|
| Bike mit Schritten (`:63`) | zweimal `Log.error` und `throw` |
| Spinning ohne Schritte (`:84`) | zweimal `Log.error` und `throw` |
| Sport ohne Schritte (`:96`) | nur `Log.error`, ausdrücklich „keine Exception" |
| Outdoor Bike (`:105`) | `Log.error` bei Schritten, dazu **immer** ein Alert |
| unbekannte Aktivität (`:124`) | Alert und `Log.warn`, Aktivität wird verworfen |

Welche Anomalie den Import abbricht und welche nur eine Notiz wert ist, ist eine fachliche
Entscheidung — und sie steht nirgends, außer im jeweiligen Zweig. Der unterste Fall ist der
unangenehmste: Eine unbekannte Aktivität geht mit einem OK-Klick als null Punkte in die
Tagespunkte ein und ist danach nicht mehr auffindbar.

**Kleinster Schnitt:** `getDayPoints` liefert ein
`record DayPoints(int punkte, List<String> auffaelligkeiten)`; `DataReviewService` zeigt die
Liste in seinem ohnehin vorhandenen Abschluss-Dialog. Die zwei `throw`-Fälle bleiben, wo sie
sind — das sind echte FailFast-Fälle.
**Aufwand:** dreiviertel Stunde. Lohnt vor allem, weil die Health-Migration genau hier ansetzt.

**Stand:** offen

### 5.4 „Welches Wochenziel galt in Woche X" — zwei Mechanismen im selben Paket

**Beleg:** `DashboardService.java:95` und `:124` rufen `repository.getWeeklyGoalForDate(...)`
**in der Schleife**, einmal je Woche; `FitbitStatisticsPresenter.java:35` lädt stattdessen
`repository.getAllGoalHistory()` einmal und ordnet selbst zu

Beide beantworten dieselbe Frage, einer mit N Abfragen und einer mit einer. `calculateRecordStreak`
läuft über die gesamte Historie — bei fünf Jahren sind das 260 Einzelabfragen, und der Aufruf
hängt am Aufbau des Dashboards.

Wichtiger als die Laufzeit ist, dass es zwei Wege gibt: Wer die Ziel-Historie einmal anfasst
(etwa um rückwirkende Änderungen zu erlauben), muss beide finden, und der bessere steht schon da.

**Kleinster Schnitt:** `DashboardService` holt `getAllGoalHistory()` einmal und benutzt dieselbe
Zuordnung wie der Presenter — die sich dabei anbietet, in eine kleine gemeinsame Methode zu
ziehen.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 5.5 `DashboardService` bekommt „heute" übergeben und benutzt es dann nicht

**Beleg:** `DashboardService.java:70-71` — `calculateCurrentStreak(LocalDate today)` ruft
`repository.getWeeksInRange(LocalDate.now().minus(9999, WEEKS), LocalDate.now())`; `:113-114`
dasselbe ohne Parameter

Der Aufrufer (`DashboardScreen`) übergibt `AppClock.TODAY`, also den Tag, auf den sich die ganze
Suite geeinigt hat. Die Methode nimmt ihn entgegen, benutzt ihn für die Montags-Prüfung und holt
sich die Daten dann über die Systemuhr. Läuft die Suite über Mitternacht, rechnen die beiden
Hälften derselben Methode mit verschiedenen Tagen.

Nebenbei: `LocalDate.now().minus(9999, ChronoUnit.WEEKS)` ist zweimal die Schreibweise für
„alles". Ein `getAllWeeks()` im Repository sagt dasselbe und braucht keine Zahl, die nur deshalb
stimmt, weil sie groß genug ist.

**Kleinster Schnitt:** `today` durchreichen (und dann konsequent nutzen), `calculateRecordStreak`
denselben Parameter geben. Gehört zum übergreifenden Uhr-Befund in Gruppe 7.
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 5.6 `logApiResponse` loggt keine API-Antwort

**Beleg:** `Repository.java:238` (`logApiResponse(LocalDate date, String jsonResponse)`) gegen
`DataReviewService.java:224-236` („VERWENDETE Daten loggen") — geschrieben wird das Ergebnis
**nach** der Korrektur im Dialog, nicht die Antwort der API

Der Name ist das Einzige, was noch sagt, es gehe um die API. Wer die Datei
`log/fitbit_import.log` später öffnet, um nachzuvollziehen, was Fitbit geliefert hat, findet
darin die eigenen Korrekturen. Für den anstehenden Umzug auf eine andere Datenquelle ist das
genau die Datei, die man als Referenz nehmen würde — und sie taugt nicht dafür.

**Kleinster Schnitt:** `logImportedDay(LocalDate, String json)` — der Name sagt dann, was
drinsteht. Wer wirklich die Rohantwort will, bekommt sie als zweite Zeile; `ApiClient.ApiResponse`
trägt das `originalJson` bereits mit sich und niemand liest es heute aus.
**Aufwand:** zehn Minuten für den Namen, eine halbe Stunde mit der Rohantwort.

**Stand:** offen

### 5.7 Ein Übergangsgerüst, dessen Termin verstrichen ist

**Beleg:** `DataFetcher.java:102-117` (`!tmp: … Fällt mit dem Vergleicher im September weg`),
`FitbitDayProjection.java` (dieselbe Zusage im Javadoc), dazu die namentliche Ausnahme für
`app.tmp` in `ArchitekturRegelnTest.keinSeitwaertsgriffAufObersterEbene`

Der Marker nennt September als Ende; der Stand dieses Berichts ist der 20.09.2026. Solange das
Gerüst steht, hält `app.fitbit` eine öffentliche Projektion nur für einen Abnehmer offen, und
der Architekturtest trägt eine Ausnahmezeile, die laut ihrem eigenen Javadoc mit `app.tmp`
verschwinden soll („greift dann noch etwas seitwärts, bricht der Build, und das ist richtig so").

Das ist kein Konstruktionsfehler — es ist ein bewusst befristetes Gerüst, dessen Frist erreicht
ist. Es steht hier, damit es beim Aufräumen nicht übersehen wird; der eigentliche Abriss gehört
zu Gruppe 6 (`app.tmp`).

**Stand:** offen

### 5.8 Kleinkram

| Stelle | Was |
|---|---|
| `WeekdayDialog.java:30` | `LocalDateTime.now().getHour() < 6` — ein Tagesbeginn als Literal, während `whatsapp.daystartHour` dieselbe Idee als Config-Schlüssel führt. Zwei Features, zwei Antworten auf „wann fängt der Tag an". |
| `DataFetcher.java:37-73` | Der Rumpf von `fetch()` ist durchgehend eine Ebene zu tief eingerückt — der Rest eines entfernten try-Blocks. Beim Lesen sucht man die schließende Klammer. |
| `DataReviewService.java:226-236` | Jackson und `LinkedHashMap` voll qualifiziert mitten im Methodenrumpf, statt importiert. |
| `DataReviewService.java:238-241` | `Log.error(...)` **und** `throw new RuntimeException(e)` — derselbe Fehler wird zweimal gemeldet, einmal in der Datei und einmal im globalen Alert. |
| `DiaryEditorPresenter.java:26-33` | „Erwartete DiaryEditor-API (gebaut in Schicht 2)" — ein Bauplan aus der Entstehungszeit, der die Signatur einer anderen Klasse zweitkopiert. Regel 7, und er wird beim nächsten Parameter still falsch. |
| `StartupService.java:27`, `DiaryEditorPresenter.java:49`, `MattressTurnDialog.java:28,50,51` | `LocalDate.now()` / `LocalDateTime.now()` statt `AppClock.TODAY`; siehe Gruppe 7. |

**Aufwand:** zusammen eine halbe Stunde.

**Stand:** offen

### Was in dieser Gruppe trägt

- **`app.activity` ist die Blaupause für den Datenquellen-Wechsel.** Der Client hält nichts fest,
  persistiert nichts, kennt kein Repository — und sein Javadoc sagt in vier Zeilen, worin sich
  das OAuth-Modell von Fitbit unterscheidet und warum ein einmaliger Refresh genügt. Dazu der
  Satz, der die eigentliche Falle benennt: Ein fehlender Tag heißt „nicht getragen" und darf
  **nicht** als 0 zählen. Genau solche Sätze fehlen, wenn eine Migration schiefgeht.
- **Die zwei Statistik-Presenter (`alc`, `fitbit`) sind framework-frei und liefern eine reine
  Diagramm-Beschreibung** (`BarChartData`), die dieselbe geteilte View zeichnet. Zwei Features,
  ein Screen-Typ, keine Kopie — und die Grenze trägt Daten, keine Nodes.
- **Der Alkohol-Kontostand wird nicht gespeichert, sondern gerechnet**, und die Ratio-Historie
  hängt am `valid_from`. Rückwirkende Änderungen an der Gewichtung bleiben damit möglich, ohne
  gespeicherte Summen nachziehen zu müssen.
- **Die Abfragesprache des Tagebuchs ist ein sauberer rekursiver Abstieg** mit eigener
  Fehlerklasse und Positionsangaben in den Meldungen. Befund 5.1 betrifft ihren *Ort*, nicht
  ihre Qualität.
- **`DataFetcher` fängt nichts.** Das Javadoc sagt ausdrücklich, warum: Ob ein totes Netz den
  Start reißen darf, ist eine Aussage über den Startablauf und gehört dem Controller. Das ist
  die Sorte Zuständigkeitsgrenze, um die es in diesem Bericht geht — hier ist sie richtig gezogen
  und aufgeschrieben.

---

## 6 · Controller, Start und Screens

Gelesen: `app.controller` mit `model`, `app.tmp` und `ThosSuiteApp` — 11 Dateien, rund 1.850
Zeilen.

### 6.1 Das Statistik-Menü wird über seinen Anzeigetext angesteuert — und erzeugt dabei einen toten Screen

**Beleg:** `MainWindow.java:181,184,187` (`onStatisticsSelected.accept("Dashboard")`) gegen
`Controller.java:268-280` (`if ("Dashboard".equals(item)) …`)

Zwischen Fenster und Controller läuft hier ein String als Protokoll. Nebenan steht mit
`PlayMenuNode` die richtige Lösung für genau dasselbe Problem: ein sealed Interface mit zwei
Records, dessen Javadoc sogar erklärt, warum es zwei Typen sein müssen. Das Spielen-Menü ist also
typsicher, das Statistik-Menü daneben nicht.

Der Preis ist mehr als Geschmack. `onStatisticsMenuItemSelected` steht **innerhalb** von
`requestSessionSwitch`, und das hat den bisherigen Screen zu diesem Zeitpunkt bereits über
`closeSilent(false)` beendet. Trifft keiner der drei Vergleiche — ein geänderter Menütext, ein
vierter Eintrag ohne passenden Zweig —, bleibt `currentScreen` der **alte, gerade geschlossene**
Screen, und die zwei Zeilen darunter zeigen ihn wieder an und rufen `start()` auf ihm. Das
Regelwerk gibt dagegen eine ausdrückliche Zusage: „Es gibt keinen toten Screen. Ein Screen ist
entweder lebendig und erreichbar, oder er ist ersetzt und damit unerreichbar — dazwischen liegt
kein Zustand."

**Kleinster Schnitt:** ein `enum StatisticsItem { DASHBOARD, FITBIT, ALKOHOL }` mit Anzeigename,
`Consumer<StatisticsItem>` statt `Consumer<String>`, und im Controller ein `switch` über das
Enum — dann ist der fehlende Zweig ein Übersetzungsfehler. Der `default`-Fall entfällt damit
ersatzlos.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 6.2 Die Suite kann sich nicht selbst einrichten

Das ist kein Befund an einer Stelle, sondern ein Muster, das erst hier sichtbar wird, weil hier
alle Features zusammenlaufen:

| Stelle | Was fehlt, und was dann passiert |
|---|---|
| `DataFetcher.java:41-45` | „Kein Fitbit-Import-History gefunden. **Bitte manuell das erste Datum in die Datenbank eintragen.**" — die Meldung sagt es selbst |
| `MattressRepository.java:100` | `throw new RuntimeException("Keine Matratzen-Einträge vorhanden.")`, gerufen aus `DashboardScreen.java:66` |
| `MessageRepository.getLastWhatsAppMessageDate()` | `max(sent_at)` auf leerer Tabelle → NPE, gerufen aus `DashboardScreen.java:78` |
| `RegionDeckService` (Befund 2.1) | kein Codeweg legt die erste Zeile in `region_learn_stat` an; `scripts/learn/CopyLandkreiseStats.java` tut es von Hand |
| `Config` („throw on miss") | „Jeder Key wird vor seinem ersten Lauf mit Startwert angelegt" — von Hand |

Einzeln ist jedes davon eine bewusste FailFast-Entscheidung, und als solche in Ordnung. Zusammen
ergeben sie etwas, das keiner von ihnen für sich beabsichtigt: **Das Dashboard — der einzige
Screen, der quer durch alle Features liest — stirbt an der ersten Feature-Tabelle, die leer ist.**
Acht funktionierende Kacheln gehen verloren, weil eine neunte keine Zeile hat. Und wer die Suite
neu aufsetzt oder ein Feature zum ersten Mal einschaltet, muss vorher wissen, in welche Tabellen
er von Hand schreibt.

Das ist keine Aufforderung, FailFast aufzugeben. Aber „eine Zeile fehlt" ist beim
*ersten* Lauf kein Bug, sondern der Normalzustand, und die Suite unterscheidet die beiden nicht.

**Kleinster Schnitt:** Die Dashboard-Kacheln sind der Ort, an dem es am wenigsten kostet und am
meisten bringt — `buildContent()` sammelt pro Kachel und setzt bei fehlenden Daten „—" statt die
ganze Seite zu reißen. Die eigentliche Frage („legt ein Feature seine erste Zeile selbst an?")
gehört pro Feature entschieden; für die Region-Decks steht sie schon als Befund 2.1.
**Aufwand:** eine halbe Stunde fürs Dashboard; die Feature-Frage je nach Antwort.

**Stand:** offen

### 6.3 Der Exporter fängt genau den Fehler ab, den `Config` bewusst wirft

**Beleg:** `SuiteExporter.java:53-61` — vier `Config`-Zugriffe in einem `try`, im `catch` ein
Alert („Ich kann vermutlich einen Ordner nicht finden.") und `return`; die Exception wird
verworfen

Der Config-Kontrakt ist ausdrücklich: „Ein unbekannter Key ist per Design ein Bug, kein
abzufangender Fall." Genau dieser Wurf wird hier gefangen, in eine Vermutung übersetzt und
weggeworfen. Ein Tippfehler in `exporter.oneDriveFolder` erzeugt damit dauerhaft denselben Satz,
der nicht sagt, welcher der vier Werte fehlt — und die Exception, die es wüsste, ist weg.

Zwei weitere Stellen derselben Datei:
- `:81` — `throw new RuntimeException("Export fehlgeschlagen: " + e.getMessage())` **ohne** `e`.
  Ein Zip- oder IO-Fehler verliert seinen Stacktrace. (Dieselbe Sorte wie Befund 2.5.)
- `:104` — die Meldung sagt „unbekannte Ignore-Zeile **ignoriert**", der Code wirft. Wer das
  liest, sucht die übersprungene Zeile und nicht den Abbruch.

**Kleinster Schnitt:** Den Config-`try` ersatzlos streichen (oder wenigstens `e.getMessage()` in
den Alert nehmen), bei `:81` das `e` durchreichen, bei `:104` den Text auf „abgebrochen" ändern.
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 6.4 Vier öffentliche Methoden am `MainWindow` ohne Aufrufer — samt der Mechanik dahinter

**Beleg:** `MainWindow.java:294` (`updateLearnItems`), `:412` (`setWidth`), `:416` (`setHeight`),
`:424` (`setCurrentSortOrder`)

`updateLearnItems` ist außerdem zeichengleich mit `setLearnItems`. Der interessanteste Fall ist
`setCurrentSortOrder`: Die Methode trägt ein Javadoc, das behauptet, sie werde „initial ein Mal
vom Controller nach Lesen der config aufgerufen" — niemand ruft sie. Dafür existiert eine ganze
kleine Mechanik nur für sie: `item.setUserData(order)` in `:151`, mit einem Kommentar daneben,
der erklärt, warum die Zuordnung nicht über den Anzeigetext läuft. Der Kommentar hat recht, die
Mechanik ist richtig gebaut — und sie läuft nie.

Das ist ein sauberes Beispiel dafür, warum toter Code teurer ist als seine Zeilenzahl: Er zieht
Begründungen nach sich, die man beim Lesen ernst nimmt.

**Kleinster Schnitt:** die vier Methoden löschen; bei `setCurrentSortOrder` entscheiden, ob sie
fehlt (dann vom Controller rufen — das Menü markiert die aktive Reihenfolge beim Start heute über
den Umweg `lastSortOrderString` in `:148`) oder überflüssig ist (dann auch `setUserData` mit weg).
**Aufwand:** eine Viertelstunde, plus die eine Entscheidung.

**Stand:** offen

### 6.5 Vier Kommentare, die etwas anderes sagen als der Code

- `MainWindow.java:315-316` — „Ignoriere wenn weniger als **300ms** seit letztem ESC", darunter
  `if (now - lastEscapeTime < 500)`.
- `SuiteExporter.java:104` — „ignoriert", der Code wirft (siehe 6.3).
- `ThosSuiteApp.java:368` — „ScenicView-Exceptions ignorieren … Nicht crashen für externes Tool".
  Die Prüfung steht **hinter** `alert.showAndWait()`, der Fehlerdialog samt Stacktrace erscheint
  also trotzdem; nur das `Platform.exit()` entfällt. Nebenbei: Ein Entwicklungswerkzeug hat hier
  eine namentliche Ausnahme im Produktivpfad.
- `Controller.java:85-88` — ein Kommentar, der eine Entwurfsfrage (soll das MainWindow den
  Controller direkt kennen?) als Gespräch protokolliert und offen lässt. Die Frage ist
  berechtigt und die Antwort steht im Regelwerk („Das MainWindow kennt den Controller nicht
  direkt — es ruft die hinterlegten Callbacks"). Als Kommentar an dieser Stelle beschreibt er
  nicht, was ist, sondern was man einmal erwogen hat.

**Kleinster Schnitt:** Zahl angleichen, Texte richtigstellen, die ScenicView-Prüfung vor den
Alert ziehen, den Entwurfs-Dialog durch einen Satz ersetzen, der die Richtung benennt.
**Aufwand:** zwanzig Minuten.

**Stand:** offen

### 6.6 Das Übergangsgerüst `app.tmp` ist fällig

**Beleg:** `Comparison.java:26` und `HealthImportLog.java:31` („fällt im September ersatzlos
weg" / „Im September ersetzt durch DB-Spalten"), `Controller.java:73-74,150-157,187-190`
(vier `!tmp`-Stellen), `DataFetcher.java:102-117` (`getProjection`),
`FitbitDayProjection.java`, dazu die Ausnahmezeile in
`ArchitekturRegelnTest.keinSeitwaertsgriffAufObersterEbene`

Das Gerüst ist vorbildlich gebaut: eigenes Paket, kein Feature-Bezug, zweigeteilt für den
Startup-Split, mit Fristangabe in beiden Klassen und einer sichtbaren statt einer fehlenden
Architekturregel. Der einzige Befund ist, dass die Frist mit dem Stand dieses Berichts
(20.09.2026) erreicht ist — und dass daran sechs Stellen in vier Paketen hängen, die beim Abriss
alle mitgehen müssen. Solange es steht, hält `app.fitbit` eine öffentliche Projektion offen, die
nur dieser eine Abnehmer braucht.

Der Abriss selbst ist nicht der Punkt — die Frage, die er offenlässt, schon: Der
Fitbit-Health-Vergleich ist die Vorarbeit für den Datenquellen-Wechsel, und der steht in
`Feature-Details.md` als „noch komplett offen". Siehe Szenario 3 in Gruppe 11.

**Stand:** offen

### 6.7 Kleinkram

| Stelle | Was |
|---|---|
| `MainWindow.java:36` | `@SuppressWarnings("deprecation")` auf der ganzen 444-Zeilen-Klasse. Gebraucht wird es für ein, zwei JavaFX-Aufrufe; so deckt es auch jede künftige Verwarnung in der Datei zu. |
| `Controller.java:360,390` | dieselbe `=== … ===`-Ablaufverfolgung auf `Log.info` wie in Befund 1.13. |
| `DashboardScreen.java:33-96` | Neun Kacheln, jede baut ihr Repository selbst und setzt ihren Beschriftungstext (`"Aktueller Fitbit-Streak in Wochen (Rekord: " + …)`) zusammen. Für den Querschnitts-Screen der Orchestrierung ist das vertretbar; auffällig ist nur, dass auch die Zahlformatierung (`NumberFormat.getInstance(Locale.GERMANY)`) hier liegt und nicht in der View. |
| `ThosSuiteApp.java:198,281,346` | `e.printStackTrace()` und `System.err.println` statt `Log`. Alle drei liegen im Fenster **vor** der Log-Initialisierung; dort geht es nicht anders. Kein Befund, aber der Grund gehört als halber Satz daneben, sonst sieht es beim Lesen wie ein Versehen aus. |

**Aufwand:** zusammen zwanzig Minuten.

**Stand:** offen

### Was in dieser Gruppe trägt

- **`requestSessionSwitch` nimmt die Aufbau-Routine, nicht die fertige Session** — und das
  Javadoc sagt, warum (`Controller.java:382-388`). Drei Strategien, drei klar getrennte Zweige,
  `suspend()`/`resume()` sauber um die Dialoge gelegt. Das ist die tragende Mechanik der ganzen
  Suite, und sie ist auf einer Bildschirmseite lesbar.
- **Die Begründung, warum der Start einen toten Fremddienst aushält** (`Controller.java:120-132`):
  Die Ausnahme von FailFast steht dort, wo die Regel gemacht wird, statt in den Datenklassen
  vergraben — und die drei Fehler werden zu *einer* Meldung zusammengefasst, statt zu dreien, die
  dasselbe sagen. Beides ist ausdrücklich aufgeschrieben.
- **Jeder Folgeschritt hängt an seinem eigenen Import** (`Controller.java:183-190`): Kein
  Sammelstatus, kein „wenn irgendwas schiefging, machen wir gar nichts".
- **`PlayMenuNode` als sealed Interface**, mit der Begründung, warum zwei Records und nicht ein
  Record mit Nutzlast. Genau das Muster, das 6.1 im Nachbarmenü vermisst.
- **`MainWindow` enthält keine Fachlogik.** Es baut Menüs und reicht Callbacks weiter; die
  einzigen Fachentscheidungen, die es trifft, sind Navigationsfragen — und das steht als Satz
  über `buildMenuBar` (`:129`).
- **Der White-Flash-Umweg im Startablauf** (`ThosSuiteApp.java:157-189`): drei verschachtelte
  `runLater`, eine `PauseTransition`, und an jedem Schritt steht, welches konkrete Problem er
  löst. Ohne diese Kommentare wäre die Konstruktion in einem Jahr nicht mehr zu verantworten —
  mit ihnen ist sie es.

---

## 7 · shared — Basisdienste und Modell

Gelesen: `app.shared` (Wurzel) und `app.shared.model` — 43 Dateien, rund 2.040 Zeilen.

### 7.1 Die Suite hat zwei Antworten auf „welcher Tag ist heute"

**Beleg:** `AppClock.java:6` — `public static final LocalDate TODAY = LocalDate.now();` gegen
**46 Stellen in 25 Dateien**, die `LocalDate.now()` oder `LocalDateTime.now()` direkt aufrufen;
`AppClock` selbst wird von 11 Dateien benutzt

`AppClock.TODAY` friert das Datum beim Klassenladen ein — das ist offenkundig Absicht: Eine
Lernsession, die um 23:58 beginnt, soll um 00:03 nicht plötzlich in den nächsten Tag rutschen und
zwei Fälligkeitsrechnungen mit verschiedenen Tagen anstellen. Die Klasse hat sogar eine leere
`init()`, nur damit `ThosSuiteApp` den Ladezeitpunkt festlegen kann.

Benutzt wird sie von der Lernseite, von `alc` und vom Dashboard. Alles andere — fitbit, diary,
mattress, messaging, movie, weekday und `Config.getDaysSince` selbst (`Config.java:120`) — fragt
die Systemuhr. Am selben Objekt trifft beides aufeinander: `DashboardScreen` übergibt
`AppClock.TODAY` an `DashboardService.calculateCurrentStreak(today)`, und die Methode holt sich
ihre Daten eine Zeile später über `LocalDate.now()` (Befund 5.5).

**Was es kostet:** Läuft die Suite über Mitternacht — und sie läuft laut ihrem eigenen Zuschnitt
tagelang durch —, rechnen zwei Hälften derselben Auswertung mit verschiedenen Tagen. Das ist
kein Absturz, sondern eine falsche Zahl, die man nicht bemerkt: ein Streak, der um eins daneben
liegt, ein Tagesbudget für neue Karten, das aus dem Vortag stammt, eine Matratzen-Kachel, die
nicht zum Dialog passt (Befund 5.2). Solche Fehler fallen im täglichen Gebrauch gerade nicht auf
— und darauf beruht die Testentscheidung der Suite.

Was fehlt, ist nicht die Klasse, sondern die Aussage darüber, wann welche gilt. `AppClock`
trägt keinen einzigen Satz Javadoc.

**Kleinster Schnitt:** Zuerst die Regel aufschreiben — drei Sätze im Javadoc von `AppClock`:
*„Suite-Tag" heißt der Tag, an dem die Suite gestartet wurde. Alles, was einen Tag als
fachlichen Schlüssel benutzt (Fälligkeit, Tagesbudget, gespeicherte Tagesdatensätze), nimmt
`AppClock.TODAY`. Was einen echten Zeitstempel braucht (Log-Zeile, `played_timestamp`,
Antwortzeit), nimmt `LocalDateTime.now()`.* Danach die Stellen durchgehen, die einen *Tag*
meinen; die Zeitstempel bleiben, wie sie sind. `AppClock` sollte dafür ein `NOW()` für
Zeitstempel dazubekommen, sonst bleibt die Unterscheidung eine Konvention ohne Halt.
**Aufwand:** eine halbe Stunde für die Regel, zwei Stunden für die Durchsicht der 46 Stellen.

Dies ist der einzige Befund des Berichts, der quer durch **alle** Gruppen reicht.

**Stand:** offen

### 7.2 `Config.getString` ist ein zweiter Name für `Config.get`

**Beleg:** `Config.java:78-80` — `public static String getString(String key) { return get(key); }`;
drei Aufrufer (`SuiteExporter.java:57`, `SignalIncrementalImport.java:142,143`) gegen 24 für
`Config.get`

Die Fassade ist die meistgenutzte Klasse der Suite (27 Pakete importieren sie), und ihr wichtigster
Zug ist, dass sie *die eine Tür* ist. Zwei Namen für dieselbe Tür kosten an genau der Stelle, an
der die Klasse ihren Wert hat: Wer wissen will, wo ein Schlüssel gelesen wird, greppt nach
`Config.get(` und übersieht drei Stellen — darunter die, an der der Signal-Datenbankschlüssel
gelesen wird.

**Kleinster Schnitt:** `getString` löschen, drei Aufrufstellen umstellen.
**Aufwand:** fünf Minuten.

**Stand:** offen

### 7.3 `DB` baut viermal dieselbe Verbindung auf

**Beleg:** `DB.java:192-206`, `:222-232`, `:238-252`, `:257-267` — vier Methoden, die sich in
zwei Dingen unterscheiden: welches Pfad-Feld und ob `setAutoCommit(false)`

In allen vieren steht `connection.createStatement().execute("PRAGMA foreign_keys = ON")`.
Fremdschlüssel sind in SQLite pro Verbindung abzuschalten und einzuschalten — wer eine fünfte
Verbindungsart ergänzt (oder eine dritte Datenbank, siehe Szenario 3) und die Zeile vergisst,
bekommt keine Fehlermeldung, sondern stillschweigend keine Fremdschlüsselprüfung mehr. Das ist
die teuerste Sorte vergessener Kopie: eine, die nichts kaputtmacht, sondern eine Sicherung
abschaltet.

Dazu verweist das Javadoc in `:176` auf `{@link #getNonAutoCommitConnection()}` — diese Methode
gibt es nicht, gemeint ist `getNewConnection()`. Und das in dem Javadoc, das die
SQLITE_BUSY-Regel der ganzen Suite erklärt und das man deshalb wirklich liest.

**Kleinster Schnitt:** ein privates `open(Path path, boolean autoCommit)`; die vier öffentlichen
Methoden werden zu je zwei Zeilen. Den toten Link richtigstellen.
**Aufwand:** zwanzig Minuten.

**Stand:** offen

### 7.4 `FilenIgnoreSource`: zweimal dieselben vier Zeilen, und die zweite wirft beim Herunterfahren

**Beleg:** `FilenIgnoreSource.java:59-61` gegen `:76-78`

```java
// addToIgnore
String lineToAdd = Config.get("filenIgnore.lineToAdd", null);   // Default: null

// removeFromIgnore
String lineToAdd = Config.get("filenIgnore.lineToAdd");          // throw on miss
```

Die beiden Methoden lesen dieselben zwei Schlüssel auf dieselbe umständliche Art (ein
`Config.get(key, null) != null ? Config.getPath(key) : null` in einer Zeile), aber die eine
verträgt einen fehlenden Schlüssel und die andere nicht. Ist `filenIgnore.path` gesetzt und
`filenIgnore.lineToAdd` nicht, startet die Suite anstandslos — und `removeFromIgnore()` wirft
beim Beenden, aus `ThosSuiteApp.stop()` heraus. Dort fängt es ein `catch`, das nur loggt; man
sieht es also gar nicht, und die Ignore-Zeile bleibt stehen.

Nebenbei: Die Klasse heißt `…Source`, das Suffix steht laut Benennungsregel für „die Daten
stammen aus einer Datei". Diese hier **schreibt** eine.

**Kleinster Schnitt:** ein privates `config()`, das beide Werte einmal liest und `null`
zurückgibt, wenn eines fehlt; beide Methoden rufen es. Die Klasse in `FilenIgnoreFile` oder
`FilenIgnore` umbenennen.
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 7.5 Zwei Stellen werfen ohne Ursache, eine reduziert sie auf den Text

**Beleg:** `SingleInstanceGuard.java:32` und `DbRegionDeckProgressRepository.java:34` werfen eine
neue Exception, ohne `e` durchzureichen; `SuiteExporter.java:81` reicht nur `e.getMessage()` weiter

Ich habe alle 27.700 Zeilen daraufhin durchsucht: Es sind genau diese drei. In jedem anderen
`catch`, das wirft, hängt die Ursache dran. Das ist eine bemerkenswerte Disziplin — und macht die
drei Ausreißer zu einer Fünf-Minuten-Arbeit statt zu einem Thema.

Beim `SingleInstanceGuard` wiegt es am schwersten: Die Klasse läuft, bevor das Logging steht;
scheitert der Datei-Lock aus irgendeinem Grund (Rechte, gesperrter Ordner, fehlender log-Ordner),
ist „Oops. Probleme beim Versuch sicherzustellen, dass die App nicht bereits läuft." alles, was
es je zu sehen gibt.

**Kleinster Schnitt:** `, e` an drei Stellen.
**Aufwand:** fünf Minuten.

**Stand:** offen

### 7.6 `UiUtils` trägt drei unverwandte Dinge, eines davon globalen Zustand

**Beleg:** `UiUtils.java:22,37-43` (das Owner-Fenster aller Dialoge) neben `:55-71` (Farb-Hex),
`:81-141` (Bild-Tinting, Helligkeits-Ableitung), `:143-172` (ESC-Sperre, Plus-Badge)

Das Klassen-Javadoc sagt „Diverse JavaFx Bildmanipulationen für die Anzeige" — das deckt zwei der
drei Gruppen. Die dritte ist etwas anderer Natur: `ownerWindow` ist der einzige veränderliche
statische Zustand der gesamten `shared`-Wurzel, er wird einmal beim Start gesetzt, und **jeder
Dialog der Suite hängt daran** (ohne ihn bleibt er ungestylt — das steht sauber dokumentiert in
`:28-32`). Das Javadoc räumt selbst ein: „Etwas geschummelt ist es trotzdem."

**Was es kostet:** Die Frage „wo wohnt das Owner-Fenster?" hat keine vorhersagbare Antwort —
`UiUtils` ist ein Name, der nichts verspricht, und die Klasse muss im Javadoc erklären, warum
das Ding nicht im `SkinService` liegt. Genau diese Vorhersagbarkeit ist laut Regelwerk der
Maßstab, an dem alles andere gemessen wird.

**Kleinster Schnitt:** eine eigene Mini-Klasse `DialogOwner` in `app.shared` (ein Feld, zwei
Methoden), samt dem vorhandenen Javadoc. `UiUtils` bleibt, was sein Name sagt: Farbe und Bild.
Sechs Aufrufstellen.
**Aufwand:** zwanzig Minuten.

**Stand:** offen

### 7.7 Der Screen-Vertrag verweist auf Methoden, die es nicht gibt

**Beleg:** `Screen.java:18` — `{@code currentScreen.sort(...)}`; die Methode heißt
`sortOrderChanged()` und nimmt nichts entgegen. `Screen.java:93` — `@param order` an einer
parameterlosen Methode.

Derselbe Fehler steht in `CardSortOrder` (Befund 1.7) und stammt offensichtlich aus derselben
Umbenennung. Hier wiegt er mehr, weil dieses Javadoc die zentrale Entwurfsentscheidung begründet
(„Warum nicht in Sub-Interfaces aufgeteilt") — und wer die Begründung nachvollziehen will, sucht
eine Methode, die es nicht gibt.

**Kleinster Schnitt:** zwei Zeilen.
**Aufwand:** zwei Minuten.

**Stand:** offen

### 7.8 Kleinkram

| Stelle | Was |
|---|---|
| `ConfigFileSource.java:21` | auskommentiertes Feld `computedProps` — die computed-Pfade liegen inzwischen in derselben Map. |
| `Activity.java:52-62` | Ein Problem, dreimal gemeldet: `Log.error` mit Text, `Log.error` mit dem serialisierten Objekt, dann `throw` mit demselben Text. Dazwischen ein leeres `catch (Exception e) {}` ohne Kommentar — der einzige kommentarlose im Anwendungscode. |
| `UiUtils.java:55` | Parametertyp voll qualifiziert (`javafx.scene.paint.Color`), obwohl `Color` importiert ist. |
| `SingleInstanceGuard.java:9` | `@SuppressWarnings("resource")` — hier zu Recht (Channel und Lock leben bis zum Shutdown-Hook), aber ohne den Satz, der das sagt. |

**Aufwand:** zusammen eine Viertelstunde.

**Stand:** offen

### Was in dieser Gruppe trägt

- **Die Config-Fassade mit ihren zwei versteckten Stores.** Eine Tür, zwei Partitionen, die
  Typumwandlung genau einmal, Schreiben auf einen unveränderlichen Key wirft, und beim Start eine
  Kollisionsprüfung über den gemeinsamen Namensraum. Dazu das Javadoc über `init` (`:30-40`), das
  erklärt, warum `DB` seine Pfade als Parameter bekommt und was passiert, wenn jemand das
  „vereinfacht": ein Konstruktionszyklus, der klaglos kompiliert. Das ist die Sorte Wissen, die
  nach einem Jahr Pause nicht mehr rekonstruierbar wäre.
- **Der FailFast-Grundsatz hält wirklich.** In 27.700 Zeilen fangen 23 `catch`-Blöcke einen
  Fehler ab, ohne weiterzuwerfen — und fast alle davon sind namentlich begründet: die fünf
  Start-Importe im `Controller` mit einem eigenen Javadoc-Absatz, das Fenster vor der
  Log-Initialisierung in `ThosSuiteApp`, die Poster-Aufräumung nach einem Rollback, die
  Query-Validierung im Tagebuch. Was nicht begründet ist, steht als Befund 3.5, 4.8 und 6.3 in
  diesem Bericht. Bei einer Regel, die niemand prüft, ist das ein beachtlicher Wert.
- **`ShapeGeometry` benennt seine eigene Naht** (`:18-42`): dass `type` zwei unabhängige
  Bedeutungen trägt, wer welche ableitet, warum eine Aufspaltung denkbar und trotzdem
  zurückgestellt ist, und was passieren würde, wenn man sie in einem Nebenobjekt versteckte.
  Endet mit „Das ist die Naht. Nicht überrascht sein." Genau so sollte eine bewusst offene Stelle
  aussehen — der Unterschied zu einem Befund ist, dass hier jemand hingeschaut hat.
- **`Log` begründet die Wahl von JUL** (`:25-42`) und dokumentiert die
  WeakReference-Falle samt `KEEP_ALIVE_LOGGERS` in einer Ausführlichkeit, die man nur einmal
  schreibt und dann nie wieder herleiten muss.
- **`ButtonEnum` — geprüft und für richtig befunden.** Die Liste enthält mit „Ganze Staffel",
  „Andere Richtung genommen" und den Ampelfarben unübersehbar Feature-Vokabular im Fundament, und
  das sieht zunächst wie ein Regelverstoß aus. Es gibt aber keinen billigeren Entwurf: Die Rolle
  des Enums ist die *Identität* der Antwort, die der Aufrufer zurückbekommt, und die kann kein
  durchgereichter String leisten. Der Preis — jeder neue Dialogknopf eines Features ändert eine
  Datei in `shared.model` — ist niedriger als jede Alternative. Nicht anfassen.
- **`DB` erklärt die SQLITE_BUSY-Regel dort, wo man sie braucht**, samt der begründeten Ausnahme
  für das PRAGMA-Statement. Dass diese Regel in allen 19 Methoden von `MessageRepository`
  eingehalten wird (Gruppe 4), ist die Folge davon.

---

## 8 · shared/ui — Oberflächenrahmen

Gelesen: `app.shared.ui` (Wurzel) — 23 Dateien, rund 2.820 Zeilen.

### 8.1 Regel 6 beschreibt nicht den Code, und der Architekturtest sagt das bereits

**Beleg:** `Design-Regeln.md:58` — „**Null-Layout:** keine LayoutManager, feste Positionen"; dem
gegenüber benutzen **15 der 23 Klassen** dieses Pakets `VBox`, `HBox`, `FlowPane`, `BorderPane`
oder `GridPane` — darunter `Alerts`, `DiaryScreenView`, `MovieViewerScreenView`,
`BarChartScreenView`, `DashboardScreenView` und jeder einzelne Dialog.

Das ist kein Schlendrian, sondern die richtige Bauweise: Ein Dialog mit drei Feldern übereinander
braucht keine Rechtecke aus dem Skin. Das Regeldokument weiß das selbst — 286 Zeilen weiter unten
steht der passende Satz: „**Discriminator: wer positioniert** — eine Layout-Pane (VBox/HBox)
selbst → sie ist der View, kein Host; werden die Bausteine mit Rechtecken aus dem Skin gesetzt →
`ComponentHost`." Die beiden Stellen widersprechen sich.

Aufgefallen ist es schon: Der Architekturtest verzichtet ausdrücklich auf eine Regel dafür und
begründet es im Schlusskommentar — „keine LayoutManager" hieße ein Verbot von VBox/HBox, und die
kämen legitim vor. Es wurde also gesehen, im Test vermerkt und im Regeldokument nicht nachgezogen.

**Was es kostet:** Regel 6 steht unter den acht festen Regeln, die man vor dem Bauen liest. Wer
einen neuen Dialog anlegt und sie wörtlich nimmt, positioniert Felder absolut — und baut damit
etwas, das kein anderer Dialog der Suite tut.

**Welche Seite ich für die bessere halte:** die des Codes. Die Null-Layout-Regel gilt für die
Lern-Oberflächen, weil dort der Skin die Rechtecke besitzt; für alles andere ist sie eine
Behauptung ohne Nutzen.
**Kleinster Schnitt:** Regel 6 auf den Discriminator umschreiben, der weiter unten schon steht:
Lern-Oberflächen und alles, was über `ComponentHost` läuft, sind Null-Layout; Dialoge und
Listen-/Diagramm-Screens layouten selbst.
**Aufwand:** eine Viertelstunde Doku.

**Stand:** offen

### 8.2 Der Erweiterungsvertrag von `AnkiLearnView` ist an drei Stellen überholt

`AnkiLearnView` ist die einzige Klasse der Suite mit einem ausdrücklichen „Achtung beim
Erweitern"-Abschnitt — und genau dieser Abschnitt ist die Stelle, an der man beim fünften
Lernformat nachschlägt. Drei Dinge stimmen nicht mehr:

**a) Drei statt vier Unterklassen.** `AnkiLearnView.java:30-35` zählt `ShapeMapLearnView`,
`McLearnView` und `ImageMapLearnView` auf und nennt sie „die drei Unterklassen".
`FastWriteLearnView` fehlt, obwohl es seit dem Fast-Write-Feature dazugehört und in
`Feature-Details.md` korrekt als vierte geführt wird.

**b) Ein Übergangszustand, der vorbei ist.** `:53-55` sagt, „nur noch die Antwortauswahl kommt
über eine Bau-Methode des Skins (`createMultipleChoicePane`)". Diese Methode gibt es im ganzen
Projekt nicht mehr; Zeile 122 baut die Pane selbst und holt sich Rechteck und Maße über die
regulären Skin-Zugänge. Der Umbau ist also fertig, und der Hinweis behauptet das Gegenteil.

**c) Zwei Antworten auf „hat diese Lernform eine Antwortauswahl?"** Es gibt den Schalter
`hasMcPane()` (`:91`), der `rebuild()` steuert — und zusätzlich überschreiben `McLearnView` und
`FastWriteLearnView` jeweils `disableMcPanel()` leer. Denn die Basisklasse greift in `:195` und
`:209` ungeprüft auf `mcPane` zu, während sie in `:205`, `:206`, `:213` und `:214` sauber gegen
`null` prüft. Eine künftige fünfte Lernform ohne Auswahl, die `hasMcPane()` auf `false` setzt und
die Überschreibung vergisst, bekommt eine NPE — und der Erweiterungs-Abschnitt erwähnt diese
zweite Pflicht nicht.

**Kleinster Schnitt:** (a) und (b) sind Textänderungen. Für (c): in `:195` und `:209` gegen
`null` prüfen wie in den vier Nachbarmethoden; die zwei leeren Überschreibungen entfallen dann,
und `hasMcPane()` ist die einzige Wahrheit. Der Kommentar in `McLearnView.java:23` („darf in
einer MC-Session niemals deaktiviert werden") bleibt als Sonderfall erhalten — dort ist
`hasMcPane()` ja `true`.
**Aufwand:** eine halbe Stunde.

**Stand:** offen

### 8.3 Die Thumbnail-Höhe steht in beiden Hälften des Tagebuch-Splits

**Beleg:** `DiaryEditor.java:59` und `:208` gegen `DiaryEditorPresenter.java:41` und `:175` —
beide Klassen halten ein eigenes `DEFAULT_THUMBNAIL_HEIGHT = 120` und lesen damit
`Config.getInt("diary.thumbnailHeight", …)`

Der Presenter **erzeugt** das Thumbnail in dieser Höhe, der Editor **zeigt** es in dieser Höhe.
Die beiden müssen sich einig sein, und nichts im Code verbindet sie: Zwei private Konstanten
gleichen Namens in zwei Paketen, die zufällig dieselbe Zahl tragen.

Der Fall ist auch deshalb bemerkenswert, weil das Regelwerk die Diary-Aufteilung ausdrücklich
beschreibt („Sonderfall: zustandsbehafteter Editor … Fachregeln, die das Verhalten steuern
(Schwellen, Timer-Dauer), kommen als framework-freie Werte hinein"). Genau das passiert bei den
Invasiv-Schwellen (`InvasiveConfig`) — und bei der Thumbnail-Höhe nicht.

**Kleinster Schnitt:** Die Höhe wie die Invasiv-Werte behandeln: Der Presenter liest sie und gibt
sie dem Editor mit. Dann steht die Zahl einmal und der Vertrag ist sichtbar.
**Aufwand:** eine Viertelstunde.

**Stand:** offen

### 8.4 „Die einzige Stelle der Suite, die `ButtonType` kennt" — das sind 14 Stellen

**Beleg:** `Alerts.java:42-44`; tatsächlich taucht `ButtonType` in 14 Dateien auf, darunter
`DiaryEditor`, `AnkiConfigDialog`, `RegionConfigDialog`, `TextPromptDialog`,
`MessageContactDialog`, `WhatsAppChatDialog`, `ImageComparisonDialog`, `DatePickerDialog`,
`ActivityTableDialog`, `SuiteIconButton`, `SkinProperties` und `ThosSuiteApp`

Der **Entwurf** ist in Ordnung: Die bespoke-Dialoge dürfen laut Regelwerk eigene Knöpfe bauen,
und keiner von ihnen gibt einen `ButtonType` nach außen — sie liefern alle ein Record, ein Enum
oder `null`. Falsch ist nur der Satz. Und er steht an prominenter Stelle: Wer einen neuen Dialog
baut und ihn liest, hält `ButtonType` für verboten und sucht einen Weg, der nicht existiert.

**Kleinster Schnitt:** „Die einzige Stelle, die zwischen `ButtonEnum` und `ButtonType`
**übersetzt**" — plus einen Halbsatz, dass bespoke-Dialoge ihre Knöpfe selbst bauen und ihr
Ergebnis als Record zurückgeben.
**Aufwand:** fünf Minuten.

**Stand:** offen

### 8.5 Ein bekannter Mangel steht als Fließtext statt als Marker

**Beleg:** `Alerts.java:55-59` — „Das ist sehr speziell für den Matratze wenden Dialog gebaut und
gehört verbessert, wenn auch andere Bilder angezeigt werden sollen."

Die Aussage ist richtig und nützlich: `buildContent` färbt jedes übergebene Bild mit der
Textfarbe ein (`:160`), was nur für monochrome Symbole sinnvoll ist. Nur steht sie im laufenden
Text eines `@param`-losen Javadoc-Blocks und taucht damit in keiner Marker-Übersicht auf. Die
Suite hat für genau diesen Fall eine Konvention.

**Kleinster Schnitt:** als `// !Später:` formulieren, dann steht sie bei den anderen offenen
Punkten.
**Aufwand:** zwei Minuten.

**Stand:** offen

### Was in dieser Gruppe trägt

- **`Alerts` als einzige Übersetzung zwischen `ButtonEnum` und `ButtonType`.** Der Aufrufer nennt
  Knöpfe fachlich, bekommt fachlich zurück, und „Dismiss und X bedeuten immer CANCEL" ist eine
  Zusage, auf die sich acht Features verlassen. Die `ButtonData`-Zuordnung in `:182-188` ist ein
  vollständiger `switch` über das Enum — ein neuer Knopf ist ein Übersetzungsfehler, kein
  Laufzeitproblem. Genau das, was Befund 6.1 im Statistik-Menü fehlt.
- **Der JavaFX-26-Rundungsfehler ist dokumentiert, nicht nur umgangen** (`Alerts.java:126-130`):
  was schiefgeht, warum die ganzzahlige Labelbreite hilft, dass es an openjfx-dev gemeldet ist
  und wo Messwerte und Reproducer liegen. Ohne diese fünf Zeilen wäre die überschriebene
  `computePrefWidth` in einem Jahr ein unerklärlicher Hack, den jemand „aufräumt".
- **`ComponentHost` kennt nichts.** Kein Feature, kein Deck, kein Layout — eine reine `Pane`
  (ausdrücklich keine `StackPane`, mit Begründung), in die absolut positionierte Bausteine
  gehängt werden. Vier Methoden. Dass alle vier Lern-Oberflächen und beide Zweige damit
  auskommen, ist das Ergebnis.
- **Die Aufteilung der Lern-Oberflächen entlang dessen, was sie *sind*.** Die vier Unterklassen
  unterscheiden sich in genau drei Entscheidungen (`createMap`, `hasInputField`, `hasMcPane`) und
  sind zwischen 24 und 96 Zeilen lang. Die Benennung nach der Bauart statt nach dem Deck ist im
  Javadoc von `McLearnView` sogar gegen den naheliegenden Einwand verteidigt — mit dem richtigen
  Argument.
- **`RegionConfigDialog` nimmt den Reducer als Parameter entgegen.** Die Verflechtung (welche
  Modi verschwinden, welche Decks ausgrauen) bleibt im Feature, der Dialog kennt nur
  `Zustand → Zustand`. Damit ist der komplizierteste Dialog der Suite der mit dem schmalsten
  Wissen.
- **Die Feature-Freiheit hält.** Keine Klasse dieses Pakets importiert ein Feature-Paket; was
  feature-gebunden ist, steht im Klassennamen (`DiaryScreenView`, `MovieViewerScreenView`,
  `WhatsAppChatDialog`) und nicht im Import.

---

## 9 · UI-Bausteine

Gelesen: `app.shared.ui.components` und `app.shared.ui.components.map` — 28 Dateien, rund 3.660
Zeilen.

Diese Gruppe ist die regelkonformste der Suite. Die vier Zusagen, die das Regelwerk für Bausteine
macht, habe ich einzeln nachgeprüft; alle vier halten (siehe „Was in dieser Gruppe trägt"). Was
bleibt, ist wenig und klein.

### 9.1 `SuiteImage` reicht zwei Innen-Nodes nach außen — und niemand nimmt sie

**Beleg:** `SuiteImage.java:329` und `:332` — `public Rectangle getBackgroundRect()` und
`public Rectangle getBorderRect()`; kein Aufrufer im gesamten Projekt, auch nicht in der Klasse
selbst

Ein Baustein ist laut Regelwerk das, was eingebaut wird — er hält sein Innenleben. Diese zwei
Getter geben veränderliche JavaFX-Nodes aus dem eigenen Szenengraphen heraus; wer sie nimmt, kann
Größe, Füllung und Stil des Bilderrahmens von außen verändern, an der Klasse vorbei. Dass es
niemand tut, ist Glück, nicht Konstruktion.

**Kleinster Schnitt:** beide löschen. Zwei Zeilen.
**Aufwand:** zwei Minuten.

**Stand:** offen

### 9.2 `ImageMapPane` bietet zwei Vokabulare an, von denen eines nur nach innen zeigt

**Beleg:** `ImageMapPane.java:350` (`public void addToCorrect(List<ShapeGeometry>)`), `:359`
(`public void setMarked(List<ShapeGeometry>)`), `:368` (`public void markLastClickAsIncorrect()`)
— alle drei werden ausschließlich aus dieser Klasse gerufen, aus den `LearnMap`-Methoden in
`:310-313` und `:330-333`

Der ganze Sinn von `LearnMap` steht als Überschrift darüber: „Das gemeinsame Vokabular — spricht
Ids, übersetzt nach Geometrien". Die Übersetzung passiert genau hier, in vier Einzeilern. Die drei
Methoden dahinter sind die andere Seite der Übersetzung und damit Innenleben — öffentlich bieten
sie einen zweiten Zugang an, der mit Geometrien statt mit Ids spricht und den Vertrag umgeht.

**Kleinster Schnitt:** `private`. Drei Schlüsselwörter, kein Aufrufer betroffen.
**Aufwand:** zwei Minuten.

Im selben Zug: `SuiteInfoLabel.setFixedWidth()` und `setFixedHeight()` (`:102`, `:109`) werden
nur aus dem eigenen Konstruktor gerufen (`:52-53`) und sind ebenfalls `public`.

**Stand:** offen

### 9.3 Die Thumbnail-Höhe steht ein drittes Mal — Erweiterung zu Befund 8.3

**Beleg:** `DiaryCard.java:48` — `Config.getInt("diary.thumbnailHeight", 120)`, diesmal ohne
benannte Konstante

Damit steht dieselbe Zahl in drei Dateien aus drei Paketen: `DiaryEditorPresenter` **erzeugt** das
Thumbnail in dieser Höhe, `DiaryEditor` **zeigt** es im Editor, `DiaryCard` **zeigt** es in der
Trefferliste. Drei Vorgabewerte, die übereinstimmen müssen, und nichts verbindet sie.

`DiaryCard` ist dabei der Fall, der am wenigsten wehtut und am meisten verrät: Ein Baustein liest
einen Config-Wert, obwohl das Regelwerk sagt, dass er sich holt, was für jede Verwendung gleich
ist, und übergeben bekommt, was von der Verwendung abhängt. Die Thumbnail-Höhe hängt davon ab,
womit sie erzeugt wurde — sie ist also Kontext und gehört in den Konstruktor, wie `DiaryCardData`
selbst auch.

**Kleinster Schnitt:** wie in 8.3 — der Presenter liest die Höhe einmal und reicht sie weiter;
`DiaryCard` bekommt sie als Parameter.
**Aufwand:** zusammen mit 8.3 eine Viertelstunde.

**Stand:** offen

### 9.4 Ein Rückblick zu viel — und zwei, die bleiben dürfen

Ich habe das Paket auf Regel 7 durchsucht („Kommentare beschreiben, was ist — nie, was war") und
drei Kandidaten gefunden. Nur einer ist wirklich einer:

- **`ImageMapPane.java:71`** — `// Radius des Falsch-Klick-Markers (früher CircleSizes.SMALL).`
  Die Klammer sagt nichts über heute. Schlimmer: Sie nennt einen Typ aus `app.learn.model`, also
  ein Feature — und erzeugt damit im Kopf des Lesers eine Abhängigkeit, die es im Code (zu Recht)
  nicht gibt. **Löschen.**
- **`ShapeLayer.java:11`** — „früher fielen sie im überladenen `isInteractive` zusammen." Das ist
  kein Rückblick, sondern eine Warnung: Es sagt, warum die zwei Ableitungen aus demselben `type`
  getrennt bleiben müssen. Genau der Fall, den das Regelwerk als „verworfene Alternative mit
  Begründung" ausdrücklich erlaubt. **Bleibt.**
- **`MapNodeBuilder.java:19,23`** — „vorher verstreut über GeoJsonLoader, ShapeMap-Konstruktor und
  GeoMap.createCircle" und „Die frühere id/styleClass-Asymmetrie auf der Shape-Seite ist weg."
  Der erste Halbsatz nennt Klassen, die es noch gibt, die aber nichts mehr damit zu tun haben —
  wer nachschaut, sucht umsonst. Der zweite hat keinen Gegenwartsinhalt. Der tragende Teil des
  Absatzes („die **einzige** Stelle, an der Karten-Nodes entstehen") steht davor und bleibt.
  **Die zwei Rückblicke streichen.**

**Aufwand:** fünf Minuten.

**Stand:** offen

### 9.5 Kleinkram

| Stelle | Was |
|---|---|
| `ImageMapPane.java:302-308` | Zwei Abschnitts-Banner hintereinander, das erste („Shapes (Geometrien mit id, von der learn-Seite)") ohne Inhalt darunter. |
| `SuiteImage.java:195` | `Config.getPath("imageFolder").resolve("svg")` bzw. `learnImageFolder` — korrekt nach der Pfad-Regel, aber die Fallunterscheidung SVG/Raster entscheidet hier über zwei *verschiedene* Config-Schlüssel. Beim Lesen erwartet man einen Ordner und findet eine Weiche. Ein Kommentar von einer Zeile würde reichen. |
| `MovieCard.java:85` | Der Platzhalter-Dateiname `None_available_en-US_154_231.jpg` steht als Literal — nach der Pfad-Regel richtig (Dateinamen gehören der Aufrufstelle), aber die Zahlen darin sind die Bildmaße und damit stumm an die 154er-Variante gekoppelt. |

**Aufwand:** zehn Minuten.

**Stand:** offen

### Was in dieser Gruppe trägt

Vier Zusagen aus dem Regelwerk, alle nachgeprüft:

- **Die Schlüssel-Regel hält ausnahmslos.** Sie lautet: Ein Baustein holt sich beim Skin, was für
  jede Verwendung gleich ist; was von der Verwendung abhängt, bekommt er übergeben — und der Test
  steht in der Signatur des Skin-Zugangs. In 28 Dateien gibt es genau sechs Skin-Zugriffe:
  `bigComponentStyle()`, `sketchStrokeWidth()`, `shapeMapStrokeReserve()`, `popupMonitorMargin()`
  und `iconFor(rolle)`. Kein einziger nimmt Deck-Id, Kartenname oder Kategorie entgegen. Das
  Auflösen bleibt vollständig in `shared.ui` — wo es hingehört. Die Regel behauptet
  „ausnahmslos", und sie stimmt.
- **Bausteine erben, sie verhüllen nicht.** Jede Komponente des Pakets erbt von ihrem
  JavaFX-Typ. Die drei Ausnahmen sind genau die im Regelwerk benannten: `SuiteBackground` als
  Fabrik (JavaFX-Typ ist `final`), `DiaryTagInputComponent` als Fabrik zweier getrennter
  Bausteine, `ButtonTextFit` als paketprivater Helfer.
- **Kein Baustein kennt ein Feature, keiner greift in `shared.ui` zurück.** Beides geprüft: null
  Importe in beide Richtungen. Die beiden Architekturregeln dazu (Wächter 3 und seine
  Gegenrichtung) haben hier nichts zu tun.
- **Der Kartencluster ist sauber geschnitten.** `MapNodeBuilder` ist die *einzige* Stelle, an der
  aus `ShapeGeometry` ein Node wird; `ShapeLayer` ist paketprivat und leitet nur Darstellung ab;
  `LearnMap` spricht durchgehend Ids, und `EmptyLearnMap` ist ein echtes Nullobjekt statt einer
  Sonderbehandlung in den Aufrufern. Dass die MC-Session „eine Karte hat, die nichts tut", ist
  der Grund, warum `AnkiLearnView` keine einzige Fallunterscheidung über Kartenarten enthält.
- **Zwei Invarianten sind an der Stelle aufgeschrieben, an der sie brechen könnten**
  (`ImageMapPane.java:315-328`): warum der Falsch-Klick-Marker nie unter einer aufgedeckten Form
  liegen kann und warum `lastClick` immer frisch ist — beides folgt daraus, dass klickbar nur
  `mandatory ∪ optional` ist. Mit dem Schlusssatz „Wer die Klickziele weiter fasst, holt sich
  beide Fehler ein." Das ist die teuerste Art von Wissen, und sie steht da, wo man sie braucht.
- **`SuiteInfoLabel` mit eigenem Mini-Parser** statt einer Browser-Engine für drei Tags: Das ist
  die Sorte Entscheidung, die man normalerweise bereut — hier trägt sie, weil der Umfang (`<br/>`,
  `<b>`, `<i>`, weiche Trennung) fest ist und nicht wachsen soll.

---

## 10 · Skin

Gelesen: `app.shared.skin` — 14 Dateien, rund 2.790 Zeilen. Dazu als Maßstab
`docs/skin/Skin-Felder.md`, das die Mechanik dieses Pakets beschreibt.

### 10.1 Zwei neue Felder sind in eine Falle gelaufen, die schon aufgeschrieben war

**Beleg:** `SkinProperties.java:108` (`protected Double sketchStrokeWidth = 1.5;`) und `:112`
(`protected Double sketchMarkedHatchWidth = 4.0;`) gegen `SkinProperties.java:633-645` — die
if/else-Kette des Loaders kennt `Color`, `Font`, `BorderParams`, `Integer`/`int`, `Rectangle2D`
und `String`, **keinen** `Double`-Zweig und **kein** abschließendes `else`

`docs/skin/Skin-Felder.md:94-101` beschreibt genau diese Falle, vollständig und richtig:

> **Kommazahlen gibt es nicht.** … Die beiden Felder dieses Typs, `shapeMapStandardBorderWidth`
> und `shapeMapFederalStateBorderWidth`, lassen sich aus einer properties-Datei deshalb **gar
> nicht setzen**: Der Schlüssel käme durch die FailFast-Prüfung, weil das Feld existiert, und der
> Wert würde danach stillschweigend verschluckt. … Ein neues Feld für einen Bruchteil wird deshalb
> als **Prozentzahl** angelegt, nicht als Kommazahl.

Es sind inzwischen **vier** Felder, nicht zwei. Die zwei neuen kamen mit den Skizzen dazu, und sie
sind der unangenehmere Fall: Über `sketchStrokeWidth` steht im Code ausdrücklich, dass „Strich und
Markierung … zum Skin gehören" — die Absicht war also, dass ein Skin sie setzt. Er kann es nicht.
Schreibt jemand `sketchStrokeWidth=2.5` in eine properties-Datei, läuft der Start durch (das Feld
existiert, `checkKeysHaveFields` ist zufrieden), und der Wert wird verworfen. Die Strichbreite
bleibt 1,5, ohne eine einzige Meldung.

Das ist exakt die Fehlerart, gegen die `checkKeysHaveFields` gebaut wurde — sein eigenes Javadoc
erzählt, dass `borderBackButton` und `inactiveButtonBgColor` „über Jahre ins Leere" liefen. Die
Prüfung schließt eine Richtung (Schlüssel ohne Feld). Die andere (Feld mit unbekanntem Typ) ist
offen geblieben, und seitdem sind zwei weitere Felder hineingefallen.

**Kleinster Schnitt:** Ein `else { throw new RuntimeException("Feldtyp ohne Loader-Zweig: " +
field.getName() + " (" + field.getType() + ")"); }` an das Ende der Kette (`:645`). Danach
brechen `contentSize` und die vier `Double`-Felder den Start — also ergänzt man im selben Zug
einen `Double`-Zweig (zwei Zeilen) und lässt `contentSize` als benannte Ausnahme durch, so wie es
sein Javadoc in `:308-310` ohnehin beschreibt. Danach kann die Klasse Kommazahlen, und die Regel
„Bruchteile als Prozentzahl" wird zur Wahl statt zur Notlösung.
**Aufwand:** eine halbe Stunde, plus das Nachziehen der Stelle in `Skin-Felder.md`.

**Offene Frage:** Ob eine der ausgelieferten properties-Dateien heute schon einen dieser vier
Schlüssel setzt, ließ sich hier nicht feststellen — die Dateien liegen im Datenordner, nicht im
Repo. Falls ja, weicht das sichtbare Ergebnis seit dem jeweiligen Eintrag von dem ab, was dort
steht.

**Stand:** offen

### 10.2 Die Beschreibung der Staffelung stimmt in drei Punkten nicht mehr

**Beleg:** `Architektur-Dokumentation.md:336-346` gegen `SkinProperties.java:369-380`
(`cascadingValue`) und `:349-356`, `:420-450`, `:459-475`

1. **Der Methodenname.** Die Doku nennt `SkinProperties.staffelung(…)`; die Methode heißt
   `cascadingValue`. Wer sie sucht, findet sie nicht.
2. **Die Zahl der Stufen.** Der Kasten in der Doku zeigt zwei Stufen plus `null`:
   „spezifisch (mapName des Decks)" → „Kategorie" → „sonst null". Der Code hat **drei**:
   `deckId` → `mapName` → `category`. Die Deck-Stufe fehlt in der Doku ganz — und sie ist die
   feinste, also die, die man beim Feinschliff eines einzelnen Decks braucht. Das Javadoc von
   `cascadingValue` beschreibt sie dagegen genau richtig („Die vier Berlin-Decks tragen denselben
   mapName … die vierzehn Landkreis-Decks lassen auch den offen").
3. **Der Satz zu den Wallpapern.** „Dieselbe Staffelung gilt für die Hintergrundbilder, dort mit
   einer **dritten** Stufe" ist die Folge des Fehlers aus Punkt 2. Die Wallpaper haben keine Stufe
   mehr als die Maße; sie haben denselben Dreischritt und statt `null` einen Vorgabewert
   (`emptyWallpaperName`, `SkinProperties.java:448-453`).

**Was es kostet:** Das ist der Abschnitt, den man liest, bevor man ein Deck anlegt oder zwei Decks
ein Layout teilen lässt. Er beschreibt ein System mit zwei Stufen, wo es drei gibt.

**Welche Seite die bessere ist:** die des Codes — drei Stufen sind genau richtig, und das Javadoc
an der Methode erklärt sie gut. Nachzuziehen ist die Architekturdoku.
**Kleinster Schnitt:** den Kasten auf drei Zeilen erweitern, den Methodennamen korrigieren, den
Wallpaper-Satz auf „derselbe Dreischritt, statt `null` das leere Wallpaper" ändern.
**Aufwand:** zehn Minuten.

**Stand:** offen

### 10.3 Kleinkram

| Stelle | Was |
|---|---|
| `SkinService.java:64` | `throw new RuntimeException("What the heck?")` — der Wächter fängt den Fall ab, dass der aktuelle Skin nicht in der Liste steht. Die Meldung sagt dem, der sie im Alert liest, nichts; der umschließende catch ergänzt zwar „Fehler beim Reload des Skins: X", aber die Ursache bleibt inhaltsleer. |
| `SkinService.java:58` | `// NEU: Die Methode für den Refresh` — „neu" ist ein Zustand von einst; Regel 7. |
| `Skin.java:1347` | Ein Kommentar im Skin verweist auf `createCard`, eine Methode in `MovieCard` (`shared.ui.components`). Inhaltlich harmlos, aber es ist die einzige Stelle, an der der Skin auf die Anzeige-Schicht zeigt — und genau das bewacht Wächter 1. Im Bytecode fällt ein Kommentar nicht auf. |

**Aufwand:** zehn Minuten.

**Stand:** offen

### Was in dieser Gruppe trägt

- **Der Skin baut wirklich nichts.** Geprüft: Keine einzige Komponenten-Konstruktion in `Skin`
  oder `SkinProperties`. Nach außen gehen genau zwei Methoden (`styleScene`, `buildCss`) plus
  zweckgeschnittene Records. Die Umkehrung, die im Architekturdokument als solche beschrieben
  ist („das ist die Umkehrung des früheren Zustands … und diese Regel ist per Build prüfbar, die
  alte war es nicht"), ist vollständig durchgezogen — und ArchUnit bewacht sie.
- **Kein Property-Name verlässt das Paket.** 91 `Rectangle2D`-, 45 `Color`- und 34
  `String`-Felder, und nach außen gehen `DialogStyle`, `MovieStyle`, `McMetrics`,
  `BigComponentStyle`, `MapImages`, `DashboardTileStyle` — sechs zugeschnittene Records. Das
  Javadoc von `bigComponentStyle()` sagt sogar, warum nicht das ganze `borderBigComponent`
  hinausgeht: „das Feld hat sieben Komponenten und trägt einen Namen, der im skin-Paket bleiben
  soll."
- **`checkKeysHaveFields` ist ein Wächter mit Vorgeschichte.** Er läuft vor der Befüllung, damit
  eine kaputte Datei nicht halb angewandt wird, und je Datei statt je Skin, damit ein abgeleiteter
  Skin beide Dateien für sich prüft. Sein Javadoc erzählt, welches konkrete Problem ihn ausgelöst
  hat. Befund 10.1 ist die Aufforderung, ihn zu Ende zu bauen, nicht Kritik an ihm.
- **Die Kaskade löst der *Aufrufer* auf, nicht der Baustein.** `learnComponentBounds(deckId,
  mapName, category, teil)` nimmt den Schlüssel entgegen — und wer ihn liefern muss, ist laut
  Schlüssel-Regel die Ansicht. Das Javadoc von `mcMetrics` formuliert es selbst: „Deckabhängiges
  löst die Ansicht auf und reicht das Ergebnis hinein … Die Komponenten unter
  `shared.ui.components` wissen deshalb weiterhin nicht, was ein Deck ist." In Gruppe 9 ist
  nachgeprüft, dass das stimmt.
- **`SkinImageCache` cacht nur für den aktiven Skin und leert sich beim Wechsel selbst.** Damit
  ist die eine Stelle, an der große Bilder von der Platte kommen, auch die eine, die beim
  Skinwechsel aufräumen muss — und niemand sonst muss daran denken.
- **`docs/skin/Skin-Felder.md` und `Skin-Matrix.xlsx` mit ihrer Trennregel** („Was im Stylesheet
  landet, steht in der Tabelle. Was nicht ins CSS geht, steht hier. Jedes Feld erscheint mit
  seiner Vorgabe in genau einem der beiden") — inklusive der benannten Kante, an der die Regel
  nicht aufgeht. Dass Befund 10.1 überhaupt so genau zu beschreiben war, liegt an diesem Dokument.

---

## 11 · Änderungsszenarien

Drei Änderungen durch den Code verfolgt. Maßstab ist nicht, wie viele Zeilen entstehen, sondern
wie viele **Dateien über wie viele Schichten** angefasst werden müssen — und ob das Vergessen
einer davon auffällt.

### Szenario A · Ein weiteres Anki-Deck

Angenommen, es kommt ein Deck „Chemie" dazu, mit Multiple-Choice-Fragen und ohne Karte.

| Was | Wo | Fällt ein Vergessen auf? |
|---|---|---|
| Enum-Eintrag | `Deck.java` | — (der Anfang) |
| View-Zweig | `learn/anki/SessionPresenter.java:66-77` | **nein**, siehe unten |
| CSV-Datei | `deckFolder` (Datenordner) | ja, `CsvDeckCardSource` wirft beim Start |
| Config-Schlüssel `newChemieCardsPerDay` | config.txt (Datenordner) | ja, `Config.get` wirft beim Start |
| GeoJSON + `MapMetadata`-Eintrag | nur bei einem Deck **mit** Karte | ja, `MapRepository` wirft |
| Skin-Werte | nichts nötig | — |

**Das ist billig, und zwar aus einem nachprüfbaren Grund:**
`SessionPresenter.createViewFor` ist die **einzige Stelle der gesamten Suite**, die auf konkrete
Anki-Deck-Konstanten schaltet. Ich habe danach gesucht: außerhalb des Enums selbst gibt es keine
zweite. Alles andere läuft über `Deck.values()` mit Kategorie-Filter — `AnkiDeckService`,
`CsvDeckCardSource`, `Controller.setPlayMenuItemLabels`, `RegionPlaySetup.buildDeckColumns`.
Die Skin-Kaskade fällt automatisch auf die Kategorie zurück, die DB legt ihre Zeilen beim ersten
Speichern selbst an, und das Menü baut sich aus der Aufzählung.

**Zwei Dateien plus Daten** — das ist der beste Wert der drei Szenarien, und er ist verdient.

**Der eine harte Punkt:** Die Zeile, die man vergisst, ist die einzige, die nicht meckert.
`createViewFor` endet mit `default -> null` (Befund 1.1). Alle anderen fehlenden Teile werfen beim
Start; dieser eine führt zu einer NPE mitten in der ersten Karte. Genau deshalb steht 1.1 in der
Liste der wichtigsten Befunde — der Aufwand ist eine Minute, der Gewinn betrifft jede künftige
Deck-Erweiterung.

**Der weiche Punkt:** Der Enum-Eintrag hat acht Spalten, von denen für ein MC-Deck zwei `null`
sind (Befund 1.6). Beim Abschreiben einer bestehenden Zeile nimmt man die `null` mit, ohne zu
wissen, welche man setzen dürfte.

### Szenario B · Ein weiterer Screen

Angenommen, es kommt ein Screen „Ausgaben" dazu, mit eigenem Menüpunkt unter *Module*.

Die Feature-Seite ist geradeaus — neues Paket `app.spending` mit `repository` und `model`, eine
Klasse, die `Screen` implementiert, dazu eine `…ScreenView` in `shared.ui`. Wenn es ein
Balkendiagramm werden soll, entfällt sogar die View: `BarChartScreenView` nimmt einen
`BarChartDataProvider` entgegen, und `alc` wie `fitbit` zeigen zweimal vor, wie wenig dafür nötig
ist. Das ist gut gebaut.

**Teuer ist die Verdrahtung ans Menü.** Für *einen* Eintrag sind es fünf Stellen in zwei Klassen:

```
MainWindow   private Runnable onSpendingSelected = null;      // Feld
MainWindow   public void setSpendingRunnable(Runnable r)      // Setter
MainWindow   MenuItem + setOnAction in buildMenuBar()         // zwei Zeilen
Controller   mainWindow.setSpendingRunnable(this::spending…)  // Registrierung
Controller   public void spendingSelected() { … }             // Methode
```

Das steht heute **achtzehnmal** so da: `MainWindow` hält 17 Callback-Felder mit 17 Settern, der
Controller-Konstruktor besteht aus 18 Registrierungszeilen am Stück (`Controller.java:89-106`).
Jede einzelne ist trivial, und keine fällt auf, wenn sie fehlt — ein nicht registriertes Callback
ist `null`, und der Menüpunkt wirft beim Klick eine NPE.

**Und wenn der Screen unter *Statistik* landet**, ist es schlimmer: Dort läuft die Zuordnung über
den **Anzeigetext** (`Befund 6.1`). Ein neuer Eintrag bedeutet eine MenuItem-Zeile plus einen
`else if ("Ausgaben".equals(item))`-Zweig — und wer den Zweig vergisst, bekommt keinen Fehler,
sondern einen geschlossenen und wieder angezeigten alten Screen.

**Kleinster Schnitt:** Ein `enum MenuAction` mit Anzeigename, ein einziges
`Consumer<MenuAction>`, und im Controller ein `switch` über das Enum. `MainWindow` verliert 17
Felder und 17 Setter, der Controller 18 Registrierungszeilen, und ein vergessener Zweig wird zum
Übersetzungsfehler. Die Vorlage dafür steht schon im Haus: `PlayMenuNode` macht es für das
Spielen-Menü genau so.
**Aufwand:** zwei bis drei Stunden. Der einzige größere Umbau, den dieser Bericht vorschlägt —
und der einzige, der beim nächsten Screen sofort etwas zurückgibt.

**Stand:** offen

### Szenario C · Eine zweite Datenquelle neben Fitbit

Das ist keine Spekulation: `Feature-Details.md` führt die Umstellung auf Google Health als
ausstehend („Noch komplett offen"), `app.activity` steht fertig da, und `app.tmp.Comparison`
vergleicht seit Monaten beide Seiten.

**Was schon neutral ist — und das ist überraschend viel:**

| Schicht | Zustand |
|---|---|
| `fitbit`-Tabelle (`date`, `points`, `remark`) und der View `fitbit_weekly_points` | quellenneutral, speichert nur das Ergebnis |
| `DashboardService`, `FitbitStatisticsPresenter`, `FitbitStatisticsScreen` | lesen nur die Tabelle — **gar nicht betroffen** |
| `ActivityTableDialog` und `ActivityTableRow` | schon quellenneutral, inklusive `carry`-Feld für das, was die Quelle zurückbraucht |
| `app.activity.ApiClient` + `Exercise` | fertig, mit gekapselter mm→km-Umrechnung und der dokumentierten Falle „fehlender Tag heißt nicht 0" |

**Was nicht neutral ist — eine einzige, klar benennbare Kette:**

```
ApiClient (Fitbit-JSON)
   → DataFetcher.DayData(date, ActivityLogList, ActivityDaySummary)
      → PointsCalculator.getDayPoints(ActivityLogList, ActivityDaySummary)
      → ActivityTablePresenter(date, List<Activity>, ActivityDaySummary)
         → DataReviewService  (schreibt korrigierte Schritte in das Fitbit-DTO zurück)
```

Vier Klassen, die das **Drahtformat der Fitbit-API** als Arbeitsformat benutzen. Die
Punkteregel — 20 Schritte = 1 Punkt, 1 km Rad = 19 Punkte, Spinning = 300 — ist damit an
Fitbits JSON geschweißt, obwohl sie mit der Quelle nichts zu tun hat.

**Kleinster Schnitt:** Ein framework- und quellenfreier Tagesdatensatz, etwa
`record Bewegungstag(LocalDate datum, int schritte, List<Aktivitaet> aktivitaeten)` mit
`record Aktivitaet(String art, Double km, Integer schritte, String start)`. Beide `ApiClient`
liefern ihn, `PointsCalculator` und `ActivityTablePresenter` nehmen ihn. Danach ist der
Quellenwechsel ein Austausch **einer** Klasse. Dass das geht, zeigt `ActivityTableRow`: Für den
Dialog ist dieser Schnitt bereits einmal gemacht worden, und er hat gehalten.
**Aufwand:** ein halber Tag für den Typ und die vier Umstellungen.

**Drei Dinge, die vorher aufgeräumt gehören:**

1. **`PointsCalculator` öffnet Dialoge** (Befund 5.3). Solange das so ist, kann man die Punkte
   nicht zweimal rechnen — einmal je Quelle — um sie zu vergleichen. Genau das wird man aber
   wollen, bevor man umschaltet. Das ist der Grund, warum 5.3 vor dieser Umstellung dran ist und
   nicht danach.
2. **`app.fitbit` heißt nach der Quelle, nicht nach der Sache.** Das Regelwerk hat dafür einen
   eigenen Prüfsatz: „Bausteine heißen nach dem, was sie sind. Nicht nach dem, wofür sie gerade
   benutzt werden … was, wenn ein zweites Deck derselben Bauart dazukäme?" Hier kommt eine zweite
   Quelle derselben Bauart dazu, und danach heißen Paket, Tabelle, Screen und Logdatei nach einem
   Dienst, den es nicht mehr gibt. Ein Umbenennen auf `app.activity` (dort, wo der neue Client
   schon liegt) oder auf etwas Sachliches wie `app.movement` ist keine Kosmetik, sondern der
   Unterschied zwischen einem Feature und einem Denkmal. Die DB-Tabelle darf `fitbit` heißen
   bleiben — Historie umzubenennen ist teurer als der Gewinn; das sollte dann aber als Satz
   danebenstehen.
3. **Zwei Logdateien mit zwei Formaten** — `fitbit_import.log` (geschrieben von
   `fitbit.repository.Repository`, Inhalt: die *korrigierten* Daten, siehe Befund 5.6) und
   `health_import.log` (geschrieben von `app.tmp.HealthImportLog`, Inhalt: die Rohdaten). Wer
   nach der Umstellung eine Abweichung untersucht, vergleicht zwei Dateien, die Verschiedenes
   enthalten und Verschiedenes heißen. Beim Abriss von `app.tmp` (Befund 5.7 / 6.6) fällt die
   zweite weg — dann ist der Moment, das Format der ersten festzulegen.

### Was die drei Szenarien zusammen zeigen

- **Nach unten ist die Suite gut geschnitten.** Ein neues Deck kostet zwei Dateien, ein neuer
  Screen eine Handvoll, und die Persistenz- und Anzeigeschicht der Gesundheitsdaten übersteht
  einen Quellenwechsel unberührt. Die Grenzobjekte (`ShapeGeometry`, `CardData`,
  `ActivityTableRow`, `BarChartData`, `DiaryCardData`) tragen das — jedes von ihnen ist einmal
  bewusst geschnitten worden und zahlt sich in genau diesen Szenarien aus.
- **Teuer ist die Verdrahtung nach oben.** Die 18 Callback-Paare zwischen `MainWindow` und
  `Controller` sind die einzige Stelle, an der eine Erweiterung mit der Zahl der Features wächst
  statt konstant zu bleiben.
- **Das gemeinsame Muster der drei harten Punkte** — `default -> null` im Deck-Switch, der
  String-Vergleich im Statistik-Menü, das nicht registrierte Callback — ist dasselbe: **Eine
  vergessene Erweiterung meldet sich nicht beim Übersetzen, sondern beim Klicken.** Überall
  dort, wo stattdessen ein Enum oder ein `sealed interface` steht (`PlayMenuNode`, `ButtonEnum`,
  `Card.Step`, `LearnComponent`, `SessionSwitchStrategy`), tritt das Problem nicht auf. Die
  Suite kennt die Lösung also — sie ist nur an drei Stellen nicht angewandt.

---

## Offene Fragen

Sieben Punkte, die sich aus dem Code nicht beantworten ließen. Sie sind hier Fragen und keine
Befunde, weil die Antwort die Bewertung ändert — nicht nur den Aufwand.

**1 · Setzt eine der ausgelieferten Skin-Dateien einen der vier `Double`-Schlüssel?**
Betrifft Befund 10.1. `shapeMapStandardBorderWidth`, `shapeMapFederalStateBorderWidth`,
`sketchStrokeWidth`, `sketchMarkedHatchWidth` kommen durch die FailFast-Prüfung und werden
danach still verworfen. Steht einer davon heute in einer `skin_*.properties`, weicht das
sichtbare Ergebnis seit dem Eintrag von dem ab, was dort steht — dann ist 10.1 kein latenter,
sondern ein aktiver Fehler. Die Dateien liegen im Datenordner und waren von hier nicht
einsehbar.

**2 · Fehlt `MainWindow.setCurrentSortOrder` oder ist sie überflüssig?**
Betrifft Befund 6.4. Die Methode hat keinen Aufrufer, ihr Javadoc behauptet, der Controller rufe
sie beim Start, und eine eigene Mechanik (`item.setUserData(order)`) existiert nur für sie. Heute
markiert das Menü die aktive Reihenfolge über den Umweg `lastSortOrderString` beim Bauen. Ist das
die Absicht — dann kann beides weg. War der Aufruf einmal geplant und fiel weg — dann fehlt er,
und das Menü ist nach einem Skinwechsel möglicherweise nicht mehr korrekt markiert. Das
unterscheidet „drei Zeilen löschen" von „eine Zeile ergänzen".

**3 · Ist die Frist von `app.tmp` erreicht oder verlängert?**
Betrifft Befunde 5.7 und 6.6. Beide Klassen nennen September als Wegfall, der Stand dieses
Berichts ist der 20.09.2026. Am Gerüst hängen sechs Stellen in vier Paketen, darunter eine
öffentliche Projektion in `app.fitbit` und eine Ausnahmezeile im Architekturtest. Ob der Abriss
ansteht oder der Vergleich weiterlaufen soll, ist eine Entscheidung über den Health-Umstieg und
keine über den Code.

**4 · Ist `Config.getString` als Markierung für Geheimnisse gemeint?**
Betrifft Befund 7.2. Die Methode ist ein reiner Alias für `Config.get` und hat drei Aufrufer —
zwei davon lesen ein Geheimnis (`exporter.zipPassword`, `signal.key`), der dritte einen Pfad.
Wenn das Zufall ist, gehört die Methode gelöscht. Wenn dahinter die Absicht stand, sensible
Zugriffe erkennbar zu machen, ist der Befund falsch herum — dann gehört die Idee ausgebaut statt
zurückgenommen, und `tmdb.v3.apiKey` und `whatsapp.key` müssten mit.

**5 · Trägt eine JDBC-Fehlermeldung die Signal-URL mitsamt Schlüssel?**
`SignalIncrementalImport.java:142-143` baut die Verbindungs-URL mit
`?cipher=sqlcipher&key=x'…'`, und `DriverManager.getConnection(url)` bekommt sie. Der eigene
`catch` in `:164-166` gibt die URL nicht weiter — was der SQLite-Treiber selbst in seine
`SQLException` schreibt, habe ich nicht geprüft und wollte es nicht raten. Wenn er die URL
mitnimmt, ist das derselbe Fall wie Befund 3.1, nur für den Datenbankschlüssel.

**6 · Soll `MarkMapElements.right` (die optionalen Shapes) noch kommen?**
`Card.java:35` führt das Feld samt Kommentar „Momentan ist right immer leer. Vielleicht will ich
später aber auch mal die optionalen Shapes berücksichtigen…". Es wird gefüllt und nie gelesen.
Als Platzhalter für eine geplante Erweiterung ist es in Ordnung — dann gehört ein Marker dran.
Als Überbleibsel gehört es weg (Befund 1.12).

**7 · Wird `app.fitbit` beim Quellenwechsel umbenannt?**
Betrifft Szenario C. Nach dem Umstieg heißen Paket, Screen, Presenter und Logdatei nach einem
Dienst, den es nicht mehr gibt — und der Prüfsatz des Regelwerks („was, wenn ein zweites Deck
derselben Bauart dazukäme?") fällt eindeutig aus. Die DB-Tabelle sollte dagegen `fitbit` heißen
bleiben; Historie umzubenennen kostet mehr, als es bringt. Ob der Code mitgeht und ob der
Unterschied zwischen Tabellen- und Paketnamen dann als Satz danebensteht, ist eine Entscheidung,
die vor dem Umbau fällt und nicht danach.

# ThosSuite

Eine persönliche All-in-One-Desktop-Anwendung in JavaFX — geschrieben von einem Nutzer
für genau einen Nutzer. ThosSuite bündelt mehrere Lebensbereiche in einer Oberfläche:
Geografie-Lernen (Anki- und Region-Decks), Fitness-Tracking, Tagebuch, ein Nachrichten-Archiv,
Filmbewertungen und ein paar kleinere Helfer.

![Screenshot der Suite](docs/img/ThosSuite1.png)

## Die drei Dokumente

Die Doku ist in folgende Teile geschnitten:

**[Architektur-Dokumentation](docs/Architektur-Dokumentation.md)** — 
Das einer LLM immer mitzugebende Fundament: Überblick, technische Basis, Paketstruktur,
Orchestrierungs-Mechanik, Startup. Was bei jeder Aufgabe gilt, egal welches Feature — die Karte
für den Wiedereinstieg nach Monaten Pause.

**[Design-Regeln](docs/Design-Regeln.md)** —
Das Regelwerk. Nach welchen Prinzipien die Suite in Pakete und Klassen geschnitten, benannt und
verbunden ist, und *warum*. Hier schlägt man nach, bevor man etwas Neues baut, damit es in
die Struktur passt.

**[Feature-Details](docs/Feature-Details.md)** —
Das Nachschlagewerk. Pro Feature, wie es konkret gebaut ist — Zweck, Mechanik, DB-Schema, Screen.
Man liest nur den Block, den man gerade braucht.

## Womit gebaut

JavaFX 25 · Java 25 LTS · SQLite · Jackson · `java.util.logging`. Build mit Maven, Deployment
über jpackage für Windows.

## Leitgedanken

Ein paar Grundhaltungen, die sich durch alles ziehen — ausführlich in den Design-Regeln:

- **Auffindbarkeit vor Eleganz.** Wichtigstes Ziel ist, nach Monaten den Ort einer Sache
  *vorhersagen* zu können, ohne zu suchen.
- **FailFast.** Unerwartetes crasht sofort mit Stacktrace, statt still weiterzulaufen.
- **Keine Tests.** Einziger Nutzer und Entwickler ist Thorsten; ein Fehler fällt im täglichen
  Gebrauch sofort auf.
# Thos Suite

Ein persönliches Lernprogramm von Perminides für Perminides basierend auf
[Spaced Repetition](https://de.wikipedia.org/wiki/Spaced_Repetition). Zumeist basiert das Lernen
auf einer Landkarte: Man klickt an, wo etwas liegt, tippt Namen ein, beantwortet weiterführende
Fragen oder kreuzt bei Multiple Choice die richtige Antwort an.

## Lernbereiche

### Welt 
Orte und Sehenswürdigkeiten auf der Weltkarte verorten und vertiefende Fragen beantworten.

![Welt – Weltkarte mit Orten](docs/img/ThosSuite1.png)

### Deutschland
Einzelne Kreise und kreisfreie Städte auf der Deutschlandkarte, dazu Wissenswertes aus jeder
Region.

![Deutschland – Kreise und kreisfreie Städte](docs/img/ThosSuite2.png)

### Hannover
Die Straßen und markante Orte Hannovers lernen.

![Hannover – Straßen auf der Stadtkarte](docs/img/ThosSuite5.png)

### Multiple Choice
Reines Faktenwissen ohne Karte.

![Multiple Choice – Frage mit Antwortvorgaben](docs/img/ThosSuite3.png)

### Regionen-Serien
Eine Gebietsgruppe komplett durchgehen (US-Bundesstaaten, Schweizer
Kantone, Kreise und Städte Niedersachsens und mehr): alle Gebiete auf der Karte finden oder
benennen, am Ende bestanden oder nicht.

![Regionen-Serie – Gebiete auf der Karte](docs/img/ThosSuite4.png)

---

Um diesen Lernkern ist über die Jahre eine kleine Suite gewachsen: Fitness-Tracking, ein
Tagebuch, ein Nachrichten-Archiv, Filmbewertungen und ein weitere kleinere Helfer.

## Dokumentation

Die Doku ist in folgende Teile geschnitten:

**[Architektur-Dokumentation](docs/Architektur-Dokumentation.md)** —
Das einem LLM immer mitzugebende Fundament: Überblick, technische Basis, Paketstruktur,
Orchestrierungs-Mechanik, Startup. Was bei jeder Aufgabe gilt, egal welches Feature — die Karte
für den Wiedereinstieg nach Monaten Pause.

**[Design-Regeln](docs/Design-Regeln.md)** —
Das Regelwerk. Nach welchen Prinzipien die Suite in Pakete und Klassen geschnitten, benannt und
verbunden ist, und *warum*. Hier schlägt man nach, bevor man etwas Neues baut, damit es in die
Struktur passt.

**[Feature-Details](docs/Feature-Details.md)** —
Das Nachschlagewerk. Pro Feature, wie es konkret gebaut ist — Zweck, Mechanik, DB-Schema, Screen.
Man liest nur den Block, den man gerade braucht.

## Womit gebaut

JavaFX 25 · Java 25 LTS · SQLite · Jackson · `java.util.logging`. Build mit Maven, Deployment
über jpackage für Windows.
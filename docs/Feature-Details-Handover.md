## Übergabe: Feature-Details.md — Stand & Konventionen

**Wo wir stehen:** Feature-Details.md wird Block für Block geprüft. Fertig: alc, diary,
mattress, weekday, fitbit. Als Nächstes: messaging (erster Zweig-Block, erster
Landkarten-Schema-Fall). Danach: movie, die Orchestrierung (oben, kein Feature-Block), der
Lern-Kern.

**Noch nicht in den Dateien (vor Start einpflegen):**
- Die fünf überarbeiteten Blöcke + neue Einleitung + Header-Fix in Feature-Details.md.
- Header-Fix: kaputte Cross-Referenzen auf `ThosSuite_Architektur_allgemein.md` /
  `ThosSuite_Design_Regeln.md` → richtige Namen `Architektur-Dokumentation.md` /
  `Design-Regeln.md`.
- Neuer Architekturblock „Externe Attachments" in Architektur-Dokumentation.md, unter
  „Technische Basis", nach „Datenbankzugriff & Connection-Management", vor „Code-Generierung".

**Block-Skelett (feste Reihenfolge, leere Slots entfallen):**
Zweck · Mechanik · DB-Schema · Screen (als Unterabschnitt). Config-Keys stehen inline in der
Mechanik, aber nur wenn der Key die Mechanik steuert; reine Stellknöpfe (z. B. thumbnailHeight)
gehören nicht ins Dokument.

**Prinzip — nur Feature-Spezifisches:** Der Block dokumentiert, was das Feature ausmacht, nicht
was die Paketregel ohnehin garantiert. repository/model-Standard nicht wiederholen; Zeilen wie
„DB-Zugriff in feature.repository.Repository" fliegen raus. repository/model nur erwähnen, wenn
daran etwas auffällig ist (mehrere Repos, Fremdquellen-Repo, Naming-Bruch).

**Schema-Schwelle:** Passt das Schema in ~10 Zeilen und erklärt sich selbst → `sql`-Block (alc,
fitbit). Sonst Tabellen-Landkarte in Prosa/Liste — Tabellen mit Halbsatz-Zweck, Beziehungen,
Fallstricke, KEINE Spaltenlisten (movie sicher; messaging Grenzfall, aktueller msg_-Stand ist
schon fast Landkarte).

**Offene Punkte — keine separate Liste.** Offenes geht entweder als Zeile in den Feature-Block
(wenn wichtig genug) oder als Code-Marker. Kein separates Backlog-Dokument (starten ohne, bei
Bedarf anlegen). Marker-System im Code: Später, Sofort, Architektur, Feature, TODO. Eine
Invariante ist KEIN ToDo (wird nie erledigt) → höchstens Warnung an der Codestelle.

**Zwei Sorten „offen" im Block auseinanderhalten:** Invariante (Dauerbedingung, „so gebaut, halt
dich dran") vs. unerledigte Arbeit („noch offen, ändert sich"). Für „gilt gerade, externer
Zwang, ändert sich absehbar" ist die Warnform ein `>`-Blockzitat gesetzt (Beispiel:
Fitbit-Health-Migration im fitbit-Block).

**Überschriften-Ebenen in Feature-Details:** `#` Titel · `##` Feature (bzw.
feature-übergreifender Block) · `###` genau eine Unterebene (Screen; Lern-Kern-Bereich). Tiefer
wird Fett-Label, Fließtext oder Liste.

**Offene Entscheidung Lern-Kern:** Ebenentiefe — flach halten (ein `##`, Bausteine als
Fett-Label) vs. in mehrere `##` aufteilen. Erst beim Lern-Kern entscheiden.

**Raus aus dem Doc bis geklärt:** signal.attachmentDir vs. attachments.folder — Widerspruch,
Thorstens !Sofort-ToDo im Code, bleibt bis zur Klärung aus dem messaging-Block.

**Arbeitsweise:** Ich (Thorsten) schlage vor, du (Claude) prüfst kritisch, kein vorschnelles
Loben. Pro Block: erst Accuracy, dann Struktur/Slot, dann Wortlaut. Nie raten bei Fakten, die du
nicht aus Code/Daten kennst — nachfragen. Kein Code ohne expliziten Zuruf.
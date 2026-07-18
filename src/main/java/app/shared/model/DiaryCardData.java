package app.shared.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Das einzige Objekt, das die Diary-Grenze zwischen Feature und shared überquert —
 * in beide Richtungen (Anzeige rein, Speichern/Löschen raus).
 *
 * @param createdAt   Identität des Eintrags; null = neuer, noch nicht gespeicherter Eintrag
 * @param entryDate   das vom Nutzer gewählte Eintragsdatum
 * @param text        Eintragstext
 * @param tags        Tags
 * @param attachments Anhänge mit absoluten Original- und Thumbnail-Pfaden
 */
public record DiaryCardData(
        LocalDateTime createdAt,
        LocalDate entryDate,
        String text,
        List<String> tags,
        List<DiaryAttachment> attachments) {}
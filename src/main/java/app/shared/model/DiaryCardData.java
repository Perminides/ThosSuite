package app.shared.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// !Annahme: createdAt-Typ = Entry.createdAt(). Falls Instant/LocalDate → hier + im Screen-Mapping anpassen.
public record DiaryCardData(
        LocalDateTime createdAt,
        LocalDate entryDate,
        String text,
        List<String> tags,
        List<String> attachmentPaths) {
}
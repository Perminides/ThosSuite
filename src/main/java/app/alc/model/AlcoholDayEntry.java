package app.alc.model;

import java.time.LocalDate;

public record AlcoholDayEntry(
    LocalDate date,
    AlcoholStatus status,
    int balance
) {}
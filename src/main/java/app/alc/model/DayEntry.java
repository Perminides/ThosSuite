package app.alc.model;

import java.time.LocalDate;

public record DayEntry(
    LocalDate date,
    Status status,
    int balance
) {}
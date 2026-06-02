package app.alc.model;

import java.time.LocalDate;

public record RatioEntry(
    LocalDate validFrom,
    int greenPoints,
    int yellowPoints,
    int redPoints
) {}
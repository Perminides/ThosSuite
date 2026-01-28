package app.alc;

import java.time.LocalDate;

public record AlcoholRatioEntry(
    LocalDate validFrom,
    int greenPoints,
    int yellowPoints,
    int redPoints
) {}
package app.fitbit;

import java.time.LocalDate;
import java.util.List;

import app.fitbit.model.json.Activity;

/**
 * Schlanke, öffentliche Projektion der von {@code DataFetcher} bereits geholten
 * Fitbit-Rohwerte eines Tages — der einzige nach außen geöffnete Zugang, damit der
 * isolierte Vergleicher (Paket {@code app.tmp}) mitlesen kann, OHNE Fitbit ein zweites
 * Mal abzufragen und OHNE die paket-privaten Records ({@code DayData}) offenzulegen.
 *
 * <p>Übergangsgerüst: fällt mit dem Vergleicher im September weg.</p>
 *
 * @param date       der importierte Tag
 * @param steps      Tagesschritte (roh, wie von der Fitbit-Web-API geliefert)
 * @param activities die Aktivitäten des Tages — bewusst als Liste (nicht als fertige km),
 *                   damit die "was ist Radfahren"-Definition an einer Stelle im Vergleicher liegt
 */
public record FitbitDayProjection(LocalDate date, int steps, List<Activity> activities) {}
package app.shared.model;

/**
 * Framework-freie Zeile für die editierbare Aktivitätstabelle.
 *
 * @param deletable false = Zeile kann nicht gelöscht werden (z.B. Summenzeile)
 * @param carry     opaker Durchreich-Wert; die Komponente liest ihn nie, gibt ihn
 *                  unverändert zurück (das Feature legt hier ab, was es zum Rückbau braucht)
 */
public record ActivityTableRow(
        String  startTime,
        String  activityName,
        String  distanceUnit,
        Double  distance,
        Integer steps,
        boolean deletable,
        String  carry) {}
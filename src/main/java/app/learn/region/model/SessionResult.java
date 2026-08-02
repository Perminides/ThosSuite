package app.learn.region.model;

/**
 * Wie eine Regions-Session ausgegangen ist — zugeschnitten auf das, was die Schale davon braucht.
 *
 * <p>Der Progress baut das Ergebnis, wenn er fertig ist, und meldet sich dann bei der Session. Die
 * holt es sich ab und entscheidet daraufhin, was angezeigt und ob gespeichert wird.</p>
 *
 * <p>Bewusst <b>nicht</b> enthalten ist die falsch beantwortete Form: Die braucht nur das
 * Fortschreiben, und das macht der Progress inzwischen selbst.</p>
 *
 * @param correct       war die Session erfolgreich
 * @param incorrectText was im Fehlerdialog steht; bei {@code correct} immer {@code null}
 * @param allowResume   darf der Nutzer im Fehlerdialog fortsetzen statt zu beenden
 */
public record SessionResult(boolean correct, String incorrectText, boolean allowResume) {}

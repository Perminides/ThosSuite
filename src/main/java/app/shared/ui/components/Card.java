package app.shared.ui.components;

/**
 * Was in eine {@link SuiteCardList} darf.
 *
 * <p>Ohne Methoden — die Schnittstelle sagt nur, wofür etwas gedacht ist: eine Kachel, die
 * untereinander mit ihresgleichen in einem Rollbereich steht. Um die Breite muss sie sich nicht
 * kümmern, das erledigt die Liste.</p>
 *
 * <p>Erweitert bewusst nicht {@code Region}: Das geht nicht, {@code Region} ist eine Klasse. Die
 * Liste verlangt deshalb beides zugleich ({@code <T extends Region & Card>}), und genau das ist die
 * Aussage — eine Fläche, die als Karte gedacht ist.</p>
 */
public interface Card {
}

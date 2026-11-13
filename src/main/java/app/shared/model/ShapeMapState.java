package app.shared.model;

import java.util.Set;

/**
 * Der Stand einer Shape-Karte: welche Formen richtig, falsch, markiert, aktiv oder inaktiv sind.
 *
 * <p>Framework-frei — fünf Mengen von Ids und ein Schalter. Deshalb darf das Feature ihn halten:
 * {@code region.SessionPresenter} sichert damit den Stand vor einem Fehlklick, um ihn danach
 * wiederherzustellen. Das ist Spielregel, nicht Anzeige.</p>
 */
public record ShapeMapState(
		Set<String> correctShapes,
		Set<String> incorrectShapes,
		Set<String> markedShapes,
		Set<String> activeShapes,
		Set<String> inactiveShapes,
		boolean interactive) {}

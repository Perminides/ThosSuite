package app.controller.model;

/**
 * Ein Eintrag im "Statistik"-Menü. Die Konstante ist das Protokoll zwischen Fenster und
 * Controller, der Anzeigetext hängt nur dran — so kann eine Umbenennung im Menü die Zuordnung
 * nicht mehr zerreißen.
 *
 * <p>Ein Enum statt eines sealed Interface wie bei {@link PlayMenuNode}: Hier trägt kein Eintrag
 * eigene Daten, jeder steht für genau einen Screen.</p>
 */
public enum StatisticsItem {

	DASHBOARD("Dashboard"),
	ACTIVITY("Aktivität"),
	ALCOHOL("Alkohol");

	private final String label;

	StatisticsItem(String label) {
		this.label = label;
	}

	/** Der Text, der im Menü angezeigt wird. */
	public String label() {
		return label;
	}
}

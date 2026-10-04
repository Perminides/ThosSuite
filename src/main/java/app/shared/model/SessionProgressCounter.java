package app.shared.model;

/**
 * Wie eine Lernsession steht: richtig, falsch, und wie viele Karten sie überhaupt hat.
 *
 * <p>Wohnt hier, weil die Zahlen die Grenze zur Anzeige überqueren — das Fortschrittsfeld
 * bekommt dieses Record und entscheidet selbst, ob es daraus drei Zeilen Text oder einen Balken
 * macht.</p>
 */
public record SessionProgressCounter(int correct, int incorrect, int total) {

	/** Was noch aussteht. */
	public int open() {
		return total - correct - incorrect;
	}
}

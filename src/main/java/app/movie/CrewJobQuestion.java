package app.movie;

import app.shared.model.ButtonEnum;
import app.shared.ui.Alerts;

/**
 * Die Frage nach einem unbekannten Crew-Job: aufnehmen oder künftig überspringen?
 *
 * <p>Zwei Wege stellen sie — der {@code MovieCleanup}-PostTask aus den Pending-Tabellen und der
 * Serien-Import unmittelbar beim Import. Deshalb steht sie hier und nicht in einem der beiden.</p>
 */
final class CrewJobQuestion {

	private CrewJobQuestion() {
	}

	/**
	 * @param context worum es geht — der Filmtitel, der Serienname, die Episode
	 * @return        {@code true} für die Whitelist, {@code false} für die Blacklist
	 */
	static boolean ask(String personName, String job, String department, String context) {
		ButtonEnum result = Alerts.show(
				"Unbekannter Crew-Job",
				"Person: " + personName + "\n"
				+ "Job: " + job + "\n"
				+ "Department: " + department + "\n"
				+ "Kontext: " + context,
				ButtonEnum.WHITELIST, ButtonEnum.BLACKLIST);

		if (result != ButtonEnum.WHITELIST && result != ButtonEnum.BLACKLIST)
			throw new RuntimeException("Crew-Dialog ohne Auswahl geschlossen. person=" + personName
					+ ", job=" + job);

		return result == ButtonEnum.WHITELIST;
	}
}

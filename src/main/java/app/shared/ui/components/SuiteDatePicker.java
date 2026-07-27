package app.shared.ui.components;

import java.time.LocalDate;

import javafx.scene.control.DatePicker;

/**
 * Der DatePicker der Suite.
 *
 * <p>Braucht keinen einzigen Skin-Wert — das Aussehen kommt komplett aus dem CSS
 * ({@code addDatePickerStyles}). Übrig bleibt eine einzige Festlegung: keine Kalenderwochen.</p>
 *
 * <p>TODO: Ein alter Kommentar im Skin fragte, warum das Tagebuch Kalenderwochen zeigt und die
 *   Statistik-Screens nicht. Der Code sagt etwas anderes — beide liefen über dieselbe Fabrik und
 *   damit beide <b>ohne</b>. Entweder ist die Beobachtung veraltet, oder sie betraf etwas anderes.
 *   Bei Gelegenheit nachsehen und die Frage schließen.</p>
 */
public class SuiteDatePicker extends DatePicker {

	public SuiteDatePicker(LocalDate defaultDate) {
		super(defaultDate);
		setShowWeekNumbers(false);
	}
}

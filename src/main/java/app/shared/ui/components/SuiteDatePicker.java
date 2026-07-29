package app.shared.ui.components;

import java.time.LocalDate;

import javafx.scene.control.DatePicker;

/**
 * Der DatePicker der Suite.
 * Eine einzige Festlegung: keine Kalenderwochen.
 */
public class SuiteDatePicker extends DatePicker {

	public SuiteDatePicker(LocalDate defaultDate) {
		super(defaultDate);
		setShowWeekNumbers(false);
	}
}

package app.shared.ui;

import java.time.LocalDate;
import java.util.Optional;

import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Fragt ein einzelnes Datum ab.
 *
 * <p>Parametrisierter Standarddialog: Text rein, Datum oder {@code null} raus. Kein
 * feature-seitiges Objekt, kein Ergebnis-Record — bei einem einzelnen Wert wäre das Zeremonie.</p>
 *
 * <p>Vorher lag dieser Dialog inline im {@code SuiteExporter} und damit im {@code controller}, der
 * dafür {@code SuiteDialog} aus {@code shared.ui.components} importieren musste — ein Verstoß gegen
 * die Regel, dass nur {@code shared.ui} die Bausteine kennt.</p>
 */
public class DatePickerDialog {

	private DatePickerDialog() {
	}

	/**
	 * @return das gewählte Datum, oder {@code null} bei Abbruch (auch wenn kein Datum gesetzt ist)
	 */
	public static LocalDate show(String title, String question, LocalDate defaultDate) {
		DatePicker picker = SkinService.get().createDatePicker(defaultDate);

		SuiteDialog<ButtonType> dialog = new SuiteDialog<>(title);
		VBox content = dialog.contentBox();
		content.getChildren().add(new Label(question));
		content.getChildren().add(picker);

		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

		Optional<ButtonType> result = dialog.showAndWait();
		if (result.isEmpty() || result.get().equals(ButtonType.CANCEL))
			return null;
		return picker.getValue();
	}
}

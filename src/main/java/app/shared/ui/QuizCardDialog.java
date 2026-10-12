package app.shared.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import app.shared.model.QuizCardDecision;
import app.shared.model.QuizCardResult;
import app.shared.model.QuizQuestionData;
import app.shared.ui.components.SuiteDialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Fragt, ob aus einer Quizfrage inzwischen eine Karte geworden ist. Zeigt Frage, Lösung und — falls
 * es einen gibt — den Vorschlag von Claude in einem Textfeld, aus dem man kopieren kann. Darunter
 * ein Hinweisfeld: „Nachbessern“ holt damit einen neuen Vorschlag. Gibt es noch keinen, heißt der
 * Knopf „Claude“ und holt den ersten.
 *
 * <p>Esc und das X bedeuten {@link QuizCardDecision#DONE}.</p>
 */
public class QuizCardDialog {

	private QuizCardDialog() {
	}

	/** @param suggestion der Vorschlag von Claude, oder {@code null}, wenn es noch keinen gibt */
	public static QuizCardResult show(QuizQuestionData data, String suggestion) {
		SuiteDialog<QuizCardDecision> dialog = new SuiteDialog<>("Karte erstellt? · " + QuizQuestionDialog.heading(data));
		VBox content = dialog.contentBox();
		content.getStyleClass().add("my-quiz-dialog");
		content.getChildren().addAll(
				QuizQuestionDialog.wrapped(data.question()),
				QuizQuestionDialog.wrapped("Lösung: " + data.answer()));
		if (suggestion != null) {
			TextArea text = new TextArea(suggestion);
			text.setEditable(false);
			text.setWrapText(true);
			text.setPrefRowCount(16);
			content.getChildren().add(text);
		} else {
			content.getChildren().add(QuizQuestionDialog.wrapped("Noch ohne Vorschlag von Claude."));
		}
		TextField instruction = QuizQuestionDialog.instructionField();
		content.getChildren().add(instruction);
		dialog.getDialogPane().setContent(content);

		Map<ButtonType, QuizCardDecision> decisions = new LinkedHashMap<>();
		for (QuizCardDecision decision : QuizCardDecision.values()) {
			ButtonData buttonData = decision == QuizCardDecision.DONE ? ButtonData.CANCEL_CLOSE : ButtonData.OTHER;
			String label = decision == QuizCardDecision.REFINE && suggestion == null ? "Claude" : decision.getText();
			decisions.put(new ButtonType(label, buttonData), decision);
		}
		dialog.getDialogPane().getButtonTypes().setAll(decisions.keySet());
		dialog.setResultConverter(decisions::get);

		QuizCardDecision decision = dialog.showAndWait().orElse(QuizCardDecision.DONE);
		return new QuizCardResult(decision, QuizQuestionDialog.instructionOf(instruction));
	}
}

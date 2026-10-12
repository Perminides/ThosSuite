package app.shared.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.shared.model.QuizAnswer;
import app.shared.model.QuizQuestionData;
import app.shared.model.QuizQuestionResult;
import app.shared.ui.components.SuiteDialog;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

/**
 * Stellt eine Quizfrage. Erst die Frage, auf „Lösung zeigen“ die Lösung und das Hinweisfeld für
 * Claude; die Antwortknöpfe sind bis dahin ausgegraut, damit niemand antwortet, bevor er die Lösung
 * gesehen hat — „Überspringen“ und „Fertig“ gehen immer. Beim Aufdecken landen Frage und Lösung in der Zwischenablage — für Fragen, die sich
 * so, wie sie sind, als Karte übernehmen lassen.
 *
 * <p>Alles, was später sichtbar wird, ist von Anfang an da und nur verborgen. So vermisst JavaFX das
 * Fenster beim Öffnen vollständig — kommen Knöpfe erst nach dem Öffnen dazu, sind sie beim
 * Neuvermessen noch nicht aufgebaut, und die Knopfleiste kürzt ihre Texte mit Auslassungspunkten.</p>
 *
 * <p>Esc und das X bedeuten {@link QuizAnswer#DONE}.</p>
 */
public class QuizQuestionDialog {

	private QuizQuestionDialog() {
	}

	public static QuizQuestionResult show(QuizQuestionData data) {
		SuiteDialog<QuizAnswer> dialog = new SuiteDialog<>(heading(data));
		VBox content = dialog.contentBox();
		content.getStyleClass().add("my-quiz-dialog");

		Label solution = wrapped("Lösung: " + data.answer());
		solution.setVisible(false);
		TextField instruction = instructionField();
		instruction.setVisible(false);
		content.getChildren().addAll(wrapped(data.question()), solution, instruction);
		dialog.getDialogPane().setContent(content);

		ButtonType revealType = new ButtonType("Lösung zeigen", ButtonData.OTHER);
		Map<ButtonType, QuizAnswer> answers = new LinkedHashMap<>();
		for (QuizAnswer answer : QuizAnswer.values()) {
			ButtonData buttonData = answer == QuizAnswer.DONE ? ButtonData.CANCEL_CLOSE : ButtonData.OTHER;
			answers.put(new ButtonType(answer.getText(), buttonData), answer);
		}
		List<ButtonType> buttonTypes = new ArrayList<>();
		buttonTypes.add(revealType);
		buttonTypes.addAll(answers.keySet());
		dialog.getDialogPane().getButtonTypes().setAll(buttonTypes);

		List<Node> answerButtons = new ArrayList<>();
		for (Map.Entry<ButtonType, QuizAnswer> entry : answers.entrySet()) {
			if (entry.getValue() != QuizAnswer.DONE && entry.getValue() != QuizAnswer.SKIP)
				answerButtons.add(dialog.getDialogPane().lookupButton(entry.getKey()));
		}
		for (Node button : answerButtons)
			button.setDisable(true);

		Node reveal = dialog.getDialogPane().lookupButton(revealType);
		// Knöpfe der Leiste schließen den Dialog; dieser soll nur aufdecken
		reveal.addEventFilter(ActionEvent.ACTION, event -> {
			event.consume();
			reveal.setDisable(true);
			solution.setVisible(true);
			instruction.setVisible(true);
			for (Node button : answerButtons)
				button.setDisable(false);
			ClipboardContent clip = new ClipboardContent();
			clip.putString(data.question() + "\n\n" + data.answer());
			Clipboard.getSystemClipboard().setContent(clip);
		});
		dialog.setOnShown(_ -> reveal.requestFocus());
		dialog.setResultConverter(answers::get);

		QuizAnswer answer = dialog.showAndWait().orElse(QuizAnswer.DONE);
		return new QuizQuestionResult(answer, instructionOf(instruction));
	}

	/** Titel aus Kategorie und Thema; Mystery-Fragen haben kein Thema. */
	static String heading(QuizQuestionData data) {
		return data.topic() == null ? data.category() : data.category() + " · " + data.topic();
	}

	static Label wrapped(String text) {
		Label label = new Label(text);
		label.setWrapText(true);
		return label;
	}

	static TextField instructionField() {
		TextField field = new TextField();
		field.setPromptText("Hinweis an Claude (optional)");
		return field;
	}

	/** Der Hinweis, oder {@code null}, wenn nichts eingetragen ist. */
	static String instructionOf(TextField field) {
		String text = field.getText().strip();
		return text.isEmpty() ? null : text;
	}
}

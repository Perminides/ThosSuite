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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

/**
 * Stellt eine Quizfrage. Erst die Frage, auf „Lösung zeigen“ die Lösung, der Vorschlag von Claude
 * (falls es einen gibt) und das Hinweisfeld; die Antwortknöpfe sind bis dahin ausgegraut, damit
 * niemand antwortet, bevor er die Lösung gesehen hat — „Überspringen“ und „Fertig“ gehen immer.
 * Beim Aufdecken landen Frage und Lösung in der Zwischenablage, für Fragen, die sich so, wie sie
 * sind, als Karte übernehmen lassen. Der Vorschlag steht in einem Textfeld, aus dem man kopieren
 * kann.
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

	/**
	 * @param suggestion der neueste Vorschlag von Claude, oder {@code null}, wenn es keinen gibt
	 * @param revealed   {@code true}, wenn die Frage schon aufgedeckt erscheinen soll — nach einem
	 *                   frischen Vorschlag, dessen Lösung ohnehin schon gesehen ist
	 */
	public static QuizQuestionResult show(QuizQuestionData data, String suggestion, boolean revealed) {
		SuiteDialog<QuizAnswer> dialog = new SuiteDialog<>(heading(data));
		VBox content = dialog.contentBox();
		content.getStyleClass().add("my-quiz-dialog");

		List<Node> hidden = new ArrayList<>();
		Label solution = wrapped("Lösung: " + data.answer());
		hidden.add(solution);
		content.getChildren().addAll(wrapped(data.question()), solution);
		if (suggestion != null) {
			// Der Vorschlag verrät die Lösung, also bis zum Aufdecken verborgen
			TextArea text = new TextArea(suggestion);
			text.setEditable(false);
			text.setWrapText(true);
			text.setPrefRowCount(16);
			hidden.add(text);
			content.getChildren().add(text);
		}
		TextField instruction = new TextField();
		instruction.setPromptText("Hinweis an Claude (optional)");
		hidden.add(instruction);
		content.getChildren().add(instruction);
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
		Node reveal = dialog.getDialogPane().lookupButton(revealType);
		setRevealed(revealed, reveal, hidden, answerButtons);

		// Knöpfe der Leiste schließen den Dialog; dieser soll nur aufdecken
		reveal.addEventFilter(ActionEvent.ACTION, event -> {
			event.consume();
			setRevealed(true, reveal, hidden, answerButtons);
			ClipboardContent clip = new ClipboardContent();
			clip.putString(data.question() + "\n\n" + data.answer());
			Clipboard.getSystemClipboard().setContent(clip);
		});
		if (!revealed)
			dialog.setOnShown(_ -> reveal.requestFocus());
		dialog.setResultConverter(answers::get);

		QuizAnswer answer = dialog.showAndWait().orElse(QuizAnswer.DONE);
		String text = instruction.getText().strip();
		return new QuizQuestionResult(answer, text.isEmpty() ? null : text);
	}

	private static void setRevealed(boolean revealed, Node reveal, List<Node> hidden, List<Node> answerButtons) {
		reveal.setDisable(revealed);
		for (Node node : hidden)
			node.setVisible(revealed);
		for (Node button : answerButtons)
			button.setDisable(!revealed);
	}

	/** Titel aus Kategorie und Thema; Mystery-Fragen haben kein Thema. */
	private static String heading(QuizQuestionData data) {
		return data.topic() == null ? data.category() : data.category() + " · " + data.topic();
	}

	private static Label wrapped(String text) {
		Label label = new Label(text);
		label.setWrapText(true);
		return label;
	}
}

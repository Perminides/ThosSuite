package app.shared.ui;

import java.util.Optional;

import app.shared.ui.components.SuiteDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Kopftext + mehrzeiliges Textfeld, OK/Abbrechen.
 * Ergebnis = eingegebener Text oder null
 */
public class TextPromptDialog {

    public static String show(String title, String headerText, String prefill) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);
        textArea.setPrefColumnCount(40);
        if (prefill != null)
            textArea.setText(prefill);

        SuiteDialog<ButtonType> dialog = new SuiteDialog<>(title);
        VBox content = dialog.contentBox();
        content.getChildren().addAll(new Label(headerText), textArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<?> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().equals(ButtonType.CANCEL))
            return null;

        return textArea.getText();
    }
}
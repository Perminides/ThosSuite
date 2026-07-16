package app.shared.ui.dialog;

import java.util.Optional;

import app.shared.skin.SkinService;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Kopftext + mehrzeiliges Textfeld, OK/Abbrechen.
 * Ergebnis = eingegebener Text; Optional.empty() bei Abbrechen/X.
 */
public class TextPromptDialog {

    public static Optional<String> show(String title, String headerText, String prefill) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);
        textArea.setPrefColumnCount(40);
        if (prefill != null)
            textArea.setText(prefill);

        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), title);
        VBox content = SkinService.get().createDialogContent();
        content.getChildren().addAll(new Label(headerText), textArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<?> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().equals(ButtonType.CANCEL))
            return Optional.empty();

        return Optional.of(textArea.getText() == null ? "" : textArea.getText());
    }
}
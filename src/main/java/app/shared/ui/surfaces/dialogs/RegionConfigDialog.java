package app.shared.ui.surfaces.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import app.shared.model.RegionDialogState;
import app.shared.model.RegionDialogState.Choice;
import app.shared.model.RegionDialogState.Toggle;
import app.shared.skin.SkinService;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RegionConfigDialog {

    private final RegionDialogState initial;
    private final UnaryOperator<RegionDialogState> reduce;

    private final ComboBox<String> modeCombo = new ComboBox<>();
    private final List<List<CheckBox>> deckBoxes = new ArrayList<>();
    private Button okButton;
    private boolean applying = false;

    public RegionConfigDialog(RegionDialogState initial, UnaryOperator<RegionDialogState> reduce) {
        this.initial = initial;
        this.reduce = reduce;
    }

    public Optional<RegionDialogState> showAndWait() {
        Dialog<?> dialog = SkinService.get().createDialog(null, "Regionen spielen"); // Owner intern
        DialogPane pane = dialog.getDialogPane();

        applying = true;
        VBox content = SkinService.get().createDialogContent();

        modeCombo.setOnAction(_ -> onChange());
        content.getChildren().add(modeCombo);

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_CENTER);
        for (List<Toggle> column : initial.deckColumns()) {
            VBox col = new VBox(5);
            col.setAlignment(Pos.TOP_LEFT);
            List<CheckBox> boxColumn = new ArrayList<>();
            for (Toggle toggle : column) {
                CheckBox box = new CheckBox(toggle.label());
                box.setOnAction(_ -> onChange());
                boxColumn.add(box);
                col.getChildren().add(box);
            }
            deckBoxes.add(boxColumn);
            columns.getChildren().add(col);
        }
        content.getChildren().add(columns);

        pane.setContent(content);
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        okButton = (Button) pane.lookupButton(ButtonType.OK);
        applying = false;

        render(initial);

        Optional<?> result = dialog.showAndWait();
        return (result.isPresent() && result.get() == ButtonType.OK)
                ? Optional.of(readState())
                : Optional.empty();
    }

    private void onChange() {
        if (!applying) render(reduce.apply(readState()));
    }

    // UI -> State
    private RegionDialogState readState() {
        List<Choice> modes = new ArrayList<>();
        for (String label : modeCombo.getItems())
            modes.add(new Choice(label, label.equals(modeCombo.getValue())));

        List<List<Toggle>> deckColumns = new ArrayList<>();
        for (List<CheckBox> boxColumn : deckBoxes) {
            List<Toggle> column = new ArrayList<>();
            for (CheckBox box : boxColumn)
                column.add(new Toggle(box.getText(), box.isSelected(), box.isDisabled()));
            deckColumns.add(column);
        }
        return new RegionDialogState(modes, deckColumns);
    }

    // State -> UI
    private void render(RegionDialogState state) {
        applying = true;
        try {
            List<String> items = new ArrayList<>();
            String selected = null;
            for (Choice choice : state.modes()) {
                items.add(choice.label());
                if (choice.selected()) selected = choice.label();
            }
            
         // setItems mit NEUER Liste, nicht getItems().setAll(...): Beim In-place-Mutieren
         // der Item-Liste bleibt die Button-Zelle auf einem alten Eintrag stehen, während
         // die value-Property unverändert bleibt. Das folgende setValue(selected) ist dann
         // ein No-op (Wert ändert sich nicht) -> Zelle wird nicht neu gezeichnet und zeigt
         // einen falschen Modus an, obwohl der wirksame Wert korrekt ist. Ein neues Listen-
         // Objekt setzt die Selektion zurück, wodurch setValue eine echte Änderung ist und
         // die Zelle frisch rendert.
            modeCombo.setItems(FXCollections.observableArrayList(items));
            modeCombo.setValue(selected);

            boolean anyChecked = false;
            for (int c = 0; c < deckBoxes.size(); c++) {
                List<Toggle> column = state.deckColumns().get(c);
                List<CheckBox> boxColumn = deckBoxes.get(c);
                for (int r = 0; r < boxColumn.size(); r++) {
                    Toggle toggle = column.get(r);
                    CheckBox box = boxColumn.get(r);
                    box.setSelected(toggle.checked());
                    box.setDisable(toggle.disabled());
                    if (toggle.checked()) anyChecked = true;
                }
            }
            okButton.setDisable(!anyChecked); // OK: mindestens eine Region gewählt
        } finally {
            applying = false;
        }
    }
}
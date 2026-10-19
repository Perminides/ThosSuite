package app.shared.ui.components;

import java.util.List;
import java.util.function.Consumer;

import app.shared.model.CollectChip;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

/**
 * Die gesammelten Wörter eines Collect-Steps als Chips, umgebrochen auf die übergebene Breite.
 *
 * <p>Den Zustand jedes Chips entscheidet der Aufrufer ({@link CollectChip.State}); hier wird er nur
 * zur Pseudoklasse: {@code :correct}, {@code :incorrect} (falsch gewählt), {@code :missing}
 * (vergessen). Ein tolerierter Chip bekommt keine. Nur ein noch ungewerteter Chip trägt ein ×.</p>
 *
 * <p>CSS: Chip = {@code .collect-chip}, × = {@code .tag-chip-remove} wie im Tagebuch.</p>
 */
public class CollectChipPane extends FlowPane {

	private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
	private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
	private static final PseudoClass MISSING = PseudoClass.getPseudoClass("missing");

	private Consumer<String> onRemove;

	public CollectChipPane(double width) {
		super(6, 6);
		setPrefWidth(width);
		setPrefWrapLength(width);
		setAlignment(Pos.CENTER_LEFT);
	}

	public void onRemove(Consumer<String> onRemove) {
		this.onRemove = onRemove;
	}

	public void setChips(List<CollectChip> chips) {
		getChildren().clear();
		for (CollectChip chip : chips)
			getChildren().add(createChip(chip));
	}

	private HBox createChip(CollectChip chip) {
		HBox box = new HBox(2, new Label(chip.text()));
		box.setAlignment(Pos.CENTER);
		box.getStyleClass().add("collect-chip");
		box.pseudoClassStateChanged(CORRECT, chip.state() == CollectChip.State.CORRECT);
		box.pseudoClassStateChanged(INCORRECT, chip.state() == CollectChip.State.WRONG);
		box.pseudoClassStateChanged(MISSING, chip.state() == CollectChip.State.MISSING);

		if (chip.state() == CollectChip.State.CHOSEN) {
			Button remove = new Button("×");
			remove.getStyleClass().add("tag-chip-remove");
			remove.setFocusTraversable(false);
			remove.setOnAction(_ -> onRemove.accept(chip.text()));
			box.getChildren().add(remove);
		}
		return box;
	}
}

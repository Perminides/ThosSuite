package app.shared.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import app.shared.model.McMetrics;
import app.shared.model.UiComponent;
import app.shared.skin.SkinService;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * A pane that shows up to 8 Multiple-Choice-Buttons. Three different layouts depending on font-size:
 * 		- Single line normal font for short text
 * 		- 2 lines normal font with reduced padding for longer text (squeezed)
 * 		- up to 3 lines in small font for very long text (tiny)
 *
 * <p>MC misst selbst, welche Stufe ein Antworttext braucht, und togglet die passende Pseudo-Klasse. Die dafür
 * nötigen Maße holt es sich als {@link McMetrics} beim Skin — sie hängen an der Schrift und den Rändern, nicht
 * am Aufrufer. Übergeben wird nur die Breite beziehungsweise das Feld.</p>
 *
 * CSS-classes
 * 		Button	= "my-mc-button"
 */
public class MultipleChoicePane extends Pane implements UiComponent{

    // --- Logik-Zustände (Exklusiv) ---
    private static final PseudoClass STATE_INACTIVE = PseudoClass.getPseudoClass("inactive");
    private static final PseudoClass STATE_ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass STATE_CORRECT = PseudoClass.getPseudoClass("correct");
    private static final PseudoClass STATE_INCORRECT = PseudoClass.getPseudoClass("incorrect");

    // --- Layout-Zustände (Additiv zu Logik-Zuständen) ---
    // squeezed: Text umbrechen, Padding reduzieren
    private static final PseudoClass STATE_SQUEEZED = PseudoClass.getPseudoClass("squeezed");
    // tiny: Text umbrechen, Padding reduzieren UND Schrift verkleinern
    private static final PseudoClass STATE_TINY = PseudoClass.getPseudoClass("tiny");

    private final List<Button> buttons = new ArrayList<>();
    private final McMetrics metrics;

    private Consumer<Integer> listener;

    /** Mit fester Lage — für absolut positionierende Hosts. Nur die Breite zählt, die Höhe ergibt sich. */
    public MultipleChoicePane(Rectangle2D bounds) {
        this(bounds.getWidth());
        setLayoutX(bounds.getMinX());
        setLayoutY(bounds.getMinY());
    }

    /** Ohne feste Lage — für Aufrufer, die die Auswahl in ein Layout hängen. */
    public MultipleChoicePane(double width) {
        this.metrics = SkinService.get().mcMetrics();

        double buttonHeight = metrics.buttonHeight();
        int verticalGap = metrics.verticalGap();

        double totalHeight = (buttonHeight * 8) + (verticalGap * 7);
        this.setPrefSize(width, totalHeight);

        double yPos = 0;
        for (int i = 0; i < 8; i++) {
            Button btn = createButton(i, width, buttonHeight);
            btn.setLayoutX(0);
            btn.setLayoutY(yPos);
            buttons.add(btn);
            getChildren().add(btn);
            yPos += buttonHeight + verticalGap;
        }
    }
    
    @Override
	public Node getView() {
		return this;
	}

    private Button createButton(int index, double width, double height) {
        Button btn = new Button();
        btn.setPrefSize(width, height);
        btn.setMinSize(width, height);
        btn.setMaxSize(width, height);
        btn.getStyleClass().add("my-mc-button");

        // WrapText muss für alle an sein, damit es wirkt, wenn wir es brauchen.
        // Gesteuert wird das Aussehen aber über CSS (Padding/Font).
        btn.setWrapText(true);
        btn.setTextAlignment(TextAlignment.CENTER);
        btn.setAlignment(Pos.CENTER);

        btn.setOnAction(_ -> {
            if (listener != null) listener.accept(index);
        });

        btn.setText("");
        setButtonLogicState(btn, STATE_INACTIVE);

        return btn;
    }

    public void initiateMultipleChoice(List<String> answers) {
        for (int i = 0; i < 8; i++) {
            Button btn = buttons.get(i);

            // Reset aller Layout-States
            btn.pseudoClassStateChanged(STATE_SQUEEZED, false);
            btn.pseudoClassStateChanged(STATE_TINY, false);

            if (i < answers.size()) {
                String text = answers.get(i);
                btn.setText(text);

                double availableTextWidth = btn.getPrefWidth() - metrics.horizontalOverhead();

                Text measure = new Text(text);
                measure.setFont(metrics.font());

                // SCHRITT 1: Passt es einzeilig?
                if (measure.getLayoutBounds().getWidth() > availableTextWidth) {

                    // Nein. Passt es "gequetscht" zweizeilig?
                    measure.setWrappingWidth(availableTextWidth);
                    measure.setLineSpacing(metrics.lineSpacingSqueezed());

                    // Limit: ButtonHöhe - (Rahmen Oben + Rahmen Unten). Kein Padding!
                    double absoluteMaxHeight = btn.getPrefHeight() - (metrics.borderWidth() * 2);

                    if (measure.getLayoutBounds().getHeight() <= absoluteMaxHeight) {
                        // JA! Es passt mit Quetschen.
                        btn.pseudoClassStateChanged(STATE_SQUEEZED, true);
                    } else {
                        // NEIN! Selbst Quetschen reicht nicht -> Tiny Mode.
                        btn.pseudoClassStateChanged(STATE_TINY, true);
                    }
                }

                setButtonLogicState(btn, STATE_ACTIVE);
            } else {
                btn.setText("");
                setButtonLogicState(btn, STATE_INACTIVE);
            }
        }
    }

    public void clearAndSetInactive() {
        for (Button btn : buttons) {
            btn.setText("");
            setButtonLogicState(btn, STATE_INACTIVE);
        }
    }

    public void setCorrectAndInactive(Collection<Integer> correctIndices) {
        for (int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            if (correctIndices.contains(i)) {
                setButtonLogicState(btn, STATE_CORRECT);
            } else if (btn.getPseudoClassStates().contains(STATE_INCORRECT)) {
                // Bleibt incorrect
            } else {
                setButtonLogicState(btn, STATE_INACTIVE);
            }
        }
    }

    public void setCorrect(int index, boolean correct) {
        setButtonLogicState(buttons.get(index), correct ? STATE_CORRECT : STATE_INCORRECT);
    }

    public void addListener(Consumer<Integer> listener) {
        this.listener = listener;
    }

    // Nur für Farben/Logik zuständig, fasst Layout nicht an!
    private void setButtonLogicState(Button btn, PseudoClass state) {
        btn.pseudoClassStateChanged(STATE_INACTIVE, state == STATE_INACTIVE);
        btn.pseudoClassStateChanged(STATE_ACTIVE, state == STATE_ACTIVE);
        btn.pseudoClassStateChanged(STATE_CORRECT, state == STATE_CORRECT);
        btn.pseudoClassStateChanged(STATE_INCORRECT, state == STATE_INCORRECT);
    }
}
package app.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class MultipleChoicePane extends Pane {

    // --- Logik-Zustände (Exklusiv) ---
    private static final PseudoClass STATE_INACTIVE = PseudoClass.getPseudoClass("inactive");
    private static final PseudoClass STATE_ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass STATE_CORRECT = PseudoClass.getPseudoClass("correct");
    private static final PseudoClass STATE_INCORRECT = PseudoClass.getPseudoClass("incorrect");

    // --- Layout-Zustände (Additiv zu Logik-Zuständen) ---
    // multiline: Text umbrechen, Padding reduzieren
    private static final PseudoClass STATE_SQUEEZED = PseudoClass.getPseudoClass("squeezed"); // NEU
    // tiny: Text umbrechen, Padding reduzieren UND Schrift verkleinern
    private static final PseudoClass STATE_TINY = PseudoClass.getPseudoClass("tiny");

    private final List<Button> buttons = new ArrayList<>();
    private final Font font;
    private final Font smallFont;
    private final double borderWidth; // NEU: Damit wir das absolute Limit kennen
    private final double horizontalOverhead; // Ersatz für die Magic Number "24"
    private final double lineSpacingSqueezed;
    private final double lineSpacingTiny;
    
    private Consumer<Integer> listener;

    public MultipleChoicePane(double width, double fixedButtonHeight, double horizontalOverhead, double borderWidth, Font font, Font smallFont, int verticalGap, double lineSpacingSqueezed, double lineSpacingTiny) {
        this.font = font;
        this.smallFont = smallFont;
        this.horizontalOverhead = horizontalOverhead;
        this.borderWidth = borderWidth;
        this.lineSpacingSqueezed = lineSpacingSqueezed;
        this.lineSpacingTiny = lineSpacingTiny;
        
        double totalHeight = (fixedButtonHeight * 8) + (verticalGap * 7);
        this.setPrefSize(width, totalHeight);

        double yPos = 0;
        for (int i = 0; i < 8; i++) {
            Button btn = createButton(i, width, fixedButtonHeight);
            btn.setLayoutX(0);
            btn.setLayoutY(yPos);
            buttons.add(btn);
            getChildren().add(btn);
            yPos += fixedButtonHeight + verticalGap;
        }
    }

    private Button createButton(int index, double width, double height) {
        Button btn = new Button();
        btn.setPrefSize(width, height);
        btn.setMinSize(width, height);
        btn.setMaxSize(width, height);
        btn.getStyleClass().add("mc-button");
        
        // WrapText muss für alle an sein, damit es wirkt, wenn wir es brauchen.
        // Gesteuert wird das Aussehen aber über CSS (Padding/Font).
        btn.setWrapText(true); 
        btn.setTextAlignment(TextAlignment.CENTER);
        btn.setAlignment(Pos.CENTER);
        
        btn.setOnAction(e -> {
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
                
                double availableTextWidth = btn.getPrefWidth() - horizontalOverhead;
                
                Text measure = new Text(text);
                measure.setFont(font);

                // SCHRITT 1: Passt es einzeilig?
                if (measure.getLayoutBounds().getWidth() > availableTextWidth) {
                    
                    // Nein. Passt es "gequetscht" zweizeilig?
                    measure.setWrappingWidth(availableTextWidth);
                    
                    // NEU: Variable statt Magic Number -6
                    measure.setLineSpacing(lineSpacingSqueezed); 
                    
                    // Limit: ButtonHöhe - (Rahmen Oben + Rahmen Unten). Kein Padding!
                    double absoluteMaxHeight = btn.getPrefHeight() - (borderWidth * 2);
                    
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
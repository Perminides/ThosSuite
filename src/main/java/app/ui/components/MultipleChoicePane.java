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

    // Die 4 exklusiven Zustände (wie von dir definiert)
    private static final PseudoClass STATE_INACTIVE = PseudoClass.getPseudoClass("inactive");
    private static final PseudoClass STATE_ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass STATE_CORRECT = PseudoClass.getPseudoClass("correct");
    private static final PseudoClass STATE_INCORRECT = PseudoClass.getPseudoClass("incorrect");

    private final List<Button> buttons = new ArrayList<>();
    private final Font font;
    private final Font smallFont;
    private final double maxTextHeight;
    
    private Consumer<Integer> listener;

    public MultipleChoicePane(double width, double fixedButtonHeight, double maxTextHeight, Font font, Font smallFont, int verticalGap) {
        this.font = font;
        this.smallFont = smallFont;
        this.maxTextHeight = maxTextHeight;
        
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
        btn.setWrapText(true);
        btn.setTextAlignment(TextAlignment.CENTER);
        btn.setAlignment(Pos.CENTER);
        
        // WICHTIG: Button feuert IMMER Events. Der Presenter entscheidet.
        // Wir nutzen kein setDisable(true) mehr!
        btn.setOnAction(_ -> {
            if (listener != null) listener.accept(index);
        });
        
        // Initial: Leer und Inaktiv
        btn.setText("");
        setButtonState(btn, STATE_INACTIVE);
        
        return btn;
    }

    
    public void initiateMultipleChoice(List<String> answers) {
        for (int i = 0; i < 8; i++) {
            Button btn = buttons.get(i);
            
            if (i < answers.size()) {
                // SLOT BELEGT: Text setzen + Zustand ACTIVE
                String text = answers.get(i);
                
                // Font-Logik
                double textAreaWidth = btn.getPrefWidth() - 24; 
                Text measure = new Text(text);
                measure.setFont(font);
                measure.setWrappingWidth(textAreaWidth);
                
                if (measure.getLayoutBounds().getHeight() > maxTextHeight) {
                    btn.setFont(smallFont);
                } else {
                    btn.setFont(font);
                }
                
                btn.setText(text);
                setButtonState(btn, STATE_ACTIVE);
                
            } else {
                // SLOT UNBENUTZT: Text weg + Zustand INACTIVE
                btn.setText("");
                setButtonState(btn, STATE_INACTIVE);
            }
        }
    }

    /**
     * Setzt das gesamte Panel in den "Schlafmodus" (z.B. zwischen Fragen).
     * Alle Texte werden gelöscht, alles wird inactive.
     */
    public void clearAndSetInactive() {
        for (Button btn : buttons) {
            btn.setText("");
            setButtonState(btn, STATE_INACTIVE);
        }
    }

    /**
     * Zeigt das Ergebnis an (Feedback/Pause Modus).
     * - Der geklickte (falsche) Button wird rot.
     * - Der korrekte Button wird grün.
     * - Alle anderen gehen auf INACTIVE (behalten aber ihren Text!).
     */
    public void setCorrectAndInactive(Collection<Integer> correctIndices) {
        for (int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            
            if (correctIndices.contains(i)) {
                // Button ist Teil der korrekten Lösung
            	setButtonState(btn, STATE_CORRECT);
            } else if (btn.getPseudoClassStates().contains(STATE_INCORRECT)) {
            	//Da machen wir mal nix :-)
            } else {
                // Die restlichen Buttons gehen in den inaktiven visuellen Zustand
                // (Text bleibt stehen, nur CSS ändert sich)
            	setButtonState(btn, STATE_INACTIVE);
            }
        }
    }
    
    // Hilfsmethode für reine Anzeige der Lösung (ohne Fehler)
    public void setCorrect(int index, boolean correct) {
    	setButtonState(buttons.get(index), correct ? STATE_CORRECT : STATE_INCORRECT);
    }

    /**
     * Zentrale State-Maschine.
     * Garantiert, dass IMMER GENAU EIN Zustand aktiv ist (kein "Mischmasch").
     */
    private void setButtonState(Button btn, PseudoClass state) {
        btn.pseudoClassStateChanged(STATE_INACTIVE, state == STATE_INACTIVE);
        btn.pseudoClassStateChanged(STATE_ACTIVE, state == STATE_ACTIVE);
        btn.pseudoClassStateChanged(STATE_CORRECT, state == STATE_CORRECT);
        btn.pseudoClassStateChanged(STATE_INCORRECT, state == STATE_INCORRECT);
    }

    public void addListener(Consumer<Integer> listener) {
        this.listener = listener;
    }
}
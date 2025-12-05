package app.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import app.ui.components.CustomButtonLabel.CLBState;
import app.ui.skin.SkinService;

/**
 * Ursprünglich mit Buttons gelöst. Aber die hatten Hover-Effekt, die nur mittels setEnabled(false)
 * auszuschalten waren. Dann konnte man allerdings bei html-Buttons die Schriftfarbe nicht mehr
 * anpassen. und html brauchen wir zwingend weil bei normalem Text kein Zeilenumbruch möglich.
 * 
 * Disabled Swing components with html formatting use a uniform foreground color taken from the UIManager
 * by key "textInactiveText". The key was introduced as fix for JDK-4783068 - further refinement to allow
 * per-component disabled color rejected in JDK-7150926.  
 */

public class MultipleChoicePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final CustomButtonLabel[] answerLabels;
    private final int maxLabelWidth;
    private CLBState state = CLBState.INACTIVE;
    private Consumer<Integer> listener;
    
    public MultipleChoicePanel(int maxWidth, Font font, Font smallFont, int verticalGap) {
    	this.maxLabelWidth = maxWidth;
        
        setLayout(null);
        //setBackground(Color.RED);
        setOpaque(false);
        
        
        // Button-Höhe ermitteln über Dummy-Button
        JLabel dummyLabel = SkinService.get().createAnswerButton();
        dummyLabel.setText("Q");
        int labelHeight = dummyLabel.getPreferredSize().height;
        
        // Panel-Höhe berechnen
        int panelHeight = (labelHeight * 8) + (verticalGap * 7);
        setSize(maxWidth, panelHeight);
        
        // 8 Buttons erstellen und positionieren
        answerLabels = new CustomButtonLabel[8];
        int yPos = 0;
        
        for (int i = 0; i < 8; i++) {
            answerLabels[i] = SkinService.get().createAnswerButton();
            answerLabels[i].setBounds(0, yPos, maxWidth, labelHeight);
            answerLabels[i].setState(CLBState.DISABLED);
            
            final int index = i;
            MouseAdapter labelListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                	// Kein Check ob Active nötig, da im Pause-Modus auch Clicks auf inaktive Buttons getrackt werden müssen!
                	buttonClicked(index);
                }
            };
            answerLabels[i].addMouseListener(labelListener);            
            add(answerLabels[i]);
            yPos += labelHeight + verticalGap;
        }
    }
    
    /**
     * Ich hatte anfangs Ruckler bei Welt drin, weil das Repaint der Map immer zwischen die
     * Button-repaints gerutscht ist. Dann wurden 3 Buttons gezeichnet. Kurz Pause fürs Bild und
     * dann die restlichen 2 Buttons. Seit wir Dsun.java2d.d3d=false gesetzt haben, fällt das eigentlich
     * nicht mehr ins Gewicht. Aber schaden tut es halt auch nicht, also lassen wir das jetzt drin.
     * 
     * @param answers
     */
    public void initiateMultipleChoice(List<String> answers) {
    	for (int i = 0; i<8; i++) {
			if (i < answers.size()) {
				setText(i, answers.get(i));
				answerLabels[i].setState(CLBState.ACTIVE, false);
			}
			else {
				answerLabels[i].setState(CLBState.DISABLED, false);
			}
		}
    	state = CLBState.ACTIVE;
    	repaint();
    }
    
    public void setState (int i, CLBState state) {
    	answerLabels[i].setState(state);
    }
    
    public void disableAllButtons () {
    	if (state != CLBState.DISABLED) {
    		for (CustomButtonLabel button : answerLabels)
    			button.setState(CLBState.DISABLED, false);
    		state = CLBState.DISABLED;
    		repaint();
    	}
    }
    
    public void inactivateAllActiveButtons () {
    	if (state == CLBState.ACTIVE) {
    		for (CustomButtonLabel button : answerLabels) {
    			if (button.getState() == CLBState.ACTIVE)
    				button.setState(CLBState.INACTIVE, false);
    		}
    		state = CLBState.INACTIVE;
    		repaint();
    	}
    }
    
    public void addListener(Consumer<Integer> listener) {
    	this.listener = listener;
    }
    
    private void buttonClicked(int i) {
    	listener.accept(i);
    }
    
    private void setText(int index, String text) {
        if (index < 0 || index >= 8) return;
        CustomButtonLabel answerLabel = answerLabels[index];
        answerLabel.setTextWithWrapping(text, maxLabelWidth);  
    }
}
package app.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JTextField;

import app.ui.UIUtils;
import app.ui.skin.params.BorderParams;

public class CustomTextField extends JTextField{

	private static final long serialVersionUID = 1L;
	private Color disabledBackgroundColor;
	private Color activeBackgroundColor;
	
	/**
	 * Du kannst die Hintergrundfarbe steuern für disabled / enabled. Ob es den Fokus hat ändert aktuell
	 * an der Hintergrundfarbe nix. K.A. ob ich das mal brauche. Aber für den Border kannst Du alle 3 Farben angeben.
	 * 	
	 * @param font
	 * @param textColor
	 * @param activeBackgroundColor
	 * @param disabledBackgroundColor
	 * @param horizontalAlign
	 * @param columns
	 * @param borderParams
	 * @param margins
	 */
	public CustomTextField (Font font, Color textColor, Color incorrectTextColor, Color activeBackgroundColor, Color disabledBackgroundColor,
			int horizontalAlign, BorderParams borderParams) {
		this.disabledBackgroundColor = disabledBackgroundColor;
		this.activeBackgroundColor = activeBackgroundColor;
		this.setDisabledTextColor(incorrectTextColor);
        this.setFont(font);
        this.setForeground(textColor);
        this.setHorizontalAlignment(horizontalAlign);
        this.putClientProperty("FlatLaf.style", buildTextFieldStyle(borderParams, activeBackgroundColor));
	}
	
	/**
	 * 
	 * @param borderWidth
	 * @param borderColor
	 * @param focusedBorderColor
	 * @param activeBackgroundColor
	 * @param focusWidth
	 * @param borderArc
	 * @param margins
	 * @return
	 */
	
    private String buildTextFieldStyle(BorderParams borderParams, Color activeBackgroundColor) {
    	Insets margins = borderParams.insets();
    	return 
    			"borderWidth:" + borderParams.width() + ";" +
    			"borderColor:" + UIUtils.toHex(borderParams.color()) + ";" +
                "focusedBorderColor:" + UIUtils.toHex(borderParams.focusedColor()) + ";" +
    			"innerFocusWidth:0;" + // Ich hatte sonst nach dem Wechsel von focused zu disabled einen hässlichen inneren Rand. Glaube nicht, dass Du den jemals haben wirst wollen...
                "disabledBorderColor:" + UIUtils.toHex(borderParams.disabledColor()) + ";" +
                "background:" + UIUtils.toHex(activeBackgroundColor) + ";" +
                "focusWidth:" + borderParams.focusWidth() + ";" +
                "arc:" + borderParams.arc() + ";" +
                "margin:"+ margins.top + "," + margins.left + "," + margins.bottom + "," + margins.right + ";";
    }
    
    /**
     * Funktioniert zwar so leidlich, aber schau dir die Ecken dann lieber nicht genau an.
     * Ich kann das Eckenproblem nicht reproduzieren. Kümmern wir uns drum, wenn es wieder auftaucht!
     * disabledBackground → Ist zwar dokumentiert, ignoriert FlatLaf bei JTextFields aber, deswegen der Hack hier.
     */
    @Override
    public void setEnabled(boolean enabled) {
    	if (!enabled ) {
    		setFont(getFont().deriveFont(Font.BOLD));
    		setBackground(disabledBackgroundColor);
    	} else {
    		setFont(getFont().deriveFont(Font.PLAIN));
    		setBackground(activeBackgroundColor);
    	}
    	super.setEnabled(enabled);
    }   
}

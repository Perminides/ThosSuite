package app.ui;

public class DismissedIdeas {

    /**
     * BUttons mit HTML haben im disabled state immer graue Schriftfarbe, es ist nicht änderbar...
     * Disabled Swing components with html formatting use a uniform foreground color taken from
     * the UIManager by key "textInactiveText". The key was introduced as fix for JDK-4783068 - 
     * further refinement to allow per-component disabled color rejected in JDK-7150926.
     * public JButton createButton() {
        JButton button = new JButton();
        button.setFont(font);
        button.setForeground(textColor);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);
        button.putClientProperty("FlatLaf.style", buildButtonStyle());
        return button;
    }
    
    private String buildButtonStyle() {
        int verticalMargin = 10;
        int horizontalMargin = 20;
        
        return "borderWidth:2;" +
               "borderColor:" + toHex(borderColor) + ";" +
               "background:" + toHex(buttonNormalColor) + ";" +
               "hoverBackground:" + toHex(buttonHoverColor) + ";" +
               "hoverBorderColor:" + toHex(borderColor) + ";" +
               "pressedBackground:" + toHex(buttonHoverColor) + ";" +
               "arc:12;" +
               "margin:" + verticalMargin + "," + horizontalMargin + "," + verticalMargin + "," + horizontalMargin + ";";
    }**/
	
	/**
	 * Für die Fragen haben wir kein JLabel genommen, weil das keinen Rahmen-Support bietet. Dann hatten wir ein JLabel in
	 * einem JPanel, was auch funktioniert hat, aber uns zu komplex erschien.. Also auf den KLassiker für die Anzeige von
	 * mehrzeiligem Text:
	 * 
	 * Das funzt leider nicht mit vertikal mittigem Text, das macht eine JTextArea nie, immer nur von oben...
	public class CustomTextArea extends JTextArea{
		private static final long serialVersionUID = 1L;

		public CustomTextArea(int width, int height, Font font, Color textColor, Color displayTextBackgroundColor,
				Insets margins, Color borderColor, int borderWidth, int borderArc) {
	        setFont(font);
	        setForeground(textColor);
	        if (displayTextBackgroundColor.getAlpha() == 0) //Transparent macht im Zusammenspiel mit dem Border alles kaputt.
	        	setOpaque(false);
	        else
	        	setBackground(displayTextBackgroundColor); 
	        setEditable(false);
	        setFocusable(false);
	        setLineWrap(true);
	        setWrapStyleWord(true);
	        setSize(width, height);
	        setBorder(new FlatLineBorder(margins, borderColor, borderWidth, borderArc));
	    }	
	}**/
}

package app.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.formdev.flatlaf.ui.FlatLineBorder;

import app.ui.skin.params.BorderParams;

public class CustomButtonLabel extends JLabel{
	public static enum CLBState {ACTIVE, INACTIVE, CORRECT, INCORRECT, DISABLED}
	private static final long serialVersionUID = 1L;
	private final Color inactiveColor;
	private final Color correctColor;
	private final Color incorrectColor;
	private final Color backgroundColor;
	private final Color disabledColor;
	private final Color hoverColor;
	private final Font font;
	private final Font smallFont;
	private CLBState state;
	
	/**
	 * Buttons gingen nicht wegen: Disabled Swing components with html formatting use a uniform
	 * foreground color taken from the UIManager by key "textInactiveText". The key was introduced
	 * as fix for JDK-4783068 - further refinement to allow per-component disabled color rejected in JDK-7150926.
	 * 
	 * Buttons gingen vermutlich auch einfach nicht, weil wir doch ein paar mehr Status hier benöigen, bzw. andere als so ein normaler Button...
	 * 
	 * Ist in erster Linie für die Buttons in MCPanels gedacht. Kann aber natürlich gern für alle anderen Buttons genutzt werden.
	 * Die brauchen dann natürlich incorrect und correctColor etc. eher seltener...
	 * 
	 * @param font
	 * @param textColor
	 * @param backgroundColor
	 * @param margins
	 * @param borderColor
	 * @param borderWidth
	 * @param borderArc
	 * @param hoverColor
	 * @param inactiveColor optional. Set to null if no state is needed.
	 */
	public CustomButtonLabel(Font font, Font smallFont, Color textColor,
			Color backgroundColor, Color hoverColor,  Color inactiveColor, Color correctColor, Color incorrectColor, Color disabledColor, 
			BorderParams borderParams, 
			ImageIcon icon) {
		this.font = font;
		this.smallFont = smallFont;
		this.inactiveColor = inactiveColor;
		this.correctColor = correctColor;
		this.incorrectColor = incorrectColor;
		this.disabledColor = disabledColor;
		this.backgroundColor = backgroundColor;
		this.hoverColor = hoverColor;
		this.setIcon(icon); //If the value of icon is null, nothing is displayed.
		setFont(font);
        setForeground(textColor);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setBackground(backgroundColor);
        setBorder(new FlatLineBorder(borderParams.insets(), borderParams.color(), borderParams.width(), borderParams.arc()));
        if (borderParams.arc() == 0 || borderParams.color().getAlpha() == 0) 
        	setOpaque(true);
        setSize(getPreferredSize());
        state = CLBState.ACTIVE;
        
        MouseAdapter labelListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
            	if (state == CLBState.ACTIVE) {
            		setBackground(hoverColor);
            	}
            }
            @Override
            public void mouseExited(MouseEvent e) {
            	if (state == CLBState.ACTIVE)
            		setBackground(backgroundColor);
            }
        };
        addMouseListener(labelListener); 
	}
	
	public void setState(CLBState state) {
		setState(state, true);
	}
	
	public void setState(CLBState state, boolean repaint) {
	    this.state = state;
	    switch (state) {
	        case ACTIVE -> setBackground(backgroundColor);
	        case INACTIVE -> setBackground(inactiveColor);
	        case CORRECT -> setBackground(correctColor);
	        case INCORRECT -> setBackground(incorrectColor);
	        case DISABLED -> {  setText("");
	        					setBackground(disabledColor);
	        				}
	    }
	    Point mousePos = getMousePosition();
	    if (state == CLBState.ACTIVE && mousePos != null && contains(mousePos))
    		setBackground(hoverColor);
	    if (repaint)
	    	repaint();
	}	
	
	 /**
     * Eine einfache WordWrap-Methode. Wenn der Text nicht mehr auf eine Zeile passt, wird das Wort umgebrochen.
     * Es gibt keinen Versuch hier die Margins zu verkleinern. Wäre auch schwierig, weil ich kenne ja die Rundung
     * und das ganz Design des Textfeldes nicht, also besser nicht, oder?
     * 
     * @param text
     * @param button
     * @param maxWidth
     * @return
     */
	public void setTextWithWrapping(String text, int maxWidth) {
	    setFont(font); // Reset to normal font
	    
	    List<String> lines = wrapTextToLines(text, maxWidth);
	    
	    // If more than 2 lines: try with small font
	    if (lines.size() > 2 && !getFont().equals(smallFont)) {
	        setFont(smallFont);
	        lines = wrapTextToLines(text, maxWidth);
	    }
	    
	    String htmlText = formatAsHtml(lines);
	    setText(htmlText);
	}

	private List<String> wrapTextToLines(String text, int maxWidth) {
	    String[] words = text.split(" ");
	    List<String> lines = new ArrayList<>();
	    StringBuilder currentLine = new StringBuilder();
	    
	    for (int i = 0; i < words.length; i++) {
	        String word = words[i];
	        String testLine = currentLine.isEmpty() 
	            ? word 
	            : currentLine + " " + word;
	        
	        if (doesTextFit(testLine, maxWidth)) {
	            if (!currentLine.isEmpty()) {
	                currentLine.append(" ");
	            }
	            currentLine.append(word);
	        } else {
	            // Line is full
	            if (!currentLine.isEmpty()) {
	                lines.add(currentLine.toString());
	                currentLine = new StringBuilder(word);
	            } else {
	                // Single word too long
	                lines.add(word);
	            }
	        }
	        
	        // If we're on line 3, append all remaining words
	        if (lines.size() == 2) {
	            for (int j = i + 1; j < words.length; j++) {
	                currentLine.append(" ").append(words[j]);
	            }
	            break;
	        }
	    }
	    
	    if (!currentLine.isEmpty()) {
	        lines.add(currentLine.toString());
	    }
	    
	    return lines;
	}

	private String formatAsHtml(List<String> lines) {
	    if (lines.isEmpty()) return "";
	    if (lines.size() == 1) return "<html>" + lines.get(0) + "</html>";
	    
	    int lineSpacing = -(int)(getFont().getSize() * 0.25);
	    
	    StringBuilder html = new StringBuilder("<html>");
	    for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                html.append("<p align='center'>")
                    .append(lines.get(i))
                    .append("</p>");
            } else {
                html.append("<p align='center' style='margin-top:")
                    .append(lineSpacing)
                    .append("'>")
                    .append(lines.get(i))
                    .append("</p>");
            }
        }
	    html.append("</html>");
	    return html.toString();
	}

	private boolean doesTextFit(String text, int maxWidth) {
	    setText(text);
	    return getPreferredSize().width <= maxWidth;
	}
	
	public CLBState getState() {
		return state;
	}

}

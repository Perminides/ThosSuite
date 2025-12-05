package app.ui.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.ui.FlatLineBorder;

import app.ui.skin.params.BorderParams;

public class CustomTextLabel extends JLabel {
	private static final long serialVersionUID = 1L;
	private boolean ignoreRepaints;

	public CustomTextLabel(Color backgroundColor, BorderParams borderParams, Font font, Color textColor) {
        setBackground(backgroundColor);
        
        // FlatLineBorder malt übrigens den Background, selbst wenn Du opaque auf false setzen würdest. Also zumindest bei arc > 0.
        setBorder(new FlatLineBorder(
            borderParams.insets(),
            borderParams.color(),
            borderParams.width(),
            borderParams.arc()
        ));
        System.out.println(borderParams.insets());
        System.out.println(borderParams.color());
        System.out.println(borderParams.width());
        System.out.println(borderParams.arc());
        
        setFont(font);
        setForeground(textColor);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        // Wenn FlatLineBorder nicht einen Border mit abgerundeten Ecken zeichnet, muss Opaque auf true gesetzt werden...
        if (borderParams.arc() == 0 || borderParams.color().getAlpha() == 0) 
        	setOpaque(true);
        
        setHorizontalAlignment(SwingConstants.LEFT); // !Später konfigurierbar?
    }
    
	public void setText(String text) {
		/**
		 * Swing's HTML-Rendering macht bei Text mit Umbruch zwei Layout-Durchläufe:
		 * 1. Erster Render: HTML-Text wird geparst und umgebrochen, aber noch NICHT vertikal im Label positioniert
		 * 2. Zweiter Render: FlowView.layout berechnet Umbrüche, Text wird neu positioniert
		 * 
		 * Problem: Beide Durchläufe werden gerendert → sichtbares Flackern bei Umbruch.
		 * 
		 * Lösung:
		 * - ignoreRepaints blockiert die 6+ internen repaint()-Aufrufe während HTML-Layout
		 * - SwingUtilities.invokeLater() verzögert unser eigenes repaint() bis HTML-Layout fertig
		 * - Resultat: Nur der finale, korrekt umgebrochene Text wird gerendert
		 * 
		 * Bekanntes Swing-Problem ohne offizielle Lösung, siehe:
		 * https://stackoverflow.com/questions/16227877/how-to-update-a-jcomponent-with-html-without-flickering
		 * https://coderanch.com/t/513635/java/flickering-html-jlabel-text
		 */
	    ignoreRepaints = true;
	    super.setText("<html><div style='text-align:left;'>" + text + "</div></html>");
	    //super.setText("<html>" + text + "</html>");
	    ignoreRepaints = false;
	    SwingUtilities.invokeLater(() -> {repaint();});
	}
    
	@Override
	public void repaint() {
	    if (ignoreRepaints) return;   
	    super.repaint();
	}
    
    public void setNameForDebug(String name) {
    	this.setName(name);
    }
}
package app.ui.components;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import app.ui.skin.params.BorderParams;

public class CustomImageLabel extends JLabel {
	private static final long serialVersionUID = 1L;
	private int arc;
	private int  borderWidth;
	private Color borderColor;
	
	public CustomImageLabel (BorderParams borderParams, Color backgroundColor) {
		super((ImageIcon)null);
		this.arc = borderParams.arc();
		this.borderWidth = borderParams.width();
		this.borderColor = borderParams.color();
		setBackground(backgroundColor);
	}
	
	@Override
	public boolean isOpaque() {
	    return getBackground().getAlpha() == 255;
	}
	
	@Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Shape roundRect = new RoundRectangle2D.Float(
        	    borderWidth / 2f,
        	    borderWidth / 2f,
        	    getWidth() - borderWidth,
        	    getHeight() - borderWidth,
        	    arc,
        	    arc
        	);
        
        // Clip für Icon
        g2.setClip(roundRect);
        
        // Background malen (egal ob transparent oder nicht)
        // Sonst gibt es ganz wilde Artefakte
        // Aus der Doku. Visual artifacts appear in my GUI. → Make sure your custom component fills its painting area completely if it's opaque.
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        super.paintComponent(g2);  // Malt Icon mit Clipping
        
        // 3. Border drüber
        //g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setClip(null);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(borderWidth));
        g2.draw(roundRect);
        
        g2.dispose();
    }
}

package app.ui.components;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class TransparentImageLabel extends JLabel {
	private static final long serialVersionUID = 1L;

	private float alpha;

    public TransparentImageLabel(ImageIcon icon, float alpha) {
        super(icon);
        this.alpha = alpha;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paintComponent(g2);
        g2.dispose();
    }
}
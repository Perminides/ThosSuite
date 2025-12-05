package app.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class BackgroundPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final Image background;

    public BackgroundPanel(String imagePath, Dimension size) {
        super(null); // kein Layout
        this.background = new ImageIcon(imagePath).getImage();
        this.setPreferredSize(size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }
}
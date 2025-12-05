package app.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import app.data.GeoMap;
import app.data.MapShape;
import app.ui.MapElementListener;
import app.ui.skin.params.BorderParams;

/**
 * Es gibt eine Karte als png. Und darauf können unsichtbare Shapes gelegt werden, die ich treffen muss.
 */
public class ImageMapPanel extends JPanel {
	private static final long serialVersionUID = 1L;
		
	private final GeoMap map;
    private final int panelWidth;
    private final int panelHeight;
    private final BorderParams panelBorder;
    private final Rectangle overlayContentBounds;
    
    private final Color correctColor;
    private final Color incorrectColor;
    private final Color markColor;
    private final float shapeStrokeWidth;
    private final Color shapeStrokeColor;
    
    // Offset für die verschobene Karte
    private int offsetX = 0;
    private int offsetY = 0;
    
    // Drag-State
    private Point dragStartPoint;
    private boolean isDragging = false;
    
    private static final int MINI_MAP_INSET = 10;
    private Rectangle miniMapBounds; // Bounds der Mini-Map für Click-Detection#
    
    private MapElementListener listener;
    private final ShapesWrapper cardShapes;

    private boolean active = false;
    private Point lastClicked = null; 
    
    public ImageMapPanel(GeoMap map, int width, int height, 
                         BorderParams panelBorder,
                         Color correctColor, Color incorrectColor, Color markColor,
                         float shapeStrokeWidth, Color shapeStrokeColor,
                         BorderParams miniMapBorder, Rectangle overlayContentBounds) {
    	this.overlayContentBounds = overlayContentBounds;
        this.map = map;
        this.panelWidth = width;
        this.panelHeight = height;
        this.panelBorder = panelBorder;
        this.correctColor = correctColor;
        this.incorrectColor = incorrectColor;
        this.markColor = markColor;
        this.shapeStrokeWidth = shapeStrokeWidth;
        this.shapeStrokeColor = shapeStrokeColor;

        cardShapes = new ShapesWrapper(map);
        
        setSize(new Dimension(width, height));
        //setBorder(new FlatLineBorder(new Insets(0, 0, 0, 0), panelBorder.color(), panelBorder.width(), panelBorder.arc()));
        setBackground(new Color(0, 0, 0, 0));
        setOpaque(false);
        
        setupDragListeners();
        
        // Initiale Position: Karte zentriert
        centerMap();
    }
    
    public void setListener (MapElementListener listener) {
    	this.listener = listener;
    }
    
    public void addToCorrect(Set<String> elements) {
    	for (String id : elements)
			cardShapes.addToCorrect(id);
    	if (!areShapesFullyVisible(elements)) { // !SuiteLogik  Karte wird unter Umständen verschoben obwohl dann direkt die nächste Karte kommt (Bei Russland z. B.) Nur bei großen Shapes, das ist dann halt so?
    		Point2D center = getCenterOfShapes(elements);
    		centerOnPoint((int)center.getX(), (int)center.getY());
    	}
    	repaint();
    }
    
	public void setMarked(Set<String> elements) {
		cardShapes.setToMarkShapes(elements);
    	if (!areShapesFullyVisible(elements)) { 
    		Point2D center = getCenterOfShapes(elements);
    		centerOnPoint((int)center.getX(), (int)center.getY());
    	}
    	repaint();
	}
    
    public void markLastClickAsIncorrect() {
    	cardShapes.setToIncorrect(lastClicked);
    	repaint();
    }
    
    public void setActive(boolean active) {
    	if (active != this.active) {
    		this.active = active;
    		repaint();
 		}
    }
    
    public void resetMarkers() {
    	cardShapes.reset();
    	repaint();
    }
    

    public void setToCheckShapes(Set<String> idsInQuestion) {
        cardShapes.setToCheckShapes(idsInQuestion);
    }
    
    private void setupDragListeners() {
        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Prüfe ob Klick auf Mini-Map
                if (miniMapBounds != null && miniMapBounds.contains(e.getPoint())) {
                    handleMiniMapClick(e);
                    return; // Kein Drag starten
                }
                isDragging = false; // Reset
                dragStartPoint = e.getPoint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint == null) return;
                
                double dx = e.getX() - dragStartPoint.getX();
                double dy = e.getY() - dragStartPoint.getY();
                double distance = Math.sqrt(dx*dx + dy*dy);
                
                // Erst ab 5 Pixel Bewegung als echtes Drag werten
                if (!isDragging && distance < 5) {
                    return; // Noch innerhalb Toleranz, ignorieren
                }
                
                isDragging = true;
                
                offsetX += dx;
                offsetY += dy;
                dragStartPoint = e.getPoint();
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
            	if (!isDragging) {
                    // War ein Click, kein Drag!
                    if (listener == null)
                    	return;
                    if (miniMapBounds != null && miniMapBounds.contains(e.getPoint()) && isOpaquePixelAt(map.getOverlayImage(), e.getPoint().x - miniMapBounds.x, e.getPoint().y - miniMapBounds.y)) {
                        handleMiniMapClick(e);
                        return; // Kein Drag starten
                    }
                    MapShape shape = findShapeAt(e.getPoint());
                    lastClicked = e.getPoint();
    		        listener.mouseClicked(shape == null ? null : shape.id());
                }
                dragStartPoint = null;
            }
        };
        
        addMouseListener(dragAdapter);
        addMouseMotionListener(dragAdapter);
    }
    
    private boolean isOpaquePixelAt(BufferedImage image, int x, int y) {
        
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) 
            return false;
        
        int pixel = image.getRGB(x, y);
        int alpha = (pixel >> 24) & 0xff;
        return alpha > 128; // Threshold für "opaque"
    }
    
    private void centerMap() {
        BufferedImage img = map.getBackgroundImage();
        if (img == null) return;
        
        // Zentriere Karte initial
        offsetX = (panelWidth - img.getWidth()) / 2;
        offsetY = (panelHeight - img.getHeight()) / 2;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int borderWidth = panelBorder.width();
        int arc = panelBorder.arc();
        
        // Clipping auf abgerundetes Rechteck
        Shape clip = new RoundRectangle2D.Float(
            borderWidth / 2f, // !Später: Magic Numbers
            borderWidth / 2f,
            w - borderWidth,
            h - borderWidth,
            arc,
            arc
        );
        g2.setClip(clip); 
        
        // Hauptbild rendern
        BufferedImage img;
        if (active)
        	img = map.getBackgroundImage();
        else
        	img = map.getInactiveImage();
        if (img != null) {
            g2.drawImage(img, offsetX, offsetY, null);
        }
        
		for (MapShape shape : cardShapes.getMarkedShapes()) {
			g2.translate(offsetX, offsetY);
			g2.setColor(markColor);
			g2.setStroke(new BasicStroke(4*shapeStrokeWidth)); // !Später: Magic Numbers
			g2.setClip(shape.shape());
			g2.draw(shape.shape());
			g2.setClip(null);
			g2.setColor(shapeStrokeColor);
			g2.setStroke(new BasicStroke(shapeStrokeWidth));
			g2.draw(shape.shape());
			g2.translate(-offsetX, -offsetY);
		}
		
		for (MapShape shape : cardShapes.getCorrectShapes()) {
			drawShapeOnMap(g2, shape, correctColor, shapeStrokeColor, shapeStrokeWidth);
		}
		
        MapShape incorrectShape = cardShapes.getIncorrectShape();
        if (incorrectShape != null) {
        	drawShapeOnMap(g2, incorrectShape, incorrectColor, shapeStrokeColor, shapeStrokeWidth);
		}
        
        // Mini-Map rendern (oben rechts mit Inset)
        BufferedImage overlayImage;
        if (active)
        	overlayImage = map.getOverlayImage();
        else
        	overlayImage = map.getInactiveOverlayImage();
        if (overlayImage != null) {
            int miniMapX = w - overlayImage.getWidth() - MINI_MAP_INSET;
            int miniMapY = MINI_MAP_INSET;
            g2.drawImage(overlayImage, miniMapX, miniMapY, null);
            
            // Bounds speichern für Click-Detection
            miniMapBounds = new Rectangle(miniMapX, miniMapY, 
                                          overlayImage.getWidth(), 
                                          overlayImage.getHeight());
        } else {
            miniMapBounds = null;
        }
        
        // Clip zurücksetzen für Border
        g2.setClip(null);
        
        // Border selbst zeichnen
        g2.setStroke(new BasicStroke(borderWidth));
        g2.setColor(panelBorder.color());
        g2.drawRoundRect(
            borderWidth / 2, // !Später: Magic Numbers
            borderWidth / 2,
            w - borderWidth,
            h - borderWidth,
            arc,
            arc
        );
    }
    private void drawShapeOnMap (Graphics2D g2, MapShape shape, Color shapeColor, Color borderColor, float borderWidth) {
    	g2.translate(offsetX, offsetY);
		g2.setColor(shapeColor);
		g2.fill(shape.shape());
		g2.setColor(borderColor); 
        g2.setStroke(new BasicStroke(borderWidth));
        g2.draw(shape.shape());
        g2.translate(-offsetX, -offsetY);
    }
    
    private void handleMiniMapClick(MouseEvent e) {
        BufferedImage mainImage = map.getBackgroundImage();
        BufferedImage overlayImage = map.getOverlayImage();
        if (mainImage == null || overlayImage == null) return;
        
        // Klick-Position relativ zur Mini-Map
        int miniMapX = e.getX() - miniMapBounds.x;
        int miniMapY = e.getY() - miniMapBounds.y;
        
     // Prüfe ob Pixel transparent ist (auch minimal transparent wegen Schatten)
        int rgb = overlayImage.getRGB(miniMapX, miniMapY);
        int alpha = (rgb >> 24) & 0xFF;
        if (alpha < 255) {
            // Transparenter/halbtransparenter Pixel → behandle als normalen Drag-Start
            dragStartPoint = e.getPoint();
            return;
        }

        // Koordinate relativ zum Content (ohne Schatten-Rand)
        int contentX = miniMapX - overlayContentBounds.x;
        int contentY = miniMapY - overlayContentBounds.y;

        // Skalierungsfaktor Content → Große Map
        double scaleX = (double) mainImage.getWidth() / overlayContentBounds.width;
        double scaleY = (double) mainImage.getHeight() / overlayContentBounds.height;

        // Umrechnung auf große Map-Koordinaten
        int mainMapX = (int) (contentX * scaleX);
        int mainMapY = (int) (contentY * scaleY);

        // Zentriere große Map auf diesen Punkt
        centerOnPoint(mainMapX, mainMapY);
    }
    
    // Später: Methode für Mini-Map-Click (zentriert große Karte auf Klickpunkt)
    private void centerOnPoint(int x, int y) {
        BufferedImage img = map.getBackgroundImage();
        if (img == null) return;
        
        // Berechne neuen Offset so dass (x,y) in Bildmitte ist
        offsetX = panelWidth / 2 - x;
        offsetY = panelHeight / 2 - y;
        repaint();
    }
    
	private MapShape findShapeAt(Point mousePoint) {
		Point translated = new Point(mousePoint.x - offsetX, mousePoint.y - offsetY);
		for (MapShape shape : cardShapes.getToCheckShapes()) {
			if (shape.shape().contains(translated)) {
				return shape;
			}
		}
		return null;
	}
    
    private Point2D getCenterOfShapes(Set<String> shapes) {
        if (shapes == null || shapes.isEmpty()) return null;

        double sumX = 0;
        double sumY = 0;
        int count = 0;

        for (String s : shapes) {
            Rectangle2D b = cardShapes.get(s).shape().getBounds2D();
            sumX += b.getCenterX();
            sumY += b.getCenterY();
            count++;
        }

        return new Point2D.Double(sumX / count, sumY / count);
    }
    
    private boolean areShapesFullyVisible(Set<String> shapesToCheck) {
        if (shapesToCheck == null || shapesToCheck.isEmpty()) return false;
        
        // Sichtbarer Bereich in Map-Koordinaten
        Rectangle visibleArea = new Rectangle(
            -offsetX,
            -offsetY,
            panelWidth,
            panelHeight
        );
        
        for (String shapeID : shapesToCheck) {
            Rectangle2D shapeBounds = cardShapes.get(shapeID).shape().getBounds2D();
            if (!visibleArea.contains(shapeBounds)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Diese Klasse wrappt die eigentlichen Shapes vor dem Rest der Klasse weg (paintComponent mal ausgenommen).
     * Somit kann im Rest der Klasse konsequent nur mit ID's gearbeitet werden 
     */
    private class ShapesWrapper {
    	private final GeoMap map;
        private Set<MapShape> toCheckShapes = new HashSet<>();
        private Set<MapShape> correctShapes = new HashSet<>();
        private Set<MapShape> markedShapes = new HashSet<>();
        private MapShape incorrectShape = null;
        
        private ShapesWrapper(GeoMap map) {
        	this.map = map;
        }
    	
    	private void reset() {
    		toCheckShapes.clear();
    		correctShapes.clear();
    		markedShapes.clear();
    		incorrectShape = null;
    	}
    	
    	private void setToCheckShapes(Set<String> ids) {
    		toCheckShapes.clear();
    		for (String id : ids)
    			toCheckShapes.add(get(id));
    	}
    	
    	private void setToMarkShapes(Set<String> ids) {
    		markedShapes.clear();
    		for (String id : ids)
    			markedShapes.add(get(id));
    	}
    	
    	private void addToCorrect(String id) {
    		correctShapes.add(get(id));
    	}
    	
    	private Set<MapShape> getToCheckShapes() {
    		return toCheckShapes;
    	}
    	
    	private Set<MapShape> getMarkedShapes() {
    		return markedShapes;
    	}
    	
    	private Set<MapShape> getCorrectShapes() {
    		return correctShapes;
    	}
    	
    	private MapShape getIncorrectShape() {
    		return incorrectShape;
    	}
    	
    	private void setToIncorrect(Point wrongClick) {
    		Point translated = new Point(wrongClick.x - offsetX, wrongClick.y - offsetY);
    		incorrectShape = map.createCircle(GeoMap.CircleSizes.SMALL, translated.x, translated.y);
    	}
    	
    	private MapShape get(String id) {
    		return map.getShape(id);
    	}

    }
}
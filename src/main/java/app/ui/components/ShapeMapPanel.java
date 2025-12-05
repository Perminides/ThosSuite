package app.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import app.data.GeoMap;
import app.data.MapShape;
import app.ui.MapElementListener;

/**
 * Die Karte besteht einfach aus den einzelnen Shapes zusammengesetzt.
 * Eventuell noch ein Hintergrundbild für das Äußere...
 */
public class ShapeMapPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final Color borderColor;
    private final Color hoverColor;
    private final Color activeColor;
    private final Color correctColor;
    private final Color incorrectColor;
    private final Color inactiveColor;
    private final Color markedColor;
    private final Color decoColor0;
    private final Color decoColor1;
    private final float shapeBorderWidth;
    private final float bigShapeBorderWidth;
    private final boolean hover3D;
    private final GeoMap map;
    private String hoverIndex = "";
    private Map<String, Area> regions = new HashMap<>();
    private float paddingScale;
    private int xMove, yMove;
    private MapElementListener listener;
    private Set<String> activeShapes = new HashSet<>();
    private Set<String> correctShapes = new HashSet<>();
    private Set<String> incorrectShapes = new HashSet<>();
    private Set<String> markedShapes = new HashSet<>();
    private Set<String> fixColorShapes_0 = new HashSet<>();
    private Set<String> fixColorShapes_1 = new HashSet<>();
    private boolean active = false;
    private boolean drawRegionShapes = false;
    
    public ShapeMapPanel(GeoMap map, int height, Color activeColor, Color borderColor, Color hoverColor,
    		Color correctColor, Color incorrectColor, Color inactiveColor, Color markedColor, float shapeBorderWidth,
    		float bigShapeBorderWidth, int yPadding, boolean hover3D, Color c0, Color c1) {
    	this.map = map;
        this.borderColor = borderColor;
        this.hoverColor = hoverColor;
        this.activeColor = activeColor;
        this.correctColor = correctColor;
        this.incorrectColor = incorrectColor;
        this.inactiveColor = inactiveColor;
        this.markedColor = markedColor;
        this.decoColor0 = c0;
        this.decoColor1 = c1;
        this.shapeBorderWidth = shapeBorderWidth;
        this.bigShapeBorderWidth = bigShapeBorderWidth;
        this.hover3D = hover3D;
        
		/**
		 * Entfernt "toten Raum" links und oben, indem alle Shapes so verschoben werden,
		 * dass der am weitesten links liegende Punkt bei X=0 und der am weitesten
		 * oben liegende Punkt bei Y=0 liegt. Erleichtert Skalierung und Rendering.
		 * 
		 * Ist ein bisschen ein Hack, damit die Deutschlandkarte keinen Raum drum herum hat.
		 * Wenn die auch mal ein Hintergrundbild bekommt, ist das hier vielleicht auch überflüssig.
		 * !Erweiterung: Würde Deutschlandkarte nicht vielleicht auch besser aussehen mit was drum herum?
		 * !Sofort: Ich habe die Trandorm rausgenommen, jetzt muss Deutschland für den Moment größer und weiter nach links und oben...
		 * !Sofort: Die destatis germany.geojson → 1. in Qgis Simplify coverage mit 500 oder 1000 oder 5000, probier rum. Dann normalisieren auf (0,0) und den Wertebereich auf ne Box von k. A. 5000 x 5000? Aktuell haste ja nur 800 x ... oder so. Damit sollte die Perfgormance super sein und du hast eine recht nice Karte...
		 */
        /**
		// 1. Finde Min-Werte
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;

		for (MapShape shape : map.getShapes()) {
			Rectangle2D bounds = shape.shape().getBounds2D();
			minX = Math.min(minX, bounds.getX());
			minY = Math.min(minY, bounds.getY());
		}

		// 2. Verschiebe alle Shapes
		AffineTransform shift = AffineTransform.getTranslateInstance(-minX, -minY);		
		List<MapShape> transformed = map.getShapes().stream()
			    .map(s -> new MapShape(
			        s.id(), s.deckId(), s.regionName(), s.capitalName(),
			        s.altRegionNames(), s.altCapitalNames(), s.fixedColorSet(),
			        shift.createTransformedShape(s.shape())  // neues Shape
			    ))
			    .toList();
			map.setShapes(transformed);
        **/
        setLayout(null);
        setOpaque(false);
        
		// Hover-Detection
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				String newHover = findShapeAt(e.getPoint());
			    if (newHover.equals("")) return;
			    
			    if (!newHover.equals(hoverIndex) && isNotClicked(newHover)) {
			        hoverIndex = newHover;
			        repaint();
			    }
			}
		});
		
		addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseExited(MouseEvent e) {
		        // Maus hat Panel verlassen → Hover löschen
		    	hoverIndex = "";
		    	repaint();
		    }
		    
		    @Override
		    public void mousePressed(MouseEvent e) {
		        if (listener == null)
		        	return;
		        String clickedShape = findShapeAt(e.getPoint());
		        if (!clickedShape.equals("")) {
		            listener.mouseClicked(clickedShape);
		        }
		    }
		});
		
		for (MapShape shape : map.getShapes()) {
			if (shape.isDecoration())
				if (shape.fixedColorSet().equals("0"))
					fixColorShapes_0.add(shape.id());
				else
					fixColorShapes_1.add(shape.id());
		}
		
		// Panelgröße anhand Shapes berechnen. Regions-Shapes erstellen
		// !Sofort: Das add ist teuer! Mach DekoShapes oder Lines direkt im geojson!
		Rectangle bounds = new Rectangle();
		int index = 0;
		for (MapShape s : map.getShapes()) {
			index++;
			System.out.println(index + ": " + LocalDateTime.now());
			regions.computeIfAbsent(s.deckId(), _ -> new Area()).add(new Area(s.shape()));
			bounds = bounds.union(s.shape().getBounds());
		}
		int w = bounds.x + bounds.width;
		int h = bounds.y + bounds.height;
		paddingScale = (float) (height - yPadding) / h;
		float origScale = (float) (height) / h;
		int xPadding = (int)(w * origScale - w * paddingScale);
		setSize(new Dimension(Math.round(origScale * w), Math.round(origScale * h)));
		xMove = Math.round(xPadding / 2)+1;
		yMove = yPadding / 2;
    }
    
    public void setListener (MapElementListener listener) {
    	this.listener = listener;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
	    Graphics2D g2 = (Graphics2D) g;
	    
	    g2.scale(paddingScale, paddingScale);
	    g2.translate(xMove, yMove);
	    
	    // NUR diese Hints:
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
	                        RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, 
	                        RenderingHints.VALUE_STROKE_NORMALIZE); // NICHT PURE!
	    g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
                RenderingHints.VALUE_RENDER_QUALITY);
	    
	    // Shapes zeichnen
	    for (MapShape shape : map.getShapes()) {
	    	if (incorrectShapes.contains(shape.id()))
	    		g2.setColor(incorrectColor);
	    	else if (correctShapes.contains(shape.id()))
	    		g2.setColor(correctColor);
	    	else if (markedShapes.contains(shape.id()))
	    		g2.setColor(markedColor);
	    	else if (fixColorShapes_0.contains(shape.id()))
	    		g2.setColor(decoColor0);
	    	else if (fixColorShapes_1.contains(shape.id()))
	    		g2.setColor(decoColor1);
	    	else if (!active || !activeShapes.contains(shape.id()))
	            g2.setColor(inactiveColor);
	    	else if (shape.id().equals(hoverIndex))
	            g2.setColor(hoverColor);
	    	else
				g2.setColor(activeColor);
	        g2.fill(shape.shape());
	        
	        g2.setColor(borderColor); 
	        g2.setStroke(new BasicStroke(shapeBorderWidth));
	        g2.draw(shape.shape());
	        if (shape.id().equals(hoverIndex) && hover3D && active && isNotClicked(shape.id())) {
	            drawHoverEffect(g2, shape, new int[] {200, 150, 100, 50}, true);
	        }
	    }
	    
	    // Bundesländer
	    if (drawRegionShapes)
	    	for (Area bl : regions.values()) {
	    		g2.setColor(borderColor);
	    		g2.setStroke(new BasicStroke(bigShapeBorderWidth));
	    		g2.draw(bl);
	    	}
    }
    
    public void addToCorrect(Set<String> elements) {
    	System.out.println("Add to correct: " + elements);
    	incorrectShapes.removeAll(elements);
    	markedShapes.removeAll(elements);
    	activeShapes.removeAll(elements);
    	correctShapes.addAll(elements);
    }
    
    public void addToIncorrect(String element) {
    	System.out.println("Add to incorrect: " + element);
    	correctShapes.remove(element);
    	activeShapes.remove(element);
    	markedShapes.remove(element);
    	incorrectShapes.add(element);
    }
    
    public void addToMarked(Set<String> elements) {
    	System.out.println("Add to marked: " + elements);
    	correctShapes.removeAll(elements);
    	incorrectShapes.removeAll(elements);
    	activeShapes.removeAll(elements);
    	markedShapes.addAll(elements);
    }
    
	public void moveAllToActive() {
		System.out.println("Move all to active");
		activeShapes.addAll(correctShapes);
		activeShapes.addAll(incorrectShapes);
		activeShapes.addAll(markedShapes);
		correctShapes.clear();
		incorrectShapes.clear();
		markedShapes.clear();
	}
	
	public void moveCorrectToActive() {
		System.out.println("Move correct to active");
		activeShapes.addAll(correctShapes);
		correctShapes.clear();
	}
	
    public void makeEveryShapeActive() { // Initialisierung Anki
    	for (MapShape shape : map.getShapes())
    		activeShapes.add(shape.id());
    	incorrectShapes = new HashSet<>();
    	correctShapes = new HashSet<>();
    	markedShapes = new HashSet<>();
    	repaint();
    }
    
    public void makeActive(Set<String> activeIds) { // Initialisiserung Region-Click und resume im selben Modus...
    	activeShapes.addAll(activeIds);
    	correctShapes.removeAll(activeIds);
    	incorrectShapes.removeAll(activeIds);
    	markedShapes.removeAll(activeIds);
    }
    
    public void setActive(boolean active) {
    	this.active = active;
    }
    
	public void setState(ShapeMapState state) {
		this.activeShapes = state.activeShapes();
		this.markedShapes = state.markedShapes();
		this.correctShapes = state.correctShapes();
		this.incorrectShapes = state.incorrectShapes();
		this.active = state.interactive();
	}
    
    public ShapeMapState getState() {
    	return new ShapeMapState(
    			new HashSet<>(correctShapes),
    			new HashSet<>(incorrectShapes),
    			new HashSet<>(markedShapes),
    			new HashSet<>(activeShapes),
    			active);
    }
    
    public void drawRegionShapes() {
    	drawRegionShapes = true;
    }
    
    private boolean isNotClicked(String shapeId) {
    	return !incorrectShapes.contains(shapeId) 
    	        && !correctShapes.contains(shapeId);
    }
    
    private Point2D.Double transformMousePoint(Point mousePoint) {
        return new Point2D.Double(
            mousePoint.x / paddingScale - xMove, 
            mousePoint.y / paddingScale - yMove
        );
    }

    private String findShapeAt(Point mousePoint) {
        Point2D.Double transformed = transformMousePoint(mousePoint);
        for (MapShape shape : map.getShapes()) {
            if (shape.shape().contains(transformed)) {
                return shape.id();
            }
        }
        return "";
    }
    
    /**
     * Malt zu dem aktuellen Shape einen Schatten
     * 
     * @param g2
     * @param shape
     * @param alphas
     * @param clipToShape: Setze das auf true, wenn der Schatten nur innerhalb des Shapes sichtbar sein soll (Hover)
     */
    private void drawHoverEffect(Graphics2D g2, MapShape shape, int[] alphas, boolean clipToShape) {
        Graphics2D gCopy = (Graphics2D) g2.create();
        if (clipToShape)
        	gCopy.clip(shape.shape());
        gCopy.setStroke(new BasicStroke(1f));
        for (int alpha : alphas) {
            gCopy.setColor(new Color(
                borderColor.getRed(), 
                borderColor.getGreen(), 
                borderColor.getBlue(), 
                alpha
            ));
            gCopy.translate(1, 1);
            gCopy.draw(shape.shape());
        }
        
        gCopy.dispose();
    }
}
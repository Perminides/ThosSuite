package app.ui.components;

import java.util.Set;

import app.data.GeoMap;
import app.data.MapShape;
import app.ui.MapElementListener;
import app.ui.skin.params.BorderParams;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * A map that is basically a very large image. The image can be dragged and dropped. Shapes can be placed on the
 * image. They are invisible when a click on them is expected and after the click they are shown as correct.
 * Shapes can also be marked (What mountain range is this?). Shapes consist of two Paths organized in a group.
 * A shape can be a polygon (countries) or a line (rivers).
 * 
 * ImageMapPane
 * |-- Pane viewport (clipped und mit prefSize)
 *     |-- Group contentGroup
 *         |-- ImageView
 *         |-- Group shapeLayer
 *      |-- ImageView miniMap
 *      |-- Region borderOverlay
 *      
 * CSS:
 * 		ImageMapPane	= ".image-map-pane",
 * 		borderOverlay	= "#borderOverlay"
 * 		Shape (group)	= ".image-map-shape", ".river"
 * 		Shape (group)	= ":correct", ":incorrect", ":marked"
 * 		Shape (path)	= ".first", ".second"
 */
public class ImageMapPane extends StackPane {

	// Pseudo-Klassen für CSS
	private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
	private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
	private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");

	private final GeoMap map;
	private final java.awt.Rectangle overlayContentBounds;

	// UI Nodes
	private final Pane viewport;
	private final Group contentGroup;
	private final ImageView mainImageView;
	private final Group shapeLayer;
	private final ImageView miniMapImageView;
	private Point2D lastClick;
	private final Region borderOverlay;

	// Logic
	private final ShapesWrapper cardShapes;
	private MapElementListener listener;
	private boolean active = false;

	// Panning State
	private double dragStartX, dragStartY;
	private double initialTranslateX, initialTranslateY;
	private boolean isDragging = false;

	// Mini-Map Config
	private static final int MINI_MAP_INSET = 10;

	public ImageMapPane(GeoMap map, int width, int height, BorderParams panelBorder, java.awt.Rectangle overlayContentBounds) {
		this.map = map;
		this.overlayContentBounds = overlayContentBounds;
		this.cardShapes = new ShapesWrapper();
		this.getStyleClass().add("image-map-pane");

		// 1. Größe fixieren
		setPrefSize(width, height);
		setMaxSize(width, height);

		// 2. Main Content aufbauen
		mainImageView = new ImageView();
		mainImageView.setId("mainImageView");
		shapeLayer = new Group();
		shapeLayer.setId("shapeLayer");
		contentGroup = new Group(mainImageView, shapeLayer);
		contentGroup.setId("contentGroup");

		// 3. Viewport mit Clipping
		viewport = new Pane(contentGroup); // Einfach weglassen? → Eine StackPane ist ein Layout-Manager. Sie versucht zwanghaft, ihre Kinder (Children) zu positionieren – standardmäßig zentriert in der Mitte. Wenn du später versuchst, deine contentGroup (die Karte) zu verschieben (Panning/Verschieben mit der Maus), kämpfst du gegen die StackPane. Eine reine Pane macht kein Layout-Management. Sie sagt: "Setz deine Kinder hin, wo du willst (x, y), mir egal".
		viewport.setId("viewport");
		//viewport.setPrefSize(width, height); // Unnötig

		Rectangle clipRect = new Rectangle(width, height);
		clipRect.setArcWidth(panelBorder.arc());
		clipRect.setArcHeight(panelBorder.arc());
		clipRect.setId("clipRect");
		viewport.setClip(clipRect);

		// 4. Mini-Map
		miniMapImageView = new ImageView();
		miniMapImageView.setId("miniMapImageView");
		StackPane.setAlignment(miniMapImageView, Pos.TOP_RIGHT);
		StackPane.setMargin(miniMapImageView, new javafx.geometry.Insets(MINI_MAP_INSET));

		// 5. Border Overlay
		borderOverlay = new Region();
		borderOverlay.setId("borderOverlay");
		borderOverlay.setMouseTransparent(true);

		getChildren().addAll(viewport, miniMapImageView, borderOverlay);

		updateImages();
		setupInteraction();
		centerMap();
	}

	private void centerMap() {
		if (mainImageView.getImage() == null)
			return;

		double imgW = mainImageView.getImage().getWidth();
		double imgH = mainImageView.getImage().getHeight();

		centerOnPoint(imgW / 2.0, imgH / 2.0);
	}

	private void setupInteraction() {
		viewport.setOnMousePressed(e -> {
			dragStartX = e.getSceneX();
			dragStartY = e.getSceneY();
			initialTranslateX = contentGroup.getTranslateX();
			initialTranslateY = contentGroup.getTranslateY();
			isDragging = false;
		});

		viewport.setOnMouseDragged(e -> {
			double deltaX = e.getSceneX() - dragStartX;
			double deltaY = e.getSceneY() - dragStartY;

			if (!isDragging && Math.sqrt(deltaX * deltaX + deltaY * deltaY) < 5) {
				return;
			}
			isDragging = true;

			contentGroup.setTranslateX(initialTranslateX + deltaX);
			contentGroup.setTranslateY(initialTranslateY + deltaY);
		});

		viewport.setOnMouseClicked(e -> {
		    if (!isDragging && listener != null) {
		        Point2D localPoint = contentGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
		        lastClick = localPoint;
		        listener.mouseClicked(null);
		    }
		});

		miniMapImageView.setOnMouseClicked(this::handleMiniMapClick);
	}

	private void handleMiniMapClick(MouseEvent e) {
	    Image overlay = miniMapImageView.getImage();
	    if (overlay == null)
	        return;

	    int x = (int) e.getX();
	    int y = (int) e.getY();
	    
	    // Bounds-Check
	    if (x < 0 || x >= overlay.getWidth() || y < 0 || y >= overlay.getHeight()) {
	        return;
	    }

	    // Transparenz-Check
	    PixelReader reader = overlay.getPixelReader();
	    if (reader.getColor(x, y).getOpacity() < 1) {
	        return;
	    }

	    double contentX = e.getX() - overlayContentBounds.x;
	    double contentY = e.getY() - overlayContentBounds.y;

	    double scaleX = mainImageView.getImage().getWidth() / overlayContentBounds.width;
	    double scaleY = mainImageView.getImage().getHeight() / overlayContentBounds.height;

	    double mainMapX = contentX * scaleX;
	    double mainMapY = contentY * scaleY;

	    centerOnPoint(mainMapX, mainMapY);
	}

	private void centerOnPoint(double x, double y) {
		double vpCenterX = getWidth() / 2;
		double vpCenterY = getHeight() / 2;

		if (vpCenterX == 0)
			vpCenterX = getPrefWidth() / 2;
		if (vpCenterY == 0)
			vpCenterY = getPrefHeight() / 2;

		contentGroup.setTranslateX(vpCenterX - x);
		contentGroup.setTranslateY(vpCenterY - y);
	}

	private void handleShapeClick(String id) {
		if (listener != null) {
			listener.mouseClicked(id);
		}
		// Marker-Position wird NICHT hier gesetzt - das macht der Presenter
		// bei falschem Klick via markLastClickAsIncorrect()
	}

	public void setListener(MapElementListener listener) {
		this.listener = listener;
	}

	public void setActive(boolean active) {
		if (this.active != active) {
			this.active = active;
			updateImages();
		}
	}

	private void updateImages() {
		mainImageView.setImage(active ? map.getBackgroundImage() : map.getInactiveImage());
		miniMapImageView.setImage(active ? map.getOverlayImage() : map.getInactiveOverlayImage());
	}

	public void setToCheckShapes(Set<String> ids) {
		cardShapes.setToCheck(ids);
	}

	public void addToCorrect(Set<String> elements) {
		cardShapes.addToCorrect(elements);
		if (!areShapesFullyVisible(elements)) {
			Point2D center = getCenterOfShapes(elements);
			if (center != null) {
				centerOnPoint(center.getX(), center.getY());
			}
		}
	}

	public void setMarked(Set<String> elements) {
		cardShapes.setMarked(elements);
		if (!areShapesFullyVisible(elements)) {
			Point2D center = getCenterOfShapes(elements);
			if (center != null) {
				centerOnPoint(center.getX(), center.getY());
			}
		}
	}

	public void markLastClickAsIncorrect() {
	    Point2D pos = new Point2D(lastClick.getX(), lastClick.getY());
	    cardShapes.setIncorrect(pos);
	}

	public void resetMarkers() {
		cardShapes.reset();
	}

	private Point2D getCenterOfShapes(Set<String> shapeIds) {
	    if (shapeIds == null || shapeIds.isEmpty()) {
	        return null;
	    }

	    double sumX = 0, sumY = 0;
	    int count = 0;

	    for (String id : shapeIds) {
	        Node node = cardShapes.getNode(id);
	        if (node != null) {
	        	// Wir nutzen getBoundsInParent(), weil wir die effektive Position im Koordinatensystem 
	        	// des Parents (shapeLayer) benötigen.
	        	// Auch wenn aktuell keine Transforms (Scale/Translate) auf den Shapes liegen,
	        	// wäre getBoundsInLocal() falsch, sobald wir z.B. Pulsier-Effekte (Scale) 
	        	// oder Korrektur-Offsets hinzufügen. Parent-Bounds sind "What you see is what you get".
	            Bounds b = node.getBoundsInParent(); // ← HIER!
	            double centerX = b.getMinX() + b.getWidth() / 2.0;
	            double centerY = b.getMinY() + b.getHeight() / 2.0;	            
	            sumX += centerX;
	            sumY += centerY;
	            count++;
	        }
	    }

	    if (count == 0) return null;
	    
	    Point2D result = new Point2D(sumX / count, sumY / count);
	    return result;
	}

	private boolean areShapesFullyVisible(Set<String> shapeIds) {
		if (shapeIds == null || shapeIds.isEmpty())
			return false;

		// Sichtbarer Bereich in Map-Koordinaten
		double translateX = contentGroup.getTranslateX();
		double translateY = contentGroup.getTranslateY();

		double viewportWidth = getWidth() > 0 ? getWidth() : getPrefWidth();
		double viewportHeight = getHeight() > 0 ? getHeight() : getPrefHeight();

		// Visible area in content coordinates
		double visibleMinX = -translateX;
		double visibleMinY = -translateY;
		double visibleMaxX = visibleMinX + viewportWidth;
		double visibleMaxY = visibleMinY + viewportHeight;

	    for (String id : shapeIds) {
	        Node node = cardShapes.getNode(id);
	        if (node != null) {
	            Bounds b = node.getBoundsInParent(); // ← AUCH HIER!
	            
	            if (b.getMinX() < visibleMinX || b.getMaxX() > visibleMaxX ||
	                b.getMinY() < visibleMinY || b.getMaxY() > visibleMaxY) {
	                return false;
	            }
	        }
	    }
	    
	    return true;
	}

	// --- Inner Class: ShapesWrapper ---

	private class ShapesWrapper {
	    
		void reset() {
			// 1. Alle aktuellen Shapes "waschen" (Zustände entfernen)
			// Das ist wichtig, weil die GeoMap Instanzen wiederverwendet.
			for (Node node : shapeLayer.getChildren()) {
				node.pseudoClassStateChanged(CORRECT, false);
				node.pseudoClassStateChanged(INCORRECT, false);
				node.pseudoClassStateChanged(MARKED, false);
				// Sicherheitshalber wieder klickbar machen für die Zukunft
				node.setMouseTransparent(false);
			}

			// 2. Jetzt den Layer leeren
			shapeLayer.getChildren().clear();
		}
	    
		void setToCheck(Set<String> ids) {
			// 1. Ghost Mode:
			// Erstmal ALLES im Layer durchlässig machen.
			// Damit lösen wir das "Spanien liegt unter Europa"-Problem:
			// Klicks fallen durch das obere (jetzt transparente) Shape durch auf das darunterliegende.
			for (Node node : shapeLayer.getChildren()) {
				node.setMouseTransparent(true);
			}

			for (String id : ids) {
				MapShape mapShape = map.getShape(id);
				if (mapShape == null)
					throw new RuntimeException("What the heck????");

				Node shape = mapShape.shape();
				
				// Prüfen: Liegt das Shape schon auf der Karte?
				boolean alreadyInLayer = shapeLayer.getChildren().contains(shape);

				// 2. Handler setzen. Wenn alreadyInLayer, ist das hier unnötig, aber schadet auch nicht ;-)
				// Wenn Du auf den Border klickst reagiert der second-Path. Innerhalb reagiert der first-Path. Aber egal
				// welcher, es wird nur ein(!) Klick registriert und der bubbelt immer nach oben zur Gruppe.
				shape.setOnMouseClicked(e -> {
					if (!isDragging && active) {
						handleShapeClick(id);
						e.consume();
					}
				});

				if (alreadyInLayer) {
					// Szenario: Shape liegt schon da (z.B. grün aus Runde 1).
					// Wir fassen die Optik NICHT an.
					// Wir machen es nur wieder klickbar (da wir oben alles auf true gesetzt haben).
					shape.setMouseTransparent(false);
				} else {
					// Szenario: Shape kommt neu dazu.
					// Ich gehe auf Nummer sicher. Eigentlich passiert das ja in resetMarkers. Aber was wenn das z. B. beim Ende eines Spiels mal nicht aufgerufen wurde?
					shape.pseudoClassStateChanged(CORRECT, false);
					shape.pseudoClassStateChanged(INCORRECT, false);
					shape.pseudoClassStateChanged(MARKED, false);
					shape.setMouseTransparent(false);
					shapeLayer.getChildren().add(shape);
				}
			}
		}
	    
	    void addToCorrect(Set<String> ids) {
	        for (String id : ids) {
	            Node shape = map.getShape(id).shape();
	            shape.pseudoClassStateChanged(CORRECT, true);
	            if (!shapeLayer.getChildren().contains(shape)) {
	                shapeLayer.getChildren().add(shape);
	            }
	        }
	    }
	    
	    void setMarked(Set<String> ids) {
	        for (String id : ids) {
	            Node shape = map.getShape(id).shape();
	            shape.pseudoClassStateChanged(MARKED, true);
	            
	            if (!shapeLayer.getChildren().contains(shape)) {
	                shapeLayer.getChildren().add(shape);
	            }
	        }
	    }
	    
	    void setIncorrect(Point2D clickPoint) {
	        int x = (int) clickPoint.getX();
	        int y = (int) clickPoint.getY();
	        String circleId = "small|" + x + "|" + y;
	        
	        Node circle = map.getShape(circleId).shape();
	        circle.pseudoClassStateChanged(INCORRECT, true);
	        circle.toFront();
	        
	        shapeLayer.getChildren().add(circle);
	    }
	    
	    Node getNode(String id) {
	        return map.getShape(id).shape();
	    }
	}
}
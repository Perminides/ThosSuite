package app.shared.ui.components.map;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import app.shared.model.BigComponentStyle;
import app.shared.model.MapImages;
import app.shared.model.ShapeGeometry;
import app.shared.skin.SkinImageCache;
import app.shared.skin.SkinService;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
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
 * <p>Eine Karte, die im Grunde ein sehr großes Bild ist. Der Ausschnitt lässt sich per Drag & Drop verschieben.
 * Formen können darauf platziert werden. Sie sind zunächst unsichtbar, bevor man sie anklickt, und werden dann
 * sichtbar. Formen können auch markiert werden (Welches Gebirge ist das?). Formen bestehen aus zwei Pfaden,
 * die in einer Gruppe angeordnet sind. Eine Form kann ein Kreis, Polygon (Länder) oder eine Linie (Flüsse) sein.</p>
 * 
 * <p>Frameworkgebundene, aber feature-freie Karten-Pane. Sie kennt keinen Lern-Typ, keine GeoMap: die learn-Seite
 * reicht ihr framework-freie {@link ShapeGeometry} herein (jede trägt ihre id), die Pane baut daraus über den
 * {@link MapNodeBuilder} den Node. Ein {@link ShapeGeometry.Kind#CENTER} ist kein Shape — darauf wird nur zentriert.
 * Bild-<b>Pfade</b> kommen herein (nicht Images); die Pane holt die Bilder selbst aus dem {@link SkinImageCache}.
 * Klicks meldet sie über einen {@link Consumer} (id, oder {@code null} für einen Klick ins Leere).</p>
 *
 * <p>Ein Shape existiert pro id genau einmal im Layer (auffindbar über {@code userData == id}); Zustandswechsel
 * (unsichtbar → correct, marked → correct) passieren am selben Node, nicht als Stapel. Die Nodes leben pro
 * Karte: {@link #reset()} leert den Layer komplett. Der Falsch-Klick-Marker ist die bewusste Ausnahme —
 * er hat keine wiederauffindbare id (mehrere möglich) und wird schlicht angehängt.</p>
 *
 * <p>Ihr Feld und den Rahmen im Minikarten-Bild bekommt sie übergeben — beides hängt davon ab, welche
 * Karte gerade läuft. Rahmenbreite und Eckradius für den Clip holt sie sich beim Skin, die nicht.</p>
 *
 * ImageMapPane
 * |-- Pane viewport (clipped und mit prefSize)
 *     |-- Group contentGroup
 *         |-- ImageView
 *         |-- Group shapeLayer
 *     |-- ImageView miniMap
 *     |-- Region borderOverlay
 *
 * CSS:
 * 		ImageMapPane	= ".my-image-map-pane",
 * 		borderOverlay	= "#borderOverlay"
 * 		Shape (group)	= ".my-image-map-shape", ".river"
 * 		Shape (group)	= ":correct", ":incorrect", ":marked"
 * 		Shape (path)	= ".first", ".second"
 */
public class ImageMapPane extends StackPane implements LearnMap {

	// Pseudo-Klassen für CSS
	private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
	private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
	private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");

	// Radius des Falsch-Klick-Markers (früher CircleSizes.SMALL).
	private static final int MARKER_RADIUS = 10;

	private final Image background;
	private final Image overlay;
	private final Image inactiveBackground;   // darf null sein (Karte ohne inaktive Variante)
	private final Image inactiveOverlay;      // darf null sein
	private final int overlayContentInset;

	// Ids → Geometrien. Wohnt im Feature (GeoMap), kommt als Funktion herein.
	private final Function<Set<String>, List<ShapeGeometry>> geometrieFuer;

	// UI Nodes
	private final Pane viewport;
	private final Group contentGroup;
	private final ImageView mainImageView;
	private final Group shapeLayer;
	private final ImageView miniMapImageView;
	private Point2D lastClick;
	private final Region borderOverlay;

	// Logic
	private Consumer<String> listener;
	private boolean active = false;

	// Panning State
	private double dragStartX, dragStartY;
	private double initialTranslateX, initialTranslateY;
	private boolean isDragging = false;

	// Mini-Map Config
	private static final int MINI_MAP_INSET = 10;

	/**
	 * @param bilder              Hintergrund und Overlay, je aktiv und abgeschaltet.
	 * @param overlayContentInset Wie breit der Rahmen im Minikarten-Bild ringsum ist — hängt am Bild
	 *                            und damit an der Karte, löst deshalb der Aufrufer auf. <b>Wo</b> der
	 *                            Inhalt sitzt, rechnet diese Klasse daraus selbst: sie hat das Bild.
	 * @param bounds              Das Feld, in dem die Karte sitzt. Löst der Aufrufer auf.
	 * @param geometrieFuer       Übersetzt Ids in Geometrien — wohnt im Feature, diese Klasse kennt
	 *                            weder Karte noch Deck.
	 */
	public ImageMapPane(MapImages bilder, int overlayContentInset, Rectangle2D bounds,
			Function<Set<String>, List<ShapeGeometry>> geometrieFuer) {
		SkinImageCache images = SkinImageCache.getInstance();
		this.background = images.get(bilder.background());
		this.overlay = images.get(bilder.overlay());
		this.inactiveBackground = images.get(bilder.inactiveBackground());
		this.inactiveOverlay = images.get(bilder.inactiveOverlay());
		this.overlayContentInset = overlayContentInset;
		this.geometrieFuer = geometrieFuer;

		// 1. Main Content aufbauen
		mainImageView = new ImageView();
		mainImageView.setId("mainImageView");
		shapeLayer = new Group();
		shapeLayer.setId("shapeLayer");
		contentGroup = new Group(mainImageView, shapeLayer);
		contentGroup.setId("contentGroup");

		// 2. Viewport (Clip weiter unten, sobald die Größe steht)
		viewport = new Pane(contentGroup); // Einfach weglassen? → Eine StackPane ist ein Layout-Manager. Sie versucht zwanghaft, ihre Kinder (Children) zu positionieren – standardmäßig zentriert in der Mitte. Wenn du später versuchst, deine contentGroup (die Karte) zu verschieben (Panning/Verschieben mit der Maus), kämpfst du gegen die StackPane. Eine reine Pane macht kein Layout-Management. Sie sagt: "Setz deine Kinder hin, wo du willst (x, y), mir egal".
		viewport.setId("viewport");

		// 3. Mini-Map
		miniMapImageView = new ImageView();
		miniMapImageView.setId("miniMapImageView");
		StackPane.setAlignment(miniMapImageView, Pos.TOP_RIGHT);
		StackPane.setMargin(miniMapImageView, new javafx.geometry.Insets(MINI_MAP_INSET));

		// 4. Border Overlay
		borderOverlay = new Region();
		borderOverlay.setId("borderOverlay");
		borderOverlay.setMouseTransparent(true);

		getChildren().addAll(viewport, miniMapImageView, borderOverlay);

		updateImages();
		setupInteraction();

		applyBounds(bounds);
		center(); // jetzt steht die Größe
	}

	/**
	 * Größe, Lage und Clip. Der Clip ist das programmatische Gegenstück zum {@code #borderOverlay}-Rahmen:
	 * um die Rahmenbreite eingerückt, damit der Karteninhalt nicht unter den runden Ecken hervorlugt.
	 */
	private void applyBounds(Rectangle2D bounds) {
		setPrefSize(bounds.getWidth(), bounds.getHeight());
		setMaxSize(bounds.getWidth(), bounds.getHeight());
		setLayoutX(bounds.getMinX());
		setLayoutY(bounds.getMinY());
		getStyleClass().add("my-image-map-pane");

		BigComponentStyle rahmen = SkinService.get().bigComponentStyle();
		double bw = rahmen.borderWidth();

		Rectangle clip = new Rectangle(bw, bw, bounds.getWidth() - 2 * bw, bounds.getHeight() - 2 * bw);
		// Durchmesser, nicht Radius — Rectangle rechnet anders als das CSS.
		double eckDurchmesser = rahmen.cornerRadius() * 2;
		clip.setArcWidth(eckDurchmesser);
		clip.setArcHeight(eckDurchmesser);
		viewport.setClip(clip);
	}

	/** Zentriert die Karte. */
	private void center() {
		if (mainImageView.getImage() == null)
			return;

		double imgW = mainImageView.getImage().getWidth();
		double imgH = mainImageView.getImage().getHeight();

		centerOnPoint(imgW / 2.0, imgH / 2.0);
	}
	
    @Override
	public Node getView() {
		return this;
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
		        listener.accept(null);
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

	    // Die Inhaltsfläche ist das Bild abzüglich des Rahmens — relativ zum tatsächlich geladenen
	    // Overlay gerechnet, weil die Skins unterschiedlich große Minikarten mitbringen.
	    double contentX = e.getX() - overlayContentInset;
	    double contentY = e.getY() - overlayContentInset;

	    double scaleX = mainImageView.getImage().getWidth() / (overlay.getWidth() - 2 * overlayContentInset);
	    double scaleY = mainImageView.getImage().getHeight() / (overlay.getHeight() - 2 * overlayContentInset);

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
			listener.accept(id);
		}
		// Marker-Position wird NICHT hier gesetzt - das macht der Presenter
		// bei falschem Klick via markLastClickAsIncorrect()
	}

	public void setListener(Consumer<String> listener) {
		this.listener = listener;
	}

	public void setActive(boolean active) {
		if (this.active != active) {
			this.active = active;
			updateImages();
		}
	}

	private void updateImages() {
		Image inactiveMain = inactiveBackground != null ? inactiveBackground : background;

		// TODO: Guard 1:1 aus der alten GeoMap-Fassung übernommen. Er greift real fast nie (background ist bei
		// Bild-Karten gesetzt); bewusst NICHT angefasst — das ist eine eigene Runde (FailFast-Frage).
		if (inactiveMain == null && inactiveOverlay == null && mainImageView.getImage() != null)
			return;
		mainImageView.setImage(active ? background : inactiveMain);
		miniMapImageView.setImage(active ? overlay : inactiveOverlay);
	}

	// ========================================
	// Shapes (Geometrien mit id, von der learn-Seite)
	// ========================================

	// ========================================
	// Das gemeinsame Vokabular (LearnMap) — spricht Ids, übersetzt nach Geometrien
	// ========================================

	@Override public void reset()                         { resetMarkers(); }
	@Override public void markCorrect(Set<String> ids)    { addToCorrect(geometrieFuer.apply(ids)); }
	@Override public void mark(Set<String> ids)           { setMarked(geometrieFuer.apply(ids)); }
	@Override public void setClickTargets(Set<String> ids) { setClickTargets(geometrieFuer.apply(ids)); }

	/**
	 * Die Bild-Karte kennt die falsche Form nicht per id — sie merkt sich den letzten Klick selbst.
	 *
	 * <p>TODO: „EM 2021 — alle Länder grün, welches war falsch?" Das Zurücksetzen davor löscht die
	 *   bisherigen Markierungen mit.</p>
	 */
	@Override
	public void markIncorrect(String id) {
		resetMarkers();
		markLastClickAsIncorrect();
	}

	private void setClickTargets(List<ShapeGeometry> shapes) {
		// Ghost Mode: erstmal ALLES im Layer durchlässig machen. Damit lösen wir das "Spanien liegt unter
		// Europa"-Problem: Klicks fallen durch das obere (jetzt transparente) Shape auf das darunterliegende.
		for (Node node : shapeLayer.getChildren())
			node.setMouseTransparent(true);

		// Die zu suchenden Shapes bauen/wiederfinden und (nur diese) klickbar machen. Ein schon vorhandenes
		// (z.B. grün aus Runde 1) fassen wir optisch nicht an — place liefert denselben Node zurück.
		for (ShapeGeometry geometry : shapes) {
			Node node = place(geometry);
			if (node != null)
				node.setMouseTransparent(false);
		}
	}

	public void addToCorrect(List<ShapeGeometry> shapes) {
		for (ShapeGeometry geometry : shapes) {
			Node node = place(geometry);
			if (node != null)
				node.pseudoClassStateChanged(CORRECT, true);
		}
		recenterIfNeeded(shapes);
	}

	public void setMarked(List<ShapeGeometry> shapes) {
		for (ShapeGeometry geometry : shapes) {
			Node node = place(geometry);
			if (node != null)
				node.pseudoClassStateChanged(MARKED, true);
		}
		recenterIfNeeded(shapes);
	}

	public void markLastClickAsIncorrect() {
		int x = (int) lastClick.getX();
		int y = (int) lastClick.getY();

		// Marker: kein place (mehrere möglich, keine wiederauffindbare id) — schlicht anhängen.
		Node marker = MapNodeBuilder.buildImageMapNode(ShapeGeometry.circle("small|" + x + "|" + y, x, y, MARKER_RADIUS));
		marker.pseudoClassStateChanged(INCORRECT, true);
		marker.toFront();
		shapeLayer.getChildren().add(marker);
	}

	/** Pro Karte: kompletter Neustart. Kein "Waschen" der Zustände nötig, da kein Node den Reset überlebt. */
	private void resetMarkers() {
		shapeLayer.getChildren().clear();
	}

	// Sorgt dafür, dass die Geometrie im Layer liegt und gibt ihren Node zurück. CENTER ist kein Shape:
	// darauf wird nur zentriert, es gibt keinen Node (Rückgabe null). Sonst: pro id existiert genau ein
	// Node (userData == id) — vorhandenen wiederverwenden, sonst frisch bauen und anhängen.
	private Node place(ShapeGeometry geometry) {
		if (geometry.kind() == ShapeGeometry.Kind.CENTER) {
			centerOnPoint(geometry.centerX(), geometry.centerY());
			return null;
		}

		Node existing = findInLayer(geometry.id());
		if (existing != null)
			return existing;

		Node node = MapNodeBuilder.buildImageMapNode(geometry);
		node.setOnMouseClicked(e -> {
			// Border → second-Path, Innen → first-Path; egal welcher, ein Klick bubbelt hoch zur Gruppe.
			if (!isDragging && active) {
				handleShapeClick(geometry.id());
				e.consume();
			}
		});
		shapeLayer.getChildren().add(node);
		return node;
	}

	private Node findInLayer(String id) {
		for (Node child : shapeLayer.getChildren()) {
			if (id.equals(child.getUserData()))
				return child;
		}
		return null;
	}

	private void recenterIfNeeded(List<ShapeGeometry> shapes) {
		if (!areShapesFullyVisible(shapes)) {
			Point2D center = getCenterOfShapes(shapes);
			if (center != null)
				centerOnPoint(center.getX(), center.getY());
		}
	}

	private Point2D getCenterOfShapes(List<ShapeGeometry> shapes) {
	    double sumX = 0, sumY = 0;
	    int count = 0;

	    for (ShapeGeometry geometry : shapes) {
	        Node node = findInLayer(geometry.id());   // CENTER liegt nicht im Layer → fällt automatisch raus
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

	    return new Point2D(sumX / count, sumY / count);
	}

	private boolean areShapesFullyVisible(List<ShapeGeometry> shapes) {
		if (shapes == null || shapes.isEmpty())
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

	    for (ShapeGeometry geometry : shapes) {
	        Node node = findInLayer(geometry.id());
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
}
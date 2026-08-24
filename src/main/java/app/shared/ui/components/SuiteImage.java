package app.shared.ui.components;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

import app.shared.Config;
import app.shared.model.BigComponentStyle;
import app.shared.model.ShapeGeometry;
import app.shared.model.SketchColor;
import app.shared.skin.SkinService;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;


/**
 * Used to show an image. Consists of three Rectangles and one content layer:
 * 		- backgroundRect	-> Used when no image is shown.
 * 		- imageRect			-> The image to display
 * 		- contentPane		-> Die Skizze statt eines Bildes (siehe {@link #setSketch})
 * 		- borderRect		-> Optional border
 * 
 * CSS-classes:
 * 		backgroundRect	= "my-image-background-layer"
 * 		borderRect		= "my-image-border-layer"
 * 
 * <p>Bild und Skizze schließen einander aus: Wer das eine setzt, leert das andere. Rahmen, Ecken
 * und Hintergrund gelten für beide gleich — sie gehören dem Bilderrahmen, nicht seinem Inhalt.
 * Die {@link SketchPane} ist Innenleben und wird nicht herausgereicht.</p>
 * 
 * !Sofort: Mach dir bitte Gedanken, ob Du nicht a) auf das zwangsweise in ein Quadrat pressen von Bildern verzichten willst
 * (Ja willst Du). Die Suite kann auch ein rechteckiges soweit verkleinern das reinpasst und dann mittig anzeigen. Diese
 * transparenten Teile da reinzumalen, ist Quatch. Und b) wieso verkleinerst Du die immer noch? JavaFX Sklaierung ist echt super
 * Das kann die selber verkleinern! Ok. Natürlich würdest Du mit den Originalen den Bilderordner aufblähen. Das ist schon richtig
 * Aber probiere es zumindest mal aus bitte. Nicht auszuschließen, dass die JavaFX Skalierung am Ende eh besser aussieht als deine
 * verkleinerten Dateien. Dann bliebe nur noch das Filesize Argument. Aber diskutiere es mal mit Claude!
 */
public class SuiteImage extends StackPane {

    // Layer 1: Hintergrundfarbe (wenn kein Bild da ist oder Transparenz im Bild)
    private final Rectangle backgroundRect;
    // Layer 2: Das eigentliche Bild
    private final Rectangle imageRect;
    // Layer 2b: Die Skizze anstelle eines Bildes
    private final StackPane contentPane;
    // Layer 3: Der Rahmen (liegt ganz oben)
    private final Rectangle borderRect;

    // Die Maße innerhalb des Rahmens — das Bildrechteck wird auf das Seitenverhältnis seines
    // Bildes zurückgeschnitten und braucht sie bei jedem Wechsel neu.
    private final double innerWidth;
    private final double innerHeight;

    private SketchPane sketch;

    /**
     * Mit fester Lage — für absolut positionierende Hosts.
     */
    public SuiteImage(Rectangle2D bounds) {
        this(bounds.getWidth(), bounds.getHeight());
        setLayoutX(bounds.getMinX());
        setLayoutY(bounds.getMinY());
    }

    /**
     * Ohne feste Lage — für Aufrufer, die die Komponente in ein Layout hängen.
     *
     * <p>Ja. Leider brauchen wir die Größe als Parameter. Man kann mittels CSS kein Bild beschneiden.
     * Das würde sonst also unter abgerundeten Ecken hervorlugen... Siehe RoundedImageTest, falls noch da...
     * Und nein, das ist jetzt auch kein Riesenproblem: Wir müssen eh das Spielfeld neu aufbauen bei Skinwechsel,
     * weil sich ja auch Positionen ändern können von Komponenten</p>
     *
     * <p>Den Eckradius holt sich die Komponente selbst — der hängt am Skin und nicht am Aufrufer.
     * {@link Rectangle} will den Durchmesser der Eckrundung, der Skin führt (wie das CSS) den Radius;
     * die Verdopplung passiert deshalb hier, direkt an der API, die es so will.</p>
     */
    public SuiteImage(double width, double height) {
        BigComponentStyle frame = SkinService.get().bigComponentStyle();
        double bw = frame.borderWidth();
        double arcDiameter = frame.cornerRadius() * 2;

        // Die beiden Inhaltsebenen enden an der INNENkante des Rahmens, nicht an seiner Außenkante.
        // Das ist die Entsprechung zu -fx-background-insets, das es hier nicht gibt: die Eigenschaft
        // gehört zur Region, und ein Rectangle ist keine — seine Maße sind auch nicht per CSS setzbar.
        //
        // Warum das nötig ist: der Rahmen wird per stroke-type: inside gezeichnet, seine Außenkante
        // liegt also exakt auf dem Pfad und wird an der Rundung antialiasiert. Diese äußerste Pixelreihe
        // ist nur teilweise deckend — eine Füllung mit derselben Kontur scheint dort hindurch. Bei
        // kontrastreichem Hintergrund sieht man den Saum. Betrifft Hintergrund UND Bild; das Bild ist
        // eine ImagePattern-Füllung und lugt genauso hervor.
        //
        // Zwei Zweien mit verschiedenen Gründen: die Maße schrumpfen um 2*bw (links und rechts, oben
        // und unten), der Eckradius nur um bw (der wird vom Eckmittelpunkt gemessen) — und das *2 am
        // Ende ist die Umrechnung Radius → Durchmesser, die setArcWidth verlangt.
        innerWidth = width - 2 * bw;
        innerHeight = height - 2 * bw;
        double innerArcDiameter = Math.max(0, frame.cornerRadius() - bw) * 2;

        // 1. Container-Größe fixieren
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        // ---------------------------------------------------------
        // Layer 1: Hintergrund (Unten) — eingerückt
        // ---------------------------------------------------------
        backgroundRect = new Rectangle(innerWidth, innerHeight);
        backgroundRect.setArcWidth(innerArcDiameter);
        backgroundRect.setArcHeight(innerArcDiameter);
        backgroundRect.getStyleClass().add("my-image-background-layer");
        // HIER WICHTIG: Den Border-Style entfernen wir hier!

        // ---------------------------------------------------------
        // Layer 2: Bild (Mitte) — eingerückt
        // ---------------------------------------------------------
        imageRect = new Rectangle(innerWidth, innerHeight);
        imageRect.setArcWidth(innerArcDiameter);
        imageRect.setArcHeight(innerArcDiameter);
        imageRect.setFill(Color.TRANSPARENT);

        // ---------------------------------------------------------
        // Layer 2b: Inhalt (Mitte) — eingerückt wie das Bild
        // ---------------------------------------------------------
        // Der Clip ist derselbe Gedanke wie die Einrückung der beiden Rechtecke: Ein Inhalt, der bis
        // in die Ecken reicht, lügte sonst unter der Rundung des Rahmens hervor.
        contentPane = new StackPane();
        contentPane.setPrefSize(innerWidth, innerHeight);
        contentPane.setMinSize(innerWidth, innerHeight);
        contentPane.setMaxSize(innerWidth, innerHeight);
        Rectangle contentClip = new Rectangle(innerWidth, innerHeight);
        contentClip.setArcWidth(innerArcDiameter);
        contentClip.setArcHeight(innerArcDiameter);
        contentPane.setClip(contentClip);

        // ---------------------------------------------------------
        // Layer 3: Rahmen (Oben) — volle Größe, die StackPane zentriert die kleineren Ebenen darin
        // ---------------------------------------------------------
        borderRect = new Rectangle(width, height);
        borderRect.setArcWidth(arcDiameter);
        borderRect.setArcHeight(arcDiameter);
        borderRect.setFill(Color.TRANSPARENT); // Innen komplett durchsichtig!
        borderRect.setMouseTransparent(true);  // Klicks sollen durchgehen (optional, aber sauber)
        
        // Der Rahmen-Style wandert auf dieses neue Rechteck:
        borderRect.getStyleClass().add("my-image-border-layer");

        // ---------------------------------------------------------
        // Stapeln: Hintergrund -> Bild -> Inhalt -> Rahmen
        // ---------------------------------------------------------
        getChildren().addAll(backgroundRect, imageRect, contentPane, borderRect);
    }
    

    /**
     * Wenn der String null oder leer ist, wird der Bilderrahmen geleert. Das wird bspw. bei Multiple Choice häufiger
     * genutzt. Wenn das Bild im Lern-Bilder-Ordner nicht gefunden wird, so gibt es eine Exception. Dann stimmt etwas
     * nicht! Wenn diese Komponente mal außerhalb des Lern-Pakets genutzt wird, muss man hier natürlich nochmal ran.
     * Aber YAGNI...
     * 
     * @param imageName
     */
    public void setImage(String imageName) {
        sketch = null;
        contentPane.getChildren().clear();
        if (imageName == null || imageName.isEmpty()) {
            clearImageLayer();
            return;
        }

        File imageFile = Config.getPath("learnImageFolder").resolve(imageName).toFile();
        if (!imageFile.exists()) {
            throw new RuntimeException("Konnte das Bild nicht finden: " + imageFile);
        }

        try {
            String url = imageFile.toURI().toURL().toExternalForm(); // sauberer als toString()
            Image img = new Image(url, false); // backgroundLoading=false => lädt synchron
            if (img.isError()) {
                throw new RuntimeException("Fehler beim Laden des Bildes: " + imageName, img.getException());
            }
            imageRect.setStyle(""); // CSS weg, falls vorher gesetzt
            fitToImage(img);
            imageRect.setFill(new ImagePattern(img));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ungültige Bild-URL: " + imageName, e);
        }
    }
    
    /**
     * Das Bildrechteck bekommt das Seitenverhältnis seines Bildes, die StackPane zentriert es darin.
     *
     * <p>Sonst würde gestreckt: Eine {@code ImagePattern}-Füllung bildet das Bild auf die Fläche des
     * Rechtecks ab, ohne nach dem Verhältnis zu fragen. Das Muster über einen kleineren Ausschnitt zu
     * verankern hilft nicht — dann kachelt es. Also schrumpft das Rechteck selbst.</p>
     */
    private void fitToImage(Image img) {
        double factor = Math.min(innerWidth / img.getWidth(), innerHeight / img.getHeight());
        imageRect.setWidth(img.getWidth() * factor);
        imageRect.setHeight(img.getHeight() * factor);
    }

    /** Kein Bild: durchsichtig, und wieder in voller Größe für das nächste. */
    private void clearImageLayer() {
        imageRect.setStyle("");
        imageRect.setFill(Color.TRANSPARENT);
        imageRect.setWidth(innerWidth);
        imageRect.setHeight(innerHeight);
    }

    /**
     * Zeigt eine neue, noch leere Skizze aus den gegebenen Teilflächen — ein zuvor gesetztes Bild
     * verschwindet dabei, wie umgekehrt auch.
     *
     * <p>Die Skizze wird an den Ecken beschnitten wie das Bild. Sie fällt dabei eine Spur größer aus
     * als die Ebene, damit ihre äußere Kontur ganz im Clip verschwindet; siehe {@link SketchPane}.</p>
     */
    public void setSketch(List<ShapeGeometry> areas) {
        clearImageLayer();
        sketch = new SketchPane(areas, contentPane.getPrefWidth(), contentPane.getPrefHeight());
        contentPane.getChildren().setAll(sketch);
    }

    /** Hebt eine Fläche der Skizze hervor. */
    public void markSketchArea(int area) {
    	activeSketch().mark(area);
    }

    /** Färbt eine Fläche der Skizze. */
    public void fillSketchArea(int area, SketchColor color) {
    	activeSketch().fill(area, color);
    }

    private SketchPane activeSketch() {
        if (sketch == null)
            throw new IllegalStateException("Im Bilderrahmen liegt keine Skizze — fehlt ein SketchImage-Schritt?");
        return sketch;
    }

    // Getter: Falls jemand Zugriff auf den Hintergrund braucht
    public Rectangle getBackgroundRect() { return backgroundRect; }
    
    // Falls du später mal Zugriff auf den Rahmen brauchst, könntest du das hier ergänzen:
    public Rectangle getBorderRect() { return borderRect; }
}
package scripts.svg;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * 4-Wege-Vergleich, alle in derselben Anzeigebreite:
 *   kein Supersampling (jsvg 1x) · jsvg 2x · jsvg 4x · runtergerechnete Chrome-PNG.
 *
 * Aufruf: SvgProbe <Brasilien.svg> <chrome.png> [Anzeigebreite=400]
 * Launcher-Main erbt bewusst NICHT von Application.
 */
public class SvgProbe {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    /** jsvg → BufferedImage der Breite {@code rasterWidth}. */
    static Image jsvg(File file, double rasterWidth) throws Exception {
        SVGDocument doc = new SVGLoader().load(file.toURI().toURL());
        FloatSize size = doc.size();
        double scale = rasterWidth / Math.max(size.width, size.height);
        int w = Math.max(1, (int) Math.round(size.width * scale));
        int h = Math.max(1, (int) Math.round(size.height * scale));
        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bimg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        doc.render((Component) null, g, new ViewBox(0, 0, (float) w, (float) h));
        g.dispose();
        return SwingFXUtils.toFXImage(bimg, null);
    }

    static VBox panel(String title, Image img, double display) {
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setFitWidth(display);
        VBox box = new VBox(6, new Label(title), iv);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    public static class App extends Application {
        @Override
        public void start(Stage stage) throws Exception {
            var p = getParameters().getRaw();
            if (p.size() < 2) {
                System.out.println("Aufruf: SvgProbe <svg> <chrome.png> [Anzeigebreite]");
                stage.close();
                return;
            }
            File svg = new File(p.get(0));
            File png = new File(p.get(1));
            double dw = p.size() > 2 ? Double.parseDouble(p.get(2)) : 400;
            System.out.println("Anzeigebreite = " + dw);

            Image pngImg = new Image(png.toURI().toString());

            GridPane grid = new GridPane();
            grid.setHgap(30);
            grid.setVgap(20);
            grid.add(panel("kein Supersampling (jsvg 1x)", jsvg(svg, dw), dw), 0, 0);
            grid.add(panel("jsvg 2x → " + (int) dw, jsvg(svg, dw * 2), dw), 1, 0);
            grid.add(panel("jsvg 4x → " + (int) dw, jsvg(svg, dw * 4), dw), 0, 1);
            grid.add(panel("Chrome-PNG runtergerechnet", pngImg, dw), 1, 1);
            grid.setStyle("-fx-padding: 24; -fx-background-color: #cccccc;");

            stage.setScene(new Scene(grid));
            stage.setTitle("4-Wege: " + svg.getName() + "  @ " + (int) dw + "px");
            stage.show();
        }
    }
}
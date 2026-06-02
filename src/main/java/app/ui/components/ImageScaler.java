package app.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import app.shared.Config;
import app.shared.Log;
import app.ui.skin.SkinService;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class ImageScaler {
	
	private static final Integer LEFT = 0;
	private static final Integer RIGHT = 1;
	
	public static void processImages() {

		Path targetDir500 = Path.of(Config.get("learnImageFolder"));
	    Path sourceDir = targetDir500.getParent();
	    Path targetDirOrig = sourceDir.resolve("origs");

	    if (!Files.exists(sourceDir)) return;

	    try (Stream<Path> stream = Files.list(sourceDir)) {

	        for (Path imgPath : stream.toList()) {

	            String name = imgPath.getFileName().toString().toLowerCase();
	            if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")))
	                continue;

	            Path target = targetDir500.resolve(imgPath.getFileName());

	            if (Files.exists(target)) {
	                Alert alert = SkinService.get().createAlert(null, "Achtung", imgPath.getFileName() + " ist bereits verkleinert. Ich breche ab.", false, false);
	                alert.showAndWait();
	                return;
	            }

	            BufferedImage original = ImageIO.read(imgPath.toFile());

	            Image resultLeft = scaleImageScalr(original, new Dimension (500, 500));
	            Image resultRight = scaleImagePlain(original, new Dimension (500, 500));
	            Image chosen = chooseImage(resultLeft, resultRight);

	            if (chosen == null)
	            	return;
	            
	            Log.debug(ImageScaler.class, "Wir schreiben und verschieben " + imgPath.getFileName());
	            BufferedImage buffered = SwingFXUtils.fromFXImage(chosen, null);
	            if (name.endsWith("jpg") || name.endsWith("jpeg")) {
	            	BufferedImage rgb = new BufferedImage(
	            			buffered.getWidth(),
	            			buffered.getHeight(),
	                        BufferedImage.TYPE_INT_RGB
	                );

	                Graphics2D g = rgb.createGraphics();
	                g.drawImage(buffered, 0, 0, Color.WHITE, null);
	                g.dispose();
	                
	                buffered = rgb;
	            }
	            
	            Log.info(ImageScaler.class, "Wir erstellen eine verkleinerte Version und verschieben das Original von " + imgPath.getFileName());
	            ImageIO.write(buffered, name.endsWith(".png") ? "png" : "jpg", target.toFile());
	            Files.move(imgPath, targetDirOrig.resolve(imgPath.getFileName()));
	        } 
	    } catch (Exception e) {
			throw new RuntimeException("Irgendwas ist beim Bilder verkleinern schiefgelaufen", e);
		}
	}
	
	private static Image chooseImage(Image imgLeft, Image imgRight) {

		Window owner = SkinService.getOwnerWindow();
		// createDialog gibt Dialog<?> zurück – wir casten bewusst auf Integer,
        // um setResult() nutzen zu können und die showAndWait()-Event-Loop sauber zu beenden.
        @SuppressWarnings("unchecked")
	    Dialog<Integer> dialog = (Dialog<Integer>) SkinService.get().createDialog(owner, "Bild auswählen");
	    
	    // CANCEL_CLOSE damit JavaFX den Dialog grundsätzlich schließen lässt
        ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        // ButtonBar auf Höhe 0 zwingen – kein sichtbarer Inhalt, kein reservierter Platz
        var buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setMinHeight(0);
            buttonBar.setPrefHeight(0);
            buttonBar.setMaxHeight(0);
            var cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
            if (cancelBtn != null) {
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
            }
        }
	    
	    DialogPane pane = dialog.getDialogPane();

	    int[] chosen = { -1 };

	    Button okLeft = new Button("OK");
	    Button okRight = new Button("OK");

	    okLeft.setOnAction(_ -> {
	    	chosen[0] = LEFT;
	    	dialog.setResult(LEFT);
	    });
	    okRight.setOnAction(_ -> {
	    	chosen[0] = RIGHT;
	    	dialog.setResult(RIGHT);
	    });

	    ImageView viewLeft = new ImageView(imgLeft);
	    ImageView viewRight = new ImageView(imgRight);

	    viewLeft.setPreserveRatio(true);
	    viewRight.setPreserveRatio(true);

	    VBox left = new VBox(10, viewLeft, okLeft);
	    VBox right = new VBox(10, viewRight, okRight);

	    left.setAlignment(Pos.CENTER);
	    right.setAlignment(Pos.CENTER);

	    HBox root = new HBox(20, left, right);
	    root.setAlignment(Pos.CENTER);
	    root.setPadding(new Insets(20));

	    pane.setContent(root);

	    dialog.showAndWait();
	    
	    if (chosen[0] == -1)
	    	return null;
	    else {
	    	Image result = chosen[0] == LEFT ? imgLeft : imgRight;
	    	Log.debug(ImageScaler.class, (chosen[0] == LEFT ? "Linkes " : "Rechtes ") + "Bild gewählt.");
	    	return result;
	    }
	}
	
    private static Image scaleImageScalr(BufferedImage img, Dimension d) {
    	BufferedImage br = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, d.width, d.height, Scalr.OP_ANTIALIAS);
    	return SwingFXUtils.toFXImage(br, null);
    }
    
	// https://www.locked.de/fast-image-scaling-in-java/
	// Wenn die Ergebisse nicht gut sind, probiere es halt mal mit JavaXT z. B.

    private static Image scaleImagePlain(BufferedImage img, Dimension d) {
        img = scaleByHalf(img, d);
        img = scaleExact(img, d);
        return SwingFXUtils.toFXImage(img, null);
    }

    private static BufferedImage scaleByHalf(BufferedImage img, Dimension d) {
        int w = img.getWidth();
        int h = img.getHeight();
        float factor = getBinFactor(w, h, d);

        // make new size
        w *= factor;
        h *= factor;
        BufferedImage scaled = new BufferedImage(w, h,
                img.getType());
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage scaleExact(BufferedImage img, Dimension d) {
        float factor = getFactor(img.getWidth(), img.getHeight(), d);

        // create the image
        int w = Math.round(img.getWidth() * factor);
        int h = Math.round(img.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(w, h,
                img.getType());

        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private static float getBinFactor(int width, int height, Dimension dim) {
        float factor = 1;
        float target = getFactor(width, height, dim);
        if (target <= 1) { while (factor / 2 > target) { factor /= 2; }
        } else { while (factor * 2 < target) { factor *= 2; }         }
        return factor;
    }

    private static float getFactor(int width, int height, Dimension dim) {
        float sx = dim.width / (float) width;
        float sy = dim.height / (float) height;
        return Math.min(sx, sy);
    }
	
}

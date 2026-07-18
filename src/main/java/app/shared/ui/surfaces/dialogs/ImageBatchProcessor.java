package app.shared.ui.surfaces.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.model.SelectionEnum;
import app.shared.skin.SkinService;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class ImageBatchProcessor {

    public static void process(Path sourceDir, Path targetDir, Path backupDir, int width, int height) {
        if (!Files.exists(sourceDir)) return;
        Dimension size = new Dimension(width, height);

        try (Stream<Path> stream = Files.list(sourceDir)) {
            for (Path imgPath : stream.toList()) {
                String name = imgPath.getFileName().toString().toLowerCase();
                if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")))
                    continue;

                Path target = targetDir.resolve(imgPath.getFileName());
                if (Files.exists(target)) {
                    SkinService.get().showAlert("Achtung",
                            imgPath.getFileName() + " liegt mit dem Namen bereits verkleinert vor.", ButtonEnum.OK);
                    return;
                }

                BufferedImage original = ImageIO.read(imgPath.toFile());
                BufferedImage left = scaleImageScalr(original, size);
                BufferedImage right = scaleImagePlain(original, size);

                Optional<SelectionEnum> choice = chooseImage(left, right);
                if (choice.isEmpty()) return;

                BufferedImage chosen = choice.get() == SelectionEnum.ZERO ? left : right;

                if (name.endsWith("jpg") || name.endsWith("jpeg")) {
                    BufferedImage rgb = new BufferedImage(
                            chosen.getWidth(), chosen.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgb.createGraphics();
                    g.drawImage(chosen, 0, 0, Color.WHITE, null);
                    g.dispose();
                    chosen = rgb;
                }

                Log.info(ImageBatchProcessor.class,
                        "Verkleinern + Original sichern: " + imgPath.getFileName());
                ImageIO.write(chosen, name.endsWith(".png") ? "png" : "jpg", target.toFile());
                Files.move(imgPath, backupDir.resolve(imgPath.getFileName()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Irgendwas ist beim Bilder verkleinern schiefgelaufen", e);
        }
    }

    private static Optional<SelectionEnum> chooseImage(BufferedImage left, BufferedImage right) {
        Window owner = SkinService.getOwnerWindow();

        @SuppressWarnings("unchecked")
        Dialog<SelectionEnum> dialog = (Dialog<SelectionEnum>) SkinService.get().createDialog(owner, "Bild auswählen");

        ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
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

        Button okLeft = new Button("OK");
        Button okRight = new Button("OK");
        okLeft.setOnAction(_ -> dialog.setResult(SelectionEnum.ZERO));
        okRight.setOnAction(_ -> dialog.setResult(SelectionEnum.ONE));

        ImageView viewLeft = new ImageView(SwingFXUtils.toFXImage(left, null));
        ImageView viewRight = new ImageView(SwingFXUtils.toFXImage(right, null));
        viewLeft.setPreserveRatio(true);
        viewRight.setPreserveRatio(true);

        VBox leftBox = new VBox(10, viewLeft, okLeft);
        VBox rightBox = new VBox(10, viewRight, okRight);
        leftBox.setAlignment(Pos.CENTER);
        rightBox.setAlignment(Pos.CENTER);

        HBox root = new HBox(20, leftBox, rightBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        pane.setContent(root);
        return dialog.showAndWait();
    }

    private static BufferedImage scaleImageScalr(BufferedImage img, Dimension d) {
        return Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, d.width, d.height, Scalr.OP_ANTIALIAS);
    }

	// https://www.locked.de/fast-image-scaling-in-java/
	// Wenn die Ergebisse nicht gut sind, probiere es halt mal mit JavaXT z. B.
    private static BufferedImage scaleImagePlain(BufferedImage img, Dimension d) {
        img = scaleByHalf(img, d);
        img = scaleExact(img, d);
        return img;
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
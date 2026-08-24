package app.learn;

import java.awt.image.BufferedImage;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import app.shared.Config;
import app.shared.ImageUtils;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.model.SelectionEnum;
import app.shared.ui.Alerts;
import app.shared.ui.ImageComparisonDialog;

/**
 * Verkleinert die Bilder, die im Lern-Ordner abgelegt wurden, und sichert die Originale.
 *
 * <p>Pro Bild werden beide Skalierverfahren aus {@link ImageUtils} gerechnet und nebeneinander
 * gezeigt; der Benutzer entscheidet, welches gespeichert wird. Das Original wandert danach nach
 * {@code origs}.</p>
 *
 * <p>Die Schleife ordnet nur an — gerechnet wird in {@link ImageUtils}, gefragt im
 * {@link ImageComparisonDialog}.</p>
 */
public class ImageScaler {

	private static final int TARGET_WIDTH = 500;
	private static final int TARGET_HEIGHT = 500;

	public static void processImages() {
		Path targetDir = Config.getPath("learnImageFolder");
		Path sourceDir = targetDir.getParent();
		Path backupDir = sourceDir.resolve("origs");

		if (!Files.exists(sourceDir))
			return;

		try {
			for (Path imgPath : filesIn(sourceDir)) {
				String name = imgPath.getFileName().toString().toLowerCase();
				if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")))
					continue;

				Path target = targetDir.resolve(imgPath.getFileName());
				if (Files.exists(target)) {
					Alerts.show("Achtung",
							imgPath.getFileName() + " liegt mit dem Namen bereits verkleinert vor.", ButtonEnum.OK);
					continue;
				}

				BufferedImage original = ImageIO.read(imgPath.toFile());
				BufferedImage left = ImageUtils.scaleSmooth(original, TARGET_WIDTH, TARGET_HEIGHT,
						Scalr.Method.ULTRA_QUALITY, Scalr.OP_ANTIALIAS);
				BufferedImage right = ImageUtils.scaleStepwise(original, TARGET_WIDTH, TARGET_HEIGHT);

				SelectionEnum choice = ImageComparisonDialog.show(left, right);
				if (choice == null)
					return;

				BufferedImage chosen = choice == SelectionEnum.ZERO ? left : right;
				if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
					chosen = ImageUtils.toRgb(chosen);

				Log.info(ImageScaler.class, "Verkleinern + Original sichern: " + imgPath.getFileName());
				ImageIO.write(chosen, name.endsWith(".png") ? "png" : "jpg", target.toFile());
				Files.move(imgPath, backupDir.resolve(imgPath.getFileName()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Irgendwas ist beim Bilder verkleinern schiefgelaufen", e);
		}
	}

	/** Erst den Ordner einlesen, dann verarbeiten: die Schleife verschiebt die Bilder weg. */
	private static List<Path> filesIn(Path ordner) throws Exception {
		List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> content = Files.newDirectoryStream(ordner)) {
			for (Path eintrag : content)
				files.add(eintrag);
		}
		return files;
	}
}

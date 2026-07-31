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

	private static final int ZIEL_BREITE = 500;
	private static final int ZIEL_HOEHE = 500;

	public static void processImages() {
		Path targetDir = Config.getPath("learnImageFolder");
		Path sourceDir = targetDir.getParent();
		Path backupDir = sourceDir.resolve("origs");

		if (!Files.exists(sourceDir))
			return;

		try {
			for (Path imgPath : dateienIn(sourceDir)) {
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
				BufferedImage links = ImageUtils.scaleSmooth(original, ZIEL_BREITE, ZIEL_HOEHE,
						Scalr.Method.ULTRA_QUALITY, Scalr.OP_ANTIALIAS);
				BufferedImage rechts = ImageUtils.scaleStepwise(original, ZIEL_BREITE, ZIEL_HOEHE);

				SelectionEnum wahl = ImageComparisonDialog.show(links, rechts);
				if (wahl == null)
					return;

				BufferedImage gewaehlt = wahl == SelectionEnum.ZERO ? links : rechts;
				if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
					gewaehlt = ImageUtils.toRgb(gewaehlt);

				Log.info(ImageScaler.class, "Verkleinern + Original sichern: " + imgPath.getFileName());
				ImageIO.write(gewaehlt, name.endsWith(".png") ? "png" : "jpg", target.toFile());
				Files.move(imgPath, backupDir.resolve(imgPath.getFileName()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Irgendwas ist beim Bilder verkleinern schiefgelaufen", e);
		}
	}

	/** Erst den Ordner einlesen, dann verarbeiten: die Schleife verschiebt die Bilder weg. */
	private static List<Path> dateienIn(Path ordner) throws Exception {
		List<Path> dateien = new ArrayList<>();
		try (DirectoryStream<Path> inhalt = Files.newDirectoryStream(ordner)) {
			for (Path eintrag : inhalt)
				dateien.add(eintrag);
		}
		return dateien;
	}
}

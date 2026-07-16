package app.learn;

import java.nio.file.Path;

import app.shared.Config;
import app.shared.image.ImageBatchProcessor;

public class ImageScaler {

    public static void processImages() {
        Path targetDir = Config.getPath("learnImageFolder");
        Path sourceDir = targetDir.getParent();
        Path backupDir = sourceDir.resolve("origs");
        ImageBatchProcessor.process(sourceDir, targetDir, backupDir, 500, 500);
    }
}
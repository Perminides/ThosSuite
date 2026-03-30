package app.misc;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class MergeJavaToTxt {

    public static void main(String[] args) throws IOException {
        Path sourceDir = Paths.get("C:/Users/Markgraf/git/ThosSuite/src/main/java/app");   // anpassen
        Path outputFile = Paths.get("C:/Users/Markgraf/Desktop/notebooklm_alles.txt"); // anpassen

        Files.createDirectories(outputFile.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {

                        // Trennlinie + Dateiname
                        writer.write("===== " + sourceDir.relativize(file) + " =====");
                        writer.newLine();
                        writer.newLine();

                        // Dateiinhalt schreiben
                        Files.lines(file).forEach(line -> {
                            try {
                                writer.write(line);
                                writer.newLine();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        writer.newLine();
                        writer.newLine();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        System.out.println("Fertig.");
    }
}
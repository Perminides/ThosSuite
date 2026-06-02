package app.scripts;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class CopyJavaAsTxt {

    public static void main(String[] args) throws IOException {
        Path sourceDir = Paths.get("C:/Users/Markgraf/git/ThosSuite/src/main/java/app");   // anpassen
        Path targetDir = Paths.get("C:/Users/Markgraf/Desktop/notebooklm");     // anpassen

        Files.createDirectories(targetDir);

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    String newName = file.getFileName().toString().replaceAll("\\.java$", ".txt");
                    Path targetFile = targetDir.resolve(newName);

                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("Fertig.");
    }
}
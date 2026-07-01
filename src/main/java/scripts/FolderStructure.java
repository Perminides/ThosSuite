package scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class FolderStructure {

    private static final String ROOT_PATH = "C:/Users/permi/git/ThosSuite/src/main/java/app";
    private static final String OUTPUT_FILE = "C:/Users/permi/git/ThosSuite/docs/Ordnerstruktur.txt";

    public static void main(String[] args) throws IOException {
        File root = new File(ROOT_PATH);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            printTree(root, 0, writer);
        }
    }

    private static void printTree(File file, int depth, BufferedWriter writer) throws IOException {
        if (!file.exists()) {
            return;
        }

        String indent = "--".repeat(depth);

        if (!file.getName().equals("migration") && !file.getName().equals("batch")) {
            writer.write(indent + (file.isDirectory() ? "📁 " : "📄 ") + file.getName());
            writer.newLine();
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, Comparator
                        .comparing(File::isDirectory)
                        .thenComparing(f -> f.getName().toLowerCase()));

                for (File child : children) {
                    if (!file.getName().equals("migration") && !file.getName().equals("batch")) {
                        printTree(child, depth + 1, writer);
                    }
                }
            }
        }
    }
}

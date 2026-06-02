package app.scripts;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FolderStructure {
	
    private static final String ROOT_PATH = "C:/Users/permi/git/ThosSuite/src/main/java/app"; // <– anpassen
	
	public static void main(String[] args) {
        File root = new File(ROOT_PATH);
        printTree(root, 0);
    }

	private static void printTree(File file, int depth) {
        if (!file.exists()) return;

        String indent = "--".repeat(depth);
        if (!file.getName().equals("migration") && !file.getName().equals("batch"))
        	System.out.println(indent + (file.isDirectory() ? "📁 " : "📄 ") + file.getName());

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, Comparator
                        .comparing(File::isDirectory)
                        .thenComparing(f -> f.getName().toLowerCase()));
                for (File child : children) {
                	if (!file.getName().equals("migration") && !file.getName().equals("batch"))
                		printTree(child, depth + 1);
                }
            }
        }
    }
}

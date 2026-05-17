package app.data.persistence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import app.config.Config;

public class FilenIgnoreSource {

	public static void addToIgnore() throws Exception {
	    Path ignoreFile = Config.get("filenIgnore.path") != null ? Path.of(Config.get("filenIgnore.path")) : null;
	    String lineToAdd = Config.get("filenIgnore.lineToAdd");
	    if (ignoreFile == null || lineToAdd == null)
	    	return;

	    List<String> lines = Files.exists(ignoreFile)
	            ? new ArrayList<>(Files.readAllLines(ignoreFile, StandardCharsets.UTF_8))
	            : new ArrayList<>();

	    if (!lines.contains(lineToAdd)) {
	        lines.add(lineToAdd);
	        Files.write(ignoreFile, lines, StandardCharsets.UTF_8);
	    }
	}

	public static void removeFromIgnore() throws Exception {
		Path ignoreFile = Config.get("filenIgnore.path") != null ? Path.of(Config.get("filenIgnore.path")) : null;
	    String lineToAdd = Config.get("filenIgnore.lineToAdd");
	    if (ignoreFile == null || lineToAdd == null)
	    	return;

	    if (Files.exists(ignoreFile)) {
	        List<String> lines = new ArrayList<>(Files.readAllLines(ignoreFile, StandardCharsets.UTF_8));
	        if (lines.remove(lineToAdd)) {
	            Files.write(ignoreFile, lines, StandardCharsets.UTF_8);
	        }
	    }
	}
	
}

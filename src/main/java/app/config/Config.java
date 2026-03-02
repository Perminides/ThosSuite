package app.config;

import java.io.*;
import java.util.Properties;

public class Config {
    private static Properties configProps = new Properties();    // Aus Datei, wird gespeichert
    private static Properties computedProps = new Properties();  // In Memory, wird NICHT gespeichert
    public static String ROOT;
    private static boolean dirty = false;
    private static File configFile;
    
    private Config() {}

    public static void init(String folderPath) {
        ROOT = folderPath;
        
        // 1. config.txt laden
        configFile = new File(folderPath, "config/config.txt");
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                configProps.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim lesen der config-Datei in " + configFile.getAbsolutePath(), e);
            }
        } else {
            throw new RuntimeException("Ich finde die Config-Datei nicht in " + configFile.getAbsolutePath());
        }
        
        // 2. Computed properties in Memory setzen
        computedProps.setProperty("rootFolder", folderPath);
        computedProps.setProperty("imageFolder", folderPath + "data/images/500x500/");
        computedProps.setProperty("deckFolder", folderPath + "data/decks/");
        computedProps.setProperty("iconFolder", folderPath + "data/icons/");
        computedProps.setProperty("geoJsonFolder", folderPath + "data/maps/geojson/");
        computedProps.setProperty("mapImagesFolder", folderPath + "data/maps/png/");
        computedProps.setProperty("wallpaperFolder", folderPath + "data/wallpapers/");
        computedProps.setProperty("dbFolder", folderPath + "data/");
        computedProps.setProperty("configFolder", folderPath + "config/");
        computedProps.setProperty("fitbitFolder", folderPath + "fitbit/");
        computedProps.setProperty("logFolder", folderPath + "log/");
        
        dirty = false;
    }

    public static String get(String key) {
        // Erst in configProps schauen
        String value = configProps.getProperty(key);
        if (value != null) return value;
        
        // Dann in computedProps
        return computedProps.getProperty(key);
    }
    
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
    
	public static int getInt(String key, int defaultValue) {
		String value = get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
	}
    
    public static void set(String key, String value) {
    	if (computedProps.containsKey(key)) {
            throw new RuntimeException("Computed property kann nicht geändert werden: " + key);
        }
    	
        String current = configProps.getProperty(key);
        
        if (current == null || !current.equals(value)) {
            configProps.setProperty(key, value);
            dirty = true;
        }
    }
    
    public static void save() {
        if (!dirty) return;
        
        if (configFile == null) {
            throw new RuntimeException("Config wurde nicht initialisiert");
        }
        
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            configProps.store(out, "ThosSuite Configuration");
            dirty = false;
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Schreiben der config-Datei in " + configFile.getAbsolutePath(), e);
        }
    }

}
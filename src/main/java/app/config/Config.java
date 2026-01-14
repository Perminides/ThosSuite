package app.config;

import java.io.*;
import java.util.Properties;

public class Config {
    private static Properties props = new Properties();
    public static String ROOT;
    
    private Config() {}

    public static void init(String folderPath) {
        props.setProperty("rootFolder", folderPath);
        props.setProperty("imageFolder", folderPath + "data/images/500x500/");
        props.setProperty("deckFolder", folderPath + "data/decks/");
        props.setProperty("iconFolder", folderPath + "data/icons/");
        props.setProperty("geoJsonFolder", folderPath + "data/maps/geojson/");
        props.setProperty("mapImagesFolder", folderPath + "data/maps/png/");
        props.setProperty("wallpaperFolder", folderPath + "data/wallpapers/");
        props.setProperty("dbFolder", folderPath + "data/");
        props.setProperty("configFolder", folderPath + "config/");
        props.setProperty("fitbitFolder", folderPath + "fitbit/");
        props.setProperty("logFolder", folderPath + "log/");
        
        ROOT = 	folderPath;
        
        File cfgFile = new File(folderPath, "config/config.txt");

        if (cfgFile.exists()) {
            try (FileInputStream in = new FileInputStream(cfgFile)) {
                props.load(in);
            } catch (IOException e) {
            	throw new RuntimeException("Fehler beim lesen der config-Datei in " + cfgFile.getAbsolutePath(), e);
            }
        } else {
        	throw new RuntimeException("Ich finde die Config-Datei nicht in " + cfgFile.getAbsolutePath());
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

}
package app.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    private static final LinkedHashMap<String, String> configProps = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> computedProps = new LinkedHashMap<>();
    public static String ROOT;
    private static boolean dirty = false;
    private static File configFile;

    private Config() {}

    public static void init(String folderPath) {
        ROOT = folderPath;

        // 1. config.txt laden
        configFile = new File(folderPath, "config/config.txt");
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int sep = line.indexOf('=');
                    if (sep < 0) continue;
                    String key = line.substring(0, sep).strip();
                    String value = line.substring(sep + 1).strip();
                    configProps.put(key, value);
                }
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Lesen der config-Datei in " + configFile.getAbsolutePath(), e);
            }
        } else {
            throw new RuntimeException("Ich finde die Config-Datei nicht in " + configFile.getAbsolutePath());
        }

        // 2. Computed properties in Memory setzen
        computedProps.put("rootFolder",      folderPath);
        computedProps.put("imageFolder",     folderPath + "data/images/");
        computedProps.put("learnImageFolder",     folderPath + "data/images/500x500/");
        computedProps.put("miscImageFolder", folderPath + "data/images/misc/");
        computedProps.put("deckFolder",      folderPath + "data/decks/");
        computedProps.put("iconFolder",      folderPath + "data/icons/");
        computedProps.put("geoJsonFolder",   folderPath + "data/maps/geojson/");
        computedProps.put("mapImagesFolder", folderPath + "data/maps/png/");
        computedProps.put("wallpaperFolder", folderPath + "data/wallpapers/");
        computedProps.put("dbFolder",        folderPath + "data/");
        computedProps.put("configFolder",    folderPath + "config/");
        computedProps.put("fitbitFolder",    folderPath + "fitbit/");
        computedProps.put("logFolder",       folderPath + "log/");

        dirty = false;
    }

    public static String get(String key) {
        String value = configProps.get(key);
        if (value != null) return value;
        return computedProps.get(key);
    }

    public static String getString(String key) {
        return get(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
    
    public static int getInt(String key) {
        String value = get(key);
        return value != null ? Integer.parseInt(value) : null;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static void set(String key, String value) {
        if (computedProps.containsKey(key)) {
            throw new RuntimeException("Computed property kann nicht geändert werden: " + key);
        }
        String current = configProps.get(key);
        if (current == null || !current.equals(value)) {
            configProps.put(key, value);
            dirty = true;
        }
    }

    public static void save() {
        if (!dirty) return;

        if (configFile == null) {
            throw new RuntimeException("Config wurde nicht initialisiert");
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.write("# ThosSuite Configuration");
            writer.newLine();
            for (Map.Entry<String, String> entry : configProps.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            dirty = false;
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Schreiben der config-Datei in " + configFile.getAbsolutePath(), e);
        }
    }
}
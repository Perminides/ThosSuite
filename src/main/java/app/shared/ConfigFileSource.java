package app.shared;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/**
 * Liest die config-Datei und haelt deren rohe Werte sowie die daraus abgeleiteten
 * (computed) Pfade. Read-only: nach dem Einlesen aendert sich hier nichts mehr.
 * <p>
 * Package-private. Nach aussen spricht nur die Config-Fassade; dieser Store ist ihr
 * interner Kollaborateur fuer die unveraenderliche Menge (Datei + computed).
 */
class ConfigFileSource {

    private final LinkedHashMap<String, String> fileProps = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> computedProps = new LinkedHashMap<>();

    ConfigFileSource(String folderPath) {
        if (!folderPath.endsWith("/") && !folderPath.endsWith("\\")) {
            folderPath = folderPath + "/";
        }

        File configFile = new File(folderPath, "config/config.txt");
        if (!configFile.exists()) {
            throw new RuntimeException("Ich finde die Config-Datei nicht in " + configFile.getAbsolutePath());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int sep = line.indexOf('=');
                if (sep < 0) {
                    continue;
                }
                String key = line.substring(0, sep).strip();
                String value = line.substring(sep + 1).strip();
                fileProps.put(key, value);
            }
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Lesen der config-Datei in " + configFile.getAbsolutePath(), e);
        }

        computedProps.put("rootFolder",       folderPath);
        computedProps.put("imageFolder",      folderPath + "data/images");
        computedProps.put("learnImageFolder", folderPath + "data/images/500x500");
        computedProps.put("miscImageFolder",  folderPath + "data/images/misc");
        computedProps.put("deckFolder",       folderPath + "data/decks");
        computedProps.put("iconFolder",       folderPath + "data/icons");
        computedProps.put("geoJsonFolder",    folderPath + "data/maps/geojson");
        computedProps.put("mapImagesFolder",  folderPath + "data/maps/png");
        computedProps.put("wallpaperFolder",  folderPath + "data/wallpapers");
        computedProps.put("dbFolder",         folderPath + "data");
        computedProps.put("configFolder",     folderPath + "config");
        computedProps.put("fitbitFolder",     folderPath + "fitbit");
        computedProps.put("logFolder",        folderPath + "log");
        
        if (!fileProps.get("attachments.folder").endsWith("/") && !fileProps.get("attachments.folder").endsWith("\\")) {
        	fileProps.put("attachments.folder", fileProps.get("attachments.folder") + "/");
        }
        computedProps.put("diaryAttachmentsFolder",        fileProps.get("attachments.folder") + "diary");
        computedProps.put("signalAttachmentsFolder",        fileProps.get("attachments.folder") + "signal");
        computedProps.put("whatsappAttachmentsFolder",        fileProps.get("attachments.folder") + "whatsapp");
    }

    /** Liegt der Key in der unveraenderlichen Menge (Datei oder computed)? */
    boolean contains(String key) {
        return fileProps.containsKey(key) || computedProps.containsKey(key);
    }

    /** Roher Wert. Wirft, wenn der Key nicht in der Datei/computed liegt (throw on miss). */
    String get(String key) {
        if (fileProps.containsKey(key)) {
            return fileProps.get(key);
        }
        if (computedProps.containsKey(key)) {
            return computedProps.get(key);
        }
        throw new RuntimeException("Config-Key fehlt in der Datei: " + key);
    }
}
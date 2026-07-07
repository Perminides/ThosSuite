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

    private final LinkedHashMap<String, String> props = new LinkedHashMap<>();
    //private final LinkedHashMap<String, String> computedProps = new LinkedHashMap<>();

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
                props.put(key, value);
            }
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Lesen der config-Datei in " + configFile.getAbsolutePath(), e);
        }

        props.put("rootFolder",       folderPath);
        props.put("imageFolder",      folderPath + "data/images");
        props.put("learnImageFolder", folderPath + "data/images/500x500");
        props.put("miscImageFolder",  folderPath + "data/images/misc");
        props.put("deckFolder",       folderPath + "data/decks");
        props.put("iconFolder",       folderPath + "data/icons");
        props.put("geoJsonFolder",    folderPath + "data/maps/geojson");
        props.put("mapImagesFolder",  folderPath + "data/maps/png");
        props.put("wallpaperFolder",  folderPath + "data/wallpapers");
        props.put("dbFolder",         folderPath + "data");
        props.put("configFolder",     folderPath + "config");
        props.put("fitbitFolder",     folderPath + "fitbit");
        props.put("logFolder",        folderPath + "log");
        
        if (!props.get("attachments.folder").endsWith("/") && !props.get("attachments.folder").endsWith("\\")) {
        	props.put("attachments.folder", props.get("attachments.folder") + "/");
        }
        props.put("diaryAttachmentsFolder",        props.get("attachments.folder") + "diary");
        props.put("signalAttachmentsFolder",        props.get("attachments.folder") + "signal");
        props.put("whatsappAttachmentsFolder",        props.get("attachments.folder") + "whatsapp");
    }

    /** Liegt der Key in der unveraenderlichen Menge (Datei oder computed)? */
    boolean contains(String key) {
        return props.containsKey(key);
    }

    /** Roher Wert. Wirft, wenn der Key nicht in der Datei/computed liegt (throw on miss). */
    String get(String key) {
        if (props.containsKey(key)) {
            return props.get(key);
        }
        throw new RuntimeException("Config-Key fehlt in der Datei: " + key);
    }
}
package app.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Liefert Bauzeit und Git-Infos des laufenden Builds aus dem Manifest der eigenen JAR.
 * Die Werte schreibt der Maven-Build (pom.xml: maven-jar-plugin + git-commit-id-Plugin).
 *
 * <p>Aus der IDE gestartet zeigt der Code-Source auf ein Klassenverzeichnis statt auf eine
 * JAR — dann gibt es kein Manifest, und alle Werte sind null.</p>
 */
public final class BuildInfo {

    private static final String KEY_BUILD_TIMESTAMP = "Build-Timestamp";
    private static final String KEY_COMMIT_ID = "Git-Commit-Id";
    private static final String KEY_COMMIT_TIME = "Git-Commit-Time";
    private static final String KEY_COMMIT_MESSAGE = "Git-Commit-Message";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Manifest manifest;
    private static boolean loaded;

    private BuildInfo() {
    }

    /** @return Bauzeitpunkt in der lokalen Zeitzone, oder null wenn nicht ermittelbar */
    public static String buildTimestamp() {
        String raw = value(KEY_BUILD_TIMESTAMP);
        if (raw == null)
            return null;
        // Im Manifest steht der Zeitstempel immer als UTC — zur Anzeige umrechnen.
        LocalDateTime utc = LocalDateTime.parse(raw, TIMESTAMP_FORMAT);
        return utc.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(TIMESTAMP_FORMAT);
    }

    /** @return Kurz-Id des letzten Commits, oder null wenn nicht ermittelbar */
    public static String commitId() {
        return value(KEY_COMMIT_ID);
    }

    /** @return Zeitpunkt des letzten Commits, oder null wenn nicht ermittelbar */
    public static String commitTime() {
        return value(KEY_COMMIT_TIME);
    }

    /** @return Kurznachricht (Betreff) des letzten Commits, oder null wenn nicht ermittelbar */
    public static String commitMessage() {
        return value(KEY_COMMIT_MESSAGE);
    }

    private static String value(String key) {
        Manifest m = manifest();
        return m == null ? null : m.getMainAttributes().getValue(key);
    }

    private static synchronized Manifest manifest() {
        if (loaded)
            return manifest;
        loaded = true;
        try {
            URL location = BuildInfo.class.getProtectionDomain().getCodeSource().getLocation();
            if (!location.getPath().endsWith(".jar"))
                return null; // IDE: Klassenverzeichnis statt JAR, kein Manifest vorhanden
            try (JarFile jar = new JarFile(Paths.get(location.toURI()).toFile())) {
                manifest = jar.getManifest();
            }
            return manifest;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}

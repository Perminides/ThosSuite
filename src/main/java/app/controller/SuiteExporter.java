package app.controller;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import app.shared.Config;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.ui.Alerts;
import app.shared.ui.DatePickerDialog;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * Sichert die Suite nach außen: sammelt unter dem Wurzelordner alle Dateien ein, die seit einem
 * im Dialog gewählten Stichtag angefasst wurden, und legt sie als AES-verschlüsseltes Zip im
 * OneDrive-Ordner ab. Was nicht mitsoll, steht als Zeilen-Regel in {@code export.ignore.txt}.
 *
 * <p><b>Warum hier und nicht woanders:</b> Der Export ist ein Querschnitt über die ganze Suite —
 * er kennt kein einzelnes Feature, sondern nur den Wurzelordner. Das ist dieselbe Sorte Sache wie
 * das Dashboard und gehört damit in die Orchestrierung.
 */
public class SuiteExporter {

    private static final String KEY_ROOT_FOLDER    = "rootFolder";
    private static final String KEY_CONFIG_FOLDER    = "configFolder";
    private static final String KEY_ONEDRIVE_FOLDER = "exporter.oneDriveFolder";
    private static final String KEY_ZIP_PASSWORD   = "exporter.zipPassword";
    private static final String IGNORE_FILE_NAME    = "export.ignore.txt";

    private Path   rootFolder;
    private Path   ignoreFile;
    private Path   oneDriveFolder;
    private String zipPassword;

    public SuiteExporter() {
    }

    public void export() {
    	
		try {
			this.rootFolder = Config.getPath(KEY_ROOT_FOLDER);
			this.ignoreFile = Config.getPath(KEY_CONFIG_FOLDER).resolve(IGNORE_FILE_NAME);
			this.oneDriveFolder = Config.getPath(KEY_ONEDRIVE_FOLDER);
			this.zipPassword = Config.getString(KEY_ZIP_PASSWORD);
		} catch (Exception e) {
			Alerts.show("Kein Export möglich", "Ich kann vermutlich einen Ordner nicht finden.", ButtonEnum.OK);
			return;
		}

    	
        LocalDate since = DatePickerDialog.show(
                "Suite Export", "Änderungen seit welchem Datum exportieren?", LocalDate.now().minusDays(7));
        if (since == null) return;

        try {
            List<IgnoreRule> rules = loadIgnoreRules();
            List<Path> files = scanFiles(since, rules);

            if (files.isEmpty()) {
            	Alerts.show("Suite Export", "Keine geänderten Dateien seit " + since + " gefunden.", ButtonEnum.OK);
                return;
            }

            Path zipPath = buildZip(files);
            Log.info(this.getClass(), "SuiteExporter: " + files.size() + " Dateien exportiert nach " + zipPath);
            Alerts.show("Suite Export", files.size() + " Dateien exportiert:\n" + zipPath.getFileName(), ButtonEnum.OK);
        } catch (Exception e) {
            throw new RuntimeException("Export fehlgeschlagen: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Ignore rules
    // -------------------------------------------------------------------------

    private record IgnoreRule(boolean isDir, Pattern pattern) {}

    private List<IgnoreRule> loadIgnoreRules() throws IOException {
        List<IgnoreRule> rules = new ArrayList<>();
        if (!Files.exists(ignoreFile)) return rules;

        for (String raw : Files.readAllLines(ignoreFile)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("dir:")) {
                rules.add(new IgnoreRule(true,  Pattern.compile(line.substring(4).strip())));
            } else if (line.startsWith("file:")) {
                rules.add(new IgnoreRule(false, Pattern.compile(line.substring(5).strip())));
            } else {
                throw new RuntimeException("SuiteExporter: unbekannte Ignore-Zeile ignoriert: " + line);
            }
        }
        return rules;
    }

    private boolean isIgnored(Path file, List<IgnoreRule> rules) {
        Path rel = rootFolder.relativize(file);
        String relParent = rel.getParent() != null ? rel.getParent().toString().replace('\\', '/') : "";

        for (IgnoreRule rule : rules) {
            if (rule.isDir()) {
                if (rule.pattern().matcher(relParent).matches()) return true;
            } else {
                if (rule.pattern().matcher(file.getFileName().toString()).matches()) return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // File scan
    // -------------------------------------------------------------------------

    private List<Path> scanFiles(LocalDate since, List<IgnoreRule> rules) throws IOException {
        List<Path> result = new ArrayList<>();
        collectFiles(rootFolder, since, rules, result);
        return result;
    }

    /**
     * Laeuft einen Ordner samt Unterordnern ab und sammelt die Dateien ein, die nicht ignoriert sind
     * und seit dem Stichtag angefasst wurden. Verknuepfungen werden nicht verfolgt.
     */
    private void collectFiles(Path ordner, LocalDate since, List<IgnoreRule> rules, List<Path> result)
            throws IOException {
        try (DirectoryStream<Path> content = Files.newDirectoryStream(ordner)) {
            for (Path eintrag : content) {
                if (Files.isDirectory(eintrag, LinkOption.NOFOLLOW_LINKS))
                    collectFiles(eintrag, since, rules, result);
                else if (Files.isRegularFile(eintrag)
                        && !isIgnored(eintrag, rules)
                        && lastModifiedDate(eintrag).compareTo(since) >= 0)
                    result.add(eintrag);
            }
        }
    }

    private LocalDate lastModifiedDate(Path p) {
        try {
            return Files.getLastModifiedTime(p).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
        } catch (IOException e) {
            throw new RuntimeException("Kann Änderungsdatum nicht lesen: " + p, e);
        }
    }

    // -------------------------------------------------------------------------
    // Zip
    // -------------------------------------------------------------------------

    private Path buildZip(List<Path> files) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
        Path zipPath = oneDriveFolder.resolve("suiteChanges-" + timestamp + ".zip");

        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(CompressionMethod.DEFLATE);
        params.setEncryptFiles(true);
        params.setEncryptionMethod(EncryptionMethod.AES);
        params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), zipPassword.toCharArray())) {
            for (Path file : files) {
                Path relative = rootFolder.relativize(file);
                params.setFileNameInZip(relative.toString().replace('\\', '/'));
                zipFile.addFile(file.toFile(), params);
            }
        }
        return zipPath;
    }
}
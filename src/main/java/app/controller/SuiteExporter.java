package app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import app.shared.Config;
import app.shared.Log;
import app.ui.skin.SkinService;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

// TODO: Nur in controller geschoben, damit das mit den Pfeilen besser aussieht. Das kann ja aber auch nicht sein.
// Gehört an sich eher in shared. Aber braucht halt SkinService zum Zeigen eines Alerts. Was tun?
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

    private Optional<LocalDate> showDatePickerDialog() {
    	DatePicker picker = SkinService.get().createDatePicker(LocalDate.now().minusDays(7));

        Dialog<?> dialog = SkinService.get().createDialog(null, "Suite Export");
        VBox content = SkinService.get().createDialogContent();
        content.getChildren().add(new javafx.scene.control.Label("Änderungen seit welchem Datum exportieren?"));
        content.getChildren().add(picker);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<?> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().equals(ButtonType.CANCEL)) return Optional.empty();
        if (picker.getValue() == null) return Optional.empty();
        return Optional.of(picker.getValue());
    }

    public void export() {
    	
		try {
			this.rootFolder = Paths.get(Config.getString(KEY_ROOT_FOLDER));
			this.ignoreFile = Paths.get(Config.getString(KEY_CONFIG_FOLDER) + IGNORE_FILE_NAME);
			this.oneDriveFolder = Paths.get(Config.getString(KEY_ONEDRIVE_FOLDER));
			this.zipPassword = Config.getString(KEY_ZIP_PASSWORD);
		} catch (Exception e) {
			SkinService.get().createAlert(SkinService.getOwnerWindow(), "Kein Export möglich", "Ich kann vermutlich einen Ordner nicht finden.", false, false)
					.showAndWait();
			return;
		}

    	
        Optional<LocalDate> since = showDatePickerDialog();
        if (since.isEmpty()) return;

        try {
            List<IgnoreRule> rules = loadIgnoreRules();
            List<Path> files = scanFiles(since.get(), rules);

            if (files.isEmpty()) {
                SkinService.get().createAlert(null, "Suite Export", "Keine geänderten Dateien seit " + since.get() + " gefunden.", ButtonType.OK).showAndWait();
                return;
            }

            Path zipPath = buildZip(files);
            Log.info(this.getClass(), "SuiteExporter: " + files.size() + " Dateien exportiert nach " + zipPath);
            SkinService.get().createAlert(null, "Suite Export", files.size() + " Dateien exportiert:\n" + zipPath.getFileName(), ButtonType.OK).showAndWait();

        } catch (Exception e) {
            new RuntimeException("Export fehlgeschlagen: " + e.getMessage());
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
                new RuntimeException("SuiteExporter: unbekannte Ignore-Zeile ignoriert: " + line);
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
        Files.walk(rootFolder)
             .filter(Files::isRegularFile)
             .filter(p -> !isIgnored(p, rules))
             .filter(p -> lastModifiedDate(p).compareTo(since) >= 0)
             .forEach(result::add);
        return result;
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
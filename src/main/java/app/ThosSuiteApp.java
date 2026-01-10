package app;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import app.config.Config;
import app.controller.Controller;
import app.data.AppClock;
import app.ui.MainWindow;
import app.util.Log;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class ThosSuiteApp extends Application {
	
	// TODO: In Amerika fehlt ein Strich zwischen Utah und Arizona
	// TODO: Vergiss das Bilder verkleinern nicht. JavaFX sollte das besser können...
	
	/** JavaFX-Nachteile
	 * 
	 * https://bugs.openjdk.org/browse/JDK-8227679
	 * Font-Rendering schlechter → Wäre ein Showstopper gewesen, aber ist seit "Adjust Clear Type Text" nicht mehr vorhanden und konnte auch auf anderen Laptops nicht reproduziert werden...
	 * Icon-Spacing umständlich
     * Kein HTML in Labels. Keine einzelnen Wörte fett. Wir haben einen Work-Around implementiert. Glaube, dass man das so nennen muss...
     * Anfangsruckler beim Hovereffekt in der Deutschlandkarte
     * Für Soft-Hyphens hat auch JavaFX keine vernünftige Lösung. Und naja, siehe oben zu HTML in Labels...
     * 
	 */

    private MainWindow mainWindow;
    @SuppressWarnings("unused")
    private Controller controller;

    public static void main(String[] args) {
        launch(args); // JavaFX Application.launch() startet die App und den JavaFX Application Thread
        Log.info(ThosSuiteApp.class, "End Suite");
    }

    @Override
    public void init() throws Exception {
        AppClock.init();

        // UncaughtExceptionHandler mit JUL-Check
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            // Prüfen ob JUL konfiguriert ist
            boolean julConfigured = java.util.logging.LogManager.getLogManager().getLogger("").getHandlers().length > 0;
            
            if (julConfigured) {
                Log.error(ThosSuiteApp.class, "Uncaught exception in thread " + thread.getName(), ex);
            } else {
                System.err.println("Uncaught exception in thread " + thread.getName());
                ex.printStackTrace(System.err);
            }
            
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("ThosSuite Fehler");
                alert.setHeaderText("Ein Fehler ist aufgetreten");
                
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                
                TextArea textArea = new TextArea(sw.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(40);
                textArea.setPrefColumnCount(160);
                
                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
                
                System.exit(1);
            });
        });
    }

    @Override
    public void start(Stage primaryStage) {  	
        // DataFolder aus Parametern oder DirectoryChooser
        String dataFolder = getDataFolder();
        if (dataFolder == null) {
            System.exit(-1);
            return;
        }
     // Log-Verzeichnis erstellen
        File logDir = new File(dataFolder + "log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // Config initialisieren (MUSS vor SkinService passieren!)
        Config.init(dataFolder);
        Log.initLog(dataFolder, getParameters());
        Log.info(ThosSuiteApp.class, "Start Suite (Logging finally enabled :))");
        
     // Font laden (JavaFX-Style)
        try {
            Font font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos.ttf"), 20);
            Log.debug(this, font != null ? "Aptos Font geladen" : "Aptos nicht geladen");
            font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Bold.ttf"), 20);
            Log.debug(this, font != null ? "Aptos-Bold Font geladen" : "Aptos-Bold nicht geladen");
            font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Bold-Italic.ttf"), 20);
            Log.debug(this, font != null ? "Aptos-Bold-Italic Font geladen" : "Aptos-Bold-Italic nicht geladen");
            font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Italic.ttf"), 20);
            Log.debug(this, font != null ? "Aptos-Italic Font geladen" : "Aptos-Italic nicht geladen");
        } catch (Exception e) {
            throw new RuntimeException("Probleme beim Laden der Fonts: Aptos", e);
        }
        
        mainWindow = new MainWindow(primaryStage);
        // mainWindow wird gleich gestylet, aber erst noch: Icons setzen
        try {
            String iconFolder = Config.get("iconFolder") + "suite_icon/";
            mainWindow.getIcons().addAll(
                new Image(new File(iconFolder + "mondrian_rounded_16.png").toURI().toString()),
                new Image(new File(iconFolder + "mondrian_rounded_24.png").toURI().toString()),
                new Image(new File(iconFolder + "mondrian_rounded_32.png").toURI().toString()),
                new Image(new File(iconFolder + "mondrian_rounded_64.png").toURI().toString()),
                new Image(new File(iconFolder + "mondrian_rounded_128.png").toURI().toString()),
                new Image(new File(iconFolder + "mondrian_rounded_256.png").toURI().toString())
            );
        } catch (Exception e) {
            throw new RuntimeException("Probleme beim Laden der Icons", e);
        }
        mainWindow.buildStyledUi();

        // Controller erstellen
        controller = new Controller(mainWindow);

        // Fenster zentrieren und anzeigen
        mainWindow.centerOnScreen();
        mainWindow.show();
    }

	@Override
    public void stop() throws Exception {
        Log.debug(this, "App wird gestoppt, räume auf...");
        app.data.persistence.DB.closeConnection();
        super.stop();
    }

    private String getDataFolder() {
        // Erst Parameter prüfen
        Parameters params = getParameters();
        java.util.List<String> args = params.getRaw();
        
        if (!args.isEmpty()) {
            String dataFolder = args.get(0);
            if (new File(dataFolder).isDirectory()) {
                return dataFolder;
            }
        }

        // Fallback: DirectoryChooser
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Wo finde ich denn die Dateien?");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
        
        File selectedDir = dirChooser.showDialog(null);
        if (selectedDir != null) {
            return selectedDir.getAbsolutePath() + "/";
        }
        
        return null;
    }
}
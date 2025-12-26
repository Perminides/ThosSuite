package app;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

import app.config.Config;
import app.controller.Controller;
import app.data.AppClock;
import app.ui.MainWindow;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
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
        System.out.println("Start Suite: " + LocalDateTime.now());
        // JavaFX Application.launch() startet die App
        launch(args);
        System.out.println("End Suite: " + LocalDateTime.now());
    }

    @Override
    public void init() throws Exception {
        // Wird VOR start() aufgerufen, aber NACH JavaFX-Initialisierung
        // Hier können wir die nicht-UI Sachen machen
        
        AppClock.init();

        // UncaughtExceptionHandler für JavaFX
        Thread.setDefaultUncaughtExceptionHandler((_, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            
            // JavaFX Alert statt JOptionPane
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("ThosSuite Fehler");
                alert.setHeaderText("Ein Fehler ist aufgetreten");
                
                TextArea textArea = new TextArea(trace);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(40);
                textArea.setPrefColumnCount(160);
                
                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
                
                ex.printStackTrace();
                System.exit(1);
            });
        });

        // Font laden (JavaFX-Style)
        try {
            Font font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos.ttf"), 20);
            System.out.println(font != null ? "Aptos Font geladen" : "Aptos nicht geladen");
            font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Bold.ttf"), 20);
            System.out.println(font != null ? "Aptos-Bold Font geladen" : "Aptos-Bold nicht geladen");
            font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Bold-Italic.ttf"), 20);
            System.out.println(font != null ? "Aptos-Bold-Italic Font geladen" : "Aptos-Bold-Italic nicht geladen");
            font = Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Italic.ttf"), 20);
            System.out.println(font != null ? "Aptos-Italic Font geladen" : "Aptos-Italic nicht geladen");
        } catch (Exception e) {
            throw new RuntimeException("Probleme beim Laden der Fonts: Aptos", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // DataFolder aus Parametern oder DirectoryChooser
        String dataFolder = getDataFolder();
        if (dataFolder == null) {
            System.exit(-1);
            return;
        }

        // Config initialisieren (MUSS vor SkinService passieren!)
        Config.init(dataFolder);

        // Skin holen und MainWindow erstellen
        Skin skin = SkinService.get();
        mainWindow = new MainWindow(primaryStage);
        
        // Icons setzen
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
        
        System.out.println("Hauptfenster ist da: " + LocalDateTime.now());
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
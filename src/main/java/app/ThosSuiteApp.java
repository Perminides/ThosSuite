package app;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import app.config.Config;
import app.controller.Controller;
import app.data.AppClock;
import app.data.persistence.DB;
import app.data.persistence.FilenIgnoreSource;
import app.data.persistence.TmdbMovieRepository;
import app.tmdb.TmdbApiClient;
import app.tmdb.TmdbImporter;
import app.ui.MainWindow;
import app.util.Log;
import app.util.SingleInstanceGuard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ThosSuiteApp extends Application {
	
	// TODO: In Amerika fehlt ein Strich zwischen Utah und Arizona
	
	/** JavaFX-Nachteile
	 * 
	 * https://bugs.openjdk.org/browse/JDK-8227679
	 * Font-Rendering schlechter → Wäre ein Showstopper gewesen, aber ist seit "Adjust Clear Type Text" nicht mehr vorhanden und konnte auch auf anderen Laptops nicht reproduziert werden...
	 * Icon-Spacing umständlich
     * Kein HTML in Labels. Keine einzelnen Wörte fett. Wir haben einen Work-Around implementiert. Glaube, dass man das so nennen muss...
     * Anfangsruckler beim Hovereffekt in der Deutschlandkarte
     * Für Soft-Hyphens hat auch JavaFX keine vernünftige Lösung. Und naja, siehe oben zu HTML in Labels...
     * Es gibt keine Möglichkeit eine Table ein einem Alert anzuzeigen, der genau so groß ist, wie die Tabelle sein muss. Es ist nicht möglich. Ich habe 1.5 Tage an meinem Fitbit Alert gesessen und muss leider aufgeben. Superhack gefunden...
     * Es ist sauschwierig einen Alert/Dialog ohne parent-Window mittig ohne Ruckler anzuzeigen. Also beim startup, wenn das Spielfeld noch nicht existiert...
     * 
	 */

    private MainWindow mainWindow;
    private Controller controller;

    public static void main(String[] args) {
    	// JavaFX DatePicker nutzt Locale.getDefault(Category.FORMAT) für Monatsnamen.
    	// Windows-Regionsformat kann abweichen von der Sprache → explizit auf Deutsch setzen.
    	Locale.setDefault(Locale.GERMANY);    	
        launch(args); // JavaFX Application.launch() startet die App und den JavaFX Application Thread
        Log.info(ThosSuiteApp.class, "End Suite");
        
     // Zeige alle lebenden Non-Daemon Threads !Sofort: Das ist ein Hack weil das Programm manchmal nach diesem Logging noch weiterläuft...
        Thread.getAllStackTraces().keySet().stream()
            .filter(t -> !t.isDaemon())  // Nur Non-Daemon Threads (die halten JVM am Leben)
            .forEach(t -> {
                Log.info(ThosSuiteApp.class, "Non-Daemon Thread: " + t.getName());
            });
    }

    @Override
    public void init() throws Exception {
        AppClock.init();
        setupGlobalExceptionHandler();
    }

    @Override
    public void start(Stage primaryStage) {
    	// 0. Zeitzone überprüfen
    	ZoneId expected = ZoneId.of("Europe/Berlin");
    	ZoneId current = ZoneId.systemDefault();
    	if (!current.equals(expected)) {
    	    Alert alert = new Alert(Alert.AlertType.WARNING);
    	    alert.setTitle("Falsche Zeitzone");
    	    alert.setHeaderText("Die Suite läuft in der Zeitzone " + current + " statt " + expected);
    	    alert.setContentText("Timestamps werden falsch berechnet. (Beim Message Import z.B.)\n\nTrotzdem starten?");
    	    ButtonType yes = new ButtonType("Ja, trotzdem starten", ButtonBar.ButtonData.YES);
    	    ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
    	    alert.getButtonTypes().setAll(yes, cancel);
    	    Optional<ButtonType> result = alert.showAndWait();
    	    if (result.isEmpty() || result.get() != yes)
    	        Platform.exit();
    	}
    	
        // 1. Splash Screen erstellen und anzeigen
        Stage splashStage = new Stage();
        showSplashScreen(splashStage);

        // 2. DataFolder ermitteln
        // Zuerst schauen wir nur in die Argumente (das geht ohne UI)
        String dataFolder = getDataFolderFromArgs();

        // Wenn kein Argument da ist, müssen wir den User fragen
        if (dataFolder == null) {            
            dataFolder = showDirectoryChooser();
            
            // Wenn User abbricht -> Ende
            if (dataFolder == null) {
                System.exit(-1);
                return;
            }
        }
        
        // Läuft die Suite bereits?
        if (!SingleInstanceGuard.lockInstance(Path.of(dataFolder + "log/suite.lock"))) {
        	Alert alert = new Alert(AlertType.WARNING);
        	alert.setTitle("Programm bereits gestartet");
        	alert.setHeaderText(null);
        	alert.setContentText("Die Suite läuft bereits.");
        	alert.getDialogPane().setStyle("-fx-font-size: 18px;");
        	alert.showAndWait();
            System.exit(0);
        }
        
        // Locks auf die Log-Dateien entfernen, falls beim letzten mal abgestürzt.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(dataFolder + "log") , "*.lck")) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Probleme beim Entfernen der Locks auf die log-Dateien", e);
        }
        
        // Variable final machen für den Thread
        final String finalDataFolder = dataFolder;

        // 3. Die eigentliche Initialisierung im Hintergrund starten
        new Thread(() -> {
            try {
                // A) Log & Config & DB
                File logDir = new File(finalDataFolder + "log");
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }

                Config.init(finalDataFolder);
                Log.initLog(finalDataFolder, getParameters());
                Log.info(ThosSuiteApp.class, "Start Suite (Async Init via Splash)");
                Log.info(ThosSuiteApp.class, "prism.allowhidpi: " + System.getProperty("prism.allowhidpi"));
                
                FilenIgnoreSource.addToIgnore();

                // B) Fonts laden
                loadFonts();

                // C) Zurück in den UI-Thread
				Platform.runLater(() -> {
					try {
						// Pre-Tasks (Splash noch sichtbar)
						initializeMainWindow(primaryStage);
						primaryStage.setOpacity(0); // MainWindow unsichtbar starten
						
						//TmdbApiClient client = new TmdbApiClient();
						//client.getImage("/rxeDxo8FvZpLu6iplNpxdtAVnfu.jpg", "w154");
						//TmdbMovieRepository repo = new TmdbMovieRepository();
						//System.out.println(repo.getMovieRating(10));
						new TmdbImporter().run();
						
						controller = new Controller(mainWindow);
						controller.runPreTasks();

						// MainWindow zeigen (aber unsichtbar via opacity=0)
						// Grund: JavaFX rendert Fenster BEVOR CSS vollständig angewendet ist.
						// Bei komplexem CSS (hunderte Zeilen, dutzende Selektoren) führt das zu
						// einem sichtbaren "White Flash" - das Fenster erscheint erst weiß, dann
						// mit Styling. Je komplexer das CSS und das Fenster, desto länger dauert die Anwendung,
						// desto deutlicher der Flash.
						//
						// Lösung: Fenster mit opacity=0 anzeigen, 500ms warten (CSS hat Zeit sich
						// zu setzen), dann opacity=1. Der Splash bleibt währenddessen sichtbar,
						// so dass der User keine leere Fläche sieht.
						mainWindow.show();
						mainWindow.centerOnScreen();

						// 500ms warten, damit CSS vollständig angewendet wird
						javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
						pause.setOnFinished(_ -> {
							splashStage.close();

							// Platform.runLater nötig, weil showAndWait() nicht während einer
							// Animation aufgerufen werden darf (IllegalStateException).
							// Das runLater verschiebt die Ausführung in den nächsten Frame,
							// wo die Animation bereits beendet ist.
							Platform.runLater(() -> {
								controller.runPostTasks(); // Zeigt Startup-Dialoge (Fitbit, Alkohol)
								primaryStage.setOpacity(1); // MainWindow wird NACH den Dialogen sichtbar
								// Nochmal runLater für toFront - kommt dann garantiert nach allem
							    Platform.runLater(() -> {
							        primaryStage.toFront();
							        primaryStage.requestFocus();
							    });
							});
						});
						pause.play();

					} catch (Exception e) {
						Log.error(ThosSuiteApp.class, "Fehler beim UI-Start", e);
						throw new RuntimeException(e);
					}
				});

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR, "Kritischer Fehler: " + e.getMessage());
                    alert.showAndWait();
                    System.exit(1);
                });
            }
        }).start();
    }

    // --- Neue Hilfsmethoden ---

    private String getDataFolderFromArgs() {
        Parameters params = getParameters();
        java.util.List<String> args = params.getRaw();
        
        if (!args.isEmpty()) {
            String dataFolder = args.get(0);
            if (new File(dataFolder).isDirectory()) {
                return dataFolder;
            }
        }
        return null;
    }

    private String showDirectoryChooser() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Wo finde ich denn die Dateien?");
        try {
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
        } catch (Exception e) { /* fallback */ }
        
        File selectedDir = dirChooser.showDialog(null); // null ist okay, da wir kein Parent-Window haben
        if (selectedDir != null) {
            return selectedDir.getAbsolutePath() + "/";
        }
        return null;
    }

    /**
     * Baut den Splash-Screen (30% Monitorbreite, Transparent, Zentriert)
     */
    private void showSplashScreen(Stage splashStage) {
        try {
            // 1. Monitor-Infos holen
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            double screenWidth = screenBounds.getWidth();
            double screenHeight = screenBounds.getHeight();

            // 2. Zielbreite berechnen (30% vom Monitor)
            double targetWidth = screenWidth * 0.3;

            // 3. Bild laden
            Image image = new Image(getClass().getResourceAsStream("/GeminiSplash.png"));
            
            // 4. Höhe basierend auf dem Bild-Verhältnis berechnen
            // (Damit wir die Y-Koordinate zum Zentrieren berechnen können)
            double originalWidth = image.getWidth();
            double originalHeight = image.getHeight();
            double ratio = originalHeight / originalWidth;
            double targetHeight = targetWidth * ratio;

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(targetWidth);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            StackPane root = new StackPane(imageView);
            root.setStyle("-fx-background-color: transparent;");

            Scene scene = new Scene(root, Color.TRANSPARENT);
            splashStage.setScene(scene);
            splashStage.initStyle(StageStyle.TRANSPARENT);
            //splashStage.setAlwaysOnTop(true);

            // 5. WICHTIG: Koordinaten manuell setzen (Zentrieren)
            // Formel: Startpunkt + (Gesamtbreite - Fensterbreite) / 2
            splashStage.setX(screenBounds.getMinX() + (screenWidth - targetWidth) / 2);
            splashStage.setY(screenBounds.getMinY() + (screenHeight - targetHeight) / 2);

            splashStage.show();
            
        } catch (Exception e) {
            System.err.println("Konnte Splash-Screen nicht laden: " + e.getMessage());
        }
    }

    /**
     * Die Logik zum Aufbau des Hauptfensters (läuft wieder im FX Thread)
     * Holt sich die Icons, erstellt das MainWindow und den Controller...
     */
    private void initializeMainWindow(Stage primaryStage) {
        mainWindow = new MainWindow(primaryStage);
        
        // Icons laden
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
        
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            controller.requestSessionSwitch(Platform::exit);
        });
    }

    private void loadFonts() {
        try {
            Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos.ttf"), 20);
            Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Bold.ttf"), 20);
            Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Bold-Italic.ttf"), 20);
            Font.loadFont(ThosSuiteApp.class.getClassLoader().getResourceAsStream("Aptos-Italic.ttf"), 20);
        } catch (Exception e) {
            throw new RuntimeException("Probleme beim Laden der Fonts: Aptos", e);
        }
    }

    @Override
    public void stop() throws Exception {
        Log.debug(this, "App wird gestoppt, räume auf...");
        
        // Config speichern
        try {
            app.config.Config.save();
        } catch (Exception e) {
            Log.error(this, "Fehler beim Speichern der Config", e);
        }
        
        // DB schließen
        try {
            DB.closeConnection(); // Sicherstellen, dass DB geschlossen wird (auch wenn Controller evtl. null ist bei Fehler)
            FilenIgnoreSource.removeFromIgnore();
        } catch (Exception e) {
        	Log.error(this, "Fehler beim Schließen der DB-Connection oder beim Zurückkopieren", e);
        }
        
        super.stop();
    }

    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            boolean julConfigured = false;
            try {
                julConfigured = java.util.logging.LogManager.getLogManager().getLogger("").getHandlers().length > 0;
            } catch (Exception e) { /* ignore */ }
            
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
                
                // ScenicView-Exceptions ignorieren (externe Library, kein App-Fehler)
                if (ex.toString().contains("org.fxconnector") || ex.toString().contains("ScenicView")) {
                    return; // Nicht crashen für externes Tool
                }
                
                Platform.exit();
            });
        });
    }
}
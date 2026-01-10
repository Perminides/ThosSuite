package app.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import app.ThosSuiteApp;
import javafx.application.Application.Parameters;

/**
 * Zentrale Logging-Facade für die ThosSuite.
 * 
 * <p>Nutzt java.util.logging (JUL) - das Java-Standard-Logging-Framework ohne externe Dependencies.
 * 
 * <h3>Warum java.util.logging statt Logback?</h3>
 * <p>Ursprünglich sollte Logback als Logging-Framework verwendet werden. Dies führte jedoch zu
 * unlösbaren Problemen mit dem Java Module System (JPMS):
 * <ul>
 *   <li>JavaFX läuft als Module (--module-path, --add-modules)</li>
 *   <li>Logback läuft auf dem Classpath (non-modular)</li>
 *   <li>SLF4J's ServiceLoader konnte Logback nicht finden (NOPLogger)</li>
 *   <li>Workarounds (--add-opens, manueller module-path) waren zu komplex und fehleranfällig</li>
 * </ul>
 * 
 * <p>java.util.logging ist Teil der Java-Runtime und funktioniert problemlos mit dem Module System.
 * Es bietet alle benötigten Features:
 * <ul>
 *   <li>File-Logging mit automatischem Rolling (Size-based)</li>
 *   <li>Flexible Log-Levels (FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE)</li>
 *   <li>Console- und File-Handler mit Custom-Formattern</li>
 *   <li>Keine externe Dependencies - funktioniert immer</li>
 * </ul>
 * 
 * <h3>Verwendung:</h3>
 * <pre>
 * // In Instanz-Methoden:
 * Log.info(this, "Session started");
 * Log.debug(this, "Entering method");
 * Log.error(this, "Failed to load cards", exception);
 * 
 * // In statischen Methoden:
 * Log.info(MyClass.class, "Static initialization");
 * Log.error(Config.class, "Config file not found", exception);
 * </pre>
 * 
 * <h3>Konfiguration:</h3>
 * <p>Das Logging wird in {@code ThosSuiteApp.start()} programmatisch konfiguriert:
 * <ul>
 *   <li>FileHandler: Logs ab INFO-Level in Datei (10MB, 5 Rotationen)</li>
 *   <li>ConsoleHandler: Logs ab FINE-Level in Eclipse-Console</li>
 *   <li>Mit --debug Parameter: FileHandler loggt auch FINE-Level</li>
 * </ul>
 * 
 * @see ThosSuiteApp#start(javafx.stage.Stage)
 */
public class Log {
	
    public static void initLog(String dataFolder, Parameters params) {
    	// Debug-Mode prüfen
    	boolean debugMode = params.getRaw().contains("--debug");
    	
    	// GEMEINSAMER Formatter für File UND Console
        Formatter commonFormatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
            	return String.format("%1$tF %1$tT [%2$s] %3$s - %4$s%n",
            		    LocalDateTime.ofInstant(
            		        Instant.ofEpochMilli(record.getMillis()), 
            		        ZoneId.systemDefault()
            		    ),
            		    record.getLevel().getName(),
            		    record.getLoggerName(),
            		    formatMessage(record)
            		);
            }
        };

        // JUL programmatisch konfigurieren
        try {
            // FileHandler mit korrektem Pfad
            String logPattern = dataFolder + "log/thossuite%u.log";
            FileHandler fileHandler = new FileHandler(logPattern, 10485760, 5, true);
            fileHandler.setLevel(debugMode ? Level.FINE : Level.INFO);
            fileHandler.setFormatter(commonFormatter); // <- Gemeinsamer Formatter
            
            // StreamHandler für Console (stdout statt stderr)
            StreamHandler consoleHandler = new StreamHandler(System.out, commonFormatter) { // <- Gemeinsamer Formatter
                @Override
                public synchronized void publish(LogRecord record) {
                    super.publish(record);
                    flush();
                }
            };
            consoleHandler.setLevel(Level.ALL);
            
            // Root-Logger konfigurieren
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.FINE);
            
            // Alte Handler entfernen
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
            
            // Neue Handler hinzufügen
            rootLogger.addHandler(fileHandler);
            rootLogger.addHandler(consoleHandler);
            
            // Java-interne Messages unterdrücken
            Logger.getLogger("java").setLevel(Level.WARNING);
            Logger.getLogger("javafx").setLevel(Level.WARNING);
            Logger.getLogger("sun.rmi").setLevel(Level.WARNING);
            Logger.getLogger("sun.rmi.transport").setLevel(Level.WARNING);
            Logger.getLogger("sun.rmi.transport.tcp").setLevel(Level.WARNING);
            
            Log.debug(ThosSuiteApp.class, "Logging initialisiert");
            
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException("Fehler beim Initialisieren von Logging:", e);
        }		
	}
    
    // === DEBUG ===
    
    public static void debug(Object caller, String message) {
        debug(caller.getClass(), message);
    }
    
    public static void debug(Class<?> callerClass, String message) {
        Logger.getLogger(callerClass.getName()).log(Level.FINE, message);
    }
    
    // === INFO ===
    
    public static void info(Object caller, String message) {
        info(caller.getClass(), message);
    }
    
    public static void info(Class<?> callerClass, String message) {
        Logger.getLogger(callerClass.getName()).log(Level.INFO, message);
    }
    
    // === WARN ===
    
    public static void warn(Object caller, String message) {
        warn(caller.getClass(), message);
    }
    
    public static void warn(Class<?> callerClass, String message) {
        Logger.getLogger(callerClass.getName()).log(Level.WARNING, message);
    }
    
    // === ERROR ===
    
    public static void error(Object caller, String message, Throwable ex) {
        error(caller.getClass(), message, ex);
    }
    
    public static void error(Object caller, String message) {
        error(caller.getClass(), message, null);
    }
    
    public static void error(Class<?> callerClass, String message, Throwable ex) {
        Logger logger = Logger.getLogger(callerClass.getName());
        if (ex != null) {
            logger.log(Level.SEVERE, message, ex);
        } else {
            logger.log(Level.SEVERE, message);
        }
    }
    
    public static void error(Class<?> callerClass, String message) {
        error(callerClass, message, null);
    }
}
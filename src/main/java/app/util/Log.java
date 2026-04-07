package app.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
 * <h3>WICHTIG: Garbage Collection und Logger</h3>
 * <p>JUL (java.util.logging) hält Logger nur als WeakReferences. Ohne harte Referenz kann der
 * Garbage Collector konfigurierte Logger entfernen. Beim nächsten Zugriff wird ein neuer Logger
 * mit Default-Einstellungen erzeugt.
 * 
 * <p>Lösung: {@link #KEEP_ALIVE_LOGGERS} hält harte Referenzen auf konfigurierte Logger,
 * damit der GC sie nicht abräumt.
 * 
 * @see ThosSuiteApp#start(javafx.stage.Stage)
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/logging/LogManager.html">JUL LogManager Dokumentation</a>
 */
public class Log {
    
    /**
     * Harte Referenzen auf konfigurierte Logger.
     * 
     * <p><b>Warum notwendig?</b>
     * JUL's LogManager speichert Logger als WeakReferences. Wenn keine harte Referenz
     * auf einen Logger existiert, kann der Garbage Collector ihn entfernen.
     * 
     * <p><b>Problem:</b>
     * <ol>
     *   <li>Wir konfigurieren z.B. jdk.event.security auf Level.WARNING</li>
     *   <li>Die lokale Variable endet (z.B. am Ende von initLog())</li>
     *   <li>GC läuft und entfernt den Logger (nur WeakReference im LogManager)</li>
     *   <li>Später ruft Java intern Logger.getLogger("jdk.event.security") auf</li>
     *   <li>LogManager findet keinen Logger mehr → erzeugt neuen mit Default-Einstellungen</li>
     *   <li>Neuer Logger erbt Level vom Root-Logger → unsere Konfiguration ist weg!</li>
     * </ol>
     * 
     * <p><b>Lösung:</b>
     * Diese static final Liste hält harte Referenzen. Der GC kann diese Logger nie entfernen.
     * 
     * <p><b>Welche Logger hier aufnehmen?</b>
     * Alle Logger die wir explizit konfigurieren UND die nicht von der App selbst genutzt werden.
     * App-Logger (z.B. "app.config.Config") werden durch Nutzung automatisch am Leben gehalten.
     * 
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/logging/LogManager.html">
     *      JUL Dokumentation zu WeakReferences</a>
     */
    private static final List<Logger> KEEP_ALIVE_LOGGERS = List.of(
        Logger.getLogger("jdk.event.security"),     // Java Security Events (SSL-Zertifikate etc.)
        Logger.getLogger("java"),                    // Java-interne Messages
        Logger.getLogger("javafx"),                  // JavaFX-Framework Messages
        Logger.getLogger("sun.rmi"),                 // RMI-Framework (falls genutzt)
        Logger.getLogger("sun.rmi.transport"),       // RMI Transport Layer
        Logger.getLogger("sun.rmi.transport.tcp")    // RMI TCP Transport
    );
    
    /**
     * Initialisiert das Logging-System programmatisch.
     * 
     * <p><b>Warum programmatische Konfiguration statt logging.properties?</b>
     * <ul>
     *   <li>Dynamischer Log-Pfad (abhängig von dataFolder)</li>
     *   <li>Debug-Mode über Command-Line-Parameter steuerbar</li>
     *   <li>Eigener Formatter für beide Handler (File + Console)</li>
     *   <li>Einfacher wartbar - alles an einer Stelle im Code</li>
     * </ul>
     * 
     * <p><b>Konfigurierte Handler:</b>
     * <ol>
     *   <li><b>FileHandler:</b>
     *     <ul>
     *       <li>Pfad: {dataFolder}/log/thossuite%u.log</li>
     *       <li>Max Size: 10MB pro Datei</li>
     *       <li>Rotationen: 5 Dateien (thossuite0.log bis thossuite4.log)</li>
     *       <li>Append: true (existierende Logs werden erweitert)</li>
     *       <li>Level: INFO (oder FINE im Debug-Mode)</li>
     *     </ul>
     *   </li>
     *   <li><b>ConsoleHandler (StreamHandler):</b>
     *     <ul>
     *       <li>Output: System.out (nicht stderr!)</li>
     *       <li>Level: ALL (zeigt alles was der Root-Logger durchlässt)</li>
     *       <li>Flush: nach jedem Log (wichtig für Eclipse-Console)</li>
     *     </ul>
     *   </li>
     * </ol>
     * 
     * <p><b>Root-Logger Konfiguration:</b>
     * <ul>
     *   <li>Level: FINE (erlaubt Debug-Logs für App-Code)</li>
     *   <li>Alte Handler werden entfernt (Standard Console-Handler etc.)</li>
     * </ul>
     * 
     * <p><b>Unterdrückte Logger:</b>
     * Java-interne und Framework-Logger werden auf WARNING gesetzt um Spam zu vermeiden:
     * <ul>
     *   <li>jdk.event.security - SSL-Zertifikats-Loading (FINE-Level Spam)</li>
     *   <li>java.* - Java-Runtime Messages</li>
     *   <li>javafx.* - JavaFX-Framework Messages</li>
     *   <li>sun.rmi.* - RMI-Framework (falls aktiviert)</li>
     * </ul>
     * 
     * <p><b>WICHTIG:</b> Diese Logger werden in {@link #KEEP_ALIVE_LOGGERS} hart referenziert,
     * damit der Garbage Collector sie nicht abräumt!
     * 
     * @param dataFolder Basis-Pfad für Log-Dateien (z.B. "C:/Users/Name/Desktop/daten/")
     * @param params Application Parameters (für --debug Flag)
     * @throws RuntimeException bei Fehler (z.B. Log-Datei nicht erstellbar)
     */
    public static void initLog(String dataFolder, Parameters params) {
        // Debug-Mode aus Command-Line-Parametern lesen
        boolean debugMode = params.getRaw().contains("--debug");
        
        // === FORMATTER ===
        // Gemeinsamer Formatter für File UND Console
        // Format: "YYYY-MM-DD HH:MM:SS [LEVEL] logger.name - message"
        Formatter commonFormatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("%1$tF %1$tT [%2$s] %3$s - %4$s%n",
                    // %1 = LocalDateTime (aus LogRecord Timestamp)
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(record.getMillis()), 
                        ZoneId.systemDefault()
                    ),
                    // %2 = Level (INFO, WARNING, SEVERE, FINE, etc.)
                    record.getLevel().getName(),
                    // %3 = Logger Name (z.B. "app.config.Config")
                    record.getLoggerName(),
                    // %4 = Formatierte Message (mit StackTrace falls vorhanden)
                    formatMessage(record)
                );
            }
        };

		try {
			// === FILE HANDLER ===
			// %u = eindeutige Nummer für Rotation (0-4)
			String logPattern = dataFolder + "log/thossuite%u.log";
			FileHandler fileHandler = new FileHandler(logPattern, // Pattern
					10485760, // Limit: 10MB (10 * 1024 * 1024)
					5, // Count: 5 Rotationen
					true // Append: true (erweitert existierende Datei)
			) {
				@Override
				public synchronized void publish(LogRecord record) {
					super.publish(record);
					flush();
				}
			};
			// Im Debug-Mode auch FINE-Level in Datei, sonst nur INFO+
			fileHandler.setLevel(debugMode ? Level.FINE : Level.INFO);
			fileHandler.setFormatter(commonFormatter);

			// === CONSOLE HANDLER ===
			// StreamHandler statt ConsoleHandler weil:
			// 1. Wir wollen System.out statt stderr
			// 2. Wir wollen auto-flush nach jedem Log
			StreamHandler consoleHandler = new StreamHandler(System.out, commonFormatter) {
				@Override
				public synchronized void publish(LogRecord record) {
					super.publish(record);
					flush(); // Wichtig! Sonst sieht man Logs erst bei Buffer-Full
				}
			};
			// ALL = zeigt alles was der Root-Logger durchlässt
			consoleHandler.setLevel(Level.ALL);

			// === ROOT LOGGER KONFIGURATION ===
			Logger rootLogger = Logger.getLogger("");
			// FINE erlaubt Debug-Logs für App-Code (Log.debug(...))
			rootLogger.setLevel(Level.FINE);

			// Alte Handler entfernen (Standard ConsoleHandler etc.)
			for (Handler handler : rootLogger.getHandlers()) {
				rootLogger.removeHandler(handler);
			}

			// Neue Handler hinzufügen
			rootLogger.addHandler(fileHandler);
			rootLogger.addHandler(consoleHandler);

			// === LOGGER-LEVEL FÜR JAVA-INTERNA ===
			// Diese Logger auf WARNING setzen um FINE/INFO Spam zu vermeiden
			// WICHTIG: Diese werden in KEEP_ALIVE_LOGGERS hart referenziert!
			for (Logger logger : KEEP_ALIVE_LOGGERS) {
				logger.setLevel(Level.WARNING);
			}

			Log.debug(ThosSuiteApp.class, "Logging initialisiert");

		} catch (Exception e) {
			// Fail-Fast: Logging-Fehler sind kritisch!
			e.printStackTrace();
			throw new RuntimeException("Fehler beim Initialisieren von Logging:", e);
		}
	}
    
    // =========================================================================
    // PUBLIC API - Logging-Methoden
    // =========================================================================
    
    /**
     * Loggt eine DEBUG-Message (Level.FINE).
     * 
     * <p>Debug-Logs sind für Entwickler gedacht und werden standardmäßig nur in der
     * Console angezeigt, nicht in der Log-Datei (außer --debug Mode).
     * 
     * @param caller Aufrufendes Objekt (this)
     * @param message Debug-Message
     */
    public static void debug(Object caller, String message) {
        debug(caller.getClass(), message);
    }
    
    /**
     * Loggt eine DEBUG-Message (Level.FINE).
     * 
     * @param callerClass Aufrufende Klasse (MyClass.class)
     * @param message Debug-Message
     */
    public static void debug(Class<?> callerClass, String message) {
        Logger.getLogger(callerClass.getName()).log(Level.FINE, message);
    }
    
    /**
     * Loggt eine INFO-Message (Level.INFO).
     * 
     * <p>Info-Logs sind für wichtige Ereignisse gedacht (Start, Stop, wichtige State-Changes).
     * Sie werden in File UND Console geloggt.
     * 
     * @param caller Aufrufendes Objekt (this)
     * @param message Info-Message
     */
    public static void info(Object caller, String message) {
        info(caller.getClass(), message);
    }
    
    /**
     * Loggt eine INFO-Message (Level.INFO).
     * 
     * @param callerClass Aufrufende Klasse (MyClass.class)
     * @param message Info-Message
     */
    public static void info(Class<?> callerClass, String message) {
        Logger.getLogger(callerClass.getName()).log(Level.INFO, message);
    }
    
    /**
     * Loggt eine WARNING-Message (Level.WARNING).
     * 
     * <p>Warnings sind für Probleme die nicht kritisch sind aber beachtet werden sollten
     * (z.B. deprecated API-Nutzung, unerwartete aber handhabbare Zustände).
     * 
     * @param caller Aufrufendes Objekt (this)
     * @param message Warning-Message
     */
    public static void warn(Object caller, String message) {
        warn(caller.getClass(), message);
    }
    
    /**
     * Loggt eine WARNING-Message (Level.WARNING).
     * 
     * @param callerClass Aufrufende Klasse (MyClass.class)
     * @param message Warning-Message
     */
    public static void warn(Class<?> callerClass, String message) {
        Logger.getLogger(callerClass.getName()).log(Level.WARNING, message);
    }
    
    /**
     * Loggt eine ERROR-Message mit Exception (Level.SEVERE).
     * 
     * <p>Error-Logs sind für kritische Fehler gedacht die zum Abbruch führen oder
     * zu unerwartetem Verhalten. Der komplette StackTrace wird geloggt.
     * 
     * <p><b>WICHTIG:</b> Der StackTrace wird manuell in die Message eingebaut, da
     * der Standard-Formatter den StackTrace nicht immer korrekt anzeigt.
     * 
     * @param caller Aufrufendes Objekt (this)
     * @param message Error-Message (Kontext)
     * @param ex Exception mit StackTrace
     */
    public static void error(Object caller, String message, Throwable ex) {
        error(caller.getClass(), message, ex);
    }
    
    /**
     * Loggt eine ERROR-Message ohne Exception (Level.SEVERE).
     * 
     * @param caller Aufrufendes Objekt (this)
     * @param message Error-Message
     */
    public static void error(Object caller, String message) {
        error(caller.getClass(), message, null);
    }
    
    /**
     * Loggt eine ERROR-Message mit optionaler Exception (Level.SEVERE).
     * 
     * <p>Der StackTrace wird manuell als String in die Log-Message eingebaut, damit er
     * garantiert im Log erscheint (Standard-Formatter zeigt ihn nicht immer an).
     * 
     * @param callerClass Aufrufende Klasse (MyClass.class)
     * @param message Error-Message (Kontext)
     * @param ex Exception mit StackTrace (kann null sein)
     */
    public static void error(Class<?> callerClass, String message, Throwable ex) {
        Logger logger = Logger.getLogger(callerClass.getName());
        if (ex != null) {
            // StackTrace manuell als String bauen
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            
            // Message + StackTrace zusammen loggen
            logger.log(Level.SEVERE, message + "\n" + sw.toString());
        } else {
            logger.log(Level.SEVERE, message);
        }
    }
    
    /**
     * Loggt eine ERROR-Message ohne Exception (Level.SEVERE).
     * 
     * @param callerClass Aufrufende Klasse (MyClass.class)
     * @param message Error-Message
     */
    public static void error(Class<?> callerClass, String message) {
        error(callerClass, message, null);
    }
}
package app;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import app.config.Config;
import app.controller.Controller;
import app.data.AppClock;
import app.ui.MainWindow;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;

public class ThosSuiteApp {

	// Ja, diese Attribute werden niemals wieder genutzt werden können. Aber man macht das halt so...
	private MainWindow mainWindow;
	@SuppressWarnings("unused")
	private Controller controller;

	public static void main(String[] args) {
		System.out.println("Start Suite: " + LocalDateTime.now()); //!Später Logging
		AppClock.init();
		
		Thread.setDefaultUncaughtExceptionHandler((_, ex) -> {
			// Stacktrace sammeln. Wir wollen die GUI im EDT laufen lassen, dann landen Exceptions aber nicht mehr
			// in dieser main-Methode, weil die beendet sich nach invokeLater, wie der Name later bereits suggeriert.
			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));
			String trace = sw.toString();
			JTextArea textArea = new JTextArea(trace);
			textArea.setEditable(false);
			textArea.setRows(40);
			textArea.setColumns(160);
			JScrollPane scrollPane = new JScrollPane(textArea);
			JOptionPane.showMessageDialog(null, scrollPane, "GeoSuite", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
			System.exit(1);
		});

		// !Später: Sauberes javadoc erstellen. Du muss mehr kommentieren! Anschließend bei javadoc.io hochladen
		// !Später: -Dsun.java2d.d3d=false → Ist nicht so super. Brauchen wir hier, weil tja, wir wissen es nicht genau. Offenbar nutzt Windows für die Suite
		// sehr wohl die GraKa. Man könnte das noch einmal genauer untersuchen, warum die GraKa damit so hoffnungslos überfordert ist. GraKa-3D-D-Einstellungen?
		// Oder auf neuen Rechner warten
		// !Erweiterung: Ein Log wäre natürlich schon ganz nice...

		// !Später: Schriften sauber einbinden. Das ist hier aktuell nur ein PoC!
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, ThosSuiteApp.class.getClassLoader().getResourceAsStream("ARCADECLASSIC.TTF"));
			System.out.println("ARCADECLASSIC newly registered: " + GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font));
		} catch (Exception e) {
			throw new RuntimeException("Probs beim Laden der Font: ARCADECLASSIC");
		}

		// Claude: Swing sagt "alle UI-Komponenten sollten im EDT erstellt werden"
		SwingUtilities.invokeLater(() -> {
			String dataFolder = args[0];
			if (!new File(dataFolder).isDirectory()) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
				fc.setDialogTitle("Wo finde ich denn die Dateien?");
				int result = fc.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION) {
					dataFolder = fc.getSelectedFile().getAbsolutePath() + "/";
				} else
					System.exit(-1);
			}

			Config.init(dataFolder);
			new ThosSuiteApp();
		});
	}
	
	public ThosSuiteApp() {
		System.setProperty("FlatLaf.debug", "true");
		FlatLightLaf.setup();
		Skin skin = SkinService.get();
		mainWindow = skin.createMainWindow();
		List<Image> icons = null;
		try {
			icons = List.of(ImageIO.read(new File(Config.get("iconFolder") + "suite_icon/mondrian_rounded_16.png")),
					ImageIO.read(new File(Config.get("iconFolder") + "suite_icon/mondrian_rounded_24.png")),
					ImageIO.read(new File(Config.get("iconFolder") + "suite_icon/mondrian_rounded_32.png")),
					ImageIO.read(new File(Config.get("iconFolder") + "suite_icon/mondrian_rounded_64.png")),
					ImageIO.read(new File(Config.get("iconFolder") + "suite_icon/mondrian_rounded_128.png")),
					ImageIO.read(new File(Config.get("iconFolder") + "suite_icon/mondrian_rounded_256.png")));
		} catch (Exception e) {
			throw new RuntimeException("Schon Probleme beim Laden der Icons? Ui...");
		}
		mainWindow.setIconImages(icons);
		controller = new Controller(mainWindow);
		mainWindow.setLocationRelativeTo(null);
		System.out.println("Hauptfenster ist da: " + LocalDateTime.now()); //!Später Logging
	}
}

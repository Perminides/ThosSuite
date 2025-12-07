package app.ui.skin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import app.config.Config;
import app.data.DeckType;
import app.data.GeoMap;
import app.data.MapService;
import app.ui.MainWindow;
import app.ui.UIUtils;
import app.ui.components.BackgroundPanel;
import app.ui.components.CustomButtonLabel;
import app.ui.components.CustomImageLabel;
import app.ui.components.CustomTextField;
import app.ui.components.CustomTextLabel;
import app.ui.components.ImageMapPanel;
import app.ui.components.MultipleChoicePanel;
import app.ui.components.ShapeMapPanel;
import app.ui.skin.params.BorderParams;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * Ja, das ist hier heftige Reflection, es tut mir leid. Aber was ich wollte ist: - Skins, die voneinander erben können - Jedes Skin hat seine eigene
 * Konfigurationsdatei - Wenn in einem "niedrigeren" Skin eine Property definiert ist, die weiter oben im Baum auch nochmal definiert ist, gewinnt die untere -
 * Ein Ändern einer Property-Datei wird beim nächsten Neustart automatisch aktiv - Die Werte können in den Skinklassen mittels einfacher Variablen genutzt
 * werden. Kein get("textColor") nötig.
 */
public abstract class Skin {

	public enum IconButtonType {
		BACK, SKIP, PLAY, CANCEL
	};

	public enum TextLabelType {
		QUESTION("Question"), PROGRESS("Progress"), CARD_HISTORY("History");

		private final String text;

		TextLabelType(final String text) {
			this.text = text;
		}

		public String toString() {
			return text;
		}
	}

	public abstract String getDisplayName();

	protected Color textColor; // Standard-TextFarbe. Arbeite möglichst nur mit einer, wenn es geht.
	protected Color textActiveComponentColor;
	protected Color incorrectTextColor; // Fürs Textfeld natürlich

	protected Color incorrectColor; // Für MC, Deutschlandkarte, Welt
	protected Color correctColor; // Für MC, Deutschlandkarte
	protected Color markedColor; // Für die Karten
	protected Color shapeMapColor0; // Für die Karten
	protected Color shapeMapColor1; // Für die Karten

	protected Color activeComponentBgColor; // Default für aktive MCButton, Karte, BackButton
	protected Color activeComponentHoverColor; // Default für MCButton, Kartem BackButton
	protected Color disabledComponentBgColor; // Default für MCButton, Map, JTextField
	protected Color displayTextBgColor; // Default für Textfields (Fragen), signalisiert: Hier nichts klickbares!

	protected Color displayTextQuestionBgColor;
	protected Color displayTextProgressBgColor;
	protected Color displayTextHistoryBgColor;
	protected Color activeButtonBgColor; // MCLabels !Später Das ergibt wenig Sinn. Ein Button ist auch ein InputElement und sollte somit
											// activeComponentBackgroundColor nutzen!
	protected Color activeButtonHoverColor = adjustBrightness(activeButtonBgColor, 20); // MCLabels, Karte !Später siehe oben
	protected Color inactiveButtonBgColor;
	protected Color disabledButtonBgColor;
	protected Color imageLabelBgColor = new Color(0, 0, 0, 0); // In der Regel soll das ImagePanel transparent sein..,

	protected Color menuBarBackground;
	protected Color menuBarHoverBackground = adjustBrightness(menuBarBackground, 20);
	protected Color menuDisabledForeground = adjustBrightness(textColor, 90);

	protected Font font;
	protected Font smallFont;

	protected BorderParams borderSmallComponent; // MC Buttons, InputField
	protected BorderParams borderMediumComponent; // QuestionLabel
	protected BorderParams borderBigComponent; // Für das Bild
	protected BorderParams borderBackButton; // BackButton
	protected Color borderColor; // TextField, Panels, Karten, ...
	protected Color thinBorderColor; // Um contentPane und unten die MenuBar

	protected ImageIcon backButtonIcon;
	protected ImageIcon skipButtonIcon;
	protected ImageIcon playButtonIcon;
	protected ImageIcon cancelButtonIcon;

	protected String worldMapImageName;
	protected String worldMapInactiveImageName;
	protected String worldMapOverlayImageName;
	protected String worldMapInactiveOverlayImageName;
	protected String defaultWallpaperName;
	protected String emptyWallpaperName;
	protected String mcWallpaperName;
	protected String worldWallpaperName;
	protected String germanyWallpaperName;

	protected Rectangle mcSessionQuestionPanel;
	protected Rectangle mcSessionImagePanel;
	protected Rectangle mcSessionMcPanel;
	protected Rectangle mcSessionProgressPanel;
	protected Rectangle mcSessionHistoryPanel;
	protected Rectangle mcSessionBackButton;
	protected Rectangle worldSessionMapPanel;
	protected Rectangle worldSessionQuestionPanel;
	protected Rectangle worldSessionTextInputPanel;
	protected Rectangle worldSessionImagePanel;
	protected Rectangle worldSessionMcPanel;
	protected Rectangle worldSessionProgressPanel;
	protected Rectangle worldSessionHistoryPanel;
	protected Rectangle worldSessionBackButton;
	protected Rectangle germanySessionMapPanel;
	protected Rectangle germanySessionQuestionPanel;
	protected Rectangle germanySessionTextInputPanel;
	protected Rectangle germanySessionImagePanel;
	protected Rectangle germanySessionMcPanel;
	protected Rectangle germanySessionProgressPanel;
	protected Rectangle germanySessionHistoryPanel;
	protected Rectangle germanySessionBackButton;
	protected Rectangle regionSessionQuestionPanel;
	protected Rectangle regionSessionMapPanel;
	protected Rectangle regionSessionTextInputPanel;
	protected Rectangle esSessionQuestionPanel;
	protected Rectangle esSessionMapPanel;
	protected Rectangle esSessionTextInputPanel;

	protected Integer verticalGapMC;

	/**
	 * Ist die Breite der ContentPane. Menüzeile und Border um die Rootpane sowie der Windows-Border gehören nicht dazu! --- Fenster = Spielfeld [+ Menü] +
	 * 2xBorder --- Von daher: Wenn deine Font größer wird bei einem Skinwechsel, dann vergrößert sich das Fenster
	 * 
	 * @return
	 */
	public Dimension getContentSize() {
		return new Dimension(1910, 1000);
	}

	/**
	 * Falls wir doch mal UIManager.puts benötigen, dann dürfen die erst nach Auswahl des skins gesetzt werden...
	 */
	public void activate() {
		configureUiManager();
	}

	public MainWindow createMainWindow() {
		MainWindow mainWindow = new MainWindow();
		// redecorateMainWindow(mainWindow);
		return mainWindow;
	}

	public void redecorateMainWindow(MainWindow window) {
		// window.setTitleBarColor(UIUtils.toHex(menuBarBackground));
		window.setWidth(getContentSize().width);
		window.setHeight(getContentSize().height);
	}

	public MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		// !Sofort: Ich glaube das macht gar nix...
		menuBar.setStyle("-fx-background-color: " + UIUtils.toHex(menuBarBackground) + ";");
		return menuBar;
	}

	public Menu createMenu(String text) {
		return new Menu(text);
	}

	public MenuItem createMenuItem(String text) {
		return new MenuItem(text);
	}
	
	public HeaderBar createHeaderBar() {
	    return new HeaderBar();
	}
	
	/**
	 * !Sofort Anpassen an createBackgroundPanel mit dem entsprechenden Hintergrundbild!
	 * @return
	 */
	public Pane createContentPane() {
	    return new Pane();
	}
	
	// In SkinService:
	public void styleScene(Scene scene) {
	    // Background Fill
	    scene.setFill(javafx.scene.paint.Color.rgb(
	    		menuBarBackground.getRed(), 
	    		menuBarBackground.getGreen(), 
	    		menuBarBackground.getBlue()
	    ));
	    
	    // CSS generieren
	    String css = "";
	    css = addCssRule(css, ".menu .label", "-fx-text-fill", UIUtils.toHex(textColor)); // Lernen
	    css = addCssRule(css, ".menu .label", "-fx-font-size", "" + font.getSize() + "px");  // Schriftgröße Lernen
	    css = addCssRule(css, ".menu:hover", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hover über Lernen
	    css = addCssRule(css, ".menu:showing", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hintergrund von Lernen, wenn ich über ein Untermenü hovere, wie Multiple Choice oder so.
	    css = addCssRule(css, ".menu", "-fx-font-family", "'" + font.getFamily() + "'"); // Lernen
	    
	    css = addCssRule(css, ".menu-item .label", "-fx-text-fill", UIUtils.toHex(textColor)); // Multiple Choice unter Lernen
	    css = addCssRule(css, ".menu-item .label", "-fx-font-size", "" + font.getSize() + "px");  // Schriftgröße Multiple Choice unter Lernen
	    css = addCssRule(css, ".menu-item", "-fx-padding", "2px 10px"); // Vertikaler Zeilenabstand und Padding links/rechts von Multiple Choice
	    css = addCssRule(css, ".menu-item:hover", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hover über Multiple Choice unter Lernen
	    css = addCssRule(css, ".menu-item:disabled:hover", "-fx-background-color", "transparent"); // Schaltet den Hover für disabled Items aus.
	    css = addCssRule(css, ".context-menu", "-fx-background-color", UIUtils.toHex(menuBarBackground));  // Untermenüs (Multiple Choice unter Lernen)
	    css = addCssRule(css, ".menu-item", "-fx-font-family", "'" + font.getFamily() + "'"); // Multiple Choice
	    css = addCssRule(css, ".menu-item:disabled .label", "-fx-text-fill", UIUtils.toHex(textColor)); // JavaFX macht das eigenständig entsättigt. Also selbst Color.Red setzen würde nur ein schmutziges graurot erzeugen.
	    
	    css = addCssRule(css, ".context-menu", "-fx-border-color", UIUtils.toHex(thinBorderColor));
	    css = addCssRule(css, ".context-menu", "-fx-border-width", "1px");
	    css = addCssRule(css, ".thorstens-bar", "-fx-background-color", UIUtils.toHex(menuBarBackground));
	    css = addCssRule(css, ".thorstens-bar", "-fx-border-color", UIUtils.toHex(thinBorderColor));
	    css = addCssRule(css, ".thorstens-bar", "-fx-border-width", "0 0 1 0");
	    
	    css = addCssRule(css, ".thorstens-pane", "-fx-background-color", UIUtils.toHex(menuBarBackground));
	    css = addCssRule(css, ".thorstens-pane", "-fx-pref-width", getContentSize().width + "px");
	    css = addCssRule(css, ".thorstens-pane", "-fx-pref-height", getContentSize().height + "px");
	    
	    css = addCssRule(css, ".thorstens-title", "-fx-font-family", "'Aptos'");
	    css = addCssRule(css, ".thorstens-title", "-fx-font-size", font.getSize() + "px");  // Vom Skin-Font!
	    css = addCssRule(css, ".thorstens-title", "-fx-text-fill", UIUtils.toHex(textColor));
	    
	    css = addCssRule(css, ".thorstens-root", "-fx-border-color", "white");
	    css = addCssRule(css, ".thorstens-root", "-fx-border-width", "1px");
	    
	    //css = addCssRule(css, ".menu-bar", "-fx-background-color", UIUtils.toHex(Color.RED)); Kein Effekt
	    //css = addCssRule(css, ".menu", "-fx-text-fill", UIUtils.toHex(Color.RED)); Kein Effekt
	    //css = addCssRule(css, ".menu", "-fx-font-size", "20px");  // Kein Effekt
	    //css = addCssRule(css, ".menu-item", "-fx-text-fill", UIUtils.toHex(Color.red)); Kein Effekt
	    //css = addCssRule(css, ".menu-item", "-fx-font-size", "10px");  // Hat Einfluss auf den vertikalen Abstand zwischen Deutschland und Multiple Choice. Aber nicht auf die Fontgröße!
	    //css = addCssRule(css, ".menu-item:disabled", "-fx-text-fill", UIUtils.toHex(Color.RED)); Kein Effekt
	    
	 // Als Data-URL zur Scene hinzufügen
	    scene.getStylesheets().add("data:text/css," + css);
	}
		
	private ImageView createHeaderIconView(Stage stage, double minSize) {
	    ObservableList<Image> icons = stage.getIcons();
	    if (icons.isEmpty()) return null;
	    
	    // Finde Icon >= 20px (gut für Downscaling)
	    Image bestIcon = icons.stream()
	        .filter(img -> img.getWidth() >= minSize)
	        .min((a, b) -> Double.compare(a.getWidth(), b.getWidth()))
	        .orElse(icons.get(icons.size() - 1));  // Fallback: größtes
	    
	    ImageView iconView = new ImageView(bestIcon);
	    iconView.setPreserveRatio(true);
	    iconView.setSmooth(true);
	    return iconView;
	}
	
	public void setupHeaderBar(HeaderBar headerBar, Stage stage, MenuBar menuBar) {
	    // CENTER: Title
	    Label titleLabel = new Label("Thos Suite");
	    titleLabel.getStyleClass().add("thorstens-title");
	    headerBar.setCenter(titleLabel);
	    
	    // LEADING: Icon + MenuBar (nach Layout, wenn Höhe bekannt)
	    Platform.runLater(() -> {
	        double height = headerBar.getHeight();
	        double targetIconSize = height * 0.55;
	        
	        // Icon mit richtiger Größe wählen
	        ImageView icon = createHeaderIconView(stage, targetIconSize);
	        if (icon != null) {
	            icon.setFitHeight(targetIconSize);
	            icon.setFitWidth(targetIconSize);
	            
	            double spacing = font.getSize() * 0.5;
	            HBox leftBox = new HBox(0, icon, menuBar);
	            leftBox.setAlignment(Pos.CENTER_LEFT);
	            leftBox.setPadding(new javafx.geometry.Insets(0, 0, 0, spacing));
	            
	            headerBar.setLeading(leftBox);
	        }
	    });
	}

	public BackgroundPanel createBackgroundPanel(DeckType type) {
		return new BackgroundPanel(getBackgroundImagePath(type), getContentSize());
	}

	public CustomTextField createInputField(DeckType type) {
		Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionTextInputPanel");
		if (bounds == null)
			bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "SessionTextInputPanel");
		Color textC = textActiveComponentColor == null ? textColor : textActiveComponentColor;
		Color incorrectText = incorrectTextColor == null ? incorrectColor : incorrectTextColor;
		CustomTextField result = new CustomTextField(font, textC, incorrectText, activeComponentBgColor, disabledComponentBgColor, SwingConstants.CENTER,
				borderSmallComponent);
		result.setBounds(bounds.x, bounds.y, bounds.width, result.getPreferredSize().height);
		return result;
	}

	public CustomImageLabel createImageLabel(DeckType type) {
		Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionImagePanel");
		CustomImageLabel result = new CustomImageLabel(borderBigComponent, imageLabelBgColor);
		result.setBounds(bounds);
		return result;
	}

	public CustomButtonLabel createAnswerButton() {
		Color activeBg = activeButtonBgColor == null ? activeComponentBgColor : activeButtonBgColor;
		Color hoverBg = activeButtonHoverColor == null ? activeComponentHoverColor : activeButtonHoverColor;
		Color inactiveBg = inactiveButtonBgColor == null ? displayTextBgColor : inactiveButtonBgColor;
		Color disabledBg = disabledButtonBgColor == null ? disabledComponentBgColor : disabledButtonBgColor;
		Color textC = textActiveComponentColor == null ? textColor : textActiveComponentColor;

		return new CustomButtonLabel(font, smallFont, textC, activeBg, hoverBg, inactiveBg, correctColor, incorrectColor, disabledBg, borderSmallComponent,
				null);
	}

	public CustomButtonLabel createIconButton(DeckType type, IconButtonType buttonType) {
		switch (buttonType) {
		case BACK: {
			Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionBackButton");
			CustomButtonLabel result = new CustomButtonLabel(null, null, textColor, activeComponentBgColor, activeComponentHoverColor, null, null, null,
					disabledComponentBgColor, borderBackButton, backButtonIcon);
			result.setLocation(bounds.x, bounds.y);
			return result;
		}
		case SKIP:
			return new CustomButtonLabel(null, null, textColor, activeComponentBgColor, activeComponentHoverColor, null, null, null, disabledComponentBgColor,
					borderBackButton, skipButtonIcon);
		case PLAY:
			return new CustomButtonLabel(null, null, textColor, activeComponentBgColor, activeComponentHoverColor, null, null, null, disabledComponentBgColor,
					borderBackButton, playButtonIcon);
		case CANCEL:
			return new CustomButtonLabel(null, null, textColor, activeComponentBgColor, activeComponentHoverColor, null, null, null, disabledComponentBgColor,
					borderBackButton, cancelButtonIcon);
		default:
			throw new RuntimeException("Was ist denn das für ein ButtonType: " + type);
		}
	}

	public CustomTextLabel createTextLabel(DeckType deckType, TextLabelType labelType) {
		Rectangle bounds = (Rectangle) getFieldValue(deckType.getId() + "Session" + labelType + "Panel");
		if (bounds == null)
			bounds = (Rectangle) getFieldValue(deckType.getCategory().toString() + "Session" + labelType + "Panel");
		Color bg = (Color) getFieldValue("displayText" + labelType + "BgColor");
		bg = bg == null ? displayTextBgColor : bg;
		CustomTextLabel result = new CustomTextLabel(bg, borderMediumComponent, font, textColor);
		result.setBounds(bounds);
		return result;
	}

	public MultipleChoicePanel createMultipleChoicePanel(DeckType type) {
		Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMcPanel");
		MultipleChoicePanel result = new MultipleChoicePanel(bounds.width, font, smallFont, verticalGapMC);
		result.setLocation(bounds.x, bounds.y);
		return result;
	}

	/**
	 * Erstellt ein Deutschlandpanel. Aktuell noch ohne Hintergrundbild. Die Größe berechnet sich aus: Höhe = height + 2 * yPadding. Der scale findet ohne
	 * Verzerrung statt, von daher ist die width dann auch aus diesen Parametern und der Auflösung der Map bestimmt.
	 */
	public ShapeMapPanel createShapeMapPanel(DeckType type) {
		GeoMap map = MapService.getInstance().getMap(type);
		Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMapPanel");
		if (bounds == null)
			bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "SessionMapPanel");
		// !Später: Magic Numbers
		ShapeMapPanel result = new ShapeMapPanel(map, bounds.height, activeComponentBgColor, borderColor, activeComponentHoverColor, correctColor,
				incorrectColor, disabledComponentBgColor, markedColor, 1.8f, 3.5f, 10, false, shapeMapColor0, shapeMapColor1);
		result.setLocation(bounds.x, bounds.y);
		return result;
	}

	public ImageMapPanel createImageMapPanel(DeckType type) {
		GeoMap map = MapService.getInstance().getMap(type);
		Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMapPanel");
		ImageMapPanel result = new ImageMapPanel(map, bounds.width, bounds.height, borderBigComponent, correctColor, incorrectColor, markedColor, 2,
				borderColor, borderBackButton, new Rectangle(11, 11, 410, 254));
		result.setLocation(bounds.x, bounds.y);
		return result;
	}

	/**
	 * Die Kartenbilder werden vom MapRepositories geholt
	 * 
	 * @param id
	 * @return
	 */
	public String getMapImagePath(DeckType type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapImageName;
		default:
			return null;
		}
	}

	public String getMapInactiveImagePath(DeckType type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapInactiveImageName;
		default:
			return null;
		}
	}

	public String getMapInactiveOverlayImagePath(DeckType type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapInactiveOverlayImageName;
		default:
			return null;
		}
	}

	public String getMapOverlayImagePath(DeckType type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapOverlayImageName;
		default:
			return null;
		}
	}

	// --- Hintergrund / Farben ---
	protected String getBackgroundImagePath(DeckType type) {
		if (type == null)
			return Config.get("wallpaperFolder") + (emptyWallpaperName == null ? defaultWallpaperName : emptyWallpaperName);
		String bgName = (String) getFieldValue(type.getId() + "WallpaperName");
		if (bgName == null)
			return Config.get("wallpaperFolder") + defaultWallpaperName;
		else
			return Config.get("wallpaperFolder") + bgName;

	}

	/**
	 * 
	 * @param c
	 * @param intensity:
	 *            an int value between 0 (no change) and 100 (maximal change will lead to white or black...)
	 * @return
	 */
	public static Color adjustBrightness(Color c, int intensity) {
		if (c == null)
			return null; // Das kann in der Tat passieren z.B. im Konstruktor von Skin hier :)
		if (intensity < 0 || intensity > 100)
			throw new RuntimeException("Das soll ein Prozentwert sein für die adjustBrightness, du Witzbold :)");
		float intensityF = (float) intensity / 100;
		float threshold = 1 - intensityF;
		float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
		float brightness = hsb[2];
		if (brightness > threshold) {
			brightness = Math.max(0, brightness - intensityF); // abdunkeln
		} else {
			brightness = Math.min(1, brightness + intensityF); // aufhellen
		}

		Color adjusted = Color.getHSBColor(hsb[0], hsb[1], brightness);
		return new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), c.getAlpha());
	}

	protected void configureUiManager() {

		// Titelzeile, alles auch rechts von den Menüs (Farben gehen nur über putClientProperty)
		UIManager.put("TitlePane.font", font); // Titel des Hauptfensters
		UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, thinBorderColor)); // Farbe der Linie unter der Titel / Menüzeile des
																										// JFrames.

		// Menüpunkte der ersten Ebene (Datei)
		UIManager.put("MenuBar.background", menuBarBackground); // Hintergrund
		UIManager.put("MenuBar.font", font); // Schrift
		UIManager.put("MenuBar.foreground", textColor); // Schriftfarbe
		UIManager.put("MenuBar.hoverBackground", menuBarHoverBackground); // Hintergrund beim Hovern vor(!) dem Click
		// UIManager.put("MenuBar.hoverForeground", Color.RED); -> Gibt es nicht. Aus welchem UIKey wird das bloß gezogen?
		UIManager.put("Menu.selectionBackground", menuBarHoverBackground); // Hintergrund wenn aufgeklickt

		// Menüpunkte 2. Ebene (Deutschland Hints (19 / 33)
		UIManager.put("PopupMenu.border", BorderFactory.createMatteBorder(1, 1, 1, 1, thinBorderColor)); // Um jeden einzelnen Menüpunkt in der zweiten Ebene
		UIManager.put("PopupMenu.background", menuBarBackground); // Hintergrund
		UIManager.put("MenuItem.font", font); // Schrift
		UIManager.put("MenuItem.foreground", textColor); // Schriftfarbe
		UIManager.put("MenuItem.disabledForeground", menuDisabledForeground);
		UIManager.put("MenuItem.selectionBackground", menuBarHoverBackground); // Hintergrund beim Hovern

		// Submenüs 2. Ebene (Anzeigereihenfolge)
		UIManager.put("Menu.font", font); // Schrift

		// !Architektur: Wir wollten setzen auf 1) Swings set-Methoden, 2) FlatLafStyles und 3) UIManager.puts wo es nicht anders geht. Das refactoring fehlt
		// noch!
		// Dieser ganze Block wird für die JOptionPane und Ihre Buttons benötigt.
		UIManager.put("OptionPane.background", menuBarBackground); // Hintergrundfarbe der Hauptarea der JOptionPane
		UIManager.put("OptionPane.messageFont", font); // Font in der Hauptarea der JOptionPane
		UIManager.put("TitlePane.unifiedBackground", false); // Sonst funktioniert TitlePane.background nicht.
		UIManager.put("TitlePane.background", menuBarBackground); // Titelzeile im JDialog...
		UIManager.put("TitlePane.borderColor", Color.WHITE); // Farbe der Linie unter der Titelzeile eines JDialogs.
		UIManager.put("Button.default.background", activeComponentBgColor); // Hintergrundfarbe des Default-Buttons (OK) wenn er nicht fokussiert ist, also nach
																			// Click auf einen anderen Button und dann Wegziehen der Maus...
		UIManager.put("Button.default.focusedBackground", activeComponentBgColor); // Hintergrund des fokussierten Buttons (OK)
		UIManager.put("Button.default.pressedBackground", activeComponentHoverColor); // Ok Button während des Drückens
		UIManager.put("Button.background", activeComponentBgColor); // Hintergrundfarbe der Nicht-Default Buttons (Abbrechen) wenn nicht im Fokus oder Pressed
		UIManager.put("Button.focusedBackground", activeComponentBgColor); // Abbrechen Buttons wenn im Fokus
		UIManager.put("Button.pressedBackground", activeComponentHoverColor); // Abbrechen Button während des Drückens

		UIManager.put("Button.hoverBackground", activeComponentHoverColor); // Hoverfarbe für nicht aktive Buttons (Abbrechen)
		UIManager.put("Button.default.hoverBackground", activeComponentHoverColor); // Hover Hintergrund des Buttons mit Fokus (OK)

		Color textC = textActiveComponentColor == null ? textColor : textActiveComponentColor;
		UIManager.put("Button.foreground", textC); // Schriftfarbe der Nicht-Default-Buttons (Abbrechen)
		UIManager.put("Button.default.foreground", textC); // Schriftfarbe des Default-Buttons (OK)

		UIManager.put("Button.default.borderColor", textC); // Default Button Border Color wenn nicht fokussiert
		UIManager.put("Button.default.hoverBorderColor", textC); // Default Button Borderfarbe beim Hovern
		UIManager.put("Button.default.pressedBorderColor", textC); // Default Button Borderfarbe beim Drücken
		UIManager.put("Button.default.focusedBorderColor", textC); // Default Button Borderfarbe wenn fokussiert
		UIManager.put("Button.borderColor", textC); // Nicht-Default Button nicht fokussiert
		UIManager.put("Button.hoverBorderColor", textC); // Nicht-Default Button Borderfarbe beim Hovern
		UIManager.put("Button.pressedBorderColor", textC); // Nicht-Default Button Borderfarbe beim Drücken
		UIManager.put("Button.focusedBorderColor", textC); // Nicht-Default Button Borderfarbe wenn fokussiert

		UIManager.put("Button.font", font); // Schriftart aller Buttons

		UIManager.put("Button.arc", borderSmallComponent.arc()); // Rundung der Ecken für alle Buttons

		UIManager.put("Button.default.borderWidth", borderSmallComponent.width()); // DefaultButton (OK) in allen Status
		UIManager.put("Button.borderWidth", borderSmallComponent.width()); // Nicht Default Buttons in allen Status**/

		/**
		 * MenuBar direkt setzen für die Exceptions JMenuBar menuBar = mainWindow.getJMenuBar(); menuBar.setForeground(newSkin.getTextColor());
		 * menuBar.setBackground(newSkin.getMenuBarColor());
		 **/
	}

	protected void loadAllConfigs(String configPath) {
		try (FileInputStream in = new FileInputStream(configPath)) {
			Properties props = new Properties();
			props.load(in);

			// ganze Klassenhierarchie durchlaufen
			for (Class<?> cls = this.getClass(); cls != null; cls = cls.getSuperclass()) {
				for (Field field : cls.getDeclaredFields()) {
					field.setAccessible(true);

					String value = props.getProperty(field.getName());
					if (value == null)
						continue;

					if (field.getType() == Color.class) {
						field.set(this, parseColor(value));
					} else if (field.getType() == Font.class) {
						field.set(this, parseFont(value));
					} else if (field.getType() == BorderParams.class) {
						field.set(this, parseBorderParams(value));
					} else if (field.getType() == ImageIcon.class) {
						field.set(this, new ImageIcon(Config.get("iconFolder") + value));
					} else if (field.getType() == Integer.class || field.getType() == int.class) {
						field.set(this, Integer.parseInt(value));
					} else if (field.getType() == Rectangle.class) {
						field.set(this, parseRectangle(value));
					} else if (field.getType() == String.class) {
						field.set(this, value);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Probleme beim Lesen der Skins", e);
		}
	}

	protected Color parseColor(String value) {
		String[] values = value.split(",");
		if (values.length == 1 && value.length() == 7)
			return Color.decode(value);
		else if (values.length == 1 && value.length() == 9) {
			Color result = Color.decode(value.substring(0, 7));
			int alpha = Integer.parseInt(value.substring(7), 16);
			return new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
		} else if (values.length == 4)
			return new Color(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3]));
		else
			throw new RuntimeException("Das Color-Format kenne ich nicht: " + value);
	}

	protected Font parseFont(String value) {
		String[] values = value.split(",");
		if (values.length == 3)
			return new Font(values[0], Integer.parseInt(values[1]), Integer.parseInt(values[2]));
		else
			throw new RuntimeException("Das Font-Format kenne ich nicht: " + value);
	}

	protected BorderParams parseBorderParams(String value) {
		String[] values = value.split(",");
		if (values.length == 7)
			return BorderParams.of(Integer.parseInt(values[0]), parseColor(values[1]),
					new Insets(Integer.parseInt(values[2]), Integer.parseInt(values[3]), Integer.parseInt(values[4]), Integer.parseInt(values[5])),
					Integer.parseInt(values[6]));
		else if (values.length == 3)
			return BorderParams.of(Integer.parseInt(values[0]), parseColor(values[1]), Integer.parseInt(values[2]));
		else
			throw new RuntimeException("Das Borderparams-Format kenne ich nicht: " + value);
	}

	protected Rectangle parseRectangle(String value) {
		String[] values = value.split(",");
		if (values.length == 4)
			return new Rectangle(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3]));
		else
			throw new RuntimeException("Das Bound-Format kenne ich nicht: " + value);
	}

	/**
	 * 
	 * @param feldName
	 * @return null if field not found!
	 */
	protected Object getFieldValue(String feldName) {
		Class<?> clazz = this.getClass();
		while (clazz != null) {
			try {
				Field f = clazz.getDeclaredField(feldName);
				f.setAccessible(true);
				return f.get(this);
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass(); // weiter nach oben
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Kein Zugriff auf Feld: " + feldName, e);
			}
		}
		return null;
	}
	
	private String addCssRule(String existingCss, String selector, String property, String value) {
	    return existingCss + String.format("%s { %s: %s; }", selector, property, value);
	}

	/**
	 * Funktioniert zumindest für Buttons ohne html. Für andere Komponenten noch ungetestet.
	 * 
	 * @param component
	 * @param bgColor
	 * @param fgColor
	 */
	/**
	 * public default void addDisabledColors(JComponent component, Color bgColor, Color fgColor) { Map<String, String> updates = Map.of( "disabledBackground",
	 * toHex(bgColor), "disabledText", toHex(fgColor) ); updateFlatLafStyle(component, updates); }
	 **/
}

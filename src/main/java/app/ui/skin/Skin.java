package app.ui.skin;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import app.config.Config;
import app.data.Deck;
import app.data.GeoMap;
import app.data.MapService;
import app.ui.UIUtils;
import app.ui.components.DashboardTile;
import app.ui.components.ImageMapPane;
import app.ui.components.ImagePane;
import app.ui.components.MultipleChoicePane;
import app.ui.components.SessionInfoLabel;
import app.ui.components.ShapeMapPane;
import app.ui.skin.params.BorderParams;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Ja, das ist hier heftige Reflection, es tut mir leid. Aber was ich wollte ist: - Skins, die voneinander erben können - Jedes Skin hat seine eigene
 * Konfigurationsdatei - Wenn in einem "niedrigeren" Skin eine Property definiert ist, die weiter oben im Baum auch nochmal definiert ist, gewinnt die untere -
 * Ein Ändern einer Property-Datei wird beim nächsten Neustart automatisch aktiv - Die Werte können in den Skinklassen mittels einfacher Variablen genutzt
 * werden. Kein get("textColor") nötig.
 * 
 */

/**
 * Abstrakte Basis-Klasse für alle Skins der ThosSuite.
 * 
 * <h2>CSS-Architektur</h2>
 * 
 * <h3>1. Clustering nach Komponenten</h3>
 * <ul>
 *   <li>Jede Komponente hat ihre eigene {@code addXYZStyles()}-Methode</li>
 *   <li>Alles was eine Komponente betrifft, steht in EINER add___Styles-Methode</li>
 *   <li>Keine abstrakten "Ebenen" oder "Gruppen"</li>
 * </ul>
 * 
 * <h3>2. Wer setzt CSS-Klassen?</h3>
 * <ul>
 *   <li><b>Eigene Komponenten</b> (CustomTextLabel, MultipleChoicePane):
 *       Setzen ihre Klasse im Konstruktor</li>
 *   <li><b>Native JavaFX</b> (Button, TextField):
 *       Werden über Standard-Selektoren gestylt (.button, .text-field)</li>
 *   <li><b>Container ohne Logik</b> (Dialog-VBox):
 *       Factory-Methode im Skin setzt die Klasse</li>
 * </ul>
 * 
 * <h3>3. Naming-Konvention</h3>
 * <ul>
 *   <li>Native JavaFX: Ohne "my-" (.button, .text-field, .dialog-pane)</li>
 *   <li>Eigene Komponenten: Mit "my-" (.my-mc-button, .my-shape-map-pane)</li>
 *   <li>Ausnahme: JavaFX-Komponenten die nie intern erstellt werden (.header-bar)</li>
 * </ul>
 * 
 * <h3>4. Globale Styles</h3>
 * <ul>
 *   <li>Font (Family, Size, Color) wird auf .root gesetzt</li>
 *   <li>Alle anderen Styles sind komponentenspezifisch</li>
 * </ul>
 * 
 * <h3>5. Code-Duplikation bei Kontext-spezifischem Styling</h3>
 * <p>Wenn eine Komponente in verschiedenen Kontexten anders aussieht,
 * wird sie in BEIDEN Methoden gestylt (mit Kommentar-Warnung).</p>
 * <p>Beispiel: {@code .header-bar} wird in {@code addMainWindowStyles()} 
 * UND {@code addDialogStyles()} gestylt.</p>
 * <p>Kommentar: "⚠️ ACHTUNG: Identische Styles auch in addDialogStyles()"</p>
 * 
 * <h3>6. Spezielle Fälle</h3>
 * <ul>
 *   <li><b>CustomTextLabel:</b> Wird über IDs gestylt (#QuestionLabel, #ProgressLabel, #HistoryLabel),
 *       keine generische Basis-Klasse</li>
 *   <li><b>ListView:</b> Aktuell nur in ComboBox, daher in addComboBoxStyles()
 *       mit Kommentar falls später woanders gebraucht</li>
 * </ul>
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
	
	// ========== Instanzvariablen ==========
	// region

	// !Sofort: Wenn Du die borderColor auch in den borderParams angibst, kannst Du sie mit borderColor nicht mehr global setzen! Das muss da raus oder borderColor überschreibt das. Eins von beiden
	// !Sofort: Mal aktuelleres Design ausprobieren: Button mit runden Ecken, ohne Border und mit box-shadow. Sicher sehr interessant, aber ich fürchte das wird ein Refactoring-Alptraum, weil Du immer den Platz für den Schatten brauchst überall...
	// !Sofort: Einfacher als box-shadow wäre ein Design mit transparenten Hintergründen der Buttons und TextFields und so und ohne Border. Hehe...
	
	public abstract String getDisplayName();

	protected Color textColor; // Standard-TextFarbe. Arbeite möglichst nur mit einer, wenn es geht.
	protected Color textActiveComponentColor;
	protected Color incorrectTextColor; // Fürs Textfeld natürlich
	protected Color mcIncorrectTextColor;
	protected Color mcCorrectTextColor;

	protected Color incorrectColor; // Für MC, Deutschlandkarte, Welt
	protected Color correctColor; // Für MC, Deutschlandkarte
	protected Color markedColor; // Für die Karten
	protected Color shapeMapColor0; // Für die Karten
	protected Color shapeMapColor1; // Für die Karten

	protected Color activeComponentBgColor; // Default für aktive MCButton, Karte, BackButton
	protected Color activeComponentHoverColor; // Default für MCButton, Kartem BackButton
	protected Color disabledComponentBgColor; // Default für MCButton, Map, JTextField
	protected Color displayTextBgColor; // Default für Textfields (Fragen), signalisiert: Hier nichts klickbares!

	protected Color displayTextQuestionBgColor; // displayTextBgColor
	protected Color displayTextProgressBgColor; // displayTextBgColor
	protected Color displayTextHistoryBgColor; // displayTextBgColor
	protected Color disabledButtonBgColor;
	protected Color imageLabelBgColor = new Color(0, 0, 0, 0); // In der Regel soll das ImagePanel transparent sein..,

	protected Color menuBarBackground;
	protected Color playFieldBackground; // default = menuBarBackground;
	protected Color menuBarHoverBackground; // default = adjustBrightness(menuBarBackground, 20);
	protected Color menuDisabledForeground; // default = adjustBrightness(textColor, 90);
	protected String menuButtonPadding; // default = font.getSize() * 0.3 + "px " + font.getSize() * 0.4 + "px";
	protected String menuItemPadding; // default = font.getSize() * 0.1 + "px " + font.getSize() * 0.5 + "px";
	
	protected Integer imageMapShapeBorderWidth; // default = 2;
	protected Integer imageMapLineShapeInnerWidth; // default = 12. Nur für den unsichtbaren Click-Bereich!
	protected Integer imageMapShapeMarkedOuterWidth; // default = 7
	protected Integer imageMapShapeMarkedInnerWidth; // default = 4
	
	protected Double shapeMapStandardBorderWidth; // default = 1.8;
	protected Double shapeMapFederalStateBorderWidth; // default 2.8 für Niedersachsen z. B.

	protected Font font;
	protected Font smallFont;

	protected BorderParams borderSmallComponent; // MC Buttons, InputField
	protected BorderParams borderMediumComponent; // QuestionLabel
	protected BorderParams borderBigComponent; // Für das Bild
	protected Color borderColor; // Komponenten
	protected Color borderShapeColor; // Karten
	protected Color thinBorderColor; // Um contentPane und unten die MenuBar
	protected Integer thinBorderWidth; // default = 1
	
	protected Integer dashBoardTileWidth; // 250
	protected Integer dashBoardTileTopHeight; // 250
	protected Integer dashBoardTileBottomHeight; // 100
	protected Integer dashBoardTileTopFontSize; // font * 4
	protected Integer dashBoardTileBottomFontSize; // font * 2

	protected String backButtonIcon;
	protected String skipButtonIcon;
	protected String playButtonIcon;
	protected String cancelButtonIcon;

	protected String worldMapImageName;
	protected String worldMapInactiveImageName;
	protected String worldMapOverlayImageName;
	protected String worldMapInactiveOverlayImageName;
	protected String defaultWallpaperName;
	protected String emptyWallpaperName;
	protected String mcWallpaperName;
	protected String worldWallpaperName;
	protected String germanyWallpaperName;
	protected String regionWallpaperName;
	protected String lk_bbWallpaperName;
	protected String itWallpaperName;
	protected String esWallpaperName;
	protected String csWallpaperName;

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
	protected Rectangle itSessionQuestionPanel;
	protected Rectangle itSessionMapPanel;
	protected Rectangle itSessionTextInputPanel;
	protected Rectangle usSessionQuestionPanel;
	protected Rectangle usSessionMapPanel;
	protected Rectangle usSessionTextInputPanel;
	protected Rectangle csSessionQuestionPanel;
	protected Rectangle csSessionMapPanel;
	protected Rectangle csSessionTextInputPanel;
	protected Rectangle beSessionQuestionPanel;
	protected Rectangle beSessionMapPanel;
	protected Rectangle beSessionTextInputPanel;

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
	
	// endregion
	
	// ========== CSS ==========
	// region
	
	// !Sofort: Müssen wir nicht weiter oben stylen gleich die ganze Stage? Ne, wahrscheinlich reicht Scene
	public void styleScene(Scene scene) {
		menuBarHoverBackground = menuBarHoverBackground == null ? adjustBrightness(menuBarBackground, 20) : menuBarHoverBackground;
		menuDisabledForeground = menuDisabledForeground == null ? adjustBrightness(textColor, 90) : menuDisabledForeground;
		menuButtonPadding = menuButtonPadding == null ? font.getSize() * 0.3 + "px " + font.getSize() * 0.4 + "px" : menuButtonPadding;
		menuItemPadding = menuItemPadding == null ? font.getSize() * 0.1 + "px " + font.getSize() * 0.5 + "px" : menuItemPadding;
		thinBorderWidth = thinBorderWidth == null ? 1 : thinBorderWidth;
		imageMapShapeBorderWidth = imageMapShapeBorderWidth == null ? 2 : imageMapShapeBorderWidth;
		imageMapLineShapeInnerWidth = imageMapLineShapeInnerWidth == null ? 12 : imageMapLineShapeInnerWidth; // Nur für den unsichtbaren Klickbereich
		imageMapShapeMarkedOuterWidth = imageMapShapeMarkedOuterWidth == null ? 7 : imageMapShapeMarkedOuterWidth;
		imageMapShapeMarkedInnerWidth = imageMapShapeMarkedInnerWidth == null ? 4 : imageMapShapeMarkedInnerWidth;
		shapeMapStandardBorderWidth = shapeMapStandardBorderWidth == null ? 1.8 : shapeMapStandardBorderWidth;
		shapeMapFederalStateBorderWidth = shapeMapFederalStateBorderWidth == null ? 2.8 : shapeMapFederalStateBorderWidth;
		playFieldBackground = playFieldBackground == null ? menuBarBackground : playFieldBackground;
		borderShapeColor = borderShapeColor == null ? borderColor : borderShapeColor;
		textActiveComponentColor = textActiveComponentColor == null ? textColor : textActiveComponentColor;
		dashBoardTileWidth = dashBoardTileWidth == null ? 300 : dashBoardTileWidth;
		dashBoardTileTopHeight = dashBoardTileTopHeight == null ? 300 : dashBoardTileTopHeight;
		dashBoardTileBottomHeight = dashBoardTileBottomHeight == null ? 100 : dashBoardTileBottomHeight;
		dashBoardTileTopFontSize = dashBoardTileTopFontSize == null ? (int)font.getSize()*4 : dashBoardTileTopFontSize;
		dashBoardTileBottomFontSize = dashBoardTileBottomFontSize == null ? (int)font.getSize() : dashBoardTileBottomFontSize;
		displayTextHistoryBgColor = displayTextHistoryBgColor == null ? displayTextBgColor : displayTextHistoryBgColor;
		displayTextProgressBgColor = displayTextProgressBgColor == null ? displayTextBgColor : displayTextProgressBgColor;
		displayTextQuestionBgColor = displayTextQuestionBgColor == null ? displayTextBgColor : displayTextQuestionBgColor;
		
		
	    scene.setFill(menuBarBackground);
	    
	    CssBuilder css = new CssBuilder();
	    
	    // === GLOBAL: FONT ===
	    css.start(".root")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", font.getSize() + "px")
	       .end();
	    css.rule(".text", "-fx-fill", UIUtils.toHex(textColor));
	    
	    // CSS generieren
		// Java-FX Klassen, deren Logik wir übernehmen und die durchaus auch mal in anderen Komponenten implizit benutzt werden könnten.
	    addButtonStyles(css);
	    addCheckBoxStyles(css);
	    addComboBoxStyles(css);
	    addDialogStyles(css);
	    addTextFieldStyles(css);
	    addTextAreaStyles(css);
	    addMenuStyles(css);
	    
	    // Komponenten mit meiner eigenen Logik:-)
	    addSessionInfoLabelStyles(css);
	    addIconButtonStyles(css);
	    addImageMapStyles(css);
	    addImagePaneStyles(css);
	    addMainWindowStyles(css);
	    addMultipleChoiceStyles(css);
	    addShapeMapStyles(css);
	    addMyTableStyles(css);
	    addDashboardStyles(css);
	    addChartStyles(css);
	    
	    String rawCss = css.build(); // Hier kommt sauberes CSS raus: ".rule { color: #fff; }"

	    // 2. Für URL maskieren (Transport)
	    // Das ist der Schritt, den du meinst:
	    String encodedCss = rawCss.replace("%", "%25").replace("#", "%23");

	    // 3. Setzen
	    scene.getStylesheets().clear();
	    scene.getStylesheets().add("data:text/css," + encodedCss);
	    
	    //Log.debug(this, rawCss);
	}
	
	private void addButtonStyles(CssBuilder css) {
		
		System.out.println("textActiveComponentColor: " + textActiveComponentColor);
		
	    // Standard Button (überall, auch TableView intern)
	    css.start(".button")
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", "0")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.color()))
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .end();
	    
	    css.rule(".button .text", "-fx-fill", UIUtils.toHex(textActiveComponentColor));
	    css.rule(".button:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    css.rule(".button:pressed", "-fx-background-color", UIUtils.toHex(adjustBrightness(activeComponentHoverColor, 8)));
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-mc-button:active:pressed", "-fx-translate-y", "1px");
	    //css.rule(".my-mc-button:active:pressed", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 0)");
	}
	
	private void addCheckBoxStyles(CssBuilder builder) {  
		builder.start(".box")
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", "0")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.color()))
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       //.add("-fx-text-fill", UIUtils.toHex(textActiveComponentColor))
	       .end();
	    builder.rule(".box .text", "-fx-fill", UIUtils.toHex(textActiveComponentColor));
	    builder.rule(".box:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    builder.rule(".box:pressed", "-fx-background-color", UIUtils.toHex(adjustBrightness(activeComponentHoverColor, 8)));
	    builder.rule(".check-box:selected .mark", "-fx-background-color", UIUtils.toHex(textActiveComponentColor)); // Die Farbe des Hakens in der Checkbox :-)
	}
	
	private void addComboBoxStyles(CssBuilder css) {
	    css.start(".combo-box-base")
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", "0")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.color()))
	       .end();
	    
	    css.rule(".combo-box-base .text", "-fx-fill", UIUtils.toHex(textActiveComponentColor));
	    
	    // ListView in ComboBox
	    // ⚠️ ACHTUNG: Wenn ListView woanders gebraucht wird, dort analog stylen
	    css.rule(".combo-box-popup .list-view", "-fx-background-color", UIUtils.toHex(activeComponentBgColor));
	    css.rule(".combo-box-popup .list-view .list-cell", "-fx-background-color", "'transparent'");
	    css.rule(".combo-box-popup .list-view .list-cell:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    css.rule(".combo-box-popup .list-view .list-cell:filled:selected", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	}
	
	private void addDialogStyles(CssBuilder css) {
	    // Dialog Container
	    css.start(".dialog-pane")
	       .add("-fx-border-color", "white") // analog der Stage
	       .add("-fx-border-width", 1 + "px") // analog der Stage
	       .add("-fx-background-color", UIUtils.toHex(playFieldBackground)) // Für den Bereich mit den Buttons.
	       .add("-fx-effect", "dropshadow(gaussian, rgba(255,0,0,1.0), 50, 0, 20, 20)")
	       .end();
	    
	    // HeaderBar in Dialogs
	    // ⚠️ ACHTUNG: Identische Styles auch in addMainWindowStyles() für .my-main-window .header-bar
	    css.start(".dialog-pane .header-bar")
	       .add("-fx-border-color", UIUtils.toHex(thinBorderColor))
	       .add("-fx-border-width", "0 0 " + thinBorderWidth + " 0")
	       .add("-fx-background-color", UIUtils.toHex(menuBarBackground))
	       .end();
	    
	    // Content VBox
	    css.start(".my-dialog-vbox")
	    .add("-fx-background-color", UIUtils.toHex(playFieldBackground))
	    .add("-fx-padding", font.getSize() * 0.5 + "px")
	    .add("-fx-alignment", "top-center") 
	    .end();
	    
	    // Content in ScrollPane
	    css.start(".my-dialog-scrollpane")
	    .add("-fx-background-color", UIUtils.toHex(playFieldBackground)) 
	    .end();
	    
	    // Viewport in Dialog
	    css.start(".dialog-pane .viewport")
	    .add("-fx-background-color", UIUtils.toHex(playFieldBackground)) 
	    .end();
	}
	
	private void addTextFieldStyles(CssBuilder css) {
	    Insets i = borderSmallComponent.insets();
	    String paddingCss = String.format("%dpx %dpx %dpx %dpx", i.top, i.right, i.bottom, i.left);
	    
	    css.start(".text-field")
	       .add("-fx-text-fill", UIUtils.toHex(textActiveComponentColor))
	       .add("-fx-alignment", "center")
	       .add("-fx-padding", paddingCss)
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.color()))
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .end();
	    
	    css.start(".text-field:disabled")
	       .add("-fx-opacity", "1.0")
	       .add("-fx-background-color", UIUtils.toHex(disabledComponentBgColor))
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.disabledColor()))
	       .add("-fx-text-fill", UIUtils.toHex(incorrectTextColor))
	       .add("-fx-font-weight", "bold")
	       .end();
	}
	
	private void addTextAreaStyles(CssBuilder css) {
	    css.start(".text-area")
	       .add("-fx-text-fill", UIUtils.toHex(textActiveComponentColor))
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.color()))
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .end();
	    
	    css.start(".text-area .content")
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .end();
	    
	    css.start(".text-area .viewport")
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .end();
	}
	
	private void addMenuStyles(CssBuilder builder) {   
		builder.rule(".menu-bar", "-fx-background-color", UIUtils.toHex(menuBarBackground)); // Hintergrund rechts vom Icon, hinter den Top-Menüs hinter dem Schrifthintergrund (Labels). Default ist hellgrau
		
		builder.rule(".menu-button", "-fx-padding", menuButtonPadding); // Für die Top-Level-Menüs wie Lernen, Datei, ... Wenn fontsize global gesetzt wird, berechnet javafx daraus paddings und die sind einfach zu groß...
	    builder.start(".menu-item")
	    		.add("-fx-padding", menuItemPadding) // Vertikaler Zeilenabstand und Padding links/rechts von Multiple Choice
	    		.add("-fx-font-family", "'" + font.getFamily() + "'") // Multiple Choice
	    		.end();
	    
	    builder.start(".my-spacer")
	    	.add("-fx-opacity", "0")
	    	.add("-fx-pref-height", "" + font.getSize() * 0.3 + "px")
	    	.end();
	    
	    // Der "klebende" Fokus wird unsichtbar mit "transparent" und Hover mit UIUtils.toHex(menuBarHoverBackground). Diesen klebenden Fokus gibt es allerdings nur beim Öffnen eines Untermnüs, nicht beim Öffnen eines Top-Menüs. Ich bin noch nicht überzeugt, dass man dieses Verhalten akzeptieren muss tbh...
	    // Ok, Gemini hat mir folgenden Link geschickt, das überzeugt mich nun zu 90% dass es ein JavaFX-Problem ist: https://bugs.openjdk.org/browse/JDK-8227679
	    // Auch wenn hier von ContextMenus gesprochen wird. Aber This is a minor annoyance, but not a serious issue. Lowering priority to P4. → Really???
	    builder.rule(".menu-item:focused", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground));
	    builder.rule(".menu-item:hover", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hover über Multiple Choice unter Lernen
	    builder.rule(".menu-item:disabled:hover", "-fx-background-color", "transparent"); // Schaltet den Hover für disabled Items aus.
	    builder.rule(".menu-item:disabled .label", "-fx-text-fill", UIUtils.toHex(textColor)); // "Speichern und beenden" ohne Session. JavaFX entsättigt die gewählte Farbe hier nochmal. Also selbst Color.Red setzen würde nur ein schmutziges graurot erzeugen. Ist aber ok für mich.
	    builder.rule(".menu:hover", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hover. Standard ist sonst einfach ein wahlloses blau. Scheint auch von nix abgeleitet zu sein, soweit ich es sehe.
	    builder.rule(".menu:showing", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hintergrund von Lernen, wenn ich über ein Untermenü hovere, wie Multiple Choice oder so. Standard ist das oben genannte wahllose blau.
	    
	    builder.start(".context-menu")
	    		.add("-fx-background-color", UIUtils.toHex(menuBarBackground))  // Untermenüs (Multiple Choice unter Lernen). Standard wäre hellgrau
	    		.add("-fx-border-color", UIUtils.toHex(thinBorderColor)) // Standard wäre kein Rahmen (außer dem Schatten, den gibt es immer.
	    		.add("-fx-border-width", thinBorderWidth + "px")
	    		.end();
	    
	    builder.start(".my-header-bar")
	    		.add("-fx-border-color", thinBorderColor) // Der Strich zwischen Menü und Spielfeld. Ja, transparent wir dnicht gezeichnet.
	    		.add("-fx-border-width", "0 0 1 0") // Die Dicke dieses Striches :-)
	    		.add("-fx-background-color", UIUtils.toHex(menuBarBackground))
	    		.end();
	}
	
	/**
	 * Sets the css hard for the Id of the TextLabel: #HistoryLabel, #ProgressLabel , #QuestionLabel
	 * 
	 * @param builder
	 */
	private void addSessionInfoLabelStyles(CssBuilder builder) {
		// History, Progress und Question
        for (TextLabelType type : TextLabelType.values()) {
            String selector = "#" + type.toString() + "Label";
            
            String fieldName = "displayText" + type.toString() + "BgColor";
            Color bg = (Color) getFieldValue(fieldName);
            
            BorderParams border = borderMediumComponent;
            java.awt.Insets insets = border.insets();
            String padding = String.format("%dpx %dpx %dpx %dpx", insets.top, insets.right, insets.bottom, insets.left);
            
            // 1. Container Styles (StackPane)
            // Background, Border, Padding etc. gehören auf den Container
            builder.start(selector)
            		.add("-fx-background-color", UIUtils.toHex(bg))
            		.add("-fx-border-color", UIUtils.toHex(border.color()))
            		.add("-fx-border-width", border.width() + "px")
            		.add("-fx-border-radius", border.arc() + "px")
            		.add("-fx-background-radius", border.arc() + "px")
            		.add("-fx-padding", padding)
            		.end();
            
            // 2. Text Styles (Text-Nodes)
            // Ein StackPane vererbt die Textfarbe NICHT automatisch an Text-Nodes. Wir müssen "Jeden javafx.scene.text.Text innerhalb von selector" ansprechen.
            builder.rule(selector + " Text", "-fx-fill", UIUtils.toHex(textColor));
        }
	}
	
	private void addIconButtonStyles(CssBuilder css) {
	    css.start(".my-icon-button")
	       .add("-fx-padding", "0")
	       .add("-fx-background-insets", "0")
	       .add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	       .add("-fx-border-color", UIUtils.toHex(borderSmallComponent.color()))
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .end();
	    
	    css.rule(".my-icon-button:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    
	    css.start(".my-icon-button:disabled")
	       .add("-fx-opacity", "1.0") // JavaFX setzt da sonst per se einen Default von 40% oder so für disabled...
	       .add("-fx-background-color", UIUtils.toHex(disabledComponentBgColor))
	       .end();
	}
	
	private void addImageMapStyles(CssBuilder builder) {
		// Border Overlay
	    builder.start(".my-image-map-pane #borderOverlay")
	       .add("-fx-border-color", UIUtils.toHex(borderBigComponent.color()))
	       .add("-fx-border-width", borderBigComponent.width() + "px")
	       .add("-fx-border-radius", (borderBigComponent.arc() / 2) + "px")
	       .end();
	    
	    // First Path (wird zuerst gezeichnet, unten). Transparent zum Raten. Mausclick wird ignoriert
	    builder.start(".first")
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", "transparent")
	       .add("-fx-stroke-line-cap", "round")
	       .add("-fx-stroke-line-join", "round")
	       .end();

	    // Second Path (wird danach gezeichnet, oben) Transparent zum Raten. Mausclick wird registriert
	    builder.start(".second")
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", "transparent")
	       .add("-fx-stroke-width", imageMapShapeBorderWidth + "px")
	       .add("-fx-stroke-line-cap", "round")
	       .add("-fx-stroke-line-join", "round")
	       .end();

	    // Rivers: Breite fürs Registrieren eines Klicks
	    builder.rule(".river .second", "-fx-stroke-width", imageMapLineShapeInnerWidth + "px");

	    // --- CORRECT State für Multipolygone ---
	    // First = Fill (grün), Second = Border (schwarz)
	    builder.rule(".my-image-map-shape:correct .first", "-fx-fill", UIUtils.toHex(correctColor));
	    builder.rule(".my-image-map-shape:correct .second", "-fx-stroke", UIUtils.toHex(borderShapeColor));
	    
	    // CORRECT State für Rivers: Erst dicken Border malen und dann kleiner darein korrekt malen
	    builder.start(".my-image-map-shape:correct.river .first")
	    	.add("-fx-stroke", UIUtils.toHex(borderShapeColor))
	    	.add("-fx-fill", "transparent")
	    	.add("-fx-stroke-width", imageMapShapeMarkedOuterWidth + "px")
	    	.end();
	    builder.start(".my-image-map-shape:correct.river .second")
    		.add("-fx-stroke", UIUtils.toHex(correctColor))
    		.add("-fx-fill", "transparent")
    		.add("-fx-stroke-width", imageMapShapeMarkedInnerWidth + "px")
    		.end();
	     
	    // --- INCORRECT State --- Gibt es aktuell nur für einen immer gleich großen Kreis...
	    // First = Fill (rot), Second = Border (schwarz)
	    builder.rule(".my-image-map-shape:incorrect .first", "-fx-fill", UIUtils.toHex(incorrectColor));
	    builder.rule(".my-image-map-shape:incorrect .second", "-fx-stroke", UIUtils.toHex(borderShapeColor));
	     
	    // --- MARKED State ---
	    // First = Border (schwarz dick), Second = Fill (gelb dünner)
	    builder.start(".my-image-map-shape:marked .first")
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", UIUtils.toHex(borderShapeColor))
	       .add("-fx-stroke-width", imageMapShapeMarkedOuterWidth + "px")
	       .end();
	    builder.start(".my-image-map-shape:marked .second")
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", UIUtils.toHex(markedColor))
	       .add("-fx-stroke-width", imageMapShapeMarkedInnerWidth + "px")
	       .end();
	}
	
	private void addImagePaneStyles(CssBuilder css) {
	    css.rule(".my-image-background-layer", "-fx-fill", UIUtils.toHex(imageLabelBgColor));
	    
	    css.start(".my-image-border-layer")
	       .add("-fx-stroke", UIUtils.toHex(borderBigComponent.color()))
	       .add("-fx-stroke-width", borderBigComponent.width() + "px")
	       .add("-fx-stroke-type", "inside")
	       .end();
	}
	
	private void addMainWindowStyles(CssBuilder css) {
	    // Root Container (Stage Border)
	    css.start(".my-root")
	       .add("-fx-border-color", "white") // Wir wollen einen weißen Rahmen um das gesamte Fenster!
	       .add("-fx-border-width", "1px") // Einen dünnen.
	       .end();
	    
	    // HeaderBar in MainWindow
	    /**css.start(".my-root .header-bar")
	       .add("-fx-border-color", "aqua")
	       .add("-fx-border-width", "0 0 1 0")
	       .add("-fx-background-color", "aqua")
	       .end();**/
	}

	private void addShapeMapStyles(CssBuilder builder) {
	    // Basis-Styling für alle Shapes (Border wird gezeichnet)
	    builder.start(".my-map-shape")
	       .add("-fx-stroke", UIUtils.toHex(borderShapeColor))
	       .add("-fx-stroke-width", shapeMapStandardBorderWidth + "px")
	       .end();
	    
	    // Aktive werden gefüllt und haben Hover
	    builder.rule(".my-map-shape:active", "-fx-fill", UIUtils.toHex(activeComponentBgColor));
	    
	    builder.start(".my-map-shape:active:hover")
	       .add("-fx-fill", UIUtils.toHex(activeComponentHoverColor))
	       .add("-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 0)")
	       .end();
	       
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "innershadow(one-pass-box, rgba(0,0,0,0.6), 4, 1.0, 3, 3)");
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "bloom(0.1)");
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "lighting(light(distant, -45, 45, white), 5.0, 1.5, 20, bump-input)");
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "reflection(top-offset 0, fraction 0.7, top-opacity 0.5, bottom-opacity 0.0)");
	    
	    // Korrekte, markierte und inkorrekte werden auch gefüllt aber haben keinen Hover...
	    builder.rule(".my-map-shape:correct", "-fx-fill", UIUtils.toHex(correctColor));
	    builder.rule(".my-map-shape:incorrect", "-fx-fill", UIUtils.toHex(incorrectColor));
	    builder.rule(".my-map-shape:marked", "-fx-fill", UIUtils.toHex(markedColor));

	    // Wenn Spiel pausiert ist (.game-paused auf dem Parent) bekommen aktive die disabledComponentBgColor und keinen Hover-Effekt
	    builder.rule(".my-shape-map-pane:paused .my-map-shape:active", "-fx-fill", UIUtils.toHex(disabledComponentBgColor));
	    
	    builder.start(".my-shape-map-pane:paused .my-map-shape:active:hover")
	       .add("-fx-fill", UIUtils.toHex(disabledComponentBgColor))
	       .add("-fx-effect", "null")
	       .end();
	    
	    // Spezifische Farben für Deko-Sets
	    if (shapeMapColor0 != null) {
	        builder.rule(".layer-neighbor", "-fx-fill", UIUtils.toHex(shapeMapColor0)); // Länder
	    }
	    if (shapeMapColor1 != null) {
	        builder.rule(".layer-water", "-fx-fill", UIUtils.toHex(shapeMapColor1)); // Gewässer
	    }	    
	    builder.start(".my-map-shape.layer-overlay") // Bundesländer z. B.
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", UIUtils.toHex(borderShapeColor))
	       .add("-fx-stroke-width", shapeMapFederalStateBorderWidth + "px")
	       .end();
	}

	private void addMultipleChoiceStyles(CssBuilder builder) {
	    // Padding dynamisch aus BorderParams
	    java.awt.Insets mcInsets = borderSmallComponent.insets();
	    String paddingCss = String.format("%dpx %dpx %dpx %dpx", 
	        mcInsets.top, mcInsets.right, mcInsets.bottom, mcInsets.left);
	    
	    // MC Buttons
	    builder.rule(".my-mc-button", "-fx-padding", paddingCss);
	    builder.rule(".my-mc-button:active", "-fx-background-color", UIUtils.toHex(activeComponentBgColor));
	    builder.rule(".my-mc-button:active:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    builder.rule(".my-mc-button:active:pressed", "-fx-background-color", UIUtils.toHex(adjustBrightness(activeComponentHoverColor, 8)));
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-mc-button:active:pressed", "-fx-translate-y", "1px");
	    //css.rule(".my-mc-button:active:pressed", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 0)");
	    builder.rule(".my-mc-button:inactive", "-fx-background-color", UIUtils.toHex(disabledComponentBgColor));
	    builder.start(".my-mc-button:correct")
	    		.add("-fx-background-color", UIUtils.toHex(correctColor));
	    if (mcIncorrectTextColor != null)
	        builder.add("-fx-text-fill", UIUtils.toHex(mcCorrectTextColor));
	    builder.end();
	    builder.start(".my-mc-button:incorrect")
	    		.add("-fx-background-color", UIUtils.toHex(incorrectColor));
	    if (mcIncorrectTextColor != null)
	    	builder.add("-fx-text-fill", UIUtils.toHex(mcIncorrectTextColor));
	    builder.end();
	    
	    // --- MC Button Layout Varianten (Pseudo-Klassen) ---
	    
	    // Padding für mehrzeilige Buttons berechnen (nur 1px oben/unten, damit 2 Zeilen passen)
	    // Horizontal lassen wir das normale Padding (insets.right/left), damit es optisch gleich aussieht
	    java.awt.Insets i = borderSmallComponent.insets();
	    double lineSpacingSqueezed = font.getSize() * -0.4;
	    double lineSpacingTiny = smallFont.getSize() * -0.4;
	    String squeezedPadding = String.format("0px %dpx 0px %dpx", i.right, i.left);

	    // ZWISCHENSTUFE: Squeezed (Normaler Font, aber extrem kompakt)
	    builder.start(".my-mc-button:squeezed")
	       .add("-fx-wrap-text", "true")
	       .add("-fx-padding", squeezedPadding)
	       .add("-fx-line-spacing", lineSpacingSqueezed + "px") 
	       .add("-fx-text-alignment", "center")
	       .end();
	    
	    // EXTREM: Tiny (Kleiner Font, enges Padding & Umbruch)
	    builder.start(".my-mc-button:tiny")
	       .add("-fx-wrap-text", "true")
	       .add("-fx-padding", squeezedPadding)
	       .add("-fx-line-spacing", lineSpacingTiny + "px") 
	       .add("-fx-font-size", smallFont.getSize() + "px")
	       .end();
	}
	
	/**
	 * Uses textColor as borderColor (e. g. because of skins without border)
	 * 
	 * @param css
	 */
	private void addMyTableStyles(CssBuilder css) {
		
	    css.start(".my-table-view .table-row-cell:odd")
	       .add("-fx-background-color", UIUtils.toHex(textColor) + ", " + UIUtils.toHex(playFieldBackground))
	       .end();
	    
	    css.start(".my-table-view .table-row-cell:focused")
	    	.add("-fx-background-insets", "0, 0 0 1 0")
	    	.end();
	    
	    css.start(".my-table-view .table-row-cell")
	       .add("-fx-background-color", UIUtils.toHex(textColor) + ", " + UIUtils.toHex(adjustBrightness(playFieldBackground, 5)))
	       .end();
	    
	    css.start(".my-table-view .table-row-cell:selected")
	    	.add("-fx-background-color", UIUtils.toHex(textColor) + ", " + UIUtils.toHex(menuBarHoverBackground))
	    	.add("-fx-background-insets", "0, 0 0 1 0")
	    	.end();
	    
	    css.start(".my-table-view .column-header-background")
	    	.add("-fx-background-color", UIUtils.toHex(playFieldBackground))
	    	.end();
	    
	    css.start(".my-table-view .column-header, .my-table-view .filler")
	    	.add("-fx-background-color", UIUtils.toHex(menuBarBackground))
	    	.add("-fx-border-color", "transparent " + UIUtils.toHex(textColor) + " " +  UIUtils.toHex(textColor) + " transparent")
	    	.end();
	    
	    css.start(".my-table-view .column-header .label")
	    	.add("-fx-alignment", "CENTER-LEFT")
	    	.end();
	    
	    css.start(".my-table-view .table-cell")
	       .add("-fx-border-color", "transparent " + UIUtils.toHex(textColor) + " transparent transparent")
	       .end();
	    
	    css.start(".my-table-view")
	    .add("-fx-border-color", textColor)
	    .add("-fx-focus-color", "transparent")
	    .add("-fx-border-width", "1") 
	    .add("-fx-faint-focus-color", "transparent")
	    .add("-fx-background-insets", "0")
	    .add("-fx-padding", "0")
	    .end();
	    
	    css.start(".my-table-view:focused")
	    .add("-fx-background-color", "-fx-control-inner-background")
	    .add("-fx-background-insets", "0")
	    .add("-fx-padding", "0")
	    .end();
	}
	
	private void addDashboardStyles(CssBuilder css) {
	    // === Gesamtes Tile ===
	    css.start(".dashboard-tile")
	       .add("-fx-border-color", borderBigComponent.color())
	       .add("-fx-border-width", borderBigComponent.width() + "px")
	       .add("-fx-border-radius",  borderBigComponent.arc() + "px")
	       .add("-fx-background-radius", borderBigComponent.arc() + "px")
	       .end();
	    
	    // === Oberer Bereich (große Zahl) ===
	    css.start(".dashboard-tile-top")
	       .add("-fx-background-color", displayTextProgressBgColor)
	       .add("-fx-pref-height", dashBoardTileTopHeight + "px")
	       .add("-fx-background-radius", borderBigComponent.arc() + "px " + borderBigComponent.arc() +  "px 0 0") // Nur oben abgerundet
	       .end();
	    
	    // === Unterer Bereich (Beschreibung) ===
	    css.start(".dashboard-tile-bottom")
	       .add("-fx-background-color", menuBarBackground) // !Sofort: Was soll denn der Default hier mal sein???
	       .add("-fx-pref-height", dashBoardTileBottomHeight + "px")
	       .add("-fx-border-color", borderBigComponent.color())
	       .add("-fx-border-width", borderBigComponent.width() + "px 0 0 0") // Trennstrich oben
	       .add("-fx-background-radius", "0 0 " + borderBigComponent.arc() + "px " + borderBigComponent.arc() +  "px") // Nur unten abgerundet
	       .end();
	    
	    // === Schrift oben (große Zahl) ===
	    css.start(".dashboard-tile-value")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", dashBoardTileTopFontSize + "px")
	       .add("-fx-fill", UIUtils.toHex(textColor))
	       .end();
	    
	    // === Schrift unten (Beschreibung) ===
	    css.start(".dashboard-tile-label")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", dashBoardTileBottomFontSize + "px")
	       .add("-fx-fill", UIUtils.toHex(textColor))
	       .end();
	}
	
	// !Später: Naja, so richtig überprüft habe ich nicht, ob die alle nötig sind. Und außerdem hier auch noch tooltip zu verstecken, hm...
	private void addChartStyles(CssBuilder css) {
		
		// Chart an sich
	    css.start(".chart")
	    	.add("-fx-background-color", "transparent")
	    	.add("-fx-category-gap", "3")
	    .end();
		
	    // Balken stylen - Standard (Ziel nicht erreicht)
	    css.start(".chart-bar")
	       .add("-fx-bar-fill", UIUtils.toHex(incorrectColor))
	       .add("-fx-border-color", UIUtils.toHex(textColor))
	       .add("-fx-border-width", "1px")
	       .end();
	    
	    // Balken stylen - Ziel erreicht
	    css.rule(".chart-bar:achieved", "-fx-bar-fill", UIUtils.toHex(correctColor));
	    
	    // Ziellinie stylen
	    css.start(".chart-series-line")
	       .add("-fx-stroke", UIUtils.toHex(textColor))
	       .add("-fx-stroke-width", "1px")
	       .end();
	    
	    // Achsen-Beschriftung stylen
	    css.start(".chart .axis")
	       .add("-fx-tick-label-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-tick-label-font-size", font.getSize() + "px")
	       .add("-fx-tick-label-fill", UIUtils.toHex(textColor))
	       //.add("-fx-tick-label-rotation", "-45")
	       .end();

	    // Grpße Tick-Marks auf y-Achse
	    css.start(".chart .axis:left .axis-tick-mark")
	    	.add("-fx-stroke", UIUtils.toHex(textColor))
	    	.add("-fx-stroke-width", "1px")
	    .end();

	    // Minor Tick-Marks weg
	    css.start(".chart .axis .axis-minor-tick-mark")
	    	.add("-fx-stroke", "transparent")
	    	.add("-fx-stroke-width", "0px")
	    .end();
	 
	    // Tick-Marks auf x-Achse weg
	    css.start(".chart .axis:bottom .axis-tick-mark")
	    	.add("-fx-stroke", "transparent")
	    	.add("-fx-stroke-width", "0px")
	    .end();
	    
	    css.start(".chart .axis:bottom")
	    	.add("-fx-border-color", UIUtils.toHex(textColor) + " transparent transparent transparent") // Es gibt einen Border um die ganze Beschriftung der x-Achse. Der obere Teil dieses Borders ist die x-Achse selbst. Herrje...
	    .end();
	    
	    css.start(".chart .axis:left")
    		.add("-fx-border-color", "transparent " + UIUtils.toHex(textColor) + " transparent transparent") // Siehe oben. Hier müssen wir dann rechts setzen
    	.end();
	    
	    // Achsen-Titel stylen (optional)
	    css.start(".chart .axis-label")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", font.getSize() + "px")
	       .add("-fx-text-fill", UIUtils.toHex(textColor))
	       .end();
	    
	    css.start(".fitbit-chart-container")
	    	.add("-fx-padding", "150px 200px 150px 200px")
	    .end();

	    css.rule(".chart-plot-background", "-fx-background-color", "transparent");
	    css.rule(".chart-content", "-fx-background-color", "transparent");
	    
	    css.start(".tooltip")
	    	.add("-fx-background-color", UIUtils.toHex(activeComponentBgColor)+"88")
	    .end();
	    
	    css.start(".tooltip .text")
	    	.add("-fx-fill", textActiveComponentColor)
	    .end();
	}
	
	private static class CssBuilder {
	    private final StringBuilder sb = new StringBuilder();
	    private boolean insideBlock = false;
	    private String currentSelector = null;
	    private final Set<String> usedSelectors = new HashSet<String>(); 

	    /**
	     * Öffnet einen CSS-Block.
	     * KNALLT, wenn der vorherige nicht geschlossen wurde!
	     */
	    public CssBuilder start(String selector) {
	    	checkSelector(selector);
	        if (insideBlock) {
	            throw new RuntimeException("CSS ERROR: You forgot to .end() the block for selector: '" + currentSelector + "' before starting '" + selector + "'");
	        }
	        sb.append(selector).append(" { ");
	        this.insideBlock = true;
	        this.currentSelector = selector;
	        return this;
	    }

	    /**
	     * Fügt eine Property zum aktuellen Block hinzu.
	     * KNALLT, wenn kein Block offen ist oder der Wert NULL ist.
	     */
	    public CssBuilder add(String property, String value) {
	        if (!insideBlock) {
	            throw new RuntimeException("CSS ERROR: Cannot add property '" + property + "' without calling .start(selector) first!");
	        }
	        if (value == null) {
	            throw new RuntimeException("CSS ERROR: Value is NULL for property '" + property + "' in selector '" + currentSelector + "'");
	        }
	        sb.append(property).append(": ").append(value).append("; ");
	        return this;
	    }
	    
	    public CssBuilder add(String property, Color color) {
	        return add(property, UIUtils.toHex(color));
	    }

	    /**
	     * Schließt den Block.
	     * KNALLT, wenn kein Block offen war.
	     */
	    public CssBuilder end() {
	        if (!insideBlock) {
	            throw new RuntimeException("CSS ERROR: Called .end() but no block was open!");
	        }
	        sb.append("}\n");
	        this.insideBlock = false;
	        this.currentSelector = null;
	        return this;
	    }

	    /**
	     * Komfort-Methode für Einzeiler (Shortcut).
	     * Macht intern start().add().end() automatisch.
	     */
	    public CssBuilder rule(String selector, String property, String value) {
	        return start(selector).add(property, value).end();
	    }
	    
	    /**
	     * Komfort-Methode für Einzeiler (Shortcut).
	     * Macht intern start().add().end() automatisch.
	     */
	    public CssBuilder rule(String selector, String property, Color color) {
	        return start(selector).add(property, UIUtils.toHex(color)).end();
	    }
	    
	    private void checkSelector(String selector) {
	    	if (usedSelectors.contains(selector))
	    		throw new RuntimeException("Warum nutzt Du nicht einen Block für " + selector + "?");
	    	else
	    		usedSelectors.add(selector);
	    }

	    /**
	     * Erzeugt das finale CSS.
	     * KNALLT, wenn noch ein Block offen ist! (Fail Fast für die Runtime)
	     */
	    public String build() {
	        if (insideBlock) {
	            throw new RuntimeException("CSS ERROR: Unclosed block at the end of generation! Missing .end() for: '" + currentSelector + "'");
	        }
	        return sb.toString();
	    }

	    /**
	     * Safe für Debugger & Logging.
	     * Wirft KEINE Exception, zeigt aber den Status an.
	     */
	    @Override
	    public String toString() {
	        if (insideBlock) {
	            return "[[🚧 BUILDING IN PROGRESS - Block open: " + currentSelector + "]]\n" + sb.toString();
	        }
	        return sb.toString();
	    }
	}
	
	// endregion

	// ========== create-Methoden
	// region
	public MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		return menuBar;
	}

	public Menu createMenu(String text) {
		return new Menu(text);
	}

	public MenuItem createMenuItem(String text) {
		return new MenuItem(text);
	}
	
	/**
	 * Holt das passende Hintergrundbild zur laufenden Session bzw das "leere", wenn keine Session läuft.
	 * Packt dieses in ein javafx.scene.layout.BackgroundImage. Kleinere werden hochskaliert damit sie passen.
	 * 
	 * @param type Darf null sein!
	 * @return
	 */
	public BackgroundImage getBackgroundImage(Deck type) {
		String bgPath = getBackgroundImagePath(type);
		BackgroundImage background;
	    try {
	        Image bgImage = new Image(new File(bgPath).toURI().toString());
	        background = new BackgroundImage(
	            bgImage,
	            BackgroundRepeat.NO_REPEAT,
	            BackgroundRepeat.NO_REPEAT,
	            BackgroundPosition.CENTER,
	            new BackgroundSize(
	                BackgroundSize.AUTO, 
	                BackgroundSize.AUTO, 
	                false, 
	                false, 
	                true,  // contain (Bild wird skaliert um reinzupassen. Ändert die Proportionen nicht)
	                true // cover (Bild wird hochskaliert um alles auszufüllen. Auch gestreckt wenn es sein muss)
	            )
	        );
	    } catch (Exception e) {
	        throw new RuntimeException("Konnte Hintergrundbild nicht laden: " + bgPath, e);
	    }
	    return background;
	}

	/**
	 * Creates a TextField with css-class = "input-field"
	 * 
	 * @param deck
	 * @return
	 */
	public TextField createInputField(Deck deck) {
	    Rectangle bounds = (Rectangle) getFieldValue(deck.getMapName() + "SessionTextInputPanel");
	    if (bounds == null)
			bounds = (Rectangle) getFieldValue(deck.getCategory().toString() + "SessionTextInputPanel");
	    
	    TextField textField = new TextField();
	    textField.setLayoutX(bounds.x);
	    textField.setLayoutY(bounds.y);
	    textField.setPrefWidth(bounds.width);
	    // Höhe wird von Font + Padding bestimmt
	    
	    return textField;
	}

	// Rückgabetyp angepasst
	public ImagePane createImageComponent(Deck type) {
	    // 1. Config laden (wie vorher)
	    java.awt.Rectangle bounds = (java.awt.Rectangle) getFieldValue(type.getId() + "SessionImagePanel");
	    if (bounds == null) 
	         bounds = (java.awt.Rectangle) getFieldValue(type.getCategory().toString() + "SessionImagePanel");

	    // 2. Parameter für Konstruktor vorbereiten
	    // Wir holen den Radius direkt aus der Config, da Rectangles ihn im Konstruktor wollen
	    double arc = borderBigComponent.arc() * 2; // JavaFX Arc ist Durchmesser, Config oft Radius? Prüf das kurz! 
	    // In deinem Upload BorderParams steht "arc". Bei Rectangle ist arcWidth der Durchmesser.
	    // Wenn borderBigComponent.arc() = 20 ist (Radius), braucht Rectangle 40.
	    
	    // Instanz erstellen
	    var pane = new ImagePane(bounds.width, bounds.height, arc);
	    pane.setLayoutX(bounds.x);
	    pane.setLayoutY(bounds.y);

	    // 3. Styling ist jetzt in der Klasse via "my-image-background-layer" vorbereitet.
	    // Wir müssen nur sicherstellen, dass die CSS-Regeln stimmen.

	    return pane;
	}
	
	// --- Bereinigte Factory-Methode ---
	public SessionInfoLabel createSessionInfoLabel(Deck deck, TextLabelType labelType) {
        Rectangle bounds = (Rectangle) getFieldValue(deck.getMapName() + "Session" + labelType + "Panel");
        if (bounds == null)
            bounds = (Rectangle) getFieldValue(deck.getCategory().toString() + "Session" + labelType + "Panel");
        
        SessionInfoLabel label = new SessionInfoLabel("");
        label.setLayoutX(bounds.x);
        label.setLayoutY(bounds.y);
        
        // StackPane braucht Breite & Höhe für Layout/Zentrierung
        label.setFixedWidth(bounds.width); 
        label.setFixedHeight(bounds.height); 
        
        // ID setzen, damit das CSS oben greift
        label.setId(labelType.toString() + "Label");
        
        return label;
    }

	public MultipleChoicePane createMultipleChoicePane(Deck deck) {
        // 1. Bounds holen
        Rectangle bounds = (Rectangle) getFieldValue(deck.getId() + "SessionMcPanel");
        if (bounds == null) 
            bounds = (Rectangle) getFieldValue(deck.getCategory().toString() + "SessionMcPanel");
        
        // 2. Padding und Border Werte holen
        java.awt.Insets insets = borderSmallComponent.insets();
        double verticalPadding = insets.top + insets.bottom;
        double horizontalPadding = insets.left + insets.right;
        double borderWidth = borderSmallComponent.width();
        
        double horizontalOverhead = horizontalPadding + (borderWidth * 2);

        // 3. Button-Höhe berechnen
        Text dummyText = new Text("Q");
        dummyText.setFont(font);
        
        double fixedButtonHeight = Math.ceil(dummyText.getLayoutBounds().getHeight() + verticalPadding + (borderWidth * 2));
        // JavaFX ist ein bisschen sehr generös bei der Höhe. Ein bissi weniger tut es auch...
        // Ja, das ist ein Hack. Aber ich vermute, einer der mich nie wieder stören wird, also sei es drum...
        fixedButtonHeight = Math.round(fixedButtonHeight * 0.95745f);

        // NEU: Berechnung des Spacings (identisch zur Logik in styleScene!)
        // !Sofort! Wieso wird das hier nochmal übergeben, wenn es in styleScene doch bereits als css definiert wurde???
        double lineSpacingSqueezed = font.getSize() * -0.4;

        // 5. Pane erstellen mit neuen Parametern0
        MultipleChoicePane result = new MultipleChoicePane(
            bounds.width, 
            fixedButtonHeight, 
            horizontalOverhead,
            borderWidth,
            font, 
            verticalGapMC,
            lineSpacingSqueezed // NEU übergeben
        );
        
        result.setLayoutX(bounds.x);
        result.setLayoutY(bounds.y);
        
        return result;
    }
	
	public Button createIconButton(Deck type, IconButtonType buttonType) {
	    // Icon laden
		String iconPath = switch (buttonType) {
			case BACK -> backButtonIcon;
			case SKIP -> skipButtonIcon;
			case PLAY -> playButtonIcon;
			case CANCEL -> cancelButtonIcon;
		};

		ImageView icon = new ImageView(new Image(new File(Config.get("iconFolder") + iconPath).toURI().toString()));
	    
	    // Button erstellen
	    Button button = new Button();
	    button.setGraphic(icon); // Icon setzen
	    button.getStyleClass().add("my-icon-button");
	    
	    // Bounds holen
	    Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionBackButton");
	    button.setPrefSize(bounds.width, bounds.height);
	    button.setLayoutX(bounds.x);
	    button.setLayoutY(bounds.y);
	    
	    return button;
	}

	public HeaderBar createMainWindowHeaderBar(Stage stage, MenuBar menuBar) {
	    HeaderBar headerBar = new HeaderBar();
	    
	    // CENTER: Title
	    Label titleLabel = new Label("Thos Suite (FX)");
	    titleLabel.getStyleClass().add("my-title");
	    headerBar.setCenter(titleLabel);
	    
	    // LEADING: Icon + MenuBar (Logik direkt hier)
	    // Wir nutzen Bindings statt runLater!
	    
	    // 1. Icon View erstellen (bindet sich an die Header-Höhe)
	    ImageView iconView = createResponsiveHeaderIcon(stage, headerBar);
	    
	    // 2. Layout zusammenbauen
	    double spacing = font.getSize() * 0.5;
	    HBox leftBox = new HBox(0); // Items kommen rein, sobald verfügbar
	    leftBox.setAlignment(Pos.CENTER_LEFT);
	    leftBox.setPadding(new javafx.geometry.Insets(0, 0, 0, spacing));
	    
	    if (iconView != null) {
	        leftBox.getChildren().add(iconView);
	    }
	    leftBox.getChildren().add(menuBar);
	    
	    headerBar.setLeading(leftBox);
	    
	    return headerBar;
	}

	// Hilfsmethode für das responsive Icon
	private ImageView createResponsiveHeaderIcon(Stage stage, HeaderBar headerBar) {
	    ObservableList<Image> icons = stage.getIcons(); // Schön, dass die Observable ist, aber für uns hier nicht von Belang!
	    if (icons.isEmpty()) return null;

	    ImageView iconView = new ImageView();
	    iconView.setPreserveRatio(true);
	    iconView.setSmooth(true); // Wichtig, aber bei passender Icon-Wahl weniger kritisch

	    // 1. Die Zielgröße berechnen (Live-Wert) Wir nehmen mal 55% der Höhe, zu groß soll das Icon ja auch nicht sein...
	    DoubleBinding targetSize = headerBar.heightProperty().multiply(0.55);
	    iconView.fitHeightProperty().bind(targetSize);

	    // 2. Binding für das "beste Bild" definieren
	    // Das aktualisiert sich automatisch, sobald targetSize sich ändert!
	    ObjectBinding<Image> bestIconBinding = Bindings.createObjectBinding(() -> {
	        double neededHeight = targetSize.get();
	        
	        // Initialer Layout-Pass kann 0 sein
	        if (neededHeight <= 0) return icons.get(0); 

	        return icons.stream()
	            // Nimm alle Icons, die mindestens so groß sind wie benötigt
	            .filter(img -> img.getHeight() >= neededHeight)
	            // Von denen nimm das kleinste (um unnötiges Downscaling zu vermeiden)
	            .min((a, b) -> Double.compare(a.getHeight(), b.getHeight()))
	            // Fallback: Wenn alle kleiner sind als benötigt, nimm das größte was da ist
	            .orElse(icons.get(icons.size() - 1));
	            
	    }, targetSize); // <--- WICHTIG: Abhängigkeit angeben!
	    // 3. Verkabeln
	    iconView.imageProperty().bind(bestIconBinding);

	    return iconView;
	}

	/**
	 * 
	 * We struggled quite a bit with having a maxHeight, having it always positioned on center and only having the
	 * maxHeight if really needed. This made the code quite ugly. But it works for now...
	 * 
	 * @param parent
	 * @param title
	 * @param message
	 * @param showCancelOption
	 * @param showResumeOption
	 * @return
	 */
	public Alert createAlert(Window parent, String title, String message, ButtonType... buttonTypes) {
	    Alert alert = new Alert(Alert.AlertType.NONE); 
	    alert.initOwner(parent);
	    alert.initStyle(StageStyle.EXTENDED);

	    // HeaderBar hinzufügen
	    HeaderBar headerBar = createDialogHeaderBar(title);
	    alert.getDialogPane().setHeader(headerBar);
	    
	    // --- Content Aufbau ---
	    Label label = new Label(message);
	    label.setWrapText(true);
	    label.setMaxWidth(Double.MAX_VALUE);

	    ScrollPane scroll = new ScrollPane(label) {
	        @Override
	        protected double computePrefHeight(double width) {
	            double contentHeight = super.computePrefHeight(width);
	            return Math.min(contentHeight, 1000);
	        }
	    };
	    
	    scroll.setFitToWidth(true);
	    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
	    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
	    scroll.getStyleClass().add("my-dialog-scrollpane");

	    alert.getDialogPane().setContent(scroll);
	    alert.getButtonTypes().setAll(buttonTypes);
	    alert.setGraphic(null);
	   
	    DialogPane dialogPane = alert.getDialogPane();
	    // !Architektur: Ohne dieses if waren die Alerts bei Fitbit nicht gestylet (die mit parent = null aufgerufen wurden)
	    if (dialogPane != null && dialogPane.getScene() != null)
	    	styleScene(dialogPane.getScene());
	    // !Architektut: Ohne diesen Listener waren der Großteil der Alerts sonst so im Spiel nicht gestylet...
	    dialogPane.sceneProperty().addListener((_, _, newScene) -> {
	        if (newScene != null) styleScene(newScene);
	    });
	    
	    if (parent == null && dialogPane.getScene() != null) {
	        Platform.runLater(() -> {
	        	Window window = alert.getDialogPane().getScene().getWindow();
	            window.sizeToScene();
	            
	            // Manuelle Zentrierung
	            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
	            double centerX = (screenBounds.getWidth() - window.getWidth()) / 2;
	            double centerY = (screenBounds.getHeight() - window.getHeight()) / 2;
	            
	            window.setX(centerX);
	            window.setY(centerY);
	            
	            System.out.println("Manually centered to: " + centerX + ", " + centerY);
	        });
	    }
	    
	    // Oder lieber gar kein Windows Close-Button oben rechts? Dann 0 setzen!
		headerBar.heightProperty().addListener((obs, oldVal, newVal) -> {
			if (alert.getDialogPane().getScene().getWindow() instanceof Stage)
				HeaderBar.setPrefButtonHeight((Stage) alert.getDialogPane().getScene().getWindow(), (double)newVal);
		});

	    return alert;
	}

	/**
	 * Convenience-Methode für Standard-Alerts (OK und optional: Abbrechen / Fortsetzen)
	 * Zeilenumbrüche einfach mit \n.
	 */
	public Alert createAlert(Window parent, String title, String message, boolean showCancelOption, boolean showResumeOption) {
	    List<ButtonType> buttons = new ArrayList<>();
	    buttons.add(new ButtonType("OK", ButtonBar.ButtonData.YES));
	    
	    if (showCancelOption) {
	        buttons.add(new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE));
	    }
	    if (showResumeOption) {
	        buttons.add(new ButtonType("Fortsetzen", ButtonBar.ButtonData.OTHER));
	    }
	    
	    // Aufruf der neuen generischen Methode
	    return createAlert(parent, title, message, buttons.toArray(new ButtonType[0]));
	}
	
	/**
	 * For just text with standard buttons, we use alerts. For more sophisticated popups
	 * like the playConfigDialogs, we create a simple dialog and leave the rest to the caller.
	 * 
	 * @param parent → Nur übergeben, wenn das Suitefenster bereits sauber angezeigt wird... 
	 * @return
	 */
	public Dialog<?> createDialog(Window parent, String title) {
	    Dialog<?> dialog = new Dialog<>();
	    dialog.initOwner(parent);
	    dialog.initStyle(StageStyle.EXTENDED);
	    
	    DialogPane dialogPane = dialog.getDialogPane();
	    
	    HeaderBar headerBar = createDialogHeaderBar(title);
	    dialogPane.setHeader(headerBar);
	    
	    // Zentrieren wenn kein Parent
	    if (parent == null) {
	        Platform.runLater(() -> {
	            Window window = dialog.getDialogPane().getScene().getWindow();	            
	            window.sizeToScene();
	            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
	            window.setX((bounds.getWidth() - window.getWidth()) / 2);
	            window.setY((bounds.getHeight() - window.getHeight()) / 2);
	        });
	    }
	    
	    // Oder lieber gar kein Windows Close-Button oben rechts? Dann 0 setzen!
        // Sicherstellen, dass Minimize und Close-Button die ganze Höhe ausnutzen...
        // Einigermaßen gefährlich, weil aus der Doku zu Dialogs:
        // this essentially means that the DialogPane is shown to users inside a Stage,
        // but future releases may offer alternative options (such as 'lightweight' or 'internal' dialogs).
	    headerBar.heightProperty().addListener((obs, oldVal, newVal) -> {
			if (dialog.getDialogPane().getScene().getWindow() instanceof Stage)
				HeaderBar.setPrefButtonHeight((Stage) dialog.getDialogPane().getScene().getWindow(), (double)newVal);
		});
	    
	    styleScene(dialogPane.getScene());
	    
	    return dialog;
	}

	// Im Skin
	public VBox createDialogContent() {
	    VBox vbox = new VBox(15);  // Nur Spacing im Konstruktor
	    vbox.getStyleClass().add("my-dialog-vbox");
	    return vbox;
	}
	
	private HeaderBar createDialogHeaderBar(String title) {
	    HeaderBar headerBar = new HeaderBar();
	    headerBar.getStyleClass().add("my-header-bar");
	    
	    Label titleLabel = new Label(title);
	    titleLabel.getStyleClass().add("my-title");
	    
	    double verticalPadding = font.getSize() * 0.3;
	    titleLabel.setPadding(new javafx.geometry.Insets(verticalPadding, 0, verticalPadding, 0));
	    
	    headerBar.setCenter(titleLabel);
	    return headerBar;
	}

	/**
	 * Erstellt ein Deutschlandpanel. Aktuell noch ohne Hintergrundbild. Die Größe berechnet sich aus: Höhe = height + 2 * yPadding. Der scale findet ohne
	 * Verzerrung statt, von daher ist die width dann auch aus diesen Parametern und der Auflösung der Map bestimmt.
	 */
	public ShapeMapPane createShapeMapPane(Deck deck) {
        // 1. Daten holen
        GeoMap map = MapService.getInstance().getMap(deck);
        
        // 2. Bounds via Reflection holen (AWT Rectangle)
        Rectangle bounds = (Rectangle) getFieldValue(deck.getMapName() + "SessionMapPanel");
        if (bounds == null) 
            bounds = (Rectangle) getFieldValue(deck.getCategory().toString() + "SessionMapPanel");
            
        // 3. Komponente erstellen (Skin bestimmt die Ziel-Höhe für den Zoom!)
        ShapeMapPane pane = new ShapeMapPane(map, bounds.height);
        
        // 4. Positionieren (Absolut)
        pane.setLayoutX(bounds.x);
        pane.setLayoutY(bounds.y);
        
        return pane;
    }

	public ImageMapPane createImageMapPanel(Deck type) {
		GeoMap map = MapService.getInstance().getMap(type);
		java.awt.Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMapPanel");
		BorderParams borderForRectangle = new BorderParams(borderBigComponent.width(), borderBigComponent.color(), borderBigComponent.insets(), borderBigComponent.arc()*2, borderBigComponent.focusWidth(), borderBigComponent.focusedColor(), borderBigComponent.disabledColor());
		ImageMapPane result = new ImageMapPane(map, bounds.width, bounds.height, borderForRectangle, new Rectangle(11, 11, 410, 254));
		result.setLayoutX(bounds.x);
		result.setLayoutY(bounds.y);
		return result;
	}
	
	public DashboardTile createDashboardTile(String value, String label) {
	    DashboardTile tile = new DashboardTile(value, label);
	    tile.setPrefSize(dashBoardTileWidth, dashBoardTileBottomHeight + dashBoardTileTopHeight);
	    tile.setMinSize(dashBoardTileWidth, dashBoardTileBottomHeight + dashBoardTileTopHeight);
	    tile.setMaxSize(dashBoardTileWidth, dashBoardTileBottomHeight + dashBoardTileTopHeight);
	    return tile;
	}
	
	// endregion

	/**
	 * Die Kartenbilder werden vom MapRepositories geholt
	 * 
	 * @param id
	 * @return
	 */
	public String getMapImagePath(Deck type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapImageName;
		default:
			return null;
		}
	}

	public String getMapInactiveImagePath(Deck type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapInactiveImageName;
		default:
			return null;
		}
	}

	public String getMapInactiveOverlayImagePath(Deck type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapInactiveOverlayImageName;
		default:
			return null;
		}
	}

	public String getMapOverlayImagePath(Deck type) {
		switch (type) {
		case WORLD_CARDS:
			return Config.get("mapImagesFolder") + worldMapOverlayImageName;
		default:
			return null;
		}
	}

	/**
	 * Holt das passende Hintergrundbild zur laufenden Session bzw das "leere", wenn keine Session läuft
	 * 
	 * @param deck Darf null sein!
	 * @return
	 */
	protected String getBackgroundImagePath(Deck deck) {
		if (deck == null)
			return Config.get("wallpaperFolder") + (emptyWallpaperName == null ? defaultWallpaperName : emptyWallpaperName);
		String bgName = (String) getFieldValue(deck.getMapName() + "WallpaperName");
		if (bgName != null)
			return Config.get("wallpaperFolder") + bgName;
		bgName = (String) getFieldValue(deck.getCategory().toString() + "WallpaperName");
		if (bgName != null)
			return Config.get("wallpaperFolder") + bgName;
		return Config.get("wallpaperFolder") + defaultWallpaperName;
	}

	/**
	 * 
	 * @param c
	 * @param intensity:
	 *            an int value between 0 (no change) and 100 (maximal change will lead to white or black...)
	 * @return
	 */
	public static Color adjustBrightness(Color c, int intensity) {	    
	    if (intensity < 0 || intensity > 100) {
	        throw new RuntimeException("Das soll ein Prozentwert sein für die adjustBrightness, du Witzbold :)");
	    }

	    double intensityF = intensity / 100.0;
	    double threshold = 1.0 - intensityF;

	    double brightness = c.getBrightness();
	    double newBrightness;

	    if (brightness > threshold) {
	        newBrightness = Math.max(0.0, brightness - intensityF); // abdunkeln
	    } else {
	        newBrightness = Math.min(1.0, brightness + intensityF); // aufhellen
	    }

	    // Neue Farbe erstellen via HSB-Factory
	    // Wichtig: c.getOpacity() übernimmt den Alpha-Wert (0.0 - 1.0)
	    return Color.hsb(c.getHue(), c.getSaturation(), newBrightness, c.getOpacity());
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
			return Color.web(value);
		else if (values.length == 1 && value.length() == 9) {
			Color result = Color.web(value.substring(0, 7));
			int alpha = Integer.parseInt(value.substring(7), 16);
			return new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
		} else if (values.length == 4)
			return new Color(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), (Float.parseFloat(values[3]) / 255));
		else
			throw new RuntimeException("Das Color-Format kenne ich nicht: " + value);
	}

	protected Font parseFont(String value) {
	    String[] values = value.split(",");
	    if (values.length == 3) {
	        String family = values[0];
	        int style = Integer.parseInt(values[1]); // Swing-Logik: 0=Plain, 1=Bold, 2=Italic
	        double size = Double.parseDouble(values[2]); // JavaFX nutzt double für Größe

	        // Wir übersetzen die Swing-Bitmaske in JavaFX Enums
	        // 1 = Bold, 2 = Italic, 3 = Bold + Italic
	        FontWeight weight = (style & 1) != 0 ? FontWeight.BOLD : FontWeight.NORMAL;
	        FontPosture posture = (style & 2) != 0 ? FontPosture.ITALIC : FontPosture.REGULAR;

	        return Font.font(family, weight, posture, size);
	    } else {
	        throw new RuntimeException("Das Font-Format kenne ich nicht: " + value);
	    }
	}

	/**
	 * Rechnet den arc / 2, weil FlatLaf nimmt das als Durchmesser und JavaFX als Radius...
	 * 
	 * @param value
	 * @return
	 */
	protected BorderParams parseBorderParams(String value) {
		String[] values = value.split(",");
		if (values.length == 7)
			return BorderParams.of(Integer.parseInt(values[0]), parseColor(values[1]),
					new Insets(Integer.parseInt(values[2]), Integer.parseInt(values[3]), Integer.parseInt(values[4]), Integer.parseInt(values[5])),
					Integer.parseInt(values[6]) / 2);
		else if (values.length == 3)
			return BorderParams.of(Integer.parseInt(values[0]), parseColor(values[1]), Integer.parseInt(values[2]) / 2);
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
	
}

package app.shared.skin;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import app.shared.Config;
import app.shared.Log;
import app.shared.UIUtils;
import app.shared.model.BorderParams;
import app.shared.model.CardData;
import app.shared.ui.DashboardTile;
import app.shared.ui.ImagePane;
import app.shared.ui.MultipleChoicePane;
import app.shared.ui.SessionInfoLabel;
import app.shared.ui.SuggestionTextField;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
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
 * 
 * TODO: Die beiden Alc- und FitbitStatisticScreens sind so voll mit UI-Kram. Ist das korrekt?
 * An sich sollte die Restsuite frei davon sein!
 * 
 * TODO: Gottklasse: CSS-Erzeugung, Property-Laden, Layout-Bounds, Komponenten-Bau in einer Klasse. Nope!
 * 
 * TODO: - **Komponentenerstellung vereinheitlichen** — ein durchgängiges Muster (gleicher Schnitt, gleicher Bau-/Nutzungsweg).
 * Und denk dran, auch in Swing oder JavaFX gibt man den einzelnen Komponenten Werte über setter und getter oder im Konstruktor mit
 * Das ist kein an sich verbotenes Muster solange die Aufrufe halt nur aus einem dezidierten Paket kommen. Das ist kein
 * leaken von Skin-Wissen in die Suite an sich. Aber vielleicht sollten die Komponenten auch besser hier liegen?
 * Also die Komponenten per se feature-agnostik bauen? Aber naja. Du willst ja nicht Swing oder JavaFX nachbauen! 
 * 
 */
@SuppressWarnings("deprecation")
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
	
	public record DiaryViewerComponents(
		    Pane root,
		    DatePicker fromPicker,
		    DatePicker toPicker,
		    TextField queryField,
		    VBox resultBox
		) {}
	
	public record MovieViewerComponents(
	        Pane root,
	        SuggestionTextField directorField,
	        SuggestionTextField actorField,
	        SuggestionTextField titleField,
	        VBox resultBox
	) {}
	
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

	/** Für MC, Deutschlandkarte, Welt **/ protected Color incorrectColor;  
	protected Color correctColor; // Für MC, Deutschlandkarte
	protected Color markedColor; // Für die Karten
	protected Color shapeMapColor0; // Für die Karten
	protected Color shapeMapColor1; // Für die Karten

	protected Color activeComponentBgColor; // Default für aktive MCButton, Karte, BackButton
	protected Color activeComponentHoverColor; // Default für MCButton, Karten, BackButton
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
	
	protected Integer imageMapShapeBorderWidth = 2;
	protected Integer imageMapLineShapeInnerWidth = 12;
	protected Integer imageMapShapeMarkedOuterWidth = 7;
	protected Integer imageMapShapeMarkedInnerWidth = 4;
	protected Rectangle2D worldSessionOverlayContentBounds = new Rectangle2D(11, 11, 410, 254);
	protected Rectangle2D defaultOverlayContentBounds      = new Rectangle2D(0, 0, 390, 300);
	
	protected Double shapeMapStandardBorderWidth = 1.8;
	protected Double shapeMapFederalStateBorderWidth = 2.8; // für Niedersachsen z. B.

	protected Font font;
	protected Font smallFont;

	protected BorderParams borderSmallComponent; // MC Buttons, InputField
	protected BorderParams borderMediumComponent; // QuestionLabel
	protected BorderParams borderBigComponent; // Für das Bild
	protected Color borderColor; // Komponenten
	protected Color borderShapeColor; // Karten
	protected Color thinBorderColor; // Um contentPane und unten die MenuBar
	protected Integer thinBorderWidth = 1; // default = 1
	protected Color stageBorderColor = Color.WHITE;
	
	protected Integer dashBoardTileWidth = 250;
	protected Integer dashBoardTileTopHeight = 250;
	protected Integer dashBoardTileBottomHeight = 100;
	protected Integer dashBoardTileTopFontSize; // font * 4
	protected Integer dashBoardTileBottomFontSize; // font * 2
	
	protected Integer diaryViewerContentWidth = 1200; // Hartcodiert. Für andere Auflösungen dann überschreiben.
	protected Integer diaryTooltipMargin = 20;
	
	protected Integer moviePosterWidth = 154;

	protected String backButtonIcon;
	protected String skipButtonIcon;
	protected String playButtonIcon;
	protected String cancelButtonIcon;

	protected String worldMapImageName;
	protected String worldMapInactiveImageName;
	protected String worldMapOverlayImageName;
	protected String worldMapInactiveOverlayImageName;
	protected String hannoverMapImageName = "Hannover 500.jpg";
	protected String hannoverMapInactiveImageName = null;
	protected String hannoverMapOverlayImageName = "Hannover small.png";
	protected String hannoverMapInactiveOverlayImageName = null;
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
	protected String chWallpaperName;
	protected String hsWallpaperName;
	protected String ozWallpaperName;
	protected String auWallpaperName;
	protected String brWallpaperName;
	protected String hrWallpaperName;

	protected Rectangle2D mcSessionQuestionPanel;
	protected Rectangle2D mcSessionImagePanel;
	protected Rectangle2D mcSessionMcPanel;
	protected Rectangle2D mcSessionProgressPanel;
	protected Rectangle2D mcSessionHistoryPanel;
	protected Rectangle2D mcSessionBackButton;
	protected Rectangle2D worldSessionMapPanel;
	protected Rectangle2D worldSessionQuestionPanel;
	protected Rectangle2D worldSessionTextInputPanel;
	protected Rectangle2D worldSessionImagePanel;
	protected Rectangle2D worldSessionMcPanel;
	protected Rectangle2D worldSessionProgressPanel;
	protected Rectangle2D worldSessionHistoryPanel;
	protected Rectangle2D worldSessionBackButton;
	protected Rectangle2D hannoverSessionMapPanel;
	protected Rectangle2D hannoverSessionQuestionPanel;
	protected Rectangle2D hannoverSessionTextInputPanel;
	protected Rectangle2D hannoverSessionImagePanel;
	protected Rectangle2D hannoverSessionMcPanel;
	protected Rectangle2D hannoverSessionProgressPanel;
	protected Rectangle2D hannoverSessionHistoryPanel;
	protected Rectangle2D hannoverSessionBackButton;
	protected Rectangle2D germanySessionMapPanel;
	protected Rectangle2D germanySessionQuestionPanel;
	protected Rectangle2D germanySessionTextInputPanel;
	protected Rectangle2D germanySessionImagePanel;
	protected Rectangle2D germanySessionMcPanel;
	protected Rectangle2D germanySessionProgressPanel;
	protected Rectangle2D germanySessionHistoryPanel;
	protected Rectangle2D germanySessionBackButton;
	protected Rectangle2D regionSessionQuestionPanel;
	protected Rectangle2D regionSessionMapPanel;
	protected Rectangle2D regionSessionTextInputPanel;
	protected Rectangle2D esSessionQuestionPanel;
	protected Rectangle2D esSessionMapPanel;
	protected Rectangle2D esSessionTextInputPanel;
	protected Rectangle2D itSessionQuestionPanel;
	protected Rectangle2D itSessionMapPanel;
	protected Rectangle2D itSessionTextInputPanel;
	protected Rectangle2D usSessionQuestionPanel;
	protected Rectangle2D usSessionMapPanel;
	protected Rectangle2D usSessionTextInputPanel;
	protected Rectangle2D csSessionQuestionPanel;
	protected Rectangle2D csSessionMapPanel;
	protected Rectangle2D csSessionTextInputPanel;
	protected Rectangle2D beSessionQuestionPanel;
	protected Rectangle2D beSessionMapPanel;
	protected Rectangle2D beSessionTextInputPanel;
	protected Rectangle2D enSessionQuestionPanel;
	protected Rectangle2D enSessionMapPanel;
	protected Rectangle2D enSessionTextInputPanel;
	protected Rectangle2D chSessionQuestionPanel;
	protected Rectangle2D chSessionMapPanel;
	protected Rectangle2D chSessionTextInputPanel;
	protected Rectangle2D hsSessionQuestionPanel;
	protected Rectangle2D hsSessionMapPanel;
	protected Rectangle2D hsSessionTextInputPanel;
	protected Rectangle2D ozSessionQuestionPanel;
	protected Rectangle2D ozSessionMapPanel;
	protected Rectangle2D ozSessionTextInputPanel;
	protected Rectangle2D auSessionQuestionPanel;
	protected Rectangle2D auSessionMapPanel;
	protected Rectangle2D auSessionTextInputPanel;
	protected Rectangle2D brSessionQuestionPanel;
	protected Rectangle2D brSessionMapPanel;
	protected Rectangle2D brSessionTextInputPanel;
	protected Rectangle2D hrSessionQuestionPanel;
	protected Rectangle2D hrSessionMapPanel;
	protected Rectangle2D hrSessionTextInputPanel;

	protected Integer verticalGapMC;

	/**
	 * Ist die Breite der ContentPane. Menüzeile und Border um die Rootpane sowie der Windows-Border gehören nicht dazu! --- Fenster = Spielfeld [+ Menü] +
	 * 2xBorder --- Von daher: Wenn deine Font größer wird bei einem Skinwechsel, dann vergrößert sich das Fenster
	 * 
	 * @return
	 */
	public Dimension2D getContentSize() {
		return new Dimension2D(1910, 1000);
	}
	
	// endregion
	
	// ========== CSS ==========
	// region
	
	public void styleScene(Scene scene) {
		menuBarHoverBackground = menuBarHoverBackground == null ? UIUtils.adjustBrightness(menuBarBackground, 20) : menuBarHoverBackground;
		menuDisabledForeground = menuDisabledForeground == null ? UIUtils.adjustBrightness(textColor, 90) : menuDisabledForeground;
		menuButtonPadding = menuButtonPadding == null ? font.getSize() * 0.3 + "px " + font.getSize() * 0.4 + "px" : menuButtonPadding;
		menuItemPadding = menuItemPadding == null ? font.getSize() * 0.1 + "px " + font.getSize() * 0.5 + "px" : menuItemPadding;
		playFieldBackground = playFieldBackground == null ? menuBarBackground : playFieldBackground;
		borderShapeColor = borderShapeColor == null ? borderColor : borderShapeColor;
		textActiveComponentColor = textActiveComponentColor == null ? textColor : textActiveComponentColor;
		dashBoardTileTopFontSize = dashBoardTileTopFontSize == null ? (int)font.getSize()*4 : dashBoardTileTopFontSize;
		dashBoardTileBottomFontSize = dashBoardTileBottomFontSize == null ? (int)font.getSize() : dashBoardTileBottomFontSize;
		displayTextHistoryBgColor = displayTextHistoryBgColor == null ? displayTextBgColor : displayTextHistoryBgColor;
		displayTextProgressBgColor = displayTextProgressBgColor == null ? displayTextBgColor : displayTextProgressBgColor;
		displayTextQuestionBgColor = displayTextQuestionBgColor == null ? displayTextBgColor : displayTextQuestionBgColor;
		hannoverSessionMapPanel = hannoverSessionMapPanel == null ? worldSessionMapPanel : hannoverSessionMapPanel;
		hannoverSessionQuestionPanel = hannoverSessionQuestionPanel == null ? worldSessionQuestionPanel : hannoverSessionQuestionPanel;
		hannoverSessionTextInputPanel = hannoverSessionTextInputPanel == null ? worldSessionTextInputPanel : hannoverSessionTextInputPanel;
		hannoverSessionImagePanel = hannoverSessionImagePanel == null ? worldSessionImagePanel : hannoverSessionImagePanel;
		hannoverSessionMcPanel = hannoverSessionMcPanel == null ? worldSessionMcPanel : hannoverSessionMcPanel;
		hannoverSessionProgressPanel = hannoverSessionProgressPanel == null ? worldSessionProgressPanel : hannoverSessionProgressPanel;
		hannoverSessionHistoryPanel = hannoverSessionHistoryPanel == null ? worldSessionHistoryPanel : hannoverSessionHistoryPanel;
		hannoverSessionBackButton = hannoverSessionBackButton == null ? worldSessionBackButton : hannoverSessionBackButton;
		
		/**
		// Dieser Code ist natürlich Quatsch. Aber ich bin so angepisst über diese -1 dass ich mir das jetzt gebaut habe.
		// Siehe auch den Kommentar in der Methode, die die Buttons stylet!
		Button button = new Button("Test Button");
        // 1. Der Button braucht eine Scene, um Zugriff auf die User-Agent-Styles (Modena) zu haben
        Scene dummyScene = new Scene(new StackPane(button));
        // 2. Jetzt CSS anwenden - JavaFX schaut nun in die Modena-Stylesheets
        button.applyCss();
        Background background = button.getBackground();
			// Wir nehmen den ersten BackgroundFill (wie in ScenicView zu sehen)
		    BackgroundFill firstFill = background.getFills().get(0);
		    javafx.geometry.Insets insets = firstFill.getInsets();
		    if (insets.getBottom() != -1)
		    	throw new RuntimeException("Alter jetzt ist der bottomInset für den Background plötzlich nicht mehr -1?");**/
	    
		// The color scheme of the default header buttons is automatically adjusted to remain easily recognizable by inspecting the Scene.fill property to gauge the brightness of the user interface. Applications should set the scene fill to a color that matches the user interface of the header bar area, even if the scene fill is not visible because it is obscured by other controls.
	    scene.setFill(menuBarBackground);
		
		CssBuilder css = new CssBuilder();
	    
	    // === GLOBAL: FONT ===
	    css.start(".root")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", font.getSize() + "px")
	       //.add("-fx-background-color", UIUtils.toHex(playFieldBackground)) // NEU!
	       .end();
	    css.rule(".text", "-fx-fill", textColor);
	    
	    // CSS generieren
		// Java-FX Klassen, deren Logik wir übernehmen und die durchaus auch mal in anderen Komponenten implizit benutzt werden könnten.
	    addButtonStyles(css);
	    addCheckBoxStyles(css);
	    addComboBoxStyles(css);
	    addDialogStyles(css);
	    addTextFieldStyles(css);
	    addTextAreaStyles(css);
	    addMenuStyles(css);
	    addDatePickerStyles(css);
	    addSpinnerStyles(css);
	    addScrollbarStyles(css);
	    
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
	    addSuggestionBoxStyles(css);
	    addDiaryViewerStyles(css);
	    addMovieViewerStyles(css);
	    
	    String rawCss = css.build(); // Hier kommt sauberes CSS raus: ".rule { color: #fff; }"

	    // 2. Für URL maskieren (Transport)
	    // Das ist der Schritt, den du meinst:
	    String encodedCss = rawCss.replace("%", "%25").replace("#", "%23");

	    // 3. Setzen
	    scene.getStylesheets().clear();
	    scene.getStylesheets().add("data:text/css," + encodedCss);
	    
	    //Log.debug(this, rawCss);
	}
	
	private void addButtonStyles(CssBuilder builder) {
	    // Standard Button (überall, auch TableView intern)
		// // Das date-picker muss dadrin stehen, weil es gibt auch arrow-buttons in den Menüs und mit denen wollen wir uns nicht anlegen!
	    builder.start(".button, .date-picker .arrow-button, .spinner .increment-arrow-button, .spinner .decrement-arrow-button")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // // Buttons haben in javafx per default -fx-background-insets = 0, 0, -1, 0. Wenn ich den erwische, der das verbrochen hat. Ich bisher keine Probleme sehen können durch das Angleichen an alle anderen Komponenten. Hier dokumentiert: https://www.pragmaticcoding.ca/javafx/elements/buttons
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", borderSmallComponent.color())
	       .add("-fx-background-color", activeComponentBgColor)
	       .end();
	    
	    builder.rule(".button .text, .date-picker .arrow-button .text", "-fx-fill", textActiveComponentColor);
	    builder.rule(".button:hover, .date-picker .arrow-button:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".button:pressed, .date-picker .arrow-button:pressed", "-fx-background-color", UIUtils.adjustBrightness(activeComponentHoverColor, 8));
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-mc-button:active:pressed", "-fx-translate-y", "1px");
	    //css.rule(".my-mc-button:active:pressed", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 0)");
	  
	}
	
	private void addCheckBoxStyles(CssBuilder builder) {  
		builder.start(".box")
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", "0")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", borderSmallComponent.color())
	       .add("-fx-background-color", activeComponentBgColor)
	       //.add("-fx-text-fill", UIUtils.toHex(textActiveComponentColor))
	    .end();
		
	    builder.rule(".box .text", "-fx-fill", textActiveComponentColor);
	    builder.rule(".box:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".box:pressed", "-fx-background-color", UIUtils.adjustBrightness(activeComponentHoverColor, 8));
	    builder.rule(".check-box:selected .mark", "-fx-background-color", textActiveComponentColor); // Die Farbe des Hakens in der Checkbox :-)
	}
	
	private void addScrollbarStyles(CssBuilder builder) {
		builder.rule(".scroll-bar .track", "-fx-background-color", UIUtils.adjustBrightness(playFieldBackground,10));
		builder.rule(".scroll-bar .thumb", "-fx-background-color", activeComponentBgColor);
		builder.rule(".scroll-bar .thumb:hover", "-fx-background-color", activeComponentHoverColor);
		builder.rule(".scroll-bar .increment-button, .scroll-bar .decrement-button", "-fx-background-color", menuBarBackground);
		builder.rule(".scroll-bar .increment-arrow, .scroll-bar .decrement-arrow", "-fx-background-color", textColor);
	}
	
	private void addComboBoxStyles(CssBuilder builder) {
	    builder.start(".combo-box-base")
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", borderSmallComponent.color())
	       .end();
	    
	    builder.rule(".combo-box-base .text", "-fx-fill", textActiveComponentColor);
	    builder.rule(".combo-box-base .arrow", "-fx-background-color", textActiveComponentColor);
	    
	    // ListView in ComboBox
	    // ⚠️ ACHTUNG: Wenn ListView woanders gebraucht wird, dort analog stylen
	    builder.rule(".combo-box-popup .list-view", "-fx-background-color", activeComponentBgColor);
	    builder.rule(".combo-box-popup .list-view .list-cell", "-fx-background-color", "'transparent'");
	    builder.rule(".combo-box-popup .list-view .list-cell:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".combo-box-popup .list-view .list-cell:filled:selected", "-fx-background-color", activeComponentHoverColor);
	}
	
	private void addDialogStyles(CssBuilder builder) {
	    // Dialog Container
	    builder.start(".dialog-pane")
	      // Ein dünner weißer Border sieht super aus auf Windows 10. Auf Windows 11 weniger, siehe Mail vom 10.02. ToDo
	      // .add("-fx-border-color", stageBorderColor) // analog der Stage
	      // .add("-fx-border-width", 1 + "px") // analog der Stage
	       .add("-fx-background-color", playFieldBackground) // Für den Bereich mit den Buttons.
	       .add("-fx-effect", "dropshadow(gaussian, rgba(255,0,0,1.0), 50, 0, 20, 20)")
	       .end();
	    
	    // HeaderBar in Dialogs
	    // ⚠️ ACHTUNG: Identische Styles auch in addMainWindowStyles() für .my-main-window .header-bar
	    builder.start(".dialog-pane .header-bar")
	       .add("-fx-border-color", thinBorderColor)
	       .add("-fx-border-width", "0 0 " + thinBorderWidth + " 0")
	       .add("-fx-background-color", menuBarBackground)
	       .end();
	    
	    // Content VBox
	    builder.start(".my-dialog-vbox")
	    .add("-fx-background-color", playFieldBackground)
	    .add("-fx-padding", font.getSize() * 0.5 + "px")
	    .add("-fx-alignment", "top-center") 
	    .end();
	    
	    // Content in ScrollPane
	    builder.start(".my-dialog-scrollpane")
	    .add("-fx-background-color", playFieldBackground)
	    .end();
	    
	    /* Fix: ScrollPane bekommt Fokus z. B. durch Mausklick und würde
	     * dann durch JavaFX's default -1.4 Insets (für Glow-Effekt) nach außen wachsen
	     * und den Border der DialogPane überdecken. Insets auf 0 halten.
	     */
	    builder.rule(".my-dialog-scrollpane:focused", "-fx-background-insets", "0");
	    
	    // Viewport in Dialog
	    builder.start(".dialog-pane .viewport")
	    .add("-fx-background-color", playFieldBackground) 
	    .end();
	}
	
	private void addTextFieldStyles(CssBuilder builder) {
	    Insets i = borderSmallComponent.insets();
	    String paddingCss = String.format("%dpx %dpx %dpx %dpx", (int)i.getTop(), (int)i.getRight(), (int)i.getBottom(), (int)i.getLeft());
	    
	  builder.start(".text-field")
	       .add("-fx-text-fill", textActiveComponentColor)
	       .add("-fx-alignment", "center")
	       .add("-fx-padding", paddingCss)
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-color", borderSmallComponent.color())
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .end();
	    
	    builder.start(".text-field:disabled")
	       .add("-fx-opacity", "1.0")
	       .add("-fx-background-color", disabledComponentBgColor)
	       .add("-fx-border-color", borderSmallComponent.disabledColor())
	       .add("-fx-text-fill", incorrectTextColor)
	       .add("-fx-font-weight", "bold")
	       .end();
	    
	    builder.start(".text-field:invalid-query")
	    	.add("-fx-text-fill", incorrectTextColor)
	    .end();
	}
	
	private void addTextAreaStyles(CssBuilder builder) {
	    builder.start(".text-area")
	       .add("-fx-text-fill", textActiveComponentColor)
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-color", borderSmallComponent.color())
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .end();
	    
	    builder.start(".text-area .content")
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .end();
	    
	    builder.start(".text-area .viewport")
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .end();
	}
	
	private void addMenuStyles(CssBuilder builder) {   
		builder.rule(".menu-bar", "-fx-background-color", menuBarBackground); // Hintergrund rechts vom Icon, hinter den Top-Menüs hinter dem Schrifthintergrund (Labels). Default ist hellgrau
		
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
	    builder.rule(".menu-item:focused", "-fx-background-color", menuBarHoverBackground);
	    builder.rule(".menu-item:hover", "-fx-background-color", menuBarHoverBackground); // Hover über Multiple Choice unter Lernen
	    builder.rule(".menu-item:disabled:hover", "-fx-background-color", "transparent"); // Schaltet den Hover für disabled Items aus.
	    builder.rule(".menu-item:disabled .label", "-fx-text-fill", textColor); // "Speichern und beenden" ohne Session. JavaFX entsättigt die gewählte Farbe hier nochmal. Also selbst Color.Red setzen würde nur ein schmutziges graurot erzeugen. Ist aber ok für mich.
	    builder.rule(".menu:hover", "-fx-background-color", menuBarHoverBackground); // Hover. Standard ist sonst einfach ein wahlloses blau. Scheint auch von nix abgeleitet zu sein, soweit ich es sehe.
	    builder.rule(".menu:showing", "-fx-background-color", menuBarHoverBackground); // Hintergrund von Lernen, wenn ich über ein Untermenü hovere, wie Multiple Choice oder so. Standard ist das oben genannte wahllose blau.
	    
	    builder.start(".context-menu")
	    		.add("-fx-background-color", menuBarBackground)  // Untermenüs (Multiple Choice unter Lernen). Standard wäre hellgrau
	    		.add("-fx-border-color", thinBorderColor) // Standard wäre kein Rahmen (außer dem Schatten, den gibt es immer.
	    		.add("-fx-border-width", thinBorderWidth + "px")
	    		.end();
	    
	    builder.start(".my-header-bar")
	    		.add("-fx-border-color", thinBorderColor) // Der Strich zwischen Menü und Spielfeld. Ja, transparent wir dnicht gezeichnet.
	    		.add("-fx-border-width", "0 0 1 0") // Die Dicke dieses Striches :-)
	    		.add("-fx-background-color", menuBarBackground)
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
            Insets insets = border.insets();
            String padding = String.format("%dpx %dpx %dpx %dpx", (int)insets.getTop(), (int)insets.getRight(), (int)insets.getBottom(), (int)insets.getLeft());
            
            // 1. Container Styles (StackPane)
            // Background, Border, Padding etc. gehören auf den Container
            builder.start(selector)
            		.add("-fx-background-color", bg)
            		.add("-fx-border-color", border.color())
            		.add("-fx-border-width", border.width() + "px")
            		.add("-fx-border-radius", border.arc() + "px")
            		.add("-fx-background-insets", border.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
            		.add("-fx-background-radius", border.arc() + "px")
            		.add("-fx-padding", padding)
            		.end();
            
            // 2. Text Styles (Text-Nodes)
            // Ein StackPane vererbt die Textfarbe NICHT automatisch an Text-Nodes. Wir müssen "Jeden javafx.scene.text.Text innerhalb von selector" ansprechen.
            builder.rule(selector + " Text", "-fx-fill", textColor);
        }
	}
	
	private void addIconButtonStyles(CssBuilder builder) {
	    builder.start(".my-icon-button")
	       .add("-fx-padding", "0")
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-border-color", borderSmallComponent.color())
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .end();
	    
	    builder.rule(".my-icon-button:hover", "-fx-background-color", activeComponentHoverColor);
	    
	    builder.start(".my-icon-button:disabled")
	       .add("-fx-opacity", "1.0") // JavaFX setzt da sonst per se einen Default von 40% oder so für disabled...
	       .add("-fx-background-color", disabledComponentBgColor)
	       .end();
	}
	
	private void addImageMapStyles(CssBuilder builder) {
		// Border Overlay
	    builder.start(".my-image-map-pane #borderOverlay")
	       .add("-fx-border-color", borderBigComponent.color())
	       .add("-fx-border-width", borderBigComponent.width() + "px")
	       .add("-fx-border-radius", (borderBigComponent.arc()) + "px")
	       .add("-fx-background-insets", borderBigComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
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
	    builder.rule(".my-image-map-shape:correct .first", "-fx-fill", correctColor);
	    builder.rule(".my-image-map-shape:correct .second", "-fx-stroke", borderShapeColor);
	    
	    // CORRECT State für Rivers: Erst dicken Border malen und dann kleiner darein korrekt malen
	    builder.start(".my-image-map-shape:correct.river .first")
	    	.add("-fx-stroke", borderShapeColor)
	    	.add("-fx-fill", "transparent")
	    	.add("-fx-stroke-width", imageMapShapeMarkedOuterWidth + "px")
	    	.end();
	    builder.start(".my-image-map-shape:correct.river .second")
    		.add("-fx-stroke", correctColor)
    		.add("-fx-fill", "transparent")
    		.add("-fx-stroke-width", imageMapShapeMarkedInnerWidth + "px")
    		.end();
	     
	    // --- INCORRECT State --- Gibt es aktuell nur für einen immer gleich großen Kreis...
	    // First = Fill (rot), Second = Border (schwarz)
	    builder.rule(".my-image-map-shape:incorrect .first", "-fx-fill", incorrectColor);
	    builder.rule(".my-image-map-shape:incorrect .second", "-fx-stroke", borderShapeColor);
	     
	    // --- MARKED State ---
	    // First = Border (schwarz dick), Second = Fill (gelb dünner)
	    builder.start(".my-image-map-shape:marked .first")
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", borderShapeColor)
	       .add("-fx-stroke-width", imageMapShapeMarkedOuterWidth + "px")
	       .end();
	    builder.start(".my-image-map-shape:marked .second")
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", markedColor)
	       .add("-fx-stroke-width", imageMapShapeMarkedInnerWidth + "px")
	       .end();
	}
	
	private void addImagePaneStyles(CssBuilder builder) {
	    builder.rule(".my-image-background-layer", "-fx-fill", imageLabelBgColor);
	    
	    builder.start(".my-image-border-layer")
	       .add("-fx-stroke", borderBigComponent.color())
	       .add("-fx-stroke-width", borderBigComponent.width() + "px")
	       .add("-fx-stroke-type", "inside")
	       .end();
	}
	
	private void addMainWindowStyles(CssBuilder builder) {
	    // Root Container (Stage Border)
		// Siehe  Kommentar im Dialog. Wegen Windows 11 rausgenommen, der hat eigenen Border und runde Ecken un ddas verträgt sich leider so gar nicht... 
	 /**   css.start(".my-root")
	       .add("-fx-border-color", stageBorderColor) // Wir wollen einen weißen Rahmen um das gesamte Fenster!
	       .add("-fx-border-width", "1px") // Einen dünnen.
	       .end();**/
	    
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
	       .add("-fx-stroke", borderShapeColor)
	       .add("-fx-stroke-width", shapeMapStandardBorderWidth + "px")
	       .end();
	    
	    // Aktive werden gefüllt und haben Hover
	    builder.rule(".my-map-shape:active", "-fx-fill", activeComponentBgColor);
	    
	    builder.start(".my-map-shape:active:hover")
	       .add("-fx-fill", activeComponentHoverColor)
	       .add("-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 0)")
	       .end();
	       
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "innershadow(one-pass-box, rgba(0,0,0,0.6), 4, 1.0, 3, 3)");
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "bloom(0.1)");
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "lighting(light(distant, -45, 45, white), 5.0, 1.5, 20, bump-input)");
	    //css.rule(".my-map-shape:active:hover", "-fx-effect", "reflection(top-offset 0, fraction 0.7, top-opacity 0.5, bottom-opacity 0.0)");
	    
	    // Korrekte, markierte und inkorrekte werden auch gefüllt aber haben keinen Hover...
	    builder.rule(".my-map-shape:correct", "-fx-fill", correctColor);
	    builder.rule(".my-map-shape:incorrect", "-fx-fill", incorrectColor);
	    builder.rule(".my-map-shape:marked", "-fx-fill", markedColor);
	    builder.rule(".my-map-shape:inactive", "-fx-fill", disabledComponentBgColor);

	    // Wenn Spiel pausiert ist (.game-paused auf dem Parent) bekommen aktive die disabledComponentBgColor und keinen Hover-Effekt
	    builder.rule(".my-shape-map-pane:paused .my-map-shape:active", "-fx-fill", disabledComponentBgColor);
	    
	    builder.start(".my-shape-map-pane:paused .my-map-shape:active:hover")
	       .add("-fx-fill", disabledComponentBgColor)
	       .add("-fx-effect", "null")
	       .end();
	    
	    // Spezifische Farben für Deko-Sets
	    if (shapeMapColor0 != null) {
	        builder.rule(".layer-neighbor", "-fx-fill", shapeMapColor0); // Länder
	    }
	    if (shapeMapColor1 != null) {
	        builder.rule(".layer-water", "-fx-fill", shapeMapColor1); // Gewässer
	    }	    
	    builder.start(".my-map-shape.layer-overlay") // Bundesländer z. B.
	       .add("-fx-fill", "transparent")
	       .add("-fx-stroke", borderShapeColor)
	       .add("-fx-stroke-width", shapeMapFederalStateBorderWidth + "px")
	       .end();
	}

	private void addMultipleChoiceStyles(CssBuilder builder) {
	    // Padding dynamisch aus BorderParams
	    Insets mcInsets = borderSmallComponent.insets();
	    String paddingCss = String.format("%dpx %dpx %dpx %dpx", 
	    		(int)mcInsets.getTop(), (int)mcInsets.getRight(), (int)mcInsets.getBottom(), (int)mcInsets.getLeft());
	    
	    // MC Buttons
	    builder.rule(".my-mc-button", "-fx-padding", paddingCss);
	    builder.rule(".my-mc-button:active", "-fx-background-color", activeComponentBgColor);
	    builder.rule(".my-mc-button:active:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".my-mc-button:active:pressed", "-fx-background-color", UIUtils.adjustBrightness(activeComponentHoverColor, 8));
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-mc-button:active:pressed", "-fx-translate-y", "1px");
	    //css.rule(".my-mc-button:active:pressed", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 0)");
	    builder.rule(".my-mc-button:inactive", "-fx-background-color", disabledComponentBgColor);
	    builder.rule(".my-mc-button:correct", "-fx-background-color", correctColor);
	    if (mcCorrectTextColor != null)
	        builder.rule(".my-mc-button:correct .text", "-fx-fill", mcCorrectTextColor); 
	        
	    builder.rule(".my-mc-button:incorrect", "-fx-background-color", incorrectColor);
	    if (mcIncorrectTextColor != null)
	        builder.rule(".my-mc-button:incorrect .text", "-fx-fill", mcIncorrectTextColor);
	    
	    // --- MC Button Layout Varianten (Pseudo-Klassen) ---
	    
	    // Padding für mehrzeilige Buttons berechnen (nur 1px oben/unten, damit 2 Zeilen passen)
	    // Horizontal lassen wir das normale Padding (insets.right/left), damit es optisch gleich aussieht
	    Insets i = borderSmallComponent.insets();
	    double lineSpacingSqueezed = mcLineSpacingSqueezed();
	    double lineSpacingTiny = smallFont.getSize() * -0.4;
	    String squeezedPadding = String.format("0px %dpx 0px %dpx", (int)i.getRight(), (int)i.getLeft());

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
	 * @param builder
	 */
	private void addMyTableStyles(CssBuilder builder) {
		
	    builder.start(".my-table-view .table-row-cell:odd")
	       .add("-fx-background-color", UIUtils.toHex(textColor) + ", " + UIUtils.toHex(playFieldBackground))
	       .end();
	    
	    builder.start(".my-table-view .table-row-cell:focused")
	    	.add("-fx-background-insets", "0, 0 0 1 0")
	    	.end();
	    
	    builder.start(".my-table-view .table-row-cell")
	       .add("-fx-background-color", UIUtils.toHex(textColor) + ", " + UIUtils.toHex(UIUtils.adjustBrightness(playFieldBackground, 5)))
	       .end();
	    
	    builder.start(".my-table-view .table-row-cell:selected")
	    	.add("-fx-background-color", UIUtils.toHex(textColor) + ", " + UIUtils.toHex(menuBarHoverBackground))
	    	.add("-fx-background-insets", "0, 0 0 1 0")
	    	.end();
	    
	    builder.start(".my-table-view .column-header-background")
	    	.add("-fx-background-color", playFieldBackground)
	    	.end();
	    
	    builder.start(".my-table-view .column-header, .my-table-view .filler")
	    	.add("-fx-background-color", menuBarBackground)
	    	.add("-fx-border-color", "transparent " + UIUtils.toHex(textColor) + " " +  UIUtils.toHex(textColor) + " transparent")
	    	.end();
	    
	    builder.start(".my-table-view .column-header .label")
	    	.add("-fx-alignment", "CENTER-LEFT")
	    	.end();
	    
	    builder.start(".my-table-view .table-cell")
	       .add("-fx-border-color", "transparent " + UIUtils.toHex(textColor) + " transparent transparent")
	       .end();
	    
	    builder.start(".my-table-view")
	    .add("-fx-border-color", textColor)
	    .add("-fx-focus-color", "transparent")
	    .add("-fx-border-width", "1") 
	    .add("-fx-faint-focus-color", "transparent")
	    .add("-fx-background-insets", "0")
	    .add("-fx-padding", "0")
	    .end();
	    
	    builder.start(".my-table-view:focused")
	    .add("-fx-background-color", "-fx-control-inner-background")
	    .add("-fx-background-insets", "0")
	    .add("-fx-padding", "0")
	    .end();
	}
	
	private void addDashboardStyles(CssBuilder builder) {
	    // === Gesamtes Tile ===
	    builder.start(".dashboard-tile")
	       .add("-fx-border-color", borderBigComponent.color())
	       .add("-fx-border-width", borderBigComponent.width() + "px")
	       .add("-fx-border-radius",  borderBigComponent.arc() + "px")
	       .add("-fx-background-insets", borderBigComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .add("-fx-background-radius", borderBigComponent.arc() + "px")
	       .end();
	    
	    // === Oberer Bereich (große Zahl) ===
	    builder.start(".dashboard-tile-top")
	       .add("-fx-background-color", displayTextProgressBgColor)
	       .add("-fx-pref-height", dashBoardTileTopHeight + "px")
	       .add("-fx-background-radius", borderBigComponent.arc() + "px " + borderBigComponent.arc() +  "px 0 0") // Nur oben abgerundet
	       .end();
	    
	    // === Unterer Bereich (Beschreibung) ===
	    builder.start(".dashboard-tile-bottom")
	       .add("-fx-background-color", menuBarBackground) // !Sofort: Was soll denn der Default hier mal sein???
	       .add("-fx-pref-height", dashBoardTileBottomHeight + "px")
	       .add("-fx-border-color", borderBigComponent.color())
	       .add("-fx-border-width", borderBigComponent.width() + "px 0 0 0") // Trennstrich oben
	       .add("-fx-background-radius", "0 0 " + borderBigComponent.arc() + "px " + borderBigComponent.arc() +  "px") // Nur unten abgerundet
	       .end();
	    
	    // === Schrift oben (große Zahl) ===
	    builder.start(".dashboard-tile-value")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", dashBoardTileTopFontSize + "px")
	       .add("-fx-fill", textColor)
	       .end();
	    
	    // === Schrift unten (Beschreibung) ===
	    builder.start(".dashboard-tile-label")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", dashBoardTileBottomFontSize + "px")
	       .add("-fx-fill", textColor)
	       .end();
	}
	
	// !Später: Naja, so richtig überprüft habe ich nicht, ob die alle nötig sind. Und außerdem hier auch noch tooltip zu verstecken, hm...
	private void addChartStyles(CssBuilder builder) {
		
		// Chart an sich
	    builder.start(".chart")
	    	.add("-fx-background-color", "transparent") // Hintertgrund des ganzen (Balken-)Diagramms mit Achsen und Gedöns.
	    .end();
		
	    // Borders für alle Balken
	    builder.start(".chart-bar")
	    	.add("-fx-border-color", textColor)
	    	.add("-fx-border-width", "1px")
	    .end();
	    
	    // Balken stylen - Ziel erreicht / nicht erreicht
	    builder.rule(".chart-bar:achieved", "-fx-bar-fill", correctColor);
	    builder.rule(".chart-bar:failed", "-fx-bar-fill", incorrectColor);
	    builder.rule(".chart-bar:in-progress", "-fx-bar-fill", markedColor);
	    
	    // Ziellinie stylen
	    builder.start(".chart-series-line")
	       .add("-fx-stroke", textColor)
	       .add("-fx-stroke-width", "1px")
	       .end();
	    
	    // Achsen-Beschriftung stylen
	    builder.start(".chart .axis")
	       .add("-fx-tick-label-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-tick-label-font-size", font.getSize() + "px")
	       .add("-fx-tick-label-fill", textColor)
	       .add("-fx-tick-label-rotation", "-30")
	       .end();

	    // Grpße Tick-Marks auf y-Achse
	    builder.start(".chart .axis:left .axis-tick-mark")
	    	.add("-fx-stroke", textColor)
	    	.add("-fx-stroke-width", "1px")
	    .end();

	    // Minor Tick-Marks weg
	    builder.start(".chart .axis .axis-minor-tick-mark")
	    	.add("-fx-stroke", "transparent")
	    	.add("-fx-stroke-width", "0px")
	    .end();
	 
	    // Tick-Marks auf x-Achse weg
	    builder.start(".chart .axis:bottom .axis-tick-mark")
	    	.add("-fx-stroke", "transparent")
	    	.add("-fx-stroke-width", "0px")
	    .end();
	    
	    builder.start(".chart .axis:bottom")
	    	.add("-fx-border-color", UIUtils.toHex(textColor) + " transparent transparent transparent") // Es gibt einen Border um die ganze Beschriftung der x-Achse. Der obere Teil dieses Borders ist die x-Achse selbst. Herrje...
	    .end();
	    
	    builder.start(".chart .axis:left")
    		.add("-fx-border-color", "transparent " + UIUtils.toHex(textColor) + " transparent transparent") // Siehe oben. Hier müssen wir dann rechts setzen
    	.end();
	    
	    // Achsen-Titel stylen
	    builder.start(".chart .axis-label")
	       .add("-fx-font-family", "'" + font.getFamily() + "'")
	       .add("-fx-font-size", font.getSize() + "px")
	       .add("-fx-text-fill", textColor)
	       .end();
	    
	    builder.start(".chart-root")
	    	.add("-fx-padding", "50px 50px 50px 50px") // !Sofort: Wird ausgelagert in properties!
	    .end();

	    builder.rule(".chart-plot-background", "-fx-background-color", "transparent");
	    builder.rule(".chart-content", "-fx-background-color", "transparent");
	    
	    // Chart Layout (VBox)
	    builder.start(".chart-container") // Der Container, der Kinder vertikal anordnet und die Steuer-Bar und darunter das Diagramm enthält.
	       .add("-fx-spacing", "20px") // vertikaler Abstand zwischen den Kind-Elementen (Datepicker und Diagramm)
	    .end();

	    // Chart Controls (HBox)
	    double spacing = font.getSize() * 0.5;
	    builder.start(".chart-controls") // Der Container mit den Datepickern und der Balkenbreite
	       .add("-fx-spacing", spacing + "px") // Abstand zwischen den Kindern (z. B. Label "Von", "Bis" und dem Datepicker daneben)
	       .add("-fx-alignment", "center-left") // center damit die Labels mittig platziert sind. "Left" macht gerade nix
	    .end();
	    
	    // Der Tooltip ist bisher noch nicht weiter gestylet bezüglich der Ecken und Border und so. Ist mir gerade nicht so wichtig... Default ist ohne Border und abgerundet anscheinend.
	    
	   /** builder.start(".tooltip")
	    	.add("-fx-background-color", UIUtils.toHex(activeComponentBgColor))
	    .end();
	    
	    builder.start(".tooltip .text")
	    	.add("-fx-fill", textActiveComponentColor)
	    .end();**/
	}
	
	private void addDatePickerStyles(CssBuilder builder) {
		
		// === Der Datepicker selber hat sonst unsere Borders des Textfelds und Kalenders kaputt gemacht ===
		builder.start(".date-picker")
		   .add("-fx-background-color", "transparent")  // Kein eigener Background
		   .add("-fx-border-color", "transparent")      // Kein eigener Border
		   .add("-fx-background-insets", "0")
		   .add("-fx-padding", "0")
		   .end();
	    
	    // === Arrow Button (Kalender-Icon) ===
	    builder.start(".date-picker .arrow-button")
	       .add("-fx-background-color", activeComponentBgColor)
	       .end();
	    
	    builder.rule(".date-picker .arrow-button:hover", "-fx-background-color", activeComponentHoverColor);
	    
	    builder.start(".date-picker .arrow-button .arrow")
	       .add("-fx-background-color", textActiveComponentColor)
	       .end();
	    
	    // === Popup Container ===
	    builder.start(".date-picker-popup")
	       .add("-fx-background-color", playFieldBackground)
	       .add("-fx-border-color", borderColor)
	       .add("-fx-border-width", thinBorderWidth + "px")
	       .end();
	    
	    // === Month/Year Header ===
	    builder.start(".date-picker-popup .month-year-pane")
	       .add("-fx-background-color", menuBarBackground)
	       .end();
	    
	    builder.start(".date-picker-popup .month-year-pane .label .text")
	    .add("-fx-fill", textColor)  // -fx-fill für Text-Nodes!
	    .add("-fx-font-weight", "bold")
	    .end();
	    
	    // === Spinner Buttons (< >) ===
	    builder.start(".date-picker-popup .spinner .button")
	       .add("-fx-background-color", activeComponentBgColor)
	       .end();
	    
	    builder.rule(".date-picker-popup .spinner .button:hover", "-fx-background-color", activeComponentHoverColor);
	    
	    builder.start(".date-picker-popup .spinner .button .left-arrow")
	       .add("-fx-background-color", textActiveComponentColor)
	       .end();
	    
	    builder.start(".date-picker-popup .spinner .button .right-arrow")
	       .add("-fx-background-color", textActiveComponentColor)
	       .end();
	    
	    // === Wochentag-Header ===
	    builder.start(".date-picker-popup .day-name-cell")
	       .add("-fx-background-color", playFieldBackground)
	       .end();
	    builder.start(".date-picker-popup .day-name-cell .text")
	       .add("-fx-fill", textColor)
	       .end();
	    
	    // === Tages-Zellen ===
	    // Normale Tage des aktuellen Monats
	    builder.start(".date-picker-popup .day-cell .text")
	    .add("-fx-fill", textColor)
	    	.end();
	    builder.start(".date-picker-popup .day-cell")
	       .add("-fx-background-color", playFieldBackground)
	    .end();

	    // Tage anderer Monate explizit
	    builder.start(".date-picker-popup .day-cell.previous-month .text, " +
	              ".date-picker-popup .day-cell.next-month .text")
	    	.add("-fx-fill", textColor)
	    .end();
	    builder.start(".date-picker-popup .day-cell.previous-month, .date-picker-popup .day-cell.next-month")
	       .add("-fx-background-color", UIUtils.adjustBrightness(playFieldBackground, 20))
	    .end();
	    
	    builder.rule(".date-picker-popup .day-cell:hover", "-fx-background-color", UIUtils.adjustBrightness(playFieldBackground, 40));
	    
	    /**
	    // === Heutiges Datum ===
	    css.start(".date-picker-popup .today")
	       .add("-fx-border-color", textColor)
	       .add("-fx-border-width", "1px")
	       .end();
	    
	    // === Ausgewähltes Datum ===
	    css.start(".date-picker-popup .selected")
	       .add("-fx-background-color", activeComponentBgColor)
	       .end();**/
	}
	
	private void addSpinnerStyles(CssBuilder builder) {
		
		// Siehe Datepicker. Zur Sicherheit setzen wir in dem Container mal alles schön auf 0
		builder.start(".spinner")
		   .add("-fx-background-color", "transparent")
		   .add("-fx-border-color", "transparent")
		   .add("-fx-pref-width", "-1")
		   .add("-fx-background-insets", "0")
		   .add("-fx-padding", "0")
		   .add("-fx-pref-width", "-1") // Tendenziell keine Wirkung, kann wohl weg
		   .add("-fx-min-width", "-fx-pref-width") // Tendenziell keine Wirkung, kann wohl weg
		   .end();
		
		builder.start(".spinner .text-field")
			.add("-fx-pref-column-count", "5")
			.end();

		builder.start(".spinner .text-field:focused")
			.add("-fx-background-insets", "0") // Den Fokus-Border auf diesem Fake-TextField bekommt man nur so weg. Gerade keine Nerven da tiefer einzusteigen.
		.end();
	}
	
	// !Sofort: Magic Numbers
	// !Sofort: Highlight-Background der Labels ignoriert Border-Radius der VBox — JavaFX clippt Children nicht an abgerundeten Ecken, Lösung: Clip im Skin setzen
	private void addSuggestionBoxStyles(CssBuilder builder) {
		builder.start(".suggestion-box")
	    	.add("-fx-background-color", menuBarBackground)
	    	.add("-fx-border-color", borderColor)
	    	.add("-fx-border-width", "1px")
	    	//.add("-fx-effect", "dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2)")
	    .end();
		
		builder.start(".suggestion-box .label")
	    	.add("-fx-padding", "5 5")
	    .end();
		
		builder.start(".suggestion-box :highlighted")
			.add("-fx-background-color", menuBarHoverBackground)
		.end();
		
	    builder.start(".tag-chip-remove")
	       .add("-fx-border-width", "0px")
	       .add("-fx-border-color", Color.TRANSPARENT)
	       .add("-fx-background-color", playFieldBackground)
	       .add("-fx-padding", "0 0")
	    .end();
	    
	    builder.rule(".tag-chip-remove .text", "-fx-fill", textColor);
	    
	}
	
	private void addDiaryViewerStyles(CssBuilder css) {
	    css.start(".diary-card")
	        .add("-fx-background-color", displayTextQuestionBgColor)
	        .add("-fx-border-color", borderBigComponent.color())
	        .add("-fx-border-width", borderBigComponent.width() + "px")
	        .add("-fx-border-radius", borderBigComponent.arc() + "px")
	        .add("-fx-background-radius", borderBigComponent.arc() + "px")
	        .add("-fx-background-insets", borderBigComponent.width() + "px")
	        .add("-fx-padding", font.getSize() + "px")
	        .add("-fx-cursor", "hand")
	        .end();

	    css.rule(".diary-card:hover", "-fx-background-color", displayTextQuestionBgColor);

	    css.start(".diary-card-date")
	        .add("-fx-font-weight", "bold")
	        .add("-fx-fill", textColor)
	        .end();

	    css.start(".diary-card-tags")
	        .add("-fx-fill", textColor)
	        .add("-fx-font-style", "italic")
	        .end();

	    css.start(".diary-card-text")
	        .add("-fx-fill", textColor)
	        .end();

	    css.start(".diary-viewer-hint")
	        .add("-fx-fill", incorrectTextColor)
	        .add("-fx-font-style", "italic")
	        .end();

	    css.start(".diary-viewer-content")
	    	.add("-fx-spacing", font.getSize() + "px")
	    .end();
	    
	    css.start(".diary-viewer-scroll")
	    	.add("-fx-padding", "0 " + font.getSize() * 0.5 + "px 0 0")
	    	.add("-fx-background-color", "transparent")
	    	.add("-fx-background", "transparent")
	    .end();

	    css.start(".diary-viewer-scroll .viewport")
	    	.add("-fx-background-color", "transparent")
	    	.end();
	    
	    css.start(".diary-viewer-filter-bar")
	    .add("-fx-max-width", diaryViewerContentWidth + "px")
	    .add("-fx-min-width", diaryViewerContentWidth + "px")
	    .end();

	css.start(".diary-viewer-root")
	    .add("-fx-padding", font.getSize() + "px 0px")
	    .end();
	}
	
	private void addMovieViewerStyles(CssBuilder css) {
		 
	    // === Gesamtlayout ===
	    double padding = font.getSize();
	    css.start(".movie-viewer-root")
	        .add("-fx-padding", padding + "px")
	        .end();
	 
	    css.start(".movie-viewer-content")
	        .add("-fx-spacing", padding + "px")
	        .end();
	 
	    // === SWYT-Bereich (links) ===
	    // Die Breite wird prozentual über prefWidth/maxWidth im createMovieViewer gesetzt?
	    // Nein — wir nutzen CSS min/max-width nicht, weil die prozentuale Berechnung
	    // zur Laufzeit im createMovieViewer stattfindet. Hier nur Spacing und Padding.
	    css.start(".movie-viewer-swyt")
	        .add("-fx-padding", "0")
	        .end();
	 
	    css.start(".movie-viewer-swyt .label")
	        .add("-fx-fill", textColor)
	        .end();
	 
	    // === ScrollPane ===
	    css.start(".movie-viewer-scroll")
	        .add("-fx-background-color", "transparent")
	        .add("-fx-background", "transparent")
	        .end();
	 
	    css.start(".movie-viewer-scroll .viewport")
	        .add("-fx-background-color", "transparent")
	        .end();
	 
	    // === Rating-Zahl ===
	    // Schriftgröße wird dynamisch im createCard gesetzt (50% der Posterbreite).
	    // min-width sorgt dafür, dass einstellige und zweistellige Zahlen gleich breit sind.
	    css.start(".movie-card-rating")
	    	.add("-fx-font-weight", "bold")
	    	.add("-fx-alignment", "center")
	    	.add("-fx-font-size", (int)(moviePosterWidth * 0.5) + "px")
	    	//.add("-fx-min-width", "80px")
	    	.add("-fx-padding", "0")
	    	.end();
	 
	    css.start(".movie-card-rating .text")
	        .add("-fx-fill", textColor)
	        .end();
	 
	    // === Header ===	 
	    css.start(".movie-card-header .text")
	        .add("-fx-fill", textColor)
	        .add("-fx-font-weight", "bold")
	        .end();
	 
	    // === Text ===
	    css.start(".movie-card-text .text")
	        .add("-fx-fill", textColor)
	        .end();
	 
	    // === Links (Schauspieler/Regisseure) ===
	    css.start(".movie-card-link")
	        .add("-fx-underline", "true")
	        .add("-fx-cursor", "hand")
	        .end();
	 
	    css.start(".movie-card-link .text")
	        .add("-fx-fill", textColor)
	        .end();
	 
	 // === Kommentar-Popup (nutzt Popup statt Tooltip, siehe setupCommentTooltip) ===
        BorderParams border = borderMediumComponent;
	    css.start(".movie-comment-popup")
	    	.add("-fx-border-color", border.color())
	    	.add("-fx-border-width", border.width() + "px")
	    	.add("-fx-border-radius", border.arc() + "px")
    		.add("-fx-background-insets", border.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
    		.add("-fx-background-radius", border.arc() + "px")
	        .add("-fx-background-color", disabledComponentBgColor)  // TODO: anpassen             // TODO: anpassen
	        .add("-fx-padding", font.getSize() * 0.5 + "px") // TODO: anpassen
	        .add("-fx-font-size", font.getSize() + "px")      // TODO: anpassen
	        .end();
	    
	    //css.rule(".movie-comment-popup .text", "-fx-fill", textColor);
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
	public BackgroundImage getBackgroundImage(String mapName, String deckCategoryName) {
		Path bgPath = Config.getPath("wallpaperFolder").resolve(getBackgroundImageName(mapName, deckCategoryName));
		BackgroundImage background;
		try {
		    Image bgImage = new Image(bgPath.toUri().toString());
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
	
	// !Sofort: Boah, also wie viele getBackgroundImage-Methoden mit Code-Duplizierung denn noch? Das muss refactoret werden! 
	public BackgroundImage getStartBackgroundImage() {
		String wallpaperName = emptyWallpaperName == null ? defaultWallpaperName : emptyWallpaperName;
		Path bgPath = Config.getPath("wallpaperFolder").resolve(wallpaperName);
		BackgroundImage background;
		try {
		    Image bgImage = new Image(bgPath.toUri().toString());
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
	
	// TODO: !Sofort: Also mit empty und default geht es aber ein bisschen durcheinander. Hier holt empty das default *lol*
	public BackgroundImage getEmptyBackgroundImage() {
		return getBackgroundImage(null, null);
	}
	
	private String getBackgroundImageName (String mapName, String categoryName) {
		if (mapName == null || categoryName == null)
			return defaultWallpaperName == null ? emptyWallpaperName : defaultWallpaperName;
		String bgName = (String) getFieldValue(mapName + "WallpaperName");
		if (bgName != null)
			return bgName;
		bgName = (String) getFieldValue(categoryName + "WallpaperName");
		if (bgName != null)
			return bgName;
		return defaultWallpaperName;
	}

	/**
	 * Creates a TextField with css-class = "input-field"
	 * 
	 * @param deck
	 * @return
	 */
	public TextField createInputField(String mapName, String categoryName) {
	    Rectangle2D bounds = (Rectangle2D) getFieldValue(mapName + "SessionTextInputPanel");
	    if (bounds == null)
			bounds = (Rectangle2D) getFieldValue(categoryName + "SessionTextInputPanel");
	    
	    TextField textField = new TextField();
	    textField.setLayoutX(bounds.getMinX());
	    textField.setLayoutY(bounds.getMinY());
	    textField.setPrefWidth(bounds.getWidth());
	    // Höhe wird von Font + Padding bestimmt
	    
	    return textField;
	}

	// Rückgabetyp angepasst
	public ImagePane createImageComponent(String deckId, String deckCategory) {
	    // 1. Config laden (wie vorher)
	    Rectangle2D bounds = (Rectangle2D) getFieldValue(deckId + "SessionImagePanel");
	    if (bounds == null) 
	         bounds = (Rectangle2D) getFieldValue(deckCategory + "SessionImagePanel");

	    // 2. Parameter für Konstruktor vorbereiten
	    // Wir holen den Radius direkt aus der Config, da Rectangles ihn im Konstruktor wollen
	    double arc = borderBigComponent.arc() * 2; // JavaFX Arc ist Durchmesser, Config oft Radius? Prüf das kurz! 
	    // In deinem Upload BorderParams steht "arc". Bei Rectangle ist arcWidth der Durchmesser.
	    // Wenn borderBigComponent.arc() = 20 ist (Radius), braucht Rectangle 40.
	    
	    // Instanz erstellen
	    var pane = new ImagePane(bounds.getWidth(), bounds.getHeight(), arc);
	    pane.setLayoutX(bounds.getMinX());
	    pane.setLayoutY(bounds.getMinY());

	    // 3. Styling ist jetzt in der Klasse via "my-image-background-layer" vorbereitet.
	    // Wir müssen nur sicherstellen, dass die CSS-Regeln stimmen.

	    return pane;
	}
	
	// --- Bereinigte Factory-Methode ---
	public SessionInfoLabel createSessionInfoLabel(String deckMapName, String deckCategory, TextLabelType labelType) {
        Rectangle2D bounds = (Rectangle2D) getFieldValue(deckMapName + "Session" + labelType + "Panel");
        if (bounds == null)
            bounds = (Rectangle2D) getFieldValue(deckCategory + "Session" + labelType + "Panel");
        
        SessionInfoLabel label = new SessionInfoLabel("");
        label.setLayoutX(bounds.getMinX());
        label.setLayoutY(bounds.getMinY());
        
        // StackPane braucht Breite & Höhe für Layout/Zentrierung
        label.setFixedWidth(bounds.getWidth()); 
        label.setFixedHeight(bounds.getHeight()); 
        
        // ID setzen, damit das CSS oben greift
        label.setId(labelType.toString() + "Label");
        
        return label;
    }

	public MultipleChoicePane createMultipleChoicePane(String id, String category) {
	    Rectangle2D bounds = (Rectangle2D) getFieldValue(id + "SessionMcPanel");
	    if (bounds == null)
	        bounds = (Rectangle2D) getFieldValue(category + "SessionMcPanel");

	    Insets insets = borderSmallComponent.insets();
	    double borderWidth = borderSmallComponent.width();
	    double horizontalOverhead = insets.getLeft() + insets.getRight() + (borderWidth * 2);

	    // TODO: Hier reichen wir skin-eigene Werte (font, overhead, border, spacing) als Daten nach draußen an MC —
	    //   wenn auch nur innerhalb von shared. Streng genommen sickern damit skin.properties in eine UI-Komponente.
	    //   Nochmal prüfen, wie ok das ist. Gegenargument: result.setLayoutX(bounds.getMinX()) unten tut faktisch
	    //   dasselbe, und nicht jeder UI-Parameter ist per CSS stylebar — ganz verhindern lässt sich das nicht.
	    //   Also hier vielleicht nicht zu dogmatisch sein.
	    MultipleChoicePane.Metrics metrics = new MultipleChoicePane.Metrics(
	            font, horizontalOverhead, borderWidth, mcLineSpacingSqueezed());

	    MultipleChoicePane result = new MultipleChoicePane(
	            bounds.getWidth(), computeMcButtonHeight(), verticalGapMC, metrics);
	    result.setLayoutX(bounds.getMinX());
	    result.setLayoutY(bounds.getMinY());
	    return result;
	}

	private double computeMcButtonHeight() {
	    Insets insets = borderSmallComponent.insets();
	    double verticalPadding = insets.getTop() + insets.getBottom();
	    double borderWidth = borderSmallComponent.width();

	    Text dummyText = new Text("Q");
	    dummyText.setFont(font);

	    // JavaFX ist großzügig mit der Höhe; ein bisschen weniger reicht. Hack wie gehabt.
	    double h = Math.ceil(dummyText.getLayoutBounds().getHeight() + verticalPadding + (borderWidth * 2));
	    return Math.round(h * 0.95745f);
	}

	private double mcLineSpacingSqueezed() {
	    return font.getSize() * -0.4;
	}
	
	public Button createIconButton(String deckId, IconButtonType buttonType) {
	    // Icon laden
		String iconName = switch (buttonType) {
			case BACK -> backButtonIcon;
			case SKIP -> skipButtonIcon;
			case PLAY -> playButtonIcon;
			case CANCEL -> cancelButtonIcon;
		};

		Image image = new Image(Config.getPath("iconFolder").resolve(iconName).toUri().toString());
		if (buttonType == IconButtonType.BACK)
		    image = UIUtils.tintImage(image, textActiveComponentColor);
		ImageView icon = new ImageView(image);
	    
	    // Button erstellen
	    Button button = new Button();
	    button.setGraphic(icon); // Icon setzen
	    button.getStyleClass().add("my-icon-button");
	    
	    // Bounds holen
	    Rectangle2D bounds = (Rectangle2D) getFieldValue(deckId + "SessionBackButton");
	    button.setPrefSize(bounds.getWidth(), bounds.getHeight());
	    button.setLayoutX(bounds.getMinX());
	    button.setLayoutY(bounds.getMinY());
	    
	    return button;
	}

	public HeaderBar createMainWindowHeaderBar(Stage stage, MenuBar menuBar) {
	    HeaderBar headerBar = new HeaderBar();
	    
	    // CENTER: Title
	    Label titleLabel = new Label("Thos Suite (FX)");
	    HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE_SUBTREE);
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
	    
	    // Fallback auf ownerStage wenn parent == null
	    // !Sofort: Hilfsmethoden ohne Angabe eines parents wären besser. Diese dann nur, wenn man dezidiert ein anderes braucht. Kann also erst einmal private vermutlich
	    Window effectiveParent = parent != null ? parent : SkinService.getOwnerWindow();
	    if (effectiveParent != null) {
	        alert.initOwner(effectiveParent);
	    }
	    
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
	    //Log.info(this, "ScrollPane StyleClass: " + scroll.getStyleClass());

	    alert.getDialogPane().setContent(scroll);
	    alert.getButtonTypes().setAll(buttonTypes);
	    alert.setGraphic(null);
	   
	    DialogPane dialogPane = alert.getDialogPane();
	    // !Architektur: Ohne dieses if waren die Alerts bei Fitbit nicht gestylet (die mit parent = null aufgerufen wurden)
	    if (dialogPane != null && dialogPane.getScene() != null)
	    	styleScene(dialogPane.getScene());
	    // !Architektur: Ohne diesen Listener waren der Großteil der Alerts sonst so im Spiel nicht gestylet...
	    dialogPane.sceneProperty().addListener((_, _, newScene) -> {
	        if (newScene != null)
	        	styleScene(newScene);
	    });
	    
	    // Oder lieber gar kein Windows Close-Button oben rechts? Dann 0 setzen!
		headerBar.heightProperty().addListener((_, _, newVal) -> {
			if (alert.getDialogPane().getScene().getWindow() instanceof Stage)
				HeaderBar.setPrefButtonHeight((Stage) alert.getDialogPane().getScene().getWindow(), (double)newVal);
		});

		// Direkt vor return alert;
		Log.info(this, "=== ALERT ERSTELLT ===");
		Log.info(this, "ScrollPane background: " + scroll.getBackground());
		if (scroll.getBackground() != null && !scroll.getBackground().getFills().isEmpty()) {
		    Log.info(this, "ScrollPane background-insets: " + scroll.getBackground().getFills().get(0).getInsets());
		}
		Log.info(this, "ScrollPane boundsInLocal: " + scroll.getBoundsInLocal());
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
	    Window effectiveParent = parent != null ? parent : SkinService.getOwnerWindow();
	    if (effectiveParent != null) {
	        dialog.initOwner(effectiveParent);
	    }
	    dialog.initOwner(parent); // !Sofort: Was war denn hier der Grund, dann ergibt ja der if darüber keinen Sinn. Hier ist was falsch!
	    dialog.initStyle(StageStyle.EXTENDED);
	    
	    DialogPane dialogPane = dialog.getDialogPane();
	    
	    HeaderBar headerBar = createDialogHeaderBar(title);
	    dialogPane.setHeader(headerBar);
	    
	    // Oder lieber gar kein Windows Close-Button oben rechts? Dann 0 setzen!
        // Sicherstellen, dass Minimize und Close-Button die ganze Höhe ausnutzen...
        // Einigermaßen gefährlich, weil aus der Doku zu Dialogs:
        // this essentially means that the DialogPane is shown to users inside a Stage,
        // but future releases may offer alternative options (such as 'lightweight' or 'internal' dialogs).
	    headerBar.heightProperty().addListener((_, _, newVal) -> {
			if (dialog.getDialogPane().getScene().getWindow() instanceof Stage)
				HeaderBar.setPrefButtonHeight((Stage) dialog.getDialogPane().getScene().getWindow(), (double)newVal);
		});
	    
	    styleScene(dialogPane.getScene());
	    
	    return dialog;
	}
	
	public void setDialogTitle(Dialog<?> dialog, String title) {
	    javafx.scene.Node node = dialog.getDialogPane().getHeader().lookup(".my-title");
	    if (node instanceof Label label) {
	        label.setText(title);
	    }
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
	    HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE_SUBTREE);
	    return headerBar;
	}

	/**
	 * 
	 * TODO: Dieses Wrapper-Konstrukt hier wurde mal eingeführt, um die Zirkel aus den Paketen rauszubekommen. Allerdings
	 * wurde dabei echt ein Monster geschaffen. Das muss wieder vereinfacht werden. Noch mal mit frischem Blick drauf
	 * schauen bitte. Das muss doch einfacher gehen. PS: Wieso gibt es bei ImageMapPane eigentlich nichts vergleichbares?
	 * 
	 * @param shapeNodes
	 * @param mapName
	 * @param category
	 * @return
	 */
	public Region buildShapeMapWrapper(List<Node> shapeNodes, String mapName, String category) {
	    Rectangle2D bounds = (Rectangle2D) getFieldValue(mapName + "SessionMapPanel");
	    if (bounds == null)
	        bounds = (Rectangle2D) getFieldValue(category + "SessionMapPanel");

	    // Inhalt in eine Group — den Container baut der Skin, nicht die Domäne.
	    Group contentGroup = new Group();
	    for (Node node : shapeNodes) {
	        node.setStyle(""); // Altlasten (alte Inline-Strichdicken der letzten Session) raus → sauber messen.
	        contentGroup.getChildren().add(node);
	    }

	    // Messen auf Default-Styles (noch keine Scene → stabile UA-Defaults).
	    contentGroup.applyCss();
	    contentGroup.layout();
	    final double scaleFactor = bounds.getHeight() / contentGroup.getBoundsInLocal().getHeight();

	    Scale scale = new Scale(scaleFactor, scaleFactor);
	    scale.setPivotX(0);
	    scale.setPivotY(0);
	    contentGroup.getTransforms().add(scale);

	    StackPane wrapper = new StackPane(contentGroup); // StackPane zentriert automatisch.
	    wrapper.getStyleClass().add("my-shape-map-pane");
	    wrapper.setLayoutX(bounds.getMinX());
	    wrapper.setLayoutY(bounds.getMinY());

	    // Inverse-Scaling-Fix für konstante Strichdicken — erst wenn die Scene (und damit das Skin-CSS) da ist.
	    wrapper.sceneProperty().addListener(new javafx.beans.value.ChangeListener<javafx.scene.Scene>() {
	        @Override
	        public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> obs,
	                javafx.scene.Scene oldScene, javafx.scene.Scene newScene) {
	            if (newScene != null) {
	                contentGroup.applyCss();
	                for (Node node : contentGroup.getChildren()) {
	                    if (node instanceof javafx.scene.shape.Shape s) {
	                        double w = s.getStrokeWidth();
	                        if (w > 0)
	                            s.setStyle("-fx-stroke-width: "
	                                    + String.format(java.util.Locale.US, "%.4f", w / scaleFactor) + "px;");
	                    }
	                }
	                wrapper.sceneProperty().removeListener(this);
	            }
	        }
	    });

	    return wrapper;
	}

	public Node applyImageMapLayout(Region pane, String id) {
	    Rectangle2D bounds = (Rectangle2D) getFieldValue(id + "SessionMapPanel");
	    pane.setPrefSize(bounds.getWidth(), bounds.getHeight());
	    pane.setMaxSize(bounds.getWidth(), bounds.getHeight());
	    pane.setLayoutX(bounds.getMinX());
	    pane.setLayoutY(bounds.getMinY());
	    pane.getStyleClass().add("my-image-map-pane");

	    // Clip = programmatisches Gegenstück zum #borderOverlay-Border. Um die Border-Breite eingerückt,
	    // arc*2 weil JavaFX den Arc als Durchmesser nimmt, unsere Config als Radius.
	    double bw = borderBigComponent.width();
	    Rectangle clip = new Rectangle(bw, bw, bounds.getWidth() - 2 * bw, bounds.getHeight() - 2 * bw);
	    clip.setArcWidth(borderBigComponent.arc() * 2);
	    clip.setArcHeight(borderBigComponent.arc() * 2);
	    return clip;
	}

	// TODO: overlayContentBounds beschreibt, wo der Inhalt im Mini-Map-Bild sitzt — eigentlich
	// Karten-/Asset-Daten, kein Styling. Liegt nur hier, weil hartcodiert. Sobald berechenbar
	// (Overlay-Größe + prozentualer Rand), wandert das hoch zur Karte. Bis dahin: Felder mit Defaults.
	public Rectangle2D getOverlayContentBounds(String id) {
	    Rectangle2D b = (Rectangle2D) getFieldValue(id + "SessionOverlayContentBounds");
	    return b != null ? b : defaultOverlayContentBounds;
	}
	
	public DashboardTile createDashboardTile(String value, String label) {
	    DashboardTile tile = new DashboardTile(value, label);
	    tile.setPrefSize(dashBoardTileWidth, dashBoardTileBottomHeight + dashBoardTileTopHeight);
	    tile.setMinSize(dashBoardTileWidth, dashBoardTileBottomHeight + dashBoardTileTopHeight);
	    tile.setMaxSize(dashBoardTileWidth, dashBoardTileBottomHeight + dashBoardTileTopHeight);
	    return tile;
	}
	
	public DatePicker createDatePicker(LocalDate defaultDate) {
		DatePicker result = new DatePicker(defaultDate);
		result.setShowWeekNumbers(false);
		result.setOnShowing(_ -> {
            Platform.runLater(() -> {                
                Window.getWindows().stream()
                    .filter(w -> w instanceof PopupWindow)
                    .forEach(popup -> {
                        Scene popupScene = popup.getScene();
                        if (popupScene != null) {
                            SkinService.get().styleScene(popupScene);
                        }
                    });
            });
        });
		return result;
	}
	
	public DiaryViewerComponents createDiaryViewer() {
	    // Filterleiste
	    DatePicker fromPicker = createDatePicker(LocalDate.now().minusMonths(1));
	    DatePicker toPicker = createDatePicker(LocalDate.now());

	    TextField queryField = new TextField();
	    queryField.setPromptText("(x or y) and tag:z");
	    HBox.setHgrow(queryField, Priority.ALWAYS);

	    HBox filterBar = new HBox(12);
	    filterBar.setAlignment(Pos.CENTER_LEFT);
	    filterBar.getStyleClass().add("diary-viewer-filter-bar");
	    filterBar.getChildren().addAll(
	            new Label("Von:"), fromPicker,
	            new Label("Bis:"), toPicker,
	            queryField);

	    // Ergebnisbereich
	    VBox resultBox = new VBox(12);
	    resultBox.setPadding(new Insets(12, 0, 12, 0));

	    // Content-VBox (Karten)
	    VBox content = new VBox(12);
	    content.getStyleClass().add("diary-viewer-content");
	    content.setMaxWidth(diaryViewerContentWidth);
	    //content.setMinWidth(diaryViewerContentWidth);
	    content.getChildren().add(resultBox);

	    // ScrollPane
	    ScrollPane scrollPane = new ScrollPane(content);
	    scrollPane.setFitToWidth(true);
	    scrollPane.setFitToHeight(false);
	    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
	    scrollPane.getStyleClass().add("diary-viewer-scroll");
	    scrollPane.setMaxWidth(diaryViewerContentWidth);
	    scrollPane.setMinWidth(diaryViewerContentWidth);

	    // Äußerer Wrapper — zentriert alles
	    VBox root = new VBox(12);
	    root.setAlignment(Pos.TOP_CENTER);
	    root.getStyleClass().add("diary-viewer-root");
	    root.getChildren().addAll(filterBar, scrollPane);
	    VBox.setVgrow(scrollPane, Priority.ALWAYS);

	    return new DiaryViewerComponents(root, fromPicker, toPicker, queryField, resultBox);
	}

	public VBox createDiaryCard(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags, List<String> attachments) {
	    Label dateLabel = new Label(entryDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
	    dateLabel.getStyleClass().add("diary-card-date");

	    String tagsText = tags.isEmpty() ? "" : String.join(" · ", tags);
	    Label tagsLabel = new Label(tagsText);
	    tagsLabel.getStyleClass().add("diary-card-tags");
	    tagsLabel.managedProperty().bind(tagsLabel.visibleProperty());
	    tagsLabel.setVisible(!tags.isEmpty());

	    Label textLabel = new Label(text);
	    textLabel.getStyleClass().add("diary-card-text");
	    textLabel.setWrapText(true);
	    textLabel.setMaxWidth(Double.MAX_VALUE);

	    VBox card = new VBox(6, dateLabel, tagsLabel);

	    if (!attachments.isEmpty()) {
	        int thumbHeight = Config.getInt("diary.thumbnailHeight", 120);
	        Path diaryFolder = Config.getPath("diaryAttachmentsFolder");

	        FlowPane thumbPane = new FlowPane(8, 8);
	        thumbPane.getStyleClass().add("diary-card-thumbs");

	        Tooltip tooltip = new Tooltip();
	        tooltip.setShowDelay(javafx.util.Duration.millis(300));
	        tooltip.setShowDuration(javafx.util.Duration.INDEFINITE);
	        tooltip.setHideDelay(javafx.util.Duration.ZERO);
	        tooltip.setAutoFix(false);
	        tooltip.setStyle("-fx-padding: 0;");

	        for (String relativePath : attachments) {
	            Path thumbPath    = diaryFolder.resolve("thumbnails").resolve(Path.of(relativePath).getFileName());
	            Path originalPath = diaryFolder.resolve(relativePath);

	            ImageView iv = new ImageView(new Image(thumbPath.toUri().toString(), -1, thumbHeight, true, true));

	            iv.setOnMouseEntered(e -> {
	                javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
	                double mouseX = e.getScreenX();
	                double mouseY = e.getScreenY();

	                // Bild einmal laden
	                Image original = new Image(originalPath.toUri().toString());
	                double naturalW = original.getWidth();
	                double naturalH = original.getHeight();
	                double aspectRatio = naturalW / naturalH;

	                // Schritt 1: Gewinner links/rechts
	                double leftW  = mouseX - screen.getMinX() - 2 * diaryTooltipMargin;
	                double rightW = screen.getMaxX() - mouseX - 2 * diaryTooltipMargin;
	                double hemiH  = screen.getHeight() - 2 * diaryTooltipMargin;

	                boolean useRight  = rightW >= leftW;
	                double hWinner_W  = useRight ? rightW : leftW;

	                double hLR_imgW, hLR_imgH;
	                if (naturalW <= hWinner_W && naturalH <= hemiH) {
	                    hLR_imgW = naturalW; hLR_imgH = naturalH;
	                } else if (naturalW / hWinner_W >= naturalH / hemiH) {
	                    hLR_imgW = hWinner_W; hLR_imgH = hWinner_W / aspectRatio;
	                } else {
	                    hLR_imgH = hemiH; hLR_imgW = hemiH * aspectRatio;
	                }
	                double areaLR = hLR_imgW * hLR_imgH;

	                // Schritt 2: Gewinner oben/unten
	                double topH  = mouseY - screen.getMinY() - 2 * diaryTooltipMargin;
	                double botH  = screen.getMaxY() - mouseY - 2 * diaryTooltipMargin;
	                double hemiW = screen.getWidth() - 2 * diaryTooltipMargin;

	                boolean useBottom = botH >= topH;
	                double hWinner_H  = useBottom ? botH : topH;

	                double hTB_imgW, hTB_imgH;
	                if (naturalW <= hemiW && naturalH <= hWinner_H) {
	                    hTB_imgW = naturalW; hTB_imgH = naturalH;
	                } else if (naturalW / hemiW >= naturalH / hWinner_H) {
	                    hTB_imgW = hemiW; hTB_imgH = hemiW / aspectRatio;
	                } else {
	                    hTB_imgH = hWinner_H; hTB_imgW = hWinner_H * aspectRatio;
	                }
	                double areaTB = hTB_imgW * hTB_imgH;

	                // Schritt 3: Finale Entscheidung
	                double imgW, imgH, tooltipX, tooltipY;

	                if (areaLR >= areaTB) {
	                    imgW = hLR_imgW; imgH = hLR_imgH;
	                    tooltipX = useRight ? mouseX + diaryTooltipMargin : mouseX - diaryTooltipMargin - imgW;
	                    tooltipY = mouseY - imgH / 2.0;
	                    tooltipY = Math.max(screen.getMinY() + diaryTooltipMargin, tooltipY);
	                    tooltipY = Math.min(screen.getMaxY() - diaryTooltipMargin - imgH, tooltipY);
	                } else {
	                    imgW = hTB_imgW; imgH = hTB_imgH;
	                    tooltipY = useBottom ? mouseY + diaryTooltipMargin : mouseY - diaryTooltipMargin - imgH;
	                    tooltipX = mouseX - imgW / 2.0;
	                    tooltipX = Math.max(screen.getMinX() + diaryTooltipMargin, tooltipX);
	                    tooltipX = Math.min(screen.getMaxX() - diaryTooltipMargin - imgW, tooltipX);
	                }

	                // ImageView mit Originalbild, bei Bedarf via setFitWidth/setFitHeight skaliert
	                ImageView imageView = new ImageView(original);
	                if (naturalW > imgW || naturalH > imgH) {
	                    imageView.setFitWidth(imgW);
	                    imageView.setFitHeight(imgH);
	                    imageView.setPreserveRatio(true);
	                    imageView.setSmooth(true);
	                }

	                tooltip.setGraphic(imageView);
	                tooltip.show(iv, tooltipX, tooltipY);
	            });

	            iv.setOnMouseExited(_ -> tooltip.hide());

	            thumbPane.getChildren().add(iv);
	        }
	        card.getChildren().add(thumbPane);
	    }

	    card.getChildren().add(textLabel);
	    card.getStyleClass().add("diary-card");
	    card.setMaxWidth(Double.MAX_VALUE);
	    return card;
	}
	
	public MovieViewerComponents createMovieViewer() {
	    // SWYT-Felder
	    SuggestionTextField directorField = new SuggestionTextField("Choose Director...");
	    SuggestionTextField actorField = new SuggestionTextField("Choose Actor...");
	    SuggestionTextField titleField = new SuggestionTextField("Choose Title...");
	 
	    // Labels für die Felder
	    Label dirLabel = new Label("Director:");
	    Label actLabel = new Label("Actor:");
	    Label titLabel = new Label("Title:");
	 
	    // SWYT-Felder mit Labels in einer VBox
	    double swytWidth = getContentSize().getWidth() * 0.2;
	    VBox swytPane = new VBox(font.getSize() * 0.8);
	    swytPane.getStyleClass().add("movie-viewer-swyt");
	    swytPane.setPrefWidth(swytWidth);
	    swytPane.setMinWidth(swytWidth);
	    swytPane.setMaxWidth(swytWidth);
	    swytPane.getChildren().addAll(
	            dirLabel, directorField.getTextField(),
	            actLabel, actorField.getTextField(),
	            titLabel, titleField.getTextField());
	 
	    // Ergebnisbereich
	    VBox resultBox = new VBox(font.getSize() * 0.5);
	    resultBox.getStyleClass().add("movie-viewer-results");
	 
	    // ScrollPane für Ergebnisse
	    ScrollPane scrollPane = new ScrollPane(resultBox);
	    scrollPane.setFitToWidth(true);
	    scrollPane.setFitToHeight(false);
	    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
	    scrollPane.getStyleClass().add("movie-viewer-scroll");
	 
	    // Hauptlayout: SWYT links (20%), Kacheln rechts (70%), je 5% Rand
	    HBox contentBox = new HBox(font.getSize());
	    contentBox.getStyleClass().add("movie-viewer-content");
	    HBox.setHgrow(scrollPane, Priority.ALWAYS);
	    contentBox.getChildren().addAll(swytPane, scrollPane);
	 
	    // Äußerer Wrapper mit Padding
	    VBox root = new VBox();
	    root.getStyleClass().add("movie-viewer-root");
	    root.getChildren().add(contentBox);
	    VBox.setVgrow(contentBox, Priority.ALWAYS);
	 
	    return new MovieViewerComponents(root, directorField, actorField, titleField, resultBox);
	}
	
	public Pane createCard(CardData data,
	        Consumer<String> onDirectorClicked,
	        Consumer<String> onActorClicked) {

	    boolean hasComment = data.comment() != null
	            && !data.comment().isEmpty()
	            && !".".equals(data.comment());

	    // === Poster ===
	    // Das +-Zeichen wird direkt auf das Bild gemalt (UIUtils.addPlusSign),
	    // kein separater Badge-Node nötig.
	    StackPane posterPane = null;
	    File imageFile = data.imageFilename() != null
	            ? Config.getPath("imageFolder").resolve("tmdb").resolve(data.imageFilename()).toFile()
	            : null;

	    if (imageFile != null && imageFile.exists()) {
	        Image posterImage = new Image(imageFile.toURI().toString());

	        if (hasComment) {
	            posterImage = UIUtils.addPlusSign(posterImage, moviePosterWidth);
	        }

	        ImageView posterView = new ImageView(posterImage);
	        posterView.setFitWidth(moviePosterWidth);
	        posterView.setPreserveRatio(true);
	        posterView.setSmooth(true);

	        posterPane = new StackPane(posterView);
	    }

	    // Kommentar-Popup auf dem Poster
	    if (hasComment && posterPane != null) {
	        setupCommentTooltip(posterPane, data.comment());
	    }

	    // === Rating-Zahl ===
	    Label ratingLabel = new Label(String.valueOf(data.rating()));
	    ratingLabel.getStyleClass().add("movie-card-rating");
	    Text widestRating = new Text("10");
	    widestRating.setFont(Font.font(font.getFamily(), FontWeight.BOLD, moviePosterWidth * 0.5));
	    double ratingWidth = widestRating.getLayoutBounds().getWidth();
	    ratingLabel.setPrefWidth(ratingWidth);
	    ratingLabel.setMinWidth(Region.USE_PREF_SIZE);
	    

	    // === Info-Bereich ===
	    VBox infoBox = new VBox(0);
	    infoBox.getStyleClass().add("movie-card-info");
	    HBox.setHgrow(infoBox, Priority.ALWAYS);

	    String headerString = String.join("\n", data.headerLines());
  	    Label headerLabel = new Label(headerString);
	    headerLabel.getStyleClass().add("movie-card-header");
	    infoBox.getChildren().add(headerLabel);

	    // Abstand nach Header-Block
	    javafx.scene.layout.Region headerSpacer = new javafx.scene.layout.Region();
	    headerSpacer.setPrefHeight(font.getSize());
	    infoBox.getChildren().add(headerSpacer);
	    
	    
		String detailString = String.join("\n", data.detailLines());
		if (detailString != null && !detailString.isEmpty()) {
			Label detailLabel = new Label(detailString);
			detailLabel.getStyleClass().add("movie-card-text");
			infoBox.getChildren().add(detailLabel);
		}
		
	    if (data.ratedAt() != null) {
	        Label ratedLabel = new Label("Rated: " + data.ratedAt());
	        ratedLabel.getStyleClass().add("movie-card-text");
	        infoBox.getChildren().add(ratedLabel);
	    }

	    if (!data.directors().isEmpty()) {
	        infoBox.getChildren().add(
	                createLinkedPersonLine("Director: ", data.directors(), onDirectorClicked));
	    }

	    if (!data.actors().isEmpty()) {
	        infoBox.getChildren().add(
	                createLinkedPersonLine("Stars: ", data.actors(), onActorClicked));
	    }

	    if (data.overview() != null && !data.overview().isEmpty()) {
	        javafx.scene.layout.Region overviewSpacer = new javafx.scene.layout.Region();
	        overviewSpacer.setPrefHeight(font.getSize());
	        infoBox.getChildren().add(overviewSpacer);

	        Label overviewLabel = new Label(data.overview());
	        overviewLabel.setWrapText(true);
	        overviewLabel.getStyleClass().add("movie-card-text");
	        infoBox.getChildren().add(overviewLabel);
	    }
	    
	 // TODO: Workaround für JDK-8350149 / JDK-8362873: HBox berechnet die Höhe von
	 // Kindern mit contentBias HORIZONTAL basierend auf deren prefWidth statt
	 // der tatsächlich zugewiesenen Breite. Ohne diesen Workaround wird die
	 // Kachel bei Filmen ohne Poster viel zu hoch. Kann entfernt werden,
	 // sobald der Bug in JavaFX gefixt ist.
	    infoBox.setPrefWidth(getContentSize().getWidth() * 0.7);

	    // === Kachel zusammenbauen ===
	    HBox card = new HBox(font.getSize() * 0.5);
	    card.getStyleClass().add("diary-card");
	    card.setMaxWidth(Double.MAX_VALUE);
	    if (posterPane != null) {
	        card.getChildren().add(posterPane);
	    }
	    card.getChildren().addAll(ratingLabel, infoBox);
	    ratingLabel.setMaxHeight(Double.MAX_VALUE);

	    return card;
	}
	
	// endregion

	/**
	 * Die Kartenbilder werden dann von den MapRepositories geholt
	 * 
	 * @param id
	 * @return
	 */
	public Path getMapImagePath(String mapName) {
	    String name = (String) getFieldValue(mapName + "MapImageName");
	    return name == null ? null : Config.getPath("mapImagesFolder").resolve(name);
	}

	public Path getMapInactiveImagePath(String mapName) {
	    String name = (String) getFieldValue(mapName + "MapInactiveImageName");
	    return name == null ? null : Config.getPath("mapImagesFolder").resolve(name);
	}

	public Path getMapInactiveOverlayImagePath(String mapName) {
	    String name = (String) getFieldValue(mapName + "MapInactiveOverlayImageName");
	    return name == null ? null : Config.getPath("mapImagesFolder").resolve(name);
	}

	public Path getMapOverlayImagePath(String mapName) {
	    String name = (String) getFieldValue(mapName + "MapOverlayImageName");
	    return name == null ? null : Config.getPath("mapImagesFolder").resolve(name);
	}
	
	public Image tintImageWithTextColor(Image img) {
		return UIUtils.tintImage(img, textColor);
	}

	protected void loadAllConfigs(Path configPath) {
	    try (InputStream in = Files.newInputStream(configPath)) {
	        Properties props = new Properties();
	        props.load(in);

	        // ganze Klassenhierarchie durchlaufen
	        for (Class<?> cls = this.getClass(); cls != null; cls = cls.getSuperclass()) {
	            for (Field field : cls.getDeclaredFields()) {
	                field.setAccessible(true);

	                String value = props.getProperty(field.getName());
	                if (value == null)
	                    continue;
	                else
	                    value = value.trim();

	                if (field.getType() == Color.class) {
	                    field.set(this, parseColor(value));
	                } else if (field.getType() == Font.class) {
	                    field.set(this, parseFont(value));
	                } else if (field.getType() == BorderParams.class) {
	                    field.set(this, parseBorderParams(value));
	                } else if (field.getType() == Integer.class || field.getType() == int.class) {
	                    field.set(this, Integer.parseInt(value));
	                } else if (field.getType() == Rectangle2D.class) {
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

	protected Rectangle2D parseRectangle(String value) {
		String[] values = value.split(",");
		if (values.length == 4)
			return new Rectangle2D(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3]));
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
	

	 
	/**
	 * Zeigt einen Kommentar als Popup auf dem Poster-Image.
	 * 
	 * Wir nutzen ein Popup statt eines Tooltips, weil der JavaFX-Tooltip
	 * bei setGraphic() die maxWidth des Graphic-Nodes nicht zuverlässig
	 * respektiert: Einzeilige Texte (ohne \n) werden nicht umgebrochen,
	 * mehrzeilige schon. Das ist eine Eigenheit des Tooltip-Layouts,
	 * kein dokumentierter Bug, aber reproduzierbar (getestet mit JavaFX 25).
	 * Ein Popup mit einem Label darin bricht zuverlässig um.
	 */
	private void setupCommentTooltip(Pane target, String commentText) {
	    Popup popup = new Popup();

	    target.setOnMouseEntered(e -> {
	        popup.getContent().clear();

	        double tooltipWidth = target.getScene().getWindow().getWidth() * 0.5;

	        Label content = new Label(commentText);
	        content.setWrapText(true);
	        content.setMaxWidth(tooltipWidth);
	        content.getStyleClass().add("movie-comment-popup");

	        popup.getContent().add(content);

	        // Links oder rechts vom Mauszeiger, je nachdem wo mehr Platz ist
	        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
	        double mouseX = e.getScreenX();
	        double mouseY = e.getScreenY();

	        double leftSpace = mouseX - screen.getMinX();
	        double rightSpace = screen.getMaxX() - mouseX;

	        double popupX;
	        if (rightSpace >= leftSpace) {
	            popupX = mouseX + diaryTooltipMargin;
	        } else {
	            popupX = mouseX - diaryTooltipMargin - tooltipWidth;
	        }

	        double popupY = Math.max(screen.getMinY() + diaryTooltipMargin, mouseY);

	        popup.show(target, popupX, popupY);
	    });

	    target.setOnMouseExited(_ -> popup.hide());
	}

	/**
	 * Baut eine Zeile mit Prefix ("Director: " / "Stars: ") und klickbaren Namen. Die Namen werden als einzelne Labels in einer FlowPane platziert, damit sie
	 * bei Bedarf umbrechen.
	 */
	private Pane createLinkedPersonLine(String prefix, List<String> names, Consumer<String> onClick) {
		FlowPane flow = new FlowPane();
		flow.setHgap(0);
		flow.setVgap(2);
		flow.getStyleClass().add("movie-card-person-line");

		Label prefixLabel = new Label(prefix);
		prefixLabel.getStyleClass().add("movie-card-text");
		flow.getChildren().add(prefixLabel);

		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			Label link = new Label(name);
			link.getStyleClass().add("movie-card-link");
			link.setOnMouseClicked(_ -> onClick.accept(name));
			flow.getChildren().add(link);

			if (i < names.size() - 1) {
				Label comma = new Label(", ");
				comma.getStyleClass().add("movie-card-text");
				flow.getChildren().add(comma);
			}
		}

		return flow;
	}

}

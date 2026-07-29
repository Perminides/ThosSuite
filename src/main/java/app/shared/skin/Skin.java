package app.shared.skin;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.shared.Config;
import app.shared.UiUtils;
import app.shared.model.BorderParams;
import app.shared.model.MapImages;
import app.shared.model.McMetrics;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

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
public abstract class Skin extends SkinProperties {

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
	
	
	

	
	// ========== CSS ==========
	// region
	
	public void styleScene(Scene scene) {
		menuBarHoverBackground = menuBarHoverBackground == null ? UiUtils.adjustBrightness(menuBarBackground, 20) : menuBarHoverBackground;
		menuDisabledForeground = menuDisabledForeground == null ? UiUtils.adjustBrightness(textColor, 90) : menuDisabledForeground;
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
	    builder.rule(".button:pressed, .date-picker .arrow-button:pressed", "-fx-background-color", UiUtils.adjustBrightness(activeComponentHoverColor, 8));
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
	    builder.rule(".box:pressed", "-fx-background-color", UiUtils.adjustBrightness(activeComponentHoverColor, 8));
	    builder.rule(".check-box:selected .mark", "-fx-background-color", textActiveComponentColor); // Die Farbe des Hakens in der Checkbox :-)
	}
	
	private void addScrollbarStyles(CssBuilder builder) {
		builder.rule(".scroll-bar .track", "-fx-background-color", UiUtils.adjustBrightness(playFieldBackground,10));
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
	
	/**
	 * !Sofort: Das Padding der Scrollpane (wo eine existiert) und das Padding der ButtonBar ergeben zusammen einen unschönen Abstand
	 * Man müsste am besten das Padding der Scrollpane, wenn es eine gibt, in einem Dialog auf unten = 0 setzen und nur da.
	 * Keine Ahnung ob das geht. Und wie das aussieht, wenn darunter keine ButtonBAr mehr mit Padding kommt.
	 * Nach kurzer Internet-Recherche geht das wohl leider nicht
	 * @param builder
	 */
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
	    .add("-fx-padding", "16.7 16.7 0 16.7") // !Sofort. Fieser Hack für das im JavaDoc beschriebene Problem.
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

	    // Abstand von Symbol und Menü zum linken Fensterrand, halbe Schriftgröße.
	    builder.rule(".my-header-leading", "-fx-padding", "0 0 0 " + (font.getSize() * 0.5) + "px");
	}
	
	/**
	 * Grundlage und Abweichung: {@code .my-info-label} trägt alles, was für jedes Info-Label gilt,
	 * die drei Kennungen ({@code #QuestionLabel}, {@code #ProgressLabel}, {@code #HistoryLabel})
	 * setzen nur noch den abweichenden Hintergrund.
	 *
	 * <p>Damit sieht ein {@code SuiteInfoLabel} auch außerhalb einer Session richtig aus — vorher
	 * kam sein ganzes Aussehen aus den drei Kennungen, es war also ohne Session nackt.</p>
	 */
	private void addSessionInfoLabelStyles(CssBuilder builder) {
	    BorderParams border = borderMediumComponent;
	    Insets insets = border.insets();
	    String padding = String.format("%dpx %dpx %dpx %dpx", (int)insets.getTop(), (int)insets.getRight(), (int)insets.getBottom(), (int)insets.getLeft());

	    builder.start(".my-info-label")
	    		.add("-fx-background-color", displayTextBgColor)
	    		.add("-fx-border-color", border.color())
	    		.add("-fx-border-width", border.width() + "px")
	    		.add("-fx-border-radius", border.arc() + "px")
	    		.add("-fx-background-insets", border.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	    		.add("-fx-background-radius", border.arc() + "px")
	    		.add("-fx-padding", padding)
	    		.end();

	    // Ein StackPane vererbt die Textfarbe NICHT automatisch an Text-Nodes. Wir müssen "Jeden
	    // javafx.scene.text.Text innerhalb des Labels" ansprechen.
	    builder.rule(".my-info-label Text", "-fx-fill", textColor);

	    // Nur noch die Abweichung. Die drei Farben fallen bereits beim Laden auf displayTextBgColor
	    // zurück, wenn sie nicht gesetzt sind — dann schreibt das hier denselben Wert nochmal, was
	    // nichts kostet und die Regel gleichförmig hält.
	    for (TextLabelType type : TextLabelType.values()) {
	        Color bg = (Color) getFieldValue("displayText" + type + "BgColor");
	        builder.rule("#" + type + "Label", "-fx-background-color", bg);
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
	    builder.rule(".my-mc-button:active:pressed", "-fx-background-color", UiUtils.adjustBrightness(activeComponentHoverColor, 8));
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
	       .add("-fx-background-color", UiUtils.toHex(textColor) + ", " + UiUtils.toHex(playFieldBackground))
	       .end();
	    
	    builder.start(".my-table-view .table-row-cell:focused")
	    	.add("-fx-background-insets", "0, 0 0 1 0")
	    	.end();
	    
	    builder.start(".my-table-view .table-row-cell")
	       .add("-fx-background-color", UiUtils.toHex(textColor) + ", " + UiUtils.toHex(UiUtils.adjustBrightness(playFieldBackground, 5)))
	       .end();
	    
	    builder.start(".my-table-view .table-row-cell:selected")
	    	.add("-fx-background-color", UiUtils.toHex(textColor) + ", " + UiUtils.toHex(menuBarHoverBackground))
	    	.add("-fx-background-insets", "0, 0 0 1 0")
	    	.end();
	    
	    builder.start(".my-table-view .column-header-background")
	    	.add("-fx-background-color", playFieldBackground)
	    	.end();
	    
	    builder.start(".my-table-view .column-header, .my-table-view .filler")
	    	.add("-fx-background-color", menuBarBackground)
	    	.add("-fx-border-color", "transparent " + UiUtils.toHex(textColor) + " " +  UiUtils.toHex(textColor) + " transparent")
	    	.end();
	    
	    builder.start(".my-table-view .column-header .label")
	    	.add("-fx-alignment", "CENTER-LEFT")
	    	.end();
	    
	    builder.start(".my-table-view .table-cell")
	       .add("-fx-border-color", "transparent " + UiUtils.toHex(textColor) + " transparent transparent")
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
	    	.add("-fx-border-color", UiUtils.toHex(textColor) + " transparent transparent transparent") // Es gibt einen Border um die ganze Beschriftung der x-Achse. Der obere Teil dieses Borders ist die x-Achse selbst. Herrje...
	    .end();
	    
	    builder.start(".chart .axis:left")
    		.add("-fx-border-color", "transparent " + UiUtils.toHex(textColor) + " transparent transparent") // Siehe oben. Hier müssen wir dann rechts setzen
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
	       .add("-fx-background-color", UiUtils.adjustBrightness(playFieldBackground, 20))
	    .end();
	    
	    builder.rule(".date-picker-popup .day-cell:hover", "-fx-background-color", UiUtils.adjustBrightness(playFieldBackground, 40));
	    
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
	    // Die Breite wird prozentual über prefWidth/maxWidth in MovieViewerScreenView gesetzt?
	    // Nein — wir nutzen CSS min/max-width nicht, weil die prozentuale Berechnung
	    // zur Laufzeit in MovieViewerScreenView stattfindet. Hier nur Spacing und Padding.
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
	 
	 // === Kommentar-Popup (nutzt Popup statt Tooltip, siehe MovieCard.setupCommentPopup) ===
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
	        return add(property, UiUtils.toHex(color));
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
	        return start(selector).add(property, UiUtils.toHex(color)).end();
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

	/** Die Maße einer Multiple-Choice-Auswahl. Ohne Schlüssel — die Auswahl holt sie sich selbst. */
	public McMetrics mcMetrics() {
	    Insets insets = borderSmallComponent.insets();
	    double borderWidth = borderSmallComponent.width();
	    double horizontalOverhead = insets.getLeft() + insets.getRight() + (borderWidth * 2);

	    return new McMetrics(font, horizontalOverhead, borderWidth, mcLineSpacingSqueezed(),
	            computeMcButtonHeight(), verticalGapMC);
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
	
	/**
	 * Das Bild zu einer Knopf-Rolle, fertig eingefärbt. Hängt am Skin und nicht am Aufrufer — der
	 * Knopf holt es sich deshalb selbst.
	 */
	public Image iconFor(IconButtonType rolle) {
		String iconName = switch (rolle) {
			case BACK -> backButtonIcon;
			case SKIP -> skipButtonIcon;
			case PLAY -> playButtonIcon;
			case CANCEL -> cancelButtonIcon;
		};

		Image image = new Image(Config.getPath("iconFolder").resolve(iconName).toUri().toString());
		if (rolle == IconButtonType.BACK)
			image = UiUtils.tintImage(image, textActiveComponentColor);

		return image;
	}




	

	
	

	

	// TODO: overlayContentBounds beschreibt, wo der Inhalt im Mini-Map-Bild sitzt — eigentlich
	// Karten-/Asset-Daten, kein Styling. Liegt nur hier, weil hartcodiert. Sobald berechenbar
	// (Overlay-Größe + prozentualer Rand), wandert das hoch zur Karte. Bis dahin: Felder mit Defaults.
	public Rectangle2D getOverlayContentBounds(String id) {
	    Rectangle2D b = (Rectangle2D) getFieldValue(id + "SessionOverlayContentBounds");
	    return b != null ? b : defaultOverlayContentBounds;
	}
	
	
	// !Sofort: Im Tagebuch mit Kalenderwochen und bei den StatisticsScreens ohne. Wieso?
	

	
	
	
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

	/** Die vier Bilder einer Karte als Bündel — sie werden nur zusammen gebraucht. */
	public MapImages mapImages(String mapName) {
		return new MapImages(getMapImagePath(mapName), getMapOverlayImagePath(mapName),
				getMapInactiveImagePath(mapName), getMapInactiveOverlayImagePath(mapName));
	}
	

	 


}

package app.shared.skin;

import java.util.HashSet;
import java.util.Set;

import app.shared.UiUtils;
import app.shared.model.BorderParams;
import app.shared.model.SketchColor;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

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
 * <p>Beispiel: {@code .my-header-bar} wird in {@code addMenuStyles()} 
 * UND {@code addDialogStyles()} gestylt.</p>
 * <p>Kommentar: "⚠️ ACHTUNG: Identische Styles auch in addDialogStyles()"</p>
 * 
 * <h3>6. Spezielle Fälle</h3>
 * <ul>
 *   <li><b>SuiteInfoLabel:</b> Wird über .my-info-label plus einen Modifikator gestylt (.question, .progress, .history),
 *       keine generische Basis-Klasse</li>
 *   <li><b>ListView:</b> Aktuell nur in ComboBox, daher in addComboBoxStyles()
 *       mit Kommentar falls später woanders gebraucht</li>
 * </ul>
 */
public abstract class Skin extends SkinProperties {
	
	/**
	 * Hängt das Stylesheet an die Scene — der einzige Aufrufpunkt der Anwendung, unverändert.
	 *
	 * <p>Das Erzeugen des CSS steckt in {@link #buildCss()}. Grund ist die {@code Scene}: Die gibt es
	 * nur mit laufendem Fenster, und damit käme niemand an das Stylesheet heran, der es bloß
	 * <em>lesen</em> will — etwa der Vergleichslauf, der prüft, ob ein Umbau die Darstellung
	 * unangetastet gelassen hat.</p>
	 */
	public void styleScene(Scene scene) {
		String rawCss = buildCss();

		// The color scheme of the default header buttons is automatically adjusted to remain easily recognizable by inspecting the Scene.fill property to gauge the brightness of the user interface. Applications should set the scene fill to a color that matches the user interface of the header bar area, even if the scene fill is not visible because it is obscured by other controls.
	    scene.setFill(menuBarBackground);

	    // Für URL maskieren (Transport)
	    String encodedCss = rawCss.replace("%", "%25").replace("#", "%23");

	    scene.getStylesheets().clear();
	    scene.getStylesheets().add("data:text/css," + encodedCss);

	    //Log.debug(this, rawCss);
	}

	/**
	 * Das fertige Stylesheet als Text — ohne Scene, ohne Wirkung auf die Oberfläche.
	 *
	 * <p>Beginnt mit dem Vorgaben-Durchlauf: Felder, die die properties-Datei offengelassen hat,
	 * bekommen hier ihren abgeleiteten Wert. Der Durchlauf füllt nur, was {@code null} ist, und ist
	 * deshalb wiederholbar — zweimal aufgerufen kommt zweimal dasselbe heraus.</p>
	 */
	public String buildCss() {
		menuBarHoverBackground = menuBarHoverBackground == null ? UiUtils.contrastingShade(menuBarBackground, 20) : menuBarHoverBackground;
		// Gedämpfter Text heißt: halb zum eigenen Hintergrund verblasst. Helligkeit allein kennt den nicht.
		menuDisabledForeground = menuDisabledForeground == null ? textColor.interpolate(menuBarBackground, 0.5) : menuDisabledForeground;
		menuButtonPadding = menuButtonPadding == null ? font.getSize() * 0.3 + "px " + font.getSize() * 0.4 + "px" : menuButtonPadding;
		menuItemPadding = menuItemPadding == null ? font.getSize() * 0.1 + "px " + font.getSize() * 0.5 + "px" : menuItemPadding;
		playFieldBackground = playFieldBackground == null ? menuBarBackground : playFieldBackground;
		borderShapeColor = borderShapeColor == null ? borderColor : borderShapeColor;
		borderSmallComponent = borderSmallComponent.withFallbackColor(borderColor);
		borderMediumComponent = borderMediumComponent.withFallbackColor(borderColor);
		borderBigComponent = borderBigComponent.withFallbackColor(borderColor);
		textActiveComponentColor = textActiveComponentColor == null ? textColor : textActiveComponentColor;
		dashBoardTileTopFontSize = dashBoardTileTopFontSize == null ? (int)font.getSize()*4 : dashBoardTileTopFontSize;
		dashBoardTileBottomFontSize = dashBoardTileBottomFontSize == null ? (int)font.getSize() : dashBoardTileBottomFontSize;
		displayTextHistoryBgColor = displayTextHistoryBgColor == null ? displayTextBgColor : displayTextHistoryBgColor;
		displayTextProgressBgColor = displayTextProgressBgColor == null ? displayTextBgColor : displayTextProgressBgColor;
		displayTextQuestionBgColor = displayTextQuestionBgColor == null ? displayTextBgColor : displayTextQuestionBgColor;
		displayTextClockBgColor = displayTextClockBgColor == null ? displayTextBgColor : displayTextClockBgColor;
		clockFont = clockFont == null ? Font.font(font.getFamily(), font.getSize() * 3) : clockFont;
		clockPausedTextColor = clockPausedTextColor == null ? textColor.interpolate(displayTextClockBgColor, 0.5) : clockPausedTextColor;
		// Die Aktiv-Farbe, Richtung Spielfeld zurückgenommen: gleiche Familie, ohne die Klick-Andeutung.
		answerSlotWaitingBgColor = answerSlotWaitingBgColor == null ? activeComponentBgColor.interpolate(playFieldBackground, 0.1) : answerSlotWaitingBgColor;
		hannoverSessionMapPanel = hannoverSessionMapPanel == null ? worldSessionMapPanel : hannoverSessionMapPanel;
		hannoverSessionQuestionPanel = hannoverSessionQuestionPanel == null ? worldSessionQuestionPanel : hannoverSessionQuestionPanel;
		hannoverSessionTextInputPanel = hannoverSessionTextInputPanel == null ? worldSessionTextInputPanel : hannoverSessionTextInputPanel;
		hannoverSessionImagePanel = hannoverSessionImagePanel == null ? worldSessionImagePanel : hannoverSessionImagePanel;
		hannoverSessionMcPanel = hannoverSessionMcPanel == null ? worldSessionMcPanel : hannoverSessionMcPanel;
		hannoverSessionProgressPanel = hannoverSessionProgressPanel == null ? worldSessionProgressPanel : hannoverSessionProgressPanel;
		hannoverSessionHistoryPanel = hannoverSessionHistoryPanel == null ? worldSessionHistoryPanel : hannoverSessionHistoryPanel;
		hannoverSessionBackButton = hannoverSessionBackButton == null ? worldSessionBackButton : hannoverSessionBackButton;
		sketchStrokeColor = sketchStrokeColor == null ? borderShapeColor : sketchStrokeColor;
		sketchMarkedColor = sketchMarkedColor == null ? markedColor : sketchMarkedColor;
		toEliminateColor = toEliminateColor == null ? disabledComponentBgColor : toEliminateColor;
		dashBoardTileBottomColor = dashBoardTileBottomColor == null ? menuBarBackground : dashBoardTileBottomColor;

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
	    addTooltipStyles(css);

	    // Komponenten mit meiner eigenen Logik:-)
	    addSessionInfoLabelStyles(css);
	    addIconButtonStyles(css);
	    addImageMapStyles(css);
	    addImagePaneStyles(css);
	    addSketchStyles(css);
	    addMultipleChoiceStyles(css);
	    addAnswerSlotStyles(css); // muss nach den MC-Regeln stehen, siehe dort
	    addShapeMapStyles(css);
	    addMyTableStyles(css);
	    addDashboardStyles(css);
	    addChartStyles(css);
	    addSuggestionBoxStyles(css);
	    addCardListStyles(css);
	    addDiaryViewerStyles(css);
	    addMovieViewerStyles(css);
	    
	    return css.build(); // Hier kommt sauberes CSS raus: ".rule { color: #fff; }"
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
	       .effect(componentShadow)
	       .end();

	    builder.rule(".button .text, .date-picker .arrow-button .text", "-fx-fill", textActiveComponentColor);
	    builder.rule(".button:hover, .date-picker .arrow-button:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".button:pressed, .date-picker .arrow-button:pressed", "-fx-background-color", UiUtils.contrastingShade(activeComponentHoverColor, 8));
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
	       .effect(componentShadow)
	    .end();
		
	    builder.rule(".box .text", "-fx-fill", textActiveComponentColor);
	    builder.rule(".box:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".box:pressed", "-fx-background-color", UiUtils.contrastingShade(activeComponentHoverColor, 8));
	    builder.rule(".check-box:selected .mark", "-fx-background-color", textActiveComponentColor); // Die Farbe des Hakens in der Checkbox :-)
	}
	
	private void addScrollbarStyles(CssBuilder builder) {
		builder.rule(".scroll-bar .track", "-fx-background-color", UiUtils.contrastingShade(playFieldBackground,10));
		builder.rule(".scroll-bar .thumb", "-fx-background-color", activeComponentBgColor);
		builder.rule(".scroll-bar .thumb:hover", "-fx-background-color", activeComponentHoverColor);
		builder.rule(".scroll-bar .increment-button, .scroll-bar .decrement-button", "-fx-background-color", menuBarBackground);
		builder.rule(".scroll-bar .increment-arrow, .scroll-bar .decrement-arrow", "-fx-background-color", textColor);
	}

	/**
	 * Dieselbe Fläche wie das Kommentar-Popup der Filmkacheln ({@code .movie-comment-popup}) — beide
	 * ziehen ihre Werte aus denselben Feldern, damit eine schwebende Info überall gleich aussieht.
	 *
	 * <p>Die Schriftfarbe steht hier bewusst nicht: Die globale {@code .text}-Regel färbt auch den
	 * Text im Popup-Fenster, denn ein Popup erbt das Stylesheet seiner Szene. Ohne Hintergrund von
	 * hier stünde diese Farbe auf JavaFX' eingebautem Grau — in jedem Skin eine andere, nie
	 * abgestimmte Paarung.</p>
	 */
	private void addTooltipStyles(CssBuilder builder) {
		BorderParams border = borderMediumComponent;
		builder.start(".tooltip")
		   .add("-fx-border-color", border.color())
		   .add("-fx-border-width", border.width() + "px")
		   .add("-fx-border-radius", border.arc() + "px")
		   .add("-fx-background-insets", border.width() + "px") // sonst lugt der Hintergrund an den runden Ecken hervor
		   .add("-fx-background-radius", border.arc() + "px")
		   .add("-fx-background-color", disabledComponentBgColor)
		   .add("-fx-padding", font.getSize() * 0.5 + "px")
		   .add("-fx-font-size", font.getSize() + "px")
		.end();
	}

	private void addComboBoxStyles(CssBuilder builder) {
	    builder.start(".combo-box-base")
	       .add("-fx-background-color", activeComponentBgColor)
	       .add("-fx-background-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-border-radius", borderSmallComponent.arc() + "px")
	       .add("-fx-background-insets", borderSmallComponent.width() + "px") // Der Hintergrund wird sonst bis zum Border gezeichnet und lugt dann an runden Ecken hervor, was man zuvorderst bei dunklen Hintergründen sieht, also in der Regel gar nicht, aber sicher ist sicher.
	       .add("-fx-border-width", borderSmallComponent.width() + "px")
	       .add("-fx-border-color", borderSmallComponent.color())
	       .effect(componentShadow)
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
	 * !MagicNumber: Das Padding der Scrollpane (wo eine existiert) und das Padding der ButtonBar ergeben zusammen einen unschönen Abstand
	 * Man müsste am besten das Padding der Scrollpane, wenn es eine gibt, in einem Dialog auf unten = 0 setzen und nur da.
	 * Keine Ahnung ob das geht. Und wie das aussieht, wenn darunter keine ButtonBAr mehr mit Padding kommt.
	 * Nach kurzer Internet-Recherche geht das wohl leider nicht. Fieser Hack unten.
	 * @param builder
	 */
	private void addDialogStyles(CssBuilder builder) {
	    // Dialog Container
	    builder.start(".dialog-pane")
	      // Ein dünner weißer Border sieht super aus auf Windows 10. Auf Windows 11 weniger, siehe Mail vom 10.02. ToDo
	      // .add("-fx-border-color", stageBorderColor) // analog der Stage
	      // .add("-fx-border-width", 1 + "px") // analog der Stage
	       .add("-fx-background-color", playFieldBackground) // Für den Bereich mit den Buttons.
	       .end();
	    
	    // Der Titel in der Dialog-Titelleiste. Nur vertikales Padding — sonst erbt er alles vom Label.
	    builder.rule(".my-dialog-title", "-fx-padding", (font.getSize() * 0.3) + "px 0 " + (font.getSize() * 0.3) + "px 0");

	    // HeaderBar in Dialogs
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
	    .add("-fx-padding", "16.7 16.7 0 16.7") // !Der fiese Hack für das im JavaDoc beschriebene Problem.
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
	       .effect(componentShadow)
	       .end();

	    // „Jetzt bist du dran": SuiteTextField.setActive holt den Fokus, der Zustand ist also schon da.
	    if (activeBorderColor != null)
	        builder.start(".text-field:focused")
	           .ring(activeBorderColor, activeBorderWidth)
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
	       .effect(componentShadow)
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
	    // !Blocked: Ok, Gemini hat mir folgenden Link geschickt, das überzeugt mich nun zu 90% dass es ein JavaFX-Problem ist: https://bugs.openjdk.org/browse/JDK-8227679
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
	 * die Modifikatoren ({@code .question}, {@code .progress}, {@code .history}, {@code .clock})
	 * setzen nur noch den abweichenden Hintergrund — die Uhr zusätzlich ihre eigene Schriftgröße.
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
	    		.effect(componentShadow)
	    		.end();

	    // Ein StackPane vererbt die Textfarbe NICHT automatisch an Text-Nodes. Wir müssen "Jeden
	    // javafx.scene.text.Text innerhalb des Labels" ansprechen.
	    builder.rule(".my-info-label Text", "-fx-fill", textColor);

	    // Nur noch die Abweichung. Die drei Farben fallen bereits beim Laden auf displayTextBgColor
	    // zurück, wenn sie nicht gesetzt sind — dann schreibt das hier denselben Wert nochmal, was
	    // nichts kostet und die Regel gleichförmig hält.
	    for (TextLabelType type : TextLabelType.values()) {
	        Color bg = (Color) getFieldValue("displayText" + type + "BgColor");
	        builder.rule(".my-info-label." + type.styleClass(), "-fx-background-color", bg);
	    }

	    // Die Uhr ist dieselbe Fläche, nur mit einer großen Zahl darin — und blass, solange sie steht.
	    builder.start(".my-info-label.clock Text")
	    		.add("-fx-font-family", "'" + clockFont.getFamily() + "'")
	    		.add("-fx-font-size", clockFont.getSize() + "px")
	    		.end();
	    builder.rule(".my-info-label.clock:paused Text", "-fx-fill", clockPausedTextColor);
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
		// Der Schatten sitzt auf der Karte selbst, nicht auf ihrem Rahmen: #borderOverlay hat bei
		// Rahmenbreite 0 keine Pixel und könnte nichts werfen. Die Pane trägt die Silhouette ihrer
		// Kinder, also den runden Viewport. Nur bei einem Skin mit Schatten — sonst stünde hier ein
		// leerer Block.
		if (componentShadow != null)
			builder.rule(".my-image-map-pane", "-fx-effect", componentShadow);

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
	    // Der Schatten sitzt auf der gefüllten Hintergrund-Ebene. Die Rahmen-Ebene darüber hat bei
	    // Rahmenbreite 0 weder Füllung noch Strich — sie wäre unsichtbar und würde nichts werfen.
	    // Ein Skin mit Schatten braucht deshalb eine deckende imageLabelBgColor.
	    builder.start(".my-image-background-layer")
	       .add("-fx-fill", imageLabelBgColor)
	       .effect(componentShadow)
	       .end();


	    builder.start(".my-image-border-layer")
	       .add("-fx-stroke", borderBigComponent.color())
	       .add("-fx-stroke-width", borderBigComponent.width() + "px")
	       .add("-fx-stroke-type", "inside")
	       .end();
	}
	

	/**
	 * Die Flächen einer Skizze.
	 *
	 * <p>Drei Zustände, und sie bauen aufeinander auf. Die Grundregel setzt nur den Strich — die
	 * Füllung bleibt auf der Voreinstellung eines {@code Path}, nämlich keine. Das ist der Zustand
	 * „noch nicht beantwortet": ein sichtbares Skelett, durch das der Bilderrahmen scheint.</p>
	 *
	 * <p>{@code :marked} legt eine Füllung darauf. Die Farbklassen füllen endgültig und nehmen den
	 * Strich <b>weg</b>: Zwei benachbarte Flächen derselben Farbe sollen am Ende verschmelzen, denn
	 * die Naht zwischen ihnen ist eine Erfindung der Strukturdatei und steht nicht auf dem Original.</p>
	 */
	private void addSketchStyles(CssBuilder builder) {
	    builder.start(".my-sketch-area")
	       .add("-fx-stroke", sketchStrokeColor)
	       .add("-fx-stroke-width", sketchStrokeWidth + "px")
	       .end();

	    builder.rule(".my-sketch-area:marked", "-fx-fill", sketchMarkedColor);

	    addSketchColorRule(builder, SketchColor.RED, sketchRed);
	    addSketchColorRule(builder, SketchColor.BLUE, sketchBlue);
	    addSketchColorRule(builder, SketchColor.LIGHT_BLUE, sketchLightBlue);
	    addSketchColorRule(builder, SketchColor.GREEN, sketchGreen);
	    addSketchColorRule(builder, SketchColor.YELLOW, sketchYellow);
	    addSketchColorRule(builder, SketchColor.ORANGE, sketchOrange);
	    addSketchColorRule(builder, SketchColor.WHITE, sketchWhite);
	    addSketchColorRule(builder, SketchColor.BLACK, sketchBlack);
	}

	/** Eine gefüllte Fläche trägt ihre Farbe und keinen Strich mehr — siehe {@link #addSketchStyles}. */
	private void addSketchColorRule(CssBuilder builder, SketchColor color, Color value) {
	    builder.start(".my-sketch-area." + color.styleClass())
	       .add("-fx-fill", value)
	       .add("-fx-stroke", "transparent")
	       .end();
	}

	/**
	 * Die Formen der Shape-Karten.
	 *
	 * <p><b>Jede Zustandsregel hier muss {@code -fx-fill} setzen — die Füllung ist die Klickfläche.</b>
	 * JavaFX entscheidet das nicht nach Sichtbarkeit, sondern danach, ob überhaupt eine Füllung
	 * gesetzt ist: {@code transparent} nimmt Klicks entgegen, ein fehlender Wert nicht. Wer eine
	 * dieser Regeln <em>entfernt</em>, macht die Form also nicht bloß unsichtbar, sondern
	 * unbedienbar; wer sie auf {@code transparent} setzt, macht sie unsichtbar und lässt sie
	 * bedienbar. Die Bild-Karte lebt genau davon — siehe {@code addImageMapStyles}: durchsichtige
	 * Formen über dem Kartenbild, die trotzdem Klicks annehmen.</p>
	 *
	 * <p>Umgekehrt hat {@code .layer-region} — die Klasse, die alle interaktiven Formen tragen —
	 * bewusst <b>keine</b> Regel. Nur so bleiben die Formen fremder Decks auf derselben Karte ohne
	 * Füllung und damit außen vor.</p>
	 */
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
	    builder.rule(".my-map-shape:inactive", "-fx-fill", toEliminateColor);

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
	    
	    // MC Buttons — Schatten und Ecken kommen von .button, das sind welche.
	    builder.rule(".my-mc-button", "-fx-padding", paddingCss);
	    // Ein antwortbarer Knopf ist die zweite Stelle, an der die Suite etwas will — er trägt deshalb
	    // denselben Rahmen wie das Eingabefeld im Fokus.
	    builder.start(".my-mc-button:active")
	       .add("-fx-background-color", activeComponentBgColor)
	       .ring(activeBorderColor, activeBorderWidth)
	       .end();

	    builder.rule(".my-mc-button:active:hover", "-fx-background-color", activeComponentHoverColor);
	    builder.rule(".my-mc-button:active:pressed", "-fx-background-color", UiUtils.contrastingShade(activeComponentHoverColor, 8));
	    // Alternative Effekte (für andere Skins):
	    //css.rule(".my-mc-button:active:pressed", "-fx-translate-y", "1px");
	    //css.rule(".my-mc-button:active:pressed", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 0)");
	    builder.rule(".my-mc-button:inactive", "-fx-background-color", disabledComponentBgColor);

	    // Ergebnis in zwei Dosierungen: ohne mcResultBorderWidth färbt sich die ganze Fläche in der
	    // Signalfarbe, mit ihm trägt ein Ring sie und die Fläche wird nur zu mcResultTintPercent
	    // dorthin gemischt. Beide Male dieselbe Farbe, nur in anderer Menge.
	    double tint = mcResultTintPercent / 100.0;
	    Color correctFill = mcResultBorderWidth > 0
	            ? activeComponentBgColor.interpolate(correctColor, tint) : correctColor;
	    Color incorrectFill = mcResultBorderWidth > 0
	            ? activeComponentBgColor.interpolate(incorrectColor, tint) : incorrectColor;

	    builder.start(".my-mc-button:correct")
	       .add("-fx-background-color", correctFill)
	       .ring(mcResultBorderWidth > 0 ? correctColor : null, mcResultBorderWidth)
	       .end();

	    builder.start(".my-mc-button:incorrect")
	       .add("-fx-background-color", incorrectFill)
	       .ring(mcResultBorderWidth > 0 ? incorrectColor : null, mcResultBorderWidth)
	       .end();

	    if (mcCorrectTextColor != null)
	        builder.rule(".my-mc-button:correct .text", "-fx-fill", mcCorrectTextColor);
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
	 * Die Antwortfelder von Fast Write. Sie tragen zusätzlich {@code .my-mc-button} und erben von dort
	 * Maße, Layout-Stufen und die Farben für richtig und aufgedeckt; hier steht nur die Abweichung.
	 *
	 * <p>Ein wartendes Feld ist im Spiel, nimmt aber keine Klicks entgegen — die Aktiv-Farbe der
	 * Bedienelemente verspricht dort also zu viel. Es trägt deshalb dieselbe Farbe, zum Spielfeld hin
	 * zurückgenommen: nah genug an der Familie des Skins, um nicht zu beißen, ruhig genug, um nicht
	 * nach Knopf auszusehen.</p>
	 *
	 * <p>Das erwartete Feld ({@code :expected}) bleibt ohne eigene Regel — das oberste nicht-grüne ist
	 * ohnehin das gesuchte.</p>
	 *
	 * <p><b>Muss nach {@code addMultipleChoiceStyles} laufen:</b> die Regel hat dieselbe Spezifität
	 * wie ihr MC-Pendant, es entscheidet also die Reihenfolge im Stylesheet.</p>
	 */
	private void addAnswerSlotStyles(CssBuilder builder) {
	    builder.start(".my-answer-slot:active")
	       .add("-fx-background-color", answerSlotWaitingBgColor)
	       .resetRing(activeBorderColor, borderSmallComponent)
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
	       .add("-fx-background-color", UiUtils.toHex(textColor) + ", " + UiUtils.toHex(UiUtils.contrastingShade(playFieldBackground, 5)))
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
	    .effect(componentShadow)
	    .end();

	    builder.start(".my-table-view:focused")
	    .add("-fx-background-color", "-fx-control-inner-background")
	    .add("-fx-background-insets", "0")
	    .add("-fx-padding", "0")
	    .end();

	    /* Das Textfeld im Editiermodus einer Zelle.
	     *
	     * Es bekommt bewusst nichts Eigenes: Ohne Polsterung, Rahmen und Insets sitzt sein Text
	     * genau dort, wo vorher der Anzeigetext saß — die Zelle bringt ihre Polsterung ja schon mit.
	     * Nichts springt beim Umschalten, und der Editor bleibt so hoch wie eine Zeile. Mit den
	     * Werten aus borderSmallComponent (rund 10 px oben und unten) wäre er rund 12 px zu hoch für
	     * die Zeile und würde abgeschnitten.
	     *
	     * Die zweite Regel nimmt den Zustands-Ring von .text-field:focused zurück — während des
	     * Editierens ist das Feld immer fokussiert, und der Ring machte den Editor wieder zu hoch.
	     * Sie muss ausgeschrieben sein: .my-table-view .text-field und .text-field:focused wiegen
	     * beide zwei Selektoren, es entschiede sonst die Reihenfolge der Aufrufe in styleScene.
	     *
	     * Zurückgenommen wird nur, was es heute gibt. Bekommt .text-field später eine weitere
	     * geometrische Eigenschaft (etwa eine Mindesthöhe), gehört sie hier ebenfalls hin.
	     *
	     * ActivityTableDialog verlässt sich darauf: Dort steht die Zeilenhöhe fest, die Zeile kann
	     * für den Editor also nicht mehr wachsen. Nachmessen mit scripts.ui.ActivityTableSizeProbe.
	     */
	    builder.start(".my-table-view .text-field")
	    .add("-fx-padding", "0")
	    .add("-fx-border-width", "0")
	    .add("-fx-background-insets", "0")
	    .end();

	    builder.start(".my-table-view .text-field:focused")
	    .add("-fx-border-width", "0")
	    .add("-fx-background-insets", "0")
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
	       .effect(componentShadow)
	       .end();

	    // === Oberer Bereich (große Zahl) ===
	    builder.start(".dashboard-tile-top")
	       .add("-fx-background-color", displayTextProgressBgColor)
	       .add("-fx-pref-height", dashBoardTileTopHeight + "px")
	       .add("-fx-background-radius", borderBigComponent.arc() + "px " + borderBigComponent.arc() +  "px 0 0") // Nur oben abgerundet
	       .end();
	    
	    // === Unterer Bereich (Beschreibung) ===
	    builder.start(".dashboard-tile-bottom")
	       .add("-fx-background-color", dashBoardTileBottomColor)
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
	
	// !Später: Naja, so richtig überprüft habe ich nicht, ob die alle nötig sind.
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
	    	.add("-fx-padding", chartRootPadding)
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
	       .add("-fx-background-color", UiUtils.contrastingShade(playFieldBackground, 20))
	    .end();
	    
	    builder.rule(".date-picker-popup .day-cell:hover", "-fx-background-color", UiUtils.contrastingShade(playFieldBackground, 40));
	    
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
	
	// !MagicNumber — gehört in eine eigene Runde über alle Magic Numbers der Suite,
	// nicht als Einzelfix hier.
	// Highlight-Background der Labels ignoriert Border-Radius der VBox — JavaFX clippt Children
	// nicht an abgerundeten Ecken, Lösung: Clip im Skin setzen wenn du mal runde Ecken im Vorschlagsfenster haben willst.
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
	
	private String padding(double oben, int right, double unten, int left) { // Kurzform wo möglich: ohne Schatten steht dort dasselbe wie zuvor, der Vergleich bleibt aussagekräftig.
		if (oben == unten && right == left)
			return oben + "px " + right + "px";
		return oben + "px " + right + "px " + unten + "px " + left + "px";
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
	        .effect(componentShadow)
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

	    ShadowSpace shadow = ShadowSpace.of(componentShadow);
	    double scrollPadding = font.getSize() * 0.5; // Muss dem Polster von .suite-card-list entsprechen, sonst stehen Filterleiste und Hinweis nicht mehr über der Kartenkante
	    String columnPadding = "0px 0px 0px " + (scrollPadding + shadow.left()) + "px"; // Links auf die Kartenkante: Polster der Liste plus Schattenplatz. Rechts null, weil die Scrollbar am äußeren Spaltenrand sitzt und das Polster sie nicht einrückt.

	    css.start(".diary-viewer-hint")
	        .add("-fx-fill", incorrectTextColor)
	        .add("-fx-font-style", "italic")
	        .add("-fx-padding", columnPadding)
	        .end();

	    css.start(".diary-viewer-filter-bar")
	    .add("-fx-spacing", scrollPadding + "px")
	    .add("-fx-padding", columnPadding)
	    .end();

	    css.start(".diary-viewer-root")
	    .add("-fx-padding", font.getSize() + "px 0px")
	    .add("-fx-spacing", font.getSize() * 0.5 + "px")
	    .end();
	}
	
	/**
	 * Die Trefferliste, wie Tagebuch und Film sie gemeinsam benutzen ({@code SuiteCardList}).
	 *
	 * <p>Der Innenabstand der Karten ist keine Kosmetik: Der Viewport einer ScrollPane clippt hart,
	 * und ohne diesen Platz schneidet er den Schatten der Karten an den Seiten ab. Gerechnet wird er
	 * als <em>Untergrenze</em> — hat der Skin keinen Schatten, bleibt es beim gewöhnlichen Abstand.</p>
	 */
	private void addCardListStyles(CssBuilder css) {
	    ShadowSpace shadow = ShadowSpace.of(componentShadow);

	    css.start(".suite-card-list")
	        .add("-fx-padding", font.getSize() * 0.5 + "px")
	        .add("-fx-background-color", "transparent")
	        .add("-fx-background", "transparent")
	        .end();

	    css.start(".suite-card-list .viewport")
	        .add("-fx-background-color", "transparent")
	        .end();

	    css.start(".suite-card-list .cards")
	        .add("-fx-spacing", Math.max(font.getSize(), shadow.bottom()) + "px")
	        .add("-fx-padding", padding(Math.max(font.getSize(), shadow.top()),
	        		shadow.right(), Math.max(font.getSize(), shadow.bottom()), shadow.left()))
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
	    // Festgenagelt, anders als die Kartenspalte: eine Seitenspalte neben etwas anderem.
	    css.start(".movie-viewer-swyt")
	        .add("-fx-padding", "0")
	        .add("-fx-spacing", padding * 0.8 + "px")
	        .end();
	 
	    css.start(".movie-viewer-swyt .label")
	        .add("-fx-fill", textColor)
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
	        .add("-fx-background-color", disabledComponentBgColor)  // !Später: DURCH ALLE PROPERTIES GEHEN. DOKUMENTATION. WAS WIRD WOFÜR GENUTZT? WORAUF ACHTEN BEI NEUEM SKIN? EINEN SKIN TESTER BAUEN. Diese 3 Zeilen hier sind ein bisschen aus der Not geboren und weil ich nicht mehr durch die ganzen Felder durchsteige.
	        .add("-fx-padding", font.getSize() * 0.5 + "px")
	        .add("-fx-font-size", font.getSize() + "px")
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
	     * Der Schatten des Skins — und nichts, wenn er keinen hat.
	     *
	     * <p>Eigene Methode statt {@code add}, weil {@code add} bei {@code null} knallt. Hier ist
	     * {@code null} aber kein Fehler, sondern die Aussage „dieser Skin wirft keine Schatten": die
	     * Zeile entfällt und das Stylesheet sieht aus wie vor der Einführung des Schattens.</p>
	     */
	    public CssBuilder effect(String shadow) {
	        return addIfSet("-fx-effect", shadow);
	    }

	    /**
	     * Wie {@code add}, nur dass {@code null} die Zeile entfallen lässt statt zu knallen.
	     *
	     * <p>Für Properties, deren Fehlen eine Aussage ist und kein Versehen — ein Skin ohne Schatten
	     * braucht auch keinen Platz für einen. {@code add} bleibt streng, damit ein vergessenes
	     * Pflichtfeld weiterhin auffliegt.</p>
	     */
	    public CssBuilder addIfSet(String property, String value) {
	        if (value == null)
	            return this;
	        return add(property, value);
	    }

	    /**
	     * Ein Ring, der einen Zustand trägt — „jetzt bist du dran" oder „so war die Antwort". Ohne
	     * Farbe entsteht keine Zeile, ein Skin ohne Ringe bleibt also unverändert.
	     *
	     * <p>Die Insets ziehen mit der Breite mit: sonst zeichnet der Hintergrund bis unter den Ring
	     * und lugt an den runden Ecken hervor. Aus demselben Grund stehen sie überall sonst auf der
	     * jeweiligen Rahmenbreite.</p>
	     */
	    public CssBuilder ring(Color color, int width) {
	        if (color == null)
	            return this;
	        return add("-fx-border-color", color)
	              .add("-fx-border-width", width + "px")
	              .add("-fx-background-insets", width + "px");
	    }

	    /**
	     * Nimmt einen Zustands-Ring wieder zurück auf den gewöhnlichen Rahmen — für Elemente, die
	     * einen Zustandsnamen mit einem Bedienelement teilen, ohne selbst eines zu sein.
	     *
	     * <p>Schreibt nichts, wenn der Skin gar keine Ringe kennt: dann gibt es auch nichts
	     * zurückzunehmen, und sein Stylesheet bleibt unverändert.</p>
	     */
	    public CssBuilder resetRing(Color activeColor, BorderParams gewoehnlich) {
	        if (activeColor == null)
	            return this;
	        return add("-fx-border-color", gewoehnlich.color())
	              .add("-fx-border-width", gewoehnlich.width() + "px")
	              .add("-fx-background-insets", gewoehnlich.width() + "px");
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
}

package app.ui.skin;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import app.config.Config;
import app.data.DeckType;
import app.data.GeoMap;
import app.data.MapService;
import app.ui.UIUtils;
import app.ui.components.CustomTextLabel;
import app.ui.components.ImageMapPane;
import app.ui.components.ImagePane;
import app.ui.components.MultipleChoicePane;
import app.ui.components.ShapeMapPane;
import app.ui.skin.params.BorderParams;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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

	// !Sofort: Wenn Du die borderColor auch in den borderParams angibst, kannst Du sie mit borderColor nicht mehr global setzen! Das muss da raus oder borderColor überschreibt das. Eins von beiden
	// !Sofort: Mal aktuelleres Design ausprobieren: Button mit runden Ecken, ohne Border und mit box-shadow. Sicher sehr interessant, aber ich fürchte das wird ein Refactoring-Alptraum, weil Du immer den Platz für den Schatten brauchst überall...
	// !Sofort: Einfacher als box-shadow wäre ein Design mit transparenten Hintergründen der Buttons und TextFields und so und ohne Border. Hehe...
	
	public abstract String getDisplayName();
	
	// !Sofort: alle Methodenaufruf von adjustBrightness werfen hier null. Wie an welcher stelle kann ich die wirklich sauber setzen?

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
	protected Color borderColor; // Karten
	protected Color thinBorderColor; // Um contentPane und unten die MenuBar

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
	protected String lk_bbWallpaperName;

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

	public MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		// !Sofort: Ich glaube das macht gar nix...
		menuBar.setStyle("-fx-background-color: " + UIUtils.toHex(menuBarBackground) + ";"); // Hintergrund rechts vom Icon, hinter den Top-Menüs
		return menuBar;
	}

	public Menu createMenu(String text) {
		return new Menu(text);
	}

	public MenuItem createMenuItem(String text) {
		return new MenuItem(text);
	}
	
	// app.ui.skin.Skin.java

	public HeaderBar createHeaderBar(Stage stage, MenuBar menuBar) {
	    HeaderBar headerBar = new HeaderBar();
	    
	    // CENTER: Title
	    Label titleLabel = new Label("Thos Suite (FX)");
	    titleLabel.getStyleClass().add("thorstens-title");
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

	    // 1. Die Zielgröße berechnen (Live-Wert)
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
	
	// !Sofort: Hier gibt es natürlich so einige Magic Numbers noch, die müssen dann leider perspektivisch raus? Oder machen wir das, wann immer ich das mal für andere Auflösungen brauche? Keine Ahnung, ob das je passiert tbh
	// In SkinService:
	public void styleScene(Scene scene) {
	    scene.setFill(menuBarBackground);
	    
	    // CSS generieren
	    String css = "";
	    //css = addCssRule(css, ".label", "-fx-opacity", "0.92"); //!Sofort Sieht nen Tick besser aus mit -Dprism.lcdtext=false als JVM-Argument
	    
	 // 1. LCD muss an sein (also KEIN -Dprism.lcdtext=false Argument!)
	 // Damit hast du die Schärfe und die "blauen Ränder".

	 // 2. Wir dicken die Schrift künstlich an:
	 // Setze die Kontur-Farbe auf die Textfarbe
	/** css = addCssRule(css, ".label .text", "-fx-stroke", UIUtils.toHex(textColor)); 

	 // Wähle eine Dicke. 
	 // 0.3px bis 0.5px entspricht meist dem Sprung von "Thin" zu "Normal" oder "Normal" zu "Medium".
	 // Experimentiere hier: 0.2px, 0.4px, 0.6px...
	 css = addCssRule(css, ".label .text", "-fx-stroke-width", "0.01px");**/
	    
	    // !Sofort Du musst hier so dringend aufräumen. Setze mehr global vielleicht(?) und überschreibe nur, wenn es sein muss z. B.
	    // !Sofort Alle custom styles müssen einen identischen präfix haben. thorsten oder besser custom... 
	    // !Sofort: Leider nein! Kein * weil: Wir müssen aufhören, * für Schriftgrößen zu benutzen. Der Stern-Selektor ist in JavaFX extrem aggressiv, weil er wirklich bis in die tiefsten Eingeweide der Komponenten (Scrollbars, Text-Nodes, Pfeile in ComboBoxen) greift und dort Eigenschaften hart setzt, die eigentlich vererbt werden sollten.
	    // !Sofort: Naja, aber vielleicht können wir statt *, der sich ja iwie komisch verhält, auch einfach root nehmen? :-)
	    // https://stackoverflow.com/questions/79307490/the-universal-selector-overrides-more-specific-selectors-in-css-for-javafx-fxm
	    css = addCssRule(css, ".menu-button", "-fx-padding", 
	    		font.getSize() * 0.3 + "px " + font.getSize() * 0.4 + "px"); // Wenn fontsize global gesetzt wird, berechnet javafx daraus paddings und die sind einfach zu groß...
	    
	    // 1. Der "klebende" Fokus wird unsichtbar mit "transparent" und Hover mit UIUtils.toHex(menuBarHoverBackground). Diesen klebenden Fokus gibt es allerdings nur beim Öffnen eines Untermnüs, nicht beim Öffnen eines Top-Menüs. Ich bin noch nicht überzeugt, dass man dieses Verhalten akzeptieren muss tbh...
	    // Ok, Gemini hat mir folgenden Link geschickt, das überzeugt mich nun zu 90% dass es ein JavaFX-Problem ist: https://bugs.openjdk.org/browse/JDK-8227679
	    // Auch wenn hier von ContextMenus gesprochen wird. Aber This is a minor annoyance, but not a serious issue. Lowering priority to P4. → Really???
	    css = addCssRule(css, ".menu-item:focused", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground));
	    
	    css = addCssRule(css, ".menu .label", "-fx-text-fill", UIUtils.toHex(textColor)); // Lernen
	    css = addCssRule(css, ".menu .label", "-fx-font-size", "" + font.getSize() + "px");  // Schriftgröße Lernen
	    css = addCssRule(css, ".menu:hover", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hover über Lernen
	    css = addCssRule(css, ".menu:showing", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hintergrund von Lernen, wenn ich über ein Untermenü hovere, wie Multiple Choice oder so.
	    css = addCssRule(css, ".menu:focused", "-fx-background-color", UIUtils.toHex(menuBarHoverBackground)); // Hintergrund von Anzeigereihenfolge, wenn ich über ein Untermenü hovere, wie Zufällig oder so.
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
	    
	    css = addCssRule(css, ".thorstens-title", "-fx-font-family", "'" + font.getFamily() + "'");
	    css = addCssRule(css, ".thorstens-title", "-fx-font-size", font.getSize() + "px");  // Vom Skin-Font!
	    css = addCssRule(css, ".thorstens-title", "-fx-text-fill", UIUtils.toHex(textColor));
	    
	    css = addCssRule(css, ".thorstens-root", "-fx-border-color", "white");
	    css = addCssRule(css, ".thorstens-root", "-fx-border-width", "1px");
	    
	 // Generierung der Rules für CustomTextLabels
        for (TextLabelType type : TextLabelType.values()) {
            String selector = "#" + type.toString() + "Label"; // z.B. #QuestionLabel
            
            String fieldName = "displayText" + type.toString() + "BgColor";
            Color bg = (Color) getFieldValue(fieldName);
            if (bg == null) bg = displayTextBgColor;
            
            BorderParams border = borderMediumComponent;
            java.awt.Insets insets = border.insets();
            
            // 1. Container Styles (StackPane)
            // Background, Border, Padding etc. gehören auf den Container
            css = addCssRule(css, selector, "-fx-background-color", UIUtils.toHex(bg));
            
            // Font wird vererbt, kann also auch auf den Container
            css = addCssRule(css, selector, "-fx-font-family", "'" + font.getFamily() + "'");
            css = addCssRule(css, selector, "-fx-font-size", font.getSize() + "px");
            
            css = addCssRule(css, selector, "-fx-border-color", UIUtils.toHex(border.color()));
            css = addCssRule(css, selector, "-fx-border-width", border.width() + "px");
            css = addCssRule(css, selector, "-fx-border-radius", border.arc() + "px");
            css = addCssRule(css, selector, "-fx-background-radius", border.arc() + "px");
            
            String padding = String.format("%dpx %dpx %dpx %dpx", 
                insets.top, insets.right, insets.bottom, insets.left);
            css = addCssRule(css, selector, "-fx-padding", padding);
            
            // 2. Text Styles (Text-Nodes) - NEU!
            // Ein StackPane vererbt die Textfarbe NICHT automatisch an Text-Nodes.
            // Wir müssen "Jeder Text innerhalb von selector" ansprechen.
            css = addCssRule(css, selector + " Text", "-fx-fill", UIUtils.toHex(textColor));
        }
        
     // --- ImageMap Shapes: Basis ---

     // First Path (wird zuerst gezeichnet, unten)
     css = addCssRule(css, ".first", "-fx-fill", "transparent");
     css = addCssRule(css, ".first", "-fx-stroke", "transparent");
     css = addCssRule(css, ".first", "-fx-mouse-transparent", "true");
     css = addCssRule(css, ".first", "-fx-stroke-line-cap", "round");
     css = addCssRule(css, ".first", "-fx-stroke-line-join", "round");

     // Second Path (wird danach gezeichnet, oben)
     css = addCssRule(css, ".second", "-fx-fill", "transparent");
     css = addCssRule(css, ".second", "-fx-stroke", "transparent");
     css = addCssRule(css, ".second", "-fx-stroke-width", 2 + "px");
     css = addCssRule(css, ".second", "-fx-stroke-line-cap", "round");
     css = addCssRule(css, ".second", "-fx-stroke-line-join", "round");

     // Rivers: Andere Basis-Breiten
     css = addCssRule(css, ".river .first", "-fx-stroke-width", "11px");
     css = addCssRule(css, ".river .second", "-fx-stroke-width", "7px");

     // --- CORRECT State ---
     // First = Fill (grün), Second = Border (schwarz)
     css = addCssRule(css, ".image-map-shape:correct .first", "-fx-fill", UIUtils.toHex(correctColor));
     css = addCssRule(css, ".image-map-shape:correct .second", "-fx-stroke", UIUtils.toHex(borderColor));
     // Rivers bei correct: Erst dicken Border malen und dann kleiner darein korrekt malen
     css = addCssRule(css, ".image-map-shape:correct.river .first", "-fx-stroke", UIUtils.toHex(borderColor));
     css = addCssRule(css, ".image-map-shape:correct.river .first", "-fx-stroke", UIUtils.toHex(correctColor));
     
     // --- INCORRECT State ---
     // First = Fill (rot), Second = Border (schwarz)
     css = addCssRule(css, ".image-map-shape:incorrect .first", "-fx-fill", UIUtils.toHex(incorrectColor));
     css = addCssRule(css, ".image-map-shape:incorrect .second", "-fx-stroke", UIUtils.toHex(borderColor));
     

     // --- MARKED State ---
     // First = Border (schwarz dick), Second = Fill (gelb dünner)
     css = addCssRule(css, ".image-map-shape:marked .first", "-fx-fill", "transparent");
     css = addCssRule(css, ".image-map-shape:marked .first", "-fx-stroke", UIUtils.toHex(borderColor));
     css = addCssRule(css, ".image-map-shape:marked .first", "-fx-stroke-width", (7) + "px"); // ~9px
     css = addCssRule(css, ".image-map-shape:marked .second", "-fx-fill", "transparent");
     css = addCssRule(css, ".image-map-shape:marked .second", "-fx-stroke", UIUtils.toHex(markedColor));
     css = addCssRule(css, ".image-map-shape:marked .second", "-fx-stroke-width", (4) + "px"); // ~7.2px

     // Rivers bei marked: Siehe correct
     css = addCssRule(css, ".image-map-shape:marked.river .first", "-fx-stroke-width", "11px");
     css = addCssRule(css, ".image-map-shape:marked.river .second", "-fx-stroke-width", "7px");
     css = addCssRule(css, ".image-map-shape:marked.river .first", "-fx-stroke", UIUtils.toHex(borderColor));
     css = addCssRule(css, ".image-map-shape:marked.river .second", "-fx-stroke", UIUtils.toHex(markedColor));
	    
	 // --- ShapeMap Styles ---
	    
	    // Basis-Styling für alle Shapes (Standard: Inaktiv/Grau)
	    // Wir nutzen hier disabledComponentBgColor, das entspricht deinem bisherigen "inactiveColor"
	    //css = addCssRule(css, ".map-shape", "-fx-fill", UIUtils.toHex(disabledComponentBgColor));
	    css = addCssRule(css, ".map-shape", "-fx-stroke", UIUtils.toHex(borderColor));
	    css = addCssRule(css, ".map-shape", "-fx-stroke-width", "1.8px"); // Dein Wert aus dem alten Panel
	    
	 // Anstatt globalem Hover definieren wir Hover spezifisch für den Spiel-Zustand!
	    
	    // Zuerst den normalen Active-State
	    css = addCssRule(css, ".map-shape:active-game", "-fx-fill", UIUtils.toHex(activeComponentBgColor));
	    
	    // DANN den Hover für Active-State (gewinnt durch Reihenfolge UND Spezifität)
	    // Das entspricht deiner Swing-Logik: "Nur wenn aktiv, dann Hover-Effekt"
	    css = addCssRule(css, ".map-shape:active-game:hover", "-fx-fill", UIUtils.toHex(activeComponentHoverColor));

	    // --- DER 3D HOVER EFFEKT ---
	    css = addCssRule(css, ".map-shape:active-game:hover", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 0)");
	    //css = addCssRule(css, ".map-shape:active-game:hover", "-fx-effect", "innershadow(one-pass-box, rgba(0,0,0,0.6), 4, 1.0, 3, 3)");
	    //css = addCssRule(css, ".map-shape:active-game:hover", "-fx-effect", "bloom(0.1)");
	    //css = addCssRule(css, ".map-shape:active-game:hover", "-fx-effect", "lighting(light(distant, -45, 45, white), 5.0, 1.5, 20, bump-input)");
	    //css = addCssRule(css, ".map-shape:active-game:hover", "-fx-effect", "reflection(top-offset 0, fraction 0.7, top-opacity 0.5, bottom-opacity 0.0)");
	    
	    // Andere Zustände (Correct/Incorrect)
	    // Da diese Shapes NICHT :active-game sind (die States sind exklusiv),
	    // greift der Hover von oben hier nicht. Perfekt!
	    css = addCssRule(css, ".map-shape:correct", "-fx-fill", UIUtils.toHex(correctColor));
	    css = addCssRule(css, ".map-shape:incorrect", "-fx-fill", UIUtils.toHex(incorrectColor));
	    
	    // :marked = Markiert (z.B. bei Elimination) -> markedColor
	    css = addCssRule(css, ".map-shape:marked", "-fx-fill", UIUtils.toHex(markedColor));
	    
	 // --- PAUSE LOGIK ---
	    // Wenn das Spiel pausiert ist (.game-paused auf dem Parent),
	    // sollen aktive Shapes (.map-shape:active-game) aussehen wie inaktive (disabledComponentBgColor).
	    // Wichtig: Correct/Incorrect Shapes bleiben unberührt, da sie den Status :active-game nicht haben!
	    String pausedColor = UIUtils.toHex(disabledComponentBgColor);
	    
	    // Farbe überschreiben
	    css = addCssRule(css, ".game-paused .map-shape:active-game", "-fx-fill", pausedColor);
	    
	    // Hover-Effekt im Pause-Modus unterdrücken (sonst würden sie beim Drüberfahren wieder bunt/leuchtend)
	    css = addCssRule(css, ".game-paused .map-shape:active-game:hover", "-fx-fill", pausedColor);
	    css = addCssRule(css, ".game-paused .map-shape:active-game:hover", "-fx-effect", "null"); // 3D Effekt aus
	    //css = addCssRule(css, ".game-paused .map-shape:active-game:hover", "-fx-cursor", "default"); // Hand aus

	    // Dekorationen (Kontext-Shapes wie Meer oder Nachbarländer)
	    // Basis-Regel für Deko (keine Interaktion)
	    css = addCssRule(css, ".decoration", "-fx-mouse-transparent", "true"); // Klicks gehen durch
	    
	    // Spezifische Farben für Deko-Sets
	    if (shapeMapColor0 != null) {
	        css = addCssRule(css, ".decoration-0", "-fx-fill", UIUtils.toHex(shapeMapColor0));
	    }
	    if (shapeMapColor1 != null) {
	        css = addCssRule(css, ".decoration-1", "-fx-fill", UIUtils.toHex(shapeMapColor1));
	    }
	    // Bundesländer z. B.
	    // DEBUG NUR!
	    /**css = addCssRule(css, ".map-shape.decoration.decoration-2", "-fx-fill", "transparent");
	    //css = addCssRule(css, ".map-shape.decoration-2", "-fx-fill", "transparent"); // Ohne "map-shape" würde hier sonst .map-shape:active-game gewinnen!
	    css = addCssRule(css, ".decoration-2", "-fx-stroke", UIUtils.toHex(borderColor));
		css = addCssRule(css, ".decoration-2", "-fx-stroke-width", "2.8px"); // !Sofort Magic Number**/
		css = addCssRule(css, ".map-shape.decoration-2", "-fx-fill", "transparent");
		css = addCssRule(css, ".map-shape.decoration-2", "-fx-stroke", UIUtils.toHex(borderColor));
		css = addCssRule(css, ".map-shape.decoration-2", "-fx-stroke-width", "2.8px");

	    // Optional: Hover-Effekt bei Deko ausschalten (falls nötig, aber mouse-transparent regelt das eh)
	    css = addCssRule(css, ".decoration:hover", "-fx-fill", "derive(-fx-fill, 0%)"); // Farbe beibehalten
	    
	 // --- MC Button Styling ---
	    
	    // Padding dynamisch aus BorderParams
	    java.awt.Insets mcInsets = borderSmallComponent.insets();
	    String paddingCss = String.format("%dpx %dpx %dpx %dpx", 
	        mcInsets.top, mcInsets.right, mcInsets.bottom, mcInsets.left);
	    css = addCssRule(css, ".mc-button", "-fx-padding", paddingCss);
	    
	    // Basis-Rahmen und Radius
	    css = addCssRule(css, ".button", "-fx-font-family", "'" + font.getFamily() + "'");
	    css = addCssRule(css, ".button", "-fx-font-size", font.getSize() + "px");
	    css = addCssRule(css, ".button", "-fx-text-fill", UIUtils.toHex(textColor));
	    css = addCssRule(css, ".button", "-fx-background-radius", borderSmallComponent.arc() + "px");
	    css = addCssRule(css, ".button", "-fx-background-insets", "0");
	    css = addCssRule(css, ".button", "-fx-border-radius", borderSmallComponent.arc() + "px");
	    css = addCssRule(css, ".button", "-fx-border-width", borderSmallComponent.width() + "px");
	    css = addCssRule(css, ".button", "-fx-border-color", UIUtils.toHex(borderSmallComponent.color()));
	    css = addCssRule(css, ".button", "-fx-text-fill", UIUtils.toHex(textActiveComponentColor == null ? textColor : textActiveComponentColor));

	    // MC Buttons
	    css = addCssRule(css, ".mc-button:active", "-fx-background-color", UIUtils.toHex(activeComponentBgColor));
	    css = addCssRule(css, ".mc-button:active:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    //css = addCssRule(css, ".mc-button:active:pressed", "-fx-translate-y", "1px");
	    //css = addCssRule(css, ".mc-button:active:pressed", "-fx-effect", "innershadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 0)");
	    css = addCssRule(css, ".mc-button:active:pressed", "-fx-background-color", UIUtils.toHex(adjustBrightness(activeComponentHoverColor, 8)));
	    css = addCssRule(css, ".mc-button:inactive", "-fx-background-color", UIUtils.toHex(disabledComponentBgColor));
	    if (mcIncorrectTextColor != null) {
	    	css = addCssRule(css, ".mc-button:correct", "-fx-text-fill", UIUtils.toHex(mcCorrectTextColor));
	    	css = addCssRule(css, ".mc-button:incorrect", "-fx-text-fill", UIUtils.toHex(mcIncorrectTextColor));
	    }
	    css = addCssRule(css, ".mc-button:correct", "-fx-background-color", UIUtils.toHex(correctColor));
	    css = addCssRule(css, ".mc-button:incorrect", "-fx-background-color", UIUtils.toHex(incorrectColor));
	    
	    // --- MC Button Layout Varianten (Pseudo-Klassen) ---
	    
	    // Padding für mehrzeilige Buttons berechnen (nur 1px oben/unten, damit 2 Zeilen passen)
	    // Horizontal lassen wir das normale Padding (insets.right/left), damit es optisch gleich aussieht
	    java.awt.Insets i = borderSmallComponent.insets();
	    String densePadding = String.format("1px %dpx 1px %dpx", i.right, i.left);
	 // Berechnung des Spacings basierend auf der Font-Größe (ca. -40% der Größe sorgt für enges Layout)
	    double lineSpacingSqueezed = font.getSize() * -0.4;
	    double lineSpacingTiny = smallFont.getSize() * -0.4;

	 // ZWISCHENSTUFE: Squeezed (Normaler Font, aber extrem kompakt)
	    String squeezedPadding = String.format("0px %dpx 0px %dpx", i.right, i.left);
	    css = addCssRule(css, ".mc-button:squeezed", "-fx-wrap-text", "true");
	    css = addCssRule(css, ".mc-button:squeezed", "-fx-padding", squeezedPadding);
	    
	    // NEU: Berechneter Wert statt harter "-6px"
	    css = addCssRule(css, ".mc-button:squeezed", "-fx-line-spacing", lineSpacingSqueezed + "px"); 
	    css = addCssRule(css, ".mc-button:squeezed", "-fx-text-alignment", "center");
	    
	    // VARIANTE B: Tiny (Kleiner Font, enges Padding & Umbruch)
	    css = addCssRule(css, ".mc-button:tiny", "-fx-wrap-text", "true");
	    css = addCssRule(css, ".mc-button:tiny", "-fx-padding", squeezedPadding);
	    
	    // NEU: Berechneter Wert statt harter "-6px"
	    css = addCssRule(css, ".mc-button:tiny", "-fx-line-spacing", lineSpacingTiny + "px"); 
	    css = addCssRule(css, ".mc-button:tiny", "-fx-font-size", smallFont.getSize() + "px");
	    
	 // --- Image Styling (Rectangle Version) ---
	    
	    // 1. Hintergrund-Layer (Das untere Rectangle)
	    // Hier definieren wir Füllfarbe (Gold) und den Rahmen.
	    // Da es ein Rectangle ist, nutzen wir wieder -fx-fill und -fx-stroke!
	    
	    // Farbe
	    css = addCssRule(css, ".image-background-layer", "-fx-fill", UIUtils.toHex(imageLabelBgColor));
	    
	    // Rahmen (Stroke)
	    // Da das backgroundRect unten liegt, sieht man den Stroke evtl. nur zur Hälfte,
	    // aber bei StrokeType.INSIDE (Default ist Centered) ist das wichtig.
	    // Besser: Wir geben dem backgroundRect auch den Rahmen.
	    css = addCssRule(css, ".image-border-layer", "-fx-stroke", UIUtils.toHex(borderBigComponent.color()));
	    css = addCssRule(css, ".image-border-layer", "-fx-stroke-width", borderBigComponent.width() + "px");
	    css = addCssRule(css, ".image-border-layer", "-fx-stroke-type", "inside"); // WICHTIG für saubere Kanten!
	    
	 // Icon-Button Styling
	    css = addCssRule(css, ".icon-button", "-fx-padding", "0");
	    css = addCssRule(css, ".icon-button", "-fx-background-insets", "0");
	    css = addCssRule(css, ".icon-button", "-fx-background-color", UIUtils.toHex(activeComponentBgColor));
	    css = addCssRule(css, ".icon-button", "-fx-border-color", UIUtils.toHex(borderSmallComponent.color()));
	    css = addCssRule(css, ".icon-button", "-fx-border-width", borderSmallComponent.width() + "px");
	    css = addCssRule(css, ".icon-button", "-fx-border-radius", borderSmallComponent.arc() + "px");
	    css = addCssRule(css, ".icon-button", "-fx-background-radius", borderSmallComponent.arc() + "px");
	    // Hover
	    css = addCssRule(css, ".icon-button:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    // Disabled (falls nötig)
	    css = addCssRule(css, ".icon-button:disabled", "-fx-opacity", "1.0");
	    css = addCssRule(css, ".icon-button:disabled", "-fx-background-color", UIUtils.toHex(disabledComponentBgColor));

	 // --- TextField Styling ---;
	    // Basis
	    css = addCssRule(css, ".input-field", "-fx-font-family", "'" + font.getFamily() + "'");
	    css = addCssRule(css, ".input-field", "-fx-font-size", font.getSize() + "px");
	    css = addCssRule(css, ".input-field", "-fx-text-fill", UIUtils.toHex(textActiveComponentColor == null ? textColor : textActiveComponentColor));
	    css = addCssRule(css, ".input-field", "-fx-alignment", "center");
	    // Padding
	    paddingCss = String.format("%dpx %dpx %dpx %dpx", 
	        borderSmallComponent.insets().top, borderSmallComponent.insets().right, borderSmallComponent.insets().bottom, borderSmallComponent.insets().left);
	    css = addCssRule(css, ".input-field", "-fx-padding", paddingCss);
	    // Background & Border (Active)
	    css = addCssRule(css, ".input-field", "-fx-background-color", UIUtils.toHex(activeComponentBgColor));
	    css = addCssRule(css, ".input-field", "-fx-background-radius", borderSmallComponent.arc() + "px");
	    css = addCssRule(css, ".input-field", "-fx-border-color", UIUtils.toHex(borderSmallComponent.color()));
	    css = addCssRule(css, ".input-field", "-fx-border-width", borderSmallComponent.width() + "px");
	    css = addCssRule(css, ".input-field", "-fx-border-radius", borderSmallComponent.arc() + "px");
	    // Disabled State
	    css = addCssRule(css, ".input-field:disabled", "-fx-opacity", "1.0");
	    css = addCssRule(css, ".input-field:disabled", "-fx-background-color", UIUtils.toHex(disabledComponentBgColor));
	    css = addCssRule(css, ".input-field:disabled", "-fx-border-color", UIUtils.toHex(borderSmallComponent.disabledColor()));
	    css = addCssRule(css, ".input-field:disabled", "-fx-text-fill", UIUtils.toHex(incorrectTextColor));
	    css = addCssRule(css, ".input-field:disabled", "-fx-font-weight", "bold");
	    
	    // Alert und Dialog
	    css = addCssRule(css, ".dialog-pane", "-fx-background-color", UIUtils.toHex(menuBarBackground));
	    css = addCssRule(css, ".dialog-pane", "-fx-border-color", UIUtils.toHex(thinBorderColor));
	    css = addCssRule(css, ".dialog-pane", "-fx-border-width", borderSmallComponent.width() + "px");
	    css = addCssRule(css, ".dialog-pane", "-fx-font-family", "'" + font.getFamily() + "'");
	    css = addCssRule(css, ".dialog-pane", "-fx-font-size", font.getSize() + "px");
	    css = addCssRule(css, ".dialog-pane", "-fx-text-fill", UIUtils.toHex(textColor));
	    css = addCssRule(css, ".dialog-pane .label", "-fx-text-fill", UIUtils.toHex(textColor)); // Auf dialog-pane reicht nicht für die Schriftfarbe in der Pane....
	    css = addCssRule(css, ".dialog-pane .button:focused", "-fx-background-color", UIUtils.toHex(activeComponentBgColor));
	    css = addCssRule(css, ".dialog-pane .button:hover", "-fx-background-color", UIUtils.toHex(activeComponentHoverColor));
	    css = addCssRule(css, ".dialog-pane .button:pressed", "-fx-background-color", UIUtils.toHex(adjustBrightness(activeComponentHoverColor, 8)));
	    
	    // Wir maskieren kritische Zeichen für die Data-URI
	    String encodedCss = css.replace("%", "%25")  // % muss zu %25 werden
	                           .replace("#", "%23"); // # muss zu %23 werden (sicher ist sicher)
	    scene.getStylesheets().clear();
	    
	    scene.getStylesheets().add("data:text/css," + encodedCss);
	    System.out.println(css.replaceAll("}", "}\n"));
	}

	public Pane createBackgroundPane(DeckType type) {
	    Pane pane = new Pane();
	    
	    // Größe setzen
	    Dimension size = getContentSize();
	    pane.setPrefSize(size.width, size.height);
	    pane.setMinSize(size.width, size.height);
	    pane.setMaxSize(size.width, size.height);
	    
	    // Hintergrundbild setzen
	    String bgPath = getBackgroundImagePath(type);
	    try {
	        Image bgImage = new Image(new File(bgPath).toURI().toString());
	        BackgroundImage background = new BackgroundImage(
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
	        pane.setBackground(new Background(background));
	    } catch (Exception e) {
	        throw new RuntimeException("Konnte Hintergrundbild nicht laden: " + bgPath, e);
	    }
	    
	    return pane;
	}

	public TextField createInputField(DeckType type) {
	    Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionTextInputPanel");
	    if (bounds == null)
			bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "SessionTextInputPanel");
	    
	    TextField textField = new TextField();
	    textField.getStyleClass().add("input-field");
	    textField.setLayoutX(bounds.x);
	    textField.setLayoutY(bounds.y);
	    textField.setPrefWidth(bounds.width);
	    // Höhe wird von Font + Padding bestimmt
	    
	    return textField;
	}

	// Rückgabetyp angepasst
	public ImagePane createImageComponent(DeckType type) {
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

	    // 3. Styling ist jetzt in der Klasse via "image-background-layer" vorbereitet.
	    // Wir müssen nur sicherstellen, dass die CSS-Regeln stimmen.

	    return pane;
	}
	
	// --- Bereinigte Factory-Methode ---
	public CustomTextLabel createCustomTextLabel(DeckType type, TextLabelType labelType) {
        Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "Session" + labelType + "Panel");
        if (bounds == null)
            bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "Session" + labelType + "Panel");
        
        CustomTextLabel label = new CustomTextLabel("");
        label.setLayoutX(bounds.x);
        label.setLayoutY(bounds.y);
        
        // StackPane braucht Breite & Höhe für Layout/Zentrierung
        label.setFixedWidth(bounds.width); 
        label.setFixedHeight(bounds.height); 
        
        // ID setzen, damit das CSS oben greift
        label.setId(labelType.toString() + "Label");
        
        return label;
    }

	/**public Label createCustomTextLabel(DeckType type, TextLabelType labelType) {
	    // 1. Bounds & Colors
	    Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "Session" + labelType + "Panel");
	    if (bounds == null)
	        bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "Session" + labelType + "Panel");
	    
	    Color bg = (Color) getFieldValue("displayText" + labelType + "BgColor");
	    bg = bg == null ? displayTextBgColor : bg;
	    
	    // 2. BorderParams (enthalten bereits JavaFX Color und AWT Insets)
	    BorderParams border = borderMediumComponent;
	    
	    // 3. Label konfigurieren
	    Label label = new Label();
	    label.setLayoutX(bounds.x);
	    label.setLayoutY(bounds.y);
	    label.setPrefWidth(bounds.width);
	    label.setPrefHeight(bounds.height);
	    
	    label.setWrapText(true);
	    // Linksbündig, aber vertikal zentriert (entspricht meistens dem Swing-Default für TextAreas/Labels mit Border)
	    label.setAlignment(Pos.CENTER_LEFT); 
	    
	    // 4. Insets aus BorderParams holen
	    // Hinweis: BorderParams nutzt java.awt.Insets (top, left, bottom, right)
	    java.awt.Insets insets = border.insets();

	    // 5. CSS bauen
	    String style = String.format(
	            "-fx-background-color: %s;" +
	            "-fx-text-fill: %s;" +
	            
	            // HIER: Hart 'Aptos Medium' erzwingen.
	            // Falls das nicht greift, probier auch mal "Aptos SemiBold" oder "Aptos Display Medium"
	            "-fx-font-family: '%s';" + //"-fx-font-family: 'Aptos SemiBold';"
	            
	            // Optional zur Sicherheit (falls er die Family nicht findet, fällt er auf Regular zurück und wir versuchen es nochmal via CSS)
	            //"-fx-font-weight: 700;" + 
	            
	            "-fx-font-size: %fpx;" +
	            "-fx-border-color: %s;" +
	            "-fx-border-width: %dpx;" +
	            "-fx-border-radius: %dpx;" +
	            "-fx-background-radius: %dpx;" +
	            "-fx-padding: %dpx %dpx %dpx %dpx;", 
	            
	            UIUtils.toHex(bg),
	            UIUtils.toHex(textColor),
	            font.getFamily(),
	            font.getSize(),
	            UIUtils.toHex(border.color()), 
	            border.width(),
	            border.arc(),
	            border.arc(),
	            insets.top, insets.right, insets.bottom, insets.left
	        );
	    
	    System.out.println("Arc: " + border.arc());
	    
	    label.setStyle(style);
	    label.setId(labelType.toString() + "Label");
	    
	    return label;
	}**/

	public MultipleChoicePane createMultipleChoicePane(DeckType type) {
        // 1. Bounds holen
        Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMcPanel");
        if (bounds == null) 
            bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "SessionMcPanel");
        
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
        System.out.println("Höhe der Buttons gesetzt auf: " + fixedButtonHeight);

        // NEU: Berechnung des Spacings (identisch zur Logik in styleScene!)
        double lineSpacingSqueezed = font.getSize() * -0.4;
        double lineSpacingTiny = smallFont.getSize() * -0.4;

        // 5. Pane erstellen mit neuen Parametern0
        MultipleChoicePane result = new MultipleChoicePane(
            bounds.width, 
            fixedButtonHeight, 
            horizontalOverhead,
            borderWidth,
            font, 
            smallFont, 
            verticalGapMC,
            lineSpacingSqueezed, // NEU übergeben
            lineSpacingTiny      // NEU übergeben
        );
        
        result.setLayoutX(bounds.x);
        result.setLayoutY(bounds.y);
        
        return result;
    }
	
	public Button createIconButton(DeckType type, IconButtonType buttonType) {
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
	    button.getStyleClass().add("icon-button");
	    
	    // Bounds holen
	    Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionBackButton");
	    button.setPrefSize(bounds.width, bounds.height);
	    button.setLayoutX(bounds.x);
	    button.setLayoutY(bounds.y);
	    
	    return button;
	}
	
	public Alert createAlert (Window parent, String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.initOwner(parent);
		
		DialogPane dialogPane = alert.getDialogPane();
		Scene dialogScene = dialogPane.getScene();
		styleScene(dialogScene);
		
		alert.setGraphic(null);
		alert.initStyle(StageStyle.UNDECORATED);
		return alert;
	}

	/**
	 * Erstellt ein Deutschlandpanel. Aktuell noch ohne Hintergrundbild. Die Größe berechnet sich aus: Höhe = height + 2 * yPadding. Der scale findet ohne
	 * Verzerrung statt, von daher ist die width dann auch aus diesen Parametern und der Auflösung der Map bestimmt.
	 */
	public ShapeMapPane createShapeMapPane(DeckType type) {
        // 1. Daten holen
        GeoMap map = MapService.getInstance().getMap(type);
        
        // 2. Bounds via Reflection holen (AWT Rectangle)
        Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMapPanel");
        if (bounds == null) 
            bounds = (Rectangle) getFieldValue(type.getCategory().toString() + "SessionMapPanel");
            
        // 3. Komponente erstellen (Skin bestimmt die Ziel-Höhe für den Zoom!)
        ShapeMapPane pane = new ShapeMapPane(map, bounds.height);
        
        // 4. Positionieren (Absolut)
        pane.setLayoutX(bounds.x);
        pane.setLayoutY(bounds.y);
        
        return pane;
    }

	public ImageMapPane createImageMapPanel(DeckType type) {
		GeoMap map = MapService.getInstance().getMap(type);
		java.awt.Rectangle bounds = (Rectangle) getFieldValue(type.getId() + "SessionMapPanel");
		BorderParams borderForRectangle = new BorderParams(borderBigComponent.width(), borderBigComponent.color(), borderBigComponent.insets(), borderBigComponent.arc()*2, borderBigComponent.focusWidth(), borderBigComponent.focusedColor(), borderBigComponent.disabledColor());
		ImageMapPane result = new ImageMapPane(map, bounds.width, bounds.height, borderForRectangle, new Rectangle(11, 11, 410, 254));
		result.setLayoutX(bounds.x);
		result.setLayoutY(bounds.y);
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
	    if (c == null) {
	        return null; // Das kann in der Tat passieren z.B. im Konstruktor von Skin hier :)
	    }
	    
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

package app.shared.skin;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import app.shared.model.BorderParams;
import app.shared.model.DialogStyle;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * Die Werte eines Skins — und das Laden dieser Werte aus einer {@code .properties}-Datei.
 *
 * <p><b>Rolle.</b> Diese Klasse hält, was ein Skin ausmacht: Farben, Fonts, Border, Maße und die
 * Layout-Rechtecke der Lern-Sessions. Sie baut nichts und stylt nichts — sie ist der Zulieferer.
 * Das CSS erzeugt eine eigene Klasse, gebaut wird in {@code shared.ui}.</p>
 *
 * <p><b>Vererbung.</b> Die konkreten Skins ({@code DarkMode}, {@code FlatWebSkin}, …) sind
 * Unterklassen von hier. Sie tragen nichts als einen Anzeigenamen und den Namen ihrer
 * properties-Datei; ein abgeleiteter Skin lädt erst die Eltern-Config, dann die eigene, und das
 * Spätere gewinnt.</p>
 *
 * <p><b>Ordnung (siehe {@code docs/Skin-Refactoring-Plan.md} §1).</b> {@code shared.skin} liegt
 * <em>unter</em> {@code shared.ui}: die UI-Schicht liest hier Werte, dieses Paket kennt
 * {@code shared.ui} nicht. Und kein Feature-Paket greift jemals hierher — beides ist per grep
 * bewacht.</p>
 *
 * <p><b>Namenswissen bleibt drinnen.</b> Nach außen gehen nur zweckgeschnittene Werte, nie
 * Property-Namen. Niemand fragt von außerhalb nach {@code hannoverMapOverlayImageName}.</p>
 */
public abstract class SkinProperties {

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
	 * Ist die Breite der ContentPane. Menüzeile und Border um die Rootpane sowie der Windows-Border
	 * gehören nicht dazu! --- Fenster = Spielfeld [+ Menü] + 2xBorder --- Von daher: Wenn deine Font
	 * größer wird bei einem Skinwechsel, dann vergrößert sich das Fenster.
	 *
	 * <p>Als Feld, damit ein HighRes-Skin mit größerem Fenster es überschreiben kann. Achtung:
	 * {@code loadAllConfigs} kennt keinen {@code Dimension2D}-Zweig — ein {@code contentSize=…} in
	 * einer properties-Datei würde <b>still ignoriert</b>. Wer das braucht, erweitert den Parser.</p>
	 */
	protected Dimension2D contentSize = new Dimension2D(1910, 1000);

	public Dimension2D getContentSize() {
		return contentSize;
	}

	// ========== Öffentliche Fläche ==========
	// region
	// Nach außen gehen zweckgeschnittene Records, keine Feld-Getter und niemals Property-Namen.
	// Wächst nach Bedarf: erst anlegen, wenn eine Klasse in shared.ui den Wert wirklich braucht.

	/** Die Werte, die der Dialog- und Alert-Pfad braucht. */
	public DialogStyle dialogStyle() {
		return new DialogStyle(font, textColor);
	}

	// endregion
	
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

}

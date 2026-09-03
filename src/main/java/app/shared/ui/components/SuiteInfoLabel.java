package app.shared.ui.components;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

/**
 * A custom TextLabel that can parse simple html without using the expensive browser-engine.
 * Supported html-tags are: &lt;br /&gt;, &lt;b&gt;, &lt;i&gt;. Child nodes are used for this.
 * Soft hyphening is not possible but if there is a hyphen in a word then it can be wrapped there
 * 
 *  Css-classes:
 *  		das Label selbst	= "my-info-label"
 *  		text in Nodes		= "text"
 */
public class SuiteInfoLabel extends StackPane {

    private final TextFlow textFlow;
    private String rawText = "";

    // Regex erklärt:
    // (?i)       -> Case-insensitive (erkennt <BR> genauso wie <br>)
    // <br\s*/?>  -> Erkennt <br>, <br/> und <br /> (mit beliebig viel Leerzeichen)
    // <b>|</b>   -> Erkennt Start- und End-Tag für Bold
    // <i>|</i>   -> Erkennt Start- und End-Tag für Italic
    private static final Pattern TAG_PATTERN = Pattern.compile("(?i)(<br\\s*/?>|<b>|</b>|<i>|</i>)");

    /**
     * Zeichen, die keine der geladenen Schriften hat und die deshalb aus der Ersatzschrift des
     * Systems kommen: Zwinkersmiley (U+1F609) und leichtes Lächeln (U+1F642). Als Codepoint
     * geschrieben, damit die Quelldatei selbst kein Emoji tragen muss. Ein weiteres ist ein Eintrag
     * mehr — mehr braucht es nicht, weil alle Texte der Suite von Hand geschrieben sind.
     */
    private static final List<String> SMILEYS = List.of(
            new String(Character.toChars(0x1F609)), new String(Character.toChars(0x1F642)));

    /**
     * Mit fester Lage und Größe — für absolut positionierende Hosts.
     */
    public SuiteInfoLabel(String text, Rectangle2D bounds) {
        this(text);
        setLayoutX(bounds.getMinX());
        setLayoutY(bounds.getMinY());
        setFixedWidth(bounds.getWidth());
        setFixedHeight(bounds.getHeight());
    }

    /**
     * Ohne feste Lage — für Aufrufer, die die Komponente in ein Layout hängen.
     *
     * <p>Das Aussehen kommt über {@code .my-info-label}. Eine Lern-Ansicht setzt zusätzlich einen
     * Modifikator ({@code .question}, {@code .progress}, {@code .history}), der nur den Hintergrund
     * abweichend färbt — alles andere steht schon in der Klasse. Ohne ihn sieht das Label nicht
     * falsch aus, nur neutral.</p>
     */
    public SuiteInfoLabel(String text) {
        this.textFlow = new TextFlow();

        getStyleClass().add("my-info-label");
        setAlignment(Pos.CENTER_LEFT);

        // Verhindert, dass das StackPane unnötig Platz einnimmt
        textFlow.setMaxHeight(Region.USE_PREF_SIZE);

        getChildren().add(textFlow);

        setText(text);
    }
    

    /**
     * Zentriert den Text waagerecht — für einzeilige Felder, in denen linksbündig verloren aussieht.
     * Bei mehrzeiligem Fließtext (Anki-Frage, 500×330) bleibt linksbündig richtig, dort fransen
     * zentrierte Zeilenenden aus.
     *
     * <p>Es muss der {@code TextFlow} sein und nicht das StackPane: der bekommt in
     * {@link #setFixedWidth(double)} die volle Feldbreite und füllt das StackPane damit aus — für
     * {@link #setAlignment} bleibt waagerecht nichts zu verschieben. Senkrecht zentriert der
     * Konstruktor ohnehin schon.</p>
     */
    public void centerText() {
        textFlow.setTextAlignment(TextAlignment.CENTER);
    }

    public void setText(String text) {
        this.rawText = text != null ? text : "";
        rebuildChildren();
    }

    public String getText() {
        return rawText;
    }

    public void setFixedWidth(double width) {
        setPrefWidth(width);
        setMaxWidth(width);
        textFlow.setPrefWidth(width);
        textFlow.setMaxWidth(width);
    }

    public void setFixedHeight(double height) {
        setPrefHeight(height);
        setMaxHeight(height);
    }

    private void rebuildChildren() {
        textFlow.getChildren().clear();

        Matcher matcher = TAG_PATTERN.matcher(rawText);

        int lastEnd = 0;
        boolean isBold = false;
        boolean isItalic = false;

        while (matcher.find()) {
            // 1. Text VOR dem gefundenen Tag hinzufügen
            String textPart = rawText.substring(lastEnd, matcher.start());
            if (!textPart.isEmpty()) {
                createNode(textPart, isBold, isItalic);  // KEIN .add() mehr!
            }

            // 2. Das gefundene Tag analysieren
            String tag = matcher.group().toLowerCase();

            if (tag.startsWith("<br")) {
                // Ein Zeilenumbruch im TextFlow wird durch "\n" erreicht
                textFlow.getChildren().add(new Text("\n"));
            } else if (tag.equals("<b>")) {
                isBold = true;
            } else if (tag.equals("</b>")) {
                isBold = false;
            } else if (tag.equals("<i>")) {
                isItalic = true;
            } else if (tag.equals("</i>")) {
                isItalic = false;
            }

            lastEnd = matcher.end();
        }

        // 3. Den Rest des Textes nach dem letzten Tag hinzufügen
        if (lastEnd < rawText.length()) {
            createNode(rawText.substring(lastEnd), isBold, isItalic);  // KEIN .add() mehr!
        }
    }

    private void createNode(String content, boolean bold, boolean italic) {
        // Ein Smiley kommt aus der Ersatzschrift und ist dort eine Haarlinie, die neben kräftiger
        // Schrift dünn aussieht. Er bekommt deshalb seinen eigenen Knoten mit eigener Stilklasse —
        // erst damit kann der Skin ihn nachziehen, ohne die Wörter daneben mitzutreffen.
        String smiley = firstSmileyIn(content);
        if (smiley != null) {
            int at = content.indexOf(smiley);
            if (at > 0) {
                createNode(content.substring(0, at), bold, italic);
            }
            Text node = new Text(smiley);
            applyStyle(node, bold, italic);
            node.getStyleClass().add("smiley");
            textFlow.getChildren().add(node);
            String rest = content.substring(at + smiley.length());
            if (!rest.isEmpty()) {
                createNode(rest, bold, italic);
            }
            return;
        }

        // Wenn der Content Bindestriche enthält, aufteilen und Hair Spaces einfügen
    	// Leider war ein echtes Soft-Hyphening nicht möglich. Also dass wir sagen "Hier
    	// darf umgebrochen werden" und wenn nötig, dann tut JavaFX das und setzt auch einen
    	// Bindestrich. Und wenn nicht, dann bleibt das Wort zusammen. Aber dass man hiermit
    	// zumindest lange Wörter mit Bindestrich umgebrochen bekommt, ist schon ein Fortschritt!
        if (content.contains("-")) {
            String[] parts = content.split("-", -1); // -1 behält leere Strings
            
            for (int i = 0; i < parts.length; i++) {
                // Text vor/nach dem Bindestrich
                if (!parts[i].isEmpty()) {
                    Text textNode = new Text(parts[i]);
                    applyStyle(textNode, bold, italic);
                    textFlow.getChildren().add(textNode);
                }
                
                // Bindestrich mit Hair Space dahinter (außer beim letzten Teil)
                if (i < parts.length - 1) {
                    Text hyphen = new Text("-");
                    applyStyle(hyphen, bold, italic);
                    textFlow.getChildren().add(hyphen);
                    
                    Text hairSpace = new Text("\u200A");
                    hairSpace.setStyle("-fx-font-size: 1px;");
                    textFlow.getChildren().add(hairSpace);
                }
            }
        } else {
            // Kein Bindestrich -> normaler Text-Node
            Text node = new Text(content);
            applyStyle(node, bold, italic);
            textFlow.getChildren().add(node);
        }
    }

    /** Der Smiley, der in diesem Stück am weitesten vorn steht — oder {@code null}, wenn keiner drin ist. */
    private static String firstSmileyIn(String content) {
        String found = null;
        int first = Integer.MAX_VALUE;
        for (String smiley : SMILEYS) {
            int at = content.indexOf(smiley);
            if (at >= 0 && at < first) {
                first = at;
                found = smiley;
            }
        }
        return found;
    }

    private void applyStyle(Text node, boolean bold, boolean italic) {
        StringBuilder style = new StringBuilder();
        
        if (bold) {
            style.append("-fx-font-weight: bold; ");
        }
        if (italic) {
            style.append("-fx-font-style: italic; ");
        }
        
        if (style.length() > 0) {
            node.setStyle(style.toString());
        }
        
        node.getStyleClass().add("text");
    }
}
package app.ui.components;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * A custom TextLabel that can parse simple html without using the expensive browser-engine.
 * Supported html-tags are: <br />, <b>, <i>. Child nodes are used for this.
 * Soft hyphening is not possible but if there is a hyphen in a word then it can be wrapped there
 * 
 *  Css-classes:
 *  		CustomTextLabel	= "custom-text-label"
 *  		text in Nodes	= "text"
 */
public class SessionInfoLabel extends StackPane {

    private final TextFlow textFlow;
    private String rawText = "";

    // Regex erklärt:
    // (?i)       -> Case-insensitive (erkennt <BR> genauso wie <br>)
    // <br\s*/?>  -> Erkennt <br>, <br/> und <br /> (mit beliebig viel Leerzeichen)
    // <b>|</b>   -> Erkennt Start- und End-Tag für Bold
    // <i>|</i>   -> Erkennt Start- und End-Tag für Italic
    private static final Pattern TAG_PATTERN = Pattern.compile("(?i)(<br\\s*/?>|<b>|</b>|<i>|</i>)");

    public SessionInfoLabel(String text) {
        this.textFlow = new TextFlow();

        //getStyleClass().add("custom-text-label"); Wird gerade nicht genutzt anscheinend.
        //this.pseudoClassStateChanged(PseudoClass.getPseudoClass("meine-klasse"), true);
        setAlignment(Pos.CENTER_LEFT);

        // Verhindert, dass das StackPane unnötig Platz einnimmt
        textFlow.setMaxHeight(Region.USE_PREF_SIZE);

        getChildren().add(textFlow);

        setText(text);
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
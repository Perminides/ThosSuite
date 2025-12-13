package app.ui.components;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomTextLabel extends TextFlow {

    private String rawText = "";
    private Font baseFont = Font.getDefault();
    private Paint textFill = Color.BLACK;

    public CustomTextLabel(String text) {
        // Diese CSS-Klasse ist wichtig für die Skin-Generierung später
        getStyleClass().add("custom-text-label");
        setText(text);
    }

    public void setText(String text) {
        this.rawText = text != null ? text : "";
        rebuildChildren();
    }

    public void setFont(Font font) {
        this.baseFont = font;
        rebuildChildren();
    }

    public void setTextFill(Paint fill) {
        this.textFill = fill;
        rebuildChildren();
    }

    /**
     * WICHTIG: Setzt die Breite für das Null-Layout.
     * TextFlow bricht automatisch am Ende der prefWidth um.
     */
    public void setFixedWidth(double width) {
        setPrefWidth(width);
        setMaxWidth(width);
        // Hinweis: Padding wird vom TextFlow automatisch berücksichtigt
    }

    private void rebuildChildren() {
        getChildren().clear();

        // Parser für <b> Tags
        Pattern pattern = Pattern.compile("<b>(.*?)</b>");
        Matcher matcher = pattern.matcher(rawText);
        List<Text> nodes = new ArrayList<>();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                nodes.add(createNode(rawText.substring(lastEnd, matcher.start()), false));
            }
            nodes.add(createNode(matcher.group(1), true)); // Fett
            lastEnd = matcher.end();
        }
        if (lastEnd < rawText.length()) {
            nodes.add(createNode(rawText.substring(lastEnd), false));
        }

        getChildren().addAll(nodes);
    }

    private Text createNode(String content, boolean bold) {
        Text node = new Text(content);
        node.setFill(textFill);
        if (bold) {
            // Fett basierend auf dem aktuellen Font
            node.setFont(Font.font(baseFont.getFamily(), FontWeight.BOLD, baseFont.getSize()));
        } else {
            node.setFont(baseFont);
        }
        return node;
    }
}
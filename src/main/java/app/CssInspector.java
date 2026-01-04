package app;

import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import java.util.List;

public class CssInspector {

    // Einstiegspunkt: Ruft rekursive Suche auf
    public static void dumpRecursive(Node node) {
        System.out.println("\n========== START CSS DUMP ==========");
        dumpNodeRecursive(node, 0);
        System.out.println("=========== END CSS DUMP ===========\n");
    }

    private static void dumpNodeRecursive(Node node, int depth) {
        // 1. WICHTIG: Erzwinge CSS-Berechnung, falls noch nicht geschehen!
        node.applyCss(); 

        String indent = "  ".repeat(depth);
        String nodeName = node.getClass().getSimpleName();
        String idInfo = (node.getId() != null) ? " #" + node.getId() : "";
        String classInfo = node.getStyleClass().isEmpty() ? "" : " ." + String.join(".", node.getStyleClass());

        System.out.println(indent + "▶ " + nodeName + idInfo + classInfo);

        dumpStylesForNode(node, indent + "    ");

        // Rekursion für Kinder (falls es ein Parent ist)
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                dumpNodeRecursive(child, depth + 1);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dumpStylesForNode(Node node, String indent) {
        List<CssMetaData<? extends Styleable, ?>> metaDataList = node.getCssMetaData();

        boolean foundAny = false;
        for (CssMetaData<? extends Styleable, ?> metaData : metaDataList) {
            CssMetaData rawMetaData = (CssMetaData) metaData;
            StyleableProperty<?> prop = rawMetaData.getStyleableProperty(node);

            if (prop == null) continue;

            StyleOrigin origin = prop.getStyleOrigin();
            
            // Nur ausgeben, wenn tatsächlich ein Style greift (nicht null)
            if (origin != null) {
                foundAny = true;
                Object value = prop.getValue();
                String propertyName = metaData.getProperty();
                String sourceTag = "[" + origin + "]";

                // Farbe: Grün für USER/INLINE, Grau für Default
                String colorCode = (origin == StyleOrigin.USER || origin == StyleOrigin.INLINE) 
                                   ? "\u001B[32m" : ""; // Grün start
                String resetCode = (origin == StyleOrigin.USER || origin == StyleOrigin.INLINE) 
                                   ? "\u001B[0m" : "";  // Reset

                System.out.println(indent + colorCode + String.format("%-12s %s = %s", sourceTag, propertyName, value) + resetCode);
            }
        }
        if (!foundAny) {
            // Optional: Zeigen, dass hier nichts gestylt wurde
            // System.out.println(indent + "(keine aktiven Styles gefunden)");
        }
    }
}
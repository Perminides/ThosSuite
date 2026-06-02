package app.scripts.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

/**
 * Standalone-Test: Kommentar-Popup mit langem Text.
 * 
 * Nutzt ein Popup mit einem Label statt eines Tooltips.
 * Label 1: Langer Text OHNE Zeilenumbrüche
 * Label 2: Langer Text MIT Zeilenumbrüchen
 */
public class TooltipWrapTest extends Application {

    private static final double POPUP_WIDTH = 300;

    private static final String TEXT_SHORT =
            "Ein kurzer Kommentar der ungefaehr hundert Zeichen lang ist und in einer einzigen Zeile daherkommt ohne Umbruch.";

    private static final String TEXT_TWO_PARAGRAPHS =
            "Erster Absatz mit ungefaehr hundert Zeichen, der einen ganz normalen Filmkommentar darstellen soll, nichts besonderes.\n\n" +
            "Zweiter Absatz, ebenfalls so um die hundert Zeichen lang, mit ein paar weiteren Gedanken zum Film und zur Regie.";

    private static final String TEXT_VERY_LONG =
            "Erster Absatz. Das war mir dann doch ein bisschen zu sehr unterkuehlt alles. " +
            "Diese sterile Stimmung, die die ganze Zeit durchgehalten wird und nur " +
            "auf der Dialogebene durchbrochen, die schlaefert dann auch ganz schoen " +
            "ein auf Dauer.\n\n" +
            "Zweiter Absatz. Ich habe keine Lust gerade noch mehr ueber den Film zu lesen, aber ich finde " +
            "diese Kritik trifft es so gar nicht. Fuer mich war das herausragende an dem Film schon sein " +
            "Humor und seine Dekonstruktion des Auftragsmoerders in Filmen. Der auch noch The Smiths hoert, " +
            "unpassender geht es ja wohl kaum. Und von Spannung kann doch keine Rede sein, der Film " +
            "verweigerte sich doch permanent jeglicher Spanungsdramaturgie.\n\n" +
            "Dritter Absatz. Die Szene mit dem schwarzen Auftraggeber und seiner Sekretaerin und auch die " +
            "Szene mit Tilda Swinton im Restaurant waren schon sehr sehr lustig. Keine Ahnung, seltsamer " +
            "Film echt. Aber ich geb dem eine freundliche 8.\n\n" +
            "Vierter Absatz. Und damit landet der sehr weit vorne in meinen Jahrescharts. So richtig " +
            "greifen kann ich es aber auch noch nicht. Vielleicht muss ich den nochmal schauen.\n\n" +
            "Fuenfter Absatz. Nochmal zur Regie: David Fincher ist einfach ein Meister der Bildkomposition. " +
            "Jede einzelne Einstellung sitzt, jedes Licht ist perfekt gesetzt. Das allein ist schon den " +
            "Eintritt wert, auch wenn die Geschichte manchmal etwas duenn ist.\n\n" +
            "Sechster Absatz. Was mich auch beeindruckt hat war der Soundtrack. Die Smiths passen " +
            "ueberhaupt nicht zu einem Auftragsmoerder und genau das macht es so gut. Diese Ironie " +
            "zieht sich durch den ganzen Film.\n\n" +
            "Siebter Absatz. Insgesamt ein Film den man gesehen haben muss, auch wenn er nicht perfekt " +
            "ist. Die handwerkliche Qualitaet allein rechtfertigt die Sichtung. Und Tilda Swinton ist " +
            "sowieso immer grossartig, egal in welchem Film sie auftaucht.";

    private final Popup popup = new Popup();

    @Override
    public void start(Stage primaryStage) {
    	Label label1 = new Label("Hover mich - Kurzer Text (eine Zeile)");
    	label1.setStyle("-fx-background-color: #cc9900; -fx-padding: 20;");
    	label1.setPrefWidth(400);
    	setupPopup(label1, TEXT_SHORT);

    	Label label2 = new Label("Hover mich - Zwei Absaetze");
    	label2.setStyle("-fx-background-color: #99cc00; -fx-padding: 20;");
    	label2.setPrefWidth(400);
    	setupPopup(label2, TEXT_TWO_PARAGRAPHS);

    	Label label3 = new Label("Hover mich - Sehr viel Text");
    	label3.setStyle("-fx-background-color: #cc6600; -fx-padding: 20;");
    	label3.setPrefWidth(400);
    	setupPopup(label3, TEXT_VERY_LONG);

    	VBox root = new VBox(20, label1, label2, label3);
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 500, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tooltip Wrap Test");
        primaryStage.show();
    }

    private void setupPopup(Label target, String commentText) {
        target.setOnMouseEntered(e -> {
            popup.getContent().clear();

            Label content = new Label(commentText);
            content.setWrapText(true);
            content.setMaxWidth(POPUP_WIDTH);
            content.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-padding: 8; -fx-font-size: 20px;");

            popup.getContent().add(content);
            popup.show(target, e.getScreenX() + 15, e.getScreenY() + 15);
        });

        target.setOnMouseExited(_ -> popup.hide());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
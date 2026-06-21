package app.shared;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * TextFieldTableCell, das Tab und Shift-Tab wie Enter behandelt:
 * committet die aktuelle Eingabe und verlässt den Editiermodus.
 *
 * <h3>Hintergrund</h3>
 * Die Stock-{@link TextFieldTableCell} bindet im Editor nur Enter (Commit) und
 * Escape (Cancel). Tab ist an nichts gebunden und läuft in die normale
 * Fokus-Traversierung: Der Fokus wird aus dem TextField gezogen, die Zelle
 * bleibt aber im Editing-State. Das TextField wird weiter gerendert (sieht aus
 * wie Editmodus), hat aber keinen Fokus mehr; ein folgendes Enter erreicht
 * deshalb nicht mehr den Editor, sondern feuert den Default-Button des
 * DialogPanes (= ungewolltes Bestätigen des ganzen Dialogs).
 *
 * <h3>Lösung</h3>
 * Beim Start des Editierens wird ein {@code KEY_PRESSED}-Filter auf den Editor
 * gelegt. Filter laufen vor der Skin-/Behavior-Tastenbehandlung, also vor der
 * Traversierung. Bei Tab (mit oder ohne Shift) wird über den Converter
 * committet und das Event konsumiert, sodass keine Traversierung stattfindet.
 *
 * <h3>Verhalten bei nicht-parsebarer Eingabe</h3>
 * Identisch zu Enter: Die Konvertierung läuft über denselben Converter. Wirft
 * der Converter (z. B. {@code DoubleStringConverter} bei Buchstaben), verhält
 * sich das exakt wie ein Enter mit derselben Eingabe — bewusst nicht abgefangen.
 */
public class TabCommitTextFieldTableCell<S, T> extends TextFieldTableCell<S, T> {

    /** Der Editor wird von TextFieldTableCell gecacht und wiederverwendet;
     *  hier gemerkt, damit der Filter pro Editor nur einmal angehängt wird. */
    private TextField wiredEditor;

    public TabCommitTextFieldTableCell(StringConverter<T> converter) {
        super(converter);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(StringConverter<T> converter) {
        return column -> new TabCommitTextFieldTableCell<>(converter);
    }

    @Override
    public void startEdit() {
        super.startEdit();

        // super.startEdit() kehrt ohne Editmodus zurück, wenn Cell/Column/Table
        // nicht editierbar sind. Dann gibt es keinen Editor zum Verdrahten.
        if (!isEditing()) {
            return;
        }

        // Nach erfolgreichem startEdit ist das gerenderte Graphic der Editor.
        if (getGraphic() instanceof TextField editor && editor != wiredEditor) {
            editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.TAB) {
                    commitEdit(getConverter().fromString(editor.getText()));
                    event.consume();
                }
            });
            wiredEditor = editor;
        }
    }
}
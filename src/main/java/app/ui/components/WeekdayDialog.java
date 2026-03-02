package app.ui.components;

import app.data.persistence.WeekdayRepository;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

public class WeekdayDialog {

    private static final LocalDate RANGE_START = LocalDate.of(1600, 1, 1);
    private static final int      RANGE_DAYS   = 255_669;

    private static final String[] WEEKDAYS = {
        "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
    };

    private final WeekdayRepository repository = new WeekdayRepository();

    public void showForDaily() {
        if (repository.playedToday()) {
            Log.debug(this, "WeekdayDialog: heute bereits gespielt, überspringe.");
            return;
        }
        show(true);
    }

    public void showForPractice() {
        show(false);
    }

    private void show(boolean saveResult) {
        LocalDate puzzleDate = randomDate();
        String   dateString  = formatDate(puzzleDate);

        Window owner = SkinService.getOwnerWindow();

        // createDialog gibt Dialog<?> zurück – wir casten bewusst auf Integer,
        // um setResult() nutzen zu können und die showAndWait()-Event-Loop sauber zu beenden.
        @SuppressWarnings("unchecked")
        Dialog<Integer> dialog = (Dialog<Integer>) SkinService.get().createDialog(owner, "Wochentag berechnen");

        // X-Button blockieren
        dialog.getDialogPane().getScene().getWindow().addEventFilter(
            WindowEvent.WINDOW_CLOSE_REQUEST, WindowEvent::consume);

        // CANCEL_CLOSE damit JavaFX den Dialog grundsätzlich schließen lässt
        ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        // ButtonBar auf Höhe 0 zwingen – kein sichtbarer Inhalt, kein reservierter Platz
        var buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setMinHeight(0);
            buttonBar.setPrefHeight(0);
            buttonBar.setMaxHeight(0);
            var cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
            if (cancelBtn != null) {
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
            }
        }

        int[] chosen = { -1 };
        dialog.getDialogPane().setContent(buildContent(dialog, dateString, chosen));

        long start = System.currentTimeMillis();
        dialog.showAndWait();

        Log.debug(this, "showAndWait() beendet, chosen[0] = " + chosen[0]);

        int correctIndex = puzzleDate.getDayOfWeek().getValue() - 1;
        int seconds      = (int) ((System.currentTimeMillis() - start) / 1000);

        if (chosen[0] == correctIndex) {
            handleCorrect(owner, seconds, puzzleDate, saveResult);
        } else {
            handleWrong(owner, puzzleDate, correctIndex, saveResult);
        }
    }

    private VBox buildContent(Dialog<Integer> dialog, String dateString, int[] chosen) {
        Label label = new Label(dateString);
        label.getStyleClass().add("weekday-puzzle-date");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        for (int i = 0; i < WEEKDAYS.length; i++) {
            final int index = i;
            Button btn = new Button(WEEKDAYS[i]);
            btn.setOnAction(_ -> {
                Log.debug(this, "Wochentag geklickt: " + index);
                chosen[0] = index;
                dialog.setResult(index); // beendet die showAndWait()-Event-Loop
            });
            buttons.getChildren().add(btn);
        }

        return new VBox(10, label, buttons);
    }

    private void handleCorrect(Window owner, int seconds, LocalDate puzzleDate, boolean saveResult) {
        SkinService.get()
            .createAlert(owner, "Wochentag berechnen",
                "Korrekt in " + seconds + " Sekunden.", false, false)
            .showAndWait();
        if (saveResult)
            repository.save(puzzleDate, seconds);
    }

    private void handleWrong(Window owner, LocalDate puzzleDate, int correctIndex, boolean saveResult) {
        String correctName = puzzleDate.getDayOfWeek()
            .getDisplayName(TextStyle.FULL, Locale.GERMANY);
        SkinService.get()
            .createAlert(owner, "Wochentag berechnen",
                "Leider falsch. Es war ein " + correctName + ".", false, false)
            .showAndWait();
        if (saveResult)
            repository.save(puzzleDate, -1);
    }

    private static LocalDate randomDate() {
        return RANGE_START.plusDays((long) (Math.random() * RANGE_DAYS));
    }

    private static String formatDate(LocalDate date) {
        double r = Math.random() * 3;
        if (r < 1)
            return date.toString();
        if (r < 2)
            return date.format(DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.GERMANY));
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
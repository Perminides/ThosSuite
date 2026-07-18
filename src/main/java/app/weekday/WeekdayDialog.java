package app.weekday;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import app.shared.Log;
import app.shared.model.AlertOptions;
import app.shared.model.ButtonEnum;
import app.shared.skin.SkinService;
import app.weekday.repository.WeekdayRepository;

public class WeekdayDialog {

    private static final LocalDate RANGE_START = LocalDate.of(1600, 1, 1);
    private static final int      RANGE_DAYS   = 255_669;

    // Reihenfolge = Anzeige-Reihenfolge = Index 0..6 (Montag..Sonntag)
    private static final List<ButtonEnum> WEEKDAY_BUTTONS = List.of(
        ButtonEnum.MONDAY, ButtonEnum.TUESDAY, ButtonEnum.WEDNESDAY,
        ButtonEnum.THURSDAY, ButtonEnum.FRIDAY, ButtonEnum.SATURDAY, ButtonEnum.SUNDAY);

    private final WeekdayRepository repository = new WeekdayRepository();

    public void showForDaily() {
        if (repository.playedToday() || LocalDateTime.now().getHour() < 6) {
            Log.info(this, "WeekdayDialog gerade nicht dran. Wir überspringen das mal (bereits gespielt oder es ist noch nachts).");
            return;
        }
        show(true);
    }

    public void showForPractice() {
        show(false);
    }

    private void show(boolean saveResult) {
        LocalDate puzzleDate = randomDate();
        String dateString = formatDate(puzzleDate);

        long start = System.currentTimeMillis();
        ButtonEnum chosen = SkinService.get().showAlert(
        	    "Wochentag berechnen", dateString, new AlertOptions().centered().mandatory(),
        	    WEEKDAY_BUTTONS.toArray(new ButtonEnum[0]));
        int seconds = (int) ((System.currentTimeMillis() - start) / 1000);

        int chosenIndex = WEEKDAY_BUTTONS.indexOf(chosen);
        if (chosenIndex < 0)
            throw new IllegalStateException("Unerwarteter DialogButton aus Wochentag-Dialog: " + chosen);

        int correctIndex = puzzleDate.getDayOfWeek().getValue() - 1;

        if (chosenIndex == correctIndex)
            handleCorrect(seconds, puzzleDate, saveResult);
        else
            handleWrong(puzzleDate, saveResult);
    }

    private void handleCorrect(int seconds, LocalDate puzzleDate, boolean saveResult) {
        SkinService.get().showAlert("Wochentag berechnen",
            "Korrekt in " + seconds + " Sekunden.", ButtonEnum.OK);
        if (saveResult)
            repository.save(puzzleDate, seconds);
    }

    private void handleWrong(LocalDate puzzleDate, boolean saveResult) {
        String correctName = puzzleDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMANY);
        SkinService.get().showAlert("Wochentag berechnen",
            "Leider falsch. " + puzzleDate + " war ein " + correctName + ".", ButtonEnum.OK);
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
            return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.GERMANY));
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
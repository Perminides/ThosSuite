package app.fitbit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import app.fitbit.model.json.Activity;
import app.fitbit.model.json.ActivityDaySummary;
import app.shared.model.ActivityTableRow;
import app.shared.ui.surfaces.dialogs.ActivityTableDialog;

public class ActivityTablePresenter {

    private final LocalDate date;
    private final List<Activity> activities;
    private final ActivityDaySummary daySummary;

    public ActivityTablePresenter(LocalDate date, List<Activity> activities, ActivityDaySummary daySummary) {
        this.date = date;
        this.activities = activities;
        this.daySummary = daySummary;
    }

    public DialogResult showAndWait() {
        List<ActivityTableRow> edited =
            new ActivityTableDialog().show("Aktivitäten bearbeiten vom " + date, toRows());
        if (edited == null)
            return null;
        return fromRows(edited);
    }

    private List<ActivityTableRow> toRows() {
        List<ActivityTableRow> rows = new ArrayList<>();
        // Summenzeile: synthetisch, nicht löschbar, kein Original → leerer carry
        rows.add(new ActivityTableRow(
            "(gesamt)", "Steps", "", null, daySummary.getSummary().getSteps(), false, ""));
        for (Activity a : activities) {
            rows.add(new ActivityTableRow(
                a.getStartTime(),
                a.getActivityName(),
                a.getDistanceUnit() != null ? a.getDistanceUnit() : "",
                a.getDistance(),
                a.getSteps(),
                true,
                a.getOriginalStartTime()));   // carry: originalStartTime reist mit der Zeile
        }
        return rows;
    }

    private DialogResult fromRows(List<ActivityTableRow> rows) {
        int correctedTotalSteps = 0;
        List<Activity> edited = new ArrayList<>();

        for (ActivityTableRow row : rows) {
            if (!row.deletable()) {                 // die Summenzeile (einzige nicht löschbare)
                correctedTotalSteps = row.steps();
                continue;
            }
            Activity a = new Activity(
                row.carry(),                        // originalStartTime, unverändert vom Original
                row.activityName(),
                row.distanceUnit().isEmpty() ? null : row.distanceUnit(),
                row.distance(),
                row.steps());
            a.setStartTime(row.startTime());
            edited.add(a);
        }
        return new DialogResult(edited, correctedTotalSteps);
    }

    public record DialogResult(List<Activity> activities, int totalSteps) {}
}
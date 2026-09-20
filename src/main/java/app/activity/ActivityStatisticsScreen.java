package app.activity;

import app.shared.AppClock;
import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.BarChartScreenView;

public class ActivityStatisticsScreen implements Screen {

    private final BarChartScreenView view = new BarChartScreenView(new ActivityStatisticsPresenter(), AppClock.TODAY.minusYears(2), AppClock.TODAY);

    public ActivityStatisticsScreen() {
        view.rebuild();
    }

    @Override
    public ScreenView getView() {
        return view;
    }

    @Override
    public void refresh() {
        view.rebuild();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}

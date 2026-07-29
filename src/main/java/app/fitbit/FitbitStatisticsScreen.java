package app.fitbit;

import java.time.LocalDate;

import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.BarChartScreenView;

public class FitbitStatisticsScreen implements Screen {

    private final BarChartScreenView view = new BarChartScreenView(new FitbitStatisticsPresenter(), LocalDate.now().minusYears(2), LocalDate.now());

    public FitbitStatisticsScreen() {
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
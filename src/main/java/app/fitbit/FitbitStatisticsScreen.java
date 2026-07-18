package app.fitbit;

import java.time.LocalDate;

import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.BarChartScreenView;

public class FitbitStatisticsScreen implements Screen {

    private final BarChartScreenView view = new BarChartScreenView(new FitbitStatisticsPresenter(), LocalDate.now().minusYears(2), LocalDate.now());

    public FitbitStatisticsScreen() {
        view.reload();
    }

    @Override
    public ScreenView getView() {
        return view;
    }

    @Override
    public void refresh() {
        view.reload();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}
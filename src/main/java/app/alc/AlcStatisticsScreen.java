package app.alc;

import java.time.LocalDate;

import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.BarChartScreenView;

public class AlcStatisticsScreen implements Screen {

    private final BarChartScreenView view = new BarChartScreenView(new AlcStatisticsPresenter(), LocalDate.now().minusYears(1), LocalDate.now());

    public AlcStatisticsScreen() {
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
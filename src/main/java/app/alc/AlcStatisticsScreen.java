package app.alc;

import java.time.LocalDate;

import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.BarChartScreenView;

public class AlcStatisticsScreen implements Screen {

    private final BarChartScreenView view = new BarChartScreenView(new AlcStatisticsPresenter(), LocalDate.now().minusYears(1), LocalDate.now());

    public AlcStatisticsScreen() {
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
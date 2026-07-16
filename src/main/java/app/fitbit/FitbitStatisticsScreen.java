package app.fitbit;

import java.time.LocalDate;

import app.shared.Screen;
import app.shared.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.components.fitbit.WeekPointsChartData;
import app.shared.ui.components.fitbit.WeekPointsChartPane;

/**
 * !Sofort: Naja, das geht natürlich gar nicht mit den 2 Jahren hie rund in der Pane, die dann synchron sein müssen. Herrje.
 */
public class FitbitStatisticsScreen implements Screen {
	
	WeekPointsChartPane thePane;
	FitbitStatisticsPresenter thePresenter;
	
	public FitbitStatisticsScreen () {
		thePane = new WeekPointsChartPane();
		thePresenter = new FitbitStatisticsPresenter();
		thePane.setData(thePresenter.get(LocalDate.now().minusYears(2), LocalDate.now()));
		thePane.onDateChange((d, e) -> {
			WeekPointsChartData data = thePresenter.get(d, e);
			thePane.setData(data);
		});
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		return SessionSwitchStrategy.IMMEDIATE;
	}

	@Override
	public void refresh() {
		thePane.setData(thePresenter.get(LocalDate.now().minusYears(2), LocalDate.now()));
	}

	@Override
	public ScreenView getView() {
		return thePane.getView();
	}

}

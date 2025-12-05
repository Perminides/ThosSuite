package app.controller;

import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import app.data.AppClock;
import app.data.LearnStat;
import app.data.MapShape;
import app.data.RegionClickSessionProgress;
import app.data.RegionDeckService;
import app.data.RegionEliminationSessionProgress;
import app.data.RegionMode;
import app.data.RegionSessionProgress;
import app.data.RegionSessionSpec;
import app.data.RegionWriteSessionProgress;
import app.presenter.RegionSessionPresenter;
import app.ui.MainWindow;

public class RegionSession implements Session {
	
	private final RegionSessionPresenter presenter;
	private final RegionDeckService service;
    private final Controller controller;
    private final RegionSessionSpec spec;
    private final RegionSessionProgress progress;

	public RegionSession(MainWindow mainWindow, RegionSessionSpec spec, Set<MapShape> regions, Controller controller, RegionDeckService regionService) {
    	this.spec = spec;
    	this.service = regionService;
    	switch (spec.getMode().getSubCategory()) {
    		case RegionMode.SubCategory.CLICK: {
    			this.progress = new RegionClickSessionProgress(regions, spec, this);
    			break;
    		}
    		case RegionMode.SubCategory.ELIMINATION: {
    			this.progress = new RegionEliminationSessionProgress(regions, spec, this);
    			break;
    		}
    		case RegionMode.SubCategory.WRITE: {
    			this.progress = new RegionWriteSessionProgress(regions, spec, this);
    			break;
    		}
    		default: throw new RuntimeException("Das ist leider noch nicht implementiert :-)");
    	}
    	this.presenter = new RegionSessionPresenter(mainWindow, progress, spec);
        this.controller = controller;
	}
	
    public void start() {
        presenter.start();
        progress.start();
    }

	@Override
	public void cancel() {
		progress.cancel();
	}

	@Override
	public void end() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void endPause() {
		progress.endPause();
	}
	
	public void end(boolean correct, String wrongId, String text, boolean allowResume) {
		if (!correct && allowResume) {
			JOptionPane pane = new JOptionPane("<html>Schade. Wir speichern nun?<br/><br/>" + text + "</html>", JOptionPane.PLAIN_MESSAGE,
					JOptionPane.YES_NO_OPTION, null);
			JDialog dialog = pane.createDialog(null, "Nicht korrekt");
			dialog.setVisible(true);
			Object selectedValue = pane.getValue();
			int result = -1;
			if (selectedValue == null)
				result = JOptionPane.YES_OPTION;
			else
				result = (Integer) selectedValue;
			if (result == JOptionPane.NO_OPTION) {
				progress.resume();
				return;
			}
		}
		LearnStat stats = service.getLearnStat(spec);
		stats.setLevel(progress.calculateNewLevel(stats.getLastPlayed(), correct, false));
		stats.setLastPlayed(AppClock.TODAY);
		if (!correct)
			stats.incrementWrongCount();
		service.savePlayedCards(spec, stats, correct, wrongId);
		JOptionPane pane = new JOptionPane("Wir sehen uns wieder am: " + stats.getDueDate(), JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
    	JDialog dialog = pane.createDialog(null, "Abschluss");
    	dialog.setVisible(true);
		controller.sessionEnded();
	}

	@Override
	public void refresh() {
		presenter.refresh();
	}
	
}

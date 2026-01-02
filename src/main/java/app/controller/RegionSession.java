package app.controller;

import java.util.Optional;
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
import app.ui.skin.SkinService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;

public class RegionSession implements Session {
	
	private final RegionSessionPresenter presenter;
	private final RegionDeckService service;
    private final Controller controller;
    private final RegionSessionSpec spec;
    private final RegionSessionProgress progress;

	public RegionSession(RegionSessionSpec spec, Set<MapShape> regions, Controller controller, RegionDeckService regionService) {
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
    	this.presenter = new RegionSessionPresenter(progress, spec);
        this.controller = controller;
	}
	
    public void start() {
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
		if (!correct) {
			Alert alert = SkinService.get().createAlert(getView().getScene().getWindow(), "Titelzeile wird aktuell nicht angezeigt...", text, true, allowResume);
	    	Optional<ButtonType> result = alert.showAndWait();
	    		if (!result.isPresent() || result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
	    	        controller.sessionEnded();
	    	        return;
	    	    } else if (result.get().getButtonData() == ButtonBar.ButtonData.YES) {
	    	        System.out.println("User hat 'OK' geklickt.");
	    	        // Nichts weiter zu tun...
	    	    } else if (result.get().getButtonData() == ButtonBar.ButtonData.OTHER) {
	    	    	progress.resume();
					return;
	    	    }
		}
		if (!spec.isPlaySession()) {
			LearnStat stats = service.getLearnStat(spec);
			stats.setLevel(progress.calculateNewLevel(stats.getLastPlayed(), correct, false));
			stats.setLastPlayed(AppClock.TODAY);
			if (!correct)
				stats.incrementWrongCount();
			service.savePlayedCards(spec, stats, correct, wrongId);
			// !Sofort: Also mal abgesehen davon, dass wir hier Swing nutzen, solltest Du dich schon entscheiden, wo diese PopUps erstellt werden. Ach so, ja
			// ok, die Entscheidung ist wohl "in der session" und zumindest konsequent, oder"
			JOptionPane pane = new JOptionPane("Wir sehen uns wieder am: " + stats.getDueDate(), JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
			JDialog dialog = pane.createDialog(null, "Abschluss");
			dialog.setVisible(true);
		}
		controller.sessionEnded();
	}

	@Override
	public void refresh() {
		presenter.refresh();
	}
	
	public Pane getView() {
		return presenter.getView();
	}
}

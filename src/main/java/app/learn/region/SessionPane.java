package app.learn.region;

import java.util.Set;

import app.learn.MapService;
import app.learn.model.Deck;
import app.shared.ScreenView;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.SessionInfoLabel;
import app.shared.ui.components.ComponentHost;
import app.shared.ui.components.InputField;
import app.shared.ui.components.learn.ShapeMapPane;
import app.shared.ui.components.learn.ShapeMapPane.ShapeMapState;

public class SessionPane {
	private final SessionPresenter presenter;
	private final Deck deckType;
	private final ComponentHost host = new ComponentHost();
	private ShapeMapPane karte;
	private SessionInfoLabel questionArea;
	private InputField inputField;

	public SessionPane(SessionPresenter presenter, Deck deckType, boolean questionAreaVisible) {
		this.presenter = presenter;
		this.deckType = deckType;
		rebuild(questionAreaVisible);
	}

	public void rebuild(boolean questionAreaVisible) {
		Skin skin = SkinService.get();
		host.setBackgroundImage(skin.getBackgroundImage(deckType.getMapName(), deckType.getCategory().toString()));
		
		// holt sich die Geometrien selbst (SessionPane ist learn); die framework-freie Karte baut ihre Nodes selbst.
		karte = new ShapeMapPane(
				MapService.getInstance().getMap(deckType).getShapeGeometries(),
				deckType.getMapName(),
				deckType.getCategory().toString());
		karte.setClickListener(id -> presenter.clickedMapElement(id));

		// !Sofort: Hier wäre allerdings CENTER schon angesagt
		if (questionAreaVisible) {
			questionArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.QUESTION);
			questionArea.setText(""); // Initial leer
			host.setComponents(karte, questionArea);
		} else {
			inputField = new InputField(skin.createInputField(deckType.getMapName(), deckType.getCategory().toString()));
			inputField.onType(text -> presenter.typedText(text));
			host.setComponents(karte, inputField);
		}
	}

	public ScreenView getView() {
		return host;
	}

	// ----- MAP -----

	public void addIdsToActive(Set<String> ids) { karte.makeActive(ids); }
	public void addIdsToMarked(Set<String> ids) { karte.addToMarked(ids); }
	public void moveAllToActive() { karte.moveAllToActive(); } // CLICK_REGION_BLANK
	public void moveCorrectToActive() { karte.moveCorrectToActive(); }
	public void addIdsToCorrect(Set<String> elements) { karte.addToCorrect(elements); }
	public void addIdsToInactive(Set<String> elements) { karte.makeInactive(elements); }
	public void setIdToIncorrect(String element) { karte.addToIncorrect(element); }
	public void setMapActive(boolean active) { karte.setInteractive(active); }
	public ShapeMapState getState() { return karte.getState(); }
	public void setState(ShapeMapState state) { karte.setState(state); }

	// ----- Text -----

	public void setTextInTextField(String string) { inputField.setText(string); }
	public void setTextFieldActive(boolean active) { inputField.setActive(active); }

	public void setQuestion(String text) {
		if (questionArea != null)
			questionArea.setText(text);
	}

	public String getQuestion() {
		if (questionArea != null)
			return questionArea.getText();
		else
			return null;
	}
}
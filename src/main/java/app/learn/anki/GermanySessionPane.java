package app.learn.anki;

import java.util.List;
import java.util.Set;

import app.learn.MapService;
import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.components.MultipleChoicePane;
import app.shared.ui.components.SuiteIconButton;
import app.shared.ui.components.SuiteImage;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteTextField;
import app.shared.ui.components.learn.ShapeMapPane;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.ComponentHost;

/**
 * UI der Deutschland-Session. Hält kein javafx mehr selbst, sondern ein SessionCanvas plus die
 * Komponenten (über ihre schmalen Fassaden). Das Kompositionswissen — welche Komponenten einen
 * Deutschland-Screen ausmachen — bleibt hier in learn; das Zusammenstecken macht der Canvas.
 * 
 */
public class GermanySessionPane implements SessionPane {
	private static final Deck DECKTYPE = Deck.GERMANY_CARDS;

	private final SessionPresenter presenter;
	private final ComponentHost canvas = new ComponentHost();

	private SuiteTextField inputField;
	private SuiteInfoLabel questionArea;
	private SuiteInfoLabel progressArea;
	private SuiteInfoLabel cardHistoryArea;
	private MultipleChoicePane mcPane;
	private SuiteIconButton backButton;
	private SuiteImage imageComponent;
	private ShapeMapPane deutschlandkarte;

	public GermanySessionPane(SessionPresenter presenter) {
		this.presenter = presenter;
		rebuild();
	}

	/**
	 * Sofort: Was nun? new ShapeMapPane oder skin.create?
	 */
	public void rebuild() {
		Skin skin = SkinService.get();
		
		canvas.setBackgroundImage(skin.getBackgroundImage(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString()));

		deutschlandkarte = new ShapeMapPane(
				MapService.getInstance().getMap(DECKTYPE).getShapeGeometries(),
				DECKTYPE.getMapName(),
				DECKTYPE.getCategory().toString());
		deutschlandkarte.setClickListener(id -> presenter.clickedMapElement(id));
		deutschlandkarte.moveAllToActive();

		questionArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.QUESTION);
		questionArea.setText("");

		inputField = new SuiteTextField(skin.createInputField(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString()));
		inputField.onType(text -> presenter.typedText(text));

		imageComponent = skin.createImageComponent(DECKTYPE.getId(), DECKTYPE.getCategory().toString());

		mcPane = skin.createMultipleChoicePane(DECKTYPE.getId(), DECKTYPE.getCategory().toString());
		mcPane.addListener(index -> presenter.clickedMCAnswer(index));

		progressArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.PROGRESS);
		progressArea.setText("");

		cardHistoryArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.CARD_HISTORY);
		cardHistoryArea.setText("");

		// !Sofort: Weil wir keinen javafx-Button halten wollen. Aber natürlich darf Skin auch keine javafx-Komponenten mehr rausgeben!
		backButton = new SuiteIconButton(skin.createIconButton(DECKTYPE.getId(), Skin.IconButtonType.BACK));
		backButton.onClick(() -> presenter.clickedBack());

		canvas.setComponents(deutschlandkarte, questionArea, inputField, imageComponent, mcPane, progressArea, cardHistoryArea, backButton);
	}

	// ===== Called from presenter =====

	@Override
	public void setQuestion(String text) {
		questionArea.setText(text);
	}

	@Override
	public void setImage(String imageName) {
		imageComponent.setImage(imageName);
	}

	@Override
	public void setMultipleChoice(List<String> answers) {
		mcPane.initiateMultipleChoice(answers);
	}

	@Override
	public void disableMcPanel() {
		mcPane.clearAndSetInactive();
	}

	@Override
	public void setMcCorrect(int id, boolean correct) {
		mcPane.setCorrect(id, correct);
	}

	@Override
	public void setMcSolution(Set<Integer> correctIds) {
		mcPane.setCorrectAndInactive(correctIds);
	}

	@Override
	public void resetMarkers() {
		deutschlandkarte.moveAllToActive();
	}

	@Override
	public void addIdsToCorrect(Set<String> elements) {
		deutschlandkarte.addToCorrect(elements);
	}

	@Override
	public void setMarkedIds(Set<String> elements) {
		deutschlandkarte.addToMarked(elements);
	}

	@Override
	public void setIdToIncorrect(String element) {
		deutschlandkarte.addToIncorrect(element);
	}

	@Override
	public void setMapActive(boolean active) {
		deutschlandkarte.setInteractive(active);
	}

	@Override
	public void setTextFieldActive(boolean active) {
		inputField.setActive(active);
	}

	@Override
	public void setTextInTextField(String text) {
		inputField.setText(text);
	}

	@Override
	public void setProgressText(String text) {
		progressArea.setText(text);
	}

	@Override
	public void setCardHistoryText(String text) {
		cardHistoryArea.setText(text);
	}

	@Override
	public ScreenView getView() {
		return canvas;
	}
}
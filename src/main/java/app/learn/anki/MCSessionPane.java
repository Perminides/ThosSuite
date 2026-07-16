package app.learn.anki;

import java.util.List;
import java.util.Set;

import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.shared.ScreenView;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.MultipleChoicePane;
import app.shared.ui.SessionInfoLabel;
import app.shared.ui.components.ComponentHost;
import app.shared.ui.components.IconButton;
import app.shared.ui.components.ImagePane;

public class MCSessionPane implements SessionPane {
	private static final Deck DECKTYPE = Deck.MC_CARDS;

	private final SessionPresenter presenter;
	private final ComponentHost canvas = new ComponentHost();

	private SessionInfoLabel questionArea;
	private SessionInfoLabel progressArea;
	private SessionInfoLabel cardHistoryArea;
	private MultipleChoicePane mcPane;
	private IconButton backButton;
	private ImagePane imageComponent;

	public MCSessionPane(SessionPresenter presenter) {
		this.presenter = presenter;
		rebuild();
	}

	@Override
	public void rebuild() {
		Skin skin = SkinService.get();
		
		canvas.setBackgroundImage(skin.getEmptyBackgroundImage());

		questionArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.QUESTION);
		questionArea.setText("");

		imageComponent = skin.createImageComponent(DECKTYPE.getId(), DECKTYPE.getCategory().toString());

		mcPane = skin.createMultipleChoicePane(DECKTYPE.getId(), DECKTYPE.getCategory().toString());
		mcPane.addListener(index -> presenter.clickedMCAnswer(index));

		progressArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.PROGRESS);
		progressArea.setText("");

		cardHistoryArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.CARD_HISTORY);
		cardHistoryArea.setText("");

		backButton = new IconButton(skin.createIconButton(DECKTYPE.getId(), Skin.IconButtonType.BACK));
		backButton.onClick(() -> presenter.clickedBack());

		canvas.setComponents(questionArea, imageComponent, mcPane, progressArea, cardHistoryArea, backButton);
	}

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
		/* Original: auskommentiert / no-op */ }

	@Override
	public void setMcCorrect(int id, boolean correct) {
		mcPane.setCorrect(id, correct);
	}

	@Override
	public void setMcSolution(Set<Integer> correctIds) {
		mcPane.setCorrectAndInactive(correctIds);
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
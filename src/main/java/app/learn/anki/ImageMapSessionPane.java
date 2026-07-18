package app.learn.anki;

import java.util.List;
import java.util.Set;

import app.learn.MapService;
import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.MapImagePaths;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.components.MultipleChoicePane;
import app.shared.ui.components.SuiteIconButton;
import app.shared.ui.components.SuiteImage;
import app.shared.ui.components.SuiteInfoLabel;
import app.shared.ui.components.SuiteTextField;
import app.shared.ui.components.learn.ImageMapPane;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.ComponentHost;

public class ImageMapSessionPane implements SessionPane {

	private final Deck deckType;
	private final SessionPresenter presenter;
	private final ComponentHost canvas = new ComponentHost();

	private GeoMap map;
	private SuiteTextField inputField;
	private SuiteInfoLabel questionArea;
	private SuiteInfoLabel progressArea;
	private SuiteInfoLabel cardHistoryArea;
	private MultipleChoicePane mcPane;
	private SuiteIconButton backButton;
	private ImageMapPane mapPane;
	private SuiteImage imageComponent;

	public ImageMapSessionPane(SessionPresenter presenter, Deck deckType) {
		this.presenter = presenter;
		this.deckType = deckType;
		rebuild();
	}

	@Override
	public void rebuild() {
		Skin skin = SkinService.get();
		canvas.setBackgroundImage(skin.getEmptyBackgroundImage());
		map = MapService.getInstance().getMap(deckType);
		MapImagePaths paths = MapService.getInstance().imagePathsFor(deckType);
		mapPane = new ImageMapPane(
				paths.background(), paths.overlay(), paths.inactiveBackground(), paths.inactiveOverlay(),
				skin.getOverlayContentBounds(deckType.getId()));
		mapPane.setViewportClip(skin.applyImageMapLayout(mapPane, deckType.getId())); // TODO: Ich verstehe nicht, was hier passiert. Muss diese Zeile hier wirklich hin?
		mapPane.center(); // jetzt steht die Größe
		mapPane.setListener(id -> presenter.clickedMapElement(id));

		questionArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.QUESTION);
		questionArea.setText("");

		inputField = new SuiteTextField(skin.createInputField(deckType.getMapName(), deckType.getCategory().toString()));
		inputField.onType(text -> presenter.typedText(text));

		imageComponent = skin.createImageComponent(deckType.getId(), deckType.getCategory().toString());

		mcPane = skin.createMultipleChoicePane(deckType.getId(), deckType.getCategory().toString());
		mcPane.addListener(index -> presenter.clickedMCAnswer(index));

		progressArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.PROGRESS);
		progressArea.setText(""); // FEHLER (Dein Marker — die alte getChildren().add-Zeile ist weg, Signal hier geparkt)

		cardHistoryArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.CARD_HISTORY);
		cardHistoryArea.setText(""); // FEHLER (Dein Marker)

		backButton = new SuiteIconButton(skin.createIconButton(deckType.getId(), Skin.IconButtonType.BACK));
		backButton.onClick(() -> presenter.clickedBack());

		canvas.setComponents(mapPane, questionArea, inputField, imageComponent, mcPane, progressArea, cardHistoryArea, backButton);
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
		mapPane.resetMarkers();
	}

	@Override
	public void setMapActive(boolean active) {
		mapPane.setActive(active);
	}

	@Override
	public void setIdsInQuestion(Set<String> ids) {
		mapPane.setToCheckShapes(map.geometryFor(ids));
	}

	@Override
	public void setMarkedIds(Set<String> ids) {
		mapPane.setMarked(map.geometryFor(ids));
	}

	@Override
	public void addIdsToCorrect(Set<String> elements) {
		mapPane.addToCorrect(map.geometryFor(elements));
	}

	@Override
	public void setIdToIncorrect(String id) { // id ist eh null
		mapPane.resetMarkers(); // TODO: EM 2021 — alle Länder grün, welches war falsch?
		mapPane.markLastClickAsIncorrect();
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
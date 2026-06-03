package app.learn.anki;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.learn.MapService;
import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.LearnStat;
import app.learn.model.MapElementListener;
import app.learn.model.SessionProgressCounter;
import app.shared.ImagePane;
import app.shared.MultipleChoicePane;
import app.shared.SessionInfoLabel;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class ImageMapSessionPane extends Pane implements SessionPane {
	
	private final Deck deckType; 
	private final SessionPresenter presenter;
    private TextField textInputField;
    private SessionInfoLabel questionArea;
    private SessionInfoLabel progressArea;
    private SessionInfoLabel cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImageMapPane mapPane;
    private ImagePane imageComponent;

    public ImageMapSessionPane(SessionPresenter presenter, Deck deckType) {
        this.presenter = presenter;
        this.deckType = deckType;
        this.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));
        initUI();
    }   

    private void initUI() {
        Skin skin = SkinService.get();
        GeoMap map = MapService.getInstance().getMap(deckType);
        mapPane = new ImageMapPane(map, skin.getOverlayContentBounds(deckType.getId()));
        mapPane.setViewportClip(skin.applyImageMapLayout(mapPane, deckType.getId()));  // Größe/Position/CSS + Clip zurück
        mapPane.center();                                                       // jetzt steht die Größe

    	mapPane.setListener(new MapElementListener() {
    	    @Override
    	    public void mouseClicked(String id) {
    	        mapElementClicked(id);  // ← Über Helper
    	    }
    	});
    	getChildren().add(mapPane);
    	
    	questionArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.QUESTION);
    	questionArea.setText("");
    	getChildren().add(questionArea);
    	
    	textInputField = skin.createInputField(deckType.getMapName(), deckType.getCategory().toString());
        textInputField.setOnKeyReleased(_ -> textInputChanged());
        getChildren().add(textInputField);
    	
        imageComponent = skin.createImageComponent(deckType.getId(), deckType.getCategory().toString());
    	getChildren().add(imageComponent);
    	
    	mcPane = skin.createMultipleChoicePane(deckType.getId(), deckType.getCategory().toString());
    	mcPane.addListener(
    		new Consumer<Integer>() {
				@Override
				public void accept(Integer i) {
					onAnswerSelected(i);
				}
    		}
    	);
    	getChildren().add(mcPane);
    	
    	progressArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.PROGRESS);
    	progressArea.setText("");
    	getChildren().add(progressArea); // FEHLER
    	
    	cardHistoryArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	getChildren().add(cardHistoryArea); // FEHLER
    	
    	backButton = skin.createIconButton(deckType.getId(), Skin.IconButtonType.BACK);
    	backButton.setOnAction(_ -> backButtonClicked());
    	getChildren().add(backButton);
    }
	
	// ========================================
	// Called from presenter
	// ========================================
    
    // Question
	
    public void setQuestion(String text) {
    	questionArea.setText(text);
    }
    
	// Image
	
    public void setImage(String imagePath) {
    	imageComponent.setImage(imagePath);
    }
    
    // Multiple Choice
    
	public void setMultipleChoice(List<String> answers) {
		mcPane.initiateMultipleChoice(answers);
	}
	
	/**
	 * Setzt aktive Buttons auf inaktiv. Nach falschem Klick z.B.
	 * @param active
	 */
	public void disableMcPanel() {
		mcPane.clearAndSetInactive();
	}
	
    public void setMcCorrect(int id, boolean correct) {
    	mcPane.setCorrect(id, correct);
    }
    
	public void setMcSolution(Set<Integer> correctIds) {
		mcPane.setCorrectAndInactive(correctIds);
	}
	
	// Map
	
    public void resetMarkers() {
    	mapPane.resetMarkers();
    }
    
    public void setMapActive(boolean active) {
    	mapPane.setActive(active);
    }
    
	@Override
	public void setIdsInQuestion(Set<String> ids) {
		mapPane.setToCheckShapes(ids);
	}
	
	@Override
	public void setMarkedIds(Set<String> ids) {
		mapPane.setMarked(ids);
	}
	
    @Override
	public void addIdsToCorrect(Set<String> elements) {
		mapPane.addToCorrect(elements);
    }
    
    @Override
	public void setIdToIncorrect(String id) { // Die id ist eh null, braucht uns nicht zu interessieren.
		mapPane.resetMarkers(); // Wo war die EM 2021? Alle Länder werden grün. Wer gewann? Wenn ich falsch klicke und jetzt eins der 10 nochmal grün wird sehe ich nicht, welches!
    	mapPane.markLastClickAsIncorrect();
    }
    
    // Input
    
    public void setTextFieldActive(boolean active) {
        if (active) {
            textInputField.setText("");
            textInputField.setDisable(false);
            textInputField.requestFocus();
        } else {
            textInputField.setDisable(true);
        }
    }
    
	public void setTextInTextField(String text) {
		textInputField.setText(text); 
	}	
	
	// ========================================
	// Called from components
	// ========================================
	
	public void onAnswerSelected(int index) {
		presenter.clickedMCAnswer(index);
	}
	
	private void textInputChanged() {
	    presenter.typedText(textInputField.getText());
	}

	private void mapElementClicked(String id) {
	    presenter.clickedMapElement(id);
	}
	
	private void backButtonClicked() {
	    presenter.clickedBack();
	}

	@Override
	public void sessionProgressChanged(SessionProgressCounter progress) {
		String text = "Korrekt: " + progress.correct() + "\nFalsch: "
				+ progress.incorrect() + "\nOffen: "
				+ (progress.remaining()-progress.correct()-progress.incorrect());
		progressArea.setText(text);
	}


	@Override
	public void updateCardStats(LearnStat stats) {
		String text = "";
		if (stats != null) {
		text = "Zuletzt gespielt: " + stats.getLastPlayed()
			+ "\nLevel: " + stats.getCurrentLevel()
			+ "\nFalsch beantwortet: " + stats.getWrongCount();
		}
		cardHistoryArea.setText(text);
	}
	
	@Override
	public Pane asPane() {
		return this;
	}
}

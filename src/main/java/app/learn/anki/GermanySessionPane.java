package app.learn.anki;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.learn.MapService;
import app.learn.ShapeMapPane;
import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.LearnStat;
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


/**
 * UI for the Germany quiz session. Sets its own background from Skin.
 * Size is determined by MainWindow's contentPane.
 * 
 * This pane can be recreated during skin changes (via presenter.refresh()).
 * The Presenter handles state preservation across recreations.
 */
public class GermanySessionPane extends Pane implements SessionPane {
	private static final Deck DECKTYPE = Deck.GERMANY_CARDS; 

	private final SessionPresenter presenter;
    private TextField textInputField;
    private SessionInfoLabel questionArea;
    private SessionInfoLabel progressArea;
    private SessionInfoLabel cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImagePane imageComponent;
    private ShapeMapPane deutschlandkarte;

    public GermanySessionPane(SessionPresenter presenter) {
        this.presenter = presenter;
        this.setBackground(new Background(SkinService.get().getBackgroundImage(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString())));
        initUI();
    }

    private void initUI() {
        Skin skin = SkinService.get();
        
        GeoMap map = MapService.getInstance().getMap(DECKTYPE);  // holt sich die SessionPane selbst (sie ist learn)
        deutschlandkarte = new ShapeMapPane(map, DECKTYPE.getMapName(), DECKTYPE.getCategory().toString());
        getChildren().add(deutschlandkarte.getView());
    	deutschlandkarte.setListener(id -> mapElementClicked(id));
    	deutschlandkarte.moveAllToActive();
    	
    	
    	questionArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.QUESTION);
    	questionArea.setText(""); // Initial leer
        getChildren().add(questionArea);
    	
        textInputField = skin.createInputField(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString());
        textInputField.setOnKeyReleased(_ -> textInputChanged());
        getChildren().add(textInputField);
    	
    	imageComponent = skin.createImageComponent(DECKTYPE.getId(), DECKTYPE.getCategory().toString());
    	getChildren().add(imageComponent); 
    	
    	mcPane = skin.createMultipleChoicePane(DECKTYPE.getId(), DECKTYPE.getCategory().toString());
    	mcPane.addListener(
    		new Consumer<Integer>() {
				@Override
				public void accept(Integer i) {
					onAnswerSelected(i);
				}
    		}
    	);
    	getChildren().add(mcPane);
    	
    	progressArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.PROGRESS);
    	progressArea.setText("");
    	getChildren().add(progressArea);
    	
    	cardHistoryArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	getChildren().add(cardHistoryArea);
    	
    	backButton = skin.createIconButton(DECKTYPE.getId(), Skin.IconButtonType.BACK);
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
    	//TODO: Es wäre nice, ein "Bild fehlt" einzublenden, wenn das Bild nicht gefunden werden kann und nicht abzubrechen! Oder?
        imageComponent.setImage(imagePath);
    }
    
    // Multiple Choice
    
	public void setMultipleChoice(List<String> answers) {
		mcPane.initiateMultipleChoice(answers);
	}
	
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
    	deutschlandkarte.moveAllToActive();
    }
	
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
    
    public void setMapActive(boolean active) {
    	deutschlandkarte.setInteractive(active);
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
				+ (progress.total()-progress.correct()-progress.incorrect());
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
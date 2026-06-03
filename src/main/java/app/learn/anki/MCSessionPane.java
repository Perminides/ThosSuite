package app.learn.anki;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.learn.model.LearnStat;
import app.learn.model.SessionProgressCounter;
import app.shared.ImagePane;
import app.shared.MultipleChoicePane;
import app.shared.SessionInfoLabel;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class MCSessionPane extends Pane implements SessionPane{
	private static final Deck DECKTYPE = Deck.MC_CARDS; 

	private final SessionPresenter presenter;
    private SessionInfoLabel questionArea;
    private SessionInfoLabel progressArea;
    private SessionInfoLabel cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImagePane imageComponent;

    public MCSessionPane (SessionPresenter presenter) {
        this.presenter = presenter;
        this.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));
        initUI();
    }
    
    private void initUI() {
    	Skin skin = SkinService.get();
    	
    	questionArea = skin.createSessionInfoLabel(DECKTYPE.getMapName(), DECKTYPE.getCategory().toString(), Skin.TextLabelType.QUESTION);
    	questionArea.setText(""); // Initial leer
        getChildren().add(questionArea);
    	
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
    	imageComponent.setImage(imagePath);
    }
    
    // Multiple Choice
    
	public void setMultipleChoice(List<String> answers) {
		mcPane.initiateMultipleChoice(answers);
	}
	
	/**
	 * Setzt aktive Buttons auf inaktiv
	 * @param active
	 */
	public void setMCPanelActive(boolean active) {
		/**if (active)
			throw new RuntimeException("Das habe ich nicht vorhergesehen.");
		else
			mcPanel.inactivateAllActiveButtons();**/
	}
	
	/**
	 * Setzt alle Buttons auf disabled. Für wenn MC gerade überhaupt nicht gebraucht wird...
	 */
	public void disableMcPanel() {
		/**mcPanel.disableAllButtons();**/
	}
	
    public void setMcCorrect(int id, boolean correct) {
    	mcPane.setCorrect(id, correct);
    }
    
	public void setMcSolution(Set<Integer> correctIds) {
		mcPane.setCorrectAndInactive(correctIds);
	}

	public void onAnswerSelected(int index) {
		presenter.clickedMCAnswer(index);
	}
	
	private void backButtonClicked() {
	    presenter.clickedBack();
	}

	// !Architektur Wenn das jetzt jedes Panel für sich implementiert, dann doch besser ins Interface? Aber das hat keinen Zugriff auf progressArea...
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

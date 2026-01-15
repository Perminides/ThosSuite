package app.ui.panes;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.components.SessionInfoLabel;
import app.ui.components.ImagePane;
import app.ui.components.MultipleChoicePane;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class MCSessionPane extends Pane implements AnkiSessionPane{
	private static final DeckType DECKTYPE = DeckType.MC_CARDS; 

	private final AnkiSessionPresenter presenter;
    private SessionInfoLabel questionArea;
    private SessionInfoLabel progressArea;
    private SessionInfoLabel cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImagePane imageComponent;

    public MCSessionPane (AnkiSessionPresenter presenter) {
        this.presenter = presenter;
        this.setBackground(new Background(SkinService.get().getBackgroundImage(DECKTYPE)));
        initUI();
    }
    
    private void initUI() {
    	Skin skin = SkinService.get();
    	
    	questionArea = skin.createSessionInfoLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText(""); // Initial leer
        getChildren().add(questionArea);
    	
        imageComponent = skin.createImageComponent(DECKTYPE);
    	getChildren().add(imageComponent);
    	
    	mcPane = skin.createMultipleChoicePane(DECKTYPE);
    	mcPane.addListener(
        		new Consumer<Integer>() {
    				@Override
    				public void accept(Integer i) {
    					onAnswerSelected(i);
    				}
        		}
        	);
    	getChildren().add(mcPane);
    	
    	progressArea = skin.createSessionInfoLabel(DECKTYPE, Skin.TextLabelType.PROGRESS);
    	progressArea.setText("");
    	getChildren().add(progressArea);
    	
    	cardHistoryArea = skin.createSessionInfoLabel(DECKTYPE, Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	getChildren().add(cardHistoryArea);
    	
    	backButton = skin.createIconButton(DECKTYPE, Skin.IconButtonType.BACK);
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
	public void sessionProgressChanged(SessionProgress progress) {
		String text = "Korrekt: " + progress.correct() + "\nFalsch: "
				+ progress.incorrect() + "\nOffen: "
				+ (progress.details().size()-progress.correct()-progress.incorrect());
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

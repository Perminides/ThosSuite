package app.ui.panels;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.MainWindow;
import app.ui.components.BackgroundPanel;
import app.ui.components.CustomButtonLabel;
import app.ui.components.CustomButtonLabel.CLBState;
import app.ui.components.CustomTextLabel;
import app.ui.components.MultipleChoicePanel;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;

public class MCSessionPanel extends JPanel implements AnkiSessionPanel{
	private static final long serialVersionUID = 1L;
	private static final DeckType DECKTYPE = DeckType.MC_CARDS; 

	private final MainWindow mainWindow;
	private final AnkiSessionPresenter presenter;
    private Skin skin;
    private CustomTextLabel questionArea;
    private CustomTextLabel progressArea;
    private CustomTextLabel cardHistoryArea;
    private MultipleChoicePanel mcPanel;
    private CustomButtonLabel backButton;
    private JLabel imageLabel;

    public MCSessionPanel (MainWindow mainWindow, AnkiSessionPresenter presenter) {
        this.mainWindow = mainWindow;
        this.presenter = presenter;
        this.skin = SkinService.get();
        setLayout(null);
        setOpaque(false);
        setSize(skin.getContentSize());
        initUI();
    }
    
    private void initUI() {
    	
    	questionArea = skin.createTextLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText("");
    	add(questionArea);
    	
    	imageLabel = skin.createImageLabel(DECKTYPE);
    	add(imageLabel);
    	
    	mcPanel = skin.createMultipleChoicePanel(DECKTYPE);
    	mcPanel.addListener(
        		new Consumer<Integer>() {
    				@Override
    				public void accept(Integer i) {
    					onAnswerSelected(i);
    				}
        		}
        	);
    	mcPanel.disableAllButtons();
    	add(mcPanel);
    	
    	progressArea = skin.createTextLabel(DECKTYPE, Skin.TextLabelType.PROGRESS);
    	progressArea.setText("");
    	add(progressArea);
    	
    	cardHistoryArea = skin.createTextLabel(DECKTYPE, Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	add(cardHistoryArea);
    	
    	backButton = skin.createIconButton(DECKTYPE, Skin.IconButtonType.BACK);
    	backButton.addMouseListener(new MouseInputAdapter() {
   		 @Override
            public void mousePressed(MouseEvent e) {
   			 	backButtonClicked();
            }
		});
    	add(backButton);
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
    	imageLabel.setIcon(imagePath == null ? null : new ImageIcon(imagePath));
    }
    
    // Multiple Choice
    
	public void setMultipleChoice(List<String> answers) {
		mcPanel.initiateMultipleChoice(answers);
	}
	
	/**
	 * Setzt aktive Buttons auf inaktiv
	 * @param active
	 */
	public void setMCPanelActive(boolean active) {
		if (active)
			throw new RuntimeException("Das habe ich nicht vorhergesehen.");
		else
			mcPanel.inactivateAllActiveButtons();
	}
	
	/**
	 * Setzt alle Buttons auf disabled. Für wenn MC gerade überhaupt nicht gebraucht wird...
	 */
	public void disableMcPanel() {
		mcPanel.disableAllButtons();
	}
	
    public void setMcCorrect(int id, boolean correct) {
    	if (correct)
    		mcPanel.setState(id, CLBState.CORRECT);
    	else
    		mcPanel.setState(id, CLBState.INCORRECT);
    }
    
	public void setMcSolution(Set<Integer> correctIds) {
		for (int id : correctIds) {
			mcPanel.setState(id, CLBState.CORRECT);
		}
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
		String text = "<p>Korrekt: " + progress.correct() + "<br/>Falsch: "
				+ progress.incorrect() + "<br/>Offen: "
				+ (progress.details().size()-progress.correct()-progress.incorrect());
		progressArea.setText(text);
	}

	@Override
	public void updateCardStats(LearnStat stats) {
		String text = "";
		if (stats != null) {
		text = "Zuletzt gespielt: " + stats.getLastPlayed()
			+ "<br/>Level: " + stats.getCurrentLevel()
			+ "<br/>Falsch beantwortet: " + stats.getWrongCount();
		}
		cardHistoryArea.setText(text);
	}

	// Claude zu der Duplizierung der Methode in den SessionPanels: Lass es wie es ist. Die Duplizierung ist minimal, die Methode ist stabil, und du hältst deine Kapselung sauber. Das ist pragmatischer als die Sichtbarkeit zu opfern oder eine Basisklasse einzuführen.
    @Override
    public void show() { /**
    	BackgroundPanel bg = SkinService.get().createBackgroundPanel(DECKTYPE);
    	bg.add(this);
        mainWindow.showPanel(bg);
        revalidate();
        repaint();**/
    }
}

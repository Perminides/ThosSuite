package app.ui.panels;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.MainWindow;
import app.ui.MapElementListener;
import app.ui.components.BackgroundPanel;
import app.ui.components.CustomButtonLabel;
import app.ui.components.CustomButtonLabel.CLBState;
import app.ui.components.CustomTextLabel;
import app.ui.components.ImageMapPanel;
import app.ui.components.MultipleChoicePanel;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;

public class WorldSessionPanel extends JPanel implements AnkiSessionPanel {
	private static final long serialVersionUID = 1L;
	private static final DeckType DECKTYPE = DeckType.WORLD_CARDS; 

	private final MainWindow mainWindow;
	private final AnkiSessionPresenter presenter;
    private JTextField textInputField;
    private CustomTextLabel questionArea;
    private CustomTextLabel progressArea;
    private CustomTextLabel cardHistoryArea;
    private MultipleChoicePanel mcPanel;
    private CustomButtonLabel backButton;
    private ImageMapPanel weltkarte;
    private JLabel imageLabel;

    public WorldSessionPanel(MainWindow mainWindow, AnkiSessionPresenter presenter) {
        this.mainWindow = mainWindow;
        this.presenter = presenter;
        setLayout(null);
        setOpaque(false);
        setSize(SkinService.get().getContentSize());
        initUI();
    }
    
    @Override
    public void show() {/**
    	BackgroundPanel bg = SkinService.get().createBackgroundPanel(DECKTYPE);
    	bg.add(this);
        mainWindow.showPanel(bg);
        revalidate();
        repaint();**/
    }    

    private void initUI() {
        Skin skin = SkinService.get();
        weltkarte = skin.createImageMapPanel(DECKTYPE);
    	weltkarte.setListener(new MapElementListener() {
    	    @Override
    	    public void mouseClicked(String id) {
    	        mapElementClicked(id);  // ← Über Helper
    	    }
    	});
    	add(weltkarte);
    	
    	questionArea = skin.createTextLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText("");
    	add(questionArea);
    	
    	textInputField = skin.createInputField(DECKTYPE);
    	textInputField.setEnabled(false);
    	textInputField.addKeyListener(new KeyAdapter() {
    		@Override
    		public void keyReleased(KeyEvent e) {
    			textInputChanged();
    		}
    		
		});
    	add(textInputField);
    	
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
	 * Setzt aktive Buttons auf inaktiv. Nach falschem Klick z.B.
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
	
	// Map
	
    public void resetMarkers() {
    	weltkarte.resetMarkers();
    }
    
    public void setMapActive(boolean active) {
    	weltkarte.setActive(active);
    }
    
	@Override
	public void setIdsInQuestion(Set<String> ids) {
		weltkarte.setToCheckShapes(ids);
	}
	
	@Override
	public void setMarkedIds(Set<String> ids) {
		weltkarte.setMarked(ids);
	}
	
    @Override
	public void addIdsToCorrect(Set<String> elements) {
		weltkarte.addToCorrect(elements);
    }
    
    @Override
	public void setIdToIncorrect(String id) { // Die id ist eh null, braucht uns nicht zu interessieren.
		weltkarte.resetMarkers(); // Wo war die EM 2021? Alle Länder werden grün. Wer gewann? Wenn ich falsch klicke und jetzt eins der 10 nochmal grün wird sehe ich nicht, welches!
    	weltkarte.markLastClickAsIncorrect();
    }
    
    // Input
    
	public void setTextFieldActive(boolean active) {
		if (active) {
			textInputField.setText("");
			textInputField.setEnabled(true);
			textInputField.requestFocusInWindow();
		} else {
			textInputField.setEnabled(false);
			// Text bleibt stehen (z.B. für Lösungs-Anzeige)
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
	
	/**private void missedAllShapes() {
		presenter.mapClicked();
	}**/
	
	private void backButtonClicked() {
	    presenter.clickedBack();
	}

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
}

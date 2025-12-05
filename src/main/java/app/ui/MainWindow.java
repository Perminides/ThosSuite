package app.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import app.data.CardSortOrder;
import app.data.LearnSessionInfo;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;

public class MainWindow extends JFrame {

    /**
	 * Zeigt immer ein BackgroundPanel an, welches entweder ein SessionPanel beherbergt oder halt auch nicht.
	 */
	private static final long serialVersionUID = 1L;
    private JMenuBar myMenuBar = null;
    private JMenu menuLearn = null;
    private JMenu menuOptions = null;
    private JMenu menuSort = null;
    private JMenuItem itemSave = null;
    
    private Consumer<LearnSessionInfo> onSessionSelected = null;
    private Consumer<CardSortOrder> onSortSelected = null;
    private Consumer<Skin> onNewSkinSelected = null;
    private Runnable onSaveSelected = null;
    private Runnable onEscPressed = null;
    private Runnable onPausePressed = null;
    private List<LearnSessionInfo> todaysLearnSessions;

    public MainWindow() {
        super("Thos Suite");

        initKeyBindings();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        createMenuBar();
        setResizable(false);
    }
    
    public void createMenuBar() {
    	Skin skin = SkinService.get();
    	skin.redecorateMainWindow(this);
    	if (myMenuBar != null)
    		remove(myMenuBar);
    	myMenuBar = skin.createJMenuBar();
    	JMenu menuFile = skin.createJMenu("Datei");
    	itemSave = skin.createJMenuItem("Speichern und beenden");
    	itemSave.addActionListener(_ -> onSaveSelected.run());
    	itemSave.setEnabled(false);
    	menuFile.add(itemSave);
    	
    	menuSort = skin.createJMenu("Anzeigereihenfolge");
    	JMenuItem item = null;
    	for (CardSortOrder order : CardSortOrder.values()) {
    		item = skin.createJMenuItem(order.getDisplayName());
    		if (order == CardSortOrder.BY_WRONG_COUNT_DESC) // !Später: Wenn aus config gelesen wird, dann natürlich anders...
    			item.setEnabled(false);
    		item.addActionListener(e -> {
    			onSortSelected.accept(order);
    			for (java.awt.Component comp : menuSort.getMenuComponents()) {
    				comp.setEnabled(comp != e.getSource()); 
    			}
    		});
    		menuSort.add(item);
    	}
    	menuOptions = skin.createJMenu("Optionen");
    	menuOptions.add(menuSort);
    	
    	menuLearn = skin.createJMenu("Lernen");
    	if (todaysLearnSessions != null)
    		setLearnItems();
    	
    	JMenu menuView = skin.createJMenu("Ansicht");

    	List<JMenuItem> skinMenuItems = new ArrayList<>();
    	Skin currentSkin = SkinService.get();

    	for (Skin availableSkin : SkinService.getAllSkins()) {
    	    String displayName = availableSkin.getDisplayName();
    	    
    	    // Checkmark wenn aktueller Skin
    	    boolean isCurrentSkin = availableSkin.getClass() == currentSkin.getClass();
    	    String menuText = (isCurrentSkin ? "✓ " : "  ") + displayName;
    	    
    	    item = skin.createJMenuItem(menuText);
    	    item.addActionListener(_ -> {
    	    	if (availableSkin == currentSkin) return;
    	    	onNewSkinSelected.accept(availableSkin);
    	    });
    	    
    	    skinMenuItems.add(item);
    	    menuView.add(item);
    	}
    	
    	myMenuBar.add(menuFile);
    	myMenuBar.add(menuOptions);
        myMenuBar.add(menuLearn);
        myMenuBar.add(menuView);
        myMenuBar.setOpaque(true);        
        setJMenuBar(myMenuBar);
        pack(); // Falls die Schriftgröße sich geändert hat, wird das Menu höher und soll ja keinen Platz vom Spielfeld klauen, deswegen hier das pack
        revalidate();
        repaint();
    }
    
    public void updateLearnItems(List<LearnSessionInfo> infoList) {
        menuLearn.removeAll();
        addLearnItems(infoList);
        menuLearn.revalidate();
    }
    
    public void setLearnItems(List<LearnSessionInfo> infoList) {
    	todaysLearnSessions = infoList;
    	setLearnItems();
    }
    
    public void addLearnItems(List<LearnSessionInfo> infoList) {
    	todaysLearnSessions.addAll(infoList);
    	setLearnItems();
    }
    
    public void setSaveRunnable(Runnable action) {
    	this.onSaveSelected  = action;
    }
    
    public void setLearnSessionConsumer(Consumer<LearnSessionInfo> consumer) {
    	this.onSessionSelected  = consumer;
    }
    
    public void setSkinChangeConsumer(Consumer<Skin> consumer) {
    	this.onNewSkinSelected  = consumer;
    }
    
    public void setSortConsumer(Consumer<CardSortOrder> consumer) {
    	this.onSortSelected  = consumer;
    }
    
    public void setEscPressedRunnable(Runnable action) {
        this.onEscPressed = action;
    }

    public void setPausePressedRunnable(Runnable action) {
        this.onPausePressed = action;
    }
    
    public void showPanel(JPanel panel) {
    	getContentPane().removeAll();
    	add(panel, BorderLayout.CENTER);
    	pack();
        revalidate();
        repaint();
    }

	public void showSaveSession(boolean b) {
		itemSave.setEnabled(b);
	}
	
	private void setLearnItems() {
		menuLearn.removeAll();
		for (LearnSessionInfo info : todaysLearnSessions) {
    		JMenuItem item = new JMenuItem(info.formatForMenu());
    		if (!info.isStillDueToday())
    			item.setEnabled(false);
    		else
    			item.addActionListener(_ -> onSessionSelected.accept(info));
    		menuLearn.add(item);
    	}

	}
    
    private void initKeyBindings() {
        JRootPane root = getRootPane();

        // ESC
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "escPressed");
        root.getActionMap().put("escPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onEscPressed != null)
                    onEscPressed.run();
            }
        });

        // PAUSE
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("PAUSE"), "pausePressed");
        root.getActionMap().put("pausePressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onPausePressed != null)
                    onPausePressed.run();
            }
        });
    }
}

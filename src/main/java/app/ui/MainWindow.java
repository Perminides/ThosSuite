package app.ui;

import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;

import app.data.CardSortOrder;
import app.data.LearnSessionInfo;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainWindow extends Stage {

    private MenuItem itemSave = null;
    private Menu menuLearn = null;
    private Menu menuOptions = null;
    private Menu menuSort = null;
    private List<LearnSessionInfo> todaysLearnSessions;
    
    private Consumer<LearnSessionInfo> onSessionSelected = null;
    private Consumer<CardSortOrder> onSortSelected = null;
    private Consumer<Skin> onNewSkinSelected = null;
    private Runnable onSaveSelected = null;
    private Runnable onEscPressed = null;
    private Runnable onPausePressed = null;
    
    private HeaderBar headerBar;
    private BorderPane root;
    private Pane contentPane;

    public MainWindow() {
        initStyle(StageStyle.EXTENDED);
        setTitle("Thos Suite");
        setResizable(false);
        
        Skin skin = SkinService.get();
        
        // Alles vom Skin holen - kein Styling hier!
        headerBar = skin.createHeaderBar();
        headerBar.getStyleClass().add("thorstens-bar");  // Style-Class setzen!

        contentPane = skin.createContentPane();
        contentPane.getStyleClass().add("thorstens-pane");
        
        root = new BorderPane();
        root.getStyleClass().add("thorstens-root");
        root.setTop(headerBar);
        root.setCenter(contentPane);
        
        Scene scene = new Scene(root);
        skin.styleScene(scene);
        setScene(scene);
        Platform.runLater(() -> {
            double headerHeight = headerBar.getHeight();
            HeaderBar.setPrefButtonHeight(this, headerHeight);
            System.out.println("HeaderBar height: " + headerHeight);
        });
        initKeyBindings();
    }
    
    public void createMenuBar() {
        Skin skin = SkinService.get();
        
        MenuBar menuBar = skin.createMenuBar();
	    HeaderBar.setDragType(menuBar, HeaderDragType.DRAGGABLE_SUBTREE);
        
        // DATEI-MENÜ
        Menu menuFile = skin.createMenu("Datei");
        itemSave = skin.createMenuItem("Speichern und beenden");
        itemSave.setOnAction(_ -> onSaveSelected.run());
        itemSave.setDisable(true);
        menuFile.getItems().add(itemSave);
        
        // OPTIONEN-MENÜ
        menuOptions = skin.createMenu("Optionen");
        menuSort = skin.createMenu("Anzeigereihenfolge");
        
        for (CardSortOrder order : CardSortOrder.values()) {
            MenuItem item = skin.createMenuItem(order.getDisplayName());
            if (order == CardSortOrder.BY_WRONG_COUNT_DESC) {
                item.setDisable(true);
            }
            item.setOnAction(e -> {
                onSortSelected.accept(order);
                // Alle anderen enablen, dieses disablen
                for (MenuItem menuItem : menuSort.getItems()) {
                    menuItem.setDisable(menuItem == e.getSource());
                }
            });
            menuSort.getItems().add(item);
        }
        menuOptions.getItems().add(menuSort);
        
        // LERNEN-MENÜ
        menuLearn = skin.createMenu("Lernen");
        if (todaysLearnSessions != null) {
            updateLearnMenuItems();
        }
        
        // ANSICHT-MENÜ
        Menu menuView = skin.createMenu("Ansicht");
        Skin currentSkin = SkinService.get();
        
        for (Skin availableSkin : SkinService.getAllSkins()) {
            String displayName = availableSkin.getDisplayName();
            boolean isCurrentSkin = availableSkin.getClass() == currentSkin.getClass();
            String menuText = (isCurrentSkin ? "✓ " : "  ") + displayName;
            
            MenuItem item = skin.createMenuItem(menuText);
            item.setOnAction(_ -> {
                if (availableSkin == currentSkin) return;
                onNewSkinSelected.accept(availableSkin);
            });
            menuView.getItems().add(item);
        }
        
        // Menüs zur MenuBar hinzufügen
        menuBar.getMenus().addAll(menuFile, menuOptions, menuLearn, menuView);
        
     // HeaderBar komplett vom Skin aufbauen lassen
        skin.setupHeaderBar(headerBar, this, menuBar);
        
        headerBar.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("HeaderBar height changed: " + oldVal + " -> " + newVal);
            HeaderBar.setPrefButtonHeight(this, newVal.doubleValue());
        });
    }
    
    private void updateLearnMenuItems() {
        menuLearn.getItems().clear();
        for (LearnSessionInfo info : todaysLearnSessions) {
            MenuItem item = new MenuItem(info.formatForMenu());
            if (!info.isStillDueToday()) {
                item.setDisable(true);
            } else {
                item.setOnAction(_ -> onSessionSelected.accept(info));
            }
            menuLearn.getItems().add(item);
        }
    }
    
    public void setLearnItems(List<LearnSessionInfo> infoList) {
        todaysLearnSessions = infoList;
        updateLearnMenuItems();
    }
    
    public void addLearnItems(List<LearnSessionInfo> infoList) {
        todaysLearnSessions.addAll(infoList);
        updateLearnMenuItems();
    }
    
    public void updateLearnItems(List<LearnSessionInfo> infoList) {
        todaysLearnSessions = infoList;
        updateLearnMenuItems();
    }
    
    public void showSaveSession(boolean enabled) {
        itemSave.setDisable(!enabled);
    }
    
    public void showPanel(JPanel panel) {
        // TODO: Panel-System später
        System.out.println("TODO: showPanel() - " + panel.getClass().getSimpleName());
    }
    
    private void initKeyBindings() {
        // TODO: ESC und PAUSE KeyBindings
        getScene().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    if (onEscPressed != null) onEscPressed.run();
                    break;
                case PAUSE:
                    if (onPausePressed != null) onPausePressed.run();
                    break;
                default:
                    break;
            }
        });
    }
    
    // Callback Setter
    public void setSaveRunnable(Runnable action) {
        this.onSaveSelected = action;
    }
    
    public void setLearnSessionConsumer(Consumer<LearnSessionInfo> consumer) {
        this.onSessionSelected = consumer;
    }
    
    public void setSkinChangeConsumer(Consumer<Skin> consumer) {
        this.onNewSkinSelected = consumer;
    }
    
    public void setSortConsumer(Consumer<CardSortOrder> consumer) {
        this.onSortSelected = consumer;
    }
    
    public void setEscPressedRunnable(Runnable action) {
        this.onEscPressed = action;
    }

    public void setPausePressedRunnable(Runnable action) {
        this.onPausePressed = action;
    }
    
    // Helper
    private Color toFXColor(java.awt.Color awtColor) {
        return Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
    }
}
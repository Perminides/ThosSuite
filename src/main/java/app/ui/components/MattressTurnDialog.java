package app.ui.components;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import app.config.Config;
import app.data.persistence.MattressRepository;
import app.data.persistence.MattressRepository.MattressTurn;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class MattressTurnDialog {

    private static final String DIR_UP_DOWN    = "up down";
    private static final String DIR_RIGHT_LEFT = "right left";

    private static final String IMG_UP_DOWN    = "Bett vertikal small.png";
    private static final String IMG_RIGHT_LEFT = "Bett horizontal small.png";

    private final MattressRepository repository = new MattressRepository();

    public void showIfDue() {
        MattressTurn last = repository.getLastTurn();

        if (last != null) {
            long weeksSince = ChronoUnit.WEEKS.between(last.turnedAt(), LocalDateTime.now());
            if (weeksSince < Config.getInt("mattress.dueAfterWeeks", 4)) {
                return;
            }
        }

        show();
    }
    
    public void show() {
    	MattressTurn last = repository.getLastTurn();
    	String nextDirection = (last == null || last.direction().equals(DIR_UP_DOWN))
                ? DIR_RIGHT_LEFT
                : DIR_UP_DOWN;

        show(nextDirection);
    }

    private void show(String suggestedDirection) {
        Window owner = SkinService.getOwnerWindow();

        @SuppressWarnings("unchecked")
        Dialog<Integer> dialog = (Dialog<Integer>) SkinService.get().createDialog(owner, "Matratze");

        int[] result = { -1 };

        // X-Button: interpretieren als "Mache ich spaeter"
        dialog.getDialogPane().getScene().getWindow().addEventFilter(
            WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                e.consume();
                result[0] = 2;
                dialog.setResult(2);
            });

        dialog.getDialogPane().setContent(buildContent(dialog, suggestedDirection, result));

        ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        var buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setMinHeight(0);
            buttonBar.setPrefHeight(0);
            buttonBar.setMaxHeight(0);
            var cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
            if (cancelBtn != null) {
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
            }
        }

        dialog.showAndWait();

        String otherDirection = suggestedDirection.equals(DIR_UP_DOWN) ? DIR_RIGHT_LEFT : DIR_UP_DOWN;

        switch (result[0]) {
            case 0 -> repository.save(LocalDateTime.now(), suggestedDirection);
            case 1 -> repository.save(LocalDateTime.now(), otherDirection);
            case 2 -> Log.debug(this, "Matratze wenden auf später verschoben.");
        }
    }

    private VBox buildContent(Dialog<Integer> dialog, String direction, int[] result) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        try {
            String imgPath = Config.get("miscImageFolder") + (direction.equals(DIR_UP_DOWN) ? IMG_UP_DOWN : IMG_RIGHT_LEFT);
            Image img = new Image(Path.of(imgPath).toUri().toString());
            imageView.setImage(SkinService.get().tintImageWithTextColor(img));
        } catch (Exception e) {
            throw new RuntimeException("Häh? Wo ist das Bild mit der Matratze?", e);
        }

        Button btnDone  = new Button("Habe ich getan");
        Button btnOther = new Button("Andere Richtung genommen");
        Button btnLater = new Button("Mache ich später");

        btnDone.setOnAction(_ -> { result[0] = 0; dialog.setResult(0); });
        btnOther.setOnAction(_ -> { result[0] = 1; dialog.setResult(1); });
        btnLater.setOnAction(_ -> { result[0] = 2; dialog.setResult(2); });

        HBox buttons = new HBox(10, btnDone, btnOther, btnLater);
        buttons.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(20, imageView, buttons);
        vbox.setAlignment(Pos.CENTER);
        return vbox;
    }
}
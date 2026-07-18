package app.mattress;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import app.mattress.model.MattressTurn;
import app.mattress.repository.MattressRepository;
import app.shared.Config;
import app.shared.Log;
import app.shared.model.AlertOptions;
import app.shared.model.ButtonEnum;
import app.shared.skin.SkinService;

public class TurnDialog {

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
        String suggested = (last == null || last.direction().equals(DIR_UP_DOWN))
                ? DIR_RIGHT_LEFT
                : DIR_UP_DOWN;
        String other = suggested.equals(DIR_UP_DOWN) ? DIR_RIGHT_LEFT : DIR_UP_DOWN;

        String imgName = suggested.equals(DIR_UP_DOWN) ? IMG_UP_DOWN : IMG_RIGHT_LEFT;
        Path image = Config.getPath("miscImageFolder").resolve(imgName);

        ButtonEnum answer = SkinService.get().showAlert("Matratze", "", new AlertOptions().image(image),
        	      ButtonEnum.DONE, ButtonEnum.OTHER_DIRECTION, ButtonEnum.LATER);

        switch (answer) {
            case DONE            -> repository.save(LocalDateTime.now(), suggested);
            case OTHER_DIRECTION -> repository.save(LocalDateTime.now(), other);
            case LATER, CANCEL   -> Log.debug(this, "Matratze wenden auf später verschoben.");
            default              -> throw new IllegalStateException("Unerwarteter DialogButton: " + answer);
        }
    }
}
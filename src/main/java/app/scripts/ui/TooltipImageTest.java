package app.scripts.ui;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TooltipImageTest extends Application {

    private static final String THUMBNAIL_PATH = "file:C:/Users/Markgraf/Desktop/DiaryAttachments/diary/thumbnails/2026-03-30%2019_46_27-Diät.xlsx%20-%20Excel.png";
    private static final String ORIGINAL_PATH  = "file:C:/Users/Markgraf/Pictures/Privat/Sex%20Unsortiert/mehmet2.JPG";
    private static final int MARGIN = 20;

    @Override
    public void start(Stage stage) {
        ImageView thumbnail = new ImageView(new Image(THUMBNAIL_PATH, -1, 120, true, true));

        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.millis(300));
        tooltip.setShowDuration(Duration.INDEFINITE);
        tooltip.setHideDelay(Duration.ZERO);
        tooltip.setAutoFix(false);
        tooltip.setStyle("-fx-padding: 0;");

        thumbnail.setOnMouseEntered(e -> {
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double mouseX = e.getScreenX();
            double mouseY = e.getScreenY();

            System.out.println("=== HOVER EVENT ===");
            System.out.printf("  Bildschirm: minX=%.0f maxX=%.0f minY=%.0f maxY=%.0f%n",
                    screen.getMinX(), screen.getMaxX(), screen.getMinY(), screen.getMaxY());
            System.out.printf("  Mausposition: screenX=%.0f screenY=%.0f%n", mouseX, mouseY);

            // Bild einmal laden — naturalW/naturalH ablesen, kein zweites Laden mehr
            Image original = new Image(ORIGINAL_PATH);
            double naturalW = original.getWidth();
            double naturalH = original.getHeight();
            double aspectRatio = naturalW / naturalH;
            System.out.printf("  Natürliche Bildgröße: naturalW=%.0f  naturalH=%.0f  aspect=%.3f%n",
                    naturalW, naturalH, aspectRatio);

            // --- Schritt 1: Gewinner links/rechts (mehr Breite gewinnt) ---
            double leftW  = mouseX - screen.getMinX() - 2 * MARGIN;
            double rightW = screen.getMaxX() - mouseX - 2 * MARGIN;
            double hemiH  = screen.getHeight() - 2 * MARGIN;

            System.out.printf("  Hemisphären links/rechts: leftW=%.0f  rightW=%.0f  hemiH=%.0f%n",
                    leftW, rightW, hemiH);

            boolean useRight = rightW >= leftW;
            double hWinner_W = useRight ? rightW : leftW;
            System.out.printf("  Gewinner links/rechts: %s  (Breite=%.0f)%n",
                    useRight ? "rechts" : "links", hWinner_W);

            double hLR_imgW, hLR_imgH;
            if (naturalW <= hWinner_W && naturalH <= hemiH) {
                hLR_imgW = naturalW; hLR_imgH = naturalH;
            } else if (naturalW / hWinner_W >= naturalH / hemiH) {
                hLR_imgW = hWinner_W; hLR_imgH = hWinner_W / aspectRatio;
            } else {
                hLR_imgH = hemiH; hLR_imgW = hemiH * aspectRatio;
            }
            double areaLR = hLR_imgW * hLR_imgH;
            System.out.printf("  Bildfläche in links/rechts-Gewinner: imgW=%.0f imgH=%.0f area=%.0f%n",
                    hLR_imgW, hLR_imgH, areaLR);

            // --- Schritt 2: Gewinner oben/unten (mehr Höhe gewinnt) ---
            double topH  = mouseY - screen.getMinY() - 2 * MARGIN;
            double botH  = screen.getMaxY() - mouseY - 2 * MARGIN;
            double hemiW = screen.getWidth() - 2 * MARGIN;

            System.out.printf("  Hemisphären oben/unten: topH=%.0f  botH=%.0f  hemiW=%.0f%n",
                    topH, botH, hemiW);

            boolean useBottom = botH >= topH;
            double hWinner_H = useBottom ? botH : topH;
            System.out.printf("  Gewinner oben/unten: %s  (Höhe=%.0f)%n",
                    useBottom ? "unten" : "oben", hWinner_H);

            double hTB_imgW, hTB_imgH;
            if (naturalW <= hemiW && naturalH <= hWinner_H) {
                hTB_imgW = naturalW; hTB_imgH = naturalH;
            } else if (naturalW / hemiW >= naturalH / hWinner_H) {
                hTB_imgW = hemiW; hTB_imgH = hemiW / aspectRatio;
            } else {
                hTB_imgH = hWinner_H; hTB_imgW = hWinner_H * aspectRatio;
            }
            double areaTB = hTB_imgW * hTB_imgH;
            System.out.printf("  Bildfläche in oben/unten-Gewinner: imgW=%.0f imgH=%.0f area=%.0f%n",
                    hTB_imgW, hTB_imgH, areaTB);

            // --- Schritt 3: Finale Entscheidung ---
            double imgW, imgH, tooltipX, tooltipY;
            String hemisphere;

            if (areaLR >= areaTB) {
                imgW = hLR_imgW; imgH = hLR_imgH;
                if (useRight) {
                    hemisphere = "rechts";
                    tooltipX = mouseX + MARGIN;
                } else {
                    hemisphere = "links";
                    tooltipX = mouseX - MARGIN - imgW;
                }
                tooltipY = mouseY - imgH / 2.0;
                tooltipY = Math.max(screen.getMinY() + MARGIN, tooltipY);
                tooltipY = Math.min(screen.getMaxY() - MARGIN - imgH, tooltipY);
            } else {
                imgW = hTB_imgW; imgH = hTB_imgH;
                if (useBottom) {
                    hemisphere = "unten";
                    tooltipY = mouseY + MARGIN;
                } else {
                    hemisphere = "oben";
                    tooltipY = mouseY - MARGIN - imgH;
                }
                tooltipX = mouseX - imgW / 2.0;
                tooltipX = Math.max(screen.getMinX() + MARGIN, tooltipX);
                tooltipX = Math.min(screen.getMaxX() - MARGIN - imgW, tooltipX);
            }

            System.out.printf("  Gewählte Hemisphäre: %s%n", hemisphere);
            System.out.printf("  Angezeigte Bildgröße: imgW=%.0f  imgH=%.0f%n", imgW, imgH);
            System.out.printf("  Tooltip-Position: x=%.0f  y=%.0f%n", tooltipX, tooltipY);
            System.out.printf("  Erwartete Tooltip-Grenzen: links=%.0f  rechts=%.0f  oben=%.0f  unten=%.0f%n",
                    tooltipX, tooltipX + imgW, tooltipY, tooltipY + imgH);

            boolean overflowRight  = (tooltipX + imgW) > screen.getMaxX();
            boolean overflowLeft   = tooltipX < screen.getMinX();
            boolean overflowBottom = (tooltipY + imgH) > screen.getMaxY();
            boolean overflowTop    = tooltipY < screen.getMinY();
            if (overflowRight || overflowLeft || overflowBottom || overflowTop) {
                System.out.printf("  *** OVERFLOW: right=%b left=%b bottom=%b top=%b ***%n",
                        overflowRight, overflowLeft, overflowBottom, overflowTop);
            } else {
                System.out.println("  OK: kein Overflow erwartet");
            }

            // ImageView mit berechneter Zielgröße — original wird nicht nochmal von Platte geladen
            ImageView imageView = new ImageView(original);
            if (naturalW > imgW || naturalH > imgH) {
                imageView.setFitWidth(imgW);
                imageView.setFitHeight(imgH);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                System.out.println("  Bild via ImageView runterskaliert");
            } else {
                System.out.println("  Bild 1:1 angezeigt");
            }

            tooltip.setGraphic(imageView);
            tooltip.show(thumbnail, tooltipX, tooltipY);

            javafx.application.Platform.runLater(() -> {
                if (tooltip.getSkin() != null && tooltip.getSkin().getNode() != null) {
                    javafx.scene.Node skinNode = tooltip.getSkin().getNode();
                    javafx.stage.Window w = skinNode.getScene() != null ? skinNode.getScene().getWindow() : null;
                    if (w != null) {
                        System.out.printf("  [runLater] Tatsächliches Tooltip-Fenster: x=%.0f y=%.0f w=%.0f h=%.0f%n",
                                w.getX(), w.getY(), w.getWidth(), w.getHeight());
                        System.out.printf("  [runLater] Tooltip rechte Kante: %.0f  untere Kante: %.0f%n",
                                w.getX() + w.getWidth(), w.getY() + w.getHeight());
                    } else {
                        System.out.println("  [runLater] Tooltip-Fenster nicht erreichbar über Skin");
                    }
                }
            });
        });

        thumbnail.setOnMouseExited(_ -> tooltip.hide());

        StackPane root = new StackPane(thumbnail);
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("Hemisphären Image Test");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
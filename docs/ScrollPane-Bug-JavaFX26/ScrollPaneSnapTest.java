import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Reproducer for a regression introduced by JDK-8370652.
 *
 * ScrollPaneSkin.computePrefWidth/computePrefHeight now round their result (snapSpace) instead of
 * returning it unsnapped. Rounding down makes the ScrollPane's preferred size *smaller* than the
 * size its own content asked for. When the ScrollPane's preferred size determines the size of an
 * auto-sized window, the content is laid out one pixel too narrow. Content that grows taller when
 * it gets narrower - wrapped text - then overflows, and an AS_NEEDED scroll bar appears.
 *
 * The content here is a Region standing in for a wrapped Label: it reports a natural width with a
 * fractional part, and needs "one more line" as soon as it gets less. No fonts are involved, so
 * the result does not depend on platform text metrics.
 *
 * Run with any JDK that supports the JavaFX version under test:
 *   java --module-path <javafx-lib> --add-modules javafx.controls ScrollPaneSnapTest.java
 *
 * Output on JavaFX 25:  PART A ok   / PART B PASS (no scroll bar)
 * Output on JavaFX 26:  PART A FAIL / PART B FAIL (unwanted scroll bar)
 */
public class ScrollPaneSnapTest extends Application {

    /** Natural width of the content, with a fractional part below .5 - as real text usually has. */
    private static final double NATURAL_WIDTH = 300.2;

    private static final double PADDING = 10;

    /** Stands in for a wrapped Label: one pixel too little and it needs an extra line. */
    private static final class Content extends Region {
        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computePrefWidth(double height) {
            return NATURAL_WIDTH;
        }

        @Override
        protected double computePrefHeight(double width) {
            // Same contract as a wrapped Label: unconstrained means "at natural width" = one line.
            return width < 0 || width >= NATURAL_WIDTH ? 100 : 200;
        }
    }

    @Override
    public void start(Stage stage) {
        Content content = new Content();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(PADDING));
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // An auto-sized window: its size comes from the preferred size of the root.
        stage.setScene(new Scene(scrollPane));
        stage.sizeToScene();
        stage.show();

        Platform.runLater(() -> {
            report(stage, scrollPane, content);
            stage.close();
            Platform.exit();
        });
    }

    private void report(Stage stage, ScrollPane scrollPane, Content content) {
        Node vbar = scrollPane.lookup(".scroll-bar:vertical");
        Node viewport = scrollPane.lookup(".viewport");
        boolean barShown = vbar != null && vbar.isVisible();

        double insets = scrollPane.getInsets().getLeft() + scrollPane.getInsets().getRight();
        double needed = NATURAL_WIDTH + insets;
        double reported = scrollPane.prefWidth(-1);

        System.out.println("javafx.runtime.version   = " + System.getProperty("javafx.runtime.version"));
        System.out.println("render scale             = " + stage.getRenderScaleX());
        System.out.println();
        System.out.println("PART A - preferred width must cover the content");
        System.out.println("  content natural width  = " + NATURAL_WIDTH);
        System.out.println("  ScrollPane insets      = " + insets);
        System.out.println("  needed (content+insets)= " + needed);
        System.out.println("  ScrollPane.prefWidth   = " + reported);
        System.out.println("  " + (reported < needed
                ? "FAIL: preferred width is " + Math.round((needed - reported) * 1000) / 1000.0
                        + " px smaller than the content needs"
                : "ok: preferred width covers the content"));
        System.out.println();
        System.out.println("PART B - visible consequence");
        System.out.println("  ScrollPane width       = " + scrollPane.getWidth());
        System.out.println("  viewport width         = "
                + (viewport != null ? viewport.getLayoutBounds().getWidth() : "?"));
        System.out.println("  viewport height        = "
                + (viewport != null ? viewport.getLayoutBounds().getHeight() : "?"));
        System.out.println("  content height         = " + content.getHeight());
        System.out.println("  vertical scroll bar    = " + (barShown ? "SHOWN" : "not shown"));
        System.out.println("  " + (barShown ? "FAIL (unwanted scroll bar)" : "PASS"));
    }

    public static void main(String[] args) {
        // Keep the numbers readable and the result independent of the desktop's display scaling.
        // The regression is not caused by scaling; it only changes the exact values.
        System.setProperty("glass.win.uiScale", "1");
        System.setProperty("glass.gtk.uiScale", "1");
        System.setProperty("prism.allowhidpi", "false");
        launch(args);
    }
}

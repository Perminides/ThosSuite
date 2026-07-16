package app.shared.model;

import java.nio.file.Path;

public class AlertOptions {

    private boolean centered = false;
    private Dismiss dismiss  = Dismiss.NORMAL;
    private Path    image    = null;

    public AlertOptions centered()    { this.centered = true;           return this; }
    public AlertOptions noEsc()       { this.dismiss = Dismiss.NO_ESC;   return this; }
    public AlertOptions mandatory()   { this.dismiss = Dismiss.MANDATORY; return this; }
    public AlertOptions image(Path p) { this.image = p;                  return this; }

    public boolean isCentered() { return centered; }
    public Dismiss dismiss()  { return dismiss; }
    public Path    image()    { return image; }
}
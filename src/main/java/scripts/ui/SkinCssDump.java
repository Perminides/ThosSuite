package scripts.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import app.shared.Config;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;

/**
 * Schreibt das erzeugte Stylesheet jedes Skins nach {@code docs/skin/css}, je eine Datei.
 *
 * <p>Gedacht für den Umbau der Skin-Schicht. Ob eine Änderung die Darstellung angetastet hat, ist
 * von Hand nicht zu beantworten — sieben Skins mal ein Dutzend Screens mal Hover- und
 * Disabled-Zustände sieht kein Auge zuverlässig durch, und der Fehler, der durchrutscht, ist per
 * Definition der unauffällige. Am Ende mündet aber alles in <b>eine</b> Zeichenkette je Skin.</p>
 *
 * <p><b>Der Ablauf:</b> Nach einer Änderung an Skins, properties-Dateien oder der CSS-Erzeugung
 * dieses Werkzeug starten und {@code git diff docs/skin/css} lesen. Dort steht zeichengenau, was
 * sich am Stylesheet geändert hat — nichts, wenn es ein reiner Umbau war, und sonst genau die
 * Regeln, die gemeint waren. Die neuen Dateien gehören in denselben Commit wie die Änderung, die
 * sie verursacht hat. Das Vorher muss deshalb nirgends aufgehoben werden: Es ist der letzte
 * Commit.</p>
 *
 * <p>Verglichen wird bewusst nicht hier, sondern von Git. Eine eigene Vergleichslogik wäre Code,
 * der selbst falsch sein kann.</p>
 *
 * <p>Nicht zu verwechseln mit {@link CssInspector}: Der liest die <em>wirksamen</em> Styles eines
 * lebenden Node-Baums und braucht dafür ein Fenster. Hier geht es um das <em>erzeugte</em>
 * Stylesheet, und das gibt es ohne JavaFX-Toolkit — deshalb genügt ein gewöhnliches {@code main}.</p>
 *
 * <p>Läuft aus Eclipse heraus (Run As → Java Application). Das Arbeitsverzeichnis muss die
 * Projektwurzel sein, sonst schreibt es ins Leere; danach wird geprüft. Der Datenordner steht
 * hartcodiert und lässt sich als erstes Argument überschreiben.</p>
 */
public class SkinCssDump {

	private static final String DATA_FOLDER = "C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite/";
	private static final Path ZIEL = Path.of("docs/skin/css");

	public static void main(String[] args) throws IOException {
		if (!Files.isDirectory(Path.of("docs/skin")))
			throw new IllegalStateException("Ich finde docs/skin nicht. Das Arbeitsverzeichnis muss die"
					+ " Projektwurzel sein, ist aber: " + Path.of("").toAbsolutePath());

		Config.init(args.length > 0 ? args[0] : DATA_FOLDER);
		Files.createDirectories(ZIEL);

		// Die Skins der Registry, keine eigene Liste — sonst fiele ein neuer Skin still heraus.
		for (Skin skin : SkinService.getAllSkins()) {
			Path datei = ZIEL.resolve(skin.getClass().getSimpleName() + ".css");
			Files.writeString(datei, skin.buildCss(), StandardCharsets.UTF_8);
			System.out.println("geschrieben: " + datei);
		}

		System.out.println("\nWas sich geändert hat, zeigt: git diff docs/skin/css");
	}
}

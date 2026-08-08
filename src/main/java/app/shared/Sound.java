package app.shared;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Spielt kurze Klänge ab. Der Aufrufer nennt nur den Dateinamen — wo der {@code soundFolder} liegt,
 * weiß die Config.
 *
 * <p>Jeder Clip wird beim ersten Mal geladen und danach gehalten; ein laufender Klang wird
 * abgeschnitten und beginnt von vorn, damit auch schnell aufeinanderfolgende Treffer jeder für sich
 * hörbar sind. Fehlt die Datei, fliegt es hier — ein stummes Spiel wäre schlimmer als ein Absturz.</p>
 */
public final class Sound {

	private static final Map<String, Clip> clips = new HashMap<>();

	private Sound() {}

	public static void play(String fileName) {
		Clip clip = clips.computeIfAbsent(fileName, Sound::load);
		clip.stop();
		clip.setFramePosition(0);
		clip.start();
	}

	private static Clip load(String fileName) {
		Path file = Config.getPath("soundFolder").resolve(fileName);
		// getAudioInputStream braucht mark/reset, deshalb der BufferedInputStream.
		try (InputStream in = new BufferedInputStream(Files.newInputStream(file));
				AudioInputStream stream = AudioSystem.getAudioInputStream(in)) {
			Clip clip = AudioSystem.getClip();
			clip.open(stream);
			return clip;
		} catch (Exception e) {
			throw new RuntimeException("Sounddatei lässt sich nicht laden: " + file, e);
		}
	}
}

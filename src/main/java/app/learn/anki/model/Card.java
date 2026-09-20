package app.learn.anki.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.learn.model.LearnStat;
import app.shared.model.SketchColor;
import app.shared.AppClock;

/**
 * Aktueller Ansatz: Jede Karte gibt es genau 1x im Speicher!
 */
public class Card {
    public sealed interface Step permits Image, ClickMapElements, Output, Input, ChoiceStep, MarkMapElements, Pause, Fast,
            SketchImage, SketchImageAdd, SketchImageMark, SketchImageFill {}

    /** MC und MC+ teilen sich Optionen und Reihenfolge; der Typ trägt das Verhalten. */
    public sealed interface ChoiceStep extends Step permits MC, MCPlus {
        Set<AnswerOption> options();
        List<String> orderHint();
    }

    /** So viele Antwortfelder passen höchstens auf den Schirm — gilt für jeden Skin gleich. */
    public static final int MAX_FAST_SLOTS = 10;

    // Die unterschiedlichen Steps
    public record Image(String file) implements Step {}
    public record ClickMapElements(Set<String> mandatory, Set<String> optional) implements Step {}
    public record Output(String text) implements Step {}
    public record Input(List<String> parts) implements Step {}
    public record MC(Set<AnswerOption> options, List<String> orderHint) implements ChoiceStep {}
    public record MCPlus(Set<AnswerOption> options, List<String> orderHint) implements ChoiceStep {}
    public record MarkMapElements(Set<String> left, Set<String> right) implements Step {} // Momentan ist right immer leer. Vielleicht will ich später aber auch mal die optionalen Shapes berücksichtigen...
    public record Pause() implements Step {}
    public record Fast(int seconds, boolean ordered, int slots, List<Answer> answers) implements Step {}

    /**
     * Die drei Schritte einer Skizze: laden, eine Fläche hervorheben, eine Fläche färben.
     *
     * <p>Sie zeigen nur an und fragen nichts — gefragt wird daneben per MC. Getrennt, damit der
     * Ablauf vollständig in der Zeile steht und keiner der Schritte etwas über seine Nachbarn
     * wissen muss. {@code SketchImage} ist zugleich das Zurücksetzen: eine neu geladene Struktur
     * ist wieder leer.</p>
     */
    public record SketchImage(String structure) implements Step {}
    public record SketchImageAdd(String structure, int cell, double size, double offsetX,
            double offsetY) implements Step {}
    public record SketchImageMark(List<Integer> areas) implements Step {}
    public record SketchImageFill(List<Integer> areas, SketchColor color) implements Step {}
    
    public record AnswerOption(String text, Role role) {}
    public enum Role {
        CORRECT, WRONG_ALWAYS_SHOWN, TOLERATED, TOLERATED_ALWAYS_SHOWN, DISTRACTOR_OPTIONAL;

        /** Falsch, aber ohne Abbruch — gleich ob gelost oder immer sichtbar. */
        public boolean tolerated() {
            return this == TOLERATED || this == TOLERATED_ALWAYS_SHOWN;
        }

        /** Steht in jedem Fall in der Auswahl und wird nie weggelost. */
        public boolean alwaysShown() {
            return this == CORRECT || this == WRONG_ALWAYS_SHOWN || this == TOLERATED_ALWAYS_SHOWN;
        }
    }
    public record Answer(String hint, List<String> variants) {} // Eine gesuchte Antwort: ihre Schreibvarianten und ein Hinweis, der bis zum Treffer im Feld steht.  

    /**
     * Ein Stück Karte: ein fester Schritt oder ein Block, dessen Segmente gegeneinander gewürfelt
     * werden. Die Karte hält ihre Stücke und nicht die fertige Folge — gewürfelt wird beim Abrufen,
     * einmal je Durchgang.
     */
    public sealed interface Chunk permits FixedStep, ShuffleBlock {}
    public record FixedStep(Step step) implements Chunk {}
    public record ShuffleBlock(List<List<Step>> segments) implements Chunk {}

    private final List<Chunk> chunks;
	private final List<Step> onFailSteps;
	private final int id;
	private final String remark;
	private final Set<String> labels;
	
	private LearnStat learnStat;

	/**
	 * Baut die Karte aus ihren fertigen Teilen. Die Prüfungen hier sind Sinnfragen — sie lesen sich
	 * an der Struktur ab und hängen nicht daran, wie die Zeile geschrieben war. Die Grammatik der
	 * Zeile selbst prüft der Parser.
	 */
	public Card(int id, String remark, Set<String> labels, List<Chunk> chunks, List<Step> onFailSteps) {
		this.id = id;
		this.remark = remark;
		this.labels = Set.copyOf(labels);
		this.chunks = List.copyOf(chunks);
		this.onFailSteps = List.copyOf(onFailSteps);

		// Die Karte ist an dieser Stelle längst entschieden — eine Frage danach wäre keine.
		for (Step step : this.onFailSteps)
			if (expectsInput(step))
				throw new RuntimeException("Karte " + id + ": der <OnFail>-Block fragt etwas");

		boolean asks = false;
		for (Chunk chunk : this.chunks) {
			if (chunk instanceof FixedStep fixed) {
				if (expectsInput(fixed.step()))
					asks = true;
				continue;
			}
			// Jedes Segment muss selbst nach Input fragen. Gewürfelt wird die Reihenfolge der
			// Segmente — eines ohne Input rutscht deshalb an JEDER Position unbemerkt durch, und je
			// nach Wurf mal hier, mal dort. Die Prüfung auf Kartenebene reicht nicht: die ist schon
			// zufrieden, wenn irgendein anderes Segment etwas fragt.
			List<List<Step>> segments = ((ShuffleBlock) chunk).segments();
			for (int i = 0; i < segments.size(); i++) {
				if (!expectsInput(segments.get(i)))
					throw new RuntimeException("Karte " + id + ": Shuffle-Segment " + (i + 1) + " von "
							+ segments.size() + " erwartet keinen Input");
				asks = true;
			}
		}
		if (!asks)
			throw new RuntimeException("Karte " + id + " erwartet keinen Input");
	}

	/** Die Schritte, bei denen die Karte etwas von mir will. Alles andere zeigt nur an. */
	private static boolean expectsInput(Step step) {
		return step instanceof ChoiceStep || step instanceof Input || step instanceof ClickMapElements
				|| step instanceof Fast;
	}

	/** Ob in dieser Folge überhaupt irgendwo nach Input gefragt wird. */
	private static boolean expectsInput(List<Step> steps) {
		for (Step step : steps)
			if (expectsInput(step))
				return true;
		return false;
	}

    public boolean isDueToday() {
    	return (learnStat != null && learnStat.getDueDate().isBefore(AppClock.TODAY.plusDays(1)));
    }
    
    public boolean isNew() {
    	return (learnStat == null);
    }
    
    public int getId() {
    	return id;
    }
    
    public LearnStat getLearnStat() {
    	return learnStat;
    }

	public void setLearnStat(LearnStat learnStat) {
		this.learnStat = learnStat;
	}

	/**
	 * Die Schritte eines Durchgangs. Shuffle-Blöcke werden dabei frisch gewürfelt
	 */
	public List<Step> getSteps() {
		List<Step> out = new ArrayList<>();
		for (Chunk chunk : chunks) {
			if (chunk instanceof FixedStep fixed) {
				out.add(fixed.step());
				continue;
			}
			List<List<Step>> segments = new ArrayList<>(((ShuffleBlock) chunk).segments());
			Collections.shuffle(segments);
			for (List<Step> segment : segments)
				out.addAll(segment);
		}
		return List.copyOf(out);
	}

	/**
	 * Der Abspann hinter {@code <OnFail>} — läuft nur, wenn die Karte falsch beantwortet wurde, und
	 * ersetzt dann das sofortige Ende. Leer, wenn die Zeile keinen Block trägt.
	 *
	 * <p>Er zeigt nur an: eine Merkhilfe fürs nächste Mal, die Auflösung, das echte Bild. Eine Frage
	 * darf nicht darin stehen — an dieser Stelle ist die Karte längst entschieden.</p>
	 *
	 * <p>Eine eigene Liste und kein Index in {@link #getSteps()}: So kann kein Aufrufer versehentlich
	 * durchlaufen und den Abspann auch nach einem fehlerfreien Durchgang zeigen.</p>
	 */
	public List<Step> getOnFailSteps() {
		return onFailSteps;
	}
	
	public Set<String> getLabels() {
		return labels;
	}
}
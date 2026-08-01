package app;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Die Ordnungsregeln aus {@code docs/Design-Regeln.md} — als Prüfung statt als Vorsatz.
 *
 * <p>Solange sie von Hand gegreppt wurden, waren sie eine Gewohnheit. Hier brechen sie den Build.
 * Geprüft wird der <b>Bytecode</b>, nicht der Quelltext: eine voll qualifizierte Nutzung ohne
 * {@code import} rutscht also nicht durch, anders als bei den greps.</p>
 *
 * <p>{@code scripts} bleibt außen vor — dort liegt Wegwerf- und Analysecode, der die Regeln nicht
 * einhalten muss. Erreicht wird das dadurch, dass nur das Paket {@code app} eingelesen wird.</p>
 *
 * <p><b>Läuft nicht beim Speichern in Eclipse.</b> Der m2e-Builder ruft kein Surefire. Entweder
 * Run As → Maven build (Ziel {@code test} oder {@code verify}) oder Run As → JUnit Test.</p>
 */
@AnalyzeClasses(packages = "app", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitekturRegelnTest {

	/**
	 * Wächter 2 — kein Feature kennt den Skin.
	 *
	 * <p>Features bekommen fertige Oberflächen aus {@code shared.ui}; woher deren Maße und Farben
	 * kommen, geht sie nichts an. Ausgenommen ist {@code app.controller}: der schaltet den Skin um
	 * und muss ihn deshalb kennen.</p>
	 */
	@ArchTest
	static final ArchRule keinFeatureKenntDenSkin = noClasses()
			.that().resideOutsideOfPackages("app.shared..", "app.controller..")
			.should().dependOnClassesThat().resideInAPackage("app.shared.skin..")
			.because("Features bekommen fertige Oberflächen aus shared.ui — der Skin ist deren Innenleben");

	/**
	 * Wächter 1 — der Skin kennt die UI nicht.
	 *
	 * <p>Er hält die Werte, die CSS nicht ausdrücken kann, und erzeugt das Stylesheet. Gebaut wird
	 * auf der anderen Seite.</p>
	 */
	@ArchTest
	static final ArchRule derSkinKenntDieUiNicht = noClasses()
			.that().resideInAPackage("app.shared.skin..")
			.should().dependOnClassesThat().resideInAPackage("app.shared.ui..")
			.because("die UI baut, der Skin liefert Werte und erzeugt das CSS");

	/**
	 * Wächter 3 — die Bausteine kennt nur {@code shared.ui}.
	 *
	 * <p>Features und {@code controller} sehen ausschließlich fertige Oberflächen; woraus die
	 * bestehen, ist Sache von {@code shared.ui}.</p>
	 */
	@ArchTest
	static final ArchRule bausteineNurAusSharedUi = noClasses()
			.that().resideOutsideOfPackage("app.shared.ui..")
			.should().dependOnClassesThat().resideInAPackage("app.shared.ui.components..")
			.because("Bausteine werden verbaut, nicht gezeigt — wer sie braucht, geht über shared.ui");

	/** Keine Paketzyklen. Eine von Thorstens sieben Ausgangsforderungen. */
	@ArchTest
	static final ArchRule keineZyklen = slices()
			.matching("app.(**)")
			.should().beFreeOfCycles();

	/**
	 * Style-Klassen werden nur in der Anzeige-Schicht vergeben.
	 *
	 * <p>Wie etwas aussieht, entscheidet {@code shared.ui} (setzt die Klasse) und {@code shared.skin}
	 * (schreibt die Regel dazu). Ein {@code getStyleClass().add(…)} anderswo ist eine
	 * Anzeige-Entscheidung außerhalb der Anzeige-Schicht.</p>
	 *
	 * <p><b>Eine benannte Ausnahme:</b> {@code MainWindow} ist das Fenster selbst — die Stelle, an
	 * der die Anwendung ihren Rahmen baut, bevor es überhaupt etwas zu zeigen gibt. Dass dort JavaFX
	 * steht, ist kein Verstoß, sondern der Ort, an dem der Rahmen entsteht.</p>
	 */
	@ArchTest
	static final ArchRule styleKlassenNurInDerAnzeigeSchicht = noClasses()
			.that().resideOutsideOfPackages("app.shared.ui..", "app.shared.skin..")
			.and().doNotHaveFullyQualifiedName("app.controller.MainWindow")
			.should().callMethodWhere(target(name("getStyleClass")))
			.because("wie etwas aussieht, entscheidet die Anzeige-Schicht");

	// !Später: Die Vier-Sprossen-Ordnung innerhalb von shared (ui → skin → model → shared) als
	// layeredArchitecture() aufnehmen. Die fünf Regeln hier prüfen die drei Wächter, die
	// Zyklenfreiheit und die Style-Klassen — die *Reihenfolge* der Sprossen prüft bislang nichts.
	
	/** !Sofort: Ungedeckt bleiben: Regel 2 (Abwärtsrichtung/Sprossenordnung — der Test räumt das in Zeile 90-92 selbst ein),
	 *  Regel 3 (Seitwärtsgriff, siehe Fund 4), Regel 4 (Optional, siehe Fund 9), Regel 5 (Streams), Regel 6 (Null-Layout)
	 *   und vor allem „jedes Feature-Paket ist 100 % framework-frei" — die am nachdrücklichsten formulierte Zusage des 
	 *   Regeldokuments hat keine Regel. **/ 
}

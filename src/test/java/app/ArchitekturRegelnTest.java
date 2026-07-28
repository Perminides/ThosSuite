package app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Die Ordnungsregeln aus {@code docs/Skin-Refactoring-Plan.md} §1 — als Prüfung statt als Vorsatz.
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

	// ===== Kommen dazu, sobald sie halten — heute würden sie den Build brechen =====
	//
	// Keine Paketzyklen. Heute genau einer: app.shared.skin <-> app.shared.ui.components. Das ist
	// derselbe Sachverhalt wie Wächter 1 und 3, nur von der anderen Seite gesehen — der Skin
	// importiert die Bausteine, die Bausteine holen sich den SkinService. Frei nach Schritt 3f.
	//
	// @ArchTest
	// static final ArchRule keineZyklen = slices()
	//         .matching("app.(**)")
	//         .should().beFreeOfCycles();
	//
	// Wächter 1 — der Skin kennt die UI nicht. Frei nach Schritt 3f (heute noch drei Importe:
	// MultipleChoicePane, SuiteImage, SuiteInfoLabel).
	//
	// @ArchTest
	// static final ArchRule derSkinKenntDieUiNicht = noClasses()
	//         .that().resideInAPackage("app.shared.skin..")
	//         .should().dependOnClassesThat().resideInAPackage("app.shared.ui..")
	//         .because("die UI baut, der Skin liefert Werte und erzeugt das CSS");
	//
	// Wächter 3 — die Bausteine kennt nur shared.ui. Frei nach Schritt 4.
	//
	// @ArchTest
	// static final ArchRule bausteineNurAusSharedUi = noClasses()
	//         .that().resideOutsideOfPackage("app.shared.ui..")
	//         .should().dependOnClassesThat().resideInAPackage("app.shared.ui.components..")
	//         .because("Bausteine werden verbaut, nicht gezeigt — wer sie braucht, geht über shared.ui");
}

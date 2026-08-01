package app;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.Optional;

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

	/** Keine Paketzyklen. Eine von Perminidess sieben Ausgangsforderungen. */
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

	/**
	 * Die Sprossenordnung in {@code shared}, Teil 1 — {@code shared.model} kennt nichts über sich.
	 *
	 * <p>Die Leiter lautet ui → skin → model → shared. Ein {@code model} ist Datenvokabular; wer es
	 * lädt, baut oder anzeigt, geht es nichts an. Die oberste Kante (skin → ui) bewacht bereits
	 * Wächter 1, die beiden unteren diese Regel und ihre Nachbarin.</p>
	 */
	@ArchTest
	static final ArchRule sharedModelKenntNichtsDarueber = noClasses()
			.that().resideInAPackage("app.shared.model..")
			.should().dependOnClassesThat().resideInAnyPackage("app.shared.ui..", "app.shared.skin..")
			.because("ein model ist Datenvokabular — es weiß nicht, wer es lädt oder anzeigt");

	/**
	 * Die Sprossenordnung in {@code shared}, Teil 2 — die Wurzel kennt nichts über sich.
	 *
	 * <p>{@code Config}, {@code DB}, {@code Log}, {@code AppClock}, {@code UiUtils} sind das
	 * Fundament des Fundaments. Griffen sie nach oben, gäbe es keine Reihenfolge mehr, in der die
	 * Suite überhaupt baubar wäre — dasselbe Problem, das {@code Config.init} mit seiner streng
	 * linearen Konstruktion umgeht.</p>
	 *
	 * <p>{@code "app.shared"} <b>ohne</b> {@code ..} meint genau die Wurzel und nicht den Teilbaum.</p>
	 */
	@ArchTest
	static final ArchRule sharedWurzelKenntNichtsDarueber = noClasses()
			.that().resideInAPackage("app.shared")
			.should().dependOnClassesThat().resideInAnyPackage(
					"app.shared.ui..", "app.shared.skin..", "app.shared.model..")
			.because("die unterste Sprosse trägt — sie greift nicht nach oben");

	/**
	 * Regel 2 und 3 des Regeldokuments in einem Ausdruck: auf oberster Ebene läuft alles nach
	 * unten, und seitwärts greift niemand.
	 *
	 * <p>Eine Abhängigkeit zwischen zwei obersten Paketen ist nur erlaubt, wenn sie aus
	 * {@code controller} kommt (der darf nach unten in jedes Feature) oder nach {@code shared} geht
	 * (dorthin darf jeder). Alles andere fällt: Feature → Feature (Regel 3), Feature → controller
	 * (Aufwärtsgriff), shared → Feature (das Fundament kennt kein einzelnes Feature).</p>
	 *
	 * <p>Bewusst über {@code slices} statt über eine {@code layeredArchitecture()} mit einem Layer
	 * je Feature: die müsste alle Feature-Pakete aufzählen und wäre beim nächsten neuen Feature
	 * still unvollständig. So wird ein neues oberstes Paket automatisch mitbewacht.</p>
	 *
	 * <p>{@code ThosSuiteApp} liegt direkt in {@code app} und fällt aus dem Schnitt
	 * {@code app.(*)..} heraus — es braucht keine Ausnahme.</p>
	 *
	 * <p><b>Eine befristete Ausnahme: {@code app.tmp}.</b> Das Wegwerf-Gerüst des
	 * Fitbit-/Health-Vergleichs steht über beiden Seiten und greift deshalb in {@code fitbit} und
	 * {@code activity}. Die Ausnahme steht hier sichtbar, statt dass die Regel fehlt. Fällt
	 * {@code app.tmp} weg, fällt diese Zeile mit — greift dann noch etwas seitwärts, bricht der
	 * Build, und das ist richtig so. Umgekehrt bleibt {@code app.tmp} als <i>Ziel</i> geschützt:
	 * ein Feature darf es weiterhin nicht anfassen, nur {@code controller} darf das.</p>
	 */
	@ArchTest
	static final ArchRule keinSeitwaertsgriffAufObersterEbene = slices()
			.matching("app.(*)..")
			.should().notDependOnEachOther()
			.ignoreDependency(resideInAPackage("app.controller.."), alwaysTrue())
			.ignoreDependency(alwaysTrue(), resideInAPackage("app.shared.."))
			.ignoreDependency(resideInAPackage("app.tmp.."), alwaysTrue()) // !tmp — siehe Javadoc
			.because("auf oberster Ebene geht es nur nach unten: controller hinab, alle nach shared");

	/**
	 * Regel 4 — {@code null} statt {@code Optional} bei Rückgaben.
	 *
	 * <p>Geprüft wird der Rückgabetyp; mehr geht nicht. Die dokumentierte Ausnahme rutscht damit
	 * von selbst durch: ein {@code Optional} aus einer JDK-API ({@code Dialog.showAndWait()}) wird
	 * am Entstehungsort in eine lokale Variable ausgepackt und taucht in keiner Signatur auf.</p>
	 *
	 * <p>Der zweite Halbsatz der Regel — {@code null}-Rückgaben gehören ins Javadoc — bleibt
	 * ungeprüft. Kommentare sieht der Bytecode nicht.</p>
	 */
	@ArchTest
	static final ArchRule keineOptionalRueckgaben = noMethods()
			.should().haveRawReturnType(Optional.class)
			.because("fehlt ein Rückgabewert, kommt null zurück — Optional wird nicht weitergereicht");

	/**
	 * Regel 5 — keine Streams.
	 *
	 * <p><b>Diese Regel ist schärfer als ihr Text im Regeldokument.</b> Dort steht „außer sie sind
	 * unbedingt nötig"; ArchUnit kennt kein „unbedingt nötig" und verbietet sie ganz. Wird einer
	 * doch einmal gebraucht, ist das Aufmachen dieser Regel der bewusste Schritt dahin — statt
	 * dass die Kette nebenbei einzieht. Genau das ist die Absicht.</p>
	 */
	@ArchTest
	static final ArchRule keineStreams = noClasses()
			.should().dependOnClassesThat().resideInAPackage("java.util.stream..")
			.because("eine Schleife liest sich nach Monaten ohne Anlauf, eine filter/map/collect-Kette nicht");

	/**
	 * Die harte Fassung von „jedes Feature-Paket ist 100 % framework-frei".
	 *
	 * <p>JavaFX ist in {@code shared} und {@code controller} eingezäunt. Ausgenommen ist außerdem
	 * das Wurzelpaket {@code app} selbst — dort liegt {@code ThosSuiteApp}, das eine
	 * {@code javafx.application.Application} <i>ist</i> und es sein muss. {@code "app"} ohne
	 * {@code ..} meint genau diese Wurzel, nicht den Teilbaum darunter.</p>
	 *
	 * <p>Weil Bytecode geprüft wird und nicht der Import-Block, greift auch die Nachhut der Regel:
	 * ein opak durchgereichtes JavaFX-Objekt trägt den Typ in der Signatur und fällt auf. Was ein
	 * Feature nach {@code shared} hinabreicht, bleibt damit zwangsläufig framework-freies
	 * Datenvokabular.</p>
	 */
	@ArchTest
	static final ArchRule featuresSindFrameworkFrei = noClasses()
			.that().resideOutsideOfPackages("app.shared..", "app.controller..", "app")
			.should().dependOnClassesThat().resideInAnyPackage("javafx..")
			.because("Feature-Code bleibt ohne UI-Framework-Kenntnisse lesbar und änderbar");

	// Ungeprüft bleibt bewusst Regel 6 (Null-Layout): „keine LayoutManager" hieße ein Verbot von
	// VBox/HBox, und die kommen legitim vor (DiaryCard extends VBox). Da ist keine Regel drin, die
	// nicht mehr Fehlalarme als Nutzen brächte.
}

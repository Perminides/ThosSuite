package app.learn.anki;

import java.util.List;

/**
 * Die Vorschläge eines Collect-Steps. Prototyp: fest eingebaut, alle Länder aus der Spalte
 * {@code Name} des Flaggenblatts, damit sie zu den Namen der Weltkarte passen.
 */
final class CollectWords {

	static final List<String> COUNTRIES = List.of(
			"Abchasien", "Afghanistan", "Ägypten", "Albanien", "Algerien", "Andorra", "Angola",
			"Antigua und Barbuda", "Äquatorialguinea", "Argentinien", "Armenien", "Aserbaidschan",
			"Äthiopien", "Australien", "Bahamas", "Bahrain", "Bangladesch", "Barbados", "Belarus",
			"Belgien", "Belize", "Benin", "Bermuda", "Bhutan", "Bolivien", "Bosnien und Herzegowina",
			"Botswana", "Brasilien", "Brunei", "Bulgarien", "Burkina Faso", "Burundi", "Chile", "China",
			"Cookinseln", "Costa Rica", "Dänemark", "Demokratische Republik Kongo", "Deutschland",
			"Dominica", "Dominikanische Republik", "Dschibuti", "Ecuador", "El Salvador", "Elfenbeinküste",
			"England", "Eritrea", "Estland", "Eswatini", "Fidschi", "Finnland", "Frankreich", "Gabun",
			"Gambia", "Georgien", "Ghana", "Grenada", "Griechenland", "Grönland", "Guam", "Guatemala",
			"Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hongkong", "Indien", "Indonesien",
			"Irak", "Iran", "Irland", "Island", "Israel", "Italien", "Jamaika", "Japan", "Jemen",
			"Jordanien", "Kambodscha", "Kamerun", "Kanada", "Kap Verde", "Kasachstan", "Katar", "Kenia",
			"Kirgisistan", "Kiribati", "Kolumbien", "Komoren", "Kosovo", "Kroatien", "Kuba", "Kuwait",
			"Laos", "Lesotho", "Lettland", "Libanon", "Liberia", "Libyen", "Liechtenstein", "Litauen",
			"Luxemburg", "Madagaskar", "Malawi", "Malaysia", "Malediven", "Mali", "Malta", "Marokko",
			"Marshallinseln", "Mauretanien", "Mauritius", "Mexiko", "Mikronesien", "Moldau", "Monaco",
			"Mongolei", "Montenegro", "Mosambik", "Myanmar", "Namibia", "Nauru", "Nepal", "Neukaledonien",
			"Neuseeland", "Nicaragua", "Niederlande", "Niger", "Nigeria", "Niue", "Nordkorea",
			"Nordmazedonien", "Nordzypern", "Norwegen", "Oman", "Österreich", "Osttimor", "Pakistan",
			"Palästina", "Palau", "Panama", "Papua-Neuguinea", "Paraguay", "Peru", "Philippinen", "Polen",
			"Portugal", "Puerto Rico", "Republik Kongo", "Ruanda", "Rumänien", "Russland", "Salomonen",
			"Sambia", "Samoa", "San Marino", "Saudi-Arabien", "Schottland", "Schweden", "Schweiz",
			"Senegal", "Serbien", "Seychellen", "Sierra Leone", "Simbabwe", "Singapur", "Slowakei",
			"Slowenien", "Somalia", "Somaliland", "Spanien", "Sri Lanka", "St. Kitts und Nevis",
			"St. Lucia", "St. Vincent und die Grenadinen", "Südafrika", "Sudan", "Südkorea", "Südossetien",
			"Südsudan", "Surinam", "Syrien", "São Tomé und Príncipe", "Tadschikistan", "Taiwan", "Tansania",
			"Thailand", "Togo", "Tokelau", "Tonga", "Transnistrien", "Trinidad und Tobago", "Tschad",
			"Tschechien", "Tunesien", "Türkei", "Turkmenistan", "Tuvalu", "Uganda", "UK", "Ukraine",
			"Ungarn", "Uruguay", "USA", "Usbekistan", "Vanuatu", "Vatikanstadt", "Venezuela",
			"Vereinigte Arabische Emirate", "Vietnam", "Wales", "Wallis und Futuna", "Westsahara",
			"Zentralafrikanische Republik", "Zypern");

	private CollectWords() {
	}
}

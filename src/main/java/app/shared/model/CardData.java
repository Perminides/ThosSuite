package app.shared.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Dummes Eingabe-DTO für eine Kachel im Viewer — der Kontrakt, den der Skin ({@code createCard}) rendert.
 * Typ-agnostisch: Filme, Serien und Episoden nutzen dasselbe Record.
 *
 * <p>Enthält <b>keine</b> Anzeige-Logik mehr. Alle Felder sind bereits fertig aufbereitet:
 * <ul>
 *   <li>{@code headerLines} werden bold dargestellt</li>
 *   <li>{@code detailLines} normal, zwischen Header und "Rated:"</li>
 *   <li>danach Rated, Directors (Links), Actors (Links), Overview, Comment</li>
 * </ul>
 * Wer entscheidet, was in diesen Listen steht (z.B. "Episode:" vs. "Bis Episode:"), ist die movie-Domäne —
 * dort liegen jetzt die Factories, die dieses Record bauen.
 *
 * <p>{@code id} und {@code sortDate} nutzt der Skin nicht; sie sind Identität/Sortierung für die movie-Seite,
 * reisen aber im selben Austauschobjekt mit.
 */
public record CardData(
		int id,
		List<String> headerLines,
		List<String> detailLines,
		LocalDate sortDate,
		int rating,
		LocalDate ratedAt,
		List<String> directors,
		List<String> actors,
		String overview,
		String comment,
		String imageFilename) {
}
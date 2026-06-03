package app.learn.anki.repository;

import java.time.LocalDateTime;

/**
 * Reines Persistenz-Datenobjekt einer gespielten Karte. Existiert ausschliesslich, um der
 * Persistenz genau das zu liefern, was sie zum Schreiben braucht - nicht mehr.
 *
 * Bewusst hier (und nicht in model): Die Klasse ist kein Domaenenbegriff, sondern der Kontrakt
 * von {@code saveLearned}. Wer die Datei sieht, weiss sofort, wofuer sie da ist.
 *
 * Befuellt wird sie vom AnkiSessionProgress - der einzigen Stelle, die Karte und CardProgress hat.
 * first_played/last_played stehen bewusst NICHT drin: das "heute" setzt die Persistenz selbst.
 */
public record PlayedCardData(
        int cardId,
        int level,
        int wrongCount,
        boolean correctFlag,
        LocalDateTime playedTimestamp) {
}
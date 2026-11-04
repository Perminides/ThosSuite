package scripts.learn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.sqlite.SQLiteConfig;

public class QuizWordFrequency {

    private static final String DB_PATH = "C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite/data/quiz.db";
    private static final int TOP_COUNT = 100;

    private static final Set<String> IGNORED_WORDS = Set.of(
            "a", "ab", "aber", "akz", "akzeptiert", "akzeptiere", "alle", "allem", "aller", "also", "als",
            "am", "an", "and", "andere", "anderem", "anderen", "antwort", "art", "auch", "auf", "aufgrund",
            "aus", "b", "begriff", "bei", "beide", "beiden", "beim", "bekannt", "benannt", "bereits",
            "bezeichnet", "besteht", "bis", "bzw", "buchstaben", "chr", "da", "dabei", "damit", "das", "dass",
            "de", "dem", "den", "der", "deren", "des", "dessen", "deutsche", "deutschen", "die", "diese",
            "diesem", "diesen", "dieser", "dieses", "doch", "dort", "drei", "durch", "ein", "eine", "einem",
            "einen", "einer", "eines", "einzige", "ende", "engl", "englischen", "er", "erschien", "erst",
            "erste", "ersten", "erstmals", "es", "etwa", "fachbegriff", "falsch", "findet", "fünf", "für",
            "führte", "gegen", "gehören", "gehört", "gemeinsam", "genannt", "gesucht", "gesuchte", "gesuchten",
            "gewann", "gibt", "gilt", "große", "großen", "größte", "größten", "haben", "handelt", "hat",
            "hatte", "häufig", "heißt", "heute", "heutigen", "hier", "hieß", "i", "ihm", "ihn", "ihr", "ihre",
            "ihrem", "ihren", "ihrer", "im", "immer", "in", "ins", "ist", "jahr", "jahre", "jahren", "jahres",
            "jedoch", "kann", "kein", "keine", "konnte", "können", "land", "landes", "lautet", "liegt", "m",
            "man", "mehr", "mit", "muss", "müssen", "nach", "nachdem", "name", "namen", "nachname", "nachnamen",
            "neben", "nennt", "nicht", "noch", "nur", "of", "oder", "ohne", "person", "platz", "s", "schon",
            "sehen", "sein", "seine", "seinem", "seinen", "seiner", "seines", "seit", "selbst", "sich", "sie",
            "sind", "so", "soll", "sondern", "sowie", "später", "spielen", "spielt", "stadt", "stammt", "statt",
            "steht", "teil", "teilen", "the", "titel", "trägt", "u", "über", "um", "und", "unter",
            "ursprünglich", "us", "v", "veröffentlichte", "vier", "von", "vom", "vor", "war", "waren", "was",
            "weil", "weitere", "welche", "welchem", "welchen", "welcher", "welches", "weltweit", "wenn", "wer",
            "werden", "wieder", "wie", "wir", "wird", "wohl", "wort", "wurde", "wurden", "während", "wörtern",
            "x", "xx", "xxx", "zählt", "zu", "zum", "zur", "zusammen", "zuvor", "zwei", "zwischen",
            "film", "song", "band", "deutschland", "leben", "serie", "republik", "frau", "zeit", "geschichte",
            "jahrhundert", "rolle", "familie", "britische", "roman", "menschen", "album", "olympischen",
            "unternehmen", "figur", "geht", "könig", "oscar", "britischen", "französischen", "usa", "deutsch",
            "englisch", "welt", "werk", "amerikanische", "befindet", "gewinnen", "krieg", "meter", "millionen",
            "hauptstadt", "insel", "jahrhunderts", "königreich", "mutter", "partei", "schauspielerin", "vater",
            "benannte", "charts", "eigenen", "eigentlich", "englische", "ersetzt", "europa", "fast", "feierte",
            "for", "gegründet", "gemälde", "genügt", "ich", "insgesamt", "la", "mann", "michael", "new",
            "regisseur", "republic", "sehr", "sowohl", "van", "you", "zweiten", "amerikanischen", "amt", "anfang",
            "berg", "dies", "entwickelte", "französische", "frauen", "ganz", "geborene", "gelten", "gewählt",
            "hit", "ihres", "ii", "je", "kommt", "laut", "mittlerweile", "nacht", "o", "oft", "peter",
            "preis", "sieben", "sohn", "spielte", "sängerin", "t", "to"
    );

    public static void main(String[] args) throws Exception {
        if (!Files.isRegularFile(Path.of(DB_PATH)))
            throw new IllegalStateException("Quiz-DB nicht gefunden: " + DB_PATH);

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        Map<String, Integer> counts = new HashMap<>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH, config.toProperties())) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT question, answer FROM question");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    countWords(rs.getString("question"), counts);
                    countWords(rs.getString("answer"), counts);
                }
            }
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((left, right) -> {
            int byCount = Integer.compare(right.getValue(), left.getValue());
            return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
        });

        int limit = Math.min(TOP_COUNT, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            System.out.printf("%5d  %s%n", entry.getValue(), entry.getKey());
        }
    }

    private static void countWords(String text, Map<String, Integer> counts) {
        if (text == null || text.isBlank())
            return;

        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (word.isEmpty() || IGNORED_WORDS.contains(word))
                continue;
            counts.merge(word, 1, Integer::sum);
        }
    }
}

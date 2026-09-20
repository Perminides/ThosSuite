package app.diary.repository;

/**
 * Übersetzt die Suchsprache des Tagebuchs in ein SQL-Fragment für die WHERE-Klausel von
 * {@link Repository#search}: Worte suchen im Text, {@code tag:name} in den Tags, dazu
 * {@code and}, {@code or} und Klammern.
 *
 * <p>Wohnt neben dem Repository, weil das Fragment dessen Tabellen, Spalten und den Alias
 * {@code de} kennen muss — dieses Wissen bleibt damit auf einer Seite.</p>
 */
class QueryParser {

    private String input;
    private int pos;

    static class InvalidQueryException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        InvalidQueryException(String message) {
            super(message);
        }
    }

    String parse(String raw) {
        this.input = raw.trim().toLowerCase();
        this.pos = 0;

        if (this.input.isEmpty()) {
            throw new InvalidQueryException("Leere Eingabe");
        }

        String result = parseExpr();

        skipWhitespace();
        if (pos < this.input.length()) {
            throw new InvalidQueryException("Unerwartetes Zeichen an Position " + pos);
        }

        return result;
    }

    private String parseExpr() {
        String left = parseTerm();
        while (true) {
            skipWhitespace();
            if (matchKeyword("or")) {
                String right = parseTerm();
                left = "(" + left + " OR " + right + ")";
            } else {
                break;
            }
        }
        return left;
    }

    private String parseTerm() {
        String left = parseFactor();
        while (true) {
            skipWhitespace();
            if (matchKeyword("and")) {
                String right = parseFactor();
                left = "(" + left + " AND " + right + ")";
            } else {
                break;
            }
        }
        return left;
    }

    private String parseFactor() {
        skipWhitespace();

        if (pos >= input.length()) {
            throw new InvalidQueryException("Unerwartetes Ende des Ausdrucks");
        }

        if (input.charAt(pos) == '(') {
            pos++;
            String inner = parseExpr();
            skipWhitespace();
            if (pos >= input.length() || input.charAt(pos) != ')') {
                throw new InvalidQueryException("Schließende Klammer fehlt");
            }
            pos++;
            return inner;
        }

        if (input.startsWith("tag:", pos)) {
            pos += 4;
            String tagName = parseWord();
            if (tagName.isEmpty()) {
                throw new InvalidQueryException("Tag-Name fehlt nach 'tag:'");
            }
            return "EXISTS (SELECT 1 FROM diary_entry_tag det WHERE det.entry_created_at = de.created_at AND lower(det.tag_name) = '" + escapeSql(tagName) + "')";
        }

        String word = parseWord();
        if (word.isEmpty()) {
            throw new InvalidQueryException("Leeres Wort an Position " + pos);
        }
        return "lower(de.text) LIKE '%" + escapeSql(word) + "%'";
    }

    private String parseWord() {
        int start = pos;
        while (pos < input.length()
                && input.charAt(pos) != ' '
                && input.charAt(pos) != '('
                && input.charAt(pos) != ')') {
            pos++;
        }
        String word = input.substring(start, pos);
        if (word.equals("and") || word.equals("or")) {
            pos = start;
            throw new InvalidQueryException("Operator '" + word + "' an ungültiger Stelle");
        }
        return word;
    }

    private boolean matchKeyword(String keyword) {
        if (!input.startsWith(keyword, pos)) {
            return false;
        }
        int after = pos + keyword.length();
        if (after < input.length()) {
            char next = input.charAt(after);
            if (next != ' ' && next != '(') {
                return false;
            }
        }
        pos += keyword.length();
        return true;
    }

    private void skipWhitespace() {
        while (pos < input.length() && input.charAt(pos) == ' ') {
            pos++;
        }
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }
}

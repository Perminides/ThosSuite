package app.diary;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import app.diary.model.Entry;
import app.diary.repository.Repository;
import app.shared.Config;
import app.shared.model.DiaryAttachment;
import app.shared.model.DiaryCardData;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.DiaryScreenView;

public class DiaryScreen implements Screen {

    private static final int DEFAULT_MAX_RESULTS = 100;
    private static final Path DIARY_ATTACHMENTS_FOLDER = Config.getPath("attachments.folder").resolve("diary");
    private static final Path THUMBS_FOLDER = DIARY_ATTACHMENTS_FOLDER.resolve("thumbnails");

    private final Repository repository = new Repository();
    private final DiaryScreenView view = new DiaryScreenView();

    public DiaryScreen() {
        view.setSearchListener(this::runSearch);
        view.setEditListener(this::openEdit);
    }

    @Override
    public ScreenView getView() {
        return view;
    }

    @Override
    public void refresh() {
        view.rebuild();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }

    private void runSearch(String rawQuery, LocalDate from, LocalDate to) {
        String whereFragment;
        if (rawQuery == null || rawQuery.isBlank()) {
            whereFragment = "1=1";
            view.setQueryValid(true);
        } else {
            try {
                whereFragment = new QueryParser().parse(rawQuery);
                view.setQueryValid(true);
            } catch (QueryParser.InvalidQueryException e) {
                view.setQueryValid(false);
                return;
            }
        }

        int maxResults = Config.getInt("diary.maxResults", DEFAULT_MAX_RESULTS);
        List<Entry> entries = repository.search(whereFragment, from, to, maxResults + 1);

        boolean truncated = entries.size() > maxResults;
        if (truncated) entries = entries.subList(0, maxResults);

        List<DiaryCardData> cards = new ArrayList<>();
        for (Entry e : entries) {
            List<DiaryAttachment> attachments = new ArrayList<>();
            for (String rel : e.attachmentPaths()) {
                String fileName = Path.of(rel).getFileName().toString();
                attachments.add(new DiaryAttachment(
                        DIARY_ATTACHMENTS_FOLDER.resolve(rel).toString(),
                        THUMBS_FOLDER.resolve(fileName).toString()));
            }
            cards.add(new DiaryCardData(
                    e.createdAt(), e.entryDate(), e.text(), e.tags(), attachments));
        }
        view.showResults(cards, truncated, maxResults);
    }

    private void openEdit(DiaryCardData c) {
        new DiaryEditorPresenter().showEdit(c);
    }

    // -------------------------------------------------------------------------
    // Innere Klasse: QueryParser
    // -------------------------------------------------------------------------
    
    private static class QueryParser {

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
}
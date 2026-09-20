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
import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.DiaryScreenView;

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
        int maxResults = Config.getInt("diary.maxResults", DEFAULT_MAX_RESULTS);
        List<Entry> entries = repository.search(rawQuery, from, to, maxResults + 1);
        if (entries == null) {
            view.setQueryValid(false);
            return;
        }
        view.setQueryValid(true);

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
}

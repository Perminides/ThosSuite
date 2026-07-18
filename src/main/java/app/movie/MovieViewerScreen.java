package app.movie;

import app.movie.repository.MovieViewerRepository;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.MovieViewerScreenView;

public class MovieViewerScreen implements Screen {

    private final MovieViewerRepository repository = new MovieViewerRepository();
    private final MovieViewerScreenView view = new MovieViewerScreenView();

    public MovieViewerScreen() {
        view.setNames(
                repository.loadAllDirectorNames(),
                repository.loadAllActorNames(),
                repository.loadAllTitles());

        view.setSelectionListener(new MovieViewerScreenView.SelectionListener() {
            @Override public void onDirectorSelected(String name) { view.showCards(repository.loadByDirector(name)); }
            @Override public void onActorSelected(String name)    { view.showCards(repository.loadByActor(name)); }
            @Override public void onTitleSelected(String name)    { view.showCards(repository.loadByTitle(name)); }
        });
    }

    @Override
    public ScreenView getView() {
        return view;
    }

    @Override
    public void refresh() {
        // TODO (dein Punkt, übernommen): optimistisch — setzt nur den Hintergrund neu. Ein Skin, der
        // Positionen/Komponenten ändert, greift so nicht. Offen: nur-Background beibehalten vs. voller
        // Rebuild (verwürfe die laufende Suche). Gehört zu "der Rest", bewusst nicht jetzt entschieden.
        view.reapplyBackground();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}
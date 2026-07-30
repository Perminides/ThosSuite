package app.movie;

import app.movie.repository.MovieViewerRepository;
import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.MovieViewerScreenView;

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
        view.rebuild();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}

package mindimagelibrary.sample.imageDB;

import android.app.FragmentManager;
import android.os.Bundle;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.AbsListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import mindimagelibrary.sample.imageDB.controller.MovieList;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class MovieListTest extends InstrumentationTestCase {

    private static final String FRAGMENT_TAG = "fragment";
    private MainActivity activity;
    private MovieList movieList;


    /**
     * Adds the fragment to a new blank activity, thereby fully
     * initializing its view.
     */
    @Before
    public void setUp() throws Exception {
        movieList = new MovieList();
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
        FragmentManager manager = activity.getFragmentManager();
        Bundle args = new Bundle();
        args.putString("currentList", "upcoming");
        movieList.setArguments(args);
        manager.beginTransaction().add(movieList, FRAGMENT_TAG).commit();
    }

    @Test
    public void testPreconditions() throws Exception {
        assertNotNull("activity is null", activity);
        assertNotNull("DrawerLayout is null", activity.getMDrawerLayout());
        assertNotNull("movieList is null", movieList);
        assertNotNull("cant find fragment", activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG));
    }

    @Test
    public void testViews() throws Exception {
        View movieListRoot = movieList.getView();
        assertNotNull("movieList rootView is null", movieListRoot);
        assertNotNull("progressBar is null", movieListRoot.findViewById(R.id.progressBar));
        AbsListView listView = (AbsListView) movieListRoot.findViewById(R.id.movieslist);
        assertNotNull("listView is null", listView);
        assertNotNull("listView listener is null", listView.getOnItemClickListener());

    }

}

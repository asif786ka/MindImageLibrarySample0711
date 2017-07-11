
package mindimagelibrary.sample.imageDB;

import android.app.FragmentManager;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.AbsListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import mindimagelibrary.sample.imageDB.controller.GenresList;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class GenresTest extends InstrumentationTestCase {

    private static final String FRAGMENT_TAG = "fragment";
    private MainActivity activity;
    private GenresList genresList;


    /**
     * Adds the fragment to a new blank activity, thereby fully
     * initializing its view.
     */
    @Before
    public void setUp() throws Exception {
        genresList = new GenresList();
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
        FragmentManager manager = activity.getFragmentManager();
        manager.beginTransaction().add(genresList, FRAGMENT_TAG).commit();
    }

    @Test
    public void testPreconditions() throws Exception {
        assertNotNull("activity is null", activity);
        assertNotNull("DrawerLayout is null", activity.getMDrawerLayout());
        assertNotNull("genresList is null", genresList);
        assertNotNull("cant find fragment", activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG));
    }

    @Test
    public void testViews() throws Exception {
        View genresListRoot = genresList.getView();
        assertNotNull("genresList rootView is null", genresListRoot);
        assertNotNull("progressBar is null", genresListRoot.findViewById(R.id.progressBar));
        AbsListView listView = (AbsListView) genresListRoot.findViewById(R.id.genresList);
        assertNotNull("listView is null", listView);
        assertNotNull("listView adapter is null", listView.getAdapter());
        assertNotNull("listView listener is null", listView.getOnItemClickListener());

    }

}

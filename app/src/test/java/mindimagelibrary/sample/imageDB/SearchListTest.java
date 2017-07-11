
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


import mindimagelibrary.sample.imageDB.controller.SearchList;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class SearchListTest extends InstrumentationTestCase {

    private static final String FRAGMENT_TAG = "fragment";
    private MainActivity activity;
    private SearchList searchList;


    /**
     * Adds the fragment to a new blank activity, thereby fully
     * initializing its view.
     */
    @Before
    public void setUp() throws Exception {
        searchList = new SearchList();
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
        FragmentManager manager = activity.getFragmentManager();
        manager.beginTransaction().add(searchList, FRAGMENT_TAG).commit();
    }

    @Test
    public void testPreconditions() throws Exception {
        assertNotNull("activity is null", activity);
        assertNotNull("DrawerLayout is null", activity.getMDrawerLayout());
        assertNotNull("searchList is null", searchList);
        assertNotNull("cant find fragment", activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG));
    }

    @Test
    public void testViews() throws Exception {
        View searchListRoot = searchList.getView();
        assertNotNull("searchList rootView is null", searchListRoot);
        AbsListView listView = (AbsListView) searchListRoot.findViewById(R.id.movieslist);
        assertNotNull("listView is null", listView);
        assertNotNull("listView listener is null", listView.getOnItemClickListener());

    }

}


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

import mindimagelibrary.sample.imageDB.controller.TVList;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class TVListTest extends InstrumentationTestCase {

    private static final String FRAGMENT_TAG = "fragment";
    private MainActivity activity;
    private TVList tvList;


    /**
     * Adds the fragment to a new blank activity, thereby fully
     * initializing its view.
     */
    @Before
    public void setUp() throws Exception {
        tvList = new TVList();
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
        FragmentManager manager = activity.getFragmentManager();
        Bundle args = new Bundle();
        args.putString("currentList", "onTheAir");
        tvList.setArguments(args);
        manager.beginTransaction().add(tvList, FRAGMENT_TAG).commit();
    }

    @Test
    public void testPreconditions() throws Exception {
        assertNotNull("activity is null", activity);
        assertNotNull("DrawerLayout is null", activity.getMDrawerLayout());
        assertNotNull("tvList is null", tvList);
        assertNotNull("cant find fragment", activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG));
    }

    @Test
    public void testViews() throws Exception {
        View movieListRoot = tvList.getView();
        assertNotNull("tvList rootView is null", movieListRoot);
        assertNotNull("progressBar is null", movieListRoot.findViewById(R.id.progressBar));
        AbsListView listView = (AbsListView) movieListRoot.findViewById(R.id.TVOnTheAirList);
        assertNotNull("listView is null", listView);
        assertNotNull("listView listener is null", listView.getOnItemClickListener());

    }

}

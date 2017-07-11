
package mindimagelibrary.sample.imageDB;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * About fragment used in the about view.
 */
public class About extends Fragment implements View.OnClickListener {

    private Resources res;

    public About() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.about, container, false);
        ImageView sourcestream = (ImageView) rootView.findViewById(R.id.sourcestream);
        sourcestream.setOnClickListener(this);
        ImageView tmdb = (ImageView) rootView.findViewById(R.id.tmdb);
        tmdb.setOnClickListener(this);
        TextView aboutSupportMail = (TextView) rootView.findViewById(R.id.aboutSupportMail);
        aboutSupportMail.setOnClickListener(this);
        res = getResources();
        getActivity().getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.aboutBackground));
        return rootView;
    }

    /**
     * Fired when clicked on Sourcestream or TMDB logo.
     *
     * @param v the view from which the event has been fired.
     */
    @Override
    public void onClick(View v) {
        //Get url from tag
        if (v.getId() == R.id.aboutSupportMail) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"info@sourcestream.de"});
            intent.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.MailSubject));
            intent.putExtra(Intent.EXTRA_TEXT, res.getString(R.string.MailDesc));
            startActivity(Intent.createChooser(intent, res.getString(R.string.MailSend)));
        } else {
            String url = (String) v.getTag();

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);

            //pass the url to intent data
            intent.setData(Uri.parse(url));

            startActivity(intent);
        }
    }

    /**
     * Fired when fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.background_material_light));
    }

}

package one.tesseract.weatherme;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] weekForecast = {
                "Mon 11/14 - Sunny - 31/17",
                "Tue 11/15 - Foggy - 21/8",
                "Wed 11/16 - Cloudy - 22/17",
                "Thu 11/17 - Rainy - 18/11",
                "Fri 11/18 - Foggy - 21/10",
                "Sat 11/19 - HELP, TRAPPED IN WEATHER STATION - 23/18",
                "Sun 11/20 - Sunny - 20/7"
        };

        ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textView,
                weekForecast);

        ListView listView = (ListView) rootView.findViewById(R.id.listView_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }
}

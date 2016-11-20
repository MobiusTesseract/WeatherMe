package one.tesseract.weatherme;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                new FetchWeatherTask().execute("Thessaloniki,GR");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.forecast_fragment, container, false);

        /**
         * Dummy data
         */
        String[] weekForecast = {
                "Mon 11/14 - Sunny - 31/17",
                "Tue 11/15 - Foggy - 21/8",
                "Wed 11/16 - Cloudy - 22/17",
                "Thu 11/17 - Rainy - 18/11",
                "Fri 11/18 - Foggy - 21/10",
                "Sat 11/19 - HELP, TRAPPED IN WEATHER STATION - 23/18",
                "Sun 11/20 - Sunny - 20/7"
        };

        /**
         * Create an adapter that makes ListView items from an Array or ArrayList
         */
        ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textView,
                weekForecast);

        /**
         * Make the ListView and populate it with the adapter
         */
        ListView listView = (ListView) rootView.findViewById(R.id.listView_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... strings) {

            // Verify size of params. If there's no query, there's nothing to look up.
            if (strings.length == 0) {
                return null;
            }

            /**
             * Network code to get the OpenWeatherMap JSON response
             */
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String units = "metric";
            int numberOfDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                Uri.Builder uriBuilder = new Uri.Builder()
                        .scheme("http")
                        .authority("api.openweathermap.org")
                        .path("data/2.5/forecast/daily")
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("cnt", Integer.toString(numberOfDays))
                        .appendQueryParameter("units", units)
                        .appendQueryParameter("q", strings[0])
                        .appendQueryParameter("appid", BuildConfig.OPEN_WEATHER_MAP_API_KEY);

                Log.v(LOG_TAG, "uriBuilder URI: " + uriBuilder.toString());
                URL url = new URL(uriBuilder.build().toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // stringBuilder for debugging.
                    stringBuilder.append(line).append("\n");
                }

                if (stringBuilder.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = stringBuilder.toString();
                Log.v(LOG_TAG, "Forecast JSON string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in
                // attempting to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }


            try {
                return getWeatherDataFromJson(forecastJsonStr, numberOfDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "getWeatherDataFromJson: " + e.getMessage(), e);
                e.printStackTrace();
            }

            // If getting or parsing the JSON response failed
            return null;
        }

        /* The date/time conversion code is going to be moved outside the AsyncTask later,
         * so for convenience we're breaking it out into its own method now.
         */

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            return roundedHigh + "/" + roundedLow;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numberOfDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            // Using the Gregorian Calendar Class instead of Time Class to get current date
            // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

            String[] resultStrings = new String[numberOfDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Getting to the current day
                Calendar calendar = new GregorianCalendar();
                calendar.add(Calendar.DATE, i);

                //Converting the integer value returned by Calendar.DAY_OF_WEEK to
                //a human-readable String
                SimpleDateFormat shortenedDateFormat =
                        new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
                day = shortenedDateFormat.format(calendar.getTime());


                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrings[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrings) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrings;
        }
    }
}

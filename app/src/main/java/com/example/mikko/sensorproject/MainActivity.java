package com.example.mikko.sensorproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.mikko.R;
import com.example.mikko.sensorproject.CompassActivity.Compass;
import com.example.mikko.sensorproject.CompassActivity.CompassFragment;
import com.example.mikko.sensorproject.autocomplete.Predictions;
import com.google.android.gms.vision.text.Text;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.PreferenceChangeEvent;

/**
 * Created by buckfast on 21.9.2017.
 */

public class MainActivity extends AppCompatActivity implements DragInterface, ChangeFragmentListener, DestinationInterface {

    private CameraFragment camerafrag;
    private MapSectionFragment mapfrag;
    private SearchView searchbar;
    private CompassFragment compassfrag;

    private ListView suggestionList;
    private ArrayAdapter<String> adapter;
    private List<String> suggestionListItems;

    private Predictions predictions;

    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private Point screenSize;
    private Point realScreenSize;

    private String currentFragment;
    private int currentDragState;
    private SharedPreferences sharedPref;

    private Timer timer;

    private GetPredictions getPredictions;

    private BroadcastReceiver broadcastReceiver;

    private Location myLocation;

    // private TextView latlon;

    @Override
    protected void onResume() {
        super.onResume();

        //startService(new Intent(this,LocationListenerService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        screenSize = new Point();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSize.set(displayMetrics.heightPixels, displayMetrics.widthPixels);
        realScreenSize = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(realScreenSize);

        currentFragment = "camera";

        myLocation = new Location("myLocation");


        startService(new Intent(this, LocationListenerService.class));

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // latlon.setText(intent.getExtras().get("lat")+",  "+intent.getExtras().get("lon") );
                String s = intent.getExtras().get("lat").toString();
                s += " " + intent.getExtras().get("lon").toString();
                Log.i("inio ", s);
                MapSectionFragment mapFragment = (MapSectionFragment) getSupportFragmentManager().findFragmentByTag("map");
                if (mapFragment != null) {

                    myLocation.setLatitude(Double.parseDouble(intent.getExtras().get("lat").toString()));
                    myLocation.setLongitude(Double.parseDouble(intent.getExtras().get("lon").toString()));
                    mapFragment.locationChanged((Double.parseDouble(intent.getExtras().get("lat").toString())), Double.parseDouble(intent.getExtras().get("lon").toString()));
                }


            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);


        suggestionListItems = new ArrayList<>();
        suggestionList = (ListView) findViewById(R.id.suggestions);
        // suggestionList = (ListView) getSupportActionBar().getCustomView().findViewById(R.id.suggestions);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestionListItems);
        suggestionList.setAdapter(adapter);


        if (savedInstanceState == null) {

            searchbar = (SearchView) findViewById(R.id.searchbar);
            camerafrag = new CameraFragment();
            mapfrag = new MapSectionFragment();

            compassfrag = new CompassFragment();


            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();

            if (currentFragment.equals("camera")) {
                fm.add(R.id.camera_fragment_placeholder, camerafrag, "camera");
            } else if (currentFragment.equals("compass")) {
                fm.add(R.id.camera_fragment_placeholder, compassfrag, "compass");
            }
            fm.add(R.id.map_fragment_placeholder, mapfrag, "map");


            fm.commit();

        } else {

            for (int i = 0; i < savedInstanceState.getInt("SUGGESTIONS_AMOUNT"); i++) {
                suggestionListItems.add(savedInstanceState.getString("SUGGESTION" + i));
            }


            TypedValue tv = new TypedValue();
            this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
            int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId); //168
            Log.i("inio", "actionbarheight: " + String.valueOf(actionBarHeight));

            FrameLayout fl = (FrameLayout) findViewById(R.id.camera_fragment_placeholder);
            ViewGroup.LayoutParams params = fl.getLayoutParams();
            Log.i("inio", "drag state  " + String.valueOf(savedInstanceState.getInt("DRAG_STATE")));
            if (getResources().getConfiguration().orientation == 2) {
                params.width = savedInstanceState.getInt("DRAG_STATE") + actionBarHeight;
            } else if (getResources().getConfiguration().orientation == 1) {
                params.height = savedInstanceState.getInt("DRAG_STATE");
            }
            fl.setLayoutParams(params);

            if (savedInstanceState.getBoolean("SUGGESTIONS_VISIBLE")) {
                suggestionList.setVisibility(View.VISIBLE);
            }

            //Log.i("inio", "tääällä taas");
            searchbar = (SearchView) findViewById(R.id.searchbar);

        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopService(new Intent(this, LocationListenerService.class));
        try {
            unregisterReceiver(broadcastReceiver);
        } catch(IllegalArgumentException e) {
            Log.e("onpause","unregisteriging broadcast", e);
        }
    }

    @Override
    public void onBackPressed() {
        if (suggestionList.getVisibility() != View.VISIBLE) {
            super.onBackPressed();
        }

        getSupportActionBar().getCustomView().findViewById(R.id.searchbar).setVisibility(View.VISIBLE);
        getSupportActionBar().getCustomView().findViewById(R.id.titlebar).setVisibility(View.GONE);
        suggestionList.setVisibility(View.GONE);

    }

    @Override
    public void dragData(float width, float height) {
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_fragment_placeholder);
        //fl.getLayoutParams().height += (int)height/2;
        Log.i("inio", "real; " + realScreenSize + "     usable; " + screenSize);
        //real; Point(1080, 1920)     usable; Point(1812, 1080)

        ViewGroup.LayoutParams params = fl.getLayoutParams();

        if (getResources().getConfiguration().orientation == 1) {
            params.height += (int) height;
        } else {
            params.width += (int) width;
        }
        fl.setLayoutParams(params);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);



        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_fragment_placeholder);
        ViewGroup.LayoutParams params = fl.getLayoutParams();
        Log.i("inio", "w: " + params.width + " h: " + params.height);
        if (getResources().getConfiguration().orientation == 1) {
            currentDragState = params.width;
        } else {
            currentDragState = params.height;
        }


        if (suggestionList.getVisibility() == View.VISIBLE) {
            outState.putBoolean("SUGGESTIONS_VISIBLE", true);
            outState.putInt("SUGGESTIONS_AMOUNT", suggestionListItems.size());
            for (int i = 0; i < suggestionListItems.size(); i++) {
                outState.putString("SUGGESTION" + i, suggestionListItems.get(i));
            }
        } else if (suggestionList.getVisibility() == View.GONE) {
            outState.putBoolean("SUGGESTIONS_VISIBLE", false);

        }

        outState.putString("FRAGMENT_STATE", currentFragment);
        outState.putInt("DRAG_STATE", currentDragState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);


        suggestionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                submitQuery(suggestionListItems.get(position));
                suggestionList.setVisibility(View.GONE);
            }
        });
        searchbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suggestionList.setVisibility(View.VISIBLE);
            }
        });
        searchbar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    suggestionList.setVisibility(View.VISIBLE);
                } else {
                    suggestionList.setVisibility(View.GONE);
                }
            }
        });

        searchbar.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                suggestionList.setVisibility(View.GONE);
                return false;
            }
        });
        searchbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(final String newText) {
                if (newText.isEmpty()) {
                    suggestionList.setVisibility(View.GONE);
                } else {
                    suggestionList.setVisibility(View.VISIBLE);
                }
                if (timer != null) {
                    timer.cancel();
                }

                timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //progressbar for waiting for results

                                getSupportActionBar().getCustomView().findViewById(R.id.progressbar).setVisibility(View.VISIBLE);

                            }
                        });

                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //maybe hide progressbar
                                getSupportActionBar().getCustomView().findViewById(R.id.progressbar).setVisibility(View.GONE);

                                //TextView tv = (TextView) findViewById(R.id.tekstview);
                                // tv.setText("haettu "+newText);

                                try {
                                    String encodedQuery = URLEncoder.encode(newText, "UTF-8");
                                    URL places = new URL("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" + encodedQuery + "&location=" + myLocation.getLatitude() + "," + myLocation.getLongitude() + "&radius=2000&key=AIzaSyBaUTD9YbQSNZlaBFRHW2t5GsBeI6CPv-A");
                                    getPredictions = new GetPredictions();
                                    getPredictions.execute(places);
                                } catch (Exception e) {
                                    Log.e("error","url", e);
                                }
                            }
                        });
                    }
                }, 500);

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                submitQuery(query);


                return true;
            }
        });

        return true;
    }

    private void submitQuery(String query) {
        if (timer != null) {
            timer.cancel();
        }
        suggestionList.setVisibility(View.GONE);

        hideKeyboard();

        MapSectionFragment mapFragment = (MapSectionFragment) getSupportFragmentManager().findFragmentByTag("map");
        if (mapFragment != null) {
            mapFragment.searchForLocation(query);
            this.getCurrentFocus().clearFocus();
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }


    private class GetPredictions extends AsyncTask<URL, Void, String> {

        protected String doInBackground(URL... url) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            URL urli = url[0];

            String json;

            try {
                urlConnection = (HttpURLConnection) urli.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    //stream was empty
                    return null;
                }
                json = buffer.toString();

                return json;

            } catch (IOException e) {
                Log.e("predictions", "error ", e);
                return null;

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("predictions", "error", e);
                    }
                }
            }
        }

        protected void onPostExecute(String result) {
            Gson gson = new Gson();
            predictions = gson.fromJson(result, Predictions.class);
            suggestionListItems.clear();
            for (int i = 0; i < predictions.getPredictions().size(); i++) {
                suggestionListItems.add(predictions.getPredictions().get(i).getDescription());
            }
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_manage) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).addToBackStack("prefs").commit();
            suggestionList.setVisibility(View.GONE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void changeEvent(String to) {
        if (currentFragment == "camera") {
            System.out.println("VAIHETAAN COMPASSIFRAGMENTTIIN");
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            compassfrag = new CompassFragment();
            fm.replace(R.id.camera_fragment_placeholder, compassfrag, "compass");
            fm.commit();
            currentFragment = "compass";
        } else if (currentFragment == "compass") {
            System.out.println("VAIHETAAN CAMERAFRAGMENTTIIN");
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            camerafrag = new CameraFragment();
            fm.replace(R.id.camera_fragment_placeholder, camerafrag, "camera");
            fm.commit();
            currentFragment = "camera";
        }
    }

    @Override
    public void changeLocation(Double longitude, Double latitude) {
        if (currentFragment == "compass") {
            compassfrag.setDest(latitude, longitude);
        }
    }

    public void getAzimuth(double azimuth) {
        if (azimuth > - 10 && azimuth < 10) {
           Log.i("Toimmiiii!!: " , String.valueOf(azimuth));
        }
    }

}

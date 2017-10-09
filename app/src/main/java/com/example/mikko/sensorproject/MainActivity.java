package com.example.mikko.sensorproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Camera;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import android.widget.ListView;
import android.widget.SearchView;

import com.example.mikko.R;
import com.example.mikko.sensorproject.CompassActivity.CompassFragment;
import com.example.mikko.sensorproject.autocomplete.Autocomplete;
import com.example.mikko.sensorproject.autocomplete.Predictions;
import com.example.mikko.sensorproject.interfaces.ChangeFragmentListener;
import com.example.mikko.sensorproject.interfaces.DestinationInterface;
import com.example.mikko.sensorproject.interfaces.DragInterface;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by buckfast on 21.9.2017.
 */

public class MainActivity extends AppCompatActivity implements DragInterface, ChangeFragmentListener, DestinationInterface {

    private CameraFragment camerafrag;
    private MapSectionFragment mapfrag;
    private SearchView searchbar;
    private CompassFragment compassfrag;


    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private Point screenSize;
    private Point realScreenSize;

    private String currentFragment;
    private int currentDragState;
    private SharedPreferences sharedPref;

    private Timer timer;

    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(80);
    Executor threadPoolExecutor = new ThreadPoolExecutor(60, 80, 10, TimeUnit.SECONDS, workQueue);


    private BroadcastReceiver broadcastReceiver;

    private Location myLocation;
    private Location lastDestination;

   // private String autocompleteJson;

    private Autocomplete autocomplete;

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
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.i("inio", "changed");

            }
        });

        autocomplete = new Autocomplete(this);
        autocomplete.setVisibility(View.GONE);

        lastDestination = new Location("destination");
        lastDestination.setLatitude(24);
        lastDestination.setLongitude(60);

        screenSize = new Point();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSize.set(displayMetrics.heightPixels, displayMetrics.widthPixels);
        realScreenSize = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(realScreenSize);



        currentFragment = sharedPref.getString("defaultview", "compass");
        Log.i("inio", "defaultview: "+sharedPref.getString("defaultview", "paska ei löydy"));





        myLocation = new Location("myLocation");


        /*
        Start service that listens for location changes
        */
        startService(new Intent(this, LocationListenerService.class));

        /*
        Create and register a broadcast receiver to receive user's current location and pass the data to fragments
        */
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



                    CompassFragment compassFragment = (CompassFragment) getSupportFragmentManager().findFragmentByTag("compass");
                    if (compassFragment != null ) {

                        compassFragment.locationChanged((Double.parseDouble(intent.getExtras().get("lat").toString())), Double.parseDouble(intent.getExtras().get("lon").toString()), Double.parseDouble(intent.getExtras().get("spd").toString()));
                    }

                CameraFragment cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag("camera");
                if (cameraFragment != null ) {

                    cameraFragment.locationChanged((Double.parseDouble(intent.getExtras().get("lat").toString())), Double.parseDouble(intent.getExtras().get("lon").toString()), Double.parseDouble(intent.getExtras().get("spd").toString()));
                }



            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);



        /*
        if nothing is saved (e.g app was just started) create and add fragments to activity
        */
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

            /*
            otherwise get data from savedinstance to keep the state (e.g. after orientation change)
            */
        } else {

            for (int i = 0; i < savedInstanceState.getInt("SUGGESTIONS_AMOUNT"); i++) {
                autocomplete.addToList(savedInstanceState.getString("SUGGESTION" + i));
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
                autocomplete.setVisibility(View.VISIBLE);
            }

            autocomplete.autocompleteJson = savedInstanceState.getString("AUTOCOMPLETE_JSON");

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

        sharedPref.edit().putInt("DRAG_STATE", currentDragState).commit();
    }

    @Override
    public void onBackPressed() {
        
        //if autocomplete suggestion list is visible, back button will hide it instead of pausing app straight away
        if (autocomplete.getVisibility() != View.VISIBLE) {
            super.onBackPressed();
        }


        autocomplete.setVisibility(View.GONE);

    }

    public Point navSize() {
        //nav right
        if (screenSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - screenSize.x, screenSize.y);
        }
        //nav bottom
        if (screenSize.y < realScreenSize.y) {
            return new Point(screenSize.x, realScreenSize.y - screenSize.y);
        }
        return new Point();
    }

/*
gets the amount of pixels moved when user touches screen and drags it in any direction.
when in portrait mode, param height is used and in landscape mode width is used
*/
    @Override
    public void dragData(float width, float height) {
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_fragment_placeholder);
        //fl.getLayoutParams().height += (int)height/2;
        //Log.i("inio", "real; " + realScreenSize + "     usable; " + screenSize);
        //real; Point(1080, 1920)     usable; Point(1812, 1080)

        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId); //168
        Log.i("inio", "actionbar: "+String.valueOf(actionBarHeight)+"   navbar: "+navSize());


        ViewGroup.LayoutParams params = fl.getLayoutParams();

        //change view's height or width and save it in realtime
        if (getResources().getConfiguration().orientation == 1) {
            if (params.height>=62) {
                params.height += (int) height;
            } else {
                params.height=62;
            }

        } else {
            params.width += (int) width;
        }
        Log.i("inio", String.valueOf(params.height));
        fl.setLayoutParams(params);
    }

    
    /*
    save state before pausing app
    */
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


        if (autocomplete.getVisibility() == View.VISIBLE) {
            outState.putBoolean("SUGGESTIONS_VISIBLE", true);
            outState.putInt("SUGGESTIONS_AMOUNT", autocomplete.getSuggestionsAmount());
            for (int i = 0; i < autocomplete.getSuggestionsAmount(); i++) {
                outState.putString("SUGGESTION" + i, autocomplete.getSuggestion(i));
            }
        } else if (autocomplete.getVisibility() == View.GONE) {
            outState.putBoolean("SUGGESTIONS_VISIBLE", false);

        }

        outState.putString("FRAGMENT_STATE", currentFragment);
        outState.putInt("DRAG_STATE", currentDragState);
        outState.putString("AUTOCOMPLETE_JSON", autocomplete.autocompleteJson);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        getSupportActionBar().getCustomView().findViewById(R.id.searchbar).setVisibility(View.VISIBLE);
        getSupportActionBar().getCustomView().findViewById(R.id.titlebar).setVisibility(View.GONE);

        /*
        when suggestion list item is clicked, it is used to submit a query to google maps api
        */
        autocomplete.getSuggestionList().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                autocomplete.setVisibility(View.GONE);

                searchbar.setQuery(Utils.removeCountry(autocomplete.getSuggestion(position)),false);

                submitQuery(autocomplete.trimQueryOnClick(position));


            }
        });
        searchbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autocomplete.setVisibility(View.VISIBLE);
            }
        });
        searchbar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    autocomplete.setVisibility(View.VISIBLE);
                } else {
                    autocomplete.setVisibility(View.GONE);
                }
            }
        });

        searchbar.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                autocomplete.setVisibility(View.GONE);
                return false;
            }
        });
        
        
        searchbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(final String newText) {
                if (newText.isEmpty()) {
                    autocomplete.setVisibility(View.GONE);
                } else {
                    autocomplete.setVisibility(View.VISIBLE);
                }

                if (sharedPref.getBoolean("autocomplete", true)) {
                    if (timer != null) {
                        timer.cancel();
                    }

                    if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("autocomplete", true)) {
                        //if autocomplete is enabled in settings, start timer after specific time everytime user changes text in searchview
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
                                    //wait before showing results because user can have pauses while typing
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
                                            URL places = new URL("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" + encodedQuery + "&location=" + myLocation.getLatitude() + "," + myLocation.getLongitude() + "&radius=10000&strictbounds&key=AIzaSyBaUTD9YbQSNZlaBFRHW2t5GsBeI6CPv-A");
                                            //  Log.i("urli on: " , suggestionListItems.get(0));
                                            // getPredictions.executeOnExecutor(threadPoolExecutor, places);

                                            //data is fetched in thread to prevent UI getting stuck
                                            PredThread sc = new PredThread(uiHandler, places);
                                            Thread t = new Thread(sc);
                                            t.start();


                                        } catch (Exception e) {
                                            Log.e("error", "url", e);
                                        }
                                    }
                                });
                            }
                        }, 500);
                    }
                }

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

    private Handler uiHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 0){

                autocomplete.populateList(msg.obj.toString());
            }
        }
    };

    /*
    private String removeCountry(String string) {
        if (string.length() > 0) {
            int lastComma = string.lastIndexOf(",");
            if (lastComma != -1) {
                return string.substring(0, lastComma);

            }
        }
        return null;
    }*/

   
    private void submitQuery(String query) {
        if (timer != null) {
            timer.cancel();
        }
        autocomplete.setVisibility(View.GONE);

        hideKeyboard();
        Log.i("inio",query);
        MapSectionFragment mapFragment = (MapSectionFragment) getSupportFragmentManager().findFragmentByTag("map");
        if (mapFragment != null) {

            mapFragment.requestDirections(" ",query);




            this.getCurrentFocus().clearFocus();
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

/*
    AsyncTask<URL, Void, String> getPredictions = new AsyncTask<URL, Void, String>() {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i("preex " , "vihdoin toimii");
        }

        @Override
        protected String doInBackground(URL... url) {
            Log.i("workiiko" , String.valueOf(url));
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
                Log.i("workiiko" , "ew");
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
        @Override
        protected void onPostExecute(String result) {
            Gson gson = new Gson();
            predictions = gson.fromJson(result, Predictions.class);
            suggestionListItems.clear();
            for (int i = 0; i < predictions.getPredictions().size(); i++) {
                suggestionListItems.add(predictions.getPredictions().get(i).getDescription());
            }
            adapter.notifyDataSetChanged();
        }
    };
*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_manage:
                //show settings screen
                getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).addToBackStack("prefs").commit();
                autocomplete.setVisibility(View.GONE);
                break;
            case R.id.action_info:
                //show/hide info box in map fragment
                MapSectionFragment f  = (MapSectionFragment) getSupportFragmentManager().findFragmentByTag("map");
                if (f!= null) {
                    f.toggleInfo();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    this is called when icon in corner of compass or ar fragment is pressed.
    changes current fragment to another.
    */
    @Override
    public void changeEvent(String to) {

        if ((getSupportFragmentManager().findFragmentByTag("compass") == null)) {
            System.out.println("VAIHETAAN COMPASSIFRAGMENTTIIN");
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            compassfrag = new CompassFragment();
            Bundle bundle = new Bundle();
            bundle.putDouble("latitude", lastDestination.getLatitude());
            bundle.putDouble("longitude", lastDestination.getLongitude());
            compassfrag.setArguments(bundle);
            fm.replace(R.id.camera_fragment_placeholder, compassfrag, "compass");
            fm.commit();
            currentFragment = "compass";

           // compassfrag.setDest(lastDestination.getLatitude(),lastDestination.getLongitude());

        } else {
            System.out.println("VAIHETAAN CAMERAFRAGMENTTIIN");
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            camerafrag = new CameraFragment();
            Bundle bundle = new Bundle();
            bundle.putDouble("latitude", lastDestination.getLatitude());
            bundle.putDouble("longitude", lastDestination.getLongitude());
            camerafrag.setArguments(bundle);
            fm.replace(R.id.camera_fragment_placeholder, camerafrag, "camera");
            fm.commit();

            //camerafrag.setDest(lastDestination.getLatitude(),lastDestination.getLongitude());

            currentFragment = "camera";

        }



    }

    /*
    gets destination location from map and passes it to either ar or compass fragment
    */
    @Override
    public void changeLocation(Double latitude, Double longitude) {

        lastDestination.setLatitude(latitude);
        lastDestination.setLongitude(longitude);
        if (getSupportFragmentManager().findFragmentByTag("compass") != null) {
            CompassFragment c = (CompassFragment) getSupportFragmentManager().findFragmentByTag("compass");
            c.setDest(latitude, longitude);
        } else if (getSupportFragmentManager().findFragmentByTag("camera") != null) {
            CameraFragment c = (CameraFragment) getSupportFragmentManager().findFragmentByTag("camera");
            c.setDest(latitude, longitude);
        }
    }

    public void getAzimuth(double azimuth) {
        if (azimuth > - 10 && azimuth < 10) {
           Log.i("Toimmiiii!!: " , String.valueOf(azimuth));
        }
    }

}

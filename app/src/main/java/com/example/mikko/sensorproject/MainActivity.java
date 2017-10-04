package com.example.mikko.sensorproject;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.SearchView;

import com.example.mikko.R;
import com.example.mikko.sensorproject.CompassActivity.Compass;
import com.example.mikko.sensorproject.CompassActivity.CompassFragment;

/**
 * Created by buckfast on 21.9.2017.
 */

/*TODO!!!!!
 -kartan autorotate
 -autocomplete ja haku
 -kameran ratio
 -asetukset
 -sql

 -places:AIzaSyBaUTD9YbQSNZlaBFRHW2t5GsBeI6CPv-A

*/


public class MainActivity extends AppCompatActivity implements DragInterface, ChangeFragmentListener, DestinationInterface{

    private CameraFragment camerafrag;
    private MapSectionFragment mapfrag;
    private SearchView searchbar;
    private CompassFragment compassfrag;

    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private Point screenSize;
    private Point realScreenSize;

    private String currentFragment;
    private int currentDragHeight;
    private int currentDragWidth;
    private SharedPreferences sharedPref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_fragment_placeholder);
        ViewGroup.LayoutParams lp = fl.getLayoutParams();

        screenSize = new Point();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSize.set(displayMetrics.heightPixels, displayMetrics.widthPixels);

        currentFragment = "camera";




        realScreenSize = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(realScreenSize);


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);


       // View view =getSupportActionBar().getCustomView();

        if (savedInstanceState == null) {

            sharedPref = this.getPreferences(this.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("DRAGSIZE", lp.height);
            Log.i("inio", "eka!!:" + lp.height);
            editor.commit();

            searchbar = (SearchView) findViewById(R.id.searchbar);
            camerafrag = new CameraFragment();
            mapfrag = new MapSectionFragment();
            compassfrag = new CompassFragment();


            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();

            fm.add(R.id.camera_fragment_placeholder,camerafrag, "camera");
            fm.add(R.id.map_fragment_placeholder, mapfrag, "map");


            fm.commit();

        } else {

/*
            sharedPref = this.getPreferences(this.MODE_PRIVATE);
            if (screenSize.x > screenSize.y) {

                lp.height = sharedPref.getInt("DRAGSIZE",700);
            } else {

                lp.width = sharedPref.getInt("DRAGSIZE",700);
            }
            fl.setLayoutParams(lp);
            */

            //Log.i("inio", "tääällä taas");
            searchbar = (SearchView) findViewById(R.id.searchbar);

            /*
            if (currentFragment == "camera") {
                camerafrag = (CameraFragment) getSupportFragmentManager().findFragmentByTag("camera");
            } else if (currentFragment == "compass") {
                compassfrag = (CompassFragment) getSupportFragmentManager().findFragmentByTag("compass");
            }
            */

        }


    }


    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onBackPressed() {
        getSupportActionBar().getCustomView().findViewById(R.id.searchbar).setVisibility(View.VISIBLE);
        getSupportActionBar().getCustomView().findViewById(R.id.titlebar).setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    public void dragData(float width, float height, int rotation) {
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_fragment_placeholder);
        //fl.getLayoutParams().height += (int)height/2;
        System.out.println(realScreenSize+"     "+screenSize);


        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);

        ViewGroup.LayoutParams params = fl.getLayoutParams();

        if (rotation ==0 || rotation == 180) {

            if (params.height <  screenSize.x-actionBarHeight) {
                params.height += (int) height;
                currentDragWidth = params.height;
            } else {
                params.height = screenSize.x-actionBarHeight-1;
            }
        } else {

                params.width += (int) width;
                 currentDragWidth = params.width;

        }
        fl.setLayoutParams(params);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);



        outState.putString("FRAGMENT_STATE", currentFragment);

//TODO korjaa et ei mee nollaksi
        sharedPref = this.getPreferences(this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Log.i("inio","orientation change  cur wid "+currentDragWidth);
        editor.putInt("DRAGSIZE", currentDragWidth);

        editor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
       // menu.setGroupVisible(R.id.group_main, true);

        //MapSectionFragment mapFragment;


        /*
        searchbar.setOnQueryTextFocusChangeListener(new SearchView.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    System.out.println("KLIKATTU HEHHEE");
                }
            }
        });*/







        searchbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                hideKeyboard();

                //MapSectionFragment mapFragment = (MapSectionFragment) getSupportFragmentManager().findFragmentByTag("map");
                if (mapfrag != null) {
                    mapfrag.searchForLocation(query);
                }


                return true;
            }
        });

        return true;
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof SearchView)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard();
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_manage){
                getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).addToBackStack("prefs").commit();
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
        } else if (currentFragment  =="compass") {
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

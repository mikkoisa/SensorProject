package com.example.mikko.sensorproject.compass;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mikko.R;
import com.example.mikko.sensorproject.utils.DragUtils;
import com.example.mikko.sensorproject.MainActivity;
import com.example.mikko.sensorproject.interfaces.ChangeFragmentListener;
import com.example.mikko.sensorproject.interfaces.DragInterface;


public class CompassFragment extends Fragment implements Compass.OnAngleChangedListener{


    private static final String TAG = "CompassActivity";

    private Compass compass;
    DragInterface dragCallback;
    DragUtils dragUtils;
    ChangeFragmentListener changeFragmentListener;


    public CompassFragment() {

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        dragCallback = (DragInterface) context;
        dragUtils = new DragUtils();
        changeFragmentListener = (ChangeFragmentListener) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_compass, container, false);
        compass = new Compass(getActivity(), this) ;
        Bundle bundle = this.getArguments();

        if (bundle != null) {
            double lat = bundle.getDouble("latitude", 60);
            double lon = bundle.getDouble("longitude", 24);

            setDest(lat,lon);
        }
        compass.arrowView = (ImageView) v.findViewById(R.id.imgPointer);
        compass.tv = (TextView)v.findViewById(R.id.txtTest);
        compass.tv.setText(" ");

        Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        dragUtils.setupViewDrag(v, dragCallback);

        //onStart();

        ImageView cornerIcon = (ImageView) v.findViewById(R.id.cornericon);
        cornerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFragmentListener.changeEvent("camera");
            }
        });

        if (savedInstanceState != null) {

            setDest(savedInstanceState.getDouble("lat"), savedInstanceState.getDouble("lon"));
        }

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        compass.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        compass.stop();
    }

    public void setDest(Double lat, Double lon){
        compass.setCoord(lat, lon);
    }
    public void locationChanged(Double newLat, Double newLon, Double speed) {

        compass.setMyLocation(newLat, newLon, speed);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putDouble("lat", compass.getDesLat());
        outState.putDouble("lon", compass.getDesLon());
    }
    public double getAzimuth(){
        return compass.getAzimuth();
    }

    @Override
    public void onAngleChanged(Float azimuth) {
        //Log.i("jee" , String.valueOf(azimuth));
        ((MainActivity)getActivity()).getAzimuth(azimuth);
    }
}
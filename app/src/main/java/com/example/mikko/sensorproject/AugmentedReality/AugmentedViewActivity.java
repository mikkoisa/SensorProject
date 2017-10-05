package com.example.mikko.sensorproject.AugmentedReality;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.mikko.R;
import com.example.mikko.sensorproject.interfaces.DestinationInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AugmentedViewActivity extends Activity implements SurfaceHolder.Callback, CurrentLocation.OnLocationChangedListener, CurrentAzimuth.OnAzimuthChangedListener {

    private double screenWidth = 0d, screenHeight = 0d;

    private DestinationInterface dest;

    private Camera mCamera;

    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraViewOn = false;

    private CurrentLocation myCurrentLocation;
    private CurrentAzimuth myCurrentAzimuth;
    private TargetPoint targetPoint;

    private double myLatitude = 60.1729325;
    private double myLongitude = 24.8125536;

    private double azimuthReal = 0;
    private double azimuthTheoretical = 0;
    private static double AZIMUTH_ACCURACY = 20;

    private TextView locDesc;
    private ImageView imgArrowTarget;
    private ImageView imgArrowLeft;
    private ImageView imgArrowRight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_augmented);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        locDesc = (TextView)findViewById(R.id.txtLocation);


        //generate surface view
        setupLayout();
        //connect to location api etc.
        setupListeners();
        //This sets the destination
        setTargetPoint();
    }

    private void setupLayout() {
        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceAug);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    private void setupListeners() {
        myCurrentLocation = new CurrentLocation(this, getApplicationContext());
        myCurrentLocation.buildApi(this);
        myCurrentLocation.start();

        myCurrentAzimuth = new CurrentAzimuth(this, getApplicationContext());
        myCurrentAzimuth.start();
    }

    //Set target point
    private void setTargetPoint() {
        targetPoint = new TargetPoint(
                60.173551,
                24.815205
        );
    }
    //Calculate the direction where the given target point is (in a coordinate plane)
    public double calculateTheoreticalAzimuth() {
        double dX = targetPoint.getLatitude() - myLatitude;
        double dY = targetPoint.getLongitude() -myLongitude;

        double phiAngle;
        double tanPhi;
        double azimuth = 0;

        tanPhi = Math.abs(dY / dX);
        phiAngle = Math.atan(tanPhi);
        phiAngle = Math.toDegrees(phiAngle);

        //TODO: some directions dont work
        if (dX > 0 && dY > 0) { // First quarter in coordinate system
            return azimuth = phiAngle;
        } else if (dX < 0 && dY > 0) { // Second quarter
            return azimuth = 180 - phiAngle;
        } else if (dX < 0 && dY < 0) { // Third quarter
            return azimuth = 180 + phiAngle;
        } else if (dX > 0 && dY < 0) { // Fourth quarter
            return azimuth = 360 - phiAngle;
        }

        return phiAngle;
    }


    private List<Double> calculateAzimuthAccuracy(double azimuth) {
        double minAngle = azimuth - AZIMUTH_ACCURACY;
        double maxAngle = azimuth + AZIMUTH_ACCURACY;
        List<Double> minMax = new ArrayList<Double>();

        if (minAngle < 0)
            minAngle += 360;

        if (maxAngle >= 360)
            maxAngle -= 360;

        minMax.clear();
        minMax.add(minAngle);
        minMax.add(maxAngle);

        return minMax;
    }

    private boolean isBetween(double minAngle, double maxAngle, double azimuth) {
        if (minAngle > maxAngle) {
            if (isBetween(0, maxAngle, azimuth) && isBetween(minAngle, 360, azimuth))
                return true;
        } else {
            if (azimuth > minAngle && azimuth < maxAngle)
                return true;
        }
        return false;
    }




    private void updateDescription() {
        locDesc.setText("Lat: " + myLatitude + "Lon: " + myLongitude + "\n" + azimuthReal + "\n" +azimuthTheoretical );
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        Log.i("Surface Created", "fd");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (isCameraViewOn) {
            mCamera.stopPreview();
            isCameraViewOn = false;
            Log.i("Surface CHanged 1", "1");
        }
        if (mCamera != null) {
            try {
                //initiates camera preview
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                isCameraViewOn = true;
                Log.i("Surface CHanged 2", "2");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        isCameraViewOn = false;
        Log.i("Surface destroyed", "yes");
    }

    @Override
    public void onLocationChanged(Location currentLocation) {
        //myLatitude = currentLocation.getLatitude();
       // myLongitude = currentLocation.getLongitude();
        azimuthTheoretical = calculateTheoreticalAzimuth();
        updateDescription();
    }

    @Override
    public void onAzimuthChanged(float azimuthChangedFrom, float azimuthChangedTo) {


        azimuthReal = azimuthChangedTo;
        azimuthTheoretical = calculateTheoreticalAzimuth();

        double posInPx = azimuthReal * (screenWidth / 90d);



        imgArrowTarget = (ImageView) findViewById(R.id.imgArrow);
        imgArrowLeft = (ImageView)findViewById(R.id.imgLeftArrow);
        imgArrowRight = (ImageView)findViewById(R.id.imgRightArrow);

        double minAngle = calculateAzimuthAccuracy(azimuthTheoretical).get(0);
        double maxAngle = calculateAzimuthAccuracy(azimuthTheoretical).get(1);


        //This diplays the correct arrow
        if (isBetween(minAngle, maxAngle, azimuthReal)) {
            imgArrowLeft.setVisibility(View.INVISIBLE);

            imgArrowRight.setVisibility(View.INVISIBLE);
        } else if (isBetween(minAngle, maxAngle, azimuthReal-45)){
            imgArrowTarget.setVisibility(View.INVISIBLE);
            imgArrowLeft.setVisibility(View.VISIBLE);
            imgArrowRight.setVisibility(View.INVISIBLE);
        } else if (isBetween(minAngle, maxAngle, azimuthReal+45)){
            imgArrowTarget.setVisibility(View.INVISIBLE);
            imgArrowLeft.setVisibility(View.INVISIBLE);
            imgArrowRight.setVisibility(View.VISIBLE);
        } else {
            imgArrowTarget.setVisibility(View.INVISIBLE);
            imgArrowLeft.setVisibility(View.INVISIBLE);
            imgArrowRight.setVisibility(View.INVISIBLE);
        }

        updateDescription();
    }

    @Override
    protected void onStop() {
        myCurrentAzimuth.stop();
        myCurrentLocation.stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myCurrentAzimuth.start();
        myCurrentLocation.start();
    }


}

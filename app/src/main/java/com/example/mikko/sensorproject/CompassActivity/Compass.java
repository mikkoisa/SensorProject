package com.example.mikko.sensorproject.CompassActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mikko.sensorproject.MapSectionFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;



public class Compass implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gsensor;
    private Sensor msensor;

    int i = 0;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];

    private float azimuth = 0f;
    private float correctAzimuth = 0;

    private Context con;
    TextView tv;

    private MapSectionFragment map;

    //Default location coordinates (not really used)
    private double loclat = 60.221951;
    private double loclon = 24.804374;
    //Default destination coordinates (replace with coordinates from map API)
    private double deslat = 60.175047;
    private double deslon = 24.814067;

    // compass arrow that points to destination
    ImageView arrowView = null;

    private OnAngleChangedListener mAngleListener;
    //SIIRÄ CAMERAFRAGMENT KUTSUMAAN LSITENERIÄ
    //Initialize sensors
    public Compass(Context context, OnAngleChangedListener anglelistener) {
        con = context;
        this.mAngleListener = anglelistener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public double getDesLat() {
        return deslat;
    }
    public double getDesLon() {
        return deslon;
    }

    public void start() {
        //GetLoc myTask = new GetLoc();
        //myTask.execute();

        sensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setCoord(Double lat, Double lon){
        deslat = lat;
        deslon = lon;
    }
    public void setMyLocation(Double lat, Double lon) {
        loclat = lat;
        loclon = lon;
        tv.setText(String.valueOf(loclat) + "\n" + String.valueOf(loclon) + "\n" + String.valueOf(deslat)+ "\n" + String.valueOf(deslon)  );

    }

    public double getAzimuth() {
        return azimuth;
    }

    //This gets called constantly when the sensors change
    //TODO: change update interval
    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //Add a low-pass filter to make the arrow move smoothly
                mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                        * event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                        * event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                        * event.values[2];
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                        * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                        * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                        * event.values[2];

            }
            //Calculate the destination (north) with rotationMatrix
            float R[] = new float[9];
            float I[] = new float[9];
            //getRotationMatrix
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                    mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                double destDeg = calculateAngle();
                //TODO: fix the calculation ( if -destdeg gives negative value
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (float) ((azimuth + 360) % 360 - destDeg);


                if (i>5){
                   // Log.i("Direction: " , String.valueOf(azimuth ) + " " + String.valueOf(destDeg));
                    mAngleListener.onAngleChanged(azimuth);
                    i = 0;
                }

                adjustArrow();
                i++;

            }
        }
    }

    public interface OnAngleChangedListener {
        void onAngleChanged(Float azimuth);
    }

    //Rotate the arrow
    private void adjustArrow() {
        if (arrowView == null) {
           // Log.i("Info: ", "arrow view is not set");
            return;
        }
        //Log.i("Info: ", "will set rotation from " + correctAzimuth + " to "+ azimuth);

        //Initialize the rotation animation
        Animation an = new RotateAnimation(-correctAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        correctAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    //Calculates the angle in which the destination coordinate differentiates from magnetic north
    private double calculateAngle() {
        double dX = deslat - loclat;
        double dY = deslon - loclon;

        double phiAngle;
        double tanPhi;
        double azimuth = 0;

        tanPhi = Math.abs(dY / dX);
        phiAngle = Math.atan(tanPhi);
        phiAngle = Math.toDegrees(phiAngle);

        //checks in which direction the destination point is located related to user location (in coordinate system)
        if (dX > 0 && dY > 0) { // Destination located in first quarter of coordinate system
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



    //Async Task inner class for location tracking
    /*
    private class GetLoc extends AsyncTask<Void, Double, Void>  {
        private FusedLocationProviderClient mFusedLocationClient;
        private LocationRequest mLocationRequest;
        private LocationCallback mLocationCallback;

        private Location loc;
        double mLatitude = 1;
        double mLongitude = 1;
        double speed = 1;
        private boolean started = false;

        //The "main-activity" of the async task class
        protected Void doInBackground(Void... params) {
            //Prepare the loop if not already created
            if (!started) {
                Looper.prepare();
                started = true;
            }
            //Initiate the location request settings and callback and start the gatherign of updates
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(con);
            createLocationCallback();
            createLocationRequest();
            startLocationUpdates();
            Looper.loop();
            //Permission check
            if (ActivityCompat.checkSelfPermission(con, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(con, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Permission check
            }
            return null;
        }

        //These are the settings used for the request
        void createLocationRequest() {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        //Callback i.e handles the result of the request by publishing the results to onProgressUpdate()
        void createLocationCallback() {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    loc = locationResult.getLastLocation();
                    speed = loc.getSpeed();
                    publishProgress(loc.getLatitude(), loc.getLongitude());
                }
            };
        }

        //starts the loop for updating location
        private void startLocationUpdates() {
            if (ActivityCompat.checkSelfPermission(con, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(con, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            //TODO: not sure if this needs Looper.myLooper() or null
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            publishProgress(mLatitude, mLongitude);
        }


        //This updates the UI with the new values
        protected void onProgressUpdate(Double... progress) {
            Log.i("New location: " , String.valueOf(progress[0]));
            loclat = progress[0];
            loclon = progress[1];
            //tv.setText(String.valueOf(loclat) + "\n" + String.valueOf(loclon) + "\n" + String.valueOf(speed) + "\n" + String.valueOf(deslat)+ "\n" + String.valueOf(deslon)  );
        }

    }*/
}
package com.example.mikko.sensorproject.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mikko.sensorproject.map.MapSectionFragment;



public class Compass implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gsensor;
    private Sensor msensor;

    private int i = 0;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];

    private float azimuth;
    private float correctAzimuth;

    private Context con;
    TextView tv;

    private MapSectionFragment map;

    private double speed;

    //Default location coordinates (not really used)
    private double loclat = 68.221951;
    private double loclon = 27.804374;
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

    double getDesLat() {
        return deslat;
    }
    double getDesLon() {
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
    public void setMyLocation(Double lat, Double lon, Double spd) {
        loclat = lat;
        loclon = lon;
        speed = spd;

        Double time = calculateDistance();


        //tv.setText(String.valueOf(loclat) + "\n" + String.valueOf(loclon) + "\n" + String.valueOf(deslat)+ "\n" + String.valueOf(deslon) + "\n" +String.valueOf(speed) + "\n" +String.valueOf(time)  );

    }

    double getAzimuth() {
        return azimuth;
    }

    //This gets called constantly when the sensors change

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


                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (float) ((azimuth + 360) % 360 - destDeg);


                if (i>5){
                   // Log.i("Direction: " , String.valueOf(azimuth ) + " " + String.valueOf(destDeg));
                    mAngleListener.onAngleChanged(azimuth);
                    //Log.i("azimuth", String.valueOf(azimuth));
                    //Log.i("azimuth", String.valueOf(deslat)+", "+deslon);
                    Log.i("azimuth", String.valueOf(destDeg));

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
       // Log.i("Info: ", "will set rotation from " + correctAzimuth + " to "
        //        + azimuth);
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

    private double calculateDistance(){
        Location loc1 = new Location("");
        loc1.setLatitude(loclat);
        loc1.setLongitude(loclon);

        Location loc2 = new Location("");
        loc2.setLatitude(deslat);
        loc2.setLongitude(deslon);

        double distance =  loc1.distanceTo(loc2);
        return distance / speed;
    }

}
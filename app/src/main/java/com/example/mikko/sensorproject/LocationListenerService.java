package com.example.mikko.sensorproject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.LocationListener;


/**
 * Created by buckfast on 28.9.2017.
 */

public class LocationListenerService extends Service {
    private static int interval = 1000;
    private static float distance = 0f;

    private LocationManager locationManager = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class LocationListener implements android.location.LocationListener {
        Location lastLocation;

        public LocationListener(String provider) {
            Log.i("inio", "LocationListener " + provider);
            lastLocation = new Location(provider);
            //System.out.println("LASTI "+lastLocation);


/*
            Intent intent = new Intent("location_update");
            intent.putExtra("lat", lastLocation.getLatitude());
            intent.putExtra("lon",  lastLocation.getLongitude());
            sendBroadcast(intent);*/
        }

        @Override
        public void onLocationChanged(Location location) {
            lastLocation.set(location);
           // System.out.println(location);
           // Log.i("inio", location.getLatitude()+", "+location.getLongitude());
            Intent intent = new Intent("location_update");
            intent.putExtra("lat", location.getLatitude());
            intent.putExtra("lon",  location.getLongitude());
            sendBroadcast(intent);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }


    LocationListener[] locationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onCreate() {


        if (locationManager == null) {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, distance,  locationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i("inio", "location update failed", ex);
        } catch (IllegalArgumentException ex) {
            Log.i("inio", "no network provider" + ex.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance,  locationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i("inio", "location update failed", ex);
        } catch (IllegalArgumentException ex) {
            Log.i("inio", "no gps provider" + ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            for (int i=0; i<locationListeners.length; i++) {
                try {
                    locationManager.removeUpdates(locationListeners[i]);
                } catch (Exception ex) {

                }
            }
        }
    }

}

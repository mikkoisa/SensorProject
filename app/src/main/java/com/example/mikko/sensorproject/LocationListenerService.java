package com.example.mikko.sensorproject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;



public class LocationListenerService extends Service {

    private LocationManager locationManager = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class LocationListener implements android.location.LocationListener {
        Location lastLocation;

        LocationListener(String provider) {
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
            intent.putExtra("spd", location.getSpeed());
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

        int interval = 1000;
        float distance = 0f;
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, distance,  locationListeners[1]);
        } catch (java.lang.SecurityException e) {
            Log.i("inio", "location update failed", e);
        } catch (IllegalArgumentException e) {
            Log.i("inio", "no network provider" + e.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance,  locationListeners[0]);
        } catch (java.lang.SecurityException e) {
            Log.i("inio", "location update failed", e);
        } catch (IllegalArgumentException e) {
            Log.i("inio", "no gps provider" + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            for (LocationListener locationListener : locationListeners) {
                try {
                    locationManager.removeUpdates(locationListener);
                } catch (Exception ignored) {

                }
            }
        }
    }

}

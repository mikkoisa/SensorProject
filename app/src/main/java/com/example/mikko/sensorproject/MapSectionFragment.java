package com.example.mikko.sensorproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.mikko.R;
import com.example.mikko.sensorproject.AugmentedReality.CurrentLocation;
import com.example.mikko.sensorproject.interfaces.DestinationInterface;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MapSectionFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap googleMap;
    private Geocoder geocoder;
    private MapView mapview;
    private CurrentLocation currentLocation;
    private Location currentLoc;
    private double desLat = 1.1;
    private double desLon = 2.2;
    private String lastQuery ="";
    private Bundle savedInstanceState;
    private DestinationInterface dest;
    private boolean startCentered = false;
    private LatLng firstPolyLine;
    public MapSectionFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        dest = (DestinationInterface) context;
//TODO: hae oma sijainti heti
        currentLoc = new Location("ass");
        Log.i("inio", "map on attach");
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        mapview = (MapView) v.findViewById(R.id.mapview);
        MapsInitializer.initialize(getContext());
        geocoder = new Geocoder(getContext(), Locale.getDefault());
        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        //MapsInitializer.initialize(this.getActivity());
        setRetainInstance(true);



        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getCurrentAddress() != null) {
            outState.putString("startAddress", getCurrentAddress().getAddressLine(0));
        }
        if (!lastQuery.isEmpty()) {
            outState.putString("endAddress", lastQuery);
        }
        outState.putBoolean("startCentered", startCentered);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mapview.onCreate(savedInstanceState);
        //mapview.onResume();
        mapview.getMapAsync(this);

        Log.i("inio","map onviewcreated");
        if (savedInstanceState != null) {
            this.savedInstanceState =savedInstanceState;
            startCentered = savedInstanceState.getBoolean("startCentered", startCentered);



        } else {
            Log.i("inio", "mitaan ei seivattu ");
        }
        //currentLocation = new CurrentLocation(this, getContext());
        //currentLocation.buildApi(this);
        //myCurrentLocation.start();


    }

    /*
        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
           // SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapppi);
            //smf.getMapAsync(this);
        }
    */
    @Override
    public void onResume() {
        super.onResume();
        mapview.onResume();
    }

    private Address getCurrentAddress() {
        List<Address> addresses = null;
        Log.i("inio", "FUUUK "+String.valueOf(currentLoc.getLatitude())+" "+String.valueOf(currentLoc.getLongitude()));
        try {
            addresses = geocoder.getFromLocation(currentLoc.getLatitude(), currentLoc.getLongitude(),1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses.size() != 0) {
            return addresses.get(0);
        } else {
            return null;
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        Log.i("inio","onmapready");

        googleMap.setOnMyLocationButtonClickListener(this);
        googleMap.setOnMyLocationClickListener(this);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);

        setupGoogleMapScreenSettings(googleMap);



        if (this.savedInstanceState != null) {

            // Restore last state
            if (savedInstanceState.get("startAddress") != null && savedInstanceState.get("endAddress") != null) {
                String start = savedInstanceState.get("startAddress").toString();
                String end = savedInstanceState.get("endAddress").toString();
                Log.i("ingo", start + "   " + end);

                DirectionsResult results = requestDirections(start, end, TravelMode.WALKING);
                if (results != null) {
                    Log.i("inio", "noniin!!!!!!!1");
                    lastQuery = end;
                    googleMap.clear();
                    desLat = results.routes[0].legs[0].endLocation.lat;
                    desLon = results.routes[0].legs[0].endLocation.lng;
                    dest.changeLocation(desLat, desLon);
                    addPolyline(results, googleMap);
                    moveCamera(results.routes[0], googleMap);
                    addMarkers(results, googleMap);

                }
            }
        } else {

        }
/*
            DirectionsResult results = getDirectionsDetails("vanha maantie 6 espoo", "Leppävaaran asema", TravelMode.WALKING);
            if (results != null) {

                addPolyline(results, googleMap);
                moveCamera(results.routes[0], googleMap);
                addMarkers(results, googleMap);


            }*/

    }

    public void searchForLocation(String query) {
        Address myAddress = getCurrentAddress();

        if (myAddress != null) {
            Log.i("inio", myAddress.getAddressLine(0));

            DirectionsResult results = requestDirections(myAddress.getAddressLine(0), query, TravelMode.WALKING);
            if (results != null) {


                lastQuery = query;
                googleMap.clear();
                if (results.routes.length  != 0) {
                    desLat = results.routes[0].legs[0].endLocation.lat;
                    desLon = results.routes[0].legs[0].endLocation.lng;
                    dest.changeLocation(desLat, desLon);
                    addPolyline(results, googleMap);
                    moveCamera(results.routes[0], googleMap);
                    addMarkers(results, googleMap);
                }

            } else {
                Toast.makeText(getContext(), "Cannot locate "+query, Toast.LENGTH_SHORT).show();
            }
        } else {

        }

    }



    private void addPolyline(DirectionsResult results, GoogleMap mMap) {
        List<LatLng> path = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());
        firstPolyLine = path.get(1);
        //mMap.addMarker(new MarkerOptions().position(path.get(1)));

        mMap.addPolyline(new PolylineOptions().addAll(path));
    }

    private void addMarkers(DirectionsResult results, GoogleMap mMap) {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].startLocation.lat,results.routes[0].legs[0].startLocation.lng)).title(results.routes[0].legs[0].startAddress));
        mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].endLocation.lat,results.routes[0].legs[0].endLocation.lng)).title(results.routes[0].legs[0].startAddress));
    }

    private void moveCamera(DirectionsRoute route, GoogleMap mMap) {
        LatLng target = new LatLng(route.legs[0].startLocation.lat, route.legs[0].startLocation.lng);
        Double aLength = (firstPolyLine.longitude - currentLoc.getLongitude());
        Double bLength = (firstPolyLine.latitude - currentLoc.getLatitude());
        float angleA = (float) Math.toDegrees(Math.atan2(aLength, bLength));
        Log.i("inio", String.valueOf(angleA));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(target)
                .zoom(17)
                .bearing(angleA-10) //90 = east
                .tilt(45)
                .build();

        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(route.legs[0].startLocation.lat, route.legs[0].startLocation.lng), 16));
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }





    private void setupGoogleMapScreenSettings(GoogleMap mMap) {
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext
                .setQueryRateLimit(3)
                .setApiKey(getString(R.string.dirkey))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

    private DirectionsResult requestDirections(String origin,String destination,TravelMode mode) {
        DateTime now = new DateTime();
        try {
            return DirectionsApi.newRequest(getGeoContext())
                    .mode(mode)
                    .origin(origin)
                    .destination(destination)
                    .departureTime(now)
                    .await();
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        //TODO: asetus että kamera seuraa itteä

    }
    public String[] getLastRoute() {
        String[] locations = new String[2];

        return locations;
    }
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(getContext(), "AHH:\n", Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(getContext(), "Current location:\n" + location, Toast.LENGTH_LONG).show();

    }

    public double getDestLat() {
        return desLat;
    }

    public double getDesLon() {
        return desLon;
    }

    public void locationChanged(Double newLat, Double newLon) {
        currentLoc.setLatitude(newLat);
        currentLoc.setLongitude(newLon);

        if (!startCentered) {
            Log.i("inio", "shiiett "+currentLoc.getLatitude()+" "+ currentLoc.getLongitude());
            LatLng latLng = new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude());
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            startCentered = true;
        }


    }
}

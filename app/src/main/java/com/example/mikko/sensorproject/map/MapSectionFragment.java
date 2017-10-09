package com.example.mikko.sensorproject.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.mikko.R;
import com.example.mikko.sensorproject.utils.Utils;
import com.example.mikko.sensorproject.interfaces.DestinationInterface;
import com.example.mikko.sensorproject.interfaces.UpdateInfoListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;


import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MapSectionFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener, InfoFragment.InfoListeners {

    private GoogleMap googleMap;
    private Geocoder geocoder;
    private MapView mapview;
    private ProgressBar progressBar;
    private Location currentLoc;
    private double desLat = 0;
    private double desLon = 0;
    private String lastQuery = "";
    private Bundle savedInstanceState;
    private DestinationInterface dest;
    private boolean startCentered = false;
    private LatLng firstPolyLine;
    private DirectionsApiRequest apiRequest;
    public DirectionsResult results;
    private InfoFragment info;
    private UpdateInfoListener updateInfoListener;
    private int currentRoute = 0;
    private ArrayList<String> altRoutes;

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
        progressBar = (ProgressBar) v.findViewById(R.id.progressbar);
altRoutes = new ArrayList<>();
        MapsInitializer.initialize(getContext());
        geocoder = new Geocoder(getContext(), Locale.getDefault());
        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        //MapsInitializer.initialize(this.getActivity());
        setRetainInstance(true);


        //create infobox as child fragment
        info = new InfoFragment();
        FragmentTransaction fm = getChildFragmentManager().beginTransaction();

        fm.replace(R.id.info_fragment_placeholder, info, "info");

        if (savedInstanceState != null) {
            if (!savedInstanceState.getBoolean("infoVisible", false)) {
                fm.show(info);
            }
        } else {
            fm.hide(info);
        }


        fm.commit();
        updateInfoListener = (UpdateInfoListener) info;


        return v;
    }


    @Override
    public void onPause() {
        super.onPause();

    }

    
    //save state for e.g orientation change
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
        if (info != null) {
            if (info.isVisible()) {
                outState.putBoolean("infoVisible", true);
            }
        }

        outState.putInt("altRoutesAmount", altRoutes.size());
        outState.putInt("currentRoute", currentRoute);
        if (!altRoutes.isEmpty()) {
            for (int i=0; i<altRoutes.size(); i++) {
                outState.putString("altRoute"+i, altRoutes.get(i));

            }
        }

        outState.putDouble("targetlat",googleMap.getCameraPosition().target.latitude);
        outState.putDouble("targetlon",googleMap.getCameraPosition().target.longitude);
        outState.putFloat("tilt",googleMap.getCameraPosition().tilt);
        outState.putFloat("bearing",googleMap.getCameraPosition().bearing);
        outState.putFloat("zoom", googleMap.getCameraPosition().zoom);
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mapview.onCreate(savedInstanceState);
        //mapview.onResume();
        mapview.getMapAsync(this);

        Log.i("inio", "map onviewcreated");
        if (savedInstanceState != null) {
            this.savedInstanceState = savedInstanceState;
            startCentered = savedInstanceState.getBoolean("startCentered", startCentered);
            if (!startCentered) {progressBar.setVisibility(View.VISIBLE);}
        } else {
            Log.i("inio", "mitaan ei seivattu ");
        }
        //currentLocation = new CurrentLocation(this, getContext());
        //currentLocation.buildApi(this);
        //myCurrentLocation.start();


    }

    @Override
    public void onResume() {
        super.onResume();
        mapview.onResume();
    }

    //uses current location from service to get user's address with geocoder
    private Address getCurrentAddress() {
        List<Address> addresses = null;
        Log.i("inio", "FUUUK " + String.valueOf(currentLoc.getLatitude()) + " " + String.valueOf(currentLoc.getLongitude()));
        try {
            addresses = geocoder.getFromLocation(currentLoc.getLatitude(), currentLoc.getLongitude(), 1);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses.size() != 0) {
            return addresses.get(0);
        } else {
            return null;
        }

    }

    private Address getAddress(Location location) {
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

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

        Log.i("inio", "onmapready");

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
            //get alternative route data from last state
            Log.i("inio", "altroutes amount: "+String.valueOf(savedInstanceState.getInt("altRoutesAmount")));
            Log.i("inio", "current route: "+String.valueOf( savedInstanceState.getInt("currentRoute")));

            for (int i=0; i<savedInstanceState.getInt("altRoutesAmount"); i++) {
                altRoutes.add(savedInstanceState.getString("altRoute"+i));
            }
            currentRoute = savedInstanceState.getInt("currentRoute");


            //restore last state
            if (savedInstanceState.get("startAddress") != null && savedInstanceState.get("endAddress") != null) {
                String start = savedInstanceState.get("startAddress").toString();
                String end = savedInstanceState.get("endAddress").toString();
                Log.i("ingo", start + "   " + end);

                requestDirections(start, lastQuery);
            }



        } else {

        }


    }

//adds path to map with polyline
    private void addPolyline(String polyline, GoogleMap mMap) {
        List<LatLng> path = PolyUtil.decode(polyline);
        firstPolyLine = path.get(1);
        //mMap.addMarker(new MarkerOptions().position(path.get(1)));

        mMap.addPolyline(new PolylineOptions().addAll(path));
    }

    private void addMarkers(Double lat, Double lon, GoogleMap mMap) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)));
    }

    private void moveCamera(DirectionsRoute route, GoogleMap mMap) {
        LatLng target = new LatLng(route.legs[0].startLocation.lat, route.legs[0].startLocation.lng);
        
        //calculates the bearing angle for map when user makes a query
        Double aLength = (firstPolyLine.longitude - currentLoc.getLongitude());
        Double bLength = (firstPolyLine.latitude - currentLoc.getLatitude());
        
        float angleA = (float) Math.toDegrees(Math.atan2(aLength, bLength));
        Log.i("inio", String.valueOf(angleA));

        CameraPosition cameraPosition;
        if (savedInstanceState == null) {
            cameraPosition = new CameraPosition.Builder()
                    .target(target)
                    .zoom(17)
                    .bearing(angleA - 10) //90 = east
                    .tilt(45)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


            //gets last zoom, tilt etc settings on orientation change
        } else {
            cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(savedInstanceState.getDouble("targetlat"), savedInstanceState.getDouble("targetlon")))
                    .zoom(savedInstanceState.getFloat("zoom"))
                    .bearing(savedInstanceState.getFloat("bearing")) //90 = east
                    .tilt(savedInstanceState.getFloat("tilt"))
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }

        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(route.legs[0].startLocation.lat, route.legs[0].startLocation.lng), 16));

    }


    private void setupGoogleMapScreenSettings(GoogleMap mMap) {
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(false);
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


    public DirectionsResult requestDirections(final String start, final String destination) {

        Address myAddress = getCurrentAddress();

        if (myAddress == null) {
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocationName(start, 1);
                myAddress = addresses.get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //if (myAddress != null) {


        DateTime now = new DateTime();
        DirectionsApiRequest apiRequest = DirectionsApi.newRequest(getGeoContext());
        apiRequest.mode(TravelMode.WALKING);
        apiRequest.origin(myAddress.getAddressLine(0));
        apiRequest.destination(destination);
        apiRequest.alternatives(true);
        apiRequest.departureTime(now);
        //.await();

        
        //on query get asynchronously directions info from maps api
        progressBar.setVisibility(View.VISIBLE);
        apiRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(final DirectionsResult result) {

                results = result;
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (results.routes.length > 0) {
                            currentRoute = 0;
                            altRoutes.clear();
                            for (int i = 0; i < result.routes.length; i++) {
                                altRoutes.add(results.routes[i].overviewPolyline.getEncodedPath());
                            }

                            lastQuery = results.routes[0].legs[0].endAddress;
                            googleMap.clear();


                            desLat = results.routes[0].legs[0].endLocation.lat;
                            desLon = results.routes[0].legs[0].endLocation.lng;


                            Log.i("inio", "jooh täs results: " + results.routes.length);

                            dest.changeLocation(desLat, desLon);
                            addPolyline(results.routes[0].overviewPolyline.getEncodedPath(), googleMap);
                            moveCamera(results.routes[0], googleMap);
                            addMarkers(desLat, desLon, googleMap);
                            progressBar.setVisibility(View.GONE);

                            //update info fragment's info
                            updateInfoListener.newInfo(Utils.removeCountry(results.routes[0].legs[0].endAddress), results.routes[0].legs[0].duration, results.routes[0].legs[0].distance);
                            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                            ft.setCustomAnimations(R.anim.slide_in_from_top, R.anim.slide_out_to_top);

                            ft.show(info);
                            ft.commit();


                        } else  {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getActivity(), "Address not found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }



            //never happens :-)
            @Override
            public void onFailure(Throwable e) {
                Log.i("inio", String.valueOf(e));
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        return null;
    }

    private void changePolyline() {
        currentRoute++;
        googleMap.clear();
        if (desLat != 0) {
            addMarkers(desLat,desLon, googleMap);
        }
        
        //loops through available alternative routes and changes it in map in realtime
        addPolyline(altRoutes.get(currentRoute%altRoutes.size()), googleMap);


    }


    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(getContext(), "AHH:\n", Toast.LENGTH_LONG).show();

        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(getContext(), "Current location:\n" + getAddress(location).getAddressLine(0), Toast.LENGTH_SHORT).show();

    }

    public double getDestLat() {
        return desLat;
    }

    public double getDesLon() {
        return desLon;
    }

    
    //get new locaton from service
    public void locationChanged(Double newLat, Double newLon) {
        currentLoc.setLatitude(newLat);
        currentLoc.setLongitude(newLon);


        /*
        LatLng loc= new LatLng(currentLoc.getLatitude(),currentLoc.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(loc)
                .build();
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/
        
        //first run: centers map to current location
        if (!startCentered) {
            progressBar.setVisibility(View.GONE);
            Log.i("inio", "shiiett " + currentLoc.getLatitude() + " " + currentLoc.getLongitude());
            LatLng latLng = new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude());
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            startCentered = true;
        }


    }

    //show/hide info box
    public void toggleInfo() {
        Log.i("inio", "asdsa");
        FragmentTransaction fm = getChildFragmentManager().beginTransaction();


        if (info == null) {
            fm = getChildFragmentManager().beginTransaction();

            Log.i("inio", "jooh null");
            info = new InfoFragment();
            fm.add(R.id.info_fragment_placeholder, info, "info");
            fm.commit();
        } else {
            Log.i("inio", "jooh ei oo null");
            fm = getChildFragmentManager().beginTransaction();
            fm.setCustomAnimations(R.anim.slide_in_from_top, R.anim.slide_out_to_top);
            if (!info.isVisible()) {

                fm.show(info);

            } else {
                fm.hide(info);
            }

            fm.commit();
        }
    }

    @Override
    public void onXClick() {
        toggleInfo();
    }

    //get next alternative route
    @Override
    public void onNextClick() {
        this.changePolyline();
    }

    @Override
    public void onLocationChanged(Location location) {


    }
}

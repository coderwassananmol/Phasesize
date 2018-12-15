package com.example.gooded.phasesizeapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.tbruyelle.rxpermissions2.RxPermissions;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient mFusedLocationClient;
    private static Boolean mRequestingLocationUpdates = true;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private double latitude;
    private double longitude;
    private Marker marker;
    private GoogleMap mMap;
    private Intent intent;
    private Intent pinsIntent;
    private PinsViewModel pinsViewModel;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pin:
                    if(latitude == 0.0 || longitude == 0.0 ) {
                        Toast.makeText(getApplicationContext(),"Initializing the geo coordindates. Please wait..",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Pins pin = new Pins(latitude,longitude);
                        pinsViewModel.insert(pin);
                        Toast.makeText(getApplicationContext(),"Pin " + latitude + ","+longitude + " saved.",Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.navigation_search:
                    startActivity(intent);
                    return true;
                case R.id.navigation_selectpin:
                    startActivity(pinsIntent);
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent = new Intent(MainActivity.this, PlacesActivity.class);
        pinsIntent = new Intent(MainActivity.this, SavedPinsActivity.class);
        Intent getIntent = getIntent();
        double checkIntent = getIntent.getDoubleExtra("latitude",0);
        pinsViewModel = ViewModelProviders.of(this).get(PinsViewModel.class);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.i("Permission","Location Result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    latitude = checkIntent == 0.0 ? location.getLatitude() : getIntent.getDoubleExtra("latitude",0);
                    longitude = checkIntent == 0.0 ? location.getLongitude() : getIntent.getDoubleExtra("longitude",0);
                    LatLng position = new LatLng(latitude,longitude);
                    Log.i("Permission",position.toString());
                    intent.putExtra("latitude",latitude);
                    intent.putExtra("longitude",longitude);
                    mMap.clear();
                    marker = mMap.addMarker(new MarkerOptions().position(position)
                            .title("Marker in position"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position,13));
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(position)      // Sets the center of the map to location user
                            .zoom(18)                   // Sets the zoom
                            .bearing(90)                // Sets the orientation of the camera to east
                            .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                            .build();                   // Creates a CameraPosition from the builder
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    stopLocationUpdates();
                }
            }
        };
        /*final Button button = findViewById(R.id.search_bar);
        button.setOnClickListener(v -> {
            startActivity(intent);
        });*/
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.setLogging(true);
        rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION) // ask single or multiple permission once
                .subscribe(granted -> {
                    if (granted) {
                        Log.i("Permission","All permissions are granted");
                        displayLocationSettingsRequest(this);
                    } else {
                        Log.w("Permission","Permission is denied");
                    }
                });
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setNeedBle(true);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        result.addOnCompleteListener(task -> {
            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
                Log.i("Permission","Satisfied");
                startLocationUpdates();
            } catch (ApiException exception) {
                switch (exception.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                    MainActivity.this,
                                    100);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("Permission","Unknown Error");
                            // Ignore the error.
                        } catch (ClassCastException e) {
                            Log.i("Permission","ClassCast Unknown Error");
                            // Ignore, should be an impossible error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        Log.i("Permission","Not bearable");
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 100:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        Log.i("Permission","Location updating");
                        // All required changes were successfully made

                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Log.i("Permission","Location permission was cancelled");
                        break;
                    default:
                        break;
                }
                break;
        }
    }
}
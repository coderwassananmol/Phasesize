package com.example.gooded.phasesizeapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
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
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.tbruyelle.rxpermissions2.RxPermissions;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener, DatePickerDialog.OnDateSetListener {

    private FusedLocationProviderClient mFusedLocationClient;
    private static Boolean mRequestingLocationUpdates = true;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Marker marker;
    private double latitude;
    private double longitude;
    private GoogleMap mMap;
    private Intent intent;
    private Intent pinsIntent;
    private PinsViewModel pinsViewModel;
    private int day;
    private int month;
    private int year;
    private Button button;
    private TextView sunriseTextView;
    private TextView sunsetTextView;
    private int milisecondsDelay;
    DecimalFormat df;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pin:
                    if(latitude == 0.0 || longitude == 0.0 ) {
                        Toast.makeText(getApplicationContext(), R.string.initcoord,Toast.LENGTH_SHORT).show();
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.date);
        sunriseTextView = findViewById(R.id.sunrise);
        sunsetTextView = findViewById(R.id.sunset);
        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH)+1;
        day = c.get(Calendar.DATE);
        df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        String formattedDate = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(new Date());
        button.setText(formattedDate);
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
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    latitude = checkIntent == 0.0 ? location.getLatitude() : getIntent.getDoubleExtra("latitude",0);
                    longitude = checkIntent == 0.0 ? location.getLongitude() : getIntent.getDoubleExtra("longitude",0);
                    LatLng position = new LatLng(latitude,longitude);
                    updatePhaseTime();
                    updateMap(position);
                    intent.putExtra("latitude",latitude);
                    intent.putExtra("longitude",longitude);
                    stopLocationUpdates();
                    updatePolyLines();
                }
            }
        };
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.setLogging(true);
        rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION) // ask single or multiple permission once
                .subscribe(granted -> {
                    if (granted) {
                        displayLocationSettingsRequest(this);
                    } else {
                        Toast.makeText(this, R.string.permissionnotfulfilled,Toast.LENGTH_SHORT).show();
                    }
                });
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
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
        mMap.setOnMarkerDragListener(this);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng position = marker.getPosition();
        latitude = position.latitude;
        longitude = position.longitude;
        updatePhaseTime();
        updatePolyLines();
    }

    public void updatePolyLines() {
        PolylineOptions sunSet = new PolylineOptions()
                .add(new LatLng(latitude, longitude))
                .add(new LatLng(latitude+0.10, longitude+0.20))  //Add 30 KM to the west.
                .color(Color.RED)
                .width(5)
                .geodesic(true);// Closes the polyline.
        PolylineOptions sunRise = new PolylineOptions()
                .add(new LatLng(latitude, longitude))
                .add(new LatLng(latitude+0.10, longitude-0.20))  //Add 30 KM to the west.
                .color(Color.BLUE)
                .width(5)
                .geodesic(true);// Closes the polyline.
        Polyline polyline1 = mMap.addPolyline(sunSet);
        Polyline polyline2 = mMap.addPolyline(sunRise);
    }

    public double calculatePhaseTime(double latitude, double longitude, int day, int month, int year, boolean sunrise) {
        //Calculating the day of the year
        double N1 = Math.floor(275 * month / 9);
        double N2 = Math.floor((month + 9) / 12);
        double N3 = (1 + Math.floor((year - 4 * Math.floor(year / 4) + 2) / 3));
        double N = N1 - (N2 * N3) + day - 30;
        double D2R = Math.PI/180;
        double R2D = 180 / Math.PI;
        //convert the longitude to hour value and calculate an approximate time
        double lngHour = longitude / 15;
        double t;
        double zenith = 90.83333333333333;
        if(sunrise) {
            t = N + ((6 - lngHour) / 24);
        }
        else {
            t = N + ((18 - lngHour) / 24);
        }

        //calculate the Sun's mean anomaly
        double M = (0.9856 * t) - 3.289;

        //calculate the Sun's true longitude
        double L = M + (1.916 * Math.sin(M * D2R)) + (0.020 * Math.sin(2 * M * D2R)) + 282.634;
        if (L > 360) {
            L = L - 360;
        } else if (L < 0) {
            L = L + 360;
        }

        //calculate the Sun's right ascension
        double RA = R2D * Math.atan(0.91764 * Math.tan(L * D2R));
        if (RA > 360) {
            RA = RA - 360;
        } else if (RA < 0) {
            RA = RA + 360;
        }

        //right ascension value needs to be in the same quadrant as L
        double Lquadrant  = (Math.floor( L/90)) * 90;
        double RAquadrant = (Math.floor(RA/90)) * 90;
        RA += (Lquadrant - RAquadrant);

        //right ascension value needs to be converted into hours
        RA /= 15;

        //calculate the Sun's declination
        double sinDec = 0.39782 * Math.sin(L * D2R);
        double cosDec = Math.cos(Math.asin(sinDec));

        //calculate the Sun's local hour angle
        double cosH = (Math.cos(zenith * D2R) - (sinDec * Math.sin(latitude * D2R))) / (cosDec * Math.cos(latitude * D2R));
        if (cosH >  1) {
            Toast.makeText(this, R.string.sunnotrise,Toast.LENGTH_SHORT).show();
        }

        else if (cosH < -1) {
            Toast.makeText(this, R.string.sunnotset,Toast.LENGTH_SHORT).show();
        }

        else {
            //finish calculating H and convert into hours
            double H;
            if(sunrise) {
                H = 360 - R2D * Math.acos(cosH);
            }
            else {
                H = R2D * Math.acos(cosH);
            }
            H/=15;

            double T = H + RA - (0.06571 * t) - 6.622;
            double UT = T - lngHour;
            if (UT > 24) {
                UT = UT - 24;
            } else if (UT < 0) {
                UT = UT + 24;
            }
            double localT = UT + 5.50;
            return localT;
        }
        return -1;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void updatePhaseTime() {
        double sunrise = calculatePhaseTime(latitude,longitude,day,month,year,true);
        double sunset = calculatePhaseTime(latitude,longitude,day,month,year,false);
        Log.i("Time",SystemClock.elapsedRealtime()+"");
        if(sunrise != -1 && sunset != -1) {
            int sunriseNumber = (int) sunrise;
            int sunsetNumber = (int) sunset;
            String sunriseDecimalString = df.format(sunrise - sunriseNumber);
            String sunsetDecimalString = df.format(sunset - sunsetNumber);
            double sunriseDecimal = Double.parseDouble(sunriseDecimalString);
            double sunsetDecimal = Double.parseDouble(sunsetDecimalString);
            if(sunriseDecimal > 0.60) {
                sunriseDecimal-=0.60;
                sunriseNumber++;
            }
            if(sunsetDecimal > 0.60) {
                sunsetDecimal-=0.60;
                sunsetNumber++;
            }
            int sunriseNo = (int) (sunriseDecimal*100);
            int sunsetNo = (int) (sunsetDecimal*100);
            if(sunriseNo < 10) {
                sunriseDecimalString = "0"+ sunriseNo;
            }
            else {
                sunriseDecimalString = sunriseNo + "";
            }
            if(sunsetNo < 10) {
                sunsetDecimalString = "0" + sunsetNo;
            }
            else {
                sunsetDecimalString = sunsetNo + "";
            }
            String sunriseTime = sunriseNumber + ":" + sunriseDecimalString + " A.M";
            String sunsetTime = sunsetNumber + ":" + sunsetDecimalString + " P.M";
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            Log.i("sunsetNo",sunsetNumber+"");
            Log.i("hour",hour+"");
            milisecondsDelay = Math.abs(hour - sunsetNumber -1)*3600*1000;
            Log.i("ms",milisecondsDelay+"");
            sunriseTextView.setText(sunriseTime);
            sunsetTextView.setText(sunsetTime);
            sunriseTextView.setVisibility(View.VISIBLE);
            sunsetTextView.setVisibility(View.VISIBLE);
            scheduleNotification(getNotification("The time of golden hour"), milisecondsDelay);
        }
    }

    private void scheduleNotification(Notification notification, int delay) {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("The rise of golden hour");
        builder.setContentText(content);
        builder.setSmallIcon(R.drawable.ic_home_black_24dp);
        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Do something with the date chosen by the user
        this.day = day;
        this.month = month;
        this.year = year;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        Date date = calendar.getTime();
        String formattedDate = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(date);
        button.setText(formattedDate);
        updatePhaseTime();
    }

    public void updateMap(LatLng position) {
        mMap.clear();
        marker = mMap.addMarker(new MarkerOptions().position(position)
                .title(getString(R.string.marker))
                .draggable(true));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position,13));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)      // Sets the center of the map to location user
                .zoom(18)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
                        Toast.makeText(this, R.string.permissionnotfulfilled,Toast.LENGTH_SHORT).show();
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
                        // All required changes were successfully made
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, R.string.permissionnotfulfilled,Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
        }
    }
}
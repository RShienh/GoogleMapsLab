package com.shienh.rajbir.googlemaplab;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.shienh.rajbir.googlemaplab.models.PlaceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MAINACTIVITY";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 47));
    RelativeLayout mBaseView;
    private LottieAnimationView anim, moving;
    private AutoCompleteTextView mSearchText;
    private ImageView mGPS;
    private Boolean mLocationGranted = false;
    private GoogleMap mMap;
    private PlaceAutocompleteAdapter mAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;
    private Marker mMarker;
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "ON_RESULT: Place did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try {

                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                mPlace.setId(place.getId());
                mPlace.setAddress(place.getAddress().toString());
                mPlace.setLatLng(place.getLatLng());
                mPlace.setRating(place.getRating());

                Log.d(TAG, "ON_RESULT: " + mPlace.toString());
            } catch (NullPointerException n) {
                Log.e(TAG, "ON_RESULT: NullPointerException " + n.getMessage());
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);
            places.release();
        }
    };
    private AdapterView.OnItemClickListener mAutoCompleteClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            HideSoftKeyboard();
            final AutocompletePrediction mItem = mAutocompleteAdapter.getItem(i);
            final String placeID = mItem.getPlaceId();

            PendingResult<PlaceBuffer> placeBuffer = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeID);
            placeBuffer.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "ON_CONNECTION_FAILED: Connection Failed");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "ON_MAP_READY: Map is ready");
        mMap = googleMap;

        if (mLocationGranted) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);
            mMap.setBuildingsEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
            init();
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        anim = findViewById(R.id.animation);
        moving = findViewById(R.id.moving_anim);
        mBaseView = findViewById(R.id.BaseView);
        isServiceOK();
        getLocationPermission();
        mSearchText = findViewById(R.id.input_search);
        mGPS = findViewById(R.id.ic_gps);
    }

    public void changeMapType(View view) {
        if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            Snackbar.make(mBaseView, "Satellite View", 3000).show();
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            Snackbar.make(mBaseView, "Terrain View", 3000).show();
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_TERRAIN) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            Snackbar.make(mBaseView, "Hybrid View", 3000).show();
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            Snackbar.make(mBaseView, "Normal View", 3000).show();
        }
    }

    private void init() {
        Log.d(TAG, "INIT: Initializing");
        anim.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                anim.setVisibility(View.GONE);
            }
        }, 1600);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        mSearchText.setOnItemClickListener(mAutoCompleteClick);
        mAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mAutocompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH
                        || i == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {
                    geoLocate();
                }
                return false;
            }
        });
        mGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "ON_CLICK: Clicked GPS Icon");
                getDeviceLocation();
            }
        });
        HideSoftKeyboard();
    }

    private void geoLocate() {
        Log.d(TAG, "GEO_LOCATE: Geo-locating");
        String search = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(search, 1);
        } catch (IOException e) {
            Log.e(TAG, "GEO_LOCATE: IOException " + e.getMessage());
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "GEO_LOCATE: Found Location: " + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "GET_DEVICE_LOCATION: Getting Device location");
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationGranted) {
                final Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<Location> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "ON_COMPLETE: Found Location");
                            Location currentLocation = task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                        } else {
                            Log.d(TAG, "ON_COMPLETE: Current location is null");
                            Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "GET_DEVICE_LOCATION: SecurityException " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latlng, float zoom, PlaceInfo placeInfo) {
        hideKeyboard(MapsActivity.this);
        moving.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                moving.setVisibility(View.GONE);
            }
        }, 1500);
        Log.d(TAG, "MOVE_CAMERA: MOVING CAMERA TO Latitude:  " + latlng.latitude + ", Longitude: " + latlng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        mMap.clear();
        if (placeInfo != null) {
            try {
                String phoneNumber = placeInfo.getPhoneNumber();
                Uri website = placeInfo.getWebsiteUri();
                float rating = placeInfo.getRating();
                if (TextUtils.isEmpty(phoneNumber) && website != null && rating > -1.0) {

                    String snippet = placeInfo.getAddress() + "\n" +
                            placeInfo.getName() + "\n" +
                            placeInfo.getPhoneNumber() + "\n" +
                            placeInfo.getWebsiteUri() + "\n" +
                            placeInfo.getPhoneNumber() + "\n" +
                            placeInfo.getWebsiteUri() + "\n" +
                            "Rating: " + placeInfo.getRating() + "\n";
                    MarkerOptions options = new MarkerOptions()
                            .position(latlng)
                            .title(placeInfo.getName())
                            .snippet(snippet).
                                    icon(BitmapDescriptorFactory.
                                            defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    mMarker = mMap.addMarker(options);
                    mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                        @Override
                        public View getInfoWindow(Marker arg0) {
                            return null;
                        }

                        @Override
                        public View getInfoContents(Marker marker) {

                            Context context = getApplicationContext();

                            LinearLayout info = new LinearLayout(context);
                            info.setOrientation(LinearLayout.VERTICAL);

                            TextView title = new TextView(context);
                            title.setTextColor(Color.BLACK);
                            title.setGravity(Gravity.CENTER);
                            title.setTypeface(null, Typeface.BOLD);
                            title.setText(marker.getTitle());
                            title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            TextView snippet = new TextView(context);
                            snippet.setTextColor(Color.GRAY);
                            snippet.setText(marker.getSnippet());
                            snippet.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            info.addView(title);
                            info.addView(snippet);

                            return info;
                        }
                    });
                } else {
                    LatLng latLng = mPlace.getLatLng();
                    String finalLatLong = "Latitude: " + String.format("%.2f", latLng.latitude) + ", Longitude: " + String.format("%.2f", latLng.longitude);
                    mMarker = mMap.addMarker(new MarkerOptions().position(latlng).title(placeInfo.getName()).snippet(placeInfo.getAddress() + "\n" + finalLatLong).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                        @Override
                        public View getInfoWindow(Marker arg0) {
                            return null;
                        }

                        @Override
                        public View getInfoContents(Marker marker) {

                            Context context = getApplicationContext();

                            LinearLayout info = new LinearLayout(context);
                            info.setOrientation(LinearLayout.VERTICAL);

                            TextView title = new TextView(context);
                            title.setTextColor(Color.BLACK);
                            title.setGravity(Gravity.CENTER);
                            title.setTypeface(null, Typeface.BOLD);
                            title.setText(marker.getTitle());
                            title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            TextView snippet = new TextView(context);
                            snippet.setTextColor(Color.GRAY);
                            snippet.setText(marker.getSnippet());
                            snippet.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            info.addView(title);
                            info.addView(snippet);

                            return info;
                        }
                    });
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "MOVE_CAMERA: NullPointerException " + e.getMessage());
            }
        } else {
            mMap.addMarker(new MarkerOptions().position(latlng));
        }
        HideSoftKeyboard();
    }

    private void moveCamera(LatLng latlng, float zoom, String title) {
        Log.d(TAG, "MOVE_CAMERA: MOVING CAMERA TO Latitude:  " + latlng.latitude + ", Longitude: " + latlng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        if (!title.equalsIgnoreCase("My Location")) {
            MarkerOptions options = new MarkerOptions().position(latlng).title(title);
            mMap.addMarker(options);
        }
        HideSoftKeyboard();
    }

    private void initMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapsActivity.this);
        Log.d(TAG, "INIT_MAP: Map is initialized");
    }

    private void getLocationPermission() {
        Log.d(TAG, "GET_LOCATION_PERMISSION: Getting location Permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "ON_REQUEST_PERMISSION_RESULT: called");
        mLocationGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationGranted = false;
                            Log.d(TAG, "ON_REQUEST_PERMISSION_RESULT: Permission Failed");
                            return;
                        }
                    }
                    Log.d(TAG, "ON_REQUEST_PERMISSION_RESULT: Permission Granted");
                    mLocationGranted = true;
                    initMap();
                }
            }
        }
    }

    public boolean isServiceOK() {
        Log.d(TAG, "IS_SERVICE_OK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapsActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "IS_SERVICE_OK: Google Play Services is Working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "IS_SERVICE_OK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapsActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void HideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}
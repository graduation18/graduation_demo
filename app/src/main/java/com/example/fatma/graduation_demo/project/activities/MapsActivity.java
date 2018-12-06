package com.example.fatma.graduation_demo.project.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import com.google.android.gms.maps.CameraUpdateFactory;
import android.widget.TextView;

import com.example.fatma.graduation_demo.R;
import com.example.fatma.graduation_demo.project.custom.PlaceAutocompleteAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG ="MapActivity" ;
    private GoogleMap mMap;
    private AutoCompleteTextView Search;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;

    private static final float DEFAULT_ZOOM = 15f;
    private Place placee;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Search=(AutoCompleteTextView) findViewById(R.id.search);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        Search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return false;
            }
        });
        double lat= Double.parseDouble(getSharedPreferences("personal_data",MODE_PRIVATE).getString("person_latitude",""));
        double lng= Double.parseDouble(getSharedPreferences("personal_data",MODE_PRIVATE).getString("person_longitude",""));

        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(lat, lng, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName();
        double city_lat=addresses.get(0).getLatitude();
        double city_lng=addresses.get(0).getLongitude();

        LatLngBounds latlng_bounds = new LatLngBounds(new LatLng(city_lat,city_lng),new LatLng(city_lat,city_lng));

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this , mGoogleApiClient , latlng_bounds,null);

        Search.setOnItemClickListener(itemAutocompleteClickListener);

        Search.setAdapter(mPlaceAutocompleteAdapter);
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }

        hideSoftKeyboard();
    }


    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    private AdapterView.OnItemClickListener itemAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeid = item.getPlaceId();
            PendingResult<PlaceBuffer> place_rsl = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient,placeid);
            place_rsl.setResultCallback(updatedcallback);



        }

    };
private ResultCallback<PlaceBuffer> updatedcallback = new ResultCallback<PlaceBuffer>() {
    @Override
    public void onResult(@NonNull PlaceBuffer places) {
        if (!places.getStatus().isSuccess()){

            places.release();
            return;
        }
        final Place place = places.get(0);
        try {

            place.getAddress();
            place.getId();
            place.getName().toString();
            place.getPhoneNumber();
            place.getPlaceTypes();
            place.getViewport();
            place.getWebsiteUri();
            place.getAttributions();

            placee = place;

        }catch (NullPointerException e) {

            Log.e(TAG, "onResult: NullPointerException: " + e.getMessage() );

        }
        moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                        place.getViewport().getCenter().longitude)
                , DEFAULT_ZOOM , (String) placee.getName());


        places.release();
    }
}
;

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        double lat= Double.parseDouble(getSharedPreferences("personal_data",MODE_PRIVATE).getString("person_latitude",""));
        double lng= Double.parseDouble(getSharedPreferences("personal_data",MODE_PRIVATE).getString("person_longitude",""));
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat,lng)).title("Marker Current Locations"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lng),15));
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

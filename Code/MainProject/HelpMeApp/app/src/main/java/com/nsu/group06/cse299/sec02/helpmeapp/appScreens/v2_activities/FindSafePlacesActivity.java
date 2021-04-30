package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.FetchedLocation;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.LocationFetcher;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.fusedLocationApi.FusedLocationFetcherApiAdapter;

public class FindSafePlacesActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "FSPA-debug";
    private static final float CLOSE_ZOOM_LEVEL = 17.5f;
    private static final float FAR_ZOOM_LEVEL = 13f;
    private static final LatLng DEFAULT_LATLNG = new LatLng(23.777176, 90.399452); // Co-ordinates of Dhaka city

    private GoogleMap mMap;

    // variables used to fetch location
    private FetchedLocation mCurrentLocation;
    private LocationFetcher mLocationFetcher;
    private LocationFetcher.LocationSettingsSetupListener mLocationSettingsSetupListener =
            new LocationFetcher.LocationSettingsSetupListener() {
                @Override
                public void onLocationSettingsSetupSuccess() {

                    mLocationFetcher.startLocationUpdate();
                }

                @Override
                public void onLocationSettingsSetupFailed(String message) {
                    // user will be automatically be asked to enable location settings
                    // see method in SetupProfileActivity: 'onActivityResult(...)'
                    Log.d(TAG, "onLocationSettingsSetupFailed: location settings setup failed ->" + message);
                }
            };
    private LocationFetcher.LocationUpdateListener mLocationUpdateListener =
            new LocationFetcher.LocationUpdateListener() {
                @Override
                public void onNewLocationUpdate(FetchedLocation fetchedLocation) {

                    if (mCurrentLocation == null || mCurrentLocation.getmAccuracy() > fetchedLocation.getmAccuracy()
                            || FetchedLocation.isLocationSignificantlyDifferent(mCurrentLocation, fetchedLocation)) {

                        mCurrentLocation = fetchedLocation;
                        fetchCurrentLocationSuccessUI(mCurrentLocation);
                        mLocationFetcher.stopLocationUpdate();
                    }

                    Log.d(TAG, "onNewLocationUpdate: location -> " + fetchedLocation.toString());
                }

                @Override
                public void onPermissionNotGranted() {

                    fetchCurrentLocationFailedUI();
                    mLocationFetcher.stopLocationUpdate();

                    Log.d(TAG, "onPermissionNotGranted: location permission not granted");
                }

                @Override
                public void onError(String message) {

                    if (mCurrentLocation == null) fetchCurrentLocationFailedUI();
                    mLocationFetcher.stopLocationUpdate();

                    Log.d(TAG, "onError: location update error -> " + message);
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_safe_places);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mLocationFetcher != null) mLocationFetcher.stopLocationUpdate();
    }

    /*
        Required for reacting to user response when setting up required location settings
        user response to default "turn on location" dialog is handled through this method
         */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case LocationFetcher.REQUEST_CHECK_LOCATION_SETTINGS:

                if (resultCode == RESULT_OK) {
                    // user enabled location settings
                    mLocationFetcher.startLocationUpdate();
                }

                break;
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        moveMapToDefaultLocation();

        // this takes some time
        moveMapToCurrentLocation();
    }

    private void moveMapToCurrentLocation() {

        // location fetcher
        mLocationFetcher = new FusedLocationFetcherApiAdapter(
                1000, this,
                mLocationSettingsSetupListener,
                mLocationUpdateListener
        );

        getLocationPermissions();
    }

    /*
    Ask for location access permission
    using the open source library- <https://github.com/Karumi/Dexter>
     */
    private void getLocationPermissions() {

        Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        mLocationFetcher.setupLocationSettings(FindSafePlacesActivity.this);
                        if (ActivityCompat.checkSelfPermission(FindSafePlacesActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // this will never get called -_-
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        fetchCurrentLocationFailedUI();
                        //showLocationPermissionExplanationDialog(permissionDeniedResponse.isPermanentlyDenied());
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // ignore for now
                        permissionToken.continuePermissionRequest();
                    }
                })
                .check();
    }

    private void showMarketAt(LatLng location) {

        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(location));
    }

    private void moveMapTo(LatLng location, float zoomLevel) {

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
    }

    private void fetchCurrentLocationSuccessUI(FetchedLocation location) {

        LatLng currentLatLng = new LatLng(location.getmLatitude(), location.getmLongitude());
        moveMapTo(currentLatLng, CLOSE_ZOOM_LEVEL);
    }

    private void fetchCurrentLocationFailedUI() {

        showToast(getString(R.string.failed_to_get_current_location));
        moveMapToDefaultLocation();
    }

    private void moveMapToDefaultLocation() {

        moveMapTo(DEFAULT_LATLNG, FAR_ZOOM_LEVEL);
    }

    /*
    show alert dialog explaining why location permission is a MUST
    with a simple dialog, quit activity if permission is permanently denied
    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
     */
//    private void showLocationPermissionExplanationDialog(boolean isPermissionPermanentlyDenied) {
//
//        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//
//        String title = getString(R.string.location_permission);
//        String explanation;
//
//        if(isPermissionPermanentlyDenied)
//            explanation = getString(R.string.location_permission_permanantely_denied_explanation);
//        else
//            explanation = getString(R.string.location_permission_explanation);
//
//        alertDialog.setTitle(title);
//        alertDialog.setMessage(explanation);
//        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
//                (dialog, which) -> {
//                    if(!isPermissionPermanentlyDenied)
//                        getLocationPermissions();
//                    else
//                        finish();
//                });
//
//        alertDialog.show();
//    }

    private void showToast(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT)
                .show();
    }

    public void backPress(View view) {

        finish();
    }
}
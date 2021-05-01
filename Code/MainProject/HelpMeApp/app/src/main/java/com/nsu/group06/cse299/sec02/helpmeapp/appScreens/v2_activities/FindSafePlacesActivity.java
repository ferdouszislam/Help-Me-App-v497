package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.database.Database;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBApiEndPoint;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBRealtime;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.FetchedLocation;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.LocationFetcher;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.fusedLocationApi.FusedLocationFetcherApiAdapter;
import com.nsu.group06.cse299.sec02.helpmeapp.models.HelpPost;
import com.nsu.group06.cse299.sec02.helpmeapp.models.MarkedUnsafeLocation;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.NosqlDatabasePathUtils;

import java.util.ArrayList;

public class FindSafePlacesActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private static final String TAG = "FSPA-debug";
    private static final float CLOSE_ZOOM_LEVEL = 17.5f;
    private static final float FAR_ZOOM_LEVEL = 13f;
    private static final LatLng DEFAULT_LATLNG = new LatLng(23.777176, 90.399452); // Co-ordinates of Dhaka city

    // ui
    private GoogleMap mMap;
    private LinearLayout mMarkerDetailsLinearLayout;
    private TextView mUnsafeLocationDescriptionTextView, mUnsafeLocationTimeTextView;

    // model
    ArrayList<MarkedUnsafeLocation> mMarkedUnsafeLocations;

    // variables to access database
    private Database.RealtimeDatabase mReadHelpPostsRealtimeDatabase;
    private FirebaseRDBApiEndPoint mApiEndPoint = new FirebaseRDBApiEndPoint("/"+ NosqlDatabasePathUtils.HELP_POSTS_NODE);
    private Database.RealtimeDatabase.RealtimeChangesDatabaseCallback<HelpPost> mHelpPostRealtimeChangesDatabaseCallback =
            new Database.RealtimeDatabase.RealtimeChangesDatabaseCallback<HelpPost>() {
                @Override
                public void onDataAddition(HelpPost data) {

                    // TODO:
                    //  put public and private posts on different nodes
                    //  and remove this client side filtration
                    if(!data.getIsPublic()) return;

                    Log.d(TAG, "onDataAddition: data added -> "+data.toString());

                    addHelpPostToMarkedLocationWiseHelpPostCollection(data);
                }

                @Override
                public void onDataUpdate(HelpPost data) {

                    updateHelpPostInMarkedLocationWiseHelpPostCollection(data);
                }

                @Override
                public void onDataDeletion(HelpPost data) {

                    removeHelpPostFromMarkedLocationWiseHelpPostCollection(data);
                }

                @Override
                public void onDatabaseOperationSuccess() {

                }

                @Override
                public void onDatabaseOperationFailed(String message) {

                    failedToLoadMarkedUnsafeLocationsUI();
                }
            };

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

        init();
    }

    private void init() {

        mMarkerDetailsLinearLayout = findViewById(R.id.activity_find_safe_places_unsafeLocationMarkerDetailsLinearLayout);
        mUnsafeLocationDescriptionTextView = findViewById(R.id.activity_find_safe_places_unsafeLocationDescriptionTextView);
        mUnsafeLocationTimeTextView = findViewById(R.id.activity_find_safe_places_unsafeLocationTimeTextView);

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        mMarkedUnsafeLocations = new ArrayList<>();

        moveMapToDefaultLocation();
        moveMapToCurrentLocation(); // this takes some time

        loadHelpPosts();
    }

    private void loadHelpPosts() {

        mReadHelpPostsRealtimeDatabase = new FirebaseRDBRealtime<>(
                HelpPost.class,
                mApiEndPoint,
                mHelpPostRealtimeChangesDatabaseCallback
        );

        mReadHelpPostsRealtimeDatabase.listenForListDataChange();
    }

    /**
     * Add help post to marked unsafe location collection
     * @param helpPost model class for help posts
     */
    private void addHelpPostToMarkedLocationWiseHelpPostCollection(HelpPost helpPost) {

        int idx = -1;
        for(MarkedUnsafeLocation markedUnsafeLocation: mMarkedUnsafeLocations) {

            idx++;
            if(markedUnsafeLocation.belongsToMarkedUnsafeLocation(helpPost)){

                mMarkedUnsafeLocations.get(idx).addHelpPostToMarkedUnsafeLocation(helpPost);
                return;
            }
        }

        // reached here means helpPost does not belong to any existing marked unsafe locations
        // so create a new marked unsafe location
        MarkedUnsafeLocation newMarkedUnsafeLocation = new MarkedUnsafeLocation(helpPost);
        mMarkedUnsafeLocations.add(newMarkedUnsafeLocation);
        // add marker for the newMarkedUnsafeLocation to the map
        showUnsafeLocationMarkerInMap(newMarkedUnsafeLocation, mMarkedUnsafeLocations.size()-1);
    }

    /**
     * Remove help post from marked unsafe location collection
     * @param helpPost model class for help posts
     */
    private void removeHelpPostFromMarkedLocationWiseHelpPostCollection(HelpPost helpPost) {

        int idx = -1;
        for(MarkedUnsafeLocation markedUnsafeLocation: mMarkedUnsafeLocations) {

            idx++;
            if(markedUnsafeLocation.hasHelpPost(helpPost)){

                mMarkedUnsafeLocations.get(idx).removeHelpPost(helpPost);
                break;
            }
        }
    }

    /**
     * Find and Update help post from marked unsafe location collection
     * @param helpPost model class for help posts
     */
    private void updateHelpPostInMarkedLocationWiseHelpPostCollection(HelpPost helpPost) {

        int idx = -1;
        for(MarkedUnsafeLocation markedUnsafeLocation: mMarkedUnsafeLocations) {

            idx++;
            if(markedUnsafeLocation.hasHelpPost(helpPost)){

                mMarkedUnsafeLocations.get(idx).removeHelpPost(helpPost);
                mMarkedUnsafeLocations.get(idx).addHelpPostToMarkedUnsafeLocation(helpPost);
                break;
            }
        }
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

    private void showUnsafeLocationMarkerInMap(MarkedUnsafeLocation markedUnsafeLocation, int markedUnsafeLocationPosition) {

        LatLng location = new LatLng(markedUnsafeLocation.getLatitude(), markedUnsafeLocation.getLongitude());

        Marker marker = mMap.addMarker(
                new MarkerOptions()
                        .position(location)
                        .icon(bitmapDescriptorFromVector(this, R.drawable.ic_danger_icon)));
        marker.setTag(markedUnsafeLocationPosition);
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        // xml -> bitmap [for map marker icons]
        // courtesy-
        // https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon

        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        Integer markedUnsafeLocationPos = (Integer) marker.getTag();

        showDetailsOfUnsafeLocation(mMarkedUnsafeLocations.get(markedUnsafeLocationPos));

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    private void showDetailsOfUnsafeLocation(MarkedUnsafeLocation markedUnsafeLocation) {

        mMarkerDetailsLinearLayout.setVisibility(View.VISIBLE);
        mUnsafeLocationDescriptionTextView.setText(markedUnsafeLocation.getDescription());
    }

    @Override
    public void onMapClick(LatLng latLng) {

        mMarkerDetailsLinearLayout.setVisibility(View.GONE);
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

    private void failedToLoadMarkedUnsafeLocationsUI() {

        showToast(getString(R.string.no_internet));
    }

//    /*
//    show alert dialog explaining why location permission is a MUST
//    with a simple dialog, quit activity if permission is permanently denied
//    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
//     */
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
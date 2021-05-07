package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.FindSafePlacesActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.InternetAlertActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.MenuActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.background.services.EmergencyModeService;
import com.nsu.group06.cse299.sec02.helpmeapp.background.workers.NotifyNearbyHelpPostWorker;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.LocationFetcher;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.fusedLocationApi.FusedLocationFetcherApiAdapter;
import com.nsu.group06.cse299.sec02.helpmeapp.sharedPreferences.AppSettingsSharedPref;

import java.util.concurrent.TimeUnit;

/**
 * App home page
 */
public class HomeActivity extends InternetAlertActivity {

    private static final String TAG = "HA-debug";
    private static final String UNIQUE_WORKER_ID = "com.nsu.group06.cse299.sec02.helpmeapp-nearbyHelpPostsNotifier";
    private static final int WORKER_INTERVAL = 30; // minutes

    // ui
    private Snackbar mSnackbar;
    private SwitchCompat mEmergencyModeSwitch;

    // nearby recent help posts notification background worker
    private PeriodicWorkRequest mNotifyNearbyHelpPostsWorkRequest;

    // emergency mode state shared preference
    private AppSettingsSharedPref mAppSettingsSharedPref;

    // location setup variables for EmergencyMode
    private LocationFetcher mLocationFetcher;
    private LocationFetcher.LocationSettingsSetupListener mLocationSettingsSetupListener =
            new LocationFetcher.LocationSettingsSetupListener() {
                @Override
                public void onLocationSettingsSetupSuccess() {

                    startEmergencyModeService();
                    Log.d(TAG, "onLocationSettingsSetupSuccess: location settings met.");
                }

                @Override
                public void onLocationSettingsSetupFailed(String message) {

                    // user will be automatically be asked to enable location settings
                    // see method in HelpPostActivity: 'onActivityResult(...)'
                    Log.d(TAG, "onLocationSettingsSetupFailed: location settings setup failed ->" + message);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();

        startNearbyHelpPostsNotificationWorker();
    }

    /*
    Required for reacting to user response when setting up required location settings
    user response to default "turn on location" dialog is handled through this method
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){

            case LocationFetcher.REQUEST_CHECK_LOCATION_SETTINGS:

                if(resultCode==RESULT_OK){
                    // user enabled location settings
                    startEmergencyModeService();
                }

                else{
                    // location settings not met
                    showLocationSettingsExplanationDialog();
                    mEmergencyModeSwitch.setChecked(false);
                    mAppSettingsSharedPref.setEmergencyModeState(false);
                }
                break;
        }
    }

    private void init() {

        View view = findViewById(R.id.menu_main_layout);
        mSnackbar = Snackbar.make(view, R.string.internet_connection_lost, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("dismiss", v -> mSnackbar.dismiss());

        mEmergencyModeSwitch = findViewById(R.id.activity_main_emergencyModeSwitch);

        mAppSettingsSharedPref = AppSettingsSharedPref.build(this);
        mEmergencyModeSwitch.setChecked(mAppSettingsSharedPref.getEmergencyModeState(false));

        mLocationFetcher =
                new FusedLocationFetcherApiAdapter(
                        EmergencyModeService.EMERGENCY_MODE_LOCATION_UPDATE_INTERVAL,
                        this, mLocationSettingsSetupListener);
    }

    private void startNearbyHelpPostsNotificationWorker() {

        mNotifyNearbyHelpPostsWorkRequest = new
                PeriodicWorkRequest.Builder(NotifyNearbyHelpPostWorker.class, WORKER_INTERVAL, TimeUnit.MINUTES)
                .setConstraints(
                        new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                UNIQUE_WORKER_ID,
                ExistingPeriodicWorkPolicy.KEEP,
                mNotifyNearbyHelpPostsWorkRequest);
    }

    public void menuClick(View view) {

        startActivity(new Intent(this, MenuActivity.class));
    }

    public void emergencyModeToggleClick(View view) {

        mAppSettingsSharedPref.setEmergencyModeState(mEmergencyModeSwitch.isChecked());

        if(mEmergencyModeSwitch.isChecked()) getSmsPermission();
        else stopEmergencyModeService();
    }

    /**
     * Ask for sms sending permission
     * and then ask for location permission
     */
    private void getSmsPermission(){

        Dexter.withContext(this)
                .withPermission(Manifest.permission.SEND_SMS)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        getLocationPermission();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        showSmsPermissionExplanationDialog(permissionDeniedResponse.isPermanentlyDenied());
                        onEmergencyModeStartFailed();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // ignore for now
                        permissionToken.continuePermissionRequest();
                    }
                })
                .check();
    }

    /**
     * Ask for location access permission
     * and then ask for background location
     */
    private void getLocationPermission() {

        Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        getBackgroundLocationPermission();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        showLocationPermissionExplanationDialog(permissionDeniedResponse.isPermanentlyDenied());
                        onEmergencyModeStartFailed();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // ignore for now
                        permissionToken.continuePermissionRequest();
                    }
                })
                .check();
    }

    /**
     * Ask for background location permission
     * and then start EmergencyModeService
     */
    private void getBackgroundLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Dexter.withContext(this)
                    .withPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                            mLocationFetcher.setupLocationSettings(HomeActivity.this);
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                            showLocationPermissionExplanationDialog(permissionDeniedResponse.isPermanentlyDenied());
                            onEmergencyModeStartFailed();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                            // ignore for now
                            permissionToken.continuePermissionRequest();
                        }
                    })
                    .check();
        }

        else mLocationFetcher.setupLocationSettings(this);
    }

    private void startEmergencyModeService() {

        // instant bug fix
        // in case permission is denied but after showing explanation dialog permission is granted
        // then before starting the service mEmergencyModeSwitch needs to be switched on
        mEmergencyModeSwitch.setChecked(true);

        Intent intent = new Intent(this, EmergencyModeService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(intent);
            Log.d(TAG, "startEmergencyModeService: foreground service started!");
        }
        else {

            startService(intent);
            Log.d(TAG, "startEmergencyModeService: couldn't start foreground service, service started with-> startService(...)");
        }
    }

    private void stopEmergencyModeService() {

        Intent intent = new Intent(this, EmergencyModeService.class);

        try{
            stopService(intent);
        } catch (Exception e) {

            Log.d(TAG, "stopEmergencyModeService: error-> "+e.getMessage());
        }
    }

    private void onEmergencyModeStartFailed() {
        mEmergencyModeSwitch.setChecked(false);
        mAppSettingsSharedPref.setEmergencyModeState(false);
    }

    /**
     * Option click listeners
     */

    public void emergencyContactsClick(View view) {

        if(!mIsInternetAvailable){
            showNoInternetDialog();
            return;
        }

        startActivity(new Intent(this, EmergencyContactsActivity.class));
    }

    public void postHelpClick(View view) {

        if(!mIsInternetAvailable){
            showNoInternetDialog();
            return;
        }

        startActivity(new Intent(this, HelpPostActivity.class));
    }

    public void findSafePlacesClick(View view) {

        if(!mIsInternetAvailable){
            showNoInternetDialog();
            return;
        }

        startActivity(new Intent(this, FindSafePlacesActivity.class));
    }

    private void showNoInternetDialog() {

        AlertDialog alertDialog = new AlertDialog.Builder(HomeActivity.this).create();

        String title = getString(R.string.no_internet);
        String explanation = getString(R.string.please_enable_internet_to_continue);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> dialog.dismiss() );

        alertDialog.show();
    }

    @Override
    public void onInternetAvailable() {

        mSnackbar.dismiss();
        mIsInternetAvailable = true;
    }

    @Override
    public void onInternetNotAvailable() {

        mSnackbar.show();
        mIsInternetAvailable = false;
    }

    /*
    show alert dialog explaining why sms permission is a MUST
    with a simple dialog, quit activity if permission is permanently denied
    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
     */
    private void showSmsPermissionExplanationDialog(boolean isPermissionPermanentlyDenied) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        String title = getString(R.string.sms_permission);
        String explanation;

        if(isPermissionPermanentlyDenied)
            explanation = getString(R.string.sms_permission_permanantely_denied_explanation);
        else
            explanation = getString(R.string.sms_permission_explanation);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> {
                    if(!isPermissionPermanentlyDenied)
                        getSmsPermission();
                    else
                        finish();
                });

        alertDialog.show();
    }

    /*
    show alert dialog explaining why location permission is a MUST
    with a simple dialog, quit activity if permission is permanently denied
    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
     */
    private void showLocationPermissionExplanationDialog(boolean isPermissionPermanentlyDenied) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        String title = getString(R.string.location_permission);
        String explanation;

        if(isPermissionPermanentlyDenied)
            explanation = getString(R.string.location_permission_permanantely_denied_explanation);
        else
            explanation = getString(R.string.location_permission_explanation);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> {
                    if(!isPermissionPermanentlyDenied)
                        getLocationPermission();
                    else
                        finish();
                });

        alertDialog.show();
    }

    /*
    show alert dialog explaining why location settings MUST be enabled
    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
     */
    private void showLocationSettingsExplanationDialog() {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        String title = getString(R.string.location_settings);
        String explanation = getString(R.string.location_settings_explanation);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> mLocationFetcher.setupLocationSettings(this));

        alertDialog.show();
    }
}
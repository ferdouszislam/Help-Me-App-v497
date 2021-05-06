package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.FindSafePlacesActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.InternetAlertActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.MenuActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.background.services.EmergencyModeService;
import com.nsu.group06.cse299.sec02.helpmeapp.background.workers.NotifyNearbyHelpPostWorker;
import com.nsu.group06.cse299.sec02.helpmeapp.sharedPreferences.AppSettingsSharedPref;

import java.util.concurrent.TimeUnit;

/**
 * App home page, which is a menu
 */
public class HomeActivity extends InternetAlertActivity {

    private static final String UNIQUE_WORKER_ID = "com.nsu.group06.cse299.sec02.helpmeapp-nearbyHelpPostsNotifier";
    private static final String TAG = "HA-debug";

    // ui
    private Snackbar mSnackbar;
    private SwitchCompat mEmergencyModeSwitch;

    // emergency mode state shared preference
    private AppSettingsSharedPref mAppSettingsSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();

        startNearbyHelpPostsNotificationWorker();
    }

    private void init() {

        View view = findViewById(R.id.menu_main_layout);
        mSnackbar = Snackbar.make(view, R.string.internet_connection_lost, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("dismiss", v -> mSnackbar.dismiss());

        mEmergencyModeSwitch = findViewById(R.id.activity_main_emergencyModeSwitch);

        mAppSettingsSharedPref = AppSettingsSharedPref.build(this);
        mEmergencyModeSwitch.setChecked(mAppSettingsSharedPref.getEmergencyModeState(false));
    }

    private void startNearbyHelpPostsNotificationWorker() {

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest notifyNearbyHelpPostsWorkRequest = new
                PeriodicWorkRequest.Builder(NotifyNearbyHelpPostWorker.class, 15, TimeUnit.MINUTES)
                //.setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                UNIQUE_WORKER_ID,
                ExistingPeriodicWorkPolicy.KEEP,
                notifyNearbyHelpPostsWorkRequest);
    }

    public void menuClick(View view) {

        startActivity(new Intent(this, MenuActivity.class));
    }

    public void emergencyModeToggleClick(View view) {

        mAppSettingsSharedPref.setEmergencyModeState(mEmergencyModeSwitch.isChecked());

        if(mEmergencyModeSwitch.isChecked()) startEmergencyModeService();
        else stopEmergencyModeService();
    }

    private void startEmergencyModeService() {

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
}
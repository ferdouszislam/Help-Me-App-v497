package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities.SettingsActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities.SetupProfileActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.FirebaseEmailPasswordAuthentication;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.SessionUtils;

public class MenuActivity extends InternetAlertActivity {

    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        init();
    }

    private void init(){

        View view = findViewById(R.id.activity_menu_mainRelativeLayout);
        mSnackbar = Snackbar.make(view, R.string.internet_connection_lost, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("dismiss", v -> mSnackbar.dismiss());
    }

    public void settingsClick(View view) {

        if(!mIsInternetAvailable){
            showNoInternetDialog();
            return;
        }

        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void setupProfileClick(View view) {

        if(!mIsInternetAvailable){
            showNoInternetDialog();
            return;
        }

        startActivity(new Intent(this, SetupProfileActivity.class));
    }

    public void logoutClick(View view) {

        SessionUtils.logout(this, new FirebaseEmailPasswordAuthentication());
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

    private void showNoInternetDialog() {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        String title = getString(R.string.no_internet);
        String explanation = getString(R.string.please_enable_internet_to_continue);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> dialog.dismiss() );

        alertDialog.show();
    }

    /*
    custom back button press
     */
    public void backPress(View view) {

        finish();
    }
}
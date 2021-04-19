package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.InternetAlertActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.MenuActivity;

/**
 * App home page, which is a menu
 */
public class HomeActivity extends InternetAlertActivity {

    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();
    }

    private void init() {

        View view = findViewById(R.id.menu_main_layout);
        mSnackbar = Snackbar.make(view, R.string.internet_connection_lost, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("dismiss", v -> mSnackbar.dismiss());
    }

    public void menuClick(View view) {

        startActivity(new Intent(this, MenuActivity.class));
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

    public void helpFeedClick(View view) {

        if(!mIsInternetAvailable){
            showNoInternetDialog();
            return;
        }

        startActivity(new Intent(this, HelpFeedActivity.class));
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
package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.Authentication;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.AuthenticationUser;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.FirebaseEmailPasswordAuthentication;
import com.rbddevs.splashy.Splashy;

/**
 * Launching activity that decides whether user is logged in or not
 * will contains welcome screen shown to user only once after installation
 */
public class MainActivity extends AppCompatActivity {

    // auth variables
    private Authentication mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        mAuth = new FirebaseEmailPasswordAuthentication(new Authentication.AuthenticationCallbacks() {
            @Override
            public void onAuthenticationSuccess(AuthenticationUser user) {

                // user is logged in start the menu activity
                startActivity(new Intent(MainActivity.this, HomeActivity.class));

                // disable going back to MainActivity
                finish();
            }

            @Override
            public void onAuthenticationFailure(String message) {
                // do nothing
            }
        });
        mAuth.authenticateUser();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            showSplashScreen();    //Show splash screen
//        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void showSplashScreen()
    {
        new Splashy(this)
                .setLogo(R.drawable.ic_app_logo_light_v2)
                .setTitle(getString(R.string.appTitle))
                //.setTitleSize(21)
                .setTitleColor("#fafafa")
                .setSubTitle(getString(R.string.subtitle))
                .setSubTitleSize(14)
                .setSubTitleColor("#fafafa")
                .setBackgroundResource(R.drawable.custom_gradient_color)
                .setProgressColor("#FFFFFF")
                .setFullScreen(true)
                .setDuration(2500)
                .show();
    }

    public void continueClick(View view) {

        // user needs to login or signup
        startActivity(new Intent(MainActivity.this, LoginActivity.class));

        // disable going back to MainActivity
        finish();
    }
}
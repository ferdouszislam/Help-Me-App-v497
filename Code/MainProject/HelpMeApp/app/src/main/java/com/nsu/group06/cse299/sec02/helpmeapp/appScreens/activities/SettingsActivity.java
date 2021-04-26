package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.FirebasePhoneAuth;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.SessionUtils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void logoutClick(View view) {

        SessionUtils.logout(this, new FirebasePhoneAuth(null));
    }

    public void backPress(View view) {

        finish();
    }
}
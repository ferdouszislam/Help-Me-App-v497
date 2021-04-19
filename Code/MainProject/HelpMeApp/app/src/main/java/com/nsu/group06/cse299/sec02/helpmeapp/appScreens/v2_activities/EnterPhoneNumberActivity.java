package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.nsu.group06.cse299.sec02.helpmeapp.R;

public class EnterPhoneNumberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_phone_number);
    }

    public void submitClick(View view) {

        startActivity(new Intent(this, EnterOTPCodeActivity.class));
    }
}
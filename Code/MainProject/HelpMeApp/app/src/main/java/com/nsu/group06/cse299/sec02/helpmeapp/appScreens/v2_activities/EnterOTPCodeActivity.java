package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.PhoneAuthUser;

public class EnterOTPCodeActivity extends AppCompatActivity {

    // model
    private PhoneAuthUser mPhoneAuthUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_otp_code);

        init();
    }

    private void init() {

        String phoneNumber = getIntent().getStringExtra(EnterPhoneNumberActivity.PHONE_NUMBER_KEY);
        mPhoneAuthUser = new PhoneAuthUser(phoneNumber);

        // TODO: implement

    }
}
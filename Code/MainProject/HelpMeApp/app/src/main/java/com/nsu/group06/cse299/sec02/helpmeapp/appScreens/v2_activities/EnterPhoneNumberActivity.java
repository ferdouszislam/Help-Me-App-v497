package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.UserInputValidator;

public class EnterPhoneNumberActivity extends AppCompatActivity {

    public static String PHONE_NUMBER_KEY = "com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities.EnterPhoneNumberActivity-phoneNumber";

    // model
    private String mPhoneNumber;

    // ui
    private EditText mPhoneNumberEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_phone_number);

        init();
    }

    private void init() {

        mPhoneNumberEditText = findViewById(R.id.activity_enter_phone_number_phoneNumberEditText);
    }

    public void submitClick(View view) {

        mPhoneNumber = mPhoneNumberEditText.getText().toString();

        if(verifyPhoneNumber(mPhoneNumber)) {

            openOtpCodeActivity();
        }
    }

    /**
     * open otp code entry activity and pass the phone number
     */
    private void openOtpCodeActivity() {

        Intent intent = new Intent(this, EnterOTPCodeActivity.class);
        intent.putExtra(PHONE_NUMBER_KEY, mPhoneNumber);
        startActivity(intent);
    }

    /**
     * verify user input
     * @param mPhoneNumber phone number
     * @return true if valid input else false
     */
    private boolean verifyPhoneNumber(String mPhoneNumber) {

        if(!UserInputValidator.isPhoneNumberValid(mPhoneNumber)){

            mPhoneNumberEditText.setError(getString(R.string.invalid_phone_number));
            return false;
        }

        return true;
    }
}
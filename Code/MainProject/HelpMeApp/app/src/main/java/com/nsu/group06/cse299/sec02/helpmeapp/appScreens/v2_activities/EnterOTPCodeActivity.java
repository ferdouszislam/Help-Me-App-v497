package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities.HomeActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.FirebasePhoneAuth;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.PhoneAuth;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.PhoneAuthUser;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.UserInputValidator;

public class EnterOTPCodeActivity extends AppCompatActivity {

    private static final String TAG = "EOCA-debug";

    // ui components
    private EditText mOtpCodeEditText;
    private Button mVerifyCodeButton;

    // model
    private PhoneAuthUser mPhoneAuthUser;

    // phone authentication variables
    private PhoneAuth mPhoneAuth;
    private PhoneAuth.PhoneAuthCallback mPhoneAuthCallback = new PhoneAuth.PhoneAuthCallback() {
        @Override
        public void onPhoneVerificationSuccess(PhoneAuthUser phoneAuthUser) {

            openHomeActivity();
        }

        @Override
        public void onPhoneVerificationFailed(String errorMessage) {

            Log.d(TAG, "onPhoneVerificationFailed: "+errorMessage);

            phoneVerificationErrorUI();
        }

        @Override
        public void onOtpCodeAutoDetected(String otpCode) {

            otpCodeAutoDetectedUI(otpCode);
        }

        @Override
        public void onInvalidOtpCodeEntry(String errorMessage) {

            invalidOtpCodeUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_otp_code);

        init();
    }

    private void init() {

        mOtpCodeEditText = findViewById(R.id.activity_enter_otp_code_OtpEditText);
        mVerifyCodeButton = findViewById(R.id.activity_enter_otp_code_verifyButton);

        String phoneNumber = getIntent().getStringExtra(EnterPhoneNumberActivity.PHONE_NUMBER_KEY);
        mPhoneAuthUser = new PhoneAuthUser(phoneNumber);

        mPhoneAuth = new FirebasePhoneAuth(mPhoneAuthUser, mPhoneAuthCallback, this);
        mPhoneAuth.sendOtpCodeTo(mPhoneAuthUser.getPhoneNumber());
    }

    // onClick methods
    public void verifyCodeClick(View view) {

        String otpCode = mOtpCodeEditText.getText().toString();

        if(validateOtpCode(otpCode)){

            verifyingOtpCodeUi();
            mPhoneAuth.verifyOtpCode(otpCode);
        }
    }

    /**
     * validate user input
     * @param otpCode user entered otp code
     * @return true if valid otp code else false
     */
    private boolean validateOtpCode(String otpCode) {

        if(!UserInputValidator.isOtpCodeValid(otpCode)) {

            mOtpCodeEditText.setError(getString(R.string.invalid_otp_code));
            return false;
        }

        return true;
    }

    public void resendCodeClick(View view) {

        finish();
    }

    // ui methods
    private void openHomeActivity() {

        startActivity(new Intent(this, HomeActivity.class));
        verificationProcessCompleteUI();
    }

    private void invalidOtpCodeUI() {

        mOtpCodeEditText.setError(getString(R.string.invalid_otp_code));
        showToast(getString(R.string.invalid_otp_code));
        verificationProcessCompleteUI();
    }

    private void otpCodeAutoDetectedUI(String otpCode) {

        mOtpCodeEditText.setText(otpCode);
    }

    private void phoneVerificationErrorUI() {

        showToast(getString(R.string.phone_verification_error));
        verificationProcessCompleteUI();
    }

    private void verifyingOtpCodeUi(){

        mVerifyCodeButton.setEnabled(false);
        mVerifyCodeButton.setText(getString(R.string.verifying));
    }

    private void verificationProcessCompleteUI(){

        mVerifyCodeButton.setEnabled(true);
        mVerifyCodeButton.setText(getString(R.string.verify));
    }

    private void showToast(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT)
                .show();
    }
}
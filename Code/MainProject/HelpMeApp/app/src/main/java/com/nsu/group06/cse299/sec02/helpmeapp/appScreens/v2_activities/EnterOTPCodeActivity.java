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
import com.nsu.group06.cse299.sec02.helpmeapp.database.Database;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBApiEndPoint;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBSingleOperation;
import com.nsu.group06.cse299.sec02.helpmeapp.models.User;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.NosqlDatabasePathUtils;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.SessionUtils;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.UserInputValidator;

public class EnterOTPCodeActivity extends AppCompatActivity {

    private static final String TAG = "EOCA-debug";

    // ui components
    private EditText mOtpCodeEditText;
    private Button mVerifyCodeButton;

    // model
    private PhoneAuthUser mPhoneAuthUser;
    private User mUser;

    // phone authentication variables
    private PhoneAuth mPhoneAuth;
    private PhoneAuth.PhoneAuthCallback mPhoneAuthCallback = new PhoneAuth.PhoneAuthCallback() {
        @Override
        public void onPhoneVerificationSuccess(PhoneAuthUser phoneAuthUser) {

            mPhoneAuthUser = phoneAuthUser;
            mUser.setPhoneNumber(phoneAuthUser.getPhoneNumber());
            mUser.setUid(phoneAuthUser.getmUid());

            checkIfUserDataExistsInDatabase(mUser);
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

    // database variables
    private Database.SingleOperationDatabase<User> mSingleOperationDatabase;
    private FirebaseRDBApiEndPoint mApiEndPoint;
    private Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User> mCreateUserSingleOperationDatabaseCallback =
            new Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User>() {
                @Override
                public void onDataRead(User data) {
                    // not reading anything keep empty
                }

                @Override
                public void onReadDataNotFound() {
                    // not reading anything keep empty
                }

                @Override
                public void onDatabaseOperationSuccess() {

                    openHomeActivity();

                    Log.d(TAG, "onDatabaseOperationSuccess: create user database operation success!");
                }

                @Override
                public void onDatabaseOperationFailed(String message) {
                    // very unusual if this method gets called!
                    // if authentication was a success it is very unlikely that any database operation will fail.
                    Log.d(TAG, "onDatabaseOperationFailed: failed to store newly registered user information in database -> "+message);
                    // TODO: delete the authentication information of the user from Auth system!
                }
            };
    private Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User> mCheckUserExistenceCallback =
            new Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User>() {
                @Override
                public void onDataRead(User data) {

                    // user exists
                    mUser = data;
                    openHomeActivity();
                }

                @Override
                public void onReadDataNotFound() {

                    storeUserInformationInDatabase();
                }

                @Override
                public void onDatabaseOperationSuccess() {
                    Log.d(TAG, "onDatabaseOperationSuccess: check user existence database opearation success");
                }

                @Override
                public void onDatabaseOperationFailed(String message) {
                    // very unusual if this method gets called!
                    // if authentication was a success it is very unlikely that any database operation will fail.
                    Log.d(TAG, "onDatabaseOperationFailed: user existence check failed-> "+message);

                    SessionUtils.logout(EnterOTPCodeActivity.this, mPhoneAuth);
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
        mUser = new User();

        mPhoneAuth = new FirebasePhoneAuth(mPhoneAuthUser, mPhoneAuthCallback, this);
        mPhoneAuth.sendOtpCodeTo(mPhoneAuthUser.getPhoneNumber());
    }

    /**
     * check database to see if user data exists
     * if exists then user has signed up before, else this is the first time
     * @param user user model
     */
    private void checkIfUserDataExistsInDatabase(User user) {

        mApiEndPoint = new FirebaseRDBApiEndPoint("/"+ NosqlDatabasePathUtils.USER_NODE+":"+user.getUid());

        mSingleOperationDatabase
                = new FirebaseRDBSingleOperation<>(User.class, mApiEndPoint, mCheckUserExistenceCallback);

        mSingleOperationDatabase.readSingle();
    }

    /**
     * store User information(just the phone number) into database
     */
    private void storeUserInformationInDatabase(){

        mApiEndPoint = new FirebaseRDBApiEndPoint("/"+ NosqlDatabasePathUtils.USER_NODE);

        mSingleOperationDatabase =
                new FirebaseRDBSingleOperation<>(User.class, mApiEndPoint, mCreateUserSingleOperationDatabaseCallback);

        mSingleOperationDatabase.createWithId(mUser.getUid(), mUser);
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

        Intent intent = new Intent(this, HomeActivity.class);
        // clear out all activities on the back stack and open LoginActivity
        // so that back press from this point on closes the app
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

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
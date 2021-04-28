package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.Authentication;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.AuthenticationUser;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.FirebasePhoneAuth;
import com.nsu.group06.cse299.sec02.helpmeapp.database.Database;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBApiEndPoint;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBSingleOperation;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.FetchedLocation;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.LocationFetcher;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.fusedLocationApi.FusedLocationFetcherApiAdapter;
import com.nsu.group06.cse299.sec02.helpmeapp.models.User;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.NosqlDatabasePathUtils;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.SessionUtils;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.UserInputValidator;

public class SetupProfileActivity extends AppCompatActivity {

    private static final String TAG = "SPA-debug";

    // ui
    private EditText mUsernameEditText; //mDateOfBirhtEditText, mAddressEditText, mPhoneNumberEditText;
    private Button mSaveButton, mFetchHomeLocationButton;


    // model
    private User mUser;


    // variable used for fetching user uid
    private Authentication mAuth;
    private Authentication.AuthenticationCallbacks mAuthenticationCallbacks = new Authentication.AuthenticationCallbacks() {
        @Override
        public void onAuthenticationSuccess(AuthenticationUser user) {

            mUser.setUid(user.getmUid());

            initDatabaseVars(); // DO NOT TOUCH THIS LINE

            loadUserProfileInformation();
        }

        @Override
        public void onAuthenticationFailure(String message) {

            SessionUtils.logout(SetupProfileActivity.this, mAuth);
        }
    };


    // variable for shortcut for using a single database operation listener
    private boolean mSaveButtonWasClicked = false;
    // variables to read/write information of users to/from the database
    private Database.SingleOperationDatabase<User> mUserInfoFirebaseRDBSingleOperation;
    private FirebaseRDBApiEndPoint mUserInfoApiEndPoint;
    private Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User> mUserInfoSingleOperationDatabaseCallback =
            new Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User>() {
                @Override
                public void onDataRead(User data) {

                    mUser = data;
                    showReadUserInfoInUI();
                }

                @Override
                public void onReadDataNotFound() {
                    // TODO: is any implementation needed here?
                }

                @Override
                public void onDatabaseOperationSuccess() {
                    // not required
                    databaseOperationCompleteUI();
                }

                @Override
                public void onDatabaseOperationFailed(String message) {

                    showToast(getString(R.string.failed_to_connect));

                    Log.d(TAG, "onDatabaseOperationFailed: user data read error -> "+message);
                }
            };


    // variables used to fetch location
    private FetchedLocation mFetchedLocation;
    private LocationFetcher mLocationFetcher;
    private LocationFetcher.LocationSettingsSetupListener mLocationSettingsSetupListener =
            new LocationFetcher.LocationSettingsSetupListener() {
                @Override
                public void onLocationSettingsSetupSuccess() {

                    mLocationFetcher.startLocationUpdate();
                }

                @Override
                public void onLocationSettingsSetupFailed(String message) {
                    // user will be automatically be asked to enable location settings
                    // see method in SetupProfileActivity: 'onActivityResult(...)'
                    Log.d(TAG, "onLocationSettingsSetupFailed: location settings setup failed ->" + message);
                }
            };
    private LocationFetcher.LocationUpdateListener mLocationUpdateListener =
            new LocationFetcher.LocationUpdateListener() {
                @Override
                public void onNewLocationUpdate(FetchedLocation fetchedLocation) {

                    if(mFetchedLocation==null || mFetchedLocation.getmAccuracy() > fetchedLocation.getmAccuracy()
                            || FetchedLocation.isLocationSignificantlyDifferent(mFetchedLocation, fetchedLocation)) {

                        if(mFetchedLocation==null) fetchLocationSuccessUI();

                        mFetchedLocation = fetchedLocation;
                    }

                    Log.d(TAG, "onNewLocationUpdate: location -> "+fetchedLocation.toString());
                }

                @Override
                public void onPermissionNotGranted() {

                    fetchLocationFailedUI();
                    mLocationFetcher.stopLocationUpdate();

                    Log.d(TAG, "onPermissionNotGranted: location permission not granted");
                }

                @Override
                public void onError(String message) {

                    if(mFetchedLocation==null) fetchLocationFailedUI();
                    mLocationFetcher.stopLocationUpdate();

                    Log.d(TAG, "onError: location update error -> "+message);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        init();
    }

    /*
    Required for reacting to user response when setting up required location settings
    user response to default "turn on location" dialog is handled through this method
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){

            case LocationFetcher.REQUEST_CHECK_LOCATION_SETTINGS:

                if(resultCode==RESULT_OK){
                    // user enabled location settings
                    mLocationFetcher.startLocationUpdate();
                }

                else{
                    // location settings not met
                    showLocationSettingsExplanationDialog();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mLocationFetcher!=null) mLocationFetcher.stopLocationUpdate();
    }

    private void init() {

        mUsernameEditText = findViewById(R.id.username_setupProfile_EditText);
        //mDateOfBirhtEditText = findViewById(R.id.dateOfBirth_setupProfile_EditText);
        //mAddressEditText = findViewById(R.id.address_setupProfile_EditText);
        //mPhoneNumberEditText = findViewById(R.id.phoneNumber_setupProfile_EditText);
        mSaveButton = findViewById(R.id.btn_setupProfile_save);
        mFetchHomeLocationButton = findViewById(R.id.activity_setup_profile_fetchHomeAddressButton);

        mUser = new User();

        // authenticate user, because we need uid here
        mAuth = new FirebasePhoneAuth(mAuthenticationCallbacks);
        mAuth.authenticateUser();

        // location fetcher
        mLocationFetcher = new FusedLocationFetcherApiAdapter(
                1000, this,
                mLocationSettingsSetupListener,
                mLocationUpdateListener
        );
    }

    /*
    Initialize database variables
     */
    private void initDatabaseVars() {

        mUserInfoApiEndPoint = new FirebaseRDBApiEndPoint(
                "/"+ NosqlDatabasePathUtils.USER_NODE +
                        ":" + mUser.getUid());

        mUserInfoFirebaseRDBSingleOperation =
                new FirebaseRDBSingleOperation<>(User.class, mUserInfoApiEndPoint, mUserInfoSingleOperationDatabaseCallback);
    }

    /*
     Download user profile information from the database
     */
    private void loadUserProfileInformation() {

        mUserInfoFirebaseRDBSingleOperation.readSingle();
        loadingProfileInfoUI();
    }

    /*
      Show read user info from database to UI
     */
    private void showReadUserInfoInUI() {

        mUsernameEditText.setText(mUser.getUsername());
        //mDateOfBirhtEditText.setText(mUser.getDateOfBirth());
        //mAddressEditText.setText(mUser.getAddress());
        //mPhoneNumberEditText.setText(mUser.getPhoneNumber());
    }


    /*
    "save profile" click listener
     */
    public void saveProfileClick(View view) {

        if(!validateInputs()) return;

        mSaveButtonWasClicked = true;
        //TODO: update database
        mUserInfoFirebaseRDBSingleOperation.update(mUser);
        updatingProfileUI();
    }

    /*
    Validate user input
     */
    private boolean validateInputs() {

        boolean isValid = true;

        String name = mUsernameEditText.getText().toString();
        //String dateOfBirth = mDateOfBirhtEditText.getText().toString();
        //String address = mAddressEditText.getText().toString();
        //String phoneNumber = mPhoneNumberEditText.getText().toString();
        //if(phoneNumber.charAt(0)=='0') phoneNumber = "+88" + phoneNumber;

        if(!UserInputValidator.isNameValid(name)){
            mUsernameEditText.setError(getString(R.string.invalid_username));
            isValid = false;
        }
        /*
        if(!UserInputValidator.isDateOfBirthValid(dateOfBirth)){
            mDateOfBirhtEditText.setError(getString(R.string.invalid_date_of_birth));
            isValid = false;
        }

        if(!UserInputValidator.isAddressValid(address)){
            // address input is optional

            address = "";
            //mAddressEditText.setError(getString(R.string.invalid_address));
            //isValid = false;
        }

        if(!UserInputValidator.isPhoneNumberValid(phoneNumber)){
            mPhoneNumberEditText.setError(getString(R.string.invalid_phone_number));
            isValid = false;
        }
         */

        if(!isValid) return false;

        if(mUser.getUsername().equals(name)
             && mFetchedLocation==null
            //&& mUser.getDateOfBirth().equals(dateOfBirth)
            //&& mUser.getAddress().equals(address)
            //&& mUser.getPhoneNumber().equals(phoneNumber)
        ){
            // user profile data was not changed
            return false;
        }

        mUser.setUsername(name);
        //mUser.setDateOfBirth(dateOfBirth);
        //mUser.setAddress(address);
        //mUser.setPhoneNumber(phoneNumber);

        if(mFetchedLocation!=null){

            mUser.setHomeAddressLatitude(mFetchedLocation.getmLatitude());
            mUser.setHomeAddressLongitude(mFetchedLocation.getmLongitude());
        }

        return true;
    }

    /*
    "fetch current location" button click
     */
    public void fetchCurrentLocationAsHomeAddressClick(View view) {

        fetchLocationInProgressUI();
        getCurrentLocationAsHome();
    }

    private void getCurrentLocationAsHome() {

        getLocationPermissions();
    }

    /*
    Ask for location access permission
    using the open source library- <https://github.com/Karumi/Dexter>
     */
    private void getLocationPermissions() {

        Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        mLocationFetcher.setupLocationSettings(SetupProfileActivity.this);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        showLocationPermissionExplanationDialog(permissionDeniedResponse.isPermanentlyDenied());
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        // ignore for now
                        permissionToken.continuePermissionRequest();
                    }
                })
                .check();
    }

    /*
    set UI during user info is being downloaded
     */
    private void loadingProfileInfoUI(){

        mSaveButton.setEnabled(false);
        mSaveButton.setText(getString(R.string.loading));

        mUsernameEditText.setEnabled(false);
        //mDateOfBirhtEditText.setEnabled(false);
        //mAddressEditText.setEnabled(false);
        //mPhoneNumberEditText.setEnabled(false);
    }

    /*
    set UI during user info update is in progress
     */
    private void updatingProfileUI(){

        mSaveButton.setEnabled(false);
        mSaveButton.setText(getString(R.string.saving));

        mUsernameEditText.setEnabled(false);
        //mDateOfBirhtEditText.setEnabled(false);
        //mAddressEditText.setEnabled(false);
        //mPhoneNumberEditText.setEnabled(false);
    }

    /*
    set UI when database operation (load/update profile info) is complete
     */
    private void databaseOperationCompleteUI() {

        if(mSaveButtonWasClicked){
            // means this was called after user data got updated at database
            showToast(getString(R.string.saved));
            finish();
        }

        mSaveButton.setEnabled(true);
        mSaveButton.setText(getString(R.string.save));

        mUsernameEditText.setEnabled(true);
        //mDateOfBirhtEditText.setEnabled(true);
        //mAddressEditText.setEnabled(true);
        //mPhoneNumberEditText.setEnabled(true);
    }

    private void fetchLocationInProgressUI(){

        mFetchHomeLocationButton.setText(getString(R.string.fetching_location));
        mFetchHomeLocationButton.setEnabled(false);
    }


    private void fetchLocationSuccessUI(){

        mFetchHomeLocationButton.setText(getString(R.string.home_address_taken));
    }


    private void fetchLocationFailedUI(){

        showToast(getString(R.string.location_fetch_failed));
        mFetchHomeLocationButton.setEnabled(true);
        mFetchHomeLocationButton.setText(getString(R.string.fetch_my_current_location_as_home_address));
    }

    /*
    show alert dialog explaining why location settings MUST be enabled
    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
     */
    private void showLocationSettingsExplanationDialog() {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        String title = getString(R.string.location_settings);
        String explanation = getString(R.string.location_settings_explanation);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> mLocationFetcher.setupLocationSettings(this));

        alertDialog.show();
    }
    /*
    show alert dialog explaining why location permission is a MUST
    with a simple dialog, quit activity if permission is permanently denied
    courtesy - <https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
     */
    private void showLocationPermissionExplanationDialog(boolean isPermissionPermanentlyDenied) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        String title = getString(R.string.location_permission);
        String explanation;

        if(isPermissionPermanentlyDenied)
            explanation = getString(R.string.location_permission_permanantely_denied_explanation);
        else
            explanation = getString(R.string.location_permission_explanation);

        alertDialog.setTitle(title);
        alertDialog.setMessage(explanation);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                (dialog, which) -> {
                    if(!isPermissionPermanentlyDenied)
                        getLocationPermissions();
                    else
                        finish();
                });

        alertDialog.show();
    }

    private void showToast(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
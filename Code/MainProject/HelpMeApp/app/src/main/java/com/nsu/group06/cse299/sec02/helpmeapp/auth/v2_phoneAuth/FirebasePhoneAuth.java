package com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * Phone number verification using firebase
 */
public class FirebasePhoneAuth extends PhoneAuth {

    private static final String TAG = "FPA-debug";

    // firebase auth variables
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private Activity mCallingActivity;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    // phone verification callbacks
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    // This callback will be invoked in two situations:
                    // 1 - Instant verification. In some cases the phone number can be instantly
                    //     verified without needing to send or enter a verification code.
                    // 2 - Auto-retrieval. On some devices Google Play services can automatically
                    //     detect the incoming verification SMS and perform verification without
                    //     user action.

                    Log.d(TAG, "onVerificationCompleted:" + credential);

                    mPhoneAuthCallback.onOtpCodeAutoDetected(credential.getSmsCode());

                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the the phone number format is not valid.

                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        // Invalid request
                        Log.d(TAG, "onVerificationFailed: invalid request! " + e.getMessage());
                    }

                    else if (e instanceof FirebaseTooManyRequestsException) {
                        // The SMS quota for the project has been exceeded
                        Log.d(TAG, "onVerificationFailed: quota exceeded! " + e.getMessage());
                    }

                    mPhoneAuthCallback.onPhoneVerificationFailed(e.getMessage());
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    // by combining the code with a verification ID.
                    Log.d(TAG, "onCodeSent:" + verificationId);

                    // Save verification ID and resending token so we can use them later
                    mVerificationId = verificationId;
                    mResendToken = token;
                }
            };

    /**
     * constructor called for only authentication, i.e login status check
     * @param mAuthenticationCallbacks callback for authentication status
     */
    public FirebasePhoneAuth(AuthenticationCallbacks mAuthenticationCallbacks) {
        super(mAuthenticationCallbacks);
    }

    /**
     * constructor called for phone verification
     * @param mPhoneAuthUser model for user with phone number
     * @param mPhoneAuthCallback callback for verification via phone number
     */
    public FirebasePhoneAuth(PhoneAuthUser mPhoneAuthUser, PhoneAuthCallback mPhoneAuthCallback, Activity activity) {
        super(mPhoneAuthUser, mPhoneAuthCallback);
        mCallingActivity = activity;
    }

    /**
     * sign in user
     * @param credential user credentials provided by firebase
     */
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {

        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");

                        try {
                            String uid = task.getResult().getUser().getUid();

                            mPhoneAuthUser.setmUid(uid);
                            mPhoneAuthCallback.onPhoneVerificationSuccess(mPhoneAuthUser);

                        } catch (NullPointerException e){

                            Log.d(TAG, "signInWithPhoneAuthCredential: why is auth user null-> "+e.getMessage());
                            mPhoneAuthCallback.onPhoneVerificationFailed(e.getMessage());
                        }
                    }

                    else {

                        // Sign in failed, display a message and update the UI
                        Log.d(TAG, "signInWithCredential:failure", task.getException());

                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            mPhoneAuthCallback.onInvalidOtpCodeEntry(task.getException().getMessage());
                        }
                    }
                });
    }

    @Override
    public void sendOtpCodeTo(String phoneNumber) {

        mPhoneAuthUser.setPhoneNumber(phoneNumber);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mFirebaseAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(mCallingActivity)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    @Override
    public void verifyOtpCode(String otpCode) {

        if(mVerificationId==null){

            mPhoneAuthCallback
                    .onPhoneVerificationFailed("mVerificationId, supposed to be provided by firebase is null!");
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otpCode);
        signInWithPhoneAuthCredential(credential);
    }

    @Override
    public void registerUserAuthentication() {

        sendOtpCodeTo(mPhoneAuthUser.getPhoneNumber());
    }

    @Override
    public void authenticateUser() {

        if(mFirebaseAuth.getCurrentUser()!=null){

            if(mPhoneAuthUser==null) mPhoneAuthUser = new PhoneAuthUser();

            mPhoneAuthUser.setmUid(mFirebaseAuth.getUid());

            try{
                String phoneNumber = mFirebaseAuth.getCurrentUser().getPhoneNumber();

                mPhoneAuthUser.setPhoneNumber(phoneNumber);
                mAuthenticationCallbacks.onAuthenticationSuccess(mPhoneAuthUser);
            }catch (NullPointerException e){

                mAuthenticationCallbacks.onAuthenticationFailure(e.getMessage());
            }
        }

        else{

            mAuthenticationCallbacks.onAuthenticationFailure("user not logged in");
        }
    }

    @Override
    public void signOut() {

        mFirebaseAuth.signOut();
    }
}

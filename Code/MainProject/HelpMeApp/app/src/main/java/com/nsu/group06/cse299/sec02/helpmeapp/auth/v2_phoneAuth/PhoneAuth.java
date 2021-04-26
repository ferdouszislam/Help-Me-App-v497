package com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth;

import com.nsu.group06.cse299.sec02.helpmeapp.auth.Authentication;

/**
 * Abstract super class for phone number authentication
 */
public abstract class PhoneAuth extends Authentication {

    protected PhoneAuthUser mPhoneAuthUser;

    // callback
    protected PhoneAuth.PhoneAuthCallback mPhoneAuthCallback;

    public PhoneAuth(AuthenticationCallbacks mAuthenticationCallbacks) {
        super(mAuthenticationCallbacks);
    }

    public PhoneAuth(PhoneAuthUser mPhoneAuthUser, PhoneAuthCallback mPhoneAuthCallback) {
        this.mPhoneAuthUser = mPhoneAuthUser;
        this.mPhoneAuthCallback = mPhoneAuthCallback;
    }

    public abstract void sendOtpCodeTo(String phoneNumber);

    public abstract void verifyOtpCode(String otpCode);

    public PhoneAuthUser getmPhoneAuthUser() {
        return mPhoneAuthUser;
    }

    public void setmPhoneAuthUser(PhoneAuthUser mPhoneAuthUser) {
        this.mPhoneAuthUser = mPhoneAuthUser;
    }

    public interface PhoneAuthCallback{

        /**
         * phone verification complete callback
         * @param phoneAuthUser user credentials
         */
        void onPhoneVerificationSuccess(PhoneAuthUser phoneAuthUser);

        /**
         * verification failed for reasons such as- invalid phone number, quota exceeded etc
         * @param errorMessage what went wrong
         */
        void onPhoneVerificationFailed(String errorMessage);

        /**
         * otp code auto detected callback
         * @param otpCode code sent via sms
         */
        void onOtpCodeAutoDetected(String otpCode);

        /**
         * user entered invalid otp code
         * @param errorMessage what went wrong
         */
        void onInvalidOtpCodeEntry(String errorMessage);
    }

}

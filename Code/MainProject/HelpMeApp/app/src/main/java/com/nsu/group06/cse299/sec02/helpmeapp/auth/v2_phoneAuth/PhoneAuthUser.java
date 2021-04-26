package com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth;

import com.nsu.group06.cse299.sec02.helpmeapp.auth.AuthenticationUser;

/**
 * Auth user with phone & verification code
 */
public class PhoneAuthUser extends AuthenticationUser {

    private String phoneNumber;

    public PhoneAuthUser() {
    }

    public PhoneAuthUser(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

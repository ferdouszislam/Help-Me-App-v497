package com.nsu.group06.cse299.sec02.helpmeapp.reverseGeocoding;


public abstract class ReverseGeocodeApiAdapter {

    protected ReverseGeocodeApiAdapter.Callback mCallback;

    public ReverseGeocodeApiAdapter(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public abstract void setupApi();

    public abstract void fetchAddress(double latitude, double longitude);

    public interface Callback {

        void onSetupSuccess();
        void onSetupFailure(String message);
        void onAddressFetchSuccess(String address);
        void onAddressFetchError(String message);
    }
}

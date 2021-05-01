package com.nsu.group06.cse299.sec02.helpmeapp.reverseGeocoding.barikoiReverseGeo;

import android.content.Context;

import com.nsu.group06.cse299.sec02.helpmeapp.reverseGeocoding.ReverseGeocodeApiAdapter;
import com.nsu.group06.cse299.sec02.helpmeapp.secretApiKey.ApiKey;

import barikoi.barikoilocation.BarikoiAPI;
import barikoi.barikoilocation.PlaceModels.ReverseGeoPlace;
import barikoi.barikoilocation.ReverseGeo.ReverseGeoAPI;
import barikoi.barikoilocation.ReverseGeo.ReverseGeoAPIListener;

public class BarikoiReverseGeocode extends ReverseGeocodeApiAdapter {

    private Context mContext;
    private boolean apiSetupDone = false;

    public BarikoiReverseGeocode(ReverseGeocodeApiAdapter.Callback mCallback, Context context) {
        super(mCallback);

        mContext = context;
    }

    @Override
    public void setupApi() {

        if(apiSetupDone) return;

        try{
            BarikoiAPI.getINSTANCE(mContext, ApiKey.BARIKOI_API_KEY);
            apiSetupDone = true;
            mCallback.onSetupSuccess();

        } catch (Exception e) {

            mCallback.onSetupFailure(e.getMessage());
        }
    }

    @Override
    public void fetchAddress(double latitude, double longitude) {

        try{

            ReverseGeoAPI.builder(mContext)
                    .setLatLng(latitude,longitude)
                    .build()
                    .getAddress(new ReverseGeoAPIListener() {
                        @Override
                        public void reversedAddress(ReverseGeoPlace place) {

                            mCallback.onAddressFetchSuccess(place.getAddress());
                        }

                        @Override
                        public void onFailure(String message) {

                            mCallback.onAddressFetchError(message);
                        }
                    });
        } catch (Exception e) {

            mCallback.onSetupFailure(e.getMessage());
        }
    }
}

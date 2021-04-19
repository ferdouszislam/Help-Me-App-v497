package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.v2_activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.broadcastReceivers.NetworkConnectionBroadcastReceiver;

public abstract class InternetAlertActivity extends AppCompatActivity implements NetworkConnectionBroadcastReceiver.InternetStatusCallback{

    private NetworkConnectionBroadcastReceiver mNetworkConnBroadcastReceiver = null;

    protected boolean mIsInternetAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
    }

    private void init() {

        mIsInternetAvailable = true;

        mNetworkConnBroadcastReceiver =
                new NetworkConnectionBroadcastReceiver(this);
        // register the broadcast receiver
        registerReceiver(mNetworkConnBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mNetworkConnBroadcastReceiver!=null) {
            // register the broadcast receiver
            registerReceiver(mNetworkConnBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

    }

    @Override
    protected void onStop() {

        if(mNetworkConnBroadcastReceiver!=null) unregisterReceiver(mNetworkConnBroadcastReceiver);

        super.onStop();
    }

    @Override
    public abstract void onInternetAvailable();

    @Override
    public abstract void onInternetNotAvailable() ;

}
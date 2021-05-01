package com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.models.HelpPost;
import com.nsu.group06.cse299.sec02.helpmeapp.models.MarkedUnsafeLocation;
import com.nsu.group06.cse299.sec02.helpmeapp.recyclerViewAdapters.HelpPostsAdapter;
import com.nsu.group06.cse299.sec02.helpmeapp.reverseGeocoding.ReverseGeocodeApiAdapter;
import com.nsu.group06.cse299.sec02.helpmeapp.reverseGeocoding.barikoiReverseGeo.BarikoiReverseGeocode;

public class HelpFeedActivity extends AppCompatActivity {

    private static final String TAG = "HFA-debug";

    // ui
    private TextView mNoDataFoundTextView, mHelpFeedLocationTextView;
    private RecyclerView mHelpPostsRecyclerView;
    private LinearLayout headerLayout;

    // model
    private MarkedUnsafeLocation mMarkedUnsafeLocation;

    // recyclerview adapter
    private HelpPostsAdapter mHelpPostsAdapter;
    private HelpPostsAdapter.CallerCallbacks mHelpPostsAdapterCallerCallbacks =
            new HelpPostsAdapter.CallerCallbacks() {
                @Override
                public void onShowLocationClick(HelpPost helpPost) {

                    Uri geolocation = Uri.parse("geo:0,0?q="+helpPost.getLatitude()+","+helpPost.getLongitude()+"(Distress Location)&z=17");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(geolocation);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }

                @Override
                public void onListNotEmpty() {

                    mNoDataFoundTextView.setVisibility(View.GONE);

                    if(mHelpPostsRecyclerView==null) mHelpPostsRecyclerView = findViewById(R.id.helpPosts_RecyclerView);
                    mHelpPostsRecyclerView.setVisibility(View.VISIBLE);

                    headerLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError() {

                    Toast.makeText(HelpFeedActivity.this, R.string.failed_to_connect, Toast.LENGTH_LONG)
                            .show();
                }
            };

    // reverse geo-coding variables
    private ReverseGeocodeApiAdapter mReverseGeocodeApi;
    private ReverseGeocodeApiAdapter.Callback mReverseGeocodeApiCallback =
            new ReverseGeocodeApiAdapter.Callback() {
                @Override
                public void onSetupSuccess() {

                    mReverseGeocodeApi
                            .fetchAddress(mMarkedUnsafeLocation.getLatitude(), mMarkedUnsafeLocation.getLongitude());
                }

                @Override
                public void onSetupFailure(String message) {

                    Log.d(TAG, "onSetupFailure: error-> "+message);
                    fetchLocationAddressFailedUI();
                }

                @Override
                public void onAddressFetchSuccess(String address) {

                    fetchLocationAddressSuccessUI(address);
                }

                @Override
                public void onAddressFetchError(String message) {

                    Log.d(TAG, "onAddressFetchError: error-> "+message);
                    fetchLocationAddressFailedUI();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_feed);

        init();
    }

    private void init() {

        mMarkedUnsafeLocation =
                (MarkedUnsafeLocation) getIntent().getSerializableExtra(MarkedUnsafeLocation.OBJECT_PASSING_KEY);

        mHelpFeedLocationTextView = findViewById(R.id.activity_help_feed_locationTextView);
        mNoDataFoundTextView = findViewById(R.id.helpFeed_empty_TextView);
        headerLayout = findViewById(R.id.header);

        mHelpPostsRecyclerView = findViewById(R.id.helpPosts_RecyclerView);
        mHelpPostsAdapter =
                new HelpPostsAdapter(this, mHelpPostsAdapterCallerCallbacks, mMarkedUnsafeLocation.getmHelpPosts());
        mHelpPostsRecyclerView.setAdapter(mHelpPostsAdapter);
        mHelpPostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchHelpFeedLocation();
    }

    private void fetchHelpFeedLocation() {

        mReverseGeocodeApi = new BarikoiReverseGeocode(mReverseGeocodeApiCallback, this);
        mReverseGeocodeApi.setupApi();

        fetchLocationAddressInProgressUI();
    }

    private void fetchLocationAddressInProgressUI() {

        mHelpFeedLocationTextView.setText(getString(R.string.fetching_location));
    }

    private void fetchLocationAddressSuccessUI(String address) {

        mHelpFeedLocationTextView.setText(address);
    }

    private void fetchLocationAddressFailedUI() {

        mHelpFeedLocationTextView.setText(getString(R.string.location_fetch_failed));
    }

    /*
    custom back button onClick
     */
    public void backPress(View view) {

        finish();
    }
}
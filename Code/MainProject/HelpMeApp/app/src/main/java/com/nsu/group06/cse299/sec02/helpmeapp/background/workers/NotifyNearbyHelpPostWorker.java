package com.nsu.group06.cse299.sec02.helpmeapp.background.workers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nsu.group06.cse299.sec02.helpmeapp.auth.Authentication;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.AuthenticationUser;
import com.nsu.group06.cse299.sec02.helpmeapp.auth.v2_phoneAuth.FirebasePhoneAuth;
import com.nsu.group06.cse299.sec02.helpmeapp.database.Database;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBApiEndPoint;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBRealtime;
import com.nsu.group06.cse299.sec02.helpmeapp.database.firebase_database.FirebaseRDBSingleOperation;
import com.nsu.group06.cse299.sec02.helpmeapp.fetchLocation.FetchedLocation;
import com.nsu.group06.cse299.sec02.helpmeapp.models.HelpPost;
import com.nsu.group06.cse299.sec02.helpmeapp.models.User;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.NosqlDatabasePathUtils;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.TimeUtils;

/**
 * Class to run on the background and notify user of recent nearby help posts
 */
public class NotifyNearbyHelpPostWorker extends Worker {

    private static final String TAG = "NNHPW-debug";

    private static final double MINIMUM_DISTANCE_DIFFERENCE = 50.00d; // meters
    private static final int MINIMUM_TIME_DIFFERENCE = 30; // minutes

    // variable used for fetching user uid
    private Authentication mAuth;
    private Authentication.AuthenticationCallbacks mAuthenticationCallbacks = new Authentication.AuthenticationCallbacks() {
        @Override
        public void onAuthenticationSuccess(AuthenticationUser user) {

            getUserHomeAddress(user);
        }

        @Override
        public void onAuthenticationFailure(String message) {

            Log.d(TAG, "onAuthenticationFailure: error-> "+message);
        }
    };


    // variables to read/write information of users to/from the database
    private User mUser;
    private Database.SingleOperationDatabase<User> mUserInfoFirebaseRDBSingleOperation;
    private FirebaseRDBApiEndPoint mUserInfoApiEndPoint;
    private Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User> mUserInfoSingleOperationDatabaseCallback =
            new Database.SingleOperationDatabase.SingleOperationDatabaseCallback<User>() {
                @Override
                public void onDataRead(User data) {

                    fetchRecentNearbyHelpPostsFor(data);
                }

                @Override
                public void onReadDataNotFound() {
                    // not reading anything
                }

                @Override
                public void onDatabaseOperationSuccess() {
                    // not required
                }

                @Override
                public void onDatabaseOperationFailed(String message) {
                    Log.d(TAG, "onDatabaseOperationFailed: user data read error -> "+message);
                }
            };


    // variables to access database
    private Database.RealtimeDatabase mReadHelpPostsRealtimeDatabase;
    private FirebaseRDBApiEndPoint mHelpPostsApiEndPoint =
            new FirebaseRDBApiEndPoint("/"+ NosqlDatabasePathUtils.HELP_POSTS_NODE);
    private Database.RealtimeDatabase.RealtimeChangesDatabaseCallback<HelpPost> mHelpPostRealtimeChangesDatabaseCallback =
            new Database.RealtimeDatabase.RealtimeChangesDatabaseCallback<HelpPost>() {
                @Override
                public void onDataAddition(HelpPost data) {
                    checkNewHelpPost(data);
                }

                @Override
                public void onDataUpdate(HelpPost data) {
                    checkNewHelpPost(data);
                }

                @Override
                public void onDataDeletion(HelpPost data) {
                    // not required
                }

                @Override
                public void onDatabaseOperationSuccess() {
                    // not required
                }

                @Override
                public void onDatabaseOperationFailed(String message) {
                    Log.d(TAG, "onDatabaseOperationFailed: error-> "+message);
                }
            };


    public NotifyNearbyHelpPostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        init();

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();

        if(mReadHelpPostsRealtimeDatabase!=null) mReadHelpPostsRealtimeDatabase.stopListeningForDataChange();
    }


    private void init() {

        mAuth = new FirebasePhoneAuth(mAuthenticationCallbacks);
        mAuth.authenticateUser();
    }

    /**
     * get user's home address
     * @param user, model for an auth user
     */
    private void getUserHomeAddress(AuthenticationUser user) {

        mUserInfoApiEndPoint = new FirebaseRDBApiEndPoint(
                "/"+ NosqlDatabasePathUtils.USER_NODE +
                        ":" + mUser.getUid());

        mUserInfoFirebaseRDBSingleOperation =
                new FirebaseRDBSingleOperation<>(User.class, mUserInfoApiEndPoint, mUserInfoSingleOperationDatabaseCallback);

        mUserInfoFirebaseRDBSingleOperation.readSingle();
    }

    /**
     * get nearby help posts
     * @param user model for an user
     */
    private void fetchRecentNearbyHelpPostsFor(User user) {

        mUser = user;

        mReadHelpPostsRealtimeDatabase = new FirebaseRDBRealtime<>(
                HelpPost.class,
                mHelpPostsApiEndPoint,
                mHelpPostRealtimeChangesDatabaseCallback
        );

        mReadHelpPostsRealtimeDatabase.listenForListDataChange();
    }

    /**
     * check if new help post is recent and nearby
     * @param helpPost, model for help post
     */
    private void checkNewHelpPost(HelpPost helpPost) {

        if(FetchedLocation.distanceBetween(mUser.getHomeAddressLatitude(),
                helpPost.getLatitude(), mUser.getHomeAddressLongitude(), helpPost.getLongitude()) <= MINIMUM_DISTANCE_DIFFERENCE) {

            if(isWithinLast30Mins(helpPost.getTimeStamp(), TimeUtils.getCurrentTime())) {

                mReadHelpPostsRealtimeDatabase.stopListeningForDataChange();
                showNotificationOf(helpPost);
            }
        }
    }

    /**
     * show notification that would open up to show helpPost
     * @param helpPost, model for help post
     */
    private void showNotificationOf(HelpPost helpPost) {

        Toast.makeText(getApplicationContext(), "new post", Toast.LENGTH_SHORT).show();
    }

    /**
     * check if help post timeStamp is <=30mins from currentTime
     * @param timeStamp, help post time
     * @param currentTime, current time
     * @return true if helpPost timeStamp<=30mins than currentTime
     */
    private boolean isWithinLast30Mins(String timeStamp, String currentTime) {

        return true;
    }
}

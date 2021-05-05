package com.nsu.group06.cse299.sec02.helpmeapp.background.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities.SingleHelpPostActivity;
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


    // variables to read information of user from the database
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


    // variables to access database for help posts
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

        Log.d(TAG, "doWork: work manager ran!");

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
                        ":" + user.getmUid());

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

            if(isWithinLastMinimumTimeDifference(helpPost.getTimeStamp(), TimeUtils.getCurrentTime())) {

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

        String notificationChannelId = "nearby-help-posts-72";
        int notificationId = 159;

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Nearby help posts";
            String description = "Notify when a nearby help post is made";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(notificationChannelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        String notificationTitle = "Someone is in trouble!";
        String notificationDescription = "A help post was made from nearby. Tap to see.";

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(getApplicationContext(), SingleHelpPostActivity.class);
        intent.putExtra(HelpPost.ACTIVITY_PASSING_KEY, helpPost);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId)
                .setSmallIcon(R.drawable.ic_app_logo_dark_v2)
                .setContentTitle(notificationTitle)
                .setContentText(notificationDescription)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(notificationSound)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * check if help post timeStamp is <=30mins from currentTime
     * @param helpPostTimeStamp, help post time
     * @param currentTime, current time
     * @return true if helpPost timeStamp<=30mins than currentTime
     */
    private boolean isWithinLastMinimumTimeDifference(String helpPostTimeStamp, String currentTime) {

        if(TimeUtils.getDateFromTimeStamp(helpPostTimeStamp).equals(TimeUtils.getDateFromTimeStamp(currentTime))) {

            int helpPost_hour, helpPost_min, curr_hour, curr_min;

            helpPost_hour = TimeUtils.getHourFromTimeStamp(helpPostTimeStamp);
            helpPost_min = TimeUtils.getMinuteFromTimeStamp(helpPostTimeStamp);
            curr_hour = TimeUtils.getHourFromTimeStamp(currentTime);
            curr_min = TimeUtils.getMinuteFromTimeStamp(currentTime);

            int hour_diff = curr_hour - helpPost_hour;

            if(curr_hour > 1) return false;

            else{

                int minute_diff = curr_min - helpPost_min;

                if(hour_diff==1) minute_diff+=60;

                return minute_diff <= MINIMUM_TIME_DIFFERENCE;
            }
        }

        return false;

    }

}

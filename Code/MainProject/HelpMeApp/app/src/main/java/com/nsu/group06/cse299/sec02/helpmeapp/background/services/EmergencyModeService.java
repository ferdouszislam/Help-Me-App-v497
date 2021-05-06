package com.nsu.group06.cse299.sec02.helpmeapp.background.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.nsu.group06.cse299.sec02.helpmeapp.R;
import com.nsu.group06.cse299.sec02.helpmeapp.appScreens.activities.HomeActivity;
import com.nsu.group06.cse299.sec02.helpmeapp.sharedPreferences.AppSettingsSharedPref;
import com.nsu.group06.cse299.sec02.helpmeapp.utils.TimeUtils;

public class EmergencyModeService extends Service {

    private static final String TAG = "EMS-debug";
    private static final int FOREGROUND_SERVICE_ID = 184;
    private static final int FOREGROUND_NOTIFICATION_ID = 236;

    // minimum number of key presses to fire a distress call
    private static final int VOLUME_KEY_PRESS_THRESHOLD = 5;
    // minimum time(in milli-seconds) interval between volume key press
    private static final int VOLUME_KEY_PRESS_MINIMUM_TIME_INTERVAL = 1000;

    // current volume key press count
    private int mVolumeKeyPressCount = 0;
    // last volume key time(in milliseconds)
    private long mLastVolumeKeyPressTime = -1;

    // minimum time interval(in milli-seconds) for two help posts
    private static final long MINIMUM_TIME_INTERVAL_BETWEEN_HELP_POSTS = 5*60*1000; // 5 minutes
    // last help post time(in milli-seconds)
    private long mLastHelpPostTime = -1;

    // for listening to volume press
    private SettingsContentObserver mSettingsContentObserver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand: EmergencyModeService started!");
        
        try {

            startForeground(FOREGROUND_SERVICE_ID, getForegroundNotification());
        } catch (Exception e) {

            // older version of android don't have foreground service
            // show notification manually
            NotificationManagerCompat.from(this).notify(FOREGROUND_NOTIFICATION_ID, getForegroundNotification());
        }

        setEmergencyModeState();

        init();

        return START_STICKY;
    }

    /**
     * start service work inside this method
     */
    private void init() {

        setupVolumeListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        resetEmergencyModeState(); // laav hoy nai kore

        removeVolumeListener();
        removeForegroundNotification();
    }

    private void setEmergencyModeState() {
        AppSettingsSharedPref.build(this).setEmergencyModeState(true);
    }
    private void resetEmergencyModeState() {
        AppSettingsSharedPref.build(this).setEmergencyModeState(false);
    }

    private void onVolumeUpPress() {

        Log.d(TAG, "onVolumeUpPress: volume up");
        handleVolumeKeyPress();
    }

    private void onVolumeDownPress() {

        Log.d(TAG, "onVolumeDownPress: volume down");
        handleVolumeKeyPress();
    }

    private void handleVolumeKeyPress() {

        if(mLastVolumeKeyPressTime==-1 || !isVolumeKeyPressWithinMinimumTimeInterval(mLastVolumeKeyPressTime)) {
            mVolumeKeyPressCount = 0;
        }

        mVolumeKeyPressCount++;
        mLastVolumeKeyPressTime = TimeUtils.getCurrentTimeMillis();

        if(mVolumeKeyPressCount>=VOLUME_KEY_PRESS_THRESHOLD) {

            sendHelpPost();
            mVolumeKeyPressCount = 0;
            mLastVolumeKeyPressTime = -1;
        }
    }

    /**
     * start the process of sending the help post
     */
    private void sendHelpPost() {

        if(mLastHelpPostTime==-1 || !isHelpPostWithinMinimumTimeInterval(mLastHelpPostTime)) {
            Log.d(TAG, "sendHelpPost: sending help post...");
            mLastHelpPostTime = TimeUtils.getCurrentTimeMillis();
        }

        else Log.d(TAG, "sendHelpPost: minimum time interval not met, not sending help post.");
    }

    /**
     * create the foreground notification shown to user
     * @return set up notification object
     */
    private Notification getForegroundNotification() {

        String notificationChannelId = "emergency-mode-72";

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Emergency mode";
            String description = "Tapping volume key will send a distress call";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(notificationChannelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        String notificationTitle = "Emergency mode active";
        String notificationDescription = "Continuously tap volume button to trigger";

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelId)
                .setSmallIcon(R.drawable.ic_app_logo_dark_v2)
                .setContentTitle(notificationTitle)
                .setContentText(notificationDescription)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        Notification notification = builder.build();
        // make notification persistent
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    /**
     * removes the foreground notification
     */
    private void removeForegroundNotification() {

        NotificationManagerCompat.from(this).cancel(FOREGROUND_NOTIFICATION_ID);
    }

    private void setupVolumeListener() {

        mSettingsContentObserver = new SettingsContentObserver(new Handler());

        this.getContentResolver().registerContentObserver(android.provider.Settings.System.
                CONTENT_URI, true, mSettingsContentObserver);
    }

    private void removeVolumeListener() {
        if(mSettingsContentObserver !=null){
            getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        }
    }

    /**
     * is time difference between current and last volume key within minimum time interval
     * @param lastVolumeKeyPressTime last volume key press time in milli-seconds
     * @return true if last volume key press time is within minimum time interval, false otherwise
     */
    private boolean isVolumeKeyPressWithinMinimumTimeInterval(long lastVolumeKeyPressTime) {

        return (TimeUtils.getCurrentTimeMillis() - lastVolumeKeyPressTime) <= VOLUME_KEY_PRESS_MINIMUM_TIME_INTERVAL;
    }

    /**
     * is time difference between last help post and current help post enough
     * @param lastHelpPostTime in milliseconds
     * @return true if time interval is enough, otherwise false
     */
    private boolean isHelpPostWithinMinimumTimeInterval(long lastHelpPostTime) {

        return (TimeUtils.getCurrentTimeMillis() - lastHelpPostTime) < MINIMUM_TIME_INTERVAL_BETWEEN_HELP_POSTS;
    }

    /**
     * Inner-class for listening to volume key press
     * courtesy- <https://www.tutorialspoint.com/how-to-listen-volume-buttons-in-android-background-service>
     */
    public class SettingsContentObserver extends ContentObserver {

        private int previousVolume;

        SettingsContentObserver(Handler handler) {
            super(handler);
            AudioManager audio = (AudioManager) EmergencyModeService.this.getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            AudioManager audio = (AudioManager) EmergencyModeService.this.getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int delta = previousVolume - currentVolume;
            if (delta > 0) {
                // volume decreased, i.e volume down pressed
                onVolumeDownPress();
                previousVolume = currentVolume;
            }
            else if (delta < 0) {
                // volume increased, i.e volume up pressed
                onVolumeUpPress();
                previousVolume = currentVolume;
            }
        }
    }
}

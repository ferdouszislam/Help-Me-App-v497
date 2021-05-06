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

public class EmergencyModeService extends Service {

    private static final String TAG = "EMS-debug";
    private static final int FOREGROUND_SERVICE_ID = 184;
    private static final int FOREGROUND_NOTIFICATION_ID = 236;

    // for listening to volume press
    private SettingsContentObserver mSettingsContentObserver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {

            startForeground(FOREGROUND_SERVICE_ID, getForegroundNotification());
        } catch (Exception e) {

            // older version of android don't have foreground service
            // show notification manually
            NotificationManagerCompat.from(this).notify(FOREGROUND_NOTIFICATION_ID, getForegroundNotification());
        }

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

        removeVolumeListener();
        removeForegroundNotification();
    }

    private void onVolumeUpPress() {

        Log.d(TAG, "onVolumeUpPress: volume up");
    }

    private void onVolumeDownPress() {

        Log.d(TAG, "onVolumeDownPress: volume down");
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
        String notificationDescription = "Continuously tap volume down key to send out distress call";

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

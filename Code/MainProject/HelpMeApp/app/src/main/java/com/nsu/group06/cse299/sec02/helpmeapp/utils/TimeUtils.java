package com.nsu.group06.cse299.sec02.helpmeapp.utils;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for managing time stamps
 */
public abstract class TimeUtils {

    private static final String TAG = "TimeUtils-debug";

    /**
     * get current time
     * @return time string format- Date/Month/Year, hour:minutes:second
     * courtesy - <https://stackoverflow.com/questions/5175728/how-to-get-the-current-date-time-in-java>
     */
    public static String getCurrentTime() {

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        return dateFormat.format(date);
    }

    /**
     * get hour from timestamp
     * @param timeStamp format- dd/MM/yyy HH:mm:ss
     * @return formatted hour, 20 -> 10 PM
     */
    public static String getHourFromTimeStamp(String timeStamp) {

        String formattedHour = "NA";

        try {
            String[] date_time = timeStamp.split(" ");
            String time = date_time[0];
            String[] hour_min_sec = time.split(":");
            String hour = hour_min_sec[0];

            int hour_integer = Integer.parseInt(hour);

            if (hour_integer >= 12) {
                if (hour_integer > 12) hour_integer -= 12;
                formattedHour = hour_integer + "PM";
            } else formattedHour = hour_integer + "AM";
        } catch (Exception e) {

            Log.d(TAG, "getHourFromTimeStamp: error formatting timeStamp to hour-> "+ e.getMessage());
        }

        return  formattedHour;
    }
}

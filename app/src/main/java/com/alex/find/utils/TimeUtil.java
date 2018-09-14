package com.alex.find.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Alex on 03/24/2017.
 */

public class TimeUtil {
    private final static String TAG = "TimeUtil";

    public static String getCurrentTime() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        String time = formatter.format(currentTime);
        Log.i(TAG, "SmartHomeUtil-getCurrentTime()-time:" + time);
        return time;
    }

    public static String getCurrentDateTime() {
        return getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
    }

    public static String getTimeStampInFilenameFormat() {
        return getCurrentDateTime("yyyyMMddHHmmssSS");
    }

    public static String getCurrentDateTime(String format) {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        String time = formatter.format(currentTime);
        return time;
    }
}

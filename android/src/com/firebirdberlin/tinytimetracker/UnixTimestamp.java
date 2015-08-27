package com.firebirdberlin.tinytimetracker;

import android.util.Log;
import android.text.format.DateFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;


public class UnixTimestamp {
    private static String TAG = TinyTimeTracker.TAG + ".UnixTimestamp";
    private long timestamp;


    public UnixTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return toDateTimeString();
    }

    public String toDateTimeString(){
        return toString("yyyyMMddHHmm");
    }

    public String toDateString(){
        return toString("yyyyMMdd");
    }

    public String toWeekString(){
        return toYearString() + " w" + toString("ww");
    }

    public String toYearString(){
        return toString("yyyy");
    }

    public String toTimeString(){
        return toString("HHmm");
    }

    public String toString(String skeleton){
        Date date = new Date(timestamp);
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return new SimpleDateFormat(pattern).format(date);
    }

    public String durationAsMinutes(){
        long min = timestamp / 1000 / 60;

        return String.format("%dmin", min);
    }

    public String durationAsHours(){
        long seconds = timestamp / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;

        //return String.format("%02d:%02d:%02d", hours, minutes, sec);
        return String.format("%02d:%02d", hours, minutes);
    }

    public static UnixTimestamp startOfToday(){
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return new UnixTimestamp(cal.getTimeInMillis());
    }

}

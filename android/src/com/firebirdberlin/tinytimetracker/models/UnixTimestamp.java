package com.firebirdberlin.tinytimetracker.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class UnixTimestamp {
    private long timestamp;

    public UnixTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return toDateTimeString();
    }

    public String toDateTimeString() {
        Date date = new Date(timestamp);
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(date);
    }

    public String toDateString() {
        Date date = new Date(timestamp);
        DateFormat df = DateFormat.getDateInstance();
        return df.format(date);
    }

    public String toLongerDateString() {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE");
        return sdf.format(date) +", " + toDateString();
    }

    public String toWeekString() {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy 'w'ww");
        return sdf.format(date);
    }

    public String toWeekStringVerbose() {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("ww '(' yyyy ')'");
        return sdf.format(date);
    }

    public String toYearString() {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        return sdf.format(date);
    }

    public String toTimeString() {
        Date date = new Date(timestamp);
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        return df.format(date);
    }

    public String toTimeString(SimpleDateFormat dateFormat) {
        Date date = new Date(timestamp);
        return dateFormat.format(date);
    }

    public long toSeconds() {
        return timestamp / 1000L;
    }

    public String durationAsMinutes() {
        long min = timestamp / 1000 / 60;
        return String.format("%dmin", min);
    }

    public String durationAsHours() {
        long seconds = timestamp / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;
        hours = (hours < 0) ? -hours : hours;
        minutes = (minutes < 0) ? -minutes : minutes;
        return String.format("%02d:%02d", hours, minutes);
    }

    public static UnixTimestamp startOfToday() {
        Calendar cal = startOfTodayAsCalendar();
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static UnixTimestamp startOfMonth() {
        Calendar cal = startOfTodayAsCalendar();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static UnixTimestamp startOfWeek() {
        Calendar cal = startOfTodayAsCalendar();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static UnixTimestamp startOfLastMonth() {
        Calendar cal = startOfTodayAsCalendar();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -1);
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static UnixTimestamp todayThreeYearsAgo() {
        Calendar cal = startOfTodayAsCalendar();
        cal.add(Calendar.YEAR, -3);
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static UnixTimestamp todayOneYearAgo() {
        Calendar cal = startOfTodayAsCalendar();
        cal.add(Calendar.YEAR, -1);
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static UnixTimestamp todayThreeMonthsAgo() {
        Calendar cal = startOfTodayAsCalendar();
        cal.add(Calendar.MONTH, -3);
        return new UnixTimestamp(cal.getTimeInMillis());
    }

    public static Calendar startOfTodayAsCalendar() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal;
    }

    public Calendar toCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar;
    }

    public void set(int specifier, int value) {
        Calendar cal = this.toCalendar();
        cal.set(specifier, value);
        this.timestamp = cal.getTimeInMillis();
    }

    public void add(int specifier, int value) {
        Calendar cal = this.toCalendar();
        cal.add(specifier, value);
        this.timestamp = cal.getTimeInMillis();
    }

    public int getWeekOfYear() {
        Calendar cal = this.toCalendar();
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public int getHourOfDay() {
        Calendar cal = this.toCalendar();
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public int getMinute() {
        Calendar cal = this.toCalendar();
        return cal.get(Calendar.MINUTE);
    }
}

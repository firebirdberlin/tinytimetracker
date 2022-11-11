package com.firebirdberlin.tinytimetracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Utility {

    public static boolean isPackageInstalled(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();

        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return true;
    }

    public static SimpleDateFormat getTimeFormat(Context context) {
        if ( DateFormat.is24HourFormat(context) ) {
            return new SimpleDateFormat("HH:mm");
        }
        return new SimpleDateFormat("hh:mm a");
    }

    public static int getColor(Context context, int resid) {
        return ResourcesCompat.getColor(context.getResources(), resid, null);
    }

    public static long getFirstInstallTime(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1L;
        }
    }

    public static long getDaysSinceFirstInstall(Context context) {
        long firstInstall = getFirstInstallTime(context);
        if (firstInstall == -1L) return -1L;
        long msDiff = Calendar.getInstance().getTimeInMillis() - firstInstall;
        return TimeUnit.MILLISECONDS.toDays(msDiff);
    }

    public static long getDateAsLong(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.YEAR, year);
        return calendar.getTimeInMillis();
    }

    public static boolean isDebuggable(Context context){
        return ( 0 != ( context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || "sdk_gphone_x86".equals(Build.PRODUCT)
                || "sdk_gphone_x86_64".equals(Build.MODEL)
                || "sdk_gphone64_x86_64".equals(Build.MODEL);
    }

    public static boolean equal(String string, String other) {
        if (string == null) return false;
        if (other == null) return false;

        return string.equals(other);
    }

    public static int getRandomMaterialColor(Context context) {
        int[] colors = context.getResources().getIntArray(R.array.materialColors);
        return colors[new Random().nextInt(colors.length)];
    }

    public static int getContrastColor(int color) {
        double y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }
    static public PendingIntent getImmutableBroadcast(Context context, int requestCode, Intent intent, int flags) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags
        );
    }

    static public PendingIntent getImmutableService(Context context, int requestCode, Intent intent, int flags) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(
                context,
                requestCode,
                intent,
                flags
        );
    }

    static public PendingIntent getImmutableBroadcast(Context context, int requestCode, Intent intent) {
        //https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
        int flag = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flag
        );
    }

    static public PendingIntent getImmutableActivity(Context context, int requestCode, Intent intent) {
        //https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
        int flag = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flag
        );
    }
}



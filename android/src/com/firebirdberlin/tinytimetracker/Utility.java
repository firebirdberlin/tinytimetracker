package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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
                || "google_sdk".equals(Build.PRODUCT);
    }
}



package com.firebirdberlin.tinytimetracker;

import java.text.SimpleDateFormat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.text.format.DateFormat;
import android.support.v4.content.res.ResourcesCompat;

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
}


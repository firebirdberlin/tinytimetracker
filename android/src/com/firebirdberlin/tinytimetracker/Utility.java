package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

public class Utility{

    public static boolean isPackageInstalled(Context context, String targetPackage){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
}


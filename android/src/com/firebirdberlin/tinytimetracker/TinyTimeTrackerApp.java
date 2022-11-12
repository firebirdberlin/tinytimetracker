package com.firebirdberlin.tinytimetracker;

import android.app.Application;
import android.content.Context;

public class TinyTimeTrackerApp extends Application {
    private static TinyTimeTrackerApp instance;

    public static TinyTimeTrackerApp getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance;
        // or return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}

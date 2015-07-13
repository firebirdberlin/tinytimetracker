package com.firebirdberlin.tinytimetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
		TinyTimeTracker.startService(context);
    }
}

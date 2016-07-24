package com.firebirdberlin.tinytimetracker.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

import de.greenrobot.event.EventBus;

import com.firebirdberlin.tinytimetracker.events.OnLocationModeChanged;

public class LocationModeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        EventBus.getDefault().post(new OnLocationModeChanged());
    }
}

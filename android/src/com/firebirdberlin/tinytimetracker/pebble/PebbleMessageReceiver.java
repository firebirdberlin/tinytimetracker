package com.firebirdberlin.tinytimetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PebbleMessageReceiver extends BroadcastReceiver {
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("7100dca9-2d97-4ea9-a1a9-f27aae08d144");
    private static int TAG_WAITING_FOR_INPUT = 6;
    private static int VALUE_WAITING_FOR_INPUT = 1;
    private static int VALUE_NOT_WAITING_FOR_INPUT = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        dumpIntent(intent);
        Bundle bundle = intent.getExtras();
        Object uuid_object = bundle.get("uuid");

        if (! PEBBLE_APP_UUID.equals(uuid_object)){
            return;
        }

        int waiting_for_input = VALUE_NOT_WAITING_FOR_INPUT;
        String msg_data = bundle.getString("msg_data", "none");
        try {
            JSONArray json = new JSONArray(msg_data);
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                int key = jsonObject.getInt("key");
                int value = jsonObject.getInt("value");
                if (key == TAG_WAITING_FOR_INPUT) {
                    waiting_for_input = value;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (waiting_for_input == VALUE_WAITING_FOR_INPUT) {
            TinyTimeTracker.startService(context);
        }
    }

    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d(TinyTimeTracker.TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
        }
    }
}

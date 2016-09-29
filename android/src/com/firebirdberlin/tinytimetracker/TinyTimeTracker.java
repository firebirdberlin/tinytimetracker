package com.firebirdberlin.tinytimetracker;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONException;

import com.firebirdberlin.tinytimetracker.events.OnTrackerAdded;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.ui.CardFragment;
import com.firebirdberlin.tinytimetracker.ui.MainFragment;
import de.firebirdberlin.pageindicator.PageIndicator;
import com.firebirdberlin.tinytimetracker.ui.StatsFragment;

import com.android.vending.billing.IInAppBillingService;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.LinearLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import de.greenrobot.event.EventBus;

public class TinyTimeTracker extends AppCompatActivity {
    public static final String TAG = "TinyTimeTracker";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_CSV_DATA_EXPORT = "csv_data_export";
    public static final int REQUEST_CODE_PURCHASE_DONATION = 1001;
    public static final int REQUEST_CODE_PURCHASE_CSV_DATA_EXPORT = 1002;
    EventBus bus = EventBus.getDefault();
    private TrackerEntry currentTracker = null;
    private FloatingActionButton action_button_add = null;
    private LinearLayout pagerLayout = null;
    private CustomViewPager pager = null;
    private PageIndicator pageIndicator = null;
    public boolean purchased_donation = false;
    public boolean purchased_csv_data_export = false;

    IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.i(TAG, "In-app billing service disconnected !");
        }

        @Override
        public void onServiceConnected(ComponentName name,
                IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            Log.i(TAG, "In-app billing service connected !");
            getPurchases();
        }
    };

    private void getPurchases() {
        if (mService == null) {
            Log.e(TAG, "mService is not connected !");
            return;
        }

        Bundle ownedItems = null;
        try {
            ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException occurred !");
            return;
        }

        if (ownedItems == null) return;

        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            ArrayList<String> ownedSkus =
                ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String>  purchaseDataList =
                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String>  signatureList =
                ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
            String continuationToken =
                ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            Log.i(TAG, "List of purchased items (" + String.valueOf(purchaseDataList.size()) + "):");
            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);
                Log.i(TAG, "Item "  + sku + " was already purchased.");

                if (sku.equals(ITEM_DONATION)) {
                    purchased_donation = true;
                    invalidateOptionsMenu();
                }
                if (sku.equals(ITEM_CSV_DATA_EXPORT)) {
                    purchased_csv_data_export = true;
                }

                // do something with this purchase information
                // e.g. display the updated list of products owned by user
            }

            // if continuationToken != null, call getPurchases again
            // and pass in the token to retrieve more items
        }

    }

    public void purchaseIntent(String sku, int REQUEST_CODE) {
        if (mService == null) return;
        try {
            String developerPayload = "abcdefghijklmnopqrstuvwxyz";
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                    sku, "inapp",developerPayload);
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (RemoteException e1) {
            return;
        } catch (SendIntentException e2) {
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PURCHASE_DONATION ||
                requestCode == REQUEST_CODE_PURCHASE_CSV_DATA_EXPORT) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    if (sku.equals(ITEM_DONATION) ) {
                        purchased_donation = true;
                        invalidateOptionsMenu();
                    } else
                    if (sku.equals(ITEM_CSV_DATA_EXPORT) ) {
                        purchased_csv_data_export = true;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // bind the in-app billing service
        Intent serviceIntent =
            new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        // runtime permissions for the WifiService
        requestServicePermissions();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        action_button_add = (FloatingActionButton) findViewById(R.id.action_button_add);
        pagerLayout = (LinearLayout) findViewById(R.id.pager_layout);
        pager = (CustomViewPager) findViewById(R.id.pager);
        pageIndicator = (PageIndicator) findViewById(R.id.page_indicator);
        pageIndicator.setPageCount(3);
        pager.setAdapter(new MyPagerAdapter(this, getSupportFragmentManager()));
        pager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                pageIndicator.setCurrentPage(position);
            }
        });
        bus.register(this);
        enableBootReceiver(this);
        scheduleWiFiService(this);
        startService(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // unbind the in-app billing service
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        private TinyTimeTracker mainActivity = null;

        public MyPagerAdapter(TinyTimeTracker mainActivity, FragmentManager fm) {
            super(fm);
            this.mainActivity = mainActivity;
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
            case 0:
            default:
                return new MainFragment();
            case 1:
                return new CardFragment();
            case 2:
                return new StatsFragment(mainActivity);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        OnTrackerAdded event = bus.getStickyEvent(OnTrackerAdded.class);
        if(event != null) {
            pager.setCurrentItem(0);
        }

        LogDataSource datasource = new LogDataSource(this);
        List<TrackerEntry> trackers = datasource.getTrackers();
        datasource.close();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        }
        else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        MenuItem item_edit = menu.findItem(R.id.action_edit);
        MenuItem item_delete = menu.findItem(R.id.action_delete);
        MenuItem item_donate = menu.findItem(R.id.action_donate);
        MenuItem item_pebble_app_store = menu.findItem(R.id.action_pebble_app_store);
        item_edit.setVisible(currentTracker != null);
        item_delete.setVisible(currentTracker != null);
        item_donate.setVisible(mService != null && purchased_donation == false);

        boolean pebbleAppStoreIsInstalled =
            Utility.isPackageInstalled(this, "com.getpebble.android") ||
            Utility.isPackageInstalled(this, "com.getpebble.android.basalt");
        item_pebble_app_store.setVisible(pebbleAppStoreIsInstalled);

        if (currentTracker == null) {
            action_button_add.show();
            pagerLayout.setVisibility(View.GONE);
            pager.setPagingEnabled(false);
        } else {
            action_button_add.hide();
            pagerLayout.setVisibility(View.VISIBLE);
            pager.setPagingEnabled(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_edit:

            if (currentTracker != null) {
                AddTrackerActivity.open(this, currentTracker.id);
            }

            return true;
        case R.id.action_add:
            AddTrackerActivity.open(this);
            return true;
        case R.id.action_delete:
            confirmDeletion();
            return true;
        case R.id.action_settings:
            Settings.openSettings(this);
            return true;
        case R.id.action_pebble_app_store:
            openPebbleAppStore();
            return true;
        case R.id.action_recommend:
            recommendApp();
            return true;
        case R.id.action_donate:
            purchaseIntent(ITEM_DONATION, REQUEST_CODE_PURCHASE_DONATION);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void onAddTracker(View v) {
        AddTrackerActivity.open(this);
    }

    private void confirmDeletion() {
        if (currentTracker == null) {
            return;
        }

        final Context mContext = this;
        new AlertDialog.Builder(this)
        .setTitle(this.getResources().getString(R.string.confirm_delete)
                  + " '" + currentTracker.verbose_name + "'")
        .setMessage(this.getResources().getString(R.string.confirm_delete_question))
        .setIcon(R.drawable.ic_delete)
        .setNegativeButton(android.R.string.no, null)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                LogDataSource datasource = new LogDataSource(mContext);
                datasource.delete(currentTracker);
                datasource.close();
            }
        }).show();
    }

    private void openPebbleAppStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pebble://appstore/55a573a4ba679a9523000071"));
            startActivity(intent);
        }
        catch ( ActivityNotFoundException e) {
        }
    }

    private void recommendApp() {
     String body = "https://play.google.com/store/apps/details?id=com.firebirdberlin.tinytimetracker";
     String subject = getResources().getString(R.string.recommend_app_subject);
     String description = getResources().getString(R.string.recommend_app_desc);
     Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
     sharingIntent.setType("text/plain");
     sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
     sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
     startActivity(Intent.createChooser(sharingIntent, description));
    }

    public static boolean startService(Context context) {
        if (hasPermission(context, Manifest.permission.WAKE_LOCK)
                && hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            Intent intent = new Intent(context, WiFiService.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(intent);
            return true;
        }
        return false;
    }

    private void requestServicePermissions() {
        checkAndRequestPermission(this, Manifest.permission.WAKE_LOCK, 1);
        checkAndRequestPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED, 1);

        try {
            long installed = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime;
            if (installed < getDateAsLong(2016, 6, 1)) {
                checkAndRequestPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION, 1);
            }
        }
        catch (NameNotFoundException e ) {
        }
    }

    public long getDateAsLong(int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.YEAR, year);
            return calendar.getTimeInMillis();
    }

    public static void checkAndRequestPermission(Activity activity, String permission,
                                                 int requestCode) {
        if (! hasPermission((Context) activity, permission) ) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        }
    }

    public static boolean hasPermission(Context context, String permission) {
        return (ContextCompat.checkSelfPermission(context, permission)
                 == PackageManager.PERMISSION_GRANTED);
    }

    public static void scheduleWiFiService(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000, 120000, sender);
    }

    public void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                                      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                      PackageManager.DONT_KILL_APP);
    }

    public void disableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                      PackageManager.DONT_KILL_APP);
    }

    @SuppressLint("NewApi")
    public static boolean isAirplaneModeOn(Context context) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return Global.getInt(context.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) != 0;
        }
        else {
            return System.getInt(context.getContentResolver(), System.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public void onEvent(OnTrackerSelected event) {
        currentTracker = event.newTracker;
        invalidateOptionsMenu();
        Log.d(TAG, "currentTracker: " + currentTracker.toString());
    }

    public void onEvent(OnTrackerDeleted event) {
        currentTracker = null;
        invalidateOptionsMenu();
        pager.setCurrentItem(0);
        Log.d(TAG, "currentTracker: null");
    }
}

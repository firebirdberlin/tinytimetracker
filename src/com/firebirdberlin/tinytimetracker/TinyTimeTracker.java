package com.firebirdberlin.tinytimetracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


import java.lang.Runnable;

public class TinyTimeTracker extends Activity {
    public static final String TAG = "TinyTimeTracker";
    private Handler viewHandler = new Handler();
    private View timeView = null;
    private int workingHoursInSeconds = 8 * 3600;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        timeView = new MyView(this);
        setContentView(timeView);
		
        enableBootReceiver(this);
        scheduleWiFiService(this);
        startService(this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        workingHoursInSeconds = (int) (Settings.getWorkingHours(this) * 3600.f);
        viewHandler.post(updateView);
    }

    @Override
    public void onPause() {
        super.onPause();
        viewHandler.removeCallbacks(updateView);
    }

    Runnable updateView = new Runnable(){
        @Override
        public void run(){
            timeView.invalidate();
            viewHandler.postDelayed(updateView, 20000);
        }
    };

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_settings:
				Settings.openSettings(this);
				return true;
			case R.id.action_donate:
				openDonationPage();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

    private void openDonationPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5PX9XVHHE6XP8"));
        startActivity(browserIntent); 
    }   

    public class MyView extends View {
         private Context mContext;

         public MyView(Context context) {
             super(context);
             mContext = context;
         }

         @Override
         protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
            Long seconds_today = settings.getLong("seconds_today", 0L);
            int angle = 360 * seconds_today.intValue() / workingHoursInSeconds; 

            int x = getWidth();
            int y = getHeight();
            int radius = 8 * x / 20;
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.TRANSPARENT);
            paint.setStrokeWidth(4);
            paint.setStrokeCap(Paint.Cap.ROUND);

            final RectF rect = new RectF();
            rect.set(x/2 - radius, y/2 - radius, x/2 + radius, y/2 + radius); 
            paint.setColor(Color.parseColor("#AA6AB4D7"));
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawArc(rect, -90, angle, true, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.parseColor("#AA33A6DE"));
            canvas.drawArc(rect, -90, 360, true, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(150);
            paint.setStrokeWidth(1);
            Rect bounds = new Rect();
            String text = WiFiService.format_seconds(seconds_today.intValue());
            paint.getTextBounds(text, 0, text.length(), bounds);
            int height = bounds.height();
            int width = bounds.width();
            canvas.drawText(text, (x - width)/2, (y + height)/2, paint);

        }
    }

    public static void startService(Context context){
        Intent intent = new Intent(context, WiFiService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent);
    }


    public static void scheduleWiFiService(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
                        
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000, 120000, sender);
    }
                        

    public void enableBootReceiver(Context context){
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }


    public void disableBootReceiver(Context context){
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }



}

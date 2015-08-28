package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;
import android.util.Pair;

public class MainView extends View {
     private TinyTimeTracker mContext;
     private int workingHoursInSeconds = 8 * 3600;

     public MainView(Context context) {
         super(context);
         mContext = (TinyTimeTracker) context;
     }

     public MainView(Context context, AttributeSet attrs) {
         super(context, attrs);
         mContext = (TinyTimeTracker) context;
     }

     @Override
     protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        UnixTimestamp today = UnixTimestamp.startOfToday();
        LogDataSource datasource = mContext.getDataSource();
        TrackerEntry tracker = mContext.getCurrentTracker();
        if (tracker == null) {
            return;
        }
        UnixTimestamp duration = datasource.getTotalDurationSince(today.getTimestamp(), tracker.getID());

        Long seconds_today = new Long(duration.getTimestamp() / 1000L);

        workingHoursInSeconds = (int) (Settings.getWorkingHours(mContext) * 3600.f);
        int angle = 360 * seconds_today.intValue() / workingHoursInSeconds;

        int x = getWidth();
        int y = getHeight();
        int radius = x < y ? 8 * x / 20 : 8 * y / 20;
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
        String text = duration.durationAsHours();
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int height = bounds.height();
        int width = bounds.width();
        canvas.drawText(text, (x - width)/2, (y + height)/2, paint);

    }
}
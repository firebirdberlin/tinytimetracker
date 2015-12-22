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
import android.util.Pair;
import android.view.View;
import de.greenrobot.event.EventBus;
import java.util.List;

public class MainView extends View {
    private Context mContext;
    private int workingHoursInSeconds = 8 * 3600;
    EventBus bus = EventBus.getDefault();
    TrackerEntry currentTracker = null;

    public MainView(Context context) {
        super(context);
        bus.register(this);
        mContext = context;
    }

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bus.register(this);
        mContext = (TinyTimeTracker) context;
    }

    public void onEvent(OnWifiUpdateCompleted event) {
        if (event.success && currentTracker.equals(event.tracker)) {
            invalidate();
        }
    }

    public void onEvent(OnTrackerSelected event) {
        currentTracker = event.newTracker;
        invalidate();
    }

    public void onEvent(OnTrackerChanged event) {
        if (currentTracker == null || event == null || event.tracker == null) {
            return;
        }

        if (currentTracker.id == event.tracker.id) {
            currentTracker = event.tracker;
            invalidate();
        }
    }

    public void onEvent(OnTrackerDeleted event) {
        currentTracker = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        UnixTimestamp today = UnixTimestamp.startOfToday();
        LogDataSource datasource = new LogDataSource(mContext);

        if (currentTracker == null) {
            return;
        }

        UnixTimestamp duration = datasource.getTotalDurationSince(today.getTimestamp(), currentTracker.id);
        Long seconds_today = new Long(duration.getTimestamp() / 1000L);
        workingHoursInSeconds = (int) (currentTracker.working_hours * 3600.f);
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
        rect.set(x / 2 - radius, y / 2 - radius, x / 2 + radius, y / 2 + radius);
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
        canvas.drawText(text, (x - width) / 2, (y + height) / 2, paint);
    }
}

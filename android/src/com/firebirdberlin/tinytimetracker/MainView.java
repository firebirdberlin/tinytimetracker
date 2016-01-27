package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
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
        if (currentTracker == null) {
            return;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        UnixTimestamp today = UnixTimestamp.startOfToday();
        UnixTimestamp todayThreeYearsAgo = UnixTimestamp.todayThreeYearsAgo();
        LogDataSource datasource = new LogDataSource(mContext);
        int highlightColor = getSystemColor(android.R.attr.colorActivatedHighlight);
        int textColor = getSystemColor(android.R.attr.textColor);

        UnixTimestamp duration = datasource.getTotalDurationSince(today.getTimestamp(), currentTracker.id);
        Long seconds_today = new Long(duration.getTimestamp() / 1000L);
        workingHoursInSeconds = (int) (currentTracker.working_hours * 3600.f);

        Pair<Long, Long> totalDurationPair = datasource.getTotalDurationPairSince(todayThreeYearsAgo.getTimestamp(), currentTracker.id);

        int angle = 360;
        if (workingHoursInSeconds > 0) {
            angle = 360 * seconds_today.intValue() / workingHoursInSeconds;
        }

        int x = getWidth();
        int y = getHeight();
        int radius = x < y ? 8 * x / 20 : 8 * y / 20;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(4);
        paint.setStrokeCap(Paint.Cap.ROUND);
        final RectF rect = new RectF();
        rect.set(x / 2 - radius, y / 2 - radius, x / 2 + radius, y / 2 + radius);


        {// draw the main circle
            paint.setColor(highlightColor);
            paint.setAlpha(100);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawArc(rect, -90, angle, true, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(highlightColor);
            paint.setAlpha(255);
            canvas.drawArc(rect, -90, 360, true, paint);

            paint.setColor(textColor);
            paint.setAlpha(255);
            paint.setTextSize(150);
            paint.setStrokeWidth(1);

            String text = duration.durationAsHours();
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);
            int height = bounds.height();
            int width = bounds.width();
            canvas.drawText(text, (x - width) / 2, (y + height) / 2, paint);
        }

        { // draw mean daily duration
            long meanDurationMillis = currentTracker.getMeanDurationMillis(totalDurationPair.first, totalDurationPair.second);

            UnixTimestamp meanDuration = new UnixTimestamp(meanDurationMillis);

            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextSize(52);
            String text = meanDuration.durationAsHours();
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);
            canvas.drawText(text, 25, (y - bounds.height()), paint);
        }

        // draw overtime
        if ( workingHoursInSeconds > 0 ) {
            Long overTimeMillis = currentTracker.getOvertimeMillis(totalDurationPair.first, totalDurationPair.second);
            UnixTimestamp overtime = new UnixTimestamp(overTimeMillis);

            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextSize(52);
            String sign = (overTimeMillis < 0 ) ? "- ": "+ ";
            String text = sign + overtime.durationAsHours();
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);
            canvas.drawText(text, (x - bounds.width() - 25), (y - bounds.height()), paint);
        }

    }
    private int getSystemColor(int colorID) {
        Resources.Theme theme = mContext.getTheme();
        TypedValue styleID = new TypedValue();
        if (theme.resolveAttribute(colorID, styleID, true)) {
            return styleID.data;
        }
        return Color.parseColor("#FFFFFF");
    }
}

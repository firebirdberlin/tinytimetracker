package com.firebirdberlin.tinytimetracker.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerChanged;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;


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
        if ( currentTracker == null ) return;
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

    public void onEvent(OnLogEntryDeleted event) {
        invalidate();
    }

    public void onEvent(OnLogEntryChanged event) {
        if ( currentTracker != null && currentTracker.id == event.entry.tracker_id ) {
            invalidate();
        }
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

        final RectF rect = new RectF();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);

        {// draw the page indicator
            int radius = 6;
            paint.setColor(textColor);
            paint.setAlpha(255);

            rect.set(x / 2 - 3 * radius, y - 5 * radius, x / 2 - radius, y - 3 * radius);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(3);
            canvas.drawArc(rect, -90, 360, true, paint);

            rect.set(x / 2 + radius, y - 5 * radius, x / 2 + 3 *  radius, y - 3 * radius);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            canvas.drawArc(rect, -90, 360, true, paint);
        }


        {// draw the main circle
            int radius = x < y ? 8 * x / 20 : 8 * y / 20;
            rect.set(x / 2 - radius, y / 2 - radius, x / 2 + radius, y / 2 + radius);
            paint.setColor(highlightColor);
            paint.setAlpha(100);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(4);
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

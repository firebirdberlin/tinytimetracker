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
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;


public class MainView extends View {
    private Context mContext;
    private int highlightColor;
    private int textColor;
    private boolean activated = false;
    private int workingHoursInSeconds = 8 * 3600;
    EventBus bus = EventBus.getDefault();

    public MainView(Context context) {
        super(context);
        bus.register(this);
        mContext = context;
        init();
    }

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bus.register(this);
        mContext = (TinyTimeTracker) context;

        init();
    }

    public void onEvent(OnWifiUpdateCompleted event) {
        if ( TinyTimeTracker.currentTracker == null ) return;
        if (event.success && TinyTimeTracker.currentTracker.equals(event.tracker)) {
            invalidate();
        }
    }

    private void init(){
        highlightColor = getSystemColor(android.R.attr.colorActivatedHighlight);
        textColor = getSystemColor(android.R.attr.textColor);
    }

    public void onEvent(OnTrackerSelected event) {
        invalidate();
    }

    public void onEvent(OnTrackerChanged event) {
        if (TinyTimeTracker.currentTracker == null || event == null || event.tracker == null) {
            return;
        }

        if (TinyTimeTracker.currentTracker.id == event.tracker.id) {
            invalidate();
        }
    }

    public void onEvent(OnTrackerDeleted event) {
        invalidate();
    }

    public void onEvent(OnLogEntryDeleted event) {
        invalidate();
    }

    public void onEvent(OnLogEntryChanged event) {
        if ( TinyTimeTracker.currentTracker != null &&
                TinyTimeTracker.currentTracker.id == event.entry.tracker_id ) {
            invalidate();
        }
    }

    public void setActivated() {
        activated = true;
        setHighlightColor("#4caf50");
    }

    public void setDeactivated() {
        activated = false;
        setHighlightColor(getSystemColor(android.R.attr.colorActivatedHighlight));
    }

    private void setHighlightColor(String color) {
        highlightColor = Color.parseColor(color);
        invalidate();
    }

    private void setHighlightColor(int color) {
        highlightColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (TinyTimeTracker.currentTracker == null) {
            return;
        }

        UnixTimestamp today = UnixTimestamp.startOfToday();
        LogDataSource datasource = new LogDataSource(mContext);

        UnixTimestamp duration = datasource.getTotalDurationSince(today.getTimestamp(), TinyTimeTracker.currentTracker.id);
        Long seconds_today = new Long(duration.getTimestamp() / 1000L);
        workingHoursInSeconds = (int) (TinyTimeTracker.currentTracker.working_hours * 3600.f);

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
        paint.setTextSize(dpToPx(60));
        paint.setStrokeWidth(1);

        String text = duration.durationAsHours();
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int height = bounds.height();
        int width = bounds.width();
        canvas.drawText(text, (x - width) / 2, (y + height) / 2, paint);
    }

    private int dpToPx(int value) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
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

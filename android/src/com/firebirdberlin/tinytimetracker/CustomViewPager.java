package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.util.AttributeSet;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager {

    private boolean enabled;

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    public CustomViewPager(Context context) {
        super(context);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public boolean isPagingEnabled() {
        return this.enabled;
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void swipeNext() {
        if (!this.enabled) {
            return;
        }
        int currentItem = getCurrentItem();
        if ( currentItem < getAdapter().getCount() - 1) {
            setCurrentItem(currentItem + 1);
        }
    }
    public void swipePrev() {
        if (!this.enabled) {
            return;
        }
        int currentItem = getCurrentItem();
        if ( currentItem > 0 ) {
            setCurrentItem(currentItem - 1);
        }
    }

}

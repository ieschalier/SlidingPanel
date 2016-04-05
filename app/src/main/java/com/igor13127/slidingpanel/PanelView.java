package com.igor13127.slidingpanel;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by igoreschalier on 05/04/16.
 */
public class PanelView extends LinearLayout {

    private OnSwipeTouchListener listener;

    public PanelView(Context context) {
        super(context);
    }

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PanelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

         listener = new OnSwipeTouchListener(context){
            @Override
            public void onSwipeBottom() {
                Log.d("mDragView", "OnSwipeTouchListener Down");
            }

            @Override
            public void onSwipeLeft() {
                Log.d("mDragView", "OnSwipeTouchListener left");
            }

            @Override
            public void onSwipeTop() {
                Log.d("mDragView", "OnSwipeTouchListener Up");
            }

            @Override
            public void onSwipeRight() {
                Log.d("mDragView", "OnSwipeTouchListener Right");
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PanelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("PanelView", "onTouchEvent action : " + event.getAction());
        return listener.getGestureDetector().onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("PanelView", "onInterceptTouchEvent action : " +ev.getAction());
        return listener.getGestureDetector().onTouchEvent(ev);
    }*/

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev);
    }
}

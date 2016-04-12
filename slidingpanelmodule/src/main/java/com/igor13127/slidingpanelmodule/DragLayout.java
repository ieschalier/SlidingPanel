package com.igor13127.slidingpanelmodule;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by igoreschalier on 04/04/16.
 */
public class DragLayout extends RelativeLayout {

    private DragLayout self = this;

    /**
     * A view is not currently being dragged or animating as a result of a fling/snap.
     */
    public static final int STATE_IDLE = 0;

    /**
     * A view is currently being dragged. The position is currently changing as a result
     * of user input or simulated user input.
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * A view is currently settling into place as a result of a fling or
     * predefined non-interactive motion.
     */
    public static final int STATE_SETTLING = 2;

    // Current drag state; idle, dragging or settling
    private int mDragState;

    private final ViewDragHelper mDragHelper;
    private DragHelperCallback dragHelperCallback;
    private PanelView mDragView;
    private View mScrollableView;
    private ScrollableViewHelper mScrollableViewHelper = new ScrollableViewHelper();

    private View mBackView;

    private float mPrevMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;

    private int mDragRange;
    private int mTop;
    private float mDragOffset;
    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    private float mSlideOffset;
    private boolean mIsUnableToDrag;
    private boolean mIsScrollableViewHandlingTouch = false;
    /**
     * True if the collapsed panel should be dragged up. always true because can have only one gravity (BOTTOM to TOP)
     */
    private boolean mIsSlidingUp = true;
    /**
     * Flag indicating that sliding feature is enabled\disabled
     */
    private boolean mIsTouchEnabled;

    private Context mContext;
    private boolean mIsPreviousMove;

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        dragHelperCallback = new DragHelperCallback();

        mDragHelper = ViewDragHelper.create(this, 0.5f, dragHelperCallback);

        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);

        mDragView = new PanelView(getContext(), attrs, defStyle);

        mSlideOffset = 0.0f;

        mIsTouchEnabled = true;
    }

    boolean smoothSlideTo(float slideOffset) {
        mSlideOffset = 1.0f-slideOffset;
        final int topBound = getPaddingTop();
        int y = (int) (topBound + slideOffset * mDragRange);

        if (mDragHelper.smoothSlideViewTo(mDragView, 0, y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    public void openPanel(){
        smoothSlideTo(1f);
        Log.d("DragLayout", "open panel action");
    }

    public void closePanel(){
        smoothSlideTo(0f);
        Log.d("DragLayout", "close panel action");
    }

    public boolean panelIsOpen(){
        if (mDragView.getX() == mTop){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (!isEnabled() || !isTouchEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.abort();
            return super.dispatchTouchEvent(ev);
        }

        final float y = ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            mIsScrollableViewHandlingTouch = false;
            mPrevMotionY = y;
            mIsPreviousMove = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dy = y - mPrevMotionY;
            // If the scroll view isn't under the touch, pass the
            // event along to the dragView.
            if (!isViewUnder(mScrollableView, (int) mInitialMotionX, (int) mInitialMotionY)) {
                return super.dispatchTouchEvent(ev);
            }

            // Which direction (up or down) is the drag moving?
            if (dy * (mIsSlidingUp ? 1 : -1) > 0) { // Collapsing
                // Is the child less than fully scrolled?
                // Then let the child handle it.
                if (mScrollableViewHelper.getScrollableViewScrollPosition(mScrollableView, mIsSlidingUp) > 0) {
                    mIsScrollableViewHandlingTouch = true;
                    return super.dispatchTouchEvent(ev);
                }

                // Was the child handling the touch previously?
                // Then we need to rejigger things so that the
                // drag panel gets a proper down event.
                if (mIsScrollableViewHandlingTouch) {
                    // Send an 'UP' event to the child.
                    MotionEvent up = MotionEvent.obtain(ev);
                    up.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(up);
                    up.recycle();

                    // Send a 'DOWN' event to the panel. (We'll cheat
                    // and hijack this one)
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = false;
                return this.onTouchEvent(ev);
            } else if (dy * (mIsSlidingUp ? 1 : -1) < 0) { // Expanding
                // Is the panel less than fully expanded?
                // Then we'll handle the drag here.
                if (mSlideOffset == 0.0f) {
                    mIsScrollableViewHandlingTouch = false;
                    return this.onTouchEvent(ev);
                }

                // Was the panel handling the touch previously?
                // Then we need to rejigger things so that the
                // child gets a proper down event.
                if (!mIsScrollableViewHandlingTouch && mDragHelper.isDragging()) {
                    mDragHelper.cancel();
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = true;
                return super.dispatchTouchEvent(ev);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // If the scrollable view was handling the touch and we receive an up
            // we want to clear any previous dragging state so we don't intercept a touch stream accidentally
            if (mIsScrollableViewHandlingTouch) {
                mDragHelper.setDragState(ViewDragHelper.STATE_IDLE);
            }

            int middleBackView = (mBackView.getHeight() - getPaddingTop())/2;
            if (mDragView.getY() > middleBackView){
                boolean passToChild = true;
                if (mSlideOffset == 1.0f){
                    passToChild = false;
                }
                openPanel();
                if (!passToChild) {
                    return false;
                }
            }else{
                boolean passToChild = true;
                if (mSlideOffset == 0.0f){
                    passToChild = false;
                }
                closePanel();
                if (!passToChild) {
                    return false;
                }
            }
            mIsPreviousMove = false;
        }
        mIsPreviousMove = false;

        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If the scrollable view is handling touch, never intercept
        if (mIsScrollableViewHandlingTouch ||!isTouchEnabled()) {
            mDragHelper.abort();
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        final float adx = Math.abs(x - mInitialMotionX);
        final float ady = Math.abs(y - mInitialMotionY);
        final int dragSlop = mDragHelper.getTouchSlop();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if ((ady > dragSlop && adx > ady) || !isViewUnder(mDragView, (int) mInitialMotionX, (int) mInitialMotionY)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If the dragView is still dragging when we get here, we need to call processTouchEvent
                // so that the view is settled
                // Added to make scrollable views work (tokudu)
                if (mDragHelper.isDragging()) {
                    mDragHelper.processTouchEvent(ev);
                    return true;
                }
                // Check if this was a click on the faded part of the screen, and fire off the listener if there is one.
                if (ady <= dragSlop
                        && adx <= dragSlop
                        && mSlideOffset > 0 && !isViewUnder(mDragView, (int) mInitialMotionX, (int) mInitialMotionY)) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                    //mFadeOnClickListener.onClick(this);
                    return true;
                }
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled() ||!isTouchEnabled()) {
            return super.onTouchEvent(ev);
        }
        try {
            mDragHelper.processTouchEvent(ev);
            return true;
        } catch (Exception ex) {
            // Ignore the pointer out of range exception
            return false;
        }
    }

    private boolean isViewUnder(View view, int x, int y) {
        if (view == null) return false;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float distanceInPx = (float) Math.sqrt(dx * dx + dy * dy);
        return pxToDp(distanceInPx);
    }

    private float pxToDp(float px) {
        return px /  mContext.getResources().getDisplayMetrics().density;
    }

    @Override
    protected boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params) {
        return super.addViewInLayout(child, index, params);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildAt(0) != null){
            mBackView = getChildAt(0);
        }
        if (getChildAt(1) != null){
            if (!getChildAt(1).getClass().equals(PanelView.class)){
                View view = getChildAt(1);
                removeView(view);
                addView(mDragView);
                mDragView.addView(view);
            }
        }

        mBackView.post(new Runnable() {
            @Override
            public void run() {
                openPanel();
            }
        });
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mIsUnableToDrag) {
                return false;
            }
            return child == mDragView;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mTop = top;

            mDragOffset = (float) top / mDragRange;

            requestLayout();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top = getPaddingTop();
            Log.d("DragLayout", "onViewReleased");
            if (yvel > 0 || (yvel == 0 && mDragOffset > 0.5f)) {
                top += mDragRange;
            }

            Log.d("DragLayout", "mDragOffset value : " + mDragOffset);

            if (mDragOffset > 0.5f){
                openPanel();
            }else{
                closePanel();
            }
            mDragHelper.settleCapturedViewAt(0, top);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            final int bottomBound = self.getHeight() - child.getHeight() + mBackView.getHeight();

            return Math.min(Math.max(top, topBound), bottomBound);
        }

    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private boolean isViewHit(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mDragRange = mBackView.getMeasuredHeight();

        mDragView.layout(
                0,
                mTop,
                r,
                mTop + mDragView.getMeasuredHeight());

        mBackView.layout(
                0,
                0,
                r,
                mBackView.getMeasuredHeight());
    }

    /**
     * Set the scrollable child of the sliding layout. If set, scrolling will be transfered between
     * the panel and the view when necessary
     *
     * @param scrollableView The scrollable view
     */
    public void setScrollableView(View scrollableView) {
        mScrollableView = scrollableView;
    }

    /**
     * Sets the current scrollable view helper. See ScrollableViewHelper description for details.
     * @param helper
     */
    public void setScrollableViewHelper(ScrollableViewHelper helper) {
        mScrollableViewHelper = helper;
    }

    /**
     * Set sliding enabled flag
     *
     * @param enabled flag value
     */
    public void setTouchEnabled(boolean enabled) {
        mIsTouchEnabled = enabled;
    }

    public boolean isTouchEnabled() {
        return mIsTouchEnabled && mDragView != null;
    }
}
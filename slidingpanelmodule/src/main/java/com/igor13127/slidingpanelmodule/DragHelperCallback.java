package com.igor13127.slidingpanelmodule;

import android.support.v4.widget.ViewDragHelper;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by igoreschalier on 04/04/16.
 */
public abstract class DragHelperCallback extends ViewDragHelper.Callback {


    private int mDragRange;
    private int mTop;
    private float mDragOffset;

    abstract int getPaddingLeft();

    abstract int getPaddingTop();

    abstract int getHeight();

    abstract int getBottomBound();

    abstract int getWidth();

    abstract View getDragView();

    abstract RelativeLayout getParentLayout();

    /*@Override
    public int clampViewPositionHorizontal(View child, int left, int dx) {
        Log.d("DragLayout", "clampViewPositionHorizontal " + left + "," + dx);

        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - child.getWidth();

        return Math.min(Math.max(left, leftBound), rightBound);
    }*/

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
        mTop = top;

        mDragOffset = (float) top / mDragRange;

        getParentLayout().requestLayout();
    }

    @Override
    public boolean tryCaptureView(View child, int pointerId) {
        return child == getDragView();
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - child.getHeight() + getBottomBound();

        return Math.min(Math.max(top, topBound), bottomBound);
    }



    public boolean dragViewIsAtTop(){
        if (getDragView().getY() == getPaddingTop()){
            return true;
        }else{
            return false;
        }
    }

    public int getmDragRange() {
        return mDragRange;
    }

    public int getmTop() {
        return mTop;
    }

    public float getmDragOffset() {
        return mDragOffset;
    }

    private boolean isViewHit(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        getParentLayout().getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    /*@Override
    public void onEdgeTouched(int edgeFlags, int pointerId) {
        super.onEdgeTouched(edgeFlags, pointerId);
        Toast.makeText(getContext(), "edgeTouched", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEdgeDragStarted(int edgeFlags, int pointerId) {
        mDragHelper.captureChildView(mDragView2, pointerId);
    }*/
}

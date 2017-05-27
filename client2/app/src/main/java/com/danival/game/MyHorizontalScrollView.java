package com.danival.game;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class MyHorizontalScrollView extends HorizontalScrollView {
    private static final int SWIPE_MIN_DISTANCE = 5;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private GestureDetector mGestureDetector;
    private int mActiveCol = 0;
    private int mColWidth;
    private Context mContext;

    public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public MyHorizontalScrollView(Context context) {
        super(context);
        mContext = context;
    }
    public void setUp(final int colWidth){
        mColWidth = colWidth;
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //If the user swipes
                if (mGestureDetector.onTouchEvent(event)) {
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ){
                    int scrollX = getScrollX();
                    mActiveCol = ((scrollX + (mColWidth/2))/mColWidth);
                    smoothScrollTo(mActiveCol*mColWidth, 0);
                    return true;
                }
                else{
                    return false;
                }
            }
        });
        mGestureDetector = new GestureDetector(mContext, new MyGestureDetector());
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        mActiveCol = ((x + (mColWidth/2))/mColWidth);
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                int MAXIMUM_FLING_VELOCITY = ViewConfiguration.get(mContext).getScaledMaximumFlingVelocity();
                int deltaCol = Math.round(Math.abs(velocityX)/MAXIMUM_FLING_VELOCITY * 16);
                int endCol = 0;
                //right to left
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    endCol = (mActiveCol + deltaCol) > 16 ? 16 : (mActiveCol + deltaCol);
                }
                //left to right
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    endCol = (mActiveCol - deltaCol) < 0 ? 0 : (mActiveCol - deltaCol);
                }
                /*
                ObjectAnimator animator = ObjectAnimator.ofInt(MyHorizontalScrollView.this, "scrollX", endCol*mColWidth);
                animator.setDuration(800);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.start();
                */
                smoothScrollTo(endCol*mColWidth, 0);
                return true;
            } catch (Exception e) {
                //Log.e("Fling", "There was an error processing the Fling event:" + e.getMessage());
            }

            return false;
        }
    }
}
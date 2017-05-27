package com.danival.game;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class Bomb {
    protected MainActivity main;
    protected boolean available;
    protected ImageView icon;

    public Bomb(MainActivity context) {
        main = context;
        available = true;
        icon = new ImageView(main);
        int idBomb = main.getResources().getIdentifier("com.danival.game:drawable/bomb", null, null);
        icon.setImageResource(idBomb);
        int bombSize = Math.round(40*main.metrics.density);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(bombSize, bombSize);
        icon.setLayoutParams(layoutParams);
        icon.setScaleX(0.01f);
        icon.setScaleY(0.01f);
        icon.setVisibility(View.INVISIBLE);
        main.grid.addView(icon);

    }

    public void launch(int xFrom, int yFrom, int xTo, int yTo) {
        //if (icon.getParent() != null) { return; } // ERRO: java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
        //main.grid.addView(icon);
        icon.setVisibility(View.VISIBLE);
        icon.bringToFront();
        icon.setX(xFrom);
        icon.setY(yFrom);
        //icon.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        ObjectAnimator animX = ObjectAnimator.ofFloat(icon, "x", xTo);
        animX.setDuration(500);
        ObjectAnimator animY = ObjectAnimator.ofFloat(icon, "y", yTo);
        animY.setDuration(500);
        ObjectAnimator jumpScaleX = ObjectAnimator.ofFloat(icon, "scaleX", 1f);
        jumpScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        jumpScaleX.setDuration(250);
        jumpScaleX.setRepeatCount(1);
        jumpScaleX.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator jumpScaleY = ObjectAnimator.ofFloat(icon, "scaleY", 1f);
        jumpScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        jumpScaleY.setDuration(250);
        jumpScaleY.setRepeatCount(1);
        jumpScaleY.setRepeatMode(ValueAnimator.REVERSE);
        AnimatorSet set = new AnimatorSet();
        animX.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                //bomb.setLayerType(View.LAYER_TYPE_NONE, null);
                //main.grid.removeView(icon);
                icon.setVisibility(View.INVISIBLE);
                available = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {}
        });
        //set.playTogether(animX, animY, jumpScaleX, jumpScaleY);
        set.playTogether(animX, animY, jumpScaleX, jumpScaleY);
        set.start();
    }

    public void clear() {
        main.grid.removeView(icon);
    }

}

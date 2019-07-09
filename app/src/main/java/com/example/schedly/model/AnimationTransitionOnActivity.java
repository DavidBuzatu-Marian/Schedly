package com.example.schedly.model;

import android.animation.Animator;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.schedly.R;

import io.opencensus.resource.Resource;

public class AnimationTransitionOnActivity {

    private View mView;

    public AnimationTransitionOnActivity(View view, int X, int Y) {
        mView = view;
        circularRevealCard(mView, X, Y);
    }

    // The expansion points is where the animation starts
    private void circularRevealCard(final View view, int expansionPointX, int expansionPointY) {

        // Radius is whichever dimension is the longest on our screen
        float finalRadius = Math.max(view.getWidth(), view.getHeight());

        // Start circular animation
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(view, expansionPointX, expansionPointY, 0, finalRadius * 1.1f);
        circularReveal.setDuration(600);

        // make the view visible and start the animation
        view.setVisibility(View.VISIBLE);
        circularReveal.start();

        circularReveal.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}

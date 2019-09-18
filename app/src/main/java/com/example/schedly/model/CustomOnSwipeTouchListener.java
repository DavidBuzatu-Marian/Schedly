package com.example.schedly.model;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class CustomOnSwipeTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;

    public CustomOnSwipeTouchListener(Context context){
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean _result = false;
            try {
                float _diffY = e2.getY() - e1.getY();
                float _diffX = e2.getX() - e1.getX();
                if (Math.abs(_diffX) > Math.abs(_diffY)) {
                    if (Math.abs(_diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (_diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        _result = true;
                    }
                }
                else if (Math.abs(_diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (_diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                    _result = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return _result;
        }
    }

    public void onSwipeRight() {
    }

    public void onSwipeLeft() {
    }

    public void onSwipeTop() {
    }

    public void onSwipeBottom() {
    }
}

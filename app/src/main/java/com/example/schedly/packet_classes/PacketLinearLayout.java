package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.cardview.widget.CardView;

import com.example.schedly.R;

import java.util.HashMap;

public class PacketLinearLayout extends CardView {

    private Activity activity;
    private HashMap<String, CardView> mLinearLayoutHashMap = new HashMap<>();
    private Animation mAnimationLR;
    private final String[] mDaysOfTheWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "AllDays"};
    private final Integer[] mIDs = {R.id.act_SWHours_CV_Monday, R.id.act_SWHours_CV_Tuesday, R.id.act_SWHours_CV_Wednesday, R.id.act_SWHours_CV_Thursday, R.id.act_SWHours_CV_Friday, R.id.act_SWHours_CV_Saturday, R.id.act_SWHours_CV_AllDays};

    public PacketLinearLayout(Context context, Activity _activity) {
        super(context);
        this.activity = _activity;
        initializeMap();
    }


    private void initializeMap() {
        int _counter;
        mAnimationLR = AnimationUtils.loadAnimation(activity, R.anim.cardview_transition_lr);
        for(_counter = 0; _counter < 7; _counter++) {
            mLinearLayoutHashMap.put(mDaysOfTheWeek[_counter], (CardView) this.activity.findViewById(mIDs[_counter]));
            if(_counter < 5) {
                mLinearLayoutHashMap.get(mDaysOfTheWeek[_counter]).setVisibility(View.GONE);
            }
            mAnimationLR.setStartOffset(_counter * 150);
            mLinearLayoutHashMap.get(mDaysOfTheWeek[_counter]).setAnimation(mAnimationLR);
        }
        this.activity.findViewById(R.id.act_SWHours_CV_Sunday).setAnimation(mAnimationLR);
    }

    public void setVisibilityOnCheck(boolean isChecked) {
        int _counter;
        for(_counter = 0; _counter < 5; _counter++) {
            if(isChecked) {
                mLinearLayoutHashMap.get(mDaysOfTheWeek[_counter]).setVisibility(View.VISIBLE);
            }
            else {
                mLinearLayoutHashMap.get(mDaysOfTheWeek[_counter]).setVisibility(View.GONE);
            }
        }
        if(isChecked) {
            mLinearLayoutHashMap.get(mDaysOfTheWeek[6]).setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.activity.findViewById(R.id.act_SWHours_CV_Saturday).getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.act_SWHours_CV_Friday);
            mLinearLayoutHashMap.get(mDaysOfTheWeek[5]).setLayoutParams(params);
        }
        else {
            mLinearLayoutHashMap.get(mDaysOfTheWeek[6]).setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.activity.findViewById(R.id.act_SWHours_CV_Saturday).getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.act_SWHours_CV_AllDays);
            mLinearLayoutHashMap.get(mDaysOfTheWeek[5]).setLayoutParams(params);
        }
    }
}

package com.davidbuzatu.schedly.packet_classes;

import android.app.Activity;
import android.content.Intent;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.davidbuzatu.schedly.CalendarActivity;
import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.ScheduleDurationActivity;
import com.davidbuzatu.schedly.SetPhoneNumberActivity;
import com.davidbuzatu.schedly.SetProfessionActivity;
import com.davidbuzatu.schedly.SetWorkingHoursActivity;
import com.davidbuzatu.schedly.model.User;


import static com.davidbuzatu.schedly.MainActivity.CA_CANCEL;

public class PacketMainLogin {
    private boolean mDialogExists;

    public static void redirectUser(Activity activity) {
        User currentUser = User.getInstance();

        if(currentUser.getUserPhoneNumber() == null) {
            startPhoneNumberActivity(activity);
            return;
        }
        if(currentUser.getUserProfession() == null ){
            startProfessionActivity(activity);
            return;
        }

        if(currentUser.getUserWorkingHours() == null) {
            startWorkingHoursActivity(activity);
            return;
        }

        if(currentUser.getUserDisplayName() == null) {
            startScheduleDurationActivity(activity);
            return;
        }
        startCalendarActivity(activity);
    }

    private static void startWorkingHoursActivity(Activity activity) {
        Intent _workingDaysIntent = new Intent(activity, SetWorkingHoursActivity.class);
        activity.startActivity(_workingDaysIntent);
    }

    private static void startPhoneNumberActivity(Activity activity) {
        Intent _phoneNumberIntent = new Intent(activity, SetPhoneNumberActivity.class);
        activity.startActivity(_phoneNumberIntent);
    }

    private static void startProfessionActivity(Activity activity) {
        Intent _professionIntent = new Intent(activity, SetProfessionActivity.class);
        activity.startActivity(_professionIntent);
    }
    private static void startCalendarActivity(Activity activity) {
        Intent _calendarIntent = new Intent(activity, CalendarActivity.class);
        activity.startActivityForResult(_calendarIntent, CA_CANCEL);
        activity.finish();
    }

    private static void startScheduleDurationActivity(Activity activity) {
        Intent _scheduleDurationIntent = new Intent(activity, ScheduleDurationActivity.class);
        activity.startActivity(_scheduleDurationIntent);
    }


    public void showProgressBar(boolean show, Activity activity) {
        ProgressBar mProgressBar = activity.findViewById(R.id.act_main_PB);
        ConstraintLayout mRootConstraintLayout = activity.findViewById(R.id.act_main_CL_Root);
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mRootConstraintLayout.setClickable(!show);
        mRootConstraintLayout.setEnabled(!show);
        if (!mDialogExists) {
            disableViews(!show, activity);
        }
    }

    private void disableViews(boolean value, Activity activity) {
        ViewGroup _viewGroup = activity.findViewById(R.id.act_main_CL_Root);
        loopThroughViews(_viewGroup, value);
        activity.findViewById(R.id.act_main_TIL_email).setEnabled(value);
        _viewGroup = activity.findViewById(R.id.act_main_RL_CV_Password);
        loopThroughViews(_viewGroup, value);
    }

    private void loopThroughViews(ViewGroup viewGroup, boolean value) {
        int _childrenNumber = viewGroup.getChildCount(), _counter;
        for (_counter = 0; _counter < _childrenNumber; _counter++) {
            View _childView = viewGroup.getChildAt(_counter);
            _childView.setEnabled(value);
        }
        viewGroup.setEnabled(value);
    }

    public void setDialogViewExists(boolean value) {
        mDialogExists = value;
    }
}

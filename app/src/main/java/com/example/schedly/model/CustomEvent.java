package com.example.schedly.model;

import android.util.Log;

import com.example.schedly.R;

import java.util.Calendar;
import java.util.Date;

public class CustomEvent extends Date {
    private static Long mUserAppointmentDuration;
    private Long mUserNumberOfAppointments;
    private Long mStartHour, mEndHour;
    private Date mDate;

    public CustomEvent(long time) {
        super(time);
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(time);
        mDate = _calendar.getTime();
    }

    public static void setUserAppointmentDuration(Long mUserAppointmentDuration) {
        CustomEvent.mUserAppointmentDuration = mUserAppointmentDuration;
    }


    public int getDayStatus() {
        long _differenceInMillis = mEndHour - mStartHour;
        long _minutes = _differenceInMillis / 60000;
        long _numberOfAppointmentsPossible = _minutes / mUserAppointmentDuration;
        if(mUserNumberOfAppointments < (_numberOfAppointmentsPossible / 2) && mUserAppointmentDuration > 0) {
            return R.drawable.event_less_than_half;
        } else if(mUserNumberOfAppointments >= (_numberOfAppointmentsPossible / 2) && mUserNumberOfAppointments < _numberOfAppointmentsPossible) {
            return R.drawable.event_more_than_half;
        } else {
            return R.drawable.event_full;
        }
    }

    public void setUserNumberOfAppointments(Long mUserNumberOfAppointments) {
        this.mUserNumberOfAppointments = mUserNumberOfAppointments;
    }

    public void setStartHour(Long mStartHour) {
        this.mStartHour = mStartHour;
    }

    public void setEndHour(Long mEndHour) {
        this.mEndHour = mEndHour;
    }
}

package com.example.schedly.model;

import android.util.Log;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Appointment{

    private String mHour;
    private String mName;
    private String mPhoneNumber;
    private String mCurrentDayID;
    private String mDate;
    private String mUserDaysWithScheduleID;


    public Appointment(String _hour, Gson _gson, String _json, String currentDayID, String userDaysWithScheduleID, String date) {
        mHour = _hour;
        Properties data = _gson.fromJson(_json, Properties.class);
        mName = data.getProperty("Name");
        mPhoneNumber = data.getProperty("PhoneNumber");
        mCurrentDayID = currentDayID;
        mUserDaysWithScheduleID = userDaysWithScheduleID;
        mDate = date;
    }

    public Appointment(String hour, String name, String phoneNumber, String currentDayID, String userDaysWithScheduleID, String date) {
        mHour = hour;
        mName = name;
        mPhoneNumber = phoneNumber;
        mCurrentDayID = currentDayID;
        mUserDaysWithScheduleID = userDaysWithScheduleID;
        mDate = date;
    }


    public String getmHour() {
        return mHour;
    }

    public void setmHour(String mHour) {
        this.mHour = mHour;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmPhoneNumber() {
        return mPhoneNumber;
    }

    public void setmPhoneNumber(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
    }

    public String getmCurrentDayID() {
        return mCurrentDayID;
    }

    public String getmUserDaysWithScheduleID() {
        return mUserDaysWithScheduleID;
    }

    public String getmDate() {
        return mDate;
    }
}

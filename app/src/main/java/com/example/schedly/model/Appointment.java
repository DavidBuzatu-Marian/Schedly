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


    public Appointment(String _hour, Gson _gson, String _json) {
        mHour = _hour;
        Properties data = _gson.fromJson(_json, Properties.class);
        mName = data.getProperty("Name");
        mPhoneNumber = data.getProperty("PhoneNumber");
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
}
package com.example.schedly.model;

import android.util.Log;

import java.util.List;
import java.util.Map;

public class Appointment{

    private String mHour;
    private String mName;
    private String mPhoneNumber;


    public Appointment(String _hour, Map<String, String> _map) {
        mHour = _hour;
        mName = _map.get("Name");
        mPhoneNumber = _map.get("PhoneNumber");
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

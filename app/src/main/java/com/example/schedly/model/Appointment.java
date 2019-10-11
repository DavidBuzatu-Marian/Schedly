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
    private String mDate;
    private String mAppointmentType;
    private Long mDateInMillis;


    public Appointment(String hour, Gson gson, String json, String date, Long dateInMillis) {
        mHour = hour;
        Properties data = gson.fromJson(json, Properties.class);
        mName = data.getProperty("Name");
        mPhoneNumber = data.getProperty("PhoneNumber");
        mAppointmentType = data.getProperty("AppointmentType");
        mDate = date;
        mDateInMillis = dateInMillis;
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


    public String getmDate() {
        return mDate;
    }

    public Long getmDateInMillis() {
        return mDateInMillis;
    }

    public String getmAppointmentType() {
        return mAppointmentType;
    }
}

package com.example.schedly.model;

import com.example.schedly.R;

public enum DaysOfWeek
{
    ALLDAYS(    "All days",     R.id.act_SWHours_CV_AllDays,    R.id.act_SWHours_Spinner_AllDaysStart,      R.id.act_SWHours_Spinner_AllDaysEnd,    false),
    MONDAY(     "Monday",       R.id.act_SWHours_CV_Monday,     R.id.act_SWHours_Spinner_MondayStart,       R.id.act_SWHours_Spinner_MondayEnd,     false),
    TUESDAY(    "Tuesday",      R.id.act_SWHours_CV_Tuesday,    R.id.act_SWHours_Spinner_TuesdayStart,      R.id.act_SWHours_Spinner_TuesdayEnd,    false),
    WEDNESDAY(  "Wednesday",    R.id.act_SWHours_CV_Wednesday,  R.id.act_SWHours_Spinner_WednesdayStart,    R.id.act_SWHours_Spinner_WednesdayEnd,  false),
    THURSDAY(   "Thursday",     R.id.act_SWHours_CV_Thursday,   R.id.act_SWHours_Spinner_ThursdayStart,     R.id.act_SWHours_Spinner_ThursdayEnd,   false),
    FRIDAY(     "Friday",       R.id.act_SWHours_CV_Friday,     R.id.act_SWHours_Spinner_FridayStart,       R.id.act_SWHours_Spinner_FridayEnd,     false),
    SATURDAY(   "Saturday",     R.id.act_SWHours_CV_Saturday,   R.id.act_SWHours_Spinner_SaturdayStart,     R.id.act_SWHours_Spinner_SaturdayEnd,   false),
    SUNDAY(     "Sunday",       R.id.act_SWHours_CV_Sunday,     R.id.act_SWHours_Spinner_SundayStart,       R.id.act_SWHours_Spinner_SundayEnd,     false);








    private String eDisplayName;
    private int eCardViewID;
    private int eSpinnerStartID;
    private int eSpinnedEndID;
    private boolean eFree;



    DaysOfWeek(String displayName, int cardViewID, int spinnerStartID, int spinnerEndID, boolean free) {
        eDisplayName = displayName;
        eCardViewID = cardViewID;
        eFree = free;
    }


    public int getStringResId() {
        return eCardViewID;
    }

    public boolean getFreeStatus() {
        return eFree;
    }


    public int geteSpinnerStartID() {
        return eSpinnerStartID;
    }

    public int geteSpinnedEndID() {
        return eSpinnedEndID;
    }
}

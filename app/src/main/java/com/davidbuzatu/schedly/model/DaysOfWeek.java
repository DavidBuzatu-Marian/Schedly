package com.davidbuzatu.schedly.model;

import com.davidbuzatu.schedly.R;

public enum DaysOfWeek
{
    ALL(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_AllDay),    R.string.act_SWHours_TV_AllDay,       R.id.act_SWHours_CV_AllDays,    R.id.act_SWHours_CB_AllDays,        R.id.act_SWHours_Spinner_AllDaysStart,      R.id.act_SWHours_Spinner_AllDaysEnd,    false),
    MON(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Monday),    R.string.act_SWHours_TV_Monday,       R.id.act_SWHours_CV_Monday,     R.id.act_SWHours_CB_MondayFree,     R.id.act_SWHours_Spinner_MondayStart,       R.id.act_SWHours_Spinner_MondayEnd,     false),
    TUE(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Tuesday),   R.string.act_SWHours_TV_Tuesday,      R.id.act_SWHours_CV_Tuesday,    R.id.act_SWHours_CB_TuesdayFree,    R.id.act_SWHours_Spinner_TuesdayStart,      R.id.act_SWHours_Spinner_TuesdayEnd,    false),
    WED(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Wednesday), R.string.act_SWHours_TV_Wednesday,    R.id.act_SWHours_CV_Wednesday,  R.id.act_SWHours_CB_WednesdayFree,  R.id.act_SWHours_Spinner_WednesdayStart,    R.id.act_SWHours_Spinner_WednesdayEnd,  false),
    THU(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Thursday),  R.string.act_SWHours_TV_Thursday,     R.id.act_SWHours_CV_Thursday,   R.id.act_SWHours_CB_ThursdayFree,   R.id.act_SWHours_Spinner_ThursdayStart,     R.id.act_SWHours_Spinner_ThursdayEnd,   false),
    FRI(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Friday),    R.string.act_SWHours_TV_Friday ,      R.id.act_SWHours_CV_Friday,     R.id.act_SWHours_CB_FridayFree,     R.id.act_SWHours_Spinner_FridayStart,       R.id.act_SWHours_Spinner_FridayEnd,     false),
    SAT(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Saturday),  R.string.act_SWHours_TV_Saturday ,    R.id.act_SWHours_CV_Saturday,   R.id.act_SWHours_CB_SaturdayFree,   R.id.act_SWHours_Spinner_SaturdayStart,     R.id.act_SWHours_Spinner_SaturdayEnd,   false),
    SUN(ContextForStrings.getContext().getResources().getString(R.string.act_SWHours_TV_Sunday),    R.string.act_SWHours_TV_Sunday  ,     R.id.act_SWHours_CV_Sunday,     R.id.act_SWHours_CB_SundayFree,     R.id.act_SWHours_Spinner_SundayStart,       R.id.act_SWHours_Spinner_SundayEnd,     false);

    private String eDisplayName;
    private int eDisplayNameResID;
    private int eCardViewID;
    private int eCheckBoxID;
    private int eSpinnerStartID;
    private int eSpinnerEndID;
    private boolean eFree;



    DaysOfWeek(String displayName, int displayNameResID, int cardViewID, int checkBoxID, int spinnerStartID, int spinnerEndID, boolean free) {
        eDisplayName = displayName;
        eDisplayNameResID = displayNameResID;
        eCardViewID = cardViewID;
        eCheckBoxID = checkBoxID;
        eSpinnerStartID = spinnerStartID;
        eSpinnerEndID = spinnerEndID;
        eFree = free;
    }


    public int getCardViewId() {
        return eCardViewID;
    }

    public boolean getFreeStatus() {
        return eFree;
    }


    public int geteSpinnerStartID() {
        return eSpinnerStartID;
    }

    public int geteSpinnerEndID() {
        return eSpinnerEndID;
    }

    public String geteDisplayName() {
        return eDisplayName;
    }

    public int geteCheckBoxID() {
        return eCheckBoxID;
    }

    public void setFreeStatus(boolean checkStatus) {
        eFree = checkStatus;
    }

    public int geteDisplayNameResID() {
        return eDisplayNameResID;
    }

}

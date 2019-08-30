package com.example.schedly.packet_classes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CalendarView;

import androidx.preference.PreferenceManager;

import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder;
import com.example.schedly.R;

public class PacketCalendarHelpers {

    private Activity mActivity;
    private SharedPreferences mUserPreferences;

    public PacketCalendarHelpers(Activity activity) {
        mActivity = activity;
        mUserPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
    }

    public void displayHelpOnDate(CalendarView view) {
        new BubbleShowCaseBuilder(mActivity)
                .title("Selected date") //Any title for the bubble view
                .backgroundColor(R.color.colorPrimaryDark)
                .description(mActivity.getString(R.string.helpDateSelected))
                .targetView(view) //View to point out
                .showOnce("FirstTimer")
                .show(); //Display the ShowCase
    }

    public void displayHelpOnAdd() {
        new BubbleShowCaseBuilder(mActivity)
                .title("Add appointment manually") //Any title for the bubble view
                .backgroundColor(R.color.colorPrimary)
                .description(mActivity.getString(R.string.helpAddExplained))
                .targetView(mActivity.findViewById(R.id.act_Calendar_IV_AddIcon))//View to point out
                .showOnce("FirstTimerAdd")
                .show(); //Display the ShowCase
    }

    public void displayHelpers() {
        if(mUserPreferences.getBoolean("firstLogin", true)) {
            /* SMS helper */
            new BubbleShowCaseBuilder(mActivity)
                    .title("How does Schedly work?") //Any title for the bubble view
                    .backgroundColor(R.color.colorPrimary)
                    .description(mActivity.getString(R.string.helpSMSExplained))
                    .targetView(mActivity.findViewById(R.id.act_Calendar_CalendarV)) //View to point out
                    .show(); //Display the ShowCase

            SharedPreferences.Editor _userEditor = mUserPreferences.edit();
            _userEditor.putBoolean("firstLogin", false);
            _userEditor.apply();
        }
    }

    public void displayHelpOnEdit() {
        new BubbleShowCaseBuilder(mActivity)
                .title("Edit appointment") //Any title for the bubble view
                .backgroundColor(R.color.colorPrimary)
                .description(mActivity.getString(R.string.helpEditExplained))
                .targetView(mActivity.findViewById(R.id.act_Calendar_IV_AddIcon))//View to point out
                .showOnce("FirstTimerEdit")
                .show(); //Display the ShowCase
    }
}

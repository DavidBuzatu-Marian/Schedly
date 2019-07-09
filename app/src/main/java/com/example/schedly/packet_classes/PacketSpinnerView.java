package com.example.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatSpinner;

import com.example.schedly.R;
import com.example.schedly.SetWorkingHoursActivity;

import java.util.HashMap;
import java.util.Map;


public class PacketSpinnerView extends AppCompatSpinner {
    /* Spinners */
    private HashMap<String, Spinner> mSpinnerArray = new HashMap<>();
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> mIDsArray = new HashMap<>(16);
    private CheckBox[] mCheckBoxArray = new CheckBox[9];
    private String[] mStartDay = new String[8];
    private String[] mEndDay = new String[8];
    private int mDaysIterator;
    private String[] mStartHours = new String[8];
    private String[] mEndHours = new String[8];
    private Integer[] mSpinnerIDs = new Integer[16];
    private PacketLinearLayout mPacketLinearLayout;
    private Activity activity;


    public PacketSpinnerView(Context context, PacketLinearLayout packetLinearLayout, Activity _activity) {
        super(context);
        mPacketLinearLayout = packetLinearLayout;
        this.activity = _activity;
        initializeStringOfIDs();
        initializeCheckBoxArray();
        initializeMaps();
        setAllDaysCheckbox();
    }

    private void initializeStringOfIDs() {
        mSpinnerIDs[0] = R.id.act_SWHours_Spinner_MondayStart;
        mSpinnerIDs[1] = R.id.act_SWHours_Spinner_MondayEnd;
        mSpinnerIDs[2] = R.id.act_SWHours_Spinner_TuesdayStart;
        mSpinnerIDs[3] = R.id.act_SWHours_Spinner_TuesdayEnd;
        mSpinnerIDs[4] = R.id.act_SWHours_Spinner_WednesdayStart;
        mSpinnerIDs[5] = R.id.act_SWHours_Spinner_WednesdayEnd;
        mSpinnerIDs[6] = R.id.act_SWHours_Spinner_ThursdayStart;
        mSpinnerIDs[7] = R.id.act_SWHours_Spinner_ThursdayEnd;
        mSpinnerIDs[8] = R.id.act_SWHours_Spinner_FridayStart;
        mSpinnerIDs[9] = R.id.act_SWHours_Spinner_FridayEnd;
        mSpinnerIDs[10] = R.id.act_SWHours_Spinner_SaturdayStart;
        mSpinnerIDs[11] = R.id.act_SWHours_Spinner_SaturdayEnd;
        mSpinnerIDs[12] = R.id.act_SWHours_Spinner_SundayStart;
        mSpinnerIDs[13] = R.id.act_SWHours_Spinner_SundayEnd;
        mSpinnerIDs[14] = R.id.act_SWHours_Spinner_AllDaysStart;
        mSpinnerIDs[15] = R.id.act_SWHours_Spinner_AllDaysEnd;
    }

    private void initializeCheckBoxArray() {
        mCheckBoxArray[0] = this.activity.findViewById(R.id.act_SWHours_CB_DiffHours);
        mCheckBoxArray[1] = this.activity.findViewById(R.id.act_SWHours_CB_MondayFree);
        mCheckBoxArray[2] = this.activity.findViewById(R.id.act_SWHours_CB_TuesdayFree);
        mCheckBoxArray[3] = this.activity.findViewById(R.id.act_SWHours_CB_WednesdayFree);
        mCheckBoxArray[4] = this.activity.findViewById(R.id.act_SWHours_CB_ThursdayFree);
        mCheckBoxArray[5] = this.activity.findViewById(R.id.act_SWHours_CB_FridayFree);
        mCheckBoxArray[6] = this.activity.findViewById(R.id.act_SWHours_CB_SaturdayFree);
        mCheckBoxArray[7] = this.activity.findViewById(R.id.act_SWHours_CB_SundayFree);
    }

    private void initializeMaps() {
        int _counter;

        mStartDay[0] = "MondayStart";
        mStartDay[1] = "TuesdayStart";
        mStartDay[2] = "WednesdayStart";
        mStartDay[3] = "ThursdayStart";
        mStartDay[4] = "FridayStart";
        mStartDay[5] = "SaturdayStart";
        mStartDay[6] = "SundayStart";
        mStartDay[7] = "AllDaysStart";

        mEndDay[0] = "MondayEnd";
        mEndDay[1] = "TuesdayEnd";
        mEndDay[2] = "WednesdayEnd";
        mEndDay[3] = "ThursdayEnd";
        mEndDay[4] = "FridayEnd";
        mEndDay[5] = "SaturdayEnd";
        mEndDay[6] = "SundayEnd";
        mEndDay[7] = "AllDaysEnd";

        for(_counter = 0; _counter < 16; _counter += 2) {
            mIDsArray.put(mSpinnerIDs[_counter], _counter / 2);
            mIDsArray.put(mSpinnerIDs[_counter + 1], _counter / 2);
            mSpinnerArray.put(mStartDay[_counter / 2], (Spinner) this.activity.findViewById(mSpinnerIDs[_counter]));
            mSpinnerArray.put(mEndDay[_counter / 2], (Spinner) this.activity.findViewById(mSpinnerIDs[_counter + 1]));
        }

        for(mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
            setCheckChanged(mCheckBoxArray[mDaysIterator + 1], mStartDay[mDaysIterator], mEndDay[mDaysIterator]);
        }
    }

    public void setCheckChanged(CheckBox checkBox, final String startKey, final String endKey) {
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mSpinnerArray.get(startKey).setVisibility(View.GONE);
                    mSpinnerArray.get(endKey).setVisibility(View.GONE);
                }
                else {
                    mSpinnerArray.get(startKey).setVisibility(View.VISIBLE);
                    mSpinnerArray.get(endKey).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void setUpSpinners(final ArrayAdapter<CharSequence> mAdapterHours) {
        for(mDaysIterator = 0; mDaysIterator < 8; mDaysIterator++) {
            mSpinnerArray.get(mStartDay[mDaysIterator]).setAdapter(mAdapterHours);
            mSpinnerArray.get(mEndDay[mDaysIterator]).setAdapter(mAdapterHours);
            mSpinnerArray.get(mStartDay[mDaysIterator]).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int counter;
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    counter = mIDsArray.get(parent.getId());
                    mStartHours[counter] = mAdapterHours.getItem(position).toString();
                    Log.d("selectedStart", "" + view.getId()  + "");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mStartHours[mDaysIterator] = null;
                }
            });
            mSpinnerArray.get(mEndDay[mDaysIterator]).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int counter;
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    counter = mIDsArray.get(parent.getId());
                    mEndHours[counter] = mAdapterHours.getItem(position).toString();
                    Log.d("selectedEnd", "" + parent.getId() + "");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mEndHours[mDaysIterator] = null;
                }
            });
        }
    }

    private void setAllDaysCheckbox() {
        mCheckBoxArray[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPacketLinearLayout.setVisibilityOnCheck(isChecked);
            }
        });
    }

    public boolean checkEmptySpinners() {
        // Write working hours to database
        boolean emptySpinner = false;
        if(mCheckBoxArray[0].isChecked()) {
            for (mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
                if (mStartHours[mDaysIterator] == null || mEndHours[mDaysIterator] == null) {
                    emptySpinner = true;
                }
            }
        }
        else {
            for(mDaysIterator = 5; mDaysIterator < 8; mDaysIterator++) {
                if (mStartHours[mDaysIterator] == null || mEndHours[mDaysIterator] == null) {
                    emptySpinner = true;
                }
            }
        }

        return emptySpinner;
    }

    public Map<String, Object> getDaysToAdd() {
        Map<String, Object> daysToAdd = new HashMap<>();
        if(mCheckBoxArray[0].isChecked()) {
            for (mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
                putToDays(mCheckBoxArray[mDaysIterator + 1].isChecked(), daysToAdd);
            }
        }
        else {
            for (mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
                if(mDaysIterator < 5) {
                    daysToAdd.put(mStartDay[mDaysIterator], mStartHours[7]);
                    daysToAdd.put(mEndDay[mDaysIterator], mEndHours[7]);
                }
                else {
                    putToDays(mCheckBoxArray[mDaysIterator + 1].isChecked(), daysToAdd);
                }
            }
        }
        return daysToAdd;
    }

    public void putToDays(boolean isChecked, Map<String, Object> daysToAdd) {
        if(isChecked) {
            daysToAdd.put(mStartDay[mDaysIterator], "Free");
            daysToAdd.put(mEndDay[mDaysIterator], "Free");
        }
        else {
            daysToAdd.put(mStartDay[mDaysIterator], mStartHours[mDaysIterator]);
            daysToAdd.put(mEndDay[mDaysIterator], mEndHours[mDaysIterator]);
        }
    }
}

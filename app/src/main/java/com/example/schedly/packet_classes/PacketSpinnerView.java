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

import androidx.appcompat.widget.AppCompatSpinner;

import com.example.schedly.R;
import com.example.schedly.model.DaysOfWeek;

import java.util.HashMap;
import java.util.Map;


public class PacketSpinnerView extends AppCompatSpinner {
    /* Spinners */
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> mIDsArray = new HashMap<>();
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private int mDaysIterator;
    private String[] mStartHours = new String[8];
    private String[] mEndHours = new String[8];

    private PacketCardView mPacketCardView;
    private Activity mActivity;


    public PacketSpinnerView(Context context, PacketCardView packetCardView, Activity _activity) {
        super(context);
        mPacketCardView = packetCardView;
        mActivity = _activity;

        initializeMaps();
        setAllDaysCheckbox();
    }



    private void initializeMaps() {
        int _counter = 0;

        for(DaysOfWeek _day : DaysOfWeek.values()) {
            setCheckChanged((CheckBox) mActivity.findViewById(_day.geteCheckBoxID()), _day.geteDisplayName().substring(0, 3).toUpperCase());
            mIDsArray.put(_day.geteSpinnerStartID(), _counter / 2);
            mIDsArray.put(_day.geteSpinnerEndID(), _counter / 2);
            _counter += 2;
        }
    }

    public void setCheckChanged(CheckBox checkBox, final String day) {
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    setDayVisibility(true, day);
                    mStartHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerStartID())] = "Free";
                    mEndHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerEndID())] = "Free";
                }
                else {
                    setDayVisibility(false, day);
                    mStartHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerStartID())] = ((Spinner) mActivity.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID())).getSelectedItem().toString();
                    mEndHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerEndID())] = ((Spinner) mActivity.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID())).getSelectedItem().toString();
                }
            }
        });
    }

    private void setDayVisibility(boolean b, String day) {
        mActivity.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID()).setVisibility(b ? View.GONE: View.VISIBLE);
        mActivity.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID()).setVisibility(b ? View.GONE: View.VISIBLE);
        DaysOfWeek.valueOf(day).setFreeStatus(b);
    }

    public void setUpSpinners(final ArrayAdapter<CharSequence> mAdapterHours) {
        mDaysIterator = 0;
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            ((Spinner) mActivity.findViewById(_day.geteSpinnerStartID())).setAdapter(mAdapterHours);
            ((Spinner) mActivity.findViewById(_day.geteSpinnerEndID())).setAdapter(mAdapterHours);
            ((Spinner) mActivity.findViewById(_day.geteSpinnerStartID())).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int counter;
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    counter = mIDsArray.get(parent.getId());
                    mStartHours[counter] = mAdapterHours.getItem(position).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mStartHours[mDaysIterator] = null;
                }
            });
            ((Spinner) mActivity.findViewById(_day.geteSpinnerEndID())).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private int counter;
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    counter = mIDsArray.get(parent.getId());
                    mEndHours[counter] = mAdapterHours.getItem(position).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mEndHours[mDaysIterator] = null;
                }
            });

            mDaysIterator++;
        }
    }

    private void setAllDaysCheckbox() {
        ((CheckBox) mActivity.findViewById(R.id.act_SWHours_CB_DiffHours)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPacketCardView.setVisibilityOnCheck(isChecked);
            }
        });
    }

    public boolean checkEmptySpinners() {
        if(((CheckBox) mActivity.findViewById(R.id.act_SWHours_CB_DiffHours)).isChecked()) {
            for (mDaysIterator = 1; mDaysIterator < 8; mDaysIterator++) {
                if (mStartHours[mDaysIterator] == null || mEndHours[mDaysIterator] == null) {
                    return true;
                }
            }
        }
        else {
            /* check if all day is null */
            if (mStartHours[0] == null || mEndHours[0] == null) {
                return true;
            }
            for(mDaysIterator = 6; mDaysIterator < 8; mDaysIterator++) {
                if (mStartHours[mDaysIterator] == null || mEndHours[mDaysIterator] == null) {
                    return true;
                }
            }
        }

        return false;
    }

    public Map<String, Object> getDaysToAdd() {
        Map<String, Object> daysToAdd = new HashMap<>();
        if( ((CheckBox) mActivity.findViewById(R.id.act_SWHours_CB_DiffHours)).isChecked()) {
            /* we have 7 days to get */
            mDaysIterator = 1;
            for (DaysOfWeek _day : DaysOfWeek.values()) {
                if(!_day.geteDisplayName().equals("All")) {
                    putToDays(_day.getFreeStatus(), daysToAdd, _day.geteDisplayName());
                    mDaysIterator++;
                }
            }
        }
        else {
            /* we add for the first 5 days the value
             * of the all week day spinners
             */
            mDaysIterator = 0;
            for (DaysOfWeek _day: DaysOfWeek.values()) {
                if(mDaysIterator < 6 && mDaysIterator > 0) {
                    mWorkingHours.put(_day.geteDisplayName() + "Start", mStartHours[0]);
                    mWorkingHours.put(_day.geteDisplayName() + "End", mEndHours[0]);
                    daysToAdd.put(_day.geteDisplayName() + "Start", mStartHours[0]);
                    daysToAdd.put(_day.geteDisplayName() + "End", mEndHours[0]);
                }
                else if (mDaysIterator >= 6) {
                    putToDays(_day.getFreeStatus(), daysToAdd, _day.geteDisplayName());
                }
                mDaysIterator++;
            }
        }
        return daysToAdd;
    }

    public void putToDays(boolean isChecked, Map<String, Object> daysToAdd, String dayName) {
        if(isChecked) {
            mWorkingHours.put(dayName + "Start", "Free");
            mWorkingHours.put(dayName + "End", "Free");
            daysToAdd.put(dayName + "Start", "Free");
            daysToAdd.put(dayName + "End", "Free");
        }
        else {
            mWorkingHours.put(dayName + "Start", mStartHours[mDaysIterator]);
            mWorkingHours.put(dayName + "End", mEndHours[mDaysIterator]);
            daysToAdd.put(dayName + "Start", mStartHours[mDaysIterator]);
            daysToAdd.put(dayName + "End", mEndHours[mDaysIterator]);
        }
    }

    public HashMap<String, String> getWorkingDays() {
        return mWorkingHours;
    }
}

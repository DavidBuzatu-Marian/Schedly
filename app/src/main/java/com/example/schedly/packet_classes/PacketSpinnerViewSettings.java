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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;

import com.example.schedly.R;
import com.example.schedly.model.Appointment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class PacketSpinnerViewSettings extends AppCompatSpinner {
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
    private String mUserWorkingDaysID;
    private View view;
    private Map<String, Object> mUserWorkingDays;
    private final ArrayAdapter<CharSequence> mAdapterHours;


    public PacketSpinnerViewSettings(Context context, String _userWorkingDaysID, View _view, ArrayAdapter<CharSequence> _adapterHours) {
        super(context);
        mUserWorkingDaysID = _userWorkingDaysID;
        this.view = _view;
        mAdapterHours = _adapterHours;
        getUserWorkingDays();
        initializeStringOfIDs();
        initializeCheckBoxArray();
        initializeMaps();
    }

    private void getUserWorkingDays() {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        mFireStore.collection("workingDays")
                .document(mUserWorkingDaysID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()) {
                            mUserWorkingDays = task.getResult().getData();
                            setUpSpinners();
                        }
                    }
                });
    }

    private void initializeStringOfIDs() {
        mSpinnerIDs[0] = R.id.frag_CWHours_Spinner_MondayStart;
        mSpinnerIDs[1] = R.id.frag_CWHours_Spinner_MondayEnd;
        mSpinnerIDs[2] = R.id.frag_CWHours_Spinner_TuesdayStart;
        mSpinnerIDs[3] = R.id.frag_CWHours_Spinner_TuesdayEnd;
        mSpinnerIDs[4] = R.id.frag_CWHours_Spinner_WednesdayStart;
        mSpinnerIDs[5] = R.id.frag_CWHours_Spinner_WednesdayEnd;
        mSpinnerIDs[6] = R.id.frag_CWHours_Spinner_ThursdayStart;
        mSpinnerIDs[7] = R.id.frag_CWHours_Spinner_ThursdayEnd;
        mSpinnerIDs[8] = R.id.frag_CWHours_Spinner_FridayStart;
        mSpinnerIDs[9] = R.id.frag_CWHours_Spinner_FridayEnd;
        mSpinnerIDs[10] = R.id.frag_CWHours_Spinner_SaturdayStart;
        mSpinnerIDs[11] = R.id.frag_CWHours_Spinner_SaturdayEnd;
        mSpinnerIDs[12] = R.id.frag_CWHours_Spinner_SundayStart;
        mSpinnerIDs[13] = R.id.frag_CWHours_Spinner_SundayEnd;
    }

    private void initializeCheckBoxArray() {
        mCheckBoxArray[0] = this.view.findViewById(R.id.frag_CWHours_CB_MondayFree);
        mCheckBoxArray[1] = this.view.findViewById(R.id.frag_CWHours_CB_TuesdayFree);
        mCheckBoxArray[2] = this.view.findViewById(R.id.frag_CWHours_CB_WednesdayFree);
        mCheckBoxArray[3] = this.view.findViewById(R.id.frag_CWHours_CB_ThursdayFree);
        mCheckBoxArray[4] = this.view.findViewById(R.id.frag_CWHours_CB_FridayFree);
        mCheckBoxArray[5] = this.view.findViewById(R.id.frag_CWHours_CB_SaturdayFree);
        mCheckBoxArray[6] = this.view.findViewById(R.id.frag_CWHours_CB_SundayFree);
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

        mEndDay[0] = "MondayEnd";
        mEndDay[1] = "TuesdayEnd";
        mEndDay[2] = "WednesdayEnd";
        mEndDay[3] = "ThursdayEnd";
        mEndDay[4] = "FridayEnd";
        mEndDay[5] = "SaturdayEnd";
        mEndDay[6] = "SundayEnd";

        for(_counter = 0; _counter < 14; _counter += 2) {
            mIDsArray.put(mSpinnerIDs[_counter], _counter / 2);
            mIDsArray.put(mSpinnerIDs[_counter + 1], _counter / 2);
            mSpinnerArray.put(mStartDay[_counter / 2], (Spinner) this.view.findViewById(mSpinnerIDs[_counter]));
            mSpinnerArray.put(mEndDay[_counter / 2], (Spinner) this.view.findViewById(mSpinnerIDs[_counter + 1]));
        }

        for(mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
            setCheckChanged(mCheckBoxArray[mDaysIterator], mStartDay[mDaysIterator], mEndDay[mDaysIterator]);
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

    public void setUpSpinners() {
        for(mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
            mSpinnerArray.get(mStartDay[mDaysIterator]).setAdapter(mAdapterHours);
            mSpinnerArray.get(mEndDay[mDaysIterator]).setAdapter(mAdapterHours);
            if(!mUserWorkingDays.get(mStartDay[mDaysIterator]).toString().equals("Free")) {
                String _valueStart = mUserWorkingDays.get(mStartDay[mDaysIterator]).toString();
                String _valueEnd = mUserWorkingDays.get(mEndDay[mDaysIterator]).toString();
                mSpinnerArray.get(mStartDay[mDaysIterator]).setSelection(mAdapterHours.getPosition(_valueStart));
                mSpinnerArray.get(mEndDay[mDaysIterator]).setSelection(mAdapterHours.getPosition(_valueEnd));
            }
            else {
                mCheckBoxArray[mDaysIterator].setChecked(true);
            }
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

    public Map<String, Object> getDaysToAdd() {
        Map<String, Object> daysToAdd = new HashMap<>();
        for (mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
            putToDays(mCheckBoxArray[mDaysIterator].isChecked(), daysToAdd);
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

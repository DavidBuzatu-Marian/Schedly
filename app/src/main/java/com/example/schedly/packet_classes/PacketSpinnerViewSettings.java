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
import com.example.schedly.model.DaysOfWeek;
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
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> mIDsArray = new HashMap<>(16);
    private String[] mStartHours = new String[8];
    private String[] mEndHours = new String[8];
    private String mUserWorkingDaysID;
    private View mView;
    private Map<String, Object> mUserWorkingDays;
    private final ArrayAdapter<CharSequence> mAdapterHours;
    private int mDaysIterator;


    public PacketSpinnerViewSettings(Context context, String _userWorkingDaysID, View _view, ArrayAdapter<CharSequence> _adapterHours) {
        super(context);
        mUserWorkingDaysID = _userWorkingDaysID;
        mView = _view;
        mAdapterHours = _adapterHours;
        getUserWorkingDays();
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


    private void initializeMaps() {
        int _counter = 0;

        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(!_day.geteDisplayName().equals("All")) {
                mIDsArray.put(_day.geteSpinnerStartID(), _counter / 2);
                mIDsArray.put(_day.geteSpinnerEndID(), _counter / 2);

                _counter += 2;
            }
        }

        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(!_day.geteDisplayName().equals("All")) {
                setCheckChanged((CheckBox) mView.findViewById(_day.geteCheckBoxID()), _day.geteDisplayName().substring(0, 3).toUpperCase());
            }
        }
    }

    public void setCheckChanged(CheckBox checkBox, final String day) {
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID()).setVisibility(View.GONE);
                    mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID()).setVisibility(View.GONE);

                    DaysOfWeek.valueOf(day).setFreeStatus(true);
                    mStartHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerStartID())] = "Free";
                    mEndHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerEndID())] = "Free";
                }
                else {
                    mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID()).setVisibility(View.VISIBLE);
                    mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID()).setVisibility(View.VISIBLE);
                    mStartHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerStartID())] = ((Spinner) mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID())).getSelectedItem().toString();
                    mEndHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerEndID())] = ((Spinner) mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID())).getSelectedItem().toString();
                    DaysOfWeek.valueOf(day).setFreeStatus(false);
                }
            }
        });
    }

    public void setUpSpinners() {
        mDaysIterator = 0;
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(!_day.geteDisplayName().equals("All")) {
                String _dayStart = _day.geteDisplayName() + "Start",
                        _dayEnd = _day.geteDisplayName() + "End";

                Spinner _startHours = mView.findViewById(_day.geteSpinnerStartID());
                Spinner _endHours = mView.findViewById(_day.geteSpinnerEndID());

                _startHours.setAdapter(mAdapterHours);
                _endHours.setAdapter(mAdapterHours);

                if (!mUserWorkingDays.get(_dayStart).toString().equals("Free")) {
                    String _valueStart = mUserWorkingDays.get(_dayStart).toString();
                    String _valueEnd = mUserWorkingDays.get(_dayEnd).toString();
                    _startHours.setSelection(mAdapterHours.getPosition(_valueStart));
                    _endHours.setSelection(mAdapterHours.getPosition(_valueEnd));

                    _day.setFreeStatus(false);
                } else {
                    ((CheckBox) mView.findViewById(_day.geteCheckBoxID())).setChecked(true);
                    _day.setFreeStatus(true);
                }
                _startHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    private int counter;

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        counter = mIDsArray.get(parent.getId());
                        mStartHours[counter] = mAdapterHours.getItem(position).toString();
                        Log.d("selectedStart", "" + view.getId() + "");
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        mStartHours[mDaysIterator] = null;
                    }
                });
                _endHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
            mDaysIterator++;
        }
    }

    public Map<String, Object> getDaysToAdd() {
        mDaysIterator = 0;
        Map<String, Object> daysToAdd = new HashMap<>();

        for (DaysOfWeek _day: DaysOfWeek.values()) {
            if(!_day.geteDisplayName().equals("All")) {
                putToDays(_day.getFreeStatus(), daysToAdd, _day.geteDisplayName());
                mDaysIterator++;
            }
        }
        return daysToAdd;
    }

    public void putToDays(boolean isChecked, Map<String, Object> daysToAdd, String dayName) {
        if(isChecked) {
            daysToAdd.put(dayName + "Start", "Free");
            daysToAdd.put(dayName + "End", "Free");
        }
        else {
            daysToAdd.put(dayName + "Start", mStartHours[mDaysIterator]);
            daysToAdd.put(dayName + "End", mEndHours[mDaysIterator]);
        }
    }
}

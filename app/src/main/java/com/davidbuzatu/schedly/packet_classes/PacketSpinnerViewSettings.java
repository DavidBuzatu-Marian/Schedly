package com.davidbuzatu.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;

import com.davidbuzatu.schedly.model.DaysOfWeek;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PacketSpinnerViewSettings extends AppCompatSpinner {
    /* Spinners */
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> mIDsArray = new HashMap<>(16);
    private String[] mStartHours = new String[8];
    private String[] mEndHours = new String[8];
    private View mView;
    private Map<String, Object> mUserWorkingDays;
    private final ArrayAdapter<CharSequence> mAdapterHours;
    private int mDaysIterator;

    public PacketSpinnerViewSettings(Context context, View _view, ArrayAdapter<CharSequence> _adapterHours) {
        super(context);
        mView = _view;
        mAdapterHours = _adapterHours;
        getUserWorkingDays();
        initializeIDArrays();
    }

    private void getUserWorkingDays() {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        mFireStore.collection("workingDays")
                .document(FirebaseAuth.getInstance().getUid())
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


    private void initializeIDArrays() {
        int _counter = 0;
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(!_day.geteDisplayName().equals("All")) {
                mIDsArray.put(_day.geteSpinnerStartID(), _counter / 2);
                mIDsArray.put(_day.geteSpinnerEndID(), _counter / 2);
                setCheckChanged((CheckBox) mView.findViewById(_day.geteCheckBoxID()), _day.geteDisplayName().substring(0, 3).toUpperCase());
                _counter += 2;
            }
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
                    mStartHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerStartID())] = ((Spinner) mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID())).getSelectedItem().toString();
                    mEndHours[mIDsArray.get(DaysOfWeek.valueOf(day).geteSpinnerEndID())] = ((Spinner) mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID())).getSelectedItem().toString();
                }
            }
        });
    }

    private void setDayVisibility(boolean b, String day) {
        mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerStartID()).setVisibility(b ? View.GONE : View.VISIBLE);
        mView.findViewById(DaysOfWeek.valueOf(day).geteSpinnerEndID()).setVisibility(b ? View.GONE : View.VISIBLE);
        DaysOfWeek.valueOf(day).setFreeStatus(b);
    }

    public void setUpSpinners() {
        mDaysIterator = 0;
        for(DaysOfWeek _day: DaysOfWeek.values()) {
            if(!_day.geteDisplayName().equals("All")) {
                Spinner _startHours = mView.findViewById(_day.geteSpinnerStartID());
                Spinner _endHours = mView.findViewById(_day.geteSpinnerEndID());
                setSpinnerAdaptersAndValues(_startHours, _endHours, _day);
                _startHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                _endHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
            }
            mDaysIterator++;
        }
    }

    private void setSpinnerAdaptersAndValues(Spinner startHours, Spinner endHours, DaysOfWeek _day) {
        String _dayStart = _day.geteDisplayName() + "Start",
                _dayEnd = _day.geteDisplayName() + "End";
        startHours.setAdapter(mAdapterHours);
        endHours.setAdapter(mAdapterHours);
        if (!mUserWorkingDays.get(_dayStart).toString().equals("Free")) {
            String _valueStart = mUserWorkingDays.get(_dayStart).toString();
            String _valueEnd = mUserWorkingDays.get(_dayEnd).toString();
            startHours.setSelection(mAdapterHours.getPosition(_valueStart));
            endHours.setSelection(mAdapterHours.getPosition(_valueEnd));
            _day.setFreeStatus(false);
        } else {
            ((CheckBox) mView.findViewById(_day.geteCheckBoxID())).setChecked(true);
            _day.setFreeStatus(true);
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

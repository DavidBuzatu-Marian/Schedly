package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.midi.MidiDevice;
import android.os.Bundle;
import android.sax.StartElementListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SetWorkingHoursActivity extends AppCompatActivity {
    private String userID;
    private String TAG = "RES";
    private final int SWH_CANCEL = 2005;
    private String workingDaysID;
    /* Spinners */
    private HashMap<String, Spinner> mSpinnerArray = new HashMap<>();
    private HashMap<Integer, Integer> mIDsArray = new HashMap<>();
    private ArrayAdapter<CharSequence> mAdapterHours;
    private CheckBox[] mCheckBoxArray = new CheckBox[9];
    private String[] mStartDay = new String[8];
    private String[] mEndDay = new String[8];
    private int mDaysIterator;
    private String[] mStartHours = new String[8];
    private String[] mEndHours = new String[8];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_working_hours);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
            getUserWorkingDaysID(userID);
        }

        FloatingActionButton floatingActionButton = findViewById(R.id.act_SWHours_floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                if (!emptySpinner) {
                    addUserDataToDatabase(userID);
                } else {
                    Toast.makeText(SetWorkingHoursActivity.this, "Both starting and ending hours are required!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout linearLayoutMonday = findViewById(R.id.act_SWHours_LL_Monday);
        LinearLayout linearLayoutTuesday = findViewById(R.id.act_SWHours_LL_Tuesday);
        LinearLayout linearLayoutWednesday = findViewById(R.id.act_SWHours_LL_Wednesday);
        LinearLayout linearLayoutThursday = findViewById(R.id.act_SWHours_LL_Thursday);
        LinearLayout linearLayoutFriday = findViewById(R.id.act_SWHours_LL_Friday);

        linearLayoutMonday.setVisibility(View.GONE);
        linearLayoutTuesday.setVisibility(View.GONE);
        linearLayoutWednesday.setVisibility(View.GONE);
        linearLayoutThursday.setVisibility(View.GONE);
        linearLayoutFriday.setVisibility(View.GONE);

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

        mCheckBoxArray[0] = findViewById(R.id.act_SWHours_CB_DiffHours);
        mCheckBoxArray[1] = findViewById(R.id.act_SWHours_CB_MondayFree);
        mCheckBoxArray[2] = findViewById(R.id.act_SWHours_CB_TuesdayFree);
        mCheckBoxArray[3] = findViewById(R.id.act_SWHours_CB_WednesdayFree);
        mCheckBoxArray[4] = findViewById(R.id.act_SWHours_CB_ThursdayFree);
        mCheckBoxArray[5] = findViewById(R.id.act_SWHours_CB_FridayFree);
        mCheckBoxArray[6] = findViewById(R.id.act_SWHours_CB_SaturdayFree);
        mCheckBoxArray[7] = findViewById(R.id.act_SWHours_CB_SundayFree);

        mIDsArray.put(R.id.act_SWHours_Spinner_AllDaysEnd, 7);
        mIDsArray.put(R.id.act_SWHours_Spinner_AllDaysStart, 7);
        mIDsArray.put(R.id.act_SWHours_Spinner_MondayStart, 0);
        mIDsArray.put(R.id.act_SWHours_Spinner_MondayEnd, 0);
        mIDsArray.put(R.id.act_SWHours_Spinner_TuesdayStart, 1);
        mIDsArray.put(R.id.act_SWHours_Spinner_TuesdayEnd, 1);
        mIDsArray.put(R.id.act_SWHours_Spinner_WednesdayStart, 2);
        mIDsArray.put(R.id.act_SWHours_Spinner_WednesdayEnd, 2);
        mIDsArray.put(R.id.act_SWHours_Spinner_ThursdayStart, 3);
        mIDsArray.put(R.id.act_SWHours_Spinner_ThursdayEnd, 3);
        mIDsArray.put(R.id.act_SWHours_Spinner_FridayStart, 4);
        mIDsArray.put(R.id.act_SWHours_Spinner_FridayEnd, 4);
        mIDsArray.put(R.id.act_SWHours_Spinner_SaturdayStart, 5);
        mIDsArray.put(R.id.act_SWHours_Spinner_SaturdayEnd, 5);
        mIDsArray.put(R.id.act_SWHours_Spinner_SundayStart, 6);
        mIDsArray.put(R.id.act_SWHours_Spinner_SundayEnd, 6);

        /* initial spinners */

        mSpinnerArray.put("AllDaysStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_AllDaysStart));
        mSpinnerArray.put("AllDaysEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_AllDaysEnd));
        mSpinnerArray.put("MondayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_MondayStart));
        mSpinnerArray.put("MondayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_MondayEnd));
        mSpinnerArray.put("TuesdayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_TuesdayStart));
        mSpinnerArray.put("TuesdayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_TuesdayEnd));
        mSpinnerArray.put("WednesdayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_WednesdayStart));
        mSpinnerArray.put("WednesdayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_WednesdayEnd));
        mSpinnerArray.put("ThursdayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_ThursdayStart));
        mSpinnerArray.put("ThursdayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_ThursdayEnd));
        mSpinnerArray.put("FridayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_FridayStart));
        mSpinnerArray.put("FridayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_FridayEnd));
        mSpinnerArray.put("SaturdayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_SaturdayStart));
        mSpinnerArray.put("SaturdayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_SaturdayEnd));
        mSpinnerArray.put("SundayStart", (Spinner) findViewById(R.id.act_SWHours_Spinner_SundayStart));
        mSpinnerArray.put("SundayEnd", (Spinner) findViewById(R.id.act_SWHours_Spinner_SundayEnd));

        mAdapterHours = ArrayAdapter.createFromResource(this,R.array.hours_array, R.layout.spinner_workinghours);
        mAdapterHours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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

        for(mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
            setCheckChanged(mCheckBoxArray[mDaysIterator + 1], mStartDay[mDaysIterator], mEndDay[mDaysIterator]);
        }

        mCheckBoxArray[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            LinearLayout linearLayoutAllDays = findViewById(R.id.act_SWHours_LL_DaysOfTheWeek);
            LinearLayout linearLayoutSaturday = findViewById(R.id.act_SWHours_LL_Saturday);
            LinearLayout linearLayoutMonday = findViewById(R.id.act_SWHours_LL_Monday);
            LinearLayout linearLayoutTuesday = findViewById(R.id.act_SWHours_LL_Tuesday);
            LinearLayout linearLayoutWednesday = findViewById(R.id.act_SWHours_LL_Wednesday);
            LinearLayout linearLayoutThursday = findViewById(R.id.act_SWHours_LL_Thursday);
            LinearLayout linearLayoutFriday = findViewById(R.id.act_SWHours_LL_Friday);
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    linearLayoutAllDays.setVisibility(View.GONE);
                    linearLayoutMonday.setVisibility(View.VISIBLE);
                    linearLayoutTuesday.setVisibility(View.VISIBLE);
                    linearLayoutWednesday.setVisibility(View.VISIBLE);
                    linearLayoutThursday.setVisibility(View.VISIBLE);
                    linearLayoutFriday.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.act_SWHours_LL_Saturday).getLayoutParams();
                    params.addRule(RelativeLayout.BELOW, R.id.act_SWHours_LL_Friday);
                    linearLayoutSaturday.setLayoutParams(params);
                }
                else {
                    linearLayoutAllDays.setVisibility(View.VISIBLE);
                    linearLayoutMonday.setVisibility(View.GONE);
                    linearLayoutTuesday.setVisibility(View.GONE);
                    linearLayoutWednesday.setVisibility(View.GONE);
                    linearLayoutThursday.setVisibility(View.GONE);
                    linearLayoutFriday.setVisibility(View.GONE);

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.act_SWHours_LL_Saturday).getLayoutParams();
                    params.addRule(RelativeLayout.BELOW, R.id.act_SWHours_LL_DaysOfTheWeek);
                    linearLayoutSaturday.setLayoutParams(params);
                }
            }
        });

    }

    private void getUserWorkingDaysID(String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        mFireStore.collection("users")
                .document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        workingDaysID = document.get("workingDaysID").toString();
                        Log.d("IDwork", workingDaysID);
                    }
                });
    }

    private void addUserDataToDatabase(String userID) {
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> daysToAdd = new HashMap<>();
        if(mCheckBoxArray[0].isChecked()) {
            for (mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
                if(mCheckBoxArray[mDaysIterator + 1].isChecked()) {
                    daysToAdd.put(mStartDay[mDaysIterator], "Free");
                    daysToAdd.put(mEndDay[mDaysIterator], "Free");
                }
                else {
                    daysToAdd.put(mStartDay[mDaysIterator], mStartHours[mDaysIterator]);
                    daysToAdd.put(mEndDay[mDaysIterator], mEndHours[mDaysIterator]);
                }
            }
        }
        else {
            for (mDaysIterator = 0; mDaysIterator < 7; mDaysIterator++) {
                if(mDaysIterator < 5) {
                    daysToAdd.put(mStartDay[mDaysIterator], mStartHours[7]);
                    daysToAdd.put(mEndDay[mDaysIterator], mEndHours[7]);
                }
                else {
                    if(mCheckBoxArray[mDaysIterator + 1].isChecked()) {
                        daysToAdd.put(mStartDay[mDaysIterator], "Free");
                        daysToAdd.put(mEndDay[mDaysIterator], "Free");
                    }
                    else {
                        daysToAdd.put(mStartDay[mDaysIterator], mStartHours[mDaysIterator]);
                        daysToAdd.put(mEndDay[mDaysIterator], mEndHours[mDaysIterator]);
                    }
                }
            }
        }

        mFireStore.collection("workingDays")
                .document(workingDaysID)
                .update(daysToAdd)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
        Intent calendarIntent = new Intent(SetWorkingHoursActivity.this, CalendarActivity.class);
        calendarIntent.putExtra("userID", userID);
        startActivityForResult(calendarIntent, SWH_CANCEL);
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
}

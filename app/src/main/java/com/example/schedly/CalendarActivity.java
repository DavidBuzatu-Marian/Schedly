package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;

import com.example.schedly.adapter.CalendarAdapter;
import com.example.schedly.model.Appointment;
import com.example.schedly.model.Day;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;

public class CalendarActivity extends AppCompatActivity {

    private String ERR = "ERRORS";
    private String userDaysWithScheduleID;
    private String userID;
    private String currentDayID;
    private GoogleSignInClient mGoogleSignInClient;
    private CalendarView mCalendarView;
    private Long mDate = 0L;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private int mCounter = 0;
    private ArrayList<Appointment> mDataSet = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
        }
        Log.d("ID_CAL", userID);

        mCalendarView = findViewById(R.id.act_Calendar_CalendarV);
        if(mDate == 0L) {
            getDateFromCalendarView(0, 0, 0, true);
            Log.d("DATE", mDate + "");
        }

        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                getDateFromCalendarView(year, month, dayOfMonth, false);
                Log.d("DATE", mDate + "");
            }
        });

        mRecyclerView = findViewById(R.id.act_Calendar_RV_Schedule);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new CalendarAdapter(this, mDataSet);
        mRecyclerView.setAdapter(mAdapter);




        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_ID))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button buttonSingOut = findViewById(R.id.act_Calendar_BUT_SignOut);
        buttonSingOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleSignInClient.signOut();
                LoginManager.getInstance().logOut();
                FirebaseAuth.getInstance().signOut();
                CalendarActivity.this.finish();
            }
        });
    }

    private void getDateFromCalendarView(int year, int month, int dayOfMonth, boolean onStart) {
        Calendar _calendar = Calendar.getInstance();
        if(onStart) {
            _calendar.setTimeInMillis(mCalendarView.getDate());
        }
        else {
            _calendar.set(year, month, dayOfMonth);
        }
        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        _calendar.set(Calendar.SECOND, 0);

        mDate = _calendar.getTimeInMillis();
        getAppointmentsForSelectedDate();
    }

    private void getAppointmentsForSelectedDate() {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = _FireStore.collection("users").document(userID);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    userDaysWithScheduleID = document.get("daysWithSchedule").toString() != null ? document.get("daysWithSchedule").toString() : null;

                } else {
                    Log.d(ERR, "get failed with ", task.getException());
                }

                if(userDaysWithScheduleID != null) {
                    getDayID();
                }
            }
        });
    }

    private void getDayID() {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        _FireStore.collection("daysWithSchedule")
                .document(userDaysWithScheduleID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        currentDayID = documentSnapshot.get(mDate.toString()).toString() != null ? documentSnapshot.get(mDate.toString()).toString() : null;
                        getEachAppointment();
                    }
                });
    }

    private void getEachAppointment() {
        Day _currentDay = new Day();
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        _FireStore.collection("daysWithSchedule")
                .document(userDaysWithScheduleID)
                .collection("scheduledHours")
                .whereEqualTo(currentDayID, true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                mDataSet.add(mCounter, new Appointment(document.getData().toString()));
                                mCounter++;
                            }
                        } else {
                            Log.d(ERR, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    /* This will be used on a button
    in order to add manually an appointment
     */
    private void onAddAppointment(View view) {

    }
}

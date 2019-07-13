package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.schedly.adapter.CalendarAdapter;
import com.example.schedly.model.Appointment;
import com.example.schedly.model.Day;
import com.example.schedly.model.MessageListener;
import com.example.schedly.model.SMSBroadcastReceiver;
import com.example.schedly.service.MonitorIncomingSMSService;
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
import com.google.firebase.firestore.Source;
import com.google.firestore.v1.FirestoreGrpc;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.schedly.MainActivity.EMAIL_CHANGED;
import static com.example.schedly.MainActivity.PASSWORD_CHANGED;

public class CalendarActivity extends AppCompatActivity {

    private String ERR = "ERRORS";
    private final int SMS_PERMISSION_CODE = 9000;
    public static final int LOG_OUT = 4001;
    public static final int SETTINGS_RETURN = 4000;
    private String userDaysWithScheduleID;
    private String userID;
    private String currentDayID;
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

        if(!isSmsPermissionGranted()) {
            showRequestPermissionsInfoAlertDialog();
        }
        /* broadcast test */
        startService(new Intent(this, MonitorIncomingSMSService.class));

//        SMSBroadcastReceiver.bindListener(this);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
        }
        Log.d("ID_CAL", userID);

        /* get appointments for current day and selected day */
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

        /* RecyclerView */
        mRecyclerView = findViewById(R.id.act_Calendar_RV_Schedule);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CalendarAdapter(CalendarActivity.this, mDataSet);
        mRecyclerView.setAdapter(mAdapter);

        ImageView imageViewSettings = findViewById(R.id.act_Calendar_IV_Settings);
        imageViewSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startSettingsActivity = new Intent(CalendarActivity.this, SettingsActivity.class);
                startActivityForResult(startSettingsActivity, SETTINGS_RETURN);
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
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
        Log.d("Date", mDate + "");
        mDataSet.clear();
        mCounter = 0;
        getAppointmentsForSelectedDate();
    }

    private void getAppointmentsForSelectedDate() {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        DocumentReference _documentReference = _FireStore.collection("users").document(userID);
        _documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot _document = task.getResult();
                    userDaysWithScheduleID = _document.get("daysWithScheduleID") != null ? _document.get("daysWithScheduleID").toString() : null;

                } else {
                    userDaysWithScheduleID = null;
                    Log.d(ERR, "get failed with ", task.getException());
                }

                if(userDaysWithScheduleID != null) {
                    Log.d("Appointments", "good" + userDaysWithScheduleID +"");
                    getDayID();
                }
            }
        });
    }

    private void getDayID() {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
                .document(userDaysWithScheduleID);
        _documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()) {
                            DocumentSnapshot _document = task.getResult();
                            currentDayID = _document.get(mDate.toString()) != null ? _document.get(mDate.toString()).toString() : null;
                            if(currentDayID != null) {
                                getEachAppointment();
                            }
                            else {
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    private void getEachAppointment() {
        Day _currentDay = new Day();
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();

        final DocumentReference _documentReference =  _FireStore.collection("daysWithSchedule")
                .document(userDaysWithScheduleID)
                .collection("scheduledHours")
                .document(currentDayID);
        _documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d("Appo", _documentReference.getPath());
                            Map<String, Object> _map = task.getResult().getData();
                            for (Map.Entry<String, Object> _entry : _map.entrySet()) {
                                Log.d("Appointment", _entry.getKey());
                                Log.d("Appointment", _entry.getValue().toString());

                                Integer _length = _entry.getValue().toString().length();
                                String _data = _entry.getValue().toString().substring(1, _length - 1);

                                Gson gson = new Gson();
                                String json = gson.toJson(_entry.getValue());
                                Log.d("APPP", json);


                                mDataSet.add(mCounter, new Appointment(_entry.getKey(), gson, json));
                                mCounter++;
                            }
                        } else {
                            Log.d(ERR, "Error getting documents: ", task.getException());
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    /* This will be used on a button
    in order to add manually an appointment
     */

//    private void populateWithData() {
//        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
//        final DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
//                .document("gXD1RxgPEFQYEbwXlisy")
//                .collection("scheduledHours")
//                .document("00JQopKY8fElhI9i04yg");
//        Map<String, Object> _appointments = new HashMap<>(16);
//        Map<String, String> _values = new HashMap<>(16);
//        for(int hours = 8; hours < 22; hours++) {
//            _values.put("PhoneNumber", "+40" + ThreadLocalRandom.current().nextLong(1000000000L, 10000000000L) + "");
//            _appointments.put("" + hours + ":00",  _values);
//        }
//
//        _documentReference.update(_appointments);
//
//    }


    public void showRequestPermissionsInfoAlertDialog() {
        showRequestPermissionsInfoAlertDialog(true);
    }

    public void showRequestPermissionsInfoAlertDialog(final boolean makeSystemRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_alert_dialog_SMS); // Your own title
        builder.setMessage(R.string.permission_dialog_SMS_body); // Your own message

        builder.setPositiveButton(R.string.permission_alert_dialog_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Display system runtime permission request?
                if (makeSystemRequest) {
                    requestReadAndSendSmsPermission();
                }
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    public boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(CalendarActivity.this, "NOT PERMITTED", Toast.LENGTH_SHORT);
                }
                break;
            }
        }
    }

    private void onAddAppointment(View view) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == LOG_OUT) {
            finish();
        }
        if(resultCode == EMAIL_CHANGED) {
            setResult(EMAIL_CHANGED);
            finish();
        }
        if(resultCode == PASSWORD_CHANGED) {
            setResult(PASSWORD_CHANGED);
            finish();
        }
    }
}

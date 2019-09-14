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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import com.example.schedly.adapter.CalendarAdapter;
import com.example.schedly.model.Appointment;
import com.example.schedly.packet_classes.PacketCalendar;
import com.example.schedly.packet_classes.PacketCalendarHelpers;
import com.example.schedly.packet_classes.PacketService;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.example.schedly.MainActivity.EMAIL_CHANGED;
import static com.example.schedly.MainActivity.PASSWORD_CHANGED;
import static com.example.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class CalendarActivity extends AppCompatActivity {

    private String ERR = "ERRORS";
    private final int SMS_PERMISSION_CODE = 9000;
    private final int CONTACTS_PERMISSION_CODE = 9001;
    public static final int LOG_OUT = 4001;
    public static final int SETTINGS_RETURN = 4000;
    private String userID;
    private String mUserWorkingHoursID;
    private CalendarView mCalendarView;
    private Long mDate = 0L;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private int mCounter = 0;
    private String mUserAppointmentDuration;
    private ArrayList<Appointment> mDataSet = new ArrayList<>();
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private PacketCalendar mPacketCalendar;
    private String mCompleteDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        /* permisions */
        if (!isSmsPermissionGranted()) {
            showRequestPermissionsInfoAlertDialog("SMS");
        }
        if (!isContactPermissionGranted()) {
            showRequestPermissionsInfoAlertDialog("CONTACTS");
        }
        if (!isLogPermissionGranted()) {
            showRequestPermissionsInfoAlertDialog("LOG");
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userID = extras.getString("userID");
            mWorkingHours = (HashMap<String, String>) extras.getSerializable("userWorkingHours");
            mUserWorkingHoursID = extras.getString("userWorkingHoursID");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
        }


//        testDB();
        /* dipslay Helpers */
        final PacketCalendarHelpers _PCH = new PacketCalendarHelpers(CalendarActivity.this);
        _PCH.displayHelpers();

        mCalendarView = findViewById(R.id.act_Calendar_CalendarV);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                PacketService _psTest = new PacketService(userID, mUserAppointmentDuration, mUserWorkingHoursID);
                _psTest.setUserWorkingHours(mWorkingHours);
                _psTest.getScheduledDays("12:00", "0724154387", "TIME");

                _PCH.displayHelpOnDate(view);
                Log.d("DATEEE", view.getId() + "; " + mCalendarView.getId());
                getDateFromCalendarView(year, month, dayOfMonth, false);
                Log.d("DATE", mDate + "");
            }
        });

        if (mDate == 0L) {
            getDateFromCalendarView(0, 0, 0, true);
            Log.d("DATE", mDate + "");
        }
        startServiceSMSMonitoring();
        setRecyclerView();

    }

    private void setRecyclerView() {
        /* RecyclerView */
        mRecyclerView = findViewById(R.id.act_Calendar_RV_Schedule);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CalendarAdapter(CalendarActivity.this, mDataSet, userID);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void getDateFromCalendarView(int year, int month, int dayOfMonth, boolean onStart) {
        Calendar _calendar = Calendar.getInstance();
        if (onStart) {
            _calendar.setTimeInMillis(mCalendarView.getDate());
        } else {
            _calendar.set(year, month, dayOfMonth);
        }
        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        _calendar.set(Calendar.SECOND, 0);

        SimpleDateFormat _SDF = new SimpleDateFormat("yyyy-MM-dd");
        mCompleteDate = _SDF.format(_calendar.getTime());
        mDate = _calendar.getTimeInMillis();

        mPacketCalendar = new PacketCalendar(this, mWorkingHours, mUserAppointmentDuration, userID);
        mPacketCalendar.setDateForTVs(year, month, dayOfMonth, mDate, mCompleteDate);


        Log.d("Date", mDate + "");
        mDataSet.clear();
        mCounter = 0;
//        getDayID();
        getEachAppointments();
    }

    public void startServiceSMSMonitoring() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (_userPreferences.getBoolean("serviceActive", true)) {
            Intent serviceIntent = new Intent(CalendarActivity.this, MonitorIncomingSMSService.class);
            serviceIntent.putExtra("userID", userID);
            serviceIntent.putExtra("userAppointmentDuration", mUserAppointmentDuration);
            serviceIntent.putExtra("userWorkingDaysID", mUserWorkingHoursID);
            serviceIntent.putExtra("userWorkingHours", mWorkingHours);
            serviceIntent.setAction("ACTION.STARTSERVICE_ACTION");
            startService(serviceIntent);
        }

        ImageView imageViewSettings = findViewById(R.id.act_Calendar_IV_Settings);
        imageViewSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startSettingsActivity = new Intent(CalendarActivity.this, SettingsActivity.class);
                startSettingsActivity.putExtra("userID", userID);
                startSettingsActivity.putExtra("userAppointmentDuration", mUserAppointmentDuration);
                startSettingsActivity.putExtra("userWorkingDaysID", mUserWorkingHoursID);
                startActivityForResult(startSettingsActivity, SETTINGS_RETURN);
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }


    private void getEachAppointments() {
        /* check if date has appointments */
//        final AtomicBoolean _dateFoundTrue = new AtomicBoolean(false);
        FirebaseFirestore.getInstance().collection("scheduledHours")
                .document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()) {
                            Map<String, Object> _map = task.getResult().getData();
                            assert _map != null;
                            Object _values = _map.containsKey(mDate.toString()) ? _map.get(mDate.toString()) : null;
                            if (_values != null) {
                                Log.d("Day", _values.toString());
                                Gson _gson = new Gson();
                                String _json = _gson.toJson(_values);
                                try {
                                    Map<String, Object> result = new ObjectMapper().readValue(_json, Map.class);
                                    Map<String, Object> _treeMap = new TreeMap<>(result);
                                    for (Map.Entry<String, Object> _schedule : _treeMap.entrySet()) {
                                        Log.d("Day", "Hour is: " + _schedule.getKey());
                                        String jjson = _gson.toJson(_schedule.getValue());
                                        Log.d("Day", "Info are: " + jjson);
                                        mDataSet.add(mCounter++, new Appointment(_schedule.getKey(), _gson, jjson, mCompleteDate, mDate));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.d("Day", "Day json is: " + _json);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                        setParamsForPacketClass();
                    }
                });
    }
    private void setParamsForPacketClass() {
        mPacketCalendar.setAdapter(mAdapter);
        mPacketCalendar.setDataSet(mDataSet);
        mPacketCalendar.setCounter(mCounter);
    }

    public int getCounter() {
        return mCounter;
    }

    public void setCounter(int counter) {
        mCounter = counter;
    }

    /* *************** PERMISSIONS ***************** */

    public void showRequestPermissionsInfoAlertDialog(String type) {
        if (type.equals("SMS")) {
            showRequestPermissionsInfoAlertDialog(true);
        } else if (type.equals("CONTACTS")) {
            showRequestPermissionsInfoAlertDialogContacts(true);
        } else {
            showRequestPermissionsInfoAlertDialogLog(true);
        }
    }

    private void showRequestPermissionsInfoAlertDialogContacts(final boolean makeSystemRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_alert_dialog_CONTACTS); // title
        builder.setMessage(R.string.permission_dialog_CONTACTS_body); // message

        builder.setPositiveButton(R.string.permission_alert_dialog_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Display system runtime permission request?
                if (makeSystemRequest) {
                    requestReadContactsPermission();
                }
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    public void showRequestPermissionsInfoAlertDialog(final boolean makeSystemRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_alert_dialog_SMS); // title
        builder.setMessage(R.string.permission_dialog_SMS_body); // message

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

    public void showRequestPermissionsInfoAlertDialogLog(final boolean makeSystemRequest) {
        requestReadLogPermission();
    }

    public boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isContactPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLogPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadLogPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALL_LOG)) {
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, CONTACTS_PERMISSION_CODE);
    }

    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    private void requestReadContactsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_CODE);
    }

    private void closeUponPermissionDenied(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
            finishAffinity();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_CODE: {
                closeUponPermissionDenied(grantResults);
                break;
            }
            case CONTACTS_PERMISSION_CODE: {
                closeUponPermissionDenied(grantResults);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("Result", resultCode + ":");
        switch(resultCode) {
            case LOG_OUT:
                setResult(LOG_OUT);
                finish();
                break;
            case EMAIL_CHANGED:
                setResult(EMAIL_CHANGED);
                finish();
                break;
            case PASSWORD_CHANGED:
                setResult(PASSWORD_CHANGED);
                finish();
                break;
            case WORKING_HOURS_CHANGED:
                setResult(WORKING_HOURS_CHANGED);
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishAffinity();
        finish();
    }
}

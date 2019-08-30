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
import android.widget.Toast;

import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder;
import com.example.schedly.adapter.CalendarAdapter;
import com.example.schedly.model.Appointment;
import com.example.schedly.packet_classes.PacketCalendar;
import com.example.schedly.packet_classes.PacketCalendarHelpers;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

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
    private String mUserDaysWithScheduleID;
    private String userID;
    private String currentDayID;
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
            mUserDaysWithScheduleID = extras.getString("userDaysWithScheduleID");
            mUserWorkingHoursID = extras.getString("userWorkingHoursID");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
        }

        /* dipslay Helpers */
        final PacketCalendarHelpers _PCH = new PacketCalendarHelpers(CalendarActivity.this);
        _PCH.displayHelpers();

        /* init calendar and get ID for days
         * with schedule
         */
        mCalendarView = findViewById(R.id.act_Calendar_CalendarV);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
//                PacketService _psTest = new PacketService(userID, mUserAppointmentDuration, mUserDaysWithScheduleID, mUserWorkingHoursID);
//                _psTest.setUserWorkingHours(mWorkingHours);
//                _psTest.makeAppointmentForFixedParameters("2019-09-22", "13:45", "0734543831", "Princess");
                _PCH.displayHelpOnDate(view);
                getDateFromCalendarView(year, month, dayOfMonth, false);
                Log.d("DATE", mDate + "");
            }
        });

        setUserDaysWithScheduleIDWrapper();
        setRecyclerView();

    }

    private void setRecyclerView() {
        /* RecyclerView */
        mRecyclerView = findViewById(R.id.act_Calendar_RV_Schedule);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CalendarAdapter(CalendarActivity.this, mDataSet);
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

        mPacketCalendar = new PacketCalendar(this, mWorkingHours, mUserDaysWithScheduleID, mUserAppointmentDuration);
        mPacketCalendar.setDateForTVs(year, month, dayOfMonth, mDate, mCompleteDate);


        Log.d("Date", mDate + "");
        mDataSet.clear();
        mCounter = 0;
        getDayID();
    }


    private void setUserDaysWithScheduleIDWrapper() {
        if (mUserDaysWithScheduleID == null) {
            setUserDaysWithScheduleID();
        } else {
            if (mDate == 0L) {
                getDateFromCalendarView(0, 0, 0, true);
                Log.d("DATE", mDate + "");
            }
            startServiceSMSMonitoring();
        }
    }

    public void startServiceSMSMonitoring() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (_userPreferences.getBoolean("serviceActive", true)) {
            Intent serviceIntent = new Intent(CalendarActivity.this, MonitorIncomingSMSService.class);
            serviceIntent.putExtra("userID", userID);
            serviceIntent.putExtra("userDaysWithScheduleID", mUserDaysWithScheduleID);
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
                startSettingsActivity.putExtra("userDaysWithScheduleID", mUserDaysWithScheduleID);
                startSettingsActivity.putExtra("userAppointmentDuration", mUserAppointmentDuration);
                startSettingsActivity.putExtra("userWorkingDaysID", mUserWorkingHoursID);
                startActivityForResult(startSettingsActivity, SETTINGS_RETURN);
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }


    private void setUserDaysWithScheduleID() {
        final FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        Map<String, Object> addDaysWithScheduleID = new HashMap<>();
        addDaysWithScheduleID.put("0", "0");
        mFireStore.collection("daysWithSchedule")
                .add(addDaysWithScheduleID)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        mUserDaysWithScheduleID = documentReference.getId();
                        updateUserDaysID(mFireStore);
                    }
                });
    }

    private void addScheduledHoursCollection(final FirebaseFirestore mFireStore) {
        getDateFromCalendarView(0, 0, 0, true);
        Map<String, Object> addDaysWithScheduleID = new HashMap<>();
        addDaysWithScheduleID.put("5:00", null);
        Log.d("Firebasee", mDate.toString());
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .add(addDaysWithScheduleID)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        addThisDateScheduledHoursID(mFireStore, documentReference.getId());
                        Log.d("Firebase-workingDays", "Succes with scheduled hours");
                    }
                });
    }

    private void addThisDateScheduledHoursID(FirebaseFirestore mFireStore, String id) {
        Map<String, Object> addToCurrentDateID = new HashMap<>();
        addToCurrentDateID.put(mDate.toString(), id);
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .update(addToCurrentDateID)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firebase-workingDays", "Succes with id");
                    }
                });
    }

    private void updateUserDaysID(final FirebaseFirestore mFireStore) {
        Map<String, Object> addUserDaysID = new HashMap<>();
        addUserDaysID.put("daysWithScheduleID", mUserDaysWithScheduleID);
        mFireStore.collection("users")
                .document(userID)
                .update(addUserDaysID)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (mDate == 0L) {
                            addScheduledHoursCollection(mFireStore);
                            Log.d("DATE", mDate + "");
                        }
                        startServiceSMSMonitoring();
                        Log.d("Change", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Change", "Error writing document", e);
                    }
                });
    }

    private void getDayID() {
        final FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();

        DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID);
        _documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot _document = task.getResult();
                    currentDayID = _document.get(mDate.toString()) != null ? _document.get(mDate.toString()).toString() : null;
                    if (currentDayID != null) {
                        getEachAppointment();
                    } else {
                        setParamsForPacketClass();
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    private void getEachAppointment() {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();

        final DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .document(currentDayID);
        _documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d("Appo", _documentReference.getPath());
                    Map<String, Object> _map = task.getResult().getData();

                    Map<String, Object> _treeMap = new TreeMap<>(_map);


                    Log.d("Calendar", _map.toString());
                    for (Map.Entry<String, Object> _entry : _treeMap.entrySet()) {
                        Log.d("Appointment", _entry.getKey());
                        if (_entry.getValue() == null) {
                            break;
                        }
                        Gson gson = new Gson();
                        String json = gson.toJson(_entry.getValue());
                        Log.d("APPP", json);

                        mDataSet.add(mCounter++, new Appointment(_entry.getKey(), gson, json, currentDayID, mUserDaysWithScheduleID, mCompleteDate));
                    }
                } else {
                    Log.d(ERR, "Error getting documents: ", task.getException());
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

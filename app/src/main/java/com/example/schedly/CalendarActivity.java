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
import android.annotation.SuppressLint;
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
import com.example.schedly.model.CustomCalendarView;
import com.example.schedly.model.CustomEvent;
import com.example.schedly.packet_classes.PacketCalendar;
import com.example.schedly.packet_classes.PacketCalendarHelpers;
import com.example.schedly.packet_classes.PacketService;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.YearMonth;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
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
    private CustomCalendarView mCalendarView;
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
    private Map<String, Object> mAppointmentsForThisMonth;
    private ListenerRegistration mRegistration;


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

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("OnResume", "Resumed");
        /* dipslay Helpers */
        final PacketCalendarHelpers _PCH = new PacketCalendarHelpers(CalendarActivity.this);
        _PCH.displayHelpers();

        mPacketCalendar = new PacketCalendar(this, mWorkingHours, mUserAppointmentDuration, userID);
        mCalendarView = findViewById(R.id.act_Calendar_CalendarV);
        mCalendarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //PacketService _psTest = new PacketService(userID, mUserAppointmentDuration, mUserWorkingHoursID);
                //_psTest.setUserWorkingHours(mWorkingHours);
                //_psTest.getScheduledDays("12:00", "0724154387", "TIME");
                Calendar _calendar = mCalendarView.getMarkedDay();
                _PCH.displayHelpOnDate(mCalendarView);
                getDateFromCalendarView(_calendar);
                Log.d("DATE", mDate + "");
            }
        });

        startServiceSMSMonitoring();
        setRecyclerView();
        monitorChanges();
    }

    @Override
    protected void onStop() {
        super.onStop();
        /* remove registration on stop in order to
         * moderate power and resources consumption
         */
        mRegistration.remove();
    }

    private void setCalendarContent() {
        long _startMonth = YearMonth.from(LocalDate.now()).atDay(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        long _endMonth = YearMonth.from(LocalDate.now()).atEndOfMonth().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        if(mAppointmentsForThisMonth != null) {
            setEvents(_startMonth, _endMonth);
        } else {
            mCalendarView.updateCalendar(null);
        }

        ImageView mBUTPrev = findViewById(R.id.calendar_prev_button);
        ImageView mBUTNext = findViewById(R.id.calendar_next_button);

        mBUTPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalDate _dateNow = mCalendarView.getDate();
                _dateNow = _dateNow.minusMonths(1);
                onButtonsClick(_dateNow);
            }
        });
        mBUTNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalDate _dateNow = mCalendarView.getDate();
                _dateNow = _dateNow.plusMonths(1);
                onButtonsClick(_dateNow);
            }
        });
    }

    
    private void onButtonsClick(LocalDate dateNow) {
        mCalendarView.setDate(dateNow);
        long _startMonth = YearMonth.from(dateNow).atDay(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        long _endMonth = YearMonth.from(dateNow).atEndOfMonth().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        if(mAppointmentsForThisMonth != null) {
            setEvents(_startMonth, _endMonth);
        } else {
            mCalendarView.updateCalendar(null );
        }
    }
    private void setEvents(long startMonth, long endMonth) {
        long _numberOfAppointments;
        CustomEvent.setUserAppointmentDuration(Long.parseLong(mUserAppointmentDuration));
        @SuppressLint("UseSparseArrays") final HashMap<Long, CustomEvent> _events = new HashMap<>();
        for(Map.Entry<String, Object> _appointment : mAppointmentsForThisMonth.entrySet()) {
            long _dateInMillis = Long.parseLong(_appointment.getKey());
            if(_dateInMillis >= startMonth && _dateInMillis <= endMonth && hasValue(_appointment.getValue())) {
                DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.US);
                LocalDate _date = Instant.ofEpochMilli(_dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate();
                String _dayOfWeek = _date.format(_DTF);

                CustomEvent _CEvent = new CustomEvent(_dateInMillis);
                _numberOfAppointments = countAppointmentsForThisDay(_dateInMillis);
                _CEvent.setUserNumberOfAppointments(_numberOfAppointments);
                String[] _timeStart = mWorkingHours.get(_dayOfWeek + "Start").split(":");
                String[] _timeEnd = mWorkingHours.get(_dayOfWeek + "End").split(":");
                LocalDateTime _dateTimeStart = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDate().atTime(Integer.parseInt(_timeStart[0]), Integer.parseInt(_timeStart[1]));
                LocalDateTime _dateTimeEnd = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDate().atTime(Integer.parseInt(_timeEnd[0]), Integer.parseInt(_timeEnd[1]));
                Long _timeStartMillis = _dateTimeStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                Long _timeEndMillis = _dateTimeEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                Log.d("DATES",  _timeStartMillis + "; " + _timeEndMillis);
                _CEvent.setStartHour(_timeStartMillis);
                _CEvent.setEndHour(_timeEndMillis);
                _events.put(_dateInMillis, _CEvent);
            }
        }
        mCalendarView.updateCalendar(_events);
    }

    private boolean hasValue(Object value) {
        Gson _gson = new Gson();
        String _json = _gson.toJson(value);
        /* not empty. Empty means "{}" */
        return _json.length() > 2;
    }

    private long countAppointmentsForThisDay(Long dateInMillis) {
        long numberOfAppointments = 0;
        assert mAppointmentsForThisMonth != null;
        Object _values = mAppointmentsForThisMonth.containsKey(dateInMillis.toString()) ? mAppointmentsForThisMonth.get(dateInMillis.toString()) : null;
        if (_values != null) {
            Log.d("Day", _values.toString());
            Gson _gson = new Gson();
            String _json = _gson.toJson(_values);
            try {
                Map<String, Object> result = new ObjectMapper().readValue(_json, Map.class);
                numberOfAppointments = result.size();
                Log.d("Number is: ", numberOfAppointments + "");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            Log.d("Day", "Day json is: " + _json);
        }
        return numberOfAppointments;
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

    private void getDateFromCalendarView(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);

        SimpleDateFormat _SDF = new SimpleDateFormat("yyyy-MM-dd");
        mCompleteDate = _SDF.format(calendar.getTime());
        mDate = calendar.getTimeInMillis();

        mPacketCalendar.setDateForTVs(calendar, mDate, mCompleteDate);


        Log.d("Date", mDate + "");
//        getDayID();
        if(mAppointmentsForThisMonth != null) {
            getEachAppointments();
        }
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


    private void monitorChanges() {
        final DocumentReference docRef = FirebaseFirestore.getInstance().collection("scheduledHours").document(userID);
        mRegistration = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ERR", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    mAppointmentsForThisMonth = snapshot.getData();
                    Log.d("TESTDB", "Current data: " + snapshot.getData());
                    if (mDate == 0L) {
                        Calendar _calendar = Calendar.getInstance();
                        _calendar.setTimeInMillis(mCalendarView.getDate().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
                        getDateFromCalendarView(_calendar);
                        Log.d("DATE", mDate + "");
                    } else {
                        getEachAppointments();
                    }
                } else {
                    Log.d("TESTDB", "Current data: null");
                    Calendar _calendar = Calendar.getInstance();
                    _calendar.setTimeInMillis(mCalendarView.getDate().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
                    getDateFromCalendarView(_calendar);
                    setCalendarContent();
                }
            }
        });
    }

    private void getEachAppointments() {
        /* check if date has appointments */
//        final AtomicBoolean _dateFoundTrue = new AtomicBoolean(false);
        mDataSet.clear();
        mCounter = 0;
        Object _values = mAppointmentsForThisMonth.containsKey(mDate.toString()) ? mAppointmentsForThisMonth.get(mDate.toString()) : null;
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
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            Log.d("Day", "Day json is: " + _json);
        }
        mAdapter.notifyDataSetChanged();
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
        switch (resultCode) {
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

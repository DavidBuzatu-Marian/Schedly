package com.davidbuzatu.schedly;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.davidbuzatu.schedly.adapter.CalendarAdapter;
import com.davidbuzatu.schedly.adapter.HelperPagerAdapter;
import com.davidbuzatu.schedly.model.Appointment;
import com.davidbuzatu.schedly.model.CustomCalendarView;
import com.davidbuzatu.schedly.model.CustomEvent;
import com.davidbuzatu.schedly.packet_classes.PacketCalendar;
import com.davidbuzatu.schedly.service.MonitorIncomingSMSService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
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

import static com.davidbuzatu.schedly.MainActivity.EMAIL_CHANGED;
import static com.davidbuzatu.schedly.MainActivity.PASSWORD_CHANGED;
import static com.davidbuzatu.schedly.MainActivity.WORKING_HOURS_CHANGED;

public class CalendarActivity extends AppCompatActivity {
    private final String TAG = "CalendarActivity";
    private final int SMS_PERMISSION_CODE = 9000;
    private final int CONTACTS_PERMISSION_CODE = 9001;
    private final int CALL_LOG_PERMISSION_CODE = 9002;
    public static final int LOG_OUT = 4001;
    public static final int SETTINGS_RETURN = 4000;
    private String mUserID;
    private CustomCalendarView mCalendarView;
    private Long mDate = 0L;
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
    private boolean mArePermissionAccepted = true;
    private boolean[] mPermissions = new boolean[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        checkEachPermission();
        getExtrasValues();
    }

    private void checkFirstLogin() {
        if(isFirstLogin()) {
            showHelperDialog();
            //setPreference(true);
        }
    }

    private void getExtrasValues() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUserID = extras.getString("userID");
            mWorkingHours = (HashMap<String, String>) extras.getSerializable("userWorkingHours");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
        }
    }

    private void checkEachPermission() {
        if (!isSmsPermissionGranted()) {
            mArePermissionAccepted = false;
            showRequestPermissionsInfoAlertDialog("SMS");
        }
        if (!isContactPermissionGranted()) {
            mArePermissionAccepted = false;
            showRequestPermissionsInfoAlertDialog("CONTACTS");
        }
        if (!isLogPermissionGranted()) {
            mArePermissionAccepted = false;
            showRequestPermissionsInfoAlertDialog("LOG");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mArePermissionAccepted) {
            setUpUI();
        }
        checkFirstLogin();
//        findViewById(R.id.act_Calendar_BUT_test).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                testMessages();
//            }
//        });
    }

    private void setPreference(boolean b) {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor _userEditor = _userPreferences.edit();
        _userEditor.putBoolean("firstLogin", b);
        _userEditor.apply();
    }

    private void showHelperDialog() {
        final Dialog _dialog = new Dialog(this);
        _dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        _dialog.setContentView(R.layout.dialog_first_login);
        _dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        _dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        HelperPagerAdapter _adapter = new HelperPagerAdapter(this);

        setPagerTabs(_dialog, _adapter);
        _dialog.show();
        _adapter.setDialog(_dialog);
    }

    private void setPagerTabs(Dialog dialog, HelperPagerAdapter adapter) {
        ViewPager _pager = dialog.findViewById(R.id.dialog_FLogin_VP);
        TabLayout _tabLayout = dialog.findViewById(R.id.dialog_FLogin_TL);
        _tabLayout.setupWithViewPager(_pager, true);
        _pager.setAdapter(adapter);
    }

    private boolean isFirstLogin() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return _userPreferences.getBoolean("firstLogin", false);
    }

    private void setUpUI() {
        mPacketCalendar = new PacketCalendar(this, mWorkingHours, mUserAppointmentDuration, mUserID);
        setCalendarViewOnClick();
        startServiceSMSMonitoring();
        setUpSettings();
        setRecyclerView();
        monitorChanges();
    }

    private void setCalendarViewOnClick() {
        mCalendarView = findViewById(R.id.act_Calendar_CalendarV);
        mCalendarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar _calendar = mCalendarView.getMarkedDay();
                getDateFromCalendarView(_calendar);
            }
        });
    }
//
//    private void testMessages() {
//        Log.d("Test", "Pressed");
//        MonitorIncomingSMSService _testMonitor = new MonitorIncomingSMSService();
//        StringBuilder _smsBody = new StringBuilder("Hello! I would like an appointment at 9:00AM");
//        TSMSMessage _testMessage = new TSMSMessage(_smsBody, "0724154387", 1570286399839L);
//        _testMonitor.setParams(this, mUserID, mUserAppointmentDuration, mWorkingHours, mAppointmentsForThisMonth);
//
//        _testMonitor.messageReceived(_testMessage);
//    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mRegistration != null) {
            mRegistration.remove();
        }
    }

    private void setCalendarContent() {
        LocalDate _month = Instant.ofEpochMilli(mDate).atZone(ZoneId.systemDefault()).toLocalDate();
        long _startMonth = YearMonth.from(_month).atDay(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        long _endMonth = YearMonth.from(_month).atEndOfMonth().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        if (mAppointmentsForThisMonth != null) {
            setEvents(_startMonth, _endMonth);
        } else {
            mCalendarView.updateCalendar(null);
        }
        setUpNavigationCalendarBUT();
    }

    private void setUpNavigationCalendarBUT() {
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
        if (mAppointmentsForThisMonth != null) {
            setEvents(_startMonth, _endMonth);
        } else {
            mCalendarView.updateCalendar(null);
        }
    }

    private void setEvents(long startMonth, long endMonth) {
        CustomEvent.setUserAppointmentDuration(Long.parseLong(mUserAppointmentDuration));
        @SuppressLint("UseSparseArrays") final HashMap<Long, CustomEvent> _events = new HashMap<>();
        for (Map.Entry<String, Object> _appointment : mAppointmentsForThisMonth.entrySet()) {
            long _dateInMillis = Long.parseLong(_appointment.getKey());
            if (_dateInMillis >= startMonth && _dateInMillis <= endMonth && hasValue(_appointment.getValue())) {
                DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.US);
                LocalDate _date = Instant.ofEpochMilli(_dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate();
                String _dayOfWeek = _date.format(_DTF);
                CustomEvent _CEvent = new CustomEvent(_dateInMillis);
                if(!mWorkingHours.get(_dayOfWeek + "Start").equals("Free")) {
                    _events.put(_dateInMillis, putDateInEvents(_CEvent, _dateInMillis, _dayOfWeek));
                }
            }
        }
        mCalendarView.updateCalendar(_events);
    }

    private CustomEvent putDateInEvents(CustomEvent _CEvent, long _dateInMillis, String _dayOfWeek) {
        long _numberOfAppointments;
        _numberOfAppointments = countAppointmentsForThisDay(_dateInMillis);
        _CEvent.setUserNumberOfAppointments(_numberOfAppointments);
        String[] _timeStart = mWorkingHours.get(_dayOfWeek + "Start").split(":");
        String[] _timeEnd = mWorkingHours.get(_dayOfWeek + "End").split(":");
        LocalDateTime _dateTimeStart = getTimeInMillis(_timeStart);
        LocalDateTime _dateTimeEnd = getTimeInMillis(_timeEnd);
        Long _timeStartMillis = getDateInMillis(_dateTimeStart);
        Long _timeEndMillis = getDateInMillis(_dateTimeEnd);
        _CEvent.setStartHour(_timeStartMillis);
        _CEvent.setEndHour(_timeEndMillis);

        return _CEvent;
    }

    private Long getDateInMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalDateTime getTimeInMillis(String[] time) {
        return Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDate().atTime(Integer.parseInt(time[0]), Integer.parseInt(time[1]));
    }

    private boolean hasValue(Object value) {
        Gson _gson = new Gson();
        String _json = _gson.toJson(value);
        /* not empty. Empty means "{}" */
        return _json.length() > 2;
    }

    private long countAppointmentsForThisDay(Long dateInMillis) {
        long _numberOfAppointments = 0;
        assert mAppointmentsForThisMonth != null;
        Object _values = mAppointmentsForThisMonth.containsKey(dateInMillis.toString()) ? mAppointmentsForThisMonth.get(dateInMillis.toString()) : null;
        if (_values != null) {
            Gson _gson = new Gson();
            String _json = _gson.toJson(_values);
            try {
                Map<String, Object> result = new ObjectMapper().readValue(_json, Map.class);
                _numberOfAppointments = result.size();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return _numberOfAppointments;
    }

    private void setRecyclerView() {
        RecyclerView mRecyclerView = findViewById(R.id.act_Calendar_RV_Schedule);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new CalendarAdapter(CalendarActivity.this, mDataSet, mUserID);
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
        if (mAppointmentsForThisMonth != null) {
            getEachAppointments();
        }
    }

    public void startServiceSMSMonitoring() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (_userPreferences.getBoolean("serviceActive", true)) {
            Intent _serviceIntent = getServiceIntent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(_serviceIntent);
            } else {
                startService(_serviceIntent);
            }
        }
    }

    private void setUpSettings() {
        setUpSettingsIV();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }

    private Intent getServiceIntent() {
        Intent _serviceIntent = new Intent(CalendarActivity.this, MonitorIncomingSMSService.class);
        _serviceIntent.putExtra("userID", mUserID);
        _serviceIntent.putExtra("userAppointmentDuration", mUserAppointmentDuration);
        _serviceIntent.putExtra("userWorkingHours", mWorkingHours);
//        _serviceIntent.setAction("ACTION.STARTSERVICE_ACTION");
        return _serviceIntent;
    }

    private void setUpSettingsIV() {
        ImageView imageViewSettings = findViewById(R.id.act_Calendar_IV_Settings);
        imageViewSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startSettingsActivity = new Intent(CalendarActivity.this, SettingsActivity.class);
                startSettingsActivity.putExtra("userID", mUserID);
                startSettingsActivity.putExtra("userAppointmentDuration", mUserAppointmentDuration);
                startSettingsActivity.putExtra("userWorkingHours", mWorkingHours);
                startActivityForResult(startSettingsActivity, SETTINGS_RETURN);
            }
        });
    }


    private void monitorChanges() {
        final DocumentReference docRef = FirebaseFirestore.getInstance().collection("scheduledHours").document(mUserID);
        mRegistration = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    snapshotExists(snapshot);
                } else {
                    snapshotNotExists();
                }
            }
        });
    }

    private void snapshotNotExists() {
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(mCalendarView.getDate().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
        getDateFromCalendarView(_calendar);
        setCalendarContent();
    }

    private void snapshotExists(DocumentSnapshot snapshot) {
        mAppointmentsForThisMonth = snapshot.getData();
        if (mDate == 0L) {
            Calendar _calendar = Calendar.getInstance();
            _calendar.setTimeInMillis(mCalendarView.getDate().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
            getDateFromCalendarView(_calendar);
        } else {
            getEachAppointments();
        }
        setCalendarContent();
    }

    private void getEachAppointments() {
        mDataSet.clear();
        mCounter = 0;
        Object _values = mAppointmentsForThisMonth.containsKey(mDate.toString()) ? mAppointmentsForThisMonth.get(mDate.toString()) : null;
        if (_values != null) {
            Gson _gson = new Gson();
            String _json = _gson.toJson(_values);
            parseJSON(_gson, _json);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void parseJSON(Gson gson, String json) {
        try {
            Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);
            Map<String, Object> _treeMap = new TreeMap<>(result);
            for (Map.Entry<String, Object> _schedule : _treeMap.entrySet()) {
                String jjson = gson.toJson(_schedule.getValue());
                mDataSet.add(mCounter++, new Appointment(_schedule.getKey(), gson, jjson, mCompleteDate, mDate));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, CALL_LOG_PERMISSION_CODE);
    }

    private void requestReadAndSendSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    private void requestReadContactsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_CODE);
    }

    private void closeUponPermissionDenied(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mArePermissionAccepted = true;
            for (boolean _permissionBool : mPermissions) {
                if (!_permissionBool) {
                    mArePermissionAccepted = false;
                }
            }
            if (mArePermissionAccepted) {
                setUpUI();
            }
        } else {
            finishAffinity();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SMS_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissions[0] = true;
                }
                break;
            case CONTACTS_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissions[1] = true;
                }
                break;
            case CALL_LOG_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissions[2] = true;
                }
                break;
        }
        closeUponPermissionDenied(grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
}

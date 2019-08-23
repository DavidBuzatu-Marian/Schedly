package com.example.schedly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.schedly.adapter.CalendarAdapter;
import com.example.schedly.model.Appointment;
import com.example.schedly.packet_classes.PacketService;
import com.example.schedly.service.MonitorIncomingSMSService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.example.schedly.MainActivity.EMAIL_CHANGED;
import static com.example.schedly.MainActivity.PASSWORD_CHANGED;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        if (!isSmsPermissionGranted()) {
            showRequestPermissionsInfoAlertDialog("SMS");
        }
        if(!isContactPermissionGranted()) {
            showRequestPermissionsInfoAlertDialog("CONTACTS");
        }
        if(!isLogPermissionGranted()) {
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
        /* init calendar and get ID for days
         * with schedule
         */
        mCalendarView = findViewById(R.id.act_Calendar_CalendarV);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
//                PacketService _psTest = new PacketService(userID, mUserAppointmentDuration, mUserDaysWithScheduleID, mUserWorkingHoursID);
//                _psTest.makeAppointmentForFixedParameters("2019-09-26", "14:00", "0724154387", "Mama");
                getDateFromCalendarView(year, month, dayOfMonth, false);
                Log.d("DATE", mDate + "");
            }
        });
        setUserDaysWithScheduleIDWrapper();
        /* RecyclerView */
        mRecyclerView = findViewById(R.id.act_Calendar_RV_Schedule);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CalendarAdapter(CalendarActivity.this, mDataSet);
        mRecyclerView.setAdapter(mAdapter);

    }

    private void setImageViewListener(final String dayOfWeek, final String dateFormat) {
        TextView _txtAdd = findViewById(R.id.act_Calendar_TV_AddNew);
        ImageView _imageAdd = findViewById(R.id.act_Calendar_IV_AddIcon);
        _imageAdd.setVisibility(View.VISIBLE);
        _txtAdd.setVisibility(View.VISIBLE);
        _imageAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View _inflatedView = LayoutInflater.from(CalendarActivity.this).inflate(R.layout.add_popup_appointment, null,false);

                // get device size
                Display _display = (findViewById(R.id.act_Calendar_CL_root)).getDisplay();
                final Point _size = new Point();
                _display.getSize(_size);

                final PopupWindow _popWindow;
                // set height depends on the device size
                _popWindow = new PopupWindow(_inflatedView, _size.x - 50,_size.y / 2, true );
                _popWindow.setBackgroundDrawable(getDrawable(R.drawable.bkg_appointment_options));
                // make it focusable to show the keyboard to enter in `EditText`
                _popWindow.setFocusable(true);
                // make it outside touchable to dismiss the popup window
                _popWindow.setOutsideTouchable(true);
                _popWindow.setAnimationStyle(R.style.PopupAnimation);

                // show the popup at bottom of the screen and set some margin at bottom
                _popWindow.showAtLocation(view, Gravity.BOTTOM, 0,0);


                setInformationInPopup(_inflatedView, dayOfWeek, dateFormat);
                setPopUpButtonsListeners(_inflatedView);

                ImageView _closeImg = _inflatedView.findViewById(R.id.popup_add_IV_Close);
                _closeImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        _popWindow.dismiss();
                    }
                });

                setSpinnerAdapter(_inflatedView, dayOfWeek);
            }
        });
    }

    private void setSpinnerAdapter(final View inflatedView, final String dayOfWeek) {
        Log.d("Det", mDate + "");
        final FirebaseFirestore _firebaseFirestore = FirebaseFirestore.getInstance();
        _firebaseFirestore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String _currentDaySHID = documentSnapshot.get(mDate.toString()) != null
                                ? documentSnapshot.get(mDate.toString()).toString() : "";
                        ArrayList<String> _hours = getHoursForDate(dayOfWeek);
                        getFreeHours(_currentDaySHID, _firebaseFirestore, _hours, inflatedView);
                    }
                });
    }

    @SuppressLint("DefaultLocale")
    private ArrayList<String> getHoursForDate(String dayOfWeek) {
        ArrayList<String> _hours = new ArrayList<>();
        String _timeStart = mWorkingHours.get(dayOfWeek + "Start");
        String _timeEnd = mWorkingHours.get(dayOfWeek + "End");

        String[] _timeStartSplitted = _timeStart.split(":");
        String[] _timeEndSplitted = _timeEnd.split(":");

        int _hourStart = Integer.parseInt(_timeStartSplitted[0]),
                _hourEnd = Integer.parseInt(_timeEndSplitted[0]),
                _minuteStart = Integer.parseInt(_timeStartSplitted[1]),
                _minuteEnd = Integer.parseInt(_timeEndSplitted[1]);

        LocalTime _time = LocalTime.of(_hourStart, _minuteStart);
        LocalTime _limitTime = LocalTime.of(_hourEnd, _minuteEnd);

        while(_time.isBefore(_limitTime)) {
            _hours.add(String.format("%02d", _time.getHour()) + ":" + String.format("%02d", _time.getMinute()));
            _time = _time.plusMinutes(Long.parseLong(mUserAppointmentDuration));
        }

        return _hours;
    }

    private void getFreeHours(String currentDaySHID, FirebaseFirestore firebaseFirestore, final ArrayList<String> hours, final View inflatedView) {
        if(currentDaySHID != null) {
            firebaseFirestore.collection("daysWithSchedule")
                    .document(mUserDaysWithScheduleID)
                    .collection("scheduledHours")
                    .document(currentDaySHID)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                Map<String, Object> _map = task.getResult().getData();
                                for (Map.Entry<String, Object> _entry : _map.entrySet()) {
                                    hours.remove(_entry.getKey());
                                }

                                Spinner _hoursSpinner = inflatedView.findViewById(R.id.popup_add_SP_Hours);
                                ArrayAdapter<String> _adapterHours = new ArrayAdapter<>(CalendarActivity.this,
                                        android.R.layout.simple_dropdown_item_1line, hours);
                                _hoursSpinner.setAdapter(_adapterHours);
                            }
                        }
                    });
        }
        else {
            Spinner _hoursSpinner = inflatedView.findViewById(R.id.popup_add_SP_Hours);
            ArrayAdapter<String> _adapterHours = new ArrayAdapter<>(CalendarActivity.this,
                    android.R.layout.simple_dropdown_item_1line, hours);
            _hoursSpinner.setAdapter(_adapterHours);
        }
    }

    private void setPopUpButtonsListeners(View inflatedView) {
        final TextView _txtName = inflatedView.findViewById(R.id.popup_add_ET_Name);
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);

        Button _buttonAddToContacts = inflatedView.findViewById(R.id.popup_add_BUT_AddToContacts);
        _buttonAddToContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent _addToContactsIntent = new Intent(Intent.ACTION_INSERT);
                _addToContactsIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                _addToContactsIntent.putExtra(ContactsContract.Intents.Insert.NAME, _txtName.getText().toString());
                _addToContactsIntent.putExtra(ContactsContract.Intents.Insert.PHONE, _txtNumber.getText().toString());

                CalendarActivity.this.startActivity(_addToContactsIntent);
            }
        });
    }

    private void setInformationInPopup(View inflatedView, String dayOfWeek, String dateFormat) {
        TextView _txtDayOfWeek = inflatedView.findViewById(R.id.popup_add_TV_DayOfWeek);
        TextView _txtDate = inflatedView.findViewById(R.id.popup_add_TV_Date);
        final TextView _txtName = inflatedView.findViewById(R.id.popup_add_ET_Name);
        _txtDate.setText(dateFormat);
        _txtDayOfWeek.setText(dayOfWeek);

        ArrayList<String[]> _callLogDetails = getCallLog();
        final ArrayList<String> _callLogPNumbers = new ArrayList<>();
        final ArrayList<String> _callLogNames = new ArrayList<>();
        for(String[] _value: _callLogDetails) {
            /* add each phone number */
            _callLogPNumbers.add(_value[0]);
            _callLogNames.add(_value[1]);
        }

        ArrayAdapter<String> _adapterNumber = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, _callLogPNumbers);

        /* auto set name if number exists */
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);
        _txtNumber.setAdapter(_adapterNumber);

        _txtNumber.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                int _index = _callLogPNumbers.indexOf(_txtNumber.getText().toString());
                Log.d("Det", _index + ": " + _txtNumber.getText().toString());
                if(_index != -1) {
                    _txtName.setText(_callLogNames.get(_index));
                }
                return false;
            }
        });
        _txtNumber.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int _index = _callLogPNumbers.indexOf(_txtNumber.getText().toString());
                Log.d("Det", _index + ": " + _txtNumber.getText().toString());
                if(_index != -1) {
                    _txtName.setText(_callLogNames.get(_index));
                }
            }
        });
    }

    private ArrayList<String[]> getCallLog() {
        ArrayList<String[]> _details = new ArrayList<>();

        String[] _projection = new String[] {
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        Cursor _managedCursor =  getApplicationContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, _projection, null, null, null);
        while (_managedCursor.moveToNext()) {
            boolean _stateTrue = false;
            String[] _NumberAndName = new String[2];
            _NumberAndName[0] = _managedCursor.getString(1); // number
            _NumberAndName[1] = _managedCursor.getString(0); // name
            for(String[] _detail: _details) {
                if(_NumberAndName[1] != null && _detail[1] != null) {
                    if (_detail[0].equals(_NumberAndName[0]) && _detail[1].equals(_NumberAndName[1])) {
                        _stateTrue = true;
                    }
                }
                else {
                    if (_detail[0].equals(_NumberAndName[0])) {
                        _stateTrue = true;
                    }
                }
            }
            if(!_stateTrue) {
                _details.add(_NumberAndName);
            }
        }

        return _details;
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

        mDate = _calendar.getTimeInMillis();

        setDateForTVs(year, month, dayOfMonth);
        
        Log.d("Date", mDate + "");
        mDataSet.clear();
        mCounter = 0;
        getDayID();
    }

    private void setDateForTVs(int year, int month, int dayOfMonth) {
        String _dayOfWeek, _dateFormat;

        if(year != 0) {
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
            DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault());
            LocalDate _date = LocalDate.of(year, month + 1, dayOfMonth);
            TextView _tvDayInfo = findViewById(R.id.act_Calendar_TV_DayOfWeek);
            TextView _tvDayDate = findViewById(R.id.act_Calendar_TV_Date);
            _dayOfWeek = _date.format(_DTF);
            _dateFormat = _date.format(_DTFDate);
            _tvDayInfo.setText(_dayOfWeek);
            _tvDayDate.setText(_dateFormat);
        } else {
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
            DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault());
            LocalDate _date = LocalDate.now();
            Log.d("Date", month + ": " + dayOfMonth + ":y " + year);
            TextView _tvDayInfo = findViewById(R.id.act_Calendar_TV_DayOfWeek);
            TextView _tvDayDate = findViewById(R.id.act_Calendar_TV_Date);
            _dayOfWeek = _date.format(_DTF);
            _dateFormat = _date.format(_DTFDate);
            _tvDayInfo.setText(_dayOfWeek);
            _tvDayDate.setText(_dateFormat);
        }
        if(mWorkingHours.get(_dayOfWeek + "Start").equals("Free")) {
            TextView _txtAdd = findViewById(R.id.act_Calendar_TV_AddNew);
            ImageView _imageAdd = findViewById(R.id.act_Calendar_IV_AddIcon);
            _imageAdd.setVisibility(View.GONE);
            _txtAdd.setVisibility(View.GONE);

            addFreeDayImage(true);

            /* for disabling scroll on free day */

//            AppBarLayout _ABL = findViewById(R.id.act_Calendar_ABL);
//            CollapsingToolbarLayout _CTL = findViewById(R.id.act_Calendar_CTL);
//            RelativeLayout.LayoutParams _layoutParamsRL = new RelativeLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.WRAP_CONTENT
//            );
//            RelativeLayout _relativeLayoutInABL = new RelativeLayout(this);
//            _relativeLayoutInABL.setLayoutParams(_layoutParamsRL);
//            int _CTLIndex = _ABL.indexOfChild(_CTL);
//            _ABL.removeViewAt(_CTLIndex);
//            _ABL.addView(_relativeLayoutInABL, _CTLIndex);

        } else {
            addFreeDayImage(false);
            setImageViewListener(_dayOfWeek, _dateFormat);
        }
    }

    private void addFreeDayImage(boolean state) {
        ImageView _imageView = findViewById(R.id.act_Calendar_IV_Free);
        if(state) {
            /* show it */
            _imageView.setVisibility(View.VISIBLE);
        }
        else {
            _imageView.setVisibility(View.GONE);
        }
    }

    private void setUserDaysWithScheduleIDWrapper() {
        if(mUserDaysWithScheduleID == null) {
                    setUserDaysWithScheduleID();
        }
        else {
            if (mDate == 0L) {
                        getDateFromCalendarView(0, 0, 0, true);
                        Log.d("DATE", mDate + "");
            }
            startServiceSMSMonitoring();
        }
    }

    public void startServiceSMSMonitoring() {
        SharedPreferences _userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(_userPreferences.getBoolean("serviceActive", true)) {
            Intent serviceIntent = new Intent(CalendarActivity.this, MonitorIncomingSMSService.class);
            serviceIntent.putExtra("userID", userID);
            serviceIntent.putExtra("userDaysWithScheduleID", mUserDaysWithScheduleID);
            serviceIntent.putExtra("userAppointmentDuration", mUserAppointmentDuration);
            serviceIntent.putExtra("userWorkingDaysID", mUserWorkingHoursID);
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
                    Log.d("Calendar", _map.toString());
                    for (Map.Entry<String, Object> _entry : _map.entrySet()) {
                        Log.d("Appointment", _entry.getKey());
                        if(_entry.getValue() == null) {
                            break;
                        }
                        Gson gson = new Gson();
                        String json = gson.toJson(_entry.getValue());
                        Log.d("APPP", json);


                        mDataSet.add(mCounter, new Appointment(_entry.getKey(), gson, json, currentDayID, mUserDaysWithScheduleID));
                        mCounter++;
                    }
                } else {
                    Log.d(ERR, "Error getting documents: ", task.getException());
                }
                mAdapter.notifyDataSetChanged();
            }
        });
    }


    public void showRequestPermissionsInfoAlertDialog(String type) {
        if(type.equals("SMS")) {
            showRequestPermissionsInfoAlertDialog(true);
        }
        else if (type.equals("CONTACTS")) {
            showRequestPermissionsInfoAlertDialogContacts(true);
        }
        else {
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
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }
    private void requestReadContactsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_CODE);
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
            case CONTACTS_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(CalendarActivity.this, "NOT PERMITTED", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == LOG_OUT) {
            setResult(LOG_OUT);
            finish();
        }
        if (resultCode == EMAIL_CHANGED) {
            setResult(EMAIL_CHANGED);
            finish();
        }
        if (resultCode == PASSWORD_CHANGED) {
            setResult(PASSWORD_CHANGED);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishAffinity();
        finish();
    }
}

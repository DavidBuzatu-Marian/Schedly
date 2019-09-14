package com.example.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.schedly.CalendarActivity;
import com.example.schedly.R;
import com.example.schedly.model.Appointment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;

public class PacketCalendar {

    private Activity mActivity;
    private Long mDate;
    private HashMap<String, String> mWorkingHours;
    private String mUserAppointmentDuration;
    private String mSelectedAppointmentHour;
    private PopupWindow mPopWindow;
    private RecyclerView.Adapter mAdapter;
    private ArrayList<Appointment> mDataSet = new ArrayList<>();
    private int mCounter;
    private String mCompleteDate, mUserID;

    public PacketCalendar(Activity activity, HashMap<String, String> workingHours, String userAppointmentDuration, String userID) {
        mActivity = activity;
        mWorkingHours = workingHours;
        mUserID = userID;
        mUserAppointmentDuration = userAppointmentDuration;
    }

    private void setImageViewListener(final String dayOfWeek, final String dateFormat) {
        TextView _txtAdd = mActivity.findViewById(R.id.act_Calendar_TV_AddNew);
        ImageView _imageAdd = mActivity.findViewById(R.id.act_Calendar_IV_AddIcon);
        _imageAdd.setVisibility(View.VISIBLE);
        _txtAdd.setText(R.string.act_Calendar_TV_AddNew);
        _imageAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View _inflatedView = LayoutInflater.from(mActivity).inflate(R.layout.add_popup_appointment, null, false);

                // get device size
                Display _display = (mActivity.findViewById(R.id.act_Calendar_CL_root)).getDisplay();
                final Point _size = new Point();
                _display.getSize(_size);
                // set height depends on the device size
                if (_size.y < 1350) {
                    mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y, true);
                } else if (_size.y > 1350 && _size.y < 1900) {
                    mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y * 3 / 4, true);
                } else {
                    mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y / 2, true);
                }
                mPopWindow.setBackgroundDrawable(mActivity.getDrawable(R.drawable.bkg_appointment_options));
                // make it focusable to show the keyboard to enter in `EditText`
                mPopWindow.setFocusable(true);
                // make it outside touchable to dismiss the popup window
                mPopWindow.setOutsideTouchable(true);
                mPopWindow.setAnimationStyle(R.style.PopupAnimation);

                // show the popup at bottom of the screen and set some margin at bottom
                mPopWindow.showAtLocation(view, Gravity.BOTTOM, 0, 0);

                setSpinnerAdapter(_inflatedView, dayOfWeek);
                setInformationInPopup(_inflatedView, dayOfWeek, dateFormat);
                setPopUpButtonsListeners(_inflatedView);

                ImageView _closeImg = _inflatedView.findViewById(R.id.popup_add_IV_Close);
                _closeImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPopWindow.dismiss();
                    }
                });

            }
        });
    }

    private void setSpinnerAdapter(final View inflatedView, final String dayOfWeek) {
        final ArrayList<String> _hours = getHoursForDate(dayOfWeek);
        FirebaseFirestore.getInstance().collection("scheduledHours")
                .document(mUserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()) {
                            Map<String, Object> _map = task.getResult().getData();
                            assert _map != null;
                            Object _values = _map.containsKey(mDate.toString()) ? _map.get(mDate.toString()) : null;
                            if (_values != null) {
                                Log.d("DayFromPacket", _values.toString());
                                Gson _gson = new Gson();
                                String _json = _gson.toJson(_values);
                                try {
                                    Map<String, Object> result = new ObjectMapper().readValue(_json, Map.class);
                                    for (Map.Entry<String, Object> _schedule : result.entrySet()) {
                                        _hours.remove(_schedule.getKey());
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.d("DayFromPacket", "Day json is: " + _json);
                            }
                        }
                        setUpSpinner(inflatedView, _hours);
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

        while (_time.isBefore(_limitTime)) {
            _hours.add(String.format("%02d", _time.getHour()) + ":" + String.format("%02d", _time.getMinute()));
            _time = _time.plusMinutes(Long.parseLong(mUserAppointmentDuration));
        }

        return _hours;
    }

    private void setUpSpinner(View inflatedView, ArrayList<String> hours) {
        Spinner _hoursSpinner = inflatedView.findViewById(R.id.popup_add_SP_Hours);
        final ArrayAdapter<String> _adapterHours = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, hours);
        _hoursSpinner.setAdapter(_adapterHours);

        _hoursSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d("Selection", _adapterHours.getItem(position) + ":..");
                mSelectedAppointmentHour = _adapterHours.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("Selection", "aee");
                mSelectedAppointmentHour = null;
            }
        });
    }

    private void setPopUpButtonsListeners(View inflatedView) {
        final AutoCompleteTextView _txtName = inflatedView.findViewById(R.id.popup_add_ATV_Name);
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);

        Button _buttonAddToContacts = inflatedView.findViewById(R.id.popup_add_BUT_AddToContacts);
        _buttonAddToContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent _addToContactsIntent = new Intent(Intent.ACTION_INSERT);
                _addToContactsIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                _addToContactsIntent.putExtra(ContactsContract.Intents.Insert.NAME, _txtName.getText().toString());
                _addToContactsIntent.putExtra(ContactsContract.Intents.Insert.PHONE, _txtNumber.getText().toString());

                mActivity.startActivity(_addToContactsIntent);
            }
        });

        Button _buttonAddAppointment = inflatedView.findViewById(R.id.popup_add_BUT_AddAppoinrmwnr);
        _buttonAddAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* we already have schedules on this day */
                    if (mSelectedAppointmentHour != null) {
                        saveAppointmentToDB(_txtName.getText().toString(), _txtNumber.getText().toString());
                    } else {
                        Toast.makeText(mActivity, "Hour for schedule is required!", Toast.LENGTH_LONG).show();
                    }
            }
        });
    }

    private void saveAppointmentToDB(final String name, final String phoneNumber) {
        Log.d("Details", name);
        if (phoneNumber.equals("")) {
            Toast.makeText(mActivity, "Phone number is required!", Toast.LENGTH_LONG).show();
        } else {
            Map<String, String> _detailsOfAppointment = new HashMap<>();
            _detailsOfAppointment.put("PhoneNumber", phoneNumber);
            _detailsOfAppointment.put("Name", name.equals("") ? null : name);
            Map<String, Object> _hourAndInfo = new HashMap<>();
            _hourAndInfo.put(mSelectedAppointmentHour, _detailsOfAppointment);
            Map<String, Object> _appointment = new HashMap<>();
            _appointment.put(mDate.toString(), _hourAndInfo);
            FirebaseFirestore.getInstance().collection("scheduledHours")
                    .document(mUserID)
                    .set(_appointment, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mCounter = ((CalendarActivity) mActivity).getCounter();
                            mDataSet.add(mCounter, new Appointment(mSelectedAppointmentHour, name.equals("") ? null : name, phoneNumber, mCompleteDate, mDate));
                            mCounter++;
                            mAdapter.notifyDataSetChanged();
                            ((CalendarActivity) mActivity).setCounter(mCounter);
                            mPopWindow.dismiss();
//                            sendMessage(phoneNumber);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(mActivity.findViewById(R.id.act_Calendar_CL_root),
                                    "Appointment failed. Please check your connection or submit the error",
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void sendMessage(String phoneNumber) {
        SmsManager.getDefault().sendTextMessage(phoneNumber, null,
                "You've been scheduled on " + mCompleteDate + " at: " + mSelectedAppointmentHour + " . If something is wrong, please contact me",
                null, null);
        Log.d("MESSAGE_ON_CANCEL_app", "CANCELED");
    }

    private void setInformationInPopup(View inflatedView, String dayOfWeek, String dateFormat) {
        TextView _txtDayOfWeek = inflatedView.findViewById(R.id.popup_add_TV_DayOfWeek);
        TextView _txtDate = inflatedView.findViewById(R.id.popup_add_TV_Date);
        _txtDate.setText(dateFormat);
        _txtDayOfWeek.setText(dayOfWeek);

        HashMap<String, String> _callLogDetails = getCallLog();
        final ArrayList<String> _callLogPNumbers = new ArrayList<>();
        final ArrayList<String> _callLogNames = new ArrayList<>();
        for (HashMap.Entry<String, String> _entry: _callLogDetails.entrySet()) {
            /* add each phone number */
            _callLogPNumbers.add(_entry.getKey());
            if(_entry.getValue() == null ) {
                _callLogNames.add("");
            } else {
                _callLogNames.add(_entry.getValue());
            }
        }

        HashMap<String, String> _contactsDetails = getContactList(_callLogPNumbers);

        for (HashMap.Entry<String, String> _entry: _contactsDetails.entrySet()) {
            /* add each phone number */
            _callLogPNumbers.add(_entry.getKey());
            if(_entry.getValue() == null ) {
                _callLogNames.add("");
            } else {
                _callLogNames.add(_entry.getValue());
            }
        }

        ArrayAdapter<String> _adapterNumber = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, _callLogPNumbers);
        ArrayAdapter<String> _adapterNames = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, _callLogNames);

        /* auto set name if number exists
         * set number if name exists */
        final AutoCompleteTextView _txtName = inflatedView.findViewById(R.id.popup_add_ATV_Name);
        Log.d("Err", _txtName.getId() + "");
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);
        _txtNumber.setAdapter(_adapterNumber);
        _txtName.setAdapter(_adapterNames);

        setListenersForATVs(_txtName, _txtNumber, _callLogNames, _callLogPNumbers);
    }

    private void setListenersForATVs(final AutoCompleteTextView txtName, final AutoCompleteTextView txtNumber,
                                     final ArrayList<String> callLogNames, final ArrayList<String> callLogPNumbers) {

        txtNumber.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                int _index = callLogPNumbers.indexOf(txtNumber.getText().toString());
                Log.d("Det", _index + ": " + txtNumber.getText().toString());
                if (_index != -1) {
                    txtName.setText(callLogNames.get(_index));
                }
                return false;
            }
        });
        txtNumber.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int _index = callLogPNumbers.indexOf(txtNumber.getText().toString());
                Log.d("Det", _index + ": " + txtNumber.getText().toString());
                if (_index != -1) {
                    txtName.setText(callLogNames.get(_index));
                }
            }
        });

        txtName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                int _index = callLogNames.indexOf(txtName.getText().toString());
                Log.d("Det", _index + ": " + txtName.getText().toString());
                if (_index != -1) {
                    txtNumber.setText(callLogPNumbers.get(_index));
                }
                return false;
            }
        });
        txtName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int _index = callLogNames.indexOf(txtName.getText().toString());
                Log.d("Det", _index + ": " + txtName.getText().toString());
                if (_index != -1) {
                    txtNumber.setText(callLogPNumbers.get(_index));
                }
            }
        });
    }

    private HashMap<String, String> getCallLog() {
        HashMap<String, String> _details = new HashMap<>();

        String[] _projection = new String[]{
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        Cursor _managedCursor = getApplicationContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, _projection, null, null, null);
        while (_managedCursor.moveToNext()) {
            boolean _stateTrue = false;
            String[] _NumberAndName = new String[2];
            _NumberAndName[0] = _managedCursor.getString(1); // number
            _NumberAndName[1] = _managedCursor.getString(0); // name
            if(_details.containsKey(_NumberAndName[0])) {
                _stateTrue = true;
            }
            if (!_stateTrue) {
                _details.put(_NumberAndName[0], _NumberAndName[1]);
            }
        }

        return _details;
    }

    private HashMap<String, String> getContactList(ArrayList<String> _callLogNumbers) {
        HashMap<String, String> _details = new HashMap<>();
        ContentResolver _contentResolver = mActivity.getContentResolver();
        Cursor _cursor = _contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        /* we got some values */
        if ((_cursor != null ? _cursor.getCount() : 0) > 0) {
            /* while we have values ... */
            while (_cursor != null && _cursor.moveToNext()) {
                /* get the id in order to get number later */
                String _contactID = _cursor.getString(
                        _cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String _contactName = _cursor.getString(_cursor.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (_cursor.getInt(_cursor.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    String[] _NumberAndName = new String[2];
                    Cursor _phoneCursor = _contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{_contactID}, null);

                    while (_phoneCursor.moveToNext()) {
                        boolean _stateTrue = false;
                        String _contactPNumber = _phoneCursor.getString(_phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        if(_contactPNumber.contains(" ")) {
                            _contactPNumber = _contactPNumber.replaceAll(" ", "");
                        }

                        _NumberAndName[0] = _contactPNumber;
                        _NumberAndName[1] = _contactName; // name

                        for (String _phoneNumberUsed : _callLogNumbers) {
                            if (_phoneNumberUsed.equals(_contactPNumber)) {
                                _stateTrue = true;
                            }
                        }

                        if(_details.containsKey(_NumberAndName[0])) {
                            _stateTrue = true;
                        }
                        if (!_stateTrue) {
                            _details.put(_NumberAndName[0], _NumberAndName[1]);
                        }
                    }
                    _phoneCursor.close();
                }
            }
        }
        if (_cursor != null) {
            _cursor.close();
        }

        return _details;
    }

//    private ArrayList<String[]> getContacts(ArrayList<String> _callLogNumbers) {
//        ArrayList<String[]> _details = new ArrayList<>();
//
//        String[] _projection = new String[]{
//                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
//                ContactsContract.CommonDataKinds.Phone.NUMBER
//        };
//
//        Cursor _managedCursor = getApplicationContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, _projection, null, null, null);
//        while (_managedCursor.moveToNext()) {
//
//            boolean _stateTrue = false;
//            String[] _NumberAndName = new String[2];
//            if(_managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) != -1) {
//                _NumberAndName[0] = _managedCursor.getString(_managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)); // number
//                _NumberAndName[1] = _managedCursor.getString(_managedCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)); // name
//                Log.d("Contact", _NumberAndName[1] + ": " + _NumberAndName[0]);
//                for (String _phoneNumberUsed : _callLogNumbers) {
//                    if (_phoneNumberUsed.equals(_NumberAndName[0])) {
//                        _stateTrue = true;
//                    }
//                }
//                if (!_stateTrue) {
//                    _details.add(_NumberAndName);
//                }
//            }
//        }
//
//        return _details;
//    }

    public void setDateForTVs(int year, int month, int dayOfMonth, long milDate, String completeDate) {
        String _dayOfWeek, _dateFormat;
        mDate = milDate;
        mCompleteDate = completeDate;

        if (year != 0) {
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.US);
            DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US);
            LocalDate _date = LocalDate.of(year, month + 1, dayOfMonth);
            TextView _tvDayInfo = mActivity.findViewById(R.id.act_Calendar_TV_DayOfWeek);
            TextView _tvDayDate = mActivity.findViewById(R.id.act_Calendar_TV_Date);
            _dayOfWeek = _date.format(_DTF);
            _dateFormat = _date.format(_DTFDate);
            _tvDayInfo.setText(_dayOfWeek);
            _tvDayDate.setText(_dateFormat);
        } else {
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.US);
            DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US);
            LocalDate _date = LocalDate.now();
            Log.d("Date", month + ": " + dayOfMonth + ":y " + year);
            TextView _tvDayInfo = mActivity.findViewById(R.id.act_Calendar_TV_DayOfWeek);
            TextView _tvDayDate = mActivity.findViewById(R.id.act_Calendar_TV_Date);
            _dayOfWeek = _date.format(_DTF);
            _dateFormat = _date.format(_DTFDate);
            _tvDayInfo.setText(_dayOfWeek);
            _tvDayDate.setText(_dateFormat);
        }
        Log.d("Details", mWorkingHours.toString() + ": " + _dayOfWeek);
        if (mWorkingHours.get(_dayOfWeek + "Start").equals("Free")) {
            TextView _txtAdd = mActivity.findViewById(R.id.act_Calendar_TV_AddNew);
            ImageView _imageAdd = mActivity.findViewById(R.id.act_Calendar_IV_AddIcon);
            _imageAdd.setVisibility(View.GONE);
            _txtAdd.setText(R.string.act_Calendar_TV_Free);

            addFreeDayImage(true);

        } else {
            PacketCalendarHelpers _PCH = new PacketCalendarHelpers(mActivity);
            addFreeDayImage(false);
            _PCH.displayHelpOnAdd();
            setImageViewListener(_dayOfWeek, _dateFormat);
        }
    }


    private void addFreeDayImage(boolean state) {
        ImageView _imageView = mActivity.findViewById(R.id.act_Calendar_IV_Free);
        if (state) {
            /* show it */
            _imageView.setVisibility(View.VISIBLE);
        } else {
            _imageView.setVisibility(View.GONE);
        }
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
    }

    public void setDataSet(ArrayList<Appointment> dataSet) {
        mDataSet = dataSet;
    }

    public void setCounter(int counter) {
        mCounter = counter;
    }
}

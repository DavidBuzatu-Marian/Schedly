package com.davidbuzatu.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
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

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.ScheduledHours;
import com.davidbuzatu.schedly.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;

public class PacketCalendar {

    private Activity mActivity;
    private Long mDate;
    private String mSelectedAppointmentHour;
    private PopupWindow mPopWindow;
    private String mCompleteDate;
    private HashMap<String, String> mCallLogDetails;
    private ArrayList<String> mCallLogPNumbers;
    private ArrayList<String> mCallLogNames;
    private User user = User.getInstance();

    public PacketCalendar(Activity activity) {
        mActivity = activity;
        getNamesAndPhoneNumbers();
    }

    private void setImageViewListener(final String dayOfWeek, final String dateFormat, final String dayOfWeekDisplay) {
        setTVText();
        setIV(dayOfWeek, dateFormat, dayOfWeekDisplay);
    }

    private void setIV(final String dayOfWeek, final String dateFormat, final String dayOfWeekDisplay) {
        ImageView _imageAdd = mActivity.findViewById(R.id.act_Calendar_IV_AddIcon);
        _imageAdd.setVisibility(View.VISIBLE);
        _imageAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View _inflatedView = inflateFromView(view, dayOfWeek, dateFormat, dayOfWeekDisplay);
                final Point _height = getScreenHeight();
                setUpPopUpWindow(_inflatedView, _height, view, dayOfWeek, dateFormat, dayOfWeekDisplay);
            }
        });
    }

    private Point getScreenHeight() {
        Display _display = (mActivity.findViewById(R.id.act_Calendar_CL_root)).getDisplay();
        final Point _size = new Point();
        _display.getSize(_size);
        return _size;
    }

    private View inflateFromView(View view, String dayOfWeek, String dateFormat, String dayOfWeekDisplay) {
        return LayoutInflater.from(mActivity).inflate(R.layout.add_popup_appointment, null, false);
    }

    private void setTVText() {
        TextView _txtAdd = mActivity.findViewById(R.id.act_Calendar_TV_AddNew);
        _txtAdd.setText(R.string.act_Calendar_TV_AddNew);
    }

    private void setUpPopUpWindow(View inflatedView, Point size, View view, String dayOfWeek, String dateFormat, String dayOfWeekDisplay) {
        if (size.y < 1350) {
            mPopWindow = new PopupWindow(inflatedView, size.x - 50, size.y, true);
        } else if (size.y > 1350 && size.y < 1900) {
            mPopWindow = new PopupWindow(inflatedView, size.x - 50, size.y * 3 / 4, true);
        } else {
            mPopWindow = new PopupWindow(inflatedView, size.x - 50, size.y / 2 + 200, true);
        }
        mPopWindow.setBackgroundDrawable(mActivity.getDrawable(R.drawable.bkg_appointment_options));
        mPopWindow.setFocusable(true);
        mPopWindow.setOutsideTouchable(true);
        mPopWindow.setAnimationStyle(R.style.PopupAnimation);
        mPopWindow.showAtLocation(view, Gravity.BOTTOM, 0, 0);
        setUpElementsInPopUp(inflatedView, dayOfWeek, dateFormat, dayOfWeekDisplay);
    }

    private void setUpElementsInPopUp(View inflatedView, String dayOfWeek, String dateFormat, String dayOfWeekDisplay) {
        setSpinnerAdapter(inflatedView, dayOfWeek);
        setInformationInPopup(inflatedView, dateFormat, dayOfWeekDisplay);
        setPopUpButtonsListeners(inflatedView);
        ImageView _closeImg = inflatedView.findViewById(R.id.popup_add_IV_Close);
        _closeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopWindow.dismiss();
            }
        });
    }

    private void setSpinnerAdapter(final View inflatedView, final String dayOfWeek) {
        final ArrayList<String> _hours = getHoursForDate(dayOfWeek);
        if(ScheduledHours.getInstance().getScheduledHoursSnapshot() == null) {
            ScheduledHours.getInstance().getScheduledHours(user.getUid())
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            getTaskValues(task, _hours, inflatedView);
                        }
                    });
        } else {
            getDocumentValues(ScheduledHours.getInstance().getScheduledHoursSnapshot(), _hours, inflatedView);
        }
    }

    private void getDocumentValues(DocumentSnapshot scheduledHoursSnapshot, ArrayList<String> hours, View inflatedView) {
        if (scheduledHoursSnapshot.exists()) {
            setUpHoursInSpinner(scheduledHoursSnapshot.getData(), hours, inflatedView);
        } else {
            setUpSpinner(inflatedView, hours);
        }
    }

    private void getTaskValues(Task<DocumentSnapshot> task, ArrayList<String> hours, View inflatedView) {
        DocumentSnapshot _document = task.getResult();
        assert _document != null;
        if (task.isSuccessful() && _document.exists()) {
            setUpHoursInSpinner(task.getResult().getData(), hours, inflatedView);
        } else {
            setUpSpinner(inflatedView, hours);
        }
    }

    private void setUpHoursInSpinner(Map<String, Object> data, ArrayList<String> _hours, View inflatedView) {
        Object _values = data.containsKey(mDate.toString()) ? data.get(mDate.toString()) : null;
        if (_values != null) {
            removeScheduledHours(_hours, _values);
        }
        setUpSpinner(inflatedView, _hours);
    }

    private void removeScheduledHours(ArrayList<String> hours, Object values) {
        Gson _gson = new Gson();
        String _json = _gson.toJson(values);
        try {
            Map<String, Object> result = new ObjectMapper().readValue(_json, Map.class);
            for (Map.Entry<String, Object> _schedule : result.entrySet()) {
                hours.remove(_schedule.getKey());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("DefaultLocale")
    private ArrayList<String> getHoursForDate(String dayOfWeek) {
        ArrayList<String> _hours = new ArrayList<>();
        String _timeStart = user.getUserWorkingHours().get(dayOfWeek + "Start");
        String _timeEnd = user.getUserWorkingHours().get(dayOfWeek + "End");
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
            _time = _time.plusMinutes(Long.parseLong(user.getUserAppointmentsDuration()));
        }
        return _hours;
    }

    private void setUpSpinner(View inflatedView, ArrayList<String> hours) {
        Spinner _hoursSpinner = inflatedView.findViewById(R.id.popup_add_SP_Hours);
        if(hours.size() == 0) {
            hours.add("Full day");
        }
        final ArrayAdapter<String> _adapterHours = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, hours);
        _hoursSpinner.setAdapter(_adapterHours);
        setOnSpinnerItemSelected(_hoursSpinner, _adapterHours, inflatedView);
    }

    private void setOnSpinnerItemSelected(Spinner hoursSpinner, final ArrayAdapter<String> adapterHours, final View inflatedView) {
        hoursSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                mSelectedAppointmentHour = adapterHours.getItem(position);
                Button _buttonAddAppointment = inflatedView.findViewById(R.id.popup_add_BUT_AddAppointment);
                if( mSelectedAppointmentHour.equals("Full day")) {
                    _buttonAddAppointment.setEnabled(false);
                } else {
                    _buttonAddAppointment.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mSelectedAppointmentHour = null;
                Button _buttonAddAppointment = inflatedView.findViewById(R.id.popup_add_BUT_AddAppointment);
                _buttonAddAppointment.setEnabled(false);
            }
        });
    }

    private void setPopUpButtonsListeners(View inflatedView) {
        final AutoCompleteTextView _txtName = inflatedView.findViewById(R.id.popup_add_ATV_Name);
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);
        setUpButtonAddContact(inflatedView, _txtName, _txtNumber);
        setUpButtonAddAppointment(inflatedView, _txtName, _txtNumber);
    }

    private void setUpButtonAddAppointment(View inflatedView, final AutoCompleteTextView txtName, final AutoCompleteTextView txtNumber) {
        Button _buttonAddAppointment = inflatedView.findViewById(R.id.popup_add_BUT_AddAppointment);
        if(mSelectedAppointmentHour == null || mSelectedAppointmentHour.equals("Full day")) {
            _buttonAddAppointment.setEnabled(false);
        }
        _buttonAddAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedAppointmentHour != null) {
                    saveAppointmentToDB(txtName.getText().toString(), txtNumber.getText().toString());
                } else {
                    Toast.makeText(mActivity, mActivity.getString(R.string.toast_add_appointment_hours), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setUpButtonAddContact(View inflatedView, final AutoCompleteTextView txtName, final AutoCompleteTextView txtNumber) {
        Button _buttonAddToContacts = inflatedView.findViewById(R.id.popup_add_BUT_AddToContacts);
        _buttonAddToContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent _addToContactsIntent = new Intent(Intent.ACTION_INSERT);
                _addToContactsIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                _addToContactsIntent.putExtra(ContactsContract.Intents.Insert.NAME, txtName.getText().toString());
                _addToContactsIntent.putExtra(ContactsContract.Intents.Insert.PHONE, txtNumber.getText().toString());
                mActivity.startActivity(_addToContactsIntent);
            }
        });
    }

    private void saveAppointmentToDB(final String name, final String phoneNumber) {
        if (phoneNumber.equals("")) {
            Toast.makeText(mActivity, mActivity.getString(R.string.toast_add_appointment_phone_number), Toast.LENGTH_LONG).show();
        } else {
                ScheduledHours.getInstance().saveScheduledHour(name, phoneNumber, null, mSelectedAppointmentHour, mDate.toString(), user.getUid())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mPopWindow.dismiss();
//                            sendMessage(phoneNumber);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(mActivity.findViewById(R.id.act_Calendar_CL_root), mActivity.getString(R.string.snackbar_add_appointment_failed), Snackbar.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void sendMessage(String phoneNumber) {
        SmsManager.getDefault().sendTextMessage(phoneNumber, null,
                mActivity.getString(R.string.add_appointment_manual_success_beg)
                        + mCompleteDate
                        + mActivity.getString(R.string.add_appointment_manual_success_at)
                        + mSelectedAppointmentHour
                        + mActivity.getString(R.string.add_appointment_manual_success_end),
                null, null);
    }

    private void setInformationInPopup(View inflatedView, String dateFormat, String dayOfWeekDisplay) {
        setUpTVsInPopUp(inflatedView, dateFormat, dayOfWeekDisplay);
        setUpAdapters(inflatedView);
    }

    private void setUpAdapters(View inflatedView) {
        ArrayAdapter<String> _adapterNumber = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, mCallLogPNumbers);
        ArrayAdapter<String> _adapterNames = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, mCallLogNames);
        final AutoCompleteTextView _txtName = inflatedView.findViewById(R.id.popup_add_ATV_Name);
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);
        _txtNumber.setAdapter(_adapterNumber);
        _txtName.setAdapter(_adapterNames);

        setListenersForATVs(_txtName, _txtNumber, mCallLogNames, mCallLogPNumbers);
    }

    private void setUpTVsInPopUp(View inflatedView, String dateFormat, String dayOfWeekDisplay) {
        TextView _txtDayOfWeek = inflatedView.findViewById(R.id.popup_add_TV_DayOfWeek);
        TextView _txtDate = inflatedView.findViewById(R.id.popup_add_TV_Date);
        _txtDate.setText(dateFormat);
        _txtDayOfWeek.setText(dayOfWeekDisplay);
    }

    private void getNamesAndPhoneNumbers() {
        mCallLogDetails = getCallLog();
        mCallLogPNumbers = new ArrayList<>();
        mCallLogNames = new ArrayList<>();
        removeDuplicates(mCallLogDetails);
        removeDuplicates(getContactList());
    }

    private void removeDuplicates(HashMap<String, String> contactsDetails) {
        for (HashMap.Entry<String, String> _entry : contactsDetails.entrySet()) {
            mCallLogPNumbers.add(_entry.getKey());
            if (_entry.getValue() == null) {
                mCallLogNames.add("");
            } else {
                mCallLogNames.add(_entry.getValue());
            }
        }
    }

    private void setListenersForATVs(final AutoCompleteTextView txtName, final AutoCompleteTextView txtNumber,
                                     final ArrayList<String> callLogNames, final ArrayList<String> callLogPNumbers) {

        txtNumber.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                int _index = callLogPNumbers.indexOf(txtNumber.getText().toString());
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
                if (_index != -1) {
                    txtName.setText(callLogNames.get(_index));
                }
            }
        });

        txtName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                int _index = callLogNames.indexOf(txtName.getText().toString());
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
            if (_details.containsKey(_NumberAndName[0])) {
                _stateTrue = true;
            }
            if (!_stateTrue) {
                _details.put(_NumberAndName[0], _NumberAndName[1]);
            }
        }

        return _details;
    }

    private HashMap<String, String> getContactList() {
        HashMap<String, String> _details = new HashMap<>();
        ContentResolver _contentResolver = mActivity.getContentResolver();
        Cursor _cursor = _contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((_cursor != null ? _cursor.getCount() : 0) > 0) {
            while (_cursor != null && _cursor.moveToNext()) {
                String _contactID = _cursor.getString(_cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String _contactName = _cursor.getString(_cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (_cursor.getInt(_cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    String[] _NumberAndName = new String[2];
                    Cursor _phoneCursor = _contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{_contactID}, null);
                    while (_phoneCursor.moveToNext()) {
                        boolean _stateTrue = false;
                        String _contactPNumber = _phoneCursor.getString(_phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        if (_contactPNumber.contains(" ")) {
                            _contactPNumber = _contactPNumber.replaceAll(" ", "");
                        }
                        _NumberAndName[0] = _contactPNumber;
                        _NumberAndName[1] = _contactName; // name
                        for (String _phoneNumberUsed : mCallLogPNumbers) {
                            if (_phoneNumberUsed.equals(_contactPNumber)) {
                                _stateTrue = true;
                            }
                        }
                        if (_details.containsKey(_NumberAndName[0])) {
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


    public void setDateForTVs(Calendar calendar, long milDate, String completeDate) {
        String _dayOfWeek, _dayOfWeekDisplay, _dateFormat;
        mDate = milDate;
        mCompleteDate = completeDate;

        DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.US);
        DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US);
        LocalDate _date = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        TextView _tvDayInfo = mActivity.findViewById(R.id.act_Calendar_TV_DayOfWeek);
        TextView _tvDayDate = mActivity.findViewById(R.id.act_Calendar_TV_Date);
        _dayOfWeek = _date.format(_DTF);
        _dayOfWeekDisplay = _date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()));
        _dateFormat = _date.format(_DTFDate);
        _tvDayInfo.setText(capitalize(_dayOfWeekDisplay));
        _tvDayDate.setText(_dateFormat);
        if (user.getUserWorkingHours().get(_dayOfWeek + "Start").equals("Free")) {
            setTVAndIVAdd();
            addFreeDayImage(true);

        } else {
            addFreeDayImage(false);
            setImageViewListener(_dayOfWeek, _dateFormat, capitalize(_dayOfWeekDisplay));
        }
    }

    private void setTVAndIVAdd() {
        TextView _txtAdd = mActivity.findViewById(R.id.act_Calendar_TV_AddNew);
        ImageView _imageAdd = mActivity.findViewById(R.id.act_Calendar_IV_AddIcon);
        _imageAdd.setVisibility(View.GONE);
        _txtAdd.setText(R.string.act_Calendar_TV_Free);
    }

    private String capitalize(String dayOfWeekDisplay) {
        StringBuilder _builder = new StringBuilder(dayOfWeekDisplay);
        _builder.setCharAt(0, Character.toUpperCase(_builder.charAt(0)));
        return _builder.toString();
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
}

package com.example.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.provider.CallLog;
import android.provider.ContactsContract;
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

import com.example.schedly.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;

public class PacketCalendar {

    private Activity mActivity;
    private Long mDate;
    private HashMap<String, String> mWorkingHours;
    private String mUserAppointmentDuration, mUserDaysWithScheduleID;
    private String mCurrentDaySHID;
    private String mSelectedAppointmentHour;
    private PopupWindow mPopWindow;

    public PacketCalendar(Activity activity, HashMap<String, String> workingHours, String userDaysWithScheduleID, String userAppointmentDuration) {
        mActivity = activity;
        mWorkingHours = workingHours;
        mUserDaysWithScheduleID = userDaysWithScheduleID;
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
                mPopWindow = new PopupWindow(_inflatedView, _size.x - 50, _size.y / 2, true);
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
        Log.d("Det", mDate + "");
        final FirebaseFirestore _firebaseFirestore = FirebaseFirestore.getInstance();
        _firebaseFirestore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        mCurrentDaySHID = documentSnapshot.get(mDate.toString()) != null
                                ? documentSnapshot.get(mDate.toString()).toString() : null;
                        ArrayList<String> _hours = getHoursForDate(dayOfWeek);
                        getFreeHours(mCurrentDaySHID, _firebaseFirestore, _hours, inflatedView);
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

    private void getFreeHours(String currentDaySHID, FirebaseFirestore firebaseFirestore, final ArrayList<String> hours, final View inflatedView) {
        if (currentDaySHID != null) {
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

                                setUpSpinner(inflatedView, hours);
                            }
                        }
                    });
        } else {
            setUpSpinner(inflatedView, hours);
        }
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
                if (mCurrentDaySHID != null) {
                    if (mSelectedAppointmentHour != null) {
                        saveAppointmentToDB(_txtName.getText().toString(), _txtNumber.getText().toString());
                    } else {
                        Toast.makeText(mActivity, "Hour for schedule is required!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (mSelectedAppointmentHour != null) {
                        addScheduledHoursForDate(_txtName.getText().toString(), _txtNumber.getText().toString());
                    } else {
                        Toast.makeText(mActivity, "Hour for schedule is required!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void addScheduledHoursForDate(final String name, final String phoneNumber) {
        Map<String, Object> addDaysWithScheduleID = new HashMap<>();
        addDaysWithScheduleID.put("5:00", null);
        FirebaseFirestore.getInstance().collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .add(addDaysWithScheduleID)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        addThisDateScheduledHoursID(documentReference.getId(), name, phoneNumber);
                        Log.d("Firebase-workingDays", "Succes with scheduled hours");
                    }
                });
    }

    private void addThisDateScheduledHoursID(String id, final String name, final String phoneNumber) {
        Map<String, Object> addToCurrentDateID = new HashMap<>();
        addToCurrentDateID.put(mDate.toString(), id);
        FirebaseFirestore.getInstance().collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .update(addToCurrentDateID)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        saveAppointmentToDB(name, phoneNumber);
                        Log.d("Firebase-workingDays", "Succes with id");
                    }
                });
    }

    private void saveAppointmentToDB(String name, String phoneNumber) {
        Log.d("Details", name);
        if(phoneNumber.equals("")) {
            Toast.makeText(mActivity, "Phone number is required!", Toast.LENGTH_LONG).show();
        } else {
            Map<String, String> _detailsOfAppointment = new HashMap<>();
            _detailsOfAppointment.put("PhoneNumber", phoneNumber);
            _detailsOfAppointment.put("Name", name.equals("") ? null : name);
            Map<String, Object> _appointment = new HashMap<>();
            _appointment.put(mSelectedAppointmentHour, _detailsOfAppointment);

            FirebaseFirestore _firebaseFirestore = FirebaseFirestore.getInstance();
            _firebaseFirestore.collection("daysWithSchedule")
                    .document(mUserDaysWithScheduleID)
                    .collection("scheduledHours")
                    .document(mCurrentDaySHID)
                    .update(_appointment)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mPopWindow.dismiss();
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

    private void setInformationInPopup(View inflatedView, String dayOfWeek, String dateFormat) {
        TextView _txtDayOfWeek = inflatedView.findViewById(R.id.popup_add_TV_DayOfWeek);
        TextView _txtDate = inflatedView.findViewById(R.id.popup_add_TV_Date);
        _txtDate.setText(dateFormat);
        _txtDayOfWeek.setText(dayOfWeek);


        ArrayList<String[]> _callLogDetails = getCallLog();
        final ArrayList<String> _callLogPNumbers = new ArrayList<>();
        final ArrayList<String> _callLogNames = new ArrayList<>();
        for (String[] _value : _callLogDetails) {
            /* add each phone number */
            _callLogPNumbers.add(_value[0]);
            _callLogNames.add(_value[1]);
        }

//        ArrayList<String[]> _contactsDetails = getContacts(_callLogPNumbers);
//        for (String[] _value : _contactsDetails) {
//            /* add each phone number */
//            _callLogPNumbers.add(_value[0]);
//            _callLogNames.add(_value[1]);
//        }

        ArrayAdapter<String> _adapterNumber = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, _callLogPNumbers);
//        ArrayAdapter<String> _adapterNames = new ArrayAdapter<>(mActivity,
//                android.R.layout.simple_dropdown_item_1line, _callLogNames);

        /* auto set name if number exists
         * set number if name exists */
        final AutoCompleteTextView _txtName = inflatedView.findViewById(R.id.popup_add_ATV_Name);
        final AutoCompleteTextView _txtNumber = inflatedView.findViewById(R.id.popup_add_ATV_PhoneNumber);
        _txtNumber.setAdapter(_adapterNumber);
//        _txtName.setAdapter(_adapterNames);

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

    private ArrayList<String[]> getCallLog() {
        ArrayList<String[]> _details = new ArrayList<>();

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
            for (String[] _detail : _details) {
                if (_NumberAndName[1] != null && _detail[1] != null) {
                    if (_detail[0].equals(_NumberAndName[0]) && _detail[1].equals(_NumberAndName[1])) {
                        _stateTrue = true;
                    }
                } else {
                    if (_detail[0].equals(_NumberAndName[0])) {
                        _stateTrue = true;
                    }
                }
            }
            if (!_stateTrue) {
                _details.add(_NumberAndName);
            }
        }

        return _details;
    }

//    private ArrayList<String[]> getContacts(ArrayList<String> _callLogNumbers) {
//        ArrayList<String[]> _details = new ArrayList<>();
//
//        String[] _projection = new String[]{
//                ContactsContract.Contacts.DISPLAY_NAME,
//                ContactsContract.CommonDataKinds.Phone.NUMBER
//        };
//
//        Cursor _managedCursor = getApplicationContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
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

    public void setDateForTVs(int year, int month, int dayOfMonth, long milDate) {
        String _dayOfWeek, _dateFormat;
        mDate = milDate;

        if (year != 0) {
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
            DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault());
            LocalDate _date = LocalDate.of(year, month + 1, dayOfMonth);
            TextView _tvDayInfo = mActivity.findViewById(R.id.act_Calendar_TV_DayOfWeek);
            TextView _tvDayDate = mActivity.findViewById(R.id.act_Calendar_TV_Date);
            _dayOfWeek = _date.format(_DTF);
            _dateFormat = _date.format(_DTFDate);
            _tvDayInfo.setText(_dayOfWeek);
            _tvDayDate.setText(_dateFormat);
        } else {
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
            DateTimeFormatter _DTFDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault());
            LocalDate _date = LocalDate.now();
            Log.d("Date", month + ": " + dayOfMonth + ":y " + year);
            TextView _tvDayInfo = mActivity.findViewById(R.id.act_Calendar_TV_DayOfWeek);
            TextView _tvDayDate = mActivity.findViewById(R.id.act_Calendar_TV_Date);
            _dayOfWeek = _date.format(_DTF);
            _dateFormat = _date.format(_DTFDate);
            _tvDayInfo.setText(_dayOfWeek);
            _tvDayDate.setText(_dateFormat);
        }
        if (mWorkingHours.get(_dayOfWeek + "Start").equals("Free")) {
            TextView _txtAdd = mActivity.findViewById(R.id.act_Calendar_TV_AddNew);
            ImageView _imageAdd = mActivity.findViewById(R.id.act_Calendar_IV_AddIcon);
            _imageAdd.setVisibility(View.GONE);
            _txtAdd.setText(R.string.act_Calendar_TV_Free);

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
        ImageView _imageView = mActivity.findViewById(R.id.act_Calendar_IV_Free);
        if (state) {
            /* show it */
            _imageView.setVisibility(View.VISIBLE);
        } else {
            _imageView.setVisibility(View.GONE);
        }
    }
}

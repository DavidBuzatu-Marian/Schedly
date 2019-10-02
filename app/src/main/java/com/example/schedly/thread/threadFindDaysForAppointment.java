package com.example.schedly.thread;

import android.content.res.Resources;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.schedly.R;
import com.example.schedly.model.ContextForStrings;
import com.example.schedly.packet_classes.PacketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.type.DayOfWeek;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class threadFindDaysForAppointment extends Thread {

    private final int HOUR_AND_HALF = 5400000, TWO_HOURS = 7200000;
    private AtomicBoolean mThreadStop = new AtomicBoolean(true);
    private String mUserAppointmentDuration;
    private FirebaseFirestore mFireStore;
    private int mCounterNextDay, mCounterDaysForAppointment;
    private boolean mResult;
    /* variable used to know when getting the
     * scheduled hours finished
     */
    private String mTimeToSchedule;
    // constant used for getting next days;
    private final Integer DAY_LENGTH_MILLIS = 86400000;
    // get the appointments for a given date
    private Map<String, Object> mCurrentDayAppointments;
    /* get all of users working days which
    have a schedule */
    private Map<String, Object> mUserDaysWithSchedule;
    private String mDayOfTheWeek;
    // used for geting start and end hours
    private String[] mDaySchedule;
    // building the sms body
    private StringBuilder mSMSBody;
    private String mPhoneNumber, mUserID;
    // used to get the required working hours
    private final String[] mDaysOfTheWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private Map<String, String> mUserWorkingDays;
    private String mMessageType;
    private Long mDateInMillisFromService = 0L;
    private Map<String, Object> mUserAppointments;
    private Resources mResources = ContextForStrings.getContext().getResources();

    public threadFindDaysForAppointment(Map<String, String> userWorkingDays,
                                        FirebaseFirestore firebaseFirestore,
                                        String userAppointmentDuration) {
        mUserWorkingDays = userWorkingDays;
        mFireStore = firebaseFirestore;
        mUserAppointmentDuration = userAppointmentDuration;
        mSMSBody = new StringBuilder();
    }

    public void setmUserDaysWithSchedule(Map<String, Object> UserDaysWithSchedule) {
        mUserDaysWithSchedule = UserDaysWithSchedule;
    }

    public void setmPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    public void setmTimeToSchedule(String timeToSchedule) {
        mTimeToSchedule = timeToSchedule;
    }

    public void setmMessageType(String type) {
        mMessageType = type;
    }

    public void setmSMSBody(String SMSBody) {
        mSMSBody.append(SMSBody);
    }

    public void setUserAppointments(Map<String, Object> mUserAppointments) {
        this.mUserAppointments = mUserAppointments;
    }

    public void setmDateInMillisFromService(Long dateInMillisFromService) {
        mDateInMillisFromService = dateInMillisFromService;
    }

    public void run() {
        Calendar _calendar = Calendar.getInstance();
        if (mMessageType.equals("TIME")) {
            mSMSBody.append(mResources.getString(R.string.responses_closest_days));
        }
        /* we need current time */
        if (mDateInMillisFromService == 0L) {
            _calendar.setTimeInMillis(System.currentTimeMillis());
        } else {
            /* we have a date */
            _calendar.setTimeInMillis(mDateInMillisFromService);
        }
        Log.d("Message at beginning:", mSMSBody.toString());
        mCounterDaysForAppointment = 0;
        // this counter is used for getting the next days
        mCounterNextDay = 0;

        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        _calendar.set(Calendar.SECOND, 0);
        Log.d("Firebase", _calendar.getTimeInMillis() + "");
        while (mCounterDaysForAppointment < 3) {
            if (checkDayFree(_calendar.getTimeInMillis())) {
                mCounterDaysForAppointment++;
//                Log.d("FirebaseDATE", mSMSBody.toString());
            }
//            Log.d("FirebaseDATE", mCounterDaysForAppointment + "");
        }
// RESUME FROM HERE
        /* ********************************************************8
         *****************************************************
         */
        sendMessage();
    }

    private void sendMessage() {
        ArrayList<String> _messageParts = new ArrayList<>(2);
        int _indexOfComma = mSMSBody.toString().indexOf(":");
        _messageParts.add(mSMSBody.toString().substring(0, _indexOfComma + 1));
        _messageParts.add(mSMSBody.toString().substring(_indexOfComma + 2));

        for (String _message : _messageParts) {
            Log.d("MESSAGE", _message);
            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, _message, null, null);
        }
        Log.d("MESSAGE", mSMSBody.toString());
        Log.d("NUMBER", mPhoneNumber);
    }

    private synchronized boolean checkDayFree(long calendarTimeInMillis) {

        final Long _dateInMillisLong;
        final String _dateInMillis;
        mResult = false;

        _dateInMillisLong = (calendarTimeInMillis + (mCounterNextDay * DAY_LENGTH_MILLIS));
        _dateInMillis = _dateInMillisLong.toString();
//        Log.d("FirebaseDatee", _dateInMillis);
        mCounterNextDay++;
        // get the data, wait until process finishes
        /* we already have appointments on this day
         * we need extra checking for each time
         * already appointed
         */
        if (mUserDaysWithSchedule.containsKey(_dateInMillis)) {
            getDayOfTheWeek(_dateInMillisLong);
            getCurrentDateAppointments(_dateInMillis);
            /* if return is true
             * then we found a valid time
             * before or after 1 hour and a half
             * if return is false
             * we need to try the next date
             */
            mDaySchedule = new String[2];
            mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start");
            mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End");

            if (mDaySchedule[0].equals("Free")) {
                mResult = false;
            } else {
                try {
                    mResult = checkDayForCurrentHour(mTimeToSchedule, true, _dateInMillisLong);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return mResult;
        }
        /* this day is free
         * we need to add it first
         * then we need to find a good hour
         * which fits with the appointment duration
         * we do all this asynchronously
         */
        else {
            Log.d("Firebasee", "COde red");
            // we got a non-working day

            getDayOfTheWeek(_dateInMillisLong);
            mDaySchedule = new String[2];
            mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start");
            mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End");

            if (mDaySchedule[0].equals("Free")) {
                mResult = false;
            } else {
                // we got a working day. Start work
                try {
                    mResult = checkDayForCurrentHour(mTimeToSchedule, false, _dateInMillisLong);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            Log.d("Firebase", "Code green");
            return mResult;

        }

    }


    private boolean checkDayForCurrentHour(String mTimeToSchedule, boolean dayHasAppointments, Long dateInMillis) throws ParseException {
        int _appointmentDurationInteger = Integer.parseInt(mUserAppointmentDuration);
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat _sDFormatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String _dateBefore = "", _dateAfter = "";
        String _hour = mDaySchedule[0];
        Calendar _calendar = Calendar.getInstance();
        Calendar _calendarToPrint = Calendar.getInstance();
        Date _dateCurrentHour = _sDFormat.parse(mTimeToSchedule);
        Date _date = _sDFormat.parse(_hour);

        long _elapsedTime = _date.getTime() - _dateCurrentHour.getTime();
//        Log.d("FirebaseTIme", _dateCurrentHour.getTime() + "");
        // hour is outside of starting hour of work
        if (_elapsedTime > 0) {
            return false;
        }

        while (gotTime(_hour, mDaySchedule[1])) {
            if (dayHasAppointments && (mCurrentDayAppointments == null || !mCurrentDayAppointments.containsKey(_hour))) {

                _elapsedTime = _date.getTime() - _dateCurrentHour.getTime();
                /* hour is before our current one
                 * and is not more than 1 hour and a half
                 */
                if (_elapsedTime <= 0 && _elapsedTime > -HOUR_AND_HALF) {
                    _calendarToPrint.setTimeInMillis(dateInMillis + _date.getTime() + TWO_HOURS);
                    _dateBefore = _sDFormatDate.format(_calendarToPrint.getTime());
                }
                /* hour is after our current one
                 * and is not more than 1 hour and a half
                 */
                else if (_elapsedTime > 0 && _elapsedTime < HOUR_AND_HALF) {
                    _calendarToPrint.setTimeInMillis(dateInMillis + _date.getTime() + TWO_HOURS);
                    _dateAfter = _sDFormatDate.format(_calendarToPrint.getTime());
                    break;
                }
            } else if (!dayHasAppointments) {
                _elapsedTime = _date.getTime() - _dateCurrentHour.getTime();
//                Log.d("ElapsedTime", _elapsedTime + " = " + _date.getTime() + " - " + _dateCurrentHour.getTime());
                /* hour is before our current one
                 * and is not more than 1 hour and a half
                 */
//                Log.d("FirebaseDate", _date.toString());
                if (_elapsedTime <= 0 && _elapsedTime > -HOUR_AND_HALF) {
                    _calendarToPrint.setTimeInMillis(dateInMillis + _date.getTime() + TWO_HOURS);
                    _dateBefore = _sDFormatDate.format(_calendarToPrint.getTime());
                }
                /* hour is after our current one
                 * and is not more than 1 hour and a half
                 */
                else if (_elapsedTime > 0 && _elapsedTime < HOUR_AND_HALF) {
                    _calendarToPrint.setTimeInMillis(dateInMillis + _date.getTime() + TWO_HOURS);
                    _dateAfter = _sDFormatDate.format(_calendarToPrint.getTime());
                    break;
                }
            }

            _calendar.setTimeInMillis(_date.getTime());
            _calendar.add(Calendar.MINUTE, _appointmentDurationInteger);
            _hour = _sDFormat.format(_calendar.getTime());
            _date = _sDFormat.parse(_hour);
        }
        // we found at least an hour in that day
        if (!_dateAfter.equals("") || !_dateBefore.equals("")) {
            if (_dateAfter.equals("")) {
                mSMSBody.append("\n").append(_dateBefore);
            } else if (_dateBefore.equals("")) {
                mSMSBody.append("\n").append(_dateAfter);
            } else {
                mSMSBody.append("\n").append(_dateBefore).append("\n").append(_dateAfter);
            }
            return true;
        }
        // day is full, try another time
        else {
            return false;
        }
    }

    // function to get all the appointments for a given date ID
    private void getCurrentDateAppointments(final String dateInMillis) {
        mCurrentDayAppointments = new HashMap<>();
        Object _values = mUserAppointments.containsKey(dateInMillis) ? mUserAppointments.get(dateInMillis) : null;
        if (_values != null) {
            Log.d("DayINSERVICE", _values.toString());
            Gson _gson = new Gson();
            String _json = _gson.toJson(_values);
            try {
                mCurrentDayAppointments = new ObjectMapper().readValue(_json, Map.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("Day", "Day json is: " + _json);
        } else {
            mCurrentDayAppointments = null;
        }
    }

    // check if we still are in the working hours
    private boolean gotTime(String startTime, String endTime) throws ParseException {
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        Date d1 = _sDFormat.parse(startTime);
        Date d2 = _sDFormat.parse(endTime);
        long elapsedTime = d2.getTime() - d1.getTime();
//        Log.d("FirebaseGotTime", elapsedTime + "");
//        Log.d("FirebaseGotTime", startTime + "; " + endTime);
        return elapsedTime > 0;
    }

    private void getDayOfTheWeek(Long dateInMillis) {
        Log.d("FirebaseGetDayOfWeeek", dateInMillis + "");
        Date _date = new Date();
        _date.setTime(dateInMillis);
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTime(_date);
        mDayOfTheWeek = mDaysOfTheWeek[_calendar.get(Calendar.DAY_OF_WEEK) - 1];
        Log.d("FirebaseDay", mDayOfTheWeek + "; " + _calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));
    }

    public void setmUserID(String mUserID) {
        this.mUserID = mUserID;
    }
}

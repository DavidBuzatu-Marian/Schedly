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

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class threadFindDaysForAppointment extends Thread {

    private final int HOUR_AND_HALF = 5400000, TWO_HOURS = 7200000;
    private String mUserAppointmentDuration;
    private int mCounterNextDay, mCounterDaysForAppointment;
    private boolean mResult;
    /* variable used to know when getting the
     * scheduled hours finished
     */
    private String mTimeToSchedule;
    // constant used for getting next days;
    private final Integer DAY_LENGTH_MILLIS = 86400000;
    private Map<String, Object> mCurrentDayAppointments;
    private Map<String, Object> mUserDaysWithSchedule;
    private String mDayOfTheWeek;
    private String[] mDaySchedule;
    private StringBuilder mSMSBody;
    private String mPhoneNumber;
    private Map<String, String> mUserWorkingDays;
    private String mMessageType;
    private Long mDateInMillisFromService = 0L;
    private Map<String, Object> mUserAppointments;
    private Resources mResources = ContextForStrings.getContext().getResources();

    public threadFindDaysForAppointment(Map<String, String> userWorkingDays,
                                        String userAppointmentDuration) {
        mUserWorkingDays = userWorkingDays;
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
        if (mMessageType.equals("TIME")) {
            mSMSBody.append(mResources.getString(R.string.responses_closest_days));
        }
        mCounterDaysForAppointment = 0;
        mCounterNextDay = 0;
        findHoursForAppointment();
    }

    private void findHoursForAppointment() {
         if(timeToScheduleNotOutside()) {
             Calendar _calendar = getCalendar();
             while (mCounterDaysForAppointment < 3) {
                 if (checkDayFree(_calendar.getTimeInMillis())) {
                     mCounterDaysForAppointment++;
                 }
             }
             sendMessage();
         } else {
             sendMessageOutOfHours();
         }
    }

    private Calendar getCalendar() {
        Calendar _calendar = Calendar.getInstance();
        /* we need current time */
        if (mDateInMillisFromService == 0L) {
            _calendar.setTimeInMillis(System.currentTimeMillis());
        } else {
            /* we have a date */
            _calendar.setTimeInMillis(mDateInMillisFromService);
        }
        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        _calendar.set(Calendar.SECOND, 0);

        return _calendar;
    }

    private boolean timeToScheduleNotOutside() {
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        for (Map.Entry<String, String> _schedule : mUserWorkingDays.entrySet()) {
            if(!_schedule.getValue().equals("Free")) {
                try {
                    Date _closeOrOpenHour = _sDFormat.parse(_schedule.getValue());
                    Date _appHour = _sDFormat.parse(mTimeToSchedule);
                    Log.d("TEST", _closeOrOpenHour + "; " + _appHour);
                    long elapsedTime = _closeOrOpenHour.getTime() - _appHour.getTime();
                    if (elapsedTime > 0) {
                        return true;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void sendMessage() {
//        ArrayList<String> _messageParts = new ArrayList<>(2);
//        int _indexOfComma = mSMSBody.toString().indexOf(":");
//        _messageParts.add(mSMSBody.toString().substring(0, _indexOfComma + 1));
//        _messageParts.add(mSMSBody.toString().substring(_indexOfComma + 2));
//
//        for (String _message : _messageParts) {
//            Log.d("MESSAGE", _message);
//            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, _message, null, null);
//        }
        Log.d("MESSAGE", mSMSBody.toString());
        Log.d("NUMBER", mPhoneNumber);
    }

    private void sendMessageOutOfHours() {
        mSMSBody.delete(0, mSMSBody.length());
//        mSMSBody.append(mResources.getString(R.string.responses_out_of_schedule));
//        SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        Log.d("TEST", "Sent" + mSMSBody.toString());
    }

    private synchronized boolean checkDayFree(long calendarTimeInMillis) {
        final Long _dateInMillisLong;
        final String _dateInMillis;
        mResult = false;

        _dateInMillisLong = (calendarTimeInMillis + (mCounterNextDay * DAY_LENGTH_MILLIS));
        _dateInMillis = _dateInMillisLong.toString();
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
            Log.d("TEST", "USER HAS APP FOR " + _dateInMillis + mDayOfTheWeek);
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
            getDayOfTheWeek(_dateInMillisLong);
            Log.d("TEST", "USER DOES NOT HAVE APP FOR " + _dateInMillis + mDayOfTheWeek);
            mDaySchedule = new String[2];
            mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start");
            mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End");

            if (mDaySchedule[0].equals("Free")) {
                mResult = false;
            } else {
                try {
                    mResult = checkDayForCurrentHour(mTimeToSchedule, false, _dateInMillisLong);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return mResult;

        }

    }


    private boolean checkDayForCurrentHour(String timeToSchedule, boolean dayHasAppointments, Long dateInMillis) throws ParseException {
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        String[] _hoursBefAfter;
        String _hour = mDaySchedule[0];
        Date _dateAppointmentHour = _sDFormat.parse(timeToSchedule);
        Date _date = _sDFormat.parse(_hour);

        long _elapsedTime = _date.getTime() - _dateAppointmentHour.getTime();
        Log.d("TEST TIME", _elapsedTime + "");
        if (_elapsedTime > 0) {
            return false;
        }
        _hoursBefAfter = findAppointmentsBefOrAfter(dayHasAppointments, dateInMillis, _hour, _dateAppointmentHour, _date);
        return foundHour(_hoursBefAfter);
    }

    private boolean foundHour(String[] hoursBefAfter) {
        if (!hoursBefAfter[1].equals("") || !hoursBefAfter[0].equals("")) {
            if (hoursBefAfter[1].equals("")) {
                mSMSBody.append("\n").append(hoursBefAfter[0]);
            } else if (hoursBefAfter[0].equals("")) {
                mSMSBody.append("\n").append(hoursBefAfter[1]);
            } else {
                mSMSBody.append("\n").append(hoursBefAfter[0]).append("\n").append(hoursBefAfter[1]);
            }
            return true;
        }
        else {
            return false;
        }
    }

    private String[] findAppointmentsBefOrAfter(boolean dayHasAppointments, Long dateInMillis, String hour, Date dateAppointmentHour, Date date) throws ParseException {
        int _appointmentDurationInteger = Integer.parseInt(mUserAppointmentDuration);
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        Calendar _calendar = Calendar.getInstance();
        Calendar _calendarToPrint = Calendar.getInstance();
        String[] _hoursBefAfter = {"", ""};

        while (gotTime(hour, mDaySchedule[1]) && _hoursBefAfter[1].equals("")) {
            if (dayHasAppointments && !mCurrentDayAppointments.containsKey(hour)) {
                _hoursBefAfter = getHoursBefOrAfter(date, dateAppointmentHour, dateInMillis, _calendarToPrint);
            } else if (!dayHasAppointments) {
                _hoursBefAfter = getHoursBefOrAfter(date, dateAppointmentHour, dateInMillis, _calendarToPrint);
            }

            _calendar.setTimeInMillis(date.getTime());
            _calendar.add(Calendar.MINUTE, _appointmentDurationInteger);
            hour = _sDFormat.format(_calendar.getTime());
            date = _sDFormat.parse(hour);
        }

        return _hoursBefAfter;
    }

    private String[] getHoursBefOrAfter(Date date, Date dateAppointmentHour, Long dateInMillis, Calendar calendarToPrint) {
        long _elapsedTime = date.getTime() - dateAppointmentHour.getTime();
        SimpleDateFormat _sDFormatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] _hoursBefAfter = {"", ""};

        if (_elapsedTime <= 0 && _elapsedTime > -HOUR_AND_HALF) {
            calendarToPrint.setTimeInMillis(dateInMillis + date.getTime() + TWO_HOURS);
            _hoursBefAfter[0] = _sDFormatDate.format(calendarToPrint.getTime());
        }
        else if (_elapsedTime > 0 && _elapsedTime < HOUR_AND_HALF) {
            calendarToPrint.setTimeInMillis(dateInMillis + date.getTime() + TWO_HOURS);
            _hoursBefAfter[1] = _sDFormatDate.format(calendarToPrint.getTime());
        }

        return _hoursBefAfter;
    }

    // function to get all the appointments for a given date ID
    private void getCurrentDateAppointments(final String dateInMillis) {
        mCurrentDayAppointments = new HashMap<>();
        Object _values = mUserAppointments.containsKey(dateInMillis) ? mUserAppointments.get(dateInMillis) : null;
        if (_values != null) {
            Gson _gson = new Gson();
            String _json = _gson.toJson(_values);
            Log.d("TEST", _json);
            try {
                mCurrentDayAppointments = new ObjectMapper().readValue(_json, Map.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        return elapsedTime > 0;
    }

    private void getDayOfTheWeek(Long dateInMillis) {
        DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.US);
        LocalDate _date = Instant.ofEpochMilli(dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate();

        mDayOfTheWeek = _date.format(_DTF);
    }
}

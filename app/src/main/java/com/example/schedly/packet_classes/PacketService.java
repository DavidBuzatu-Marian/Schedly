package com.example.schedly.packet_classes;

import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.Math.abs;

public class PacketService {
    private Long mDateInMillis;
    private String mUserDaysWithScheduleID;
    private String mUserID;
    private String mUserAppointmentDuration;
    private String mCurrentDaySHoursID;
    private FirebaseFirestore mFireStore;
    private final int OPTION_HOUR = 0, OPTION_DAY = 1;
    private int mCounterNextDay;
    private boolean mFinishedGettingData = false;
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

    private String mUserWorkingDaysID;
    private String mDayOfTheWeek;
    // used for geting start and end hours
    private String[] mDaySchedule;
    // building the sms body
    private StringBuilder mSMSBody;
    private String mPhoneNumber;
    // used to get the required working hours
    private final String[] mDaysOfTheWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public PacketService(String userID, String userAppointmentDuration, String userDaysWithScheduleID, String userWorkingDaysID) {
        mUserID = userID;
        mUserAppointmentDuration = userAppointmentDuration;
        mUserDaysWithScheduleID = userDaysWithScheduleID;
        mUserWorkingDaysID = userWorkingDaysID;
        mFireStore = FirebaseFirestore.getInstance();
    }


    public Long getDateInMillis(String _dateFromUser) {
        Calendar _calendar = Calendar.getInstance();
        SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("YYYY-MM-DD");
        Date _date;
        try {
            _date = _simpleDateFormat.parse(_dateFromUser);
            _calendar.setTimeInMillis(_date.getTime());
        } catch (ParseException e) {
            Log.d("FirebaseExc", "Exception");
            e.printStackTrace();
        }
        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        _calendar.set(Calendar.SECOND, 0);
        Log.d("FirebaseTime", _calendar.getTimeInMillis() + "");
        mDateInMillis = _calendar.getTimeInMillis();
        return mDateInMillis;
    }


    private void getDayOfTheWeek(Long mDateInMillis) {
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(mDateInMillis);
        mDayOfTheWeek = mDaysOfTheWeek[_calendar.get(Calendar.DAY_OF_WEEK)];
    }

    public void getCurrentDateSHoursID(String _dateFromUser, String _phoneNumber) {
        mPhoneNumber = _phoneNumber;
        getDateInMillis(_dateFromUser);
        Log.d("FirebaseTime", _dateFromUser);
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        mCurrentDaySHoursID = documentSnapshot.get(mDateInMillis.toString()) != null
                                ? documentSnapshot.get(mDateInMillis.toString()).toString() : "";
                        /* we dont have this day in scheduled hours */
                        Log.d("Firebase", mCurrentDaySHoursID);
                        if(mCurrentDaySHoursID.equals("")) {
                            addScheduledHoursCollection();
                        }
                        else {
                            getCurrentDateAppointments(mCurrentDaySHoursID, OPTION_HOUR, mDateInMillis);
                        }
                    }
                });
    }

    private void addScheduledHoursCollection() {
        Map<String, Object> addDaysWithScheduleID = new HashMap<>();
        addDaysWithScheduleID.put("5:00", null);
        Log.d("Firebaseservice", "Added ID for this day");
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .add(addDaysWithScheduleID)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        addThisDateScheduledHoursID(documentReference.getId());
                        Log.d("FirebaseworkingDays", "Succes with scheduled hours");
                    }
                });
    }

    private void addThisDateScheduledHoursID(final String id) {
        Map<String, Object> addToCurrentDateID = new HashMap<>();
        addToCurrentDateID.put(mDateInMillis.toString(), id);
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .update(addToCurrentDateID)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mCurrentDaySHoursID = id;
                        Log.d("FirebaseworkingDays", "Succes with id");
                        getUserWorkingHoursForCurrentDate(mDateInMillis, 0);
                    }
                });
    }

    private void getUserWorkingHoursForCurrentDate(Long mDateInMillis, final int option) {
        getDayOfTheWeek(mDateInMillis);
        mFireStore.collection("workingDays")
                .document(mUserWorkingDaysID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        mDaySchedule = new String[2];
                        Log.d("Firebaseworking", mDayOfTheWeek + documentSnapshot.getId());
                        mDaySchedule[0] = documentSnapshot.get(mDayOfTheWeek + "Start").toString();
                        mDaySchedule[1] = documentSnapshot.get(mDayOfTheWeek + "End").toString();
                        if(option == OPTION_HOUR) {
                            sendScheduleOptionsWrapper();
                        }
                        else if(option == OPTION_DAY){
                            mFinishedGettingData = true;
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(option == OPTION_DAY) {
                            mFinishedGettingData = true;
                        }
                    }
                });

    }


    // check if user is working first on that day
    private void sendScheduleOptionsWrapper() {
        if(mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on this day. Please try another date or call me");
            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        }
        else {
            try {
                Log.d("FirebaseSEND", "NotFree");
                sendScheduleOptions();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendScheduleOptions() throws ParseException {
        String _hour = mDaySchedule[0];
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        Date _date;
        Calendar _calendar = Calendar.getInstance();
        mSMSBody = new StringBuilder("For this date, I can take you on:");
        while(gotTime(_hour, mDaySchedule[1])) {
            if(!mCurrentDayAppointments.containsKey(_hour)) {
                mSMSBody.append("\n").append(_hour);
            }
            _date = _sDFormat.parse(_hour);
            _calendar.setTime(_date);
            _calendar.add(Calendar.MINUTE, Integer.parseInt(mUserAppointmentDuration));
            _hour = _sDFormat.format(_calendar.getTime());
        }
        if(_sDFormat.toString().contains("\n")) {
            SmsManager.getDefault()
                    .sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        }
        else {
            mSMSBody.delete(0, mSMSBody.length() - 1);
            mSMSBody.append("I am sorry, but this day if already full! Please try another date");
            SmsManager.getDefault()
                    .sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        }
    }

    // check if we still are in the working hours
    private boolean gotTime(String startTime, String endTime) throws ParseException {
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        Date d1 = _sDFormat.parse(startTime);
        Date d2 = _sDFormat.parse(endTime);
        long elapsedTime = d2.getTime() - d1.getTime();
        Log.d("Firebase", elapsedTime + "");
        Log.d("Firebase", startTime + "; " + endTime);
        return elapsedTime > 0;
    }

    // function to get all the appointments for a given date ID
    private void getCurrentDateAppointments(String currentDaySHoursID, final int option, final Long time) {
        mCurrentDayAppointments = new HashMap<>();
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .document(currentDaySHoursID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        mCurrentDayAppointments = documentSnapshot.getData();
                        if(option == OPTION_HOUR) {
                            getUserWorkingHoursForCurrentDate(time, option);
                        }
                        else if (option == OPTION_DAY) {
                            getUserWorkingHoursForCurrentDate(time, option);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(option == OPTION_DAY) {
                            mFinishedGettingData = true;
                        }
                    }
                });
    }


    /* this is called when we get a message
     * with only a date
     * we start by getting all the days with schedule
     */
    public void getAllDaysIDs(final String mTime, String mMessagePhoneNumber) {
        mTimeToSchedule = mTime;
        mPhoneNumber = mMessagePhoneNumber;
        mFireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        mUserDaysWithSchedule = documentSnapshot.getData();

                        findDaysForAppointment();
                    }
                });
    }

    private void findDaysForAppointment() {
        /* this counter is used to get
         *  the 3 closest days for the
         * appointment
         */
        mSMSBody = new StringBuilder("These are the closest days I can take you in:");
        int _counterDaysForAppointment = 0;
        // this counter is used for getting the next days
        mCounterNextDay = 0;

        while(_counterDaysForAppointment < 3) {
            if(checkDayFree()) {
                _counterDaysForAppointment++;
                Log.d("FirebaseDATE", mSMSBody.toString());
            }
            Log.d("FirebaseDATE", _counterDaysForAppointment + "");
        }

        SmsManager.getDefault()
                .sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
    }

    private boolean checkDayFree() {
        Long _dateInMillisLong;
        String _dateInMillis;
        String _dateFromUserScheduledHoursID;


        mCounterNextDay++;
        mFinishedGettingData = false;
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(System.currentTimeMillis());
        _calendar.set(Calendar.HOUR_OF_DAY, 0);
        _calendar.set(Calendar.MINUTE, 0);
        _calendar.set(Calendar.MILLISECOND, 0);
        _calendar.set(Calendar.SECOND, 0);

        _dateInMillisLong = (_calendar.getTimeInMillis() + mCounterNextDay * DAY_LENGTH_MILLIS);
        _dateInMillis = _dateInMillisLong.toString();
        Log.d("FirebaseDate", _dateInMillis);
        _dateFromUserScheduledHoursID = mUserDaysWithSchedule.get(_dateInMillis).toString();
        Log.d("FirebaseDate", _dateFromUserScheduledHoursID);
        // get the data, wait until process finishes
        /* we already have appointments on this day
         * we need extra checking for each time
         * already appointed
         */
        if(mUserDaysWithSchedule.containsKey(_dateInMillis)) {
            getCurrentDateAppointments(_dateFromUserScheduledHoursID, OPTION_DAY, _dateInMillisLong);
            while(!mFinishedGettingData);
            try {
                /* if return is true
                 * then we found a valid time
                 * before or after 1 hour and a half
                 * if return is false
                 * we need to try the next date
                 */
                return checkDayForCurrentHour(mTimeToSchedule, true);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        /* this day is free
         * we need to add it first
         * then we need to find a good hour
         * which fits with the appointment duration
         * we do all this asynchronously
         */
        else {
            getUserWorkingHoursForCurrentDate(_dateInMillisLong, OPTION_DAY);
            while(!mFinishedGettingData);
            // we got a non-working day
            if(mDaySchedule[0].equals("Free")) {
                return false;
            } else {
                // we got a working day. Start work
                try {
                    return checkDayForCurrentHour(mTimeToSchedule, false);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }


        return true;
    }

    private boolean checkDayForCurrentHour(String mTimeToSchedule, boolean dayHasAppointments) throws ParseException {
        int _appointmentDurationInteger = Integer.parseInt(mUserAppointmentDuration);
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        String _dateBefore = "", _dateAfter = "";
        String _hour = mDaySchedule[0];
        Calendar _calendar = Calendar.getInstance();
        Date _dateCurrentHour = _sDFormat.parse(mTimeToSchedule);
        Date _date = _sDFormat.parse(_hour);
        long _elapsedTime = _date.getTime() - _dateCurrentHour.getTime();
        if(_elapsedTime > 0) {
            // hour is outside of starting hour of work
            return false;
        }

        while(gotTime(_hour, mDaySchedule[1])) {
            if(dayHasAppointments && !mCurrentDayAppointments.containsKey(_hour)) {
                _elapsedTime = _date.getTime() - _dateCurrentHour.getTime();
                /* hour is before our current one
                 * and is not more than 1 hour and a half
                */
                if(_elapsedTime < 0 && _elapsedTime > -5400000) {
                    _dateBefore = _date.toString();
                }
                /* hour is after our current one
                 * and is not more than 1 hour and a half
                 */
                else if(_elapsedTime > 0 && _elapsedTime < 5400000) {
                    _dateAfter = _date.toString();
                    break;
                }
            }
            else if (!dayHasAppointments) {
                _elapsedTime = _date.getTime() - _dateCurrentHour.getTime();
                /* hour is before our current one
                 * and is not more than 1 hour and a half
                 */
                if(_elapsedTime < 0 && _elapsedTime > -5400000) {
                    _dateBefore = _date.toString();
                }
                /* hour is after our current one
                 * and is not more than 1 hour and a half
                 */
                else if(_elapsedTime > 0 && _elapsedTime < 5400000) {
                    _dateAfter = _date.toString();
                    break;
                }
            }

            _calendar.setTime(_date);
            _calendar.add(Calendar.MINUTE, _appointmentDurationInteger);
            _hour = _sDFormat.format(_calendar.getTime());
            _date = _sDFormat.parse(_hour);
        }
        // we found at least an hour in that day
        if(!_dateAfter.equals("")) {
            mSMSBody.append("\n").append(_dateBefore).append(_dateAfter);
            return true;
        }
        // day is full, try another time
        else {
            return false;
        }
    }


    public void saveAppointmentToDatabase() {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        final DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .document("00JQopKY8fElhI9i04yg");
        Map<String, Object> _appointments = new HashMap<>(16);
        Map<String, String> _values = new HashMap<>(16);
        for(int hours = 8; hours < 22; hours++) {
            _appointments.put("" + hours + ":00",  _values);
        }

        _documentReference.update(_appointments);

    }
}

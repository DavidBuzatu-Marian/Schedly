package com.example.schedly.packet_classes;

import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.schedly.thread.threadFindDaysForAppointment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PacketService {
    private String mContactName;
    private Long mDateInMillis;
    private String mUserDaysWithScheduleID;
    private String mUserID;
    private String mUserAppointmentDuration;
    private String mCurrentDaySHoursID;
    private FirebaseFirestore mFireStore;
    private threadFindDaysForAppointment mWorkThread;
    // get the appointments for a given date
    private Map<String, Object> mCurrentDayAppointments;
    /* variable used to know the hour
     * to make the schedule for this client
     */
    private String mTimeToSchedule;
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
    private volatile Map<String, Object> mUserWorkingDays;

    public PacketService(String userID, String userAppointmentDuration, String userDaysWithScheduleID, String userWorkingDaysID) {
        mUserID = userID;
        mUserAppointmentDuration = userAppointmentDuration;
        mUserDaysWithScheduleID = userDaysWithScheduleID;
        mUserWorkingDaysID = userWorkingDaysID;
        mFireStore = FirebaseFirestore.getInstance();
        getUserWorkingHours();
    }


    private void sendMessage() {
        SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
    }

    private void getDateInMillis(String dateFromUser) {
        Calendar _calendar = Calendar.getInstance();
        SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date _date;
        try {
            _date = _simpleDateFormat.parse(dateFromUser);
            Log.d("Firebase", _date.toString());
            _calendar.setTimeInMillis(_simpleDateFormat.parse(dateFromUser).getTime());
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
    }


    private void getDayOfTheWeek(Long dateInMillis) {
        Calendar _calendar = Calendar.getInstance();
        _calendar.setTimeInMillis(dateInMillis);
        mDayOfTheWeek = mDaysOfTheWeek[_calendar.get(Calendar.DAY_OF_WEEK) - 1];
        Log.d("FirebaseDay", mDayOfTheWeek + "; " + _calendar.get(Calendar.DAY_OF_WEEK));
    }
    private void getUserWorkingHours() {
        Log.d("Firebase", mUserWorkingDaysID);
        mFireStore.collection("workingDays")
                .document(mUserWorkingDaysID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        mUserWorkingDays = documentSnapshot.getData();
                        mWorkThread = new threadFindDaysForAppointment(mUserDaysWithScheduleID,
                                mUserWorkingDays,
                                mFireStore,
                                mUserAppointmentDuration);
                        Log.d("Firebase", "Succes getting working hours!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Firebase", "Code failed");
                    }
                });
    }

    /* used to get hours for a day
     * message type can be for
     * message with DATE only. messageType = 'DATE'
     * message with DATE and TIME. messageType = 'FULL'
     */
    public void getCurrentDateSHoursID(final String dateFromUser, String phoneNumber, final String messageType) {
        mPhoneNumber = phoneNumber;
        getDateInMillis(dateFromUser);
        Log.d("FirebaseTime", dateFromUser);
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
                            addScheduledHoursCollection(messageType);
                        }
                        else {
                            getCurrentDateAppointments(mCurrentDaySHoursID, mDateInMillis, messageType, dateFromUser);
                        }
                    }
                });
    }
    // function to get all the appointments for a given date ID
    private void getCurrentDateAppointments(String currentDaySHoursID, final Long dateInMillis, final String messageType, final String dateFromUser) {
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
                        if(messageType.equals("DATE")) {
                            sendScheduleOptionsWrapper(dateInMillis);
                        }
                        else if(messageType.equals("FULL")) {
                            checkIfAppointmentCanBeMade(dateFromUser);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("FirebaseFailService", e.toString());
                    }
                });
    }

    /* if this user doesnt have appointments for this day */
    private void addScheduledHoursCollection(final String messageType) {
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
                        addThisDateScheduledHoursID(documentReference.getId(), messageType);
                        Log.d("FirebaseworkingDays", "Succes with scheduled hours");
                    }
                });
    }

    private void addThisDateScheduledHoursID(final String id, final String messageType) {
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
                        if(messageType.equals("DATE")) {
                            sendScheduleOptionsWrapper(mDateInMillis);
                        }
                    }
                });
    }


    // check if user is working first on that day
    private void sendScheduleOptionsWrapper(Long dateInMillis) {
        getDayOfTheWeek(dateInMillis);
        mDaySchedule = new String[2];
        mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start").toString();
        mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End").toString();

        Log.d("Firebase", "Getting the values for: " + mDayOfTheWeek + ": " + mDaySchedule[0] + "; " + mDaySchedule[1]);

        if(mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on this day. Please try another date, time or call me");
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

    /* all hours from the day when the appointment can be made */
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
        if(mSMSBody.toString().contains("\n")) {
            sendMessage();
        }
        else {
            mSMSBody.delete(0, mSMSBody.length() - 1);
            mSMSBody.append("I am sorry, but this day if already full! Please try another date");
            sendMessage();
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


    /* ************************************************************************************* */

    /* this part is used for the time message */

    /* this is called when we get a message
     * with only a time
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
                        findDaysForAppointment("TIME");
                    }
                });
    }

    /* start the thread with the necessary operations */

    private void findDaysForAppointment(String messageType) {
        /* this counter is used to get
         *  the 3 closest days for the
         * appointment
         */
        mWorkThread.setmPhoneNumber(mPhoneNumber);
        mWorkThread.setmTimeToSchedule(mTimeToSchedule);
        mWorkThread.setmUserDaysWithSchedule(mUserDaysWithSchedule);
        mWorkThread.setmMessageType(messageType);
        mWorkThread.start();
    }

    /* ************************************************************************************* */


    /* this part is called for a time and date message */
    public void makeAppointmentForFixedParameters(String dateFromUser, String time, String messagePhoneNumber, String contactName) {
        mTimeToSchedule = time;
        mContactName = contactName;

        getCurrentDateSHoursID(dateFromUser, messagePhoneNumber, "FULL");
    }


    /* try to appoint at the exact date and time
     * if not, get back the hours possible
     * or the days for which this hour works
     */
    private void checkIfAppointmentCanBeMade(String dateFromUser) {
        getDayOfTheWeek(mDateInMillis);
        mDaySchedule = new String[2];
        mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start").toString();
        mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End").toString();

        Log.d("Firebase", "Getting the values for: " + mDayOfTheWeek + ": " + mDaySchedule[0] + "; " + mDaySchedule[1]);

        if(mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on" + dateFromUser + ". But please chose one convenient date from below:\n");
            findDaysForAppointment("FULL");
        }
        else {
            /* not a free day */
            String _hour = mDaySchedule[0];
            SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
            Date _date;
            Calendar _calendar = Calendar.getInstance();
            mSMSBody = new StringBuilder();
            try {
                while (gotTime(_hour, mDaySchedule[1])) {
                    int _status = 0;
                    if (mCurrentDayAppointments.containsKey(_hour)) {
                        mWorkThread.setmSMSBody("I am sorry, but this date and hour are already scheduled. You can try one of these other days: \n");
                        findDaysForAppointment("FULL");
                        break;
                    }
                    switch (getClosestHour(_hour, mTimeToSchedule)) {
                        case 1:
                            mSMSBody.append("Alright! I can take you in at: ").append(_hour).append("Please send a confirmation message with the date and time");
                            sendMessage();
                            _status = 1;
                            break;
                        case 2:
                            mSMSBody.append("Alright! You scheduled yourself on: ").append(_hour);
                            sendMessage();
                            saveAppointmentToDatabase(_hour);
                            _status = 1;
                            break;
                    }
                    if(_status == 1) {
                        break;
                    }
                    _date = _sDFormat.parse(_hour);
                    _calendar.setTime(_date);
                    _calendar.add(Calendar.MINUTE, Integer.parseInt(mUserAppointmentDuration));
                    _hour = _sDFormat.format(_calendar.getTime());
                }
                if (_sDFormat.toString().contains("\n")) {
                    sendMessage();
                } else {
                    mSMSBody.delete(0, mSMSBody.length() - 1);
                    mSMSBody.append("I am sorry, but this day if already full! Please try another date");
                    sendMessage();
                }
            }
            catch (ParseException ex) {
                Log.d("ERROR-SERVICE", ex.toString());
            }
        }
    }

    private int getClosestHour(String currentTime, String scheduleTime) {
        /* we send back the option
         * option = 1 -> around that time is okay
         * option = 2 -> perfect time, schedule immediately
         * option = 0 -> not good
         */
        SimpleDateFormat _sDFormat = new SimpleDateFormat("HH:mm");
        try {
            /* transform minutes to desired format */
            int _appointmentInMinutes = Integer.parseInt(mUserAppointmentDuration);
            int _hours = _appointmentInMinutes / 60;
            int _minutes = _appointmentInMinutes % 60;
            String _appointmentDurationString = _hours + ":" + _minutes;
            Date _dateForAppDur = _sDFormat.parse(_appointmentDurationString);
            long _appointmentDuration = _dateForAppDur.getTime();

            Date _date1 = _sDFormat.parse(currentTime);
            Date _date2 = _sDFormat.parse(scheduleTime);
            long _elapsedTime = Math.abs(_date2.getTime() - _date1.getTime());
            if(_elapsedTime < _appointmentDuration) {
                /* we can put him in a time near the request */
                return 1;
            }
            else if(_elapsedTime == _appointmentDuration) {
                return 2;
            }
        } catch (ParseException ex) {
            Log.d("ERROR - getclosesthour", ex.toString());
        }
        return 0;
    }


    /* ************************************************************************************* */
    /* save the appointment in the database */
    private void saveAppointmentToDatabase(String hour) {
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        final DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
                .document(mUserDaysWithScheduleID)
                .collection("scheduledHours")
                .document(mCurrentDaySHoursID);
        Map<String, String> _detailsOfAppointment = new HashMap<>();
        _detailsOfAppointment.put("PhoneNumber", mPhoneNumber);
        _detailsOfAppointment.put("Name", mContactName.equals("") ? null : mContactName);
        Map<String, Object> _appointment = new HashMap<>();
        _appointment.put(hour, _detailsOfAppointment);

        _documentReference.update(_appointment);

    }

//    public boolean isThreadWorkFinished() {
//        return mResultForService.get();
//    }
}

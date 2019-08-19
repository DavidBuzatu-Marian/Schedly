package com.example.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.schedly.thread.threadFindDaysForAppointment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        //SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        Log.d("MESSAGE", mSMSBody.toString());
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
                        if (mCurrentDaySHoursID.equals("")) {
                            addScheduledHoursCollection(messageType);
                        } else {
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
                        if (messageType.equals("DATE")) {
                            sendScheduleOptionsWrapper(dateInMillis);
                        } else if (messageType.equals("FULL")) {
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
                        if (messageType.equals("DATE")) {
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

        if (mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on this day. Please try another date, time or call me");
            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        } else {
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
        while (gotTime(_hour, mDaySchedule[1])) {
            if (!mCurrentDayAppointments.containsKey(_hour)) {
                mSMSBody.append("\n").append(_hour);
            }
            _date = _sDFormat.parse(_hour);
            _calendar.setTime(_date);
            _calendar.add(Calendar.MINUTE, Integer.parseInt(mUserAppointmentDuration));
            _hour = _sDFormat.format(_calendar.getTime());
        }
        if (mSMSBody.toString().contains("\n")) {
            sendMessage();
        } else {
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

        if (mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on" + dateFromUser + ". But please chose one convenient date from below:\n");
            findDaysForAppointment("FULL");
        } else {
            /* not a free day */
            String _hour = mDaySchedule[0];
            mSMSBody = new StringBuilder();
            if (mCurrentDayAppointments.containsKey(mTimeToSchedule)) {
                mWorkThread.setmSMSBody("I am sorry, but this date and hour are already scheduled. If you want a schedule for this day, send a message with this date or you can try one of these other days: \n");
                findDaysForAppointment("FULL");
            }
            getClosestHour(_hour, mTimeToSchedule);
        }
    }

    private void getClosestHour(String currentTime, String scheduleTime) {
        /* we get appointment time in millis
         * appointment duration in millis
         * current time in millis
         */
        int _appointmentInMinutes = Integer.parseInt(mUserAppointmentDuration);
        long _appointmentDuration = TimeUnit.HOURS.toMillis(_appointmentInMinutes / 60) + TimeUnit.MINUTES.toMillis(_appointmentInMinutes % 60);
        Log.d("AppointmentDurr", _appointmentDuration + "");

        String[] _curTimeHm = currentTime.split(":");
        String[] _scheduleTimeHm = scheduleTime.split(":");
        String[] _closeTimeHm = mDaySchedule[1].split(":");

        int _timeH = Integer.parseInt(_curTimeHm[0]), _timeM = Integer.parseInt(_curTimeHm[1]);
        long _curTimeMil = TimeUnit.HOURS.toMillis(_timeH) + TimeUnit.MINUTES.toMillis(_timeM);
        long _closeTimeMil = TimeUnit.HOURS.toMillis(Integer.parseInt(_closeTimeHm[0])) + TimeUnit.MINUTES.toMillis(Integer.parseInt(_closeTimeHm[1]));
        long _scheduleTimeMil = TimeUnit.HOURS.toMillis(Integer.parseInt(_scheduleTimeHm[0])) + TimeUnit.MINUTES.toMillis(Integer.parseInt(_scheduleTimeHm[1]));
        Log.d("App", "Here: " + _curTimeMil);
        /* check if this exact time is available */
        String _fixHour = checkExactTime(_curTimeMil, _closeTimeMil, _scheduleTimeMil, _appointmentDuration);
        if(_fixHour != null) {
            Log.d("aPP", _fixHour);
            if (!mCurrentDayAppointments.containsKey(_fixHour)) {
                Log.d("Appoint", "REturn 2" + ": " + _fixHour);
                mSMSBody.append("Alright! You scheduled yourself on: ").append(_fixHour);
                sendMessage();
                saveAppointmentToDatabase(_fixHour);
            }
        } else {
            /* until we have time and we are not stuck
             * try to find the perfect time
             */
            Log.d("App", "Hre: " + _curTimeMil);
            while ((_closeTimeMil - _curTimeMil) > 0) {
                /* find the difference between current time and
                 * schedule time
                 */
                long _elapsedTime = Math.abs(_curTimeMil - _scheduleTimeMil);
                Log.d("AppointmentDreee", _elapsedTime + "");

                /* we got in range of our hour
                 * we check if here or after appointment duration
                 * is free and return one or both of hours
                 */
                if (_elapsedTime < _appointmentDuration) {
                    @SuppressLint("DefaultLocale") String _hourBefore = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toHours(_curTimeMil),
                            TimeUnit.MILLISECONDS.toMinutes(_curTimeMil) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(_curTimeMil))
                    );
                    @SuppressLint("DefaultLocale") String _hourAfter = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toHours(_curTimeMil + _appointmentDuration),
                            TimeUnit.MILLISECONDS.toMinutes(_curTimeMil + _appointmentDuration) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(_curTimeMil + _appointmentDuration))
                    );
                    /* we do not have an appointment at this hour
                     * we can put him in a time near the request
                     * before or after the requested time
                     */
                    boolean _freeBefore = !mCurrentDayAppointments.containsKey(_hourBefore);
                    boolean _freeAfter = !mCurrentDayAppointments.containsKey(_hourAfter);

                    if (_freeBefore || _freeAfter) {
                        Log.d("Appointment", "MESSAGE SENT: " + _hourBefore + " ... AFTER: " + _hourAfter);
                        mSMSBody.append("Alright! I can take you in at: ")
                                .append(_freeBefore ? ("\n" + _hourBefore) : "")
                                .append(_freeAfter ? ("\n" + _hourAfter) : "")
                                .append(". Please send a confirmation message with the date and time");
                        sendMessage();
                        Log.d("Appoint", "REturn 1");
                    } else {
                        sendScheduleOptionsWrapper(mDateInMillis);
                    }

                    break;
                }

                _curTimeMil += _appointmentDuration;
            }
        }
    }

    private String checkExactTime(long curTimeMil, long closeTimeMil, long scheduleTimeMil, long appointmentDuration) {
        long _curTime = curTimeMil;
        while(_curTime < scheduleTimeMil && _curTime < closeTimeMil) {
            _curTime += appointmentDuration;
        }
        @SuppressLint("DefaultLocale") String _hour = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(_curTime),
                TimeUnit.MILLISECONDS.toMinutes(_curTime) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(_curTime))
        );
        if ((_curTime == scheduleTimeMil) && !mCurrentDayAppointments.containsKey(_hour)) {
            return _hour;
        }
        return null;
    }


    /* ************************************************************************************* */
    /* save the appointment in the database */
    private void saveAppointmentToDatabase(String hour) {
        Log.d("APPointment", mCurrentDaySHoursID + ": " + hour);
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

        _documentReference.update(_appointment).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("AppErr", e.toString());
            }
        });

    }

//    public boolean isThreadWorkFinished() {
//        return mResultForService.get();
//    }
}

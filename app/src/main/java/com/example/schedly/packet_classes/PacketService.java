package com.example.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.schedly.model.Appointment;
import com.example.schedly.thread.threadFindDaysForAppointment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PacketService {
    private String mContactName;
    private Long mDateInMillis;
    private String mUserID;
    private String mUserAppointmentDuration;
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
    private volatile Map<String, String> mUserWorkingDays;
    private Integer mNrOfAppointmentsForDate;

    public PacketService(String userID, String userAppointmentDuration, String userWorkingDaysID) {
        mUserID = userID;
        mUserAppointmentDuration = userAppointmentDuration;
        mUserWorkingDaysID = userWorkingDaysID;
        mFireStore = FirebaseFirestore.getInstance();
    }


    private void sendMessage() {
        SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        Log.d("MESSAGE", mSMSBody.toString());
    }


    private void getDayOfTheWeek(Long dateInMillis) {
        DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
        LocalDate _date = Instant.ofEpochMilli(dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        Log.d("FirebaseDay2", _date.format(_DTF));
        mDayOfTheWeek = _date.format(_DTF);
    }

    public void setUserWorkingHours(HashMap<String, String> workingHours) {
        mUserWorkingDays = workingHours;
        Log.d("Firebase", "Succes setting working hours!");
        mWorkThread = new threadFindDaysForAppointment(mUserWorkingDays,
                mFireStore,
                mUserAppointmentDuration);
    }

    /* used to get hours for a day
     * message type can be for
     * message with DATE only. messageType = 'DATE'
     * message with DATE and TIME. messageType = 'FULL'
     */
    public void getCurrentDate(final String dateFromUser, Long dateInMillis, String phoneNumber, final String messageType) {
        mPhoneNumber = phoneNumber;
        mDateInMillis = dateInMillis;
        Log.d("FirebaseTime-ServiceGET", mDateInMillis + "");
        mFireStore.collection("scheduledHours")
                .document(mUserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            Map<String, Object> _map = task.getResult().getData();
                            assert _map != null;
                            Object _values = _map.containsKey(mDateInMillis.toString()) ? _map.get(mDateInMillis.toString()) : null;
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
                        if (messageType.equals("DATE")) {
                            sendScheduleOptionsWrapper(mDateInMillis);
                        } else if (messageType.equals("FULL")) {
                            checkIfAppointmentCanBeMade(dateFromUser);
                        }
                    }
                });
    }


    // check if user is working first on that day
    private void sendScheduleOptionsWrapper(Long dateInMillis) {
        getDayOfTheWeek(dateInMillis);
        mDaySchedule = new String[2];
        mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start");
        mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End");

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
            if (mCurrentDayAppointments == null || !mCurrentDayAppointments.containsKey(_hour)) {
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
    public void getScheduledDays(final String mTime, String mMessagePhoneNumber, final String messageType) {
        mTimeToSchedule = mTime;
        mPhoneNumber = mMessagePhoneNumber;
        mFireStore.collection("scheduledHours")
                .document(mUserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            mUserDaysWithSchedule = task.getResult().getData();
                            findDaysForAppointment(messageType);
                        }
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
        mWorkThread.setmUserID(mUserID);
        mWorkThread.setmTimeToSchedule(mTimeToSchedule);
        mWorkThread.setmUserDaysWithSchedule(mUserDaysWithSchedule);
        mWorkThread.setmMessageType(messageType);
        mWorkThread.start();
    }

    /* ************************************************************************************* */


    /* this part is called for a time and date message */
    public void makeAppointmentForFixedParameters(String dateFromUser, Long dateInMillis,
                                                  String time, String messagePhoneNumber,
                                                  String contactName) {
        mTimeToSchedule = time;
        mContactName = contactName;

        getCurrentDate(dateFromUser, dateInMillis, messagePhoneNumber, "FULL");
    }


    /* try to appoint at the exact date and time
     * if not, get back the hours possible
     * or the days for which this hour works
     */
    private void checkIfAppointmentCanBeMade(String dateFromUser) {
        getDayOfTheWeek(mDateInMillis);
        mDaySchedule = new String[2];
        mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start");
        mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End");

        Log.d("Firebase", "Getting the values for: " + mDayOfTheWeek + ": " + mDaySchedule[0] + "; " + mDaySchedule[1]);

        if (mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on " + dateFromUser + ". But please chose one convenient date from below:");
            mWorkThread.setmSMSBody(mSMSBody.toString());
            getScheduledDays(mTimeToSchedule, mPhoneNumber, "FULL");
        } else {
            /* not a free day */
            String _hour = mDaySchedule[0];
            mSMSBody = new StringBuilder();
            if (mCurrentDayAppointments != null && mCurrentDayAppointments.containsKey(mTimeToSchedule)) {
                mWorkThread.setmDateInMillisFromService(mDateInMillis);
                mWorkThread.setmSMSBody("This date and hour are already scheduled. If you want a schedule for this day, send a message with this date or you can try one of these other days:");
                Log.d("APP_ERROR", "This is scheduled.");
                getScheduledDays(mTimeToSchedule, mPhoneNumber, "FULL");
            } else {
                getClosestHour(_hour, mTimeToSchedule, dateFromUser);
            }
        }
    }


    /* *********************** make this one smoother!!!! *************************************** */
    private void getClosestHour(String currentTime, String scheduleTime, String dateFromUser) {
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

        int _timeH = Integer.parseInt(_curTimeHm[0]),
                _timeM = Integer.parseInt(_curTimeHm[1]);
        long _curTimeMil = TimeUnit.HOURS.toMillis(_timeH) + TimeUnit.MINUTES.toMillis(_timeM);
        long _closeTimeMil = TimeUnit.HOURS.toMillis(Integer.parseInt(_closeTimeHm[0])) + TimeUnit.MINUTES.toMillis(Integer.parseInt(_closeTimeHm[1]));
        long _scheduleTimeMil = TimeUnit.HOURS.toMillis(Integer.parseInt(_scheduleTimeHm[0])) + TimeUnit.MINUTES.toMillis(Integer.parseInt(_scheduleTimeHm[1]));

        Log.d("App", "Here: " + _curTimeMil);
        /* check if this exact time is available */
        String _fixHour = checkExactTime(_curTimeMil, _closeTimeMil, _scheduleTimeMil, _appointmentDuration);
        if (_fixHour != null && (mCurrentDayAppointments == null || !mCurrentDayAppointments.containsKey(_fixHour))) {
            Log.d("Appoint", "REturn fixed appointment successfully" + ": " + _fixHour);
            mSMSBody.append("Alright! You scheduled yourself on ").append(dateFromUser).append(", at:").append(_fixHour);
            sendMessage();
            saveAppointmentToDatabase(_fixHour);
        } else if (_fixHour == null) {
            /* until we have time and we are not stuck
             * try to find the perfect time
             */
            Log.d("App", "Hre: " + _curTimeMil);

            /* variable to know if time was found */
            boolean _foundTime = false;
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
                if (_elapsedTime <= _appointmentDuration && (_curTimeMil + _appointmentDuration < _closeTimeMil)) {
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
                                .append(_freeBefore ? ("" + _hourBefore) : "")
                                .append(_freeAfter ? (" " + _hourAfter) : "")
                                .append(". Please send a confirmation message with the date and time");
                        sendMessage();
                        Log.d("Appoint", "REturn message send with confirmation");
                    } else {
                        sendScheduleOptionsWrapper(mDateInMillis);
                    }
                    _foundTime = true;
                    break;
                }
                _curTimeMil += _appointmentDuration;
            }

            if (!_foundTime) {
                sendScheduleOptionsWrapper(mDateInMillis);
            }
        }
    }

    private String checkExactTime(long curTimeMil, long closeTimeMil, long scheduleTimeMil, long appointmentDuration) {
        long _curTime = curTimeMil;
        while (_curTime < scheduleTimeMil && _curTime < closeTimeMil) {
            _curTime += appointmentDuration;
        }
        @SuppressLint("DefaultLocale") String _hour = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(_curTime),
                TimeUnit.MILLISECONDS.toMinutes(_curTime) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(_curTime))
        );
        if ((_curTime == scheduleTimeMil) && (mCurrentDayAppointments == null || !mCurrentDayAppointments.containsKey(_hour))) {
            return _hour;
        }

        return null;
    }


    /* ************************************************************************************* */
    /* save the appointment in the database */
    private void saveAppointmentToDatabase(String hour) {
        Map<String, String> _detailsOfAppointment = new HashMap<>();
        _detailsOfAppointment.put("PhoneNumber", mPhoneNumber);
        _detailsOfAppointment.put("Name", mContactName.equals("") ? null : mContactName);
        Map<String, Object> _hourAndInfo = new HashMap<>();
        _hourAndInfo.put(hour, _detailsOfAppointment);
        Map<String, Object> _appointment = new HashMap<>();
        _appointment.put(mDateInMillis.toString(), _hourAndInfo);

        FirebaseFirestore.getInstance().collection("scheduledHours")
                .document(mUserID)
                .set(_appointment, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        increaseNrOfAppointments(mDateInMillis.toString(), mUserID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("AppErrSavePS", e.toString());
                    }
                });

    }

    private void increaseNrOfAppointments(String date, String userID) {
        mNrOfAppointmentsForDate++;
        Map<String, String> _date = new HashMap<>();
        _date.put(date, mNrOfAppointmentsForDate.toString());
        Map<String, Object> _infoForThisUser = new HashMap<>();
        _infoForThisUser.put(userID, _date);
        FirebaseFirestore.getInstance().collection("phoneNumbersFromClients")
                .document(mPhoneNumber)
                .set(_infoForThisUser, SetOptions.merge())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("AppErrNROfAppPS", e.toString());
                    }
                });
    }

//    public boolean isThreadWorkFinished() {
//        return mResultForService.get();
//    }

    public void setNrOfAppointmentsForNumber(int nrOfAppointmentsForDate) {
        mNrOfAppointmentsForDate = nrOfAppointmentsForDate;
    }
}

package com.davidbuzatu.schedly.packet_classes;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.telephony.SmsManager;


import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.model.ContextForStrings;
import com.davidbuzatu.schedly.model.ScheduledHours;
import com.davidbuzatu.schedly.model.User;
import com.davidbuzatu.schedly.thread.threadFindDaysForAppointment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PacketService {
    private static final int MAX_SMS_CHAR = 141;
    private Long mDateInMillis;
    private String mContactName;
    private threadFindDaysForAppointment mWorkThread;
    // get the appointments for a given date
    private Map<String, Object> mCurrentDayAppointments;
    private Map<String, Object> mUserAppointments;
    /* variable used to know the hour
     * to make the schedule for this client
     */
    private String mTimeToSchedule, mAppointmentType;
    /* get all of users working days which
    have a schedule */
    private Map<String, Object> mUserDaysWithSchedule;
    private String mDayOfTheWeek, mPhoneNumber;
    // used for geting start and end hours
    private String[] mDaySchedule;
    // building the sms body
    private StringBuilder mSMSBody;
    private Integer mNrOfAppointmentsForDate;
    private Resources mResources = ContextForStrings.getContext().getResources();
    private User user = User.getInstance();


    private void sendMessage() {
        ArrayList<String> _messageParts = new ArrayList<>(3);
        if(mSMSBody.length() > MAX_SMS_CHAR) {
            _messageParts.add(mSMSBody.toString().substring(0, MAX_SMS_CHAR));
            _messageParts.add(mSMSBody.toString().substring(MAX_SMS_CHAR + 1));
        } else {
            _messageParts.add(mSMSBody.toString());
        }

        for (String _message : _messageParts) {
            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, _message, null, null);
        }
    }


    private void getDayOfTheWeek(Long dateInMillis) {
        DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
        LocalDate _date = Instant.ofEpochMilli(dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        mDayOfTheWeek = _date.format(_DTF);
    }

    public void setThreadInstance() {
        mWorkThread = new threadFindDaysForAppointment();
    }

    /* used to get hours for a day
     * message type can be for
     * message with DATE only. messageType = 'DATE'
     * message with DATE and TIME. messageType = 'FULL'
     */
    public void getCurrentDate(final String dateFromUser, Long dateInMillis, String phoneNumber, final String messageType) {
        mPhoneNumber = phoneNumber;
        mDateInMillis = dateInMillis;
        Object _values = mUserAppointments.containsKey(mDateInMillis.toString()) ? mUserAppointments.get(mDateInMillis.toString()) : null;
        if (_values != null) {
            getCurrentDateAppointments(_values);
        } else {
            mCurrentDayAppointments = null;
        }
        if (messageType.equals("DATE")) {
            sendScheduleOptionsWrapper(mDateInMillis);
        } else if (messageType.equals("FULL")) {
            checkIfAppointmentCanBeMade(dateFromUser);
        }
    }

    private void getCurrentDateAppointments(Object values) {
        Gson _gson = new Gson();
        String _json = _gson.toJson(values);
        try {
            mCurrentDayAppointments = new ObjectMapper().readValue(_json, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // check if user is working first on that day
    private void sendScheduleOptionsWrapper(Long dateInMillis) {
        getDayOfTheWeek(dateInMillis);
        mDaySchedule = new String[2];
        mDaySchedule[0] = user.getUserWorkingHours().get(mDayOfTheWeek + "Start");
        mDaySchedule[1] = user.getUserWorkingHours().get(mDayOfTheWeek + "End");

        if (mDaySchedule[0].equals("Free")) {
            mSMSBody = new StringBuilder(mResources.getString(R.string.resources_not_working));
            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
        } else {
            try {
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
        Calendar _calendar = Calendar.getInstance();
        mSMSBody = new StringBuilder(mResources.getString(R.string.responses_beginning_options));
        loopToGetEachHour(_hour, _sDFormat, _calendar);
        checkSMSBodyContent();
    }

    private void checkSMSBodyContent() {
        if (mSMSBody.toString().contains("\n")) {
            sendMessage();
        } else {
            mSMSBody.delete(0, mSMSBody.length() - 1);
            mSMSBody.append(mResources.getString(R.string.responses_day_full));
            sendMessage();
        }
    }

    private void loopToGetEachHour(String hour, SimpleDateFormat sDFormat, Calendar calendar) throws ParseException {
        Date _date;
        while (gotTime(hour, mDaySchedule[1])) {
            if (mCurrentDayAppointments == null || !mCurrentDayAppointments.containsKey(hour)) {
                mSMSBody.append("\n").append(hour);
            }
            _date = sDFormat.parse(hour);
            calendar.setTime(_date);
            calendar.add(Calendar.MINUTE, Integer.parseInt(user.getUserAppointmentsDuration()));
            hour = sDFormat.format(calendar.getTime());
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


    /* ************************************************************************************* */

    /* this part is used for the time message */

    /* this is called when we get a message
     * with only a time
     * we start by getting all the days with schedule
     */
    public void getScheduledDays(final String mTime, String mMessagePhoneNumber, final String messageType) {
        mTimeToSchedule = mTime;
        mPhoneNumber = mMessagePhoneNumber;
        mUserDaysWithSchedule = mUserAppointments;
        findDaysForAppointment(messageType);
    }

    /* start the thread with the necessary operations */

    private void findDaysForAppointment(String messageType) {
        /* this counter is used to get
         *  the 3 closest days for the
         * appointment
         */
        mWorkThread.setUserAppointments(mUserAppointments);
        mWorkThread.setmPhoneNumber(mPhoneNumber);
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
        mDaySchedule[0] = user.getUserWorkingHours().get(mDayOfTheWeek + "Start");
        mDaySchedule[1] = user.getUserWorkingHours().get(mDayOfTheWeek + "End");

        if (mDaySchedule[0].equals("Free")) {
            setUpForFreeDay(dateFromUser);
        } else {
            setUpForWorkDay(dateFromUser);
        }

    }

    private void setUpForFreeDay(String dateFromUser) {
        mSMSBody = new StringBuilder(mResources.getString(R.string.responses_not_working_with_thread_start)
                + dateFromUser
                + mResources.getString(R.string.responses_not_working_with_thread_end));
        mWorkThread.setmSMSBody(mSMSBody.toString());
        getScheduledDays(mTimeToSchedule, mPhoneNumber, "FULL");
    }


    private void setUpForWorkDay(String dateFromUser) {
        String _hour = mDaySchedule[0];
        if (!checkIfNotPassedWHours(mTimeToSchedule)) {
            mSMSBody = new StringBuilder();
            if (mCurrentDayAppointments != null && mCurrentDayAppointments.containsKey(mTimeToSchedule)) {
                mWorkThread.setmDateInMillisFromService(mDateInMillis);
                mWorkThread.setmSMSBody(mResources.getString(R.string.responses_fixed_scheduled));
                getScheduledDays(mTimeToSchedule, mPhoneNumber, "FULL");
            } else {
                getClosestHour(_hour, mTimeToSchedule, dateFromUser);
            }
        } else {
            sendSmsForTimePassed(mDayOfTheWeek);
        }
    }

    private void sendSmsForTimePassed(String dayOfWeek) {
        mSMSBody = new StringBuilder(mResources.getString(R.string.responses_hour_out) +
                " " +
                dayOfWeek +
                " " +
                mResources.getString(R.string.responses_from) +
                " " +
                mDaySchedule[0] +
                mResources.getString(R.string.responses_to) +
                mDaySchedule[1]);
        SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
    }


    private boolean checkIfNotPassedWHours(String scheduleTime) {
        String[] _scheduleTimeHm = scheduleTime.split(":");
        String[] _closeTimeHm = mDaySchedule[1].split(":");
        long _closeTimeMil = TimeUnit.HOURS.toMillis(Integer.parseInt(_closeTimeHm[0])) + TimeUnit.MINUTES.toMillis(Integer.parseInt(_closeTimeHm[1]));
        long _scheduleTimeMil = TimeUnit.HOURS.toMillis(Integer.parseInt(_scheduleTimeHm[0])) + TimeUnit.MINUTES.toMillis(Integer.parseInt(_scheduleTimeHm[1]));

        return _scheduleTimeMil > _closeTimeMil;
    }

    private long getTimeInMillis(String[] timeSplit) {
        int _timeH = Integer.parseInt(timeSplit[0]), _timeM = Integer.parseInt(timeSplit[1]);
        return (TimeUnit.HOURS.toMillis(_timeH) + TimeUnit.MINUTES.toMillis(_timeM));
    }

    /* *********************** make this one smoother!!!! *************************************** */
    private void getClosestHour(String currentTime, String scheduleTime, String dateFromUser) {
        int _appointmentInMinutes = Integer.parseInt(user.getUserAppointmentsDuration());
        long _appointmentDuration = TimeUnit.HOURS.toMillis(_appointmentInMinutes / 60) + TimeUnit.MINUTES.toMillis(_appointmentInMinutes % 60);
        String[] _curTimeHm = currentTime.split(":");
        String[] _scheduleTimeHm = scheduleTime.split(":");
        String[] _closeTimeHm = mDaySchedule[1].split(":");
        long _curTimeMil = getTimeInMillis(_curTimeHm);
        long _closeTimeMil = getTimeInMillis(_closeTimeHm);
        long _scheduleTimeMil = getTimeInMillis(_scheduleTimeHm);

        if (_scheduleTimeMil > _closeTimeMil) {
            sendSmsForTimePassed(mDayOfTheWeek);
            return;
        }
        /* check if this exact time is available */
        checkExactTimeAndFindClosestHours(_appointmentDuration, _curTimeMil, _closeTimeMil, _scheduleTimeMil, dateFromUser);
    }

    private void checkExactTimeAndFindClosestHours(long _appointmentDuration, long _curTimeMil, long _closeTimeMil, long _scheduleTimeMil, String dateFromUser) {
        String _fixHour = checkExactTime(_curTimeMil, _closeTimeMil, _scheduleTimeMil, _appointmentDuration);
        if (_fixHour != null) {
            mSMSBody.append(mResources.getString(R.string.resources_scheduled_final))
                    .append(" ")
                    .append(dateFromUser)
                    .append(mResources.getString(R.string.resources_at)).append(_fixHour);
            sendMessage();
            saveAppointmentToDatabase(_fixHour);
        } else {
            /* until we have time and we are not stuck
             * try to find the perfect time
             */
            /* variable to know if time was found */
            boolean _foundTime = findTimeInRemainingDay(_closeTimeMil, _curTimeMil, _scheduleTimeMil, _appointmentDuration);
            if (!_foundTime) {
                sendScheduleOptionsWrapper(mDateInMillis);
            }
        }
    }

    private boolean findTimeInRemainingDay(long _closeTimeMil, long _curTimeMil, long _scheduleTimeMil, long _appointmentDuration) {
        boolean _foundTime = false;
        while ((_closeTimeMil - _curTimeMil) > 0) {
            /* find the difference between current time and
             * schedule time
             */
            long _elapsedTime = Math.abs(_curTimeMil - _scheduleTimeMil);
            /* we got in range of our hour
             * we check if here or after appointment duration
             * is free and return one or both of hours
             */
            if (_elapsedTime <= _appointmentDuration && (_curTimeMil + _appointmentDuration < _closeTimeMil)) {
                String _hourBefore = formatHours(_curTimeMil, 0);
                String _hourAfter = formatHours(_curTimeMil, _appointmentDuration);
                /* we do not have an appointment at this hour
                 * we can put him in a time near the request
                 * before or after the requested time
                 */
                boolean _freeBefore = !mCurrentDayAppointments.containsKey(_hourBefore);
                boolean _freeAfter = !mCurrentDayAppointments.containsKey(_hourAfter);
                if (_freeBefore || _freeAfter) {
                    mSMSBody.append(mResources.getString(R.string.resources_take))
                            .append(_freeBefore ? (" " + _hourBefore) : "")
                            .append(_freeAfter ? (" " + _hourAfter) : "")
                            .append(mResources.getString(R.string.resources_send_conf));
                    sendMessage();
                } else {
                    sendScheduleOptionsWrapper(mDateInMillis);
                }
                _foundTime = true;
                break;
            }
            _curTimeMil += _appointmentDuration;
        }
        return _foundTime;
    }

    @SuppressLint("DefaultLocale")
    private String formatHours(long curTimeMil, long appointmentDuration) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(curTimeMil + appointmentDuration),
                TimeUnit.MILLISECONDS.toMinutes(curTimeMil + appointmentDuration) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(curTimeMil + appointmentDuration))
        );
    }

    private String checkExactTime(long curTimeMil, long closeTimeMil, long scheduleTimeMil, long appointmentDuration) {
        long _curTime = curTimeMil;
        while (_curTime < scheduleTimeMil && _curTime < closeTimeMil) {
            _curTime += appointmentDuration;
        }
        String _hour = formatHours(_curTime, 0);
        if ((_curTime == scheduleTimeMil) && (mCurrentDayAppointments == null || !mCurrentDayAppointments.containsKey(_hour))) {
            return _hour;
        }
        return null;
    }


    /* ************************************************************************************* */
    /* save the appointment in the database */
    private void saveAppointmentToDatabase(String hour) {
        ScheduledHours.getInstance().saveScheduledHour(mContactName.equals("") ? null : mContactName, mPhoneNumber, mAppointmentType, hour, mDateInMillis.toString(), user.getUid())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        increaseNrOfAppointments(mDateInMillis.toString());
                    }
                });

    }

    private void increaseNrOfAppointments(String date) {
        mNrOfAppointmentsForDate++;
        Map<String, String> _date = new HashMap<>();
        _date.put(date, mNrOfAppointmentsForDate.toString());
        Map<String, Object> _infoForThisUser = new HashMap<>();
        _infoForThisUser.put(mPhoneNumber, _date);
        FirebaseFirestore.getInstance().collection("phoneNumbersFromClients")
                .document(user.getUid())
                .set(_infoForThisUser, SetOptions.merge());
    }

    public void setNrOfAppointmentsForNumber(int nrOfAppointmentsForDate) {
        mNrOfAppointmentsForDate = nrOfAppointmentsForDate;
    }

    public void setAppointmentType(String mAppointmentType) {
        this.mAppointmentType = mAppointmentType;
    }

    public void setmUserAppointments(Map<String, Object> mUserAppointments) {
        this.mUserAppointments = mUserAppointments;
    }
}

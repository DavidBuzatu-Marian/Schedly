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
    public static AtomicBoolean mResultForService;
    private Long mDateInMillis;
    private String mUserDaysWithScheduleID;
    private String mUserID;
    private String mUserAppointmentDuration;
    private String mCurrentDaySHoursID;
    private FirebaseFirestore mFireStore;
    private threadFindDaysForAppointment mWorkThread;
    // get the appointments for a given date
    private Map<String, Object> mCurrentDayAppointments;
    /* variable used to know when getting the
     * scheduled hours finished
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


    private void getDateInMillis(String _dateFromUser) {
        Calendar _calendar = Calendar.getInstance();
        SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date _date;
        try {
            _date = _simpleDateFormat.parse(_dateFromUser);
            Log.d("Firebase", _date.toString());
            _calendar.setTimeInMillis(_simpleDateFormat.parse(_dateFromUser).getTime());
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
                            getCurrentDateAppointments(mCurrentDaySHoursID, mDateInMillis);
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
                        sendScheduleOptionsWrapper(mDateInMillis);
                    }
                });
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


    // check if user is working first on that day
    private void sendScheduleOptionsWrapper(Long dateInMillis) {
        mResultForService = new AtomicBoolean(false);
        getDayOfTheWeek(dateInMillis);
        mDaySchedule = new String[2];
        mDaySchedule[0] = mUserWorkingDays.get(mDayOfTheWeek + "Start").toString();
        mDaySchedule[1] = mUserWorkingDays.get(mDayOfTheWeek + "End").toString();

        Log.d("Firebase", "Getting the values for: " + mDayOfTheWeek + ": " + mDaySchedule[0] + "; " + mDaySchedule[1]);

        if(mDaySchedule[0].equals("Free")) {
            Log.d("FirebaseSEND", "Free");
            mSMSBody = new StringBuilder("I am sorry, but I don't work on this day. Please try another date or call me");
            SmsManager.getDefault().sendTextMessage(mPhoneNumber, null, mSMSBody.toString(), null, null);
            mResultForService.set(true);
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
        mResultForService.set(true);
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
    private void getCurrentDateAppointments(String currentDaySHoursID, final Long dateInMillis) {
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
                        sendScheduleOptionsWrapper(dateInMillis);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("FirebaseFailService", e.toString());
                    }
                });
    }


    /* this is called when we get a message
     * with only a date
     * we start by getting all the days with schedule
     */
    public void getAllDaysIDs(final String mTime, String mMessagePhoneNumber) {
        mResultForService = new AtomicBoolean(false);
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
        mWorkThread.setmPhoneNumber(mPhoneNumber);
        mWorkThread.setmTimeToSchedule(mTimeToSchedule);
        mWorkThread.setmUserDaysWithSchedule(mUserDaysWithSchedule);
        mWorkThread.start();
        mResultForService.set(true);
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

    public boolean isThreadWorkFinished() {
        return mResultForService.get();
    }

    public void makeAppointmentForFixedParameters(String dateFromUser, String time, String messagePhoneNumber) {
        mTimeToSchedule = time;
        mPhoneNumber = messagePhoneNumber;
    }
}

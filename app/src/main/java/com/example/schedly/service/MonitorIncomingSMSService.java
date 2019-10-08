package com.example.schedly.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.schedly.MainActivity;
import com.example.schedly.R;
import com.example.schedly.SettingsActivity;
import com.example.schedly.StartSplashActivity;
import com.example.schedly.model.InternetReceiver;
import com.example.schedly.model.MessageListener;
import com.example.schedly.model.SMSBroadcastReceiver;
import com.example.schedly.model.TSMSMessage;
import com.example.schedly.packet_classes.PacketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.schedly.CalendarActivity.SETTINGS_RETURN;

public class MonitorIncomingSMSService extends Service implements MessageListener {

    private ArrayDeque<TSMSMessage> mSMSQueue;
    private HashMap<String, String> mUUID;
    private HashMap<String, Object> mResultFromDialogFlow;
    private String mTime, mDateFromUser, mAppointmentType;
    private Long mDateFromUserInMillis;
    private String mUserID, mUserAppointmentDuration, mMessagePhoneNumber;
    private HashMap<String, String> mContactName;
    private PacketService mPacketService;
    public static final int SERVICE_ID = 4000;
    public static boolean sServiceRunning = false;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private ListenerRegistration mRegistration;
    private int mNROfAppointmentsForThisDay;
    private Map<String, Object> mUserAppointments;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle _extras = null;
        if (intent != null) {
            _extras = intent.getExtras();
        }
        initObjects();
        if (_extras != null) {
            getExtrasValues(_extras);
        }
        monitorChanges();
        sServiceRunning = true;
        return START_STICKY;
    }

    private void getExtrasValues(Bundle extras) {
        mUserID = extras.getString("userID");
        mUserAppointmentDuration = extras.getString("userAppointmentDuration");
        mWorkingHours = (HashMap<String, String>) extras.getSerializable("userWorkingHours");
    }

    private void initObjects() {
        mSMSQueue = new ArrayDeque<>();
        mUUID = new HashMap<>();
        mContactName = new HashMap<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiverAndBroadcast();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startOwnForeground();
        else
            startForeground(SERVICE_ID, new Notification());
    }

    private void registerReceiverAndBroadcast() {
        SMSBroadcastReceiver.bindListener(this);
        BroadcastReceiver _internetBroadcast = new InternetReceiver();
        IntentFilter _intentFilter = new IntentFilter();
        _intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(_internetBroadcast, filter);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.example.schedly";
        String _channelName = "Schedly SMS monitoring";
        NotificationChannel _channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, _channelName, NotificationManager.IMPORTANCE_NONE);
        _channel.setLightColor(R.color.colorPrimaryDark);
        _channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager _manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert _manager != null;
        _manager.createNotificationChannel(_channel);
        Notification _notification = setUpActionsInNotification(NOTIFICATION_CHANNEL_ID);
        startForeground(SERVICE_ID, _notification);
    }

    private Notification setUpActionsInNotification(String NOTIFICATION_CHANNEL_ID) {
        Intent calendarIntent = new Intent(this, StartSplashActivity.class);
        calendarIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent _intentDefault = PendingIntent.getActivity(this, 0, calendarIntent, 0);
        /* intent for stopping monitoring */
        Intent _startSettingsIntent = getIntentForCalendar();
        PendingIntent _intentSettings = PendingIntent.getActivity(this, SETTINGS_RETURN, _startSettingsIntent, 0);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        return notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(this.getString(R.string.notification_monitor_sms))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(_intentDefault)
                .addAction(R.drawable.ic_close, "Disable monitoring", _intentSettings)
                .build();
    }

    private Intent getIntentForCalendar() {
        Intent _intent = new Intent(this, SettingsActivity.class);
        _intent.putExtra("userID", mUserID);
        _intent.putExtra("userAppointmentDuration", mUserAppointmentDuration);
        _intent.putExtra("userWorkingHours", mWorkingHours);
        _intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return _intent;
    }

    private void monitorChanges() {
        final DocumentReference docRef = FirebaseFirestore.getInstance().collection("scheduledHours").document(mUserID);
        mRegistration = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ERR", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    getSnapshotValues(snapshot);
                } else {
                    mUserAppointments = null;
                }
            }
        });
    }

    private void getSnapshotValues(DocumentSnapshot snapshot) {
        mUserAppointments = snapshot.getData();
        if (mPacketService != null) {
            mPacketService.setmUserAppointments(mUserAppointments);
        }
    }

    @Override
    public void messageReceived(TSMSMessage newSMSMessage) {
        mNROfAppointmentsForThisDay = 0;
        initNewPacketService();
        if (!phoneBlocked(mMessagePhoneNumber)) {
            mSMSQueue.add(newSMSMessage);
            setUpBeforeFirebase();
        } else {
            Log.d("MonitorSMS", "Blocked!");
        }

    }

    private void initNewPacketService() {
        mPacketService = new PacketService(mUserID, mUserAppointmentDuration);
        mPacketService.setUserWorkingHours(mWorkingHours);
        mPacketService.setmUserAppointments(mUserAppointments);
    }

    private void setUpBeforeFirebase() {
        try {
            loopThroughSMSQueue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loopThroughSMSQueue() {
        String _sender, _message;
        while (!mSMSQueue.isEmpty()) {
            TSMSMessage _currentMessage = mSMSQueue.pop();
            _sender = _currentMessage.getmSMSSender();
            _message = _currentMessage.getmSMSBody();
            mMessagePhoneNumber = _sender;
            if (!mUUID.containsKey(_sender)) {
                mUUID.put(_sender, UUID.randomUUID().toString());
                if (!mContactName.containsKey(mMessagePhoneNumber)) {
                    getContact(mMessagePhoneNumber);
                }
            }
            callToFirebaseFunction(_message, mUUID.get(_sender));
        }
    }

    private void getContact(String mMessagePhoneNumber) {
        Uri lookupUriContacts = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mMessagePhoneNumber));
        Cursor cursor = this.getContentResolver().query(lookupUriContacts, new String[]{ContactsContract.Data.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mContactName.put(mMessagePhoneNumber, cursor.getString(0));
            } else {
                mContactName.put(mMessagePhoneNumber, "");
            }
            cursor.close();
        }
    }

    @Override
    public void onDestroy() {
        sServiceRunning = false;
        mRegistration.remove();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void callToFirebaseFunction(final String message, String sessionID) {
        FirebaseFunctions _functionsInstance = FirebaseFunctions.getInstance();
        addMessage(message, _functionsInstance, sessionID).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    getParametersAndRedirect(task, message);
                } else {
                    Log.d("MonitorSMS", task.getException().toString());
                }
            }
        });
    }

    private void getParametersAndRedirect(Task<String> task, String message) {
        Log.d("Succes", mResultFromDialogFlow.get("parameters").toString());
        Gson _gson = new Gson();
        Properties data = _gson.fromJson(mResultFromDialogFlow.get("parameters").toString(), Properties.class);
        mTime = getLocaleTimeString(data.getProperty("time"));
        mDateFromUser = getLocaleDateString(data.getProperty("date"));
        mAppointmentType = data.getProperty("Appointment-type");
        String _keyWord = data.getProperty("Key-word");
        if (mDateFromUser != null && !checkPhoneNumberNrAppointments(mMessagePhoneNumber, mDateFromUser)) {
            responseOptions(_keyWord, message);
        } else if (isMessageForAppointment(mDateFromUser, mTime, mAppointmentType, _keyWord)) {
            responseOptions(_keyWord, message);
        } else {
            Log.d("MonitorSMS", "Ignored message");
        }
    }

    private boolean isMessageForAppointment(String dateFromUser, String time, String appointmentType, String keyWord) {
        if (dateFromUser == null && time == null && (appointmentType == null || keyWord == null)) {
            return false;
        } else return appointmentType != null || keyWord != null;
    }

    private void responseOptions(String keyWord, String message) {
        if (keyWord == null && mAppointmentType == null && (mTime == null || mDateFromUser == null)) {
            Log.d("Monitor", "Ignored message from user: " + message);
        } else {
            if (dateFromUserIsNotPast(mDateFromUser)) {
//                markMessageRead(MonitorIncomingSMSService.this, mMessagePhoneNumber, message);
                selectOptions();
            }
        }
    }

    private void selectOptions() {
        if (mTime == null && mDateFromUser != null) {
            sendMessageForTime();
        } else if (mDateFromUser == null && mTime != null) {
            sendMessageForDate();
        } else if (mDateFromUser == null && mTime == null) {
            sendMessageForAppointment(mResultFromDialogFlow.get("response").toString());
        } else {
            sendMessageForFixedParameters();
        }
        mDateFromUser = null;
        mTime = null;
    }

    private void sendMessageForFixedParameters() {
        if (mAppointmentType != null) {
            mPacketService.setAppointmentType(mAppointmentType);
        }
        mPacketService.makeAppointmentForFixedParameters(mDateFromUser, mDateFromUserInMillis, mTime, mMessagePhoneNumber, mContactName.get(mMessagePhoneNumber));
        mUUID.remove(mMessagePhoneNumber);
    }

//    private void markMessageRead(Context context, String number, String body) {
//        /* mark the message as read */
//        Uri _uri = Uri.parse("content://sms/inbox");
//        Cursor _cursor = context.getContentResolver().query(_uri, null, null, null, null);
//        try {
//            while (_cursor.moveToNext()) {
//                /* find in cursor the message with the same phone number
//                 * get the one which is unread
//                 * get its id and mark it as read
//                 */
//                Log.d("Cursor", _cursor.getString(_cursor.getColumnIndex("adress")));
//                if ((_cursor.getString(_cursor.getColumnIndex("address")).equals(number)) && (_cursor.getInt(_cursor.getColumnIndex("read")) == 0)) {
//                    if (_cursor.getString(_cursor.getColumnIndex("body")).startsWith(body)) {
//                        String _SMSMessageId = _cursor.getString(_cursor.getColumnIndex("_id"));
//                        ContentValues _values = new ContentValues();
//                        _values.put("read", true);
//                        context.getContentResolver().update(Uri.parse("content://sms/inbox"), _values, "_id=" + _SMSMessageId, null);
//                        return;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.e("Mark Read", "Error in Read: " + e.toString());
//        }
//        _cursor.close();
//    }

    private void sendMessageForDatePast() {
        String _message = "Sorry, but your date: " + mDateFromUser + " has passed already. Try a valid one.";
        SmsManager.getDefault().sendTextMessage(mMessagePhoneNumber, null, _message, null, null);
    }

    /* function to check if date from user
     * is not in the past
     */
    private boolean dateFromUserIsNotPast(String dateFromUser) {
        if (dateFromUser != null) {
            LocalDate _localDate = LocalDate.now(ZoneId.systemDefault());
            long _curDayInMillis = _localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            DateTimeFormatter _DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            _localDate = LocalDate.parse(dateFromUser, _DTF);
            long _userDayInMillis = _localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return _userDayInMillis >= _curDayInMillis;
        }
        return true;
    }

    private void sendMessageForTime() {
        if (mAppointmentType != null) {
            mPacketService.setAppointmentType(mAppointmentType);
        }
        mPacketService.getCurrentDate(mDateFromUser, mDateFromUserInMillis, mMessagePhoneNumber, "DATE");
    }

    private void sendMessageForAppointment(String response) {
        SmsManager.getDefault().sendTextMessage(mMessagePhoneNumber, null, response, null, null);
    }

    private void sendMessageForDate() {
        if (mAppointmentType != null) {
            mPacketService.setAppointmentType(mAppointmentType);
        }
        mPacketService.getScheduledDays(mTime, mMessagePhoneNumber, "TIME");
    }


    private Task<String> addMessage(String text, FirebaseFunctions mFunctions, String sessionID) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("sessionID", sessionID);
        return mFunctions
                .getHttpsCallable("detectTextIntent")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        mResultFromDialogFlow = (HashMap<String, Object>) task.getResult().getData();
                        Log.d("Succes", mResultFromDialogFlow.get("response").toString() + ";" + mResultFromDialogFlow.get("parameters").toString());
                        return mResultFromDialogFlow.get("parameters") != null ? mResultFromDialogFlow.get("parameters").toString() : null;
                    }
                });

    }
    private String getLocaleTimeString(String time) {
        if (time != null && !time.equals("")) {
            String[] splitTTime = time.split("T");
            String[] splitPlusTime = splitTTime[1].split("\\+");
            return splitPlusTime[0];
        } else {
            return null;
        }
    }
    private String getLocaleDateString(String date) {
        if (date != null && !date.equals("")) {
            String[] splitTDate = date.split("T");
            return splitTDate[0];
        } else {
            return null;
        }
    }
    private boolean checkPhoneNumberNrAppointments(String phoneNumber, String dateFromUser) {
        /* get date in millis */
        LocalDate _localDate = LocalDate.parse(dateFromUser);
        mDateFromUserInMillis = _localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        final AtomicBoolean _phoneNumberBlocked = new AtomicBoolean(false);
        FirebaseFirestore.getInstance()
                .collection("phoneNumbersFromClients")
                .document(phoneNumber)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        getNrOfAppointments(task);
                        _phoneNumberBlocked.set(mNROfAppointmentsForThisDay > 3);
                        mPacketService.setNrOfAppointmentsForNumber(mNROfAppointmentsForThisDay);
                    }
                });
        return _phoneNumberBlocked.get();
    }

    private void getNrOfAppointments(Task<DocumentSnapshot> task) {
        mNROfAppointmentsForThisDay = 0;
        if (task.getResult() != null && task.getResult().exists()) {
            Map<String, Object> _maps = task.getResult().getData();
            Gson _gson = new Gson();
            try {
                Object _values = _maps.values();
                String _json = _gson.toJson(_values);
                Map<String, Object> _data = new ObjectMapper().readValue(_json.substring(1, _json.length() - 1), Map.class);
                if (_data.containsKey(mDateFromUserInMillis.toString())) {
                    mNROfAppointmentsForThisDay = Integer.parseInt(_data.get(mDateFromUserInMillis.toString()).toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean phoneBlocked(final String mMessagePhoneNumber) {
        final AtomicBoolean _phoneNumberBlocked = new AtomicBoolean(false);
        FirebaseFirestore.getInstance()
                .collection("blockLists")
                .document(mUserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        _phoneNumberBlocked.set(isPhoneNumberBlocked(task));
                    }
                });
        return _phoneNumberBlocked.get();
    }

    private boolean isPhoneNumberBlocked(Task<DocumentSnapshot> task) {
        if (task.getResult() != null && task.getResult().exists()) {
            Map<String, Object> _maps = task.getResult().getData();
            assert _maps != null;
            return _maps.containsKey(mMessagePhoneNumber);
        }
        return false;
    }


    public void setParams(String userID, String userAppointmentDuration, HashMap<String, String> workingHours, Map<String, Object> userAppointments) {
        mUserID = userID;
        mUserAppointmentDuration = userAppointmentDuration;
        mWorkingHours = workingHours;
        mUserAppointments = userAppointments;

        mSMSQueue = new ArrayDeque<>();
        mUUID = new HashMap<>();
        mContactName = new HashMap<>();
    }
}

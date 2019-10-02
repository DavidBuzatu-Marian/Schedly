package com.example.schedly.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
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
    private String mUserID, mUserAppointmentDuration, mMessagePhoneNumber, mUserWorkingDaysID;
    private HashMap<String, String> mContactName;
    private PacketService mPacketService;
    public static final int SERVICE_ID = 4000;
    public static boolean sServiceRunning = false;
    private HashMap<String, String> mWorkingHours = new HashMap<>();
    private ListenerRegistration mRegistration;
    private Map<String, Object> mUserAppointments;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle _extras = null;
        if (intent != null && intent.getAction().equals("ACTION.STOPFOREGROUND_ACTION")) {
            Log.d("Service", "STOPPED SERVICE");
            stopForeground(true);
            stopSelf();
        }
        if(intent != null) {
            _extras = intent.getExtras();
        }
        mSMSQueue = new ArrayDeque<>();
        mUUID = new HashMap<>();
        mContactName = new HashMap<>();
        Log.d("Service", "Started");
        Log.d("Service", "Onstart");
        if (_extras != null) {
            mUserID = _extras.getString("userID");
            mUserAppointmentDuration = _extras.getString("userAppointmentDuration");
            mUserWorkingDaysID = _extras.getString("userWorkingDaysID");
            mWorkingHours = (HashMap<String, String>) _extras.getSerializable("userWorkingHours");
        }
        monitorChanges();
        sServiceRunning = true;
        Log.d("Service", mUserAppointmentDuration + "; " + mUserID);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "Created");

        IntentFilter _intentFilter = new IntentFilter();
        _intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
//        mSMSBroadcastReceiver = new SMSBroadcastReceiver();
//        registerReceiver(mSMSBroadcastReceiver, _intentFilter);
        SMSBroadcastReceiver.bindListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startOwnForeground();
        else
            startForeground(SERVICE_ID, new Notification());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.example.schedly";
        String channelName = "Schedly SMS monitoring";
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(R.color.colorPrimaryDark);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);
        Intent calendarIntent = new Intent(this, StartSplashActivity.class);
        calendarIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent _intentDefault = PendingIntent.getActivity(this, 0,
                calendarIntent, 0);

        /* intent for stopping monitoring */
        Intent _startSettingsIntent = new Intent(this, SettingsActivity.class);
        _startSettingsIntent.putExtra("userID", mUserID);
        _startSettingsIntent.putExtra("userAppointmentDuration", mUserAppointmentDuration);
        _startSettingsIntent.putExtra("userWorkingDaysID", mUserWorkingDaysID);
        _startSettingsIntent.putExtra("userWorkingHours", mWorkingHours);
        _startSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent _intentSettings = PendingIntent.getActivity(this, SETTINGS_RETURN, _startSettingsIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Schedly is making appointments for you")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(_intentDefault)
                .addAction(R.drawable.ic_close, "Disable monitoring", _intentSettings)
                .build();
        startForeground(SERVICE_ID, notification);
    }

    /* monitor changes for appointments */

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
                    mUserAppointments = snapshot.getData();
                    if(mPacketService != null) {
                        mPacketService.setmUserAppointments(mUserAppointments);
                    }
                    Log.d("TESTDB", "Current data: " + snapshot.getData());
                } else {
                    mUserAppointments = null;
                }
            }
        });
    }

    @Override
    public void messageReceived(TSMSMessage newSMSMessage) {
        String _sender, _message;
        mPacketService = new PacketService(mUserID, mUserAppointmentDuration, mUserWorkingDaysID);
        mPacketService.setUserWorkingHours(mWorkingHours);
        mPacketService.setmUserAppointments(mUserAppointments);
        mSMSQueue.add(newSMSMessage);
        try {
            while (!mSMSQueue.isEmpty()) {
                /* get the next phone number */
                TSMSMessage _currentMessage = mSMSQueue.pop();
                _sender = _currentMessage.getmSMSSender();
                _message = _currentMessage.getmSMSBody();
                /* if we already have this phone number
                 * in the hashmap, we just add to
                 * the smsBody
                 */
                mMessagePhoneNumber = _sender;
                /* phone number sent a message  already
                 * use the same session ID
                 */
                if (!mUUID.containsKey(_sender)) {
                    mUUID.put(_sender, UUID.randomUUID().toString());
                    if (!mContactName.containsKey(mMessagePhoneNumber)) {
                        getContact(mMessagePhoneNumber);
                    }
                }
                Log.d("MESSAGEReceiver", _sender);
                Log.d("Succes", "Contact name: " + mContactName.get(mMessagePhoneNumber));
                callToFirebaseFunction(_message, mUUID.get(_sender));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Map<String, String> messageToAdd = new HashMap<>();
//        messageToAdd.put("message", message);
//        messageToAdd.put("sender", sender);
//        mFireStore = FirebaseFirestore.getInstance();
//        mFireStore.collection("TestMessages")
//                .add(messageToAdd)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.d("SUCCESSTEST", "YEs");
//                    }
//                });

    }

    private void getContact(String mMessagePhoneNumber) {
        Uri lookupUriContacts = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mMessagePhoneNumber));
        Cursor cursor = this.getContentResolver().query(lookupUriContacts, new String[]{ContactsContract.Data.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {

            if (cursor.moveToFirst()) {
                mContactName.put(mMessagePhoneNumber, cursor.getString(0));
                Log.d("Firebase", mContactName.get(mMessagePhoneNumber));
            } else {
                mContactName.put(mMessagePhoneNumber, "");
            }

            cursor.close();
        }
    }

    @Override
    public void onDestroy() {
        sServiceRunning = false;
        super.onDestroy();
//        unregisterReceiver(mSMSBroadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void callToFirebaseFunction(final String message, String sessionID) {
        FirebaseFunctions mFunctions;
        mFunctions = FirebaseFunctions.getInstance();
        addMessage(message, mFunctions, sessionID).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    Log.d("Succes", mResultFromDialogFlow.get("parameters").toString());

                    Gson _gson = new Gson();
                    Properties data = _gson.fromJson(mResultFromDialogFlow.get("parameters").toString(), Properties.class);
                    mTime = getLocaleTimeString(data.getProperty("time"));
                    mDateFromUser = getLocaleDateString(data.getProperty("date"));
                    mAppointmentType = data.getProperty("Appointment-type");
                    String _keyWord = data.getProperty("Key-word");
//                    if(!dateFromUserIsNotPast(mDateFromUser)) {
//                        sendMessageForDatePast();
//                    } else {
                    if (_keyWord == null && mAppointmentType == null && (mTime == null || mDateFromUser == null)) {
                        /* ignore message */
                        Log.d("Monitor", "Ignored message from user: " + message);
                    } else {
                        if (!phoneBlocked(mMessagePhoneNumber) && !checkPhoneNumberNrAppointments(mMessagePhoneNumber, mDateFromUser) && dateFromUserIsNotPast(mDateFromUser)) {
                            markMessageRead(MonitorIncomingSMSService.this, mMessagePhoneNumber, message);
                            if (mTime == null && mDateFromUser != null) {
                                Log.d("Service", "Sent Time");
                                sendMessageForTime();
                            } else if (mDateFromUser == null && mTime != null) {
                                sendMessageForDate();
                                Log.d("Service", "Sent Date");
                            } else if (mDateFromUser == null && mTime == null) {
                                sendMessageForAppointment(mResultFromDialogFlow.get("response").toString());
                            } else {
                                Log.d("Succes", mDateFromUser + ": " + mTime);
                                Log.d("Service", "Sent Time and Date");
                                if (mAppointmentType != null) {
                                    mPacketService.setAppointmentType(mAppointmentType);
                                }
                                mPacketService.makeAppointmentForFixedParameters(mDateFromUser, mDateFromUserInMillis, mTime, mMessagePhoneNumber, mContactName.get(mMessagePhoneNumber));
                                mUUID.remove(mMessagePhoneNumber);
                            }
                        }
                    }
                } else {
                    Log.d("Succes", task.getException().toString());
                    this.notifyAll();
                }
            }
        });
    }

    private void markMessageRead(Context context, String number, String body) {
        /* mark the message as read */
        Uri _uri = Uri.parse("content://sms/inbox");
        Cursor _cursor = context.getContentResolver().query(_uri, null, null, null, null);
        try {
            while (_cursor.moveToNext()) {
                /* find in cursor the message with the same phone number
                 * get the one which is unread
                 * get its id and mark it as read
                 */
                Log.d("Cursor", _cursor.getString(_cursor.getColumnIndex("adress")));
                if ((_cursor.getString(_cursor.getColumnIndex("address")).equals(number)) && (_cursor.getInt(_cursor.getColumnIndex("read")) == 0)) {
                    if (_cursor.getString(_cursor.getColumnIndex("body")).startsWith(body)) {
                        String _SMSMessageId = _cursor.getString(_cursor.getColumnIndex("_id"));
                        ContentValues _values = new ContentValues();
                        _values.put("read", true);
                        context.getContentResolver().update(Uri.parse("content://sms/inbox"), _values, "_id=" + _SMSMessageId, null);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Mark Read", "Error in Read: " + e.toString());
        }
        _cursor.close();
    }

    private void sendMessageForDatePast() {
        String _message = "Sorry, but your date: " + mDateFromUser + " has passed already. Try a valid one.";
        SmsManager.getDefault().sendTextMessage(mMessagePhoneNumber, null, _message, null, null);
    }

    /* function to check if date from user
     * is not in the past
     */
    private boolean dateFromUserIsNotPast(String dateFromUser) {
        /* we got a date */
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
        // this function finishes by sending the message to the phoneNumber
        if (mAppointmentType != null) {
            mPacketService.setAppointmentType(mAppointmentType);
        }
        mPacketService.getCurrentDate(mDateFromUser, mDateFromUserInMillis, mMessagePhoneNumber, "DATE");
//       threadPaused();
    }

    private void sendMessageForAppointment(String response) {
        SmsManager.getDefault().sendTextMessage(mMessagePhoneNumber, null, response, null, null);
        this.notifyAll();
    }

    private void sendMessageForDate() {
        if (mAppointmentType != null) {
            mPacketService.setAppointmentType(mAppointmentType);
        }
        mPacketService.getScheduledDays(mTime, mMessagePhoneNumber, "TIME");
//        threadPaused();
    }

//    private void threadPaused() {
//        while(!mPacketService.isThreadWorkFinished()) {
//            try {
//                this.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        this.notifyAll();
//    }

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
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        mResultFromDialogFlow = (HashMap<String, Object>) task.getResult().getData();
                        Log.d("Succes", mResultFromDialogFlow.get("response").toString() + ";" + mResultFromDialogFlow.get("parameters").toString());
                        return mResultFromDialogFlow.get("parameters") != null ? mResultFromDialogFlow.get("parameters").toString() : null;
                    }
                });

    }

    // a helper function to get the time from the dialogflow format
    private String getLocaleTimeString(String time) {
        if (time != null && !time.equals("")) {
            String[] splitTTime = time.split("T");
            String[] splitPlusTime = splitTTime[1].split("\\+");
            return splitPlusTime[0];
        } else {
            return null;
        }
    }

    // a helper function to get the date from the dialogflow format
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
                        int _nrOfAppointmentsForThisDay = 0;
                        if (task.getResult() != null && task.getResult().exists()) {
                            /* we get a map which has objects with the values needed.
                             */
                            Map<String, Object> _maps = task.getResult().getData();
                            Gson _gson = new Gson();
                            try {
                                Object _values = _maps.values();
                                String _json = _gson.toJson(_values);
                                /* remove [] from the collection type of values
                                 * in order to get a working json for mapping
                                 */
                                Map<String, Object> _data = new ObjectMapper().readValue(_json.substring(1, _json.length() - 1), Map.class);
                                Log.d("Logged", _data.keySet().toString());
                                if (_data.containsKey(mDateFromUserInMillis.toString())) {
                                    _nrOfAppointmentsForThisDay = Integer.parseInt(_data.get(mDateFromUserInMillis.toString()).toString());
                                    Log.d("Logged", _nrOfAppointmentsForThisDay + "; ");
                                    _phoneNumberBlocked.set(_nrOfAppointmentsForThisDay > 3);
                                }
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }
                        mPacketService.setNrOfAppointmentsForNumber(_nrOfAppointmentsForThisDay);
                    }
                });
        return _phoneNumberBlocked.get();
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
                        if (task.getResult() != null && task.getResult().exists()) {
                            /* we get a map which has objects with the values needed.
                             */
                            Map<String, Object> _maps = task.getResult().getData();
                            assert _maps != null;
                            if (_maps.containsKey(mMessagePhoneNumber)) {
                                _phoneNumberBlocked.set(true);
                            }
                        }
                    }
                });
        return _phoneNumberBlocked.get();
    }
}

package com.example.schedly.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import com.example.schedly.model.MessageListener;
import com.example.schedly.model.SMSBroadcastReceiver;
import com.example.schedly.model.TSMSMessage;
import com.example.schedly.packet_classes.PacketService;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;

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

public class MonitorIncomingSMSService extends Service implements MessageListener {

    private ArrayDeque<TSMSMessage> mSMSQueue;
    private HashMap<String, String> mUUID;
    private HashMap<String, Object> mResultFromDialogFlow;
    private String mTime, mDateFromUser;
    private Long mDateInMillis;
    private String mUserDaysWithScheduleID;
    private String mUserID;
    private String mUserAppointmentDuration;
    private String mMessagePhoneNumber;
    private String mUserWorkingDaysID;
    private HashMap<String, String> mContactName;
    private PacketService mPacketService;
    public static final int SERVICE_ID = 4000;

    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Objects.requireNonNull(intent.getAction()).equals("ACTION.STOPFOREGROUND_ACTION")) {
            Log.d("TEST", "STOPPED SERVICE");
            stopForeground(true);
            stopSelf();
        }
        mSMSQueue = new ArrayDeque<>();
        mUUID = new HashMap<>();
        mContactName = new HashMap<>();
        Log.d("Service", "Started");
        Log.d("Service", "Onstart");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mUserID = extras.getString("userID");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
            mUserDaysWithScheduleID = extras.getString("userDaysWithScheduleID");
            mUserWorkingDaysID = extras.getString("userWorkingDaysID");
        }
        mPacketService = new PacketService(mUserID, mUserAppointmentDuration, mUserDaysWithScheduleID, mUserWorkingDaysID);
        Log.d("Service", mUserAppointmentDuration + mUserID + mUserDaysWithScheduleID);
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
    private void startOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.schedly";
        String channelName = "Schedly SMS monitoring";
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(R.color.colorPrimaryDark);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);
        Intent calendarIntent = new Intent(this, MainActivity.class);

        PendingIntent intent = PendingIntent.getActivity(this, 0,
                calendarIntent, 0);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_baseline_menu_24px)
                .setContentTitle("Schedly is making appointments for you")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(intent)
                .build();
        startForeground(SERVICE_ID, notification);
    }


    @Override
    public void messageReceived(TSMSMessage newSMSMessage) {
        String _sender, _message;
        mSMSQueue.add(newSMSMessage);
        try {
            while(!mSMSQueue.isEmpty()) {
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
                if(!mUUID.containsKey(_sender)) {
                    mUUID.put(_sender, UUID.randomUUID().toString());
                    getContact(mMessagePhoneNumber);
                }
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
        Cursor cursor = this.getContentResolver().query(lookupUriContacts, new String[]{ContactsContract.Data.DISPLAY_NAME},null,null,null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                mContactName.put(mMessagePhoneNumber, cursor.getString(0));
                Log.d("Firebase", mContactName.get(mMessagePhoneNumber));
            }
            else {
                mContactName.put(mMessagePhoneNumber, "");
            }

            cursor.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(mSMSBroadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void callToFirebaseFunction(String message, String sessionID) {
        FirebaseFunctions mFunctions;
        mFunctions = FirebaseFunctions.getInstance();
        addMessage(message, mFunctions, sessionID).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if(task.isSuccessful()) {
                    Log.d("Succes", mResultFromDialogFlow.get("parameters").toString());

                    Gson _gson = new Gson();
                    Properties data = _gson.fromJson(mResultFromDialogFlow.get("parameters").toString(), Properties.class);
                    mTime = getLocaleTimeString(data.getProperty("time"));
                    mDateFromUser = getLocaleDateString(data.getProperty("date"));
                    Log.d("FirebaseS", mDateFromUser);
                    if(mTime == null && mDateFromUser != null) {
                        sendMessageForTime();
                    }
                    else if(mDateFromUser == null && mTime != null) {
                        sendMessageForDate();
                    }
                    else if(mDateFromUser == null && mTime == null) {
                        sendMessageForAppointment(mResultFromDialogFlow.get("response").toString());
                    }
                    else {
                        Log.d("Succes", mDateFromUser + ": " + mTime);
                        mPacketService.makeAppointmentForFixedParameters(mDateFromUser, mTime, mMessagePhoneNumber);
                    }
                }
                else {
                    Log.d("Succes", task.getException().toString());
                    this.notifyAll();
                }
            }
        });
    }

    private void sendMessageForTime() {
        // this function finishes by sending the message to the phoneNumber
        mPacketService.getCurrentDateSHoursID(mDateFromUser, mMessagePhoneNumber);
       threadPaused();
    }

    private void sendMessageForAppointment(String response) {
        SmsManager.getDefault().sendTextMessage(mMessagePhoneNumber, null, response, null,null);
        this.notifyAll();
    }

    private void sendMessageForDate() {
        mPacketService.getAllDaysIDs(mTime, mMessagePhoneNumber);
        threadPaused();
    }

    private void threadPaused() {
        while(!mPacketService.isThreadWorkFinished()) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.notifyAll();
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
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        mResultFromDialogFlow = (HashMap<String, Object>) task.getResult().getData();
                        Log.d("Succes", mResultFromDialogFlow.get("response").toString() + ";" + mResultFromDialogFlow.get("parameters").toString());
                        return mResultFromDialogFlow.get("parameters") != null ? mResultFromDialogFlow.get("parameters").toString() : null;
                    }
                });

    }

    // A helper function that converts the Date instance 'dateObj' into a string that represents this time in English.
    private String getLocaleTimeString(String time){
        if(time != null && !time.equals("")) {
            String[] splitTTime = time.split("T");
            String[] splitPlusTime = splitTTime[1].split("\\+");
            return splitPlusTime[0];
        }
        else {
            return null;
        }
    }

    // A helper function that converts the Date instance 'dateObj' into a string that represents this date in English.
    private String getLocaleDateString(String date){
        if(date != null && !date.equals("")) {
            String[] splitTDate = date.split("T");
            return splitTDate[0];
        }
        else {
            return null;
        }
    }
}

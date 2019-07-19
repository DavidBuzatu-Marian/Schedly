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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.schedly.MainActivity;
import com.example.schedly.R;
import com.example.schedly.model.MessageListener;
import com.example.schedly.model.SMSBroadcastReceiver;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class MonitorIncomingSMSService extends Service implements MessageListener {

    private SMSBroadcastReceiver mSMSBroadcastReceiver;
    private HashMap<String, Object> mResultFromDialogFlow;
    private String mTime, mDateFromUser;
    private Long mDateInMillis;
    private String mUserDaysWithScheduleID;
    private String mUserID;
    private String mUserAppointmentDuration;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "Started");
        Log.d("Service", "Onstart");
        SMSBroadcastReceiver.bindListener(this);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mUserID = extras.getString("userID");
            mUserAppointmentDuration = extras.getString("userAppointmentDuration");
            mUserDaysWithScheduleID = extras.getString("userDaysWithScheduleID");
        }
        Log.d("Service", mUserAppointmentDuration + mUserID + mUserDaysWithScheduleID);
//        Intent activityIntent = new Intent(this, Calendar.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
//                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // This always shows up in the notifications area when this Service is running.
//        Notification notification = new Notification.Builder(this).
//                setContentTitle(getText(R.string.app_name)).
//                setContentInfo("Doing stuff in the background...").setSmallIcon(R.mipmap.ic_launcher).
//                setContentIntent(pendingIntent).build();
//        startForeground(1, notification);
//
//        // Other code goes here...
//
//        return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "Created");

        IntentFilter _intentFilter = new IntentFilter();
        _intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        mSMSBroadcastReceiver = new SMSBroadcastReceiver();
        registerReceiver(mSMSBroadcastReceiver, _intentFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startOwnForeground();
        else
            startForeground(1, new Notification());
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
        startForeground(2, notification);
    }


    @Override
    public void messageReceived(String message, String sender) {
        try {
            callToFirebaseFunction(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, String> messageToAdd = new HashMap<>();
        messageToAdd.put("message", message);
        messageToAdd.put("sender", sender);
        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
        _FireStore.collection("TestMessages")
                .add(messageToAdd)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("SUCCESSTEST", "YEs");
                    }
                });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSMSBroadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void callToFirebaseFunction(String message) {
        FirebaseFunctions mFunctions;
        mFunctions = FirebaseFunctions.getInstance();
        addMessage(message, mFunctions).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if(task.isSuccessful()) {
                    Log.d("Succes", mResultFromDialogFlow.get("parameters").toString());

                    Gson _gson = new Gson();
                    Properties data = _gson.fromJson(mResultFromDialogFlow.get("parameters").toString(), Properties.class);
                    mTime = getLocaleTimeString(data.getProperty("time"));
                    mDateFromUser = getLocaleDateString(data.getProperty("date"));
                    Log.d("Succes", mDateFromUser + ": " + mTime);

                    Calendar _calendar = Calendar.getInstance();
                    SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("YYYY-MM-DD", Locale.getDefault());
                    try {
                        _calendar.setTime(_simpleDateFormat.parse(mDateFromUser));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    mDateInMillis = _calendar.getTimeInMillis();
                    Log.d("Succes date in millis: ",  mDateInMillis.toString());
//                    getCurrentDayID(mDateInMillis);
                }
                else {
                    Log.d("Succes", task.getException().toString());
                }
            }
        });
    }

//    private void getCurrentDayID(Long mDateInMillis) {
//        FirebaseFirestore _FireStore = FirebaseFirestore.getInstance();
//        DocumentReference _documentReference = _FireStore.collection("daysWithSchedule")
//                .document(userDaysWithScheduleID);
//        _documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()) {
//                    DocumentSnapshot _document = task.getResult();
//                    currentDayID = _document.get(mDate.toString()) != null ? _document.get(mDate.toString()).toString() : null;
//                    if (currentDayID != null) {
//                        getEachAppointment();
//                    } else {
//                        mAdapter.notifyDataSetChanged();
//                    }
//                }
//            }
//        });
//    }

    private Task<String> addMessage(String text, FirebaseFunctions mFunctions) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);

        return mFunctions
                .getHttpsCallable("detectTextIntent")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        Log.d("Succes", "sUCCES");
                        mResultFromDialogFlow = (HashMap<String, Object>) task.getResult().getData();
                        Log.d("Succes", mResultFromDialogFlow.get("response").toString() + ";" + mResultFromDialogFlow.get("parameters").toString());
                        return mResultFromDialogFlow.get("parameters").toString();
                    }
                });

    }

    private void saveAppointmentToDatabase() {
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



    // A helper function that converts the Date instance 'dateObj' into a string that represents this time in English.
    private String getLocaleTimeString(String time){
        String[] splitTTime = time.split("T");
        String[] splitPlusTime = splitTTime[1].split("\\+");
        return splitPlusTime[0];
    }

    // A helper function that converts the Date instance 'dateObj' into a string that represents this date in English.
    private String getLocaleDateString(String date){
        String[] splitTDate = date.split("T");
        return splitTDate[0];
    }
}

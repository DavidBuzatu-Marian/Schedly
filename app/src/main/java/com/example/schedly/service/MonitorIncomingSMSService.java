package com.example.schedly.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.schedly.CalendarActivity;
import com.example.schedly.model.MessageListener;
import com.example.schedly.model.SMSBroadcastReceiver;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MonitorIncomingSMSService extends IntentService implements MessageListener {

    private SMSBroadcastReceiver mSMSBroadcastReceiver;
    public MonitorIncomingSMSService() {
        super("Constructor");
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MonitorIncomingSMSService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "Created");

        IntentFilter _intentFilter = new IntentFilter();
        _intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        mSMSBroadcastReceiver = new SMSBroadcastReceiver();
        registerReceiver(mSMSBroadcastReceiver, _intentFilter);
    }


    @Override
    public void messageReceived(String message, String sender) {
        Log.d("Service", "HAHA");
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
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("Service", "Onstart");
        SMSBroadcastReceiver.bindListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSMSBroadcastReceiver);
    }
}

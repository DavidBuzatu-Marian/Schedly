package com.example.schedly.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.schedly.CalendarActivity;
import com.example.schedly.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String ACTION_INTERNET = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String ACTION_SMS = "Telephony.Sms.Intents.SMS_RECEIVED_ACTION";
    private static final String TAG = "SMSBroadcastReceiver";
    private final String serviceProviderNumber;
    private final String serviceProviderSmsCondition;


    private static MessageListener mMessageListener;

    public SMSBroadcastReceiver() {
        this.serviceProviderSmsCondition = null;
        this.serviceProviderNumber = null;
    }

    public SMSBroadcastReceiver(String serviceProviderNumber, String serviceProviderSMSCondition) {
        this.serviceProviderNumber = serviceProviderNumber;
        this.serviceProviderSmsCondition = serviceProviderSMSCondition;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_SMS)) {
            TSMSMessage _newSMSMessage;
            String _smsSender = "";
            StringBuilder _smsBody = new StringBuilder();
            long _timeReceived = 0L;
            for (SmsMessage _smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                _smsSender = _smsMessage.getDisplayOriginatingAddress();
                _smsBody.append(_smsMessage.getMessageBody());
                _timeReceived = _smsMessage.getTimestampMillis();
            }
            _newSMSMessage = new TSMSMessage(_smsBody, _smsSender, _timeReceived);
            if(mMessageListener != null) {
                mMessageListener.messageReceived(_newSMSMessage);
            }
        }
    }


    public static void bindListener(MessageListener _messageListener){
        mMessageListener = _messageListener;
    }


}

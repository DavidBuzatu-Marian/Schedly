package com.example.schedly.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.example.schedly.CalendarActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SMSBroadcastReceiver extends BroadcastReceiver {

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
        if(intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            String _smsSender = "";
            String _smsBody = "";
            for (SmsMessage _smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                _smsSender = _smsMessage.getDisplayOriginatingAddress();
                _smsBody += _smsMessage.getMessageBody();
            }
            if(mMessageListener != null) {
                mMessageListener.messageReceived(_smsBody, _smsSender);
            }
        }
    }

    public static void bindListener(MessageListener _messageListener){
        mMessageListener = _messageListener;
    }


}

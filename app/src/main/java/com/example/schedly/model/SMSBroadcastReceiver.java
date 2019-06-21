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

            if(true) {
                if(mMessageListener != null) {
                    mMessageListener.messageReceived(_smsBody, _smsSender);
                }
            }
        }
        /*
        Bundle data = intent.getExtras();
        Object[] pdus = (Object[]) data.get("pdus");
        for(int i=0; i<pdus.length; i++){
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
            String message = "Sender : " + smsMessage.getDisplayOriginatingAddress()
                    + "Email From: " + smsMessage.getEmailFrom()
                    + "Emal Body: " + smsMessage.getEmailBody()
                    + "Display message body: " + smsMessage.getDisplayMessageBody()
                    + "Time in millisecond: " + smsMessage.getTimestampMillis()
                    + "Message: " + smsMessage.getMessageBody();
            mListener.messageReceived(message);
        }
         */
    }

    private boolean messageWithSchedule(String smsBody) {
        boolean _hasTokenTrue = false;
        boolean _hasDateTrue = false;
        boolean _hasHourTrue = false;
        String[] _searchedTokens = {"appointment", "schedule"};
        for(String _token: _searchedTokens) {
            if(smsBody.contains(_token)) {
                _hasTokenTrue = true;
            }
        }
        Pattern _pattern = Pattern.compile(".*([01]?[0-9]|2[0-3]):[0-5][0-9].*");
        Matcher _matcher = _pattern.matcher(smsBody);
        if(_matcher.matches()) {
            return _hasTokenTrue;
        }
        else {
            return false;
        }
    }

    public static void bindListener(MessageListener _messageListener){
        mMessageListener = _messageListener;
    }


}

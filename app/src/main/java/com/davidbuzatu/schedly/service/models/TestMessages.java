package com.davidbuzatu.schedly.service.models;

import android.util.Log;

import com.davidbuzatu.schedly.service.MonitorIncomingSMSService;

public class TestMessages {

    private static TestMessages testMessages;

    public static TestMessages getInstance() {
        if(testMessages == null) {
            testMessages = new TestMessages();
        }
        return testMessages;
    }

    public void test(StringBuilder message) {
        TSMSMessage _testMessage = new TSMSMessage(message, "0724154387", 1570286399839L);
        MonitorIncomingSMSService _testMonitor = new MonitorIncomingSMSService();
        Log.d("Testing", "Testing...");
        _testMonitor.messageReceived(_testMessage);
    }
}

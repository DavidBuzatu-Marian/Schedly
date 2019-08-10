package com.example.schedly.model;

public class TSMSMessage {
    private StringBuilder mSMSBody;
    private String mSMSSender;
    private Long mTimeReceived;

    public TSMSMessage(StringBuilder smsBody, String smsSender, Long timeReceived) {
        mSMSBody = smsBody;
        mSMSSender = smsSender;
        mTimeReceived = timeReceived;
    }

    public void setmSMSBody(String newSMSBody) {

    }

    public Long getmTimeReceived() {
        return mTimeReceived;
    }

    public String getmSMSSender() {
        return mSMSSender;
    }

    public String getmSMSBody() {
        return mSMSBody.toString();
    }
}

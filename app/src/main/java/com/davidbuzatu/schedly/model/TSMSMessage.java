package com.davidbuzatu.schedly.model;

public class TSMSMessage {
    private StringBuilder mSMSBody;
    private String mSMSSender;
    private Long mTimeReceived;
    private int mNROfAppointments;

    public TSMSMessage(StringBuilder smsBody, String smsSender, Long timeReceived) {
        mSMSBody = smsBody;
        mSMSSender = smsSender;
        mTimeReceived = timeReceived;
        mNROfAppointments = 0;
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

    public int getmNROfAppointments() {
        return mNROfAppointments;
    }

    public void setmNROfAppointments(int mNROfAppointments) {
        this.mNROfAppointments = mNROfAppointments;
    }
}

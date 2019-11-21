package com.davidbuzatu.schedly.model;

public interface MessageListener {
    void messageReceived(TSMSMessage newSMSMessage);
}

package com.davidbuzatu.schedly.service.models;

public interface MessageListener {
    void messageReceived(TSMSMessage newSMSMessage);
}

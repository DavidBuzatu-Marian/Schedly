package com.davidbuzatu.schedly.service.models;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageChecker {

    private String messageContent;

    public MessageChecker(TSMSMessage newSMSMessage) {
        this.messageContent = newSMSMessage.getmSMSBody();
    }

    public boolean isMessageForAppointment() {
        Pattern patternDate = Pattern.compile("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])");
        Pattern patternDay = Pattern.compile("(?i)(monday|tuesday|wednesday|thursday|friday|saturday|sunday)");
        Pattern patternHour = Pattern.compile("([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])\\s*([AaPp][Mm])|([0-9]|0[0-9]|1[0-9]|2[0-3])\\s*([AaPp][Mm])");
        Pattern patternKeyWork = Pattern.compile("\\b(\\w*appointment|appoint|schedule\\w*)\\b");
        ArrayList<Matcher> matchers = new ArrayList<>(4);
        matchers.add(patternDate.matcher(messageContent));
        matchers.add(patternDay.matcher(messageContent));
        matchers.add(patternHour.matcher(messageContent));
        matchers.add(patternKeyWork.matcher(messageContent));
        for(Matcher matcher: matchers) {
            if(matcher.find()) {
                return true;
            }
        }
        return false;
    }
}

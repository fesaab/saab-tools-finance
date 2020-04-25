package com.saab.tools.finance.service;

import com.saab.tools.finance.model.entity.SMSNotification;

import java.time.Instant;
import java.time.ZoneId;

public class SMSParser {

    public boolean isIgnored(String message) {
        return !message.startsWith("Nedbank: Transaction.")
                && !message.startsWith("Nedbank: Debit order")
                && !message.startsWith("Nedbank: Warning.");
    }

    public SMSNotification parse(String id, String smsMessage, Long date, String number) {
        SMSNotification sms = SMSNotification.builder()
                .date(Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDateTime())
                .number(number)
                .message(smsMessage)
                .id(id)
                .build();

        return sms;
    }
}

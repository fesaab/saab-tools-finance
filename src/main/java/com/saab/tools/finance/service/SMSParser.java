package com.saab.tools.finance.service;

import com.saab.tools.finance.model.entity.SMSNotification;

import java.time.Instant;
import java.time.ZoneId;

public class SMSParser {

    public boolean isIgnored(String message) {
        return !message.contains("Itau Uniclass")
                && !message.startsWith("O pagamento de TITULOS");
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

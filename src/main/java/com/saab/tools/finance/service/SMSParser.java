package com.saab.tools.finance.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.saab.tools.finance.model.entity.SMSNotification;

import java.io.IOException;

public class SMSParser {

    private ObjectMapper objectMapper;

    public SMSParser() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    }

    public SMSNotification parse(String smsText) {
        try {
            SMSNotification sms = objectMapper.readValue(smsText, SMSNotification.class);
            sms.setRawMessage(smsText);
            return sms;
        } catch (IOException e) {
            throw new RuntimeException("Error converting SMSNotification!", e);
        }
    }

}

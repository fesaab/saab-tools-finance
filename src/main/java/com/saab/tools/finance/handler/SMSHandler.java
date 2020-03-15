package com.saab.tools.finance.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saab.tools.finance.model.entity.SMSNotification;

import java.io.IOException;

public class SMSHandler implements RequestHandler<SQSEvent, Void> {

    private ObjectMapper objectMapper;

    public SMSHandler() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for(SQSEvent.SQSMessage msg : event.getRecords()){
            System.out.println("Received message: " + msg.getBody());

            try {
                SMSNotification sms = objectMapper.readValue(msg.getBody(), SMSNotification.class);
                System.out.println("SMS sucessfully converted: " + sms.toString());

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error converting the event body to SMSNotification!", e);
            }

        }
        return null;

    }

}
package com.saab.tools.finance.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.repository.TransactionRepository;

import java.io.IOException;
import java.math.BigDecimal;

public class SMSHandler implements RequestHandler<SQSEvent, Void> {

    private ObjectMapper objectMapper;
    private TransactionRepository repository;

    public SMSHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        this.repository = new TransactionRepository();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            System.out.println("Received message: " + msg.getBody());

            try {
                SMSNotification sms = objectMapper.readValue(msg.getBody(), SMSNotification.class);
                System.out.println("SMS sucessfully converted: " + sms.toString());

                Transaction t = new Transaction();
                t.setDate(sms.getDate());
                t.setType("EXPENSE");
                t.setCategory("TEST");
                t.setDescription(String.format("%s (%s)", sms.getMessage(), sms.getNumber()));
                t.setValue(new BigDecimal("465.77"));

                repository.insert(t);

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error converting the event body to SMSNotification!", e);
            }

        }
        return null;

    }

}
package com.saab.tools.finance.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.repository.SmsRepository;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;

@Log4j2
public class SMSHandler implements RequestHandler<SQSEvent, Void> {

    private ObjectMapper objectMapper;
    private SmsRepository repository;
    private AmazonSQS sqs;
    private final String QUEUE_URL = System.getenv("QUEUE_URL");

    public SMSHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        this.repository = new SmsRepository();

        sqs = AmazonSQSClientBuilder.defaultClient();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            log.info("Start process of message: " + msg.getBody());

            try {
                // Convert the body of the message to a SMSNotification
                SMSNotification sms = objectMapper.readValue(msg.getBody(), SMSNotification.class);
                log.info("SMS sucessfully converted: " + sms.toString());

                // From the SMSNotification create the transaction on DB
                repository.insert(sms);
                log.info("Sms saved on DB: " + sms.getId());

                // Finaly remove the register from the queue
                log.info("About to delete message from the queue. Message receipt handler: " + msg.getReceiptHandle());
                DeleteMessageResult deleteResult = sqs.deleteMessage(new DeleteMessageRequest(QUEUE_URL, msg.getReceiptHandle()));
                int statusCode = deleteResult.getSdkHttpMetadata().getHttpStatusCode();
                if (statusCode != 200) {
                    log.info("Error deleting from the queue! Message receipt handler: " + msg.getReceiptHandle());
                    throw new RuntimeException("Error removing the message from the queue! Received http status code " + statusCode);
                } else {
                    log.info("Message deleted from the queue! Message receipt handler: " + msg.getReceiptHandle());
                }

            } catch (Exception e) {
                log.error("Error processing message!", e);
            }

        }
        return null;

    }

}
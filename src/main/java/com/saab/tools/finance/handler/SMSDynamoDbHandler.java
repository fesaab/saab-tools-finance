package com.saab.tools.finance.handler;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Sms;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.repository.SmsRepository;
import com.saab.tools.finance.model.repository.TransactionRepository;
import com.saab.tools.finance.service.SMSParser;
import com.saab.tools.finance.service.TransactionParser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;
import java.util.Map;

@Log4j2
public class SMSDynamoDbHandler implements RequestHandler<DynamodbEvent, Void> {

    private TransactionRepository transactionRepository;
    private TransactionParser transactionParser;
    private SMSParser smsParser;
    private SmsRepository smsRepository;

    public SMSDynamoDbHandler() {
        this.transactionRepository = new TransactionRepository();
        this.transactionParser = new TransactionParser();
        this.smsParser = new SMSParser();
        this.smsRepository = new SmsRepository();
    }

    @Override
    public Void handleRequest(DynamodbEvent ddbEvent, Context context) {

        for (DynamodbEvent.DynamodbStreamRecord record: ddbEvent.getRecords()) {
            if (record.getEventName().equals("INSERT")) {
                Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
                if (newImage != null) {
                    try {
                        log.info("Start process of message: " + newImage);

                        // Parse the Transaction object from the SMS
                        Transaction transaction = this.parseTransaction(newImage);

                        // Persist the transaction
                        if (transaction != null) {
                            this.persistTransaction(transaction);
                        }

                    } catch (Exception e) {
                        log.error("Error processing message!", e);
                        throw e;
                    }
                } else {
                    log.info(String.format("No new image in the message! %s", ReflectionToStringBuilder.toString(record)));
                }
            } else {
                log.info(String.format("Event name is not INSERT! Record=%s", ReflectionToStringBuilder.toString(record)));
            }
        }

        return null;
    }

    private Transaction parseTransaction(Map<String, AttributeValue> newImage) {

        // Check if the message should be ignored
        String smsMessage = newImage.get("message").getS();
        if (smsParser.isIgnored(smsMessage)) {
            log.info("SMS ignored: " + smsMessage);
        } else {
            // Convert the image to a SMSNotification
            Long date = Long.parseLong(newImage.get("date").getN());
            String number = newImage.get("number").getS();
            String id = newImage.get("id").getS();
            SMSNotification sms = smsParser.parse(id, smsMessage, date, number);
            log.info("SMS sucessfully converted: " + sms);

            // Parse the SMS to a Transaction
            Transaction transaction = transactionParser.parseFromSms(sms);
            log.info("Transaction parsed from SMS: " + transaction);

            return transaction;
        }

        return null;
    }

    private void persistTransaction(Transaction transaction) {
        // If it is a new transaction then save it
        if (!transaction.isReversed()) {
            transactionRepository.save(transaction);
            log.info("Transaction saved on DB: " + transaction.toString());

            // Flag that the SMS generated a transaction
            Sms sms = smsRepository.findById(transaction.getSmsId());
            sms.setGenerateTransaction(true);
            smsRepository.save(sms);
            log.info("SMS updated on DB: " + sms.toString());
        }
        // If the transaction was reversed then just updated it
        else {
            List<Transaction> foundTransactionList = transactionRepository.findByDescriptionAndValue(transaction.getDescription(), transaction.getValue());
            if (foundTransactionList != null && !foundTransactionList.isEmpty()) {
                Transaction lastTransaction = foundTransactionList.get(foundTransactionList.size() - 1);
                lastTransaction.setReversed(transaction.isReversed());
                lastTransaction.setReversedDate(transaction.getReversedDate());
                transactionRepository.save(lastTransaction);
                log.info("Transaction reversed on DB: " + lastTransaction);
            } else {
                log.warn("COULD NOT FIND THE ORIGINAL TRANSACTION TO REVERT!!! Transaction: " + transaction + ". SMS: " + transaction.getSmsId());
                // TODO: publicar métrica
            }
        }
    }
}

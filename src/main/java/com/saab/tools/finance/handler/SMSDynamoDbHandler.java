package com.saab.tools.finance.handler;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.repository.TransactionRepository;
import com.saab.tools.finance.service.TransactionParser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Log4j2
public class SMSDynamoDbHandler implements RequestHandler<DynamodbEvent, Void> {

    private TransactionRepository transactionRepository;
    private TransactionParser transactionParser;

    public SMSDynamoDbHandler() {
        this.transactionRepository = new TransactionRepository();
        this.transactionParser = new TransactionParser();
    }

    @Override
    public Void handleRequest(DynamodbEvent ddbEvent, Context context) {

        for (DynamodbEvent.DynamodbStreamRecord record: ddbEvent.getRecords()) {
            Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
            if (newImage != null) {
                log.info("Start process of message: " + newImage);

                try {
                    // Convert the image to a SMSNotification
                    SMSNotification sms = SMSNotification.builder()
                            .date(Instant.ofEpochMilli(Long.parseLong(newImage.get("date").getN())).atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .number(newImage.get("number").getS())
                            .message(newImage.get("message").getS())
                            .build();
                    log.info("SMS sucessfully converted: " + sms);

                    // Parse the SMS to a Transaction
                    Transaction transaction = transactionParser.parseFromSms(sms);
                    log.info("Transaction parsed from SMS: " + transaction);

                    // If it is a new transaction then save it
                    if (!transaction.isReversed()) {
                        transactionRepository.save(transaction);
                        log.info("Transaction saved on DB: " + transaction.getId());
                        // TODO: publicar métrica com o valor da transação (será que faz sentido?)
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
                            log.warn("COULD NOT FIND THE ORIGINAL TRANSACTION TO REVERT!!! Transaction: " + transaction + ". SMS: " + sms);
                            // TODO: publicar métrica
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing message!", e);
                }
            } else {
                log.warn(String.format("No new image in the message! %s", ReflectionToStringBuilder.toString(record)));
            }

        }

        return null;
    }
}

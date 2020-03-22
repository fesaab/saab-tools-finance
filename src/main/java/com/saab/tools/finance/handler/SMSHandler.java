package com.saab.tools.finance.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.repository.SmsRepository;
import com.saab.tools.finance.model.repository.TransactionRepository;
import com.saab.tools.finance.service.SMSParser;
import com.saab.tools.finance.service.TransactionParser;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 {
 "date": 1584888588001,
 "number": "+2783930001111",
 "message": "Nedbank: Transaction. Purchase of R349,00 on a/c **2370 at NORMAN GOODFELLOWS UN. 21 Mar 20 at 12:22
 }

 {
 "date": 1584888588001,
 "number": "+2783930001111",
 "message": "Nedbank: Transaction. Purchase of R21,56 on a/c **2370 at UBER SA helpubercom Ga. 21 Mar 20 at 12:22"
 }

 {
 "date": 1584888588001,
 "number": "+2783930001111",
 "message": "Nedbank: Transaction.  R21,00 on a/c **2370 at UBER SA helpubercom Ga was reversed.  21 Mar 20 at 12:29"
 }
 */
@Log4j2
public class SMSHandler implements RequestHandler<SQSEvent, Void> {

    private SmsRepository smsRepository;
    private TransactionRepository transactionRepository;
    private AmazonSQS sqs;
    private SMSParser smsParser;
    private TransactionParser transactionParser;

    private final String QUEUE_URL = System.getenv("QUEUE_URL");

    public SMSHandler() {
        this.sqs = AmazonSQSClientBuilder.defaultClient();
        this.smsRepository = new SmsRepository();
        this.transactionRepository = new TransactionRepository();
        this.smsParser = new SMSParser();
        this.transactionParser = new TransactionParser();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            log.info("Start process of message: " + msg.getBody());

            try {
                // Convert the body of the message to a SMSNotification
                SMSNotification sms = smsParser.parse(msg.getBody());
                log.info("SMS sucessfully converted: " + sms);

                // Parse the SMS to a Transaction
                Transaction transaction = transactionParser.parseFromSms(sms);
                log.info("Transaction parsed from SMS: " + transaction);

                // TODO: antes de salvar no banco primeiro atualizar no Google Sheets!!!!

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
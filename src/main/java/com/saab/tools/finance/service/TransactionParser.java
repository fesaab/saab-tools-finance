package com.saab.tools.finance.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.saab.tools.finance.exception.SMSNotParsedException;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionParser {

    private static Pattern REGEX_TRANSACTION_VALUE_AND_SHOP = Pattern.compile("Purchase of R([0-9]+,[0-9]*) on a\\/c .* at (.*)");
    private static Pattern REGEX_TRANSACTION_REVERSED_VALUE_AND_SHOP = Pattern.compile("R([0-9]+,[0-9]*) on a\\/c .* at (.*) was reversed");
    private static Pattern REGEX_DEBIT_EXTRACT_VALUE = Pattern.compile("Debit order of R([0-9]+,[0-9]*)");
    private static Pattern REGEX_DEBIT_EXTRACT_SHOP = Pattern.compile("Ref: (.*)");
    private static Pattern REGEX_WARNING_EXTRACT_VALUE_AND_SHOP = Pattern.compile("Card transaction of R([0-9]+,[0-9]*) on a\\/c .* at (.*)");
    private static Pattern REGEX_PAYMENT_VALUE = Pattern.compile("Payment of R([0-9]+,[0-9]*) from a\\/c .*");
    private static Pattern REGEX_PAYMENT_SHOP = Pattern.compile("Ref: (.*)");
    private static Pattern REGEX_OVERNIGHT_VALUE = Pattern.compile("Overnight Transaction deposit of R([0-9]+,[0-9]*) into a\\/c .*");
    private static Pattern REGEX_OVERNIGHT_SHOP = Pattern.compile("Ref: (.*)");
    private static Pattern REGEX_TRANSFER_VALUE_AND_SHOP = Pattern.compile("R([0-9]+,[0-9]*) transferred (from .*)");
    private static Pattern REGEX_PAID_VALUE = Pattern.compile("R([0-9]+,[0-9]*) paid to a\\/c .*");
    private static Pattern REGEX_PAID_SHOP = Pattern.compile("Ref: (.*)");


    private static String REVERSE_MESSAGE = "was reversed";

    private ObjectMapper objectMapper;

    public TransactionParser() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    }

    public Transaction parseFromSms(SMSNotification sms) {
        ExtractedTransaction et = extractData(sms.getMessage());

        return Transaction.builder()
                .date(sms.getDate())
                .value(et.getValue())
                .description(et.getShop())
                .category("TODO")
                .type(et.getType())
                .reversed(et.isReversed())
                .reversedDate(et.isReversed() ? sms.getDate(): null)
                .smsId(sms.getId())
                .build();
    }

    private ExtractedTransaction extractData(String message) {
        ExtractedTransaction et = null;

        // Normalize te dots and spaces on the message. More than one consecutive dot is a problem for the algorithm
        message = message.replaceAll("[\\.]+", "\\.");
        message = message.replaceAll("[ ]+", " ");

        if (message.contains("Nedbank: Transaction.")) {
            et = extractDataTransaction(message);
        } else if (message.contains("Nedbank: Debit order")) {
            et = extractDataDebit(message);
        } else if (message.contains("Nedbank: Warning.")) {
            et = extractDataWarning(message);
        }

        if (et == null)
            throw new IllegalArgumentException("Message not recognized! '" + message + "'");
        else
            return et;
    }

    private ExtractedTransaction extractDataWarning(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);

        // Splitted message:
        // -----------------
        // Nedbank: Warning
        // Card transaction of R14,96 on a/c **1111 outside SA at Amazon web servic...
        // 22 Feb 20 at 16:37
        String[] messageArray = message.split("\\.");
        String warningMessage = messageArray[1].trim();

        // Extract value and shop
        Matcher valueAndShopMatcher = REGEX_WARNING_EXTRACT_VALUE_AND_SHOP.matcher(warningMessage);
        if (valueAndShopMatcher.find()) {

            // Value
            String valueStr = valueAndShopMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(valueAndShopMatcher.group(2).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataDebit(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);

        // Splitted message:
        // -----------------
        // Nedbank: Debit order of R441,55 paid from a/c **1111
        // Ref: VOXTELECOMR111111
        // 23 Feb 20 at 05:00
        String[] messageArray = message.split("\\.");
        String debitMessage = messageArray[0].trim();
        String refMessage = messageArray[1].trim();

        // Extract value
        Matcher valueMatcher = REGEX_DEBIT_EXTRACT_VALUE.matcher(debitMessage);
        if (valueMatcher.find()) {
            String valueStr = valueMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);
        } else {
            throw new SMSNotParsedException("Couldn't extract value from the message '" + message + "'");
        }

        // Extract shop
        Matcher shopMatcher = REGEX_DEBIT_EXTRACT_SHOP.matcher(refMessage);
        if (shopMatcher.find()) {
            et.setShop(shopMatcher.group(1).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTransaction(String message) {
        ExtractedTransaction et = null;

        if (message.contains("Purchase")) {
            et = extractDataTransactionPurchase(message);
        } else if (message.contains("Payment")) {
            et = extractDataTransactionPayment(message);
        } else if (message.contains("Overnight")) {
            et = extractDataTransactionOvernight(message);
        } else if (message.contains(REVERSE_MESSAGE)) {
            et = extractDataTransactionReversed(message);
        } else if (message.contains("transferred from")) {
            et = extractDataTransactionTransferred(message);
        } else if (message.contains("paid to")) {
            et = extractDataTransactionPaid(message);
        }

        return et;
    }

    private ExtractedTransaction extractDataTransactionPaid(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_INCOME);
        String[] messageArray = message.split("\\.");

        // Splitted message:
        // -----------------
        // Nedbank: Transaction
        // R7152,52 paid to a/c **1111
        // Ref: COMPANY CMPN-23
        // 09 Apr 20 at 02:01
        String valueMessage = messageArray[1].trim();
        String shopMessage = messageArray[2].trim();
        Matcher valueMatcher = REGEX_PAID_VALUE.matcher(valueMessage);
        Matcher shopMatcher = REGEX_PAID_SHOP.matcher(shopMessage);
        if (valueMatcher.find() && shopMatcher.find()) {
            // Value
            String valueStr = valueMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(shopMatcher.group(1).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTransactionTransferred(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);
        String[] messageArray = message.split("\\.");

        // Splitted message:
        // -----------------
        // Nedbank: Transaction
        // R2000,00 transferred from a/c **1111 to a/c **2222
        // 09 Apr 20 at 02:01
        String valueMessage = messageArray[1].trim();
        Matcher valueAndShopMatcher = REGEX_TRANSFER_VALUE_AND_SHOP.matcher(valueMessage);
        if (valueAndShopMatcher.find()) {
            // Value
            String valueStr = valueAndShopMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(valueAndShopMatcher.group(2).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTransactionOvernight(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_INCOME);
        String[] messageArray = message.split("\\.");

        // Splitted message:
        // -----------------
        // Nedbank: Transaction
        // Overnight Transaction deposit of R49,99 into a/c **1111
        // Ref: Bottlesapp 541282
        // 09 Apr 20 at 02:01
        String valueMessage = messageArray[1].trim();
        String shopMessage = messageArray[2].trim();
        Matcher valueMatcher = REGEX_OVERNIGHT_VALUE.matcher(valueMessage);
        Matcher shopMatcher = REGEX_OVERNIGHT_SHOP.matcher(shopMessage);
        if (valueMatcher.find() && shopMatcher.find()) {
            // Value
            String valueStr = valueMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(shopMatcher.group(1).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTransactionReversed(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);
        String[] messageArray = message.split("\\.");

        // Splitted message:
        // -----------------
        // Nedbank: Transaction
        // R21,00 on a/c **2370 at UBER SA helpubercom Ga was reversed
        // 21 Mar 20 at 12:29
        String purchaseMessage = messageArray[1].trim();

        // Extract value and shop name
        Matcher valueAndShopMatcher = REGEX_TRANSACTION_REVERSED_VALUE_AND_SHOP.matcher(purchaseMessage);
        if (valueAndShopMatcher.find()) {

            // Value
            String valueStr = valueAndShopMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(valueAndShopMatcher.group(2).trim());

            // Reversed
            et.setReversed(true);
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTransactionPayment(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);
        String[] messageArray = message.split("\\.");

        // Splitted message:
        // -----------------
        // Nedbank: Transaction
        // Purchase of R114,99 from a/c **1111
        // Ref: English one
        // 22 Mar 20 at 11:16
        String valueMessage = messageArray[1].trim();
        String shopMessage = messageArray[2].trim();
        Matcher valueMatcher = REGEX_PAYMENT_VALUE.matcher(valueMessage);
        Matcher shopMatcher = REGEX_PAYMENT_SHOP.matcher(shopMessage);
        if (valueMatcher.find() && shopMatcher.find()) {
            // Value
            String valueStr = valueMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(shopMatcher.group(1).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTransactionPurchase(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);
        String[] messageArray = message.split("\\.");

        // Splitted message:
        // -----------------
        // Nedbank: Transaction
        // Purchase of R114,99 on a/c **1111 at FLM Roeland Street W
        // 22 Mar 20 at 11:16
        String purchaseMessage = messageArray[1].trim();
        Matcher valueAndShopMatcher = REGEX_TRANSACTION_VALUE_AND_SHOP.matcher(purchaseMessage);
        if (valueAndShopMatcher.find()) {
            // Value
            String valueStr = valueAndShopMatcher.group(1).replace(",", ".");
            BigDecimal value = new BigDecimal(valueStr).setScale(2);
            et.setValue(value);

            // Shop
            et.setShop(valueAndShopMatcher.group(2).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private class ExtractedTransaction {
        private BigDecimal value;
        private String shop;
        private boolean reversed;
        private String type;
    }

}

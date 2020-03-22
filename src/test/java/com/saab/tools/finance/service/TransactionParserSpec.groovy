package com.saab.tools.finance.service

import com.saab.tools.finance.model.entity.SMSNotification
import com.saab.tools.finance.model.entity.Transaction
import org.junit.Assert
import spock.lang.Specification

import java.time.LocalDateTime

class TransactionParserSpec extends Specification {

    TransactionParser transactionParser;

    def "setup"() {
        transactionParser = new TransactionParser()
    }

    def "test transaction message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Transaction. Purchase of %s on a/c **1111 at %s. 22 Mar 20 at 11:16.",
                                            strValue,
                                            shop)
        SMSNotification sms = buildSmsNotification(smsMessage, date)

        when: "the parser is executed"
        Transaction t = transactionParser.parseFromSms(sms)

        then: "no exception =]"
        noExceptionThrown()

        and: "the conversion matches"
        Assert.assertTrue(t.getDate() == date)
        Assert.assertTrue(t.getValue() == value)
        Assert.assertTrue(t.getDescription() == shop)
        Assert.assertTrue(t.isReversed() == reversed)

        where: "many test values"
        strValue  | value                    | shop                    | reversed
        "R114,99" | new BigDecimal("114.99") | "FLM Roeland Street W"  | false
        "R9,07"   | new BigDecimal("9.07")   | "Amazon AWS"            | false
        "R349,00" | new BigDecimal("349.00") | "NORMAN GOODFELLOWS UN" | false
    }

    def "test debit message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Debit order of %s paid from a/c **1111. Ref: %s. 23 Feb 20 at 05:00. Queries - Use the Money app",
                strValue,
                shop)
        SMSNotification sms = buildSmsNotification(smsMessage, date)

        when: "the parser is executed"
        Transaction t = transactionParser.parseFromSms(sms)

        then: "no exception =]"
        noExceptionThrown()

        and: "the conversion matches"
        Assert.assertTrue(t.getDate() == date)
        Assert.assertTrue(t.getValue() == value)
        Assert.assertTrue(t.getDescription() == shop)
        Assert.assertTrue(t.isReversed() == reversed)

        where: "many test values"
        strValue  | value                    | shop                 | reversed
        "R441,55" | new BigDecimal("441.55") | "VOXTELECOMR1111111" | false
    }

    def "test warning message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Warning. Card transaction of %s on a/c **1111 outside SA at %s. 22 Feb 20 at 16:37.",
                strValue,
                shop)
        SMSNotification sms = buildSmsNotification(smsMessage, date)

        when: "the parser is executed"
        Transaction t = transactionParser.parseFromSms(sms)

        then: "no exception =]"
        noExceptionThrown()

        and: "the conversion matches"
        Assert.assertTrue(t.getDate() == date)
        Assert.assertTrue(t.getValue() == value)
        Assert.assertTrue(t.getDescription() == shopToAssert)

        where: "many test values"
        strValue  | value                    | shop                   | shopToAssert        | reversed
        "R14,96"  | new BigDecimal("14.96")  | "Amazon web servic..." | "Amazon web servic" | false
    }

    def "test transaction reversed message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = "Nedbank: Transaction.  R21,00 on a/c **1111 at UBER SA helpubercom Ga was reversed.  21 Mar 20 at 12:29."
        SMSNotification sms = buildSmsNotification(smsMessage, date)

        when: "the parser is executed"
        Transaction t = transactionParser.parseFromSms(sms)

        then: "no exception =]"
        noExceptionThrown()

        and: "the conversion matches"
        Assert.assertTrue(t.getDate() == date)
        Assert.assertTrue(t.getValue() == new BigDecimal("21.00"))
        Assert.assertTrue(!t.getDescription().contains("was reversed"))
        Assert.assertTrue(t.isReversed())
    }

    private SMSNotification buildSmsNotification(String message, LocalDateTime date) {
        return SMSNotification.builder()
                .date(date)
                .message(message)
                .build();
    }

}

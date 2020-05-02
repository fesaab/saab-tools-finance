package com.saab.tools.finance.service

import com.saab.tools.finance.model.entity.SMSNotification
import com.saab.tools.finance.model.entity.Transaction
import org.junit.Assert
import spock.lang.Unroll

import java.awt.font.TransformAttribute
import java.time.LocalDateTime

@Unroll
class TransactionParserSpec extends AbstractSpec {

    TransactionParser transactionParser;
    CategoryMapper categoryMapper;

    def "setup"() {
        categoryMapper = Mock(CategoryMapper)
        categoryMapper.map(_) >> "TODO"

        transactionParser = new TransactionParser(null, categoryMapper, null)
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
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)

        where: "many test values"
        strValue  | value                    | shop                    | reversed
        "R114,99" | new BigDecimal("114.99") | "FLM Roeland Street W"  | false
        "R9,07"   | new BigDecimal("9.07")   | "Amazon AWS"            | false
        "R349,00" | new BigDecimal("349.00") | "NORMAN GOODFELLOWS UN" | false
    }

    def "test overnight message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Transaction. Overnight Transaction  deposit of %s into a/c **2370. Ref: %s.... 09 Apr 20 at 02:01.",
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
        Assert.assertTrue(t.getType() == Transaction.TYPE_INCOME)

        where: "many test values"
        strValue  | value                    | shop                    | reversed
        "R49,99"  | new BigDecimal("49.99")  | "Bottlesapp 541282"     | false
    }

    def "test transferred message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Transaction. %s transferred %s. 28 Mar 20 at 15:34.",
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
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)

        where: "many test values"
        strValue    | value                      | shop                                | reversed
        "R2000,00"  | new BigDecimal("2000.00")  | "from a/c **1111 to a/c **2222"     | false
        "R4123,25"  | new BigDecimal("4123.25")  | "from a/c **3333 to a/c **4444"     | false
    }

    def "test paid message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Transaction. %s paid to a/c **1111. Ref: %s. 28 Mar 20 at 15:34.",
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
        Assert.assertTrue(t.getType() == Transaction.TYPE_INCOME)

        where: "many test values"
        strValue    | value                      | shop                  | reversed
        "R7152,52"  | new BigDecimal("7152.52")  | "COMPANY CMPN-23"     | false
    }

    def "test payment message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        String smsMessage = String.format("Nedbank: Transaction. Payment of %s from a/c **1111. Ref: %s. 04 Apr 20 at 17:15.",
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
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)

        where: "many test values"
        strValue   | value                      | shop                    | reversed
        "R5620,00" | new BigDecimal("5620.00")  | "English One"           | false
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
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)

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
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)

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
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)
    }

}

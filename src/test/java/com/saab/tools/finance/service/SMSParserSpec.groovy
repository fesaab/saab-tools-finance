package com.saab.tools.finance.service

class SMSParserSpec extends AbstractSpec {

    SMSParser smsParser;

    def "setup"() {
        smsParser = new SMSParser()
    }

    def "test if message should be ignored"() {
        when: "the parser is executed"
        boolean isValid = smsParser.isIgnored(smsMessage)

        then: "no exception =]"
        noExceptionThrown()

        and: "the message should or shouldn't be ignored"
        shouldBeIgnored == isValid

        where: "many test values"
        smsMessage << [
                "Nedbank. #StayHomeChallenge Tip: Need to pay your municipal accounts, DSTV and more? Pay online with Nedbank and EasyPay. It is safe, easy and convenient. Get the EasyPay App or visit www.easypay.co.za, load your Nedbank Card and pay.",
                "Nedbank. We have removed Saswitch fees from ATM withdrawals, meaning you can use any Banks ATM to withdraw emergency cash. Remember using your card to pay is the safer ,cleaner option during lockdown period.  Keep safe & on top of your goals, do all your banking on the Nedbank Money app/Online.",
                "Nedbank: Transaction. Payment of R12381,27 from a/c **2370. Ref: Rental. 30 Mar 20 at 12:25.",
                "Nedbank: Transaction. Purchase of R21,00 on a/c **2370 at UBER SA helpubercom Ga. 25 Mar 20 at 13:43."
        ]

        shouldBeIgnored << [
                true,
                true,
                false,
                false
        ]
    }

}

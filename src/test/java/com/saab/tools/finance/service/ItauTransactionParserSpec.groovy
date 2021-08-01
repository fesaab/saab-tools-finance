package com.saab.tools.finance.service

import com.saab.tools.finance.model.entity.SMSNotification
import com.saab.tools.finance.model.entity.Transaction
import org.junit.Assert
import spock.lang.Unroll

import java.time.LocalDateTime

@Unroll
class ItauTransactionParserSpec extends AbstractSpec {

    ItauTransactionParser transactionParser;
    CategoryMapper categoryMapper;

    def "setup"() {
        categoryMapper = Mock(CategoryMapper)
        categoryMapper.map(_) >> "TODO"

        transactionParser = new ItauTransactionParser(null, categoryMapper, null)
    }

    def "test message"() {
        given: "the SMS Notification"
        LocalDateTime date = LocalDateTime.now()
        SMSNotification sms = buildSmsNotification(smsMessage, date)

        when: "the parser is executed"
        Transaction t = transactionParser.parseFromSms(sms)

        then: "no exception =]"
        noExceptionThrown()

        and: "the conversion matches"
        Assert.assertTrue(t.getDate() == date)
        Assert.assertTrue(t.getValue() == value)
        Assert.assertTrue(t.getDescription() == shop)
        Assert.assertTrue(t.isReversed() == false)
        Assert.assertTrue(t.getType() == Transaction.TYPE_EXPENSE)

        where: "many test values"
        smsMessage << [
                "Itau Uniclass: COMPRA APROVADA no cartao com final 9999 de R\$ 322,63 no dia 31/07 as 12:49 em SUPERMERC. Consulte seus lancamentos no app Itau no celular.",
                "Itau Uniclass: COMPRA APROVADA no cartao com final 9999 de R\$ 167,69 no dia 29/07 as 17:07 em DROG CARR. Consulte seus lancamentos no app Itau no celular.",
                "Itau Uniclass: COMPRA APROVADA no cartao com final 9999 de R\$ 106,00 no dia 29/07 as 17:49 em PET SHOP. Consulte seus lancamentos no app Itau no celular.",
                "Itau Uniclass C/C XXX99-9: Saque de R\$ 300,00 realizado em 26/07 as 15:11 no BCO24H. Consulte seu extrato no app Itau no celular.",
                "Itau Uniclass C/C XXX99-9: Saque de R\$ 125,00 realizado em 29/07 as 20:44 no AG123. Consulte seu extrato no app Itau no celular.",
                "O pagamento de TITULOS agendado para o dia 15/07/2021, valor de R\$4.034,84 foi efetivado. Conta XXX99-9, 15/07 21:31.",
                "Realizado TED no valor de R\$1.950,00 em 21/07 12:21. Conta Itau Uniclass debitada: XXX99-9.",
                "Realizado DOC no valor de R\$129,00 em 07/06 14:54. Conta Itau Uniclass debitada: XXX99-9.",
        ]
        value                         | shop
        new BigDecimal("322.63")  | "SUPERMERC"
        new BigDecimal("167.69")  | "DROG CARR"
        new BigDecimal("106.00")  | "PET SHOP"
        new BigDecimal("300.00")  | "Saque BCO24H"
        new BigDecimal("125.00")  | "Saque AG123"
        new BigDecimal("4034.84") | "Agendada"
        new BigDecimal("1950.00") | "TED"
        new BigDecimal("129.00")  | "DOC"
    }

}

package com.saab.tools.finance.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.saab.tools.finance.exception.SMSNotParsedException;
import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.repository.CategoryMappingRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItauTransactionParser implements TransactionParser {

    // Itau Uniclass: COMPRA APROVADA no cartao com final 9999 de R$ 322,63 no dia 31/07 as 12:49 em SUPERMERC. Consulte seus lancamentos no app Itau no celular.
    private static Pattern REGEX_ITAU_TRANSACTION_VALUE_AND_SHOP = Pattern.compile("Itau Uniclass: COMPRA APROVADA no cartao com final .* de R\\$ (.*) no dia .* as .* em (.*)\\. .*");

    // Itau Uniclass C/C XXX99-9: Saque de R$ 300,00 realizado em 26/07 as 15:11 no BCO24H. Consulte seu extrato no app Itau no celular.
    private static Pattern REGEX_ITAU_WITHDRAW = Pattern.compile("Itau Uniclass C/C .*: Saque de R\\$ (.*) realizado em .* as .* no (.*)\\. .*");

    // O pagamento de TITULOS agendado para o dia 15/07/2021, valor de R$4.034,84 foi efetivado. Conta XXX99-9, 15/07 21:31.
    private static Pattern REGEX_ITAU_SCHEDULED = Pattern.compile("O pagamento de TITULOS agendado para o dia .*, valor de R\\$(.*) foi efetivado\\. .*");

    // Realizado TED no valor de R$1.950,00 em 21/07 12:21. Conta Itau Uniclass debitada: XXX99-9.
    // Realizado DOC no valor de R$129,00 em 07/06 14:54. Conta Itau Uniclass debitada: XXX99-9.
    private static Pattern REGEX_ITAU_TED_DOC = Pattern.compile("Realizado (.*) no valor de R\\$(.*) em .*\\. .*");

    private ObjectMapper objectMapper;
    private CategoryMapper categoryMapper;

    public ItauTransactionParser() {
        this(null, null, null);
    }

    public ItauTransactionParser(CategoryMappingRepository categoryMappingRepository) {
        this(null, null, categoryMappingRepository);
    }

    public ItauTransactionParser(ObjectMapper objectMapper, CategoryMapper categoryMapper, CategoryMappingRepository categoryMappingRepository) {
        if (objectMapper == null) {
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        } else {
            this.objectMapper = objectMapper;
        }

        if (categoryMapper == null)
            this.categoryMapper = new CategoryMapper(categoryMappingRepository);
        else
            this.categoryMapper = categoryMapper;
    }

    public Transaction parseFromSms(SMSNotification sms) {
        ExtractedTransaction et = extractData(sms.getMessage());

        return Transaction.builder()
                .date(sms.getDate())
                .value(et.getValue())
                .description(et.getShop())
                .category(categoryMapper.map(et.getShop()))
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

        if (message.startsWith("Itau Uniclass:") && message.contains("COMPRA APROVADA no cartao")) {
            et = extractDataCard(message);
        } else if (message.startsWith("Itau Uniclass") && message.contains("Saque de R$")) {
            et = extractDataWithdraw(message);
        } else if (message.startsWith("O pagamento de TITULOS agendado")) {
            et = extractDataScheduled(message);
        } else if (message.startsWith("Realizado TED no valor de") || message.startsWith("Realizado DOC no valor de")) {
            et = extractDataTedDoc(message);
        }

        if (et == null)
            throw new IllegalArgumentException("Message not recognized! '" + message + "'");
        else
            return et;
    }

    private ExtractedTransaction extractDataCard(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);

        String purchaseMessage = message.trim();
        Matcher valueAndShopMatcher = REGEX_ITAU_TRANSACTION_VALUE_AND_SHOP.matcher(purchaseMessage);
        if (valueAndShopMatcher.find()) {
            // Value
            et.setValue(parseValue(valueAndShopMatcher.group(1)));

            // Shop
            et.setShop(valueAndShopMatcher.group(2).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataWithdraw(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);

        String purchaseMessage = message.trim();
        Matcher valueAndShopMatcher = REGEX_ITAU_WITHDRAW.matcher(purchaseMessage);
        if (valueAndShopMatcher.find()) {
            // Value
            et.setValue(parseValue(valueAndShopMatcher.group(1)));

            // Shop
            et.setShop("Saque " + valueAndShopMatcher.group(2).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataScheduled(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);
        et.setShop("Agendada");

        String purchaseMessage = message.trim();
        Matcher valueMatcher = REGEX_ITAU_SCHEDULED.matcher(purchaseMessage);
        if (valueMatcher.find()) {
            // Value
            et.setValue(parseValue(valueMatcher.group(1)));
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private ExtractedTransaction extractDataTedDoc(String message) {
        ExtractedTransaction et = new ExtractedTransaction();
        et.setType(Transaction.TYPE_EXPENSE);

        String purchaseMessage = message.trim();
        Matcher valueAndShopMatcher = REGEX_ITAU_TED_DOC.matcher(purchaseMessage);
        if (valueAndShopMatcher.find()) {
            // Value
            et.setValue(parseValue(valueAndShopMatcher.group(2)));

            // Shop
            et.setShop(valueAndShopMatcher.group(1).trim());
        } else {
            throw new SMSNotParsedException("Couldn't extract value and shop from the message '" + message + "'");
        }

        return et;
    }

    private BigDecimal parseValue(String valueStr) {
        return new BigDecimal(valueStr.replace(".", "").replace(",", ".")).setScale(2);
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

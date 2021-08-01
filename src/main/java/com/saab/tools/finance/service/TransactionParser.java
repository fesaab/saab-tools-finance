package com.saab.tools.finance.service;

import com.saab.tools.finance.model.entity.SMSNotification;
import com.saab.tools.finance.model.entity.Transaction;

public interface TransactionParser {

    /**
     * Parse a SMS into a Transaction.
     *
     * @param sms
     * @return
     */
    Transaction parseFromSms(SMSNotification sms);

}

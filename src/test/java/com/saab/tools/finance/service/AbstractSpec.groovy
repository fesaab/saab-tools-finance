package com.saab.tools.finance.service

import com.saab.tools.finance.model.entity.SMSNotification
import spock.lang.Specification

import java.time.LocalDateTime

abstract class AbstractSpec extends Specification {

    protected SMSNotification buildSmsNotification(String message, LocalDateTime date) {
        return SMSNotification.builder()
                .date(date)
                .message(message)
                .build();
    }

}

package com.saab.tools.finance.model.converter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimestampLocalDateTimeConverter implements DynamoDBTypeConverter<Long, LocalDateTime> {

    @Override
    public Long convert(final LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public LocalDateTime unconvert(final Long longValue) {
        return Instant.ofEpochMilli(longValue).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}

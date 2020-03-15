package com.saab.tools.finance.model.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class LocalDateTimeJsonDeserializer extends StdDeserializer<LocalDateTime> {

    protected LocalDateTimeJsonDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Instant.ofEpochMilli(p.readValueAs(Long.class)).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}

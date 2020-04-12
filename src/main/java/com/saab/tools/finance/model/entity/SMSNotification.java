package com.saab.tools.finance.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.saab.tools.finance.model.converter.LocalDateTimeJsonDeserializer;
import lombok.*;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class SMSNotification {

    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    private LocalDateTime date;
    private String number;
    private String message;
    private String id;

    @JsonIgnore
    private String rawMessage;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.DEFAULT_STYLE, true, true);
    }
}

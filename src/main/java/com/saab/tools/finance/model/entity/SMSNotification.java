package com.saab.tools.finance.model.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.*;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**

   {
      "date": 1584212228295,
      "number": "072893933733",
      "message": "text of the sms message..."
   }

 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SMSNotification {

    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDateTime date;
    private String number;
    private String message;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.DEFAULT_STYLE, true, true);
    }
}

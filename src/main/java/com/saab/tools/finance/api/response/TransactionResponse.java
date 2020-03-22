package com.saab.tools.finance.api.response;

import com.saab.tools.finance.model.entity.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZoneId;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionResponse {

    private String id;
    private long date;
    private String category;
    private String description;
    private BigDecimal value;
    private String type; // EXPENSE or INCOME

    public static TransactionResponse mapFromEntity(Transaction orm) {
        if (orm == null)
            return null;

        return TransactionResponse.builder()
                .id(orm.getId())
                .date(orm.getDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .category(orm.getCategory())
                .description(orm.getDescription())
                .type(orm.getType())
                .value(orm.getValue())
                .build();
    }

}

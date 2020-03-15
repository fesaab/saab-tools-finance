package com.saab.tools.finance.model.mapper;

import com.saab.tools.finance.api.response.TransactionResponse;
import com.saab.tools.finance.model.entity.Transaction;

import java.time.ZoneId;

public class TransactionResponseMapper {

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

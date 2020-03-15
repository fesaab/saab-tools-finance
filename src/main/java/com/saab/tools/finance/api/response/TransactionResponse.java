package com.saab.tools.finance.api.response;

import lombok.*;

import java.math.BigDecimal;

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

}

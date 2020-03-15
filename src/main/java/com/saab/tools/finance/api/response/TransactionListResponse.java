package com.saab.tools.finance.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class TransactionListResponse {

    @Getter @Setter
    private List<TransactionResponse> transactionList;

    public TransactionListResponse() {
        this.transactionList = new ArrayList<>();
    }

}

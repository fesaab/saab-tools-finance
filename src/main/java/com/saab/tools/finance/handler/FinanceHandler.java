package com.saab.tools.finance.handler;

import com.saab.tools.finance.api.ResponseHandler;
import com.saab.tools.finance.api.response.TransactionListResponse;
import com.saab.tools.finance.api.response.TransactionResponse;
import com.saab.tools.finance.model.entity.Transaction;
import com.saab.tools.finance.model.mapper.TransactionResponseMapper;
import com.saab.tools.finance.model.repository.TransactionRepository;

import java.util.List;
import java.util.stream.Collectors;

public class FinanceHandler {

    private TransactionRepository repository;

    public FinanceHandler() {
        this.repository = new TransactionRepository();
    }

    public Object listTransactions() {
        try {
            List<Transaction> transactions = this.repository.findAll();
            TransactionListResponse response = new TransactionListResponse();
            response.setTransactionList(transactions.stream()
                    .map(TransactionResponseMapper::mapFromEntity)
                    .collect(Collectors.toList()));
            return ResponseHandler.getInstance().buildGatewayResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseHandler.getInstance().buildGatewayResponse(e);
        }
    }

}

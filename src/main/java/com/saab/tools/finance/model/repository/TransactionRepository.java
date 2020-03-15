package com.saab.tools.finance.model.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.saab.tools.finance.model.entity.Transaction;

import java.util.List;

public class TransactionRepository {

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private DynamoDBMapper dao;

    public TransactionRepository() {
        this.dao = new DynamoDBMapper(client);
    }

    public List<Transaction> findAll() {
        List<Transaction> result = this.dao.scan(Transaction.class, new DynamoDBScanExpression());
        return result;
    }

    public void insert(Transaction t) {
        this.dao.save(t);
    }

}

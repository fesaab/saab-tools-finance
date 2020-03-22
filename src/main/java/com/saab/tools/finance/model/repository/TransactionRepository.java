package com.saab.tools.finance.model.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.saab.tools.finance.model.entity.Transaction;
import sun.font.AttributeMap;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void save(Transaction t) {
        this.dao.save(t);
    }

    public List<Transaction> findByDescriptionAndValue(String description, BigDecimal value) {

        Map<String, AttributeValue> scanAttributeValues = new HashMap<String, AttributeValue>();
        scanAttributeValues.put(":tDesc", new AttributeValue().withS(description));
        scanAttributeValues.put(":tValue", new AttributeValue().withN(value.toString()));

        Map<String, String> scanAttributeNames = new HashMap<String, String>();
        scanAttributeNames.put("#tDesc", "description");
        scanAttributeNames.put("#tValue", "value");

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#tDesc = :tDesc and #tValue = :tValue")
                .withExpressionAttributeValues(scanAttributeValues)
                .withExpressionAttributeNames(scanAttributeNames);

        List<Transaction> result = this.dao.scan(Transaction.class, scanExpression);

        // Sort the results by date ASC
        result.stream().sorted(new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });

        return result;
    }

}

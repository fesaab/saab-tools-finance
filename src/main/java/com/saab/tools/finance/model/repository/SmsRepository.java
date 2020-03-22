package com.saab.tools.finance.model.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.saab.tools.finance.model.entity.SMSNotification;

public class SmsRepository {

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private DynamoDBMapper dao;

    public SmsRepository() {
        this.dao = new DynamoDBMapper(client);
    }

    public void save(SMSNotification t) {
        this.dao.save(t);
    }

}

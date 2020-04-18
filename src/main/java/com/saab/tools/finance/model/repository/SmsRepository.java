package com.saab.tools.finance.model.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.saab.tools.finance.model.entity.Sms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsRepository {

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private DynamoDBMapper dao;

    public SmsRepository(String tableName) {
        //https://blog.jayway.com/2013/10/09/dynamic-table-name-in-dynamodb-with-java-dynamomapper/
        this.dao = new DynamoDBMapper(client, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName)));
    }

    public void save(Sms s) { this.dao.save(s); }

    public Sms findById(String id) {

        Map<String, AttributeValue> queryAttributeValues = new HashMap<String, AttributeValue>();
        queryAttributeValues.put(":tId", new AttributeValue().withS(id));

        DynamoDBQueryExpression<Sms> queryExpression = new DynamoDBQueryExpression<Sms>()
                .withKeyConditionExpression("id = :tId")
                .withExpressionAttributeValues(queryAttributeValues);

        List<Sms> result = this.dao.query(Sms.class, queryExpression);

        return result.stream().findFirst().orElseThrow(() -> new RuntimeException("Wasn't found SMS with ID " + id));
    }

}

package com.saab.tools.finance.model.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.saab.tools.finance.model.entity.CategoryMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryMappingRepository {
    private DynamoDBMapper dao;

    // Used for test purposes
    protected CategoryMappingRepository() {
    }

    public CategoryMappingRepository(String tableName) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();

        //https://blog.jayway.com/2013/10/09/dynamic-table-name-in-dynamodb-with-java-dynamomapper/
        this.dao = new DynamoDBMapper(client, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName)));
    }

    public CategoryMapping query(String description) {

        Map<String, AttributeValue> queryAttributeValues = new HashMap<String, AttributeValue>();
        queryAttributeValues.put(":description", new AttributeValue().withS(description));

        Map<String, String> queryAttributeNames = new HashMap<String, String>();
        queryAttributeNames.put("#description", "description");

        DynamoDBQueryExpression<CategoryMapping> queryExpression = new DynamoDBQueryExpression<CategoryMapping>()
                .withKeyConditionExpression("#description = :description")
                .withExpressionAttributeNames(queryAttributeNames)
                .withExpressionAttributeValues(queryAttributeValues);

        List<CategoryMapping> result = this.dao.query(CategoryMapping.class, queryExpression);

        return result.stream().findFirst().orElse(null);
    }

    public List<CategoryMapping> getRegexList() {

        Map<String, AttributeValue> scanAttributeValues = new HashMap<String, AttributeValue>();
        scanAttributeValues.put(":regex", new AttributeValue().withBOOL(true));

        Map<String, String> scanAttributeNames = new HashMap<String, String>();
        scanAttributeNames.put("#regex", "regex");

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#regex = :regex")
                .withExpressionAttributeValues(scanAttributeValues)
                .withExpressionAttributeNames(scanAttributeNames);

        List<CategoryMapping> result = this.dao.scan(CategoryMapping.class, scanExpression);
        return result;
    }

}

package com.saab.tools.finance.model.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@DynamoDBTable(tableName="REPLACED_BY_ENVIRONMENT_VARIABLE")
@ToString
public class CategoryMapping {

    @DynamoDBHashKey(attributeName="description")
    private String description;

    @DynamoDBAttribute(attributeName="category")
    private String category;

    @DynamoDBAttribute(attributeName="regex")
    private Boolean regex;

}

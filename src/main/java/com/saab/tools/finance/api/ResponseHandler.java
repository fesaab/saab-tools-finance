package com.saab.tools.finance.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saab.tools.finance.api.response.ErrorResponse;
import com.saab.tools.finance.api.response.GatewayResponse;

import java.util.HashMap;
import java.util.Map;

public class ResponseHandler {

    private static ResponseHandler instance;
    private Map<String, String> defaultHeaders;
    private ObjectMapper defaultObjectMapper;

    private ResponseHandler() {
        defaultHeaders = new HashMap<>();
        defaultHeaders.put("Content-Type", "application/json; charset=utf-8");
        defaultHeaders.put("X-Custom-Header", "application/json");

        defaultObjectMapper = new ObjectMapper();
    }

    public static ResponseHandler getInstance() {
        if (instance == null) {
            instance = new ResponseHandler();
        }
        return instance;
    }

    public Object buildGatewayResponse(Object jsonObject) throws JsonProcessingException {
        return buildGatewayResponse(jsonObject, defaultHeaders, 200);
    }

    public Object buildGatewayResponse(Object jsonObject, int statusCode) throws JsonProcessingException {
        return buildGatewayResponse(jsonObject, defaultHeaders, statusCode);
    }

    public Object buildGatewayResponse(Object jsonObject, Map<String, String> headers) throws JsonProcessingException {
        return buildGatewayResponse(jsonObject, headers, 200);
    }

    public Object buildGatewayResponse(Object jsonObject, Map<String, String> headers, int statusCode) throws JsonProcessingException {
        String json = defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        return new GatewayResponse(json, headers, statusCode);
    }

    public Object buildGatewayResponse(Exception e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        try {
            return buildGatewayResponse(errorResponse, 500);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            return new RuntimeException("Error building the error response!", ex);
        }
    }

}

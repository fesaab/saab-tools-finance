package com.saab.tools.finance.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saab.tools.finance.api.response.GatewayResponse;
import com.saab.tools.finance.api.response.TransactionListResponse;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class FinanceHandlerTest {

    @Ignore
    @Test
    public void test_list_all() {
        FinanceHandler handler = new FinanceHandler();
        GatewayResponse result = (GatewayResponse)handler.listTransactions();
        assertEquals(result.getStatusCode(), 200);
        assertEquals(result.getHeaders().get("Content-Type"), "application/json; charset=utf-8");

        String content = result.getBody();
        assertNotNull(content);
        ObjectMapper mapper = new ObjectMapper();
        try {
            TransactionListResponse response = mapper.readValue(content, TransactionListResponse.class);
            assertNotNull(response);
            assertNotNull(response.getTransactionList());
            assertEquals(response.getTransactionList().size(), 2);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void test() {
        LocalDateTime date = LocalDateTime.now();
        System.out.println(date.toString());
    }
}

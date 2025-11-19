package com.hsbc.iwpb.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MoneyTransferRequestTest {
    @Test
    void testAllArgsConstructorAndGetters() {
        MoneyTransferRequest req = new MoneyTransferRequest(1L, 2L, 300L);
        assertEquals(1L, req.sourceAccountNumber());
        assertEquals(2L, req.destinationAccountNumber());
        assertEquals(300L, req.amount());
    }
    @Test
    void testEqualsAndHashCode() {
        MoneyTransferRequest r1 = new MoneyTransferRequest(1L, 2L, 300L);
        MoneyTransferRequest r2 = new MoneyTransferRequest(1L, 2L, 300L);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
    @Test
    void testToString() {
        MoneyTransferRequest req = new MoneyTransferRequest(1L, 2L, 300L);
        assertTrue(req.toString().contains("sourceAccountNumber=1"));
    }
}

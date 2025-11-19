package com.hsbc.iwpb.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class DepositOrWithdrawHistoryTest {
    @Test
    void testSettersAndGetters() {
        DepositOrWithdrawHistory h = new DepositOrWithdrawHistory();
        h.setAccountNumber(2L);
        h.setAmount(100L);
        h.setCreatedAt(LocalDateTime.now());
        h.setTransactionId(123L);
        assertEquals(2L, h.getAccountNumber());
        assertEquals(100L, h.getAmount());
        assertNotNull(h.getCreatedAt());
        assertEquals(123L, h.getTransactionId());
    }
    @Test
    void testEqualsAndHashCode() {
        DepositOrWithdrawHistory h1 = new DepositOrWithdrawHistory();
        h1.setAccountNumber(2L);
        h1.setAmount(100L);
        h1.setTransactionId(123L);
        DepositOrWithdrawHistory h2 = new DepositOrWithdrawHistory();
        h2.setAccountNumber(2L);
        h2.setAmount(100L);
        h2.setTransactionId(123L);
        assertNotEquals(h1, null);
        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
    @Test
    void testToString() {
        DepositOrWithdrawHistory h = new DepositOrWithdrawHistory();
        h.setAccountNumber(2L);
        h.setTransactionId(123L);
        assertTrue(h.toString().contains("accountNumber=2") || h.toString().contains("transactionId=123"));
    }
}
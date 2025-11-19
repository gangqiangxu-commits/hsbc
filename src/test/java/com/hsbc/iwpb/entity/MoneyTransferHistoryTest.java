package com.hsbc.iwpb.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class MoneyTransferHistoryTest {
    @Test
    void testSettersAndGetters() {
        MoneyTransferHistory h = new MoneyTransferHistory();
        h.setTransactionId(2L);
        h.setSourceAccountNumber(3L);
        h.setDestinationAccountNumber(4L);
        h.setAmount(500L);
        h.setCreatedAt(LocalDateTime.now());
        assertEquals(2L, h.getTransactionId());
        assertEquals(3L, h.getSourceAccountNumber());
        assertEquals(4L, h.getDestinationAccountNumber());
        assertEquals(500L, h.getAmount());
        assertNotNull(h.getCreatedAt());
    }
    @Test
    void testEqualsAndHashCode() {
        MoneyTransferHistory h1 = new MoneyTransferHistory();
        h1.setTransactionId(2L);
        h1.setSourceAccountNumber(3L);
        h1.setDestinationAccountNumber(4L);
        h1.setAmount(500L);
        MoneyTransferHistory h2 = new MoneyTransferHistory();
        h2.setTransactionId(2L);
        h2.setSourceAccountNumber(3L);
        h2.setDestinationAccountNumber(4L);
        h2.setAmount(500L);
        assertNotEquals(h1, null);
        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
    @Test
    void testToString() {
        MoneyTransferHistory h = new MoneyTransferHistory();
        h.setTransactionId(2L);
        h.setSourceAccountNumber(3L);
        h.setDestinationAccountNumber(4L);
        assertTrue(h.toString().contains("transactionId=2") || h.toString().contains("sourceAccountNumber=3") || h.toString().contains("destinationAccountNumber=4"));
    }
}
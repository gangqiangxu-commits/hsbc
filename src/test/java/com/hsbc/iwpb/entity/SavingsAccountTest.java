package com.hsbc.iwpb.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class SavingsAccountTest {
    @Test
    void testSettersAndGetters() {
        SavingsAccount acc = new SavingsAccount();
        acc.setAccountNumber(1L);
        acc.setName("Bob");
        acc.setPersonalId(123L);
        acc.setBalance(1000L);
        LocalDateTime now = LocalDateTime.now();
        acc.setCreatedAt(now);
        acc.setLastUpdated(now);
        assertEquals(1L, acc.getAccountNumber());
        assertEquals("Bob", acc.getName());
        assertEquals(123L, acc.getPersonalId());
        assertEquals(1000L, acc.getBalance());
        assertEquals(now, acc.getCreatedAt());
        assertEquals(now, acc.getLastUpdated());
    }
    @Test
    void testEqualsAndHashCode() {
        SavingsAccount a1 = new SavingsAccount();
        a1.setAccountNumber(1L);
        SavingsAccount a2 = new SavingsAccount();
        a2.setAccountNumber(1L);
        assertNotEquals(a1, null);
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }
    @Test
    void testToString() {
        SavingsAccount acc = new SavingsAccount();
        acc.setAccountNumber(1L);
        assertTrue(acc.toString().contains("accountNumber=1"));
    }
}

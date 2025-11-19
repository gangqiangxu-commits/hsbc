package com.hsbc.iwpb.dto;

import com.hsbc.iwpb.entity.SavingsAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class MoneyTransferResponseTest {
    @Test
    void testAllArgsConstructorAndGetters() {
        SavingsAccount account = mock(SavingsAccount.class);
        MoneyTransferResponse resp = new MoneyTransferResponse(account, true, "ok");
        assertTrue(resp.success());
        assertEquals(account, resp.sourceAccount());
        assertEquals("ok", resp.errorMessage());
    }

    @Test
    void testEqualsAndHashCode() {
        SavingsAccount account = mock(SavingsAccount.class);
        MoneyTransferResponse r1 = new MoneyTransferResponse(account, true, "ok");
        MoneyTransferResponse r2 = new MoneyTransferResponse(account, true, "ok");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testToString() {
        MoneyTransferResponse resp = new MoneyTransferResponse(null, true, "ok");
        assertTrue(resp.toString().contains("success=true"));
    }
}
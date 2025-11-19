package com.hsbc.iwpb.dto;

import com.hsbc.iwpb.entity.SavingsAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DepositWithdrawResponseTest {
    @Test
    void testAllArgsConstructorAndGetters() {
        SavingsAccount account = mock(SavingsAccount.class);
        DepositWithdrawResponse resp = new DepositWithdrawResponse(account, true, "msg");
        assertTrue(resp.success());
        assertEquals("msg", resp.errorMessage());
        assertEquals(account, resp.account());
    }
    @Test
    void testEqualsAndHashCode() {
        SavingsAccount account = mock(SavingsAccount.class);
        DepositWithdrawResponse r1 = new DepositWithdrawResponse(account, true, "msg");
        DepositWithdrawResponse r2 = new DepositWithdrawResponse(account, true, "msg");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
    @Test
    void testToString() {
        DepositWithdrawResponse resp = new DepositWithdrawResponse(null, true, "msg");
        assertTrue(resp.toString().contains("success=true"));
    }
}
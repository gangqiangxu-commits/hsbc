package com.hsbc.iwpb.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DepositWithdrawRequestTest {
    @Test
    void testAllArgsConstructorAndGetters() {
        DepositWithdrawRequest req = new DepositWithdrawRequest(1L, 100L);
        assertEquals(1L, req.accountNumber());
        assertEquals(100L, req.amount());
    }
    @Test
    void testEqualsAndHashCode() {
        DepositWithdrawRequest req1 = new DepositWithdrawRequest(1L, 100L);
        DepositWithdrawRequest req2 = new DepositWithdrawRequest(1L, 100L);
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
    }
    @Test
    void testToString() {
        DepositWithdrawRequest req = new DepositWithdrawRequest(1L, 100L);
        assertTrue(req.toString().contains("accountNumber=1"));
    }
}

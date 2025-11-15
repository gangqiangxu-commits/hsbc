package com.hsbc.iwpb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SavingsAccountUtilTest {
    @Test
    void isAccountValid_returnsTrueForPositiveNumbers() {
        assertTrue(SavingsAccountUtil.isAccountValid(1L));
        assertTrue(SavingsAccountUtil.isAccountValid(123456789L));
    }

    @Test
    void isAccountValid_returnsFalseForZeroOrNegative() {
        assertFalse(SavingsAccountUtil.isAccountValid(0L));
        assertFalse(SavingsAccountUtil.isAccountValid(-1L));
        assertFalse(SavingsAccountUtil.isAccountValid(-99999L));
    }

    @Test
    void isTransactionAmountValid_returnsTrueForPositiveAmounts() {
        assertTrue(SavingsAccountUtil.isTransactionAmountValid(1L));
        assertTrue(SavingsAccountUtil.isTransactionAmountValid(10000L));
    }

    @Test
    void isTransactionAmountValid_returnsFalseForZeroOrNegative() {
        assertFalse(SavingsAccountUtil.isTransactionAmountValid(0L));
        assertFalse(SavingsAccountUtil.isTransactionAmountValid(-1L));
        assertFalse(SavingsAccountUtil.isTransactionAmountValid(-100L));
    }
}

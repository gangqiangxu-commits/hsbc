package com.hsbc.iwpb.util;

import com.hsbc.iwpb.dto.MoneyTransferRequest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

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

    @Test
    void parseBatchMoneyTransferFile_parsesValidFile() throws Exception {
        String content = "1001 2001 500\n1002 2002 1000\n";
        BufferedReader reader = new BufferedReader(new StringReader(content));
        List<MoneyTransferRequest> result = SavingsAccountUtil.parseBatchMoneyTransferFile(reader);
        assertEquals(2, result.size());
        assertEquals(1001L, result.get(0).sourceAccountNumber());
        assertEquals(2001L, result.get(0).destinationAccountNumber());
        assertEquals(500, result.get(0).amount());
        assertEquals(1002L, result.get(1).sourceAccountNumber());
        assertEquals(2002L, result.get(1).destinationAccountNumber());
        assertEquals(1000, result.get(1).amount());
    }

    @Test
    void parseBatchMoneyTransferFile_throwsOnInvalidColumnCount() {
        String content = "1001 2001\n1002 2002 1000\n";
        BufferedReader reader = new BufferedReader(new StringReader(content));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            SavingsAccountUtil.parseBatchMoneyTransferFile(reader);
        });
        assertTrue(ex.getMessage().contains("Row 1: Invalid format"));
    }

    @Test
    void parseBatchMoneyTransferFile_throwsOnInvalidNumberFormat() {
        String content = "1001 2001 abc\n";
        BufferedReader reader = new BufferedReader(new StringReader(content));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            SavingsAccountUtil.parseBatchMoneyTransferFile(reader);
        });
        assertTrue(ex.getMessage().contains("Row 1"));
        assertTrue(ex.getMessage().toLowerCase().contains("numberformat"));
    }
}
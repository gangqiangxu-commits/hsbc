package com.hsbc.iwpb.controller;

import com.hsbc.iwpb.dto.*;
import com.hsbc.iwpb.entity.*;
import com.hsbc.iwpb.service.SavingsAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SavingsAccountControllerTest {
    @Mock
    private SavingsAccountService savingsAccountService;

    @InjectMocks
    private SavingsAccountController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHello() {
        ResponseEntity<String> response = controller.hello();
        assertEquals("Hello, World!", response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testProcessTransactionJson_success() {
        MoneyTransferRequest req = new MoneyTransferRequest(1L, 2L, 1000L);
        SavingsAccount sa = new SavingsAccount(1L, "Alice", 123L, 2000L, LocalDateTime.now(), LocalDateTime.now());
        when(savingsAccountService.processMoneyTransfer(req)).thenReturn(sa);
        ResponseEntity<MoneyTransferResponse> response = controller.processTransactionJson(req);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(sa, response.getBody().sourceAccount());
        assertEquals("", response.getBody().errorMessage());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testProcessTransactionJson_failure() {
        MoneyTransferRequest req = new MoneyTransferRequest(1L, 2L, 1000L);
        when(savingsAccountService.processMoneyTransfer(req)).thenThrow(new RuntimeException("fail"));
        ResponseEntity<MoneyTransferResponse> response = controller.processTransactionJson(req);
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().sourceAccount());
        assertEquals("fail", response.getBody().errorMessage());
    }

    @Test
    void testOpenAccount() {
        OpenAccountRequest req = new OpenAccountRequest("Bob", 456L);
        SavingsAccount sa = new SavingsAccount(2L, "Bob", 456L, 5000L, LocalDateTime.now(), LocalDateTime.now());
        when(savingsAccountService.createAccount("Bob", 456L)).thenReturn(sa);
        ResponseEntity<SavingsAccount> response = controller.openAccount(req);
        assertNotNull(response.getBody());
        assertEquals(sa, response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testBatchOpenAccounts_defaultValues() {
        SavingsAccount sa = new SavingsAccount(3L, "Mocked User1000", 200000L, 10000L, LocalDateTime.now(), LocalDateTime.now());
        when(savingsAccountService.createAccount(anyString(), anyLong())).thenReturn(sa);
        when(savingsAccountService.depositOrWithdraw(anyLong(), anyLong())).thenReturn(sa);
        when(savingsAccountService.getAccount(anyLong())).thenReturn(sa);
        ResponseEntity<List<SavingsAccount>> response = controller.batchOpenAccounts(-1, -1);
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        assertEquals(10000, response.getBody().size());
        for (SavingsAccount account : response.getBody()) {
            assertNotNull(account);
        }
    }

    @Test
    void testDepositWithdraw_success() {
        DepositWithdrawRequest req = new DepositWithdrawRequest(1L, 1000L);
        SavingsAccount sa = new SavingsAccount(1L, "Alice", 123L, 3000L, LocalDateTime.now(), LocalDateTime.now());
        when(savingsAccountService.depositOrWithdraw(1L, 1000L)).thenReturn(sa);
        ResponseEntity<DepositWithdrawResponse> response = controller.depositWithdraw(req);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(sa, response.getBody().account());
        assertEquals("", response.getBody().errorMessage());
    }

    @Test
    void testDepositWithdraw_failure() {
        DepositWithdrawRequest req = new DepositWithdrawRequest(1L, -1000L);
        when(savingsAccountService.depositOrWithdraw(1L, -1000L)).thenThrow(new RuntimeException("Insufficient funds"));
        ResponseEntity<DepositWithdrawResponse> response = controller.depositWithdraw(req);
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().account());
        assertEquals("Insufficient funds", response.getBody().errorMessage());
    }

    @Test
    void testListAccounts() {
        SavingsAccount sa = new SavingsAccount(1L, "Alice", 123L, 2000L, LocalDateTime.now(), LocalDateTime.now());
        when(savingsAccountService.listAccounts()).thenReturn(Collections.singletonList(sa));
        ResponseEntity<List<SavingsAccount>> response = controller.listAccounts();
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(sa, response.getBody().get(0));
    }

    @Test
    void testListDepositOrWithdrawHistory() {
        DepositOrWithdrawHistory h = new DepositOrWithdrawHistory(1L, 1000L, LocalDateTime.now(), 10L);
        when(savingsAccountService.listDepositOrWithdrawHistory(1L)).thenReturn(Collections.singletonList(h));
        ResponseEntity<List<DepositOrWithdrawHistory>> response = controller.listDepositOrWithdrawHistory(1L);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(h, response.getBody().get(0));
    }

    @Test
    void testFindByTransactionId() {
        MoneyTransferHistory h = new MoneyTransferHistory(10L, 1L, 2L, 1000L, LocalDateTime.now());
        when(savingsAccountService.findByTransactionId(10L)).thenReturn(h);
        ResponseEntity<MoneyTransferHistory> response = controller.findByTransactionId(10L);
        assertNotNull(response.getBody());
        assertEquals(h, response.getBody());
    }

    @Test
    void testFindBySourceAccountNumber() {
        MoneyTransferHistory h = new MoneyTransferHistory(10L, 1L, 2L, 1000L, LocalDateTime.now());
        when(savingsAccountService.findBySourceAccountNumber(1L)).thenReturn(Collections.singletonList(h));
        ResponseEntity<List<MoneyTransferHistory>> response = controller.findBySourceAccountNumber(1L);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(h, response.getBody().get(0));
    }

    @Test
    void testFindByDestinationAccountNumber() {
        MoneyTransferHistory h = new MoneyTransferHistory(10L, 1L, 2L, 1000L, LocalDateTime.now());
        when(savingsAccountService.findByDestinationAccountNumber(2L)).thenReturn(Collections.singletonList(h));
        ResponseEntity<List<MoneyTransferHistory>> response = controller.findByDestinationAccountNumber(2L);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(h, response.getBody().get(0));
    }

    @Test
    void testSearchBySourceAndDestination() {
        MoneyTransferHistory h = new MoneyTransferHistory(10L, 1L, 2L, 1000L, LocalDateTime.now());
        when(savingsAccountService.findBySourceAndDestinationAccountNumber(1L, 2L)).thenReturn(Collections.singletonList(h));
        ResponseEntity<List<MoneyTransferHistory>> response = controller.searchBySourceAndDestination(1L, 2L);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(h, response.getBody().get(0));
    }
}
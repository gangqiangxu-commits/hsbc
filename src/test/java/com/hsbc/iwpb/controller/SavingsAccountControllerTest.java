package com.hsbc.iwpb.controller;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.dto.AccountBalanceTransanctionRequest;
import com.hsbc.iwpb.dto.DepositWithdrawRequest;
import com.hsbc.iwpb.dto.OpenAccountRequest;
import com.hsbc.iwpb.entity.MoneyTransfer;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.entity.service.SavingsAccountService;
import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SavingsAccountControllerTest {
    @Mock
    private SavingsAccountService savingsAccountService;
    @Mock
    private RedisService redisService;
    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void hello_returnsHelloWorld() {
        ResponseEntity<String> response = accountController.hello();
        assertEquals("Hello, World!", response.getBody());
    }

    @Test
    void openAccount_returnsSavingsAccount() {
        OpenAccountRequest req = new OpenAccountRequest("John", 12345L);
        SavingsAccount sa = mock(SavingsAccount.class);
        when(savingsAccountService.createAccount("John", 12345L)).thenReturn(sa);
        ResponseEntity<SavingsAccount> response = accountController.openAccount(req);
        assertEquals(sa, response.getBody());
    }

    @Test
    void depositWithdraw_returnsSavingsAccount() {
        DepositWithdrawRequest req = new DepositWithdrawRequest(1001L, 5000L); // 5000 cents = $50
        SavingsAccount sa = mock(SavingsAccount.class);
        when(savingsAccountService.depositOrWithdraw(1001L, 5000L)).thenReturn(sa);
        ResponseEntity<SavingsAccount> response = accountController.depositWithdraw(req);
        assertEquals(sa, response.getBody());
    }

    @Test
    void listAccounts_returnsAllAccounts() {
        SavingsAccount sa1 = mock(SavingsAccount.class);
        SavingsAccount sa2 = mock(SavingsAccount.class);
        List<SavingsAccount> accounts = Arrays.asList(sa1, sa2);
        when(savingsAccountService.listAccounts()).thenReturn(accounts);
        ResponseEntity<List<SavingsAccount>> response = accountController.listAccounts();
        assertEquals(accounts, response.getBody());
    }

    @Test
    void listDepositOrWithdrawHistory_returnsHistory() {
        DepositOrWithdrawHistory h1 = mock(DepositOrWithdrawHistory.class);
        DepositOrWithdrawHistory h2 = mock(DepositOrWithdrawHistory.class);
        List<DepositOrWithdrawHistory> history = Arrays.asList(h1, h2);
        when(savingsAccountService.listDepositOrWithdrawHistory(1001L)).thenReturn(history);
        ResponseEntity<List<DepositOrWithdrawHistory>> response = accountController.listDepositOrWithdrawHistory(1001L);
        assertEquals(history, response.getBody());
    }

    @Test
    void processTransactionJson_returnsTransaction() {
        AccountBalanceTransanctionRequest req = new AccountBalanceTransanctionRequest(1001L, 2002L, 1500L);
        when(redisService.nextTransactionId()).thenReturn(1L);
        ResponseEntity<MoneyTransfer> response = accountController.processTransactionJson(req);
        MoneyTransfer tx = response.getBody();
        assertNotNull(tx);
        assertEquals(1L, tx.transactionId());
        assertEquals(1001L, tx.sourceAccountNumber());
        assertEquals(2002L, tx.destinationAccountNumber());
        assertEquals(1500L, tx.amount());
    }
}
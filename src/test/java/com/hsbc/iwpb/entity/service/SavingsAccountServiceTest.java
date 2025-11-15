package com.hsbc.iwpb.entity.service;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.mapper.SavingsAccountMapper;
import com.hsbc.iwpb.mapper.DepositOrWithdrawHistoryMapper;
import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import com.hsbc.iwpb.entity.MoneyTransferHistory;
import com.hsbc.iwpb.entity.MoneyTransfer;
import com.hsbc.iwpb.mapper.MoneyTransferHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SavingsAccountServiceTest {
    private static final long VALID_ACCOUNT_NUMBER = 1001L;
    private static final long INVALID_ACCOUNT_NUMBER = 9999L;
    private static final long INITIAL_BALANCE = 1000L;
    private static final long DEPOSIT_AMOUNT = 500L;
    private static final long WITHDRAW_AMOUNT = -500L;
    private static final long INSUFFICIENT_WITHDRAW_AMOUNT = -200L;
    private static final long SMALL_BALANCE = 100L;
    private static final long TRANSACTION_ID = 1L;

    @Mock
    private SavingsAccountMapper savingsAccountMapper;
    @Mock
    private DepositOrWithdrawHistoryMapper userDepositOrWithdrawHistoryMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private MoneyTransferHistoryMapper transactionHistoryMapper;
    @InjectMocks
    private SavingsAccountService savingsAccountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private SavingsAccount mockAccountWithBalance(long balance) {
        SavingsAccount account = mock(SavingsAccount.class);
        when(account.getBalance()).thenReturn(balance);
        return account;
    }

    @Test
    void getAccount_returnsAccount() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        assertEquals(account, savingsAccountService.getAccount(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void depositOrWithdraw_depositSuccess() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        when(userDepositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class))).thenReturn(1);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        SavingsAccount result = savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT);
        assertEquals(account, result);
        verify(account).setBalance(INITIAL_BALANCE + DEPOSIT_AMOUNT);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
    }

    @Test
    void depositOrWithdraw_withdrawSuccess() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        when(userDepositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class))).thenReturn(1);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        SavingsAccount result = savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, WITHDRAW_AMOUNT);
        assertEquals(account, result);
        verify(account).setBalance(INITIAL_BALANCE + WITHDRAW_AMOUNT);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
    }

    @Test
    void depositOrWithdraw_accountNotFound_throws() {
        when(savingsAccountMapper.findByAccountNumber(INVALID_ACCOUNT_NUMBER)).thenReturn(null);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                savingsAccountService.depositOrWithdraw(INVALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void depositOrWithdraw_insufficientFunds_throws() {
        SavingsAccount account = mockAccountWithBalance(SMALL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, INSUFFICIENT_WITHDRAW_AMOUNT));
        assertTrue(ex.getMessage().contains("Insufficient funds"));
    }

    @Test
    void depositOrWithdraw_rollbackOnException() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        doThrow(new RuntimeException("Simulated DB error")).when(userDepositOrWithdrawHistoryMapper).insert(any(DepositOrWithdrawHistory.class));
        assertThrows(RuntimeException.class, () ->
                savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT));
        verify(account).setBalance(INITIAL_BALANCE + DEPOSIT_AMOUNT);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
        verify(userDepositOrWithdrawHistoryMapper).insert(any(DepositOrWithdrawHistory.class));
    }

    @Test
    void createAccount_createsAndReturnsAccount() {
        String name = "John Doe";
        long personalId = 12345L;
        SavingsAccount account = mock(SavingsAccount.class);
        when(account.getAccountNumber()).thenReturn(VALID_ACCOUNT_NUMBER);
        // Simulate insert sets account number
        doAnswer(invocation -> {
            ((SavingsAccount) invocation.getArgument(0)).setAccountNumber(VALID_ACCOUNT_NUMBER);
            return null;
        }).when(savingsAccountMapper).insert(any(SavingsAccount.class));
        SavingsAccount result = savingsAccountService.createAccount(name, personalId);
        assertEquals(name, result.getName());
        assertEquals(personalId, result.getPersonalId());
        verify(savingsAccountMapper).insert(any(SavingsAccount.class));
    }

    @Test
    void updateBalance_updatesBalanceSuccessfully() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        savingsAccountService.updateBalance(VALID_ACCOUNT_NUMBER, 2000L);
        verify(account).setBalance(2000L);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
    }

    @Test
    void updateBalance_accountNotFound_throws() {
        when(savingsAccountMapper.findByAccountNumber(INVALID_ACCOUNT_NUMBER)).thenReturn(null);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                savingsAccountService.updateBalance(INVALID_ACCOUNT_NUMBER, 2000L));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void updateBalance_negativeBalance_throws() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                savingsAccountService.updateBalance(VALID_ACCOUNT_NUMBER, -1L));
        assertTrue(ex.getMessage().contains("Balance cannot be negative"));
    }

    @Test
    void listAccounts_returnsAccounts() {
        List<SavingsAccount> accounts = List.of(mockAccountWithBalance(100L), mockAccountWithBalance(200L));
        when(savingsAccountMapper.listAll()).thenReturn(accounts);
        assertEquals(accounts, savingsAccountService.listAccounts());
    }

    @Test
    void listDepositOrWithdrawHistory_returnsHistory() {
        List<DepositOrWithdrawHistory> history = List.of(mock(DepositOrWithdrawHistory.class));
        when(userDepositOrWithdrawHistoryMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(history);
        assertEquals(history, savingsAccountService.listDepositOrWithdrawHistory(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void findByTransactionId_returnsHistory() {
        MoneyTransferHistory history = mock(MoneyTransferHistory.class);
        when(transactionHistoryMapper.findByTransactionId(TRANSACTION_ID)).thenReturn(history);
        assertEquals(history, savingsAccountService.findByTransactionId(TRANSACTION_ID));
    }

    @Test
    void findBySourceAccountNumber_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(transactionHistoryMapper.findBySourceAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(list);
        assertEquals(list, savingsAccountService.findBySourceAccountNumber(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void findByDestinationAccountNumber_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(transactionHistoryMapper.findByDestinationAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(list);
        assertEquals(list, savingsAccountService.findByDestinationAccountNumber(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void findBySourceAndDestinationAccountNumber_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(transactionHistoryMapper.findBySourceAndDestinationAccountNumber(VALID_ACCOUNT_NUMBER, 2002L)).thenReturn(list);
        assertEquals(list, savingsAccountService.findBySourceAndDestinationAccountNumber(VALID_ACCOUNT_NUMBER, 2002L));
    }

    @Test
    void listAllMoneyTransferHistories_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(transactionHistoryMapper.listAll()).thenReturn(list);
        assertEquals(list, savingsAccountService.listAllMoneyTransferHistories());
    }

    @Test
    void processTransaction_lockAcquisitionFailure_throws() {
        MoneyTransfer transfer = mock(MoneyTransfer.class);
        when(transfer.sourceAccountNumber()).thenReturn(VALID_ACCOUNT_NUMBER);
        when(transfer.destinationAccountNumber()).thenReturn(2002L);
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        // Simulate lock acquisition failure
        when(redisService.getLock(VALID_ACCOUNT_NUMBER)).thenReturn(null);
        // The retry logic may swallow the exception, so we need to ensure it propagates
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                savingsAccountService.processTransaction(transfer));
        assertTrue(ex.getMessage().contains("Could not acquire lock"));
    }
}
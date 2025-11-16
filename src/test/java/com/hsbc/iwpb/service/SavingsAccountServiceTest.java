package com.hsbc.iwpb.service;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.mapper.SavingsAccountMapper;
import com.hsbc.iwpb.mapper.DepositOrWithdrawHistoryMapper;
import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import com.hsbc.iwpb.entity.MoneyTransferHistory;
import com.hsbc.iwpb.mapper.MoneyTransferHistoryMapper;
import com.hsbc.iwpb.dto.MoneyTransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private DepositOrWithdrawHistoryMapper depositOrWithdrawHistoryMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private MoneyTransferHistoryMapper moneyTransferHistoryMapper;
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

    private Lock mockLock() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock(); // Ensure the lock is held by the current thread so unlock() is valid
        return lock;
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
        when(depositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class))).thenReturn(1);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        when(redisService.getLock(VALID_ACCOUNT_NUMBER)).thenReturn(mockLock());
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
        when(depositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class))).thenReturn(1);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        when(redisService.getLock(VALID_ACCOUNT_NUMBER)).thenReturn(mockLock());
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
        when(redisService.getLock(VALID_ACCOUNT_NUMBER)).thenReturn(mockLock());
        doThrow(new RuntimeException("Simulated DB error")).when(depositOrWithdrawHistoryMapper).insert(any(DepositOrWithdrawHistory.class));
        assertThrows(RuntimeException.class, () ->
                savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT));
        verify(account).setBalance(INITIAL_BALANCE + DEPOSIT_AMOUNT);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
        verify(depositOrWithdrawHistoryMapper).insert(any(DepositOrWithdrawHistory.class));
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
    void listAccounts_returnsAccounts() {
        List<SavingsAccount> accounts = List.of(mockAccountWithBalance(100L), mockAccountWithBalance(200L));
        when(savingsAccountMapper.listAll()).thenReturn(accounts);
        assertEquals(accounts, savingsAccountService.listAccounts());
    }

    @Test
    void listDepositOrWithdrawHistory_returnsHistory() {
        List<DepositOrWithdrawHistory> history = List.of(mock(DepositOrWithdrawHistory.class));
        when(depositOrWithdrawHistoryMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(history);
        assertEquals(history, savingsAccountService.listDepositOrWithdrawHistory(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void findByTransactionId_returnsHistory() {
        MoneyTransferHistory history = mock(MoneyTransferHistory.class);
        when(moneyTransferHistoryMapper.findByTransactionId(TRANSACTION_ID)).thenReturn(history);
        assertEquals(history, savingsAccountService.findByTransactionId(TRANSACTION_ID));
    }

    @Test
    void findBySourceAccountNumber_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(moneyTransferHistoryMapper.findBySourceAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(list);
        assertEquals(list, savingsAccountService.findBySourceAccountNumber(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void findByDestinationAccountNumber_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(moneyTransferHistoryMapper.findByDestinationAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(list);
        assertEquals(list, savingsAccountService.findByDestinationAccountNumber(VALID_ACCOUNT_NUMBER));
    }

    @Test
    void findBySourceAndDestinationAccountNumber_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(moneyTransferHistoryMapper.findBySourceAndDestinationAccountNumber(VALID_ACCOUNT_NUMBER, 2002L)).thenReturn(list);
        assertEquals(list, savingsAccountService.findBySourceAndDestinationAccountNumber(VALID_ACCOUNT_NUMBER, 2002L));
    }

    @Test
    void listAllMoneyTransferHistories_returnsList() {
        List<MoneyTransferHistory> list = List.of(mock(MoneyTransferHistory.class));
        when(moneyTransferHistoryMapper.listAll()).thenReturn(list);
        assertEquals(list, savingsAccountService.listAllMoneyTransferHistories());
    }

    @Test
    void processMoneyTransfer_success() {
        MoneyTransferRequest req = new MoneyTransferRequest(1001L, 2002L, 100L);
        SavingsAccount sa = mockAccountWithBalance(200L); // Sufficient funds
        SavingsAccount dest = mockAccountWithBalance(100L);
        when(savingsAccountMapper.findByAccountNumber(1001L)).thenReturn(sa);
        when(savingsAccountMapper.findByAccountNumber(2002L)).thenReturn(dest);
        when(redisService.getLock(anyLong())).thenReturn(mock(Lock.class));
        when(redisService.nextTransactionId()).thenReturn(1L);
        SavingsAccount result = savingsAccountService.processMoneyTransfer(req);
        assertNotNull(result);
    }

    @Test
    void processMoneyTransfer_successfulTransfer() {
        MoneyTransferRequest req = new MoneyTransferRequest(1001L, 2002L, 100L);
        SavingsAccount source = mockAccountWithBalance(200L);
        SavingsAccount dest = mockAccountWithBalance(100L);
        when(savingsAccountMapper.findByAccountNumber(1001L)).thenReturn(source);
        when(savingsAccountMapper.findByAccountNumber(2002L)).thenReturn(dest);
        when(redisService.getLock(anyLong())).thenReturn(mock(Lock.class));
        when(redisService.nextTransactionId()).thenReturn(1L);
        SavingsAccount result = savingsAccountService.processMoneyTransfer(req);
        assertNotNull(result);
        verify(savingsAccountMapper, atLeastOnce()).update(source);
        verify(savingsAccountMapper, atLeastOnce()).update(dest);
    }

    @Test
    void processMoneyTransfer_insufficientFunds_throws() {
        MoneyTransferRequest req = new MoneyTransferRequest(1001L, 2002L, 500L);
        SavingsAccount source = mockAccountWithBalance(100L);
        when(savingsAccountMapper.findByAccountNumber(1001L)).thenReturn(source);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        assertTrue(ex.getMessage().contains("Insufficient funds"));
    }

    @Test
    void processMoneyTransfer_sourceAccountNotFound_throws() {
        MoneyTransferRequest req = new MoneyTransferRequest(9999L, 2002L, 100L);
        when(savingsAccountMapper.findByAccountNumber(9999L)).thenReturn(null);
        assertThrows(NullPointerException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
    }

    @Test
    void processMoneyTransfer_destinationAccountNotFound_throws() {
        MoneyTransferRequest req = new MoneyTransferRequest(1001L, 8888L, 100L);
        SavingsAccount source = mockAccountWithBalance(200L);
        when(savingsAccountMapper.findByAccountNumber(1001L)).thenReturn(source);
        when(savingsAccountMapper.findByAccountNumber(8888L)).thenReturn(null);
        when(redisService.getLock(anyLong())).thenReturn(mock(Lock.class));
        when(redisService.nextTransactionId()).thenReturn(1L);
        assertThrows(NullPointerException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
    }

    @Test
    void processMoneyTransfer_lockAcquisitionFails_throws() {
        MoneyTransferRequest req = new MoneyTransferRequest(1001L, 2002L, 100L);
        SavingsAccount source = mockAccountWithBalance(200L);
        SavingsAccount dest = mockAccountWithBalance(100L);
        when(savingsAccountMapper.findByAccountNumber(1001L)).thenReturn(source);
        when(savingsAccountMapper.findByAccountNumber(2002L)).thenReturn(dest);
        when(redisService.getLock(1001L)).thenReturn(null); // Simulate lock failure
        assertThrows(IllegalStateException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
    }

    @Test
    void processMoneyTransfer_exceptionDuringTransfer_releasesLocks() {
        MoneyTransferRequest req = new MoneyTransferRequest(1001L, 2002L, 100L);
        SavingsAccount source = mockAccountWithBalance(200L);
        SavingsAccount dest = mockAccountWithBalance(100L);
        Lock lock1 = mock(Lock.class);
        Lock lock2 = mock(Lock.class);
        when(savingsAccountMapper.findByAccountNumber(1001L)).thenReturn(source);
        when(savingsAccountMapper.findByAccountNumber(2002L)).thenReturn(dest);
        when(redisService.getLock(1001L)).thenReturn(lock1);
        when(redisService.getLock(2002L)).thenReturn(lock2);
        when(redisService.nextTransactionId()).thenReturn(1L);
        doThrow(new RuntimeException("DB error")).when(moneyTransferHistoryMapper).insert(any(MoneyTransferHistory.class));
        assertThrows(RuntimeException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        verify(lock1).unlock();
        verify(lock2).unlock();
    }
}
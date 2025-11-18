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
import org.springframework.transaction.support.TransactionTemplate;

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
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private SavingsAccountService savingsAccountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        savingsAccountService = new SavingsAccountService(
            savingsAccountMapper,
            redisService,
            depositOrWithdrawHistoryMapper,
            moneyTransferHistoryMapper,
            transactionTemplate
        );
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
        // Simulate transactionTemplate.executeWithoutResult
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> consumer =
                (java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) invocation.getArgument(0);
            consumer.accept(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        SavingsAccount result = savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT);
        assertEquals(account, result);
        verify(account).setBalance(INITIAL_BALANCE + DEPOSIT_AMOUNT);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
        verify(depositOrWithdrawHistoryMapper).insert(any(DepositOrWithdrawHistory.class));
    }

    @Test
    void depositOrWithdraw_withdrawSuccess() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        when(depositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class))).thenReturn(1);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        when(redisService.getLock(VALID_ACCOUNT_NUMBER)).thenReturn(mockLock());
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> consumer =
                (java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) invocation.getArgument(0);
            consumer.accept(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        SavingsAccount result = savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, WITHDRAW_AMOUNT);
        assertEquals(account, result);
        verify(account).setBalance(INITIAL_BALANCE + WITHDRAW_AMOUNT);
        verify(account).setLastUpdated(any(LocalDateTime.class));
        verify(savingsAccountMapper).update(account);
        verify(depositOrWithdrawHistoryMapper).insert(any(DepositOrWithdrawHistory.class));
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
        // Simulate exception in depositOrWithdrawHistoryMapper.insert
        when(depositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class)))
            .thenThrow(new RuntimeException("DB error"));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> consumer =
                (java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) invocation.getArgument(0);
            consumer.accept(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        Exception ex = assertThrows(RuntimeException.class, () ->
                savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("error during money transfer processing"), "Actual exception message: " + ex.getMessage());
    }

    @Test
    void depositOrWithdraw_retriesOnTransientFailure() {
        SavingsAccount account = mockAccountWithBalance(INITIAL_BALANCE);
        when(savingsAccountMapper.findByAccountNumber(VALID_ACCOUNT_NUMBER)).thenReturn(account);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        when(redisService.getLock(VALID_ACCOUNT_NUMBER)).thenReturn(mockLock());
        // Fail first, succeed on retry
        final int[] callCount = {0};
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> consumer =
                (java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) invocation.getArgument(0);
            callCount[0]++;
            if (callCount[0] == 1) {
                throw new RuntimeException("transient error");
            }
            consumer.accept(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(depositOrWithdrawHistoryMapper.insert(any(DepositOrWithdrawHistory.class))).thenReturn(1);
        SavingsAccount result = savingsAccountService.depositOrWithdraw(VALID_ACCOUNT_NUMBER, DEPOSIT_AMOUNT);
        assertEquals(account, result);
        assertTrue(callCount[0] > 1, "Should have retried at least once");
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
    void generateMockTransactions_basicFunctionality() {
        // Prepare 5 accounts with increasing balances
        SavingsAccount acc1 = mock(SavingsAccount.class);
        when(acc1.getAccountNumber()).thenReturn(1L);
        when(acc1.getBalance()).thenReturn(1000L);
        SavingsAccount acc2 = mock(SavingsAccount.class);
        when(acc2.getAccountNumber()).thenReturn(2L);
        when(acc2.getBalance()).thenReturn(2000L);
        SavingsAccount acc3 = mock(SavingsAccount.class);
        when(acc3.getAccountNumber()).thenReturn(3L);
        when(acc3.getBalance()).thenReturn(3000L);
        SavingsAccount acc4 = mock(SavingsAccount.class);
        when(acc4.getAccountNumber()).thenReturn(4L);
        when(acc4.getBalance()).thenReturn(4000L);
        SavingsAccount acc5 = mock(SavingsAccount.class);
        when(acc5.getAccountNumber()).thenReturn(5L);
        when(acc5.getBalance()).thenReturn(5000L);
        List<SavingsAccount> accounts = List.of(acc1, acc2, acc3, acc4, acc5);
        when(savingsAccountMapper.listAll()).thenReturn(accounts);

        // Test with 3 source accounts, 2 destinations each
        List<MoneyTransferRequest> requests = savingsAccountService.generateMockTransactions(3, 2);
        assertEquals(6, requests.size()); // 3 sources * 2 destinations
        for (MoneyTransferRequest req : requests) {
            assertNotEquals(req.sourceAccountNumber(), req.destinationAccountNumber());
            assertTrue(accounts.stream().anyMatch(a -> a.getAccountNumber() == req.sourceAccountNumber()));
            assertTrue(accounts.stream().anyMatch(a -> a.getAccountNumber() == req.destinationAccountNumber()));
            // Amount should be balance/2 for the source
            long expectedAmount = accounts.stream()
                .filter(a -> a.getAccountNumber() == req.sourceAccountNumber())
                .findFirst().get().getBalance() / 2;
            assertEquals(expectedAmount, req.amount());
        }
    }

    @Test
    void generateMockTransactions_capsInputAndHandlesZeroAccounts() {
        // No accounts in DB
        when(savingsAccountMapper.listAll()).thenReturn(List.of());
        List<MoneyTransferRequest> empty = savingsAccountService.generateMockTransactions(10, 5);
        assertTrue(empty.isEmpty());

        // Test input capping
        SavingsAccount acc = mock(SavingsAccount.class);
        when(acc.getAccountNumber()).thenReturn(1L);
        when(acc.getBalance()).thenReturn(1000L);
        List<SavingsAccount> accounts = List.of(acc);
        when(savingsAccountMapper.listAll()).thenReturn(accounts);
        List<MoneyTransferRequest> capped = savingsAccountService.generateMockTransactions(200, 20);
        // Only 1 account, so should return empty (no destination possible)
        assertTrue(capped.isEmpty());
    }

    @Test
    void generateMockTransactions_stringList_basic() {
        MoneyTransferRequest req1 = new MoneyTransferRequest(1001L, 2002L, 500L);
        MoneyTransferRequest req2 = new MoneyTransferRequest(1002L, 2003L, 1000L);
        List<MoneyTransferRequest> requests = List.of(req1, req2);
        List<String> result = savingsAccountService.generateMockTransactions(requests);
        assertEquals(2, result.size());
        assertEquals("1001 2002 500", result.get(0));
        assertEquals("1002 2003 1000", result.get(1));
    }

    @Test
    void generateMockTransactions_stringList_empty() {
        List<String> result = savingsAccountService.generateMockTransactions(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void processMoneyTransfer_success() {
        long sourceAccountNumber = 2001L;
        long destinationAccountNumber = 2002L;
        long amount = 300L;
        SavingsAccount sourceAccount = mock(SavingsAccount.class);
        SavingsAccount destinationAccount = mock(SavingsAccount.class);
        MoneyTransferRequest req = new MoneyTransferRequest(sourceAccountNumber, destinationAccountNumber, amount);
        Lock sourceLock = mockLock();
        Lock destinationLock = mockLock();

        when(savingsAccountMapper.findByAccountNumber(sourceAccountNumber)).thenReturn(sourceAccount);
        when(savingsAccountMapper.findByAccountNumber(destinationAccountNumber)).thenReturn(destinationAccount);
        when(sourceAccount.getBalance()).thenReturn(1000L);
        when(destinationAccount.getBalance()).thenReturn(0L); // Added stub for destination account
        when(redisService.getLock(sourceAccountNumber)).thenReturn(sourceLock);
        when(redisService.getLock(destinationAccountNumber)).thenReturn(destinationLock);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        when(moneyTransferHistoryMapper.insert(any(MoneyTransferHistory.class))).thenReturn(1);
        when(savingsAccountMapper.update(any(SavingsAccount.class))).thenReturn(1);
        when(transactionTemplate.execute(any())).then(invocation -> {
            Object callback = invocation.getArgument(0);
            return ((org.springframework.transaction.support.TransactionCallback<?>) callback).doInTransaction(null);
        });

        SavingsAccount result = savingsAccountService.processMoneyTransfer(req);
        assertEquals(sourceAccount, result);
        verify(sourceAccount).setBalance(700L);
        verify(destinationAccount).setBalance(300L);
        verify(moneyTransferHistoryMapper).insert(any(MoneyTransferHistory.class));
    }

    @Test
    void processMoneyTransfer_insufficientFunds_throws() {
        long sourceAccountNumber = 2001L;
        long destinationAccountNumber = 2002L;
        long amount = 300L;
        SavingsAccount sourceAccount = mock(SavingsAccount.class);
        when(savingsAccountMapper.findByAccountNumber(sourceAccountNumber)).thenReturn(sourceAccount);
        when(sourceAccount.getBalance()).thenReturn(100L);
        MoneyTransferRequest req = new MoneyTransferRequest(sourceAccountNumber, destinationAccountNumber, amount);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        assertTrue(ex.getMessage().contains("Insufficient funds"));
    }

    @Test
    void processMoneyTransfer_sourceAccountNotFound_throws() {
        long sourceAccountNumber = 2001L;
        long destinationAccountNumber = 2002L;
        long amount = 300L;
        when(savingsAccountMapper.findByAccountNumber(sourceAccountNumber)).thenReturn(null);
        MoneyTransferRequest req = new MoneyTransferRequest(sourceAccountNumber, destinationAccountNumber, amount);
        Exception ex = assertThrows(NullPointerException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        assertTrue(ex.getMessage().contains("Source account not found"));
    }

    @Test
    void processMoneyTransfer_destinationAccountNotFound_throws() {
        long sourceAccountNumber = 2001L;
        long destinationAccountNumber = 2002L;
        long amount = 300L;
        SavingsAccount sourceAccount = mock(SavingsAccount.class);
        when(savingsAccountMapper.findByAccountNumber(sourceAccountNumber)).thenReturn(sourceAccount);
        when(sourceAccount.getBalance()).thenReturn(1000L);
        when(savingsAccountMapper.findByAccountNumber(destinationAccountNumber)).thenReturn(null);
        MoneyTransferRequest req = new MoneyTransferRequest(sourceAccountNumber, destinationAccountNumber, amount);
        Exception ex = assertThrows(NullPointerException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        assertTrue(ex.getMessage().contains("Destination account not found"));
    }

    @Test
    void processMoneyTransfer_lockAcquisitionFails_throws() {
        long sourceAccountNumber = 2001L;
        long destinationAccountNumber = 2002L;
        long amount = 300L;
        SavingsAccount sourceAccount = mock(SavingsAccount.class);
        SavingsAccount destinationAccount = mock(SavingsAccount.class);
        when(savingsAccountMapper.findByAccountNumber(sourceAccountNumber)).thenReturn(sourceAccount);
        when(sourceAccount.getBalance()).thenReturn(1000L);
        when(savingsAccountMapper.findByAccountNumber(destinationAccountNumber)).thenReturn(destinationAccount);
        when(redisService.getLock(sourceAccountNumber)).thenReturn(null);
        MoneyTransferRequest req = new MoneyTransferRequest(sourceAccountNumber, destinationAccountNumber, amount);
        Exception ex = assertThrows(IllegalStateException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        assertTrue(ex.getMessage().contains("Could not acquire lock"));
    }

    @Test
    void processMoneyTransfer_rollbackOnException() {
        long sourceAccountNumber = 2001L;
        long destinationAccountNumber = 2002L;
        long amount = 300L;
        SavingsAccount sourceAccount = mock(SavingsAccount.class);
        SavingsAccount destinationAccount = mock(SavingsAccount.class);
        Lock sourceLock = mockLock();
        Lock destinationLock = mockLock();
        when(savingsAccountMapper.findByAccountNumber(sourceAccountNumber)).thenReturn(sourceAccount);
        when(sourceAccount.getBalance()).thenReturn(1000L);
        when(savingsAccountMapper.findByAccountNumber(destinationAccountNumber)).thenReturn(destinationAccount);
        when(redisService.getLock(sourceAccountNumber)).thenReturn(sourceLock);
        when(redisService.getLock(destinationAccountNumber)).thenReturn(destinationLock);
        when(redisService.nextTransactionId()).thenReturn(TRANSACTION_ID);
        when(moneyTransferHistoryMapper.insert(any(MoneyTransferHistory.class))).thenThrow(new RuntimeException("DB error"));
        when(transactionTemplate.execute(any())).then(invocation -> {
            Object callback = invocation.getArgument(0);
            org.springframework.transaction.TransactionStatus mockStatus = mock(org.springframework.transaction.TransactionStatus.class);
            return ((org.springframework.transaction.support.TransactionCallback<?>) callback).doInTransaction(mockStatus);
        });
        MoneyTransferRequest req = new MoneyTransferRequest(sourceAccountNumber, destinationAccountNumber, amount);
        Exception ex = assertThrows(RuntimeException.class, () ->
            savingsAccountService.processMoneyTransfer(req));
        assertNotNull(ex.getMessage(), "Exception message should not be null");
        assertTrue(ex.getMessage().contains("error during money transfer processing"), "Actual exception message: " + ex.getMessage());
    }
}

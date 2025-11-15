package com.hsbc.iwpb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.mapper.SavingsAccountMapper;
import com.hsbc.iwpb.mapper.DepositOrWithdrawHistoryMapper;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import com.hsbc.iwpb.mapper.MoneyTransferHistoryMapper;
import com.hsbc.iwpb.entity.MoneyTransferHistory;
import com.hsbc.iwpb.entity.MoneyTransfer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

@Service
public class SavingsAccountService {
    private static final Logger log = LoggerFactory.getLogger(SavingsAccountService.class);

    private final SavingsAccountMapper savingsAccountMapper;
    private final RedisService redisService;
    private final DepositOrWithdrawHistoryMapper userDepositOrWithdrawHistoryMapper;
    private final MoneyTransferHistoryMapper transactionHistoryMapper;

    @Autowired
    public SavingsAccountService(SavingsAccountMapper savingsAccountMapper, RedisService redisService, DepositOrWithdrawHistoryMapper userDepositOrWithdrawHistoryMapper, MoneyTransferHistoryMapper ahm) {
        this.savingsAccountMapper = savingsAccountMapper;
        this.redisService = redisService;
        this.userDepositOrWithdrawHistoryMapper = userDepositOrWithdrawHistoryMapper;
        this.transactionHistoryMapper = ahm;
    }

    public SavingsAccount getAccount(long accountNumber) {
        return savingsAccountMapper.findByAccountNumber(accountNumber);
    }
    
    @Transactional
    public SavingsAccount depositOrWithdraw(long accountNumber, long amount) {
        SavingsAccount account = savingsAccountMapper.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account " + accountNumber + " does not exist");
        }
        long newBalance = account.getBalance() + amount;
        if (newBalance < 0) {
            throw new IllegalArgumentException("Insufficient funds in account " + accountNumber + " for withdrawal of " + (-amount));
        }
        final long transId = this.redisService.nextTransactionId();
        account.setBalance(newBalance);
        account.setLastUpdated(LocalDateTime.now());
        savingsAccountMapper.update(account);
        log.info("Account {} new balance after deposit/withdraw: {}", accountNumber, newBalance);
        DepositOrWithdrawHistory history = new DepositOrWithdrawHistory(accountNumber, amount, LocalDateTime.now(), transId);
        log.info("Recording deposit/withdraw history: accountNumber={}, amount={}", accountNumber, amount);
        userDepositOrWithdrawHistoryMapper.insert(history);
        return savingsAccountMapper.findByAccountNumber(accountNumber);
    }

    public SavingsAccount createAccount(String name, long personalId) {
        LocalDateTime now = LocalDateTime.now();
        SavingsAccount account = new SavingsAccount();
        account.setName(name);
        account.setPersonalId(personalId);
        account.setCreatedAt(now);
        account.setLastUpdated(now);
        savingsAccountMapper.insert(account);
        log.info("Created account {} with name='{}' personalId={} balance=0", account.getAccountNumber(), name, personalId);
        return account;
    }

    public void updateBalance(long accountNumber, long newBalance) {
        SavingsAccount existing = savingsAccountMapper.findByAccountNumber(accountNumber);
        if (existing == null) {
            throw new IllegalArgumentException("Account " + accountNumber + " does not exist");
        }
        if (newBalance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        LocalDateTime now = LocalDateTime.now();
        existing.setBalance(newBalance);
        existing.setLastUpdated(now);
        savingsAccountMapper.update(existing);
        log.info("Updated account {} balance to {}", accountNumber, newBalance);
    }

    public List<SavingsAccount> listAccounts() {
        return savingsAccountMapper.listAll();
    }

    public List<DepositOrWithdrawHistory> listDepositOrWithdrawHistory(long accountNumber) {
        return userDepositOrWithdrawHistoryMapper.findByAccountNumber(accountNumber);
    }
    
    public MoneyTransferHistory findByTransactionId(long transactionId) {
        return this.transactionHistoryMapper.findByTransactionId(transactionId);
    }

    public List<MoneyTransferHistory> findBySourceAccountNumber(long accountNumber) {
        return transactionHistoryMapper.findBySourceAccountNumber(accountNumber);
    }

    public List<MoneyTransferHistory> findByDestinationAccountNumber(long accountNumber) {
        return transactionHistoryMapper.findByDestinationAccountNumber(accountNumber);
    }
    
    public List<MoneyTransferHistory> findBySourceAndDestinationAccountNumber(long sourceAccountNumber, long destinationAccountNumber) {
        return transactionHistoryMapper.findBySourceAndDestinationAccountNumber(sourceAccountNumber, destinationAccountNumber);
    }

    public List<MoneyTransferHistory> listAllMoneyTransferHistories() {
        return transactionHistoryMapper.listAll();
    }
    
    private boolean checkIfSourceAccountHasEnoughBalance(long accountNumber, long amount) {
        return false;
    }

    public void processTransaction(MoneyTransfer transaction) {
        SavingsAccount account = this.savingsAccountMapper.findByAccountNumber(transaction.sourceAccountNumber());
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();
        final Retry retry = Retry.of("transactionServiceRetry", retryConfig);
        Callable<Lock[]> callable = Retry.decorateCallable(retry, () -> {
            Lock sourceAccountLock = redisService.getLock(transaction.sourceAccountNumber());
            if (sourceAccountLock == null) {
                throw new IllegalStateException("Could not acquire lock for source account: " + transaction.sourceAccountNumber());
            }
            Lock destinationAccountLock = redisService.getLock(transaction.destinationAccountNumber());
            if (destinationAccountLock == null) {
                sourceAccountLock.unlock();
                throw new IllegalStateException("Could not acquire lock for destination account: " + transaction.destinationAccountNumber());
            }
            return new Lock[] {sourceAccountLock, destinationAccountLock};
        });
        try {
            // TODO: add logic
            Lock[] locks = callable.call();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
package com.hsbc.iwpb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.dto.MoneyTransferRequest;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.mapper.SavingsAccountMapper;
import com.hsbc.iwpb.mapper.DepositOrWithdrawHistoryMapper;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import com.hsbc.iwpb.mapper.MoneyTransferHistoryMapper;
import com.hsbc.iwpb.entity.MoneyTransferHistory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

@Service
public class SavingsAccountService {
    private static final Logger log = LoggerFactory.getLogger(SavingsAccountService.class);

    private final SavingsAccountMapper savingsAccountMapper;
    private final RedisService redisService;
    private final DepositOrWithdrawHistoryMapper userDepositOrWithdrawHistoryMapper;
    private final MoneyTransferHistoryMapper moneyTransferHistoryMapper;

    @Autowired
    public SavingsAccountService(SavingsAccountMapper savingsAccountMapper, RedisService redisService, DepositOrWithdrawHistoryMapper userDepositOrWithdrawHistoryMapper, MoneyTransferHistoryMapper ahm) {
        this.savingsAccountMapper = savingsAccountMapper;
        this.redisService = redisService;
        this.userDepositOrWithdrawHistoryMapper = userDepositOrWithdrawHistoryMapper;
        this.moneyTransferHistoryMapper = ahm;
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
        
        account.setBalance(newBalance);
        account.setLastUpdated(LocalDateTime.now());
        // lock accounts
        Lock lock = lockAccount(accountNumber);
        
        try {
	        final long transId = this.redisService.nextTransactionId();
	        savingsAccountMapper.update(account);
	        log.info("Account {} new balance after deposit/withdraw: {}", accountNumber, newBalance);
	        
	        DepositOrWithdrawHistory history = new DepositOrWithdrawHistory(accountNumber, amount, LocalDateTime.now(), transId);
	        log.info("Recording deposit/withdraw history: accountNumber={}, amount={}", accountNumber, amount);
	        userDepositOrWithdrawHistoryMapper.insert(history);
	        
	        return savingsAccountMapper.findByAccountNumber(accountNumber);
        } finally {
			lock.unlock();
		}
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

    public List<SavingsAccount> listAccounts() {
        return savingsAccountMapper.listAll();
    }

    public List<DepositOrWithdrawHistory> listDepositOrWithdrawHistory(long accountNumber) {
        return userDepositOrWithdrawHistoryMapper.findByAccountNumber(accountNumber);
    }
    
    public MoneyTransferHistory findByTransactionId(long transactionId) {
        return this.moneyTransferHistoryMapper.findByTransactionId(transactionId);
    }

    public List<MoneyTransferHistory> findBySourceAccountNumber(long accountNumber) {
        return moneyTransferHistoryMapper.findBySourceAccountNumber(accountNumber);
    }

    public List<MoneyTransferHistory> findByDestinationAccountNumber(long accountNumber) {
        return moneyTransferHistoryMapper.findByDestinationAccountNumber(accountNumber);
    }
    
    public List<MoneyTransferHistory> findBySourceAndDestinationAccountNumber(long sourceAccountNumber, long destinationAccountNumber) {
        return moneyTransferHistoryMapper.findBySourceAndDestinationAccountNumber(sourceAccountNumber, destinationAccountNumber);
    }

    public List<MoneyTransferHistory> listAllMoneyTransferHistories() {
        return moneyTransferHistoryMapper.listAll();
    }
    
    private Lock lockAccount(long accountNumber) {
    	RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();
        final Retry retry = Retry.of("transactionServiceRetry", retryConfig);
        Supplier<Lock> lockSupplier = Retry.decorateSupplier(retry, () -> {
            Lock sourceAccountLock = redisService.getLock(accountNumber);
            if (sourceAccountLock == null) {
                throw new IllegalStateException("Could not acquire lock for source account: " + accountNumber);
            }
            return sourceAccountLock;
        });
        return lockSupplier.get();
	}
    
    private Lock[] lockAccounts(long sourceAccountNumber, long destinationAccountNumber) {
    	RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();
        final Retry retry = Retry.of("transactionServiceRetry", retryConfig);
        Supplier<Lock[]> lockSupplier = Retry.decorateSupplier(retry, () -> {
            Lock sourceAccountLock = redisService.getLock(sourceAccountNumber);
            if (sourceAccountLock == null) {
                throw new IllegalStateException("Could not acquire lock for source account: " + sourceAccountNumber);
            }
            Lock destinationAccountLock = redisService.getLock(destinationAccountNumber);
            if (destinationAccountLock == null) {
                sourceAccountLock.unlock();
                throw new IllegalStateException("Could not acquire lock for destination account: " + destinationAccountNumber);
            }
            return new Lock[] {sourceAccountLock, destinationAccountLock};
        });
        return lockSupplier.get();
	}

    @Transactional
    public SavingsAccount processMoneyTransfer(MoneyTransferRequest req) {
        SavingsAccount account = this.savingsAccountMapper.findByAccountNumber(req.sourceAccountNumber());
        if (account.getBalance() < req.amount()) {
			throw new IllegalArgumentException("Insufficient funds in source account: " + req.sourceAccountNumber());
		}
        final long san = req.sourceAccountNumber();
        final long dan = req.destinationAccountNumber();
        
        
        Lock[] locks = lockAccounts(san, dan);
        try  {
        	// save money transfer history
            long tid = redisService.nextTransactionId();
            MoneyTransferHistory transfer = new MoneyTransferHistory(tid, san, dan, req.amount(), LocalDateTime.now());
            this.moneyTransferHistoryMapper.insert(transfer);
            log.info("Recorded money transfer: {}", transfer);
            
            // reduce money in source account
            SavingsAccount sourceAccount = this.savingsAccountMapper.findByAccountNumber(san);
            sourceAccount.setBalance(sourceAccount.getBalance() - req.amount());	
            this.savingsAccountMapper.update(sourceAccount);
            log.info("Debited source account {} by amount {}. New balance: {}", san, req.amount(), sourceAccount.getBalance());
            
            // increment money in destination account
            SavingsAccount destinationAccount = this.savingsAccountMapper.findByAccountNumber(dan);
            destinationAccount.setBalance(destinationAccount.getBalance() + req.amount());
            this.savingsAccountMapper.update(destinationAccount);
            log.info("Credited destination account {} by amount {}. New balance: {}", dan, req.amount(), destinationAccount.getBalance());
            
            return sourceAccount;
        } catch (Exception e) {
			log.error("Error processing transaction: {}", e.getMessage());
			throw e;
		} finally {
			for (Lock lock : locks) {
				lock.unlock();
			}
		} 
    }
}
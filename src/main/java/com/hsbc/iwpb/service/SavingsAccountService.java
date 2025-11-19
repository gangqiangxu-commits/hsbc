package com.hsbc.iwpb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.dto.MoneyTransferRequest;
import com.hsbc.iwpb.dto.MoneyTransferResponse;
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
import java.util.ArrayList;
import java.util.Collections;
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
    private final TransactionTemplate transactionTemplate;

    private static final RetryConfig lockAccountRetryConfig = RetryConfig.custom()
            .maxAttempts(10)
            .waitDuration(Duration.ofMillis(1000))
            .build();
    
    private static final RetryConfig depositOrWithdrawRetryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .build();
    
    private static final RetryConfig moneyTransferRetryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(200))
            .build();
    
    @Autowired
    public SavingsAccountService(SavingsAccountMapper savingsAccountMapper, RedisService redisService, DepositOrWithdrawHistoryMapper userDepositOrWithdrawHistoryMapper, MoneyTransferHistoryMapper ahm, TransactionTemplate transactionTemplate) {
        this.savingsAccountMapper = savingsAccountMapper;
        this.redisService = redisService;
        this.userDepositOrWithdrawHistoryMapper = userDepositOrWithdrawHistoryMapper;
        this.moneyTransferHistoryMapper = ahm;
        this.transactionTemplate = transactionTemplate;
    }

    public SavingsAccount getAccount(long accountNumber) {
        return savingsAccountMapper.findByAccountNumber(accountNumber);
    }
    
    private SavingsAccount getAccountWithNewBalance(long accountNumber, long amount) {
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
        return account;
    }
    
    private void conductDepositOrWithdraw(SavingsAccount account, DepositOrWithdrawHistory history) {
    	this.transactionTemplate.executeWithoutResult(status -> {
    		try {
    			savingsAccountMapper.update(account);
    	        log.info("Account {} new balance after deposit/withdraw: {}", account.getAccountNumber(), account.getBalance());
    	        
    	        
    	        log.info("Recording deposit/withdraw history: accountNumber={}, amount={}", history.getAccountNumber(), history.getAmount());
    	        userDepositOrWithdrawHistoryMapper.insert(history);
    		} catch(Exception e) {
				log.error("Error during money transfer processing, marking for rollback: {}", e.getMessage());
				status.setRollbackOnly();
				throw new RuntimeException("error during money transfer processing", e);
    		}
    	});
    }
    
    public SavingsAccount depositOrWithdraw(long accountNumber, long amount) {
    	SavingsAccount account = getAccountWithNewBalance(accountNumber, amount);
        // lock accounts
        Lock lock = lockAccount(accountNumber);
        
        try {
	        final long transId = this.redisService.nextTransactionId();
	        DepositOrWithdrawHistory history = new DepositOrWithdrawHistory(accountNumber, amount, account.getLastUpdated(), transId);
	        
	        Retry retry = Retry.of("depositOrWithdrawRetry", depositOrWithdrawRetryConfig);
	        Retry.decorateRunnable(retry, () -> conductDepositOrWithdraw(account, history)).run();
	        
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
        final Retry retry = Retry.of("lockAccountRetry", lockAccountRetryConfig);
        Supplier<Lock> lockSupplier = Retry.decorateSupplier(retry, () -> {
            Lock accountLock = redisService.getLock(accountNumber);
            if (accountLock == null) {
                throw new IllegalStateException("Could not acquire lock for account: " + accountNumber);
            }
            return accountLock;
        });
        return lockSupplier.get();
	}
    
    private Lock[] lockAccounts(long sourceAccountNumber, long destinationAccountNumber) {
    	// avoid deadlock by always locking in the order of smaller account number first
    	long smallerAccountNumber = Math.min(sourceAccountNumber,  destinationAccountNumber);
    	long largerAccountNumber = Math.max(sourceAccountNumber,  destinationAccountNumber);
    	
        final Retry retry = Retry.of("lockAccountsRetry", lockAccountRetryConfig);
        Supplier<Lock[]> lockSupplier = Retry.decorateSupplier(retry, () -> {
            Lock smallerAccountLock = redisService.getLock(smallerAccountNumber);
            if (smallerAccountLock == null) {
                throw new IllegalStateException("Could not acquire lock for account: " + smallerAccountNumber);
            }
            Lock largerAccountLock = redisService.getLock(largerAccountNumber);
            if (largerAccountLock == null) {
            	smallerAccountLock.unlock();
                throw new IllegalStateException("Could not acquire lock for account: " + largerAccountNumber);
            }
            return new Lock[] {smallerAccountLock, largerAccountLock};
        });
        return lockSupplier.get();
	}

    public List<MoneyTransferResponse> processMoneyTransferList(List<MoneyTransferRequest> req) {
        if (req == null || req.isEmpty()) {
            return Collections.emptyList();
        }
        
        req = new ArrayList<>(req); // make a modifiable copy
        List<MoneyTransferResponse> responses = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        // shuffle the request list to simulate random order processing - avoid same source account contention (lock)
        Collections.shuffle(req);
        for (MoneyTransferRequest r : req) {
            Thread t = Thread.ofVirtual().unstarted(() -> {
                try {
                    SavingsAccount result = processMoneyTransfer(r);
                    responses.add(new MoneyTransferResponse(result, true, ""));
                } catch (Exception e) {
                    log.error("Error processing money transfer request: {}", r, e);
                    responses.add(new MoneyTransferResponse(null, false, e.getMessage()));
                }
            });
            threads.add(t);
            t.start();
        }
        
        log.info("Started {} threads for processing money transfers", threads.size());
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while processing money transfers", e);
            }
        }
        return responses;
    }
    
    private void validateMoneyTransferRequest(MoneyTransferRequest req) {
    	if (req.sourceAccountNumber() == req.destinationAccountNumber()) {
			throw new IllegalArgumentException("Source and destination account numbers cannot be the same: " + req.sourceAccountNumber());
		}
    	
    	SavingsAccount account = this.savingsAccountMapper.findByAccountNumber(req.sourceAccountNumber());
        if (account == null) {
        	throw new NullPointerException("Source account not found");
        }
        if (account.getBalance() < req.amount()) {
            throw new IllegalArgumentException("Insufficient funds in source account: " + req.sourceAccountNumber());
        }
        
        account = this.savingsAccountMapper.findByAccountNumber(req.destinationAccountNumber());
        if (account == null) {
        	throw new NullPointerException("Destination account not found");
        }
    }
    
    private SavingsAccount doProcessMoneyTransfer(MoneyTransferRequest req) {
    	return  this.transactionTemplate.execute(status -> {
    		try {
		    	final long san = req.sourceAccountNumber();
		        final long dan = req.destinationAccountNumber();
		        
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
    		} catch(Exception e) {
				log.error("Error during money transfer processing, marking for rollback: {}", e.getMessage());
				status.setRollbackOnly();
				throw new RuntimeException("error during money transfer processing", e);
    		}
    	});
    }
    
    public SavingsAccount processMoneyTransfer(MoneyTransferRequest req) {
    	validateMoneyTransferRequest(req);
        
        Lock[] locks = lockAccounts(req.sourceAccountNumber(), req.destinationAccountNumber());
        try  {
            final Retry retry = Retry.of("processMoneyTransferRetry", moneyTransferRetryConfig);
            return Retry.decorateSupplier(retry, () -> doProcessMoneyTransfer(req)).get();
        } catch (RuntimeException re) {
        	log.error("Error processing money transfer: {}, req:{}", re.getMessage(), req);
            throw re;
		} catch (Exception e) {
            log.error("Error processing transaction: {}, req:{}", e.getMessage(), req);
            throw new RuntimeException("error process money transfer in transaction", e);
        } finally {
            for (Lock lock : locks) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Generate mock money transfer requests for testing.
     * @param countOfSourceAccounts number of source accounts to use (max 100)
     * @param countOfDestinationAccountsForEachSourceAccount number of destination accounts per source (max 100)
     * @return list of MoneyTransferRequest
     */
    public List<MoneyTransferRequest> generateMockTransactions(int countOfSourceAccounts, int countOfDestinationAccountsForEachSourceAccount) {
        // Cap the input values
    	final int MAX = 100;
        if (countOfSourceAccounts > MAX) {
        	countOfSourceAccounts = MAX;
        }
        if (countOfDestinationAccountsForEachSourceAccount > MAX) {
        	countOfDestinationAccountsForEachSourceAccount = MAX;
        }

        List<SavingsAccount> allAccounts = listAccounts();
        int totalAccounts = allAccounts.size();
        if (totalAccounts < countOfSourceAccounts) {
            countOfSourceAccounts = totalAccounts;
        }
        if (countOfDestinationAccountsForEachSourceAccount >= totalAccounts) {
			countOfDestinationAccountsForEachSourceAccount = totalAccounts - 1;
		}
        
        if (countOfSourceAccounts == 0 || countOfDestinationAccountsForEachSourceAccount == 0) {
            return Collections.emptyList();
        }

        List<SavingsAccount> sourceAccounts = new ArrayList<>(allAccounts);
        Collections.sort(sourceAccounts, (a, b) -> Long.compare(b.getBalance(), a.getBalance()));
        sourceAccounts = sourceAccounts.subList(0, countOfSourceAccounts);

        List<MoneyTransferRequest> requests = new ArrayList<>();
        for (SavingsAccount source : sourceAccounts) {
            long balance = source.getBalance();
            if (balance <= 0) {
            	continue;
            }
            long amount = balance / countOfDestinationAccountsForEachSourceAccount;
            if (amount <= 0) {
            	continue;
            }
            // Prepare destination candidates (exclude self)
            List<SavingsAccount> destCandidates = new ArrayList<>(allAccounts);
            destCandidates.remove(source);
            if (destCandidates.isEmpty()) {
            	continue;
            }
            Collections.shuffle(destCandidates);
            
            final long san  = source.getAccountNumber();
            for (int i = 0; i < countOfDestinationAccountsForEachSourceAccount; i++) {
                requests.add(new MoneyTransferRequest(san, destCandidates.get(i).getAccountNumber(), amount));
            }
        }
        return requests;
    }
    
    /**
     * Converts a list of MoneyTransferRequest to a list of strings with 3 columns: source account, destination account, amount.
     * Uses StringBuilder for efficient string concatenation.
     * @param requests list of MoneyTransferRequest
     * @return list of string, each string formatted as "sourceAccount destinationAccount amount"
     */
    public List<String> generateMockTransactions(List<MoneyTransferRequest> requests) {
        List<String> result = new ArrayList<>();
        for (MoneyTransferRequest req : requests) {
            StringBuilder sb = new StringBuilder();
            sb.append(req.sourceAccountNumber())
              .append(" ")
              .append(req.destinationAccountNumber())
              .append(" ")
              .append(req.amount());
            result.add(sb.toString());
        }
        return result;
    }
}

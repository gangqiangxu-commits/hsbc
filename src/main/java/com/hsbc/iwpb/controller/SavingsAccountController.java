package com.hsbc.iwpb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hsbc.iwpb.dto.MoneyTransferRequest;
import com.hsbc.iwpb.dto.MoneyTransferResponse;
import com.hsbc.iwpb.dto.DepositWithdrawRequest;
import com.hsbc.iwpb.dto.DepositWithdrawResponse;
import com.hsbc.iwpb.dto.OpenAccountRequest;
import com.hsbc.iwpb.entity.MoneyTransferHistory;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.service.SavingsAccountService;

import java.util.List;
import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class SavingsAccountController {
	
	private static final Logger log = LoggerFactory.getLogger(SavingsAccountController.class);

    private final SavingsAccountService savingsAccountService;

    @Autowired
    public SavingsAccountController(SavingsAccountService savingsAccountService) {
        this.savingsAccountService = savingsAccountService;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello, World!");
    }
    
    // JSON-based endpoint: explicitly consume application/json
    @PostMapping(path = "/transaction:process", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MoneyTransferResponse> processTransactionJson(@RequestBody MoneyTransferRequest req) {
    	try {
	    	var account = this.savingsAccountService.processMoneyTransfer(req);
	        return ResponseEntity.ok(new MoneyTransferResponse(account, true, ""));
    	} catch (Exception e) {
    		log.error("Error processing transaction: {}, request: {}", e.getMessage(), req);
    		return ResponseEntity.ofNullable(new MoneyTransferResponse(null, false, e.getMessage()));
    	}
    }
    
    @PostMapping(path = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavingsAccount> openAccount(@RequestBody OpenAccountRequest req) {
        SavingsAccount sa = savingsAccountService.createAccount(req.name(), req.personalId());
        return ResponseEntity.ok(sa);
    }
    
    @PostMapping(path = "/accounts:batchOpen")
    public ResponseEntity<List<SavingsAccount>> batchOpenAccounts(@RequestParam int numAccounts, @RequestParam int balance) {
		log.info("Received request to create {} accounts with balance={}", numAccounts, balance);
		
		if (balance <= 0) {
			balance = 10000; // default balance
			log.info("Using default balance={}", balance);
		}
		
		if (numAccounts <= 0 || numAccounts > 1000) {
			numAccounts = 10000; // default number of accounts
			log.info("Using default numAccounts={}", numAccounts);
		}
		
    	List<SavingsAccount> createdAccounts = openAccounts(numAccounts, balance);
        return ResponseEntity.ok(createdAccounts);
    }

	private List<SavingsAccount> openAccounts(int numAccounts, int balance) {
		List<SavingsAccount> createdAccounts = new java.util.ArrayList<>();
        for (int i=0; i<numAccounts; i++) {
			String name = "Mocked User" + (1000 + i);
			long personalId = 200000 + i;
			SavingsAccount sa = savingsAccountService.createAccount(name, personalId);
			savingsAccountService.depositOrWithdraw(sa.getAccountNumber(), balance);
			sa = savingsAccountService.getAccount(sa.getAccountNumber());
			log.info("Created mock account: accountNumber={}, name={}, personalId={}, balance={}", sa.getAccountNumber(), name, personalId, sa.getBalance());
			createdAccounts.add(sa);
		}
		return createdAccounts;
	}
    
    @PostMapping(path = "/account:depositOrWithdraw", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DepositWithdrawResponse> depositWithdraw(@RequestBody DepositWithdrawRequest req) {
        try {
        	SavingsAccount sa = savingsAccountService.depositOrWithdraw(req.accountNumber(), req.amount());
        	return ResponseEntity.ok(new DepositWithdrawResponse(sa, true, ""));
        } catch (Exception e) {
			log.error("Error during deposit/withdraw for accountNumber={}, amount={}: {}", req.accountNumber(), req.amount(), e.getMessage());
			return ResponseEntity.ofNullable(new DepositWithdrawResponse(null, false, e.getMessage()));
		}
        
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<SavingsAccount>> listAccounts() {
        return ResponseEntity.ok(savingsAccountService.listAccounts());
    }
    
    @GetMapping("/account")
    public ResponseEntity<SavingsAccount> getAccount(@RequestParam("accountNumber") long accountNumber) {
        return ResponseEntity.ok(savingsAccountService.getAccount(accountNumber));
    }

    @GetMapping("/account/depositOrWithdraw:history")
    public ResponseEntity<List<DepositOrWithdrawHistory>> listDepositOrWithdrawHistory(@RequestParam("accountNumber") long accountNumber) {
        return ResponseEntity.ok(savingsAccountService.listDepositOrWithdrawHistory(accountNumber));
    }
    
    @GetMapping("/transfer:search:by-transaction-id")
    public ResponseEntity<MoneyTransferHistory> findByTransactionId(@RequestParam("transactionId") long transactionId) {
        return ResponseEntity.ok(savingsAccountService.findByTransactionId(transactionId));
    }

    @GetMapping("/transfer:search:by-source-account")
    public ResponseEntity<List<MoneyTransferHistory>> findBySourceAccountNumber(@RequestParam("accountNumber") long accountNumber) {
        return ResponseEntity.ok(savingsAccountService.findBySourceAccountNumber(accountNumber));
    }

    @GetMapping("/transfer:search:by-destination-account")
    public ResponseEntity<List<MoneyTransferHistory>> findByDestinationAccountNumber(@RequestParam("accountNumber") long accountNumber) {
        return ResponseEntity.ok(savingsAccountService.findByDestinationAccountNumber(accountNumber));
    }

    @GetMapping("/transfer:search:by-source-and-destination-account")
    public ResponseEntity<List<MoneyTransferHistory>> searchBySourceAndDestination(
            @RequestParam(value = "sourceAccountNumber", required = true) long sourceAccountNumber,
            @RequestParam(value = "destinationAccountNumber", required = true) long destinationAccountNumber) {
        List<MoneyTransferHistory> result = savingsAccountService.findBySourceAndDestinationAccountNumber(sourceAccountNumber, destinationAccountNumber);
        return ResponseEntity.ok(result);
    }
}
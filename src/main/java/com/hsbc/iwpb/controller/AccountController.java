package com.hsbc.iwpb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hsbc.iwpb.component.RedisService;
import com.hsbc.iwpb.dto.AccountBalanceTransanctionRequest;
import com.hsbc.iwpb.dto.DepositWithdrawRequest;
import com.hsbc.iwpb.dto.OpenAccountRequest;
import com.hsbc.iwpb.entity.MoneyTransfer;
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
public class AccountController {
	
	private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final SavingsAccountService savingsAccountService;
    private final RedisService redisService;

    @Autowired
    public AccountController(SavingsAccountService savingsAccountService, RedisService rs) {
        this.savingsAccountService = savingsAccountService;
        this.redisService = rs;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello, World!");
    }
    
    // JSON-based endpoint: explicitly consume application/json
    @PostMapping(path = "/transaction:process", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MoneyTransfer> processTransactionJson(@RequestBody AccountBalanceTransanctionRequest req) {
        return process(req);
    }
    
    @PostMapping(path = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavingsAccount> openAccount(@RequestBody OpenAccountRequest req) {
        SavingsAccount sa = savingsAccountService.createAccount(req.name(), req.personalId());
        return ResponseEntity.ok(sa);
    }
    
    @PostMapping(path = "/account:depositOrWithdraw", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavingsAccount> depositWithdraw(@RequestBody DepositWithdrawRequest req) {
        SavingsAccount sa = savingsAccountService.depositOrWithdraw(req.accountNumber(), req.amount());
        return ResponseEntity.ok(sa);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<SavingsAccount>> listAccounts() {
        return ResponseEntity.ok(savingsAccountService.listAccounts());
    }

    @GetMapping("/account/history")
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

    // Common processing logic
    private ResponseEntity<MoneyTransfer> process(AccountBalanceTransanctionRequest req) {
    	long ts = System.currentTimeMillis();
        long nextTransactionId = redisService.nextTransactionId();
        log.info("request: {}", req);
        MoneyTransfer transaction = new MoneyTransfer(
				nextTransactionId,
				req.sourceAccountNumber(),
				req.destinationAccountNumber(),
				req.amount(),
				ts
		);
        // TODO: add logic
        return ResponseEntity.ok(transaction);
    }
}
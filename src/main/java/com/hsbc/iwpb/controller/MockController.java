package com.hsbc.iwpb.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.entity.service.SavingsAccountService;

@RestController
public class MockController {
	
	private static final Logger log = LoggerFactory.getLogger(MockController.class);

    private final SavingsAccountService savingsAccountService;

    @Autowired
    public MockController(SavingsAccountService savingsAccountService) {
        this.savingsAccountService = savingsAccountService;
    }

    @PostMapping(path = "/mock:openAccounts")
    public ResponseEntity<List<SavingsAccount>> mockOpenAccounts(@RequestParam int numAccounts, @RequestParam int balance) {
		log.info("Received request to create {} mock accounts with balance={}", numAccounts, balance);
		
		if (balance <= 0) {
			balance = 10000; // default balance
			log.info("Using default balance={}", balance);
		}
		
		if (numAccounts <= 0 || numAccounts > 1000) {
			numAccounts = 10000; // default number of accounts
			log.info("Using default numAccounts={}", numAccounts);
		}
		
    	List<SavingsAccount> createdAccounts = new java.util.ArrayList<>();
        for (int i=0; i<numAccounts; i++) {
			String name = "User" + (1000 + i);
			long personalId = 200000 + i;
			SavingsAccount sa = savingsAccountService.createAccount(name, personalId);
			savingsAccountService.depositOrWithdraw(sa.getAccountNumber(), balance);
			sa = savingsAccountService.getAccount(sa.getAccountNumber());
			log.info("Created mock account: accountNumber={}, name={}, personalId={}, balance={}", sa.getAccountNumber(), name, personalId, sa.getBalance());
			createdAccounts.add(sa);
		}
        return ResponseEntity.ok(createdAccounts);
    }
}
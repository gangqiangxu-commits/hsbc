package com.hsbc.iwpb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.hsbc.iwpb.dto.MoneyTransferRequest;
import com.hsbc.iwpb.dto.MoneyTransferResponse;
import com.hsbc.iwpb.dto.DepositWithdrawRequest;
import com.hsbc.iwpb.dto.DepositWithdrawResponse;
import com.hsbc.iwpb.dto.OpenAccountRequest;
import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import com.hsbc.iwpb.entity.MoneyTransferHistory;
import com.hsbc.iwpb.entity.SavingsAccount;
import com.hsbc.iwpb.service.SavingsAccountService;
import com.hsbc.iwpb.util.SavingsAccountUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
    @PostMapping(path = "/moneyTransfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MoneyTransferResponse> processMoneyTransfer(@RequestBody MoneyTransferRequest req) {
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
		
		final int max = 1000;
		
		if (balance <= 0) {
			balance = max; // default balance
			log.info("Using default balance={}", balance);
		}
		
		if (numAccounts <= 0 || numAccounts > max) {
			numAccounts = max; // default number of accounts
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
    
    @PostMapping(path = "/moneyTransfer:batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<MoneyTransferResponse>> batchMoneyTransfer(@RequestPart("file") MultipartFile file) {
        List<MoneyTransferRequest> requests;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // Step 1: Parse the file
            requests = SavingsAccountUtil.parseBatchMoneyTransferFile(reader);
        } catch (IllegalArgumentException e) {
            log.error("File format error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(new MoneyTransferResponse(null, false, e.getMessage())));
        } catch (Exception e) {
            log.error("Error reading uploaded file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of(new MoneyTransferResponse(null, false, "Internal server error: " + e.getMessage())));
        }
        // Step 2: Process the parsed rows one by one
        var responses = this.savingsAccountService.processMoneyTransferList(requests);
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/mock-transactions:download")
    public ResponseEntity<byte[]> generateMockTransactionsFile(
            @RequestParam int countOfSourceAccounts,
            @RequestParam int countOfDestinationAccountsForEachSourceAccount) {
        // Step 1: Generate mock MoneyTransferRequest list
        List<MoneyTransferRequest> requests = savingsAccountService.generateMockTransactions(
                countOfSourceAccounts, countOfDestinationAccountsForEachSourceAccount);
        // Step 2: Convert to list of strings
        List<String> lines = savingsAccountService.generateMockTransactions(requests);
        // Step 3: Join lines and return as downloadable text file
        String content = String.join(System.lineSeparator(), lines);
        byte[] fileContent = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "mock-transactions.csv";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .contentType(MediaType.TEXT_PLAIN)
                .body(fileContent);
    }
}
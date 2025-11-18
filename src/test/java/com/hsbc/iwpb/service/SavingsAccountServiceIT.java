package com.hsbc.iwpb.service;

import com.hsbc.iwpb.entity.SavingsAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {com.hsbc.iwpb.SavingsAccountApplication.class, com.hsbc.iwpb.config.TestRedissonConfig.class})
@ActiveProfiles("test")
public class SavingsAccountServiceIT {

    @Autowired
    private SavingsAccountService savingsAccountService;

    @Test
    void contextLoads() {
        assertThat(savingsAccountService).isNotNull();
    }

    @Test
    void canCreateAndRetrieveAccount() {
        SavingsAccount sa = savingsAccountService.createAccount("Test User", 123456L);
        assertThat(sa).isNotNull();
        assertThat(sa.getName()).isEqualTo("Test User");
        SavingsAccount found = savingsAccountService.getAccount(sa.getAccountNumber());
        assertThat(found).isNotNull();
        assertThat(found.getPersonalId()).isEqualTo(123456L);
    }

    @Test
    void canDepositAndWithdraw() {
        SavingsAccount sa = savingsAccountService.createAccount("Test User", 123456L);
        savingsAccountService.depositOrWithdraw(sa.getAccountNumber(), 500);
        SavingsAccount updated = savingsAccountService.getAccount(sa.getAccountNumber());
        assertThat(updated.getBalance()).isEqualTo(500);
        savingsAccountService.depositOrWithdraw(sa.getAccountNumber(), -200);
        SavingsAccount afterWithdraw = savingsAccountService.getAccount(sa.getAccountNumber());
        assertThat(afterWithdraw.getBalance()).isEqualTo(300);
    }

    @Test
    void canProcessMoneyTransfer() {
        // Create source and destination accounts
        SavingsAccount source = savingsAccountService.createAccount("Source User", 111111L);
        SavingsAccount dest = savingsAccountService.createAccount("Dest User", 222222L);
        // Deposit into source account
        savingsAccountService.depositOrWithdraw(source.getAccountNumber(), 1000);
        // Transfer 400 from source to dest
        var req = new com.hsbc.iwpb.dto.MoneyTransferRequest(
            source.getAccountNumber(),
            dest.getAccountNumber(),
            400L
        );
        savingsAccountService.processMoneyTransfer(req);
        // Assert balances
        SavingsAccount updatedSource = savingsAccountService.getAccount(source.getAccountNumber());
        SavingsAccount updatedDest = savingsAccountService.getAccount(dest.getAccountNumber());
        assertThat(updatedSource.getBalance()).isEqualTo(600);
        assertThat(updatedDest.getBalance()).isEqualTo(400);
    }
}
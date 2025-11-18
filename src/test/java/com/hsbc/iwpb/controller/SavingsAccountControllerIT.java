package com.hsbc.iwpb.controller;

import com.hsbc.iwpb.dto.OpenAccountRequest;
import com.hsbc.iwpb.dto.DepositWithdrawRequest;
import com.hsbc.iwpb.entity.SavingsAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SavingsAccountControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @Test
    void canOpenAccountAndDeposit() {
        String baseUrl = "http://localhost:" + port;
        // Open account
        OpenAccountRequest req = new OpenAccountRequest("Integration User", 999999L);
        ResponseEntity<SavingsAccount> response = restTemplate.postForEntity(baseUrl + "/account", req, SavingsAccount.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SavingsAccount sa = response.getBody();
        assertThat(sa).isNotNull();
        assertThat(sa.getName()).isEqualTo("Integration User");
        // Deposit
        DepositWithdrawRequest depReq = new DepositWithdrawRequest(sa.getAccountNumber(), 1000);
        ResponseEntity<String> depResp = restTemplate.postForEntity(baseUrl + "/account:depositOrWithdraw", depReq, String.class);
        assertThat(depResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void canListAccounts() {
        String baseUrl = "http://localhost:" + port;
        ResponseEntity<SavingsAccount[]> response = restTemplate.getForEntity(baseUrl + "/accounts", SavingsAccount[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
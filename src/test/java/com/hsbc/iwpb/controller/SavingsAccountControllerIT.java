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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.hsbc.iwpb.dto.MoneyTransferResponse;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {com.hsbc.iwpb.SavingsAccountApplication.class, com.hsbc.iwpb.config.TestRedissonConfig.class})
@ActiveProfiles("test")
public class SavingsAccountControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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

    @Test
    void batchMoneyTransfer_success() {
        try {
            String baseUrl = "http://localhost:" + port;
            // Create source and destination accounts for both transfers
            SavingsAccount sa1 = restTemplate.postForEntity(baseUrl + "/account", new OpenAccountRequest("User1", 1001L), SavingsAccount.class).getBody();
            SavingsAccount sa2 = restTemplate.postForEntity(baseUrl + "/account", new OpenAccountRequest("User2", 2001L), SavingsAccount.class).getBody();
            SavingsAccount sa3 = restTemplate.postForEntity(baseUrl + "/account", new OpenAccountRequest("User3", 1002L), SavingsAccount.class).getBody();
            SavingsAccount sa4 = restTemplate.postForEntity(baseUrl + "/account", new OpenAccountRequest("User4", 2002L), SavingsAccount.class).getBody();
            // Also create and fund account 5 in case it is referenced by other logic
            SavingsAccount sa5 = restTemplate.postForEntity(baseUrl + "/account", new OpenAccountRequest("User5", 5L), SavingsAccount.class).getBody();
            restTemplate.postForEntity(baseUrl + "/account:depositOrWithdraw", new DepositWithdrawRequest(sa5.getAccountNumber(), 10000L), String.class);
            // Fund source accounts
            restTemplate.postForEntity(baseUrl + "/account:depositOrWithdraw", new DepositWithdrawRequest(sa1.getAccountNumber(), 50000L), String.class);
            restTemplate.postForEntity(baseUrl + "/account:depositOrWithdraw", new DepositWithdrawRequest(sa3.getAccountNumber(), 50000L), String.class);

            // Use actual account numbers in space-separated format
            String txt = sa1.getAccountNumber() + " " + sa2.getAccountNumber() + " 500\n" + sa3.getAccountNumber() + " " + sa4.getAccountNumber() + " 1000\n";
            ByteArrayResource resource = new ByteArrayResource(txt.getBytes()) {
                @Override
                public String getFilename() {
                    return "batch.txt";
                }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<MoneyTransferResponse[]> response = restTemplate.postForEntity(baseUrl + "/moneyTransfer:batch", requestEntity, MoneyTransferResponse[].class);
            MoneyTransferResponse[] result = response.getBody();
            // Enhanced debug output before assertions
            System.out.println("HTTP status: " + response.getStatusCode());
            if (result == null) {
                System.out.println("Batch transfer results: null");
            } else {
                System.out.println("Batch transfer results: " + Arrays.toString(result));
                for (int i = 0; i < result.length; i++) {
                    System.out.println("Result[" + i + "]: success=" + result[i].success() + ", errorMessage=" + result[i].errorMessage());
                }
            }
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result).isNotNull();
            assertThat(result.length).isEqualTo(2);
            assertThat(result[0].success()).isTrue();
            assertThat(result[1].success()).isTrue();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void batchMoneyTransfer_fileFormatError() {
        String baseUrl = "http://localhost:" + port;
        String txt = "1001 2001\n1002 2002 1000\n"; // first row invalid (only 2 columns)
        ByteArrayResource resource = new ByteArrayResource(txt.getBytes()) {
            @Override
            public String getFilename() {
                return "batch.txt";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<MoneyTransferResponse[]> response = restTemplate.postForEntity(baseUrl + "/moneyTransfer:batch", requestEntity, MoneyTransferResponse[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MoneyTransferResponse[] result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.length).isEqualTo(1);
        assertThat(result[0].success()).isFalse();
        assertThat(result[0].errorMessage()).contains("Invalid format");
    }

    @Test
    void canGetAccountByAccountNumber() {
        String baseUrl = "http://localhost:" + port;
        // Create an account
        OpenAccountRequest req = new OpenAccountRequest("Integration GetAccount", 888888L);
        ResponseEntity<SavingsAccount> createResp = restTemplate.postForEntity(baseUrl + "/account", req, SavingsAccount.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        SavingsAccount created = createResp.getBody();
        assertThat(created).isNotNull();
        // Retrieve the account by accountNumber
        ResponseEntity<SavingsAccount> getResp = restTemplate.getForEntity(baseUrl + "/account?accountNumber=" + created.getAccountNumber(), SavingsAccount.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        SavingsAccount got = getResp.getBody();
        assertThat(got).isNotNull();
        assertThat(got.getAccountNumber()).isEqualTo(created.getAccountNumber());
        assertThat(got.getName()).isEqualTo("Integration GetAccount");
        assertThat(got.getPersonalId()).isEqualTo(888888L);
    }

    @Test
    void canGenerateMockTransactionsFile() {
        String baseUrl = "http://localhost:" + port;
        // Ensure there are at least 4 accounts for the mock transaction generation, each with sufficient balance
        for (int i = 0; i < 4; i++) {
            OpenAccountRequest req = new OpenAccountRequest("MockUser" + i, 100000L + i);
            ResponseEntity<SavingsAccount> resp = restTemplate.postForEntity(baseUrl + "/account", req, SavingsAccount.class);
            SavingsAccount acc = resp.getBody();
            assertThat(acc).isNotNull();
            // Deposit a large amount to ensure sufficient balance
            DepositWithdrawRequest depReq = new DepositWithdrawRequest(acc.getAccountNumber(), 10000L);
            ResponseEntity<String> depResp = restTemplate.postForEntity(baseUrl + "/account:depositOrWithdraw", depReq, String.class);
            assertThat(depResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        // Request a mock transactions file with 2 sources and 2 destinations each (now POST instead of GET)
        String url = baseUrl + "/mock-transactions:download";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.util.MultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("countOfSourceAccounts", "2");
        params.add("countOfDestinationAccountsForEachSourceAccount", "2");
        HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, request, byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment; filename=mock-transactions.csv");
        byte[] body = response.getBody();
        assertThat(body).isNotNull();
        String content = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        // Should contain at least two lines, each with three columns (source, destination, amount)
        String[] lines = content.split("\\r?\\n");
        assertThat(lines.length).isGreaterThanOrEqualTo(2);
        for (String line : lines) {
            String[] parts = line.trim().split(" ");
            assertThat(parts.length).isEqualTo(3);
            // All parts should be parseable as numbers
            for (String part : parts) {
                assertThat(part).matches("\\d+");
            }
        }
    }
}

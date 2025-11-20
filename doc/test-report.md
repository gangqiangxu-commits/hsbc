## **1. Overview**

### **1.1 Unit Testing**

Unit testing validates individual code units (e.g., methods) using JUnit 5 and Mockito. Key objectives:

* Verify functional correctness  
* Isolate dependencies via mocking  
* Achieve 80%+ code coverage

### **1.2 Integration Testing**

Integration testing validates component interactions using Spring Boot Test and RestTemplate.

---

## **2. Unit Test Plan**

### **2.1 Tools**

| Tool | Purpose |
| ----- | ----- |
| JUnit 5 | Test case execution |
| Mockito | Mock external dependencies |
| JaCoCo | Code coverage analysis |

### **2.2 Coverage Report**

For more details, please open coverage/index.html using a web browser.

**Line coverage: 90%+**  
**Branch coverage: 80%+**

| Element | Missed Instructions | Cov. | Missed Branches                                        Cov. |  |
| :---- | ----- | ----: | ----- | ----: |
| Total | 102 of 1,800 | 94% | 17 of 94 | 81% |
| com.hsbc.iwpb.service |  | 94% |  | 86% |
| com.hsbc.iwpb.config |  | 0% |  | n/a |
| com.hsbc.iwpb.util |  | 89% |  | 100% |
| com.hsbc.iwpb |  | 0% |  | n/a |
| com.hsbc.iwpb.controller |  | 99% |  | 50% |
| com.hsbc.iwpb.entity |  | 98% |  | 75% |
| com.hsbc.iwpb.component |  | 100% |  | 100% |
| com.hsbc.iwpb.dto |  | 100% |  | n/a |
| total | 102 of 1,800 | 94% | 17 of 94 | 81% |

### **2.3 Sample Test Code**

@Test  
   **void** depositOrWithdraw\_insufficientFunds\_throws() {  
       SavingsAccount account \= mockAccountWithBalance(***SMALL\_BALANCE***);  
       *when*(savingsAccountMapper.findByAccountNumber(***VALID\_ACCOUNT\_NUMBER***)).thenReturn(account);  
       Exception ex \= *assertThrows*(IllegalArgumentException.**class**, () \-\>  
               savingsAccountService.depositOrWithdraw(***VALID\_ACCOUNT\_NUMBER***, ***INSUFFICIENT\_WITHDRAW\_AMOUNT***));  
       *assertTrue*(ex.getMessage().contains("Insufficient funds"));  
   }  
---

## **3. Integration Test Plan**

### **3.1 API Test Case**

Below is a sample integration test case. 

@Test  
   **void** canOpenAccountAndDeposit() {  
       String baseUrl \= "http://localhost:" \+ port;  
       // Open account  
       OpenAccountRequest req \= **new** OpenAccountRequest("Integration User", 999999L);  
       ResponseEntity\<SavingsAccount\> response \= restTemplate.postForEntity(baseUrl \+ "/account", req, SavingsAccount.**class**);  
       *assertThat*(response.getStatusCode()).isEqualTo(HttpStatus.***OK***);  
       SavingsAccount sa \= response.getBody();  
       *assertThat*(sa).isNotNull();  
       *assertThat*(sa.getName()).isEqualTo("Integration User");  
       // Deposit  
       DepositWithdrawRequest depReq \= **new** DepositWithdrawRequest(sa.getAccountNumber(), 1000);  
       ResponseEntity\<String\> depResp \= restTemplate.postForEntity(baseUrl \+ "/account:depositOrWithdraw", depReq, String.**class**);  
       *assertThat*(depResp.getStatusCode()).isEqualTo(HttpStatus.***OK***);  
   }  
 }

---

## **4. Performance Testing**

### **4.1 Scenarios**

| Scenario | Threads | Ramp-Up | Loops | Duration | Target TPS |
| ----- | ----- | ----- | ----- | ----- | ----- |
| Stress | 10 | 1s | 1000 |  | N/A |

### **4.2 Results**

Refer to performance-test-report/\*.csv (which is exported from jmeter) for more detail.

## **5. Resilience Testing**

### **5.1 Pod Recovery Test**

Refer to introduction.docx for more details.

Procedure:

\# Delete active pods  
In Alibaba cloud console, manually delete the active pods.

\# Monitor recovery  
In Alibaba cloud console, check the logs to see when the pods are created and services are started.

Results:

| Metric | Value |
| ----- | ----- |
| Pod Termination & Recreation Time | 5 seconds |
| Service Start Time | \< 25 seconds |
| Full Recovery Time | \< 30 seconds |

---

## **6. Conclusion**

### **6.1 Key Metrics**

| Category | Status | Target | Actual |
| ----- | ----- | ----- | ----- |
| Unit Test Coverage | ✅ | ≥80% | 94% & 81% |
| API Success Rate | ✅ | ≥99.9% | 99.99% |
| Failover Time | ✅ | ≤30s | 30s |

### **6.2 Recommendations**

1. Use java virtual thread to process “batch open accounts” & “batch deposit/withdraw” requests to improve performance.  
2. Adjust thread pool size for Tomcat connector


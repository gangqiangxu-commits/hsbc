# **HSBC Account Balance Processing Service**

## **Project Overview**

This project aims to build a high-performance realtime account balance calculation system that is high-available and resilient. 

For more details, please read doc/introduction.docx

## **Core Features**

* Real-Time Transaction Process: the “money transfer” transaction will be handled real-time with http request.  
* Traceable: all transactions that will cause the balance change (include both “money transfer” & deposit/withdraw transactions) will be saved in the system.

## **High Availability & Resilience**

* Kubernetes Cluster Deployment: Ensures automated management, scaling, and recovery via containerization.  
* Horizontal Pod Autoscaler (HPA): Dynamically adjusts service replicas based on real-time load.  
* Retry mechanism: resilence4j-retry is used to achieve retry mechanism.

## **Performance Optimization**

* Low-Latency Processing: Achieves millisecond-level transaction processing.  
* Java Virtual Thread: Increase performance when process “batch money transfer”.

## **Testing Strategy**

* Unit & Integration Tests: Validate module functionality using JUnit and Spring Boot Test.  
* Stress Testing: Simulates (JMeter) high concurrency to verify stability.  
* Resilience Testing: Tests pod/node failure recovery to ensure 30-seconds failover.

For more details, please check doc/test-report.md

## **Transaction Workflow**

1. Create accounts: http://localhost/swagger-ui/index.html\#/savings-account-controller/openAccount  
2. Deposit to the newly created account:  
   http://localhost/swagger-ui/index.html\#/savings-account-controller/depositWithdraw  
3. Transfer money from source account to destination account:  
   http://localhost/swagger-ui/index.html\#/savings-account-controller/processMoneyTransfer  
4. Generate a large number of transactions:   
   http://localhost/swagger-ui/index.html\#/savings-account-controller/generateMockTransactionsFile  
5. Batch process money transfer:  
   http://localhost/swagger-ui/index.html\#/savings-account-controller/batchMoneyTransfer


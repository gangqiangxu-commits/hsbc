package com.hsbc.iwpb.entity;

import java.time.LocalDateTime;

public class DepositOrWithdrawHistory {
    private long accountNumber;
    private long amount;
    private LocalDateTime createdAt;
    private long transactionId;

    public DepositOrWithdrawHistory() {}

    public DepositOrWithdrawHistory(long accountNumber, long amount, LocalDateTime createdAt, long transactionId) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.createdAt = createdAt;
        this.transactionId = transactionId;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }
    
    public int hashCode() {
		return Long.hashCode(transactionId);
	}
    
    public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.transactionId == ((DepositOrWithdrawHistory) obj).transactionId;
    }

    @Override
    public String toString() {
        return "DepositOrWithdrawHistory{" +
                "accountNumber=" + accountNumber +
                ", amount=" + amount +
                ", createdAt=" + createdAt +
                ", transactionId=" + transactionId +
                '}';
    }
}

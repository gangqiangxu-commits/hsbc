package com.hsbc.iwpb.entity;

import java.time.LocalDateTime;

public class MoneyTransferHistory {
    private long transactionId;
    private long sourceAccountNumber;
    private long destinationAccountNumber;
    private long amount;
    private LocalDateTime createdAt;

    public MoneyTransferHistory() {}

    public MoneyTransferHistory(long transactionId, long sourceAccountNumber, long destinationAccountNumber, long amount, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }
    public long getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(long sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }
    public long getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(long destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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
		return this.transactionId == ((MoneyTransferHistory) obj).transactionId;
    }
    
    @Override
    public String toString() {
        return "MoneyTransferHistory{" +
                "transactionId=" + transactionId +
                ", sourceAccountNumber=" + sourceAccountNumber +
                ", destinationAccountNumber=" + destinationAccountNumber +
                ", amount=" + amount +
                ", createdAt=" + createdAt +
                '}';
    }
}

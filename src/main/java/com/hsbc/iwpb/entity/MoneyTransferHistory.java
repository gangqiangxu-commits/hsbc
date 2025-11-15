package com.hsbc.iwpb.entity;

public class MoneyTransferHistory {
    private long transactionId;
    private long sourceAccountNumber;
    private long destinationAccountNumber;
    private long amount;
    private long createdAt;

    public MoneyTransferHistory() {}

    public MoneyTransferHistory(long transactionId, long sourceAccountNumber, long destinationAccountNumber, long amount, long createdAt) {
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
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

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

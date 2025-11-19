package com.hsbc.iwpb.entity;

import java.time.LocalDateTime;

public class SavingsAccount {
    private long accountNumber;
    private String name;
    private long personalId;
    private long balance; // unit: cents
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    public SavingsAccount() {
    }

    // new full constructor including name and personalId
    public SavingsAccount(long accountNumber, String name, long personalId, long balance, LocalDateTime createdAt, LocalDateTime lastUpdated) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.personalId = personalId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPersonalId() {
        return personalId;
    }

    public void setPersonalId(long personalId) {
        this.personalId = personalId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public int hashCode() {
		return Long.hashCode(accountNumber);
	}
    
    public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.accountNumber == ((SavingsAccount) obj).accountNumber;
    }

    @Override
    public String toString() {
        return "SavingsAccount{" +
                "accountNumber=" + accountNumber +
                ", name='" + name + '\'' +
                ", personalId=" + personalId +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}

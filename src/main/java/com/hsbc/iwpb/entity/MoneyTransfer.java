package com.hsbc.iwpb.entity;

import com.hsbc.iwpb.util.SavingsAccountUtil;

public record MoneyTransfer (
	long transactionId,
	long sourceAccountNumber,
	long destinationAccountNumber,
	// unit: cents
	long amount,
	long timestamp
) {
	
	public MoneyTransfer (
			long transactionId,
			long sourceAccountNumber,
			long destinationAccountNumber,
			// unit: cents
			long amount,
			long timestamp
		) {
		this.transactionId = transactionId;
		this.sourceAccountNumber = sourceAccountNumber;
		this.destinationAccountNumber = destinationAccountNumber;
		this.amount = amount;
		this.timestamp = timestamp;
		
		if (!SavingsAccountUtil.isTransactionAmountValid(amount)) {
			throw new IllegalArgumentException("Transaction amount not valid: " + amount);
		}
		if (!isAccountValid()) {
			throw new IllegalArgumentException("Invalid source/destination account number(s):source="
					+ sourceAccountNumber + ", destination=" + destinationAccountNumber);
		}
	}
	
	private boolean isAccountValid() {
		return SavingsAccountUtil.isAccountValid(sourceAccountNumber) && SavingsAccountUtil.isAccountValid(destinationAccountNumber);
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		MoneyTransfer that = (MoneyTransfer) obj;
		return transactionId == that.transactionId;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(transactionId);
	}
}
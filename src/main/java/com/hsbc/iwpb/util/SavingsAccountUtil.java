package com.hsbc.iwpb.util;

public class SavingsAccountUtil {
	// ...existing code...
	private SavingsAccountUtil() {
		// private constructor to prevent instantiation
	}
	
	public static boolean isAccountValid(long accountNumber) {
		// For simplicity, we consider an account valid if it's a positive number
		return accountNumber > 0;
	}
	
	public static boolean isTransactionAmountValid(long amount) {
		return amount > 0;
	}
}

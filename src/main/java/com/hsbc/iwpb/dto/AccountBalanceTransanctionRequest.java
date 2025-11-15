package com.hsbc.iwpb.dto;

public record AccountBalanceTransanctionRequest(long sourceAccountNumber,
	long destinationAccountNumber,
	// unit: cents
	long amount) {
}

package com.hsbc.iwpb.dto;

public record MoneyTransferRequest(long sourceAccountNumber,
	long destinationAccountNumber,
	// unit: cents
	long amount) {
}

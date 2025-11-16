package com.hsbc.iwpb.dto;

import com.hsbc.iwpb.entity.SavingsAccount;

public record MoneyTransferResponse(SavingsAccount sourceAccount,
	boolean success,
	String errorMessage) {
	
}

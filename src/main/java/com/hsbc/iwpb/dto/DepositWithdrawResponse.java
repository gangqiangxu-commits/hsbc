package com.hsbc.iwpb.dto;

import com.hsbc.iwpb.entity.SavingsAccount;

public record DepositWithdrawResponse(SavingsAccount account,
	boolean success,
	String errorMessage) {
	
}

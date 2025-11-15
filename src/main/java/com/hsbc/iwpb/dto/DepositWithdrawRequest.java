package com.hsbc.iwpb.dto;

// amount: negative means withdraw, positive means deposit
public record DepositWithdrawRequest(long accountNumber, long amount) {
}

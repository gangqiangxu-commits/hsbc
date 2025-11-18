package com.hsbc.iwpb.util;

import com.hsbc.iwpb.dto.MoneyTransferRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SavingsAccountUtil {
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
	
	public static List<MoneyTransferRequest> parseBatchMoneyTransferFile(BufferedReader reader) throws IOException {
	    List<MoneyTransferRequest> requests = new ArrayList<>();
	    String line;
	    int rowNum = 0;
	    while ((line = reader.readLine()) != null) {
	        rowNum++;
	        String[] parts = line.split(" ");
	        if (parts.length != 3) {
	            throw new IllegalArgumentException("Row " + rowNum + ": Invalid format, expected 3 columns but got " + parts.length);
	        }
	        try {
	            long sourceAccount = Long.parseLong(parts[0].trim());
	            long destinationAccount = Long.parseLong(parts[1].trim());
	            int amount = Integer.parseInt(parts[2].trim());
	            requests.add(new MoneyTransferRequest(sourceAccount, destinationAccount, amount));
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("Row " + rowNum + ": NumberFormatException: " + e.getMessage(), e);
	        } catch (Exception e) {
	            throw new IllegalArgumentException("Row " + rowNum + ": " + e.getMessage(), e);
	        }
	    }
	    return requests;
	}
}
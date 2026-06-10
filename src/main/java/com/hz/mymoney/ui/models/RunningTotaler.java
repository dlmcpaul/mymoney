package com.hz.mymoney.ui.models;

import java.math.BigDecimal;
import java.util.List;

public class RunningTotaler {
	BigDecimal runningTotal;
	String accountName;

	public RunningTotaler() {
		this.accountName = "";
	}

	public RunningTotaler(String accountName) {
		this.accountName = accountName;
	}

	public BigDecimal calculateTotal(int index, List<Transaction> transactions) {
		if (index == 1) {
			runningTotal = transactions.getFirst().amount();
		} else {
			runningTotal = runningTotal.add(transactions.get(index-1).amount());
		}

		if (accountName.startsWith("Income:")) {
			return runningTotal.abs();
		}

		return runningTotal;
	}

}

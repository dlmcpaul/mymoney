package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;

import java.util.List;

public class RunningTotaler {
	Money runningTotal;
	String accountName;

	public RunningTotaler() {
		this.accountName = "";
	}

	public RunningTotaler(String accountName) {
		this.accountName = accountName;
	}

	public Money calculateTotal(int index, List<Transaction> transactions) {
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

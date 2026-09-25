package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.SuperannuationAccount;

import java.util.ArrayList;
import java.util.List;

public class SuperTemplateData {
	public final List<SuperannuationAccount> superAccounts = new ArrayList<>();

	private Money sum(List<SuperannuationAccount> accounts) {
		return accounts.stream()
				.map(SuperannuationAccount::balance)
				.reduce(Money.ZERO, Money::add);
	}

	public Money getTotalIncome() {
		return sum(superAccounts);
	}
}
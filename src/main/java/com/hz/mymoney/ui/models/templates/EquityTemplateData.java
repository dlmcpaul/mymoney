package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.Account;

import java.util.ArrayList;
import java.util.List;

public class EquityTemplateData {
	public final List<Account> equityAccounts = new ArrayList<>();

	public List<Account> filterEquityAccounts(String filter) {
		String equityFilter = ("Equity:" + filter).toLowerCase();
		return equityAccounts.stream()
				.filter(account -> account.fullName().toLowerCase().startsWith(equityFilter))
				.toList();
	}

	private Money sum(List<Account> accounts) {
		return accounts.stream()
				.map(Account::balance)
				.reduce(Money.ZERO, Money::add);
	}

	public Money getTotalIncome() {
		return sum(equityAccounts);
	}
}
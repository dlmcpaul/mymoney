package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.ui.models.Account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EquityTemplateData {
	public final List<Account> equityAccounts = new ArrayList<>();

	public List<Account> filterAccounts(String filter) {
		return equityAccounts.stream()
				.filter(account -> account.fullName().startsWith(filter))
				.toList();
	}

	private BigDecimal sum(List<Account> accounts) {
		return accounts.stream()
				.map(Account::balance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalIncome() {
		return sum(equityAccounts);
	}
}
package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.ui.models.SuperannuationAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SuperTemplateData {
	public final List<SuperannuationAccount> superAccounts = new ArrayList<>();

	private BigDecimal sum(List<SuperannuationAccount> accounts) {
		return accounts.stream()
				.map(SuperannuationAccount::balance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalIncome() {
		return sum(superAccounts);
	}
}
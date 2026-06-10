package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.ui.models.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AllAccountsTemplateData {
	public final List<Account> assetAccounts = new ArrayList<>();
	public final List<Account> liabilityAccounts = new ArrayList<>();
	public final List<Account> incomeAccounts = new ArrayList<>();
	public final List<Account> expenseAccounts = new ArrayList<>();
	public final LocalDate financialYear;

	public AllAccountsTemplateData(LocalDate financialYear) {
		this.financialYear = financialYear;
	}

	private BigDecimal sum(List<Account> accounts) {
		return accounts.stream()
				.map(Account::balance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getNetPosition() {
		return sum(assetAccounts).add(sum(liabilityAccounts));
	}

	public BigDecimal getTotalIncome() {
		return sum(incomeAccounts);
	}

	public BigDecimal getTotalExpenses() {
		return sum(expenseAccounts);
	}

	public BigDecimal getHighestIncome() {
		return incomeAccounts.stream().map(Account::balance).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
	}

	public BigDecimal getHighestExpense() {
		return expenseAccounts.stream().map(Account::balance).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
	}

	public String financialPeriod() {
		return financialYear.getYear() + " - " + (financialYear.getYear() + 1);
	}
}
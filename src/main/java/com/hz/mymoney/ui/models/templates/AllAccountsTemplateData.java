package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.Account;

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

	private Money sum(List<Account> accounts) {
		return accounts.stream()
				.map(Account::balance)
				.reduce(Money.ZERO, Money::add);
	}

	public Money getNetPosition() {
		return sum(assetAccounts).add(sum(liabilityAccounts));
	}

	public Money getTotalIncome() {
		return sum(incomeAccounts);
	}

	public Money getTotalExpenses() {
		return sum(expenseAccounts);
	}

	public Money getHighestIncome() {
		return incomeAccounts.stream().map(Account::balance).max(Money::compareTo).orElse(Money.ZERO);
	}

	public Money getHighestExpense() {
		return expenseAccounts.stream().map(Account::balance).max(Money::compareTo).orElse(Money.ZERO);
	}

	public String financialPeriod() {
		return financialYear.getYear() + " - " + (financialYear.getYear() + 1);
	}
}
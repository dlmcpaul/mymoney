package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.ui.models.Account;
import com.hz.mymoney.ui.models.Transaction;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class SingleAccountTemplateData {
	public final String accountName;
	public final Account account;
	public final List<Transaction> fyTransactions;
	public final LocalDate financialYear;
	public final boolean allTransactions;

	public List<String> breadcrumb() {
		return Arrays.stream(accountName.split(":")).toList();
	}

	public String description() {
		return account.name() + (account.isShares() ? " Worth " : " With Balance ");
	}

	public String financialPeriod() {
		return allTransactions ? fyTransactions.getFirst().asAt().getYear() + " - " + (financialYear.getYear() + 1) : financialYear.getYear() + " - " + (financialYear.getYear() + 1);
	}

	public boolean showRunningTotal() {
		return allTransactions || accountName.startsWith("Income:") || accountName.startsWith("Expenses:");
	}

	public boolean isShareAccount() {
		return account.isShares();
	}

	public boolean isAmountOnly() {
		return accountName.startsWith("Income:") || accountName.startsWith("Expenses:") || accountName.startsWith("Equity:");
	}

	public BigDecimal balance() {
		if (accountName.startsWith("Assets:") || accountName.startsWith("Income:")) {
			return account.balance().abs();
		}
		return account.balance();
	}
}

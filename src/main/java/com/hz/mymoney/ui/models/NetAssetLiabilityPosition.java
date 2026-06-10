package com.hz.mymoney.ui.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NetAssetLiabilityPosition {
	public final LocalDate asAt;
	public final List<Account> assetAccounts = new ArrayList<>(100);
	public final List<Account> liabilityAccounts = new ArrayList<>(100);

	public NetAssetLiabilityPosition(LocalDate asAt) {
		this.asAt = asAt;
	}

	public void addAssetAccount(Account account) {
		assetAccounts.add(account);
	}

	public void addLiabilityAccount(Account account) {
		liabilityAccounts.add(account);
	}

	private BigDecimal sum(List<Account> accounts) {
		return accounts.stream()
				.map(Account::balance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getAssetBalance() {
		return sum(assetAccounts);
	}

	public BigDecimal getLiabilityBalance() {
		return sum(liabilityAccounts);
	}

	public BigDecimal getNetPosition() {
		return sum(assetAccounts).add(sum(liabilityAccounts));
	}

	public LocalDate getLastYear() {
		return this.asAt.withDayOfMonth(1).minusYears(1);
	}
}

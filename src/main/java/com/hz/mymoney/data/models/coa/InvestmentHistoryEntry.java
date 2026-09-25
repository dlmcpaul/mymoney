package com.hz.mymoney.data.models.coa;

import com.hz.mymoney.data.models.Money;

import java.time.LocalDate;

public record InvestmentHistoryEntry(LocalDate asAt, Money value) implements Comparable<InvestmentHistoryEntry> {
	@Override
	public int compareTo(InvestmentHistoryEntry o) {
		return o.asAt().compareTo(asAt);
	}
}

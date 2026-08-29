package com.hz.mymoney.data.models.coa;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentHistoryEntry(LocalDate asAt, BigDecimal value) implements Comparable<InvestmentHistoryEntry> {
	@Override
	public int compareTo(InvestmentHistoryEntry o) {
		return o.asAt().compareTo(asAt);
	}
}

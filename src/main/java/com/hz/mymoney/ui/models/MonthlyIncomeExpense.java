package com.hz.mymoney.ui.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyIncomeExpense(LocalDate asAt, BigDecimal incoming, BigDecimal outgoing, List<SummedTransaction> transactions) {
	public BigDecimal getBalance() {
		return incoming.subtract(outgoing.abs());
	}
}

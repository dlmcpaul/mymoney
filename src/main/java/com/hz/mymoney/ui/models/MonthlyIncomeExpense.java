package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;

import java.time.LocalDate;
import java.util.List;

public record MonthlyIncomeExpense(LocalDate asAt, Money incoming, Money outgoing, List<SummedTransaction> transactions) {
	public Money getBalance() {
		return incoming.subtract(outgoing.abs());
	}
}

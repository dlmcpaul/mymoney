package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;

public record IncomeExpense(Money actualIncome, Money actualExpense, Money projectedIncome, Money projectedExpense) {
	public Money sumActual() {
		return actualIncome.add(actualExpense);
	}

	public Money sumProjected() {
		return projectedIncome.add(projectedExpense);
	}
}

package com.hz.mymoney.ui.models.charts;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"financialYear", "income", "expense"})
public class YearlyIncomeExpense {
	public final String financialYear;
	public final BigDecimal income;
	public final BigDecimal expense;
}

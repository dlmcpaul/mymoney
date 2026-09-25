package com.hz.mymoney.ui.models.charts;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hz.mymoney.data.models.Money;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"financialYear", "income", "expense", "taxes"})
public class YearlyIncomeExpense {
	public final String financialYear;
	public final Money income;
	public final Money expense;
	public final Money taxes;
}

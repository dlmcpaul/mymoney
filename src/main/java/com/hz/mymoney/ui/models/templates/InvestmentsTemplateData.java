package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.ui.models.InvestmentSummary;
import com.hz.mymoney.ui.models.charts.InvestmentTotal;
import com.hz.mymoney.ui.models.charts.MarketValue;
import lombok.Data;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Data
public class InvestmentsTemplateData {
	public final List<InvestmentSummary> investmentSummaries = new ArrayList<>();
	public final String mode;

	public List<InvestmentSummary> getInvestmentSummaries(String code) {
		return investmentSummaries.stream()
				.filter(investmentSummary -> investmentSummary.code().equals(code))
				.toList();
	}

	public List<InvestmentTotal> getInvestmentTotals() {
		return investmentSummaries.stream().map(investmentSummary -> new InvestmentTotal(investmentSummary.code(), investmentSummary.count().intValue())).toList();
	}

	public List<MarketValue> getMarketValues() {
		return investmentSummaries.stream().map(investmentSummary -> new MarketValue(investmentSummary.code(), investmentSummary.balance())).toList();
	}

	public List<MarketValue> getTop20Profits() {
		return investmentSummaries.stream()
				.map(investmentSummary -> new MarketValue(investmentSummary.code(), investmentSummary.netProfitLoss()))
				.filter(marketValue -> marketValue.getValue().isPositive())
				.sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
				.limit(20)
				.toList();
	}

	public String getMarketValuesJson() {
		JsonMapper mapper = JsonMapper.builder()
				.build();

		// Convert the array to JSON
		if (mode.equalsIgnoreCase("current")) {
			return mapper.writeValueAsString(getMarketValues());
		}
		return mapper.writeValueAsString(getTop20Profits());
	}

	public Money getTotalProfitOrLoss() {
		return investmentSummaries.stream().map(InvestmentSummary::netProfitLoss).reduce(Money.ZERO, Money::add);
	}

	public Money getTotalOutlay() {
		return investmentSummaries.stream().map(InvestmentSummary::costBase).reduce(Money.ZERO, Money::add);
	}

	public String getMarketValue() {
		if (mode.equalsIgnoreCase("current")) {
			return getMarketValues().stream().map(MarketValue::getValue).reduce(Money.ZERO, Money::add).formatCurrency();
		}
		return getTotalProfitOrLoss().formatCurrency();
	}
}

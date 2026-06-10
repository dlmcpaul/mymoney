package com.hz.mymoney.ui.models.templates;

import com.hz.mymoney.ui.models.InvestmentSummary;
import com.hz.mymoney.ui.models.charts.InvestmentTotal;
import com.hz.mymoney.ui.models.charts.MarketValue;
import lombok.Data;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InvestmentsTemplateData {
	public final List<InvestmentSummary> investmentSummaries = new ArrayList<>();
	public final String mode;

	public List<InvestmentTotal> getInvestmentTotals() {
		return investmentSummaries.stream().map(investmentSummary -> new InvestmentTotal(investmentSummary.code(), investmentSummary.count())).toList();
	}

	public List<MarketValue> getMarketValues() {
		return investmentSummaries.stream().map(investmentSummary -> new MarketValue(investmentSummary.code(), investmentSummary.balance())).toList();
	}

	public List<MarketValue> getTotalProfitLossValues() {
		return investmentSummaries.stream()
				.map(investmentSummary -> new MarketValue(investmentSummary.code(), investmentSummary.netProfitLoss()))
				.filter(marketValue -> marketValue.value.compareTo(BigDecimal.ZERO) > 0)
				.sorted((o1, o2) -> o2.value.compareTo(o1.value))
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
		return mapper.writeValueAsString(getTotalProfitLossValues());
	}

	public BigDecimal getTotalProfitOrLoss() {
		return investmentSummaries.stream().map(InvestmentSummary::netProfitLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalOutlay() {
		return investmentSummaries.stream().map(InvestmentSummary::costBase).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}

package com.hz.mymoney.data.models.internal;

import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Log4j2
public record InvestmentHistory(Map<String, List<InvestmentHistoryEntry>> commodityMap) {

	public BigDecimal getInvestmentValue(String commodityCode, LocalDate asAt) {
		if (commodityMap.containsKey(commodityCode)) {
			return commodityMap.get(commodityCode)
					.stream()
					.filter(investmentHistoryEntry -> investmentHistoryEntry.asAt().isBefore(asAt))
					.min((o1, o2) -> o2.asAt().compareTo(o1.asAt()))
					.map(InvestmentHistoryEntry::value)
					.orElse(BigDecimal.ZERO);
		}
		log.error("commodityCode {} not found", commodityCode);
		return BigDecimal.ZERO;
	}
}

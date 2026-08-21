package com.hz.mymoney.data.models.internal;

import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@Log4j2
public record InvestmentHistory(Map<String, SortedSet<InvestmentHistoryEntry>> commodityMap) {

	// Get the first value prior to the asAt date
	public BigDecimal getInvestmentValue(String commodityCode, LocalDate asAt) {
		if (commodityMap.containsKey(commodityCode)) {
			return commodityMap.get(commodityCode)
					.stream()
					.filter(investmentHistoryEntry -> investmentHistoryEntry.asAt().isBefore(asAt))
					.findFirst()
					.map(InvestmentHistoryEntry::value)
					.orElse(BigDecimal.ZERO);
		}
		log.error("commodityCode {} not found", commodityCode);
		return BigDecimal.ZERO;
	}

	// Get the second value prior to the asAt date
	public BigDecimal getPreviousInvestmentValue(String commodityCode, LocalDate asAt) {
		if (commodityMap.containsKey(commodityCode)) {
			List<InvestmentHistoryEntry> list = commodityMap.get(commodityCode)
					.stream()
					.filter(investmentHistoryEntry -> investmentHistoryEntry.asAt().isBefore(asAt))
					.sorted((o1, o2) -> o2.asAt().compareTo(o1.asAt()))
					.toList();
			if (list.isEmpty() || list.size() == 1) {
				return BigDecimal.ZERO;
			}
			return list.get(1).value();
		}
		log.error("commodityCode {} not found", commodityCode);
		return BigDecimal.ZERO;
	}
}

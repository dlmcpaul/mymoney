package com.hz.mymoney.ui.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record InvestmentSummary(
		String code,
		int count,
		BigDecimal lastPrice,
		BigDecimal balance,
		BigDecimal costBase,
		BigDecimal sales,
		BigDecimal earnings,
		BigDecimal netProfitLoss,
		LocalDate firstPurchase,
		LocalDate lastDate
		) {

	public boolean isProfit() {
		return netProfitLoss.compareTo(BigDecimal.ZERO) >= 0;
	}

	public boolean isClosed() {
		return count == 0;
	}

	public BigDecimal yearlyReturn() {

		BigDecimal yearsOfOwnership = BigDecimal.valueOf((lastDate.toEpochDay() - firstPurchase.toEpochDay() + 364) / 365);
		BigDecimal adjustedBalance = costBase;
		BigDecimal closingBalance = isClosed() ? sales.add(earnings) : sales.add(earnings).add(balance);

		if (adjustedBalance.compareTo(BigDecimal.ZERO) == 0 || yearsOfOwnership.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}

		return closingBalance
				.subtract(adjustedBalance).setScale(4, RoundingMode.HALF_EVEN)
				.divide(adjustedBalance, RoundingMode.HALF_EVEN)
				.divide(yearsOfOwnership, RoundingMode.HALF_EVEN);
	}
}

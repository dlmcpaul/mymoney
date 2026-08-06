package com.hz.mymoney.ui.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;

public record InvestmentSummary(
		String code,
		BigDecimal count,
		BigDecimal lastPrice,
		BigDecimal priorPrice,
		BigDecimal balance,
		BigDecimal costBase,
		BigDecimal sales,
		BigDecimal earnings,
		BigDecimal netProfitLoss,
		LocalDate firstPurchase,
		LocalDate lastDate,
		String note
		) {

	public String quantity() {
		return new DecimalFormat("#,###.##").format(count);
	}

	public boolean hasNote() {
		return note != null;
	}

	public boolean isProfit() {
		return netProfitLoss.compareTo(BigDecimal.ZERO) >= 0;
	}

	public boolean isClosed() {
		return count.equals(BigDecimal.ZERO);
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

	public boolean isIncreaseInValue() {
		return lastPrice.compareTo(priorPrice) > 0;
	}
}

package com.hz.mymoney.data.utilities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class Money {
	public static final String MONEY_SYMBOL = "$";

	private Money() {}

	public static BigDecimal parseMoney(String amount, int scale) {
		if (amount.contains(MONEY_SYMBOL + "-") || amount.contains("-" + MONEY_SYMBOL)) {
			// special case 1
			return parseMoney(MONEY_SYMBOL + amount.substring(2), scale).multiply(BigDecimal.valueOf(-1));
		} else if (amount.contains(MONEY_SYMBOL + " -")) {
			// special case 2
			return parseMoney(MONEY_SYMBOL + amount.substring(3), scale).multiply(BigDecimal.valueOf(-1));
		} else if (amount.contains(".") == false) {
			// special case 3
			return parseMoney(amount + ".00", scale);
		} else if (amount.startsWith(MONEY_SYMBOL + " ")) {
			// special case 4
			return parseMoney(MONEY_SYMBOL + amount.substring(2), scale);
		} else if (amount.startsWith(MONEY_SYMBOL) == false) {
			// special case 5
			return parseMoney(MONEY_SYMBOL + amount, scale);
		}

		try {
			NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.of("en", "au"));

			if (currency instanceof DecimalFormat decimal) {
				decimal.setParseBigDecimal(true);
				return ((BigDecimal) decimal.parse(amount)).setScale(scale, RoundingMode.HALF_UP);
			}
		} catch (ParseException e) {
			throw new RuntimeException("Could not parse money: " + amount, e);
		}
		return BigDecimal.ZERO;
	}

}

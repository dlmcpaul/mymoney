package com.hz.mymoney.data.utilities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
	// Hard coded to AU for the moment.  How to support other symbols?
	public static final String MONEY_SYMBOL = NumberFormat.getCurrencyInstance(Locale.of("en", "au")).getCurrency().getSymbol();;

	private Money() {}

	public static boolean isMoney(String value) {
		return value.startsWith(Money.MONEY_SYMBOL);
	}

	public static BigDecimal parseMoney(String amount, int scale) {
		if (amount.startsWith(" ") || amount.endsWith(" ")) {
			return parseMoney(amount.trim(), scale);
		} else if (amount.startsWith(MONEY_SYMBOL + "-") || amount.startsWith("-" + MONEY_SYMBOL)) {
			// special case 1
			return parseMoney(amount.substring(2), scale).multiply(BigDecimal.valueOf(-1));
		} else if (amount.startsWith(MONEY_SYMBOL + " -")) {
			// special case 2
			return parseMoney(amount.substring(3), scale).multiply(BigDecimal.valueOf(-1));
		} else if (amount.contains(".") == false) {
			// special case 3
			return parseMoney(amount + ".00", scale);
		} else if (amount.startsWith(MONEY_SYMBOL + " ")) {
			// special case 4
			return parseMoney(amount.substring(2), scale);
		} else if (amount.startsWith(MONEY_SYMBOL)) {
			// special case 5
			return parseMoney(amount.substring(1), scale);
		}

		return new BigDecimal(amount).setScale(scale, RoundingMode.HALF_UP);
	}

}

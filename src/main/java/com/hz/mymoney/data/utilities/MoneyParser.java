package com.hz.mymoney.data.utilities;

import com.hz.mymoney.data.models.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyParser {
	// Hard coded to $ for the moment.  How to support other symbols?
	public static final String MONEY_SYMBOL = "$";
	public static final String DEFAULT_CURRENCY = "AUD";

	private MoneyParser() {}

	public static boolean isMoney(String value) {
		return value.startsWith(MoneyParser.MONEY_SYMBOL);
	}

	public static Money parseMoney(String amount, int scale) {
		String currency = DEFAULT_CURRENCY;

		if (amount.startsWith(" ") || amount.endsWith(" ")) {
			return parseMoney(amount.trim(), scale);
		} else if (amount.startsWith(MONEY_SYMBOL + "-") || amount.startsWith("-" + MONEY_SYMBOL)) {
			// special case 1
			return parseMoney(amount.substring(2), scale).negate();
		} else if (amount.startsWith(MONEY_SYMBOL + " -")) {
			// special case 2
			return parseMoney(amount.substring(3), scale).negate();
		} else if (amount.contains(".") == false) {
			// special case 3
			return parseMoney(amount + ".00", scale);
		} else if (amount.startsWith(MONEY_SYMBOL + " ")) {
			// special case 4
			return parseMoney(amount.substring(2), scale);
		} else if (amount.startsWith(MONEY_SYMBOL)) {
			// special case 5
			return parseMoney(amount.substring(1), scale);
		} else if (amount.contains(" ")) {
			// likely amount currency
			currency = amount.substring(amount.indexOf(" ") + 1);
			amount = amount.substring(0, amount.indexOf(" "));
		}

		return new Money(new BigDecimal(amount.replace(",","")).setScale(scale, RoundingMode.HALF_UP), currency);
	}

}

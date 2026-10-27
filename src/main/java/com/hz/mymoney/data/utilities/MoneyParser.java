package com.hz.mymoney.data.utilities;

import com.hz.mymoney.data.models.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.hz.mymoney.data.models.Money.DOLLAR_SYMBOL;
import static com.hz.mymoney.data.models.Money.EURO_SYMBOL;

public final class MoneyParser {
	private MoneyParser() {}

	public static boolean hasCurrencySymbol(String value) {
		return value.startsWith(DOLLAR_SYMBOL) || value.startsWith(EURO_SYMBOL);
	}

	public static Money parseMoney(String value) {

		// Basic cleanup
		if (value.startsWith(" ") || value.endsWith(" ")) {
			return parseMoney(value.trim());
		}

		// Strip + symbol as well
		if (value.contains("+")) {
			return parseMoney(value.replace("+", ""));
		}

		if (value.contains("-")) {
			return parseMoney(value.replace("-", "")).negate();
		}

		boolean hasCurrency = value.matches(".*\\d\\s+[A-Z]+");
		boolean hasCurrencySymbol = hasCurrencySymbol(value);

		String currency = "";
		String currencySymbol = "";

		if (hasCurrency) {
			currency = value.substring(value.indexOf(" ") + 1);
			value = value.substring(0, value.indexOf(" "));
		}

		if (hasCurrencySymbol) {
			currencySymbol = String.valueOf(value.charAt(0));
			value = value.substring(1).trim();
		}

		return parseMoney(value, currency, currencySymbol);
	}

	private static Money parseMoney(String value, String currency, String currencySymbol) {
		int decimalPlace = value.indexOf(".");
		if (decimalPlace != -1) {
			// determine how many decimal places to use
			return parseMoney(value, value.length() - decimalPlace - 1, currency, currencySymbol);
		}
		if (currency.length() > 0 || currencySymbol.length() > 0) {
			return parseMoney(value, 2, currency, currencySymbol);
		}
		return parseMoney(value, 0, currency, currencySymbol);
	}

	private static Money parseMoney(String value, int scale, String currency, String currencySymbol) {
		return new Money(new BigDecimal(value.replace(",","")).setScale(scale, RoundingMode.HALF_UP), currency, currencySymbol);
	}

}

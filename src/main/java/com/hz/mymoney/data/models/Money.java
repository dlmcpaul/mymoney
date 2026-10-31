package com.hz.mymoney.data.models;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public record Money(BigDecimal amount, String currency, String currencySymbol) implements Comparable<Money> {
	public static final String DOLLAR_SYMBOL = "$";
	public static final String EURO_SYMBOL = "€";

	public static final Money ZERO = new Money(BigDecimal.ZERO);
	public static final Money ONE = new Money(BigDecimal.ONE);

	public static final String AUSTRALIAN = "AUD";
	public static final String USA = "USD";
	public static final String EURO = "EUR";

	public Money(BigDecimal amount) {
		this(amount, AUSTRALIAN, DOLLAR_SYMBOL);
	}

	public Money(String amount) {
		this(new BigDecimal(amount), AUSTRALIAN, DOLLAR_SYMBOL);
	}

	public Money(BigDecimal amount, String currency) {
		this(amount, currency, switch (currency) {
			case AUSTRALIAN, USA -> DOLLAR_SYMBOL;
			case EURO -> EURO_SYMBOL;
			default -> "";
		});
	}

	public Money negate() {
		return new Money(amount.negate(), currency, currencySymbol);
	}

	public Money setScale(int scale, RoundingMode mode) {
		return new Money(amount.setScale(scale, mode), currency, currencySymbol);
	}

	public BigDecimal stripTrailingZeros() {
		return amount.stripTrailingZeros();
	}

	public Money multiply(Money multiplicand) {
		return new Money(amount.multiply(multiplicand.amount), currency, currencySymbol);
	}

	public Money divide(Money divisor, RoundingMode mode) {
		return new Money(amount.divide(divisor.amount, mode), currency, currencySymbol);
	}

	public BigDecimal divide(BigDecimal dividend, RoundingMode mode) {
		return amount.divide(dividend, mode);
	}

	public Money abs() {
		return new Money(amount.abs(), currency, currencySymbol);
	}

	public Money add(Money addend) {
		return new Money(amount.add(addend.amount), currency, currencySymbol);
	}

	public Money subtract(Money subtrahend) {
		return new Money(amount.subtract(subtrahend.amount), currency, currencySymbol);
	}

	public int compareTo(Money money) {
		return amount.compareTo(money.amount);
	}

	public static Money valueOf(int amount) {
		return new Money(BigDecimal.valueOf(amount), AUSTRALIAN, DOLLAR_SYMBOL);
	}

	public String formatShares() {
		return amount.stripTrailingZeros().toPlainString();
	}

	// Used by front end to display currency values
	public String formatCurrency() {
		return switch (currency) {
			case AUSTRALIAN -> NumberFormat.getCurrencyInstance(Locale.of("en", "au")).format(amount);
			case USA -> NumberFormat.getCurrencyInstance(Locale.of("en", "us")).format(amount);
			case EURO -> NumberFormat.getCurrencyInstance(Locale.of("en", "eu")).format(amount);
			default -> toString();
		};
	}

	// Used when writing to file
	@Override
	public @NotNull String toString() {
		if (currency.isEmpty() && currencySymbol.isEmpty()) {
			return amount.stripTrailingZeros().toPlainString();
		}

		String format = currencySymbol + "#0.00####" + (currency.isEmpty() || currency.equalsIgnoreCase(AUSTRALIAN) ? "" : " " + currency);
		return new DecimalFormat(format).format(amount.stripTrailingZeros());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Money money)) return false;
		return amount.compareTo(money.amount) == 0 && Objects.equals(currencySymbol, money.currencySymbol) && Objects.equals(currency, money.currency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(amount.stripTrailingZeros().toPlainString(), currencySymbol, currency);
	}
}

package com.hz.mymoney.data.models;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

@Data
public class Money implements Comparable<Money> {
	public static final Money ZERO = new Money(BigDecimal.ZERO);
	public static final Money ONE = new Money(BigDecimal.ONE);

	public static final String AUSTRALIAN = "AUD";
	public static final String USA = "USD";
	public static final String EURO = "EUR";

	private BigDecimal amount;
	private final String currency;

	public Money(BigDecimal amount) {
		this.amount = amount;
		this.currency = AUSTRALIAN;
	}

	public Money(String amount) {
		this.amount = new BigDecimal(amount);
		this.currency = AUSTRALIAN;
	}

	public Money(BigDecimal amount, String currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public Money negate() {
		return new Money(amount.negate(), currency);
	}

	public Money setScale(int scale, RoundingMode mode) {
		amount = amount.setScale(scale, mode);
		return this;
	}

	public BigDecimal stripTrailingZeros() {
		return amount.stripTrailingZeros();
	}

	public Money multiply(Money multiplicand) {
		return new Money(amount.multiply(multiplicand.amount), currency);
	}

	public Money divide(Money divisor, RoundingMode mode) {
		return new Money(amount.divide(divisor.amount, mode), currency);
	}

	public BigDecimal divide(BigDecimal dividend, RoundingMode mode) {
		return amount.divide(dividend, mode);
	}

	public Money abs() {
		return new Money(amount.abs(), currency);
	}

	public Money add(Money addend) {
		return new Money(amount.add(addend.amount), currency);
	}

	public Money subtract(Money subtrahend) {
		return new Money(amount.subtract(subtrahend.amount), currency);
	}

	public int compareTo(Money money) {
		return amount.compareTo(money.amount);
	}

	public boolean equals(Money money) {
		return amount.equals(money.amount);
	}

	public int hashCode() {
		return amount.hashCode();
	}

	public static Money valueOf(int amount) {
		return new Money(BigDecimal.valueOf(amount), AUSTRALIAN);
	}

	// Used by front end to display currency values
	public String formatCurrency() {
		return switch (currency) {
			case AUSTRALIAN -> NumberFormat.getCurrencyInstance(Locale.of("en", "au")).format(amount);
			case USA -> NumberFormat.getCurrencyInstance(Locale.of("en", "us")).format(amount);
			default -> toString();
		};
	}

	// Used when writing to file
	@Override
	public String toString() {
		return switch (currency) {
			case AUSTRALIAN -> new DecimalFormat("$#0.00####").format(amount.stripTrailingZeros());
			case USA -> new DecimalFormat("$#0.00#### USD").format(amount.stripTrailingZeros());
			default -> amount.stripTrailingZeros().toPlainString() + " " + currency;
		};
	}

}

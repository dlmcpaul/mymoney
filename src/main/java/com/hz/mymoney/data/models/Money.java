package com.hz.mymoney.data.models;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

@Log4j2
public record Money(BigDecimal amount, String currency, String currencySymbol) implements Comparable<Money> {
	public static final String DOLLAR_SYMBOL = "$";
	public static final String EURO_SYMBOL = "€";

	public static final String AUSTRALIAN = "AUD";
	public static final String USA = "USD";
	public static final String EURO = "EUR";

	// Current constant defaults are AUD
	public static final Money ZERO = Money.zero(AUSTRALIAN, DOLLAR_SYMBOL);
	public static final Money ONE = new Money(BigDecimal.ONE, AUSTRALIAN, DOLLAR_SYMBOL);

	public Money(BigDecimal amount) {
		this(Objects.requireNonNull(amount), AUSTRALIAN, DOLLAR_SYMBOL);
	}

	public Money(String amount) {
		this(new BigDecimal(Objects.requireNonNull(amount)), AUSTRALIAN, DOLLAR_SYMBOL);
	}

	public Money(String amount, String currency) {
		this(new BigDecimal(Objects.requireNonNull(amount)), Objects.requireNonNull(currency));
	}

	public Money(BigDecimal amount, String currency) {
		this(Objects.requireNonNull(amount), Objects.requireNonNull(currency), switch (currency) {
			case AUSTRALIAN, USA -> DOLLAR_SYMBOL;
			case EURO -> EURO_SYMBOL;
			default -> "";
		});
	}

	public static Money createFrom(BigDecimal newAmount, Money from) {
		return new Money(newAmount, from.currency, from.currencySymbol);
	}

	public Money abs() {
		return new Money(amount.abs(), currency, currencySymbol);
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
		return new Money(amount.multiply(multiplicand.amount), multiplicand.currency, multiplicand.currencySymbol);
	}

	public Money multiply(BigDecimal multiplicand) {
		return new Money(amount.multiply(multiplicand), currency, currencySymbol);
	}

	public Money divide(Money divisor, RoundingMode mode) {
		return new Money(amount.divide(divisor.amount, mode), divisor.currency, divisor.currencySymbol);
	}

	public BigDecimal divide(BigDecimal dividend, RoundingMode mode) {
		return amount.divide(dividend, mode);
	}

	public Money add(Money addend) {
		if (this.isZero()) {
			return new Money(addend.amount, addend.currency, addend.currencySymbol);
		}
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

	public int signum() {
		return this.amount.signum();
	}

	public boolean isZero() {
		return signum() == 0;
	}

	public boolean isNonZero() {
		return signum() != 0;
	}

	public boolean isPositive() {
		return signum() == 1;
	}

	public boolean isPositiveOrZero() {
		return signum() >= 0;
	}

	public boolean isNegative() {
		return signum() == -1;
	}

	public boolean isNegativeOrZero() {
		return signum() <= 0;
	}

	public int getScale() {
		return this.amount.scale();
	}

	public static Money zero(String currency) {
		return new Money(BigDecimal.ZERO, currency);
	}

	public static Money zero(String currency, String currencySymbol) {
		return new Money(BigDecimal.ZERO, currency, currencySymbol);
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

		if (currencySymbol.equals(DOLLAR_SYMBOL) && currency.isEmpty() && amount.scale() == 2) {
			return NumberFormat.getCurrencyInstance(Locale.of("en", "au")).format(amount);
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

package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.coa.Movement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction (
	LocalDate asAt,
	String description,
	BigDecimal amount,
	String destinationAccount,
	String sourceAccount,
	boolean isShares,
	BigDecimal sharePrice) {

	public Transaction(Movement movement, String destinationAccount, boolean isShares) {
		this(   movement.date(),
				movement.getNote(),
				isShares ? movement.amount() : movement.getValue(),
				destinationAccount,
				movement.sourceAccount(),
				isShares,
				movement.price());
	}

	public BigDecimal debitAmount() {
		if (isDebit()) {
			return amount.abs();
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal creditAmount() {
		if (isDebit()) {
			return BigDecimal.ZERO;
		}
		return amount.abs();
	}

	public boolean isCredit() {
		return isDebit() == false;
	}

	public boolean isDebit() {
		boolean negative = amount.compareTo(BigDecimal.ZERO) < 0;

		return switch (destinationAccount.substring(0,destinationAccount.indexOf(":")).toLowerCase()) {
			case "assets", "expenses" -> negative == false;
			case "liabilities", "income", "equity" -> negative;
			default ->
				throw new IllegalStateException("Unexpected value: " + destinationAccount.substring(0,destinationAccount.indexOf(":")));
		};
	}
}

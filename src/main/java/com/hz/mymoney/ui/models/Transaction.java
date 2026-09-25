package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.coa.Movement;

import java.time.LocalDate;

public record Transaction (
	LocalDate asAt,
	String description,
	Money amount,
	String destinationAccount,
	String sourceAccount,
	boolean isShares,
	Money sharePrice) {

	public Transaction(Movement movement, String destinationAccount, boolean isShares) {
		this(   movement.date(),
				movement.getNote(),
				isShares ? movement.amount() : movement.getValue(),
				destinationAccount,
				movement.sourceAccount(),
				isShares,
				movement.price());
	}

	private String formattedAmount(Money money) {
		if (isShares) {
			return money.getAmount().stripTrailingZeros().toPlainString();
		}
		return money.formatCurrency();
	}

	public String debitAmount() {
		if (isDebit()) {
			return formattedAmount(amount.abs());
		}
		return formattedAmount(Money.ZERO);
	}

	public String creditAmount() {
		if (isDebit()) {
			return formattedAmount(Money.ZERO);
		}
		return formattedAmount(amount.abs());
	}

	public boolean isCredit() {
		return isDebit() == false;
	}

	public boolean isDebit() {
		boolean negative = amount.compareTo(Money.ZERO) < 0;

		return switch (destinationAccount.substring(0,destinationAccount.indexOf(":")).toLowerCase()) {
			case "assets", "expenses" -> negative == false;
			case "liabilities", "income", "equity" -> negative;
			default ->
				throw new IllegalStateException("Unexpected value: " + destinationAccount.substring(0,destinationAccount.indexOf(":")));
		};
	}
}

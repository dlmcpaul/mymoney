package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.internal.Movement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.PriorityQueue;
import java.util.stream.Stream;

public record SuperannuationAccount(String name, String fullName, BigDecimal balance, LocalDate openingDate, LocalDate closingDate, PriorityQueue<Movement> movements) {

	public BigDecimal adminCosts() {
		return taxes().add(insurance()).add(fees());
	}

	private BigDecimal sum(Stream<BigDecimal> stream) {
		return stream.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_EVEN);
	}

	public BigDecimal contributions() {
		return sum(movements.stream()
				.filter(movement -> movement.isSuperContribution() || movement.isSuperTransferIn())
				.map(movement -> movement.amount().abs()));
	}

	public BigDecimal earnings() {
		return sum(movements.stream()
				.filter(Movement::isSuperEarnings)
				.map(Movement::amount));
	}

	public BigDecimal losses() {
		return sum(movements.stream()
				.filter(Movement::isSuperLosses)
				.map(Movement::amount))
				.abs();
	}

	public BigDecimal openingBalance() {
		return sum(movements.stream()
				.filter(Movement::isSuperOpeningBalance)
				.map(Movement::amount))
				.abs();
	}

	public BigDecimal taxes() {
		return sum(movements.stream()
				.filter(Movement::isSuperTaxes)
				.map(Movement::amount))
				.abs();
	}

	public BigDecimal insurance() {
		return sum(movements.stream()
				.filter(Movement::isSuperInsurance)
				.map(Movement::amount))
				.abs();
	}

	public BigDecimal fees() {
		return sum(movements.stream()
				.filter(Movement::isSuperFees)
				.map(Movement::amount))
				.abs();
	}

	public BigDecimal transfersOut() {
		return sum(movements.stream()
				.filter(Movement::isSuperTransferOut)
				.map(Movement::amount))
				.abs();
	}

	public boolean isClosed() {
		return balance.compareTo(BigDecimal.ZERO) == 0;
	}

	public BigDecimal yearlyReturn() {
		BigDecimal yearsOpen = BigDecimal.valueOf((closingDate.toEpochDay() - openingDate.toEpochDay() + 364) / 365);
		BigDecimal adjustedBalance = openingBalance().add(contributions());
		BigDecimal closingBalance = isClosed() ? transfersOut() : balance();

		if (adjustedBalance.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}

		return closingBalance
				.subtract(adjustedBalance).setScale(4, RoundingMode.HALF_EVEN)
				.divide(adjustedBalance, RoundingMode.HALF_EVEN)
				.divide(yearsOpen, RoundingMode.HALF_EVEN);
	}
}

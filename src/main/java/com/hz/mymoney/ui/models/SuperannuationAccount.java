package com.hz.mymoney.ui.models;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.coa.Movement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.PriorityQueue;
import java.util.stream.Stream;

public record SuperannuationAccount(String name, String fullName, Money balance, LocalDate openingDate, LocalDate closingDate, PriorityQueue<Movement> movements) {

	public Money adminCosts() {
		return taxes().add(insurance()).add(fees());
	}

	private Money sum(Stream<Money> stream) {
		return stream.reduce(Money.ZERO, Money::add)
				.setScale(2, RoundingMode.HALF_EVEN);
	}

	public Money contributions() {
		return sum(movements.stream()
				.filter(movement -> movement.isSuperContribution() || movement.isSuperTransferIn())
				.map(movement -> movement.amount().abs()));
	}

	public Money earnings() {
		return sum(movements.stream()
				.filter(Movement::isSuperEarnings)
				.map(Movement::amount));
	}

	public Money losses() {
		return sum(movements.stream()
				.filter(Movement::isSuperLosses)
				.map(Movement::amount))
				.abs();
	}

	public Money openingBalance() {
		return sum(movements.stream()
				.filter(Movement::isSuperOpeningBalance)
				.map(Movement::amount))
				.abs();
	}

	public Money taxes() {
		return sum(movements.stream()
				.filter(Movement::isSuperTaxes)
				.map(Movement::amount))
				.abs();
	}

	public Money insurance() {
		return sum(movements.stream()
				.filter(Movement::isSuperInsurance)
				.map(Movement::amount))
				.abs();
	}

	public Money fees() {
		return sum(movements.stream()
				.filter(Movement::isSuperFees)
				.map(Movement::amount))
				.abs();
	}

	public Money transfersOut() {
		return sum(movements.stream()
				.filter(Movement::isSuperTransferOut)
				.map(Movement::amount))
				.abs();
	}

	public boolean isClosed() {
		return balance.compareTo(Money.ZERO) == 0;
	}

	public BigDecimal yearlyReturn() {
		BigDecimal yearsOpen = BigDecimal.valueOf((closingDate.toEpochDay() - openingDate.toEpochDay() + 364) / 365);
		Money adjustedBalance = openingBalance().add(contributions());
		Money closingBalance = isClosed() ? transfersOut() : balance();

		if (adjustedBalance.compareTo(Money.ZERO) == 0) {
			return BigDecimal.ZERO;
		}

		return closingBalance
				.subtract(adjustedBalance).setScale(4, RoundingMode.HALF_EVEN)
				.divide(adjustedBalance, RoundingMode.HALF_EVEN)
				.divide(yearsOpen, RoundingMode.HALF_EVEN);
	}
}
